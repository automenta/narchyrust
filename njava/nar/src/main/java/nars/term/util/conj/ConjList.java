package nars.term.util.conj;

import jcog.data.bit.MetalBitSet;
import jcog.data.set.LongObjectArraySet;
import nars.NAL;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.subterm.TmpTermList;
import nars.term.Compound;
import nars.term.builder.TermBuilder;
import nars.term.util.TermException;
import nars.time.Tense;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static nars.Op.*;
import static nars.term.atom.Bool.*;

/**
 * prepares construction of a conjunction target from components,
 * in the most efficient method possible according to them.
 * it is lighter weight than Conj.java in buffering / accumulating
 * events prior to complete construction.
 */
public class ConjList extends LongObjectArraySet<Term> implements ConjBuilder, AutoCloseable {

    public ConjList() {
        this(0);
    }

    public ConjList(int expectedSize) {
        super(0, new Term[expectedSize]);
    }

    private ConjList(long when, Term onlyEvent) {
        this(1);
        add(when, onlyEvent);
    }

    private ConjList(long when, Compound conj, boolean decomposeEte, boolean decomposeXte, boolean inhExplode) {
        this();
        addConds(when, conj, decomposeEte, decomposeXte, inhExplode, null);
    }

    public static ConjList conds(Term conj) {
        return conds(conj, true, false);
    }

    public static ConjList conds(Term e, boolean decomposeEte, boolean decomposeXternal) {
        return conds(e, 0, decomposeEte, decomposeXternal);
    }

    public static ConjList conds(Term e, long when, boolean decomposeEte, boolean decomposeXternal, boolean inhExplode) {
        return e.CONDS() ?
            new ConjList(when, (Compound) e, decomposeEte, decomposeXternal, inhExplode) :
            new ConjList(when, e);
    }

    public static ConjList conds(Term e, long when, boolean decomposeEte, boolean decomposeXternal) {
        return conds(e, when, decomposeEte, decomposeXternal, false);
    }


    static int centerByIndex(int startIndex, int endIndex) {
        return startIndex + (endIndex - 1 - startIndex) / 2;
    }

    public static ConjList eventsParallel(Compound b) {
        return eventsParallel(b, 0);
    }

    private static ConjList eventsParallel(Compound b, int when) {
        assert (b.CONJ());
        var bb = b.subtermsDirect();
        var c = new ConjList(bb.subs());
        for (var x : bb)
            c.addDirect(when, x);
        return c;
    }


    @Override
    public boolean removeAll(Term x) {
        return !isEmpty() && removeIf(x.equals());
    }

    public boolean removeAllNeg(Term x) {
        return removeIf(x::equalsNeg);
    }

    /**
     * assumes its sorted
     */
    long _start() {
        return when[0];
    }

    /**
     * assumes its sorted
     */
    long _end() {
        return when[size - 1];
    }

    public boolean contains(ConjList x) {
        return contains(x, Term::equals);
    }

    public boolean contains(ConjList x, BiPredicate<Term, Term> equal) {
        return contains(x, 0, equal) != null;
    }

    public MetalBitSet contains(ConjList x, int from, BiPredicate<Term, Term> equal) {
        return find(x, from, true, equal);
    }

    @Nullable
    public MetalBitSet find(ConjList x, int from, boolean fwd, BiPredicate<Term, Term> equal) {
        var hit = MetalBitSet.bits(size());
        return CondMatcher.next(x, this, equal, from, fwd, hit, 1) ? hit : null;
    }

//    /**
//     * simple event match
//     */
//    @Nullable
//    public MetalBitSet contains(Term x, BiPredicate<Term, Term> equal) {
//        MetalBitSet indices = null;
//        for (int i = 0, n = this.size(); i < n; i++) {
//            Term z = this.get(i);
//            if (equal.test(z, x)) {
//                if (indices == null) indices = MetalBitSet.bits(n);
//                indices.set(i);
//            }
//        }
//        return indices;
//    }

