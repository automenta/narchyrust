package nars.time.clock;

import jcog.Str;
import nars.time.Time;
import tec.uom.se.quantity.time.TimeQuantities;

import javax.measure.Quantity;
import java.util.UUID;

/**
 * Created by me on 7/2/15.
 */
public abstract class RealTime extends Time {

    private final int unitsPerSecond;
    private final boolean immediate;

    long _start;
    public final boolean relativeToStart;
    long start;

    /** TODO make volatile? */
    float dur = 1;

    private final long nsPerUnit;

    RealTime(int unitsPerSecond, boolean immediate, boolean relativeToStart) {
        super(Math.abs(UUID.randomUUID().getLeastSignificantBits() ) & 0xffff0000L);

        this.immediate = immediate;
        this.relativeToStart = relativeToStart;
        this.unitsPerSecond = unitsPerSecond;
        this.nsPerUnit = 1_000_000_000/unitsPerSecond;
    }


    @Override
    public final long now() {
        return immediate ? nowSet() : nowCached();
    }

    private long nowCached() {
        return super.now();
    }

    @Override
    public void reset() {
        this._start = System.nanoTime();
        this.start = relativeToStart ?
            Math.round((System.currentTimeMillis() / 1000.0) * unitsPerSecond)
            : 0;
        nowSet();
    }

    @Override
    public final long next() {
        return nowSet();
    }

    private long nowSet() {
        var t = realtime();
        _set(t);
        return t;
    }

    protected final long realtime() {
        return start + (System.nanoTime() - _start) / nsPerUnit;
    }

//    double secondsSinceStart() {
//        return unitsToSeconds(now() - start);
//    }

    public final double unitsToSeconds(double l) {
        return l / unitsPerSecond;
    }


    @Override
    public final Time dur(float d) {
        assert(d > 0);
        this.dur = d;
        return this;
    }

    private Time durSeconds(double seconds) {
        return dur(Math.max(1, (int) Math.round(secondsToUnits(seconds))));
    }

    @Override
    public final float dur() {
        return dur;
    }

    public final Time durFPS(double fps) {
        durSeconds(1/fps);
        return this;
    }

//    @Override
//    public String timeString(long time) {
//        return Texts.timeStr(unitsToSeconds(time) * 1.0E9);
//    }

    /** ratio of duration to fps */
    public final float durSeconds() {
        return (float) unitsToSeconds(dur);
    }
    public long durNS() {
        return Math.round(1.0E9 * durSeconds());
    }

    public final double secondsPerUnit() {
        return (float) unitsToSeconds(1);
    }

    public final double secondsToUnits(double s) {
        return s / unitsToSeconds(1);
    }

//    /** get real-time frames per duration */
//    public float durRatio(Loop l) {
//        float fps = l.getFPS();
//        if (fps > Float.MIN_NORMAL)
//            return durSeconds() * fps;
//        else
//            return 1;
//    }
//    /** set real-time frames per duration */
//    public void durRatio(Loop l, float ratio) {
//        durSeconds(ratio / l.getFPS());
//    }

    @Override
    public final long toCycles(Quantity q) {
        var s = TimeQuantities.toTimeUnitSeconds(q).doubleValue(null);
        return Math.round(s * unitsPerSecond);
    }

    public final String unitsToTimeString(double durMin) {
        return Str.timeStr(unitsToNS(durMin));
    }

    /** units to nanoseconds */
    private double unitsToNS(double durMin) {
        return 1E9 * unitsToSeconds(durMin);
    }


    /** decisecond (0.1) accuracy */
    protected static class DS extends RealTime {
        DS(boolean relativeToStart, boolean imm) {
            super(10, imm, relativeToStart);
            //(100 * 1_000_000)
        }
    }


    /** centisecond (0.01) accuracy */
    public static class CS extends RealTime {


        public CS() {
            this(false, true);
        }

        CS(boolean relativeToStart, boolean imm) {
            super(100, imm, relativeToStart);
            //(10 * 1_000_000)
        }


    }

    /** millisecond accuracy */
    public static class MS extends RealTime {

        private static final boolean IMMEDIATE =
            false;
            //true;

        public MS() {
            this(false, IMMEDIATE);
        }

        public MS(boolean relativeToStart, boolean immediate) {
            super(1000, immediate, relativeToStart);
        }

        @Override
        public long durNS() {
            return (long)(dur * 1_000_000.0);
        }
    }

    /** nanosecond accuracy */
    protected static class NS extends RealTime {
        protected NS(boolean relativeToStart, boolean imm) {
            super(1000*1000*1000, imm, relativeToStart);
        }

    }
}