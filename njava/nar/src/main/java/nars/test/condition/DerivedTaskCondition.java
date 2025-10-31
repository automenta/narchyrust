package nars.test.condition;

import jcog.Util;
import jcog.data.set.PrioritySet0;
import jcog.math.LongInterval;
import nars.NALTask;
import nars.NAR;
import nars.Op;
import nars.Term;
import nars.term.Neg;
import nars.truth.Truthed;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.Predicate;

import static java.lang.Float.NaN;

/**
 * specific task matcher with known boundary parameters that can be used in approximate distance ranking calculations
 */
public class DerivedTaskCondition extends TaskCondition {

	protected final NAR nar;
	private final byte punc;
	private final Term term;
	private final LongLongPredicate time;
	private final float freqMin, freqMax;
	private final float confMin, confMax;
	private final long creationStart, creationEnd;
	private final Predicate<Term> termEquals;


	@Nullable protected PrioritySet0<NALTask> similar;
	int maxSimilars = 0;

	public DerivedTaskCondition(NAR n, long creationStart, long creationEnd, Term term, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time) throws RuntimeException {


		if (freqMax < freqMin)
			throw new RuntimeException("freqMax < freqMin");
		if (confMax < confMin) throw new RuntimeException("confMax < confMin");

		if (creationEnd - creationStart < 1)
			throw new RuntimeException("cycleEnd must be after cycleStart by at least 1 cycle");

		this.nar = n;
		this.time = time;

		this.creationStart = creationStart;
		this.creationEnd = creationEnd;

		this.confMax = Math.min(1.0f, confMax);
		this.confMin = Math.max(0.0f, confMin);
		this.punc = punc;

		if (term instanceof Neg) {
			term = term.unneg();
			freqMax = 1.0f - freqMax;
			freqMin = 1.0f - freqMin;
			if (freqMin > freqMax) {
				float f = freqMin;
				freqMin = freqMax;
				freqMax = f;
			}
		}

		this.freqMax = Math.min(1.0f, freqMax);
		this.freqMin = Math.max(0.0f, freqMin);

		this.term = term;
		this.termEquals = this.term.equals();

	}

	public long getFinalCycle() {
		return creationEnd;
	}

	@Override
	public void log(Logger logger) {

		super.log(logger);

		if (logger.isInfoEnabled()) {
			StringBuilder sb = new StringBuilder((1 + (similar != null ? similar.size() : 0)) * 2048);
			if (firstMatch != null) {
				sb.append("Exact:\n");
				log(firstMatch, sb);
			} else if (similar != null && !similar.isEmpty()) {
				sb.append("Similar:\n");
				for (NALTask s : similar)
					log(s, sb);
			}
			logger.info(sb.toString());
		}
	}

	public void log(NALTask t, Appendable sb) {
		NAR.proofAppend(t, sb);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + term.toString() + ((char) punc) + " %" +
			rangeStringN2(freqMin, freqMax) + ';' + rangeStringN2(confMin, confMax) + '%' + ' ' +
			" creation: (" + creationStart + ',' + creationEnd + ')';
	}


	@Override
	public boolean matches(@Nullable NALTask t) {
		byte punc = this.punc;
		if (t.punc() == punc) {
			if (coOccurr(t) && creationTimeMatches()) {
				if (!Op.BELIEF_OR_GOAL(punc) || truthMatches(t)) {
					if (termEquals.test(t.term())) {
						firstMatch = t;
						return true;
					}
				}
			}
		}

		if (similar != null && similar.add(t))
			similar.capacity(maxSimilars);

		return false;
	}
	public void similars(int maxSimilars) {
		this.similar = new PrioritySet0<>(x -> value(x, Float.NEGATIVE_INFINITY), new UnifiedSet<>(maxSimilars));
		this.maxSimilars = maxSimilars;
	}
	private boolean creationTimeMatches() {
		return Util.inIncl(nar.time(), creationStart, creationEnd);
	}

	public final boolean coOccurr(LongInterval t) {
		return time.accept(t.start(), t.end());
	}

	private boolean truthMatches(Truthed tt) {

		return
				Util.inIncl((float)tt.conf(), confMin, confMax)
				&&
				Util.inIncl(tt.freq(), freqMin, freqMax);
	}

	protected float value(NALTask task, float worstDiffNeg) {

		float worstDiff = -worstDiffNeg;

		float difference = 0;
		if (task.punc() != punc)
			difference += 1000;
		if (difference >= worstDiff)
			return NaN;

		Term tterm = task.term();
		difference +=
			100 * termDistance(tterm, term, worstDiff);
		if (difference >= worstDiff)
			return NaN;

		if (task.BELIEF_OR_GOAL()) {
			float f = task.freq();
			float freqDiff = Math.min(
				Math.abs(f - freqMin),
				Math.abs(f - freqMax));
			difference += 10 * freqDiff;
			if (difference >= worstDiff)
				return NaN;

			float c = (float) task.conf();
			float confDiff = Math.min(
				Math.abs(c - confMin),
				Math.abs(c - confMax));
			difference += 1 * confDiff;
			if (difference >= worstDiff)
				return NaN;
		}

		difference += 0.5f * (Math.abs(task.hashCode()) / (Integer.MAX_VALUE * 2.0f)); //HACK differentiate by hashcode

		return -difference;

	}

}