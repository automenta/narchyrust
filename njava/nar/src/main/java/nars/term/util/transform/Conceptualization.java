package nars.term.util.transform;

import jcog.Util;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.compound.CachedCompound;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

import static nars.Op.*;

/**
 * defines target -> concept mapping.
 * generally, terms name concepts directly except in temporal cases there is a many to one target to concept mapping
 * effectively using concepts to 'bin' together varieties of related terms
 */
public enum Conceptualization {
    ;

    /**
     * the standard conceptualization strategy.
     * chunks event sequence into super-events sharing no common conegated events
     * TODO rename. 'Hybrid' is no longer applicable.
     */
    public static final Untemporalize Hybrid = new Untemporalize() {

        @Override
        protected Term transformConjSeq(Compound x) {
            return conjSeq(events(x));
        }

        private static Term conjSeq(Subterms xx) {
            return xx.subs() <= 2 ?
                    CONJ.the(XTERNAL, xx) :
                    xternalSeq(xx);
        }

        private static Subterms events(Compound x) {
            return x.dt() == XTERNAL ?
                xternalEvents(x) :
                seqEvents(x, false, false);
        }

        private static Compound xternalSeq(Subterms s) {
            //HACK create term directly, to avoid subterm sorting
            return CachedCompound.the(CONJ, XTERNAL,
                s instanceof TermList ?
                    terms.subterms(s.arrayShared()) : s
            );
        }

        private static Subterms xternalEvents(Compound x) {
            return transformSubs(x.subtermsDirect(), ROOT);
        }

//        private Term transformConjSeq0(Compound x) {
//            TermList xx = seqEvents(x, false, false);
//
//            int s = xx.subs();
//
//            if (s > 1) xx.reverseThis(); //HACK put sequence start outside
//
//            Term y;
//            if (/*!flatten() ||*/ s > 2 || ((x.structureSubs() & CONJ.bit) != 0) || (s == 2 && xx.sub(0).equalsPN(xx.sub(1)))) {
//                y = ConjSeq.xSeq(xx);
//            } else {
//                y = CONJ.the(XTERNAL, (Subterms)xx);
//            }
//            xx.delete();
//
//            if (x!=y)
//                y = Util.maybeEqual(y, x);
//
//            return y;
//        }

    };
    public static final UnaryOperator<Term> ROOT = Term::root;

    public abstract static class Untemporalize extends Retemporalize {

        protected abstract Term transformConjSeq(Compound x);

        @Override
        protected final Term applyConj(Compound x) {
            return x.dt()==DTERNAL ?
                    transformConjPar(x) : transformConjSeq(x);
        }

        private static Term transformConjPar(Compound x) {
            var xx = x.subterms();
            var yy = transformSubs(xx);
//            if (yy == xx) return x;

            var xdt = x.dt();
            var ydt = conjParDT(yy, xdt);

            if (xx==yy && xdt==ydt)
                return x;

            var y =
                CONJ.the(ydt, yy);
                //terms.compoundNew(CONJ, ydt, yy); //RAW
                //terms_.compoundNew(CONJ, ydt, yy); //RAW

            return x != y && xdt == ydt ? Util.maybeEqual(y, x) : y;
        }

        private static int conjParDT(Subterms yy, int xdt) {
            return yy.subs()==2 ? XTERNAL : DTERNAL;
            //return XTERNAL;
            //return !yy.hasAny(CONJ) ? DTERNAL : XTERNAL;
//            return yy.subs() == 2 && (xdt == XTERNAL || yy.seqDur(true) == 0) ?
//                    XTERNAL : DTERNAL;
            //return DTERNAL;
        }

        @Nullable private static Subterms transformSubs(Subterms xx) {
            return xx.hasAny(Temporals) ? transformSubs(xx, /*this*/ROOT) : xx;
        }

        @Override
        protected Term applyImpl(Compound x) {
            var xx = x.subterms();
            var yy = transformSubs(xx);
            return x.dt() == XTERNAL && xx.equals(yy) ?
                    x : IMPL.the(XTERNAL, yy);
        }
    }

    static TermList seqEvents(Compound x, boolean dE, boolean dX) {
        var y = new TermList(3);
        x.conds(y::addRoot, dE, dX);
        return y;
    }

}