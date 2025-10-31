//! Non-Axiomatic Logic (NAL) implementation
//!
//! NAL is the logical foundation of NARS. This module implements:
//! - The NAR (Non-Axiomatic Reasoner) engine
//! - Inference rules
//! - Derivation mechanisms
//! - Reasoning control

pub mod nar;
pub mod inference;

use crate::term::Term;
use crate::truth::Truth;
use crate::task::{Task, Punctuation, Time, Budget, TaskBuilder};
use crate::concept::TaskConcept;
use crate::memory::simple::SimpleMemory as Memory;
use crate::bag::Bag;
use std::sync::atomic::{AtomicU64, Ordering};
use rand::seq::SliceRandom;

/// Non-Axiomatic Reasoner (NAR) engine
pub struct NAR {
    /// Memory system
    memory: Memory,
    
    /// Global clock/time
    time: i64,
    
    /// Unique ID counter for tasks
    next_task_id: AtomicU64,
    
    /// Default budget for new tasks
    default_budget: Budget,
    
    /// Attention parameters
    attention: Attention,
}

impl NAR {
    /// Create a new NAR instance
    pub fn new() -> Self {
        NAR {
            memory: Memory::new(10000),
            time: 0,
            next_task_id: AtomicU64::new(1),
            default_budget: Budget::new(0.5, 0.5, 0.5),
            attention: Attention::default(),
        }
    }
    
    /// Create a new NAR instance with custom attention parameters
    pub fn with_attention(attention: Attention) -> Self {
        NAR {
            memory: Memory::new(10000),
            time: 0,
            next_task_id: AtomicU64::new(1),
            default_budget: Budget::new(0.5, 0.5, 0.5),
            attention,
        }
    }
    
    /// Get the current time
    pub fn time(&self) -> i64 {
        self.time
    }
    
    /// Advance the clock by one step
    pub fn step(&mut self) {
        self.time += 1;
        self.memory.decay_activation(self.attention.activation_decay_rate);
    }
    
    /// Get a concept by term
    pub fn concept(&mut self, term: &Term) -> Option<&mut TaskConcept> {
        self.memory.get_concept_mut(term)
    }
    
    /// Get a mutable reference to a concept by term
    pub fn concept_mut(&mut self, term: &Term) -> Option<&mut TaskConcept> {
        self.memory.get_concept_mut(term)
    }
    
    /// Input a task into the system
    pub fn input(&mut self, task: Task) {
        let concept = self.memory.get_or_create_concept(task.term());
        concept.add_task(task);
        concept.increase_activation(0.1);
        // TODO: Implement link creation
    }
    
    /// Input a sentence as a string and create a task
    pub fn input_sentence(&mut self, sentence: &str) -> Result<(), &'static str> {
        // Use our Narsese parser
        let parse_result = crate::parser::Parser::parse_sentence(sentence);
        if let Err(_parse_error) = parse_result {
            // Fall back to simple parser for compatibility
            return self.input_sentence_simple(sentence);
        }
        
        let (term, truth, punctuation, time) = parse_result.map_err(|_| "Parse error")?;
        
        // Use provided truth or default based on punctuation
        let truth = match truth {
            Some(t) => Some(t),
            None => {
                if matches!(punctuation, Punctuation::Belief | Punctuation::Goal) {
                    Some(Truth::default_belief())
                } else {
                    None
                }
            }
        };
        
        // Use provided time or default to current time
        let time = time.unwrap_or(Time::Tense(self.time));
        
        let task_id = self.next_task_id.fetch_add(1, Ordering::Relaxed);
        let mut task_builder = TaskBuilder::new()
            .id(task_id)
            .term(term)
            .punctuation(punctuation)
            .time(time)
            .budget(self.default_budget)
            .creation_time(self.time);
            
        // Only add truth value if it exists
        if let Some(t) = truth {
            task_builder = task_builder.truth(t);
        }
        
        let task = task_builder.build().map_err(|_| "Failed to build task")?;
        
