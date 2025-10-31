package nars.task.util;

import jcog.math.Intervals;
import jcog.math.LongInterval;
import jcog.tree.rtree.HyperRegion;

/** point-like occurrence comparator */
public class TaskOccurrence extends AbstractTaskOccurrence {

    static final TaskOccurrence MIN_VALUE = new TaskOccurrence(Long.MIN_VALUE);
    static final TaskOccurrence MAX_VALUE = new TaskOccurrence(Long.MAX_VALUE);

    private final long x;

	/** x can be Long.MIN_VALUE or Long.MAX_VALUE, ignoring the ETERNAL/TIMELESS semantics, for pure numerical comparison */
	private TaskOccurrence(long x) {
		this.x = x;
	}

	public static TaskOccurrence at(long s) {
//		if (s == ETERNAL)
//			throw new UnsupportedOperationException();
//		if (s == Long.MAX_VALUE)
//			return MAX_VALUE;
//		else if (s == Long.MIN_VALUE)
//			return MIN_VALUE;
//		else
		if (s == Long.MIN_VALUE || s == Long.MAX_VALUE)
			throw new IllegalArgumentException();

		return new TaskOccurrence(s);
	}

	@Override
	public long start() {
		return x;
	}

	@Override
	public long end() {
		return x;
	}

	@Override
	public long mid() {
		return x;
	}

	@Override
	public boolean equals(Object o) {
		return this == o || (o instanceof TaskOccurrence O && x == O.x);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(x);
	}

	@Override
	public boolean intersects(HyperRegion _y) {
		LongInterval y = (LongInterval) _y;
		long ys = y.start();
		return ys==ETERNAL || Intervals.intersectsRaw(x, ys, y.end());//ys <= this.x && y.end() >= this.x);
	}
	@Override
	public boolean contains(HyperRegion _y) {
		LongInterval y = (LongInterval) _y;
		//return y.start() == x && y.end() == x;
		long ys = y.start();
		return ys!=ETERNAL && Intervals.containsRaw(x, x, ys, y.end());//ys <= this.x && y.end() >= this.x);
	}
}