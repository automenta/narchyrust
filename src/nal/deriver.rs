//! Deriver implementation for NARS
//!
//! This module implements the deriver, which is responsible for driving the inference process.

use crate::task::Task;
use crate::nal::inference;

/// A premise for inference
#[derive(Debug, Clone)]
pub struct Premise {
    pub task: Task,
}

/// The deriver
pub struct Deriver {
    premise_queue: Vec<Premise>,
}

impl Deriver {
    /// Create a new deriver
    pub fn new() -> Self {
        Deriver {
            premise_queue: Vec::new(),
        }
    }

    /// Add a premise to the queue
    pub fn add_premise(&mut self, premise: Premise) {
        self.premise_queue.push(premise);
    }

    /// Perform a single derivation step and return any derived tasks
    pub fn step(&mut self) -> Vec<Task> {
        let mut derived_tasks = Vec::new();
        if self.premise_queue.len() >= 2 {
            let p1 = self.premise_queue.pop().unwrap();
            let p2 = self.premise_queue.pop().unwrap();
            if let Some(derived_task) = inference::inference(&p1.task, &p2.task) {
                derived_tasks.push(derived_task);
            }
        }
        derived_tasks
    }
}