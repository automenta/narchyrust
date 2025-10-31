//! Compound terms in NARS
//!
//! Compound terms are constructed from other terms using operators.
//! Examples include conjunctions, implications, inheritances, etc.

use super::{TermTrait, Op, Term};
use std::fmt;
use std::hash::{Hash, Hasher};
use std::sync::Arc;

/// Compound term
#[derive(Debug, Clone)]
pub struct Compound {
    /// The operator of this compound term
    operator: Op,
    
    /// The subterms of this compound term
    subterms: Arc<[Term]>,
    
    /// Temporal relation value (for temporal operators)
    dt: Option<i32>,
}

impl Compound {
    /// Create a new compound term
    pub fn new(operator: Op, subterms: Vec<Term>) -> Self {
        Compound {
            operator,
            subterms: subterms.into(),
            dt: None,
        }
    }
    
    /// Create a new temporal compound term
    pub fn new_temporal(operator: Op, subterms: Vec<Term>, dt: i32) -> Self {
        Compound {
            operator,
            subterms: subterms.into(),
            dt: Some(dt),
        }
    }
    
    /// Get the subterms of this compound
    pub fn subterms(&self) -> &[Term] {
        &self.subterms
    }
    
    /// Get the temporal relation value
    pub fn dt(&self) -> Option<i32> {
        self.dt
    }
    
    /// Check if this is a temporal compound
    pub fn is_temporal(&self) -> bool {
        self.dt.is_some()
    }
    
    /// Check if this is a sequence (temporal conjunction)
    pub fn is_sequence(&self) -> bool {
        self.operator == Op::Conjunction && self.is_temporal()
    }
    
    /// Get the number of subterms
    pub fn len(&self) -> usize {
        self.subterms.len()
    }
    
    /// Check if there are no subterms
    pub fn is_empty(&self) -> bool {
        self.subterms.is_empty()
    }
    
    /// Get a subterm by index
    pub fn get(&self, index: usize) -> Option<&Term> {
        self.subterms.get(index)
    }
}

impl TermTrait for Compound {
    fn complexity(&self) -> usize {
        // Complexity is 1 for the operator plus the sum of complexities of subterms
        1 + self.subterms.iter().map(|t| t.complexity()).sum::<usize>()
    }
    
    fn op_id(&self) -> Op {
        self.operator
    }
    
    fn is_atomic(&self) -> bool {
        false
    }
    
    fn is_compound(&self) -> bool {
        true
    }
    
    fn concept(&self) -> Term {
        // For compounds, the concept is the normalized root
        self.root()
    }
    
    fn root(&self) -> Term {
        // Root is the same as the term for now (simplified)
        // In a full implementation, this would normalize the term
        Term::Compound(self.clone())
    }
}

impl fmt::Display for Compound {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self.operator {
            Op::Neg => {
                // Unary operator
                if let Some(term) = self.subterms.first() {
                    write!(f, "--{}", term)
                } else {
                    write!(f, "--")
                }
            },
            Op::Inheritance | Op::Implication | Op::Similarity | Op::Equivalence => {
                // Binary operators with infix notation
                if self.subterms.len() == 2 {
                    write!(f, "({} {} {})", self.subterms[0], self.operator, self.subterms[1])
                } else {
                    write!(f, "({} {})", self.operator, 
                           self.subterms.iter().map(|t| format!("{}", t)).collect::<Vec<_>>().join(" "))
                }
            },
            Op::Conjunction | Op::Disjunction => {
                // N-ary operators with infix notation
                if let Some(dt) = self.dt {
                    write!(f, "({})",
                           self.subterms.iter().map(|t| format!("{}", t)).collect::<Vec<_>>().join(&format!(" {} ", self.operator)))?;
                    if dt != 0 {
                        write!(f, "_{}", dt)?;
                    }
                    Ok(())
                } else {
                    write!(f, "({})",
                           self.subterms.iter().map(|t| format!("{}", t)).collect::<Vec<_>>().join(&format!(" {} ", self.operator)))
                }
            },
            _ => {
                // Default prefix notation
                write!(f, "({} {})", self.operator, 
                       self.subterms.iter().map(|t| format!("{}", t)).collect::<Vec<_>>().join(" "))
            }
        }
    }
}

