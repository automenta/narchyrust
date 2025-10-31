package nars.time;

import jcog.TODO;
import jcog.Util;

import javax.measure.Quantity;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.round;
import static nars.Op.ETERNAL;

/**
 * 1-D Time Model (and Clock)
 */
public abstract class Time  {

    private static final VarHandle T = Util.VAR(Time.class, "t", long.class);

    protected volatile long t;

    protected final AtomicLong nextStamp;

    protected Time(long stampSeed) {
        nextStamp = new AtomicLong(stampSeed);
    }

    public long now() {
        return (long) T.getOpaque(this);
    }

    @Override
    public final String toString() {
        return Long.toString(now());
    }


    /* TODO abstract */ public void skip(long dt) {
        throw new TODO();
    }

    /**
     * produces a new stamp serial #, used to uniquely identify inputs
     */
    public final long nextStamp() {
        return nextStamp.incrementAndGet();
    }

    /**
     * the default duration applied to input tasks that do not specify one
     * >0
     */
    public abstract float dur();


    /**
     * update to the next time
     *
     * @return
     */
    public abstract long next();

    /** clock reset */
    public abstract void reset();

    /**
     * set the duration, return this
     *
     * @param d, d>0
     */
    public abstract Time dur(float d);


    protected final void _set(long time) {
        T.setVolatile(this, time);
        //T.setOpaque(this, time);
    }

//    /**
//     * returns a string containing the time elapsed/to the given time
//     */
//    public String durationToString(long target) {
//        long now = now();
//        return durationString(now - target);
//    }

    /**
     * produces a string representative of the amount of time (cycles, not durs)
     */
//    public abstract String timeString(long time);

    public long toCycles(Quantity q) {
        throw new UnsupportedOperationException("Only in RealTime implementations");
    }


    @Deprecated public long relativeOccurrence(Tense tense) {
        /*m.duration()*/
		return switch (tense) {
			case Present -> now();
			case Past -> round((double) now() - dur());
			case Future -> round((double) now() + dur());
			default -> ETERNAL;
		};
    }


    public long[] present() {
        long now = now();
        int dur = (int)dur();
        return new long[] { now - dur/2, now + dur/2};
    }
}