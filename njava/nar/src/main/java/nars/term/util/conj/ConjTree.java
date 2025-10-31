package nars.term.util.conj;

import jcog.TODO;
import jcog.WTF;
import jcog.data.list.Lst;
import jcog.data.map.ObjIntHashMap;
import nars.NAL;
import nars.Term;
import nars.subterm.TermList;
import nars.subterm.TmpTermList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.atom.Bool;
import nars.term.builder.TermBuilder;
import nars.term.compound.LightCompound;
import nars.term.util.TermException;
import nars.term.util.TermTransformException;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static nars.Op.*;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.occToDT;

/**
 * conj tree node
 *
 *
 * WARNING
 * Complex Reduction Loops Without Re‐checking
 * In methods such as reducePN and reduceNN, the code iterates over a collection of opposing terms (from the negative or positive set) and applies transformations (using CondDiff.diffAll, condTransform, etc.). In several places the term being reduced is replaced with a transformed version (for example, via
 * x = handleCondDiffElimMode(X, yy, elimMode);
 * or
 * x = condTransform(X, new ConjEliminator(yy, true));
 * ).
 * However, once transformed, the code continues iterating over the rest of the collection without “re‐starting” the interference checks from the beginning. In principle this may be acceptable if the interference relations are monotonic, but it is a point that deserves careful review to ensure that no interfering condition is missed after a transformation. In short, the logic is very subtle, and while nothing clearly “blows up” here, it is a hotspot for potential subtle bugs.
 */
public class ConjTree implements ConjBuilder, AutoCloseable {

    //private IntObjectHashMap<ConjTree> seq;
    private TimeMap<ConjTree> seq;

    private Set<Term> pos = EMPTY_SET, neg = EMPTY_SET;
    private Term terminal;
    private long shift = TIMELESS;
    private static final Set<Term> EMPTY_SET = emptySet();

    private static MutableSet<Term> _newSet() {
        return new UnifiedSet<>(1);
    }

    private static Set<Term> _add(Set<Term> s, Term t) {
        if (!(s instanceof MutableSet) && s.isEmpty())
            s = _newSet();
        s.add(t);
        return s;
    }

    private static Term first(Collection<Term> s) {
        return switch (s) {
            case UnifiedSet<Term> su -> su.getFirst();
            case TreeSet<Term> st -> st.first();
            case null, default -> s.iterator().next();
        };
    }

    private static boolean innerDisj(Compound x, Term y) {
        return x.hasAll(CONJ.bit | NEG.bit) &&
                x.containsRecursively(y.unneg()) &&
                x.condsOR(z -> z instanceof Neg && z.unneg().CONJ(),
                        !(y.unneg().CONDS() && !y.SEQ()), y.dt() != XTERNAL);
    }

    private static Term condTransform(Compound x, UnaryOperator<Term> f) {
        if (x.SEQ()) {
            Term y;
            try (var xx = ConjList.conds(x, 0, false, false)) {
                y = xx.applyIfChanged(f);
            }
            return y == null ? x : y;
        } else {
            var xx = x.subtermsDirect();
            var yy = xx.transformSubs(f);
            return xx == yy ? x : CONJ.the(x.dt(), yy);
        }
    }

    @Deprecated
    private static boolean possibleInterference(Term X, Term yy) {
        return possibleInterference(X.struct(), yy);
    }

    private static boolean possibleInterference(int xs, Term yy) {
        return hasAll(xs, yy.struct() & ~CONJ.bit);
    }

    private static void addNegs(Set<Term> from, Collection<Term> to) {
        if (to instanceof TermList eachTL) {
            eachTL.addAllNeg(from);
        } else {
            for (var x : from)
                to.add(x.neg());
        }
    }

