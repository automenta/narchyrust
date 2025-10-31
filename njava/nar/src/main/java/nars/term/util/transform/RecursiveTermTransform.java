package nars.term.util.transform;

import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.subterm.TmpTermList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Termlike;
import nars.term.atom.Atomic;
import nars.term.builder.TermBuilder;
import nars.term.compound.LightCompound;
import nars.term.compound.LightDTCompound;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

import static nars.Op.*;
import static nars.term.atom.Bool.*;


/**
 * I = input target type, T = transformable subterm type
 */
public abstract class RecursiveTermTransform implements TermTransform {

    TermBuilder builder =
        terms
        //SimpleHeapTermBuilder.the
        //LightHeapTermBuilder.the
        ;

    private Term evalInhSubs(Subterms inhSubs) {
        var p = inhSubs.sub(1); /* pred */
        if (p instanceof InlineFunctor P) {
            var s = inhSubs.sub(0);
            if (s instanceof Compound S) {
                if (S.PROD())
                    return evalInline(P, S);
                else if (S.CONJ() && s.dt()==DTERNAL && S.ORunneg(Term::PROD))
                    return evalSpread(P, S);
            }
        }
        return null;
    }


    /** spread apply to conj subterms which are products */
    private Term evalSpread(InlineFunctor p, Term s) {
        return builder.conj(s.subterms().transformSubs(ss ->
            ss.PROD() ?
                evalInline(p, ss) :
                builder.inh(ss, (Term) p))
        );
    }

    private static Term evalInline(InlineFunctor p, Term s) {
        return p.applyInline(s.subtermsDirect());
    }

    /** apply any shifts caused by internal range changes (ex: True removal) */
    private static int realignDuration(int ydt, Subterms source, Subterms target) {
        if (source == target) {
            //Util.nop();
        } else if (source.subs() == 2 && target.subs() == 2) {
            var d = seqDurDiff(source, target, 0) + seqDurDiff(source, target, 1);
            if (d!=0)
                return (ydt==DTERNAL ? 0 : ydt) + d;
        }

        return ydt;
    }


    /**
     * Transforms subterms using the provided transformation function.
     * Returns null if untransformable, original if unchanged, or new subterms if modified.
     */
    public static @Nullable Subterms transformSubs(Subterms x, UnaryOperator<Term> f) {

        TermList y = null;

        var s = x.subs();

        for (var i = 0; i < s; i++) {

            var xi = x.sub(i);

            var yi = f instanceof RecursiveTermTransform r ?
                r.applyInner(xi) :
                f.apply(xi);

            if (yi == Null)
                return null; //short-circuit

            if (xi!=yi && y == null)
                y = new TmpTermList(s, i); //lazy alloc

            if (y != null)
                y.addFast(yi);
        }

        return y == null ? x : y.fill(x);
    }


    public final Term apply(Term x) {
        return x instanceof Neg ? applyNeg(x) : applyInner(x);
    }

    private static Term post(Term y) {
        return y instanceof LightCompound || y instanceof LightDTCompound ? postLight(y) : y;
    }

    /** 'unwrap' LightCompound, LightDTCompound */
    private static Term postLight(Term y) {
        return y.op().the(y.dt(),
            y.subtermsDirect().transformSubs(RecursiveTermTransform::post)
        );
    }

    private Term applyInner(Term x) {
        var y = (x instanceof Compound c) ?
            applyCompound(c)
            :
            applyAtomic((Atomic) x);
        return x == y ? x : post(y);
    }

    protected Term applyAtomic(Atomic a) {
        return a;
    }

    protected Term applyCompound(Compound x) {
        return x instanceof Neg  ? applyNeg(x) : applyCompound(x, x.dt());
    }

    protected final Term applyCompound(Compound x, int yDt) {
        return applyCompoundInline(x, null, yDt);
    }

    private Term applyNeg(Compound x) {
        var xu = x.unneg();
        var yu = xu instanceof Compound xuc ? applyCompound(xuc) : applyAtomic((Atomic)xu);
        return yu == xu ? x : yu.neg();
    }

