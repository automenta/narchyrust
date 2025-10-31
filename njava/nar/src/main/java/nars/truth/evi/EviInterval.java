package nars.truth.evi;

import jcog.math.LongInterval;
import jcog.sort.FloatRank;
import nars.NAL;
import nars.NALTask;
import nars.time.Moment;
import nars.truth.TruthSpan;

/**
 * an Evidence Duration - describes a time range in which
 * multiple task's/truths can be reduced to one truth value
 *
 * computes evidence projections across time, and range integrals
 *
 * TODO extend When<?>
 */
public final class EviInterval extends Moment implements FloatRank<NALTask> {

    public EviInterval(long s, long e) {
        when(s,e);
    }

    public EviInterval(long s, long e, float dur) {
        this(s, e);
        dur(dur);
    }

    public EviInterval(long[] se, float dur) {
        this(se[0], se[1], dur);
    }

    public EviInterval() { }

    /** integrated evidence using this Evidence envelope on provided Task */
    public final double eviInteg(NALTask t) {
        return eviInteg(t, 0, false);
    }

    public final double eviInteg(TruthSpan t) {
        return eviInteg(t, t.evi(), 0, false);
    }

    private double eviInteg(NALTask t, float ete, boolean meanOrSum) {
        return eviInteg(t, t.evi(), ete, meanOrSum);
    }

    private double eviInteg(LongInterval t, double evi, float ete, boolean meanOrSum) {
        return evi * integral(t, ete, meanOrSum);
    }

    public double integral(LongInterval t, float ete, boolean meanOrSum) {
        return EviProjector.integrate(t, s, e, dur, ete, meanOrSum);
    }



    public final long range() {
        if (s == ETERNAL) throw new UnsupportedOperationException("eternal range()");
        return 1 + (e - s);
    }

    /** approximates EviProjection */
    private float rankFast(NALTask x, double min) {
        double e =
            x.evi();
            //1 + x.evi();

        return e < min ? Float.NaN :
            rankFast(e, diffCenterRadii(x, true));
    }

    private float rankFast(double e, long dt) {
        return (float) (e / (1 + dt / (1.0 + dur)));
    }

    private float rankAccurate(NALTask x) {
        return (float)eviInteg(x, 0, false);
        //double v = eviInteg(x, (float)(x.conf()/100), false); //use confidence as eternalization, producing a quadratic result
        //double v = c * integral(x, (float)c, false); //use confidence as eternalization, producing a quadratic result
    }

    @Override
    public final float rank(NALTask x, float min) {
        return NAL.answer.RANK_BY_EVI_CURVE ?
            rankAccurate(x) :
            rankFast(x, min);
    }

    public final double eviInteg(NALTask t, float ete) {
        return eviInteg(t, ete, false);
    }

    public final double eviMean(NALTask t, float ete) {
        return eviInteg(t, ete, true);
    }

    /** shift forward or backward in time. returns a new instance unless dt==0 */
    public final EviInterval add(long dt) {
        return dt == 0 ? this : new EviInterval(s + dt, e + dt, dur);
    }

    public final EviInterval addDurs(float n) {
        return n!=0 ? add(Math.round(n * dur)) : this;
    }
}
