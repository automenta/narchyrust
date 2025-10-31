package nars.truth;

import jcog.TODO;
import jcog.Util;
import nars.NAL;
import nars.NALTask;
import nars.task.SerialTask;
import nars.truth.evi.EviInterval;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * mean truth aggregation by linear combination
 */
public sealed class Mean implements Predicate {

    public final EviInterval evi;
    /**
     * for use with TruthSpan's that share a priority
     */
    public double freqEviRangeWeightedSum, eviIntegral, priWeightedSum;
    public int count;

    @Deprecated private static final float priSpan = 1;

    /**
     * whether to snap to the nearest task start/end in 'mean' calculations
     */
    //@Deprecated private final static boolean snap = false;

//        /** whether to stretch the query bounds by the tested tasks */
//        private static final boolean flex = false;

    public Mean(long s, long e, float dur) {
        evi = new EviInterval(s, e, dur);
    }

    @Override
    public final boolean test(Object x) {
        return x instanceof TruthSpan ts ? testSpan(ts) : testSerialTask((SerialTask)x);
    }

    protected boolean testSerialTask(SerialTask x) {
        return test(evi.eviInteg(x), x.priElseZero(), x.freq());
    }
    protected boolean testSpan(TruthSpan x) {
        return test(evi.eviInteg(x), priSpan, x.freq());
    }

    private boolean test(double eviInteg, float pri, float freq) {
        if (eviInteg >= NAL.truth.EVI_MIN /* / range... */) {
            this.eviIntegral += eviInteg;
            this.priWeightedSum = Util.fma(pri, eviInteg, priWeightedSum);
            this.freqEviRangeWeightedSum = Util.fma(freq, eviInteg, freqEviRangeWeightedSum);
            this.count++;
        }
        return true;
    }

    public float freq() {
        return (float) (freqEviRangeWeightedSum / eviIntegral);
    }

    public double eviMean() {
        long R = evi.e - evi.s + 1;
        return eviIntegral / R;
    }

    @Nullable
    public PreciseTruth truth(double eviMin) {
        throw new TODO();
    }

    public float pri() {
        return (float) (priWeightedSum / eviIntegral);
    }

    public static final class MeanFirstSaved extends Mean {
        @Nullable public NALTask first;

        public MeanFirstSaved(long s, long e, float dur) {
            super(s, e, dur);
        }

        @Override
        public boolean testSerialTask(SerialTask x) {
            if (super.testSerialTask(x)) {
                if (first == null)
                    first = x;
                return true;
            }
            return false;
        }
    }

//        /** for debugging */
//        private Predicate<SerialTask> unique() {
//            return new Predicate<>() {
//
//                Set s;
//
//                @Override
//                public boolean test(SerialTask x) {
//                    if (s == null) s = new UnifiedSet<>(4);
//                    boolean unique = s.add(x);
//                    if (!unique)
//                        System.out.println(unique);
//                    return unique && Mean.this.test(x);
//                }
//            };
//        }
}
