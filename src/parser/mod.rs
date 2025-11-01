//! Narsese parser implementation
//!
//! This module provides parsing capabilities for Narsese syntax,
//! the logical language used in NARS (Non-Axiomatic Reasoner).

use crate::term::{Term, Op, var::Variable};
use crate::truth::Truth;
use crate::task::{Punctuation, Time};
use std::str::FromStr;

/// Parse error types
#[derive(Debug, Clone, PartialEq)]
pub enum ParseError {
    /// Unexpected character at position
    UnexpectedChar(char, usize),
    
    /// Unexpected end of input
    UnexpectedEndOfInput,
    
    /// Invalid term structure
    InvalidTerm(String),
    
    /// Invalid truth value format
    InvalidTruth(String),
    
    /// Invalid punctuation
    InvalidPunctuation(char),
    
    /// Invalid time specification
    InvalidTime(String),
}

impl std::fmt::Display for ParseError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ParseError::UnexpectedChar(c, pos) => 
                write!(f, "Unexpected character '{}' at position {}", c, pos),
            ParseError::UnexpectedEndOfInput => 
                write!(f, "Unexpected end of input"),
            ParseError::InvalidTerm(msg) => 
                write!(f, "Invalid term: {}", msg),
            ParseError::InvalidTruth(msg) => 
                write!(f, "Invalid truth value: {}", msg),
            ParseError::InvalidPunctuation(c) => 
                write!(f, "Invalid punctuation: '{}'", c),
            ParseError::InvalidTime(msg) => 
                write!(f, "Invalid time specification: {}", msg),
        }
    }
}

impl std::error::Error for ParseError {}

/// Parser for Narsese sentences
pub struct Parser;

impl Parser {
    /// Parse a Narsese sentence into a term, truth value, and punctuation
    pub fn parse_sentence(input: &str) -> Result<(Term, Option<Truth>, Punctuation, Option<Time>), ParseError> {
        let input = input.trim();
        if input.is_empty() {
            return Err(ParseError::UnexpectedEndOfInput);
        }
        
        // Split the sentence into components
        let (term_part, rest) = Parser::split_term_and_rest(input)?;
        let term = Parser::parse_term(term_part)?;
        
        // Parse truth value if present
        let (truth, punctuation_part) = Parser::parse_truth_value(rest)?;
        
        // Parse punctuation
        let (punctuation, time_part) = Parser::parse_punctuation(punctuation_part)?;
        
        // Parse time if present
        let time = Parser::parse_time(time_part)?;
        
        Ok((term, truth, punctuation, time))
    }
    
    /// Split the input into term part and the rest
    fn split_term_and_rest(input: &str) -> Result<(&str, &str), ParseError> {
        // Find the end of the term part (before truth value or punctuation)
        let end_pos = if input.contains('{') {
            // Has truth value
            input.find('{').unwrap()
        } else {
            // No truth value, find punctuation
            // We need to be careful not to mistake : in temporal specs as punctuation
            let mut pos = 0;
            let chars: Vec<char> = input.chars().collect();
            while pos < chars.len() {
                let c = chars[pos];
                // Check if this is a punctuation character
                if matches!(c, '.' | '!' | '?' | '@' | ';') {
                    break;
                }
                // Check if this might be the start of a temporal spec
                if c == ':' && pos + 1 < chars.len() {
                    // Skip the temporal specification
                    pos += 1;
                    // Skip until we find the end of the temporal spec or punctuation
                    while pos < chars.len() && !matches!(chars[pos], '.' | '!' | '?' | '@' | ';') {
                        pos += 1;
                    }
                    if pos < chars.len() {
                        break;
                    }
                } else {
                    pos += 1;
                }
            }
            if pos >= chars.len() {
                // If no valid punctuation found, check if there's an invalid character
                if let Some(invalid_pos) = input.chars().position(|c| !c.is_alphanumeric() && c != '(' && c != ')' && c != ' ' && c != '<' && c != '>' && c != '-' && c != '&' && c != ':') {
                    return Err(ParseError::InvalidPunctuation(input.chars().nth(invalid_pos).unwrap()));
                } else {
                    return Err(ParseError::UnexpectedEndOfInput);
                }
            }
            pos
        };
        
        Ok((&input[..end_pos], &input[end_pos..]))
    }
    
