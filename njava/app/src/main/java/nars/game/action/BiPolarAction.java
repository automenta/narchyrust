package nars.game.action;

import jcog.Fuzzy;
import jcog.Is;
import jcog.Util;
import jcog.decide.Decide;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.NAL;
import nars.Term;
import nars.Truth;
import nars.game.Game;
import nars.truth.AbstractMutableTruth;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Float.NaN;
import static java.lang.Math.pow;

/**
 * TODO this may actually be better named 'TriPolarAction'
 *
 * integrates and manages a pair of oppositely polarized concepts to determine a fair net motor goal.
 *
 * this actually implements a tri-state controller.  in addition to the two "polarity concepts",
 * a 'zero' or 'neither' virtual state representing both zero (degenerate evidence) and deconstructive
 * interference as a result of enforcing a mutex
 *
 * TODO convert to use the new self-contained async post-update API
 */
public class BiPolarAction extends CompoundAction {

    public Polarization model;
    /**
     * TODO include in 'model'
     */

    private final FloatToFloatFunction motor;

    public BiPolarAction(BooleanToObjectFunction<Term> id, Polarization model, FloatToFloatFunction motor) {
        super(id.valueOf(true), id.valueOf(false));

        this.model = model;
        this.motor = motor;
    }

    public final CompoundActionComponent pos() { return concepts[0];}
    public final CompoundActionComponent neg() {
        return concepts[1];
    }

    /**
     * the pos and neg .update() method should have been called just prior to this since this is
     * invoked by the frame listeners at the end of the NAgent cycle
     */
    @Override
    public void accept(Game g) {

        var w = g.time;
        float x = model.update(
                    pos().goalTruth.ifIs(),
                    neg().goalTruth.ifIs()
                , w.s, w.e);
        float y = Float.isFinite(x) ?
                motor.valueOf(Util.clampSafePolar(x)) //safety filter
                :
                NaN;

        //TODO configurable feedback model

        Truth Nb, Pb;

        if (y == y) {

            y = Util.clampSafe(y, -1, +1);

            float yp, yn;
            if (y >= 0) {
                yp = +y;
                yn = 0;
            } else {
                yn = -y;
                yp = 0;
            }

            Pb = yp == yp ? ((AbstractMutableTruth)truth(yp, g)).immutable() : null;
            Nb = yn == yn ? ((AbstractMutableTruth)truth(yn, g)).immutable() : null;

        } else {
            Pb = Nb = null;
        }

        //System.out.println(Pb + "\t" + Nb);
        set(g, Pb, Nb);
    }

    @Override
    public int size() {
        return 2;
    }


    /**
     * model for computing the net result from the current truth inputs
     */
    @FunctionalInterface
    @Is("Metastability") public interface Polarization {

        /** if false, then goal confidence also factors into the pole strength */
        boolean freqOrExp_Default =
            true;
            //false;

        /**
         * produce a value in -1..+1 range, or NaN if undetermined
         */
        float update(@Nullable Truth pos, @Nullable Truth neg, long prev, long now);

        default /* final */ double q(Truth t) {
            return q(t, freqOrExp_Default);
        }

        default /* final */ double q(Truth t, float ifNaN) {
            return q(t, freqOrExp_Default, ifNaN);
        }

        default /* final */ double q(Truth t, boolean freqOrExp, float ifNaN) {
            double q = q(t, freqOrExp);
            return q==q ? q : ifNaN;
        }

        /**
         * "Q" desire/value function. produces the scalar summary of the goal truth desire that will be
         * used in the difference comparison. return NaN or value
         */
        default /* final */ double q(Truth t, boolean freqOrExp) {

            return t != null ? q(freqOrExp ? t.freq() : t.expectation()) : NaN;
            //return q == q ? (q - 0.5f) * 2 : q;
            //return t != null ? ((freqOrExp ? t.freq() : t.expectation()) - 0.5f)*2 : Float.NaN;
            //return t != null ? ((freqOrExp ? (t.freq()>=0.5f ? t.freq() : 0) : t.expectation()) ) : Float.NaN;
        }

