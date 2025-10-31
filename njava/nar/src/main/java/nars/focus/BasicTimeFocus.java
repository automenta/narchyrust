package nars.focus;

import jcog.signal.FloatRange;
import nars.Focus;

public class BasicTimeFocus extends TimeFocus {

    /**
     * duration of present-moment perception, in global clock cycles,
     * specific to this What, and freely adjustable
     * TODO DurRange - with .fps() method
     */
    public final FloatRange dur = new FloatRange(1, 0, 16 * 1024);

    /**
     * past/present/future shift, in system durations
     */
    public final FloatRange shift = new FloatRange(0, -8 * 128, +8 * 128);

//    /** align to dur's */ private boolean ditherDur = true;

    public final float dur() {
        return dur.floatValue();
    }

    public final void dur(float nextDur) {
        dur.setSafe(nextDur);
    }

    public final void shiftDurs(float durs) {
        shift.set(durs);
    }

    @Override
    public final long[] when(Focus f) {
        return when(f.time() + Math.round(shift.floatValue() * f.durSys), dur());
    }


}