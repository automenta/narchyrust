//! Task and belief tables for NARS
//!
//! This module provides implementations for various types of tables
//! that store beliefs, goals, questions, and other task types in NARS.

use crate::task::Task;
use crate::term::Term;
use crate::truth::Truth;
use std::collections::HashMap;

/// A table for storing belief tasks
#[derive(Clone, Debug)]
pub struct BeliefTable {
    /// Map of tasks indexed by some criteria
    tasks: HashMap<u64, Task>,
    
    /// Maximum capacity for the table
    capacity: usize,
}

impl BeliefTable {
    /// Create a new empty belief table
    pub fn new() -> Self {
        BeliefTable {
            tasks: HashMap::new(),
            capacity: 100, // Default capacity
        }
    }
    
    /// Create a belief table with specified capacity
    pub fn with_capacity(capacity: usize) -> Self {
        BeliefTable {
            tasks: HashMap::new(),
            capacity,
        }
    }
    
    /// Add a task to the table
    pub fn add(&mut self, task: Task) {
        if self.tasks.len() < self.capacity {
            self.tasks.insert(task.id(), task);
        } else {
            // If at capacity, replace the lowest priority task
            let lowest_priority_id = self.tasks
                .iter()
                .min_by(|(_, a), (_, b)| {
                    a.budget().priority().partial_cmp(&b.budget().priority()).unwrap()
                })
                .map(|(id, _)| *id);
                
            if let Some(lowest_id) = lowest_priority_id {
                if task.budget().priority() > self.tasks.get(&lowest_id).unwrap().budget().priority() {
                    self.tasks.remove(&lowest_id);
                    self.tasks.insert(task.id(), task);
                }
            }
        }
    }
    
    /// Get a task by ID
    pub fn get(&self, task_id: u64) -> Option<&Task> {
        self.tasks.get(&task_id)
    }
    
    /// Get the number of tasks in the table
    pub fn len(&self) -> usize {
        self.tasks.len()
    }
    
    /// Check if the table is empty
    pub fn is_empty(&self) -> bool {
        self.tasks.is_empty()
    }
    
    /// Get all tasks in the table
    pub fn tasks(&self) -> Vec<&Task> {
        self.tasks.values().collect()
    }
    
    /// Get the task with the highest priority
    pub fn highest_priority(&self) -> Option<&Task> {
        self.tasks.values()
            .max_by(|a, b| a.budget().priority().partial_cmp(&b.budget().priority()).unwrap())
    }
    
    /// Get tasks with priority above a threshold
    pub fn tasks_above_priority(&self, threshold: f32) -> Vec<&Task> {
        self.tasks
            .values()
            .filter(|task| task.budget().priority() > threshold)
            .collect()
    }
    
    /// Get the truth value for a concept at a given time range
    pub fn truth(&self, _start: i64, _end: i64, _term: &Term) -> Option<Truth> {
        // In a real implementation, this would calculate the truth value based on
        // the tasks in the table for the given time range
        // For now, we'll return the truth of the highest priority task
        self.highest_priority()
            .and_then(|task| task.truth().cloned())
    }
    
    /// Clear the table
    pub fn clear(&mut self) {
        self.tasks.clear();
    }
}

impl Default for BeliefTable {
    fn default() -> Self {
        Self::new()
    }
}

/// A generic task table for storing various task types
#[derive(Clone, Debug)]
pub struct TaskTable {
    /// Map of tasks indexed by ID
    tasks: HashMap<u64, Task>,
    
    /// Maximum capacity for the table
    capacity: usize,
}

impl TaskTable {
    /// Create a new empty task table
    pub fn new() -> Self {
        TaskTable {
            tasks: HashMap::new(),
            capacity: 100, // Default capacity
        }
    }
    
    /// Create a task table with specified capacity
    pub fn with_capacity(capacity: usize) -> Self {
        TaskTable {
            tasks: HashMap::new(),
            capacity,
        }
    }
    
    /// Add a task to the table
    pub fn add(&mut self, task: Task) {
        if self.tasks.len() < self.capacity {
            self.tasks.insert(task.id(), task);
        } else {
            // If at capacity, replace the lowest priority task
            let lowest_priority_id = self.tasks
                .iter()
                .min_by(|(_, a), (_, b)| {
                    a.budget().priority().partial_cmp(&b.budget().priority()).unwrap()
                })
                .map(|(id, _)| *id);
                
            if let Some(lowest_id) = lowest_priority_id {
                if task.budget().priority() > self.tasks.get(&lowest_id).unwrap().budget().priority() {
                    self.tasks.remove(&lowest_id);
                    self.tasks.insert(task.id(), task);
                }
            }
        }
    }
    
    /// Get a task by ID
    pub fn get(&self, task_id: u64) -> Option<&Task> {
        self.tasks.get(&task_id)
    }
    
    /// Get the number of tasks in the table
    pub fn len(&self) -> usize {
        self.tasks.len()
    }
    
