package nars.term.util.transform;

import nars.NALTask;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.task.proxy.SpecialTermTask;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.var.NormalizedVariable;
import nars.term.var.Variable;

import java.util.function.Predicate;

/** shifts var indices of a compound
 * TODO store ranges for individual variable types to avoid unnecessary shifting when disjoint or partially disjoint
 * */
public class VariableShift extends VariableNormalization implements Predicate<Compound> {

	private final int varBits;

	public VariableShift() {
		this(Op.Variables);
	}

	public VariableShift(int varBits) {
		super();
		this.varBits = varBits;
	}

	/** post processes the beliefterm
	 * @param x the term being shifted
	 * @param y the base or root term that x will be modified against
	 * */
	public static <T extends Termed> T varShift(T x, Term y, boolean shiftEquals, boolean shiftContains) {

        var xTerm = x.term();

        if (shiftEquals || !y.equals(xTerm)) {
            var vCommon = vCommon(y, xTerm);
            if (vCommon != 0 && (shiftContains || !(y instanceof Compound) || !y.containsRecursively(xTerm))) {
                var yTerm = new VariableShift(vCommon).shift(y).apply(xTerm);
                if (!yTerm.equals(xTerm)) {
                    var z = x instanceof NALTask t ?
                            SpecialTermTask.proxyUnsafe(t, yTerm).copyMeta(t) :
                            yTerm;
                    if (z != null)
                        return (T) z;
                }
            }
        }

		return x;
    }

	private static int vCommon(Term y, Term xTerm) {
		return Op.Variables & xTerm.struct() & y.struct();
	}

	@Override
	public boolean test(Compound superterm) {
		return superterm.hasAny(varBits);
	}

	public final VariableShift shift(Term y) {
        switch (y) {
            case Compound c -> { return shift(c); }
            case NormalizedVariable n -> shift(n);
            case null, default -> { }
        }
		return this;
	}

	private void shift(NormalizedVariable y) {
		map.shift(y, varBits);
	}

	private VariableShift shift(Compound y) {
//		int[] minmax = { Integer.MAX_VALUE, Integer.MIN_VALUE };
		y.subtermsDirect().ANDrecurse(/*this*/Subterms::hasVars, t -> {
			if (t instanceof NormalizedVariable n)
				shift(n);

//			if (z instanceof Variable && z.isAny(varBits)) {
//
//				//TODO else: CommonVariable - anything necessary here?
//			}
		}, null);
//		offset = Math.max(offset, minmax[1]);
		return this;
	}

	@Override
	public Term applyVariable(Variable x) {
		return x.isAny(varBits) ? super.applyVariable(x) : x;
	}

}