package nars.truth;

import jcog.Fuzzy;
import jcog.Util;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAL;
import nars.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * strategy for mapping unit scalar values to truth values
 */
public abstract class Truther {

    @Nullable public final Truth truth(float value) { return truth(value, 0); }

    @Nullable
    public abstract Truth truth(float value, float valueRes);

    /**
     * returns a value in 0..1 of how different the truths are
     */
    public abstract float dist(Truth x, Truth y);

    public static class FreqTruther extends Truther {

        protected final AbstractMutableTruth tShared = new PlainMutableTruth();

        float freqRes;
        //double conf;
//        static final boolean precise = true;

        public FreqTruther() {
            this(0.5);
        }

        public FreqTruther(double conf) {
            conf(conf);
        }

        public FreqTruther freqRes(float f) {
            this.freqRes = f;
            return this;
        }

        public FreqTruther conf(double c) {
            tShared.conf(c);
            //this.conf = c;
            return this;
        }

//        public FreqTruther conf(float c, float confRes) {
//            return conf(Truth.conf(c, confRes));
//        }

        @Override
        public Truth truth(float f, float valueRes) {
            if (f != f) return null;
            //return $.t(f, conf);
            return tShared.freqRes(f, Math.max(this.freqRes, valueRes));
        }

        @Override
        public float dist(Truth x, Truth y) {
            return Math.abs(x.freq() - y.freq());
        }
    }

    /** discounts confidence by lack of frequency polarity */
    public static class PolarTruther extends Truther.FreqTruther {

        /** dynamic range */
        @Deprecated private static final float confMinDivision = 10;

        final FloatSupplier confMax;

        public PolarTruther(Supplier<NAL> nal) {
            this(()->(float)nal.get().beliefConfDefault.conf());
        }

        public PolarTruther(FloatSupplier confMax) {
            this.confMax = confMax;
        }

        protected static float f2c(float x) {
            return (float) Fuzzy.polarity(x);
        }

        @Override
        public @Nullable Truth truth(float f, float valueRes) {
            if (super.truth(f, valueRes)==null) return null;

            double confMax = this.confMax.asFloat();
            double c = Util.lerp(
                f2c(tShared.freq()),
                    Math.max(NAL.truth.CONF_MIN, confMax / confMinDivision),
                confMax
            );
            return tShared.conf(c);
        }
    }


    public static class ConfTruther extends Truther {
        float confRes;
        float freq;

        public ConfTruther(float freq) {
            freq(freq);
        }

        public ConfTruther confRes(float c) {
            this.confRes = c;
            return this;
        }

        public ConfTruther freq(float f) {
            this.freq = f;
            return this;
        }

        public ConfTruther freq(float f, float freqRes) {
            return freq(Truth.freq(f, freqRes));
        }

        @Override
        public Truth truth(float value, float valueRes) {
            return value == value ? $.t(freq, Truth.conf(value, Math.max(valueRes, confRes))) : null;
        }

        /** Expectation-based.  TODO test */
        @Override public float dist(Truth x, Truth y) {
            double xc = x.conf(), yc = y.conf();
            double cRange = Math.abs(xc - yc), cMin = Math.min(xc, yc);
            double xcn = (xc - cMin) / cRange, ycn = (yc - cMin) / cRange;
            return (float) Math.abs(x.freq()*xcn - y.freq()*ycn);
        }

    }
}