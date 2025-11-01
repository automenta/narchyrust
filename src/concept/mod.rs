//! Concepts in NARS
//!
//! A concept is a group of tasks that share the same term (after normalization).
//! It contains:
//! - A term (the concept's identifier)
//! - Tables of beliefs, goals, questions, and quests
//! - Links to related concepts (termlinks and tasklinks)

pub mod util;

use crate::term::Term;
use crate::task::{Task, Punctuation};
use crate::table::TaskTable;
use crate::bag::BagItem;
use std::fmt;

pub use util::{Emotion, ConceptBuilder};

/// Concept struct representing a NARS concept
#[derive(Debug)]
pub struct Concept {
    /// The term that identifies this concept
    term: Term,
    
    /// Belief table (stores beliefs about this concept)
    beliefs: TaskTable,
    
    /// Goal table (stores goals about this concept)
    goals: TaskTable,
    
    /// Question table (stores questions about this concept)
    questions: TaskTable,
    
    /// Quest table (stores quests about this concept)
    quests: TaskTable,
    
    /// Termlinks (links to related concepts based on term structure)
    termlinks: Vec<Term>,
    
    /// Tasklinks (links to related tasks)
    tasklinks: Vec<u64>, // Task IDs
    
    /// Activation level (for attention dynamics)
    activation: f32,
}

impl Concept {
    /// Create a new concept
    pub fn new(term: Term) -> Self {
        Concept {
            term,
            beliefs: TaskTable::new(),
            goals: TaskTable::new(),
            questions: TaskTable::new(),
            quests: TaskTable::new(),
            termlinks: Vec::new(),
            tasklinks: Vec::new(),
            activation: 0.0,
        }
    }
    
    /// Get the term of this concept
    pub fn term(&self) -> &Term {
        &self.term
    }
    
    /// Get the belief table
    pub fn beliefs(&self) -> &TaskTable {
        &self.beliefs
    }
    
    /// Get the goal table
    pub fn goals(&self) -> &TaskTable {
        &self.goals
    }
    
    /// Get the question table
    pub fn questions(&self) -> &TaskTable {
        &self.questions
    }
    
    /// Get the quest table
    pub fn quests(&self) -> &TaskTable {
        &self.quests
    }
    
    /// Get mutable reference to the belief table
    pub fn beliefs_mut(&mut self) -> &mut TaskTable {
        &mut self.beliefs
    }
    
    /// Get mutable reference to the goal table
    pub fn goals_mut(&mut self) -> &mut TaskTable {
        &mut self.goals
    }
    
    /// Get mutable reference to the question table
    pub fn questions_mut(&mut self) -> &mut TaskTable {
        &mut self.questions
    }
    
    /// Get mutable reference to the quest table
    pub fn quests_mut(&mut self) -> &mut TaskTable {
        &mut self.quests
    }
    
    /// Add a task to the appropriate table
    pub fn add_task(&mut self, task: Task) {
        match task.punctuation() {
            Punctuation::Belief => self.beliefs.add(task),
            Punctuation::Goal => self.goals.add(task),
            Punctuation::Question => self.questions.add(task),
            Punctuation::Quest => self.quests.add(task),
            Punctuation::Command => {
                // Commands might be handled differently, for now we ignore them
            }
        }
    }
    
    /// Get the best belief for a given time
    pub fn best_belief(&self, _time: Option<i64>) -> Option<&Task> {
        // Return the highest priority belief
        // A more sophisticated implementation would consider temporal aspects and other factors
        self.beliefs.highest_priority()
    }
    
    /// Get the best goal for a given time
    pub fn best_goal(&self, _time: Option<i64>) -> Option<&Task> {
        // Return the highest priority goal
        // A more sophisticated implementation would consider temporal aspects and other factors
        self.goals.highest_priority()
    }
    
    /// Get all tasks in this concept
    pub fn tasks(&self) -> Vec<&Task> {
        let mut tasks = Vec::new();
        tasks.extend(self.beliefs.tasks());
        tasks.extend(self.goals.tasks());
        tasks.extend(self.questions.tasks());
        tasks.extend(self.quests.tasks());
        tasks
    }
    
    /// Get the activation level
    pub fn activation(&self) -> f32 {
        self.activation
    }
    
    /// Set the activation level
    pub fn set_activation(&mut self, activation: f32) {
        self.activation = activation.clamp(0.0, 1.0);
    }
    
    /// Increase activation
    pub fn increase_activation(&mut self, amount: f32) {
        self.activation = (self.activation + amount).min(1.0);
    }
    
    /// Decrease activation (decay)
    pub fn decay_activation(&mut self, rate: f32) {
        self.activation = (self.activation * (1.0 - rate)).max(0.0);
    }
    
    /// Add a termlink
    pub fn add_termlink(&mut self, term: Term) {
        // Avoid duplicates
        if !self.termlinks.contains(&term) {
            self.termlinks.push(term);
        }
    }
    
    /// Add a tasklink
    pub fn add_tasklink(&mut self, task_id: u64) {
        // Avoid duplicates
        if !self.tasklinks.contains(&task_id) {
            self.tasklinks.push(task_id);
        }
    }
    
