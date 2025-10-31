//! Memory system in NARS (V2)
//!
//! This is a new single-threaded implementation of the memory system.

pub mod radix_tree;

use crate::concept::TaskConcept;
use crate::term::{Term, TermTrait};
use crate::memory::serial::radix_tree::RadixTree;
use std::fmt;
use rand::Rng;

/// Memory struct representing the NARS memory system
#[derive(Debug)]
pub struct Memory {
    /// Concepts stored in memory using a radix tree
    concepts: RadixTree<TaskConcept>,

    /// Maximum number of concepts in memory
    capacity: usize,
}

impl Memory {
    /// Create a new memory with default settings
    pub fn new() -> Self {
        Memory::with_capacity(10000)
    }

    /// Create a new memory with a specific capacity
    pub fn with_capacity(capacity: usize) -> Self {
        Memory {
            concepts: RadixTree::new(capacity),
            capacity,
        }
    }

    /// Get the number of concepts in memory
    pub fn len(&self) -> usize {
        self.concepts.len()
    }

    /// Check if memory is empty
    pub fn is_empty(&self) -> bool {
        self.concepts.is_empty()
    }

    /// Get a concept by term
    pub fn get_concept(&self, term: &Term) -> Option<&TaskConcept> {
        let key = Self::term_to_key(term);
        self.concepts.get(&key)
    }

    /// Get a mutable reference to a concept by term
    pub fn get_concept_mut(&mut self, term: &Term) -> Option<&mut TaskConcept> {
        let key = Self::term_to_key(term);
        self.concepts.get_mut(&key)
    }

    /// Add or update a concept in memory
    pub fn add_concept(&mut self, concept: TaskConcept) {
        let key = Self::term_to_key(concept.term());
        self.concepts.insert(key, concept);
    }

    /// Create a concept for a term if it doesn't exist
    pub fn get_or_create_concept(&mut self, term: &Term) -> &mut TaskConcept {
        let key = Self::term_to_key(term);

        if !self.concepts.contains_key(&key) {
            let concept = TaskConcept::new(term.clone());
            self.concepts.insert(key.clone(), concept);
        }
        self.concepts.get_mut(&key).unwrap()
    }

    /// Remove a concept from memory
    pub fn remove_concept(&mut self, term: &Term) -> Option<TaskConcept> {
        let key = Self::term_to_key(term);
        self.concepts.remove(&key)
    }

    /// Apply activation decay to all concepts
    pub fn decay_activation(&mut self, rate: f32) {
        for concept in self.concepts.values_mut() {
            concept.decay_activation(rate);
        }
    }

    /// Forget concepts to stay within capacity
    pub fn forget_concepts(&mut self) {
        let overflow = self.concepts.len().saturating_sub(self.capacity);
        if overflow > 0 {
            // A more sophisticated implementation would use a more targeted approach
            // For now, we'll remove a random selection of concepts
            let mut keys_to_remove = Vec::new();
            let mut all_keys = Vec::new();
            // This is inefficient, but it's a starting point
            // A better approach would be to have the radix tree provide a way to get all keys
            // or to iterate over the concepts and collect their keys
            for concept in self.concepts.values() {
                all_keys.push(Self::term_to_key(concept.term()));
            }

            let mut rng = rand::thread_rng();
            for _ in 0..overflow {
                if all_keys.is_empty() {
                    break;
                }
                let index = rng.gen_range(0..all_keys.len());
                keys_to_remove.push(all_keys.swap_remove(index));
            }

            for key in keys_to_remove {
                self.concepts.remove(&key);
            }
        }
    }

    /// Get all concepts
    pub fn concepts(&self) -> Vec<&TaskConcept> {
        self.concepts.values()
    }

    /// Convert a term to a byte sequence for use as a key in the radix tree
    fn term_to_key(term: &Term) -> Vec<u8> {
        let complexity = term.complexity() as u16;
        let mut key = Vec::with_capacity(2 + 32);
        key.extend_from_slice(&complexity.to_be_bytes());
        key.extend(format!("{}", term.concept()).as_bytes());
        key
    }
}

impl Default for Memory {
    fn default() -> Self {
        Self::new()
    }
}

impl fmt::Display for Memory {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        writeln!(f, "Memory: {} concepts", self.len())?;
        writeln!(f, "  Capacity: {}", self.capacity)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::term::atom::Atomic;

    #[test]
    fn test_memory_creation() {
        let memory = Memory::new();
        assert!(memory.is_empty());
        assert_eq!(memory.len(), 0);
    }

    #[test]
    fn test_concept_management() {
        let mut memory = Memory::new();
        let term = Term::Atomic(Atomic::new_atom("cat"));

        // Get or create concept
        let concept = memory.get_or_create_concept(&term);
        assert_eq!(concept.term(), &term);
        assert_eq!(memory.len(), 1);

        // Get existing concept
        let concept_ref = memory.get_concept(&term);
        assert!(concept_ref.is_some());
        assert_eq!(concept_ref.unwrap().term(), &term);

        // Remove concept
        let removed = memory.remove_concept(&term);
        assert!(removed.is_some());
        assert_eq!(memory.len(), 0);
    }
}