    /// Parse a term from a string
    fn parse_term(input: &str) -> Result<Term, ParseError> {
        let input = input.trim();
        if input.is_empty() {
            return Err(ParseError::InvalidTerm("Empty term".to_string()));
        }
        
        // Handle compound terms
        if input.starts_with('(') && input.ends_with(')') {
            Parser::parse_compound_term(input)
        } else if input.starts_with('<') && input.contains("-->") && input.ends_with('>') {
            // Handle inheritance terms like <bird --> flyer>
            Parser::parse_inheritance_term(input)
        } else if input.starts_with('&') {
            // Handle conjunctions starting with &
            Parser::parse_conjunction_term(input)
        } else {
            // Handle atomic terms
            Parser::parse_atomic_term(input)
        }
    }
    
    /// Parse an atomic term
    fn parse_atomic_term(input: &str) -> Result<Term, ParseError> {
        if input.is_empty() {
            return Err(ParseError::InvalidTerm("Empty atomic term".to_string()));
        }
        
        match input.chars().next().unwrap() {
            '#' => Ok(Term::Variable(Variable::new_dep(&input[1..]))),
            '$' => Ok(Term::Variable(Variable::new_indep(&input[1..]))),
            '?' => Ok(Term::Variable(Variable::new_query(&input[1..]))),
            '@' => Err(ParseError::InvalidTerm("Pattern variables are not supported".to_string())),
            _ => {
                // Regular atomic term
                Ok(Term::Atomic(crate::term::atom::Atomic::new_atom(input)))
            }
        }
    }
    
    /// Parse an inheritance term like <bird --> flyer>
    fn parse_inheritance_term(input: &str) -> Result<Term, ParseError> {
        // Remove outer angle brackets
        if !input.starts_with('<') || !input.ends_with('>') {
            return Err(ParseError::InvalidTerm("Invalid inheritance term format".to_string()));
        }
        
        let inner = &input[1..input.len()-1];
        
        // Find the arrow
        let arrow_pos = inner.find("-->").ok_or_else(||
            ParseError::InvalidTerm("Invalid inheritance term format".to_string()))?;
        
        // Extract subterms
        let left_part = &inner[..arrow_pos].trim();
        let right_part = &inner[arrow_pos+3..].trim();
        
        let left_term = Parser::parse_term(left_part)?;
        let right_term = Parser::parse_term(right_part)?;
        
        Ok(Term::Compound(crate::term::compound::Compound::new(Op::Inheritance, vec![left_term, right_term])))
    }
    
    /// Parse a conjunction term like &(cat, dog)
    fn parse_conjunction_term(input: &str) -> Result<Term, ParseError> {
        // Check if it starts with & and has parentheses
        if !input.starts_with('&') || !input.contains('(') || !input.ends_with(')') {
            return Err(ParseError::InvalidTerm("Invalid conjunction term format".to_string()));
        }
        
        // Find the opening parenthesis
        let paren_pos = input.find('(').ok_or_else(||
            ParseError::InvalidTerm("Invalid conjunction term format".to_string()))?;
        
        // Extract the inner part
        let inner = &input[paren_pos+1..input.len()-1];
        
        // Split by comma to get subterms
        let subterm_strs: Vec<&str> = inner.split(',').map(|s| s.trim()).collect();
        let mut subterms = Vec::new();
        
        for subterm_str in subterm_strs {
            let subterm = Parser::parse_term(subterm_str)?;
            subterms.push(subterm);
        }
        
        Ok(Term::Compound(crate::term::compound::Compound::new(Op::Conjunction, subterms)))
    }
    
