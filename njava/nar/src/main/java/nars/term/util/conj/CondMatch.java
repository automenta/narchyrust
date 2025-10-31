package nars.term.util.conj;

import jcog.Is;
import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.util.Image;
import nars.term.var.Variable;
import nars.unify.AbstractUnifier;
import nars.unify.Unifier;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.term.atom.Bool.True;
import static nars.time.Tense.parallel;

@Is("Sequence_alignment") public enum CondMatch { ;

    @Deprecated public static Term match(Compound conj, Term event, boolean fast, boolean includeBefore, boolean includeMatched, boolean includeAfter, Unify U) {
        return match(conj, event, fast, includeBefore, includeMatched, includeAfter, true, U);
    }

    /** @param fast return True if everything matched, False otherwise HACK assumes: includeBefore, !includeMatched, and includeAfter
     *
     * TODO complete ConjPattern API and use that
     * */
    public static Term match(Compound c, Term e, boolean fast, boolean includeBefore, boolean includeMatched, boolean includeAfter, boolean includeDuring, Unify U) {

        if (!c.CONDS())
            return Null;

        e = Image.imageNormalize(e);

        if (c.equals(e)) {
            if (includeMatched)
                return c;
            else if (includeBefore || includeAfter)
                return True;
            else
                return Null;
        }

//        java.util.Set<Term> eteCommon = Cond.factoredEternalsCommon(c, e);
//        if (eteCommon!=null) {
//            Term _c2 = Cond.eternalsRemove(c, eteCommon);
//            if (_c2 instanceof Bool)
//                return Null;
//            Compound c2 = (Compound) _c2;
//            Term e2 = Cond.eternalsRemove(e, eteCommon);
//            Term c3 = match(c2, e2,fast, includeBefore, includeMatched, includeAfter, includeDuring, U);
//            if (c3!=Null) {
//                eteCommon.add(c3);
//                return CONJ.the(eteCommon);
//            }
//        }

        int varBits = U.vars;
        boolean unifyVars = varBits!=0 && (e.hasAny(varBits) || c.hasAny(varBits));

        if (!unifyVars && !hasAll(c.structSubs(), e.struct() & ~CONJ.bit))
            return Null;

        int varOrTimeBits = varBits | Temporals;
        boolean unify = c.hasAny(varOrTimeBits) || e.hasAny(varOrTimeBits);

        boolean inhExplode = true;

        try (var C = ConjList.conds(c, 0, true, false, inhExplode)) {
            try (var E = ConjList.conds(e, 0, true, false, inhExplode)) {
                boolean fwd = (includeAfter == includeBefore) ? U.random.nextBoolean() : includeAfter;
                if (unifyVars)
                    fwd = reverseIfVar(C, E, fwd, U);

                try (CondMatcher m = new CondMatcher(C, U.dur, unify ? U : null)) {
                    return m.match(E, fwd) ?
                        m.slice(includeBefore, includeMatched, includeAfter, includeDuring, fast) :
                        Null;
                }
            }
        }
    }

    private static boolean reverseIfVar(ConjList C, ConjList E, boolean fwd, Unify U) {
        int xs = C.size(), ys;
        if (xs > 1 && (ys=E.size()) > 1) {
            //reverse direction if unifiable var is first
            Term xFirst = C.get(fwd ? 0 : xs - 1);
            Term xLast = C.get(fwd ? xs - 1 : 0);
            Term yFirst = E.get(fwd ? 0 : ys - 1);
            Term yLast = E.get(fwd ? ys - 1 : 0);

            if ((U.var(xFirst) || U.var(yFirst)) && !(U.var(xLast) || U.var(yLast)))
                return !fwd;
        }
        return fwd;
    }

