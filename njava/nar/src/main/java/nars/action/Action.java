package nars.action;

import jcog.Is;
import nars.Term;
import nars.control.Cause;
import nars.deriver.reaction.PatternReaction;
import nars.deriver.reaction.Reaction;
import nars.term.control.PREDICATE;


/**
 * rankable branch in the derivation fork
 *
 * instances of How represent a "mental strategy" of thought.
 * a mode of thinking/perceiving/acting,
 * which the system can learn to
 * deliberately apply.
 *
 * a How also implies the existence for a particular reason Why it Should.
 * so there is functional interaction between How's and Why's
 * and their combined role in thinking What-ever.
 *
 * see: https://cogsci.indiana.edu/pub/parallel-terraced-scan.pdf
 *
 * instruments the runtime resource consumption of its iteratable procedure.
 * this determines a dynamically adjusted strength parameter
 * that the implementation can use to modulate its resource needs.
 * these parameters are calculated in accordance with
 * other instances in an attempt to achieve a fair and
 * predictable (ex: linear) relationship between its scalar value estimate
 * and the relative system resources it consumes.
 * <p>
 * records runtime instrumentation, profiling, and other telemetry for a particular Causable
 * both per individual threads, and collectively
 */
@Is({
	"Effects_unit",
	"Utility_maximization_problem",
	"Optimal_decision",
	"Action_axiom",
	"Norm_(artificial_intelligence)"
})
public abstract class Action<X> extends PREDICATE<X> {

	public final Cause<Reaction<X>> why;

	Action(Term id, Cause<Reaction<X>> why) {
		super(id);
		this.why = why;
	}

	protected Action(Cause<Reaction<X>> why) {
		this(actionName(why), why);
	}

	@Override
	public boolean deterministic() {
		return false;
	}

	@Override public final float cost() {
		return Float.POSITIVE_INFINITY;
	}

	private static <X> Term actionName(Cause<Reaction<X>> why) {
		return why.name instanceof PatternReaction pr ?
				pr.idConclusion : why.name.term();
	}

//	public PREDICATE<Deriver> deferred() {
//		return new DeferredAction(why);
//	}

//	public static final class DeferredAction extends Action {
//
//		DeferredAction(Cause<Reaction> why) {
//			super(why);
//		}
//
//		@Override
//		public boolean test(Deriver d) {
//			d.can(why.id);
//			return true;
//		}

//		static final MethodType VOID_INT = MethodType.methodType(void.class, int.class);
//		static final MethodHandle SET_LONG, SET_INT;
//		static {
//			MethodHandle setLong, setInt;
//			try {
//				Lookup M = lookup();
//				setLong = MethodHandles.privateLookupIn(LongArrayBitSet.class, M).findSpecial(LongArrayBitSet.class, "set", VOID_INT, LongArrayBitSet.class);
//				setInt = MethodHandles.privateLookupIn(IntBitSet.class, M).findSpecial(IntBitSet.class,       "set", VOID_INT, IntBitSet.class);
//			} catch (NoSuchMethodException | IllegalAccessException e) {
//				setLong = setInt = null;
//				System.exit(1);
//			}
//			SET_LONG = setLong;
//			SET_INT = setInt;
//		}

//		@Override
//		public MethodHandle method(Deriver d) {
//			throw new TODO();
////			final MethodHandle m = (d.hows instanceof LongArrayBitSet ?
////					SET_LONG : SET_INT).bindTo(d.hows);
////			return filterReturnValue(
////						insertArguments(m, 0, why.id),
////					CONSTANT_TRUE);
//		}
//	}
}