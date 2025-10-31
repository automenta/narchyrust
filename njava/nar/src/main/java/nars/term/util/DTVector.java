package nars.term.util;

import jcog.TODO;
import nars.NALTask;
import nars.Term;
import nars.term.Compound;
import nars.util.SoftException;

import java.util.function.Predicate;

/** utilities for extracting the DT shape of a term's DT components */
public final class DTVector implements Predicate<Term> {

    /** dt vector */
    private final int[] dt;

    private final int dimsOffset;

    /** current index */
    transient int yi = -1;

    public DTVector(Compound x) {
        this(count(x), 0);
        set(x);
    }

    public DTVector(int dims, int dimsOffset) {
        if (dims == 0)
            throw new IllegalArgumentException();
        this.dt = new int[dims];
        this.dimsOffset = dimsOffset;
    }

    public float diff(Compound b) {
        DTVector bv = new DTVector(dt.length, 0);
        if (!bv.set(b))
            throw new UnsupportedOperationException();
        return diff(dt, bv.dt);
    }

    private float diff(int[] x, int[] y) {
        var n = x.length;
        if (n !=y.length)
            throw new UnsupportedOperationException();
            //return Float.NaN;
//        for (int i = 0; i < n; i++) {
//
//        }
//        return 0;
        throw new TODO();
    }

    public static int count(Term t) {
        return temporalable.test(t) ? _count(t) : 0;
    }

    private static int _count(Term t) {
        var c = new DTCounter();
        t.recurseTermsOrdered(temporalable, c, null);
        return c.count;
    }

    @Override
    public boolean test(Term x) {
        if (x.TEMPORAL()) {
            dt[yi] = x.DT();
            return yi++ != dt.length; //done?
        }
        return true;
    }

    private final static Predicate<Term> temporalable = Term::TEMPORALABLE;

    //x -> x.TEMPORALABLE() /*&& !x.isAny(INH.bit | SIM.bit)*/;

    @Deprecated public static final class ConceptShapeException extends SoftException {  }

    public final boolean set(NALTask x, double[] c) {
        return set((Compound)x.term(), c);
    }

    public boolean set(Compound x, double[] c) throws ConceptShapeException {
        if (set(x)) {
            copyTo(c);
            return true;
        } else
            return false;
    }

    private boolean set(Compound x) throws ConceptShapeException {
        yi = 0;
        x.recurseTermsOrdered(temporalable, this, null);
        return yi == dt.length;
    }

    private void copyTo(double[] y) {
        int offset = dimsOffset, l =  y.length - offset;
        for (var i = 0; i < l; i++)
            y[i + offset] = dt[i];
    }


    //    public static double pctDiff(double[] a, double[] b, int from, int to) {
//        double dtDiff = 0;
//        for (int i = from; i < to; i++)
//            dtDiff += Util.pctDiff(a[i], b[i]);
//        return dtDiff;
//    }

    private static final class DTCounter implements Predicate<Term> {
        private int count;

        @Override
        public boolean test(Term x) {
            if (x.TEMPORAL()) //and dt!=DTERNAL?
                count++;
            return true;
        }
    }
}