    /**
     * consistent with ConjBuilder - semantics slightly different than superclass and List.addAt: returns true only if False or Null have been added; a duplicate value returns true
     */
    @Override
    public boolean addEvent(long when, Term t) {
        if (t == False || t == Null) {
            fail(t);
            return false;
        }

        //quick test for absorb or conflict
        var n = size;
        if (n > 0) {
            var W = this.when;
            var X = this.items;
            var exists = false;
            var incomingEternal = when == ETERNAL;
            for (var i = 0; i < n; i++) {
                var ww = W[i];
                var exact = ww == when;
                var existEternal = ww == ETERNAL;
                if (incomingEternal || existEternal || exact) {
                    var ii = X[i];
                    if (ii.equals(t)) {
                        if (!exact && !existEternal)
                            W[i] = ETERNAL; //subsume
                        exists = true;
                    } else {
                        if (NAL.CONJ_BOOL && ii.equalsNeg(t)) {
                            fail(False);
                            return false; //conflict
                        }
                    }
                }
            }
            if (exists)
                return true;
        }

        addDirect(when, t);
        return true;
    }

    private void fail(Term t) {
        clear();
        addDirect(ETERNAL, t);
    }

    @Override
    public final boolean add(long at, Term t) {
        return ConjBuilder.super.add(at, t);
    }

    @Override
    public int eventOccurrences() {
        var s = size;
        var when = this.when;
        switch (s) {
            case 0 -> {
                return 0;
            }
            case 1 -> {
                return 1;
            }
            case 2 -> {
                //quick tests
                return when[0] == when[1] ? 1 : 2;
            }
            case 3 -> {
                //quick tests
                if (when[0] == when[1])
                    return when[1] == when[2] ? 1 : 2;
                else
                    return when[1] == when[2] ? 2 : 3;
            }
            default -> {
                LongHashSet h = null;
                var first = when[0];
                for (var i = 1; i < s; i++) {
                    if (h == null) {
                        if (when[i] != first) {
                            h = new LongHashSet(s - i + 1);
                            h.add(first);
                        }
                    }
                    if (h != null)
                        h.add(when[i]);
                }
                return h != null ? h.size() : 1;
            }
        }
    }

    @Override
    public int eventCount(long w) {
        var s = size;
        var when = this.when;
        var c = 0;
        for (var i = 0; i < s; i++)
            if (when[i] == w)
                c++;
        return c;
    }

    @Override
    public final LongIterator eventOccIterator() {
        return longIterator();
    }

    @Override
    public void negateConds() {
        replaceAll(Term::neg);
    }

    @Override
    public final Term term(TermBuilder B) {
        var n = size;
        if (n == 0) return True;
        var items = this.items;
        if (n == 1) return items[0];

        boolean par = true, flat = true;
        var when = this.when;
        var w0 = TIMELESS;
        for (var i = 0; i < n; i++) {
            var wi = when[i];
            if (wi == ETERNAL || wi == TIMELESS) {
                flat = false; //to be safe
                continue;
            }

            if (flat && items[i].hasAny(CONJ))
                flat = false;

            if (par) {
                if (w0 == TIMELESS)
                    w0 = wi; //initialize
                else if (w0 != wi)
                    par = false; //some difference
            }

            if (!flat && !par)
                break;
        }

        if (par)
            return B.conj(toArray());
        else
            return flat ?
                termFast(B) :
                ConjSeq.conjSeqComplex(n, items, when, B);
    }

    /**
     * supposed to be a streamlined construction process avoiding ConjTree
     */
    private Term termFast(TermBuilder B) {
        sortThis(); //HACK ensure sorted by time
        return switch (size > 2 ? paralellize(B) : 0) {
            case -1 -> False;
            case -2 -> Null;
            case +1 -> True;
            default -> ConjSeq.seqBalanced(B, this, 0, size);
        };
    }

