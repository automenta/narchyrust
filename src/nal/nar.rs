//! Non-Axiomatic Reasoner (NAR) implementation
//! 
//! This module implements the core NAR (Non-Axiomatic Reasoner) class based on the Java implementation.
//! The NAR manages the reasoning cycle, memory operations, and I/O channels.

use crate::memory::Memory;
use crate::concept::TaskConcept;
use crate::task::Task;
use crate::term::Term;
use crate::concept::util::ConceptBuilder;
use crate::time::Time;
use crate::truth::Truth;
use std::sync::{Arc, Mutex};
use std::thread;
use std::time::Duration;

/// Non-Axiomatic Reasoner (NAR) - The main reasoning system
pub struct NAR {
    /// The memory system
    pub memory: Memory,
    
    /// The concept builder
    pub concept_builder: ConceptBuilder,  // Changed from Arc to allow direct mutation
    
    /// The time control system
    pub time: Arc<Time>,
    
    /// Self identifier term
    self_term: Term,
    
    /// Running flag
    running: bool,
}

impl NAR {
    /// Create a new NAR instance
    pub fn new(memory: Memory, time: Time, concept_builder: ConceptBuilder) -> Self {
        let time_ref = Arc::new(time);
        let mut nar = NAR {
            memory,
            concept_builder,
            time: time_ref.clone(),
            self_term: Term::Atomic(crate::term::atom::Atomic::new_atom("self")),
            running: false,
        };
        
        // Initialize concept builder with emotion and time
        let emotion = crate::concept::util::Emotion::new();
        nar.concept_builder.init(emotion, nar.time.clone());
        
        nar
    }
    
    /// Input a task into the system
    pub fn input(&mut self, task: Task) {
        // For now, add the task to memory by creating or updating its concept
        let term = task.term().clone();
        if let Some(mut concept) = self.conceptualize(&term) {
            concept.add_task(task);
            // Update the concept in memory
            self.memory.add_concept(concept);
        } else if let Some(concept) = self.concept_builder.build(&term, true, false) {
            // Add the task to the new concept
            let mut concept = concept;
            concept.add_task(task);
            self.memory.add_concept(concept);
        }
    }
    
    /// Input a string as a task
    pub fn input_string(&mut self, input: &str) -> Result<Vec<Task>, String> {
        // This would parse Narsese, simplified for now
        // In a real implementation, this would use the parser module
        Ok(vec![]) // Placeholder
    }
    
    /// Get or create a concept
    pub fn conceptualize(&mut self, term: &Term) -> Option<TaskConcept> {
        if let Some(concept) = self.memory.get_concept(term) {
            // If it already exists, return it
            Some(concept)
        } else {
            // If not, create it using the concept builder
            let concept = self.concept_builder.build(term, true, false);
            if let Some(concept) = concept {
                self.memory.add_concept(concept.clone());
                Some(concept)
            } else {
                None
            }
        }
    }
    
    /// Get a concept if it exists
    pub fn concept(&self, term: &Term) -> Option<TaskConcept> {
        self.memory.get_concept(term)
    }
    
    /// Start the NAR in a loop with given frames per second
    pub fn start_fps(&mut self, fps: f32) {
        self.running = true;
        
        // We can't move self into the thread, so we need a different approach
        // For now, let's just call it sequentially
        // In a more complex implementation, we'd need to restructure
        self.running = false; // Temporarily set to false so this doesn't run
    }
    
    /// Stop the NAR
    pub fn stop(&mut self) {
        self.running = false;
    }
    
    /// Single reasoning cycle
    pub fn cycle(&mut self) {
        // In a real implementation, this would execute the full reasoning cycle
        // For now, we'll just advance time
        self.time.next();
        
        // In the future, this would:
        // 1. Process focus tasks
        // 2. Apply inference rules
        // 3. Update concepts
        // 4. Handle emotions
        // 5. Manage attention
    }
    
    /// Reset the NAR to initial state
    pub fn reset(&mut self) {
        self.stop();
        self.memory.clear();
        self.time.reset();
        // Reset other components as needed
    }
    
    /// Get belief truth for a concept at a time range
    pub fn belief_truth(&self, concept: &Term, _start: i64, _end: i64) -> Option<Truth> {
        if let Some(concept_obj) = self.concept(concept) {
            concept_obj.beliefs().truth(_start, _end, concept)
        } else {
            None
        }
    }
    
    /// Get goal truth for a concept at a time range
    pub fn goal_truth(&self, concept: &Term, _start: i64, _end: i64) -> Option<Truth> {
        if let Some(concept_obj) = self.concept(concept) {
            concept_obj.goals().truth(_start, _end, concept)
        } else {
            None
        }
    }
    
    /// Get answer for a question
    pub fn answer(&self, term: &Term, punc: u8, _start: i64, _end: i64) -> Option<Task> {
        // Get the concept for the term
        if let Some(concept) = self.concept(term) {
            // Based on punctuation, get the appropriate table
            match punc {
                0 => { // BELIEF
                    concept.beliefs().highest_priority().cloned()
                },
                1 => { // GOAL
                    concept.goals().highest_priority().cloned()
                },
                2 => { // QUESTION
                    concept.questions().highest_priority().cloned()
                },
                3 => { // QUEST
                    concept.quests().highest_priority().cloned()
                },
                _ => None
            }
        } else {
            None
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::term::atom::Atomic;
    use crate::truth::Truth;
    use crate::task::{TaskBuilder, Punctuation, Budget};

    #[test]
    fn test_nar_creation() {
        let memory = Memory::new();
        let time = Time::new();
        let concept_builder = ConceptBuilder::new();
        let mut nar = NAR::new(memory, time, concept_builder);
        
        assert_eq!(nar.running, false);
    }

    #[test]
    fn test_nar_input() {
        let memory = Memory::new();
        let time = Time::new();
        let concept_builder = ConceptBuilder::new();
        let mut nar = NAR::new(memory, time, concept_builder);
        
        // Test creating a simple task
        let term = Term::Atomic(Atomic::new_atom("test"));
        let task = TaskBuilder::new()
            .id(1)
            .term(term)
            .truth(Truth::default_belief())
            .punctuation(Punctuation::Belief)
            .time(crate::task::Time::Eternal)
            .budget(Budget::default())
            .build()
            .unwrap();
        nar.input(task);
        
        // Input should work without panicking
    }
    
    #[test]
    fn test_conceptualize() {
        let memory = Memory::new();
        let time = Time::new();
        let concept_builder = ConceptBuilder::new();
        let mut nar = NAR::new(memory, time, concept_builder);
        
        let term = Term::Atomic(Atomic::new_atom("test_concept"));
        let concept = nar.conceptualize(&term);
        
        assert!(concept.is_some());
    }
}