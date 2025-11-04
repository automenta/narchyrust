
use crate::nal::nar::NAR;
use crate::time::Time;
use crate::concept::util::ConceptBuilder;
use crate::truth::Truth;

#[test]
fn test_deduction_truth_value() {
    let time = Time::new();
    let concept_builder = ConceptBuilder::new();
    let mut nar = NAR::new(time, concept_builder);

    // Input beliefs
    nar.input_string("<robin --> bird>.").unwrap();
    nar.input_string("<bird --> animal>.").unwrap();

    // Run the reasoning cycle
    for _ in 0..10 {
        nar.cycle(None);
    }

    // Check if the conclusion is derived
    let conclusion_term = crate::parser::parse_narsese("<robin --> animal>.").unwrap().remove(0).term().clone();
    let belief = nar.get_belief(&conclusion_term);
    assert!(belief.is_some());

    // Check the truth value
    let truth1 = Truth::default_belief();
    let truth2 = Truth::default_belief();
    let expected_truth = crate::nal::inference::truth::deduction(&truth1, &truth2);
    assert_eq!(belief.unwrap().truth(), Some(&expected_truth));
}
