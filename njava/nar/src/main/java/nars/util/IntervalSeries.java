package nars.util;

import jcog.math.LongInterval;
import jcog.pri.Deleteable;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public abstract class IntervalSeries<X extends LongInterval> implements IntervalContainer<X> {

    private final int cap;

    protected IntervalSeries(int cap) {
        this.cap = cap;
    }

    public int capacity() {
        return cap;
    }

    public abstract void push(X t);

    /** remove the oldest task, and delete it */
    public abstract @Nullable X poll();

    public final void compress() {
        int toRemove = (size()+1) - cap;
        while (toRemove-- > 0) {
            if (poll() instanceof Deleteable d) d.delete();
        }
    }

    @Override
    public final void add(X s) {
        compress();
        push(s);
    }


    public abstract Stream<X> stream();







//    private static class EmptyTaskSeries extends IntervalSeries<NALTask> {
//
//        public EmptyTaskSeries() {
//            super(0);
//        }
//
//        @Override
//        public void push(NALTask t) {
//
//        }
//
//        @Nullable
//        @Override
//        public NALTask poll() {
//            return null;
//        }
//
//        @Override
//        public boolean remove(NALTask nalTask) {
//            return false;
//        }
//
//        @Override
//        public int size() {
//            return 0;
//        }
//
//        @Override
//        public boolean whileEach(long minT, long maxT, boolean intersectRequired, Predicate<? super NALTask> x) {
//            return false;
//        }
//
//        @Override
//        public void clear() {
//
//        }
//
//        @Override
//        public Stream<NALTask> stream() {
//            return Stream.empty();
//        }
//
//        @Override
//        public void forEach(Consumer<? super NALTask> action) {
//
//        }
//
//        @Override
//        public long start() {
//            return TIMELESS;
//        }
//
//        @Override
//        public long end() {
//            return TIMELESS;
//        }
//
//        @Override
//        public NALTask first() {
//            return null;
//        }
//
//        @Override
//        public NALTask last() {
//            return null;
//        }
//    }
}