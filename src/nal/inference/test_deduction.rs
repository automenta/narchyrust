//! Deduction test for NARS

use crate::nal::nar::NAR;
use crate::time::Time;
use crate::concept::util::ConceptBuilder;

#[test]
fn test_deduction() {
    let time = Time::new();
    let concept_builder = ConceptBuilder::new();
    let mut nar = NAR::new(time, concept_builder);

    // Input beliefs
    nar.input_string("<bird --> animal>.").unwrap();
    nar.input_string("<robin --> bird>.").unwrap();

    // Run cycles
    for _ in 0..10 {
        nar.cycle();
    }

    // Check for derived belief
    let tasks = crate::parser::parse_narsese("<robin --> animal>.").unwrap();
    assert_eq!(tasks.len(), 1);
    let derived_term = tasks[0].term().clone();
    let belief = nar.get_belief(&derived_term);
    assert!(belief.is_some());
}
