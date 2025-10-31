package nars.table.temporal;

import jcog.Util;
import jcog.data.set.SortedArraySet;
import nars.Answer;
import nars.NALTask;
import nars.action.memory.Remember;
import nars.task.util.ClusterRevise;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.jctools.queues.unpadded.MpscUnboundedUnpaddedArrayQueue;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.table.temporal.ArrayTemporalBeliefTable.scanNear;

/** non-blocking temporal belief table */
public non-sealed class NBTemporalBeliefTable extends TemporalBeliefTable {

    @SuppressWarnings("CanBeFinal")
    volatile SortedArraySet<NALTask> t;
    private static final VarHandle T = jcog.Util.VAR(NBTemporalBeliefTable.class, "t", SortedArraySet.class);

    private final MpscUnboundedUnpaddedArrayQueue<Consumer<SortedArraySet<NALTask>>> updates;

    private volatile boolean updating, compressing;

    public NBTemporalBeliefTable() {
        t = new SortedArraySet<>(TaskRegionComparator.the, NALTask.EmptyNALTaskArray);
        updates = new MpscUnboundedUnpaddedArrayQueue<>(4);
        //updates = new MpmcUnboundedXaddArrayQueue(4);
    }

    private SortedArraySet<NALTask> tasksProcessed() {
        process();
        return tasks();
    }

    private void process() {
        if (!(boolean)UPDATING.compareAndExchangeAcquire(this, false, true)) {
            try {
                if (!updates.isEmpty())
                    _process();
            } finally {
                UPDATING.setRelease(this, false);
            }
        }
    }

    private void _process() {
        var next = tasks().clone();

        updates.drain(u -> u.accept(next));

        T.setRelease(this, next);
    }

    @Override
    protected void resize(int cap) {
        change(x -> x.resize(cap));
    }

    @Override
    protected void compress(int preferred, int capacity, Remember r) {
        new ClusterRevise(this, r, preferred, capacity);
    }

    @Override
    public NALTask firstTask() {
        process();
        return tasks().first();
    }

    @Override
    public NALTask lastTask() {
        process();
        return tasks().last();
    }

    private SortedArraySet<NALTask> tasks() {
        return (SortedArraySet<NALTask>) T.getAcquire(this);
    }

    @Override
    protected boolean insert(NALTask x, @Nullable Remember r) {
        change(t -> insertInternal(x, r, t));
        return true;
    }

    @Override protected boolean insertInternal(NALTask x, @Nullable Remember r) {
        throw new UnsupportedOperationException();
//        insertInternal(x, r, tasks());
//        return r==null || r.stored!=null; //?
    }

    private static void insertInternal(NALTask x, @Nullable Remember r, SortedArraySet<NALTask> t) {
        var x0 = t.put(x);
        var stored = x0 != null ? x0 : x;
        if (r != null)
            r.store(stored);
    }

    @Override
    public void evictForce(int toRemove, DoubleFunction<NALTask> w) {
        change(t -> {
            var rng = ThreadLocalRandom.current();
            int remain = toRemove, tries = toRemove*2;
            for (var i = 0; i < tries; i++) {
                var s = t.size();
                if (s <= 0) break;
                if (t.remove(rng.nextInt(s))!=null)
                    if (--remain <= 0)
                        break; //done
            }
        });
    }

    @Override
    public void replace(NALTask y, NALTask xy) {
        change(t -> {
            removeInternal(y, t);
            y.delete();
            insertInternal(xy, null, t);
        });
    }

    /** return value may not be accurate if change is delayed by another thread */
    @Override public boolean remove(NALTask x, boolean delete) {
        var removed = new boolean[1];
        change(t -> removed[0] = removeInternal(x, t));
        if (delete)
            x.delete();
        return removed[0];
    }

    private static boolean removeInternal(NALTask x, SortedArraySet<NALTask> t) {
        return t.remove(x) != null;
    }

    @Override
    protected boolean removeInternal(NALTask x, boolean delete) {
        //return removeInternal(x, delete, tasks());
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAll(Iterable<NALTask> toRemove) {
        if (toRemove instanceof Collection<NALTask> c) {
            switch (c.size()) {
                case 0 -> {
                    return;
                }
                case 1 ->
                        remove(c.iterator().next(), false);
            }
        }
        change(t -> removeAllInternal(toRemove, t));
    }

    @Override
    public void removeAllInternal(Iterable<NALTask> remove) {
        throw new UnsupportedOperationException();
    }

    private static void removeAllInternal(Iterable<NALTask> remove, SortedArraySet<NALTask> t) {
        for (var r : remove)
            t.remove(r);
    }

    @Override
    protected Iterable<NALTask> intersecting(long s, long e) {
        process();
        return new MyIntersectingIterator(s, e);
    }

    @Override
    public void match(Answer a) {
        scanNear(tasksProcessed(), a);
    }

    @Override
    public void whileEach(Predicate<? super NALTask> each) {
        for (var task : tasksProcessed())
            if (!each.test(task))
                break;
    }

    @Override
    public void removeIf(Predicate<NALTask> remove, long s, long e) {
        change(t -> removeIfInternal(remove, s, e, t));
    }

    public void removeIfInternal(Predicate<NALTask> remove, long s, long e) {
        removeIfInternal(remove, s, e, tasks());
    }

    public static void removeIfInternal(Predicate<NALTask> remove, long s, long e, SortedArraySet<NALTask> t) {
        ArrayTemporalBeliefTable.removeIf(remove, s, e, t);
    }

    @Override
    public int taskCount() {
        return tasksProcessed().size();
    }


    @Override
    public void clear() {
        change(SortedArraySet::clear);
    }

    @Override
    public Stream<? extends NALTask> taskStream() {
        return tasksProcessed().stream().filter(Objects::nonNull);
    }

    private void change(Consumer<SortedArraySet<NALTask>> p) {
        //TODO if updates.size() > ...
        updates.offer(p);
        process();
    }

    private class MyIntersectingIterator extends ArrayTemporalBeliefTable.IntersectingIterator {
        MyIntersectingIterator(long s, long e) {
            super(NBTemporalBeliefTable.this.tasks(), s, e);
        }

        @Override
        public void remove() {
            NBTemporalBeliefTable.this.remove(next, true);
        }
    }

    private static final VarHandle UPDATING = Util.VAR(NBTemporalBeliefTable.class, "updating", boolean.class);
    private static final VarHandle COMPRESSING = Util.VAR(NBTemporalBeliefTable.class, "compressing", boolean.class);

}
