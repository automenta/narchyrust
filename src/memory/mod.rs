//! Memory system in NARS
//!
//! The memory manages the collection of concepts and provides mechanisms for:
//! - Storing and retrieving concepts
//! - Managing concept activation
//! - Handling concept forgetting
//! - Maintaining attention dynamics
//!
//! This implementation uses a radix tree for efficient concept storage and retrieval.

pub mod radix_tree;
pub mod serial;

use crate::concept::TaskConcept;
use crate::term::{Term, TermTrait};
use crate::memory::radix_tree::RadixTree;
use std::fmt;

/// Memory struct representing the NARS memory system
#[derive(Debug)]
pub struct Memory {
    /// Concepts stored in memory using a radix tree
    concepts: RadixTree<TaskConcept>,
    
    /// Maximum number of concepts in memory
    capacity: usize,
    
    /// Forgetting rate (how quickly activation decays)
    forgetting_rate: f32,
    
    /// Minimum activation for keeping a concept
    min_activation: f32,
    
    /// Concept linking parameters
    linking: LinkingParams,
}

/// Parameters for concept linking
#[derive(Debug, Clone)]
pub struct LinkingParams {
    /// Maximum number of termlinks per concept
    pub max_termlinks: usize,
    
    /// Maximum number of tasklinks per concept
    pub max_tasklinks: usize,
    
    /// Probability of creating a new link when processing a task
    pub link_creation_prob: f32,
}

impl Default for LinkingParams {
    fn default() -> Self {
        LinkingParams {
            max_termlinks: 10,
            max_tasklinks: 10,
            link_creation_prob: 0.1,
        }
    }
}

impl Memory {
    /// Create a new memory with default settings
    pub fn new() -> Self {
        Memory::with_capacity_and_linking(10000, LinkingParams::default())
    }
    
    /// Create a new memory with a specific capacity
    pub fn with_capacity(capacity: usize) -> Self {
        Memory::with_capacity_and_linking(capacity, LinkingParams::default())
    }
    
