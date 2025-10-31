//! Radix tree implementation for NARS memory
//!
//! This module provides a radix tree data structure for efficient storage
//! and retrieval of concepts in NARS memory.

use std::collections::HashMap;
use std::fmt::Debug;
use std::sync::Arc;
use parking_lot::RwLock;
use crate::concept::TaskConcept;
use crate::term::{Term, TermTrait};
use std::fmt;

/// A node in the radix tree
#[derive(Debug)]
struct RadixTreeNode<V> {
    /// Children nodes, keyed by byte values
    children: HashMap<u8, Arc<RwLock<RadixTreeNode<V>>>>,
    
    /// Value stored at this node (if any)
    value: Option<V>,
    
    /// Key fragment that leads to this node
    key_fragment: Vec<u8>,
}

impl<V> RadixTreeNode<V> {
    /// Create a new node with the given key fragment
    fn new(key_fragment: Vec<u8>) -> Self {
        RadixTreeNode {
            children: HashMap::new(),
            value: None,
            key_fragment,
        }
    }
    
    /// Get the key fragment for this node
    fn key_fragment(&self) -> &[u8] {
        &self.key_fragment
    }
    
    /// Check if this node has a value
    fn has_value(&self) -> bool {
        self.value.is_some()
    }
    
    /// Get a reference to the value (if any)
    fn value(&self) -> Option<&V> {
        self.value.as_ref()
    }
    
    /// Get a mutable reference to the value (if any)
    fn value_mut(&mut self) -> Option<&mut V> {
        self.value.as_mut()
    }
    
    /// Set the value for this node
    fn set_value(&mut self, value: V) {
        self.value = Some(value);
    }
    
    /// Remove the value from this node
    fn remove_value(&mut self) -> Option<V> {
        self.value.take()
    }
    
    /// Get a child node by key byte
    fn get_child(&self, key_byte: u8) -> Option<Arc<RwLock<RadixTreeNode<V>>>> {
        self.children.get(&key_byte).cloned()
    }
    
    /// Add a child node
    fn add_child(&mut self, key_byte: u8, child: Arc<RwLock<RadixTreeNode<V>>>) {
        self.children.insert(key_byte, child);
    }
    
    /// Remove a child node
    fn remove_child(&mut self, key_byte: u8) -> Option<Arc<RwLock<RadixTreeNode<V>>>> {
        self.children.remove(&key_byte)
    }
    
    /// Get all children
    fn children(&self) -> &HashMap<u8, Arc<RwLock<RadixTreeNode<V>>>> {
        &self.children
    }
}

/// Radix tree implementation
#[derive(Debug)]
pub struct RadixTree<V> {
    /// Root node of the tree
    root: Arc<RwLock<RadixTreeNode<V>>>,
    
    /// Number of values stored in the tree
    size: usize,
    
    /// Maximum size of the tree
    capacity: usize,
}

