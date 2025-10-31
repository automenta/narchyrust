package nars.truth.proj;

import jcog.math.LongInterval;
import jcog.util.SingletonIterator;
import nars.NAL;
import nars.NALTask;
import nars.Truth;
import nars.time.Moment;
import nars.time.Tense;
import nars.truth.TruthCurve;
import nars.truth.evi.EviInterval;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

import static nars.truth.PreciseTruth.byEvi;

/**
 * lightweight wrapper around a single task result
 */
public final class SingletonTruthProjection implements TruthProjection {

    private final EviInterval when;
    private NALTask x;
    private double eviMin = NAL.truth.EVI_MIN;

    public static SingletonTruthProjection the(NALTask x, LongInterval when, int ditherDT, float dur) {
        long qs = when.start(), qe = when.end();
        if (qs == EviInterval.TIMELESS || qs == EviInterval.ETERNAL) {
            qs = x.start(); qe = x.end(); //AUTO
        }
        if (ditherDT > 1) {
            qs = Tense.dither(qs, ditherDT);
            qe = Tense.dither(qe, ditherDT);
        }
        return new SingletonTruthProjection(x, qs, qe, dur);
    }

    private SingletonTruthProjection(NALTask x, long qs, long qe, float dur) {
        when = new EviInterval(qs, qe, dur);
        this.x = x;
    }

    public final SingletonTruthProjection eviMin(double eviMin) {
        this.eviMin = eviMin;
        return this;
    }

    @Override public void clear() {
        x = (null);
    }

    @Override
    public Moment when() {
        return when;
    }


    @Override @Nullable public Truth truth() {
        var w = when;
        var xt = x.truth();
        if (w.s == EviInterval.ETERNAL) {
            assert(x.ETERNAL());
            return xt; //TODO eviMin?
        } else {
            double evi = x.eviMean(w, ete());
            return evi < eviMin ? null : truth(xt, w, evi);
        }
    }

    private @Nullable Truth truth(Truth xt, EviInterval E, double evi) {
        return xt instanceof TruthCurve tc ?
            tc.cloneEviMult(evi / xt.evi()) :
            byEvi(xt.freq(), evi);
    }

    @Override
    public long start() {
        return when.start();
    }

    @Override
    public long end() {
        return when.end();
    }

    @Override
    public @Nullable NALTask task() {
        return NALTask.projectAbsolute(x, when.s, when.e, when.dur, 0, eviMin);
    }

    @Override
    public Iterator<NALTask> iterator() {
        return new SingletonIterator<>(x);
    }

    @Override
    public void delete() {
        clear();
    }

}