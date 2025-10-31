package nars.test.condition;


import jcog.Str;
import nars.NALTask;
import nars.Term;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * TODO evolve this into a generic tool for specifying constraints and conditions
 * on memory (beliefs, and other measurable quantities/qualities).
 * use these to form adaptive runtime hypervisors ensuring optimal and correct operation
 */
public abstract class TaskCondition implements NARCondition, Predicate<NALTask> {


	protected NALTask firstMatch;

	/**
	 * whether to apply meta-feedback to drive the reasoner toward success conditions
	 */
	private boolean matched;

	public static String rangeStringN2(float min, float max) {
		return '(' + Str.n2(min) + ',' + Str.n2(max) + ')';
	}

	/**
	 * a heuristic for measuring the difference between terms
	 * in range of 0..100%, 0 meaning equal
	 */
	public static float termDistance(Term a, Term b, float ifLessThan) {
		if (a.equals(b)) return 0;

		float dist = 0;
		if (a.opID() != b.opID()) {

			dist += 0.4f;
			if (dist >= ifLessThan) return dist;
		}

		if (a.subs() != b.subs()) {
			dist += 0.3f;
			if (dist >= ifLessThan) return dist;
		}

		if (a.struct() != b.struct()) {
			dist += 0.2f;
			if (dist >= ifLessThan) return dist;
		}

		dist += Str.levenshteinFraction(
			a.toString(),
			b.toString()) * 0.1f;

		if (a.dt() != b.dt()) {
			dist *= 2;
		}

		return dist;
	}

	public abstract boolean matches(@Nullable NALTask task);

	@Override
	public final boolean test(NALTask t) {

		if (!matched && matches(t)) {
			matched = true;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public long getFinalCycle() {
		return Long.MAX_VALUE;
	}

	@Override
	public final boolean getAsBoolean() {
		return matched;
	}

}