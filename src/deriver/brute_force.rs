//! Serial deriver implementation

use crate::deriver::Deriver;
use crate::deriver::rule::{Rule, RuleTree};
use crate::task::Task;
use crate::focus::{FocusBag, PriTree};
use crate::parser;

/// A simple serial deriver that processes one task at a time.
pub struct BruteForceDeriver {
    rule_tree: RuleTree,
}

impl BruteForceDeriver {
    pub fn new() -> Self {
        let mut rule_tree = RuleTree::new();
        match parser::load_nal_files() {
            Ok(rules) => {
                println!("Loaded {} rules", rules.len());
                for task in rules {
                    if let Some(truth) = task.truth() {
                        let rule = Rule {
                            term: task.term().clone(),
                            truth: truth.clone(),
                        };
                        rule_tree.add(rule);
                    }
                }
            },
            Err(e) => {
                eprintln!("Failed to load NAL files: {}", e);
            }
        }
        Self { rule_tree }
    }
}

use crate::memory::simple::SimpleMemory;
use crate::term::{Term, compound::Compound, Op, TermTrait};
use crate::truth::Truth;

impl Deriver for BruteForceDeriver {
    fn derive(&mut self, memory: &SimpleMemory, _focus_bag: &mut FocusBag, _pri_tree: &mut PriTree) -> Vec<Task> {
        self.derive_syllogism(memory)
    }
}

impl BruteForceDeriver {
    pub fn derive_syllogism(&mut self, memory: &SimpleMemory) -> Vec<Task> {
        let mut new_tasks = Vec::new();

        let concepts = memory.concepts().cloned().collect::<Vec<_>>();

        for i in 0..concepts.len() {
            for j in 0..concepts.len() {
                if i == j {
                    continue;
                }

                let c1 = &concepts[i];
                let c2 = &concepts[j];

                if let (Some(t1), Some(t2)) = (c1.beliefs().highest_priority(), c2.beliefs().highest_priority()) {
                    let term1 = t1.term();
                    let term2 = t2.term();

                    if let Term::Compound(c1_compound) = term1 {
                        if let Term::Compound(c2_compound) = term2 {
                            if c1_compound.op_id() == Op::Inheritance && c2_compound.op_id() == Op::Inheritance {
                                let subj1 = &c1_compound.subterms()[0];
                                let pred1 = &c1_compound.subterms()[1];
                                let subj2 = &c2_compound.subterms()[0];
                                let pred2 = &c2_compound.subterms()[1];

                                if pred1 == subj2 {
                                    if let (Some(truth1), Some(truth2)) = (t1.truth(), t2.truth()) {
                                        // Found a syllogistic relationship: subj1 -> pred1 and pred1 -> pred2
                                        // Create a new task: subj1 -> pred2
                                        let new_term = Term::Compound(Compound::new(Op::Inheritance, vec![subj1.clone(), pred2.clone()]));

                                        let new_truth = Truth::deduction(truth1, truth2);

                                        let new_task = Task::new(
                                            new_term,
                                            Some(new_truth),
                                            crate::task::Punctuation::Belief,
                                            crate::task::Time::Eternal,
                                            crate::task::Budget::default(),
                                            0, // id
                                            vec![], // evidence
                                            0 // creation time
                                        );
                                        new_tasks.push(new_task);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        new_tasks
    }
}