impl<V> RadixTree<V>
where
    V: Clone + Debug,
{
    /// Create a new radix tree with the specified capacity
    pub fn new(capacity: usize) -> Self {
        RadixTree {
            root: Arc::new(RwLock::new(RadixTreeNode::new(Vec::new()))),
            size: 0,
            capacity,
        }
    }
    
    /// Get the number of values stored in the tree
    pub fn len(&self) -> usize {
        self.size
    }
    
    /// Check if the tree is empty
    pub fn is_empty(&self) -> bool {
        self.size == 0
    }
    
    /// Get the capacity of the tree
    pub fn capacity(&self) -> usize {
        self.capacity
    }
    
    /// Get a value by key
    pub fn get(&self, key: &[u8]) -> Option<V>
    where
        V: Clone,
    {
        let root = self.root.read();
        self.get_recursive(&root, key)
    }
    
    /// Helper function to recursively get a value
    fn get_recursive(&self, node: &RadixTreeNode<V>, key: &[u8]) -> Option<V>
    where
        V: Clone,
    {
        if key.is_empty() {
            return node.value().cloned();
        }
        
        let first_byte = key[0];
        if let Some(child) = node.get_child(first_byte) {
            let child_node = child.read();
            let fragment = child_node.key_fragment();
            
            if key.len() >= fragment.len() && &key[..fragment.len()] == fragment {
                let remaining_key = &key[fragment.len()..];
                self.get_recursive(&child_node, remaining_key)
            } else {
                None
            }
        } else {
            None
        }
    }
    
    /// Insert a value with the given key
    pub fn insert(&mut self, key: Vec<u8>, value: V) -> Option<V> {
        let result = {
            let mut root = self.root.write();
            insert_recursive(&mut root, key, value, &mut self.size)
        };
        
        result
    }
    
    
    
    /// Remove a value by key
    pub fn remove(&mut self, key: &[u8]) -> Option<V> {
        let result = {
            let mut root = self.root.write();
            remove_recursive(&mut root, key, &mut self.size)
        };
        
        result
    }
    
    /// Get all values in the tree
    pub fn values(&self) -> Vec<V> {
        let root = self.root.read();
        let mut result = Vec::new();
        self.collect_values(&root, &mut result);
        result
    }
    
    /// Helper function to collect all values
    fn collect_values(&self, node: &RadixTreeNode<V>, result: &mut Vec<V>) {
        if let Some(value) = node.value() {
            result.push(value.clone());
        }

        for child in node.children().values() {
            let child_node = child.read();
            self.collect_values(&child_node, result);
        }
    }
    
    /// Update a value with a function
    pub fn update_with<F>(&mut self, key: &[u8], f: F) -> Option<V>
    where
        F: FnOnce(&mut V),
    {
        let mut root = self.root.write();
        self.update_recursive(&mut root, key, f)
    }

    /// Helper function to recursively update a value
    fn update_recursive<F>(&self, node: &mut RadixTreeNode<V>, key: &[u8], f: F) -> Option<V>
    where
        F: FnOnce(&mut V),
    {
        if key.is_empty() {
            if let Some(value) = node.value_mut() {
                f(value);
                return Some(value.clone());
            }
            return None;
        }

        let first_byte = key[0];
        if let Some(child) = node.get_child(first_byte) {
            let mut child_node = child.write();
            let fragment = child_node.key_fragment().to_vec();

            if key.len() >= fragment.len() && &key[..fragment.len()] == &fragment[..] {
                let remaining_key = &key[fragment.len()..];
                return self.update_recursive(&mut child_node, remaining_key, f);
            }
        }
        None
    }
}

/// Calculate the length of the common prefix between two byte slices
fn common_prefix_length(a: &[u8], b: &[u8]) -> usize {
    a.iter()
        .zip(b.iter())
        .take_while(|(x, y)| x == y)
        .count()
}

/// Helper function to recursively insert a value
fn insert_recursive<V>(
    node: &mut RadixTreeNode<V>,
    key: Vec<u8>,
    value: V,
    size: &mut usize,
) -> Option<V>
where
    V: Clone + Debug,
{
    if key.is_empty() {
        let old_value = node.remove_value();
        node.set_value(value);

        if old_value.is_none() {
            *size += 1;
        }

        return old_value;
    }

    let first_byte = key[0];
    if let Some(child) = node.get_child(first_byte) {
        let mut child_node = child.write();
        let fragment = child_node.key_fragment().to_vec();

        // Find common prefix
        let common_prefix_len = common_prefix_length(&key, &fragment);

        if common_prefix_len == fragment.len() {
            // Key continues beyond fragment
            let remaining_key = key[common_prefix_len..].to_vec();
            return insert_recursive(&mut child_node, remaining_key, value, size);
        } else {
            // Need to split the node
            split_node(node, first_byte, common_prefix_len, key, value, size);
            return None;
        }
    } else {
        // Create new node
        let new_node = Arc::new(RwLock::new(RadixTreeNode::new(key)));
        new_node.write().set_value(value);
        node.add_child(first_byte, new_node);
        *size += 1;
        None
    }
}

