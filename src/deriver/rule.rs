//! Inference rule representation

use crate::term::Term;
use crate::truth::Truth;

/// A single inference rule
#[derive(Debug, Clone)]
pub struct Rule {
    /// The term of the rule
    pub term: Term,

    /// The truth value of the rule
    pub truth: Truth,
}

/// A collection of inference rules
#[derive(Debug, Clone)]
pub struct RuleTree {
    /// The rules in the tree
    pub rules: Vec<Rule>,
}

impl RuleTree {
    /// Create a new rule tree
    pub fn new() -> Self {
        RuleTree { rules: Vec::new() }
    }

    /// Add a rule to the tree
    pub fn add(&mut self, rule: Rule) {
        self.rules.push(rule);
    }
}