    private static void _factor(TermBuilder B, Collection<Term> e, int n, ObjIntHashMap<Term> counts, ObjIntHashMap<Term> singles, ConjList s) {
        if (counts != null) {
            for (var z : counts.keyValuesView())
                if (z.getTwo() == n) e.add(z.getOne());
        }

        if (singles != null && !e.isEmpty()) {
            for (var z : singles.keyValuesView())
                if (z.getTwo() == n)
                    e.remove(z.getOne());
        }

        var E = e.isEmpty() ? null : e;

        s.replaceAll(c -> {
            Term d;
            if (c instanceof LightCompound) {
                assert (c.SETe());
                var kk = (TermList) c.subterms();
                kk.removeIf(e::contains);
                var kkn = kk.size();
                d = (kkn == 0 ? True : (kkn == 1 ? kk.sub(0) : B.conj(kk)));
            } else
                d = c;

            if (E != null) {
                if (d instanceof Neg) {
                    var du = d.unneg();

                    if (du.CONJ()) {
                        if (du.dt() == DTERNAL || du.dt() == XTERNAL) {
                            final var du0 = du;
                            for (var ee : E)
                                du = CondDiff.diffAll(du, ee);
                            return du == du0 ? d : du.neg();
                        }

                    }
                } else {
                    if (E.contains(d))
                        return True;
                }
            }
            return d;
        });
        if (singles != null) {
            e.removeIf(ee -> {
                if (singles.containsKey(ee)) {
                    s.replaceAll(ss ->
                            ss == ee ? ee : B.conj(ss, ee));
                    return true;
                }
                return false;
            });
        }
    }

    private static boolean terminates(Term z) {
        return z == False || z == Null;
    }

    private boolean addParallel(Term x) {

        assert (terminal == null);

        if (x == True)
            return true;
        else if (terminates(x)) {
            terminate(x);
            return false;
        } else
            return result(x instanceof Neg ? addParallelN(x) : addParallelP(x));
    }

    private boolean addParallelNeg(Term x) {
        return x instanceof Neg ? addParallelP(x.unneg()) : _addParallelN(x);
    }

    private boolean addParallelP(Term p) {
        assert (!(p instanceof Neg));

        if (!neg.isEmpty()) {
            p = reducePN(p, neg);
            if (p instanceof Neg) return addParallelN(p);
            if (p == False) return false;
            if (p == True) return true;
            if (p == Null) return false;
        }

        posAdd(p);
        return true;
    }

    private boolean addParallelN(Term _n) {
        assert (_n instanceof Neg);
        var n = _n.unneg();

        return _addParallelN(n);
    }

    private boolean _addParallelN(Term n) {
        n = reducePN(n, pos);

        if (!(n instanceof Bool) && !(n instanceof Neg))
            n = reduceNN(n, true);

        if (n instanceof Neg)
            return addParallelP(n.unneg());
        else if (n == False) return true;
        else if (n == True) return true;
        else if (terminates(n)) {
            terminate(n);
            return false;
        } else {
            negAdd(n);
            return true;
        }
    }

    private Term reduceNN(Term x, boolean eternal) {
        if (neg.isEmpty()) return x;

        Lst<Term> toAdd = null;


        var xs = x.struct();
        for (var nyi = neg.iterator(); nyi.hasNext(); ) {
            var yy = nyi.next();
            if (x.equalsNeg(yy))
                return terminate(False);

            if (possibleInterference(xs, yy)) {
                if (x.SEQ() && yy.CONDS()) {
                    try (var xxe = ConjList.conds(x, 0, false, false)) {
                        if (xxe.OR(xxxe -> ((Compound) yy).condOf(xxxe)))
                            return terminate(False);
                    }
                }
                if (x.CONDS()) {
                    var X = (Compound) x;

                    if (X.condOf(yy))
                        return yy;
                    if (X.condOf(yy, -1)) {
                        var x2 = CondDiff.diffAll(X, yy.neg());
                        if (x2 == True)
                            return terminate(False);
                        x = x2;
                        if (!(x instanceof Compound xc)) break;
                        X = xc;
                    }


                    if (innerDisj(X, yy)) {
                        x = condTransform(X, new ConjEliminator(yy, false));
                        continue;
                    }

                }
            }

            if (yy.CONDS() && possibleInterference(yy, x)) {
                var YY = (Compound) yy;
                if (eternal && YY.dt() != XTERNAL && YY.condOf(x, -1)) {
                    nyi.remove();
                    var z = CondDiff.diffAll(yy, x.neg());
                    if (z == True) {

                    } else if (terminates(z)) {
                        throw new TermTransformException("reduceNN fault: Conj.diffAll(" + yy + "," + x.neg() + ")", yy, z);
                    } else {
                        if (toAdd == null) toAdd = new Lst<>(1);
                        toAdd.add(z);
                    }
                }
            }


        }
        return toAdd != null && !toAdd.AND(this::addParallelNeg) ? False : x;
    }

