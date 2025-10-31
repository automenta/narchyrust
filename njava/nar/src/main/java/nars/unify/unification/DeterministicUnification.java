package nars.unify.unification;

import nars.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.var.Variable;
import nars.unify.Unification;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * an individual solution
 */
public abstract class DeterministicUnification implements Unification {

    protected DeterministicUnification() {
        super();
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) return true;
        return obj instanceof DeterministicUnification && equals((DeterministicUnification) obj);
    }

    protected abstract boolean equals(DeterministicUnification obj);

    @Override
    public final int forkKnown() {
        return 1;
    }

    @Override
    public final Iterable<Term> apply(Term x) {
        return List.of(transform(x));
    }

    public final Term transform(Term x) {
        return transform().apply(x);
    }

    protected final RecursiveTermTransform transform() {
        return transform;
    }

    public abstract @Nullable Term xy(Variable t);

    /**
     * sets the mappings in a target unify
     * @return true if successful
     */
    public abstract boolean apply(Unify y);

    private final RecursiveTermTransform transform = new RecursiveTermTransform() {
        @Override public final Term applyAtomic(Atomic a) {
            if (a instanceof Variable) {
                Term b = xy((Variable) a);
                if (b == null)
                    return Bool.Null;
                return b;
            }
            return a;
        }
    };
}