package nars.deriver.op;

import nars.$;
import nars.Deriver;
import nars.Term;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.var.Variable;
import nars.unify.constraint.RelationConstraint;

/**
 * conjunctive/disjunctive condition equality test
 *
 * x.equals(y) || ...
 *      conj:  condOf(  x,   y) || condOf(  y,   x)
 *      disj:  condOf(--x, --y) || condOf(--y, --x)
 * */
public class CeqConstraint extends RelationConstraint<Deriver.PremiseUnify> {


    /** conj/disj modes */
    private final boolean xcd, ycd;

    /**
     * x and y polarity
     */
    final boolean xp;
    final boolean yp;

    public CeqConstraint(Term x, boolean xcd, Term y, boolean ycd) {
        super(CeqConstraint.class,
                (Variable) (x instanceof Neg ? x.unneg() : x),
                (Variable) (y instanceof Neg ? y.unneg() : y),
                $.p(!(x instanceof Neg), xcd, !(y instanceof Neg), ycd));
        this.xcd = xcd;
        this.ycd = ycd;
        this.xp = !(x instanceof Neg);
        this.yp = !(y instanceof Neg);
    }

    @Override
    public boolean invalid(Term x, Term y, Deriver.PremiseUnify context) {
        return !equal(x, y)
               &&
               !condOf(x, y);
    }

    private boolean condOf(Term x, Term y) {
        if (!x.CONDS() && !y.CONDS()) return false;

        if (x.SEQ() || y.SEQ())
            return false; //HACK ignore sequences

        return condOf(x, xp ^ !xcd, y, yp ^ !xcd ? +1 : -1)
                ||
               condOf(y, yp ^ !ycd, x, xp ^ !ycd ? +1 : -1);
    }

    /** polarity = +1 or -1 */
    private static boolean condOf(Term _c, boolean cp, Term e, int polarity) {
        Term c;
        if (!cp) {
            if (!(_c instanceof Neg))
                return false;
            c = _c.unneg();
        } else
            c = _c;

        return c instanceof Compound && ((Compound) c).condOf(e, polarity);
    }

    private boolean equal(Term x, Term y) {
        return xp == yp ? x.equals(y) : x.equalsNeg(y);
    }

    @Override
    protected CeqConstraint newMirror(Variable newX, Variable newY) {
        return new CeqConstraint(newX.negIf(!yp), ycd, newY.negIf(!xp), xcd);
    }

    @Override
    public float cost() {
        return 0.9f;
    }


}