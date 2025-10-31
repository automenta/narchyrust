package nars.truth;

import nars.NAL;
import nars.Truth;
import org.jetbrains.annotations.Nullable;

import static nars.NAL.truth.hash;
import static nars.TruthFunctions.c2e;
import static nars.TruthFunctions.e2c;

/**
 * extends DiscreteTruth's raw hash representation with
 * a freq and evidence float pairs.
 *
 * this allows it to store, internally, more precision than the
 * discrete representation, yet is comparable with DiscreteTruth
 * (according to system TRUTH_EPSILON tolerance).
 *
 * the additional precision is useful for intermediate calculations
 * where premature rounding could snowball into significant error.
 *
 */
public final class PreciseTruth extends Truth {

    private final float f;

    private final double e;

    private final int hash;

    private PreciseTruth(float freq, double ec, boolean eviOrConf) {
        validateFreq(freq);
        this.f = freq;
        if (eviOrConf)
            hash = hash(freq, e2c(this.e = ec));
        else {
            hash = hash(freq, ec);
            this.e = c2e(ec);
        }
        validateEvi(e);
    }

    private static void validateEvi(double evi) {
        if (evi > NAL.truth.EVI_MAX)
            throw new TruthException("evi overflow", evi);
        if (evi < NAL.truth.EVI_MIN)
            throw new TruthException("evi underflow", evi);
//        if (NAL.DEBUG && evi < NAL.truth.EVI_MIN_safe)
//            throw new TruthException("evidence underflow", evi);
    }

    private static void validateFreq(float freq) {
        if (freq!=freq)
            throw new TruthException("NaN freq", freq);
        if (freq < 0 || freq > 1)
            throw new TruthException("invalid freq", freq);
    }

    @Nullable public static PreciseTruth byConf(float freq, double conf) {
        return conf <= NAL.truth.CONF_MIN ? null : new PreciseTruth(freq, conf, false);
        //return byEvi((float)freq, c2e(conf));
    }

    @Nullable static PreciseTruth byEvi(float freq, double evi) {
        return evi < NAL.truth.EVI_MIN ? null : new PreciseTruth(freq, evi, true);
    }

    @Nullable static PreciseTruth byEvi(float f, double e, float freqRes) {
        return byEvi(freq(f, freqRes), e);
    }

    @Deprecated public static PreciseTruth byEvi(double freq, double evi) {
        return /*freq==freq ? */byEvi((float)freq, evi)/* : null*/;
    }

    @Nullable public static PreciseTruth byEvi(float f, double e, NAL nar) {
        return byEvi(f, e, nar.freqRes.floatValue());
    }

    @Override public float freq() { return f; }

    @Override public double evi() { return e; }

//    /** assumes equality has already been tested as true.  sets the evidence to the maximum */
//    public void absorb(Truth o) {
//        this.e = Math.max(e, o.evi());
//    }

    @Override
    public String toString() {
        return truthString();
    }

    @Override public final int hashCode() {
        return hash;
    }
}