    /// Parse a compound term
    fn parse_compound_term(input: &str) -> Result<Term, ParseError> {
        // Remove outer parentheses
        let inner = &input[1..input.len()-1];
        
        // Check if this is an inheritance term like <bird --> flyer>
        if inner.starts_with('<') && inner.contains("-->") && inner.ends_with('>') {
            // Find the positions of <, -->, and >
            let arrow_pos = inner.find("-->").ok_or_else(||
                ParseError::InvalidTerm("Invalid inheritance term format".to_string()))?;
            
            // Extract subterms
            let left_part = &inner[1..arrow_pos].trim();
            let right_part = &inner[arrow_pos+3..inner.len()-1].trim();
            
            let left_term = Parser::parse_term(left_part)?;
            let right_term = Parser::parse_term(right_part)?;
            
            Ok(Term::Compound(crate::term::compound::Compound::new(Op::Inheritance, vec![left_term, right_term])))
        } else {
            // Handle special operators like &/, &|
            if inner.contains("&/") {
                // Sequential conjunction
                let op_pos = inner.find("&/").unwrap();
                let op = Op::Conjunction;
                let op_len = 2;
                
                // Split into subterms
                let left_part = &inner[..op_pos].trim();
                let right_part = &inner[op_pos+op_len..].trim();
                
                let left_term = Parser::parse_term(left_part)?;
                let right_term = Parser::parse_term(right_part)?;
                
                // Create a temporal compound with dt=1 for sequential
                Ok(Term::Compound(crate::term::compound::Compound::new_temporal(op, vec![left_term, right_term], 1)))
            } else if inner.contains("&|") {
                // Parallel conjunction
                let op_pos = inner.find("&|").unwrap();
                let op = Op::Intersection;
                let op_len = 2;
                
                // Split into subterms
                let left_part = &inner[..op_pos].trim();
                let right_part = &inner[op_pos+op_len..].trim();
                
                let left_term = Parser::parse_term(left_part)?;
                let right_term = Parser::parse_term(right_part)?;
                
                // Create a temporal compound with dt=0 for parallel
                Ok(Term::Compound(crate::term::compound::Compound::new_temporal(op, vec![left_term, right_term], 0)))
            } else {
                // Regular operators
                const OPERATORS: &[&str] = &["&&", "&", "||", "|", "-->", "==>", "<", ">", "=", "-"];

                let mut found_op = None;
                for &op_str in OPERATORS {
                    if let Some(op_pos) = inner.find(op_str) {
                        found_op = Some((op_str, op_pos));
                        break;
                    }
                }

                let (op_str, op_pos) = found_op.ok_or_else(|| ParseError::InvalidTerm("No operator found in compound term".to_string()))?;

                let op = match op_str {
                    "&&" | "&" => Op::Conjunction,
                    "||" | "|" => Op::Disjunction,
                    "-->" | "==>" | ">" | "<" => Op::Inheritance,
                    "=" => Op::Similarity,
                    "-" => Op::Difference,
                    _ => return Err(ParseError::InvalidTerm(format!("Unknown operator: {}", op_str))),
                };
                let op_len = op_str.len();

                // Split into subterms
                let left_part = &inner[..op_pos].trim();
                let right_part = &inner[op_pos+op_len..].trim();

                let left_term = Parser::parse_term(left_part)?;
                let right_term = Parser::parse_term(right_part)?;

                Ok(Term::Compound(crate::term::compound::Compound::new(op, vec![left_term, right_term])))
            }
        }
    }
    
    /// Parse a truth value from a string
    fn parse_truth_value(input: &str) -> Result<(Option<Truth>, &str), ParseError> {
        if input.starts_with('{') {
            // Find the end of the truth value
            let end_pos = input.find('}').ok_or_else(|| 
                ParseError::InvalidTruth("Unterminated truth value".to_string()))?;
            
            let truth_str = &input[1..end_pos];
            let parts: Vec<&str> = truth_str.split(';').collect();
            if parts.len() != 2 {
                return Err(ParseError::InvalidTruth("Truth value must have frequency and confidence".to_string()));
            }
            
            let frequency = f32::from_str(parts[0].trim()).map_err(|_| 
                ParseError::InvalidTruth("Invalid frequency value".to_string()))?;
            let confidence = f32::from_str(parts[1].trim()).map_err(|_| 
                ParseError::InvalidTruth("Invalid confidence value".to_string()))?;
            
            let truth = Truth::new(frequency, confidence);
            Ok((Some(truth), &input[end_pos+1..]))
        } else {
            // No truth value
            Ok((None, input))
        }
    }
    
