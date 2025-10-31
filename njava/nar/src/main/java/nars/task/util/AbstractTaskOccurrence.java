package nars.task.util;

import jcog.tree.rtree.HyperRegion;

abstract class AbstractTaskOccurrence implements TaskRegion {

	@Override
	public abstract boolean intersects(HyperRegion x);

	@Override
	public abstract boolean contains(HyperRegion x);

	@Override
	public final TaskRegion mbr(HyperRegion r) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final int dim() {
		return 3;
	}

	@Override
	public final float freqMin() {
		return 0;
	}

	@Override
	public final float freqMax() {
		return 1;
	}

	@Override
	public final float confMin() {
		return 0;
	}

	@Override
	public final float confMax() {
		return 1;
	}

	@Override
	public final double coord(int dimension, boolean maxOrMin) {
		throw new UnsupportedOperationException();
	}

}