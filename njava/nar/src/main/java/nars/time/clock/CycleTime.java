package nars.time.clock;

import nars.time.Time;

/**
 * increments time on each frame
 */
public class CycleTime extends Time {

    final int dt;

    float dur;

    CycleTime(int dt, int dur) {
        super(0);
        this.dt = dt;
        this.dur = dur;
        reset();
    }

    public CycleTime() {
        this(1, 1);
    }

    @Override
    public float dur() {
        return dur;
    }

    @Override
    public CycleTime dur(float d) {
        this.dur = d;
        return this;
    }

    @Override
    public void reset() {
        _set(0);
    }

    @Override
    public final long next() {
        //TODO fully atomic
        return set(now() + dt);
    }

    public final long set(long when) {
        _set(when);
        return when;
    }
}