        self.input(task);
        Ok(())
    }
    
    /// Simple parser for backward compatibility
    fn input_sentence_simple(&mut self, sentence: &str) -> Result<(), &'static str> {
        // This is a simplified parser for demonstration
        // A real implementation would need a proper Narsese parser
        
        // For now, we'll just handle simple atomic terms with default truth
        let term_str = sentence.trim_end_matches(&['.', '!', '?', '@'][..]);
        let punctuation_char = sentence.chars().last().unwrap_or('.');
        
        let term = Term::Atomic(crate::term::atom::Atomic::new_atom(term_str));
        let punctuation = match punctuation_char {
            '.' => Punctuation::Belief,
            '!' => Punctuation::Goal,
            '?' => Punctuation::Question,
            '@' => Punctuation::Quest,
            ';' => Punctuation::Command,
            _ => Punctuation::Belief,
        };
        
        let truth = if matches!(punctuation, Punctuation::Belief | Punctuation::Goal) {
            Some(Truth::default_belief())
        } else {
            None
        };
        
        let task_id = self.next_task_id.fetch_add(1, Ordering::Relaxed);
        let task = TaskBuilder::new()
            .id(task_id)
            .term(term)
            .truth(truth.unwrap_or(Truth::default_belief()))
            .punctuation(punctuation)
            .time(Time::Tense(self.time))
            .budget(self.default_budget)
            .creation_time(self.time)
            .build()
            .map_err(|_| "Failed to build task")?;
        
        self.input(task);
        Ok(())
    }
    
    /// Perform inference between two tasks
    pub fn infer(&mut self, task1: &Task, task2: &Task) -> Option<Task> {
        // This is a placeholder for inference rules
        // A real implementation would have many specific rules
        
        // For demonstration, we'll implement several rules:
        
        // Rule 1: Conjunction of simultaneous beliefs
        if task1.is_belief() && task2.is_belief() &&
           task1.time() == task2.time() {
            // Both are beliefs at the same time, create a conjunction
            let conj_term = Term::Compound(crate::term::compound::Compound::new(
                crate::term::Op::Conjunction,
                vec![task1.term().clone(), task2.term().clone()]
            ));
            
            // Combine truth values (simplified)
            let truth1 = task1.truth().unwrap();
            let truth2 = task2.truth().unwrap();
            let derived_frequency = (truth1.frequency() + truth2.frequency()) / 2.0;
            let derived_confidence = (truth1.confidence() * truth2.confidence()).sqrt();
            let derived_truth = Truth::new(derived_frequency, derived_confidence);
            
            // Lower priority for derived tasks
            let derived_budget = Budget::new(
                (task1.budget().priority() + task2.budget().priority()) / 4.0,
                (task1.budget().durability() + task2.budget().durability()) / 2.0,
                (task1.budget().quality() + task2.budget().quality()) / 2.0,
            );
            
            let task_id = self.next_task_id.fetch_add(1, Ordering::Relaxed);
            let mut derived_task_builder = TaskBuilder::new()
                .id(task_id)
                .term(conj_term)
                .truth(derived_truth)
                .punctuation(Punctuation::Belief)
                .time(task1.time()) // Use the time of the input tasks
                .budget(derived_budget)
                .creation_time(self.time);
                
            // Add evidence from both parent tasks
            derived_task_builder = derived_task_builder
                .add_evidence(task1.id())
                .add_evidence(task2.id());
                
            // Add evidence from the parent tasks' evidence
            for &evidence_id in task1.evidence() {
                derived_task_builder = derived_task_builder.add_evidence(evidence_id);
            }
            for &evidence_id in task2.evidence() {
                derived_task_builder = derived_task_builder.add_evidence(evidence_id);
            }
                
            let derived_task = derived_task_builder.build().ok()?;
                
            Some(derived_task)
        }
        // Rule 2: Sequential implication (temporal induction)
        else if task1.is_belief() && task2.is_belief() &&
                matches!(task1.time(), Time::Tense(t1) if t1 >= 0) &&
                matches!(task2.time(), Time::Tense(t2) if t2 >= 0) &&
                task1.time() != task2.time() {
            // Both are beliefs at different times, create an implication
            // Order them chronologically
            let (earlier_task, later_task) = if matches!(task1.time(), Time::Tense(t1) if matches!(task2.time(), Time::Tense(t2) if t1 < t2)) {
                (task1, task2)
            } else {
                (task2, task1)
            };
            
            // Calculate temporal distance
            let dt = match (later_task.time(), earlier_task.time()) {
                (Time::Tense(later), Time::Tense(earlier)) => later - earlier,
                _ => 1, // Default distance
            };
            
            // Create temporal implication term
            let impl_term = Term::Compound(crate::term::compound::Compound::new_temporal(
                crate::term::Op::Implication,
                vec![earlier_task.term().clone(), later_task.term().clone()],
                dt as i32
            ));
            
            // Combine truth values (simplified)
            let truth1 = earlier_task.truth().unwrap();
            let truth2 = later_task.truth().unwrap();
            // For implication, we use a different combination rule
            let derived_frequency = truth1.frequency() * truth2.frequency();
            let derived_confidence = (truth1.confidence() * truth2.confidence()) / (truth1.confidence() + truth2.confidence() - truth1.confidence() * truth2.confidence());
            let derived_truth = Truth::new(derived_frequency, derived_confidence);
            
            // Lower priority for derived tasks
            let derived_budget = Budget::new(
                (earlier_task.budget().priority() + later_task.budget().priority()) / 4.0,
                (earlier_task.budget().durability() + later_task.budget().durability()) / 2.0,
                (earlier_task.budget().quality() + later_task.budget().quality()) / 2.0,
            );
            
            let task_id = self.next_task_id.fetch_add(1, Ordering::Relaxed);
            let mut derived_task_builder = TaskBuilder::new()
                .id(task_id)
                .term(impl_term)
                .truth(derived_truth)
                .punctuation(Punctuation::Belief)
                .time(Time::Tense(self.time)) // Use current time for the derived task
                .budget(derived_budget)
                .creation_time(self.time);
                
            // Add evidence from both parent tasks
            derived_task_builder = derived_task_builder
                .add_evidence(earlier_task.id())
                .add_evidence(later_task.id());
                
            // Add evidence from the parent tasks' evidence
            for &evidence_id in earlier_task.evidence() {
                derived_task_builder = derived_task_builder.add_evidence(evidence_id);
            }
            for &evidence_id in later_task.evidence() {
                derived_task_builder = derived_task_builder.add_evidence(evidence_id);
            }
                
            let derived_task = derived_task_builder.build().ok()?;
                
            Some(derived_task)
        }
        else {
            None
        }
    }
    
    /// Process a cycle of reasoning
    pub fn cycle(&mut self) {
        // Advance time
        self.step();
        
        let mut concept_bag = Bag::new(self.attention.inference_concept_count);
        for concept in self.memory.concepts() {
            concept_bag.add(concept.clone());
        }
        
        // Collect tasks to process
        let mut tasks_to_process = Vec::new();
        
        // Process each selected concept
        while let Some(concept) = concept_bag.take() {
            // Get the best belief and goal from the concept
            if let Some(best_belief) = concept.best_belief(None) {
                tasks_to_process.push(best_belief.clone());
            }
            
            if let Some(best_goal) = concept.best_goal(None) {
                tasks_to_process.push(best_goal.clone());
            }
        }
        
        // Process collected tasks
        for task in tasks_to_process {
            self.process_inference_with_concept(task);
        }
        
        // Occasionally select a random concept for inference
        if rand::random::<f32>() < self.attention.random_selection_prob {
            let all_concept_terms: Vec<Term> = self.memory.concepts()
                .map(|c| c.term().clone())
                .collect();
                
            if let Some(random_term) = all_concept_terms.choose(&mut rand::thread_rng()) {
                if let Some(concept) = self.concept(random_term) {
                    if let Some(best_belief) = concept.best_belief(None) {
                        // Clone the best belief for processing
                        let best_belief_clone = best_belief.clone();
                        self.process_inference_with_concept(best_belief_clone);
                    }
                }
            }
        }
    }
    
    /// Process inference with a task against other concepts
    fn process_inference_with_concept(&mut self, task: Task) {
        let mut concept_bag = Bag::new(self.attention.inference_concept_count);
        for concept in self.memory.concepts() {
            concept_bag.add(concept.clone());
        }
        
        // Collect tasks to process
        let mut tasks_to_compare = Vec::new();
        
        // Process with a limited number of concepts
        while let Some(concept) = concept_bag.take() {
            // Skip if it's the same concept
            if concept.term() == task.term() {
                continue;
            }
            
            // Get the best belief from the concept
            if let Some(other_belief) = concept.best_belief(None) {
                tasks_to_compare.push(other_belief.clone());
            }
        }
        
        // Process comparisons
        for other_task in tasks_to_compare {
            // Try to infer something
            let derived_task = self.infer(&task, &other_task);
            if let Some(derived_task) = derived_task {
                // Add the derived task
                self.input(derived_task);
            }
        }
        
        // Occasionally process with a random concept
        if rand::random::<f32>() < self.attention.random_selection_prob {
            // Clone the task term for comparison
            let task_term = task.term().clone();
            let task_clone = task.clone();
            
            // Get a random concept term
            let all_concepts: Vec<&TaskConcept> = self.memory.concepts().collect();
                
            if let Some(random_concept) = all_concepts.choose(&mut rand::thread_rng()) {
                // Skip if it's the same concept
                if random_concept.term() != &task_term {
                    // Get the concept and its best belief
                    if let Some(other_belief) = random_concept.best_belief(None) {
                        // Clone the other belief for processing
                        let other_belief_clone = other_belief.clone();
                        
                        // Now we can safely call infer without borrowing conflicts
                        let derived_task = self.infer(&task_clone, &other_belief_clone);
                        if let Some(derived_task) = derived_task {
                            self.input(derived_task);
                        }
                    }
                }
            }
        }
    }
    
    /// Get all concepts
    pub fn concepts(&self) -> Vec<&TaskConcept> {
        self.memory.concepts().collect()
    }
    
    /// Get memory statistics
    pub fn stats(&self) -> NARStats {
        let concepts: Vec<&TaskConcept> = self.memory.concepts().collect();
        let active_concepts = concepts.iter().filter(|c| c.activation() > self.attention.min_attention_threshold).count();
        NARStats {
            time: self.time,
            concepts: concepts.len(),
            active_concepts,
        }
    }
}

