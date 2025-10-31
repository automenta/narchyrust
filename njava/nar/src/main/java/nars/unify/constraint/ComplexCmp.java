package nars.unify.constraint;

import nars.Term;
import nars.term.Neg;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.var.Variable;
import nars.unify.Unify;

/**
 * cmp = +1: X > Y
 * cmp =  0: Y == X
 * cmp = -1: Y > X
 */
public final class ComplexCmp extends RelationConstraint {

    private static final Term ONLY_IF_CONSTANT = Atomic.atom("onlyIfConstants"),
            Yneg = Atomic.atom("yNeg");

    /** TODO move to subclass */
    @Deprecated private final boolean onlyIfConstant;

    final int cmp;
    private final boolean yNeg, xNeg;

    public ComplexCmp(Variable x, Variable y, boolean onlyIfConstant, int cmp) {
        this(x, y, onlyIfConstant, cmp, false, false);
    }

    public ComplexCmp(Variable x, Variable y, boolean onlyIfConstant, int cmp, boolean xNeg, boolean yNeg) {
        super(ComplexCmp.class, x, y,
                Int.i(cmp),
                onlyIfConstant ? ONLY_IF_CONSTANT : Int.ZERO,
                yNeg ? Yneg : Int.ZERO
                );
        this.onlyIfConstant = onlyIfConstant;
        this.cmp = cmp;
        this.yNeg = yNeg;
        this.xNeg = xNeg;
    }


    @Override
    protected RelationConstraint newMirror(Variable newX, Variable newY) {
        return new ComplexCmp(newX, newY, onlyIfConstant, -cmp, yNeg, xNeg);
    }

    @Override
    public float cost() {
        return 0.08f;
    }

    @Override
    public boolean invalid(Term x, Term y, Unify context) {
        return (!onlyIfConstant || x.hasVars() || y.hasVars()) &&
            Integer.compare(vol(x, xNeg), vol(y, yNeg)) != cmp;
    }

    private static int vol(Term z, boolean neg) {
        int v = z.complexity();
        if (neg) {
            if (z instanceof Neg) v--; else v++;
        }
        return v;
    }

}