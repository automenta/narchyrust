package nars.table.util;

import jcog.WTF;
import jcog.data.list.Lst;
import nars.BeliefTable;
import nars.NAL;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.table.dynamic.AbbreviationBeliefTable;
import nars.table.dynamic.DynTruthBeliefTable;
import nars.table.dynamic.ImageBeliefTable;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.atom.BytesAtom;
import nars.term.util.Image;
import nars.term.var.Variable;
import nars.truth.dynamic.*;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.AtomicConstant;
import static nars.Op.hasAny;

/** automatic / virtual impl temporal induction */
public enum DynTables { ;

	/**
	 * returns the overlay tables builder for the term, or null if the target is not dynamically truthable
	 */
	public static @Nullable ObjectBooleanToObjectFunction<Term, BeliefTable> tableDyn(Term t) {
        if (NAL.dyn.ENABLE) {
            if (t instanceof BytesAtom ba)
                return dynAbbreviation();
            else if (t instanceof Compound c && dynPreFilter(c))
				return tableDynCompound(c);
        }
		return null;
    }

	private static @Nullable ObjectBooleanToObjectFunction<Term, BeliefTable> tableDynCompound(Compound c) {
		return switch (c.op()) {
			case INH -> dynInh(c);
			case SIM -> dynSim(c);
			case DIFF -> dynDiff(c);
			case IMPL -> dynImpl(c);
			case CONJ -> dynConj(c);
			case DELTA -> dynDelta();
			case NEG ->
					throw new WTF("negation terms can not be conceptualized as something separate from that which they negate");
			default -> null;
		};
	}

	private static @NotNull ObjectBooleanToObjectFunction<Term, BeliefTable> dynAbbreviation() {
		return (x, bOrG) -> new AbbreviationBeliefTable((BytesAtom) x, bOrG);
	}

	@Nullable private static ObjectBooleanToObjectFunction<Term, BeliefTable> dynDelta() {
		return NAL.dyn.DYN_DELTA ? tableBeliefs(DynDelta.the) : null;
	}

	@Nullable private static ObjectBooleanToObjectFunction<Term, BeliefTable> dynConj(Compound t) {
		return NAL.dyn.DYN_CONJ && DynConj.condDecomposeable(t, true) ?
				tableBeliefsAndGoals(DynConj.Conj) : null;
	}

	/**
	 * TODO maybe some Indep cases can work if they are isolated to a sub-condition
	 */
	private static boolean dynPreFilter(Compound x) {
        var s = x.struct();
		return !hasAny(s, Op.VAR_QUERY.bit
				/*| Op.VAR_INDEP.bit*/
				//| Op.VAR_DEP.bit
		) && hasAny(s, AtomicConstant);
	}

	/** TODO allow indep var if they are involved in (contained within) either but not both subj and pred */
	@Nullable private static ObjectBooleanToObjectFunction<Term, BeliefTable> dynImpl(Compound t) {

        var tt = t.subtermsDirect();
		Term s = tt.sub(0), p = tt.sub(1);
        var su = s.unneg();

        var c = dynImplModels(tt, su, s, p);

		return c.isEmpty() ? null : tableBeliefs(c.toArrayRecycled());
	}

	public static Lst<DynTruth> dynImplModels(Subterms tt, Term su, Term s, Term p) {

        var varSeparate = varSeparate(su, p);

        var c = new Lst<>(0, new DynTruth[3]);

		if (tt.hasAny(Op.CONJ) && varSeparate) {

            if (NAL.dyn.DYN_IMPL_SUBJ_CONJ && DynConj.condDecomposeable(su, true))
                c.add(s instanceof Neg ? DynImplConj.DynImplDisjSubj : DynImplConj.DynImplConjSubj);

            if (NAL.dyn.DYN_IMPL_SUBJ_DISJ_MIX && s instanceof Neg && DynConj.condDecomposeable(su, true, 2 /* because of TruthFunctions.mix non-associativity */))
                c.add(DynImplConj.DynImplDisjMixSubj);

            if (NAL.dyn.DYN_IMPL_PRED_CONJ && DynConj.condDecomposeable(p, true))
				c.add(DynImplConj.DynImplConjPred);
		}

		if (NAL.dyn.DYN_IMPL && varSeparate)
			c.add(DynImpl.DynImplInduction);

		if (NAL.dyn.DYN_IMPL_CONTRAPOSITION && DynImplContra.DynImplContraposition.validSubject(s))
			c.add(DynImplContra.DynImplContraposition);

		if (NAL.dyn.DYN_IMPL_CONVERSION && DynImplContra.DynImplConversion.validSubject(s))
			c.add(DynImplContra.DynImplConversion);
		return c;
	}

