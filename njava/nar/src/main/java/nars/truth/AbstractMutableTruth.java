package nars.truth;

import jcog.Util;
import nars.NAL;
import nars.Truth;
import nars.TruthFunctions;
import org.jetbrains.annotations.Nullable;

import static nars.TruthFunctions.c2e;

public abstract sealed class AbstractMutableTruth extends Truth permits MutableTruth, PlainMutableTruth {

    protected AbstractMutableTruth(float f, double evi) {
        if (f!=f)
            clear();
        else
            evi(evi).freq(f);
    }

    protected AbstractMutableTruth(Truthed t) {
        this(t.freq(), t.evi());
    }

    protected AbstractMutableTruth() {
        this(0.5f, 0);
    }

    protected abstract void _freq(float f);

    protected abstract void _evi(double e);

    @Deprecated public final AbstractMutableTruth freq(double f) {
        return freq((float) f);
    }

    public final AbstractMutableTruth freq(float f) {
        Util.assertUnitized(f);
        _freq(f);
        return this;
    }

    public final AbstractMutableTruth evi(double e) {
        if (!((e >= 0) || (e < NAL.truth.EVI_MAX))) //Util.assertFinite(e);
            throw new TruthException("invalid evi", e);
        _evi(e);
        return this;
    }

    public final AbstractMutableTruth freqRes(float f, float fRes) {
        return freq(freq(f, fRes));
    }

    public AbstractMutableTruth conf(double c) {
        evi(c2e(c));
        return this;
    }

    /**
     * modifies this instance
     */
    public final AbstractMutableTruth negThis() {
        freq(freqNeg());
        return this;
    }

    private void assertIs() {
        if (!is())
            throw new RuntimeException("null truth");
    }

    public final AbstractMutableTruth set(@Nullable Truthed x) {
        return this == x ? this : x != null ? evi(x.evi()).freq(x.freq()) : clear();
    }

    @Override
    public final int hashCode() {
        throw new UnsupportedOperationException();
        //return System.identityHashCode(this);
    }

    public @Nullable Truth clone() {
        return new MutableTruth(freq(), evi());
    }

    public @Nullable Truth immutable() {
        return is() ? immutableUnsafe(false) : null;
    }


    /**
     * sets to an invalid state
     */
    public AbstractMutableTruth clear() {
        evi(0);
        return this;
    }

    /**
     * whether this instance's state is set to a specific value (true), or clear (false)
     */
    public final boolean is() {
        return evi() > 0;
        //Double.MIN_NORMAL;
        //NAL.truth.EVI_MIN;
    }

    @Override
    public String toString() {
        return truthString();
    }

    @Nullable
    public final Truth ifIs() {
        return is() ? this : null;
    }

    public boolean ditherTruth(NAL n, double eviMin) {
        double nextEvi = c2e(conf(conf(), n.confRes.doubleValue()));
        if (nextEvi < eviMin) {
            clear();
            return false;
        } else {
            evi(nextEvi).freq(freq(freq(), n.freqRes.floatValue()));
            return true;
        }
    }

//    public AbstractMutableTruth setLerp(Truth x, float momentum) {
//        if (this == x) return this;
//        if (momentum <= 0 || !is())
//            set(x);
//        else {
//            freqLerp(x.freq(), momentum);
//            confLerp(x.conf(), momentum);
//        }
//        return this;
//    }
//
//    private void freqLerp(float freq, float momentum) {
//        freq(Util.lerp(momentum, freq(), freq));
//    }
//
//    private void confLerp(double conf, float momentum) {
//        conf(Util.lerp(momentum, conf(), conf));
//    }

    @Override public @Nullable Truth cloneEviMult(double eFactor, double eviMin) {
        if (eFactor == 1) return this;
        var ee = evi() * eFactor;
        return ee >= eviMin ? evi(ee) : null;
    }

    public final AbstractMutableTruth weak() {
        return conf(TruthFunctions.weak(conf()));
    }

    public PreciseTruth immutableUnsafe(boolean neg) {
        return PreciseTruth.byEvi(freqPN(freq(), neg), evi());
    }

}