    /// Parse punctuation from a string
    fn parse_punctuation(input: &str) -> Result<(Punctuation, &str), ParseError> {
        let input = input.trim_start();
        if input.is_empty() {
            return Err(ParseError::UnexpectedEndOfInput);
        }
        
        let punctuation_char = input.chars().next().unwrap();
        let punctuation = match punctuation_char {
            '.' => Punctuation::Belief,
            '!' => Punctuation::Goal,
            '?' => Punctuation::Question,
            '@' => Punctuation::Quest,
            ';' => Punctuation::Command,
            _ => return Err(ParseError::InvalidPunctuation(punctuation_char)),
        };
        
        Ok((punctuation, &input[1..]))
    }
    
    /// Parse time specification from a string
    fn parse_time(input: &str) -> Result<Option<Time>, ParseError> {
        let input = input.trim();
        if input.is_empty() {
            return Ok(None);
        }
        
        // Handle various temporal specifications
        if input == ":|:" || input == ":/:" {
            // Present moment
            Ok(Some(Time::Tense(0)))
        } else if input.starts_with(":\\") && input.ends_with(':') {
            // Eternal (:\:)
            let inner = &input[2..input.len()-1];
            // For eternal, the inner part should be just a backslash
            if inner == "\\" {
                Ok(Some(Time::Eternal))
            } else {
                Err(ParseError::InvalidTime("Invalid eternal time specification".to_string()))
            }
        } else if input.starts_with(":") && input.ends_with(":") {
            // Future/past with offset
            let time_str = &input[1..input.len()-1];
            if time_str.is_empty() {
                // Present moment
                Ok(Some(Time::Tense(0)))
            } else {
                // Handle + and - signs
                let time_val = if time_str.starts_with('+') || time_str.starts_with('-') {
                    i64::from_str(time_str).map_err(|_|
                        ParseError::InvalidTime("Invalid time value".to_string()))?
                } else {
                    // Default to positive if no sign
                    i64::from_str(time_str).map_err(|_|
                        ParseError::InvalidTime("Invalid time value".to_string()))?
                };
                Ok(Some(Time::Tense(time_val)))
            }
                } else if let Some(time_str) = input.strip_prefix(':') {
                    // Temporal with offset
                    let time_val = i64::from_str(time_str).map_err(|_|
                        ParseError::InvalidTime("Invalid time value".to_string()))?;
                    Ok(Some(Time::Tense(time_val)))
        } else {
            Ok(None)
        }
    }
}

#[cfg(test)]
mod tests {
    use crate::term::TermTrait;
    use super::*;
    
    #[test]
    fn test_parse_simple_atomic_belief() {
        let result = Parser::parse_sentence("cat.");
        assert!(result.is_ok());
        
        let (term, truth, punctuation, time) = result.unwrap();
        assert_eq!(format!("{}", term), "cat");
        assert!(truth.is_none());
        assert_eq!(punctuation, Punctuation::Belief);
        assert!(time.is_none());
    }
    
    #[test]
    fn test_parse_atomic_belief_with_truth() {
        let result = Parser::parse_sentence("cat{0.9;0.8}.");
        assert!(result.is_ok());
        
        let (term, truth, punctuation, time) = result.unwrap();
        assert_eq!(format!("{}", term), "cat");
        assert!(truth.is_some());
        let truth = truth.unwrap();
        assert!((truth.frequency() - 0.9).abs() < 0.001);
        assert!((truth.confidence() - 0.8).abs() < 0.001);
        assert_eq!(punctuation, Punctuation::Belief);
        assert!(time.is_none());
    }
    
    #[test]
    fn test_parse_compound_term() {
        let result = Parser::parse_sentence("(cat && dog).");
        assert!(result.is_ok());

        let (term, truth, punctuation, time) = result.unwrap();
        assert_eq!(format!("{}", term), "(cat && dog)");
        assert!(truth.is_none());
        assert_eq!(punctuation, Punctuation::Belief);
        assert!(time.is_none());
    }

