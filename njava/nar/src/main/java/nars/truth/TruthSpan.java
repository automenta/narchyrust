package nars.truth;

import jcog.math.LongInterval;
import nars.Truth;

public final class TruthSpan implements LongInterval {
    public /*volatile*/ long start, end;
    public final AbstractMutableTruth truth;

    public TruthSpan(long s, long e, float freq, double evi) {
        this(s, e, new PlainMutableTruth(freq, evi));
    }

    public TruthSpan(long s, long e, AbstractMutableTruth truth) {
        this.truth = truth;
        set(s, e);
    }

    @Override
    public long start() {
        return start;
    }

    @Override
    public long end() {
        return end;
    }

    public void set(long s, long e, float freq, double evi) {
        set(s, e);
        truth.freq(freq).evi(evi);
    }

    public void set(long s, long e) {
        this.start = s;
        this.end = e;
    }

    public final float freq() {
        return truth.freq();
    }
    public final double evi() {
        return truth.evi();
    }

    public TruthSpan eviScale(double factor) {
        return new TruthSpan(start, end, freq(), evi() * factor);
    }

    public TruthSpan neg() {
        return new TruthSpan(start, end, 1 - freq(), evi());
    }

    /** creates a copy with modified truth, but keeps times */
    public TruthSpan cloneTruth(Truth t) {
        return new TruthSpan(start, end, t.freq(), t.evi());
    }

    public boolean equals(TruthSpan y, float fRes, double cRes) {
        return y == this || (start == y.start && end == y.end && truth.equals(y.truth, fRes, cRes));
    }
}
