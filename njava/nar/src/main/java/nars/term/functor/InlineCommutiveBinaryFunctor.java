package nars.term.functor;

import nars.eval.Evaluation;
import nars.term.util.transform.InlineFunctor;

public abstract class InlineCommutiveBinaryFunctor extends CommutiveBinaryFunctor implements InlineFunctor<Evaluation> {

    protected InlineCommutiveBinaryFunctor(String name) {
        super(name);
    }

}
