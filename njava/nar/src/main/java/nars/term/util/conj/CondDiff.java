package nars.term.util.conj;

import jcog.data.bit.MetalBitSet;
import nars.Term;
import nars.term.Neg;
import nars.term.builder.TermBuilder;
import nars.term.util.TermTransformException;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static nars.Op.*;
import static nars.term.atom.Bool.True;

public enum CondDiff {
    ;

    public static Term diffFirst(Term include, Term exclude) {
        return diff(include, exclude, false, +1, terms);
    }

    public static Term diffAny(Term include, Term exclude, boolean pn) {
        return diff(include, exclude, pn, ThreadLocalRandom.current().nextBoolean() ? +1 : -1, terms);
    }

    public static Term diffFirst(Term c, Term e, boolean pn) {
        return diff(c, e, pn, +1, terms);
    }

    public static Term diffFirstPN(Term include, Term exclude) {
        return diff(include, exclude, true, +1, terms);
    }

    public static Term diffAll(Term include, Term exclude) {
        return diff(include, exclude, false, 0, terms);
    }

    public static Term diffAllPN(Term include, Term exclude) {
        return diff(include, exclude, true, 0, terms);
    }

    /**
     * @param dir 0 = all matches, +1 = next forward (from beginning) match, -1 = next backward (from end) match
     * TODO cleanup
     */
    private static Term diff(Term _inc, Term exc, boolean pn, int dir, TermBuilder B) {
        Term inc = _inc;
        boolean incNeg = false;
        if (pn && inc instanceof Neg) {
            inc = inc.unneg();
            incNeg = true;
        }

        if (!inc.CONDS())
            return inc;

        if (pn) {
            exc = exc.unneg();
            //if (include.unneg().equals(exclude)) return True;
        }

        if (inc.equals(exc))
            return True;

        //test structure disjointedness
        int incStructure = inc.struct() & ~CONJ.bit;
        int excStructure = exc.struct() & ~CONJ.bit;
        return hasAll(incStructure, excStructure) ?
            _diff(inc, exc, pn, dir, B).negIf(incNeg) :
            _inc;
    }

    private static Term _diff(Term inc, Term exc, boolean pn, int dir, TermBuilder B) {
        boolean xt = inc.dt() == XTERNAL;
        try (ConjList cc = ConjList.conds(inc, true, xt)) {

//            TermList eternalShadow;
//            if (!xt && !pn && cc.eventCount(ETERNAL) > 0 && exc.CONJ() && exc.dt() == DTERNAL) {
//                //try removing eternal components
//                var exc2 = exc.subtermsDirect().toTmpList();
//                eternalShadow = new TermList(Math.max(0 /* HACK */, exc2.subs() - cc.eventCount(ETERNAL)));
//                boolean removed = exc2.removeIf(e -> {
//                    if (cc.contains(ETERNAL, e)) {
//                        //if (cc.remove(ETERNAL, e))
//                        eternalShadow.add(e);
//                        return true;
//                    }
//                    return false;
//                });
//                if (removed) {
//                    exc = B.conj(exc2);
//                }
//            } else
//                eternalShadow = null;


            boolean excludeInh = exc.hasAny(INH);
            if (excludeInh) cc.inhExploded(B);

//        //HACK for factored sequences
//        if (inc.dt()==DTERNAL && cc.eventRange()==0 && cc.count(Term::SEQ)==1) {
//            if (cc.removeAll(exc, pn))
//                return cc.term(B); //HACK
//
//            cc = distribute(cc, B);
//
//            if (cc.removeAll(exc, pn))
//                return cc.term(B);
//        }

            Term d = dir != 0 ?
                    diffNext(inc, exc, pn, dir, B, excludeInh, cc) :
                    diffAll(inc, exc, pn, B, cc);
            if (d != null) return d;

//            if (eternalShadow != null && cc.eventCount(ETERNAL) == cc.size()) {
//                //all eternals left
//                cc.removeAll(eternalShadow);
//            }

            if (cc.isEmpty())
                return True;

            return xt ? B.conj(XTERNAL, cc.toArray()) : cc.term(B);
        }
    }

    @Nullable
    private static Term diffNext(Term inc, Term exc, boolean pn, int dir, TermBuilder B, boolean excludeInh, ConjList cc) {
        if (exc.CONJ() && exc.dt()==DTERNAL && exc.seqDur()==0) {
            //parallel eternal remove
            exc.subtermsDirect().forEach(cc::removeAll);
            return null;
        }

        ConjList xx = ConjList.conds(exc);
        if (excludeInh) xx.inhExploded(B);

        //random forward (from start), or reverse (from end) for complete outer-first decomposition
        //boolean fwd = ThreadLocalRandom.current().nextBoolean();

        MetalBitSet hit = cc.find(xx, 0, dir > 0, pn ? Term::equalsPN : Term::equals);
        if (hit == null)
            return inc;

        boolean rem = cc.removeAll(hit);
        if (!rem)
            throw new TermTransformException("not removed", inc, exc);
        return null;
    }

    @Nullable
    private static Term diffAll(Term inc, Term exc, boolean pn, TermBuilder B, ConjList cc) {
        Iterable<Term> excludes = exc.SEQ() ?
                List.of(exc) : //if sequence, dont decompose it
                ConjList.conds(exc).inhExploded(B);
        boolean removed = false;
        for (Term e : excludes)
            removed |= cc.removeAll(e, pn);

        if (excludes instanceof ConjList C)
            C.close();

        return removed ? null : inc;
    }
}