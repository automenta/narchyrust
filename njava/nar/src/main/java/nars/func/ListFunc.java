package nars.func;

import jcog.TODO;
import nars.Op;
import nars.Term;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.atom.Int;
import nars.term.functor.BinaryFunctor;
import nars.term.functor.UnaryBidiFunctor;
import nars.term.util.Terms;
import nars.time.Tense;

import static nars.Op.PROD;
import static nars.term.atom.Bool.Null;

/**
 * Prolog contains its own library of list programs, such as:
 * append(X, Y, Z)
 * appending
 * Y
 *  onto
 * X
 * gives
 * Z
 * reverse(X, Y)
 * reverse of
 * X
 *  is
 * Y
 * length(X, N)
 * length of
 * X
 *  is
 * N
 * member(U, X)
 * U
 *  is in
 * X
 * non_member
 * (U, X)
 * U
 *  is not in
 * X
 * sort(X, Y
 * )
 * sorting
 * X
 *  gives
 * Y
 */
public enum ListFunc {
    ;

    /**
     * emulates prolog append/3
     */
    public static final Functor append = new BinaryFunctor("append") {

        @Override
        protected Term compute(Evaluation e, Term x, Term y) {
            Term[] xx = x.PROD() ? x.subterms().arrayShared() : new Term[]{x};
            if (xx.length == 0) return y;
            Term[] yy = y.PROD() ? y.subterms().arrayShared() : new Term[]{y};
            if (yy.length == 0) return x;
            return PROD.the(Terms.concat(xx, yy));
        }

    };


    public static final Functor reverse = new UnaryBidiFunctor("reverse") {

        @Override
        protected Term compute(Term x) {
            Op o = x.op();
            switch (o) {
                case PROD -> {
                    if (x.subs() > 1)
                        return PROD.the(x.subterms().reverse());
                }
                case INH, IMPL -> {
                    return o.the(x.dt(), x.subterms().reverse());
                }
                case CONJ -> {
                    int dt = x.dt();
                    if (!Tense.parallel(dt))
                        return ((Compound) x).dt(-dt);
                }
            }
            return null;
        }

        @Override
        protected Term uncompute(Term x, Term y) {
            return compute(y);
        }
    };

    public static final Functor sub = Functor.f2("sub",
            (x, n) -> n.INT() ? x.sub(Int.i(n), Null) : null);

    public static final Functor subs = Functor.f2Or3("subs", args -> {
        if (args.subs() == 2) {
            Term n = args.sub(1);
            if (n.INT()) {
                int nn = Int.i(n);
                Subterms xx = args.sub(0).subterms();
                int m = xx.subs();
                return nn < m ? PROD.the(xx.subRangeReplace(nn, m, null, null)) : Null;
            } else {
                return null;
            }
        } else {
            throw new TODO();
        }
    });
}