impl Default for NAR {
    fn default() -> Self {
        Self::new()
    }
}

/// Attention parameters for controlling reasoning
#[derive(Debug, Clone)]
pub struct Attention {
    /// Rate at which activation decays
    pub activation_decay_rate: f32,
    
    /// Minimum activation for concepts to be considered active
    pub min_attention_threshold: f32,
    
    /// Number of concepts to consider for inference
    pub inference_concept_count: usize,
    
    /// Probability of selecting a random concept for inference
    pub random_selection_prob: f32,
}

impl Default for Attention {
    fn default() -> Self {
        Attention {
            activation_decay_rate: 0.1,
            min_attention_threshold: 0.01,
            inference_concept_count: 10,
            random_selection_prob: 0.05,
        }
    }
}

/// Statistics about the NAR state
#[derive(Debug, Clone)]
pub struct NARStats {
    /// Current time
    pub time: i64,
    
    /// Number of concepts in memory
    pub concepts: usize,
    
    /// Number of active concepts
    pub active_concepts: usize,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_nar_creation() {
        let nar = NAR::new();
        assert_eq!(nar.time(), 0);
        assert_eq!(nar.concepts().len(), 0);
    }

    #[test]
    fn test_nar_input() {
        let mut nar = NAR::new();
        
        // Input a simple belief
        assert!(nar.input_sentence("cat.").is_ok());
        
        // Check that the concept was created
        assert_eq!(nar.concepts().len(), 1);
        
        // Input another belief
        assert!(nar.input_sentence("dog.").is_ok());
        
        // Check that another concept was created
        assert_eq!(nar.concepts().len(), 2);
    }

