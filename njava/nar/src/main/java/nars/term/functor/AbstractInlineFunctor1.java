package nars.term.functor;

import nars.Term;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import nars.term.util.transform.InstantFunctor;

import java.util.function.UnaryOperator;

public abstract class AbstractInlineFunctor1 extends AbstractInlineFunctor {

    protected AbstractInlineFunctor1(Atom atom) {
        super(atom);
    }

    protected AbstractInlineFunctor1(String atom) {
        this(atom(atom));
    }

    protected abstract Term apply1(Term arg);

    @Override
    public final Term applyInline(Subterms args) {
        return args.subs() == 1 ? apply1(args.sub(0)) : Bool.Null;
    }

    @Override
    public final Term apply(Evaluation e, Subterms args) {
        return applyInline(args);
    }

    public static class MyAbstractInlineFunctor1Inline extends AbstractInlineFunctor1 {
        final UnaryOperator<Term> ff;

        public MyAbstractInlineFunctor1Inline(String termAtom, UnaryOperator<Term> ff) {
            super(termAtom);
            this.ff = ff;
        }

        @Override
        public final Term apply1(Term arg) {
            return ff.apply(arg);
        }

    }

    public abstract static class AbstractInstantFunctor1 extends AbstractInlineFunctor1 implements InstantFunctor<Evaluation> {

        protected AbstractInstantFunctor1(String termAtom) {
            super(termAtom);
        }
    }
}