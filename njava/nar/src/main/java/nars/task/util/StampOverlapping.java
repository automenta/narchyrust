package nars.task.util;

import jcog.signal.meter.SafeAutoCloseable;
import nars.NALTask;
import nars.truth.Stamp;

import java.util.function.Predicate;

import static jcog.math.Intervals.intersectsRaw;
import static jcog.math.LongInterval.ETERNAL;

public final class StampOverlapping implements Predicate<NALTask>, SafeAutoCloseable {

    private long[] xStamp;
    private long xs, xe;

    public StampOverlapping set(NALTask n) {
        this.xStamp = n.stamp();
        this.xe = (this.xs = n.start()) == ETERNAL ? ETERNAL : n.end();
        return this;
    }

    @Override
    public final boolean test(NALTask y) {
        //TODO which is faster?
        return overlapsStamp(y) && overlapsTime(y);
        //return overlapsTime(y) && overlapsStamp(y);
    }

    private boolean overlapsTime(NALTask y) {
        if (xs == ETERNAL) return true;
        long ys = y.start();
        return ys == ETERNAL || intersectsRaw(xs, xe, ys, y.end());
    }

    private boolean overlapsStamp(NALTask y) {
        return Stamp.overlapsAny(xStamp, y.stamp());
    }

    @Override public void close() {
        xStamp = null;
    }

}