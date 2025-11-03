//! Narsese parser implementation
//!
//! This module provides parsing capabilities for Narsese syntax,
//! the logical language used in NARS (Non-Axiomatic Reasoner).

use pest::Parser;
use pest_derive::Parser;
use crate::{Term, Truth, task::{Task, Punctuation, Time, Budget}};
use crate::term::{atom::Atomic, compound::Compound, Op};
use std::fs;

#[derive(Parser)]
#[grammar = "src/parser/narsese.pest"]
pub struct NarseseParser;

use std::env;
use std::path::PathBuf;

pub fn load_nal_files() -> Result<Vec<Task>, Box<dyn std::error::Error>> {
    let mut tasks = Vec::new();
    let manifest_dir = env::var("CARGO_MANIFEST_DIR")?;
    let mut resources_path = PathBuf::from(manifest_dir);
    resources_path.push("resources");

    let paths = fs::read_dir(resources_path)?;

    for path in paths {
        let path = path?.path();
        if let Some(extension) = path.extension() {
            if extension == "nal" {
                let content = fs::read_to_string(&path)?;
                match parse_narsese(&content) {
                    Ok(mut parsed_tasks) => tasks.append(&mut parsed_tasks),
                    Err(e) => println!("Failed to parse file: {:?}\nError: {}", path, e),
                }
            }
        }
    }

    Ok(tasks)
}

pub fn parse_narsese(input: &str) -> Result<Vec<Task>, pest::error::Error<Rule>> {
    let pairs = NarseseParser::parse(Rule::input, input)?;
    let mut tasks = Vec::new();

    for pair in pairs.flatten() {
        match pair.as_rule() {
            Rule::task => {
                let mut inner_rules = pair.into_inner();
                let mut budget = None;
                let term = parse_term(inner_rules.next().unwrap());
                let punctuation = parse_punctuation(inner_rules.next().unwrap());
                let mut truth = None;

                for part in inner_rules {
                    match part.as_rule() {
                        Rule::budget => budget = Some(parse_budget(part)),
                        Rule::truth => truth = Some(parse_truth(part)),
                        Rule::label => { /* TODO: Handle labels */ }
                        _ => {}
                    }
                }

                if truth.is_none() && punctuation == Punctuation::Belief {
                    truth = Some(Truth::default_belief());
                }

                tasks.push(Task::with_auto_id(
                    term,
                    truth,
                    punctuation,
                    Time::Eternal,
                    budget.unwrap_or_default(),
                    vec![],
                    0,
                ));
            }
            Rule::inference_rule => {
                let mut inner_rules = pair.into_inner();
                let mut premises = Vec::new();
                while let Some(p) = inner_rules.peek() {
                    if p.as_rule() == Rule::term {
                        premises.push(parse_term(inner_rules.next().unwrap()));
                    } else {
                        break;
                    }
                }

                let conclusion = parse_term(inner_rules.next().unwrap());
                let mut punctuation = Punctuation::Belief;
                let mut truth = None;
                let mut budget = None;

                for part in inner_rules {
                    match part.as_rule() {
                        Rule::budget => budget = Some(parse_budget(part)),
                        Rule::truth => truth = Some(parse_truth(part)),
                        Rule::punctuation => punctuation = parse_punctuation(part),
                        Rule::label => { /* TODO: Handle labels */ }
                        _ => {}
                    }
                }

                let term = Term::Compound(Compound::new(Op::Implication, vec![Term::Compound(Compound::new(Op::Conjunction, premises)), conclusion]));

                if truth.is_none() && punctuation == Punctuation::Belief {
                    truth = Some(Truth::default_belief());
                }

                tasks.push(Task::with_auto_id(
                    term,
                    truth,
                    punctuation,
                    Time::Eternal,
                    budget.unwrap_or_default(),
                    vec![],
                    0,
                ));
            }
            _ => {}
        }
    }

    Ok(tasks)
}

fn parse_term(pair: pest::iterators::Pair<Rule>) -> Term {
    match pair.as_rule() {
        Rule::term => parse_term(pair.into_inner().next().unwrap()),
        Rule::function_call => {
            let mut inner = pair.into_inner();
            let name = parse_atomic_term(inner.next().unwrap());
            let mut terms = vec![name];
            for term_pair in inner {
                terms.push(parse_term(term_pair));
            }
            Term::Compound(Compound::new(Op::Product, terms))
        }
        Rule::statement => {
            let mut inner = pair.into_inner();
            let subj = parse_term(inner.next().unwrap());
            let pred = parse_term(inner.next().unwrap());
            Term::Compound(Compound::new(Op::Inheritance, vec![subj, pred]))
        }
        Rule::compound_term => {
            let mut inner = pair.into_inner();
            let first = parse_term(inner.next().unwrap());
            if let Some(op_pair) = inner.next() {
                let op = parse_op(op_pair);
                let mut terms = vec![first];
                for term_pair in inner {
                    if term_pair.as_rule() == Rule::term {
                        terms.push(parse_term(term_pair));
                    }
                }
                Term::Compound(Compound::new(op, terms))
            } else {
                first
            }
        }
        Rule::atomic_term => parse_atomic_term(pair.into_inner().next().unwrap()),
        Rule::variable => Term::Variable(crate::term::var::Variable::new_indep(pair.as_str())),
        _ => unreachable!(),
    }
}

