package nars.func;

import jcog.Util;
import nars.$;
import nars.Term;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.atom.Int;
import nars.term.functor.InlineBinaryFunctor;
import nars.term.functor.InverseFunctor;
import org.jetbrains.annotations.Nullable;

import static nars.Op.EQ;

/**
 * general purpose comparator: cmp(x, y, x.compareTo(y))
 */
public class Cmp extends InlineBinaryFunctor implements InverseFunctor {

    public static final Functor cmp = new Cmp();

    private Cmp() {
        super("cmp");
    }

    @Override
    protected Term compute(Evaluation e, Term x, Term y) {
        int xy = x.compareTo(y);
        if (xy == 0)
            return Int.ZERO;
        else {
            return !x.hasVars() && !y.hasVars() ? Int.i(clamp(xy)) : null;
        }
    }

    /** normalizes comparator value to -1, 0, +1 in case it's out of range */
    private static int clamp(int c) {
        return Util.clamp(c, -1, +1);
    }

    @Override
    public @Nullable Term equality(Evaluation e, Compound x, Term y) {
        if (y instanceof Int I) {
            Subterms xx = args(x, 2);
            if (xx == null) return null;

            Term a = xx.sub(0), b = xx.sub(1);
            var yv = I.getAsInt();
            if (yv == 0)
                return EQ.the(a, b); //compare to equality: cmp(x,y)=0  ==>  x=y
            else if (a.compareTo(b) > 0) {
                //switch order to canonical ordering of arguments, negating the compare value
                return EQ.the(cmp(b, a), I.negative());
            }

        }
        return null;
    }

    public static Term cmp(Term a, Term b) {
        return $.func(cmp, a, b);
    }

}