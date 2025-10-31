package nars.time;

import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.edge.LazyMutableDirectedEdge;
import jcog.data.graph.path.FromTo;
import jcog.data.graph.search.Search;
import jcog.data.iterator.CartesianIterator;
import jcog.data.iterator.Concaterator;
import jcog.data.list.Lst;
import jcog.data.map.UnifriedMap;
import jcog.data.set.ArrayHashSet;
import jcog.data.set.ArrayUnenforcedSet;
import jcog.random.RandomBits;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Termlike;
import nars.term.builder.TermBuilder;
import nars.term.util.conj.*;
import nars.term.var.CommonVariable;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.procedure.primitive.LongObjectProcedure;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import static java.lang.Math.min;
import static java.util.Collections.EMPTY_MAP;
import static java.util.Collections.EMPTY_SET;
import static jcog.Util.emptyIterable;
import static jcog.Util.hashCombine;
import static nars.Op.*;
import static nars.time.TimeSpan.TS_ZERO;
import static org.eclipse.collections.impl.tuple.Tuples.pair;


/**
 * represents a multigraph of events and their relationships
 * calculates unknown times by choosing from the possible
 * pathfinding results.
 * <p>
 * it can be used in various contexts:
 * a) the tasks involved in a derivation
 * b) as a general purpose temporal index, ie. as a meta-layer
 * attached to one or more concept belief tables
 * <p>
 * DTERNAL relationships can be maintained separate
 * from +0.
 * <p>
 * TODO
 * subclass of MapNodeGraph which allows tagging edges by an ID.  then index
 * the edges by these keys so they can be efficiently iterated and removed by tag.
 * <p>
 * then use it to store the edges in categories:
 * task time edges
 * belief time edges
 * conclusion time edges
 * hypothesized (anything created during search)
 * etc.
 * <p>
 * then the premise can clear the conclusion edges while retaining task, belief edges throughout
 * each separate derivation of a premise.
 * also the tags can be used to heuristically bias the search via associated weight.
 */
public abstract class TimeGraph {

    private static final int ABSOLUTE_PAIR_SOURCE_TRY = 2;
    private static final int SubXternal_SUBSOLUTIONS_TRY = 1;
    private static final int RECURSION_MAX = 3;

    private static final long SAFETY_PAD = 32 * 1024;

    private static final IntFunction<Event> EventPri = x -> x instanceof Absolute ? -1 : +1;

    /** lower # is processed earlier */
    private static final IntFunction<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>> EdgePri = x -> {
        var e = x.id();
        var score = 0;
        if (x.from().id instanceof Absolute || x.to().id instanceof Absolute)
            score--;

        if (isSelfNeg(x))
            score += 2;  //move to last

        return score;
    };

    private static boolean isSelfNeg(FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan> x) {
        return x.id().dt()==0 && x.from().id.id.equalsNeg(x.to().id.id);
    }

    public final ArrayHashSet<Event> solutions = new ArrayHashSet<>(0);

    public final RandomGenerator rng;
    protected final Map<Term, Set<Event>> events = new UnifriedMap<>(0);
    final MapNodeGraph<Event,TimeSpan> graph;

    /**
     * current solution term template
     */
    public transient Term target;

    /**
     * whether to solve occ, or settle for non-xternal only
     */
    private transient boolean solveOcc = true;

    /**
     * current recursion level
     */
    private transient int depth;

    protected TimeGraph(Random rng) {
        this(new RandomBits(rng));
    }

    private static final int initialNodesCapacity = 16;

    protected TimeGraph(RandomGenerator rng) {
        graph = new MapNodeGraph<>(initialNodesCapacity);
        this.rng = rng;
    }

    private static boolean termsEvent(Term e) {
        return e != null && e.CONDABLE();
    }

//    private static Compound _inh(Term subj, Term pred) {
//        return CachedCompound.the(INH, new BiSubterm(subj, pred));
//    }

    private static Term conj(Compound x, int dt) {
        return conj(x, dt, terms);
    }

    private static Term conj(Compound x, int dt, TermBuilder B) {
        var xx = x.subtermsDirect();
        Term xEarly, xLate;
        if (x.dt() == XTERNAL) {
            //use the provided 'path' and 'dir'ection, if non-null, to correctly order the sequence, which may be length>2 subterms
            xEarly = xx.sub(0);
            xLate = xx.sub(1);
        } else {

            var early = Cond.condEarlyLate(x, true);
            if (early == 1)
                dt = -dt;

            xEarly = xx.sub(early);
            xLate = xx.sub(1 - early);
        }

        return ConjSeq.conj(xEarly, dt, xLate, false, B);
    }

    private static long shift(long w, int dur, boolean fwd) {
        return w != ETERNAL && dur != XTERNAL ? w + (fwd ? +dur : -dur) : w;
    }

    public final Event know(Term v) {
        return know(v, false);
    }

    public final void knowWeak(Term v) {
        know(v, true);
    }

