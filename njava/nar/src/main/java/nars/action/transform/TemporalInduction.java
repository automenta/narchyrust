package nars.action.transform;

import jcog.Is;
import jcog.random.RandomBits;
import nars.Deriver;
import nars.NAL;
import nars.NALTask;
import nars.Premise;
import nars.deriver.reaction.NativeReaction;
import nars.deriver.util.DeriverTaskify;
import nars.task.proxy.SpecialNegTask;
import nars.term.util.transform.VariableShift;
import nars.truth.dynamic.DynConj;
import nars.truth.dynamic.DynImpl;
import nars.truth.dynamic.DynTruth;
import nars.unify.constraint.TermMatch;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

/**
 * B, A, --is(B,"==>") 				   |- polarizeBelief((polarizeTask(B) ==> A)), (Belief:AbductionDD, Time:Sequence)
 * B, A, --is(A,"==>") 				   |- polarizeTask((polarizeBelief(A) ==> B)), (Belief:InductionDD, Time:Sequence)
 * B, A, --is(A,"==>"), --is(B,"==>")  |- (polarizeTask(B) && polarizeBelief(A)),  (Belief:IntersectionDD, Time:Sequence)
 */
@Is("New_riddle_of_induction")
public abstract class TemporalInduction extends NativeReaction implements TemporalComposer {

	static final boolean allowVars = true;

	protected TemporalInduction(boolean task_beliefOrGoal) {
		taskPunc(task_beliefOrGoal ? BELIEF : GOAL);

		if (!allowVars) {
			hasAny(PremiseTask, Variables, false);
			hasAny(PremiseBelief, Variables, false);
		}

		//isAny(TheBelief, Op.Taskables);
		hasBeliefTask(true);
		noOverlap();
	}

	@Override
	protected final void run(Deriver d) {
		Premise p = d.premise;
		NALTask xy = induct(p.task(), p.belief(), d);
		if (xy!=null)
			d.add(xy, false);
	}

	/** a will be before or during b */
	private @Nullable NALTask induct(NALTask a, NALTask b, Deriver d) {

		int av = a.term().complexity(), bv = b.term().complexity();
		int mv = d.complexMax;
		if (av + bv + 1 > mv)
			return null; //too large

		if (!fwd(a, b, d)) {
			//swap
			NALTask c = a;
			a = b;
			b = c;
		}

		a = filter(a, 0, d); if (a == null) return null;
		b = filter(b, 1, d); if (b == null) return null;

		return inductTask(a, b, d);
	}

	@Nullable private NALTask inductTask(final NALTask a, NALTask b, Deriver d) {
		if (allowVars)
			b = VariableShift.varShift(b, a.term(), true, true);

		return new DeriverTaskify(model(a, b), d, a, b).taskClose();
	}

	protected NALTask filter(NALTask x, int component, Deriver d) {
		return x;
	}

	protected boolean fwd(NALTask a, NALTask b, Deriver d) {
		return true;
	}

	protected abstract DynTruth model(NALTask a, NALTask b);

	public static final class ImplInduction extends TemporalInduction {

		/**
		 *  +1 forward or eternal
		 *   0 auto (time ordering)
		 *  -1 reverse
		 */
		private final int dir;

		/** +1 pos subject term
		 *   0 auto (truth freq)
		 *  -1 neg subject term
		 * */
		private final int subjPolarity;


		public ImplInduction(int subjPolarity, int dir) {
			super(true);

			this.dir = dir;
			this.subjPolarity = subjPolarity;

			isNot(PremiseTask, IMPL);
			isNot(PremiseBelief, IMPL);
			//this.fwd = fwd;

			//neq(TheTask, TheBelief);

			//in IMPL, prevent causal loops and inverting oscillators:
			//  (probably since it may confuse strong deduction results)
		}

		@Override
		protected boolean fwd(NALTask a, NALTask b, Deriver d) {
			return switch (dir) {
				case  0 ->
					d.randomBoolean(); //FAIR
					//d.randomBoolean(fwdProb(a,b));
				case +1 -> a.start() <= b.start();
				case -1 -> b.start() <= a.start();
				default -> throw new UnsupportedOperationException();
			};
		}

//		/** fwd = prob that a is more polarized than b */
//		private static float fwdProb(NALTask a, NALTask b) {
//			double ap = a.polarity(), bp = b.polarity();
//			double abp = ap + bp;
//			if (abp < Double.MIN_NORMAL)
//				return 0.5f;
//			else
//				return (float)(ap / abp);
//		}

		@Override
		protected DynTruth model(NALTask a, NALTask b) {
			return DynImpl.DynImplInduction;
		}

		@Override
		protected NALTask filter(NALTask x, int component, Deriver d) {
			return component == 0 &&
					!subjPolarity(x, d) ?
					SpecialNegTask.neg(x) : x;
		}

		/** true = positive, false = negative */
		private boolean subjPolarity(NALTask x, Deriver d) {
			return switch (subjPolarity) {
				case -1 -> false;
				case +1 -> true;
				case 0 -> subjPolarityAuto(x);
				case 2 -> subjPolarityAutoStochastic(x, d.rng);
				default -> throw new UnsupportedOperationException();
			};
		}

		private static boolean subjPolarityAuto(NALTask x) {
			return x.POSITIVE();
		}

		private static boolean subjPolarityAutoStochastic(NALTask x, RandomBits rng) {
			return rng.nextBooleanFast16(x.freq());
		}
	}

	public static final class DisjInduction extends ConjInduction {

		public DisjInduction(int polarityX, int polarityY) {
			super(polarityX, polarityY);

//			if (!NAL.temporal.DISJ_INDUCT_SEQ) {
			iffNot(PremiseTask, TermMatch.SEQ);
			iffNot(PremiseBelief, TermMatch.SEQ);
//			}
		}

		@Override
		protected DynTruth model(NALTask a, NALTask b) {
			return DynConj.Disj;
		}

	}

	public static class ConjInduction extends TemporalInduction {

		/** switches between stochastic and deterministic component polarity */
		private static final boolean stochastic = NAL.temporal.TEMPORAL_INDUCTION_POLARITY_STOCHASTIC_CONJ;

		private final int polarityX, polarityY;

		public ConjInduction(int polarityX, int polarityY) {
			super(true);

			this.polarityX = polarityX;
			this.polarityY = polarityY;

			if (!NAL.term.CONJ_INDUCT_IMPL) {
				hasNot(PremiseTask, IMPL);
				hasNot(PremiseBelief, IMPL);
			}

		}

		@Override
		protected DynTruth model(NALTask a, NALTask b) {
			return DynConj.Conj;
		}

		@Override
		protected NALTask filter(NALTask x, int component, Deriver d) {
			if (!NAL.temporal.CONJ_INDUCT_NEG_SEQUENCES && x.term().SEQ())
				return x;

			int p = component == 0 ? polarityX : polarityY;

			if (p == 0) p = polarity(x, d) ? +1 : -1; //choose

			return p < 0 ? SpecialNegTask.neg(x) : x;
		}

		private static boolean polarity(NALTask x, Deriver d) {
            //deterministic
            return stochastic ? d.randomBoolean(x.freq()) :
					!x.NEGATIVE(); //stochastic
		}
	}

}