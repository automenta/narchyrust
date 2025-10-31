package nars.table.temporal;

import jcog.TODO;
import jcog.data.set.LockedSortedArraySet;
import jcog.data.set.SortedArraySet;
import jcog.math.LongInterval;
import jcog.pri.Prioritizable;
import jcog.util.ArrayUtil;
import nars.Answer;
import nars.NALTask;
import nars.action.memory.Remember;
import nars.task.util.ClusterRevise;
import nars.task.util.TaskOccurrence;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.math.LongInterval.ETERNAL;
import static jcog.math.LongInterval.TIMELESS;

public non-sealed class ArrayTemporalBeliefTable extends TemporalBeliefTable {

    private final LockedSortedArraySet<LongInterval> tasks;

    public ArrayTemporalBeliefTable() {
        tasks = new LockedSortedArraySet<>(TaskRegionComparator.the, NALTask.EmptyNALTaskArray);
    }

    @Override
    public NALTask firstTask() {
        return task(0);
    }

    @Override
    public NALTask lastTask() {
        return task(tasks.size() - 1);
    }

    @Override
    protected boolean insert(NALTask x, @Nullable Remember r) {
        return tasks.lock.write(() -> insertInternal(x, r));
    }

    @Override
    protected boolean insertInternal(NALTask x, @Nullable Remember r) {
        var x0 = (NALTask) tasks._put(x);
        var stored = x0 != null ? x0 : x;

        if (r != null)
            r.store(stored);
        return true; //??
    }

    @Override
    protected boolean adjacentMerge(NALTask x, Remember r) {
        return tasks.lock.write(() -> super.adjacentMerge(x, r));
    }

    @Override
    protected Iterable<NALTask> intersecting(long s, long e) {
        return new IntersectingIterator(tasks, s, e);
    }

    @Override
    protected void compress(int targetSize, int capacity, Remember r) {
        new ClusterRevise(this, r, targetSize, capacity);
    }


    @Override
    public void match(Answer a) {
        tasks.lock.read(() -> scanNear(tasks, a));
    }

    static void scanNear(SortedArraySet<? extends LongInterval> tasks, Predicate<NALTask> a) {
        var s = tasks.size();
        if (s <= 0)
            return;

        var center = tasks.binarySearchNearest(TaskOccurrence.at(scanStart((Answer) a)));
        if (center < 0)
            return; //HACK

        var r = 1 + s / 2; //radial layers outward

        for (var i = 0; i < r; ) {
            boolean u;
            {
                LongInterval next = null;
                u = (((center + i < s)) && (next = tasks.get(center + i)) != null);
                if (u && !a.test((NALTask) next))
                    break;
            }
            i++;
            boolean d;
            {
                LongInterval next = null;
                d = (((center - i >= 0)) && (next = tasks.get(center - i)) != null);
                if (d && !a.test((NALTask) next))
                    break;
            }
            if (!u && !d)
                break;
        }
    }

    private NALTask task(int i) {
        return (NALTask) tasks.get(i);
    }

    @Override
    public void evictForce(int toRemove, DoubleFunction<NALTask> w) {
        tasks.lock.write(() -> {
            var rng = ThreadLocalRandom.current();
            var s = tasks.size();
            for (var i = 0; i < toRemove; i++) {
                tasks._remove(rng.nextInt(s));
                s--;
            }
        });
    }

    @Override
    public void whileEach(Predicate<? super NALTask> each) {
        throw new TODO();
    }

    @Override
    public void removeIf(Predicate<NALTask> remove, long s, long e) {
        tasks.lock.write(() -> removeIfInternal(remove, s, e));
    }
    @Override
    public void removeIfInternal(Predicate<NALTask> remove, long s, long e) {
        removeIf(remove, s, e, tasks);
    }


    public static <X extends LongInterval> void removeIf(Predicate<X> remove, long s, long e, SortedArraySet<? extends LongInterval> t) {
        var n = t.size; if (n <= 0) return;

        var intersecting = (s != ETERNAL) && (s != TIMELESS);
        var a = t.array();
        for (var i = 0; i < n; i++) {
            var x = a[i];
            if (x!=null && (!intersecting || x.intersects(s, e)) && remove.test((X)x)) {
                a[i] = null;
                ((Prioritizable)x).delete();
                t.size--;
            }
        }
        if (t.size!=n) ArrayUtil.sortNullsToEnd(a);
    }

    @Override
    public int taskCount() {
        return tasks.size;
    }


    @Override
    public void removeAll(Iterable<NALTask> toRemove) {
        throw new UnsupportedOperationException();
        //return tasks.lock.write(() -> super.removeAll(toRemove));
    }

    @Override
    public void removeAllInternal(Iterable<NALTask> toRemove) {
        for (var x : toRemove)
            tasks._remove(x);
    }

    @Override
    public boolean remove(NALTask x, boolean delete) {
        return tasks.lock.write(()->removeInternal(x, delete));
    }

    @Override
    protected boolean removeInternal(NALTask x, boolean delete) {
        var removed = (NALTask) tasks._remove(x);
        if (removed == null) return false;

        if (delete)
            removed.delete();
        return true;
    }

    @Override
    public void clear() {
        tasks.clear();
    }

    @Override
    public Stream<? extends NALTask> taskStream() {
        return tasks.stream().filter(Objects::nonNull).map(z -> (NALTask) z);
    }

    @Override
    protected void resize(int cap) {
        tasks.resize(cap);
    }

    static class IntersectingIterator implements Iterator<NALTask>, Iterable {
        final SortedArraySet tasks;
        final long s, e;
        int n, k;
        NALTask next;

        IntersectingIterator(SortedArraySet tasks, long s, long e) {
            this.tasks = tasks;
            this.s = s;
            this.e = (s == Long.MIN_VALUE) ? Long.MAX_VALUE /* set full range */ : e;
            n = tasks.size();
        }

        private void update() {
            var a = tasks.array();
            this.next = null;

            // If we've reached the end, we're done
            // Since the array is sorted, we can check if the current position
            // is already past our end time
            NALTask x;
            int k = this.k;
            if (k < n && ((x = (NALTask) a[k]) == null || x.start() <= e)) {
                // skip ahead if we're before our start time
                while ((x == null || x.end() < s) && ++k < n) {
                    x = (NALTask) a[k];
                }
                k = updateIntersecting(a, k);
            }
            this.k = k;
        }

        private int updateIntersecting(Object[] a, int k) {
            // Now we're in the potentially intersecting range
            // We only need to check one element since we've skipped
            // everything that's definitely too early
            while (k < n) {
                var x = (NALTask) a[k++];
                if (x != null && x.intersectsRaw(s, e)) {
                    next = x;
                    break;
                }
            }
            return k;
        }
//        private void update() {
//            NALTask next = null;
//            var tt = tasks.array();
//            do {
//                if (k >= n)
//                    break;
//                next = (NALTask) tt[k++];
//            } while (next == null || !next.intersectsRaw(s, e));
//            this.next = next;
//        }

        @Override
        public void remove() {
            tasks.remove(--k);
            n--;
        }

        @Override
        public boolean hasNext() {
            update();
            return this.next != null;
        }

        @Override
        public NALTask next() {
            return next;
        }

        @Override
        public Iterator iterator() {
            return this;
        }

    }
}
