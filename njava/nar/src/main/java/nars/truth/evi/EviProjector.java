package nars.truth.evi;

import jcog.Is;
import jcog.Research;
import jcog.TODO;
import jcog.Util;
import jcog.math.Intervals;
import jcog.math.LongInterval;
import nars.NAL;

import static java.lang.Math.*;
import static jcog.Util.lerpSafe;
import static jcog.Util.sqr;
import static nars.Op.ETERNAL;

/**
 * absolute temporal projection functions
 * <p>
 * computes the projected evidence at a specific distance (dt) from a perceptual moment evidence
 * with a perceptual duration used as a time constant
 * dt >= 0
 *
 * @param dt  > 0
 * @param dur > 0
 *            <p>
 *            evi(baseEvidence, dt, dur)
 *            many functions will work here, provided:
 *            <p>
 *            evidential limit
 *            integral(evi(x, 0, d), evi(x, infinity, d)) is FINITE (convergent integral for t>=0)
 *            <p>
 *            temporal identity; no temporal difference,
 *            evi(x, 0, d) = 1
 *            <p>
 *            no duration, point-like
 *            evi(x, v, 0) = 0
 *            <p>
 *            monotonically decreasing
 *            for A >= B: evi(x, A, d) >= evi(x, B, d)
 *            since dt>=0, dur
 *            <p>
 *            see:
 *            https://en.wikipedia.org/wiki/List_of_definite_integrals
 *            https://en.wikipedia.org/wiki/Template:Series_(mathematics)
 *
 *            https://www.toolfk.com/online-plotter-frame#W3sidHlwZSI6MCwiZXEiOiIxLygxK3heMC41KSIsImNvbG9yIjoiI0IzMDAwMCJ9LHsidHlwZSI6MCwiZXEiOiIxLygxK3gpIiwiY29sb3IiOiIjNDdBQjAwIn0seyJ0eXBlIjowLCJlcSI6IjEvKDEreCp4KSIsImNvbG9yIjoiIzAwNzdGRiJ9LHsidHlwZSI6MCwiZXEiOiJlXigteCkiLCJjb2xvciI6IiNGRjAwODgifSx7InR5cGUiOjAsImVxIjoiMS8oMSt4KV4yIiwiY29sb3IiOiIjOTUwMEZGIn0seyJ0eXBlIjoxMDAwLCJ3aW5kb3ciOlsiMCIsIjE2Ljc1OTYxNTM4NDYxNTM3NiIsIi0wLjEzMjgxMjQ5OTk5OTk5OTc4IiwiMSJdLCJzaXplIjpbNjQ5LDM5OV19XQ--
 */
@Research @Is("Forgetting_curve") public interface EviProjector {

    static double integrate(LongInterval t, long qs, long qe, float dur, float ete, boolean meanOrSum) {
        return NAL.evi.project.integral(qs, qe, dur, t, ete, meanOrSum);
    }

    /**
     * project a point (1 time unit)
     */
    double project(long dt, float dur);

    double integDiff(long t, float dur);


    private double integral(long qs, long qe, float dur, LongInterval t, float ete, boolean meanOrSum) {
        double x = integral(qs, qe, dur, t);
        long range = 1 + qe - qs;
        double sum = ete > 0 ? lerpSafe(ete, x, range) : x;
        return meanOrSum ? sum / range : sum;
    }

    /** computes the integral factor,
     *  result < 0..1 x (e-s+1)
     *  for a factor to evi() */
    private double integral(long qs, long qe, float dur, LongInterval t) {
//        if (NAL.evi.DUR_SCALING > 0)
//            dur *= (float)Math.pow(t.range(), NAL.evi.DUR_SCALING);
        return qs == qe ?
                integralPoint(qs, dur, t) :
                integralRange(qs, qe, dur, t);
    }

    private double integralPoint(long q, float dur, LongInterval t) {
        long dt;
        long ts = t.start();
        return q == ETERNAL || ts == ETERNAL || (dt = Intervals.diffSep(q, ts, t.end())) <= 0 ?
                1 : dur <= 0 ? 0 : project(dt, dur);
    }

    private double integralRange(long qs, long qe, float dur, LongInterval t) {
        long ts = t.start(), te;
        return (ts == ETERNAL || (qe == (te = t.end()) && qs == ts)) ?
                qe - qs + 1 //shortcut, assumes 1.0 internally
                :
                integ(qs, qe, ts, te, dur);
    }