    /// Check if the table is empty
    pub fn is_empty(&self) -> bool {
        self.tasks.is_empty()
    }
    
    /// Get all tasks in the table
    pub fn tasks(&self) -> Vec<&Task> {
        self.tasks.values().collect()
    }
    
    /// Get the task with the highest priority
    pub fn highest_priority(&self) -> Option<&Task> {
        self.tasks.values()
            .max_by(|a, b| a.budget().priority().partial_cmp(&b.budget().priority()).unwrap())
    }
    
    /// Get tasks with priority above a threshold
    pub fn tasks_above_priority(&self, threshold: f32) -> Vec<&Task> {
        self.tasks
            .values()
            .filter(|task| task.budget().priority() > threshold)
            .collect()
    }
    
    /// Clear the table
    pub fn clear(&mut self) {
        self.tasks.clear();
    }
}

impl Default for TaskTable {
    fn default() -> Self {
        Self::new()
    }
}

// Re-export for compatibility
pub use TaskTable as GenericTaskTable;

#[cfg(test)]
mod tests {
    use super::*;
    use crate::term::Term;
    use crate::truth::Truth;
    use crate::task::{TaskBuilder, Punctuation, Budget};

    #[test]
    fn test_belief_table_creation() {
        let table = BeliefTable::new();
        assert_eq!(table.len(), 0);
        assert!(table.is_empty());
        assert_eq!(table.capacity, 100);
    }

    #[test]
    fn test_belief_table_add() {
        let mut table = BeliefTable::new();
        
        let task = TaskBuilder::new()
            .id(1)
            .term(Term::Atomic(crate::term::atom::Atomic::new_atom("test")))
            .truth(Truth::new(0.8, 0.9))
            .punctuation(Punctuation::Belief)
            .budget(Budget::new(0.6, 0.5, 0.4))
            .build()
            .unwrap();
        
        table.add(task);
        assert_eq!(table.len(), 1);
        assert!(!table.is_empty());
    }

    #[test]
    fn test_belief_table_capacity() {
        let mut table = BeliefTable::with_capacity(2);
        
        let task1 = TaskBuilder::new()
            .id(1)
            .term(Term::Atomic(crate::term::atom::Atomic::new_atom("low_priority")))
            .truth(Truth::new(0.8, 0.9))
            .punctuation(Punctuation::Belief)
            .budget(Budget::new(0.3, 0.5, 0.4)) // Low priority
            .build()
            .unwrap();
        
        let task2 = TaskBuilder::new()
            .id(2)
            .term(Term::Atomic(crate::term::atom::Atomic::new_atom("medium_priority")))
            .truth(Truth::new(0.7, 0.8))
            .punctuation(Punctuation::Belief)
            .budget(Budget::new(0.6, 0.5, 0.4)) // Medium priority
            .build()
            .unwrap();
        
        let task3 = TaskBuilder::new()
            .id(3)
            .term(Term::Atomic(crate::term::atom::Atomic::new_atom("high_priority")))
            .truth(Truth::new(0.9, 0.7))
            .punctuation(Punctuation::Belief)
            .budget(Budget::new(0.9, 0.5, 0.4)) // High priority
            .build()
            .unwrap();
        
        table.add(task1);
        table.add(task2);
        table.add(task3);
        
        // Table should have 2 tasks (capacity is 2)
        assert_eq!(table.len(), 2);
        
        // Should have the two highest priority tasks
        let highest = table.highest_priority().unwrap();
        assert_eq!(highest.id(), 3); // task3 has the highest priority
    }

    #[test]
    fn test_belief_table_get() {
        let mut table = BeliefTable::new();
        
        let task = TaskBuilder::new()
            .id(1)
            .term(Term::Atomic(crate::term::atom::Atomic::new_atom("test")))
            .truth(Truth::new(0.8, 0.9))
            .punctuation(Punctuation::Belief)
            .budget(Budget::new(0.6, 0.5, 0.4))
            .build()
            .unwrap();
        
        table.add(task);
        
        let retrieved = table.get(1);
        assert!(retrieved.is_some());
        assert_eq!(retrieved.unwrap().id(), 1);
        
        let non_existent = table.get(2);
        assert!(non_existent.is_none());
    }

    #[test]
    fn test_task_table_creation() {
        let table = TaskTable::new();
        assert_eq!(table.len(), 0);
        assert!(table.is_empty());
        assert_eq!(table.capacity, 100);
    }

    #[test]
    fn test_task_table_add() {
        let mut table = TaskTable::new();
        
        let task = TaskBuilder::new()
            .id(1)
            .term(Term::Atomic(crate::term::atom::Atomic::new_atom("test")))
            .truth(Truth::new(0.8, 0.9))
            .punctuation(Punctuation::Goal)
            .budget(Budget::new(0.6, 0.5, 0.4))
            .build()
            .unwrap();
        
        table.add(task);
        assert_eq!(table.len(), 1);
        assert!(!table.is_empty());
    }
}