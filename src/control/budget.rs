
use crate::{
    deriver::Deriver,
    focus::Focus,
    task::Task,
};

/// The `Budget` trait defines the interface for different attention management strategies.
pub trait Budget {
    /// Calculates the priority of a derived task.
    fn pri_derived(&self, t: &Task, d: &dyn Deriver) -> f32;

    /// Calculates the priority of an input task.
    fn pri_in(&self, t: &Task, f: &Focus) -> f32;
}

/// `DefaultBudget` provides a basic implementation of the `Budget` trait.
pub struct DefaultBudget {
    pub punc_derived_j: f32,
    pub punc_derived_q: f32,
    pub punc_derived_a: f32,
    pub punc_derived_g: f32,
    pub simple: f32,
    pub certain: f32,
    pub input_activation: f32,
    pub polarized: f32,
}

impl Default for DefaultBudget {
    fn default() -> Self {
        DefaultBudget {
            punc_derived_j: 0.9,
            punc_derived_q: 0.9,
            punc_derived_a: 0.9,
            punc_derived_g: 0.9,
            simple: 0.5,
            certain: 1.0,
            input_activation: 1.0,
            polarized: 0.0,
        }
    }
}

impl Budget for DefaultBudget {
    /// Calculates the priority of a derived task.
    fn pri_derived(&self, t: &Task, _d: &dyn Deriver) -> f32 {
        let punc_factor = match t.punctuation() {
            crate::task::Punctuation::Belief => self.punc_derived_j,
            crate::task::Punctuation::Goal => self.punc_derived_g,
            crate::task::Punctuation::Question => self.punc_derived_q,
            crate::task::Punctuation::Quest => self.punc_derived_a,
            _ => 1.0,
        };
        punc_factor
    }

    /// Calculates the priority of an input task.
    fn pri_in(&self, t: &Task, _f: &Focus) -> f32 {
        self.input_activation * t.budget().priority()
    }
}
