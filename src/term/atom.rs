//! Atomic terms in NARS
//!
//! Atomic terms are the simplest terms in NARS. They include:
//! - Atoms (strings like "cat", "dog")
//! - Integers
//! - Boolean values

use super::{TermTrait, Op};
use crate::Term;
use std::fmt;
use std::hash::{Hash, Hasher};
use smartstring::SmartString;

/// Atomic term variants
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Atomic {
    /// Regular atomic term with a string identifier
    Atom(SmartString<smartstring::LazyCompact>),
    
    /// Integer atomic term
    Int(i32),
    
    /// Boolean atomic term
    Bool(bool),
}

impl Atomic {
    /// Create a new atom from a string
    pub fn new_atom(name: &str) -> Self {
        Atomic::Atom(SmartString::from(name))
    }
    
    /// Create a new integer term
    pub fn new_int(value: i32) -> Self {
        Atomic::Int(value)
    }
    
    /// Create a new boolean term
    pub fn new_bool(value: bool) -> Self {
        Atomic::Bool(value)
    }
    

}

impl TermTrait for Atomic {
    fn complexity(&self) -> usize {
        1
    }
    
    fn op_id(&self) -> Op {
        match self {
            Atomic::Atom(_) => Op::Atom,
            Atomic::Int(_) => Op::Int,
            Atomic::Bool(_) => Op::Bool,
        }
    }
    
    fn is_atomic(&self) -> bool {
        true
    }
    
    fn is_compound(&self) -> bool {
        false
    }
    
    fn concept(&self) -> Term {
        Term::Atomic(self.clone())
    }
    
    fn root(&self) -> Term {
        Term::Atomic(self.clone())
    }

    fn transform<F>(&self, f: &mut F) -> Term
    where
        F: FnMut(&Term) -> Term,
    {
        f(&Term::Atomic(self.clone()))
    }

    fn match_term(&self, pattern: &Term) -> bool {
        match pattern {
            Term::Atomic(p) => self == p,
            Term::Variable(_) => true,
            _ => false,
        }
    }

    fn subterms(&self) -> Vec<Term> {
        Vec::new()
    }
}

impl fmt::Display for Atomic {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Atomic::Atom(s) => write!(f, "{}", s),
            Atomic::Int(i) => write!(f, "{}", i),
            Atomic::Bool(b) => write!(f, "{}", b),
        }
    }
}

impl Hash for Atomic {
    fn hash<H: Hasher>(&self, state: &mut H) {
        match self {
            Atomic::Atom(s) => {
                Op::Atom.hash(state);
                s.hash(state);
            },
            Atomic::Int(i) => {
                Op::Int.hash(state);
                i.hash(state);
            },
            Atomic::Bool(b) => {
                Op::Bool.hash(state);
                b.hash(state);
            },
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_atom_creation() {
        let atom = Atomic::new_atom("cat");
        assert_eq!(format!("{}", atom), "cat");
        assert_eq!(atom.complexity(), 1);
        assert_eq!(atom.op_id(), Op::Atom);
    }

    #[test]
    fn test_int_creation() {
        let int_term = Atomic::new_int(42);
        assert_eq!(format!("{}", int_term), "42");
        assert_eq!(int_term.complexity(), 1);
        assert_eq!(int_term.op_id(), Op::Int);
    }

    #[test]
    fn test_bool_creation() {
        let true_term = Atomic::new_bool(true);
        let false_term = Atomic::new_bool(false);
        assert_eq!(format!("{}", true_term), "true");
        assert_eq!(format!("{}", false_term), "false");
        assert_eq!(true_term.complexity(), 1);
        assert_eq!(true_term.op_id(), Op::Bool);
    }


}