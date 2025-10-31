package nars.truth.dynamic;

import jcog.util.ObjectLongLongPredicate;
import nars.Term;
import nars.TruthFunctions;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.util.TermException;
import nars.time.Tense;
import nars.truth.DynTaskify;

import static nars.Op.*;

public enum DynImplConj {
	;

	/** subj && == union (disjunction) */
	public static final DynTruth DynImplConjSubj = new DynImplSect(true) {
		@Override
		protected TruthFunctions truthFn() {
			return TruthFunctions.Union;
		}

		@Override
		public boolean decompose(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
			Subterms ss = superterm.subtermsDirect();
			Term subj = ss.sub(0);
			if (!(subj instanceof Compound subjC))
				throw new TermException("subj not Compound in " + getClass().getSimpleName(), superterm);

			return decomposeImplConj(superterm, start, end, each,
					ss.sub(1),
					subjC,
					true, false /* reconstruct as-is; union only applies to the truth calculation */);
		}

		@Override
		public Term recompose(Compound superterm, DynTaskify components) {
			return recomposeStatement(superterm, components, true, false);
		}

	};

	/** subj || == intersection (conjunction) */
	@Deprecated public static final DynTruth DynImplDisjSubj = new DynImplDisjSubj();

	/** TODO may need to canonically sort order for applying binary truth function to n-ary (n > 2) disj's */
	public static final DynTruth DynImplDisjMixSubj = new DynImplDisjSubj() {
		@Override
		protected TruthFunctions truthFn() {
			return TruthFunctions.Mix;
		}
	};


	public static final DynTruth DynImplConjPred = new DynImplSect(false) {

		@Override
		public boolean decompose(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
			Subterms ss = superterm.subtermsDirect();
			return decomposeImplConj(superterm, start, end, each,
					DynStatement.stmtCommon(subjOrPred, ss),
					(Compound) DynStatement.stmtCommon(!subjOrPred, ss),
					false, false);
		}

	};

	private static boolean decomposeImplConj(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each, Term common, Compound decomposed, boolean subjOrPred, boolean negateConjComponents) {
		return model(decomposed).decompose(decomposed, start, end, new ObjectLongLongPredicate<>() {
			final int _superDT = superterm.dt();
			final int sdt = _superDT == DTERNAL ? 0 : _superDT;
			final boolean xternal = sdt==XTERNAL || decomposed.TEMPORAL_VAR();
			final int decRange = xternal ? XTERNAL : subjOrPred ? decomposed.seqDur() : 0;

			@Override
			public boolean accept(Term what, long s, long e) {
				int innerDT = xternal ? XTERNAL : innerDT(s);

				Term i = subjOrPred ?
					IMPL.the(what.negIf(negateConjComponents), innerDT, common)
					:
					IMPL.the(common, innerDT, what).negIf(negateConjComponents);

				return i.unneg().IMPL() && each.accept(i, start, end);
			}

			private int innerDT(long s) {
				int innerDT;
				long d = s != ETERNAL && start != ETERNAL ? s - start : 0;
				innerDT = Tense.occToDT(subjOrPred ? decRange - d : d) + sdt;
				return innerDT;
			}
		});
	}

	private static DynTruth model(Compound decomposed) {
		DynTruth model;
		if (decomposed.CONJ()) {
			model = DynConj.Conj;
		} else {
			assert(decomposed.INH());
			model = decomposed.sub(0).CONJ() ? DynStatement.DynInhSubjSect : DynStatement.DynInhPredSect;
		}
		return model;
	}

	private static class DynImplDisjSubj extends DynImplSect {

		DynImplDisjSubj() {
			super(true);
		}

		@Override
		public boolean decompose(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
			Subterms ss = superterm.subtermsDirect();
			Term subj = ss.sub(0);
			assert (subj instanceof Neg);
			return decomposeImplConj(superterm, start, end, each, ss.sub(1),
				(Compound) (subj.unneg()), true, true);
		}

		@Override
		public Term recompose(Compound superterm, DynTaskify components) {
			return recomposeStatement(superterm, components, true, true);
		}
	}

}