fn parse_atomic_term(pair: pest::iterators::Pair<Rule>) -> Term {
    Term::Atomic(Atomic::new_atom(pair.as_str()))
}

fn parse_op(pair: pest::iterators::Pair<Rule>) -> Op {
    match pair.as_str() {
        "&&" | "&" => Op::Conjunction,
        "||" | "|" => Op::Disjunction,
        "==>" | "-->" => Op::Inheritance,
        "--" => Op::Negation,
        "|-" => Op::Implication,
        "=" => Op::Equivalence,
        "<~>" => Op::Similarity,
        _ => unreachable!(),
    }
}

fn parse_punctuation(pair: pest::iterators::Pair<Rule>) -> Punctuation {
    match pair.as_str() {
        "." => Punctuation::Belief,
        "?" => Punctuation::Question,
        "!" => Punctuation::Goal,
        "@" => Punctuation::Quest,
        _ => unreachable!(),
    }
}

fn parse_budget(pair: pest::iterators::Pair<Rule>) -> Budget {
    let mut inner = pair.into_inner();
    let priority = inner.next().unwrap().as_str().parse().unwrap();
    let durability = inner.next().map(|p| p.as_str().parse().unwrap()).unwrap_or(0.5);
    let quality = inner.next().map(|p| p.as_str().parse().unwrap()).unwrap_or(0.5);
    Budget::new(priority, durability, quality)
}

fn parse_truth(pair: pest::iterators::Pair<Rule>) -> Truth {
    let mut inner = pair.into_inner();
    let f = inner.next().unwrap().as_str().parse().unwrap();
    let c = inner.next().map(|p| p.as_str().parse().unwrap()).unwrap_or(0.9);
    Truth::new(f, c)
}


#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_simple_atomic_belief() {
        let result = parse_narsese("cat.");
        assert!(result.is_ok());
        let tasks = result.unwrap();
        assert_eq!(tasks.len(), 1);
        let task = &tasks[0];
        assert_eq!(task.term().to_string(), "cat");
        assert_eq!(task.punctuation(), Punctuation::Belief);
        assert!(task.truth().is_some());
        let truth = task.truth().unwrap();
        assert_eq!(truth.frequency(), 1.0);
        assert_eq!(truth.confidence(), 0.9);
    }

    #[test]
    fn test_parse_atomic_belief_with_truth() {
        let result = parse_narsese("cat. %0.9;0.8%");
        assert!(result.is_ok());
        let tasks = result.unwrap();
        assert_eq!(tasks.len(), 1);
        let task = &tasks[0];
        assert_eq!(task.term().to_string(), "cat");
        assert_eq!(task.punctuation(), Punctuation::Belief);
        assert!(task.truth().is_some());
        let truth = task.truth().unwrap();
        assert_eq!(truth.frequency(), 0.9);
        assert_eq!(truth.confidence(), 0.8);
    }

    #[test]
    fn test_parse_compound_term() {
        let result = parse_narsese("(<a --> b> && <c --> d>).");
        assert!(result.is_ok());
        let tasks = result.unwrap();
        assert_eq!(tasks.len(), 1);
        let task = &tasks[0];
        assert_eq!(task.term().to_string(), "((a --> b) && (c --> d))");
        assert_eq!(task.punctuation(), Punctuation::Belief);
    }

    #[test]
    fn test_parse_question() {
        let result = parse_narsese("cat?");
        assert!(result.is_ok());
        let tasks = result.unwrap();
        assert_eq!(tasks.len(), 1);
        let task = &tasks[0];
        assert_eq!(task.term().to_string(), "cat");
        assert_eq!(task.punctuation(), Punctuation::Question);
    }

    #[test]
    fn test_parse_invalid_punctuation() {
        let result = parse_narsese("cat^");
        assert!(result.is_err());
    }

    #[test]
    fn test_parse_inheritance_term() {
        let result = parse_narsese("<bird --> flyer>.");
        assert!(result.is_ok());
        let tasks = result.unwrap();
        assert_eq!(tasks.len(), 1);
        let task = &tasks[0];
        assert_eq!(task.term().to_string(), "(bird --> flyer)");
        assert_eq!(task.punctuation(), Punctuation::Belief);
    }

    #[test]
    fn test_parse_temporal_specifications() {
        let result = parse_narsese("event. :/now:\\");
        assert!(result.is_ok());
    }

}
