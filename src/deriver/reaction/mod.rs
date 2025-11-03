
pub mod compiler;

use crate::deriver::Deriver;

#[allow(dead_code)]
pub struct Reaction {}

#[allow(dead_code)]
pub struct Reactions {
    reactions: Vec<Reaction>,
}

#[allow(dead_code)]
pub struct ReactionModel {}

impl Reactions {
    #[allow(dead_code)]
    pub fn new() -> Self {
        Reactions {
            reactions: Vec::new(),
        }
    }

    #[allow(dead_code)]
    pub fn add(&mut self, reaction: Reaction) {
        self.reactions.push(reaction);
    }

    #[allow(dead_code)]
    pub fn compile(&self) -> ReactionModel {
        let compiler = compiler::DecisionTreeReactionCompiler::new();
        compiler.compile(self)
    }
}
