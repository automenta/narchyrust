package nars.time;

import jcog.math.LongInterval;
import nars.NAL;

/** describes a perceptual moment in time
 *  TODO extends MutableLongInterval superclass
 * */
public class Moment implements LongInterval {

    /**
     *
     *      * this represents the 'match duration',
     *      * which isnt necessarily the same as the
     *      * 'truth projection duration' which is
     *      * applied to the match to
     *      * determine truth or task results.
     *
     * duration >= 0 */
    public float dur;

    /** start, end */
    public long s = TIMELESS, e = TIMELESS;

    /** dur >= 0 */
    public Moment dur(float dur) {
        if (dur < 0 || !Float.isFinite(dur))
            throw new UnsupportedOperationException();
        this.dur = dur;
        return this;
    }

    @Override
    public String toString() {
        return s + ".." + e + "~" + dur;
    }

    /** TODO rename 'time()...' */
    public final Moment when(long w) {
        return when(w,w);
    }

    public final Moment when(long[] se) {
        return when(se[0], se[1]);
    }

    /** TODO rename 'time()...' */
    public final Moment when(long s, long e) {
        if (e < s) {
            /* TODO this is likely caused by a Task being stretched in another thread */
            if (NAL.DEBUG)
                throw new UnsupportedOperationException();
            else
                e = s;
        }

        this.s = s; this.e = e;
        return this;
    }

    public final Moment when(long s, long e, NAL n) {
        return when(s, e, n.timeRes());
    }

    public final Moment when(long s, long e, int timeRes) {
        if (timeRes > 1) {
            s = Tense.dither(s, timeRes);
            if (s == ETERNAL)
                return eternal();

            e = Tense.dither(e, timeRes);
        }
        return when(s, e);
    }

    public final LongInterval dither(int timeRes) {
        return timeRes > 1 ? when(s, e, timeRes) : this;
    }

    public final Moment setCenter(long mid, float dur) {
        if (mid == ETERNAL) return eternal().dur(dur);

        var durHalf = (int) (dur / 2);
        return when(mid - durHalf, mid + durHalf).dur(dur);
    }

//    public final Moment setCenter(long mid, int dur, int ditherDT) {
//        return mid == ETERNAL ?
//                eternal() :
//                when(mid - dur / 2, mid + dur / 2, ditherDT);
//    }

    public final Moment eternal() {
        return when(ETERNAL,ETERNAL);
    }

    @Override
    public final long start() { return s;}

    @Override
    public final long end() {
        return e;
    }

    public final boolean equals(LongInterval l) {
        return this == l ||
            (s == l.start() && e == l.end());
    }


//    public final Moment setCenterDur(long when, float range) {
//        return setCenter(when, Math.round(range)).dur(range);
//    }
//    public final Moment setCenterDur(long when, float range, int ditherDT) {
//        return setCenter(when, Math.round(range), ditherDT).dur(range);
//    }

    public boolean set(long s, long e, int timeRes) {
        if (timeRes > 1 && s != TIMELESS && s != ETERNAL) {
            s = Tense.dither(s, timeRes/*, -1*/);
            e = Tense.dither(e, timeRes/*, +1*/);
        }
        return set(s, e);
    }



    public final boolean durIfChanged(float dur) {
        if (this.dur!=dur) {
            this.dur(dur);
            return true;
        }
        return false;
    }

    public final boolean set(long start, long end) {
        if (this.s != start || this.e != end) {
            when(start,end);
            return true;
        } else
            return false;
    }

}