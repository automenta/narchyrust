package nars.truth.proj;

import jcog.Util;
import jcog.math.LongInterval;
import nars.NALTask;
import nars.truth.evi.EviInterval;

/**
 * result truth:
 * frequency = linear combination of frequency values weighted by evidence;
 * evidence = evidence sum
 * <p>
 * this implememnt aggregates combined evidence via linear inteprolation
 */
public class IntegralTruthProjection extends LinearTruthProjection {

    public IntegralTruthProjection(int capacity) {
        super(capacity);
    }

    public IntegralTruthProjection(LongInterval when) {
        this(when.start(), when.end());
    }

    public IntegralTruthProjection(long start, long end) {
        super(start, end);
    }

    @Override protected boolean computeComponents(NALTask[] tasks, int from, int to, EviInterval at, double[] evi) {
        var mean = at.s == ETERNAL; //mean mode

        var changed = false;
        float ete = ete();
        for (var i = from; i < to; i++) {
            var t = tasks[i];
            var e0 = evi[i];
            var e = mean ? t.evi() : at.eviInteg(t, ete);
            evi[i] = e;
            changed = changed || !Util.equals(e, e0, Double.MIN_NORMAL);
        }
        return changed;
    }

}