	/** true if there are no variables shared by the two terms */
	public static boolean varSeparate(Term x, Term y) {
		if (x instanceof Variable || y instanceof Variable) return false;
		if (!x.hasVars() || !y.hasVars()) return true;
        var commonBits = (x.struct() & Op.Variables) & (y.struct() & Op.Variables);
		if (commonBits == 0)
			return true;

		var xv = ((Compound)x).recurseSubtermsToSet(commonBits);
        var yy = y.subtermsDirect();
		for (var xxv : xv)
			if (yy.containsRecursively(xxv))
				return false;

		return true;
	}

	private static @Nullable ObjectBooleanToObjectFunction<Term, BeliefTable> dynInh(Compound i) {

        var ii = i.subtermsDirect();

//		ObjectBooleanToObjectFunction<Term, BeliefTable> s;
//		if (NAL.dyn.DYN_INH_SECT) {
//			var m = DynStatement.dynInhSect(ii);
//			s = m != null ? tableBuilder(m) : null;
//		} else
//			s = null;
//

		if (ii.hasAll( Op.PROD.bit | Op.IMG.bit) ) {
				//&& !hasAny(iis, Op.Temporals)
				//&& (!hasAny(iis, Op.Variables) || ii.vars() <= 1)
			//s = dynImage(i, s);
			return dynImage(i);
		}

		return null;
	}
	@Nullable
	private static ObjectBooleanToObjectFunction<Term, BeliefTable> dynImage(Compound i) {
		var n = Image._imageNormalize(i);
		if (n.INH() && !i.equals(n))
			return (t, bOrG) -> {
				var ibt = new ImageBeliefTable(t, bOrG);
				return ibt;
				//return s == null ? new BeliefTable[]{ibt} :
				//		ArrayUtil.add(s.valueOf(t, bOrG), ibt);
			};
		return null;
	}
//	@Nullable
//	private static ObjectBooleanToObjectFunction<Term, BeliefTable[]> dynImage(Compound i, @Nullable ObjectBooleanToObjectFunction<Term, BeliefTable[]> s) {
//        var n = Image._imageNormalize(i);
//		if (n.INH() && !i.equals(n))
//			return (t, bOrG) -> {
//				var ibt = new ImageBeliefTable(t, bOrG);
//				return s == null ? new BeliefTable[]{ibt} :
//					ArrayUtil.add(s.valueOf(t, bOrG), ibt);
//			};
//		return s;
//	}

	@Nullable private static ObjectBooleanToObjectFunction<Term, BeliefTable> dynDiff(Term t) {
		return !NAL.dyn.DYN_DIFF || !varSeparate(t.sub(0), t.sub(1)) ?
			null :
			tableBeliefs(DynDiff.the);
	}
	@Nullable private static ObjectBooleanToObjectFunction<Term, BeliefTable> dynSim(Term t) {
		return !NAL.dyn.DYN_SIM || !varSeparate(t.sub(0), t.sub(1)) ?
			null : tableBeliefs(DynSim.the);
	}

	@Nullable public static BeliefTable tableDyn(Term x, boolean beliefOrGoal) {
		var dd = tableDyn(x);
        return dd != null ? dd.valueOf(x, beliefOrGoal) : null;
    }

	private static ObjectBooleanToObjectFunction<Term, BeliefTable> tableBeliefsAndGoals(DynTruth... models) {
		return (t, beliefOrGoal) ->
			new DynTruthBeliefTable(t, models, beliefOrGoal);
	}

	private static ObjectBooleanToObjectFunction<Term, BeliefTable> tableBeliefs(DynTruth... models) {
		return (t, beliefOrGoal) ->
			beliefOrGoal ? new DynTruthBeliefTable(t, models, beliefOrGoal) : null;
	}
}