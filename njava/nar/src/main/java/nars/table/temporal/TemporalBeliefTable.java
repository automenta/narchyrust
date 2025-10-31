package nars.table.temporal;

import jcog.Fuzzy;
import jcog.Util;
import jcog.math.ImmLongInterval;
import jcog.math.Intervals;
import jcog.math.LongInterval;
import jcog.sort.FloatRank;
import jcog.sort.Top;
import nars.*;
import nars.action.memory.Remember;
import nars.task.SerialTask;
import nars.truth.Stamp;
import nars.truth.TruthCurve;
import nars.truth.util.Revision;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.function.Predicate;

import static jcog.math.Intervals.containsRaw;
import static nars.Op.ETERNAL;
import static nars.Op.TIMELESS;

public abstract sealed class TemporalBeliefTable implements BeliefTable permits ArrayTemporalBeliefTable, NBTemporalBeliefTable, NavigableMapBeliefTable {
    private static final VarHandle
        CAPACITY = Util.VAR(TemporalBeliefTable.class, "capacity", int.class),
        COMPRESSING = Util.VAR(TemporalBeliefTable.class, "compressing", boolean.class);

    @SuppressWarnings("unused")
    private volatile int capacity;
    @SuppressWarnings("unused")
    private volatile boolean compressing;

    static long scanStart(Answer a) {
        return a.start() == ETERNAL ? a.time() /* NAR 's now */ :
            (NAL.temporal.SCAN_START_RANDOM_OR_MID ?
                scanStartRandom(a) :
                scanStartMid(a));
    }

    /** TODO guassian random? */
    private static long scanStartRandom(Answer a) {
        return a.rng().nextLong( //uniform random
            a.start(), a.end()
        );
    }

    private static long scanStartMid(Answer a) {
        return Fuzzy.mean(a.start(), a.end());
    }



    @Override
    public final void remember(Remember r) {
        var x = r.input;
        if (!(x instanceof SerialTask) && !x.ETERNAL())
            if (remember(x, r))
                ensureCapacity(r);
    }


    /** returns true if the input task was inserted.  false if merged or rejected */
    private boolean remember(NALTask x, @Nullable Remember r) {
        return !adjacentMerge(x, r) && insert(x.the(), r);
    }

    protected abstract boolean insertInternal(NALTask x, @Nullable Remember r);

    /**
     * attempt adjacent (or equal) merge
     */
    protected boolean adjacentMerge(NALTask x, @Nullable Remember r) {

        /* expands search interval include adjacent-but-not-intersecting tasks. >=1 */
        @Deprecated var margin = 0;

        float freqRes;
        double confRes;
        if (r == null) {
            freqRes = NAL.truth.FREQ_EPSILON; confRes = NAL.truth.CONF_MIN;
        } else {
            var n = r.nar(); freqRes = n.freqRes.floatValue(); confRes = n.confRes.doubleValue();
        }


        Top<NALTask> mergeable = null;

        var xStamp = x.stamp();
        var xTruth = x.truth();
        var xcurve = xTruth instanceof TruthCurve;
        var xt = x.term();
        var temporal = xt.TEMPORALABLE();
        long xs = x.start(), xe = x.end();
        for (var yy = intersecting(xs - margin, xe + margin).iterator(); yy.hasNext(); ) {
            var y = yy.next(); //existing task
            var yTruth = y.truth();
            var ycurve = yTruth instanceof TruthCurve;
            if (xcurve!=ycurve)
                continue;

            long ys = y.start();
            if (ycurve) {
                if (xs == ys && xe == y.end()) { //TODO relax for concatenation
                    if (Arrays.equals(xStamp, y.stamp())) {
                        if (xTruth.equals(yTruth, freqRes, confRes)) {
                            //TODO decide which, x or y, to keep
                            //TODO merge pri?
                            r.store(y);
                            return true;
                        }
                    }
                }
            } else {
                if (xe >= ys) { long ye = y.end(); if (ye >= xs) { //intersectsRaw(long as, long ae, long bs, long be): ae >= bs && be >= as
                    var yStamp = y.stamp();

                    var stampCmp = Stamp.containment(xStamp, yStamp); //+1 if x{y}, -1 if y{x}

                    if (stampCmp == 0 && NALTask.shareStamp(x, y, xStamp, yStamp) == 0) xStamp = yStamp;

                    if (NAL.temporal.ADJACENT_MERGE_STAMP_CONTAINS ? (stampCmp != Integer.MIN_VALUE) : (stampCmp == 0)) {
                        var xf = xTruth.freq();
                        if (yTruth.equalFreq(xf, freqRes)) {

                            var yt = y.term();
                            if (!temporal || xt.equals(yt)) {

                                double xc = xTruth.conf(), yc = yTruth.conf();
                                if (((xc <= yc && (stampCmp == 0 || stampCmp == +1 /* x stamp is same or larger */) && containsRaw(ys, ye, xs, xe)))) {
                                    /* keep existing */
                                    //TODO merge pri?
                                    r.store(y);
                                    return true;
                                } else if (((yc <= xc && (stampCmp == 0 || stampCmp == -1 /* y stamp is same or larger */) && containsRaw(xs, xe, ys, ye)))) {
                                    /* replace existing with incoming */
                                    //TODO merge pri? if so, do it before y is deleted by remove()
                                    yy.remove();
                                } else if (stampCmp == 0 && Util.equals(xc, yc, confRes)) {
//                                long[] stamp = xys == 0 || (
//                                        /* pessimistic: choose longer stamp */ xStamp.length >= yStamp.length
//                                        // /* dominant: most evidence */ xTruth.conf() * (ie - is + 1 /*x.range()*/) >= yTruth.conf() * (ee - es + 1 /*y.range()*/)
//                                        // /* optimistic: choose shorter stamp */ xStamp.length <= yStamp.length
//                                ) ? xStamp : yStamp;

                                    if (mergeable == null) {
                                        //float dur = r.dur();
                                        //var xRange = xe - xs;
                                        FloatRank<NALTask> dt = (Y, min) ->
                                                -Intervals.diffTotal(xs, xe, Y.start(), Y.end())
                                                //-Math.abs(Y.range() - xRange) //closest range
                                                //-Y.range() //prefer shortest
                                                ;
                                        mergeable = new Top<>(dt);
                                    }
                                    mergeable.add(y);
                                }

                            }
                        }
                    }
                }}
            }
        }

        return mergeable != null && merge(mergeable.the, x, xStamp, freqRes, confRes, r);
    }