    private Term reducePN(Term x, Collection<Term> y) {
        return reducePN(x, y, false);
    }

    private Term reducePN(Term x, Collection<Term> y, boolean elimMode) {
        if (y.isEmpty()) return x;
        if (y.contains(x)) return terminate(False);
        if (!(x instanceof Compound X)) return x;

        int xs = X.struct();
        for (Term yy : y) {
            if (!possibleInterference(xs, yy)) continue;
            x = applyPNReduction(X, yy, elimMode);
            if (!(x instanceof Compound)) break;
            X = (Compound) x;
        }
        return x;
    }

    private Term applyPNReduction(Compound X, Term yy, boolean elimMode) {
        if (yy instanceof Compound yc && yc.condOf(X)) return terminate(False);
        if (X.condOf(yy, +1)) return handleCondDiffElimMode(X, yy, elimMode);
        if (X.condOf(yy, -1)) return yy.neg();
        if (innerDisj(X, yy)) return condTransform(X, new ConjEliminator(yy, true));
        return X;
    }

    private Term handleCondDiffElimMode(Compound X, Term yy, boolean elimMode) {
        if (elimMode)
            return terminate(False);
        else {
            var x2 = CondDiff.diffAll(X, yy);
            return x2 == True ? terminate(False) : x2;
        }
    }

    private Term terminate(Term t) {
        var x = terminal;
        if (t == Null) {
            x = terminal = Null;
        } else if (t == False) {
            if (x != Null)
                x = terminal = False;
        } else
            throw new WTF();
        return x;
    }

    @Override
    public final boolean addEvent(long at, Term x) {
        if (terminal != null)
            return false;
        if (at == ETERNAL) {
            if (x.unneg().SEQ())
                throw new TermException("ConjBuilder Seq @ Eternal is Unsupported");

            return addParallel(x);
        } else {
            return addAt(at, x);
        }
    }


    private boolean addAt(long at, Term x) {
        if (at == ETERNAL || at == TIMELESS || terminal != null)
            throw new UnsupportedOperationException();
        if (NAL.DEBUG && (at == DTERNAL || at == XTERNAL))
            throw new UnsupportedOperationException("probably leak");

        if (!(x instanceof Neg)) {
            x = reducePN(x, neg, true);

        } else {
            var _xu = x.unneg();


            var xu = reducePN(_xu, pos, false);
            if (xu != _xu) {
                if (xu == False) {
                    terminate(False);
                    return false;
                }
                x = xu.neg();
            }

            if (x instanceof Neg && !(xu instanceof Bool)) {
                var xu2 = reduceNN(xu, false);
                if (xu2 != xu) x = xu2.neg();
            }
        }

        if (terminal != null) return false;
        if (x == True) return true;
        if (terminates(x)) {
            terminate(x);
            return false;
        }

        return _addAt(at, x);
    }

    private boolean _addAt(long at, Term x) {
        if (seq == null) seq = newSeq();
        var seq = this.seq.getIfAbsentPut(occToDT(at), ConjTree::new);

        var tsBefore = seq.size();
        var ok = seq.addParallel(x);
        if (ok && seq.size() > tsBefore)
            shift = TIMELESS;

        if (!ok && seq.terminal != null) {
            terminate(seq.terminal);
            return false;
        }

        return result(ok);
    }

    private TimeMap<ConjTree> newSeq() {
        return new TimeMap<>();
    }

//    private static @NotNull IntObjectHashMap<Object> newSeq() {
//        return new IntObjectHashMap<>(1);
//    }

