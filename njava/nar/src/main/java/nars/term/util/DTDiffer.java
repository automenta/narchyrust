package nars.term.util;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import jcog.sort.FloatRank;
import nars.NAL;
import nars.NALTask;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static nars.Op.*;

/**
 * TODO this is still biased unfairly against partial XTERNAL temporals in interpreting
 * them in comparison to 0.  a more exhaustive comparison is needed for fairness
 */
public abstract class DTDiffer implements FloatRank<NALTask> {

    public static final DTDiffer DTDifferZero = new DTDiffer() {
        @Override public long diff(Compound b) {
            return 0;
        }

        @Override
        public DTDiffer dur(float dur) {
            return this;
        }
    };

    /** used in rank() */
    private float dur = 1;

    public static DTDiffer the(Compound a) {
        boolean at = a.TEMPORAL();
        if (at && !hasAny(a.structSubs(), Temporals)) {
            int aDT = a.dt();
            return aDT == XTERNAL ? DTDifferZero : new DTDifferRootOnly(aDT);
        } else {
            if (a.INH() || a.SIM()) return DTDifferZero; //HACK for ConjBundle

            DTDifferExhaustive e = null;
            if (!a.COMMUTATIVE() && (!at || (/*a.IMPL() &&*/ a.dt() == XTERNAL))) {
                //specialize into comparison involving only one subterm
                //HACK TODO drill-down fully recursive
                ByteArrayList path = null;
                Compound tgt = a;

                int which;
                do {
                    if ((which = nextUniquePath(tgt)) < 0) {
                        path = null; //can not specialize to one subterm
                        break;
                    }

                    (path == null ? path = new ByteArrayList(2) : path).add((byte) which);

                    tgt = (Compound) tgt.sub(which);

                } while (!tgt.TEMPORAL());

                if (path != null)
                    e = new DTDifferSubExhaustive(tgt, path.toByteArray());
            }

            if (e == null) e = new DTDifferExhaustive(a);

            return e.definite == 0 ? DTDifferZero : e;
        }

    }

    /** returns >=0 if only one subterm is temporal */
    private static int nextUniquePath(Compound a) {
        int which = -1;
        Subterms aa = a.subtermsDirect();
        int n = aa.subs();
        for (int i = 0; i < n; i++) {
            if (aa.subUnneg(i).TEMPORALABLE()) {
                if (which != -1)
                    return -1; //multiple paths, cancel

                which = i;
            }
        }
        return which;
    }

    private static int span(int dt) {
        return switch (dt) {
            case 0, DTERNAL, XTERNAL -> 0;
            default -> abs(dt); //sequence
        };
    }

//    /**
//     * multiplier for strength or evidence preservation
//     */
//    public final float factor(Compound b) {
//        return Util.max(0, 1 - diff(b) / 2);
//    }

    @Override
    public final float rank(NALTask x, float min) {
        long diff = this.diff((Compound) x.term());
        double r =
                1 + 1 / (1 + diff/(dur+1)); //inverse linear
        //1 / (1 + diff/(dur+1)); //inverse linear
        //NAL.truth.curve.project(diff, dur);
        return (float) r;
    }

//    /**
//     * proportional difference; result is either POSITIVE_INFINITY, or a value >= 0 and <= 2.
//     * (x &&+0 y) : (x &&+1 y) |- 100% different
//     * (x &&-1 y) : (x &&+1 y) |- 200% different (full reverse)
//     */

    /** absolute difference */
    public abstract long diff(Compound b);


    public DTDiffer dur(float dur) {
        this.dur = dur;
        return this;
    }

    private static final class DTDifferRootOnly extends DTDiffer {

        //        private final int aSpan;
        private final int aDT;

        private DTDifferRootOnly(int aDT) {
            this.aDT = aDT;
//            aSpan = span(aDT);
        }

        @Override
        public long diff(Compound b) {
            int bDT = b.dt();
            return aDT == bDT || bDT == XTERNAL ? 0 :
                    Intermpolate.dtDiff(aDT, bDT);
        }
    }

    static class DTDifferExhaustive extends DTDiffer {
        public final Compound a;
        private final int aOp;
        private int unknown;
        private int definite;
        private long aSpan;

        DTDifferExhaustive(Compound a) {
            aSpan = TIMELESS;
            aSpan = span(a);
            this.a = a;
            aOp = a.opID();
        }

        @Override
        public long diff(Compound b) {
            if (definite == 0 || (a == b))
                return 0; //completely xternal; match any
            else if (b.opID != aOp)
                return TIMELESS; //TODO find why this happens
            else {
                long s = max(aSpan, span(b));
                return s <= 0 ? 0 : Intermpolate.dtDiff(this.a, b);
            }
        }

        /**
         * absolute eventRange
         *
         * @param x expected to be non-neg term, hopefully a compound
         */
        private long span(Compound x) {
            assert(!(x instanceof Neg)); //if (x instanceof Neg) x = x.unneg();

            if (!x.TEMPORALABLE()) return 0;

            int n = x.subs();
            int xid = x.opID;

            long s = (0 != ((1 << xid) & Temporals)) ? spanImplicit(x, xid) : 0;

            Subterms xx = x.subtermsDirect();

            for (int i = 0; i < n; i++) {
                Term xi = xx.subUnneg(i);
                if (xi instanceof Compound xic)
                    s += span(xic);
            }

            return s;
        }

        private long spanImplicit(Term x, int xid) {
            int xdt = x.dt();
            boolean xternal = xdt == XTERNAL;

            if (aSpan == TIMELESS) {
                if (xternal) unknown++;
                else definite++;
            }

            return (xid == IMPL.id || xid == CONJ.id) ? DTDiffer.span(xdt) : 0;
        }
    }

    static class DTDifferSubExhaustive extends DTDifferExhaustive {
        /**
         * TODO byte[] path
         */
        final byte[] path;

        private DTDifferSubExhaustive(Compound tgt, byte[] path) {
            super(tgt);
            this.path = path;
        }

        @Override
        public long diff(Compound b) {
            Term c = NAL.DEBUG ? b.sub(path) : b.subSafe(path); //HACK
            return c instanceof Compound C ? super.diff(C) : TIMELESS;
        }
    }


}