    /** @param async if existing results, null is returned regardless (elides some work) */
    @Nullable private Event know(Term t, boolean async) {
        var existing = events(t);
        if (existing != null) {
            var s = existing.size();
            if (s > 0) {
                if (async)
                    return null; //already have something and don't need a result

                return switch (Util.count(x1 -> x1 instanceof Absolute, existing)) {
                    case 1 -> Util.first(x -> x instanceof Absolute, existing);
                    case 0 -> existing.iterator().next(); //first rel
                    default -> shuffle(existing, false).iterator().next();
                };
            }
        }
        return knowRel(t);
    }

    private Event knowRel(Term t) {
        return event(new Relative(t), true);
    }

    public final Event know(Term t, long start) {
        return know(t, start, start);
    }

    public final Event know(Term t, long start, long end) {
        return event(t, start, end, true);
    }

    private Event event(Term t, long start, long end, boolean add) {
        return event(start == TIMELESS ? new Relative(t) : absolute(t, start, end), add);
    }

    private Event event(Event e, boolean add) {
        if (!add)
            return e;

        var added = graph.addNode(e).id;
        if (added == e) {
            if (events.computeIfAbsent(added.id, _e -> new ArrayUnenforcedSet<>(Event.EmptyEventArray)).add(e)) {
                //if (decompose)
                if (add)
                    decompose(e);
            }
        }
        return added;
    }

    protected Event absolute(Term t, long when) {
        return new Absolute(t, t(when));
    }

    protected Event absolute(Term t, long start, long end) {
        start = t(start);
        end = t(end);
        return (end == start) ? new Absolute(t, start) : new AbsoluteRange(t, start, end);
    }

    @Nullable private Set<Event> events(Term t) {
        return events.get(t);
    }

    private void link(Event before, TimeSpan e, Event after) {
        var x = graph.addNode(before);
        var y = before.equals(after) ? x : graph.addNode(after);
        graph.addEdgeByNode(x, e, y);
    }

    private void link(Event x, long dt, Event y) {
        if (x instanceof Absolute X && y instanceof Absolute Y) {
            long xs = X.start(), ys = Y.start();
            if (dt != ys - xs)
                return;
        }
        var vc = x.compareTo(y);
        if (vc == 0) { //equal?
            if (parallel(dt))
                return;
            y = x; //use same instance, they could differ
            if (dt < 0)
                dt = -dt; //use only positive dt values for self loops
        } else {
            if (vc > 0) {
                if (!parallel(dt))
                    dt = -dt;

                var z = x;
                x = y;
                y = z;
            }
        }

        link(x, TimeSpan.the(dt), y);
    }

    private static boolean parallel(long dt) {
        return dt == 0 || dt == ETERNAL || dt == TIMELESS;
    }

    private void decompose(Event e) {
        var x = e.id;
        switch (x) {
            case CommonVariable cv -> decomposeCommonVar(e, cv);
            case Compound c -> decomposeCompound(e, c);
            case null, default -> { }
        }
    }

    private void decomposeCompound(Event e, Compound x) {
        switch (x.op()) {
            case NEG  -> decomposeNeg(e);
            case INH  -> decomposeInh(e);
            case CONJ -> decomposeConj(e);
            case IMPL -> decomposeImpl(x);
            default   -> decomposeMisc(x);
        }
    }

    private void decomposeCommonVar(Event e, CommonVariable c) {
        //TODO dynamic
        for (var v : c.common())
            link(e, v); //equivalence
    }

    private void decomposeMisc(Term x) {
        x.ANDrecurse(Termlike::TEMPORALABLE, s -> {
            if (s.TEMPORALABLE())
                know(s);
        });
    }

    private void decomposeNeg(Event X) {
        link(X, X.id.unneg());
    }

    private void decomposeInh(Event X) {
        if (NAL.term.INH_BUNDLE) {
            var x = X.id;
            if (ConjBundle.bundled(x))
                ConjBundle.events(x, s -> link(X, s));
        }
    }

    private void decomposeImpl(Term x) {
        Term subj = x.sub(0), pred = x.sub(1);
        Event se = knowRel(subj), pe = knowRel(pred);
        var edt = x.dt();
        if (edt != XTERNAL)
            link(se, (edt == DTERNAL ? 0 : edt) + subj.seqDur(), pe);
    }

    private void decomposeConj(Event e) {
        var c = (Compound) e.id;
        var dt = c.dt();
        if (dt == XTERNAL)
            decomposeConjXternal(c);
        else {
            var cSeq = c.SEQ();
            var start = e.start();
            if (start == TIMELESS || start == ETERNAL) {
                if (!cSeq && dt == DTERNAL)
                    decomposeConjParRel(e, c);
                else
                    decomposeConjSeqRel(e, c, cSeq);
            } else
                decomposeConjAbs(e, c, start, cSeq);
        }
    }

    private void decomposeConjXternal(Compound c) {
        c.forEach(this::knowRel); //floating, with some potential temporal information
    }

    private void decomposeConjAbs(Event e, Compound c, long start, boolean cSeq) {
        c.conds(new SequenceAbsolute(e.dur()), start, !cSeq, false, false);
    }

    private void decomposeConjSeqRel(Event e, Compound c, boolean cSeq) {
        c.conds(new SequenceRelChain(e)/*new RootChainer(X)*/,
                0, !cSeq, false, false);
    }