    /// Get termlinks
    pub fn termlinks(&self) -> &[Term] {
        &self.termlinks
    }
    
    /// Get tasklinks
    pub fn tasklinks(&self) -> &[u64] {
        &self.tasklinks
    }
}

impl fmt::Display for Concept {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        writeln!(f, "Concept: {}", self.term)?;
        writeln!(f, "  Activation: {:.2}", self.activation)?;
        writeln!(f, "  Beliefs: {}", self.beliefs.len())?;
        writeln!(f, "  Goals: {}", self.goals.len())?;
        writeln!(f, "  Questions: {}", self.questions.len())?;
        writeln!(f, "  Quests: {}", self.quests.len())?;
        writeln!(f, "  Termlinks: {}", self.termlinks.len())?;
        writeln!(f, "  Tasklinks: {}", self.tasklinks.len())
    }
}

impl BagItem for TaskConcept {
    fn priority(&self) -> f32 {
        self.activation
    }
}

/// TaskConcept - A concept that supports tasks
#[derive(Clone, Debug)]
pub struct TaskConcept {
    /// The term of the concept
    term: Term,
    
    /// Belief table
    beliefs: crate::table::BeliefTable,
    
    /// Goal table
    goals: crate::table::BeliefTable,
    
    /// Question table
    questions: crate::table::TaskTable,
    
    /// Quest table
    quests: crate::table::TaskTable,
    
    /// Termlinks (links to related concepts based on term structure)
    termlinks: Vec<Term>,
    
    /// Tasklinks (links to related tasks)
    tasklinks: Vec<u64>, // Task IDs
    
    /// Activation level (for attention dynamics)
    activation: f32,
}

impl TaskConcept {
    /// Create a new TaskConcept
    pub fn new(term: Term) -> Self {
        TaskConcept {
            term,
            beliefs: crate::table::BeliefTable::new(),
            goals: crate::table::BeliefTable::new(),
            questions: crate::table::TaskTable::new(),
            quests: crate::table::TaskTable::new(),
            termlinks: Vec::new(),
            tasklinks: Vec::new(),
            activation: 0.0,
        }
    }
    
    /// Get the underlying term
    pub fn term(&self) -> &Term {
        &self.term
    }
    
    /// Get the belief table
    pub fn beliefs(&self) -> &crate::table::BeliefTable {
        &self.beliefs
    }
    
    /// Get the goal table
    pub fn goals(&self) -> &crate::table::BeliefTable {
        &self.goals
    }
    
    /// Get the question table
    pub fn questions(&self) -> &crate::table::TaskTable {
        &self.questions
    }
    
    /// Get the quest table
    pub fn quests(&self) -> &crate::table::TaskTable {
        &self.quests
    }
    
    /// Get a table for a specific punctuation type
    pub fn table(&self, punctuation: Punctuation) -> &dyn std::any::Any {
        match punctuation {
            Punctuation::Belief => &self.beliefs,
            Punctuation::Goal => &self.goals,
            Punctuation::Question => &self.questions,
            Punctuation::Quest => &self.quests,
            Punctuation::Command => &self.beliefs, // Commands might be handled differently
        }
    }
    
    /// Add a task to the concept
    pub fn add_task(&mut self, task: Task) {
        match task.punctuation() {
            Punctuation::Belief => self.beliefs.add(task),
            Punctuation::Goal => self.goals.add(task),
            Punctuation::Question => self.questions.add(task),
            Punctuation::Quest => self.quests.add(task),
            Punctuation::Command => {
                // Commands might be handled differently, for now we ignore them
            }
        }
    }
    
    /// Get the activation level
    pub fn activation(&self) -> f32 {
        self.activation
    }
    
    /// Set the activation level
    pub fn set_activation(&mut self, activation: f32) {
        self.activation = activation.clamp(0.0, 1.0);
    }
    
    /// Increase activation
    pub fn increase_activation(&mut self, amount: f32) {
        self.activation = (self.activation + amount).min(1.0);
    }
    
    /// Decrease activation (decay)
    pub fn decay_activation(&mut self, rate: f32) {
        self.activation = (self.activation * (1.0 - rate)).max(0.0);
    }
    
    /// Add a termlink
    pub fn add_termlink(&mut self, term: Term) {
        // Avoid duplicates
        if !self.termlinks.contains(&term) {
            self.termlinks.push(term);
        }
    }
    
    /// Add a tasklink
    pub fn add_tasklink(&mut self, task_id: u64) {
        // Avoid duplicates
        if !self.tasklinks.contains(&task_id) {
            self.tasklinks.push(task_id);
        }
    }
    
    /// Get termlinks
    pub fn termlinks(&self) -> &[Term] {
        &self.termlinks
    }
    
    /// Get tasklinks
    pub fn tasklinks(&self) -> &[u64] {
        &self.tasklinks
    }
    
