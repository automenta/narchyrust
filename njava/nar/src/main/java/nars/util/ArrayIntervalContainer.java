package nars.util;

import jcog.math.LongInterval;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ArrayIntervalContainer<X extends LongInterval> implements IntervalContainer<X> {
    private final X[] span;
    private int size;

    public ArrayIntervalContainer(X[] spans, int size) {
        this(spans);
        this.size = size;
    }

    public ArrayIntervalContainer(X[] spans) {
        this.span = spans;
    }

    @Override
    public X get(int i) {
        return span[i];
    }

    @Override
    public X first() {
        return size == 0 ? null : span[0];
    }

    @Override
    public X last() {
        var s = size;
        return s == 0 ? null : span[s - 1];
    }

    @Override
    public void add(X x) {
        int n = this.size;
        if (n >= span.length) {
            // Remove the oldest interval if full
            System.arraycopy(span, 1, span, 0, n - 1);
            n = --this.size;
        }

        int insertIndex = insertIndex(x); // Find insertion point to maintain temporal order

        // Shift elements to make room
        if (insertIndex < n)
            System.arraycopy(span, insertIndex, span, insertIndex + 1, n - insertIndex);
        span[insertIndex] = x;
        this.size++;
    }

    private void ensureCapacity() {
        if (size >= span.length) {
            // Remove the oldest interval if full
            System.arraycopy(span, 1, span, 0, size - 1);
            size--;
        }
    }

    private int insertIndex(X s) {
        int i = 0;
        var ss = s.start();
        while (i < size) {
            if (span[i].start() > ss) break;
            i++;
        }
        return i;
    }

    @Override
    public boolean remove(X x) {
        for (int i = 0; i < size; i++) {
            if (span[i].equals(x)) {
                int numMoved = size - i - 1;
                if (numMoved > 0)
                    System.arraycopy(span, i + 1, span, i, numMoved);
                span[--size] = null;
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int capacity() {
        return span.length;
    }

    @Override
    public void forEach(Consumer<? super X> each) {
        for (int i = 0, n = size; i < n; i++)
            each.accept(span[i]);
    }

    @Override
    public boolean whileEach(long qs, long qe, boolean intersectRequired, Predicate<? super X> pred) {
        for (int i = 0; i < size; i++) {
            var x = span[i];

            // Skip intervals that end before qs
            var xe = x.end();
            if (xe >= qs) {// Break if we've passed qe
                var xs = x.start();
                if (xs > qe)
                    break;

                if (!pred.test(x))
                    return false;
            }
        }
        return true;
    }

    @Override
    public void clear() {
        Arrays.fill(span, 0, size, null);
        size = 0;
    }
}
