//! Main entry point for the NAR system

use nar::NAR;

fn main() {
    println!("NAR (Non-Axiomatic Reasoner) - Rust Implementation");
    println!("=================================================");
    
    // Create a new NAR instance
    let mut nar = NAR::new();
    
    // Input some initial knowledge
    println!("\nInputting initial knowledge...");
    nar.input_sentence("<cat --> animal>.").expect("Failed to input sentence.");
    nar.input_sentence("<dog --> animal>.").expect("Failed to input sentence.");
    nar.input_sentence("<cat --> furry>.").expect("Failed to input sentence.");
    
    // Show initial state
    let stats = nar.stats();
    println!("\nInitial state:");
    println!("  Time: {}", stats.time);
    println!("  Concepts: {}", stats.concepts);
    
    // Run a few cycles of reasoning
    println!("\nRunning reasoning cycles...");
    for _ in 1..=10 {
        nar.cycle();
    }
    
    // Show final state
    let stats = nar.stats();
    println!("\nFinal state:");
    println!("  Time: {}", stats.time);
    println!("  Concepts: {}", stats.concepts);
    println!("  Active concepts: {}", stats.active_concepts);
    
    println!("\nNAR execution completed.");
}