    private boolean merge(NALTask existing, NALTask incoming, long[] xStamp, float freqRes, double confRes, Remember r) {
        var xy = Revision.mergeIntersect(existing, incoming,
            freqRes, confRes,
            xStamp);
        if (xy != null) {
            replace(existing, xy);
            r.store(xy);
            return true;
        }
        return false;
    }

    public void replace(NALTask y, NALTask xy) {
        remove(y, true);
        insert(xy, null);
    }

    @Override
    public boolean remove(NALTask x, boolean delete) {
        return removeInternal(x, delete);
    }

    abstract protected boolean removeInternal(NALTask x, boolean delete);

    public void removeAll(Iterable<NALTask> toRemove) {
        removeAllInternal(toRemove);
    }

    public abstract void removeAllInternal(Iterable<NALTask> remove);

    abstract protected Iterable<NALTask> intersecting(long s, long e);

    protected boolean insert(NALTask x, @Nullable Remember r) {
        return insertInternal(x, r);
    }

    @Override public final int capacity() {
        return capacity;
    }

    public final void taskCapacity(int cap) {
        if ((int)(CAPACITY.getAndSet(this, cap))!=cap) {
            capacity = cap; //if (capacity.getAndSet(cap) > cap)
            resize(cap);
            //ensureCapacity(null);
        }
    }

    protected void resize(int cap) {

    }

    protected final void ensureCapacity(Remember r) {
        if ((boolean)COMPRESSING.compareAndExchangeAcquire(this, false, true))
            return; //already compressing

        try {
            var cap = capacity();
            var excess = taskCount() - cap;
            if (excess >= 0) {
                var targetSize = Math.max(1, (int) (cap * (1 - NAL.temporal.TEMPORAL_BELIEF_TABLE_COMPRESSION_RATE)));
                compress(targetSize, cap, r);
            }
        } finally {
            COMPRESSING.setRelease(this, false);
        }

    }

    protected abstract void compress(int preferred, int cap, Remember r);

    public abstract void removeIfInternal(Predicate<NALTask> o, long values, long e);

    public abstract void evictForce(int toRemove, DoubleFunction<NALTask> w);

    public TemporalBeliefTableStats stats() {
        return new TemporalBeliefTableStats(this);
    }

    public abstract void whileEach(Predicate<? super NALTask> each);

    public abstract void removeIf(Predicate<NALTask> remove, long s, long e);

    public boolean conceptMatches(Term x) {
        Term c = _concept();
        return c == null || c.equals(x.concept());
    }

    @Deprecated
    @Nullable
    public Term _concept() {
//        try {
            var k = firstTask();
            //var k = taskStream().findFirst().orElse(null);
            return k != null ? k.term().concept() : null;
//        } catch (NoSuchElementException e) {
//            return null; //HACK
//        }
    }

    public long mid() {
        LongInterval a = firstTask();
        if (a == null) return TIMELESS;
        LongInterval b = lastTask();
        if (a == b) {
            return a.mid();
        } else {
            if (b == null) return TIMELESS;
            return Fuzzy.mean(Math.min(a.start(), b.start()), Math.max(a.end(), b.end()));
        }
    }

    @Nullable
    public LongInterval range() {
        LongInterval a = firstTask();
        if (a != null) {
            LongInterval b = lastTask();
            if (b != null)
                return new ImmLongInterval(a.start(), b.end());
        }
        return null;
    }

    @Nullable
    public abstract NALTask firstTask();

    @Nullable
    public abstract NALTask lastTask();

    public static class TemporalBeliefTableStats {
        public final LongInterval occ;
        public final int size, capacity;
        //TODO descriptive statistics: freq, conf, etc..
        public final DoubleSummaryStatistics freq, conf;

        public TemporalBeliefTableStats(LongInterval occ, int size, int capacity, Iterator<? extends NALTask> tasks) {
            this.occ = occ;
            this.size = size;
            this.capacity = capacity;
            freq = new DoubleSummaryStatistics();
            conf = new DoubleSummaryStatistics();
            while (tasks.hasNext()) {
                NALTask t = tasks.next();
                var tt = t.truth();
                if (tt == null)
                    break; //assume the table is question or quest
                freq.accept(tt.freq());
                conf.accept(tt.conf());
            }
        }

        public TemporalBeliefTableStats(TemporalBeliefTable t) {
            this(t.range(), t.taskCount(),
                    t.capacity(), t.taskStream().iterator());
        }

        @Override
        public String toString() {
            return (occ!=null ? "@" + occ : "") +
                    " size=" + size + "/" + capacity +
                            (freq.getCount() > 0 ? (", freq=" + freq +
                    ", conf=" + conf) : "");
        }
    }
}