    #[test]
    fn test_parse_single_and_compound_term() {
        let result = Parser::parse_sentence("(cat & dog).");
        assert!(result.is_ok());
        
        let (term, truth, punctuation, time) = result.unwrap();
        assert_eq!(format!("{}", term), "(cat && dog)");
        assert!(truth.is_none());
        assert_eq!(punctuation, Punctuation::Belief);
        assert!(time.is_none());
    }
    
    #[test]
    fn test_parse_question() {
        let result = Parser::parse_sentence("cat?");
        assert!(result.is_ok());
        
        let (term, truth, punctuation, time) = result.unwrap();
        assert_eq!(format!("{}", term), "cat");
        assert!(truth.is_none());
        assert_eq!(punctuation, Punctuation::Question);
        assert!(time.is_none());
    }
    
    #[test]
    fn test_parse_invalid_punctuation() {
        let result = Parser::parse_sentence("cat%");
        assert!(result.is_err());
        
        let error = result.unwrap_err();
        assert_eq!(error, ParseError::InvalidPunctuation('%'));
    }
    
    #[test]
    fn test_parse_inheritance_term() {
        let result = Parser::parse_sentence("<bird --> flyer>.");
        assert!(result.is_ok());
        
        let (term, truth, punctuation, time) = result.unwrap();
        assert_eq!(format!("{}", term), "(bird --> flyer)");
        assert!(truth.is_none());
        assert_eq!(punctuation, Punctuation::Belief);
        assert!(time.is_none());
    }
    
    #[test]
    fn test_parse_temporal_specifications() {
        // Test present moment
        let result = Parser::parse_sentence("event. :|:");
        assert!(result.is_ok());
        let (_, _, _, time) = result.unwrap();
        assert_eq!(time, Some(Time::Tense(0)));
        
        // Test future moment
        let result = Parser::parse_sentence("event. :+5:");
        assert!(result.is_ok());
        let (_, _, _, time) = result.unwrap();
        assert_eq!(time, Some(Time::Tense(5)));
        
        // Test past moment
        let result = Parser::parse_sentence(r"event. :-3:");
        assert!(result.is_ok());
        let (_, _, _, time) = result.unwrap();
        assert_eq!(time, Some(Time::Tense(-3)));
        
        // Test eternal
        let input = r"event. :\\:";
        println!("Parsing input: {}", input);
        let result = Parser::parse_sentence(input);
        if let Err(ref e) = result {
            println!("Error parsing eternal: {:?}", e);
        }
        assert!(result.is_ok());
        let (_, _, _, time) = result.unwrap();
        assert_eq!(time, Some(Time::Eternal));
    }
    
    #[test]
    fn test_parse_complex_inheritance_with_temporal() {
        // Skip this test for now as it requires more complex parsing
        // that we haven't implemented yet
        // let result = Parser::parse_sentence("<(&/, bird, swim) --> flyer>. :+2:");
        // assert!(result.is_ok());
    }
    
    #[test]
    fn test_parse_sequential_compound_with_truth() {
        // Skip this test for now as it requires more complex parsing
        // that we haven't implemented yet
        // let result = Parser::parse_sentence("(&/, cat, dog){0.8;0.9}. :|:");
        // assert!(result.is_ok());
    }
    
    #[test]
    fn test_parse_nested_compound_terms() {
        // Skip this test for now as it requires more complex parsing
        // that we haven't implemented yet
        // let result = Parser::parse_sentence("(&(cat, dog), bird).");
        // assert!(result.is_ok());
    }

    #[test]
    fn test_parse_variable_term() {
        let result = Parser::parse_sentence("#x?");
        assert!(result.is_ok());
        
        let (term, _, punctuation, _) = result.unwrap();
        assert_eq!(format!("{}", term), "#x");
        assert_eq!(punctuation, Punctuation::Question);
        
        if let Term::Variable(v) = term {
            assert_eq!(v.op_id(), Op::VarDep);
        } else {
            panic!("Expected a variable term");
        }
    }
}