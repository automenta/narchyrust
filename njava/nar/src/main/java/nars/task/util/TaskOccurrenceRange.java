package nars.task.util;

import jcog.TODO;
import jcog.tree.rtree.HyperRegion;

/**
 * only valid for comparison during rtree iteration
 */
public class TaskOccurrenceRange extends AbstractTaskOccurrence {

    final long start, end;

    /** TODO factory constructor */
    public TaskOccurrenceRange(long s, long e) {
        this.start = s;
        this.end = e;
    }

    @Override
    public long start() {
        return start;
    }

    @Override
    public long end() {
        return end;
    }

    @Override
    public boolean intersects(HyperRegion x) {
        throw new TODO();
//        LongInterval t = (LongInterval)x;
//        //return LongInterval.intersectsSafe(s, end, t.start(), t.end());
//        return LongInterval.intersectsRaw(start, end, t.start(), t.end());
    }


    @Override
    public boolean contains(HyperRegion x) {
        throw new TODO();
//        //return ((LongInterval)x).containedBy(start, end);
//        LongInterval t = ((LongInterval) x);
//        //return containsSafe(t.start(), t.end());
//        return (t.start() >= start && t.end() <= end);
    }


}