impl Hash for Compound {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.operator.hash(state);
        self.subterms.hash(state);
        self.dt.hash(state);
    }
}

impl PartialEq for Compound {
    fn eq(&self, other: &Self) -> bool {
        self.operator == other.operator && 
        self.subterms == other.subterms && 
        self.dt == other.dt
    }
}

impl Eq for Compound {}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::term::atom::Atomic;

    #[test]
    fn test_compound_creation() {
        let atom1 = Term::Atomic(Atomic::new_atom("cat"));
        let atom2 = Term::Atomic(Atomic::new_atom("dog"));
        let subterms = vec![atom1, atom2];
        
        let compound = Compound::new(Op::Conjunction, subterms);
        assert_eq!(compound.len(), 2);
        assert_eq!(compound.complexity(), 3); // 1 + 1 + 1
        assert_eq!(compound.op_id(), Op::Conjunction);
        assert!(!compound.is_temporal());
    }

    #[test]
    fn test_temporal_compound() {
        let atom1 = Term::Atomic(Atomic::new_atom("cat"));
        let atom2 = Term::Atomic(Atomic::new_atom("dog"));
        let subterms = vec![atom1, atom2];
        
        let temporal_compound = Compound::new_temporal(Op::Conjunction, subterms, 5);
        assert_eq!(temporal_compound.dt(), Some(5));
        assert!(temporal_compound.is_temporal());
        assert!(temporal_compound.is_sequence());
    }

    #[test]
    fn test_compound_display() {
        let atom1 = Term::Atomic(Atomic::new_atom("cat"));
        let atom2 = Term::Atomic(Atomic::new_atom("dog"));
        
        // Test conjunction
        let conj_subterms = vec![atom1.clone(), atom2.clone()];
        let conjunction = Compound::new(Op::Conjunction, conj_subterms);
        assert_eq!(format!("{}", conjunction), "(cat & dog)");
        
        // Test inheritance
        let inh_subterms = vec![atom1.clone(), atom2.clone()];
        let inheritance = Compound::new(Op::Inheritance, inh_subterms);
        assert_eq!(format!("{}", inheritance), "(cat --> dog)");
        
        // Test negation
        let neg_subterms = vec![atom1.clone()];
        let negation = Compound::new(Op::Neg, neg_subterms);
        assert_eq!(format!("{}", negation), "--cat");
        
        // Test temporal conjunction
        let temp_subterms = vec![atom1, atom2];
        let temporal = Compound::new_temporal(Op::Conjunction, temp_subterms, 3);
        assert_eq!(format!("{}", temporal), "(cat & dog)_3");
    }
    
    #[test]
    fn test_temporal_compound_display() {
        let a = Term::Atomic(Atomic::new_atom("a"));
        let b = Term::Atomic(Atomic::new_atom("b"));
        
        // Test sequential conjunction (&/)
        let seq_compound = Compound::new_temporal(Op::Conjunction, vec![a.clone(), b.clone()], 1);
        // Note: The display format depends on how we map operators
        // For sequential conjunction, we might want to use a different operator
        assert_eq!(format!("{}", seq_compound), "(a & b)_1");
        
        // Test parallel conjunction (&|)
        let par_compound = Compound::new_temporal(Op::Intersection, vec![a.clone(), b.clone()], 0);
        assert_eq!(format!("{}", par_compound), "(| a b)");
    }
    
    #[test]
    fn test_complex_compound_structures() {
        let cat = Term::Atomic(Atomic::new_atom("cat"));
        let dog = Term::Atomic(Atomic::new_atom("dog"));
        let animal = Term::Atomic(Atomic::new_atom("animal"));
        
        // Test nested compounds
        let conjunction = Term::Compound(Compound::new(Op::Conjunction, vec![cat.clone(), dog.clone()]));
        let inheritance = Term::Compound(Compound::new(Op::Inheritance, vec![conjunction, animal]));
        assert_eq!(format!("{}", inheritance), "((cat & dog) --> animal)");
    }
}