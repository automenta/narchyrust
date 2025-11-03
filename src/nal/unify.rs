//! Unification logic for NARS

use crate::term::{Term, TermTrait};
use std::collections::HashMap;

/// Unify a pattern term with a concrete term.
///
/// # Arguments
///
/// * `pattern` - The pattern term (may contain variables).
/// * `term` - The concrete term.
/// * `bindings` - A mutable HashMap to store variable bindings.
///
/// # Returns
///
/// `true` if the terms unify, `false` otherwise.
pub fn unify<'a>(pattern: &'a Term, term: &'a Term, bindings: &mut HashMap<&'a Term, &'a Term>) -> bool {
    match pattern {
        Term::Variable(_) => {
            if let Some(existing_binding) = bindings.get(pattern) {
                return existing_binding == &term;
            }
            bindings.insert(pattern, term);
            true
        }
        Term::Atomic(_) => {
            pattern == term
        }
        Term::Compound(p_compound) => {
            if let Term::Compound(t_compound) = term {
                if p_compound.op_id() == t_compound.op_id() && p_compound.len() == t_compound.len() {
                    for (p_sub, t_sub) in p_compound.subterms().iter().zip(t_compound.subterms().iter()) {
                        if !unify(p_sub, t_sub, bindings) {
                            return false;
                        }
                    }
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::term::{atom::Atomic, compound::Compound, var::Variable, Op};

    #[test]
    fn test_unify_atoms() {
        let a = Term::Atomic(Atomic::new_atom("a"));
        let b = Term::Atomic(Atomic::new_atom("b"));
        let mut bindings = HashMap::new();
        assert!(unify(&a, &a, &mut bindings));
        assert!(!unify(&a, &b, &mut bindings));
    }

    #[test]
    fn test_unify_variable() {
        let var_s = Term::Variable(Variable::new_pattern("S"));
        let bird = Term::Atomic(Atomic::new_atom("bird"));
        let mut bindings = HashMap::new();
        assert!(unify(&var_s, &bird, &mut bindings));
        assert_eq!(bindings.get(&var_s), Some(&&bird));
    }

    #[test]
    fn test_unify_compound() {
        let var_s = Term::Variable(Variable::new_pattern("S"));
        let bird = Term::Atomic(Atomic::new_atom("bird"));
        let animal = Term::Atomic(Atomic::new_atom("animal"));

        let pattern = Term::Compound(Compound::new(Op::Inheritance, vec![var_s.clone(), animal.clone()]));
        let term = Term::Compound(Compound::new(Op::Inheritance, vec![bird.clone(), animal.clone()]));

        let mut bindings = HashMap::new();
        assert!(unify(&pattern, &term, &mut bindings));
        assert_eq!(bindings.get(&var_s), Some(&&bird));
    }

    #[test]
    fn test_unify_compound_fail() {
        let var_s = Term::Variable(Variable::new_pattern("S"));
        let bird = Term::Atomic(Atomic::new_atom("bird"));
        let animal = Term::Atomic(Atomic::new_atom("animal"));
        let mammal = Term::Atomic(Atomic::new_atom("mammal"));

        let pattern = Term::Compound(Compound::new(Op::Inheritance, vec![var_s.clone(), animal.clone()]));
        let term = Term::Compound(Compound::new(Op::Inheritance, vec![bird.clone(), mammal.clone()]));

        let mut bindings = HashMap::new();
        assert!(!unify(&pattern, &term, &mut bindings));
    }
}