    public final Term term(int from, int to) {
        return term(from, to, terms);
    }

    /**
     * sublist
     */
    public final Term term(int from, int to, TermBuilder B) {
        if (from > to) {
            var x = from;
            from = to;
            to = x; //swap
        }

        if (from < 0 || to > size)
            throw new IndexOutOfBoundsException();

        if (from == to)
            return True;

        if (to - from == 1)
            return items[from];

        //HACK could be optimized
        try (var s = subEvents(from, to)) {
            return s.term(B);
        }
    }

//    public final Subterms asSubterms(boolean commute) {
//        DisposableTermList terms = new DisposableTermList(size, this.items);
//        if (commute)
//            terms.sortAndDedup();
//        return terms;
//    }

//    public final boolean removeNeg(long at, Term t) {
//        return removeIf((when, what) -> at == when && t.equalsNeg(what));
//    }

    public ConjList subEvents(int from, int to) {
        if (from == 0 && to == size)
            return this;

        var w = this.when;
        var ii = this.items;
        var c = new ConjList(to - from);
        for (var i = from; i < to; i++)
            c.addDirect(w[i], ii[i]);
        return c;
    }

    public boolean contains(long w, Term x, boolean polarity) {
        return indexOf(w, x, polarity) != -1;
    }

    public int indexOf(long w, Term x, boolean polarity) {
        var n = size; if (n < 1) return -1;

        var ww = this.when;
        var ii = this.items;
        Predicate<Term> eqX = null;
        for (var i = 0; i < n; i++) {
            if (ww[i] == w) {
                if ((eqX==null ? eqX = x.equals(polarity ? +1 : -1) : eqX).test(ii[i]))
                    return i;
            }
        }
        return -1;
    }

    public int indexOf(Term x, int polarity) {
        var n = size; if (n < 1) return -1;
        var ii = this.items;
        var eqX = x.equals(polarity);
        for (var i = 0; i < n; i++) {
            if (eqX.test(ii[i]))
                return i;
        }
        return -1;
    }

    /**
     * returns true if something removed
     * TODO optimize
     */
    @Deprecated
    public final int removeAll(Term x, long when, boolean polarity) {
        //assert(!(x instanceof Neg));
        if (x.CONJ()) {
            return removeAllConj((Compound) x, when, polarity);
        } else if (ConjBundle.bundled(x)) {
            return removeAllInh(x, when, polarity);
        } else
            return contains(when, x, !polarity) ?
                    -1 : (remove(when, x.negIf(!polarity)) ? +1 : 0);
    }

    private int removeAllInh(Term x, long when, boolean polarity) {
        var removed = new boolean[]{false};
        var r = ConjBundle.eventsOR(x, exx -> {
            if (contains(when, exx, !polarity))
                return true; //contradiction
            else {
                removed[0] |= remove(when, exx.negIf(!polarity));
                return false;
            }
        });
        return r ? -1 : (removed[0] ? +1 : 0);

    }

