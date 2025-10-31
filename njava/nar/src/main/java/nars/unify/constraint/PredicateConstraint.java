package nars.unify.constraint;

import nars.Term;
import nars.term.control.PREDICATE;
import nars.unify.UnifyConstraint;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public abstract class PredicateConstraint<U, C extends UnifyConstraint> extends PREDICATE<U> {

    public final C constraint;

    /** taskterm, beliefterm -> extracted */
    public final BiFunction<Term,Term,Term> extractX;
    @Nullable protected final BiFunction<Term,Term,Term> extractY;
    private final float cost;

    protected PredicateConstraint(Term id, C m, BiFunction<Term, Term, Term> extractX, @Nullable BiFunction<Term, Term, Term> extractY, float cost) {
        super(id);
        this.constraint = m;
        this.cost = cost;
        this.extractX = extractX;
        this.extractY = extractY;
    }

    @Override
    public final boolean deterministic() {
        return false;
    }

//    @Override
//    public boolean reduceIn(List<PREDICATE<U>> p) {
//        return p.removeIf(x -> {
//            if (x!=ConstraintAsPredicate.this && x instanceof ConstraintAsPredicate) {
//                if (((ConstraintAsPredicate)x).constraint.getClass().equals(constraint.getClass()))
//                    Util.nop();
//            }
//            return false;
//        });
//    }

    @Override
    public float cost() {
        return cost;
    }

}