//! Tasks in NARS
//!
//! A task represents a piece of knowledge or a command in the system.
//! It consists of:
//! - A term (what the task is about)
//! - A truth value (the belief strength)
//! - A punctuation mark (belief, goal, question, quest)
//! - A timestamp (when the task is relevant)
//! - A budget (priority and durability)

use crate::term::{Term, TermTrait};
use crate::truth::Truth;
use std::fmt;
use std::hash::{Hash, Hasher};
use std::sync::atomic::{AtomicU64, Ordering};

/// Punctuation marks for tasks
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Punctuation {
    /// Judgment (belief)
    Belief,
    
    /// Goal
    Goal,
    
    /// Question
    Question,
    
    /// Quest (for procedural questions)
    Quest,
    
    /// Command
    Command,
}

impl fmt::Display for Punctuation {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Punctuation::Belief => write!(f, "."),
            Punctuation::Goal => write!(f, "!"),
            Punctuation::Question => write!(f, "?"),
            Punctuation::Quest => write!(f, "@"),
            Punctuation::Command => write!(f, ";"),
        }
    }
}

/// Time specification for tasks
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Time {
    /// Eternal truth (timeless)
    Eternal,
    
    /// Specific time point
    Tense(i64),
}

/// Budget information for tasks
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct Budget {
    /// Priority: current importance [0.0, 1.0]
    priority: f32,
    
    /// Durability: resistance to forgetting [0.0, 1.0]
    durability: f32,
    
    /// Quality: syntactic quality of the term [0.0, 1.0]
    quality: f32,
}

impl Budget {
    /// Create a new budget
    pub fn new(priority: f32, durability: f32, quality: f32) -> Self {
        Budget {
            priority: priority.clamp(0.0, 1.0),
            durability: durability.clamp(0.0, 1.0),
            quality: quality.clamp(0.0, 1.0),
        }
    }
    
    /// Get the priority
    pub fn priority(&self) -> f32 {
        self.priority
    }
    
    /// Get the durability
    pub fn durability(&self) -> f32 {
        self.durability
    }
    
    /// Get the quality
    pub fn quality(&self) -> f32 {
        self.quality
    }
    
    /// Calculate the budget value
    pub fn value(&self) -> f32 {
        self.priority * self.durability * self.quality
    }
}

impl Default for Budget {
    fn default() -> Self {
        Budget::new(0.5, 0.5, 0.5)
    }
}

/// Global counter for generating unique task IDs
static NEXT_TASK_ID: AtomicU64 = AtomicU64::new(1);

/// Task struct representing a NARS task
#[derive(Debug, Clone)]
pub struct Task {
    /// The term of the task
    term: Term,
    
    /// The truth value (None for questions/quests)
    truth: Option<Truth>,
    
    /// The punctuation mark
    punctuation: Punctuation,
    
    /// The time specification
    time: Time,
    
    /// The budget information
    budget: Budget,
    
    /// Unique identifier for the task
    id: u64,
    
    /// Evidence trail (stamp) - list of task IDs that led to this task
    evidence: Vec<u64>,
    
    /// Creation time
    creation_time: i64,
}

impl Task {
    /// Create a new task
    pub fn new(
        term: Term,
        truth: Option<Truth>,
        punctuation: Punctuation,
        time: Time,
        budget: Budget,
        id: u64,
        evidence: Vec<u64>,
        creation_time: i64,
    ) -> Self {
        Task {
            term,
            truth,
            punctuation,
            time,
            budget,
            id,
            evidence,
            creation_time,
        }
    }
    
    /// Create a new task with auto-generated ID
    pub fn with_auto_id(
        term: Term,
        truth: Option<Truth>,
        punctuation: Punctuation,
        time: Time,
        budget: Budget,
        evidence: Vec<u64>,
        creation_time: i64,
    ) -> Self {
        let id = NEXT_TASK_ID.fetch_add(1, Ordering::Relaxed);
        Task::new(term, truth, punctuation, time, budget, id, evidence, creation_time)
    }
    
    /// Get the term
    pub fn term(&self) -> &Term {
        &self.term
    }
    
    /// Get the truth value
    pub fn truth(&self) -> Option<&Truth> {
        self.truth.as_ref()
    }
    
    /// Get the punctuation
    pub fn punctuation(&self) -> Punctuation {
        self.punctuation
    }
    
    /// Get the time
    pub fn time(&self) -> Time {
        self.time
    }
    
    /// Get the budget
    pub fn budget(&self) -> &Budget {
        &self.budget
    }
    
    /// Get the task ID
    pub fn id(&self) -> u64 {
        self.id
    }
    
    /// Get the evidence trail
    pub fn evidence(&self) -> &[u64] {
        &self.evidence
    }
    
