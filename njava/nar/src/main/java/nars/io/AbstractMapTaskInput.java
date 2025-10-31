package nars.io;

import jcog.Util;
import jcog.signal.FloatRange;
import nars.Focus;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractMapTaskInput extends DirectTaskInput {

    private static final boolean drainOnCommit = true;
    /**
     * lower values are lower latency but potentially less throughput and deduplication
     */
    public final FloatRange flushCapacities;
    private final AtomicBoolean draining = new AtomicBoolean(false);
    int overflowThresh = Integer.MAX_VALUE;

    protected AbstractMapTaskInput(float capacityProportion) {
        this.flushCapacities = new FloatRange(capacityProportion, 0, 4);
    }

    @Override
    public void commit() {
        overflowThresh = Math.max((int) Math.ceil(
            flushCapacities.asFloat() * f.attn.capacity()
        ), 1);

        if (drainOnCommit)
            drain();
    }

    void drain() {
        if (Util.enterAlone(draining)) {
            try {
                _drain();
            } finally {
                Util.exitAlone(draining);
            }
        }
    }

    protected abstract int size();

    protected abstract void _drain();

    @Override
    public void start(Focus f) {
        super.start(f);
        clear();
    }

    public abstract void clear();

}
