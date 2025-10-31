package nars.unify.constraint;

import nars.Term;
import nars.term.var.Variable;
import nars.unify.Unify;

public final class EqualNeg extends RelationConstraint {

    public EqualNeg(Variable target, Variable other) {
        super(EqualNeg.class, target, other);
    }

    @Override
    protected RelationConstraint newMirror(Variable newX, Variable newY) {
        return new EqualNeg(newX, newY);
    }

    @Override
    public float cost() {
        return 0.15f;
    }

    @Override
    public boolean invalid(Term x, Term y, Unify context) {
        return !x.equalsNeg(y);
    }

}