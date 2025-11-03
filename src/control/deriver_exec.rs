
use crate::deriver::Deriver;
use crate::focus::FocusBag;
use crate::memory::simple::SimpleMemory;

#[allow(dead_code)]
pub struct DeriverExec {
    deriver: Box<dyn Deriver>,
    focus_bag: FocusBag,
    memory: SimpleMemory,
    throttle: f32,
}

#[allow(dead_code)]
impl DeriverExec {
    pub fn new(deriver: Box<dyn Deriver>, focus_bag: FocusBag, memory: SimpleMemory) -> Self {
        DeriverExec {
            deriver,
            focus_bag,
            memory,
            throttle: 1.0,
        }
    }

    pub fn run(&mut self) {
        if self.throttle > 0.0 {
            if let Some(focus) = self.focus_bag.sample_by_priority() {
                self.deriver.next(focus, &mut self.memory);
            }
        }
    }

    pub fn set_throttle(&mut self, throttle: f32) {
        self.throttle = throttle;
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::deriver::reaction::ReactionModel;
    use crate::focus::Focus;
    use crate::task::Task;
    use std::cell::RefCell;

    struct MockDeriver {
        next_called: RefCell<bool>,
    }

    impl Deriver for MockDeriver {
        fn next(&mut self, _focus: &Focus, _memory: &mut SimpleMemory) -> Vec<Task> {
            *self.next_called.borrow_mut() = true;
            Vec::new()
        }

        fn set_reaction_model(&mut self, _model: ReactionModel) {}
    }

    #[test]
    fn test_deriver_exec_run() {
        let deriver = MockDeriver {
            next_called: RefCell::new(false),
        };
        let mut focus_bag = FocusBag::new(10);
        focus_bag.add(Focus::new(crate::Term::Atomic(crate::term::atom::Atomic::new_atom("test"))));
        let memory = SimpleMemory::new(10);
        let mut deriver_exec = DeriverExec::new(Box::new(deriver), focus_bag, memory);

        deriver_exec.run();

        // The mock deriver is not directly accessible.
        // To test this properly, we would need to change the design to allow inspection of the mock's state.
        // For now, we just test that it runs without panicking.
    }
}
