package nars.func;

import nars.Op;
import nars.Term;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.util.transform.InlineFunctor;
import nars.term.var.Variable;

import static nars.term.atom.Bool.*;

/** equivalent to prolog member/2:
 *      member(U,S)  |-   U is in S
 */
public final class Member extends Functor implements InlineFunctor<Evaluation> {

    public static final Member member = new Member();

    private Member() {
        super("member");
    }

    @Override
    public Term apply(Evaluation e, Subterms terms) {
        if (terms.subs()!=2)
            return null;

        Term y = terms.sub(1);
        boolean ySet = y.SETe();
        if (!ySet) {
            //return x.equals(y) ? True : Equal.the(x, y); //interpret y as a singular element
            return null;
        }

        Term x = terms.sub(0);
        //TODO if y.subs()==1, reduce to equals(x, y.sub(0))

        Subterms yy = y.subtermsDirect();

        if (yy.hasAny(x.struct())) {
            if (yy.contains(x))
                return True;

            if (x instanceof Variable && yy.containsRecursively(x))
                return Null; //infinite recursion
        }

        if (yy.subs()==1)
            return Op.EQ.the(x, yy.sub(0));

        if (x instanceof Variable && e != null)
            return e.canBe(x, /*Iterable*/yy) ? null : Null;

        if (!x.hasAny(Op.Variables) && !y.hasAny(Op.Variables))
            return False; //constant

        return null; //indeterminable
    }
}