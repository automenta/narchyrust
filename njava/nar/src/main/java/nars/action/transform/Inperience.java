package nars.action.transform;

import jcog.math.Intervals;
import nars.*;
import nars.action.TaskTransformAction;
import nars.task.SerialTask;
import nars.task.proxy.SpecialPuncTermAndTruthTask;
import nars.term.Functor;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.control.PREDICATE;
import nars.term.util.transform.VariableTransform;
import nars.term.var.Variable;
import nars.time.When;
import nars.truth.PreciseTruth;
import nars.unify.constraint.TermMatch;
import org.jetbrains.annotations.Nullable;

import static nars.$.$$;
import static nars.Op.*;
import static nars.TruthFunctions.c2e;
import static nars.TruthFunctions.weak;
import static nars.term.atom.Bool.Null;
import static nars.term.util.Image.imageNormalize;

/**
 * Internal Experience (NAL9)
 * To remember activity as internal action operations
 * <p>
 * https:
 * "Imperience": http:
 * <p>
 * snapshots of belief table aggregates, rather than individual tasks
 */
public abstract class Inperience extends TaskTransformAction {


	private static final boolean intersectWithFocus = true;

	/**
	 * reified belief verb
	 */
	public static final Atomic believe = Atomic.atomic("believe");

	/**
	 * reified goal verb
	 */
	public static final Atomic want = Atomic.atomic("want");

	/**
	 * reified question verb
	 */
	private static final Atomic wonder = Atomic.atomic("wonder");

	/**
	 * reified quest verb (note: OpenNARS calls this 'evaluate')
	 */
	private static final Atomic plan = Atomic.atomic("plan");


	public static final Atomic IF = Atomic.atomic("if");
	public static final Atomic AND = Atomic.atomic("and");

	private static final boolean AllowSerialTasks = false;
	private static final boolean seq = false, impl = false;


//	private static final Atomic SEQ = Atomic.the("seq");
//	private static final Atomic BEFORE = Atomic.the("before");
//	private static final Atomic AFTER = Atomic.the("after");

	Inperience(byte punc) {
		super();
		pre(premiseTaskLink.neg());
		taskPunc(punc);
		taskEqualsBelief();
		hasBeliefTask(false);

		if (!seq)
			iffNot(taskPattern, TermMatch.SEQ);
		if (!impl)
			isNot(taskPattern, IMPL);

		pre(InperiencePredicate);

		//HACK temporary disable temporal-containing terms, which also spam a lot
		//hasAny(PremiseTask, VAR_INDEP.bit, false);
	}

	static private final PREDICATE<Deriver> InperiencePredicate = new PREDICATE<>($$("(" + Inperience.class.getSimpleName() + "_filter" + ")")) {

		@Override
		public boolean test(Deriver d) {
			NALTask x = d.premise.task();
			if (!AllowSerialTasks && x instanceof SerialTask)
				return false;

			if (intersectWithFocus) {
				//TODO rewrite this test as PREDICATE
				if (!Intervals.intersects(x, d.focus.when()))
					return false;
//			long s = d.focus.now();
//			long e = s; //HACK TODO dur range?
//			if (!x.intersects(s, e))
//				return false;
			}
			return true;
		}

		@Override
		public float cost() {
			return 0.5f;
		}
	};


	public abstract Term reify(NALTask t, Term self);

//	public final Inperience noTemporals() {
//		hasAny(PremiseTask, Temporals,false);
//		return this;
//	}
	public final Inperience timelessOnly() {
		//hasAny(PremiseTask, Temporals,false);
		iff(PremiseTask, TermMatch.Timeless);
		return this;
	}

	public static class BeliefInperience extends Inperience {
		/**
		 * levels=0 |- negated at root level
		 * levels=1 |-     { -1, 0, +1 }
		 * levels=2 |- { -2, -1, 0, +1, +2 }
		 */
		final int truthLevels;
		public BeliefInperience(byte punc, int truthLevels) {
			super(punc);
			this.truthLevels = truthLevels;
		}

		@Override
		public Term reify(NALTask t, Term self) {
			Term y = reifyRoot(t.term());
			return y == Null ? Null : $.func(verb(t),
					truthLevels > 0 ?
						new Term[] { self, y, reifyFreq(t.freq()) }
						:
						new Term[] { self, y.negIf(t.NEGATIVE()) }
					);
		}


		/** freq digitizer */
		private Term reifyFreq(float freq) {
			return Int.i(Math.round(reifyFreqPrecise(freq)));
		}

		private float reifyFreqPrecise(float freq) {
			return (freq-0.5f) * 2 * truthLevels;
		}

		@Override
		protected @Nullable When<Truth> truth(NALTask t, Deriver d) {
			return truth(t, d, then(t, d));
		}

		@Nullable
		private When<Truth> truth(NALTask t, Deriver d, When<Truth> w) {
			Truth y = t.truth();
			//t.truth(w.evi.s, w.evi.e);

			double c = weak(y.conf());

			//decrease conf by distance from the ideal reified value
			if (truthLevels>0) {
				c *= 1 - err(y);
			} else {
				c *= t.polarity();
			}

			double e = c2e(c);
			if (e < d.eviMin) {
//				if (NAL.truth.EVI_STRICT)
					return null;
//				else
//					e = d.eviMin;
			}

            return (w.x = PreciseTruth.byEvi(1, e, d.nar)) == null ? null : w;
        }

