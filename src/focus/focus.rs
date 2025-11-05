//! Focus struct for NARS

use crate::term::Term;
use crate::truth::Truth;

/// Represents a focus of attention in the reasoner
#[derive(Debug, Clone, PartialEq)]
pub struct Focus {
    pub id: Term,
    pub truth: Option<Truth>,
}

impl Focus {
    /// Create a new focus
    pub fn new(id: Term, truth: Option<Truth>) -> Self {
        Self { id, truth }
    }

    /// Get the frequency of the focus
    pub fn freq(&self) -> f32 {
        // Placeholder for frequency calculation
        0.5
    }
}
