package nars.func;

import nars.Term;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.atom.Atom;
import nars.term.util.transform.InlineFunctor;
import org.jetbrains.annotations.Nullable;

import static nars.term.atom.Bool.Null;


/**
 * if STRICT is 4th argument, then there will only be a valid result
 * if the input has changed (not if nothing changed, and not if the attempted change had no effect)
 */
public class Replace extends Functor implements InlineFunctor<Evaluation> {

    public static final Replace replace = new Replace("replace");

    protected Replace(String id) {
        this((Atom) atomic(id));
    }

    protected Replace(Atom id) {
        super(id);
    }

    @Override
    public @Nullable Term apply(Evaluation e, Subterms s) {
        return replaceEval(s);
    }

    public static Term replaceEval(Subterms s) {
        return replace(s.sub(0), s.sub(1), s.sub(2), s.subs() > 3 && s.subEquals(3, UniSubst.NOVEL));
    }

    protected static Term replace(Term input, Term x, Term y, boolean novel) {
        var z = input.replace(x, y);
        return (z == Null) || (novel && input.equals(z)) ? Null : z;
    }

}