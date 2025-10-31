package nars.table.dynamic;

import jcog.data.list.Lst;
import nars.*;
import nars.table.temporal.TemporalBeliefTable;
import nars.task.SerialTask;
import nars.truth.Mean;
import nars.truth.PreciseTruth;
import nars.truth.evi.EviInterval;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.Op.ETERNAL;
import static nars.Op.TIMELESS;

/**
 * adds a TaskSeries additional Task buffer which can be evaluated from, or not depending
 * if a stored task is available or not.
 */
abstract public class SerialBeliefTable extends DynBeliefTable {

    public float stretchDurs = NAL.signal.SERIAL_TASK_STRETCH_DURS_MAX;

//    private static final VarHandle ENABLED = Util.VAR(SerialBeliefTable.class, "enabled", boolean.class);
//    @SuppressWarnings({"unused", "FieldMayBeFinal"})
//    private volatile boolean enabled = true;
    private boolean enabled = true;

    /** TODO protected or private */
    public long[] sharedStamp;

    /** this is useful if the signal is sparse, having periods of absence, in which it should not filter non-signal beliefs
     *  TODO make this non-static, and autodetect if the signal explicitly or implicitly gets disconnected (NaN), and enable this flag
     *  based on that.  this way if the signal is durable, it will remain dense, and if it experiences interruption, it will auto switch
     *  to sparse.
     * */
    private static final boolean SPARSE = false;

    abstract public long start();
    abstract public long end();

    protected SerialBeliefTable(Term c, boolean beliefOrGoal) {
        super(c, beliefOrGoal);
    }

    boolean continuePrev(SerialTask prev, Truth next, SerialUpdater u) {
        var prevEnd = prev.end();

        var w = u.w;
        var gapThresh = NAL.signal.SIGNAL_LATCH_LIMIT_DURS * w.dur;

        var s = w.s;
        if (s - prevEnd <= gapThresh) {

            if (stretchDurs > 0 && u.equals(next, prev.truth())) {
                if (s - prev.start() < stretchDurs * w.dur) {
                    //continue if not excessively long
                    prev.setEnd(Math.max(prevEnd, w.e));
                    return true;
                }
            }

            //form new task either because the value changed, or because the latch duration was exceeded
            if (prevEnd > s - gapThresh)
                prev.setEnd(s - 1);
        }

        return false;
    }

    @Override abstract public boolean isEmpty();

    abstract public boolean isEmpty(long s, long e);

    @Override
    public boolean remove(NALTask x, boolean delete) {
        return !delete || x.delete(); //HACK until RingBufferTaskSeries can remove items
    }

    public final void enabled(boolean e) {
        enabled = e;
    }

    @Override
    public final void match(Answer a) {
        if (!enabled)
            return;

        long s = a.start(), e;
        if (s == ETERNAL || s == TIMELESS) {
            var durHalf = (int) Math.ceil(a.dur() / 2);
            var now = a.time();
            s = now - durHalf;
            e = now + durHalf;
        } else {
            e = a.end();
        }

        answer(a, s, e);
    }

    protected final void answer(Answer a, long s, long e) {
        if (!answerPrecise(a, s, e))
            answerSample(a, s, e);
    }

    protected boolean answerPrecise(Answer a, long s, long e) {
        var m = new Mean.MeanFirstSaved(s, e, a.durTruth);
        whileEachLinear(s, e, m);
        var y = taskMean(m, a.eviMin, false, false, a.nar);
        return y != null && a.testForce(y);
    }

    abstract protected void answerSample(Answer a, long s, long e);

    /** scans linearly from start to end */
    abstract public void whileEachLinear(long s, long e, Predicate<? super SerialTask> m);

    /** /** @param w start point, s <= w <= e */
    abstract public void whileEachRadial(long s, long e, long w, boolean intersectRequired, Predicate<? super SerialTask> each);


    @Override abstract public Stream<? extends NALTask> taskStream();

//    @Override abstract public void forEachTask(long minT, long maxT, Consumer<? super NALTask> x);

    @Override
    public abstract void forEachTask(Consumer<? super NALTask> action);

