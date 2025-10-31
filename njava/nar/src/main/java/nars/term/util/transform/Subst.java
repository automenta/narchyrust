package nars.term.util.transform;

import nars.Term;
import nars.term.Compound;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.builder.TermBuilder;
import org.jetbrains.annotations.Nullable;

import static nars.term.atom.Bool.Null;



public abstract class Subst extends RecursiveTermTransform {

	public static final Atom SUBSTITUTE = Atomic.atom("substitute");

    protected Subst(TermBuilder b) {
        builder = b;
    }

    /**
     * the assigned value for x
     */
    @Nullable abstract Term xy(Term t);

    @Override public Term applyAtomic(Atomic x) {
        Term y = xy(x);
        return y != null ? y : x;
    }

    @Override
    @Nullable
    protected Term applyCompound(Compound x0) {
        Term x = super.applyCompound(x0);
        if (x == Null) return Null;

        Term y = xy(x);
        return y != null ? y : x;
    }


}