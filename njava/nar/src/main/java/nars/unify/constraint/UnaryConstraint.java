package nars.unify.constraint;

import nars.Op;
import nars.Term;
import nars.term.var.Variable;
import nars.unify.Unify;
import nars.unify.UnifyConstraint;

public final class UnaryConstraint<U extends Unify> extends UnifyConstraint<U> {

    final TermMatch matcher;
    public final boolean trueOrFalse;

    public UnaryConstraint(TermMatch i, Variable x, boolean trueOrFalse) {
        super(x, i.getClass().isAnonymousClass() ? i.getClass().toString() : i.getClass().getSimpleName(),
                 (i.param() != null ? i.param() : Op.EmptyProduct).negIf(!trueOrFalse));
        this.matcher = i;
        this.trueOrFalse = trueOrFalse;
    }

    @Override
    public float cost() {
        return matcher.cost();
    }

    @Override
    public final boolean invalid(Term x, Unify f) {
        return match(x) != trueOrFalse;
    }

    public final boolean match(Term x) {
        return matcher.test(x);
    }

}