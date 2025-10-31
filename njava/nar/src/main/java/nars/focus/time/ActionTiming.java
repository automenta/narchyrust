package nars.focus.time;


import jcog.math.Intervals;
import jcog.signal.FloatRange;
import nars.Deriver;
import nars.NALTask;

public class ActionTiming extends PresentTiming {

    /** TODO mutable histogram model for temporal focus duration  */
    public final FloatRange durFocus = new FloatRange(1, 0, 32);

    /** TODO mutable histogram model for temporal focus position  */
    public final FloatRange durHorizon = new FloatRange(1, 0, 32);

    @Override
    public long[] whenRelative(NALTask t, Deriver d) {

        float dur = d.dur();

        long shift =  Math.round(
            dur * durHorizon.doubleValue() * d.rng.nextGaussian()
        );

        return Intervals.range(d.now() + shift, dur * durFocus.floatValue());
    }


}