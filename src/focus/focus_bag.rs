//! Focus bag implementation for NARS
//!
//! This module implements a focus bag for managing focus in NARS.

use crate::focus::Focus;
use crate::term::Term;

/// A bag of focused items
pub struct FocusBag {
    items: Vec<Focus>,
    capacity: usize,
}

impl FocusBag {
    /// Create a new focus bag
    pub fn new(capacity: usize) -> Self {
        FocusBag {
            items: Vec::with_capacity(capacity),
            capacity,
        }
    }

    /// Add a focus to the bag
    pub fn add(&mut self, focus: Focus) {
        if self.items.len() < self.capacity {
            self.items.push(focus);
        } else {
            // Placeholder for replacement logic
        }
    }

    /// Remove a focus from the bag by its term
    pub fn remove(&mut self, term: &Term) {
        self.items.retain(|f| &f.id != term);
    }

    /// Get the capacity of the bag
    pub fn capacity(&self) -> usize {
        self.capacity
    }

    /// Commit changes to the focus bag
    pub fn commit(&mut self) {
        // Placeholder for commit logic, e.g., updating priorities
    }

    /// Sample a focus from the bag by priority
    pub fn sample_by_priority(&mut self) -> Option<Focus> {
        if self.items.is_empty() {
            None
        } else {
            Some(self.items.remove(0))
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::term::Term;

    #[test]
    fn test_focus_bag_creation() {
        let focus_bag = FocusBag::new(10);
        assert_eq!(focus_bag.capacity(), 10);
        assert_eq!(focus_bag.items.len(), 0);
    }

    #[test]
    fn test_focus_bag_add() {
        let mut focus_bag = FocusBag::new(10);
        let focus = Focus::new(Term::Atomic(crate::term::atom::Atomic::new_atom("test")), None);
        focus_bag.add(focus);
        assert_eq!(focus_bag.items.len(), 1);
    }

    #[test]
    fn test_focus_bag_remove() {
        let mut focus_bag = FocusBag::new(10);
        let term = Term::Atomic(crate::term::atom::Atomic::new_atom("test"));
        let focus = Focus::new(term.clone(), None);
        focus_bag.add(focus);
        focus_bag.remove(&term);
        assert_eq!(focus_bag.items.len(), 0);
    }
}
