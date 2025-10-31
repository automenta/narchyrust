//! Radix tree implementation for NARS memory (V2)
//!
//! This is a new single-threaded implementation of the radix tree.

use std::collections::HashMap;

/// A node in the radix tree
#[derive(Debug)]
struct RadixTreeNode<V> {
    /// Children nodes, keyed by byte values
    children: HashMap<u8, RadixTreeNode<V>>,

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
}

/// Radix tree implementation
#[derive(Debug)]
pub struct RadixTree<V> {
    /// Root node of the tree
    root: RadixTreeNode<V>,

    /// Number of values stored in the tree
    size: usize,

    /// Maximum size of the tree
    capacity: usize,
}

impl<V> RadixTree<V> {
    /// Create a new radix tree with the specified capacity
    pub fn new(capacity: usize) -> Self {
        RadixTree {
            root: RadixTreeNode::new(Vec::new()),
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

    /// Get a value by key
    pub fn get(&self, key: &[u8]) -> Option<&V> {
        let mut node = &self.root;
        let mut key_pos = 0;

        while key_pos < key.len() {
            let remaining_key = &key[key_pos..];
            if remaining_key.is_empty() { break; }

            if let Some(child) = node.children.get(&remaining_key[0]) {
                if remaining_key.starts_with(&child.key_fragment) {
                    key_pos += child.key_fragment.len();
                    node = child;
                } else {
                    return None;
                }
            } else {
                return None;
            }
        }

        node.value.as_ref()
    }

    /// Get a mutable value by key
    pub fn get_mut(&mut self, key: &[u8]) -> Option<&mut V> {
        let mut node = &mut self.root;
        let mut key_pos = 0;

        while key_pos < key.len() {
            let remaining_key = &key[key_pos..];
            if remaining_key.is_empty() { break; }

            if let Some(child) = node.children.get_mut(&remaining_key[0]) {
                if remaining_key.starts_with(&child.key_fragment) {
                    key_pos += child.key_fragment.len();
                    node = child;
                } else {
                    return None;
                }
            } else {
                return None;
            }
        }

        node.value.as_mut()
    }

    /// Check if the tree contains a key
    pub fn contains_key(&self, key: &[u8]) -> bool {
        self.get(key).is_some()
    }

    /// Insert a value with the given key
    pub fn insert(&mut self, key: Vec<u8>, value: V) -> Option<V> {
        let mut node = &mut self.root;
        let mut key_pos = 0;

        loop {
            let remaining_key = &key[key_pos..];

            if remaining_key.is_empty() {
                let old_value = node.value.replace(value);
                if old_value.is_none() {
                    self.size += 1;
                }
                return old_value;
            }

            let first_byte = remaining_key[0];

            let common_prefix_len = if let Some(child_node) = node.children.get(&first_byte) {
                common_prefix_length(remaining_key, &child_node.key_fragment)
            } else {
                0
            };

            if common_prefix_len == 0 {
                // No child with this prefix, create a new one
                let mut new_node = RadixTreeNode::new(remaining_key.to_vec());
                new_node.value = Some(value);
                self.size += 1;
                node.children.insert(first_byte, new_node);
                return None;
            }

            let child_node = node.children.get_mut(&first_byte).unwrap();

            if common_prefix_len == child_node.key_fragment.len() {
                key_pos += common_prefix_len;
                node = child_node;
                continue;
            } else {
                // Split the node
                let mut new_child = RadixTreeNode::new(child_node.key_fragment[common_prefix_len..].to_vec());
                new_child.value = child_node.value.take();
                new_child.children = std::mem::take(&mut child_node.children);

                child_node.key_fragment.truncate(common_prefix_len);
                child_node.value = None;
                child_node.children.insert(new_child.key_fragment[0], new_child);

                let remaining_new_key = &key[key_pos + common_prefix_len..];
                if remaining_new_key.is_empty() {
                    child_node.value = Some(value);
                    self.size += 1;
                } else {
                    let mut new_node = RadixTreeNode::new(remaining_new_key.to_vec());
                    new_node.value = Some(value);
                    self.size += 1;
                    child_node.children.insert(new_node.key_fragment[0], new_node);
                }
                return None;
            }
        }
    }

    /// Remove a value by key
    pub fn remove(&mut self, key: &[u8]) -> Option<V> {
        let mut node = &mut self.root;
        let mut key_pos = 0;

        while key_pos < key.len() {
            let remaining_key = &key[key_pos..];
            if remaining_key.is_empty() { break; }

            if let Some(child) = node.children.get_mut(&remaining_key[0]) {
                if remaining_key.starts_with(&child.key_fragment) {
                    key_pos += child.key_fragment.len();
                    node = child;
                } else {
                    return None;
                }
            } else {
                return None;
            }
        }

        let old_value = node.value.take();
        if old_value.is_some() {
            self.size -= 1;
        }
        old_value
    }

    /// Get all values in the tree
    pub fn values(&self) -> Vec<&V> {
        let mut result = Vec::new();
        Self::collect_values(&self.root, &mut result);
        result
    }

    /// Get all mutable values in the tree
    pub fn values_mut(&mut self) -> Vec<&mut V> {
        let mut result = Vec::new();
        Self::collect_values_mut(&mut self.root, &mut result);
        result
    }

    /// Retain elements based on a predicate
    pub fn retain<F>(&mut self, mut f: F)
    where
        F: FnMut(&[u8], &mut V) -> bool,
    {
        let mut keys_to_remove = Vec::new();

        let mut stack = vec![(&mut self.root, Vec::new())];

        while let Some((node, key)) = stack.pop() {
            if let Some(value) = node.value.as_mut() {
                if !f(&key, value) {
                    keys_to_remove.push(key.clone());
                }
            }

            for child in node.children.values_mut() {
                let mut child_key = key.clone();
                child_key.extend_from_slice(&child.key_fragment);
                stack.push((child, child_key));
            }
        }

        for key in keys_to_remove {
            self.remove(&key);
        }
    }

    /// Helper function to collect all values
    fn collect_values<'a>(node: &'a RadixTreeNode<V>, result: &mut Vec<&'a V>) {
        if let Some(value) = node.value.as_ref() {
            result.push(value);
        }

        for child in node.children.values() {
            Self::collect_values(child, result);
        }
    }

    /// Helper function to collect all mutable values
    fn collect_values_mut<'a>(node: &'a mut RadixTreeNode<V>, result: &mut Vec<&'a mut V>) {
        if let Some(value) = node.value.as_mut() {
            result.push(value);
        }

        for child in node.children.values_mut() {
            Self::collect_values_mut(child, result);
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
