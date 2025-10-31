package nars.func;

import jcog.data.list.Lst;
import nars.$;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.functor.AbstractInlineFunctor1;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * recursively collects the contents of setAt/list compound target argument's
 * into a list, to one of several resulting target types:
 * product
 * set (TODO)
 * conjunction (TODO)
 * <p>
 * TODO recursive version with order=breadth|depth option
 */
public abstract class flat extends AbstractInlineFunctor1 {

    public static final flat flatProduct = new flat() {

        @Override
        public Term result(List<Term> terms) {
            return $.p(terms);
        }

    };

    protected flat() {
        super("flat");
    }

    public static List<Term> collect(Term[] x, List<Term> l) {
        for (Term a : x) {
            Op ao = a.op();
            if (ao == Op.PROD || ao.set || ao == Op.CONJ)
                ((Subterms) a).addAllTo(l);
            else
                l.add(a);
        }
        return l;
    }

    @Override
    public @Nullable Term apply1(Term x) {

		List<Term> l = (List<Term>) new Lst(x.complexity());
        collect(((Compound)x).arrayClone(), l);
        return result(l);

    }

    public abstract Term result(List<Term> terms);


}