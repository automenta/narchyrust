package nars.deriver.op.time;

import nars.Deriver;
import nars.NAL;
import nars.Term;
import nars.deriver.op.DerivedOccurrence;
import nars.premise.NALPremise;
import nars.task.SerialTask;
import nars.term.Neg;
import nars.term.Termlike;
import nars.truth.MutableTruthInterval;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.ETERNAL;
import static nars.Op.TIMELESS;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

public interface OccSolver {

    @Nullable Pair<Term, MutableTruthInterval> solve(NALPremise i, Term x, MutableTruthInterval t, byte punc, DerivedOccurrence solver);

    static @Nullable Pair<Term, MutableTruthInterval> occ(NALPremise i, Term x, MutableTruthInterval t, byte punc, Deriver d, DerivedOccurrence time) {
        var o = simple(i, x, time) ? pair(x, t) :
                new UltimateOccSolver(d).solve(i, x, t, punc, time);
        if (NAL.DEBUG && o!=null)
            assertOccValid(o.getTwo(), d);
        return o;
    }

    private static boolean simple(NALPremise i, Term x, DerivedOccurrence time) {
        var xtv = x.TEMPORAL_VAR();
        return time.relative() ?
            !xtv && !((NALPremise.NonSeedTaskPremise)i).isTemporal()
            :
            !xtv;
    }

    private static void assertOccValid(MutableTruthInterval o, Deriver d) {
        assertOccValid(o.s, o.e, d);
    }

    private static void assertOccValid(long s, long e, Deriver d) {

        var task = d.premise.task();
        var belief = d.premise.belief();
        var single = belief == null;

        if (!((e != TIMELESS) &&
                (s == ETERNAL) == (e == ETERNAL) &&
                (e >= s)) ||
                ((s == ETERNAL) &&
                        (single ? !task.ETERNAL() : (!task.ETERNAL() || !belief.ETERNAL())))) {
            throw new RuntimeException("invalid occurrence result: " + s + ".." + e);
        }
        if (NAL.test.DEBUG_EXTRA /*&& solver!=Now*/) {
            if (s != ETERNAL && !(task instanceof SerialTask) && !(belief instanceof SerialTask)) {
                var taskRange = task.ETERNAL() ? 0 : task.range();
                var beliefRange = single ? 0 : (belief.ETERNAL() ? 0 : belief.range());
                var concRange = e - s;
                if (concRange - 2L * d.timeRes() > Math.max(taskRange, beliefRange))
                    throw new RuntimeException("excessive derived task occurrence: task=" + taskRange + ", belief=" + beliefRange + " conc=" + concRange);
            }
        }
    }

    boolean[] FALSE_THEN_TRUE = {false, true}, FALSE = {false};


    static boolean negWrap(Term x, Term T, Term B, short[] pn) {
        var xn = x instanceof Neg;

        var p = xn ? x.unneg() : x;
        var e = p.equals();
        boolean tt = !e.test(T), bb = !e.test(B);

        if (tt || bb) {
            var i = p.impossibleSubTermOf();

            if (tt) count(pn, T, e, i);
            if (bb) count(pn, B, e, i);
            if (xn) {
                if (pn[0] > 0) return pn[1] == 0; //mix; dont change: neg->pos to match all contained pos
            } else {
                if (pn[1] > 0) return pn[0] == 0; //mix; dont change: pos->neg to match all contained neg
            }
        }
        return xn;
    }

    static void count(short[] pn, Term c, Predicate<Term> e, Predicate<Termlike> impossibleSubtermOf) {
        c.ANDrecurse(z ->  !impossibleSubtermOf.test(z), (z, p) -> {
            if (e.test(z))
                pn[p instanceof Neg ? 1 : 0]++;
            return true;
        }, null);
    }

}
