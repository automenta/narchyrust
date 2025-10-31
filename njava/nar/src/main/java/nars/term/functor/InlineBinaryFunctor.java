package nars.term.functor;

import nars.eval.Evaluation;
import nars.term.util.transform.InlineFunctor;

public abstract class InlineBinaryFunctor extends BinaryFunctor implements InlineFunctor<Evaluation> {

    protected InlineBinaryFunctor(String name) {
        super(name);
    }
}