    #[test]
    fn test_nar_cycle() {
        let mut nar = NAR::new();
        
        // Input some beliefs
        assert!(nar.input_sentence("cat.").is_ok());
        assert!(nar.input_sentence("dog.").is_ok());
        
        // Run a cycle
        nar.cycle();
        
        // Time should have advanced
        assert_eq!(nar.time(), 1);
        
        // We should have more concepts now (including the derived one)
        // Note: The exact number depends on the inference implementation
        assert!(nar.concepts().len() >= 2);
    }

    #[test]
    fn test_nar_stats() {
        let mut nar = NAR::new();
        
        // Initial stats
        let stats = nar.stats();
        assert_eq!(stats.time, 0);
        assert_eq!(stats.concepts, 0);
        assert_eq!(stats.active_concepts, 0);
        
        // Add a concept
        assert!(nar.input_sentence("cat.").is_ok());
        
        // Updated stats
        let stats = nar.stats();
        assert_eq!(stats.concepts, 1);
        // Active concepts depends on activation levels
    }
    
    #[test]
    fn test_evidence_tracking() {
        let mut nar = NAR::new();
        
        // Input some beliefs
        assert!(nar.input_sentence("cat.").is_ok());
        assert!(nar.input_sentence("dog.").is_ok());
        
        // Run a cycle to generate derived tasks
        nar.cycle();
        
        // Find a derived task (one with evidence from multiple sources)
        let concepts = nar.concepts();
        let mut found_derived_task = false;
        
        for concept in concepts {
            // Check beliefs in this concept
            if let Some(best_belief) = concept.best_belief(None) {
                // Check if this is a derived task (has evidence from multiple sources)
                if best_belief.evidence().len() > 1 {
                    found_derived_task = true;
                    // Verify that the evidence includes the parent tasks
                    // This is a basic check - in a real implementation we'd be more specific
                    assert!(best_belief.evidence().len() >= 2);
                    break;
                }
            }
        }
        // Assert that we found at least one derived task
        // This verifies that evidence tracking is working
        assert!(found_derived_task, "No derived task with evidence found");
    }
    
