//! Focus bag implementation for NARS
//!
//! This module implements the focus bag which manages the focus of attention
//! in the NARS system.

use crate::task::Task;
use std::collections::VecDeque;

/// Focus bag for managing attention focus
pub struct FocusBag {
    /// Capacity of the focus bag
    capacity: usize,
    
    /// Tasks in the focus bag
    tasks: VecDeque<Task>,
    
    /// Priority threshold
    priority_threshold: f32,
}

impl FocusBag {
    /// Create a new focus bag with specified capacity
    pub fn new(capacity: usize) -> Self {
        FocusBag {
            capacity,
            tasks: VecDeque::new(),
            priority_threshold: 0.1,
        }
    }
    
    /// Accept a task into the focus bag
    pub fn accept(&mut self, task: Task) {
        if self.tasks.len() < self.capacity {
            self.tasks.push_back(task);
        } else {
            // Simple replacement policy: replace the lowest priority task
            let mut min_idx = 0;
            let mut min_priority = f32::MAX;
            
            for (i, t) in self.tasks.iter().enumerate() {
                if t.budget().priority() < min_priority {
                    min_priority = t.budget().priority();
                    min_idx = i;
                }
            }
            
            // Replace if the new task has higher priority
            if task.budget().priority() > min_priority {
                self.tasks.remove(min_idx);
                self.tasks.push_back(task);
            }
        }
    }
    
    /// Commit focus changes
    pub fn commit(&mut self) {
        // Apply decay to tasks in focus - but task doesn't have decay_activation method
        // For now, just keep the tasks as they are
    }
    
    /// Clear the focus bag
    pub fn clear(&mut self) {
        self.tasks.clear();
    }
    
    /// Get tasks in the focus bag
    pub fn tasks(&self) -> &VecDeque<Task> {
        &self.tasks
    }
    
    /// Get mutable tasks in the focus bag
    pub fn tasks_mut(&mut self) -> &mut VecDeque<Task> {
        &mut self.tasks
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::task::{Task, TaskBuilder, Budget, Punctuation};
    use crate::term::Term;
    use crate::truth::Truth;

    #[test]
    fn test_focus_bag_creation() {
        let focus_bag = FocusBag::new(10);
        assert_eq!(focus_bag.tasks().len(), 0);
        assert_eq!(focus_bag.capacity, 10);
    }

    #[test]
    fn test_focus_bag_accept() {
        let mut focus_bag = FocusBag::new(2);
        
        // Create a simple task
        let term = Term::Atomic(crate::term::atom::Atomic::new_atom("test"));
        let truth = Truth::new(0.8, 0.9);
        let task = TaskBuilder::new()
            .id(1)
            .term(term)
            .truth(truth)
            .punctuation(Punctuation::Belief)
            .time(crate::task::Time::Eternal)
            .budget(Budget::new(0.5, 0.5, 0.5))
            .build()
            .unwrap();
        
        focus_bag.accept(task);
        assert_eq!(focus_bag.tasks().len(), 1);
    }

    #[test]
    fn test_focus_bag_capacity_limit() {
        let mut focus_bag = FocusBag::new(2);
        
        // Create tasks with different priorities
        let term1 = Term::Atomic(crate::term::atom::Atomic::new_atom("test1"));
        let task1 = TaskBuilder::new()
            .id(1)
            .term(term1)
            .truth(Truth::new(0.8, 0.9))
            .punctuation(Punctuation::Belief)
            .time(crate::task::Time::Eternal)
            .budget(Budget::new(0.3, 0.5, 0.5)) // Lower priority
            .build()
            .unwrap();
        
        let term2 = Term::Atomic(crate::term::atom::Atomic::new_atom("test2"));
        let task2 = TaskBuilder::new()
            .id(2)
            .term(term2)
            .truth(Truth::new(0.9, 0.8))
            .punctuation(Punctuation::Belief)
            .time(crate::task::Time::Eternal)
            .budget(Budget::new(0.7, 0.5, 0.5)) // Higher priority
            .build()
            .unwrap();
            
        let term3 = Term::Atomic(crate::term::atom::Atomic::new_atom("test3"));
        let task3 = TaskBuilder::new()
            .id(3)
            .term(term3)
            .truth(Truth::new(0.7, 0.9))
            .punctuation(Punctuation::Belief)
            .time(crate::task::Time::Eternal)
            .budget(Budget::new(0.9, 0.5, 0.5)) // Highest priority
            .build()
            .unwrap();
        
        focus_bag.accept(task1);
        focus_bag.accept(task2);
        focus_bag.accept(task3); // Should replace the lowest priority task
        
        // Should have 2 tasks in the bag
        assert_eq!(focus_bag.tasks().len(), 2);
        
        // Verify that the highest priority tasks are kept
        let tasks: Vec<_> = focus_bag.tasks().iter().collect();
        let priorities: Vec<f32> = tasks.iter().map(|t| t.budget().priority()).collect();
        assert!(priorities.contains(&0.9)); // task3 should be kept
        assert!(priorities.contains(&0.7)); // task2 should be kept
        assert!(!priorities.contains(&0.3)); // task1 should be replaced
    }
    
    #[test]
    fn test_focus_bag_commit() {
        let mut focus_bag = FocusBag::new(10);
        
        // Add a task
        let term = Term::Atomic(crate::term::atom::Atomic::new_atom("test"));
        let task = TaskBuilder::new()
            .id(1)
            .term(term)
            .truth(Truth::new(0.8, 0.9))
            .punctuation(Punctuation::Belief)
            .time(crate::task::Time::Eternal)
            .budget(Budget::new(0.5, 0.5, 0.5))
            .build()
            .unwrap();
            
        focus_bag.accept(task);
        
        // Check that commit doesn't panic
        focus_bag.commit();
    }
    
    #[test]
    fn test_focus_bag_clear() {
        let mut focus_bag = FocusBag::new(10);
        
        // Add a task
        let term = Term::Atomic(crate::term::atom::Atomic::new_atom("test"));
        let task = TaskBuilder::new()
            .id(1)
            .term(term)
            .truth(Truth::new(0.8, 0.9))
            .punctuation(Punctuation::Belief)
            .time(crate::task::Time::Eternal)
            .budget(Budget::new(0.5, 0.5, 0.5))
            .build()
            .unwrap();
            
        focus_bag.accept(task);
        assert_eq!(focus_bag.tasks().len(), 1);
        
        focus_bag.clear();
        assert_eq!(focus_bag.tasks().len(), 0);
    }
}