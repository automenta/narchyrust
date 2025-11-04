//! Inference engine for NARS

pub mod truth;

use crate::task::{Task, Punctuation};
use crate::term::{Term, Op, TermTrait};
use crate::truth::Truth;
use crate::task::TaskBuilder;

/// Perform inference on a concept and a task
pub fn inference(task1: &Task, task2: &Task) -> Option<Task> {
    if let (Term::Compound(c1), Term::Compound(c2)) = (task1.term(), task2.term()) {
        if c1.op_id() == Op::Inheritance && c2.op_id() == Op::Inheritance {
            let a = &c1.subterms()[0];
            let b1 = &c1.subterms()[1];
            let b2 = &c2.subterms()[0];
            let c = &c2.subterms()[1];

            if b1 == b2 {
                // Deduction: (A --> B) and (B --> C) => (A --> C)
                let new_term = Term::Compound(crate::term::compound::Compound::new(Op::Inheritance, vec![a.clone(), c.clone()]));
                let new_task = TaskBuilder::new()
                    .term(new_term)
                    .truth(Truth::default_belief()) // Placeholder truth
                    .punctuation(Punctuation::Belief)
                    .build()
                    .unwrap();
                return Some(new_task);
            }
        }
    }
    None
}

#[cfg(test)]
mod test_deduction;
#[cfg(test)]
mod test_truth;
