//! Term representation in NARS
//!
//! In NARS, a term is a word or phrase that refers to a concept. Terms can be atomic
//! (like "cat" or "dog") or compound (like "(&&, cat, dog)" or "(cat --> dog)").
//! Terms are the basic building blocks of NARS's logical language.

pub mod atom;
pub mod compound;
pub mod var;

use std::fmt;
use std::hash::{Hash, Hasher};

/// The basic trait for all terms in NARS
pub trait TermTrait: fmt::Display + fmt::Debug + Hash + Eq {
    /// Get the complexity of the term (number of subterms + 1)
    fn complexity(&self) -> usize;
    
    /// Get the operator ID of the term
    fn op_id(&self) -> Op;
    
    /// Check if the term is atomic
    fn is_atomic(&self) -> bool;
    
    /// Check if the term is compound
    fn is_compound(&self) -> bool;
    
    /// Get the term as a conceptual representation
    fn concept(&self) -> Term;
    
    /// Get the root of the term
    fn root(&self) -> Term;
}

/// Operator types for terms
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Op {
    // Atomic operators
    Atom,
    Int,
    Bool,
    
    // Variable operators
    VarDep,
    VarIndep,
    VarQuery,
    VarPattern,
    
    // Compound operators
    Neg,
    Conjunction,
    Disjunction,
    Intersection,
    Difference,
    Inheritance,
    Similarity,
    Implication,
    Equivalence,
    Instance,
    Property,
    InstanceProperty,
    ImageExt,
    ImageInt,
    
    // Special operators
    SetExt,
    SetInt,
    Product,
    ExtensionalImage,
    IntensionalImage,
}

impl fmt::Display for Op {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Op::Atom => write!(f, "Atom"),
            Op::Int => write!(f, "Int"),
            Op::Bool => write!(f, "Bool"),
            Op::VarDep => write!(f, "#"),
            Op::VarIndep => write!(f, "$"),
            Op::VarQuery => write!(f, "?"),
            Op::VarPattern => write!(f, "@"),
            Op::Neg => write!(f, "--"),
            Op::Conjunction => write!(f, "&"),
            Op::Disjunction => write!(f, "||"),
            Op::Intersection => write!(f, "|"),
            Op::Difference => write!(f, "~"),
            Op::Inheritance => write!(f, "-->"),
            Op::Similarity => write!(f, "<->"),
            Op::Implication => write!(f, "==>"),
            Op::Equivalence => write!(f, "<=>"),
            Op::Instance => write!(f, "{{"),
            Op::Property => write!(f, "}}"),
            Op::InstanceProperty => write!(f, "}}]"),
            Op::ImageExt => write!(f, "\\"),
            Op::ImageInt => write!(f, "/"),
            Op::SetExt => write!(f, "{{}}"),
            Op::SetInt => write!(f, "[]"),
            Op::Product => write!(f, "*"),
            Op::ExtensionalImage => write!(f, "\\"),
            Op::IntensionalImage => write!(f, "/"),
        }
    }
}

/// Base Term struct that can represent both atomic and compound terms
#[derive(Debug, Clone)]
pub enum Term {
    Atomic(atom::Atomic),
    Compound(compound::Compound),
    Variable(var::Variable),
}

impl TermTrait for Term {
    fn complexity(&self) -> usize {
        match self {
            Term::Atomic(a) => a.complexity(),
            Term::Compound(c) => c.complexity(),
            Term::Variable(v) => v.complexity(),
        }
    }
    
    fn op_id(&self) -> Op {
        match self {
            Term::Atomic(a) => a.op_id(),
            Term::Compound(c) => c.op_id(),
            Term::Variable(v) => v.op_id(),
        }
    }
    
    fn is_atomic(&self) -> bool {
        match self {
            Term::Atomic(_) => true,
            Term::Compound(_) => false,
            Term::Variable(_) => true,
        }
    }
    
    fn is_compound(&self) -> bool {
        match self {
            Term::Atomic(_) => false,
            Term::Compound(_) => true,
            Term::Variable(_) => false,
        }
    }
    
    fn concept(&self) -> Term {
        match self {
            Term::Atomic(a) => a.concept(),
            Term::Compound(c) => c.concept(),
            Term::Variable(v) => v.concept(),
        }
    }
    
    fn root(&self) -> Term {
        match self {
            Term::Atomic(a) => a.root(),
            Term::Compound(c) => c.root(),
            Term::Variable(v) => v.root(),
        }
    }
}

impl fmt::Display for Term {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Term::Atomic(a) => write!(f, "{}", a),
            Term::Compound(c) => write!(f, "{}", c),
            Term::Variable(v) => write!(f, "{}", v),
        }
    }
}

