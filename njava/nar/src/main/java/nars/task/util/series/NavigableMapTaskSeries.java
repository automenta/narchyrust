package nars.task.util.series;

import jcog.TODO;
import jcog.math.LongInterval;
import jcog.pri.Deleteable;
import nars.util.IntervalSeries;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.math.LongInterval.ETERNAL;

public class NavigableMapTaskSeries<X extends LongInterval> extends IntervalSeries<X> {

    /**
     * tasks are indexed by their midpoint. since the series
     * is guaranteed to be ordered and non-overlapping, two entries
     * should not have the same mid-point even if they overlap
     * slightly.
     */
    final NavigableMap<Long, X> q;

    public NavigableMapTaskSeries(int cap) {
        this(new ConcurrentSkipListMap /*2*/<>(), cap);
    }


    public NavigableMapTaskSeries(NavigableMap<Long, X> q, int cap) {
        super(cap);
        this.q = q;
    }

    @Override
    public final boolean isEmpty() {
        return q.isEmpty();
    }

    @Override
    public final long start() {
        return super.start();
    }

    @Override
    public final long end() {
        return super.end();
    }

    @Override
    public final int size() {
        return q.size();
    }

    @Override
    public Stream<X> stream() {
        return q.values().stream();
    }

    @Override
    public void forEach(Consumer<? super X> each) {
        q.values().forEach(each);
    }

    @Override
    public void clear() {
        q.clear();
    }

    @Override
    public boolean whileEach(long minT, long maxT, boolean isectRequired, Predicate<? super X> p) {
        assert(minT!=ETERNAL);

        Long low = q.floorKey(minT);
        if (low == null)
            low = minT;

        if (!isectRequired) {
            Long lower = q.lowerKey(low);
            if (lower != null)
                low = lower;
        }

        Long high = q.ceilingKey(maxT);
        if (high == null)
            high = maxT;

        if (!isectRequired) {
            Long higher = q.higherKey(high);
            if (higher != null)
                high = higher;
        }

        for (X y : q.subMap(low, true, high, true).values()) {
            if (!isectRequired || y.intersectsRaw(minT, maxT)) {
                if (!p.test(y))
                    return false;
            }
        }

        return true;
    }

    @Override
    public @Nullable X last() {
        Map.Entry<Long, X> x = q.lastEntry();
        return x!=null ? x.getValue() : null;
    }
    @Override
    public @Nullable X first() {
        Map.Entry<Long, X> x = q.firstEntry();
        return x!=null ? x.getValue() : null;
    }

    @Override
    @Nullable
    public X poll() {
        Map.Entry<Long, X> e = q.pollFirstEntry();
        return e!=null ? e.getValue() : null;
    }

    @Override
    public void push(X t) {
        q.put(t.start(), t);
    }


    public boolean removeIf(Predicate<X> t, boolean delete) {
        return q.values().removeIf(delete ? z -> {
            if (t.test(z)) {
                if (z instanceof Deleteable d)
                    d.delete();
                return true;
            }
            return false;
        } : t);
    }

    @Override
    public boolean remove(X x) {
        throw new TODO();
    }
}