package nars.table.dynamic;

import jcog.TODO;
import jcog.Util;
import jcog.data.list.Lst;
import jcog.math.Intervals;
import jcog.math.LongInterval;
import nars.*;
import nars.task.SerialTask;
import nars.truth.MutableTruth;
import nars.truth.TruthCurve;
import nars.truth.TruthSpan;
import nars.truth.evi.EviInterval;
import nars.util.RingIntervalSeries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.math.LongInterval.TIMELESS;


/** replacement for TaskSeriesBeliefTable; uses one common task instance which contains the curve deque */
public class TruthCurveBeliefTable extends SerialBeliefTable {

    final CurveTask task;

    final TruthCurve truth;

    public TruthCurveBeliefTable(Term c, boolean beliefOrGoal) {
        super(c, beliefOrGoal);

        truth = new TruthCurve(new RingIntervalSeries<>(NAL.signal.SIGNAL_BELIEF_TABLE_SERIES_CAPACITY));
        task = new CurveTask(c, beliefOrGoal);
    }

    @Override
    public final long start() {
        return task.start();
    }

    @Override
    public final long end() {
        return task.end();
    }

    @Override
    public final boolean isEmpty() {
        return truth.isEmpty();
    }

    @Override
    public Stream<? extends NALTask> taskStream() {
        return Stream.of(task);
    }

    public void forEachTask(Consumer<? super NALTask> each) {
        each.accept(task);
    }

    @Override
    public void whileEachRadial(long s, long e, long w, boolean intersectRequired, Predicate<? super SerialTask> each) {
        each.test(task);
    }

    @Override
    public void whileEachLinear(long s, long e, Predicate<? super SerialTask> each) {
        each.test(task);
    }

    @Override
    public final boolean isEmpty(long s, long e) {
        return truth.isEmpty(s, e);
    }

    @Override
    public final void forEachTask(long minT, long maxT, Consumer<? super NALTask> x) {
        whileEachLinear(minT, maxT, t -> {
            x.accept(t);
            return true;
        });
    }

    @Override
    public int taskCount() {
        return 1;
    }

    @Override
    protected boolean answerPrecise(Answer a, long qs, long qe) {
        var S = start();
        if (S!=TIMELESS) {
            var E = end();
            if (E != TIMELESS) {
                var when = when(a, qs, qe, S, E);
                var z = task(when[0], when[1], a.eviMin, a.nar);
                if (z != null)
                    return a.test(z);
//                else {
//                    //TEMPORARY
//                    when = when(a, qs, qe, S, E);
//                    z = task(a, when[0], when[1]);
//                    Util.nop();
//                }
            }
        }
        return false;
    }

    @Deprecated public @Nullable NALTask task(long s, long e, double eviMin, @Nullable NAL n) {
        long ts = start(), te = end();
        if (s <= ts && e >= te) {
            return this.task; //requested region containing the entire task
        } else {
            return taskPart(s, e, eviMin, n);
        }
    }

    /** part of the task */
    private @Nullable NALTask taskPart(long s, long e, double eviMin, NAL n) {
        var spans = new Lst<>(0, new TruthSpan[4]);
        this.truth.forEach(s, e, true, spans::add);
        var t = TruthCurve.truth(spans);
        if (t == null || t.evi() < eviMin) return null;

        var y = NALTask.taskUnsafe(term, punc(), t, s, e, stamp(n));
        y.pri(this.task.priElseZero());
        return y;
    }

    @Override
    protected void answerSample(Answer a, long s, long e) {
        //HACK:
//        var last = this.truth.last();
//        if (last!=null) {
//            var lastTask = task(a, last.start, last.end);
//            if (lastTask!=null)
//                a.test(lastTask);
//        }
    }

