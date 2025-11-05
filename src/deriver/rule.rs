//! Rule-based deriver for NARS

use crate::deriver::Deriver;
use crate::focus::Focus;
use crate::memory::simple::SimpleMemory;
use crate::task::{Task, Punctuation, Time, Budget};
use crate::term::{Term, TermTrait};
use crate::nal::{unify::unify, truth_functions};
use crate::parser;
use crate::control::budget;
use std::collections::HashMap;
use crate::truth::Truth;
use std::fmt;

/// Represents a single inference rule.
pub struct Rule {
    pub premises: Vec<Term>,
    pub conclusion: Term,
    pub truth_function: Box<dyn truth_functions::TruthFunction>,
}

impl fmt::Debug for Rule {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("Rule")
            .field("premises", &self.premises)
            .field("conclusion", &self.conclusion)
            .finish()
    }
}

/// A deriver that uses a set of rules to derive new tasks.
pub struct RuleDeriver {
    rules: Vec<Rule>,
}

impl RuleDeriver {
    /// Create a new rule deriver and load rules from `.nal` files.
    pub fn new() -> Self {
        let mut rules = Vec::new();
        // This is a temporary solution that only parses the syllogism rule.
        rules.push(parser::parse_syllogism_rule());
        RuleDeriver { rules }
    }
}

impl Deriver for RuleDeriver {
    fn next(&mut self, focus: &Focus, memory: &mut SimpleMemory, budget: &Box<dyn budget::Budget>) -> Vec<Task> {
        let mut new_tasks = Vec::new();

        let all_beliefs: Vec<&Task> = memory.concepts()
            .flat_map(|c| c.beliefs().tasks())
            .collect();

        for rule in &self.rules {
            if let Some(p1_pattern) = rule.premises.get(0) {
                let mut initial_bindings = HashMap::new();
                if unify(p1_pattern, &focus.id, &mut initial_bindings) {
                    let other_premises = &rule.premises[1..];
                    let other_beliefs: Vec<&Task> = all_beliefs.iter()
                        .filter(|b| b.term() != &focus.id)
                        .map(|b| *b)
                        .collect();

                    let solutions = find_matches(other_premises, &other_beliefs, initial_bindings);
                    for (bindings, premises) in solutions {
                        let mut full_premises_truths: Vec<Truth> = vec![focus.truth.unwrap()];
                        full_premises_truths.extend(premises.iter().map(|t| t.truth().unwrap()));
                        let full_premises: Vec<&Truth> = full_premises_truths.iter().collect();

                        let new_term = apply_bindings(&rule.conclusion, &bindings);
                        let truth = rule.truth_function.call(&full_premises);
                        let mut new_task = Task::with_auto_id(
                            new_term,
                            Some(truth),
                            Punctuation::Belief,
                            Time::Eternal,
                            Budget::default(),
                            vec![],
                            0,
                        );
                        let priority = budget.pri_derived(&new_task, &*self);
                        new_task.budget_mut().set_priority(priority);
                        new_tasks.push(new_task);
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
    beliefs: &'a [&'a Task],
    bindings: HashMap<Term, Term>,
) -> Vec<(HashMap<Term, Term>, Vec<&'a Task>)> {
    if premises.is_empty() {
        return vec![(bindings, Vec::new())];
    }

    let mut solutions = Vec::new();
    let p1 = &premises[0];

    for belief in beliefs {
        let mut current_bindings = bindings.clone();
        if unify(p1, belief.term(), &mut current_bindings) {
            let sub_solutions = find_matches(&premises[1..], beliefs, current_bindings);
            for (sub_bindings, mut sub_premises) in sub_solutions {
                sub_premises.insert(0, belief);
                solutions.push((sub_bindings, sub_premises));
            }
        }
    }

    solutions
}


/// Apply bindings to a term.
fn apply_bindings(term: &Term, bindings: &HashMap<Term, Term>) -> Term {
    term.transform(&mut |t| {
        if let Term::Variable(_) = t {
            if let Some(bound_term) = bindings.get(t) {
                return bound_term.clone();
            }
        }
        t.clone()
    })
}


#[cfg(test)]
mod tests {
    use super::*;
    use crate::term::{atom::Atomic, compound::Compound, Op};
    use crate::memory::simple::SimpleMemory;
    use crate::control::budget::DefaultBudget;

    #[test]
    fn test_derivation_loop_multi_premise() {
        let mut deriver = RuleDeriver::new();
        let mut memory = SimpleMemory::new(10);
        let budget: Box<dyn budget::Budget> = Box::new(DefaultBudget::default());

        // Create a memory with beliefs: (robin --> bird) and (bird --> animal)
        let robin = Term::Atomic(Atomic::new_atom("robin"));
        let bird = Term::Atomic(Atomic::new_atom("bird"));
        let animal = Term::Atomic(Atomic::new_atom("animal"));

        let belief1_term = Term::Compound(Compound::new(Op::Inheritance, vec![robin.clone(), bird.clone()]));
        let belief1_truth = Truth::new(1.0, 0.9);
        let belief1_task = Task::with_auto_id(
            belief1_term.clone(),
            Some(belief1_truth),
            Punctuation::Belief,
            Time::Eternal,
            Budget::default(),
            vec![],
            0,
        );
        let mut concept1 = crate::concept::TaskConcept::new(robin.clone());
        concept1.add_task(belief1_task);
        memory.add_concept(concept1);

        let belief2_term = Term::Compound(Compound::new(Op::Inheritance, vec![bird.clone(), animal.clone()]));
        let belief2_truth = Truth::new(1.0, 0.9);
        let belief2_task = Task::with_auto_id(
            belief2_term.clone(),
            Some(belief2_truth),
            Punctuation::Belief,
            Time::Eternal,
            Budget::default(),
            vec![],
            0,
        );
        let mut concept2 = crate::concept::TaskConcept::new(bird.clone());
        concept2.add_task(belief2_task);
        memory.add_concept(concept2);

        // Run the deriver
        let new_tasks = deriver.next(&Focus::new(belief1_term, Some(belief1_truth)), &mut memory, &budget);

        // Check the derived task
        assert_eq!(new_tasks.len(), 1);
        let derived_task = &new_tasks[0];
        let expected_term = Term::Compound(Compound::new(Op::Inheritance, vec![robin, animal]));
        assert_eq!(derived_task.term(), &expected_term);
    }
}
