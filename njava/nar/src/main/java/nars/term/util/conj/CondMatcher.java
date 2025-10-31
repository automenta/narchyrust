package nars.term.util.conj;

import jcog.data.bit.MetalBitSet;
import nars.Term;
import nars.term.var.Variable;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static nars.Op.TIMELESS;
import static nars.term.atom.Bool.*;

public class CondMatcher implements BiPredicate<Term, Term>, AutoCloseable {
    @Nullable public Unify u;
    private int dur;
    public transient long matchStart;
    private MetalBitSet matches;
    private ConjList x;

    public CondMatcher(ConjList x) {
        this(x, 1);
    }

    public CondMatcher(ConjList x, int dur) {
        this(x, dur, null);
    }

    public CondMatcher(ConjList x, int dur, @Nullable Unify u) {
        reset(x, dur, u);
    }

    public final CondMatcher reset(ConjList x) {
        return reset(x, 1);
    }
    public final CondMatcher reset(ConjList x, int dur) {
        return reset(x, dur, null);
    }
    public final CondMatcher reset(ConjList x, int dur, @Nullable Unify u) {
        this.x = x;

        int xs = x.size();
        if (this.matches==null || this.matches.capacity() < xs)
            this.matches = MetalBitSet.bits(xs);
        else
            this.matches.clear();

        this.u = u;
        this.dur = dur;
        this.matchStart = TIMELESS;
        return this;
    }

    /** assumes they are both sorted and/or expanded/condensed in the same way
     * warning: if not forward, both arrays will be reversed.  a restoration by re-reversing is not performed TODO
     *
     * returns whether matched
     * */
    static boolean next(ConjList x, ConjList y, BiPredicate<Term,Term> equal, int from, boolean fwd /* or reverse */, MetalBitSet hit, int dtTolerance) {

        if (y == x) {
            if (hit!=null) hit.negateThis(); //set all true
            return true;
        }

        int ys = y.size();
        if (ys == 0) return false; //?? why

        int xs = x.size();
        if (xs == 0) return false; //?? why

        int to = ys - xs;
        if (to < from) return false; //too small

        boolean konstant = true;
        if (equal instanceof CondMatcher ee) {
            Unify eu = ee.u;
            if (eu!=null) {
                Predicate<Term> isUnifiableVar = z -> z instanceof Variable && z.isAny(eu.vars);
                konstant = !(y.OR(isUnifiableVar) || x.OR(isUnifiableVar));
            }
        }

        if (konstant && xs > ys)
            return false;

        x.sortThis();
        y.sortThis();

        if (konstant) {
            //fast event range comparison for constants
            int dtMargin = (dtTolerance-1) * xs; //max possible range difference
            if (x._start() + dtMargin < y._start() || x._end() - dtMargin > y._end())
                return false; //too short
        }

        int start = (equal instanceof CondMatcher && ((CondMatcher)equal).u!=null) ? ((CondMatcher)equal).u.size() : -1;

        for (int j = from; j <= to; j++) {

            int head = fwd ? j : (ys - 1 - j);

            if (CondMatch.match(x, y, head, fwd, equal, hit, dtTolerance) && CondMatch.matched(equal))
                return true;

            if (hit!=null) hit.clear();
            if (start >= 0)
                ((CondMatcher) equal).revert(start);
        }

        return false;
    }


    @Override
    public void close() {
        //u.clear(); //destructive, not safe
        u = null;
        matches = null;
        x.delete();
        x = null;
    }

    @Override
    public boolean test(Term x, Term y) {
        return u != null ? u.uni(x, y) : x.equals(y);
    }

//	public Term apply(Term x) {
//		return u.apply(x);
//	}

    public boolean match(ConjList y, boolean fwd) {
        //TODO DTERNAL contains

        boolean hit = next(y, x, this, 0, fwd,
                matches,
                dur);

        if (!hit)
            return false;
        else {
            matchStart = x.when(matches.first(true));
            return true;
        }
    }

    public Term slice(boolean includeBefore, boolean includeMatched, boolean includeAfter, boolean includeDuring, boolean fast) {
        if (!includeMatched)
            x.removeAll(matches);

        boolean empty = x.isEmpty();
        if (!empty && (!includeAfter || !includeBefore)) {

            if (!includeAfter) {
                if (x.removeIf((w, e) -> includeDuring ? w > matchStart : w >= matchStart))
                    empty = x.isEmpty();
            }

            if (!includeBefore && !empty) {
                if (x.removeIf((w, e) -> includeDuring ? w < matchStart : w <= matchStart))
                    empty = x.isEmpty();
            }
        }

        if (fast)
            return empty ? True : False;
        else {
            Term ss = x.term();
            return u != null && ss != Null ? u.postUnify(ss) : ss;
        }
    }

    public void revert(int start) {
        if (u != null)
            u.revert(start);
    }

    /** structure of any unifiable vars. 0 if no unification is attempted */
    public int unifyVars() {
        return u!=null ? u.vars : 0;
    }
}