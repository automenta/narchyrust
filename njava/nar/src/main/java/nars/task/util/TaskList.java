package nars.task.util;

import jcog.TODO;
import jcog.data.list.Lst;
import jcog.math.LongInterval;
import nars.NALTask;
import nars.Term;
import nars.Truth;
import nars.task.proxy.SpecialNegTask;
import nars.truth.Stamp;
import nars.truth.util.TaskEviList;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import static nars.NAL.STAMP_CAPACITY;
import static nars.NALTask.*;

/**
 * A List of Task's which can be used for various purposes, including dynamic truth and evidence calculations (as utility methods)
 */
public class TaskList extends Lst<NALTask> implements TaskRegion, IntToFloatFunction {

    public TaskList() {
        super(0, EmptyNALTaskArray);
    }

    public TaskList(int initialCap) {
        super(0, initialCap > 0 ? new NALTask[initialCap] : EmptyNALTaskArray);
    }

    public TaskList(NALTask[] t, int n) {
        super(n, t);
    }

    public TaskList(Collection<NALTask> t) {
        super(0, new NALTask[t.size()]);
        for (var task : t)
            addFast(task);
    }

    public final TaskList neg() {
        replaceAll(SpecialNegTask::neg);
        return this;
    }

    public final boolean tasksEqual() {
        var s = size();
        if (s >= 2) {
            var items = this.items;
            var first = items[0];
            for (var i = 1; i < s; i++) {
                if (!first.equals(items[i]))
                    return false;
            }
        }
        return true;
    }

    public float freq(int i) {
        return get(i).freq();
    }
    public double conf(int i) {
        return get(i).conf();
    }

    public final boolean addIfNotContainsInstance(NALTask ti) {
        if (containsInstance(ti)) {
            return false;
        } else {
            add(ti);
            return true;
        }
    }

    protected IntToFloatFunction eviPrioritizer(TruthComputer t) {
        return (t instanceof TaskEviList e) ?
            e.eviPrioritizer() :
            this;
    }

    /** weighted average of priorities by the contributed evidence */
    public void fund(NALTask y, IntToFloatFunction pri) {
        //assert(DefaultBudget.priMerge==Mean);
        double p;
        double pSum = 0;

        var s = size;
        for (var i = 0; i < s; i++)
            pSum += pri.valueOf(i);

        p =
            pSum / s; //MEAN
            //pSum;

        y.pri((float) p);
    }

    private NALTask[] arrayCommit() {
        var s = size;
        if (s == 0)
            return EmptyNALTaskArray;

        var i = this.items;
        return s == i.length ? i :
            (this.items = Arrays.copyOf(i, s));
    }

    @Override
    public long start() {

        var start = longify((m, t) -> {
            var s = t.start();
            return s != ETERNAL && s < m ? s : m;
        }, TIMELESS);

		return start == TIMELESS ? ETERNAL : start;
    }

    @Override
    public long end() {
        return maxValue(LongInterval::end);
    }

//    @Override
//    @Nullable
//    public short[] why() {
//        return CauseMerge.AppendUnique.merge(NAL.causeCapacity.intValue(),
//                Util.map(0, size(), short[][]::new, x -> get(x).why()));
//    }

    @Override
    public float freqMin() {
        throw new TODO();
    }

    @Override
    public float freqMax() {
        throw new TODO();
    }

    @Override
    public float confMin() {
        throw new TODO();
    }

    @Override
    public float confMax() {
        throw new TODO();
    }

    @Override
    public final boolean addAll(Collection<? extends NALTask> source) {
        for (var task : source)
            add(task);
        return true;
    }

    protected final NALTask task(Term x) {
        var punc = getFirst().punc();
        var X = taskTerm(x, punc, true, true);
        if (X != null) {
            var truth = ((TruthComputer) this).computeTruth();
            if (taskValidTruth(truth, X, punc, true)) {
                var se = this.startEndArray();
                if (se != null)
                    return task(X, punc, truth, se[0], se[1]);
            }
        }
        return null;
    }

    @Nullable private NALTask task(Term x, byte punc, Truth truth, long start, long end) {
        if (!taskValidOcc(start, end, x, false))
            return null;

        var e = stampZip(STAMP_CAPACITY).get();
        taskValidStamp(e, x);

        var y = taskUnsafe(x, punc, truth, start, end, e);
        if (y.uncreated())
            fund(y, eviPrioritizer((TruthComputer) this));
        return y;
    }

    private Supplier<long[]> stampZip(int capacity) {
        var s = size;
        return s == 1 ? this::stampFirst : ()->stampZipN(capacity);
    }

    private long[] stampZipN(int capacity) {
        return Stamp.zip(capacity, this::stamp, this, size); //calculate stamp after filtering and after intermpolation filtering
    }

    @Nullable private long[] stampFirst() {
        return stamp(0);
    }

    @Nullable public final long[] stamp(int component) {
        var t = items[component];
        return t!=null ? t.stamp() : null;
    }

    public final Term term(int i) {
        return items[i].term();
    }

    @Nullable public Truth taskTruth(int i) {
        return items[i].truth();
    }

    /** this impl returns mean evi of a task */
    public double eviMean(int i) {
        return items[i].evi();
    }


    /**
     * test for whether an amount of evidence is valid
     */
    protected static boolean eviValid(double e) {
        return e > 0;
        //return e > Double.MIN_NORMAL;
    }



    /** calculate the minimum range (ie. intersection of the ranges).
     *  @return >=0 */
    public final long rangeMin() {
        var l = minValue(LongInterval::rangeElseTimeless);
        return l == TIMELESS ? 0 : l - 1;
    }
//    /**
//     * latest start time, excluding ETERNALs.  returns ETERNAL if all ETERNAL
//     */
//    public long latestStart() {
//        return maxValue(LongInterval::start);
//    }

    public long latestEnd() {
        return maxValue(LongInterval::end);
    }
//    /** occurrence end + term's sequence range */
//    public long latestEndWithRange() {
//        return maxValue((NALTask z) ->
//            z.end() + (z.term().seqDur()));
//    }
    public long latestStartPlusRange() {
        return maxValue((NALTask z) ->
                z.start() + z.term().seqDur());
    }


    /**
     * "Flat" mean priority calculator
     * @see that DefaultBudget.derivePri == PriMerge.mean
     */
    @Override public float valueOf(int i) {
        return items[i].priElseZero() / size;
    }

//    public double eviSum(@Nullable IntPredicate each) {
//        double e = 0;
//        int n = size;
//        for (int i = 0; i < n; i++) {
//            if (each == null || each.test(i)) {
//                double ce = evi(i);
//                if (eviValid(ce))
//                    e += ce;
//            }
//        }
//        return e;
//    }

    //    protected float pri(long start, long end) {
//
//        //TODO maybe instead of just range use evi integration
//
//
//        if (start == ETERNAL) {
//            //TODO if any sub-tasks are non-eternal, maybe combine in proportion to their relative range / evidence
//            return reapply(DynTruth::pri, Param.DerivationPri);
//        } else {
//
//
//            double range = (end - start) + 1;
//
//            return reapply(sub -> {
//                float subPri = DynTruth.pri(sub);
//                long ss = sub.start();
//                double pct = ss!=ETERNAL ? (1.0 + Longerval.intersectLength(ss, sub.end(), start, end))/range : 1;
//                return (float) (subPri * pct);
//            }, Param.DerivationPri);
//
//        }
//    }
}