package nars.term.functor;

import nars.Term;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.atom.Atom;
import org.jetbrains.annotations.Nullable;

import static nars.term.atom.Bool.Null;

/** Functor template for a binary functor with bidirectional parameter cases */
public abstract class BinaryFunctor extends Functor {

    protected BinaryFunctor(String name) {
        super(name);
    }

    protected BinaryFunctor(Atom atom) {
        super(atom);
    }

    @Override
    public final @Nullable Term apply(Evaluation e, Subterms args) {
        int s = args.subs();
        if (s>0) {
            Term x = args.sub(0);
            if (s == 1)
                return unary(x);
            if (s == 2)
                return compute(e, x, args.sub(1));
        }
        return Null;
    }

    /** implementations may override to support 1-ary behavior
     * @param x*/
    protected Term unary(Term x) {
        return Null;
    }

    protected abstract Term compute(Evaluation e, Term x, Term y);

//    @Deprecated protected Term apply3(Evaluation e, Term x, Term y, Term xy) {
//        boolean xVar = x instanceof Variable;
//        boolean yVar = y instanceof Variable;
//        if (xy instanceof Variable) {
//
//            if (xVar || yVar) {
//                return null;
//            } else {
//                Term XY = compute(e, x, y);
//                if (XY!=null)
//                    return e.is(xy, XY) ? Bool.True : Null /* contradiction result */;
//                else
//                    return null;
//            }
//        } else {
//            if (xVar && !yVar) {
//                return computeXfromYandXY(e, x, y, xy);
//            } else if (yVar && !xVar) {
//                return computeYfromXandXY(e, x, y, xy);
//            } else if (!yVar /*&& !xVar*/) {
//
//                Term XY = compute(e, x, y);
//                if (XY == null)
//                    return null; //normalized(x, y, xy, null);
//                //assert(XY!=null): "functor " + this + " " + x + "," + y + ", " + xy + " -> compute=null";
//
//				//true, keep
//				//false?
//				return XY.equals(xy) ? True : False;
//            } else {
//                return computeFromXY(e, x, y, xy);
//            }
//        }
//    }

}