    @Nullable
    @Deprecated public static AbstractUnifier unifyPossibleConjSubterms(Compound x, Compound y, int vars) {


        int xdt = x.dt(), ydt = y.dt();
        if ((xdt==XTERNAL) || (ydt==XTERNAL) || (xdt==DTERNAL && ydt==DTERNAL)) {

            //PARALLEL/XTERNAL

            int xs = x.subs(), ys = y.subs();

            if (xs == ys) {
                //SAME ARITY
                return parallel(xdt) && parallel(ydt) ?
                    Unifier.Commutive :
                    Unifier.Conj;
            }
            else {
                //DIFFERENT ARITY
                if (xs < ys && hasAny(x.structSurfaceUnneg(), vars))
                    return Unifier.ConjPartition;
                if (ys < xs && hasAny(y.structSurfaceUnneg(), vars))
                    return Unifier.ConjPartition;

//                if (xv && xs > ys /* TODO ensure the extra size consists of vars or neg-vars */) {
//                    //pad the shorter subterm list with True's, and commute
//                    return Unifier.CommutivePad;
//                }
//                if (yv && ys > xs /* TODO ensure the extra size consists of vars or neg-vars */) {
//                    //pad the shorter subterm list with True's, and commute
//                    return Unifier.CommutivePad;
//                }

                if (xdt == XTERNAL || ydt == XTERNAL) {
//                    if (xdt == XTERNAL && ydt!=XTERNAL) {
//                        if (y.root().equals(x)) return Unifier.EqualExceptDT;
//                    } else if (ydt == XTERNAL && xdt!=XTERNAL) {
//                        if (x.root().equals(y)) return Unifier.EqualExceptDT;
//                    }

                    return Unifier.Conj;
                }

                return null; //??
            }
        } else {
            //SEQUENCE
            return Unifier.Conj;
        }
    }

    private static int unifyConjXternal2Ary(Compound x, Compound y, Unify u) {
        if (y.dt()== XTERNAL && y.subs()==2) {
            Subterms yy = y.subterms();
            Predicate<Term> uVar = u::var;
            if (yy.count(uVar) == 1) {
                int other = yy.indexOf(uVar.negate());
                Term yo = yy.sub(other);
                if (x.condOf(yo)) { //HACK
                    int xdt = x.dt();
                    try (ConjList xx = ConjList.conds(x, xdt == DTERNAL, xdt == XTERNAL)) {
                        if (xx.remove(yo)) {
                            if (u.uni(yy.sub(1 - other), xx.term()))// ? +1 : -1
                                return +1; //if fail, try default strategy in callee
                        }
                    }
                }
            }
        }
        return 0;
    }
    public static boolean conjPartition(Term x, Term y, Unify u, Subterms xx, Subterms yy) {

        Subterms[] exy = Unifier.eliminateResolve(xx, yy, u);
        if (exy != null) {
            if (exy.length == 0) return true;
            xx = exy[0]; yy = exy[1];
        }

        return switch (Integer.compare(xx.subs(), yy.subs())) {
            case -1 -> _unifyConjPartition(xx, yy, y.dt(), u);
            case +1 -> _unifyConjPartition(yy, xx, x.dt(), u);
            default ->
                    throw new WTF();
        };
    }

    /** xx contains the variables to map the larger yy */
    private static boolean _unifyConjPartition(Subterms xx, Subterms yy, int ydt, Unify u) {
        //assert(parallel(ydt));

        int uv = u.vars | NEG.bit;
        if ((xx.struct() & ~uv)!=0)
            return false; //x not entirely vars

        int xn = xx.subs();
        if (xn == 0)
            return false; //??
        else if (xn == 1) {
            Term c = CONJ.the(ydt, yy);
            if (c == Null)
                return false;
//			if (c.volume() > u.volMax)
//				return false;

            return u.uni(xx.sub(0), c); //only choice
        } else {
            //u.termute(new CommutativeCombinations(xx, yy, yy.subs() - xn)); return true;
            return false;
        }
    }

    /** conjunction unification when # subterms differ */
    public static boolean unifyConj(final Compound x, final Compound y, Unify u) {
        //TODO dternal cases?
        int xdt = x.dt(), ydt = y.dt();
        if (xdt == XTERNAL || ydt == XTERNAL) {
            return unifyConjXternal(x, y, u, xdt, ydt);
        } else {
            return True == match(x, y, true, true, false, true, u);
        }
    }