    /// Get the creation time
    pub fn creation_time(&self) -> i64 {
        self.creation_time
    }
    
    /// Check if this is an input task (has minimal evidence)
    pub fn is_input(&self) -> bool {
        self.evidence.len() <= 1
    }
    
    /// Add evidence to this task
    pub fn add_evidence(&mut self, evidence_id: u64) {
        if !self.evidence.contains(&evidence_id) {
            self.evidence.push(evidence_id);
        }
    }
    
    /// Create a derived task from this task and another task
    pub fn derive_from(&self, other: &Task, new_term: Term, new_truth: Option<Truth>,
                      new_punctuation: Punctuation, new_time: Time, new_budget: Budget) -> Task {
        let mut evidence = self.evidence.clone();
        // Add evidence from the other task
        for &id in other.evidence.iter() {
            if !evidence.contains(&id) {
                evidence.push(id);
            }
        }
        // Add the tasks themselves as evidence
        if !evidence.contains(&self.id) {
            evidence.push(self.id);
        }
        if !evidence.contains(&other.id) {
            evidence.push(other.id);
        }
        
        Task::with_auto_id(
            new_term,
            new_truth,
            new_punctuation,
            new_time,
            new_budget,
            evidence,
            self.creation_time.max(other.creation_time),
        )
    }
    
    /// Check if this is a belief task
    pub fn is_belief(&self) -> bool {
        matches!(self.punctuation, Punctuation::Belief)
    }
    
    /// Check if this is a goal task
    pub fn is_goal(&self) -> bool {
        matches!(self.punctuation, Punctuation::Goal)
    }
    
    /// Check if this is a question task
    pub fn is_question(&self) -> bool {
        matches!(self.punctuation, Punctuation::Question)
    }
    
    /// Check if this is a quest task
    pub fn is_quest(&self) -> bool {
        matches!(self.punctuation, Punctuation::Quest)
    }
    
    /// Check if this is a command task
    pub fn is_command(&self) -> bool {
        matches!(self.punctuation, Punctuation::Command)
    }
    
    /// Check if this is a judgment (belief or goal)
    pub fn is_judgment(&self) -> bool {
        self.is_belief() || self.is_goal()
    }
    
    /// Check if this is a question-like task (question or quest)
    pub fn is_question_like(&self) -> bool {
        self.is_question() || self.is_quest()
    }
    
    /// Check if this task is eternal
    pub fn is_eternal(&self) -> bool {
        matches!(self.time, Time::Eternal)
    }
    
    /// Get the complexity of the task (based on the term)
    pub fn complexity(&self) -> usize {
        self.term.complexity()
    }
}

impl fmt::Display for Task {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self.punctuation {
            Punctuation::Question | Punctuation::Quest => {
                // Questions don't have truth values
                write!(f, "{}{}", self.term, self.punctuation)
            },
            _ => {
                // Judgments have truth values
                if let Some(truth) = self.truth {
                    write!(f, "{}{}{}", self.term, truth, self.punctuation)
                } else {
                    write!(f, "{}{}", self.term, self.punctuation)
                }
            }
        }
    }
}

impl Hash for Task {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.id.hash(state);
    }
}

impl PartialEq for Task {
    fn eq(&self, other: &Self) -> bool {
        self.id == other.id
    }
}

impl Eq for Task {}

/// Builder for creating tasks
pub struct TaskBuilder {
    term: Option<Term>,
    truth: Option<Truth>,
    punctuation: Option<Punctuation>,
    time: Time,
    budget: Budget,
    id: Option<u64>,
    evidence: Vec<u64>,
    creation_time: i64,
}

impl TaskBuilder {
    /// Create a new task builder
    pub fn new() -> Self {
        TaskBuilder {
            term: None,
            truth: None,
            punctuation: None,
            time: Time::Eternal,
            budget: Budget::default(),
            id: None,
            evidence: Vec::new(),
            creation_time: 0,
        }
    }
    
    /// Set the term
    pub fn term(mut self, term: Term) -> Self {
        self.term = Some(term);
        self
    }
    
    /// Set the truth value
    pub fn truth(mut self, truth: Truth) -> Self {
        self.truth = Some(truth);
        self
    }
    
    /// Set the punctuation
    pub fn punctuation(mut self, punctuation: Punctuation) -> Self {
        self.punctuation = Some(punctuation);
        self
    }
    
    /// Set the time
    pub fn time(mut self, time: Time) -> Self {
        self.time = time;
        self
    }
    
    /// Set the budget
    pub fn budget(mut self, budget: Budget) -> Self {
        self.budget = budget;
        self
    }
    
