//! A Bag data structure for managing collections of items with priorities.

use std::collections::BinaryHeap;
use std::cmp::Ordering;

/// A trait for items that can be stored in a Bag.
pub trait BagItem: Clone {
    /// Returns the priority of the item.
    fn priority(&self) -> f32;
}

#[derive(Debug)]
struct HeapItem<T: BagItem>(T);

impl<T: BagItem> PartialEq for HeapItem<T> {
    fn eq(&self, other: &Self) -> bool {
        self.0.priority() == other.0.priority()
    }
}

impl<T: BagItem> Eq for HeapItem<T> {}

impl<T: BagItem> PartialOrd for HeapItem<T> {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        other.0.priority().partial_cmp(&self.0.priority())
    }
}

impl<T: BagItem> Ord for HeapItem<T> {
    fn cmp(&self, other: &Self) -> Ordering {
        self.partial_cmp(other).unwrap()
    }
}

/// A Bag data structure for managing collections of items with priorities.
#[derive(Debug)]
pub struct Bag<T: BagItem> {
    items: BinaryHeap<HeapItem<T>>,
    capacity: usize,
}

impl<T: BagItem> Bag<T> {
    /// Creates a new Bag with the given capacity.
    pub fn new(capacity: usize) -> Self {
        Bag {
            items: BinaryHeap::with_capacity(capacity),
            capacity,
        }
    }

    /// Adds an item to the Bag.
    pub fn add(&mut self, item: T) {
        if self.items.len() < self.capacity {
            self.items.push(HeapItem(item));
        } else if item.priority() > self.items.peek().unwrap().0.priority() {
            self.items.pop();
            self.items.push(HeapItem(item));
        }
    }

    /// Takes an item from the Bag.
    pub fn take(&mut self) -> Option<T> {
        self.items.pop().map(|item| item.0)
    }

    /// Returns the number of items in the Bag.
    pub fn len(&self) -> usize {
        self.items.len()
    }

    /// Returns true if the Bag is empty.
    pub fn is_empty(&self) -> bool {
        self.items.is_empty()
    }
}
