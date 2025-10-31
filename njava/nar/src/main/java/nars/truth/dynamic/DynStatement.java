package nars.truth.dynamic;

import jcog.Util;
import jcog.util.ObjectLongLongPredicate;
import nars.NALTask;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.table.util.DynTables;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.util.TermException;
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjList;
import nars.time.Tense;
import nars.truth.DynTaskify;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.term.atom.Bool.True;

public enum DynStatement {
	;

	public static final DynTruth DynInhSubjSect = new DynStatementSect(true);

	public static final DynTruth DynInhPredSect = new DynStatementSect(false);

	private static final DynTruth[] DynInhSubSectArray = {DynInhSubjSect}, DynInhPredSectArray = {DynInhPredSect}, DynInhSubjPredSectArray = {DynInhSubjSect, DynInhPredSect};


	/**
	 * statement common component
	 */
	static Term stmtCommon(boolean subjOrPred, Subterms superterm) {
		return superterm.sub(subjOrPred ? 1 : 0);
	}

	public static @Nullable DynTruth[] dynInhSect(Subterms ii) {


        Term s = ii.sub(0), p = ii.sub(1);
        boolean sc = DynConj.condDecomposeable(s, false),
                pc = DynConj.condDecomposeable(p, false);
        if ((sc || pc) && DynTables.varSeparate(s, p)) {
            if (sc && pc)
                return DynInhSubjPredSectArray;
            else if (sc)
                return DynInhSubSectArray;
            else if (pc)
                return DynInhPredSectArray;
        }
        return null;
	}


	/**
	 * functionality shared between INH and IMPL
	 */
	static class DynStatementSect extends DynSect {

		final boolean subjOrPred;

		DynStatementSect(boolean subjOrPred) {
			this.subjOrPred = subjOrPred;
		}

//		@Override
//		public boolean projectComponents(Term template) {
//			return true;
//		}

		static Term recomposeStatement(Compound superterm, DynTaskify d, boolean subjOrPred, boolean union) {
            var ss = superterm.subterms();

            var commonIndex = subjOrPred ? 1 : 0;
            var common = ss.sub(commonIndex);
            var n = d.size();
			if (common.TEMPORALABLE()) {
				//test all components for consistency, and use the value especially if the superterm has XTERNAL in it
				//TODO intermpolate if not completely consistent
				Term cc = null;
				for (var i = 0; i < n; i++) {
                    var xi = d.term(i).unneg().sub(commonIndex);
					if (i == 0) cc = xi;
					else {
						if (!xi.equals(cc))
							return Null;
					}
				}
				common = cc;
			}

            var superSect = ss.sub(subjOrPred ? 0 : 1);

            var negOuter = superSect instanceof Neg;
			if (negOuter)
				superSect = superSect.unneg();

			Term sect;
			int outerDT;
            var op = superterm.op();
			if (op == IMPL) {
				//TODO DynamicConjTruth.ConjIntersection.reconstruct()

				long cs;
				Term constantCondition = null;
				//IMPL: compute innerDT for the conjunction
				ConjBuilder c =
						new ConjList(n);
						//new ConjTree();

				for (var i = 0; i < n; i++) {
                    var xx = d.term(i);
					if (xx.unneg().IMPL()) {

                        var neg = xx instanceof Neg;
						if (neg) xx = xx.unneg();

                        var tdt = xx.dt();

						if (tdt == XTERNAL)
							throw new TermException("xternal in dynamic impl reconstruction", xx);

						if (tdt == DTERNAL)
							tdt = 0;
						long tWhen = Tense.dither((subjOrPred ? (-tdt) : (+tdt)), d.timeRes);

						if (!c.add(tWhen, xx.sub(subjOrPred ? 0 : 1).negIf(union ^ neg)))
							return Null;
					} else {
						//conjoin any constant conditions (which may precipitate from reductions)


						constantCondition = constantCondition != null ? CONJ.the(constantCondition, xx) : xx;

						if (constantCondition == True)
							constantCondition = null;
						else if (!xx.CONDABLE() || constantCondition == Null)
							return Null;
					}
				}

				sect = c.term();
				cs = c.shift();
				c.clear();

				if (constantCondition != null)
					sect = CONJ.the(constantCondition, sect);

				if (sect == Null)
					return Null; //allow non-Null Bool's?

				//temporal information not available or was destroyed
				outerDT = cs == ETERNAL ? DTERNAL :
						Tense.occToDT(subjOrPred ? -cs - sect.seqDur() : cs);

			} else {
				sect = superSect.op().the(Util.arrayOf(i ->
					subSubjPredWithNegRewrap(subjOrPred, d.get(i)), 0, n, Term[]::new)
				);
				outerDT = DTERNAL;
			}


			if (negOuter)
				sect = sect.neg();

			outerDT = Tense.dither(outerDT, d.timeRes);

			return subjOrPred ? op.the(sect, outerDT, common) : op.the(common, outerDT, sect);
		}

		private static Term subSubjPredWithNegRewrap(boolean subjOrPred, NALTask tr) {
            var x = tr.term();
            var neg = x instanceof Neg;
			if (neg) x = x.unneg();
			return x.sub(subjOrPred ? 0 : 1 /* reverse */).negIf(neg);
		}


		/**
		 * statement component
		 */
		private static Term stmtDecomposeStructural(Op superOp, boolean subjOrPred, Term subterm, Term common) {
			boolean outerNegate;
			if (outerNegate = (subterm instanceof Neg))
				subterm = subterm.unneg();

			Term s, p;
			if (subjOrPred) {
				s = subterm;
				p = common;
			} else {
				s = common;
				p = subterm;
			}

			return superOp.the(s, p).negIf(outerNegate);

		}

		@Override
		public Term recompose(Compound superterm, DynTaskify components) {
			return recomposeStatement(superterm, components, subjOrPred, false);
		}

		@Override
		public boolean decompose(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {


            var decomposed = stmtCommon(!subjOrPred, superterm);
			if (!decomposed.unneg().CONJ()) {
				//superterm = (Compound) Image.imageNormalize(superterm);
				decomposed = stmtCommon(!subjOrPred, superterm);
			}

			//if (unionOrIntersection) {
			if (decomposed instanceof Neg) {
				decomposed = decomposed.unneg();
			}

            var common = stmtCommon(subjOrPred, superterm);

            var op = superterm.op();

			for (var y : decomposed.subterms()) {
				if (!each.accept(stmtDecomposeStructural(op, subjOrPred, y, common), start, end))
					return false;
			}

			return true;
		}

	}

}