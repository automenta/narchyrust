package nars.term.functor;

import nars.Op;
import nars.Term;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.atom.Bool;
import nars.term.var.Variable;
import org.jetbrains.annotations.Nullable;

import static nars.term.atom.Bool.False;
import static nars.term.atom.Bool.True;

/** (potentially) reversible function */
@Deprecated public abstract class UnaryBidiFunctor extends Functor {

    protected UnaryBidiFunctor(String atom) { super(atom); }

    @Override
    public final @Nullable Term apply(Evaluation e, Subterms terms) {
        int s = terms.subs();
        return switch (s) {
            case 1 -> apply1(terms.sub(0));
            case 2 -> apply2(e, terms.sub(0), terms.sub(1));
            default -> Bool.Null;
        };
    }

    Term apply1(Term x) {
		return x  instanceof Variable ? null : compute(x);
    }

    protected abstract Term compute(Term x);

    /** override for reversible functions, though it isnt required */
    protected Term uncompute(Term x, Term y) {
        return null;
    }

    protected Term apply2(Evaluation e, Term x, Term y) {
        boolean xVar = x instanceof Variable;
        if (y instanceof Variable) {

            if (xVar) {
                return null;
            } else {
                Term XY = compute(x);
                if (XY!=null) {
                    return e.is(y, XY) ?
                            null : Bool.Null;
                } else {
                    return null;
                }
            }
        } else {
            if (x.hasAny(Op.Variables)) {
                Term X = uncompute(x, y);
                if (X!=null) {
                    return e.is(x, X) ?
                        null : Bool.Null;
                } else {
                    return null;
                }
            } else {

                Term yActual = compute(x);
                if (yActual == null)
                    return null;
                //else
                    //return Equal.the(y,yActual);
                    //return yActual;
				return y.equals(yActual) ? True : False;
            }
        }
    }
}