//! Priority tree implementation for NARS
//!
//! This module implements a priority tree for managing priorities in NARS.

use petgraph::graph::{DiGraph, NodeIndex};
use std::collections::HashMap;
use crate::term::Term;
use petgraph::visit::Bfs;

/// A node in the priority tree
#[derive(Debug, Clone, PartialEq)]
pub struct PriNode {
    /// The term associated with this node
    pub term: Term,
    /// The priority of this node
    pub priority: f32,
}

impl Eq for PriNode {}

impl std::hash::Hash for PriNode {
    fn hash<H: std::hash::Hasher>(&self, state: &mut H) {
        self.term.hash(state);
    }
}

/// Priority tree for managing priorities
pub struct PriTree {
    /// The graph representing the priority tree
    graph: DiGraph<PriNode, ()>,
    
    /// A map from the node to its index in the graph
    node_map: HashMap<Term, NodeIndex>,
}

impl PriTree {
    /// Create a new priority tree
    pub fn new() -> Self {
        PriTree {
            graph: DiGraph::new(),
            node_map: HashMap::new(),
        }
    }

    /// Add a node to the tree
    pub fn add_node(&mut self, node: PriNode) -> NodeIndex {
        if let Some(&index) = self.node_map.get(&node.term) {
            index
        } else {
            let index = self.graph.add_node(node.clone());
            self.node_map.insert(node.term.clone(), index);
            index
        }
    }

    /// Add an edge to the tree
    pub fn add_edge(&mut self, source: PriNode, target: PriNode) {
        let source_index = self.add_node(source);
        let target_index = self.add_node(target);
        self.graph.add_edge(source_index, target_index, ());
    }
    
    /// Commit priority changes
    pub fn commit(&mut self) {
        if self.graph.node_count() == 0 {
            return;
        }
        // Create a breadth-first search starting from the first node
        let mut bfs = Bfs::new(&self.graph, *self.node_map.values().next().unwrap());
        while let Some(nx) = bfs.next(&self.graph) {
            // Placeholder for priority update logic
            let _node = &self.graph[nx];
        }
    }
    
    /// Clear the tree
    pub fn clear(&mut self) {
        self.graph.clear();
        self.node_map.clear();
    }
}

impl Default for PriTree {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::term::atom::Atomic;

    #[test]
    fn test_pri_tree_creation() {
        let pri_tree = PriTree::new();
        assert_eq!(pri_tree.graph.node_count(), 0);
    }

    #[test]
    fn test_add_node() {
        let mut pri_tree = PriTree::new();
        let term = Term::Atomic(Atomic::new_atom("test"));
        let node = PriNode { term, priority: 0.5 };
        pri_tree.add_node(node);
        assert_eq!(pri_tree.graph.node_count(), 1);
    }

    #[test]
    fn test_add_edge() {
        let mut pri_tree = PriTree::new();
        let term1 = Term::Atomic(Atomic::new_atom("test1"));
        let node1 = PriNode { term: term1, priority: 0.5 };
        let term2 = Term::Atomic(Atomic::new_atom("test2"));
        let node2 = PriNode { term: term2, priority: 0.5 };
        pri_tree.add_edge(node1, node2);
        assert_eq!(pri_tree.graph.edge_count(), 1);
    }

    #[test]
    fn test_clear() {
        let mut pri_tree = PriTree::new();
        let term1 = Term::Atomic(Atomic::new_atom("test1"));
        let node1 = PriNode { term: term1, priority: 0.5 };
        let term2 = Term::Atomic(Atomic::new_atom("test2"));
        let node2 = PriNode { term: term2, priority: 0.5 };
        pri_tree.add_edge(node1, node2);
        pri_tree.clear();
        assert_eq!(pri_tree.graph.node_count(), 0);
        assert_eq!(pri_tree.graph.edge_count(), 0);
    }
}