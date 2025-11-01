//! Focus struct for NARS

use crate::term::Term;

/// Represents a focus of attention in the reasoner
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Focus {
    pub id: Term,
}

impl Focus {
    /// Create a new focus
    pub fn new(id: Term) -> Self {
        Self { id }
    }

    /// Get the frequency of the focus
    pub fn freq(&self) -> f32 {
        // Placeholder for frequency calculation
        0.5
    }
}