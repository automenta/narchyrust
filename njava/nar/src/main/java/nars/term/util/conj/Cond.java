package nars.term.util.conj;

import jcog.TODO;
import jcog.data.bit.MetalBitSet;
import nars.NAL;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.subterm.TmpTermList;
import nars.term.Compound;
import nars.term.Neg;
import nars.time.Tense;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;

/**
 * representation of conjoined (eternal, parallel, or sequential) events specified in one or more conjunctions,
 * for use while constructing, merging, and/or analyzing
 * <p>
 * https://en.wikipedia.org/wiki/Logical_equivalence
 * https://en.wikipedia.org/wiki/Negation_normal_form
 * https://en.wikipedia.org/wiki/Conjunctive_normal_form
 */
public enum Cond {
    ;

    public static TermList conds(Compound x, boolean decompE, boolean decompX) {

        TermList y = new TermList();
        if (x.CONJ()) {
            switch (x.dt()) {
                case DTERNAL, XTERNAL -> y.addAll(x.subterms());
                default -> x.conds(y::add, decompE, decompX);
            }
        } else {
            y.add(x);
        }

        return y;
    }

    @Deprecated public static int condEarlyLate(Term x, boolean earlyOrLate) {
        int dt = x.dt();
        return switch (dt) {
            case XTERNAL    -> throw new UnsupportedOperationException();
            case DTERNAL, 0 -> earlyOrLate ? 0 : 1;
            default         -> (dt < 0) ? (earlyOrLate ? 1 : 0) : (earlyOrLate ? 0 : 1);
        };
    }


    public static Term negateConds(Term x, boolean eteDecompose) {
        if (x.CONDS()) {
            try (ConjList c = ConjList.conds(x, 0, eteDecompose, false, eteDecompose)) {
                if (c.size() > 1) {
                    c.negateConds();
                    return c.term();
                }
            }
        }

        return x.neg();
    }


//    private static ConjList distribute(ConjList l, TermBuilder B) {
//        final int iseq = l.indexOf(Term::SEQ);
//        Term seq = l.get(iseq);
//        l.remove(seq);
//        ConjList y = ConjList.events(seq);
//        Term ll = l.term();
//        y.replaceAll(z -> CONJ.build(B, ll, z));
//        return y;
//    }


//                } else {
//                    MetalBitSet found = cc.contains(exclude, match);
//
//                    if (found!=null) {
//                        cc.removeThe(found.random(ThreadLocalRandom.current()));
//                        return cc.term(B);
//                    }
//                }

//        } else {
//
//            //HACK this is old
//
//            boolean iSeq = include.SEQUENCE();
//
//            if (eSeq && iSeq) {
//
//                ConjList cc = ConjList.events(include); cc.inhExplode(B);
//                ConjList xx = ConjList.events(exclude); xx.inhExplode(B);
//

//
//                return cc.term(B);
//
//            } else {
//                int edt = exclude.dt();
////                    if (iSeq) {
//
//                    //TODO use ConjMatch ?
//
//                    ConjList ii = ConjList.events(include, 0); ii.inhExplode(B);
//                    boolean removedSomething;
//                    if (exclude.CONJ()) {
//                        //int iStruct = include.structureSubs();
//                        int sizeBefore = ii.size();
//                        ((Compound) exclude).events(x -> {
//                            //if (Op.hasAll(iStruct, (pn ? x.unneg() : x).structure()))
//                            if (ConjBundle.bundled(x))
//                                ConjBundle.eventsOR(x, xx -> ii.removeAll(xx, pn));
//                            else
//                                ii.removeAll(x, pn);
//                        }, edt == DTERNAL, edt == XTERNAL);
//                        removedSomething = ii.size() < sizeBefore;
//                    } else {
//                        removedSomething = ii.removeAll(exclude, pn);
//                    }
//
//
//                    if (removedSomething)
//                        return ii.term(B);
//
////                    } else {
////
////                        Predicate<Term> q;
////                        if (exclude.CONJ() && edt != XTERNAL) {
////                            //assert (exclude.dt() == DTERNAL);
////                            Subterms ee = exclude.subterms();
////                            q = ee.contains(pn, ee.subs() > 3);
////                        } else {
////                            q = pn ? exclude::equalsPN : exclude::equals;
////                        }
////                        MetalBitSet y = incSubs.indicesOfBits(q.negate());
////
////                        int yCardinality = y.cardinality();
////                        if (yCardinality == 0)
////                            return True; //all removed
////                        else if (yCardinality == 1) {
////                            return incSubs.sub(y.first(true));
////                        } else if (yCardinality != incSubs.subs())
////                            return B.conj(include.dt(), incSubs.subsIncExc(y, true));
////                    }
//            }
//        }

//        return include; //unchanged
//    }

//    private static int selectRandom(int[] found) {
//        return found[found.length == 1 ? 0 : ThreadLocalRandom.current().nextInt(found.length)];
//    }



