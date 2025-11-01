//! Main entry point for the NAR system

use nar::NAR;
use nar::time::Time;
use nar::concept::util::ConceptBuilder;

fn main() {
    println!("NAR (Non-Axiomatic Reasoner) - Rust Implementation");
    println!("=================================================");
    
    // Create a new NAR instance
    let time = Time::new();
    let concept_builder = ConceptBuilder::new();
    let mut nar = NAR::new(time, concept_builder);
    
    // Input some initial knowledge
    println!("\nInputting initial knowledge...");
    nar.input_string("<cat --> animal>.").expect("Failed to input sentence.");
    nar.input_string("<dog --> animal>.").expect("Failed to input sentence.");
    nar.input_string("<cat --> furry>.").expect("Failed to input sentence.");
    
    // Run a few cycles of reasoning
    println!("\nRunning reasoning cycles...");
    for _ in 1..=10 {
        nar.cycle();
    }
    
    println!("\nNAR execution completed.");
}