    /**
     * integrate range (>1 time unit)
     */
    private double integ(long qs, long qe, long ts, long te, float dur) {
        //assert(qs!=ETERNAL && ts!=ETERNAL && qs!=TIMELESS && ts != TIMELESS);

        long intersectLen = min(qe, te) - max(qs, ts);
        double i = intersectLen >= 0 ? intersectLen + 1 : 0;
        if (dur <= 0)
            return i;

        long beforeStart = min(qe, ts), beforeEnd = min(qs, ts);
        double j = beforeStart >= beforeEnd ?
            integ(ts - beforeStart, ts - beforeEnd, dur) :
            0;

        long afterStart = max(qs, te), afterEnd = max(qe, te);
        if (afterStart < afterEnd)
            j += integ(afterStart - te, afterEnd - te, dur);

        return Util.fma(j, integFactor(dur), i);
    }

    default double integ(long a, long b, float dur) {
        return integDiff(b, dur) - integDiff(a, dur);
    }

    default double integFactor(float dur) {
        return 1;
    }

    /**
     * 1/(1+(x/dur)^0.5)
     */
    EviProjector InverseSqrt = new EviProjector() {
        @Override
        public double project(long dt, float dur) {
            return 1 / (1 + sqrt(((double) dt) / dur));
        }


        /**   2(sqrt(d)−ln(sqrt(d)+1)) from https://www.integral-calculator.com/ */
        @Override
        public double integDiff(long t, float dur) {
            double sqrtD = sqrt(t/dur);
            return 2*(sqrtD - Util.log1p(sqrtD));
        }
    };



//    /** 1/(1+log(1+x))
//     *  http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLygxK2xvZygxK3gpKSIsImNvbG9yIjoiI0Q0MDAwMCJ9LHsidHlwZSI6MCwiZXEiOiIxLygxK3gpIiwiY29sb3IiOiIjMDAwMDAwIn0seyJ0eXBlIjowLCJlcSI6IjEvKDEreCp4KSIsImNvbG9yIjoiIzAwRkYzQyJ9LHsidHlwZSI6MTAwMCwid2luZG93IjpbIjAiLCIxMiIsIjAiLCIxIl19XQ--
//     *  doesn't have a cheap definite integral
//     * */
//    class InverseLog implements EviProjector {
//
//        static final float scale = 1;
//
//        @Override
//        public double integDiff(long t, float dur) {
//            //e−1(E1(−ln(a+1)−1)−E1(−ln(b+1)−1))
//            throw new TODO();
//        }
//
//        @Override
//        public double project(long dt, float dur) {
//            return 1 / (1 + log(1 + ((double) dt) / (scale * dur)));
//        }
//
//    }

    /**
     * 1/(1+(t^2)/dur) inverse quadratic decay: integral finite from to infinity, see: https://en.wikipedia.org/wiki/List_of_definite_integrals
     *
     * https://www.symbolab.com/solver/indefinite-integral-calculator/%5Cint_%7Ba%7D%5E%7Bb%7D%20%5Cfrac%7B1%7D%7B%5Cleft(1%2B%5Cleft(%5Cfrac%7Bx%5E%7B2%7D%7D%7Bd%5Ccdot%20s%7D%5Cright)%5Cright)%7Ddx?or=input
     *
     * TODO seems to be wrong, debug
     * TODO add 'dur' scale factor
     */
    class InverseSquareInner2 implements EviProjector {

        @Override
        public double project(long dt, float dur) {
            return 1 / (1 + sqr(dt) / dur);
        }

        @Override
        public double integDiff(long t, float dur) {
            return atan2(t, sqrt(dur));
            //return atan(t / sqrt(dur));
        }

        @Override
        public double integFactor(float dur) {
            return sqrt(dur);
        }
    }

    /**
     *  Inverse Polynomial: (1 + t/dur)^-n, n=2
     *  @param scale dur scale factor
     *  http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLygxK3gpXjIiLCJjb2xvciI6IiNGRjAwMDAifSx7InR5cGUiOjAsImVxIjoiMS8oMSt4XjIpIiwiY29sb3IiOiIjQkZCRkJGIn0seyJ0eXBlIjowLCJlcSI6IjEvKDEreC8yKV4yIiwiY29sb3IiOiIjRkY4NDAwIn0seyJ0eXBlIjoxMDAwLCJ3aW5kb3ciOlsiLTAuODU4MjU5NDgxMjQ5OTk5MSIsIjQuNDY2NTQwNTE4NzQ5OTkyIiwiLTAuODkwNjY2Mjc3NDk5OTk5OCIsIjIuMzg2MTMzNzIyNDk5OTk2Il19XQ--
     *  https://www.symbolab.com/solver/indefinite-integral-calculator/%5Cint_%7Ba%7D%5E%7Bb%7D%20%5Cleft(1%2B%5Cfrac%7Bx%7D%7Bd%5Ccdot%20s%7D%5Cright)%5E%7B-2%7Ddx?or=input
     *
     *  integral: d*s*(-d*a*s + d*b*s)/( (d*s+b)*(d*s+a) )
     *            = (d*s)^2 * (b-a)/((a+d*s)*(b+d*s))
     * */
    record InverseSquare(double scale) implements EviProjector {

