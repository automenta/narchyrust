package nars.term.functor;

import nars.Term;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.atom.Atom;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class LambdaFunctor extends Functor {

    private final Function<Subterms, Term> f;

    public LambdaFunctor(Atom termAtom, Function<Subterms, Term> f) {
        super(termAtom);
        this.f = f;
    }

    @Override
    public final @Nullable Term apply(Evaluation e, Subterms terms) {
        return f.apply(terms);
    }


}