    private void decomposeConjParRel(Event e, Compound c) {
        c.subtermsDirect().forEach(cc -> link(e, cc));
    }

    protected final void link(Event a, Event b) {
        link(a, 0, b);
    }

    public final void link(Event a, Term b) {
        link(a, knowRel(b));
    }

    private boolean solveXternal(Compound x, Predicate<Event> each) {
        var xx = x.subtermsDirect();
        var s = xx.subs();

        if (x.CONJ()) {
            List<Absolute>[] subEvents = new Lst[s];
            if (solveAbsolutes(xx, subEvents) >= 1) {
                if (!solveConjPermuteAbsolutes(xx, subEvents, each))
                    return false;
            }
        }

        if (s == 2)
            return solveDT_2(x, each);

        return true;
    }

    private boolean solveDT_2(Compound x, Predicate<Event> each) {
        var xx = x.subtermsDirect();
        Term a = xx.sub(0), b = xx.sub(1);
        return (!a.equals(b) || solveDTrepeatRelative(x, a, each))
                && solveDTpair(x, a, b, each)
                && solveDTAbsolutePair(x, a, b, each);

    }

    /** (X &&+- X) or (X ==>+- X)  */
    @Deprecated private boolean solveDTrepeatRelative(Compound x, Term a, Predicate<Event> each) {
        var ee = new Lst<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>>();
        graph.edges(z -> {
            if (z.id().dt() == 0) return;

            var F = id(z.from());
            if (!eventMatch(F, a)) return;

            var T = id(z.to());
            if (T == F || eventMatch(T, a))
                ee.add(z);
        });
        for (int i = 0, size = ee.size(); i < size; i++) {
            var dt = occToDT(ee.get(i).id().dt());
//            if (dt == XTERNAL) throw new WTF(); //HACK shouldnt happen
            if (rng.nextBoolean()) dt = -dt; //reverse order
            if (!acceptRel(impl(x, +dt), each))
                return false;
            if (!acceptRel(impl(x, -dt), each))
                return false;
        }
        return true; //continue
    }

    private static Term id(MapNodeGraph.AbstractNode<Event, TimeSpan> zf) {
        return zf.id.id;
    }

    private static boolean eventMatch(Term c, Term e) {
        return c.equals(e) ||
                (c instanceof Compound cc && cc.seqDur(true) == 0 && cc.condOf(e));

    }

    private int solveAbsolutes(Subterms xx, List<Absolute>[] subEvents) {
        var abs = 0;
        var s = subEvents.length;
        var f = new Lst<Absolute>(s);
        Predicate<Absolute> adder = f::add;

        for (var i = 0; i < s; i++) {
            solveExact(xx.sub(i), adder);

            Lst<Absolute> ff;
            var fs = f.size();
            if (fs > 0) {
                ff = f.clone(); //TODO keep Absolute[], not List<Absolute>
                f.clear();
                if (fs > 1)
                    ff.shuffleThis(rng);
                abs++;
            } else
                ff = null;

            subEvents[i] = ff;
        }
        return abs;
    }

    public void clear() {
        graph.clear();
        solutions.clear();
        events.clear();
        target = null;
    }

    private boolean solveConjPermuteAbsolutes(Subterms x, List<Absolute>[] subEvents, Predicate<Event> each) {
        var ci = new CartesianIterator<>(Event[]::new, subEvents) {
            @Override
            protected Iterable<? extends Event> iterableNull(int i) {
                return new Lst<>(new Event[]{new Relative(x.sub(i))});
            }
        };

        var n = x.subs();

        ConjBuilder cc = null;
        TermList unknowns = null;

        nextPermute:
        while (ci.hasNext()) {

            var ss = ci.next();

            if (cc != null) cc.clear();

            if (unknowns != null) unknowns.clear();

            long dur = Long.MAX_VALUE, start = Long.MAX_VALUE;
            for (var i = 0; i < n; i++) {
                var e = ss[i];

                if (!(e instanceof Absolute)) {
                    if (n == 2)
                        return true; //>=1 unknown, <=1 known; nothing to be gained from continuing

                    if (unknowns == null) unknowns = new TermList(1);
                    unknowns.addIfNotPresent(e.id);
                    continue;
                }

                dur = min(e.dur(), dur);

                var es = e.start();
                if (start == TIMELESS || start == ETERNAL)
                    start = es;
                else if (es != ETERNAL)
                    start = min(start, es); //override with specific temporal

                if (cc == null) cc = new ConjList(n);
                if (!cc.add(es, e.id))
                    continue nextPermute;
            }

            if (cc == null)
                continue;

            var y = cc.term();

            if (!termsEvent(y) || !y.CONJ())
                continue;

            if (unknowns != null) {
                y = CONJ.the(XTERNAL, y, unknowns.size() > 1 ? CONJ.the(XTERNAL, (Subterms) unknowns) : unknowns.sub(0));
                if (!termsEvent(y))
                    continue;
            }


            if (y.TEMPORAL_VAR()) {
                if (!solveAll(y, each))
                    return false;
            } else {
                var S = start;
                if (S != TIMELESS && solveOcc) {
                    var E = start != TIMELESS && start != ETERNAL && dur != XTERNAL ? start + dur : start;
                    if (!each.test(shadow(y, S, E)))
                        return false;
                } else {
                    if (!solveDirect(y, each))
                        return false;
                }
            }
        }

        return true;
    }

