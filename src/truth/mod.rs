//! Truth values in NARS
//!
//! Truth values represent the uncertainty and belief strength associated with judgments.
//! They consist of two components:
//! - Frequency (f): The estimated probability of the statement being true
//! - Confidence (c): The weight of evidence supporting the estimation

use std::fmt;
use std::hash::{Hash, Hasher};
use ordered_float::OrderedFloat;

/// Truth value representation
#[derive(Debug, Clone, Copy)]
pub struct Truth {
    /// Frequency: probability estimate [0.0, 1.0]
    frequency: OrderedFloat<f32>,
    
    /// Confidence: evidence weight [0.0, 1.0]
    confidence: OrderedFloat<f32>,
}

impl Truth {
    /// Create a new truth value
    pub fn new(frequency: f32, confidence: f32) -> Self {
        // Clamp values to valid ranges
        let f = frequency.clamp(0.0, 1.0);
        let c = confidence.clamp(0.0, 1.0);
        
        Truth {
            frequency: OrderedFloat(f),
            confidence: OrderedFloat(c),
        }
    }
    
    /// Get the frequency component
    pub fn frequency(&self) -> f32 {
        self.frequency.0
    }
    
    /// Get the confidence component
    pub fn confidence(&self) -> f32 {
        self.confidence.0
    }
    
    /// Calculate evidence amount from confidence
    pub fn evidence(&self) -> f64 {
        // Convert confidence to evidence using c = e / (e + k)
        // where k is a parameter (typically 1.0 in NARS)
        let c = self.confidence.0 as f64;
        c / (1.0 - c)
    }
    
    /// Create a truth value from evidence amount
    pub fn from_evidence(freq: f32, evidence: f64) -> Self {
        // Convert evidence to confidence using c = e / (e + k)
        let conf = (evidence / (evidence + 1.0)) as f32;
        Truth::new(freq, conf)
    }
    
    /// Get the expected value (frequency * confidence)
    pub fn expectation(&self) -> f32 {
        self.frequency.0 * self.confidence.0
    }
    
    /// Check if this truth value is analytical (confidence = 1.0)
    pub fn is_analytical(&self) -> bool {
        self.confidence.0 == 1.0
    }
    
    /// Check if this truth value is eternal (not temporal)
    pub fn is_eternal(&self) -> bool {
        // In this simple implementation, all truths are eternal
        // A more complete implementation would have temporal truths
        true
    }
    
    /// Negate this truth value (1 - frequency, same confidence)
    pub fn neg(&self) -> Self {
        Truth::new(1.0 - self.frequency.0, self.confidence.0)
    }
    
    /// Deduction: C1 and (C1 ==> C2) |- C2
    pub fn deduction(a: &Truth, b: &Truth) -> Self {
        let f = a.frequency() * b.frequency();
        let c = a.confidence() * b.confidence() * f;
        Truth::new(f, c)
    }
    
    /// Induction: C1 and C2 |- C1 ==> C2
    pub fn induction(a: &Truth, b: &Truth) -> Self {
        let f = a.frequency() * b.frequency();
        let c = a.confidence() * b.confidence() * f / (a.frequency() * b.frequency() + 1.0);
        Truth::new(f, c)
    }
    
    /// Comparison: C1 and C2 |- C1 <-> C2
    pub fn comparison(a: &Truth, b: &Truth) -> Self {
        let f = a.frequency() * b.frequency() / (a.frequency() * b.frequency() + (1.0 - a.frequency()) * (1.0 - b.frequency()));
        let c = a.confidence() * b.confidence() * f;
        Truth::new(f, c)
    }
    
    /// Conjunction: C1 and C2 |- C1 && C2
    pub fn conjunction(a: &Truth, b: &Truth) -> Self {
        let f = a.frequency() * b.frequency();
        let c = a.confidence() * b.confidence();
        Truth::new(f, c)
    }
    
    /// Disjunction: C1 and C2 |- C1 || C2
    pub fn disjunction(a: &Truth, b: &Truth) -> Self {
        let f = a.frequency() + b.frequency() - a.frequency() * b.frequency();
        let c = a.confidence() * b.confidence();
        Truth::new(f, c)
    }
    