    @Nullable public static Term unifiableCond(/*Compound*/Term conj, Term _event, boolean pn, boolean novel, Unify u) {
        if (!conj.CONDS())
            return null; //throw new WTF();

        Term event = pn ? _event.unneg() : _event;
        if (!event.CONDABLE())
            return null; //impossible

        boolean eConj = event.CONJ();
        int edt = eConj ? _event.dt() : XTERNAL;

        TermList cc = conds((Compound) conj, (!eConj || edt!=DTERNAL), (!eConj || edt!=XTERNAL));

        return
            event.dt()==DTERNAL /* par */  && event.CONDS() ?
                unifiableConds(conj, event, cc, pn, novel, u) :
                unifiableCond(conj, event, cc, pn, novel, u);
    }

    /** sub-parallel TODO */
    @Nullable private static Term unifiableConds(Term conj, Term events, TermList cc, boolean pn, boolean novel, Unify u) {

        TermList ee = conds((Compound) events, true, false);
        if (conj.hasAny(INH) && events.hasAny(INH)) {
            cc.inhExplode(false);
            ee.inhExplode(false);
        }

//        if (conj.hasAny(INH))         cc.inhExplode(false);
//        if (events.hasAny(INH))         ee.inhExplode(false);

        MetalBitSet matches = null;
        for (Term e : ee) {
            MetalBitSet xm = cc.match(filter(e, pn, novel, u));
            if (xm.isEmpty())
                return null; //fail
            if (matches == null)
                matches = xm;
            else
                matches.orThis(xm);
        }
        if (matches == null)
            return null;
        cc.removeAll(matches.negateAll(cc.size()));
        return CONJ.the((Subterms)cc);
    }

    @Nullable private static Term unifiableCond(Term conj, Term event, TermList cc, boolean pn, boolean novel, Unify u) {
        Predicate<Term> filter = filter(event, pn, novel, u);
        MetalBitSet matches = cc.match(filter);
        if (matches.isEmpty()) {
            //none, so try exploding:
            if (conj.hasAny(INH) && event.hasAny(INH) && cc.inhExplode(false))
                matches = cc.match(filter);
        }

        if (matches == null || matches.isEmpty())
            return null;

        cc.removeAll(matches.negateAll(cc.size()));
        return cc.get(u.random);
    }

    private static Predicate<Term> filter(Term event, boolean pn, boolean novel, Unify u) {
        int eo = event.opID();
        return (Term x) -> {
            if (pn && x instanceof Neg) x=x.unneg();
            return eo == x.opID() &&
                    (!novel || !x.equals(event)) &&
                    u.unifies(event, x, NAL.derive.TTL_UNISUBST);
        };
    }

//    public static Term firstEvent(Term y) {
//        Term[] first = new Term[1];
//        ((Compound)y).condsAND((w, e)->{
//            first[0] = e;
//            return false;
//        }, 0, false, false);
//        return first[0];
//    }