    private boolean solveDTpair(Compound x, Term a, Term b, Predicate<Event> each) {
        var aa = events(a);
        if (aa != null && !graph./*dfs*/bfs(shuffle(aa, false),
                new DTPairSolver(a, b, x, each)))
            return false;

        //TODO reverse direction?
        //        if (!graph./*dfs*/bfs(shuffle(ab, false),
        //                new DTPairSolver(b, a, x, each)))
        //            return false;
        return true;
    }

    private boolean solveDTAbsolutePair(Compound x, Term a, Term b, Predicate<Event> each) {
        var ae = occurrences(a, ABSOLUTE_PAIR_SOURCE_TRY);
        if (ae == null)
            return true;
        var aa = eventArray(ae);
        return a.equals(b) ?
            solveDTAbsolutePairEqual(x, each, aa) :
            solveDTAbsolutePairNotEqual(x, b, each, ae, aa);
    }

    private boolean solveDTAbsolutePairNotEqual(Compound x, Term y, Predicate<Event> each, UnifiedSet<Event> absolutes, Event[] aa) {
        absolutes.clear();
        return solveOcc(y, be -> {
            if (be instanceof Absolute && absolutes.add(be)) {
                for (var ax : aa) {
                    if (ax != be && !solveDTAbsolutePair(x, ax, be, each))
                        return false;
                }
            }
            return true;
        });
    }

    private boolean solveDTAbsolutePairEqual(Compound x, Predicate<Event> each, Event[] aa) {
        if (aa.length <= 1)
            return true;

        //IMPL must be tried both directions since it isnt commutive
        var bidi = switch (x.op()) {
            case IMPL -> true;
            case CONJ -> false;
            default -> throw new IllegalArgumentException(); //??
        };

        var n = aa.length;
        for (var i = 0; i < n; i++) {
            var ii = aa[i];
            for (var j = bidi ? 0 : i + 1; j < n; j++)
                if (i != j && !solveDTAbsolutePair(x, ii, aa[j], each))
                    return false;
        }
        return true;
    }

    /**
     * samples occurrences
     */
    private UnifiedSet<Event> occurrences(Term a, int limit) {
        var ae = new UnifiedSet<Event>(0);

        solveOcc(a, ax -> !(ax instanceof Absolute) || !ae.add(ax) || ae.size() < limit);

        return ae.isEmpty() ? null : ae;
    }

    private Event[] eventArray(UnifiedSet<Event> ae) {
        var aa = ae.toArray(Event.EmptyEventArray);
        if (aa.length > 1) ArrayUtil.shuffle(aa, rng);
        return aa;
    }

    private boolean solveDTAbsolutePair(Compound x, Event a, Event b, Predicate<Event> each) {

        var conj = x.CONJ();
        if (conj) {
            //swap to correct sequence order
            if (a.start() > b.start()) {
                var z = a;
                a = b;
                b = z;
            }
        }
//        assert (!a.equals(b));

        long aWhen = a.start(), bWhen = b.start(), when = TIMELESS;
        int dt;
        if (aWhen == ETERNAL || bWhen == ETERNAL) {
            dt = 0;
            when = aWhen == ETERNAL ? bWhen : aWhen;
        } else {
            if (conj) {
                assert (aWhen != TIMELESS && bWhen != TIMELESS);
                dt = occToDT(bWhen - aWhen);
                when = aWhen;
            } else {
                dt = occToDT(bWhen - aWhen - a.id.seqDur());
            }
        }


        var dur = min(a.dur(), b.dur());

        //for impl and other types cant assume occurrence corresponds with subject
        return conj ?
                solveOcc(ConjSeq.conj(a.id, dt, b.id, false, terms), when, dur, each) :
                solveDT(x, TIMELESS, dt, dur, each);
    }

    /**
     * TODO make this for impl only because the ordering of terms is known implicitly from 'x' unlike CONJ
     */
    private boolean solveDT(Compound x, long start, int dt, int dur, Predicate<Event> each) {

        assert (dt != XTERNAL && dt != DTERNAL);

        dt = tExternal(dt);
        start = tExternal(start);
        dur = tExternal(dur);

        var y = switch (x.op()) {
            case IMPL -> impl(x, dt);
            case CONJ -> conj(x, dt);
            default -> throw new UnsupportedOperationException();
        };

        return solveOcc ?
            solveOcc(y, start, dur, each) :
            acceptRel(y, each);
    }

    /** detect collapse in a constructed impl */
    @Nullable private static Term impl(Compound x, int dt) {
        var y = x.dt(dt);
        if (!y.IMPL() || y.sub(0).opID()!=x.sub(0).opID() || y.sub(1).opID()!=x.sub(1).opID())
            return null; //collapse or structural change
        else
            return y;
    }

    protected long tExternal(long t) {
        return t;
    }

    protected int tExternal(int dt) {
        return dt;
    }

