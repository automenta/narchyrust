package nars.truth.proj;

import nars.NALTask;
import nars.Truth;
import nars.time.Moment;
import org.jetbrains.annotations.Nullable;

public interface TruthProjection extends Iterable<NALTask> {

    @Nullable NALTask task();

    /**
     * @param s if ETERNAL or TIMELESS, auto-focus  */
    @Deprecated @Nullable default Truth truth(long s, long e) {
        time(s, e);
        return truth();
    }

    default float ete() {
        return 0; //??
    }

    default /* final */ boolean time(long s, long e) {
        return time(s, e, 0);
    }

    /**
     * true if the start or end has changed
     */
    default /* final */ boolean time(long s, long e, int ditherDT) {
        return when().set(s, e, ditherDT);
    }

    Moment when();

    @Nullable Truth truth();

    /** query start */
    long start();
    /** query end */
    long end();


    void delete();

    /** weighted by evidence */
    default double priWeighted() {
        double pSum = 0, pEvi = 0;
        for (var x : this) {
            if (x!=null) {
                var e = x.evi();
                pEvi += e;
                pSum += x.priElseZero() * e;
            }
        }
        return pEvi < Float.MIN_NORMAL ? 0 : pSum / pEvi;
    }

    default double minComponentEvi() {
        var min = Double.POSITIVE_INFINITY;
        for (var i : this) {
            var ie = i.evi();
            if (ie < min) min = ie;
        }
        return min;
    }

    void clear();
}