    /// Get all tasks in this concept
    pub fn tasks(&self, beliefs: bool, questions: bool, goals: bool, quests: bool) -> Vec<&Task> {
        let mut tasks = Vec::new();
        
        if beliefs {
            tasks.extend(self.beliefs.tasks());
        }
        if goals {
            tasks.extend(self.goals.tasks());
        }
        if questions {
            tasks.extend(self.questions.tasks());
        }
        if quests {
            tasks.extend(self.quests.tasks());
        }
        
        tasks
    }
    
    /// Get the highest priority belief for a given time
    pub fn best_belief(&self, _time: Option<i64>) -> Option<&Task> {
        self.beliefs.highest_priority()
    }
    
    /// Get the highest priority goal for a given time
    pub fn best_goal(&self, _time: Option<i64>) -> Option<&Task> {
        self.goals.highest_priority()
    }
}

impl fmt::Display for TaskConcept {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        writeln!(f, "TaskConcept: {}", self.term())?;
        writeln!(f, "  Activation: {:.2}", self.activation)?;
        writeln!(f, "  Beliefs: {}", self.beliefs.len())?;
        writeln!(f, "  Goals: {}", self.goals.len())?;
        writeln!(f, "  Questions: {}", self.questions.len())?;
        writeln!(f, "  Quests: {}", self.quests.len())?;
        writeln!(f, "  Termlinks: {}", self.termlinks.len())?;
        writeln!(f, "  Tasklinks: {}", self.tasklinks.len())
    }
}



#[cfg(test)]
mod tests {
    use super::*;
    use crate::term::atom::Atomic;
    use crate::truth::Truth;
    use crate::task::Budget;

    #[test]
    fn test_concept_creation() {
        let term = Term::Atomic(Atomic::new_atom("cat"));
        let concept = Concept::new(term.clone());
        
        assert_eq!(concept.term(), &term);
        assert_eq!(concept.beliefs().len(), 0);
        assert_eq!(concept.goals().len(), 0);
        assert_eq!(concept.questions().len(), 0);
        assert_eq!(concept.quests().len(), 0);
    }

    #[test]
    fn test_concept_activation() {
        let term = Term::Atomic(Atomic::new_atom("cat"));
        let mut concept = Concept::new(term);
        
        assert_eq!(concept.activation(), 0.0);
        
        concept.set_activation(0.7);
        assert_eq!(concept.activation(), 0.7);
        
        concept.increase_activation(0.2);
        assert_eq!(concept.activation(), 0.9);
        
        concept.decay_activation(0.1);
        assert!((concept.activation() - 0.81).abs() < 0.001);
    }

    #[test]
    fn test_task_table() {
        let mut table = TaskTable::new();
        assert!(table.is_empty());
        
        let task1 = crate::task::TaskBuilder::new()
            .id(1)
            .term(Term::Atomic(Atomic::new_atom("cat")))
            .truth(Truth::new(0.9, 0.8))
            .punctuation(Punctuation::Belief)
            .budget(Budget::new(0.7, 0.6, 0.5))
            .build()
            .expect("Failed to build task");
            
        let task2 = crate::task::TaskBuilder::new()
            .id(2)
            .term(Term::Atomic(Atomic::new_atom("dog")))
            .truth(Truth::new(0.8, 0.9))
            .punctuation(Punctuation::Belief)
            .budget(Budget::new(0.8, 0.7, 0.6))
            .build()
            .expect("Failed to build task");
            
        table.add(task1.clone());
        table.add(task2.clone());
        
        assert_eq!(table.len(), 2);
        assert!(!table.is_empty());
        
        assert_eq!(table.get(1).unwrap().id(), 1);
        assert_eq!(table.get(2).unwrap().id(), 2);
        
        let highest = table.highest_priority().unwrap();
        assert_eq!(highest.id(), 2); // task2 has higher priority
        
        let above_threshold = table.tasks_above_priority(0.75);
        assert_eq!(above_threshold.len(), 1);
        assert_eq!(above_threshold[0].id(), 2);
    }

    #[test]
    fn test_concept_add_task() {
        let term = Term::Atomic(Atomic::new_atom("cat"));
        let mut concept = Concept::new(term);
        
        let belief_task = crate::task::TaskBuilder::new()
            .id(1)
            .term(Term::Atomic(Atomic::new_atom("cat")))
            .truth(Truth::new(0.9, 0.8))
            .punctuation(Punctuation::Belief)
            .budget(Budget::new(0.7, 0.6, 0.5))
            .build()
            .expect("Failed to build task");
            
        let goal_task = crate::task::TaskBuilder::new()
            .id(2)
            .term(Term::Atomic(Atomic::new_atom("cat")))
            .truth(Truth::new(0.8, 0.9))
            .punctuation(Punctuation::Goal)
            .budget(Budget::new(0.8, 0.7, 0.6))
            .build()
            .expect("Failed to build task");
            
        concept.add_task(belief_task);
        concept.add_task(goal_task);
        
        assert_eq!(concept.beliefs().len(), 1);
        assert_eq!(concept.goals().len(), 1);
        assert_eq!(concept.questions().len(), 0);
        assert_eq!(concept.quests().len(), 0);
    }
}