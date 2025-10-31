package nars.table.dynamic;

import jcog.TODO;
import nars.Answer;
import nars.NALTask;
import nars.Term;
import nars.Truth;
import nars.task.SerialTask;
import nars.util.IntervalSeries;
import nars.util.RingIntervalSeries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class TaskSeriesSeriesBeliefTable extends SerialBeliefTable {
    public final IntervalSeries<SerialTask> tasks;

    public TaskSeriesSeriesBeliefTable(Term c, boolean beliefOrGoal, int cap) {
        super(c, beliefOrGoal);
        this.tasks = new RingIntervalSeries<>(cap);
            //TODO impl time series with concurrent ring buffer from gluegen
            //new ConcurrentSkiplistTaskSeries<>(capacity)
    }

    @Override
    public final long start() {
        return tasks.start();
    }

    @Override
    public final long end() {
        return tasks.end();
    }

    @Override
    public final boolean isEmpty() {
        return tasks.isEmpty();
    }

    @Override
    public Stream<? extends NALTask> taskStream() {
        return tasks.stream();
    }

    public void forEachTask(Consumer<? super NALTask> each) {
        tasks.forEach(each);
    }

    @Override
    public void whileEachRadial(long s, long e, long w, boolean intersectRequired, Predicate<? super SerialTask> each) {
        if (tasks instanceof RingIntervalSeries S)
            S.whileEach(s, e, w, true, each);
        else {
            //tasks.whileEach(s, e, intersectRequired, each);
            throw new TODO();
        }
    }

    @Override
    public void whileEachLinear(long s, long e, Predicate<? super SerialTask> m) {
        if (tasks instanceof RingIntervalSeries S)
            S.whileEachLinear(s, e, m);
        else
            tasks.whileEach(s, e, false, m);
    }

    @Override
    public boolean isEmpty(long s, long e) {
        return tasks.isEmpty(s, e);
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
        return tasks.size();
    }

    @Override protected void answerSample(Answer a, long s, long e) {
        var onlyOne = true;
        if (onlyOne)
            a.ttl = 1;
        if (tasks instanceof RingIntervalSeries S) {
            ((RingIntervalSeries<NALTask>)S).whileEach(s, e,
                    a.rng().nextLong(s, e) //RNG
                    //Fuzzy.mean(s, e) //deterministic
                , false,
                onlyOne ? a : x -> !a.testForce(x)
                //onlyOne ? x -> !a.testForce(x) || a.ttl <= 0 : a
            );
        } else
            throw new TODO();
    }

    @Override
    public void clear() {
        tasks.clear();
    }

    @Override
    public SerialTask add(@Nullable Truth next, SerialBeliefTable.SerialUpdater s) {
        var prev = tasks.last();
        if (prev != null && next!=null) {
            if (continuePrev(prev, next, s))
                return prev;

            //s = Util.clampSafe(s, prev.end() + 1, e); //avoid overlap
            //s = Math.min(Math.max(prev.end() + 1, s), e);
        }

        if (next == null) return null;

        var nextFreq = next.freq();
        var nextEvi = next.evi();
        var w = s.w;
        if (prev != null && tasks.capacity() == 1) {
            prev.set(w.s, w.e, nextFreq, nextEvi, Float.NaN);
            return prev;
        } else {
            var y = taskSerial(w.s, w.e, nextFreq, nextEvi, s.n);
            if (y!=null)
                tasks.add(y);
            return y;
        }
    }

//        @Override public boolean removeIf(Predicate<SerialTask> p, boolean delete) {
//            //HACK
//            return ((NavigableMapTaskSeries<SerialTask>)series).removeIf(p, delete);
//        }

}