package nars.func;

import jcog.data.bit.MetalBitSet;
import nars.Op;
import nars.Term;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.functor.InverseFunctor;
import nars.unify.Unifier;
import org.jetbrains.annotations.Nullable;

import static nars.Op.PROD;
import static nars.term.atom.Bool.False;
import static nars.term.atom.Bool.True;

public enum Equal  { ;

    @Nullable public static Term reverse(Evaluation e, Term x, boolean xHasVars, Term y, boolean yHasVars) {
        Op xOp = x.op(), yOp = y.op();
        if (xHasVars || yHasVars) {
            if (((xOp == PROD || xOp.set) || (yOp == PROD || yOp.set))){
                if (xOp != yOp)
                    return False;

                Subterms xx = x.subtermsDirect(), yy = y.subtermsDirect();
                if (xOp != PROD || xx.subs() != yy.subs()) {
                    return reverseArityDiffer(e, xOp, xx, yy);
                } else {
                    var pxy = eqVector(e, xx, yy);
                    if (pxy != null) return pxy;
                }
            }
        }

        if (xOp == Op.INH) {
            var xa = reverse(e, (Compound) x, y);
            if (xa != null) return xa;
        }

        if (yOp == Op.INH) {
            var ya = reverse(e, (Compound) y, x);
            if (ya!=null) return ya;
        }

        return null;
    }

    @Nullable
    private static Term reverseArityDiffer(Evaluation e, Op xOp, Subterms xx, Subterms yy) {
        if (xOp == PROD) return False;
        else {
            //sets with different arity might unify if containing a variable that has the value equal to other set items
            var xy = Unifier.eliminate(xx, yy);
            if (xy != null) {
                if (xy[0].subs() == 1 && xy[1].subs() == 1) {
                    //reduced to single term
                    var x = xy[0].sub(0);
                    return e.compute(x, xy[1].sub(0));
                }
                //TODO other cases where values can be assigned to multiple variable outputs
            }
            return null;
        }
    }

    @Nullable
    private static Term reverse(Evaluation e, Compound x, Term y) {
        var xf = Functor._func(x);
        return xf instanceof InverseFunctor xfi ? xfi.equality(e, x, y) : null;
    }



    //                if (xa1 instanceof Variable && xa0.op() == INT) //shouldnt be necessary if sorted in correct order
//                    throw new TODO();
    private static Term eqVector(Evaluation e, Subterms xx, Subterms yy) {
        var n = xx.subs();
        var remain = MetalBitSet.bits(n);
        //1. if any constant subterms are not equal, it's false
        for (var i = 0; i < n; i++) {
            Term a = xx.sub(i), b = yy.sub(i);
            if (!a.equals(b)) {
                if (!a.hasVars() && !b.hasVars()) return False; //non-constant mismatch
                else remain.set(i);
            }
        }
//
//        for (int i = 0; i < n; i++) {
//            if (remain.test(i)) {
//                Term xy = compute(e, xx.sub(i), yy.sub(i));
//                if (xy == Null || xy == False)
//                    return xy; //fail immediately
//
//                if (xy == null)
//                    continue;
//
//                remain.clear(i);
//                if (xy == True)
//                    continue;
//            }
//        }
        var numRemain = remain.cardinality();
        if (numRemain==0) return True;

        if (numRemain == 1) {
            var i = remain.first(true);
            return Op.EQ.the(xx.sub(i), yy.sub(i));
        } else {
            //create an equality vector of the remaining conditions
            return Op.EQ.the(
                PROD.the(xx.subsIncExc(remain, true)),
                PROD.the(yy.subsIncExc(remain, true))
            );
        }
    }


}