    private static boolean unifyConjXternal(Compound x, Compound y, Unify u, int xdt, int ydt) {
        //easy cases
        {
            int xyxyxyx = unifyConjXternal2Ary(x, y, u);
            if (xyxyxyx != 0)
                return xyxyxyx == +1;
        }
        {
            int yxyxyxy = unifyConjXternal2Ary(y, x, u);
            if (yxyxyxy != 0)
                return yxyxyxy == +1;
        }

        {
            //failsafe
            TermList xx = x.condsAdd(null, true, false, false);
            TermList yy = y.condsAdd(null, true, false, false);
            if (xx.subs()!=yy.subs() && (((x.structSubs()&CONJ.bit)!=0) || (y.structSubs()&CONJ.bit)!=0)) {
                //arity mismatch; try again by decomposing dternal's
                xx.clear(); x.condsAdd(xx, true, false, false);
                yy.clear(); y.condsAdd(yy, true, false, false);
            }

            boolean z;
            if (xx.subs()==yy.subs())
                z = Unifier.unifyCommute(xx, yy, true, u);
            else if (parallel(xdt) && parallel(ydt))
                z = conjPartition(x, y, u, xx, yy);
            else
                z = false;

            xx.delete(); yy.delete();
            return z;
        }
    }

    static boolean matched(BiPredicate<Term, Term> equal) {
        if (equal instanceof CondMatcher) {
            Unify U = ((CondMatcher) equal).u;
            return U == null || U.unified();
        } else
            return true;
    }

    static boolean match(ConjList xx, ConjList yy, int head, boolean fwd, BiPredicate<Term, Term> equal, MetalBitSet hit, int dtTolerance) {

        int xn = xx.size(), yn = yy.size();

        int yi = head;
        int xi = fwd ? 0 : xn - 1;

        Term YI = yy.get(yi);
        if (YI.unneg() instanceof Variable && xn == 1)
            return false; //HACK prevent matching a variable in the sequence to the only event

        if (!equal.test(xx.get(xi), YI))
            return false;

        hit.set(yi);

        int v = equal instanceof CondMatcher m ? m.unifyVars() : 0;


        Term[] xxx = xx.array();

        int end = fwd ? yn - 1 : 0;
        int di = fwd ? 1 : -1;

        Variable globbing = null;
        long globFrom = TIMELESS;

        long[] xWhens = xx.when, yWhens = yy.when;
        for (int i = 1; i < xn; i++) {

            long shift = yWhens[yi] - xWhens[xi];

            yi += di; if (yi < 0 || yi >= yn) return false;
            xi += di;

            if (v > 0 && globbing == null) {
                Term nextX = xx.get(xi);
                if (nextX instanceof Variable && nextX.isAny(v)) {
                    if (i == xn - 1) {
                        //glob remainder, to end of Y
                        int a, b;
                        if (fwd) {
                            a = yi;
                            b = yn;
                        } else {
                            a = 0;
                            b = yi + 1;
                        }
                        if (equal.test(nextX, yy.term(a, b))) {
                            hit.setRange(true, a, b);
                            return true;
                        } else
                            return false;
                    } else {
                        //glob internal, to next match
                        globbing = (Variable) nextX;
                        globFrom = yWhens[yi];
                        continue;
                    }
                }
            }


            int ym = yy._indexOf(xWhens[xi]+shift, xxx[xi], head, end, equal, dtTolerance,  hit);
            if (ym == -1) {
                if (globbing!=null) {
                    xi -= di; //pause X
                    i--;
                    continue;
                }
                return false;
            }
            hit.set(ym);

            if (globbing!=null) {
                if (!globbed(yy, hit, yn, yWhens[yi], yWhens, globFrom, equal, globbing))
                    return false; //failed somehow

                globbing = null;
                globFrom = TIMELESS;
            }
        }


        return true;
    }

    private static boolean globbed(ConjList yy, MetalBitSet hit, int yn, long yWhen, long[] yWhens, long globFrom, BiPredicate<Term, Term> equal, Variable globbing) {
        var gg = glob(yy, hit, yn, yWhen, yWhens, globFrom);
        if (gg != null && equal.test(globbing, gg.term())) {
            gg.delete();
            return true;
        } else
            return false;
    }

    @Nullable private static ConjList glob(ConjList yy, MetalBitSet hit, int yn, long yWhen, long[] yWhens, long globFrom) {
        long globTo = yWhen;
        if (globFrom > globTo) {
            long x = globFrom; globFrom = globTo; globTo = x;
        }
        ConjList gg = null;
        for (int j = 0; j < yn; j++) {
            if (!hit.test(j)) {
                long yj = yWhens[j];
                if (yj >= globFrom && yj <= globTo) {
                    if (gg == null) gg = new ConjList(/* TODO size estimate */);
                    gg.addDirect(yj, yy.get(j));
                    hit.set(j);
                }
            }
        }
        return gg;
    }

}