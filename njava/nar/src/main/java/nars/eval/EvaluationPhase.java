package nars.eval;

import jcog.Is;
import jcog.data.list.Lst;
import nars.Op;
import nars.Term;
import nars.term.Compound;
import nars.term.atom.Bool;
import org.jetbrains.annotations.Nullable;

import static nars.term.atom.Bool.*;

/** implements an individual evaluation pass */
@Is("Director_string") abstract sealed class EvaluationPhase {

	/** the starting term */
	public abstract Term start();

	public abstract @Nullable Term apply(Evaluation e, Term t);


	/** default interpreter impl */
	static final class Recipe extends Constant {

		private final Compound[] steps;

		/** pre-process: prioritize constant evaluables */
        Recipe(Term x, Lst<Compound> yy) {
			super(x);
			this.steps = yy.toArray(Op.EmptyCompoundArray);
		}

		@Override
		public @Nullable Term apply(Evaluation e, Term pre) {
			Term next = pre;
			for (var s : steps) {
				next = apply(s, e, next);
				if (!pre.equals(next)) {
					//terminate or force recompute
					//TODO determine if subsequent steps are actually affected, and fully or partially elide recompute
					break;
				}
			}
			return next;
		}

		private static Term apply(Compound a, Evaluation e, Term t) {
			var b = e.call(a);
			if (b!=null && !b.equals(a))
				return (b == Null ? b : t.replace(a, b, Evaluator.B));
			else
				return t;
		}
	}

	static non-sealed class Constant extends EvaluationPhase {
		final Term x;

		private static final Constant
			TRUE = new Constant(True),
			FALSE = new Constant(False),
			NULL = new Constant(Null);

		private Constant(Term x) {
			this.x = x;
		}

		@Override
		public final Term start() {
			return x;
		}

		static Constant the(Term a) {
            return switch (a) {
                case Bool bool -> {
                    if (a == Null) yield NULL;
                    else if (a == True) yield TRUE;
                    else /*if (a == False)*/ yield FALSE;
                }
                case null, default -> new Constant(a);
            };
		}

		@Override
		public Term apply(Evaluation e, Term t) {
			return x;
		}
	}

	static class Itself extends Constant {

		Itself(Compound a) {
			super(a);
		}

		@Override
		public Term apply(Evaluation e, Term t) {
			return e.call((Compound)x);
		}
	}

	static class PartOfItself extends Constant {
		private final Compound a;

		PartOfItself(Term x, Compound a) {
			super(x);
			this.a = a;
		}

		@Override
		public Term apply(Evaluation e, Term t) {
            var b = e.call(a);
            return b == null || b == Null ? b :
				t.replace(a, b, Evaluator.B);
		}
	}

	@Deprecated public static final EvaluationPhase NULL = new NullEvaluationPhase();

	private static final class NullEvaluationPhase extends EvaluationPhase {

		@Override
		public Term start() {
			return null;
		}

		@Override public Term apply(Evaluation e, Term t) {
			return Null;
		}
	}
}