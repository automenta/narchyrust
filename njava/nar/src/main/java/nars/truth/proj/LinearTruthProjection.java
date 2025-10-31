package nars.truth.proj;

import jcog.Fuzzy;
import jcog.Util;
import jcog.data.set.MetalLongSet;
import nars.NAL;
import nars.NALTask;
import nars.Truth;
import nars.truth.PreciseTruth;
import nars.truth.TruthCurve;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static jcog.Util.fma;
import static nars.TruthFunctions.c2e;
import static nars.TruthFunctions.e2c;

public abstract class LinearTruthProjection extends MutableTruthProjection {

    protected LinearTruthProjection(long start, long end) {
        super(start, end);
    }

    protected LinearTruthProjection(long start, long end, int capacity) {
        super(start, end, capacity);
    }

    protected LinearTruthProjection(int capacity) {
        super(capacity);
    }

    @Override
    @Nullable
    public final Truth computeTruth() {
        var n = size;
        long start = this.start(), end = this.end();
        var eSum = Util.sum(evi, 0, n);
        var range = 1 + (end - start);
        var eMean = eSum / range;

        if (eMean < eviMin) {
//            if (NAL.truth.EVI_STRICT)
            return null;
//            else {
//                /* round up: */
//                E = eviMin;
//            }
        }

        var _F = freq(start, end, eSum);
        if (_F!=_F)
            return null;
        var F = Util.round((float)_F, freqRes);

        var E = switch (NAL.truth.eviMerge) {
          case Sum -> eMean;
          case SumDoubtVariance -> sumDoubtVariance(n, F, eMean);
          //case Mean -> eSum / n;
          //case Max -> Util.max(evi, 0, n);
          //case Min -> Util.min(evi, 0, n);
        };
        return E <= 0 ? null : computeTruth(F, E, start, end, curve && n > 1);
    }

    private @Nullable Truth computeTruth(float F, double E, long start, long end, boolean curve) {
        if (confRes!=0)
            E = c2e(Util.round(e2c(E), confRes));

        if (curve) {
            var tc = curve(start, end, 0, NAL.truth.REVISE_CURVE_CAPACITY);
            if (tc!=null)
                return tc;
        }

        return PreciseTruth.byEvi(F, E); //flat
    }

    @Nullable private TruthCurve curve(long start, long end, float dur, int nSegments) {
        assert(nSegments > 2);
        if (end - start < 2)
            return null;

        var c =
                spansAdaptive(start, end, dur, nSegments);
                //spansUniform(start, end, dur, nSegments);
        return c!=null && c.size() >= 2 && c.freqRange() >= freqRes ? c : null;
    }

    /** N equally sized segments */
    @Nullable private TruthCurve spansUniform(long start, long end, float dur, int n) {
        final boolean allowHoles = true;

        n = (int)Math.min(end-start, n);
        var y = new TruthCurve(n);
        var r = (end - start)/n;
        long segStart = start, segEnd;
        for (int i = 0; i < n; i++) {
            //stretch to end?
            segEnd = i == n - 1 ? end : Math.min(end, segStart + r);
            if (!fDyn(y, segStart, segEnd, dur)) {
                if (!allowHoles) return null;
            }
            segStart = segEnd + 1;
            if (segStart > end) break;
        }
        return y.size() < 2 ? null : y;  //TODO trim
    }

    private @Nullable TruthCurve spansAdaptive(float dur, long[] spans) {
        final boolean allowHoles = true;
        var n = spans.length;
        var y = new TruthCurve(n - 1);
        var segStart = spans[0];
        for (int i = 1; i < n; i++) {
            var segEnd = spans[i];
            if (!fDyn(y, segStart, segEnd, dur)) {
                if (!allowHoles) return null;
            }
            segStart = segEnd + 1;
        }
        return y.size() < 2 ? null : y; //TODO trim
    }


    private TruthCurve spansAdaptive(long start, long end, float dur, int nSegments) {
        var spans = spansAdaptiveSpans(start, end, nSegments + 1);
        return spans!=null ? spansAdaptive(dur, spans) : null;
    }

    private long[] spansAdaptiveSpans(long start, long end, int n) {
        var s = spansAdaptiveSet(start, end);
        return s.size() < 3 ? null : spansAdaptiveSetReduce(s, (int) Math.min(end - start, n));
    }

    private MetalLongSet spansAdaptiveSet(long start, long end) {
        var thisSize = size;
        var y = new MetalLongSet(thisSize * 2);
        var items = this.items;
        for (int i = 0; i < thisSize; i++)
            add(items[i], y);
        return y;
    }