    /**
     * TODO only remove tasks which are weaker than the sensor
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    public void clean(List<BeliefTable> tables) {
        assert (beliefOrGoal);


        long sStart = start(), sEnd = end();
        if (sEnd <= sStart) return;

        Predicate<NALTask> cleaner = null;
        for (int i = 0, tablesSize = tables.size(); i < tablesSize; i++) {
            TaskTable b = tables.get(i);
            if (!(b instanceof DynBeliefTable) && (b instanceof TemporalBeliefTable T) && !b.isEmpty()) {
                if (cleaner == null)
                    cleaner = t -> ignore(t, sStart, sEnd);

                T.removeIf(cleaner, sStart, sEnd);
            }
        }
    }

    boolean ignore(NALTask t) {
        var seriesStart = start();
        if (seriesStart == TIMELESS)
            return false; //empty, so don't ignore
        else
            return ignore(t, seriesStart, end());
    }

    /**
     * used for if you can cache seriesStart,seriesEnd for a batch of calls
     * TODO only remove tasks which are weaker than the sensor
     */
    boolean ignore(NALTask t, long seriesStart, long seriesEnd) {

        //assert (beliefOrGoal && !t.GOAL());

        if (t.isDeleted())
            return true;

        if (!t.containedBy(seriesStart, seriesEnd))
            return false;

        if (SPARSE) {
            long tStart = t.start(), tEnd = t.end();
            //TODO actually absorb (transfer) the non-series task priority in proportion to the amount predicted, gradually until complete absorption
            //TODO store ranges tested for series rather than keep scanning for each one
            return !isEmpty(tStart, tEnd);
        } else
            return true;
    }

    /**
     * dur can be either a perceptual duration which changes, or a 'physical duration' determined by
     *            the interface itself (ex: clock rate)
     */
    @Nullable public final SerialTask add(@Nullable Truth next, Lst<BeliefTable> tables, SerialUpdater s) {

        var y = add(next, s);

        if (NAL.signal.SIGNAL_TABLE_FILTER_NON_SIGNAL_TEMPORAL_TASKS_ON_SIGNAL)
            clean(tables);

        return y;
    }

    @Nullable abstract public SerialTask add(@Nullable Truth next, SerialUpdater serialUpdater);

    /** creates a new task for storage */
    @Nullable public final SerialTask taskSerial(long start, long end, float f, double evi, NAL n) {
        var t = taskTruth(f, evi);
        return t == null ? null : new SerialTask(term, punc(), t, start, end, stamp(n));
    }

    public Truth taskTruth(float f, double evi) {
        return PreciseTruth.byEvi(f, evi);
    }

    public final long[] stamp(NAL n) {
        return NAL.signal.STAMP_SHARING ? stampShared(n) : n.evidence();
    }

    private long[] stampShared(NAL n) {
        var ss = sharedStamp;
        return ss == null ? (sharedStamp = n.evidence()) : ss;
    }

    public static final class SerialUpdater {
        public final float freqRes, confRes;
        public final NAL n;
        public final EviInterval w;

        public SerialUpdater(EviInterval w, NAL n) {
            this.w = w;
            this.freqRes = n.freqRes.floatValue();
            this.confRes = n.confRes.floatValue();
            this.n = n;
        }

        public boolean equals(Truth a, Truth b) {
            return a.equals(b, freqRes, confRes);
        }
    }


    @Nullable
    public NALTask taskMean(Mean m, double eviMin, boolean precise, boolean serialTask, NAL n) {
        if (m.count < 1/*MIN_MEAN_COMPONENTS*/) return null;

        var evi = m.eviMean();
        if (evi < eviMin) return null;

        long S = m.evi.s, E = m.evi.e;

        if (m instanceof Mean.MeanFirstSaved ms && ms.count == 1) {
            var f = ms.first;
            if (f != null && (!precise || (f.start() == S && f.end() == E)))
                return f;
        }

        var t = taskTruth(m.freq(), evi);
        var stamp = stamp(n);
        var xt = term;
        return serialTask ?
            new SerialTask(xt, punc(), t, S, E, stamp) //pri set manually by callee
            :
            NALTask.taskUnsafe(xt, punc(), t, S, E, stamp).withPri(m.pri());
    }
}