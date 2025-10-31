//! A simple memory implementation with LRU eviction.

use crate::concept::TaskConcept;
use crate::term::Term;
use lru::LruCache;
use std::num::NonZeroUsize;

/// A memory implementation that uses an LRU cache to evict concepts.
#[derive(Debug)]
pub struct SimpleMemory {
    concepts: LruCache<Term, TaskConcept>,
}

impl SimpleMemory {
    /// Create a new `SimpleMemory` with a given capacity.
    pub fn new(capacity: usize) -> Self {
        SimpleMemory {
            concepts: LruCache::new(NonZeroUsize::new(capacity).unwrap()),
        }
    }

    /// Get a concept by term.
    pub fn get_concept(&mut self, term: &Term) -> Option<&TaskConcept> {
        self.concepts.get(term)
    }

    /// Get a mutable reference to a concept by term.
    pub fn get_concept_mut(&mut self, term: &Term) -> Option<&mut TaskConcept> {
        self.concepts.get_mut(term)
    }

    /// Add or update a concept in memory.
    pub fn add_concept(&mut self, concept: TaskConcept) {
        self.concepts.put(concept.term().clone(), concept);
    }

    /// Get or create a concept for a term.
    pub fn get_or_create_concept(&mut self, term: &Term) -> &mut TaskConcept {
        if !self.concepts.contains(term) {
            let concept = TaskConcept::new(term.clone());
            self.concepts.put(term.clone(), concept);
        }
        self.concepts.get_mut(term).unwrap()
    }

    /// Get the number of concepts in memory.
    pub fn len(&self) -> usize {
        self.concepts.len()
    }

    /// Check if the memory is empty.
    pub fn is_empty(&self) -> bool {
        self.concepts.is_empty()
    }

    /// Get an iterator over the concepts in the memory.
    pub fn concepts(&self) -> impl Iterator<Item = &TaskConcept> {
        self.concepts.iter().map(|(_, v)| v)
    }

    /// Clear the memory.
    pub fn clear(&mut self) {
        self.concepts.clear();
    }

    /// Apply activation decay to all concepts.
    pub fn decay_activation(&mut self, rate: f32) {
        for (_, concept) in self.concepts.iter_mut() {
            concept.decay_activation(rate);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::term::atom::Atomic;

    #[test]
    fn test_simple_memory_creation() {
        let memory = SimpleMemory::new(10);
        assert!(memory.is_empty());
    }

    #[test]
    fn test_simple_memory_eviction() {
        let mut memory = SimpleMemory::new(2);
        let term1 = Term::Atomic(Atomic::new_atom("cat"));
        let term2 = Term::Atomic(Atomic::new_atom("dog"));
        let term3 = Term::Atomic(Atomic::new_atom("bird"));

        memory.add_concept(TaskConcept::new(term1.clone()));
        memory.add_concept(TaskConcept::new(term2.clone()));
        memory.add_concept(TaskConcept::new(term3.clone()));

        assert_eq!(memory.len(), 2);
        assert!(memory.get_concept(&term2).is_some());
        assert!(memory.get_concept(&term3).is_some());
        assert!(memory.get_concept(&term1).is_none());
    }

    #[test]
    fn test_simple_memory_lru_eviction() {
        let mut memory = SimpleMemory::new(3);
        let term1 = Term::Atomic(Atomic::new_atom("cat"));
        let term2 = Term::Atomic(Atomic::new_atom("dog"));
        let term3 = Term::Atomic(Atomic::new_atom("bird"));
        let term4 = Term::Atomic(Atomic::new_atom("fish"));

        memory.add_concept(TaskConcept::new(term1.clone()));
        memory.add_concept(TaskConcept::new(term2.clone()));
        memory.add_concept(TaskConcept::new(term3.clone()));

        // Access term1 to make it the most recently used
        memory.get_concept(&term1);

        // Add a new concept, which should evict term2
        memory.add_concept(TaskConcept::new(term4.clone()));

        assert_eq!(memory.len(), 3);
        assert!(memory.get_concept(&term1).is_some());
        assert!(memory.get_concept(&term3).is_some());
        assert!(memory.get_concept(&term4).is_some());
        assert!(memory.get_concept(&term2).is_none());
    }
}