/// Split a node when inserting a key that shares a partial prefix
fn split_node<V>(
    parent: &mut RadixTreeNode<V>,
    key_byte: u8,
    common_prefix_len: usize,
    new_key: Vec<u8>,
    new_value: V,
    size: &mut usize,
) where
    V: Clone + Debug,
{
    if let Some(old_child) = parent.remove_child(key_byte) {
        let old_fragment = old_child.read().key_fragment().to_vec();

        // Create new intermediate node with common prefix
        let common_prefix = old_fragment[..common_prefix_len].to_vec();
        let mut intermediate_node = RadixTreeNode::new(common_prefix.clone());

        // Adjust the old child's fragment
        let old_remaining = old_fragment[common_prefix_len..].to_vec();
        {
            let mut old_child_mut = old_child.write();
            old_child_mut.key_fragment = old_remaining;
        }

        // Add old child to intermediate node
        let old_first_byte = {
            let old_child_read = old_child.read();
            if !old_child_read.key_fragment().is_empty() {
                Some(old_child_read.key_fragment()[0])
            } else {
                None
            }
        };

        if let Some(first_byte) = old_first_byte {
            intermediate_node.add_child(first_byte, old_child);
        }

        // Create new node for the new value
        let new_remaining = new_key[common_prefix_len..].to_vec();
        if !new_remaining.is_empty() {
            let new_node = Arc::new(RwLock::new(RadixTreeNode::new(new_remaining)));
            new_node.write().set_value(new_value);
            let new_first_byte = new_node.read().key_fragment()[0];
            intermediate_node.add_child(new_first_byte, new_node);
            *size += 1;
        } else {
            intermediate_node.set_value(new_value);
            *size += 1;
        }

        // Add intermediate node to parent
        let intermediate_arc = Arc::new(RwLock::new(intermediate_node));
        parent.add_child(common_prefix[0], intermediate_arc);
    }
}

/// Helper function to recursively remove a value
fn remove_recursive<V>(
    node: &mut RadixTreeNode<V>,
    key: &[u8],
    size: &mut usize,
) -> Option<V>
where
    V: Clone + Debug,
{
    if key.is_empty() {
        let old_value = node.remove_value();
        if old_value.is_some() {
            *size -= 1;
        }
        return old_value;
    }

    let first_byte = key[0];
    if let Some(child) = node.get_child(first_byte) {
        let mut child_node = child.write();
        let fragment = child_node.key_fragment().to_vec();

        if key.len() >= fragment.len() && &key[..fragment.len()] == &fragment[..] {
            let remaining_key = &key[fragment.len()..];
            let result = remove_recursive(&mut child_node, remaining_key, size);

            // If child has no value and no children, remove it
            if !child_node.has_value() && child_node.children().is_empty() {
                drop(child_node);
                node.remove_child(first_byte);
            }

            return result;
        } else {
            None
        }
    } else {
        None
    }
}

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
    fn test_radix_tree_basic_operations() {
        let mut tree: RadixTree<String> = RadixTree::new(100);
        
        // Test insert and get
        tree.insert(b"key1".to_vec(), "value1".to_string());
        assert_eq!(tree.len(), 1);
        assert_eq!(tree.get(b"key1"), Some("value1".to_string()));
        assert_eq!(tree.get(b"key2"), None);
        
        // Test update
        tree.insert(b"key1".to_vec(), "value1_updated".to_string());
        assert_eq!(tree.len(), 1);
        assert_eq!(tree.get(b"key1"), Some("value1_updated".to_string()));
        
        // Test remove
        let removed = tree.remove(b"key1");
        assert_eq!(removed, Some("value1_updated".to_string()));
        assert_eq!(tree.len(), 0);
        assert_eq!(tree.get(b"key1"), None);
    }
    
    #[test]
    fn test_radix_tree_prefix_sharing() {
        let mut tree: RadixTree<String> = RadixTree::new(100);
        
        // Insert keys with common prefixes
        tree.insert(b"test".to_vec(), "value1".to_string());
        tree.insert(b"team".to_vec(), "value2".to_string());
        tree.insert(b"te".to_vec(), "value3".to_string());
        
        assert_eq!(tree.len(), 3);
        assert_eq!(tree.get(b"test"), Some("value1".to_string()));
        assert_eq!(tree.get(b"team"), Some("value2".to_string()));
        assert_eq!(tree.get(b"te"), Some("value3".to_string()));
    }

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