        @Override public double project(long dt, float dur) {
            return pow(1 + dt / (dur * scale), -2);
            //return 1 / sqr(1 + dt / (dur * scale));
        }

        @Override
        public double integ(long a, long b, float dur) {
            double D = dur * scale;
            return (b - a) / ((a + D) * (b + D));
        }

        @Override
        public double integFactor(float dur) {
            return sqr(dur * scale);
        }

        @Override
        public double integDiff(long t, float d) {
            throw new UnsupportedOperationException();
        }

    }

    /*
        TODO InverseSquareSqrt: 1/(1 + sqrt(x) )^2 = (1+x^0.5)^-2
            http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLygxK3gpXjIiLCJjb2xvciI6IiMwMDI2RkYifSx7InR5cGUiOjAsImVxIjoiMS8oMStzcXJ0KHgpKV4yIiwiY29sb3IiOiIjMDAwMDAwIn0seyJ0eXBlIjoxMDAwLCJ3aW5kb3ciOlsiLTAuMTc0MDgwMDAwMDAwMDAwMzUiLCI2LjQ4MTkyIiwiLTEuODI3ODM5OTk5OTk5OTk5NyIsIjIuMjY4MTU5OTk5OTk5OTk5NSJdfV0-
            https://www.wolframalpha.com/input?i=definite+integrate+%281%2B+sqrt%28x%2F%28d*s%29%29%29%5E-2+from+a+to+b

            doesnt seem computationally cheap: https://www.symbolab.com/solver/step-by-step/%5Cint_%7Ba%7D%5E%7Bb%7D%5Cleft(1%2B%20%5Csqrt%7Bx%7D%5Cright)%5E%7B-2%7D?or=input
     */

     /*
        TODO InverseSquareLog: 1/(1 + ln(1+x))^2
            http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLygxK3gpXjIiLCJjb2xvciI6IiMwMDI2RkYifSx7InR5cGUiOjAsImVxIjoiMS8oMStsbigxK3gpKV4yIiwiY29sb3IiOiIjRDkwMzAzIn0seyJ0eXBlIjoxMDAwLCJ3aW5kb3ciOlsiLTAuMTc0MDgwMDAwMDAwMDAwMzUiLCI2LjQ4MTkyIiwiLTEuODI3ODM5OTk5OTk5OTk5NyIsIjIuMjY4MTU5OTk5OTk5OTk5NSJdfV0-

            definite integrate (1+ log (1 + x/(d*s)))^-2 from a to b
            no (easy) solution.
     */

    /**
     * "Rational" Decay
     * 1/(1+(t/dur)^2) inverse quadratic decay: integral finite from to infinity, see: https://en.wikipedia.org/wiki/List_of_definite_integrals
     *
     * https://www.symbolab.com/solver/indefinite-integral-calculator/%5Cint_%7Ba%7D%5E%7Bb%7D%20%5Cfrac%7B1%7D%7B%5Cleft(1%2B%5Cleft(%5Cfrac%7Bx%7D%7Bd%5Ccdot%20s%7D%5Cright)%5E%7B2%7D%5Cright)%7Ddx
     * https://www.symbolab.com/solver/indefinite-integral-calculator/%5Cint_%7Ba%7D%5E%7Bb%7D%20%5Cfrac%7B1%7D%7B%5Cleft(1%2B%5Cleft(%5Cfrac%7Bx%7D%7Bd%7D%5Cright)%5E%7B2%7D%5Cright)%7Ddx
     * https://www.wolframalpha.com/input/?i=integral+1%2F%281+%2B+%28t%2Fd%29%5E2%29+from+a+to+b
     *
     */
    record InverseSquareInner(float durScale) implements EviProjector {

        @Override
        public double project(long dt, float dur) {
            double d = ((double) dt) / (dur * durScale);
            return 1 / Util.fma(d, d, 1);
        }


        /**
         * integral 1/(1 + (x/d)^2) from a to b
         * d * s * (atan(d/a) - atan(d/b)), 0 < Re[a] < b && a == Re[a]
         * <p>
         * https://www.wolframalpha.com/input/?i=atan2%28x%2C0%29  simplification for dur=0
         *
         * TODO double check this for scale!=1
         */
        @Override
        public double integDiff(long t, float dur) {
            return atan2(t, dur * durScale);
            //return atan(t / (dur * scale));
        }

