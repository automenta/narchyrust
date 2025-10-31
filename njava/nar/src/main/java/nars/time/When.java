package nars.time;

import jcog.math.LongInterval;
import nars.truth.evi.EviInterval;

/**
 * reference to a temporal context:
 *      --start, end
 *          occurrence
 *      --dur
*           time constant (may be less equal or greater than the occurrence range)
 *
 *
 * represents a mutable perceptual window in time, including a perceptual duration (dur) and
 * reference to invoking context X.
 * */
@Deprecated public class When<X> {

    public final EviInterval evi;
    public transient X x;

    public When() {
        evi = new EviInterval();
    }

    public When(long s, long e) {
        evi = new EviInterval(s, e);
    }

    public When(X x, LongInterval interval, float dur) {
        this(x, interval.start(), interval.end(), dur);
    }

    public When(X x, long start, long end, float dur) {
        evi = new EviInterval(start, end, dur);
        this.x = x;
    }

    public final boolean the(X next) {
        X prev = this.x;
        if (prev!=next) {
            this.x = next;
            return true;
        }
        return false;
    }

}