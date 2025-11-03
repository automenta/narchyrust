
use crate::deriver::reaction::ReactionModel;
use crate::deriver::Deriver;
use crate::focus::Focus;
use crate::memory::simple::SimpleMemory;
use crate::task::{Budget, Punctuation, Task, Time};
use crate::term::compound::Compound;
use crate::term::{Op, Term, TermTrait};
use crate::truth::Truth;

pub struct SyllogisticDeriver;

impl SyllogisticDeriver {
    pub fn new() -> Self {
        SyllogisticDeriver
    }
}

impl Deriver for SyllogisticDeriver {
    fn next(&mut self, _focus: &Focus, memory: &mut SimpleMemory) -> Vec<Task> {
        let mut new_tasks = Vec::new();

        let all_beliefs: Vec<Task> = memory
            .concepts()
            .flat_map(|c| c.beliefs().tasks())
            .map(|t| (*t).clone())
            .collect();

        if all_beliefs.len() < 2 {
            return new_tasks;
        }

        for i in 0..all_beliefs.len() {
            for j in 0..all_beliefs.len() {
                if i == j {
                    continue;
                }

                let t1 = &all_beliefs[i]; // Premise 1: S --> M
                let t2 = &all_beliefs[j]; // Premise 2: M --> P

                if let (Term::Compound(c1), Term::Compound(c2)) = (t1.term(), t2.term()) {
                    if c1.op_id() == Op::Inheritance && c2.op_id() == Op::Inheritance {
                        let subj1 = &c1.subterms()[0];
                        let pred1 = &c1.subterms()[1];
                        let subj2 = &c2.subterms()[0];
                        let pred2 = &c2.subterms()[1];

                        // Match the middle term M: pred1 of t1 must equal subj2 of t2
                        if pred1 == subj2 {
                            if let (Some(truth1), Some(truth2)) = (t1.truth(), t2.truth()) {
                                // Derive conclusion: S --> P
                                let new_term = Term::Compound(Compound::new(
                                    Op::Inheritance,
                                    vec![subj1.clone(), pred2.clone()],
                                ));

                                // Apply deduction rule to truth values
                                let new_truth = Truth::deduction(truth2, truth1);

                                let new_task = Task::with_auto_id(
                                    new_term,
                                    Some(new_truth),
                                    Punctuation::Belief,
                                    Time::Eternal,
                                    Budget::default(),
                                    vec![t1.id(), t2.id()], // evidence
                                    0,                      // creation time
                                );
                                new_tasks.push(new_task);
                            }
                        }
                    }
                }
            }
        }

        new_tasks
    }

    fn set_reaction_model(&mut self, _model: ReactionModel) {
        // No-op for now
    }
}