        @Override
        public double integFactor(float dur) {
            return dur * durScale;
        }
    }

    /** linear: slope = -evi / dur
     * @param max <=1, >0
     * @param slope <=1, >0
     * */
    record Linear(double max, double slope) implements EviProjector {

        @Override
        public double integDiff(long t, float dur) {
            throw new TODO();
        }

        @Override
        public double project(long dt, float dur) {
            return Math.max(0, max - slope * dur);
        }
    }

    /**
     * Hyperbolic Decay
     * 1/(1+x/dur))
     * @param scale dur scale factor; < 1 to extend decay time
     * https://www.symbolab.com/solver/indefinite-integral-calculator/%5Cint_%7Ba%7D%5E%7Bb%7D%20%5Cfrac%7B1%7D%7B%5Cleft(1%2B%5Cfrac%7Bx%7D%7Bd%5Ccdot%20s%7D%5Cright)%7Ddx
     * https://www.symbolab.com/solver/indefinite-integral-calculator/%5Cint_%7Ba%7D%5E%7Bb%7D%20%5Cfrac%7B1%7D%7B%5Cleft(1%2B%5Cfrac%7Bx%7D%7Bd%7D%5Cright)%7Ddx
     */
    record InverseLinear(double scale) implements EviProjector {

        @Override
        public double project(long dt, float dur) {
            return 1 / (1 + dt / (dur * scale));
        }

        /**
         * integral 1/(1 + (x/d)) from a to b
         *
         * TODO check this for scale!=1
         */
        @Override
        public double integDiff(long t, float dur) {
            double D = dur * scale;
            return log((D + t) / D);
        }

        @Override
        public double integFactor(float dur) {
            return dur * scale;
        }
    }

    /**
     * Math.exp(-(dt/dur)) warning: not finite definitely integrable
     */
    record InverseExponential(double durScale) implements EviProjector {

        @Override
        public double project(long dt, float dur) {
            return exp(-dt / (durScale * dur));
        }

        /**
         * https://www.symbolab.com/solver/indefinite-integral-calculator/%5Cint_%7Ba%7D%5E%7Bb%7D%20e%5E%7B-%5Cleft(%5Cfrac%7Bx%7D%7Bd%7D%5Cright)%5E%7B1%7D%7Ddx
         */
        @Override
        public double integDiff(long t, float dur) {
            double nDUR = -(durScale * dur);
            return nDUR * exp(t / nDUR);
        }
    }

    /** Shifted Power Law
     *  f(x,d): (x/d+1)^-0.5
     *  integral(x, 0, t): 2 * d * ((t/d + 1)^0.5 - 1)
     *
     *  https://www.toolfk.com/online-plotter-frame#W3sidHlwZSI6MCwiZXEiOiJNYXRoLnBvdygoMSt4KSwtMC41KSIsImNvbG9yIjoiI0IxMEJEQiJ9LHsidHlwZSI6MCwiZXEiOiJNYXRoLnBvdygoMSt4KSwtMC4yNSkiLCJjb2xvciI6IiMwMjM0QkQifSx7InR5cGUiOjAsImVxIjoiTWF0aC5wb3coKDEreCksLTAuNzUpIiwiY29sb3IiOiIjMDBEMTU0In0seyJ0eXBlIjowLCJlcSI6Ik1hdGgucG93KCgxK3gpLC0xKSIsImNvbG9yIjoiIzAwMDAwMCJ9LHsidHlwZSI6MTAwMCwid2luZG93IjpbIjAiLCIxNS41NTk3MzA3NjkyMzA3NjkiLCItMC43NjY0NjE1Mzg0NjE1Mzg3IiwiMy4wMDI3NjkyMzA3NjkyMzA2Il0sInNpemUiOls2NDksMzk5XX1d
     */
    record PowerLaw(double scale) implements EviProjector {

        @Override
        public double project(long dt, float dur) {
            return pow(dt/(dur * scale)+1, -0.5);
        }

        @Override
        public double integDiff(long t, float dur) {
            return sqrt(t / (dur * scale) + 1);
        }