        default double q(double x) {
            return x;
        }

//        default Polarization momentum(double v) {
//            return new Polarization() {
//                final Ewma f = new Ewma(v);
//
//                @Override
//                public float update(@Nullable Truth pos, @Nullable Truth neg, long prev, long now) {
//                    float x = Polarization.this.update(pos, neg, prev, now);
//                    return (float) f.acceptAndGetMean(x);
//                }
//            };
//        }
    }


//    /** TODO SoftMax or other prefilter, and optional momentum parameter */
//    public static class PWMThresh implements Polarization {
//
//        float threshold =
//            //Float.MIN_NORMAL;
//            0.5f;
//
//        final RandomBits rng = new RandomBits(new XorShift128PlusRandom());
//
//        @Override
//        public float update(@Nullable Truth pos, @Nullable Truth neg, long prev, long now) {
//            float p = q(pos, 0), n = q(neg, 0);
//
//            final float t = this.threshold;
//            final boolean P = p > t + NAL.truth.TRUTH_EPSILON/2;
//            final boolean N = n > t + NAL.truth.TRUTH_EPSILON/2;
//            if (P || N) {
//                final float pn = Math.max(p, n) - t;
//                if (!rng.nextBoolean(pn / (1 - t)))
//                    return 0;
//            }
//
//            if (P && N) {
//                p -= t; n -= t;
//                return (rng.nextBoolean(p / (p + n)) ? +1 : -1);
//            }
//            else if (P) return +1;
//            else if (N) return -1;
//            else return 0;
//        }
//    }
    public static class PWM implements Polarization {

        private final Random rng;

        public PWM() {
            this(
                new XoRoShiRo128PlusRandom()
                //new DecideRoulette(new XoRoShiRo128PlusRandom())
                //new DecideSoftmax(0.2f, new XoRoShiRo128PlusRandom())
            );
        }

        /** values > 1 help to denoise the generated motor activity by increasing polarization through higher action threshold? */
        private final float exp =
            //1;
            1.5f;
            //2;
            //0.5f;

        public PWM(Random rng) {
            this.rng = rng;
        }

        @Override
        public float update(@Nullable Truth pos, @Nullable Truth neg, long prev, long now) {
            double p = q(pos, 0), n = q(neg, 0);

//            p = (p - 0.5f) * 2;
//            n = (n - 0.5f) * 2;

            boolean P = curve(rng.nextFloat()) < p;
            boolean N = curve(rng.nextFloat()) < n;
                 if (P && !N) return +1;
            else if (N && !P) return -1;
            else              return 0;
        }

        protected double curve(float thresh) {
            return pow(thresh, 1/exp);
        }

//        @Override
//        public float update(@Nullable Truth pos, @Nullable Truth neg, long prev, long now) {
//            double p = q(pos, freqOrExp, 0), n = q(neg, freqOrExp,0);
//
//            double z = deadzone * ((1 - abs(p-n)) + (1-Math.max(p,n)));
//            //double z = (1-p) + (1-n);
//            //double z = 2 * (1 - abs(p-n));
////            double z = Math.max(p,n)*(1 - abs(p-n));
//            //double z = 2 * p * n;
//            //double z = 2 * ((1 - Math.max(p, n)) + (p*n));
//
////            double z = (Math.max(0.5 - p0, 0) + Math.max(0.5 - n0, 0)
////                    //+ (p0*n0)
////            )/2;
//            //double z = Math.max(0.5 - Math.max(p,n), 0) + (p*n);
////            double z = Math.max(0.5 - p0, 0) + Math.max(0.5 - n0, 0) + (p*n);
//
////            System.out.println(n2(p) + " " + n2(z) + " " + n2(n));
//            return switch (decide.applyAsInt(p,z,n)) {
//                case 0 -> +1;
//                case 1 -> 0;
//                case 2 -> -1;
//                default -> Float.NaN;
//            };
//        }


    }


    /**
     * offers a few parameters
     */
    public static class DecidePolarization implements Polarization {

        private final Random rng = new XoRoShiRo128PlusRandom();

        final Decide decide =
            Decide.Greedy;
            //new DecideRoulette(rng); //FAIR
            //new DecideSoftmax(0.05f, rng);

        private double pqPrev = 0.5, nqPrev = 0.5;
        private final boolean latchIfMissing = false;

        @Override public float update(Truth pos, Truth neg, long prev, long now) {

            boolean freqOrExp = true;

            double pq = q(pos, freqOrExp), nq = q(neg, freqOrExp);
            if (latchIfMissing) {
                if (pq!=pq) pq = pqPrev;
                if (nq!=nq) nq = nqPrev;
                this.pqPrev = pq; this.nqPrev = nq;
            }

            float neither = (float) (
                2 * Fuzzy.and(pq, nq) //TODO 'sensitivity' parameter
                //Math.sqrt(Fuzzy.and(pq, nq))
            );

            return (float) switch (decide.applyAsInt(pq, neither, nq)) {
                case 0 -> +pq; //p
                case 1 ->  0;  //neither
                case 2 -> -nq; //n

                case -1 -> 0; //neither

                default -> throw new UnsupportedOperationException();
            };
        }

    }

    
//    public static class AnalogFade implements Polarization {
//        float deadZone = 1;
//        public final FloatMeanEwma P,N;
//
//        public AnalogFade(float rise, float fall) {
//            P = new FloatMeanEwma(rise, fall);
//            N = new FloatMeanEwma(rise, fall);
//        }
//
//        @Override
//        public float update(@Nullable Truth pos, @Nullable Truth neg, long prev, long now) {
//            double p = q(pos, 0), n = q(neg, 0);
//            P.accept(0); N.accept(0);
//            p = P.acceptAndGetMean(p);
//            n = N.acceptAndGetMean(n);
//            return (float) ((p - n) * (1 - deadZone * (p*n)/2));
//        }
//    }

//    public static class XOR implements Polarization {
//        @Override
//        public float update(@Nullable Truth pos, @Nullable Truth neg, long prev, long now) {
//            double p = q(pos, 0), n = q(neg, 0);
//
////            double pAndNotN = d(p * (1-n));
////            double nAndNotP = d(n * (1-p));
////            return (float) (pAndNotN - nAndNotP);
//            double pAndNotN = (p * (1-n));
//            double nAndNotP = (n * (1-p));
//            double d = (pAndNotN - nAndNotP);
//            d = d >= 0 ? d(d) : -d(-d);
//            return (float)d;
//        }
//
//        /** post-processing filter */
//        protected double d(double y) {
//            return y;
//        }
//    }

