package nars.truth.func;

import nars.NAL;
import nars.Term;
import nars.Truth;
import nars.TruthFunctions;
import nars.term.atom.Atomic;
import nars.truth.AbstractMutableTruth;
import nars.truth.TruthCurve;
import org.jetbrains.annotations.Nullable;


/** within certain given assumptions about a set of truth functions,
 *  assumptions which are encoded as the approximate boundary conditions
 *  and extrema (0, 1, 0.5, etc..)
 *
 *  there are likely variations that will work more or less / better or
 *  worse than other particular implementations.  the particular
 *  uniquenesses constitute a 'mental personality' where there is not
 *  necessarily a right answer or not, but it is more of an artistic choice.
 */
public interface TruthFunction {

    static @Nullable Truth identity(Truth t, float minConf) {
        return t.conf() < minConf ? null : t;
    }

    default boolean truth(AbstractMutableTruth y, @Nullable Truth task, @Nullable Truth belief, double eviMin) {
        var t = truth(task, belief, eviMin);
        if (t==null)
            return false;
        else {
            y.set(t);
            return true;
        }
    }

    default @Nullable Truth truth(@Nullable Truth task, @Nullable Truth belief, double eviMin) {
        var confMin = (float) TruthFunctions.e2c(eviMin);

        if (task instanceof TruthCurve T && belief instanceof TruthCurve B) {
            //TODO spread T x B
            return task.evi() >= belief.evi() ?
                applySpreadT(T, belief, confMin) :
                applySpreadB(task, B, confMin);
        }

        if (task instanceof TruthCurve T)
            return applySpreadT(T, belief, confMin);
        else if (belief instanceof TruthCurve B)
            return applySpreadB(task, B, confMin);
        else
            return apply(task, belief, confMin);
    }

    private @Nullable Truth applySpreadT(TruthCurve T, @Nullable Truth belief, float confMin) {
        return T.cloneFn(tt -> apply(tt.truth, belief, confMin));
    }

    private @Nullable Truth applySpreadB(@Nullable Truth task, TruthCurve B, float confMin) {
        return B.cloneFn(bb -> apply(task, bb.truth, confMin));
    }


//    @Nullable
//    private Truth truthLenient(@Nullable Truth task, @Nullable Truth belief, double eviMin) {
//        //raise to min conf
//        Truth x = apply(task, belief, NAL.truth.CONF_MINf);
//        return x != null && x.evi() < eviMin ? PreciseTruth.byEvi(x.freq(), eviMin) : null;
//    }

    /**
     * @param confMin if confidence is less than minConf, it can return null without creating the Truth instance;
     *                if confidence is equal to or greater, then it is valid
     *                very important for minConf >= Float.MIN_NORMAL and not zero.
     * TODO 'double' confMin
     */
    @Nullable Truth apply(@Nullable Truth task, @Nullable Truth belief, float confMin);

    boolean allowOverlap();

    boolean single();

    default boolean taskTruthSignificant() {
        return true;
    }

    /**
     * only tested if not single()
     */
    default boolean beliefTruthSignificant() {
        return true;
    }

    default @Nullable Truth preTask(@Nullable Truth t) {
        return t;
    }

    default @Nullable Truth preBelief(@Nullable Truth t) {
        return t;
    }

    @Deprecated @Nullable
    default /* final */ Truth truth(@Nullable Truth task, @Nullable Truth belief) {
        return truth(task, belief, NAL.truth.EVI_MIN);
    }

    default Term term() {
        return Atomic.atomic(toString());
    }

    abstract class ProxyTruthFunction implements TruthFunction {
        protected final TruthFunction o;

        ProxyTruthFunction(TruthFunction o) {
            this.o = o;
        }

        public abstract String toString();

        @Override
        public final boolean taskTruthSignificant() {
            return o.taskTruthSignificant();
        }

        @Override
        public final boolean beliefTruthSignificant() {
            return o.beliefTruthSignificant();
        }

        @Override
        public @Nullable Truth apply(@Nullable Truth task, @Nullable Truth belief, float confMin) {
            return o.apply(task, belief, confMin);
        }

        @Override
        public final boolean allowOverlap() {
            return o.allowOverlap();
        }

        @Override
        public final boolean single() {
            return o.single();
        }

    }

    /**
     * swaps the task truth and belief truth
     */
    final class SwappedTruth extends ProxyTruthFunction {

        SwappedTruth(TruthFunction o) {
            super(o);
            if (single())
                throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable Truth apply(@Nullable Truth task, @Nullable Truth belief, float confMin) {
            return super.apply(belief, task, confMin);
        }

        @Override
        public String toString() {
            return o.toString() + 'X';
        }

    }

    /**
     * polarity specified for each component:
     * -1 = negated, 0 = depolarized, +1 = positive
     */
    final class RepolarizedTruth extends ProxyTruthFunction {

        final int taskPolarity, beliefPolarity;
        final boolean swap; //send thru another proxy
        private final String suffix;

        RepolarizedTruth(TruthFunction o, int taskPolarity, int beliefPolarty, String suffix) {
            this(o, taskPolarity, beliefPolarty, false, suffix);
        }

        RepolarizedTruth(TruthFunction o, int taskPolarity, int beliefPolarty, boolean swap, String suffix) {
            super(o);
            this.taskPolarity = taskPolarity;
            this.beliefPolarity = beliefPolarty;
            this.suffix = suffix;
            this.swap = swap;
        }

        private static @Nullable Truth repolarize(Truth t, int polarity) {
            return polarity == -1 || (polarity == 0 && t.NEGATIVE()) ? _neg(t) : t;
        }

        private static Truth _neg(Truth t) {
            if (t instanceof AbstractMutableTruth m) {
                //TODO?
                m.negThis();
                return t;
            } else
                return t.neg();
        }

        /**
         * special handling for applying the polarization to the original inputs before swapping
         */
        RepolarizedTruth swapped() {
            assert (!swap);
            return new RepolarizedTruth(o, taskPolarity, beliefPolarity, true, suffix);
        }

        @Override
        public @Nullable Truth apply(@Nullable Truth task, @Nullable Truth belief, float confMin) {
            return swap ? super.apply(belief, task, confMin) : super.apply(task, belief, confMin);
        }

        @Override
        public Truth preTask(Truth t) {
            return repolarize(t, taskPolarity);
        }

        @Override
        public Truth preBelief(Truth t) {
            return repolarize(t, beliefPolarity);
        }

        @Override
        public final String toString() {
            return o + suffix + (swap ? "X" : "");
        }
    }
}