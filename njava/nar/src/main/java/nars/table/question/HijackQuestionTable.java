package nars.table.question;

import jcog.Util;
import jcog.pri.bag.impl.hijack.PriPriHijackBag;
import jcog.pri.op.PriMerge;
import jcog.signal.NumberX;
import nars.*;
import nars.action.memory.Remember;
import nars.truth.Stamp;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.Integer.MIN_VALUE;
import static jcog.math.Intervals.unionArray;
import static jcog.pri.bag.Sampler.SampleReaction.Next;
import static jcog.pri.bag.Sampler.SampleReaction.Stop;
import static nars.Op.ETERNAL;
import static nars.Op.TIMELESS;

public class HijackQuestionTable extends PriPriHijackBag<NALTask> implements QuestionTable {

    static final int DEFAULT_REPROBES = 3;

    private static final float COMMIT_DURS = 1;


    /**
     * TODO varhandle
     */
    final AtomicLong nextCommit = new AtomicLong(TIMELESS);

    public HijackQuestionTable() {
        this(0);
    }

    public HijackQuestionTable(int cap) {
        this(cap, DEFAULT_REPROBES);
    }

    public HijackQuestionTable(int cap, int reprobes) {
        super(PriMerge.plus, cap, reprobes);
    }

    @Override
    protected NALTask merge(NALTask existing, NALTask incoming, NumberX overflowing) {
        return existing;
    }


    @Override
    public void match(Answer a) {
        //int max = Math.min(size(), Math.min(a.ttl, a.tasks.capacity()));
        int max = a.ttl;
        if (max > 0)
            sample(a.random(), new PredicateN<>(max, a));
    }

    @Deprecated private static final class PredicateN<X> implements Function<X, SampleReaction> {
        private final int max;
        private final Predicate<X> kontinue;
        private int count;

        PredicateN(int max, Predicate<X> kontinue) {
            this.max = max;
            this.kontinue = kontinue;
            count = max;
        }

        @Override public SampleReaction apply(X x) {
            return kontinue.test(x) && --count > 0 ? Next : Stop;
        }
    }

//    /** optimized for cases with zero and one stored tasks */
//    @Override public NALTask match(long start, long end, Term template, float dur, NAR nar) {
//        return switch (size()) {
//            case 0 -> null;
//            case 1 -> next(0, t -> false);
//            default -> QuestionTable.super.match(start, end, template, dur, nar);
//        };
//    }

    @Override
    public final boolean isEmpty() {
        return super.isEmpty();
    }


    /**
     * finds nearly equivalent and temporally combineable task.
     * returns null if insertion should proceed with the 'x' instance.
     * if returns 'x' it means its already present
     * if it returns another instance it is an equal but different
     * //TODO combine with NALTask.temporalMerge
     */
    private @Nullable NALTask preMerge(NALTask x) {
        if (isEmpty())
            return null;

        @Nullable NALTask ey = get(x);
        if (ey != null)
            return ey; //found exact task

        long[] xStamp = x.stamp();
        Term xt = x.term();
        long xs = x.start(), xe = x.end();
        boolean xEternal = xs == ETERNAL;
        boolean tmp = xt.TEMPORALABLE();

        for (NALTask y : this) {

            if (y.ETERNAL() ^ xEternal) continue; //keep eternal and temporal separated
            if (!y.intersects(xs, xe)) continue;

            //if (Arrays.equals(xStamp, y.stamp())) {
            long[] yStamp = y.stamp();

            int xys = Stamp.containment(xStamp, yStamp);
            if (xys == 0) {
                if (NALTask.shareStamp(x, y, xStamp, yStamp)==0)
                    xStamp = x.stamp();
            }

            if (!tmp || xt.equals(y.term())) {


                //if (LongInterval.intersectLength(xs, xe, ys, ye)>0) {
                //if (LongInterval.intersectsSafe(xs, xe, ys, ye)) {
                if ((xys == 0 || xys == 1) && y.contains(xs, xe)) {
                    //x contained within y, so merge
                    //TODO boost y priority if contributes to it, in proportion to range
                    return y;
                } else if ((xys == 0 || xys == -1) && y.containedBy(xs, xe)) { //TODO y.containedBy(xs,xe)
                    //y contained within x, so remove y.   expect x to get inserted next
                    //TODO boost x priority if contributes to it, in proportion to range
                    remove(y);
                    y.delete();
                    return null;
                }

                if (NAL.belief.QUESTION_MERGE_AGGRESSIVE && xys == MIN_VALUE && y.intersects(xs, xe)) {
                    //TODO recurse?
                    return mergeQuestions(x, y, xStamp, yStamp);
                }

            }
        }

        return null;
    }

    private NALTask mergeQuestions(NALTask x, NALTask y, long[] xStamp, long[] yStamp) {
        long xs = x.start(), xe = x.end();
        long ys = y.start(), ye = y.end();
        long[] z = unionArray(xs, xe, ys, ye);

        long zs = z[0], ze = z[1];
        double xPri = x.priElseZero(), yPri = y.priElseZero();

        NALTask xy = NALTask.clone(x, x.term(), null, x.punc(), zs, ze,
                Stamp.zip(xStamp, yStamp, x, y),
                false);
        //if (xy == null) return xPri >= yPri ? x : null; //merge failed, choose the higher priority: proceed with inserting y

        double newPri = //xs == ETERNAL || ys == ETERNAL ?
                Util.mean(xPri, yPri);// :
                //((xPri * (xe - xs + 1) + yPri * (ye - ys + 1)) / (ze - zs + 1));
        xy.pri((float) newPri);

        remove(y);
        y.delete();

        return xy;
    }


    @Override
    public void remember(Remember r) {
        NALTask _x = r.input;

        NALTask y = preMerge(_x);
        if (y == null) {
            NALTask z = put(r.input);
            if (z != null)
                r.store(z);
        } else {
            r.store(y);
        }

        tryCommit(r);
    }

    private void tryCommit(Remember r) {
        long now = r.time();
        long p = nextCommit.getOpaque();
        if (p == TIMELESS || now > p) {
            long n = now + (int) (COMMIT_DURS * r.dur());
            if (nextCommit.compareAndSet(p, n))
                commit(forget(r.nar().questionForgetting.floatValue() /* estimate */));
        }
    }

    @Override
    public final int taskCount() {
        return size();
    }

    @Override
    public final void taskCapacity(int newCapacity) {
        setCapacity(newCapacity);
    }

    @Override
    public void forEachTask(Consumer<? super NALTask> x) {
        forEachKey(x);
    }

    @Override
    public boolean remove(NALTask x, boolean delete) {
        Task r = remove(x);
        if (r != null) {
            if (delete)
                r.delete();
            return true;
        }
        return false;
    }

    @Override
    public Stream<? extends NALTask> taskStream() {
        return stream();
    }

}