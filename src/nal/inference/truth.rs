//! Truth functions for NAL inference rules.
use crate::truth::Truth;

/// Computes the truth value of a deduction.
///
/// T_ded = (f1 * f2, f1 * f2 * c1 * c2)
pub fn deduction(t1: &Truth, t2: &Truth) -> Truth {
    let f = t1.frequency() * t2.frequency();
    let c = t1.frequency() * t2.frequency() * t1.confidence() * t2.confidence();
    Truth::new(f, c)
}