impl Hash for Term {
    fn hash<H: Hasher>(&self, state: &mut H) {
        match self {
            Term::Atomic(a) => a.hash(state),
            Term::Compound(c) => c.hash(state),
            Term::Variable(v) => v.hash(state),
        }
    }
}

impl PartialEq for Term {
    fn eq(&self, other: &Self) -> bool {
        match (self, other) {
            (Term::Atomic(a1), Term::Atomic(a2)) => a1 == a2,
            (Term::Compound(c1), Term::Compound(c2)) => c1 == c2,
            (Term::Variable(v1), Term::Variable(v2)) => v1 == v2,
            _ => false,
        }
    }
}

impl Eq for Term {}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_op_display() {
        assert_eq!(format!("{}", Op::Atom), "Atom");
        assert_eq!(format!("{}", Op::Conjunction), "&");
        assert_eq!(format!("{}", Op::Inheritance), "-->");
    }
    
    #[test]
    fn test_op_display_extended() {
        assert_eq!(format!("{}", Op::Atom), "Atom");
        assert_eq!(format!("{}", Op::Int), "Int");
        assert_eq!(format!("{}", Op::Bool), "Bool");
        assert_eq!(format!("{}", Op::VarDep), "#");
        assert_eq!(format!("{}", Op::VarIndep), "$");
        assert_eq!(format!("{}", Op::VarQuery), "?");
        assert_eq!(format!("{}", Op::VarPattern), "@");
        assert_eq!(format!("{}", Op::Neg), "--");
        assert_eq!(format!("{}", Op::Conjunction), "&");
        assert_eq!(format!("{}", Op::Disjunction), "||");
        assert_eq!(format!("{}", Op::Intersection), "|");
        assert_eq!(format!("{}", Op::Difference), "~");
        assert_eq!(format!("{}", Op::Inheritance), "-->");
        assert_eq!(format!("{}", Op::Similarity), "<->");
        assert_eq!(format!("{}", Op::Implication), "==>");
        assert_eq!(format!("{}", Op::Equivalence), "<=>");
        assert_eq!(format!("{}", Op::Instance), "{");
        assert_eq!(format!("{}", Op::Property), "}");
        assert_eq!(format!("{}", Op::InstanceProperty), "}]");
        assert_eq!(format!("{}", Op::ImageExt), "\\");
        assert_eq!(format!("{}", Op::ImageInt), "/");
        assert_eq!(format!("{}", Op::SetExt), "{}");
        assert_eq!(format!("{}", Op::SetInt), "[]");
        assert_eq!(format!("{}", Op::Product), "*");
        assert_eq!(format!("{}", Op::ExtensionalImage), "\\");
        assert_eq!(format!("{}", Op::IntensionalImage), "/");
    }
    
    #[test]
    fn test_complex_term_structures() {
        use crate::term::atom::Atomic;
        use crate::term::compound::Compound;
        
        // Test a complex inheritance term
        let cat = Term::Atomic(Atomic::new_atom("cat"));
        let animal = Term::Atomic(Atomic::new_atom("animal"));
        let inheritance = Term::Compound(Compound::new(Op::Inheritance, vec![cat.clone(), animal.clone()]));
        assert_eq!(format!("{}", inheritance), "(cat --> animal)");
        
        // Test a conjunction
        let walk = Term::Atomic(Atomic::new_atom("walk"));
        let run = Term::Atomic(Atomic::new_atom("run"));
        let conjunction = Term::Compound(Compound::new(Op::Conjunction, vec![walk.clone(), run]));
        assert_eq!(format!("{}", conjunction), "(walk & run)");
        
        // Test a nested compound term
        let conjunction2 = Term::Compound(Compound::new(Op::Conjunction, vec![cat.clone(), walk.clone()]));
        let nested = Term::Compound(Compound::new(Op::Inheritance, vec![conjunction2, animal.clone()]));
        assert_eq!(format!("{}", nested), "((cat & walk) --> animal)");
    }
    
    #[test]
    fn test_term_equality() {
        use crate::term::atom::Atomic;
        use crate::term::compound::Compound;
        
        // Test equality of atomic terms
        let cat1 = Term::Atomic(Atomic::new_atom("cat"));
        let cat2 = Term::Atomic(Atomic::new_atom("cat"));
        let dog = Term::Atomic(Atomic::new_atom("dog"));
        assert_eq!(cat1, cat2);
        assert_ne!(cat1, dog);
        
        // Test equality of compound terms
        let animal = Term::Atomic(Atomic::new_atom("animal"));
        let inheritance1 = Term::Compound(Compound::new(Op::Inheritance, vec![cat1.clone(), animal.clone()]));
        let inheritance2 = Term::Compound(Compound::new(Op::Inheritance, vec![cat2.clone(), animal.clone()]));
        assert_eq!(inheritance1, inheritance2);
    }
}