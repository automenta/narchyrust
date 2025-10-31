package nars.util;

import jcog.math.LongInterval;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static jcog.math.LongInterval.TIMELESS;

public interface IntervalContainer<X extends LongInterval> {

    void add(X s);

    boolean remove(X x);

    X first();

    X last();

    int size();

    default X get(int i) { throw new UnsupportedOperationException(); }

    /**
     * returns false if there is some data which occurrs inside the given interval
     */
    default boolean isEmpty(long start, long end) {
        return whileEach(start, end, true, x -> false /* at least one found */ );
    }

    int capacity();

    /**
     * linear search
     * returns false if the predicate ever returns false; otherwise returns true even if empty.  this allows it to be chained recursively to other such iterators
     * <p>
     * intersectRequired: require returned items to temporally intersect (not contained by) the provided range
     */
    boolean whileEach(long minT, long maxT, boolean intersectRequired, Predicate<? super X> x);

    void clear();

    default void forEach(Consumer<? super X> each) {
        whileEach(Long.MIN_VALUE, Long.MAX_VALUE, false, x -> { each.accept(x); return true; } );
    }

    /** returns Tense.TIMELESS (Long.MAX_VALUE) if seemingly empty, which may occurr spuriously */
    default long start() {
        X f = first();
        return f == null ? TIMELESS : f.start();
    }

    /** returns Tense.TIMELESS (Long.MAX_VALUE) if seemingly empty, which may occurr spuriously */
    default long end() {
        X l = last();
        return l == null ? TIMELESS : l.end();
    }

    default boolean isEmpty() {
        return size() == 0;
    }

}