    public static class Greedy implements Polarization {
        @Override
        public float update(@Nullable Truth pos, @Nullable Truth neg, long prev, long now) {
            double p = q(pos, 0), n = q(neg, 0);

            boolean pn = Util.equals(p,n,NAL.truth.FREQ_EPSILON) ?
                    ThreadLocalRandom.current().nextBoolean() :
                    p > n;

            return (float) (pn ? p : -n);
        }
    }


//    public static class PowerXOR extends BiPolarAction.XOR {
//
//        final double exp;
//
//        public PowerXOR(double exp) {
//            this.exp = exp;
//        }
//
//        @Override
//        public double d(double x) {
//            return pow(x, exp);
//        }
//    }


    public static class Analog implements Polarization {

        @Override
        public float update(@Nullable Truth pos, @Nullable Truth neg, long prev, long now) {
            return (float)pn(q(pos, 0), q(neg, 0));
        }

        private double pn(double p, double n) {
            return p - n;
            //            float deadZone =
            //                    //0.5f;
            //                    //1;
            //                    2;
            //return (p - n) / (1 + deadZone * (p*n));
            //return (p - n) * (1 - deadZone * (p * n));
        }
    }
//    public static class SimplePolarization implements Polarization {
//
//        private static final float freqThresh = NAL.BUTTON_THRESHOLD_DEFAULT;
//
//        /** override threshold: lower values are more hysterical */
//        private static final float diffThresh =
//            //1/2f;
//            //1/3f;
//            1/4f;
//            //1/5f;
//
//        @Override
//        public float update(@Nullable Truth pos, @Nullable Truth neg, long prev, long now) {
//            float pf = NaN, nf = NaN;
//            boolean P = pos != null && (pf = pos.freq()) > freqThresh;
//            boolean N = neg != null && (nf = neg.freq()) > freqThresh;
//
//            if (P && N) {
//                switch (decide(pos, neg)) {
//                    case +1 -> N = false;
//                    case -1 -> P = false;
//                    default -> P = N = false;
//                }
//                //assert(!(P&&N));
//            }
//
//            if (P)      return +pf;
//            else if (N) return -nf;
//            else        return  0;
//        }
//
//        private int decide(Truth pos, Truth neg) {
//            return decideFreqAbs(pos.freq(), neg.freq(), diffThresh);
//        }
//
//        private static int decideFreqAbs(float pf, float nf, float diff) {
//            if (pf >= nf + diff) return +1;
//            else if (nf >= pf + diff) return -1;
//            else return 0;
//        }
//
//        private static double expectationish(Truth t) {
//            return t.weight();
//        }
//
////        /** metastable, stochastic. TODO more conditions for 0 */
////        private static int decideWeightedRandom(double pe, double ne) {
////            if (pe == ne) return 0;
////            else return ThreadLocalRandom.current().nextFloat() <= (pe/(pe+ne)) ? +1 : -1;
////        }
//
//        private static int decideDiffPct(double p, double n, float diffPct) {
//            if (Util.pctDiff(p, n) < diffPct) return 0;
//            else if (p > n) return +1;
//            else return -1;
//        }
//
//        private static int decideDiffRatio(double p, double n, float diffPct) {
//            float thresh = 1 + diffPct;
//            if (p / n > thresh) return +1;
//            if (n / p > thresh) return -1;
//            return 0;
//        }
//
//        private static int decideGreedy(double pe, double ne, float eviDiffThresh) {
//            if (Util.pctDiff(pe, ne) > eviDiffThresh) {
//                if (pe > ne)
//                    return +1;
//                else if (ne > pe)
//                    return -1;
//            }
//            return 0;
//        }
//    }
}