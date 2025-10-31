package nars.focus;

import nars.Focus;

/**
 * temporal sampling envelope, relative to present
 */
public abstract class TimeFocus {

    /** returns a unique instance, not shared */
    public abstract long[] when(Focus f);

    /** present moment duration ,in system durs */
    abstract public float dur();

    abstract public void dur(float dur);

    static long[] when(long when, float dur) {
        return whenCenter(when, Math.round(dur));
    }

    public static long[] whenCenter(long mid, int dur) {
        return new long[] {
            mid - dur / 2,
            mid + dur / 2
        };
    }

    /** left aligned */
    static long[] when(long start, long dur) {
        return new long[]{start, start + Math.max(1, dur) - 1};
    }

}