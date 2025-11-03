//! Rule-based deriver for NARS

//! Rule-based deriver for NARS

use crate::deriver::Deriver;
use crate::focus::Focus;
use crate::memory::simple::SimpleMemory;
use crate::task::{Task, Punctuation, Time, Budget};
use crate::term::{Term, TermTrait, Op};
use crate::nal::unify::unify;
use crate::parser;
use crate::control::budget;
use std::collections::HashMap;

/// A deriver that uses a set of rules to derive new tasks.
pub struct RuleDeriver {
    rules: Vec<Term>,
}

impl RuleDeriver {
    /// Create a new rule deriver and load rules from `.nal` files.
    pub fn new() -> Self {
        let mut rules = Vec::new();
        match parser::load_nal_files() {
            Ok(tasks) => {
                for task in tasks {
                    rules.push(task.term().clone());
                }
            }
            Err(e) => {
                eprintln!("Failed to load rules: {}", e);
            }
        }
        RuleDeriver { rules }
    }
}

impl Deriver for RuleDeriver {
    fn next(&mut self, focus: &Focus, memory: &mut SimpleMemory, budget: &Box<dyn budget::Budget>) -> Vec<Task> {
        let mut new_tasks = Vec::new();

        let all_beliefs: Vec<Term> = memory.concepts()
            .flat_map(|c| c.beliefs().tasks())
            .map(|t| t.term().clone())
            .collect();

        for rule in &self.rules {
            if let Term::Compound(rule_compound) = rule {
                if rule_compound.op_id() == Op::Rule {
                    let premises = &rule_compound.subterms()[0].subterms();
                    let conclusion_pattern = &rule_compound.subterms()[1];

                    for (i, p1) in premises.iter().enumerate() {
                        let mut initial_bindings = HashMap::new();
                        if unify(p1, &focus.id, &mut initial_bindings) {
                            let other_premises: Vec<Term> = premises.iter().enumerate()
                                .filter(|(j, _)| *j != i)
                                .map(|(_, p)| p.clone())
                                .collect();

                            let other_beliefs: Vec<Term> = all_beliefs.iter()
                                .filter(|b| **b != focus.id)
                                .cloned()
                                .collect();

                            let solutions = find_matches(&other_premises, &other_beliefs, initial_bindings);
                            for bindings in solutions {
                                let new_term = apply_bindings(conclusion_pattern, &bindings);
                                let mut new_task = Task::with_auto_id(
                                    new_term,
                                    None, // Simplified for now
                                    Punctuation::Belief,
                                    Time::Eternal,
                                    Budget::default(),
                                    vec![],
                                    0,
                                );
                                // Set the budget using the pri_derived function
                                let priority = budget.pri_derived(&new_task, &*self);
                                new_task.budget_mut().set_priority(priority);
                                new_tasks.push(new_task);
                            }
                        }
                    }
                }
            }
        }
        new_tasks
    }

    fn set_reaction_model(&mut self, _model: crate::deriver::reaction::ReactionModel) {
        // No-op for now
    }
}

/// Recursively find belief combinations that satisfy all premises.
fn find_matches<'a>(
    premises: &'a [Term],
    beliefs: &'a [Term],
    bindings: HashMap<&'a Term, &'a Term>,
) -> Vec<HashMap<&'a Term, &'a Term>> {
    if premises.is_empty() {
        return vec![bindings];
    }

    let mut solutions = Vec::new();
    let p1 = &premises[0];

    for belief in beliefs {
        let mut current_bindings = bindings.clone();
        if unify(p1, belief, &mut current_bindings) {
            let sub_solutions = find_matches(&premises[1..], beliefs, current_bindings);
            solutions.extend(sub_solutions);
        }
    }

    solutions
}