    private Term applyCompoundInline(Compound x, @Nullable Op yOp, int yDt) {

        var xx = x.subtermsDirect();

        var yy = transformSubs(xx, this);
        if (yy == null)
            return Null;

        if (yOp == null) yOp = x.op();

        switch (yOp) {
            case CONJ -> {
                if (xx != yy) {
                    var yyy = reduceConj(yy, yDt);
                    if (yyy instanceof Term yyyt)
                        return yyyt;

                    yy = (Subterms) yyy;
                }
            }
            case INH -> {
                if (evalInline()) {
                    var v = evalInhSubs(yy);
                    if (v != null)
                        return v;
                }
            }
        }

        var xEqY = xx == yy;
        if (xEqY && x.opID == yOp.id && x.dt() == yDt)
            return x; //no change
        else
            return yOp.build(builder, dtAlign(yOp, yDt, xx, yy, xEqY), yy);
    }

    private static Termlike reduceConj(Subterms yy, int yDt) {
        return switch (yy.subs()) {
            case 0 -> True;
            case 1 -> yy.sub(0);
            default -> reduceConjN(yy, yDt);
        };
    }

    private static Termlike reduceConjN(Subterms yy, int yDt) {
        if (yy.containsInstance(False))
            return False;

        if (Tense.parallel(yDt) && ((TermList) yy).removeInstances(True)) {
            switch (yy.subs()) {
                case 0 -> {
                    return True;
                }
                case 1 -> {
                    return yy.sub(0);
                }
            }
        }

        return yy;
    }

    protected int dtAlign(Op yOp, int yDt, Subterms xx, Subterms yy, boolean xEqY) {
        if (yOp.temporal) {
            if (!xEqY && yDt!=XTERNAL) yDt = realignDuration(yDt, xx, yy);
            if (yDt == 0) yDt = DTERNAL; //HACK
            return yDt;
        } else {
            assert(yDt == DTERNAL);
            return DTERNAL;
        }
    }


    /**
     * enable for inline functor evaluation
     *
     * @param args
     */
    public boolean evalInline() {
        return false;
    }

    protected final Term applyNeg(Term/*Neg*/ x) {
        var xu = x.unneg();
        var yu = applyInner(xu);
        return yu==xu ? x : yu.neg();
    }

    /** TODO what if order is reversed, then compare sub against 1-sub */
    private static int seqDurDiff(Subterms xx, Subterms yy, int sub) {
        var x = xx.sub(sub);
        var y = yy.sub(sub);

//        if (x.unneg().dt()==XTERNAL || y.unneg().dt()==XTERNAL)
//            return 0; //HACK special case.  may need to check for recursively contained XTERNAL

        if (x == y) return 0;

        var xd = x.seqDur(true);
        if (xd == XTERNAL)
            return 0; //??
        var yd = y.seqDur(true);
        if (yd == XTERNAL)
            return 0; //??
        return xd - yd;
    }



//    public RecursiveTermTransform buffered() {
//        return new RecursiveTermTransform() {
//            @Override
//            protected Term applyAtomic(Atomic a) {
//                return RecursiveTermTransform.this.applyAtomic(a);
//            }
//
//
//            @Override
//            protected Term _applyCompound(Compound x, int yDt) {
//                return applyCompoundBuffered(x);
//            }
//
//        };
//    }

//    /** TODO test */
//    public Term applyCompoundBuffered(Compound x) {
//        final TermBuffer b = new TermBuffer();
//        b.append(x, xx -> {
//            if (xx instanceof Atomic xxa) return applyAtomic(xxa);
//            return xx;
//        });
//        return b.term();
//    }

//    /** TODO test */
//    public Term applyCompoundUnverified(Compound x, Op yOp, int yDt) {
//        Subterms xx = x.subtermsDirect();
//        Subterms yy = transformSubs(xx, this);
//        boolean xEqY = xx == yy;
//
//        if (xEqY && x.opID==yOp.id && x.dt()==yDt)
//            return x; //unchanged
//
//        if (yy instanceof TmpTermList)
//            yy = new TermList(((TmpTermList)yy).arrayTake());
//
//        return yy!=null ? //Unverified.verify(
//            new UnverifiedCompound(yOp.id,
//                dtAlign(yOp, yDt, xx, yy, xEqY),
//                yy)
//         : Null;
//    }

}