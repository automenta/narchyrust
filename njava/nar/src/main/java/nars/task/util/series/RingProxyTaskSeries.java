package nars.task.util.series;

import jcog.TODO;
import jcog.data.pool.MetalPool;
import jcog.math.LongInterval;
import nars.util.IntervalSeries;
import nars.util.RingIntervalSeries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

abstract public class RingProxyTaskSeries<Y extends LongInterval, X extends LongInterval> extends IntervalSeries<Y> {

    final RingIntervalSeries<X> data;
    final MetalPool<X> xFree;

    protected RingProxyTaskSeries(int capacity) {
        super(capacity);
        data = new RingIntervalSeries<>(capacity);
        xFree = new MetalPool<>(capacity + 1, capacity + 1) {
            @Override
            public X create() {
                return RingProxyTaskSeries.this._x();
            }
        };
    }

    abstract protected X _x();

    abstract protected Y y();

    abstract protected void encode(Y y, X x);

    abstract protected void decode(X x, Y y);

    @Override
    public void push(Y y) {
        X x = xFree.get();
        encode(y, x);
        data.push(x);
    }

    @Override
    public @Nullable Y poll() {
        X x = data.poll();
        if (x == null)
            return null;
        Y y = decode(x);
        xFree.put(x);
        return y;
    }

    @Nullable
    private Y decode(@Nullable X x) {
        if (x == null)
            return null;

        Y y = y();
        decode(x, y);
        return y;
    }

    @Override
    public boolean remove(Y v) {
        throw new TODO();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean whileEach(long minT, long maxT, boolean intersectRequired, Predicate<? super Y> x) {
        return data.whileEach(minT, maxT, intersectRequired,
                z -> x.test(decode(z)));
    }

    @Override
    public void clear() {
        X x;
        while ((x = data.poll()) != null) {
            xFree.put(x);
        }
    }

    @Override
    public Stream<Y> stream() {
        return data.stream().map(this::decode);
    }

    @Override
    public void forEach(Consumer<? super Y> each) {
        stream().forEach(each);
    }

    @Override
    public Y first() {
        return decode(data.first());
    }

    @Override
    public Y last() {
        return decode(data.last());
    }
}