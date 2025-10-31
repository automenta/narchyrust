package nars.unify.constraint;

import nars.$;
import nars.Term;
import nars.subterm.util.SubtermCondition;
import nars.term.Compound;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.var.Variable;
import nars.unify.Unify;

public class SubConstraint<U extends Unify> extends RelationConstraint<U> {
    private final boolean forward;

    private final SubtermCondition contains;

    /**
     * containment of the target positively (normal), negatively (negated), or either (must test both)
     */
    private final int polarityCompare;

    public SubConstraint(SubtermCondition contains, Variable x, Variable y) {
        this(contains, x, y, +1);
    }

    public SubConstraint(SubtermCondition contains, Variable x, Variable y, int polarityCompare) {
        this(contains, x, y, true, polarityCompare);
    }

    private SubConstraint(SubtermCondition contains, Variable x, Variable y, /* HACK change to forward semantics */ boolean forward, int polarityCompare) {
        super(SubConstraint.class, x, y,
            $.p(
                Atomic.atom(contains.name()),
                Int.i(forward ? +1 : -1),
                Int.i(polarityCompare)
            )
        );

        this.forward = forward;
        this.contains = contains;
        this.polarityCompare = polarityCompare;
    }

    @Override
    protected RelationConstraint newMirror(Variable newX, Variable newY) {
        return new SubConstraint<>(contains, newX, newY, !forward, polarityCompare);
    }

    @Override
    public float cost() {
        var baseCost = contains.cost();
		return switch (polarityCompare) {
			case +1 -> baseCost;
			case  0 -> 1.9f * baseCost;
			case -1 -> 1.1f * baseCost;
			default -> throw new UnsupportedOperationException();
		};
    }

    public final boolean invalid(Term x, Term y, Unify context) {
        return !(
            (forward ? x : y) instanceof Compound C &&
            contains.test(C, forward ? y : x, polarityCompare)
        );
    }


}