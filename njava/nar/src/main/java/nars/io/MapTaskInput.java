package nars.io;

import jcog.data.map.CustomConcurrentHashMap;
import nars.NALTask;

/**
 * FIFO, non-lossy
 * TODO rate parameter (currently it flushes entire map)
 */
public class MapTaskInput extends AbstractMapTaskInput {

    private final MapBuffer<NALTask> map = new MapBuffer<>(
        new CustomConcurrentHashMap<>()
        //new ConcurrentHashMapUnsafe<>()
        //new ConcurrentOpenHashMap<>()
        //new ConcurrentHashMap<>()
        //new NonBlockingHashMap<>()
    ) {
        @Override
        protected void merge(NALTask p, NALTask n) {
            NALTask.merge(p, n);
        }
    };

    public MapTaskInput() {
        this(0);
    }

    public MapTaskInput(float capacityThreshProportion) {
        super(capacityThreshProportion);
    }

    @Override
    public void remember(NALTask t) {
        if (map.put(t)) {
            if (overflowThresh <= 0 || size() >= overflowThresh)
                drain();
        }
    }

    @Override
    protected int size() {
        return map.size();
    }

    @Override
    protected void _drain() {
        rememberAll(map.map);
    }

    @Override
    public void clear() {
        map.clear();
    }
}
