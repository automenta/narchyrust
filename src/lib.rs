//! NAR (Non-Axiomatic Reasoner) - A Rust implementation of the Non-Axiomatic Reasoning System
//!
//! This crate provides a framework for implementing Non-Axiomatic Logic (NAL), which is the
//! logical foundation of the Non-Axiomatic Reasoning System (NARS). NARS is a general-purpose
//! artificial intelligence system that realizes a methodology of intelligent reasoning that
//! is not based on mathematical logic, but on a novel approach to intelligence.

pub mod term;
pub mod truth;
pub mod task;
pub mod concept;
pub mod memory;
pub mod table;
pub mod bag;
pub mod nal;
pub mod parser;
pub mod focus;
pub mod time;

// Re-export the main components for easier access
pub use term::Term;
pub use truth::Truth;
pub use task::Task;
pub use concept::{Concept, TaskConcept};
pub use nal::NAR;

#[cfg(test)]
mod tests {
    #[test]
    fn it_works() {
        assert_eq!(2 + 2, 4);
    }
}