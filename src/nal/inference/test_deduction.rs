//! Deduction test for NARS

use crate::deriver::Deriver;
use crate::focus::Focus;
use crate::deriver::rule::RuleDeriver;
use crate::memory::simple::SimpleMemory;
use crate::task::{Task, Punctuation, Budget};
use crate::term::{Term, atom::Atomic, compound::Compound, Op};
use crate::control::budget::DefaultBudget;
use crate::truth::Truth;


#[test]
fn test_deduction() {
    let mut deriver = RuleDeriver::new();
    let mut memory = SimpleMemory::new(10);
    let budget: Box<dyn crate::control::budget::Budget> = Box::new(DefaultBudget::default());

    // Input beliefs
    let bird = Term::Atomic(Atomic::new_atom("bird"));
    let animal = Term::Atomic(Atomic::new_atom("animal"));
    let robin = Term::Atomic(Atomic::new_atom("robin"));

    let belief1_term = Term::Compound(Compound::new(Op::Inheritance, vec![bird.clone(), animal.clone()]));
    let belief1_truth = Truth::new(1.0, 0.9);
    let belief1_task = Task::with_auto_id(
        belief1_term.clone(),
        Some(belief1_truth),
        Punctuation::Belief,
        crate::task::Time::Eternal,
        Budget::default(),
        vec![],
        0,
    );
    let mut concept1 = crate::concept::TaskConcept::new(bird.clone());
    concept1.add_task(belief1_task);
    memory.add_concept(concept1);

    let belief2_term = Term::Compound(Compound::new(Op::Inheritance, vec![robin.clone(), bird.clone()]));
    let belief2_truth = Truth::new(1.0, 0.9);
    let belief2_task = Task::with_auto_id(
        belief2_term.clone(),
        Some(belief2_truth),
        Punctuation::Belief,
        crate::task::Time::Eternal,
        Budget::default(),
        vec![],
        0,
    );
    let mut concept2 = crate::concept::TaskConcept::new(robin.clone());
    concept2.add_task(belief2_task);
    memory.add_concept(concept2);

    // Run cycles
    let new_tasks = deriver.next(&Focus::new(belief2_term, Some(belief2_truth)), &mut memory, &budget);

    // Check for derived belief
    assert_eq!(new_tasks.len(), 1);
    let derived_task = &new_tasks[0];
    let expected_term = Term::Compound(Compound::new(Op::Inheritance, vec![robin, animal]));
    assert_eq!(derived_task.term(), &expected_term);

    // Check the truth value
    let expected_truth = Truth::new(1.0, 0.81);
    assert_eq!(*derived_task.truth().unwrap(), expected_truth);
}