    private static boolean acceptRel(@Nullable Term y, Predicate<Event> each) {
        return y == null || !termsEvent(y) || each.test(new Relative(y));
    }

    private boolean solveOcc(@Nullable Term y, long start, int dur, Predicate<Event> each) {
        if (!termsEvent(y))
            return true; //keep trying

        return start == TIMELESS ?
            solveOcc(y, each) :
            each.test(shadow(y, start,
                    (start != ETERNAL && dur != XTERNAL) ? start + dur : start)
            );
    }

    /**
     * dt computation for term construction
     */
    protected int occToDT(long x) {
        //if (x == TIMELESS) x = XTERNAL;
        assert (x != TIMELESS);
        return x == ETERNAL ? DTERNAL : Tense.occToDT(x);
    }

    /**
     * internal time representation, override to filter occ (ex: dither)
     */
    public long t(long when) {
        return when;
    }

    /**
     * returns false to terminate solution process.  true doesnt necessarily mean it was accepted, and vice-versa
     */
    private boolean trySolution(Event x) {
        return !solutionValid(x) || !addSolution(x) || solution(x);
    }

    private boolean addSolution(Event x) {
        if (x instanceof Relative) {
            //HACK avoid adding a less specific version of one that is present.
            var xid = x.id;
            var list = solutions.list;
            for (int i = 0, size = list.size(); i < size; i++) {
                var y = list.get(i);
                if (y instanceof Absolute && y.id.equals(xid))
                    return false;
            }
        }
        return solutions.add(x);
    }

    /**
     * @param s will be a new unique solution to the search
     *          return false to stop search; true to continue
     */
    protected abstract boolean solution(Event s);

    private boolean solveExact(Term x, Predicate<? super Absolute> each) {
        var ee = shuffle(events(x), false);
        if (ee == null) return true;

        for (var e : ee) {
            if (e instanceof Absolute ea)
                if (!each.test(ea)) return false;
        }

        return true;
    }

    /**
     * main entry point to the solver
     *
     * @see callee may need to clear the provided seen if it is being re-used
     */
    public final void solve(Term x, boolean occ) {
        if (!x.CONDABLE())
            throw new UnsupportedOperationException();

        this.solveOcc = occ;
        this.depth = 0;
        this.solutions.clear();

        knowWeak(this.target = x); //absorb assumed patterns contained in the target

        solveAll(x, TimeGraph.this::trySolution);
    }

    protected boolean solutionValid(Event s) {
        return termsEvent(s.id) && (s instanceof Absolute || !target.equals(s.id));
    }

    private boolean solveAll(Term x, Predicate<Event> each) {
        if (++depth > RECURSION_MAX)
            return true;

        try {
            if (x instanceof Compound cx) {
                if (x.subtermsDirect().TEMPORAL_VAR())
                    return solveRecursive(x, each); //inner XTERNAL

                if (x.dt() == XTERNAL)
                    return solveXternalTop(cx, each);
            }

            return solveDirect(x, each);
        } finally {
            depth--;
        }
    }

    /**
     * top-level XTERNAL
     */
    private boolean solveXternalTop(Compound x, Predicate<Event> each) {
        return solveXternal(x, y ->
                y instanceof Absolute || (depth >= RECURSION_MAX) ? each.test(y) : solveDirect(y.id, each));
    }

    private boolean solveDirect(Term x, Predicate<Event> each) {
        return solveOcc ? solveOcc(x, each) : acceptRel(x, each);
    }

    private boolean solveRecursive(Term x, Predicate<Event> each) {
        var s = new SubXternalSolver(x).solutions();
        return switch (s.size()) {
            case 0 -> true;
            case 1 -> solveRecursive1(x, each, s);
            default -> solveRecursiveN(x, each, s);
        };
    }

    /**
     * TODO use CartesianIterator instead of this random sampling
     */
    private boolean solveRecursiveN(Term x, Predicate<Event> each, Map<Compound, java.util.Set<Term>> subSolved) {
        var ns = subSolved.size();
        Term y = x;
        Pair<Compound, Term[]>[] substs = new Pair[ns];
        var permutations = 1;
        var j = 0;
        for (var entry : subSolved.entrySet()) {
            var ww = ArrayUnenforcedSet.toArrayShared(entry.getValue());
            assert (ww.length > 0);
            var k = entry.getKey();
            if (ww.length == 1) {
                substs[j] = null;
                y = y.replace(k, ww[0]);
                if (!termsEvent(y))
                    return true;
                //save structure of only result
                //knowWeak(ww[0]);
            } else {
                permutations *= ww.length;
                substs[j] = pair(k, ww);
            }
            j++;
        }

        return permutations == 1 ?
            solveAllIfDifferent(x, y, each) :
            solveRecursiveNPermutations(y, each, ns, permutations, substs);
    }

    private boolean solveRecursiveNPermutations(Term x, Predicate<Event> each, int ns, int permutations, Pair<Compound, Term[]>[] substs) {
        Map<Term, Term> m = new UnifriedMap<>(ns);
        while (permutations-- > 0) {
            for (var si : substs) {
                if (si != null) {
                    var ssi = si.getTwo();
                    m.put(si.getOne(), ssi[ssi.length > 1 ? rng.nextInt(ssi.length) : 0]);
                }
            }
            if (!solveAllIfDifferent(x, x.replace(m), each))
                return false;
            m.clear();
        }
        return true;
    }

