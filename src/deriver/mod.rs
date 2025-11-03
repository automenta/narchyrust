//! Deriver trait and implementations
//!
//! The deriver is responsible for applying inference rules to the current
//! state of the reasoner and generating new tasks.

pub mod rule;
pub mod reaction;

use crate::focus::Focus;
use crate::memory::simple::SimpleMemory;
use crate::task::Task;

use crate::deriver::reaction::ReactionModel;

/// The `Deriver` trait defines the interface for deriving new tasks.
pub trait Deriver {
    /// Takes a focused concept and derives new tasks.
    ///
    /// # Arguments
    ///
    /// * `focus` - The concept to focus on.
    /// * `memory` - A reference to the memory.
    ///
    /// # Returns
    ///
    /// A vector of new tasks.
    fn next(&mut self, focus: &Focus, memory: &mut SimpleMemory) -> Vec<Task>;

    /// Sets the reaction model for the deriver.
    ///
    /// # Arguments
    ///
    /// * `model` - The reaction model to use.
    fn set_reaction_model(&mut self, model: ReactionModel);
}
