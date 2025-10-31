package nars.truth;


import jcog.data.list.Lst;
import nars.NAL;
import nars.Truth;
import nars.util.ArrayIntervalContainer;
import nars.util.IntervalContainer;
import nars.util.RingIntervalSeries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public final class TruthCurve extends Truth {

    private final IntervalContainer<TruthSpan> spans;

    public TruthCurve(int capacity) {
        this(new ArrayIntervalContainer<>(new TruthSpan[capacity]));
    }

    public TruthCurve(IntervalContainer<TruthSpan> container) {
        this.spans = container;
    }

    public static Truth truth(Lst<TruthSpan> spans) {
        return switch (spans.size()) {
            case 0 -> null;
            case 1 -> spans.getFirst().truth;
            default -> new TruthCurve(new ArrayIntervalContainer<>(spans.toArrayRecycled(), spans.size()));
        };
    }

    public final Mean mean(long s, long e) {
        return mean(s, e, 0);
    }

    @Nullable
    public Mean mean(long s, long e, float dur) {
        var m = new Mean.MeanFirstSaved(s, e, dur);
        spans.whileEach(s, e, true, m);
        return m.count > 0 ? m : null;
    }

    @Override
    public int hashCode() {
        return NAL.truth.hash(freq(), conf());
    }

    public long end() {
        return last().end;
    }

    public long start() {
        return first().start;
    }

    @Override public final float freq(long start, long end) {
        var m = mean(start, end);
        return m!=null ? m.freq() : Float.NaN;
    }
    @Override public final double evi(long start, long end) {
        var m = mean(start, end);
        return m!=null ? m.eviMean() : Double.NaN;
    }

    /** TODO cache */
    @Override public float freq() {
        return freq(start(), end());
    }

    /** TODO cache */
    @Override public double evi() {
        if (spans instanceof RingIntervalSeries<TruthSpan>)
            return eviLast();

        return evi(start(), end());
    }

    private double eviLast() {
        var l = spans.last();
        return l == null ? 0 : l.evi();
    }

    /** min..max range of freqs */
    public float freqRange() {
        var s = spans.size();
        final float[]
            fMin = {Float.POSITIVE_INFINITY},
            fMax = {Float.NEGATIVE_INFINITY};
        for (var i = 0; i < s; i++)
            spans.forEach(x -> {
                var f = x.freq();
                fMin[0] = Math.min(fMin[0], f);
                fMax[0] = Math.max(fMax[0], f);
            });
        return fMax[0] - fMin[0];
    }

    @Nullable public final TruthSpan first() {
        return spans.first();
    }

    @Nullable public final TruthSpan last() {
        return spans.last();
    }

    public final int capacity() {
        return spans.capacity();
    }

    public final void add(long start, long end, AbstractMutableTruth truth) {
        add(new TruthSpan(start, end, truth));
    }

    public final void add(long start, long end, float freq, double evi) {
        add(new TruthSpan(start, end, freq, evi));
    }

    public final void add(TruthSpan s) {
        spans.add(s);
    }

    public void clear() {
        spans.clear();
    }

    public final int size() {
        return spans.size();
    }

    public boolean isEmpty() {
        return spans.isEmpty();
    }

    public final boolean isEmpty(long s, long e) {
        return spans.isEmpty(s, e);
    }

    public final void forEach(Consumer<TruthSpan> each) {
        spans.forEach(each);
    }

    public final void forEach(long s, long e, boolean intersectRequired, Consumer<TruthSpan> each) {
        spans.whileEach(s, e, intersectRequired, x -> { each.accept(x); return true; });
    }

    public TruthCurve cloneEviMult(double eviFactor) {
        if (eviFactor == 1) return this;
        if (eviFactor <= 0) throw new IllegalArgumentException();
        var y = new TruthCurve(size());
        spans.forEach(s -> y.add(s.eviScale(eviFactor)));
        return y;
    }

    @Override
    public @Nullable Truth cloneEviMult(double eFactor, double eviMin) {
        double eMean = evi() * eFactor;
        return eMean < eviMin ? null : cloneEviMult(eFactor);
    }

    @Override public TruthCurve neg() {
        var y = new TruthCurve(size());
        spans.forEach(s -> y.add(s.neg()));
        return y;
    }

    public @Nullable Truth cloneFn(Function<TruthSpan, Truth> truthFn) {
        var y = new TruthCurve(size());
        forEach(tt -> {
            var yy = truthFn.apply(tt);
            if (yy!=null) y.add(tt.cloneTruth(yy));
        });
        return y.isEmpty() ? null : y;
    }

    @Override
    public boolean equals(@Nullable Truth y, float freqRes, double confRes) {
        if (y == this) return true;
        if (!(y instanceof TruthCurve Y)) return false;
        var n = spans.size();
        if (n !=Y.spans.size()) return false;
        for (int i = 0; i < n; i++) {
            var xs = spans.get(i);
            var ys = Y.spans.get(i);
            if (!xs.equals(ys, freqRes, confRes))
                return false;
        }
        return true;
    }
}