    private boolean solveRecursive1(Term x, Predicate<Event> each, Map<Compound, Set<Term>> subSolved) {
        var xy = subSolved.entrySet().iterator().next();

        var sy = xy.getValue();
        if (!sy.isEmpty()) {
            var TOs = ArrayUnenforcedSet.toArrayShared(sy);
            if (TOs.length > 1) ArrayUtil.shuffle(TOs, rng);

            var from = xy.getKey();
            for (var to : TOs)
                if (!solveAllIfDifferent(x, x.replace(from, to), each))
                    return false;
        }
        return true;
    }

    private boolean solveAllIfDifferent(Term x, Term y, Predicate<Event> each) {
        return !termsEvent(y) || x.equals(y) || solveAll(y, each);
    }

    private boolean solveOcc(Term x, Predicate<Event> each) {
        Relative R = null;
        var ee = shuffle(events(x), false);
        if (ee != null) {
            for (var eee : ee) {
                if (eee instanceof Relative re)
                    R = re;
                else if (!each.test(eee))
                    return false;
            }
        }

        if (R == null) {
            R = new Relative(x);
            knowWeak(x);
        }

        return solveOcc(R, each);
    }

    private boolean solveOcc(Relative x, Predicate<Event> each) {
        return graph./*dfs*/bfs(x, new OccSolver(each)) &&
                solveSelfLoop(x, each) &&
                each.test(x);
    }

    /**
     * check for any self-loops and propagate forward and/or reverse
     * HACK i forget how this works and what it does
     */
    private boolean solveSelfLoop(Event x, Predicate<Event> each) {

        var N = graph.node(x);
        if (N==null) return true;

        var eei = N.edgeList(false, true,
                e -> e.id().dt() != 0 && e.loop());
        if (eei == emptyIterable)
            return true;

        var t = x.id;
        return eei.isEmpty() || solveExact(t, s -> {
            if (s.start() == ETERNAL) return true; //skip

            //TODO shuffle found self-loops, there could be sevreal

            for (var e : eei) {
                var dt = e.id().dt();
                /*if (dt != 0 && e.loop()) {*/

                if (rng.nextBoolean())
                    dt = -dt; //vary order

                if (!each.test(shift(s, +dt)))
                    return false;
                if (!each.test(shift(s, -dt)))
                    return false;
            }

            return true;
        });

    }

    private Event shift(Absolute e, long l) {
        assert (l != 0 && l != ETERNAL && l != TIMELESS);
        var s = e.start + l;
        var x = e.id;
        return e instanceof AbsoluteRange ?
                absolute(x, s, e.end() + l) :
                absolute(x, s);
    }

    private Iterable<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>> shuffle(Iterable<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>> e) {
        if (e == emptyIterable) return e;

        if (e instanceof Collection ee) {
            switch (ee.size()) {
                case 0 -> {return emptyIterable;}
                case 1 -> {return e;}
            }
        }

        var ee = new Lst<>(e);
        return switch (ee.size()) {
            case 0  -> emptyIterable;
            case 1  -> ee;
            default -> ee.shuffleThis(rng).sortThisByInt(EdgePri);
        };
    }

    @Nullable private Collection<Event> shuffle(@Nullable Collection<Event> e, boolean sortAbsolutesFirst) {
        if (e == null)
            return null;

        var n = e.size();
        if (n > 1) {
            var ee = new Lst<>(e);
            ee.shuffleThis(rng);
            if (sortAbsolutesFirst)
                ee.sortThisByInt(EventPri);
            return ee;
        } else
            return e;
    }

    private Event shadow(Term x, long s, long e) {
        return event(x, s, e, false);
    }

    /**
     * absolutely specified event
     */
    public static non-sealed class Absolute extends Event {
        private final long start;

        private Absolute(Term t, long start, long end) {
            super(t, start, end);

            this.start = start;
        }

        Absolute(Term t, long start) {
            this(t, start, start);
        }

        @Override
        public final long start() {
            return start;
        }

        public long end() {
            return start;
        }


    }

    public static final class AbsoluteRange extends Absolute {
        private final long end;

        AbsoluteRange(Term t, long start, long end) {
            super(t, start, end);
            if (end <= start || start == ETERNAL || end == TIMELESS)
                throw new RuntimeException("invalid AbsoluteRange start/end times: " + start + ".." + end);
            this.end = end;
        }

        public long end() {
            return end;
        }

    }

    /**
     * TODO RelativeRange?
     */
    public static final class Relative extends Event {

        Relative(Term id) {
            super(id, id.hashCode() /*hashCombine(id.hashCode(), TIMELESS)*/);
        }

        @Override
        public final long start() {
            return TIMELESS;
        }

        @Override
        public final long end() {
            return TIMELESS;
        }

    }

    public abstract static sealed class Event implements LongObjectPair<Term> {

        static final Event[] EmptyEventArray = new Event[0];

