package nars.truth.dynamic;

import jcog.math.Intervals;
import jcog.util.ObjectLongLongPredicate;
import nars.*;
import nars.term.Compound;
import nars.truth.DynTaskify;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.ETERNAL;
import static nars.TruthFunctions.e2c;


/** dynamic task calculation model */
public abstract class DynTruth {

	public abstract Truth truth(DynTaskify d /* eviMin, */);

	/** decompose term into evaluable components */
	public abstract boolean decompose(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each);

	/**
	 * construct dynamic task's term, from components
	 */
	@Nullable public abstract Term recompose(Compound superterm, DynTaskify d);

	/**
	 * number of components
	 */
	public abstract int componentsEstimate();

	/** recursion depth cut-off
	 *     0 = root only
	 * */
	public int levelMax() {
		return Integer.MAX_VALUE; //no-limit
	}

	@Nullable public Predicate<NALTask> preFilter(int component, DynTaskify d) {
		return null;
	}

	/** whether component occurrence times should be dithered internally
	 * (ex: to ensure dt in temporal terms are dithered) */
	public boolean ditherComponentOcc() {
		return NAL.answer.DYN_DITHER_COMPONENT_OCC_DEFAULT;
	}

	/** whether to project components to the target time */
	public boolean projectComponents(Term template) {
		return true;
	}

	/** pair-of-Tasks balanced timing & truth handling
	 *
	 * INTERSECT:
	 * 		TODO diagram
	 *
	 * DISJOINT:
	 *        [   S | ] ____ [E]
	 *              ^          ^
	 *              |----------|
	 *              a          b
	 */
	@Deprecated protected static Truth pair(DynTaskify d, FloatFloatToFloatFunction freq, NALTask S, NALTask E) {
		if (S.start() > E.start()) { var se = S; S = E; E = se; /* swap */ }

		long a, b;
		long ss = S.start(), se = S.end();
		long es = E.start(), ee = E.end();
		long sr = se - ss, er = ee - es;

		if (ss == ETERNAL && es == ETERNAL) {
			a = b = ETERNAL;
		} else if (ss == ETERNAL) {
			a = es; b = ee;
		} else if (es == ETERNAL) {
			a = ss; b = se;
		} else {
			//Union
			long[] ab = Intervals.unionArray(ss, se, es, ee);
			a = ab[0]; b = ab[1];
		}

		return pair(d, freq, S, E, a, b);
	}

	@Deprecated private static @Nullable Truth pair(DynTaskify d, FloatFloatToFloatFunction freq, NALTask S, NALTask E, long a, long b) {
		double C = TruthFunctions.confCompose(pairConf(S, a, b), pairConf(E, a, b));
		if (C <= NAL.truth.CONF_MIN) return null;

		d.occ(a, b);

		return $.t(freq.valueOf(S.freq(), E.freq()), C);
	}

	static double pairConf(NALTask x, long a, long b) {
		return e2c(x.eviMean(a, b,0, 0));
	}

}