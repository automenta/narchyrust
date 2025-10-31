package nars.deriver.op;

import nars.Deriver;
import nars.Term;
import nars.term.Compound;
import nars.term.var.Variable;
import nars.unify.constraint.RelationConstraint;

public class CeqPNConstraint extends RelationConstraint<Deriver.PremiseUnify> {

    public CeqPNConstraint(Variable x, Variable y) {
        super(CeqPNConstraint.class, x, y);
    }

    @Override
    public boolean invalid(Term x, Term y, Deriver.PremiseUnify context) {
        x = x.unneg(); y = y.unneg();
        if (x == y) return false;

        int xv = x.complexity(), yv = y.complexity();
        if (xv == yv)
            return !x.equals(y);
        else if (xv > yv)
            return !condOfPN(x, y);
        else
            return !condOfPN(y, x);
    }

    private static boolean condOfPN(Term container, Term event) {
        return container instanceof Compound C && C.condOf(event, 0);
    }


    @Override
    protected CeqPNConstraint newMirror(Variable newX, Variable newY) {
        return new CeqPNConstraint(newX, newY);
    }

    @Override
    public float cost() {
        return 0.8f;
    }

}