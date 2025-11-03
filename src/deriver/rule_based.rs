//! Rule-based deriver implementation

use crate::deriver::Deriver;
use crate::task::Task;
use crate::focus::{FocusBag, PriTree};
use crate::memory::simple::SimpleMemory;

use crate::deriver::rule::{Rule, RuleTree};
use crate::parser;
use crate::truth::Truth;
use crate::Term;
use crate::term::Op;
use crate::deriver::brute_force::BruteForceDeriver;
use crate::deriver::reaction::ReactionModel;
use crate::term::compound::Compound;
use crate::term::TermTrait;

/// A deriver that uses the rule tree to perform inference.
pub struct RuleDeriver {
    rule_tree: RuleTree,
    brute_force_deriver: BruteForceDeriver,
    reaction_model: Option<ReactionModel>,
}

impl RuleDeriver {
    pub fn new() -> Self {
        let mut rule_tree = RuleTree::new();
        match parser::load_nal_files() {
            Ok(rules) => {
                println!("Loaded {} rules", rules.len());
                for task in rules {
                    if let Some(truth) = task.truth() {
                        let rule = Rule {
                            term: task.term().clone(),
                            truth: truth.clone(),
                        };
                        rule_tree.add(rule);
                    }
                }
            },
            Err(e) => {
                eprintln!("Failed to load NAL files: {}", e);
            }
        }
        Self {
            rule_tree,
            brute_force_deriver: BruteForceDeriver::new(),
            reaction_model: None,
        }
    }
}

use crate::focus::Focus;

impl Deriver for RuleDeriver {
    /// Derives new tasks from the current state of the NAR.
    fn next(&mut self, _focus: &Focus, memory: &mut SimpleMemory) -> Vec<Task> {
        let mut new_tasks = self.brute_force_deriver.derive_syllogism(memory);

        let concepts = memory.concepts().cloned().collect::<Vec<_>>();

        for rule in &self.rule_tree.rules {
            if let Term::Compound(rule_compound) = &rule.term {
                if rule_compound.op_id() == Op::Implication {
                    let premises = &rule_compound.subterms()[0];
                    if let Term::Compound(premises_compound) = premises {
                        if premises_compound.op_id() == Op::Conjunction {
                            let first_premise = &premises_compound.subterms()[0];

                            for concept in &concepts {
                                if let Some(belief) = concept.beliefs().highest_priority() {
                                    if belief.term().match_term(first_premise) {
                                        // TODO: a lot more work here
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        new_tasks
    }

    fn set_reaction_model(&mut self, model: ReactionModel) {
        self.reaction_model = Some(model);
    }
}