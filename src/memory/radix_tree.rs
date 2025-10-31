//! Radix tree implementation for NARS memory
//!
//! This module provides a radix tree data structure for efficient storage
//! and retrieval of concepts in NARS memory.

use std::collections::HashMap;
use std::fmt::Debug;
use std::sync::Arc;
use parking_lot::RwLock;

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
        
        // If we're exceeding capacity, perform garbage collection
        if self.size > self.capacity {
            self.perform_garbage_collection();
        }
        
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
    
    
    /// Perform garbage collection to remove excess concepts
    fn perform_garbage_collection(&mut self) {
        // This is a placeholder implementation
        // In a real implementation, this would remove concepts based on priority/budget
        // For now, we'll just remove random concepts if we exceed capacity by too much
        let overflow = self.size.saturating_sub(self.capacity);
        if overflow > 0 {
            // Remove some concepts (simplified implementation)
            // A real implementation would use a more sophisticated strategy
        }
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
    pub fn update_with<F>(&mut self, key: Vec<u8>, f: F) -> Option<V>
    where
        F: FnOnce(V) -> V,
    {
        if let Some(value) = self.remove(&key) {
            let new_value = f(value);
            self.insert(key, new_value.clone());
            Some(new_value)
        } else {
            None
        }
    }
}

/// Calculate the length of the common prefix between two byte slices
fn common_prefix_length(a: &[u8], b: &[u8]) -> usize {
    a.iter()
        .zip(b.iter())
        .take_while(|(x, y)| x == y)
        .count()
}

#[cfg(test)]
mod tests {
    use super::*;

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
}