		/** % error in the potential representation of a truth value */
		private float err(Truth tt) {
			float f0 = reifyFreqPrecise(tt.freq());
			int nearest = Math.round(f0);
			return Math.abs(f0 - nearest);
		}

	}

	private static Atomic verb(Task t) {
		return verb(t.punc());
	}

	private static When<Truth> then(NALTask t, Deriver d) {
		//return at(t.mid(), t, d);
    	return new When(null, t,0);
	}


	public static class QuestionInperience extends Inperience {

		public QuestionInperience(byte punc) {
			super(punc);
		}


		private static When<Truth> pri(NALTask t, Deriver d, When<Truth> w) {
			float confDefault = d.nar.confDefault(BELIEF);
			double c =
					//Util.lerpSafe(t.priElseZero(), e2c(d.eviMin), confDefault);
					weak(weak(confDefault));
			w.x = PreciseTruth.byEvi(1, c2e(c), d.nar);
			return w;
		}

		@Override
		protected @Nullable When<Truth> truth(NALTask t, Deriver d) {
			return pri(t, d, then(t,d));
		}

		@Override
		public Term reify(NALTask t, Term self) {
			return $.func/*Img*/(verb(t), self, reifyRoot(t));
		}

	}

	private static Term reifyRoot(Termed t) {
		Term x = t.term();
		return postProcess(switch (x.op()) {
			//TODO
//			case CONJ -> null;
//			case IMPL -> null;
			default -> imageNormalize(x);
		});
	}

	private static Atomic verb(byte punc) {
		return switch (punc) {
			case BELIEF -> believe;
			case GOAL -> want;
			case QUESTION -> wonder;
			case QUEST -> plan;
			default -> throw new UnsupportedOperationException();
		};
	}


	private static Term postProcess(Term x) {
		//x = Retemporalize.retemporalizeXTERNALToDTERNAL.apply(x);
		x = x.hasAny(VAR_QUERY) ? VariableTransform.queryToDepVar.apply(x) : x;
		return x instanceof Bool ? Null : x; //HACK
	}

	/**
	 * attempt to filter believe(believe(....
	 */
	private static boolean isRecursive(Task t, Term self) {
		Term x = imageNormalize(t.term());
		Atomic f = Functor.func(x);
		if (f!=null && f.equals(verb(t))) {
			Term inperiencer = x.sub(0).sub(0);
			return inperiencer instanceof Variable || inperiencer.equals(self);
		}
		return false;
	}

	@Override
	@Nullable protected NALTask transform(NALTask x, Deriver d) {
		Term self = d.nar.self();
		if (isRecursive(x, self)) return null;

		@Nullable When<Truth> when =
				truth(x, d);
				//truthNow(x, d);
		if (when == null) return null;

		Term R = reify(x, self);
		if (R instanceof Bool)
			return null; //TODO eliminate with constraints
		if (R.complexity() > d.complexMax)
			return null; //too large

        return SpecialPuncTermAndTruthTask.proxy(R, BELIEF, when.x, x);

//		if (S == null)
//			return null; //HACK TODO why?
//
//		return S.copyMeta(x);
		//return NALTask.task(R, BELIEF, when.x, when.s, when.e, x.stamp());
	}


	//	@Nullable protected abstract LongInterval truthNow(NALTask t, Deriver d);
	@Deprecated @Nullable protected abstract When<Truth> truth(NALTask t, Deriver d);


//	/** TODO use RecursiveTermTransform */
//	@Deprecated private static Term reifyTerm(Term x) {
//
//		if (x instanceof Img) {
//			return $.varDep(BinTxt.uuid64() /* HACK */);
//		}
//		if (x instanceof Neg) {
//			Term xu = x.unneg();
//			Term y = reifyTerm(xu);
//			return y == xu ? x : y.neg();
//		} else if (x./*unneg().*/IMPL()) {
//			Term subj = x.sub(0);
//			Term s = reifyTerm(subj), p = reifyTerm(x.sub(1));
//			int dt = x.dt();
//			if (dt == 0 || dt == DTERNAL || dt == XTERNAL) {
//				x = $.func(IF, s, p);
//			} else {
//				Term interval;
//				if (dt > -subj.eventRange())
//					interval = AFTER;
//				else
//					interval = BEFORE;
//				x = $.func(IF, s, interval/*interval(dt)*/, p);
//			}
//		} else if (x.CONJ()) {
//            if (x.SEQUENCE()) {
//				//TODO {first, AFTER({second, AFTER({third, ...}) }) }  ?
//
//				TermList l = new TermList();
//
//				ConjList e = ConjList.events(x, 0, x.dt()==DTERNAL /* HACK */, false);
//				for (Term what : e)
//					l.add(reifyTerm(what));
//
//				x = $.func(SEQ, PROD.the((Subterms)l));
//			} else {
//				x = $.func(AND, SETe.the(x.subterms().array(Inperience::reifyTerm, Term[]::new)));
//			}
//		}
//		//TODO catch vol limit here
//		return x;
//	}


}