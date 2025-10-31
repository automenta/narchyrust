package nars.unify.constraint;

import nars.Term;
import nars.term.var.Variable;
import nars.unify.Unify;

public final class EqualPosOrNeg extends RelationConstraint {


    public EqualPosOrNeg(Variable target, Variable other) {
        super(EqualPosOrNeg.class, target, other);
    }

    @Override
    protected RelationConstraint newMirror(Variable newX, Variable newY) {
        return new EqualPosOrNeg(newX, newY);
    }

    @Override
    public float cost() {
        return 0.1f;
    }

    @Override
    public boolean invalid(Term x, Term y, Unify context) {
        return !x.equalsPN(y);
    }

}