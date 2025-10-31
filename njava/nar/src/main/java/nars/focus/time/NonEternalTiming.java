package nars.focus.time;

import jcog.math.Intervals;
import jcog.signal.FloatRange;
import nars.Deriver;
import nars.NALTask;

public class NonEternalTiming extends PresentTiming {

    public final FloatRange durFocus = new FloatRange(1, 0, 32);

    @Override
    public long[] whenRelative(NALTask t, Deriver d) {
        return t.ETERNAL() ?
            Intervals.range(d.now(), d.dur() * durFocus.floatValue()) :
            t.startEndArray();
    }

}