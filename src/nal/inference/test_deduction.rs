//! Deduction test for NARS

use crate::nal::nar::NAR;
use crate::time::Time;
use crate::concept::util::ConceptBuilder;
use crate::focus::Focus;

#[test]
fn test_deduction() {
    let time = Time::new();
    let concept_builder = ConceptBuilder::new();
    let mut nar = NAR::new(time, concept_builder);

    // Input beliefs
    nar.input_string("(bird --> animal).").unwrap();
    nar.input_string("(robin --> bird).").unwrap();

    // Run cycles
    let beliefs_to_focus = vec![
        crate::parser::parse_narsese("(robin --> bird).").unwrap().remove(0).term().clone(),
        crate::parser::parse_narsese("(bird --> animal).").unwrap().remove(0).term().clone(),
    ];
    for belief in beliefs_to_focus {
        nar.cycle(Some(Focus::new(belief)));
    }

    // Check for derived belief
    let tasks = crate::parser::parse_narsese("(robin --> animal).").unwrap();
    assert_eq!(tasks.len(), 1);
    let derived_term = tasks[0].term().clone();
    let belief = nar.get_belief(&derived_term);
    assert!(belief.is_some());
}