    private int removeAllConj(Compound x, long when, boolean polarity) {
        var removed = new boolean[]{false};
        LongObjectPredicate<Term> each = (subWhen, subX) -> {
            if (contains(subWhen, subX, !polarity)) {  //if (contains(subWhen, subX.negIf(polarity))) {
                return true; //contradiction
            } else {
                removed[0] |= remove(subWhen, subX.negIf(!polarity));
                return false;
            }
        };
//        if (OR(xx -> ConjBundle.bundled(xx.unneg()))) { //TODO also test for necessary occurrence time
//            LongObjectPredicate<Term> e0 = each;
//            each = (w, xx) ->
//                ConjBundle.bundled(xx.unneg()) && !ConjBundle.eventsAND(xx.unneg(), exx -> {
//                    if (contains(w, exx, !polarity))
//                        return false; //contradiction
//                    else {
//                        removed[0] |= remove(w, exx.negIf(!polarity));
//                        return true;
//                    }
//                }) /* contradiction */ || e0.accept(w, xx);
//        }
        var r = x.condsOR(each, when, true, false, true);
        return r ? -1 : (removed[0] ? +1 : 0);
    }

//    /**
//     * assumes sorted
//     */
//    void factor(ConjTree T) {
//
//        int n = size();
//        if (n < 2)
//            return;
//
//        //sortThis();
//        int u = eventOccurrences_if_sorted();
////        if (u == n) {
////            condense(B);
////            return;
////        }
//
//        UnifriedMap<Term, RoaringBitmap> count = new UnifriedMap<>(n);
//        for (int i = 0; i < n; i++) {
//            Term xi = items[i];
//            count.getIfAbsentPut(xi, RoaringBitmap::new).add(Tense.occToDT(when[i]));
//        }
//
//
//        if (count.allSatisfy(t -> t.getCardinality() == u)) {
//            //completely annihilates everything
//            //so also remove any occurring in the parallel events
//            //T.removeParallel(count.keySet());
//        } else {
//
//            if (!count.keyValuesView().toSortedList().allSatisfy((xcc) -> {
//                RoaringBitmap cc = xcc.getTwo();
//                int c = cc.getCardinality();
//                if (c < u) {
////                    if (x.op() != NEG) {
////                        if (T.pos != null && T.posRemove(x))
////                            toDistribute.add(x);
////                    } else {
////                        if (T.neg != null && T.negRemove(x.unneg()))
////                            toDistribute.add(x);
////                    }
//                } else {
//                    PeekableIntIterator ei = cc.getIntIterator();
//                    while (ei.hasNext()) {
//                        if (eventCount(ei.next()) == 1)
//                            return true; //factoring would erase this event so ignore it
//                    }
//                    Term x = xcc.getOne();
//                    //new factor component
//                    if (!T.addParallel(x))
//                        return false;
//                    removeAll(x);
//                }
//                return true;
//            })) {
//                T.end(False);
//            }
//
//
////            int dd = toDistribute.size();
////            if (dd > 0) {
//////            if(dd > 1)
//////                toDistribute.sortAndDedup();
////
////                n = size();
////
////                //distribute partial factors
////                for (int i = 0; i < n; i++) {
////                    Term xf = get(i);
////                    if (dd == 1 && toDistribute.sub(0).equals(xf))
////                        continue;
////
////                    Term[] t = new Term[dd + 1];
////                    toDistribute.arrayClone(t);
////                    t[t.length - 1] = xf;
////                    Term xd = CONJ.the(t);
////                    if (xd == False || xd == Null) {
////                        T.terminate(xd);
////                        return;
////                    }
////                    set(i, xd);
////                }
////            }
//        }
//
//
//    }

    /**
     * counts # of unique occurrence times, assuming that the events have already been sorted by them
     */
    private int eventOccurrences_if_sorted() {
        var c = 1;
        var when = this.when;
        var w0 = when[0];
        var s = this.size;
        for (var i = 1; i < s; i++) {
            var wi = when[i];
            if (wi != w0) {
                assert (wi > w0);
                c++;
                w0 = wi;
            }
        }
        return c;
    }

    public void sortThisByValueEternalFirst() {
        sortThis(this::compareEternalityValueTime);
    }

