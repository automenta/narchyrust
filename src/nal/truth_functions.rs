//! Truth functions for NARS

use crate::truth::Truth;

/// A trait for truth functions that can be applied to derive new truth values.
pub trait TruthFunction {
    fn call(&self, premises: &[&Truth]) -> Truth;
}

/// The `implSyl` truth function for syllogistic deduction.
pub struct ImplSyl;

impl TruthFunction for ImplSyl {
    fn call(&self, premises: &[&Truth]) -> Truth {
        if let (Some(p1), Some(p2)) = (premises.get(0), premises.get(1)) {
            let f1 = p1.frequency();
            let c1 = p1.confidence();
            let f2 = p2.frequency();
            let c2 = p2.confidence();

            let f = f1 * f2;
            let c = c1 * c2 * f;

            Truth::new(f, c)
        } else {
            // Default truth value if premises are missing.
            Truth::new(0.0, 0.0)
        }
    }
}
