//! Deriver trait and implementations
//!
//! The deriver is responsible for applying inference rules to the current
//! state of the reasoner and generating new tasks.

pub mod brute_force;
pub mod rule;
pub mod rule_based;

use crate::memory::simple::SimpleMemory;
use crate::task::Task;
use crate::focus::{FocusBag, PriTree};

/// The `Deriver` trait defines the interface for deriving new tasks.
pub trait Deriver {
    /// Derives new tasks from the current state of the NAR.
    ///
    /// # Arguments
    ///
    /// * `memory` - A reference to the memory.
    /// * `focus_bag` - A mutable reference to the focus bag.
    /// * `pri_tree` - A mutable reference to the priority tree.
    ///
    /// # Returns
    ///
    /// A vector of new tasks.
    fn derive(&mut self, memory: &SimpleMemory, focus_bag: &mut FocusBag, pri_tree: &mut PriTree) -> Vec<Task>;
}