    private int compareEternalityValueTime(int a, int b) {
        if (a == b) return 0;
        boolean ae = when[a] == ETERNAL, be = when[b] == ETERNAL;
        if (ae == be) {
            var ab = compareValue(a, b);
            return ab != 0 ? ab : compareTime(a, b);
        } else {
            return ae ? -1 : +1;
        }
    }

//    public int centerByVolume(int startIndex, int endIndex) {
//        int n= endIndex - startIndex;
//        int midIndex = centerByIndex(startIndex, endIndex);
//        if (n <= 2)
//            return midIndex;
//        int[] v = new int[10];
//        int count = 0;
//        for (int i1 = 0; i1 < n; i1++) {
//            int volume = get(startIndex + i1).volume();
//            if (v.length == count) v = Arrays.copyOf(v, count * 2);
//            v[count++] = volume;
//        }
//        v = Arrays.copyOfRange(v, 0, count);
//
//        int bestSplit = 1, bestSplitDiff = Integer.MAX_VALUE;
//        for (int i = 1; i < n-1; i++) {
//            int pd = Math.abs(Util.sum(v, 0, i) - Util.sum(v, i, n));
//            if (pd <= bestSplitDiff) {
//                bestSplit = i;
//                bestSplitDiff = pd;
//            }
//        }
//
//        return bestSplit + startIndex;
//
//    }

    /**
     * combine events at the same time into parallel conjunctions
     * WARNING assumes the list is sorted in time
     *
     * @return 0 if ok, +1 if reduced to true, -1 if reduced to false, -2 if null
     */
    public int paralellize(TermBuilder B) {
        var s = size;
        if (s <= 1)
            return 0;

        var when = this.when;
        var items = this.items;
        var start = 0;
        var last = when[0];
        for (var i = 1; i <= s; i++) {
            var end = i == s;
            var wi = end ? XTERNAL : when[i];
            if (end || last != wi) {

                if (i > start+1) {

                    var xx = Arrays.copyOfRange(items, start, i);
                    var x =
                            ConjPar.parallel(DTERNAL, new TmpTermList(xx) , true, B);
                            //B.conj(xx);
                    if (x == True) {
                        //handled below HACK
                    } else if (x == False) {
                        return -1;
                    } else if (x == Null) {
                        return -2;
                    } else if (!x.CONDABLE()) {
                        throw new TermException("conj collapse during condense", CONJ, x);
                    } else {
                        items[start] = x; //setFast(start, x);
                    }

                    var toEmpty = Math.min(i-1, s-1) - start;
                    for (var r = 0; r < toEmpty; r++) {
                        removeThe(start + 1);
                        s--;
                        i--;
                    }

                    if (x == True) {
                        removeThe(start);
                        s--;
                        i--;
                    }

                }
                last = wi;
                start = i;

            }
        }

        return s > 0 ? 0 : +1;

    }

//    /** shifts everything so that the initial when is zero. assumes it is non-empty & sorted already */
//    public void shift(long shiftFrom) {
//        long currentShift = shift();
//        long delta = shiftFrom - currentShift;
//        if (delta == 0)
//            return;
//        long[] when = this.when;
//        int s = this.size;
//        for (int k = 0; k < s; k++)
//            when[k] += delta;
//    }
//
//    public boolean removePN(long at, Term t) {
//        return removeIf((when, what) -> at == when && what.equalsPN(t));
//    }

    public final long eventRange() {
        var n = size;
        if (n <= 1)
            return 0;

        var when = this.when;
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (var i = 0; i < n; i++) {
            var w = when[i];
            if (w == ETERNAL)
                continue;
            min = Math.min(min, w);
            max = Math.max(max, w);
        }
        return min == Long.MAX_VALUE ? 0 : max - min;
    }

//    public int volEstimate() {
//
//        int n = size();
//        switch (n) {
//            case 0:
//                return 1;
//            case 1:
//                return get(0).volume();
//            default:
//                int v = 0;
//                for (int i = 0; i < n; i++)
//                    v += items[i].volume();
//                //estimation heuristic:
//                //+1 for each parallel event, +1 for each unique time
//                return v + 1 + n + uniqueEvents();
//        }
//    }

//    /** doesnt assume events are sorted, though if they were, there is a faster way to do this */
//    public int uniqueEvents() {
//        int n = size();
//        if (n == 0) return 0;
//
//        long[] w = when;
//        switch(n) {
//            case 1: return 1;
//            case 2: return w[0] == w[1] ? 1 : 2;
//            default: {
//                LongHashSet l = null;
//                for (int i = 1; i < n; i++) {
//                    long wi = w[i];
//                    long wp = w[i - 1];
//                    if (wi != wp) {
//                        if (l == null) { l = new LongHashSet(n - i + 1); l.add(wp);  }
//                        l.add(wi);
//                    }
//                }
//                if (l == null) return 1;
//                else
//                    return l.size();
//            }
//        }
//    }