        @Override
        public double integFactor(float dur) {
            return 2 * dur * scale;
        }
    }

//            //cubic decay:
//            //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLTEvKDErZV4oLXgpKSIsImNvbG9yIjoiIzAwNzdGRiJ9LHsidHlwZSI6MCwiZXEiOiIxLygxK3gqeCkiLCJjb2xvciI6IiNENDFBMUEifSx7InR5cGUiOjAsImVxIjoiMS8oMSt4KngqeCkiLCJjb2xvciI6IiM4OUFEMDkifSx7InR5cGUiOjEwMDAsIndpbmRvdyI6WyIwIiwiMTgiLCIwIiwiMSJdfV0-
//            //double e = (evi / (1.0 + Util.cube(((double)dt) / dur)));
//
//            //constant duration linear decay ("trapezoidal")
//            //e = (float) (evi * Math.max(0, (1.0 - dt / dur)));
//
//            //max(constant duration, cubic floor)
//            //e = (float) (evi * Math.max((1.0 - dt / dur), 1.0/(1.0 + Util.cube(((double)dt) / dur))));
//
//            //exponential decay: see https://en.wikipedia.org/wiki/Exponential_integral
//            //TODO
//
//            //constant duration quadratic decay (sharp falloff)
//            //e = evi * Math.max(0, (float) (1.0 - Math.sqrt(dt / decayTime)));
//
//            //constant duration quadratic discharge (slow falloff)
//            //e = evi * Math.max(0, 1.0 - Util.sqr(dt / decayTime));
//
//            //---------
//
//            //eternal noise floor (post-filter)
//            //e = ee + ((e - ee) / (1.0 + (((float)dt) / (falloffDurs * dur))));
//
//            return e;
//
//            //return evi / (1.0f +    Util.sqr(((float)dt) / (falloffDurs * dur)));
//            //return evi / (1.0f +    Util.sqr(((float)dt) / dur)/falloffDurs);
//
//
//            //return evi / (1.0f + ( Math.max(0,(dt-dur/2f)) / (dur)));
//
//            //return evi / (1.0f + ( Math.max(0f,(dt-dur)) / (dur)));
//
//
//            //return evi * 1/sqrt(Math.log(1+(Math.pow((dt/dur),3)*2))*(dt/dur)+1); //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLyhsb2coMSsoeCp4KngqMikpKih4KSsxKV4wLjUiLCJjb2xvciI6IiMyMTE1QUIifSx7InR5cGUiOjAsImVxIjoiMS8oMSt4KSIsImNvbG9yIjoiIzAwMDAwMCJ9LHsidHlwZSI6MTAwMCwid2luZG93IjpbIi0xLjg4NDM2OTA0NzQ3Njc5OTgiLCI4LjUxNTYzMDk1MjUyMzE2OCIsIi0yLjMxMTMwMDA4MTI0NjM4MTgiLCI0LjA4ODY5OTkxODc1MzU5OCJdLCJzaXplIjpbNjQ2LDM5Nl19XQ--
//            //return (float) (evi / (1.0 + Util.sqr(((double)dt) / dur)));
//            //return evi * 1/(Math.log(1+((dt/dur)*0.5))*(dt/dur)+1); //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLyhsb2coMSsoeCowLjUpKSooeCkrMSleMC41IiwiY29sb3IiOiIjMjExNUFCIn0seyJ0eXBlIjowLCJlcSI6IjEvKDEreCkiLCJjb2xvciI6IiMwMDAwMDAifSx7InR5cGUiOjEwMDAsIndpbmRvdyI6WyIyLjYzMDEyOTMyODgxMzU2ODUiLCIxOC44ODAxMjkzMjg4MTM1MzUiLCItMy45NTk4NDE5MDg3NzE5MTgiLCI2LjA0MDE1ODA5MTIyODA1NyJdLCJzaXplIjpbNjQ4LDM5OF19XQ--
//            //return evi * (Util.tanhFast((-(((float)dt)/dur)+2))+1)/2; //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIodGFuaCgteCsyKSsxKS8yIiwiY29sb3IiOiIjMjExNUFCIn0seyJ0eXBlIjowLCJlcSI6IjEvKDEreCkiLCJjb2xvciI6IiMwMDAwMDAifSx7InR5cGUiOjEwMDAsInNpemUiOls2NDgsMzk4XX1d
//
//            //return (float) (evi / (1.0 + Math.log(1 + ((double)dt) / dur)));

}
/*
def inverse_polynomial(t, d=1, n=2):
    return (1 + t / d)**-n

def hyperbolic_decay(t, A=1, k=1):
    return A / (1 + k * t)

def weibull_decay(t, d=1, lam=0.5):
    return np.exp(-(t / d)**lam)

def logarithmic_decay(t, A=1, k=1):
    return A / np.log(1 + k * t)

def gompertz_decay(t, A=1, B=1, k=0.5):
    return A * np.exp(-B * np.exp(-k * t))
 */