    /// Set the ID
    pub fn id(mut self, id: u64) -> Self {
        self.id = Some(id);
        self
    }
    
    /// Set the evidence
    pub fn evidence(mut self, evidence: Vec<u64>) -> Self {
        self.evidence = evidence;
        self
    }
    
    /// Set the creation time
    pub fn creation_time(mut self, creation_time: i64) -> Self {
        self.creation_time = creation_time;
        self
    }
    
    /// Add evidence
    pub fn add_evidence(mut self, evidence_id: u64) -> Self {
        if !self.evidence.contains(&evidence_id) {
            self.evidence.push(evidence_id);
        }
        self
    }
    
    /// Build the task
    pub fn build(self) -> Result<Task, &'static str> {
        let term = self.term.ok_or("Term is required")?;
        let punctuation = self.punctuation.ok_or("Punctuation is required")?;
        
        // Validate that questions don't have truth values
        let truth = if matches!(punctuation, Punctuation::Question | Punctuation::Quest) {
            if self.truth.is_some() {
                return Err("Questions cannot have truth values");
            }
            None
        } else {
            self.truth
        };
        
        let id = self.id.unwrap_or_else(|| NEXT_TASK_ID.fetch_add(1, Ordering::Relaxed));
        
        Ok(Task::new(term, truth, punctuation, self.time, self.budget, id, self.evidence, self.creation_time))
    }
}

impl Default for TaskBuilder {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::term::atom::Atomic;

    #[test]
    fn test_punctuation_display() {
        assert_eq!(format!("{}", Punctuation::Belief), ".");
        assert_eq!(format!("{}", Punctuation::Goal), "!");
        assert_eq!(format!("{}", Punctuation::Question), "?");
        assert_eq!(format!("{}", Punctuation::Quest), "@");
        assert_eq!(format!("{}", Punctuation::Command), ";");
    }

    #[test]
    fn test_budget() {
        let budget = Budget::new(0.8, 0.9, 0.7);
        assert_eq!(budget.priority(), 0.8);
        assert_eq!(budget.durability(), 0.9);
        assert_eq!(budget.quality(), 0.7);
        assert!((budget.value() - 0.504).abs() < 0.001);
    }

    #[test]
    fn test_task_creation() {
        let term = Term::Atomic(Atomic::new_atom("cat"));
        let truth = Truth::new(0.9, 0.8);
        let budget = Budget::new(0.7, 0.6, 0.5);
        let evidence = vec![1];
        let creation_time = 100;
        
        let task = Task::new(
            term,
            Some(truth),
            Punctuation::Belief,
            Time::Eternal,
            budget,
            1,
            evidence.clone(),
            creation_time,
        );
        
        assert_eq!(task.id(), 1);
        assert!(task.is_belief());
        assert!(task.is_judgment());
        assert!(!task.is_question_like());
        assert!(task.is_eternal());
        assert_eq!(task.evidence(), &evidence);
        assert_eq!(task.creation_time(), creation_time);
        assert!(task.is_input()); // Has minimal evidence
        
        // Test non-input task
        let term2 = Term::Atomic(Atomic::new_atom("dog"));
        let non_input_task = Task::new(
            term2,
            Some(truth),
            Punctuation::Belief,
            Time::Eternal,
            budget,
            2,
            vec![1, 2, 3],
            creation_time,
        );
        assert!(!non_input_task.is_input()); // Has more than minimal evidence
    }

    #[test]
    fn test_task_builder() {
        let term = Term::Atomic(Atomic::new_atom("cat"));
        let truth = Truth::new(0.9, 0.8);
        
        let task = TaskBuilder::new()
            .id(1)
            .term(term)
            .truth(truth)
            .punctuation(Punctuation::Belief)
            .time(Time::Tense(100))
            .budget(Budget::new(0.7, 0.6, 0.5))
            .creation_time(50)
            .build()
            .expect("Failed to build task");
            
        assert_eq!(task.id(), 1);
        assert!(task.is_belief());
        assert!(!task.is_eternal());
        assert_eq!(task.creation_time(), 50);
    }

    #[test]
    fn test_task_display() {
        let term = Term::Atomic(Atomic::new_atom("cat"));
        let truth = Truth::new(0.9, 0.8);
        
        let belief_task = Task::new(
            term.clone(),
            Some(truth),
            Punctuation::Belief,
            Time::Eternal,
            Budget::default(),
            1,
            vec![],
            0,
        );
        assert_eq!(format!("{}", belief_task), "cat(0.9, 0.8).");
        
        let question_task = Task::new(
            term,
            None,
            Punctuation::Question,
            Time::Eternal,
            Budget::default(),
            2,
            vec![],
            0,
        );
        assert_eq!(format!("{}", question_task), "cat?");
    }
}