    public boolean removeAll(Term e, boolean pn) {
        return removeIf(pn ? e::equalsPN : e.equals());
    }

    /**
     * returns false if err
     * TODO return true iff modified.
     */
    @Deprecated
    public void inhBundle(TermBuilder B) {
        var s = size();
        if (s <= 1) return;
        sortThis();
        ConjBundle.inhBundle(this.items, this.when, s, B);
        removeInstances(True);
    }

    @Deprecated
    public final boolean inhExplode() {
        return inhExplode(terms) == 1;
    }

    /**
     * @param B
     * @return -1 false
     * 0 no change
     * 1 change, ok
     */
    public int inhExplode(TermBuilder B) {
        final var n = size;
        var modified = false;
        for (var i = 0; i < n; i++) {
            var x = items[i];
            if (x.INH()) {
                Term S = x.sub(0), P = x.sub(1);
                boolean s = S.CONJ(), p = P.CONJ();
                if (s || p) {

                    setFast(i, True);

                    var w = when[i];
                    if (s && p) {
                        var PP = P.subtermsDirect();
                        for (var ss : S.subtermsDirect())
                            if (!addExploded(w, ss, PP, B))
                                return -1;
                    } else if (s) {
                        for (var ss : S.subtermsDirect()) {
                            if (!addExploded(w, ss, P, B))
                                return -1;
                        }
                    } else {
                        if (!addExploded(w, S, P.subtermsDirect(), B))
                            return -1;
                    }
                    modified = true;

                }
            }


        }
        if (modified) {
            removeInstances(True);
            return 1;
        } else
            return 0;
    }

    private boolean addExploded(long w, Term s, Subterms P, TermBuilder B) {
        for (var pp : P) {
            if (!addExploded(w, s, pp, B))
                return false;
        }
        return true;
    }

    private boolean addExploded(long w, Term s, Term p, TermBuilder B) {
        return addEvent(w,
                INH.build(B, s, p)
        );
    }

    public void dither(int ditherDT) {
        if (ditherDT <= 1) return;
        var s = size();
        var w = when;
        for (var i = 0; i < s; i++)
            w[i] = Tense.dither(w[i], ditherDT);

        //TODO? if (changed) sortThis();
    }

    public final ConjList inhExploded(TermBuilder B) {
        inhExplode(B);
        return this;
    }

    public void addConds(long start, Compound conj, boolean decomposeEte, boolean decomposeXternal, boolean inhExplode, @Nullable Predicate<Term> filter) {
        ensureCapacityForAdditional(4, true);
        conj.conds(filter == null ? this::addDirect : (w, x) -> {
            if (filter.test(x)) addDirect(w, x);
        }, start, decomposeEte, decomposeXternal, inhExplode);
    }

//    @Override
//    public boolean addAll(long w, Iterable<Term> tt) {
//        tryEnsureAdditionalCapacityFor(tt);
//        return ConjBuilder.super.addAll(w, tt);
//    }

//    private void tryEnsureCapacityForAdditional(Iterable<Term> tt) {
//        switch (tt) {
//            case Subterms ss -> ensureCapacityForAdditional(ss.subs(), true);
//            case Collection cc -> ensureCapacityForAdditional(cc.size(), true);
//            case null, default -> { }
//        }
//    }