    /// Revision: C1 and C2 |- C3 (revised belief)
    pub fn revision(a: &Truth, b: &Truth) -> Self {
        let w1 = a.evidence();
        let w2 = b.evidence();
        let w = w1 + w2;
        let f = (w1 * a.frequency() as f64 + w2 * b.frequency() as f64) / w;
        let _c = w / (w + 1.0);
        Truth::from_evidence(f as f32, w)
    }
    
    /// Abduction: C2 and (C1 ==> C2) |- C1
    pub fn abduction(a: &Truth, b: &Truth) -> Self {
        let f = a.frequency() * b.frequency();
        let c = a.confidence() * b.confidence() * f / (b.frequency() * b.frequency() + 1.0);
        Truth::new(f, c)
    }
    
    /// Exemplification: C1 and C2 |- C2 ==> C1
    pub fn exemplification(a: &Truth, b: &Truth) -> Self {
        let f = 1.0 - a.frequency() * b.frequency();
        let c = a.confidence() * b.confidence() * f / (a.frequency() * b.frequency() + 1.0);
        Truth::new(f, c)
    }
    
    /// Analogy: (C1 ==> C2) and (C2 ==> C3) |- (C1 ==> C3)
    pub fn analogy(a: &Truth, b: &Truth) -> Self {
        let f = a.frequency() * b.frequency();
        let c = a.confidence() * b.confidence() * f;
        Truth::new(f, c)
    }
}

impl fmt::Display for Truth {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "({}, {})", 
               (self.frequency.0 * 100.0).round() / 100.0, 
               (self.confidence.0 * 100.0).round() / 100.0)
    }
}

impl Hash for Truth {
    fn hash<H: Hasher>(&self, state: &mut H) {
        // Hash with reduced precision to account for floating point errors
        let freq_hash = (self.frequency.0 * 10000.0) as i32;
        let conf_hash = (self.confidence.0 * 10000.0) as i32;
        freq_hash.hash(state);
        conf_hash.hash(state);
    }
}

impl PartialEq for Truth {
    fn eq(&self, other: &Self) -> bool {
        // Compare with reduced precision to account for floating point errors
        (self.frequency.0 - other.frequency.0).abs() < 0.0001 &&
        (self.confidence.0 - other.confidence.0).abs() < 0.0001
    }
}

impl Eq for Truth {}

/// Common truth values
impl Truth {
    /// Default truth value for input beliefs
    pub fn default_belief() -> Self {
        Truth::new(1.0, 0.9)
    }
    
    /// Default truth value for input goals
    pub fn default_goal() -> Self {
        Truth::new(1.0, 0.9)
    }
    
    /// Truth value representing uncertainty
    pub fn uncertainty() -> Self {
        Truth::new(0.5, 0.0)
    }
    
    /// Truth value representing falsehood
    pub fn falsehood() -> Self {
        Truth::new(0.0, 0.9)
    }
    
