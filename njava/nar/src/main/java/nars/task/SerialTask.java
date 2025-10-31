package nars.task;

import jcog.Util;
import jcog.pri.Prioritizable;
import nars.NALTask;
import nars.Term;
import nars.Truth;
import nars.task.proxy.SpecialOccurrenceTask;
import nars.truth.AbstractMutableTruth;

/**
 * overrides equality and hashcode impl because these are generated with (mostly-)unique serial stamp by a sensor or other internal process #
 */
public non-sealed class SerialTask extends AbstractNALTask {

	private volatile long start, end;

	/**
	 * @param stamp typically a 1-element stamp
	 */
	public SerialTask(Term term, byte punc, Truth truth, long start, long end, long[] stamp) {
		super(term, punc, truth, start, end, stamp);
		this.start = start;
		this.end = end;
	}

	@Override public final boolean equals(Object x) {
		return x==this;
	}

	@Override
	protected int hashCalculate(long start, long end) {
		return System.identityHashCode(this);
		//return Util.hashCombine(term.hashCode(), stamp[0]);
		//return Long.hashCode(stamp[0]);
	}

	public final NALTask setUncreated() {
		return setCreation(TIMELESS);
	}

	public final SerialTask occ(long s, long e) {
		if (e < s)
			throw new UnsupportedOperationException();

		//special handling to avoid reverse ranged in between the two set's
		long s0 = start, e0 = end;
		long sMin = Math.min(s0, s);
		long eMax = Math.max(e0, e);

		//expand
		start = sMin; end = eMax;

		//fence?

		//then shrink, if necessary
		start = s; end = e;

		return this;
	}

	public final void setStart(long s) {
		if (end < s) throw new UnsupportedOperationException();
		this.start = s;
	}

	public final void setEnd(long e) {
		if (start > e)
			this.start = e;
			//throw new UnsupportedOperationException();
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

	/** caution */
	public final void set(long start, long end, float f, double evi, float pri) {
		setUncreated(); //forces novelty refresh
		occ(start, end);
		truth(f, evi);
		pri(pri);
	}

	/** caution */
	public final void truth(float f, double evi) {
		((AbstractMutableTruth)truth()).freq(f).evi(evi);
	}

	/** returns an immutable snapshot of this task to prevent the effects of this potentially being stretched */
	public NALTask freeze() {
		return new SpecialOccurrenceTask(this, start, end).copyMeta(this);
	}

	/** updates priority of stretching task */
	public void priStretch(float pri, long prevRange) {

		boolean samePri = false;

		long nextRange = range();

		//if this is an existing signal,
		//modify the priority in proportion to how much time has already
		//elapsed.  this way the previous priority is preserved as momentum
		float existingPri;
		float nextPri;
		if (prevRange == 0 || (samePri = Prioritizable.equals(pri, existingPri = pri()))) {
			nextPri = pri;
		} else {
			/* this _should_ work whether the signal task grows OR shrinks. */
			float dRange = ((float) Math.abs(nextRange - prevRange)) / Math.max(prevRange, nextRange);
			nextPri = Util.lerpSafe(dRange, existingPri, pri);
		}

		if (!samePri)
			pri(nextPri);

	}
}