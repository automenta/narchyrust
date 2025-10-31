package nars.truth.util;

import jcog.Util;
import nars.task.util.TaskList;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.jetbrains.annotations.Nullable;

public abstract class TaskEviList extends TaskList {
	/**
	 * active evidence cache
	 */
	@Nullable protected double[] evi;

	protected TaskEviList(int initialCap) {
		super(initialCap);
	}

	@Deprecated public final boolean valid(int i) {
		return nonNull(i) && eviValid(evi[i]);
	}

	protected final boolean nonNull(int i) {
		return items[i] != null;
	}

	public final IntToFloatFunction eviPrioritizer() {
		return new EviPrioritizer();
	}

	public double eviMax() {
		return Util.max(evi); //TODO max(evi, 0, size)
	}
	public double eviSum() {
		return Util.sum(evi); //TODO max(evi, 0, size)
	}

	/** integrated evi, not mean evi */
	public final double evi(int i) {
		return evi[i];
	}

	private final class EviPrioritizer implements IntToFloatFunction {

		private double eSum;

		@Override public float valueOf(int i) {
			if (i == 0) this.eSum = eSum();
            return (float) (items[i].priElseZero() * evi(i) / eSum);
		}

		private double eSum() {
			var eSum = 0.0;
			int size = TaskEviList.this.size;
			for (var j = 0; j < size; j++)
				eSum += evi(j);
			return eSum;
//			var eSum = new KahanSum();
//			for (var j = 0; j < size; j++)
//				eSum.add(evi(j));
//			return eSum.value();
		}

//		public long creation() {
//            long t = Util.maxIgnoreLongMax(NALTask::creation, items, size);
//            return t == Long.MIN_VALUE ? TIMELESS : t; //HACK
//		}

	}
}