    /** TODO dtDither option for dt intervals of result temporal terms */
    public static Term implConj(Term x, Term y, Term z, char mode, boolean SP, Term tImpl, Term bImpl) {
        if (mode!='c' && mode!='d')
            throw new UnsupportedOperationException();

        //assert(tImpl.IMPL());   assert(bImpl.IMPL());

//        if (!Conj.negatedSequenceFilter(x, CD))
//            return Null;
//        if (!Conj.negatedSequenceFilter(y, CD))
//            return Null;

        boolean CD = mode == 'c';
        int tdt = tImpl.dt(), bdt = bImpl.dt();
        if (tdt == XTERNAL || bdt == XTERNAL)
            return implConj(x, y, z, CD, SP, XTERNAL, XTERNAL);
        if (tdt==DTERNAL) tdt = 0; //HACK
        if (bdt==DTERNAL) bdt = 0; //HACK


        int xr = x.seqDur(), yr = y.seqDur();

        if (tdt == 0 && bdt == 0 && xr == 0 && yr == 0)
            return implConj(x, y, z, CD, SP, DTERNAL, DTERNAL);

        try (ConjList XY = new ConjList(3)) {

            if (!XY.add(SP ? (long) -tdt - xr : tdt, x.negIf(!CD)))
                return Null;
            if (!XY.add(SP ? (long) -bdt - yr : bdt, y.negIf(!CD)))
                return Null;

            Term xy = XY.term();
            //contradiction
            return xy.CONCEPTUALIZABLE() ?
                    implConj(z, SP, Tense.occToDT(SP ?
                            -XY.whenLatest() : XY.whenEarliest()), xy.negIf(!CD))
                    :
                    Null;
        }
    }




//    public static boolean negatedSequenceFilter(Term x, boolean CD) {
//        return !x.unneg().SEQ() || !(x instanceof Neg) == CD;
//    }

    private static Term implConj(Term x, Term y, Term z, boolean cd, boolean sp, int idt, int cdt) {

        if (!cd) { x = x.neg(); y = y.neg(); }

        Term xy = CONJ.the(x, cdt, y);

        return xy.CONCEPTUALIZABLE() ? implConj(z, sp, idt, xy.negIf(!cd)) : Null;

    }

    private static Term implConj(Term z, boolean sp, int idt, Term xy) {
        //assert(xy.CONCEPTUALIZABLE());
        return sp ? IMPL.the(xy, idt, z) : IMPL.the(z, idt, xy);
    }

    public static boolean commonSubCond(Term x, Term y, boolean inclEq, int polarity) {

        if (polarity == 0) { x = x.unneg(); y = y.unneg(); }

        if (inclEq && x.equals(y))
            return true;

        if (!Term.commonEventStructure(x, y))
            return false;

        boolean xe = x.CONDS(), ye = y.CONDS();
        if (!xe && !ye)
            return false;
        else if (xe && ye) {
            return commonSubCond(x, y, polarity);
        } else if (xe) {
            return ((Compound) x).condOf(y, polarity);
        } else /* if (ye) */ {
            return ((Compound) y).condOf(x, polarity);
        }
    }

    private static boolean commonSubCond(Term x, Term y, int polarity) {
        if (x.complexity() > y.complexity()) {
            Term z = y;
            y = x;
            x = z;
        }

        Compound Y = (Compound) y;
        Predicate<Term> yEq = Y.equals(polarity).or(z -> Y.condOf(z, polarity));

        return ((Compound) x).condsOR(y.hasAny(INH) ?
                ((Predicate<Term>) z ->
                    ConjBundle.bundled(z) && ConjBundle.eventsOR(z, yEq)
                ).or(yEq)
                :
                yEq
        , true, true);
    }

    public static Term intersect(Term x, Term y) {
        var xy = intersectConds(x,y);
        return xy.isEmpty() ? Null : CONJ.the(xy);
    }

    public static ConjList intersectConds(Term x, Term y) {
        if (!x.CONDS() || !y.CONDS())
            return new ConjList(); //HACK
        if (x.SEQ() || y.SEQ())
            throw new TODO(); //return java.util.Set.of();

        try (var xx = ConjList.conds(x, 0, true, true, true)) {
            try (var yy = ConjList.conds(y, 0, true, true, true)) {
                //TODO optimal order of x and y to reduce # comparisons?
                xx.retainAll(yy);
                return xx;
            }
        }
    }

//    @Nullable public static java.util.Set<Term> factoredEternalsCommon(Term c, Term x) {
//        if (x.CONJ() && x.dt()==DTERNAL && c.dt()==DTERNAL && c.SEQ()) {
//            java.util.Set<Term> common = null;
//            Subterms cc = c.subtermsDirect();
//            for (Term xx : (Compound) x) {
//                if (cc.contains(xx)) {
//                    if (common == null) common = new UnifiedSet<>(1);
//                    common.add(xx);
//                }
//            }
//            return common;
//        }
//        return null;
//    }

    public static Term eternalsRemove(Term x, Set<Term> common) {
        Predicate<Term> contains = common::contains;
        return CONJ.the((Subterms)
            new TmpTermList(x.subtermsDirect(), contains.negate()));
    }

}