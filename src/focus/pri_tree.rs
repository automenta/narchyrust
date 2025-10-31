//! Priority tree implementation for NARS
//!
//! This module implements a priority tree for managing priorities in NARS.

use std::collections::HashMap;

/// Priority tree for managing priorities
pub struct PriTree {
    /// Internal map of priorities
    priorities: HashMap<String, f32>,
    
    /// Default priority
    default_priority: f32,
}

impl PriTree {
    /// Create a new priority tree
    pub fn new() -> Self {
        PriTree {
            priorities: HashMap::new(),
            default_priority: 0.5,
        }
    }
    
    /// Set a priority for a key
    pub fn set_priority(&mut self, key: &str, priority: f32) {
        self.priorities.insert(key.to_string(), priority);
    }
    
    /// Get the priority for a key
    pub fn get_priority(&self, key: &str) -> f32 {
        *self.priorities.get(key).unwrap_or(&self.default_priority)
    }
    
    /// Commit priority changes
    pub fn commit(&mut self) {
        // Apply any priority changes or updates
        // In a real implementation, this would update the tree structure
    }
    
    /// Get all priorities
    pub fn priorities(&self) -> &HashMap<String, f32> {
        &self.priorities
    }
    
    /// Clear all priorities
    pub fn clear(&mut self) {
        self.priorities.clear();
    }
}

impl Default for PriTree {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_pri_tree_creation() {
        let pri_tree = PriTree::new();
        assert_eq!(pri_tree.priorities().len(), 0);
        assert_eq!(pri_tree.get_priority("test"), 0.5);
    }

    #[test]
    fn test_set_and_get_priority() {
        let mut pri_tree = PriTree::new();
        
        pri_tree.set_priority("test", 0.8);
        assert_eq!(pri_tree.get_priority("test"), 0.8);
        
        // Non-existent key should return default
        assert_eq!(pri_tree.get_priority("nonexistent"), 0.5);
    }

    #[test]
    fn test_commit() {
        let mut pri_tree = PriTree::new();
        pri_tree.set_priority("test", 0.8);
        
        // Commit should not panic
        pri_tree.commit();
        
        assert_eq!(pri_tree.get_priority("test"), 0.8);
    }

    #[test]
    fn test_clear() {
        let mut pri_tree = PriTree::new();
        pri_tree.set_priority("test", 0.8);
        assert_eq!(pri_tree.priorities().len(), 1);
        
        pri_tree.clear();
        assert_eq!(pri_tree.priorities().len(), 0);
        assert_eq!(pri_tree.get_priority("test"), 0.5); // Default value after clear
    }
}