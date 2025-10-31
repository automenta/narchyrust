package nars.func;

import jcog.data.list.Lst;
import nars.Op;
import nars.Term;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.atom.Atomic;
import nars.term.functor.UnaryParametricBidiFunctor;
import nars.term.util.SetSectDiff;
import nars.term.util.Terms;
import nars.term.util.transform.InlineFunctor;
import nars.term.var.Variable;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

import static nars.Op.PROD;
import static nars.term.atom.Bool.Null;
import static nars.term.atom.Bool.True;

public enum SetFunc {
    ;

    public static final Functor union = new BinarySetFunctor("union") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;
        }

        @Override
        public @Nullable Term apply(Term a, Term b) {

            return Terms.union(a.op(), a.subterms(), b.subterms() );
        }

    };

    public static final Functor unionSect = new AbstractBinarySetFunctor("unionSect") {
        @Override
        public @Nullable Term apply(Term a, Term b, Subterms s) {
            return SetSectDiff.sect(a, b, true, s);
        }
    };
    public static final Functor interSect = new AbstractBinarySetFunctor("interSect") {
        @Override
        public @Nullable Term apply(Term a, Term b, Subterms s) {
            return SetSectDiff.sect(a, b, false, s);
        }
    };
    /**
     * all X which are in the first target AND not in the second target
     */
    public static final Functor differ = new BinarySetFunctor("differ") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;
        }

        @Override
        public Term apply(Term a, Term b) {
            return a instanceof Compound && b instanceof Compound ?
                    SetSectDiff.differenceSet(a.op(), (Compound)a, (Compound)b) : Null;
        }
    };



    public static final Functor intersect = new BinarySetFunctor("intersect") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;
        }

        @Override
        public Term apply(Term a, Term b) {
            return Terms.intersect(a.op(), a.subterms(), b.subterms());
        }

    };

    /**
     * sort(input, [mappingFunction=identity], output)
     * input: a compound of >1 items
     * output: a product containing the inputs, sorted according to (the most) natural ordering
     */
    public static Functor sort() {
        return new UnaryParametricBidiFunctor("sort") {

            @Override
            protected Term compute(Term x, Term param) {
                int n = x.subs();
                if (n < 2)
                    return Null; 

                if (x.hasAny(Op.Variables))
                    return null; 

                Lst<Term> l = new Lst<>(n);
                l.adding(x.subterms().arrayShared());
                Comparator<Term> cmp;
                if (param instanceof Atomic && !param.hasVars()) {
                    cmp = Comparators.naturalOrder(); //(Term::compareTo);
                    //cmp = Comparator.comparing((Term t) -> eval(t, (Atomic) param));//.thenComparing(t -> t);
                } else
                    return Null;

                l.sort(cmp);
                return PROD.the(l);
            }

            @Override
            protected Term uncompute(Evaluation e, Term x, Term param, Term y) {
                

                
                if (!y.hasVars() && x.vars() == 1) {
                    Subterms xx = x.subterms();
                    Subterms yy = y.subterms();
                    List<Term> missing = new Lst(1);
                    for (Term sy : yy) {
                        if (!xx.contains(sy)) {
                            missing.add(sy);
                        }
                    }
                    if (missing.size() == 1) {
                        Term[] xxx = xx.terms((n, xs) -> xs instanceof Variable);
                        if (xxx.length == 1)
                            return e.is(xxx[0], missing.getFirst()) ?
                                    True :
                                    Null;
                    }
                }

                

                return null;
            }
        };

    }

    abstract static class AbstractBinarySetFunctor extends Functor implements InlineFunctor<Evaluation> {

        protected AbstractBinarySetFunctor(String id) {
            super(id);
        }

        public boolean validOp(Op o) {
            return true;
        }

        @Deprecated
        @Override
        public final @Nullable Term apply(Evaluation e, Subterms x) {
            return applyInline(x);
        }

        @Override
        public @Nullable Term applyInline(Subterms x) {

            Term a = x.sub(0);
            if (!validOp(a.op()))
                return Null;

            Term b = x.sub(1);
            if (!validOp(b.op()))
                return Null;

            return apply(a, b, x);
        }

        protected abstract Term apply(Term a, Term b, Subterms x);

    }

    abstract static class BinarySetFunctor extends AbstractBinarySetFunctor {


        protected BinarySetFunctor( String id) {
            super(id);
        }


        @Override
        public final @Nullable Term applyInline(Subterms x) {
            if (x.subs() != 2)
                throw new UnsupportedOperationException("# args must equal 2");
            return super.applyInline(x);
        }

        protected final Term apply(Term a, Term b, Subterms x) {
            return apply(a, b);
        }

        public abstract @Nullable Term apply(Term a, Term b);
    }


}