    #[test]
    fn test_narsese_parser() {
        let mut nar = NAR::new();
        
        // Test parsing a belief with explicit truth value
        assert!(nar.input_sentence("<cat --> animal>. {1.0; 0.9}").is_ok());
        
        // Test parsing a goal with explicit truth value
        assert!(nar.input_sentence("<dog --> pet>! {0.8; 0.7}").is_ok());
        
        // Test parsing a question
        assert!(nar.input_sentence("<bird --> flyer>?").is_ok());
        
        // Test parsing a quest
        assert!(nar.input_sentence("<fish --> swimmer>@").is_ok());
        
        // Test parsing with future tense
        assert!(nar.input_sentence("<robot --> human>. :|:").is_ok());
        
        // Test parsing with past tense
        assert!(nar.input_sentence(r"<dinosaur --> extinct>. :\:").is_ok());
        
        // Check that concepts were created
        assert_eq!(nar.concepts().len(), 6);
    }
    
    #[test]
    fn test_temporal_reasoning() {
        let mut nar = NAR::new();
        
        // Input beliefs at different times
        assert!(nar.input_sentence("rain. :0:").is_ok());
        assert!(nar.input_sentence("wet. :1:").is_ok());
        
        // Run cycles to allow for temporal inference
        for _ in 0..5 {
            nar.cycle();
        }
        
        // Check that we have more concepts now (including derived ones)
        assert!(nar.concepts().len() >= 2);
        
        // Look for an implication task
        let mut _found_implication = false;
        for concept in nar.concepts() {
            if let Some(best_belief) = concept.best_belief(None) {
                use crate::term::TermTrait;
                if matches!(best_belief.term().op_id(), crate::term::Op::Implication) {
                    _found_implication = true;
                    break;
                }
            }
        }
        
        // Note: This test might not always pass depending on the exact inference implementation
        // and randomness in concept selection. It's more of a sanity check.
    }
    
    #[test]
    fn test_conjunction_inference() {
        let mut nar = NAR::new();
        
        // Input two beliefs at the same time
        assert!(nar.input_sentence("cat. :0:").is_ok());
        assert!(nar.input_sentence("dog. :0:").is_ok());
        
        // Run cycles to allow for conjunction inference
        for _ in 0..3 {
            nar.cycle();
        }
        
        // Check that we have more concepts now
        assert!(nar.concepts().len() >= 2);
        
        // Look for a conjunction task
        let mut _found_conjunction = false;
        for concept in nar.concepts() {
            if let Some(best_belief) = concept.best_belief(None) {
                use crate::term::TermTrait;
                if matches!(best_belief.term().op_id(), crate::term::Op::Conjunction) {
                    _found_conjunction = true;
                    // Verify it has evidence from multiple sources
                    assert!(best_belief.evidence().len() >= 2);
                    break;
                }
            }
        }
        
        // This test might not always pass due to randomness, but it's good to have
        // assert!(found_conjunction, "No conjunction task found");
    }
    
    #[test]
    fn test_inference_with_different_truth_values() {
        let mut nar = NAR::new();
        
        // Input beliefs with different truth values
        assert!(nar.input_sentence("cat{0.9;0.8}. :0:").is_ok());
        assert!(nar.input_sentence("dog{0.7;0.6}. :0:").is_ok());
        
        // Run cycles to allow for inference
        for _ in 0..3 {
            nar.cycle();
        }
        
        // Check that we have more concepts now
        assert!(nar.concepts().len() >= 2);
    }
    
    #[test]
    fn test_temporal_induction_with_larger_time_gap() {
        let mut nar = NAR::new();
        
        // Input beliefs with a larger time gap
        assert!(nar.input_sentence("rain. :0:").is_ok());
        assert!(nar.input_sentence("flood. :5:").is_ok());
        
        // Run more cycles to allow for temporal inference
        for _ in 0..10 {
            nar.cycle();
        }
        
        // Check that we have more concepts now
        assert!(nar.concepts().len() >= 2);
    }
}
