//! Concept utilities for NARS
//!
//! This module provides utilities for concept management in NARS.

use std::sync::{Arc, Mutex};
use crate::time::Time;
use crate::concept::TaskConcept;
use crate::term::Term;

/// Emotion tracking system for NARS
#[derive(Clone)]
pub struct Emotion {
    happiness: f32,
    sadness: f32,
    arousal: f32,
}

impl Emotion {
    /// Create a new emotion tracker
    pub fn new() -> Self {
        Emotion {
            happiness: 0.0,
            sadness: 0.0,
            arousal: 0.0,
        }
    }
    
    /// Update emotion values based on reasoning outcomes
    pub fn update(&mut self) {
        // Apply decay to emotion values
        self.happiness *= 0.95;
        self.sadness *= 0.95;
        self.arousal *= 0.95;
    }
    
    /// Get happiness value
    pub fn happiness(&self) -> f32 {
        self.happiness
    }
    
    /// Get sadness value
    pub fn sadness(&self) -> f32 {
        self.sadness
    }
    
    /// Get arousal value
    pub fn arousal(&self) -> f32 {
        self.arousal
    }
    
    /// Set happiness value
    pub fn set_happiness(&mut self, value: f32) {
        self.happiness = value.max(-1.0).min(1.0); // Clamp to [-1, 1]
    }
    
    /// Set sadness value
    pub fn set_sadness(&mut self, value: f32) {
        self.sadness = value.max(0.0).min(1.0); // Clamp to [0, 1]
    }
    
    /// Set arousal value
    pub fn set_arousal(&mut self, value: f32) {
        self.arousal = value.max(0.0).min(1.0); // Clamp to [0, 1]
    }
}

impl Default for Emotion {
    fn default() -> Self {
        Self::new()
    }
}

/// Concept builder for creating and managing concepts
pub struct ConceptBuilder {
    /// Emotion tracker
    emotion: Option<Emotion>,
    
    /// Time reference
    time: Option<Arc<Time>>,
    
    /// Whether concepts should be built with task support
    task_concept_only: bool,
}

impl ConceptBuilder {
    /// Create a new concept builder
    pub fn new() -> Self {
        ConceptBuilder {
            emotion: None,
            time: None,
            task_concept_only: true,
        }
    }
    
    /// Initialize the concept builder with emotion and time
    pub fn init(&mut self, emotion: Emotion, time: Arc<Time>) {
        self.emotion = Some(emotion);
        self.time = Some(time);
    }
    
    /// Build a concept from a term
    pub fn build(&self, term: &Term, _create_if_missing: bool, _dynamic: bool) -> Option<TaskConcept> {
        // In a full implementation, this would create a concept based on the term
        // For now, we'll create a simple task concept
        Some(TaskConcept::new(term.clone()))
    }
    

    /// Get the emotion tracker
    pub fn emotion(&self) -> Option<&Emotion> {
        self.emotion.as_ref()
    }
    
    /// Get the time reference
    pub fn time(&self) -> Option<&Arc<Time>> {
        self.time.as_ref()
    }
}

impl Default for ConceptBuilder {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::term::Term;
    use crate::time::Time;

    #[test]
    fn test_emotion_creation() {
        let emotion = Emotion::new();
        assert_eq!(emotion.happiness(), 0.0);
        assert_eq!(emotion.sadness(), 0.0);
        assert_eq!(emotion.arousal(), 0.0);
    }

    #[test]
    fn test_emotion_values() {
        let mut emotion = Emotion::new();
        
        emotion.set_happiness(0.8);
        assert_eq!(emotion.happiness(), 0.8);
        
        emotion.set_sadness(0.7);
        assert_eq!(emotion.sadness(), 0.7);
        
        emotion.set_arousal(0.9);
        assert_eq!(emotion.arousal(), 0.9);
    }

    #[test]
    fn test_emotion_update() {
        let mut emotion = Emotion::new();
        emotion.set_happiness(1.0);
        emotion.set_sadness(1.0);
        emotion.set_arousal(1.0);
        
        emotion.update();
        
        assert!(emotion.happiness() < 1.0);
        assert!(emotion.sadness() < 1.0);
        assert!(emotion.arousal() < 1.0);
    }

    #[test]
    fn test_concept_builder_creation() {
        let builder = ConceptBuilder::new();
        assert!(builder.emotion().is_none());
        assert!(builder.time().is_none());
    }

    #[test]
    fn test_concept_builder_init() {
        let time = Arc::new(Time::new());
        let emotion = Emotion::new();
        
        let mut builder = ConceptBuilder::new();
        builder.init(emotion, time.clone());
        
        assert!(builder.emotion().is_some());
        assert!(builder.time().is_some());
    }
    
    #[test]
    fn test_concept_builder_build() {
        let builder = ConceptBuilder::new();
        let term = Term::Atomic(crate::term::atom::Atomic::new_atom("test"));
        
        let concept = builder.build(&term, true, false);
        assert!(concept.is_some());
    }
}