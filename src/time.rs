//! Time management for NARS
//!
//! This module handles temporal aspects of NARS including time tracking,
//! temporal reasoning, and time-related operations.

use std::sync::atomic::{AtomicI64, Ordering};

/// Time management system for NARS
pub struct Time {
    /// Current time
    now: AtomicI64,
    
    /// Duration of each cycle
    duration: f32,
    
    /// Next stamp value
    next_stamp: AtomicI64,
}

impl Time {
    /// Create a new Time instance
    pub fn new() -> Self {
        Time {
            now: AtomicI64::new(0),
            duration: 1.0,
            next_stamp: AtomicI64::new(1),
        }
    }
    
    /// Get the current time
    pub fn now(&self) -> i64 {
        self.now.load(Ordering::Relaxed)
    }
    
    /// Get the duration of each cycle
    pub fn dur(&self) -> f32 {
        self.duration
    }
    
    /// Advance the time by one step and return the new time
    pub fn next(&self) -> i64 {
        self.now.fetch_add(1, Ordering::Relaxed) + 1
    }
    
    /// Advance the time by one step and return the new stamp
    pub fn next_stamp(&self) -> i64 {
        self.next_stamp.fetch_add(1, Ordering::Relaxed)
    }
    
    /// Reset the time to 0
    pub fn reset(&self) {
        self.now.store(0, Ordering::Relaxed);
        self.next_stamp.store(1, Ordering::Relaxed);
    }
    
    /// Get relative occurrence time for a tense
    pub fn relative_occurrence(&self, tense: i64) -> i64 {
        match tense {
            t if t >= 0 => t, // Absolute time
            -1 => self.now.load(Ordering::Relaxed), // Eternal
            _ => self.now.load(Ordering::Relaxed) + tense, // Relative
        }
    }
}

impl Default for Time {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_time_creation() {
        let time = Time::new();
        assert_eq!(time.now(), 0);
        assert_eq!(time.dur(), 1.0);
    }

    #[test]
    fn test_time_next() {
        let time = Time::new();
        assert_eq!(time.next(), 1);
        assert_eq!(time.now(), 1);
        assert_eq!(time.next(), 2);
        assert_eq!(time.now(), 2);
    }

    #[test]
    fn test_time_reset() {
        let time = Time::new();
        time.next(); // Advance to 1
        assert_eq!(time.now(), 1);
        
        time.reset();
        assert_eq!(time.now(), 0);
    }

    #[test]
    fn test_next_stamp() {
        let time = Time::new();
        let stamp1 = time.next_stamp();
        let stamp2 = time.next_stamp();
        
        assert_eq!(stamp1, 1);
        assert_eq!(stamp2, 2);
    }
    
    #[test]
    fn test_relative_occurrence() {
        let time = Time::new();
        time.now.store(10, Ordering::Relaxed);
        
        // Absolute time
        assert_eq!(time.relative_occurrence(5), 5);
        
        // Eternal time
        assert_eq!(time.relative_occurrence(-1), 10);
        
        // Relative time
        assert_eq!(time.relative_occurrence(-2), 8);
        assert_eq!(time.relative_occurrence(2), 2);
    }
}