    /// Truth value representing truth
    pub fn default_truth() -> Self {
        Truth::new(1.0, 0.9)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_truth_creation() {
        let truth = Truth::new(0.8, 0.9);
        assert_eq!(truth.frequency(), 0.8);
        assert_eq!(truth.confidence(), 0.9);
    }

    #[test]
    fn test_truth_clamping() {
        let truth1 = Truth::new(1.5, 0.9);  // Frequency too high
        assert_eq!(truth1.frequency(), 1.0);
        
        let truth2 = Truth::new(-0.5, 0.9); // Frequency too low
        assert_eq!(truth2.frequency(), 0.0);
        
        let truth3 = Truth::new(0.8, 1.5);  // Confidence too high
        assert_eq!(truth3.confidence(), 1.0);
        
        let truth4 = Truth::new(0.8, -0.5); // Confidence too low
        assert_eq!(truth4.confidence(), 0.0);
    }

    #[test]
    fn test_evidence_conversion() {
        let truth = Truth::new(0.8, 0.5);
        let evidence = truth.evidence();
        assert!((evidence - 1.0).abs() < 0.0001);
        
        let truth2 = Truth::from_evidence(0.7, 3.0);
        assert!((truth2.confidence() - 0.75).abs() < 0.0001);
    }

    #[test]
    fn test_negation() {
        let truth = Truth::new(0.8, 0.9);
        let negated = truth.neg();
        assert!((negated.frequency() - 0.2).abs() < 0.0001);
        assert_eq!(negated.confidence(), 0.9);
    }

    #[test]
    fn test_expectation() {
        let truth = Truth::new(0.8, 0.9);
        assert!((truth.expectation() - 0.72).abs() < 0.0001);
    }

    #[test]
    fn test_display() {
        let truth = Truth::new(0.856, 0.912);
        assert_eq!(format!("{}", truth), "(0.86, 0.91)");
    }

    #[test]
    fn test_equality() {
        let truth1 = Truth::new(0.8, 0.9);
        let truth2 = Truth::new(0.80001, 0.89999);
        assert_eq!(truth1, truth2);
        
        let truth3 = Truth::new(0.8, 0.9);
       let truth4 = Truth::new(0.81, 0.9);
       assert_ne!(truth3, truth4);
   }
   
   #[test]
   fn test_deduction() {
       let a = Truth::new(0.9, 0.9);
       let b = Truth::new(0.8, 0.8);
       let result = Truth::deduction(&a, &b);
       assert!((result.frequency() - 0.72).abs() < 0.0001);
       assert!((result.confidence() - 0.5184).abs() < 0.0001);
   }
   
   #[test]
   fn test_induction() {
       let a = Truth::new(0.9, 0.9);
       let b = Truth::new(0.8, 0.8);
       let result = Truth::induction(&a, &b);
       assert!((result.frequency() - 0.72).abs() < 0.0001);
       // The exact value depends on the implementation, but it should be less than the product
       assert!(result.confidence() < 0.65);
   }
   
   #[test]
   fn test_comparison() {
       let a = Truth::new(0.9, 0.9);
       let b = Truth::new(0.8, 0.8);
       let result = Truth::comparison(&a, &b);
       // The exact value depends on the implementation
       assert!(result.frequency() > 0.97 && result.frequency() < 0.98);
       assert!(result.confidence() > 0.7 && result.confidence() < 0.71);
   }
   
   #[test]
   fn test_conjunction() {
       let a = Truth::new(0.9, 0.9);
       let b = Truth::new(0.8, 0.8);
       let result = Truth::conjunction(&a, &b);
       assert!((result.frequency() - 0.72).abs() < 0.0001);
       assert!((result.confidence() - 0.72).abs() < 0.0001);
   }
   
   #[test]
   fn test_disjunction() {
       let a = Truth::new(0.9, 0.9);
       let b = Truth::new(0.8, 0.8);
       let result = Truth::disjunction(&a, &b);
       assert!((result.frequency() - 0.98).abs() < 0.0001);
       assert!((result.confidence() - 0.72).abs() < 0.0001);
   }
   
   #[test]
   fn test_revision() {
       let a = Truth::new(0.9, 0.9);
       let b = Truth::new(0.8, 0.8);
       let result = Truth::revision(&a, &b);
       // The exact value depends on the evidence calculation
       assert!(result.frequency() > 0.8 && result.frequency() < 0.9);
       assert!(result.confidence() > 0.8);
   }
   
   #[test]
   fn test_abduction() {
       let a = Truth::new(0.9, 0.9);
       let b = Truth::new(0.8, 0.8);
       let result = Truth::abduction(&a, &b);
       assert!((result.frequency() - 0.72).abs() < 0.0001);
       // The exact value depends on the implementation, but it should be less than the product
       assert!(result.confidence() < 0.65);
   }
   
   #[test]
   fn test_exemplification() {
       let a = Truth::new(0.9, 0.9);
       let b = Truth::new(0.8, 0.8);
       let result = Truth::exemplification(&a, &b);
       assert!((result.frequency() - 0.28).abs() < 0.0001);
       // The exact value depends on the implementation
       assert!(result.confidence() < 0.3);
   }
   
   #[test]
   fn test_analogy() {
       let a = Truth::new(0.9, 0.9);
       let b = Truth::new(0.8, 0.8);
       let result = Truth::analogy(&a, &b);
       assert!((result.frequency() - 0.72).abs() < 0.0001);
       assert!((result.confidence() - 0.5184).abs() < 0.0001);
   }
}