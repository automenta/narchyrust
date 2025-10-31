package nars.term.builder;

import jcog.Util;
import jcog.WTF;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Term;
import nars.subterm.*;
import nars.term.Neg;
import nars.term.anon.Intrin;
import nars.term.util.Terms;

import java.util.Arrays;

/** enhanced Subterms that maximize shared components, through wrapping transforms
 *  (ex: negations, remapping, repeats, etc..)  */
public class SmartTermBuilder extends SimpleTermBuilder {

    public static final SmartTermBuilder the = new SmartTermBuilder(NAL.term.SUBTERMS_UNPERMUTE);

    private final boolean unpermute;

    public SmartTermBuilder(boolean unpermute) { this.unpermute = unpermute; }

    public final Subterms subtermsNew(Term[] x) {
        if (Intrin.intrin(x))
            return new IntrinSubterms(x);

        var n = x.length;

        if (n > 1 && unpermute)
            return subtermsSmart(x);

        return switch (n) {
            case 1  -> subterms1(x[0]);
            case 2  -> subterms2(x);
            default -> subtermsN(x);
        };
    }

    protected static UnitSubterm subterms1(Term a) {
        return new UnitSubterm(a);
    }

    protected static Subterms subterms2(Term[] x) {
        Term a = x[0], b = x[1];
        return a instanceof Neg && b instanceof Neg ?
            new BiSubterm(a.unneg(), b.unneg()).negated() :
            new BiSubterm(x/*a, b*/);
    }

    /** tries to apply 'smart' techniques */
    private Subterms subtermsSmart(final Term[] x) {
        var a = x[0];

        var n = x.length;
        if (n == 2) {
            var b = x[1];
            if (!(a instanceof Neg) && !(b instanceof Neg))
                return unpermute2(x, false, a, b);
        }

        var y = x;
        var unneged = false;
        for (var j = 0; j < n; j++) {
            var yj = y[j];
            if (yj instanceof Neg) {
                if (y == x) y = x.clone();
                y[j] = yj.unneg();
                unneged = true;
            }
        }

        if (y == x)
            y = Terms.sort(y);
        else
            Arrays.sort(y);

        //already sorted and has no negatives
        //TODO if (xx.length == 1) return RepeatedSubterms.the(xx[0],x.length);
        return !unneged && ArrayUtil.equalsIdentity(x, y) ?
            subtermsN(x) :
            remappedSubterms(x, subtermsN(y));
    }

    protected final Subterms subtermsN(Term... x) {
        return super.subtermsNew(x);
    }

    private static ArrayRemappedSubterms remappedSubterms(Term[] x, Subterms base) {
        var n = x.length;
        var m = new byte[n];

        var hash = 1;         //int hash = Subterms.hash(target);
        for (var i = 0; i < n; i++) {
            var xx = x[i];

            hash = Util.hashCombine(hash, xx); //TODO defer to after unwrap, then virtually negate the hashed term after

            var neg = (xx instanceof Neg);

            var xi = neg ? xx.unneg() : xx;

            var mi = base.indexOfInstance(xi)+1;

            if (mi <= 0) {
                //test exhaustive, since it may have been replaced by another thread
                mi = base.indexOf(xi) + 1;
                if (mi <= 0)
                    return missing(base, xi);
            }

            m[i] = (byte) (neg ? -mi : mi);
        }

        return new ArrayRemappedSubterms(base, m, hash);
    }
    /** @param dedup untested */
    private Subterms unpermute2(Term[] x, boolean dedup, Term a, Term b) {
        var i = a.compareTo(b);
        if (dedup && i == 0)
            return subtermsN(a);
        else if (i <= 0)
            return subtermsN(x);
        else
            return subtermsN(b, a).reverse();
    }

    private static ArrayRemappedSubterms missing(Subterms base, Term xi) {
        throw new WTF
                //TermException
                (xi + " not found in " + base + ", base.class=" + base.getClass() + " target.xi.class=" + xi.getClass());
    }

}