    private static long[] when(Answer a, long qs, long qe, long os, long oe) {
        if (qs == LongInterval.ETERNAL || qs == TIMELESS) {
            int r = Math.round(a.dur());
            var now = a.nar.time();
            qs = now - r/2;
            qe = now + r/2;
        }
        var intersection = Intervals.intersectionRaw(qs, qe, os, oe);
        if (intersection!=null) {
            return intersection;
        } else {
            long[] se = {qs, qe};
            Intervals.fitWithin(se, os, oe);
            return se;
        }
    }


    //    @Override protected void answerSample(Answer a, long s, long e) {
//        a.testOne(task);
////        boolean onlyOne = true;
////        if (tasks instanceof RingIntervalSeries S) {
////            ((RingIntervalSeries<NALTask>)S).whileEach(s, e,
////                    a.rng().nextLong(s, e) //RNG
////                    //Fuzzy.mean(s, e) //deterministic
////                    , false,
////                    onlyOne ? a::testOne : a
////            );
////        } else
////            throw new TODO();
//    }

    @Override
    public void clear() {
        truth.clear();
    }

    public SerialTask add(@Nullable Truth next, SerialBeliefTable.SerialUpdater u) {
        if (next == null)
            return null;

        if (task.stamp()==null)
            task.setStamp(u.n.evidence());

        var prev = truth.last();
        var w = u.w;
        long s = w.s, e = w.e;
        if (prev != null) {
            if (continuePrev(prev, next, s, e, w.dur, u.freqRes, u.confRes))
                return task;

            s = Util.clampSafe(s, prev.end() + 1, e); //avoid overlap
        }

        task.setUncreated(); //force refresh

        _add(next, s, e, prev);

        return task;
    }

    private void _add(Truth next, long start, long end, TruthSpan prev) {
        var F = next.freq();
        var E = next.evi();
        if (prev != null && truth.capacity() == 1)
            prev.set(start, end, F, E);
        else
            truth.add(start, end, new MutableTruth(F, E) /* volatile truth spans */ );
    }

    boolean continuePrev(TruthSpan prev, Truth next, long nextStart, long nextEnd, float dur, float freqRes, float confRes) {
        var prevEnd = prev.end();

        var gapThresh = NAL.signal.SIGNAL_LATCH_LIMIT_DURS * dur;

        if (nextStart - prevEnd <= gapThresh) {

            if (stretchDurs > 0 && prev.truth.equals(next, freqRes, confRes)) {
                if (nextStart - prev.start < stretchDurs * dur) {
                    //continue if not excessively long
                    prev.end = Math.max(prevEnd, nextEnd);
                    return true;
                }
            }

            //form new task either because the value changed, or because the latch duration was exceeded
            if (prevEnd < nextStart - 1 && prevEnd > nextStart - gapThresh)
                prev.end = nextStart - 1;
        }

        return false;
    }

    public final class CurveTask extends SerialTask {

        public CurveTask(Term c, boolean beliefOrGoal) {
            super(c, beliefOrGoal ? Op.BELIEF : Op.GOAL, TruthCurveBeliefTable.this.truth, Long.MIN_VALUE, Long.MAX_VALUE, TruthCurveBeliefTable.this.sharedStamp);
        }

        public NALTask task(long[] se, NAL n) {
            return TruthCurveBeliefTable.this.task(se[0], se[1], n.eviMin(), n);
        }

        void setStamp(long[] s) {
            this.stamp = s;
        }

        @Override public double eviMean(EviInterval q, float ete) {
            if (ete > 0)
                throw new TODO();
            //HACK this is slow
            var m = truth.mean(q.s, q.e, q.dur);
            return m!=null ? m.eviMean() : 0;
        }

        @Override
        public long start() {
            var t = truth.first();
            return t==null ? Long.MAX_VALUE : t.start();
        }

        @Override
        public long end() {
            var t = truth.last();
            return t==null ? Long.MAX_VALUE : t.end();
        }

        public final TruthCurve truthCurve() {
            return TruthCurveBeliefTable.this.truth;
        }
    }


}