    /// Create a new memory with specific capacity and linking parameters
    pub fn with_capacity_and_linking(capacity: usize, linking: LinkingParams) -> Self {
        Memory {
            concepts: RadixTree::new(capacity),
            capacity,
            forgetting_rate: 0.1,
            min_activation: 0.01,
            linking,
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
    pub fn get_concept(&self, term: &Term) -> Option<TaskConcept> {
        let key = Self::term_to_key(term);
        self.concepts.get(&key)
    }
    
    /// Get a mutable reference to a concept by term
    /// Note: This requires removing and re-inserting the concept
    pub fn get_concept_mut(&mut self, term: &Term) -> Option<TaskConcept> {
        let key = Self::term_to_key(term);
        self.concepts.get(&key)
    }
    
    /// Add or update a concept in memory
    pub fn add_concept(&mut self, concept: TaskConcept) {
        let key = Self::term_to_key(concept.term());
        self.concepts.insert(key, concept);
        
        if self.concepts.len() > self.capacity {
            self.perform_garbage_collection();
        }
    }

    /// Perform garbage collection to remove excess concepts
    fn perform_garbage_collection(&mut self) {
        let overflow = self.concepts.len().saturating_sub(self.capacity);
        if overflow > 0 {
            let mut concepts: Vec<TaskConcept> = self.concepts.values();

            concepts.sort_by(|a, b| {
                a.activation().partial_cmp(&b.activation()).unwrap()
            });

            for concept in concepts.iter().take(overflow) {
                let key = Self::term_to_key(concept.term());
                self.concepts.remove(&key);
            }
        }
    }
    
    /// Create a concept for a term if it doesn't exist
    pub fn get_or_create_concept(&mut self, term: &Term) -> TaskConcept {
        let key = Self::term_to_key(term);
        
        if let Some(concept) = self.concepts.get(&key) {
            concept
        } else {
            let concept = TaskConcept::new(term.clone());
            self.concepts.insert(key, concept.clone());
            concept
        }
    }
    
    /// Remove a concept from memory
    pub fn remove_concept(&mut self, term: &Term) -> Option<TaskConcept> {
        let key = Self::term_to_key(term);
        self.concepts.remove(&key)
    }
    
    /// Apply activation decay to all concepts
    pub fn decay_activation(&mut self, rate: f32) {
        // Get all concepts and decay their activation in-place
        let concepts_to_update: Vec<TaskConcept> = self.concepts.values();

        for concept in concepts_to_update {
            let key = Self::term_to_key(concept.term());
            self.concepts.update_with(&key, |c| {
                c.decay_activation(rate);
            });
        }
    }
    
    /// Forget concepts with low activation
    pub fn forget_concepts(&mut self) {
        let concepts_to_check: Vec<TaskConcept> = self.concepts.values();

        for concept in concepts_to_check {
            if concept.activation() < self.min_activation {
                let key = Self::term_to_key(concept.term());
                self.concepts.remove(&key);
            }
        }
    }
    
    /// Get all concepts
    pub fn concepts(&self) -> Vec<TaskConcept> {
        self.concepts.values()
    }
    
    /// Get concepts above an activation threshold
    pub fn active_concepts(&self, threshold: f32) -> Vec<TaskConcept> {
        self.concepts
            .values()
            .into_iter()
            .filter(|concept| concept.activation() >= threshold)
            .collect()
    }
    
    /// Get the most active concepts
    pub fn most_active_concepts(&self, count: usize) -> Vec<TaskConcept> {
        let mut concepts: Vec<TaskConcept> = self.concepts.values();
        concepts.sort_by(|a, b| {
            b.activation().partial_cmp(&a.activation()).unwrap()
        });
        concepts.truncate(count);
        concepts
    }
    
    /// Clear all concepts from memory
    pub fn clear(&mut self) {
        // Create a new radix tree to clear all concepts
        self.concepts = RadixTree::new(self.capacity);
    }
    
    /// Set the forgetting rate
    pub fn set_forgetting_rate(&mut self, rate: f32) {
        self.forgetting_rate = rate.clamp(0.0, 1.0);
    }
    
    /// Set the minimum activation threshold
    pub fn set_min_activation(&mut self, min: f32) {
        self.min_activation = min.clamp(0.0, 1.0);
    }
    
    /// Get the capacity
    pub fn capacity(&self) -> usize {
        self.capacity
    }
    
    /// Set the capacity
    pub fn set_capacity(&mut self, capacity: usize) {
        self.capacity = capacity;
        
        // Create a new radix tree with the new capacity
        let mut new_concepts = RadixTree::new(capacity);
        
        // Transfer all concepts to the new tree
        for concept in self.concepts.values() {
            let key = Self::term_to_key(concept.term());
            new_concepts.insert(key, concept);
        }
        
        self.concepts = new_concepts;
    }
    
    /// Get the linking parameters
    pub fn linking(&self) -> &LinkingParams {
        &self.linking
    }
    
    /// Set the linking parameters
    pub fn set_linking(&mut self, linking: LinkingParams) {
        self.linking = linking;
    }
    
    /// Create links between concepts when processing a task
    pub fn create_links(&mut self, task: &crate::task::Task) {
        // Add termlinks based on the task's term structure
        self.create_termlinks_for_term(task.term());
        
        // Get the key for this task's concept
        let key = Self::term_to_key(task.term());
        
        // Add a tasklink to the concept
        if let Some(mut concept) = self.concepts.get(&key) {
            concept.add_tasklink(task.id());
            
            // Limit the number of tasklinks
            if concept.tasklinks().len() > self.linking.max_tasklinks {
                // For now, just truncate
                // A more sophisticated implementation might prioritize based on relevance
            }
            
            // Reinsert the modified concept
            self.concepts.insert(key, concept);
        }
    }
    
    /// Create termlinks for a term
    fn create_termlinks_for_term(&mut self, term: &Term) {
        // For compound terms, collect subterms first to avoid borrowing issues
        let subterms = if let Term::Compound(compound) = term {
            compound.subterms().to_vec()
        } else {
            Vec::new()
        };
        
        // Add termlinks based on the term structure
        let key = Self::term_to_key(term);
        if let Some(mut concept) = self.concepts.get(&key) {
            // For compound terms, create links to subterms
            for subterm in &subterms {
                concept.add_termlink(subterm.clone());
            }
            
            // Reinsert the modified concept
            self.concepts.insert(key, concept);
        }
        
        // Recursively create links for subterms
        for subterm in &subterms {
            self.create_termlinks_for_term(subterm);
        }
    }
    
    /// Convert a term to a byte sequence for use as a key in the radix tree
    fn term_to_key(term: &Term) -> Vec<u8> {
        // Start with the complexity as a 2-byte prefix for sorting by complexity
        let complexity = term.complexity() as u16;
        let mut key = Vec::with_capacity(2 + 32); // Estimate capacity
        
        // Add complexity prefix (big endian)
        key.extend_from_slice(&complexity.to_be_bytes());
        
        // Add term representation as bytes
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
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        writeln!(f, "Memory: {} concepts", self.len())?;
        writeln!(f, "  Capacity: {}", self.capacity)?;
        writeln!(f, "  Forgetting rate: {:.2}", self.forgetting_rate)?;
        writeln!(f, "  Min activation: {:.2}", self.min_activation)?;
        writeln!(f, "  Max termlinks: {}", self.linking.max_termlinks)?;
        writeln!(f, "  Max tasklinks: {}", self.linking.max_tasklinks)
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
    fn test_memory_capacity() {
        let mut memory = Memory::with_capacity(2);
        assert_eq!(memory.capacity(), 2);

        let term1 = Term::Atomic(Atomic::new_atom("cat"));
        let term2 = Term::Atomic(Atomic::new_atom("dog"));
        let term3 = Term::Atomic(Atomic::new_atom("bird"));

        let mut concept1 = memory.get_or_create_concept(&term1);
        concept1.set_activation(0.3);
        memory.add_concept(concept1);

        let mut concept2 = memory.get_or_create_concept(&term2);
        concept2.set_activation(0.2);
        memory.add_concept(concept2);

        let mut concept3 = memory.get_or_create_concept(&term3);
        concept3.set_activation(0.4);
        memory.add_concept(concept3);

        // The concept with the lowest activation (dog) should be removed
        assert_eq!(memory.len(), 2);
        assert!(memory.get_concept(&term1).is_some());
        assert!(memory.get_concept(&term2).is_none());
        assert!(memory.get_concept(&term3).is_some());
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

    #[test]
    fn test_activation_decay() {
        let mut memory = Memory::new();
        let term = Term::Atomic(Atomic::new_atom("cat"));

        // Create concept and set high activation
        let mut concept = memory.get_or_create_concept(&term);
        concept.set_activation(1.0);
        memory.add_concept(concept);

        // Check initial activation
        let concept_ref = memory.get_concept(&term).unwrap();
        assert_eq!(concept_ref.activation(), 1.0);

        // Apply decay
        memory.decay_activation(0.1);

        // Check that activation decreased
        let concept_ref = memory.get_concept(&term).unwrap();
        assert!((concept_ref.activation() - 0.9).abs() < 0.001);
    }

    #[test]
    fn test_forget_concepts() {
        let mut memory = Memory::new();
        memory.set_min_activation(0.5);

        let term1 = Term::Atomic(Atomic::new_atom("cat"));
        let term2 = Term::Atomic(Atomic::new_atom("dog"));

        // Create concepts
        let mut concept1 = memory.get_or_create_concept(&term1);
        concept1.set_activation(0.3); // Below threshold
        memory.add_concept(concept1);

        let mut concept2 = memory.get_or_create_concept(&term2);
        concept2.set_activation(0.7); // Above threshold
        memory.add_concept(concept2);

        assert_eq!(memory.len(), 2);

        // Forget concepts with low activation
        memory.forget_concepts();

        // Check that the correct concept was forgotten
        assert_eq!(memory.len(), 1);
        assert!(memory.get_concept(&term1).is_none());
        assert!(memory.get_concept(&term2).is_some());
    }

    #[test]
    fn test_forget_concepts_empty_memory() {
        let mut memory: Memory = Memory::new();
        memory.forget_concepts();
        assert_eq!(memory.len(), 0);
    }

    #[test]
    fn test_forget_concepts_no_forget() {
        let mut memory = Memory::new();
        memory.set_min_activation(0.1);

        let term1 = Term::Atomic(Atomic::new_atom("cat"));
        let term2 = Term::Atomic(Atomic::new_atom("dog"));

        let mut concept1 = memory.get_or_create_concept(&term1);
        concept1.set_activation(0.2);
        memory.add_concept(concept1);

        let mut concept2 = memory.get_or_create_concept(&term2);
        concept2.set_activation(0.3);
        memory.add_concept(concept2);

        memory.forget_concepts();
        assert_eq!(memory.len(), 2);
    }

    #[test]
    fn test_forget_concepts_all_forget() {
        let mut memory = Memory::new();
        memory.set_min_activation(0.5);

        let term1 = Term::Atomic(Atomic::new_atom("cat"));
        let term2 = Term::Atomic(Atomic::new_atom("dog"));

        let mut concept1 = memory.get_or_create_concept(&term1);
        concept1.set_activation(0.2);
        memory.add_concept(concept1);

        let mut concept2 = memory.get_or_create_concept(&term2);
        concept2.set_activation(0.3);
        memory.add_concept(concept2);

        memory.forget_concepts();
        assert_eq!(memory.len(), 0);
    }
}