    /** Reduce the number of points to n by merging closest sequential pairs */
    private static long[] spansAdaptiveSetReduce(MetalLongSet y, int n) {
        var yy = y.toSortedArray();
        var yyn = yy.length;
        if (yyn <= n) return yy;

        while (yyn > n) {
            var indexMin = closestAdjacentPair(yy, yyn);
            yy[indexMin] = Fuzzy.mean(yy[indexMin], yy[indexMin + 1]);
            System.arraycopy(yy, indexMin + 2, yy, indexMin + 1, yyn - indexMin - 2);
            yyn--;
        }

        return Arrays.copyOf(yy, yyn);
    }

    private static int closestAdjacentPair(long[] yy, int n) {
        int indexMin = 0;
        long distMin = yy[1] - yy[0];
        for (int i = 1; i < n - 1; i++) {
            long dist = yy[i + 1] - yy[i];
            if (dist < distMin) {
                distMin = dist;
                indexMin = i;
            }
        }
        return indexMin;
    }

    private static void add(NALTask t, MetalLongSet y) {
        if (t.truth() instanceof TruthCurve nested)
            add(nested, y);
        else {
            var s = t.start();
            if (s != ETERNAL) {
                y.add(s);
                y.add(t.end());
            }
        }
    }

    private static void add(TruthCurve c, MetalLongSet l) {
        c.forEach(p -> {  l.add(p.start); l.add(p.end); });
    }

//    private static void add(NALTask t, UnifiedSet<LongLongPair> y) {
//        if (t.truth() instanceof TruthCurve nested)
//            add(nested, y);
//        else if (t.start() != ETERNAL)
//            y.add(pair(t.start(), t.end()));
//    }
//
//    private static void add(TruthCurve c, Set<LongLongPair> l) {
//        c.forEach(p -> l.add(pair(p.start, p.end)));
//    }

    private double sumDoubtVariance(int n, float F, double E) {
        if (n == 1)
            return E;

        //https://github.com/Hipparchus-Math/hipparchus/blob/master/hipparchus-stat/src/main/java/org/hipparchus/stat/descriptive/moment/Variance.java#L506C1-L506C1
        //evi
        var v = Math.abs(Util.weightedVariance(freqArray(),
                Util.normalizeToSum(evi, 0, n, n)
                //evi
                , 0, n, F));

        //v = Math.sqrt(v);

        /* discount strength */
        var s =
            0.5;
            //1;

        var EE =
            E * (1 - v * s);  //LINEAR
            //E / (1 + v * s); //DIVIDE

        return EE < eviMin ? 0 : EE;
    }

    private double freq(long start, long end, double eSum) {
        return size > 1 ?
            freqN(start, end, eSum) :
            freq1(0, start, end);
    }

    private float[] freqArray() {
        var s = size;
        var freqs = new float[s];
        for (var i = 0; i < s; i++)
            freqs[i] = items[i].freq();
        return freqs;
    }

    private boolean fDyn(TruthCurve c, long start, long end, float dur) {
        var n = size;
        var ii = items;
        double eSum = 0, freqWeightedSum = 0;//, eMin = Double.POSITIVE_INFINITY;
        for (var i = 0; i < n; i++) {
            var ei = ii[i].eviMean(start, end, dur, 0);
            if (ei > 0) {
                double fi = freq1(i, start, end);
                if (fi==fi) {
                    eSum += ei;
                    freqWeightedSum = fma(fi, ei, freqWeightedSum); //freqWeightedSum += e * f;
                }
            }
        }
        return eSum > 0 && fDyn(c, start, end, eSum, freqWeightedSum);
    }

    private boolean fDyn(TruthCurve c, long start, long end, double eSum, double freqWeightedSum) {
        var E = eSum / (end - start + 1);
        if (E < eviMin) return false;

        var F = (float) (freqWeightedSum / eSum);
        c.add(start, end, F, E);
        return true;
    }

    private double freqN(long start, long end, double eSum) {
        var n = size;
        var evi = this.evi;
        double freqWeightedSum = 0;//, eMin = Double.POSITIVE_INFINITY;
        for (var i = 0; i < n; i++) {
            double f = freq1(i, start, end);
            var e = evi[i];
            //e*e; //quadratic
            //Math.exp(e/32)-1; //exponential, like a normalized softmax WARNING: can overflow
            //w2c(e) //conf
            //e *= Util.max(NAL.truth.FREQ_EPSILON,Fuzzy.polarity(f)); /* polarity weighting */
            //e * (0.5f + Math.abs(f-0.5f)); /* polarity partial weighting */
            //e * (1 + (2*Math.abs(f-0.5f))); /* 2:1 compression polarity partial weighting */

            freqWeightedSum = fma(f, e, freqWeightedSum); //freqWeightedSum += e * f;
        }

        return freqWeightedSum / eSum;
    }

    private float freq1(int i, long start, long end) {
        return items[i].freq(start, end);
    }

}
