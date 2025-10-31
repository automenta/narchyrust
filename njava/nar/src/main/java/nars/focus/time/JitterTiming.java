package nars.focus.time;

import jcog.signal.FloatRange;
import nars.Deriver;
import nars.NALTask;
import nars.time.Tense;

import java.util.random.RandomGenerator;

import static jcog.math.LongInterval.ETERNAL;

/**
 * applies a time shift in durs determined by a random distribution
 */
public class JitterTiming extends ProxyTiming {

    public final FloatRange jitterRange = new FloatRange(1, 0, 2);
    static final float jitterDither = 0.5f;

    public JitterTiming(TaskWhen delegate) {
        super(delegate);
    }

    @Override
    public long[] whenAbsolute(Deriver d) {
        return jitter(super.whenAbsolute(d), d);
    }

    @Override
    public long[] whenRelative(NALTask t, Deriver d) {
        return jitter(super.whenRelative(t, d), d);
    }

    private long[] jitter(long[] se, Deriver d) {
        final float dDur = d.dur();
        return jitter(se, dDur,
                Math.round(Math.max(dDur * jitterDither, d.nar.dur())),
                d.rng);
    }

    private long[] jitter(long[] se, float dur, int jitterDitherDT, RandomGenerator rng) {
        if (se[0]==ETERNAL || dur <= 0) return se;

        float r = jitterRange.floatValue();
        if (r > 0) {
            //long dur = Math.max(1, se[1] - se[0]);
            long shift = Math.round(r * dur * rng.nextGaussian());

            shift = Tense.dither(shift, jitterDitherDT);

            se[0] += shift;
            se[1] += shift;
        }
        return se;
    }
}