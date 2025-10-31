//! Main entry point for the NAR system

use nar::NAR;

fn main() {
    println!("NAR (Non-Axiomatic Reasoner) - Rust Implementation");
    println!("=================================================");
    
    // Create a new NAR instance
    let mut nar = NAR::new();
    
    // Input some initial knowledge
    println!("\nInputting initial knowledge...");
    nar.input_sentence("cat.").expect("Failed to input 'cat.'");
    nar.input_sentence("dog.").expect("Failed to input 'dog.'");
    nar.input_sentence("animal.").expect("Failed to input 'animal.'");
    
    // Show initial state
    let stats = nar.stats();
    println!("\nInitial state:");
    println!("  Time: {}", stats.time);
    println!("  Concepts: {}", stats.concepts);
    
    // Run a few cycles of reasoning
    println!("\nRunning reasoning cycles...");
    for i in 1..=5 {
        nar.cycle();
        let stats = nar.stats();
        println!("  Cycle {}: {} concepts", i, stats.concepts);
    }
    
    // Show final state
    let stats = nar.stats();
    println!("\nFinal state:");
    println!("  Time: {}", stats.time);
    println!("  Concepts: {}", stats.concepts);
    println!("  Active concepts: {}", stats.active_concepts);
    
    println!("\nNAR execution completed.");
}