        private static final Comparator<Event> cmp = Comparator
                .comparingLong(Event::start)
                .thenComparingLong(Event::end)
                .thenComparing(e -> e.id);

        public final Term id;
        private final int hash;

        Event(Term id, int hash) {
            this.id = id;
            this.hash = hash;
        }

        protected Event(Term t, long start, long end) {
            this(t, hashCombine(t.hashCode(), start, end));

            if (/*start == TIMELESS || */(start != ETERNAL && (!(start > ETERNAL + SAFETY_PAD) || start >= TIMELESS - SAFETY_PAD)))
                throw new ArithmeticException();
        }

        public abstract long start();

        public abstract long end();

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) return true;
            var e = (Event) obj;
            return (hash == e.hash) && (start() == e.start()) && (end() == e.end()) && id.equals(e.id);
        }

        @Override
        public final String toString() {
            var s = start();

            if (s == TIMELESS) {
                return id.toString();
            } else if (s == ETERNAL) {
                return id + "@ETE";
            } else {
                var e = end();
                return e == s ? id + "@" + s : id + "@" + s + ".." + e;
            }
        }

        @Override
        public long getOne() {
            return start();
        }

        @Override
        public final Term getTwo() {
            return id;
        }

        @Override
        public int compareTo(LongObjectPair<Term> e) {
            return this == e ? 0 : cmp.compare(this, (Event) e);
        }

        final int dur() {
            var s = start();
            return s == ETERNAL || s == TIMELESS ? XTERNAL : Tense.occToDT(end() - s);
        }

    }

    private static class DTPathVisitor implements Consumer<BooleanObjectPair<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>>> {
        long dt;
        int dur = XTERNAL;
        boolean fwd;
        FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan> e;

        private static int shrinkDur(FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan> e, boolean fwd, int dur) {
            return dur <= 0 ? dur : min(dur, (fwd ? e.from() : e.to()).id.dur());
        }

        @Override
        public void accept(BooleanObjectPair<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>> span) {
            fwd = span.getOne();
            e = span.getTwo();

            dt += (fwd ? +1 : -1) * e.id().dt();
            dur = shrinkDur(e, fwd, dur);
        }

        long[] get() {
            dur = shrinkDur(e, !fwd, dur); //terminal event
            return new long[]{dt, dur};
        }
    }

    /**
     * computes dt, the length of time spanned from start to the end of the given path [0],
     * and the range [1]
     */
    static long[] pathDT(List<BooleanObjectPair<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>>> path) {
        var v = new DTPathVisitor();
        path.forEach(v);
        return v.get();
    }

    private abstract class CrossTimeSolver extends Search<Event, TimeSpan> {

        protected abstract boolean go(List<BooleanObjectPair<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>>> path, MapNodeGraph.AbstractNode<Event, TimeSpan> next);

        @Override
        protected Iterable<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>> search(MapNodeGraph.AbstractNode<Event, TimeSpan> x, List<BooleanObjectPair<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>>> path) {
            return shuffle(x.edgeList(true, true, filter(x)));
        }

        private Predicate<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>> filter(MapNodeGraph.AbstractNode<Event, TimeSpan> x) {
            return e -> {
                MapNodeGraph.AbstractNode<Event, TimeSpan> from = e.from(), to = e.to();
                return (to == x && !visited(from))
                        ||
                        (from == x && !visited(to));
            };
        }

        final Iterable<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>> edges(MapNodeGraph.AbstractNode<Event, TimeSpan> from, Term to) {
            return edges(from, events(to));
        }

        Iterable<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>> edges(MapNodeGraph.AbstractNode<Event, TimeSpan> from, Collection<Event> to) {
            Lst<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>> l = null;
            if (to != null) {
                for (var t : to) {
                    var T = graph.node(t);
                    if (T != null && T != from) {
                        if (!visited(T)) {
                            if (l == null) l = new Lst<>(1);
                            l.add(new LazyMutableDirectedEdge<>(from, TS_ZERO, T));
                        }
                    }
                }
            }
            return l == null ? emptyIterable : l;
        }
    }

    private class DTPairSolver extends CrossTimeSolver {

        /**
         * true: paths start from a, false: paths start from b
         */
        private final Term a, b;
        private final Compound x;
        private final Predicate<Event> each;
        private final Predicate<Term> atEq;

        private Predicate<Term> ae, be;

        DTPairSolver(Term a, Term b, Compound x, Predicate<Event> each) {
            this.a = a;
            this.b = b;
            this.x = x;
            this.each = each;

            var eqB = equals(true);
            if (b.CONJ()) {
                //HACK test for && subconditions
                var bdt = b.dt();
                if (bdt ==DTERNAL)
                    eqB = eqB.or(z -> b.subtermsDirect().contains(z));
//                else if (bdt!=XTERNAL)
//                    eqB = eqB.or(z -> ((Compound)b).condFirst(z)); //TODO requires shifting to accept
            }
            this.atEq = eqB;
        }

        protected boolean at(Term x) {
            return atEq.test(x);
        }

        private Predicate<Term> equals(boolean dir) {
            var Y = dir ? be : ae;
            return Y == null ? dir ? (be = b.equals()) : (ae = a.equals()) : Y;
        }

        @Override
        protected boolean go(List<BooleanObjectPair<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>>> path, MapNodeGraph.AbstractNode<Event, TimeSpan> next) {

            if (!at(id(pathEnd(path))))
                return true; //proceed further

            var pt = pathDT(path);

            var dt = pt[0];

            if (dt == ETERNAL)
                dt = 0; //HACK

            if (x.IMPL())
                dt -= a.seqDur();

            var dur = (int) pt[1];

            return solveDT(x, TIMELESS, occToDT(dt), dur, each);
        }
    }

    private class OccSolver extends CrossTimeSolver {

        private final Predicate<Event> each;
        private transient Event start;
        private transient long pathStartTime;

        OccSolver(Predicate<Event> each) {
            this.each = each;
        }

        @Override
        protected Iterable<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>> search(MapNodeGraph.AbstractNode<Event, TimeSpan> x, List<BooleanObjectPair<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>>> path) {
            var y = shuffle(super.search(x, path));
            return x.id instanceof Relative ? (() -> Concaterator.concat(y, shuffle(edges(x, x.id.id)))) : y;
        }

        @Override
        public void clear() {
            super.clear();
            start = null; //invalidate
        }

        @Override
        protected boolean go(List<BooleanObjectPair<FromTo<MapNodeGraph.AbstractNode<Event, TimeSpan>, TimeSpan>>> path, MapNodeGraph.AbstractNode<Event, TimeSpan> n) {
            var end = n.id;
            if (!(end instanceof Absolute))
                return true;

            if (start == null) {
                start = pathStart(path).id;
                pathStartTime = start.start();
            }

            var pathEndTime = end.start();

//            if (pathEndTime == ETERNAL)
//                return true; //?

            if (pathStartTime == TIMELESS && pathEndTime == TIMELESS)
                return true; //no absolute occurrence can be determined from this

            long startTime, endTime;

            var pt = pathDT(path);

            var dt = pt[0];
            var dur = occToDT(pt[1]);

            if (pathStartTime == ETERNAL) {
                startTime = end.start();
                endTime = shift(shift(startTime, dur, true), occToDT(dt), true);
            } else if (pathEndTime == ETERNAL) {
                if (start instanceof Absolute) {
                    startTime = pathStartTime;
                    endTime = shift(shift(startTime, dur, true), occToDT(dt), true);
                } else {
                    startTime = endTime = ETERNAL;
                }
            } else {
                if (pathStartTime != TIMELESS) {
                    startTime = pathStartTime;
                    endTime = shift(shift(startTime, occToDT(dt), true), dur, true);
                } else {
                    startTime = shift(pathEndTime, occToDT(dt), false);
                    endTime = shift(startTime, dur, true);
                }
            }

            return each.test(shadow(start.id, startTime, endTime));
        }

    }

    private final class SubXternalSolver implements Predicate<Term> {

        final Map<Compound, Set<Term>> solutions = new UnifriedMap<>(0);

        Set<Term> s;
        int subSolutionsMaxTries = -1;

        SubXternalSolver(Term x) {
            x.subterms().ANDrecurse(Term::TEMPORAL_VAR, this, null);
        }

        @Override
        public boolean test(Term y) {
            if (y instanceof Compound cy && y.dt() == XTERNAL/* && !y.subterms().hasXternal()*/) {
                solutions.computeIfAbsent(cy, yy -> {
                    if (s != null) s.clear();

                    subSolutionsMaxTries = SubXternal_SUBSOLUTIONS_TRY;

                    solveXternal(yy, z -> {
                        var zz = z.id;
                        if (termsEvent(zz) && !yy.equals(zz)) {
                            if (s == null) s = new UnifiedSet<>();
                            if (s.add(zz))
                                return --subSolutionsMaxTries > 0;
                        }
                        return true;
                    });

                    return (s==null || s.isEmpty()) ? EMPTY_SET : new ArrayUnenforcedSet<>(s.toArray(EmptyTermArray));
                });
            }

            return true;
        }

        Map<Compound, Set<Term>> solutions() {
            solutions.values().removeIf(z -> z == EMPTY_SET);
            return solutions.isEmpty() ? EMPTY_MAP : solutions;
        }
    }

    private final class RootChainer implements LongObjectProcedure<Term> {

        final Event root;

        private RootChainer(Event root) {
            this.root = root;
        }

        @Override
        public void value(long w, Term t) {
            link(root, w, knowRel(t));
        }
    }

    private final class SequenceRelChain implements LongObjectProcedure<Term> {

        private final Event root;
        Event prev;
        long prevW;

        private SequenceRelChain(Event root) {
            this.prev = this.root = root;
        }

        @Override
        public void value(long w, Term t) {
            var next = knowRel(t);
            link(prev, w - prevW, next);
            if (root!=null && root!=prev)
                link(root, w, next);
            prev = next;
            prevW = w;
        }
    }

    private final class SequenceAbsolute implements LongObjectProcedure<Term> {
        private final int xDur;

        public SequenceAbsolute(int xDur) {
            this.xDur = xDur;
        }

        @Override
        public void value(long w, Term y) {
            know(y, w, w + xDur);
        }
    }
}