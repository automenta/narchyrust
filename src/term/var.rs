//! Variable terms in NARS
//!
//! Variables in NARS are special atomic terms that can be substituted with other terms.
//! There are three types of variables:
//! - Dependent variables (#)
//! - Independent variables ($)
//! - Query variables (?)

use super::{TermTrait, Op};
use crate::Term;
use std::fmt;
use std::hash::{Hash, Hasher};
use smartstring::SmartString;

/// Variable term
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Variable {
    /// The underlying atomic term
    name: SmartString<smartstring::LazyCompact>,
    
    /// The variable type
    var_type: Op,
}

impl Variable {
    /// Create a new dependent variable
    pub fn new_dep(name: &str) -> Self {
        let full_name: SmartString<smartstring::LazyCompact> = if name.starts_with('#') {
            SmartString::from(name)
        } else {
            SmartString::from(format!("#{}", name))
        };
        Variable {
            name: full_name.into(),
            var_type: Op::VarDep,
        }
    }
    
    /// Create a new independent variable
    pub fn new_indep(name: &str) -> Self {
        let full_name: SmartString<smartstring::LazyCompact> = if name.starts_with('$') {
            SmartString::from(name)
        } else {
            SmartString::from(format!("${}", name))
        };
        Variable {
            name: full_name.into(),
            var_type: Op::VarIndep,
        }
    }
    
    /// Create a new query variable
    pub fn new_query(name: &str) -> Self {
        let full_name: SmartString<smartstring::LazyCompact> = if name.starts_with('?') {
            SmartString::from(name)
        } else {
            SmartString::from(format!("?{}", name))
        };
        Variable {
            name: full_name.into(),
            var_type: Op::VarQuery,
        }
    }
    
    /// Get the variable name without the prefix
    pub fn name(&self) -> &str {
        &self.name[1..] // Skip the first character which is the prefix
    }
    
    /// Create a new pattern variable
    pub fn new_pattern(name: &str) -> Self {
        let full_name: SmartString<smartstring::LazyCompact> = if name.starts_with('%') {
            SmartString::from(name)
        } else {
            SmartString::from(format!("%{}", name))
        };
        Variable {
            name: full_name.into(),
            var_type: Op::VarPattern,
        }
    }

    /// Get the variable prefix
    pub fn prefix(&self) -> char {
        match self.var_type {
            Op::VarDep => '#',
            Op::VarIndep => '$',
            Op::VarQuery => '?',
            Op::VarPattern => '%',
            _ => panic!("Invalid variable type"),
        }
    }
}

impl TermTrait for Variable {
    fn complexity(&self) -> usize {
        1
    }
    
    fn op_id(&self) -> Op {
        self.var_type
    }
    
    fn is_atomic(&self) -> bool {
        true
    }
    
    fn is_compound(&self) -> bool {
        false
    }
    
    fn concept(&self) -> Term {
        Term::Variable(self.clone())
    }
    
    fn root(&self) -> Term {
        Term::Variable(self.clone())
    }

    fn transform<F>(&self, f: &mut F) -> Term
    where
        F: FnMut(&Term) -> Term,
    {
        f(&Term::Variable(self.clone()))
    }

    fn match_term(&self, _pattern: &Term) -> bool {
        true
    }

    fn subterms(&self) -> Vec<Term> {
        Vec::new()
    }
}

impl fmt::Display for Variable {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.name)
    }
}

impl Hash for Variable {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.var_type.hash(state);
        self.name.hash(state);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_variable_creation() {
        let dep_var = Variable::new_dep("x");
        assert_eq!(format!("{}", dep_var), "#x");
        assert_eq!(dep_var.op_id(), Op::VarDep);
        assert_eq!(dep_var.prefix(), '#');
        assert_eq!(dep_var.name(), "x");
        
        let indep_var = Variable::new_indep("y");
        assert_eq!(format!("{}", indep_var), "$y");
        assert_eq!(indep_var.op_id(), Op::VarIndep);
        assert_eq!(indep_var.prefix(), '$');
        assert_eq!(indep_var.name(), "y");
        
        let query_var = Variable::new_query("z");
        assert_eq!(format!("{}", query_var), "?z");
        assert_eq!(query_var.op_id(), Op::VarQuery);
        assert_eq!(query_var.prefix(), '?');
        assert_eq!(query_var.name(), "z");

        let pattern_var = Variable::new_pattern("S");
        assert_eq!(format!("{}", pattern_var), "%S");
        assert_eq!(pattern_var.op_id(), Op::VarPattern);
        assert_eq!(pattern_var.prefix(), '%');
        assert_eq!(pattern_var.name(), "S");
    }

    #[test]
    fn test_variable_with_prefix() {
        let dep_var = Variable::new_dep("#x");
        assert_eq!(format!("{}", dep_var), "#x");
        
        let indep_var = Variable::new_indep("$y");
        assert_eq!(format!("{}", indep_var), "$y");
    }
}