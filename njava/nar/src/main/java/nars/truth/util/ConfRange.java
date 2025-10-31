package nars.truth.util;

import jcog.signal.DoubleRange;
import nars.NAL;
import nars.TruthFunctions;

public final class ConfRange extends DoubleRange {

    private double _evi;

    public ConfRange() {
        this(0);
    }

    public ConfRange(float initialValue) {
        super(initialValue, NAL.truth.CONF_MIN, NAL.truth.CONF_MAX);
    }

    /** TODO maybe use NAL conf resolution */
    @Override public void set(double conf) {
        super.set(conf);
        _evi = TruthFunctions.c2e(conf());
    }

    public final void conf(double conf) {
        set((float)conf);
    }

    public final void evi(double e) {
       set(TruthFunctions.e2c(e));
    }

    public final double conf() {
        return doubleValue();
    }

    public final double evi() {
        return _evi;
    }



//    public Truth truth(float freq) {
//        return PreciseTruth.byEvi(freq, evi());
//    }
//
//    /** eternalized evidence */
//    public final double eviEte() {
//        return TruthFunctions.eternalize(evi());
//    }
//
//    /** eternalized conf */
//    public final double confEte() {
//        return w2cSafe(eviEte());
//    }

}