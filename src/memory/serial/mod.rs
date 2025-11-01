//! Memory system in NARS (V2)
//!
//! This is a new single-threaded implementation of the memory system.

use crate::concept::TaskConcept;
use crate::term::Term;
use std::collections::HashMap;
use std::fmt;

/// Memory struct representing the NARS memory system
#[derive(Debug)]
pub struct Memory {
    /// Concepts stored in memory using a hash map
    concepts: HashMap<Term, TaskConcept>,

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
            concepts: HashMap::with_capacity(capacity),
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
        self.concepts.get(term)
    }

    /// Get a mutable reference to a concept by term
    pub fn get_concept_mut(&mut self, term: &Term) -> Option<&mut TaskConcept> {
        self.concepts.get_mut(term)
    }

    /// Add or update a concept in memory
    pub fn add_concept(&mut self, concept: TaskConcept) {
        self.concepts.insert(concept.term().clone(), concept);
    }

    /// Create a concept for a term if it doesn't exist
    pub fn get_or_create_concept(&mut self, term: &Term) -> &mut TaskConcept {
        self.concepts
            .entry(term.clone())
            .or_insert_with(|| TaskConcept::new(term.clone()))
    }

    /// Remove a concept from memory
    pub fn remove_concept(&mut self, term: &Term) -> Option<TaskConcept> {
        self.concepts.remove(term)
    }

    /// Get all concepts
    pub fn concepts(&self) -> impl Iterator<Item = &TaskConcept> {
        self.concepts.values()
    }

    /// Clear all concepts from memory
    pub fn clear(&mut self) {
        self.concepts.clear();
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