/// Apply bindings to a term.
fn apply_bindings(term: &Term, bindings: &HashMap<&Term, &Term>) -> Term {
    term.transform(&mut |t| {
        if let Term::Variable(_) = t {
            if let Some(bound_term) = bindings.get(t) {
                return (*bound_term).clone();
            }
        }
        t.clone()
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::term::{atom::Atomic, compound::Compound, var::Variable};
    use crate::memory::simple::SimpleMemory;
    use crate::task::{Task, Punctuation, Budget, Time};
    use crate::control::budget::DefaultBudget;
    use crate::focus::Focus;

    #[test]
    fn test_derivation_loop_single_premise() {
        // Create a rule: (%S --> %M) |- (%S ==> %M)
        let s = Term::Variable(Variable::new_pattern("S"));
        let m = Term::Variable(Variable::new_pattern("M"));
        let premise = Term::Compound(Compound::new(Op::Inheritance, vec![s.clone(), m.clone()]));
        let conclusion = Term::Compound(Compound::new(Op::Implication, vec![s.clone(), m.clone()]));
        let rule = Term::Compound(Compound::new(Op::Rule, vec![Term::Compound(Compound::new(Op::Product, vec![premise])), conclusion]));

        let mut deriver = RuleDeriver { rules: vec![rule] };

        // Create a memory with a belief: (cat --> animal)
        let mut memory = SimpleMemory::new(10);
        let cat = Term::Atomic(Atomic::new_atom("cat"));
        let animal = Term::Atomic(Atomic::new_atom("animal"));
        let belief_term = Term::Compound(Compound::new(Op::Inheritance, vec![cat.clone(), animal.clone()]));
        let belief_task = Task::with_auto_id(
            belief_term.clone(),
            None,
            Punctuation::Belief,
            Time::Eternal,
            Budget::default(),
            vec![],
            0,
        );
        let mut concept = crate::concept::TaskConcept::new(cat.clone());
        concept.add_task(belief_task);
        memory.add_concept(concept);

        // Run the deriver
        let budget: Box<dyn budget::Budget> = Box::new(DefaultBudget::default());
        let new_tasks = deriver.next(&Focus::new(belief_term.clone()), &mut memory, &budget);

        // Check the derived task
        assert_eq!(new_tasks.len(), 1);
        let derived_task = &new_tasks[0];
        let expected_term = Term::Compound(Compound::new(Op::Implication, vec![cat, animal]));
        assert_eq!(derived_task.term(), &expected_term);
    }

    #[test]
    fn test_derivation_loop_multi_premise() {
        // Create a rule for syllogism: (&& (%S --> %M) (%M --> %P)) |- (%S --> %P)
        let s = Term::Variable(Variable::new_pattern("S"));
        let m = Term::Variable(Variable::new_pattern("M"));
        let p = Term::Variable(Variable::new_pattern("P"));
        let premise1 = Term::Compound(Compound::new(Op::Inheritance, vec![s.clone(), m.clone()]));
        let premise2 = Term::Compound(Compound::new(Op::Inheritance, vec![m.clone(), p.clone()]));
        let premises = Term::Compound(Compound::new(Op::Product, vec![premise1, premise2]));
        let conclusion = Term::Compound(Compound::new(Op::Inheritance, vec![s.clone(), p.clone()]));
        let rule = Term::Compound(Compound::new(Op::Rule, vec![premises, conclusion]));

        let mut deriver = RuleDeriver { rules: vec![rule] };

        // Create a memory with beliefs: (robin --> bird) and (bird --> animal)
        let mut memory = SimpleMemory::new(10);
        let robin = Term::Atomic(Atomic::new_atom("robin"));
        let bird = Term::Atomic(Atomic::new_atom("bird"));
        let animal = Term::Atomic(Atomic::new_atom("animal"));

        let belief1_term = Term::Compound(Compound::new(Op::Inheritance, vec![robin.clone(), bird.clone()]));
        let belief1_task = Task::with_auto_id(
            belief1_term.clone(), None, Punctuation::Belief, Time::Eternal, Budget::default(), vec![], 0,
        );
        let mut concept1 = crate::concept::TaskConcept::new(robin.clone());
        concept1.add_task(belief1_task);
        memory.add_concept(concept1);

        let belief2_term = Term::Compound(Compound::new(Op::Inheritance, vec![bird.clone(), animal.clone()]));
        let belief2_task = Task::with_auto_id(
            belief2_term, None, Punctuation::Belief, Time::Eternal, Budget::default(), vec![], 0,
        );
        let mut concept2 = crate::concept::TaskConcept::new(bird.clone());
        concept2.add_task(belief2_task);
        memory.add_concept(concept2);

        // Run the deriver
        let budget: Box<dyn budget::Budget> = Box::new(DefaultBudget::default());
        let new_tasks = deriver.next(&Focus::new(belief1_term.clone()), &mut memory, &budget);

        // Check the derived task
        assert_eq!(new_tasks.len(), 1);
        let derived_task = &new_tasks[0];
        let expected_term = Term::Compound(Compound::new(Op::Inheritance, vec![robin, animal]));
        assert_eq!(derived_task.term(), &expected_term);
    }
}