    public final void addAllDirect(long w, Subterms x) {
        var s = x.subs();
        ensureCapacityForAdditional(s, false);
        if (when.length < items.length)
            when = Arrays.copyOf(when, items.length); //HACK because ensureCapacity is not overridden
        for (var i = 0; i < s; i++)
            addDirect(w, x.sub(i));
    }

//    public final void addAllNegDirect(long w, Iterable<Term> tt) {
//        tryEnsureAdditionalCapacityFor(tt);
//        for (Term t : tt)
//            addDirect(w, t.neg());
//    }

    public long whenEarliest() {
        var s = size;
        var x = Long.MAX_VALUE;
        for (var i = 0; i < s; i++) {
            var wi = when[i];
            if (wi!=ETERNAL && wi < x)
                x = wi;
        }
        if (x == TIMELESS)
            return ETERNAL;
        return x;
    }

    public long whenLatest() {
        var s = size;
        var x = Long.MIN_VALUE;
        for (var i = 0; i < s; i++) {
            var wi = when[i];
            if (/*wi!=ETERNAL && */wi > x)
                x = wi;
        }
        return x;
    }

//    public boolean equalTerms(ConjList y) {
//        if (this == y) return true;
//
//        int s = size;
//        if (s != y.size) return false;
//
//        return Arrays.equals(items, 0, s, y.items, 0, s);
//    }

    Term seq(TermBuilder B) {

        var eSize = this.size;

        if (eSize > 1) {

            this.sortThis();

//            if (NAL.term.CONJ_FACTOR) {
//                //TODO fix
//                factor(tree);
//            } else {
////                switch (this.condense(B)) {
////                    case -1 -> { return False; }
////                    case -2 -> { return Null; }
////                    //case 0, +1 -> { }
////                }
//            }
//            eSize = this.size();
        }

        return ConjSeq.seqBalanced(B, this, 0, eSize);
    }

    @Nullable public Term applyIfChanged(UnaryOperator<Term> f) {
        var ii = this.items;
        var n = size;
        var changed = false;
        for (var i = 0; i < n; i++) {
            var x = ii[i];
            var y = f.apply(x);
            if (x!=y) {
                ii[i] = y;
                changed = true;
            }
        }
        return changed ? term() : null;
    }

    public boolean containsNeg(Term x) {
        return TermList.containsNeg(this, x);
    }

    public boolean eternal(int i) {
         return when[i] == ETERNAL;
    }

//    public boolean distribute() {
//        int n = size;
//        List<Term> ete = null;
//        for (int i = 0; i < n; i++) {
//            if (when[i]==ETERNAL) {
//                Term ii = items[i];
//                nullify(i);
//                if (ete == null) ete = new Lst<>(1);
//                ete.add(ii);
//            }
//        }
//        if (ete!=null) {
//            removeNulls();
//            var w = eventOccIterator();
//            while (w.hasNext()) {
//                long ww = w.next();
//                for (Term e : ete)
//                    addEvent(ww, e);
//            }
//            sortThis();
//            return true;
//        }
//        return false;
//    }

    @Override
    public final void close() {
        delete();
    }

//     /** @return the time of first contained event (from either start or end of the list),
//      *          or XTERNAL if not found */
//    public int eventTime(Term e, boolean fromStartOrEnd) {
//        var E = e.equals();
//        int n = size;
//        Term[] items = this.items;
//        for (int i = 0; i < n; i++) {
//            int ii = fromStartOrEnd ? i : (n-1)-i;
//            if (E.test(items[ii]))
//                return occToDT(when[ii]);
//        }
//        return XTERNAL;
//    }

//    boolean drain(long wp, TermList x, TermBuilder B) {
////        assert(wp!=TIMELESS);
//        if (!NAL.term.CONJ_FACTOR) {
//            if (x.isEmpty()) return true;
//
//            Term y = B.conj(x);
//
//            x.clear();
//
//            if (y == True) return true;
//            if (y == False) return false;
//            if (!addEvent(wp, y))
//                return false; //TODO Null?
//
//        } else {
//            addAllDirect(wp, x); //keep separate
//        }
//        return true;
//    }

}