    @Deprecated
    private boolean result(boolean result) {
        if (terminal != null)
            return false;
        else if (!result) {
            terminal = False;
            return false;
        } else
            return true;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    private void posAdd(Term t) {
        pos = _add(pos, t);
    }

    private void negAdd(Term t) {
        neg = _add(neg, t);
    }

    private boolean posRemove(Term t) {
        assert (!(t instanceof Neg));
        final var s = this.pos;
        if (s != EMPTY_SET && s.remove(t)) {
            if (s.isEmpty()) this.pos = EMPTY_SET;
            return true;
        }
        return false;
    }

    private boolean negRemove(Term t) {
        assert (!(t instanceof Neg));
        final var s = this.neg;
        if (s != EMPTY_SET && s.remove(t)) {
            if (s.isEmpty()) this.neg = EMPTY_SET;
            return true;
        }
        return false;
    }

    @Override
    public int eventOccurrences() {
        return ((!pos.isEmpty() || !neg.isEmpty()) ? 1 : 0) + (seq != null ? seq.size() : 0);
    }

    @Override
    public int eventCount(long when) {
        if (when == ETERNAL)
            return (pos.isEmpty() ? 0 : pos.size()) + (neg.isEmpty() ? 0 : neg.size());
        else {
            if (seq != null) {
                var s = seq.get(occToDT(when));
                if (s != null)
                    return s.size();
            }
            return 0;
        }
    }

    @Override
    public long shift() {
        if (shift == TIMELESS)
            shift = (seq == null || seq.isEmpty()) ? (!pos.isEmpty() || !neg.isEmpty() ? ETERNAL : TIMELESS) : seq.min();
        return shift;
    }

    @Override
    public boolean removeAll(Term term) {
        var removed = removeParallel(term);
        return seq != null && !removed ? seq.countWith(ConjTree::removeAll, term) > 0 : removed;
    }

    private boolean removeParallel(Term term) {
        if (terminal != null)
            throw new UnsupportedOperationException();

        return term instanceof Neg ? negRemove(term.unneg()) : posRemove(term);
    }

    @Override
    public void negateConds() {
        throw new TODO();
    }

    private int size() {
        return pos.size() +
                neg.size() +
                (seq != null ? (int) seq.sumOfInt(ConjTree::size) : 0);
    }

    @Override
    public Term term(TermBuilder B) {
        if (terminal != null)
            return terminal;

        return seq != null && !seq.isEmpty() ? termSeq(B) : termPar(B);
    }

    /**
     * sequence component of the result
     */
    private Term termSeq(TermBuilder B) {
        var s = _termSeq(B);
        if (s == null) s = True;

        shift(); //cache the shift before clearing seq
        seq = null;

        return terminal != null ? terminal : s;
    }

    @Override
    public void clear() {
        if (pos != EMPTY_SET) {
            pos.clear();
            pos = EMPTY_SET;
        }
        if (neg != EMPTY_SET) {
            neg.clear();
            neg = EMPTY_SET;
        }
        if (seq != null) {
            seq.forEachValue(ConjTree::clear);
            seq.clear();
            seq = null;
        }
        terminal = null;
        shift = TIMELESS;
    }

    /**
     * sequence component of the result
     */
    private Term termPar(TermBuilder B) {
        int pp = pos.size(), nn = neg.size();
        if (pp + nn == 0)
            return True;
        else if (pp == 1 && nn == 0)
            return first(pos);
        else if (nn == 1 && pp == 0)
            return first(neg).neg();
        else {
            var PN = new TmpTermList(pp + nn);
            if (pp > 0) PN.addAll(pos);
            if (nn > 0) PN.addAllNeg(neg);
            return ConjPar.parallelSimple(PN, B);
        }
    }

    private @Nullable Term _termSeq(TermBuilder B) {
        var s = seq.size() == 1 ? termSeq1(B) : termSeqN(B);

        if (terminates(s))
            return terminate(s);
        else if (pos.isEmpty() && neg.isEmpty())
            return s == null ? True : s;
        else
            return  (s == null || s == True) || addParallel(s) ? termPar(B) : terminal;
    }

    private Term termSeqN(TermBuilder B) {
        var factor = NAL.term.CONJ_FACTOR && (seq.size() != 2 || !seq.getFirst().term().equals(seq.getLast().term()));
        var e = drainEte(factor);
        var n = seq.size();
        ObjIntHashMap<Term> counts = null, singles = null;
        var s = new ConjList(n);
        var y = new TermList();

        for (var wc : seq.keyValuesView()) {
            var wct = wc.getTwo();
            assert (wct.seq == null);
            wct.drainEte(y);
            wct.close();


            Term z;
            if (factor) {
                if (y.size() == 1) {
                    z = y.sub(0);
                    if (z instanceof LightCompound)
                        throw new UnsupportedOperationException("TODO conflict with LightCompound use");
                    if (singles == null) singles = new ObjIntHashMap<>(n);
                    singles.increment(z);
                    if (counts == null) counts = new ObjIntHashMap<>(n);
                    counts.increment(z);
                } else {
                    for (var yy : y) {
                        if (counts == null) counts = new ObjIntHashMap<>(n);
                        counts.increment(yy);
                    }
                    z = new LightCompound(SETe, new TermList(y));
                }
            } else {
                if (e != null) y.addAll(e);
                z = B.conj(y);
            }
            if (terminates(z))
                return terminate(z);

            if (z != True)
                s.addDirect(wc.getOne(), z);
            y.clear();
        }

        if (factor) {
            _factor(B, e, n, counts, singles, s);

            if (s.containsInstance(Null)) return terminate(Null);
            if (s.containsInstance(False)) return terminate(False);
            s.removeInstances(True);
        } else {
            if (e != null)
                e.clear();
        }

        if (!s.isEmpty()) {
            var S = s.seq(B);
            if (terminates(S)) return terminate(S);
            if (e == null || e.isEmpty())
                return S;
            e.add(S);
        }

        var ee = e.toArray(EmptyTermArray);
        var SS = ee.length > 1 ? B.compound(CONJ, ee) : ee[0];

        return terminates(SS) ? terminate(SS) : SS;
    }

    private @Nullable Collection<Term> drainEte(boolean factor) {
        Collection<Term> e = factor ? new UnifiedSet<>() : new Lst<>();
        drainEte(e);
        if (!factor && e.isEmpty()) e = null;
        return e;
    }


    private void drainEte(Collection<Term> each) {
        boolean pp = !pos.isEmpty(), nn = !neg.isEmpty();

        var neg = this.neg;
        this.neg = EMPTY_SET;
        var pos = this.pos;
        this.pos = EMPTY_SET;

        if (each instanceof Lst eachLst)
            eachLst.ensureCapacity(pos.size() + neg.size());

        if (nn) addNegs(neg, each);
        if (pp) each.addAll(pos);
    }

    @Nullable
    private Term termSeq1(TermBuilder B) {
        var skv = seq;
        var only = skv.getOnly();

        var w = only.getOne();
        shift = w;//skv.minBy(IntObjectPair::getOne).getOne();

        //flatten point?
        var x = only.getTwo();
        return x.seq != null ? x.term(B) : addAllAt(w, x, B) ? null : terminal;
    }


    private boolean addAllAt(int at, ConjTree x, TermBuilder B) {
        if (x.seq != null) {
            return addAt(at, x.term(B));
        } else {
            if (!x.pos.isEmpty()) {
                for (var p : x.pos) {
                    if (!addParallel(p))
                        return false;
                }
            }
            if (!x.neg.isEmpty()) {
                for (var n : x.neg)
                    if (!addParallelNeg(n))
                        return false;
                return true;
            }
        }

        return true;
    }

    @Override
    public LongIterator eventOccIterator() {
        throw new TODO();
    }

    @Override
    public void close() {
        clear();
    }

    Term parallelComplex(TmpTermList x, int dt, TermBuilder B) {

        var sdt = dt == DTERNAL ? ETERNAL : 0;

        if (!this.takeSimple(x, sdt))
            return this.terminal;

        var n = x.size();
        if (n == 1 && isEmpty())
            return x.getFirst();
        else
            return n > 0 && !takeComplex(x, sdt) ?
                    this.terminal : term(B);
    }

    private boolean takeSimple(TmpTermList t, long when) {
        for (var tt = t.iterator(); tt.hasNext(); ) {
            var x = tt.next();
            if (!x.unneg().CONDS()) {
                if (!add(when, x))
                    return false;
                tt.remove();
            }
        }
        return true;
    }

    private boolean takeComplex(TmpTermList t, long when) {
        var n = t.size();
        if (when == ETERNAL) {
            var seqCount = t.count(x -> x.unneg().SEQ());
            if (seqCount == 1)
                when = 0; //time frame relative to the only sequence
            else if (seqCount > 1)
                return false;
        }

        for (var x : t) {
            if (!add(when, x))
                return false;
        }

        return true;
    }

    private record ConjEliminator(Term yy, boolean polarity) implements UnaryOperator<Term> {

        @Override
        public Term apply(Term xx) {
            if (xx instanceof Neg) {
                var xu = xx.unneg();
                if (xu instanceof Compound xuc && xu.CONJ()) {
                    if (xuc.condOf(yy, polarity ? -1 : +1))
                        return True;
                    var xu2 = CondDiff.diffAll(xuc, yy.negIf(!polarity));
                    if (xu2 != xuc)
                        return xu2.neg();
                }
            }
            return xx;
        }
    }
}