package nars.truth.proj;

import com.google.common.collect.Iterables;
import jcog.Is;
import jcog.Research;
import jcog.Util;
import jcog.data.array.IntComparator;
import jcog.data.graph.BitMatrixGraph;
import jcog.data.list.Lst;
import jcog.math.Intervals;
import jcog.sort.QuickSort;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.NALTask;
import nars.Term;
import nars.Truth;
import nars.subterm.TermList;
import nars.task.ProxyTask;
import nars.task.proxy.SpecialOccurrenceTask;
import nars.task.proxy.SpecialPuncTermAndTruthTask;
import nars.task.proxy.SpecialTermTask;
import nars.task.util.StampOverlapping;
import nars.task.util.TruthComputer;
import nars.term.Compound;
import nars.term.compound.LightCompound;
import nars.term.util.DTDiffer;
import nars.term.util.Intermpolate;
import nars.term.util.conj.ConjBundle;
import nars.time.Moment;
import nars.truth.Underlap;
import nars.truth.evi.EviInterval;
import nars.truth.util.TaskEviList;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Consumer;

import static java.lang.System.arraycopy;
import static jcog.Util.fma;
import static jcog.Util.sqr;
import static nars.NAL.truth.EVI_MIN;
import static nars.NAL.truth.FREQ_EPSILON;
import static nars.Op.CONJ;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 */
@Is({"Interpolation", "Extrapolation"})
@Research
public abstract class MutableTruthProjection extends TaskEviList implements TruthProjection, TruthComputer, Consumer<NALTask>, IntComparator {

    /** may not be necessary */
    private static final boolean SORT = false;

    private final EviInterval at;

    /** true: allow generation of TruthCurve, false: flat */
    public boolean curve = NAL.truth.REVISE_CURVE;

    public MutableTruthProjection freqRes(float f) {
        freqRes = f;
        return this;
    }

    public double eviMin() {
        return eviMin;
    }

    public MutableTruthProjection overlap(boolean allow) {
        this.overlapAllow = allow;
        return this;
    }

    public final NALTask getSource(int i) {
        return unwrap(get(i));
    }

    public NALTask[] sourcesArray() {
        var s = size();
        var x = new NALTask[s];
        double valueInputs = 0;
        for (var i = 0; i < s; i++)
            x[i] = getSource(i);
        return x;
    }

    public enum EviMerge {
        Sum,
        SumDoubtVariance,
//        Mean,
//        Max,
//        Min
//      Product,
    }

    /**
     * content target, either equal in all the tasks, or the result is
     * intermpolated (and evidence reduction applied as necessary)
     */
    @Nullable public Term term;

    double eviMin = EVI_MIN;

    /** whether to ignore overlap */
    public boolean overlapAllow;

    /**
     * used in final calculation of to start/end time intervals
     */
    int timeRes = 1;

    public float freqRes;
    public double confRes;

    private int sizeMin = 1, sizeMax = Integer.MAX_VALUE;

    public boolean timeAuto;

    MutableTruthProjection(int capacity) {
        this(TIMELESS, TIMELESS, capacity);
    }

    MutableTruthProjection(long start, long end) {
        this(start, end, 0);
    }

    MutableTruthProjection(long start, long end, int capacity) {
        super(capacity);
        at = new EviInterval(start, end);
        if (start == TIMELESS)
            timeAuto = true;
    }

    @Override
    public final long[] startEndArray() {
        return AND(NALTask::ETERNAL) ? new long[]{ETERNAL, ETERNAL} : super.startEndArray();
    }

    protected final int sizeMax() {
        return sizeMax;
    }

    @Override
    public final Moment when() { return at; }

    public final TruthProjection with(NALTask[] t, int numTasks) {
        this.items = t;
        this.size = numTasks;
        return this;
    }

    public final MutableTruthProjection timeRes(int timeRes) {
        this.timeRes = Math.max(1, timeRes);
        at.dither(timeRes);
        return this;
    }

    @Override
    public final void accept(NALTask t) {
        add(t);
    }

    @Override
    public final @Nullable Truth truth() {
        return commit() ? computeTruth() : null;
    }

    /**
     * removes the weakest components sharing overlapping evidence with stronger ones.
     * should be called after all entries are added
     */
    private boolean commit() {
        if (evi!=null) throw new UnsupportedOperationException("already commited");

        var sizeMin = this.sizeMin;
        if (size < sizeMin) return false;

        focus(timeAuto);
        if (size < sizeMin) return false;

        //if (NAL.revision.TRUTH_PROJECTION_FILTER_WEAK_PRE) filterWeak();

        if (size == 1) {
            term(term(0));
            return true;
        } else {
            return commitN();
        }

        //focus(timeAuto);
    }

    private void filterWeak() {
         if (size > 1)
             filterWeak(weakPct());
    }

    private boolean commitN() {

        if (!overlapAllow) {
            filterOverlap();
            if (size < sizeMin) return false;
        }

        if (!intermpolate())
            return false;

        removeExcess();

        if (NAL.revision.TRUTH_PROJECTION_FILTER_WEAK_POST) filterWeak();

        return true;
    }

    private float weakPct() {
        /* TODO allow larger margin if all the frequency are similar, etc ?  */
        //return NAL.truth.TRUTH_EPSILON;
        return Math.max(freqRes, FREQ_EPSILON);
    }


    private void removeExcess() {
        var s = this.size;
        var excess = s - sizeMax();
        if (excess > 0) {
            for (var i = 0; i < excess; i++)
                nullify(--s);
            removeNulls(); //HACK
            //assert(this.size==s);
        }

    }

    /** filter weak contributors to avoid including them in the stamp */
    private boolean filterWeak(double pctThresh) {
        if (pctThresh < Double.MIN_NORMAL)  return false;
        var e = this.evi;  if (e == null) return false;
        var sPrev = size;       if (size <= 1) return false;
        var sMin = sizeMin;     if (sPrev <= sMin) return false;

        var eSum = Util.sum(e, 0, sPrev);
        assert(eSum >= Double.MIN_NORMAL): "uninitialized";

        var t = this.items;
        double eThresh = -1;
        var removeMax = sPrev - sMin;
        if (removeMax <= 0)
            return false; //shouldnt happen

        var removedSomething = false;
        for (var i = sPrev - 1; i >= 1; i--) {
            var ei = e[i];

            if (eThresh < 0)  //recalculate?
                eThresh = eSum * pctThresh;

            if (ei < eThresh) {
                e[i] = 0;
                t[i] = null;
                removedSomething = true;
                if (--removeMax <= 0)
                    break;

                if (ei > Double.MIN_NORMAL) {
                    //update sum and lower thresh
                    eSum -= ei; eThresh = -1 /* force recalc */;
                }
            }
        }

        if (removedSomething) {
            removeNulls();
            return true;
        } else
            return false;
    }


    /** computes an ideal (weighted) inner temporal range for the contained tasks according to a ranking function.
     *  items are ignored when the rank function returns NaN
     *  returns null if unchanged
     *
     * @param strength (anti-momentum, between 0 and 1) 0 = qs..qe unchanged, 1 = fully concentrated
     */
    @Nullable private long[] concentrate(long qs, long qe, float strength) {
        if (strength < Float.MIN_NORMAL)
            return null;

        assert(qs!=TIMELESS && qs!=ETERNAL); //if (qs == TIMELESS) throw new TODO("compute union here");

        var tasks = this.items;
        var size = this.size;
        var evi = this.evi;

        /* interpolated time range; stored as offset from qs and qe (respectively) for maximal precision */
        double ss = 0, ee = 0, rSum = 0;

        for (var i = 0; i < size; i++) {
            var t = tasks[i];
            long ts = t.start(), te;
            if (ts == ETERNAL || !Intervals.containsRaw(qs, qe, ts, te = t.end())) {
                ts = qs; te = qe;
            }

            var ei = evi[i];//  if (r!=r || r < Double.MIN_NORMAL) continue;
            rSum += ei;
            ss  = fma(ei, ts - qs, ss);
            ee  = fma(ei, te - qe, ee);
        }

        if (rSum <= Double.MIN_NORMAL)
            return null; //??

        if (strength < 1) {
            var weak = 1 - strength;
            var eWeak = rSum * weak;
            rSum += eWeak; //TODO fma ^
        }

        var cs = (long) (qs + ss/rSum);
        var ce = (long) Math.ceil (qe + ee/rSum);

        return new long[] {cs, ce};
    }

    /**
     * evidence value being computed:
     * <p>
     * if the query is ETERNAL (ie. all components are eternal): evi[] stores the average evidence,
     * otherwise it would be infinite.
     * <p>
     * else if the query is TEMPORAL (one or more components are non-eternal): evi[] stores the evidence integral (area under curve) during the query time range
     */
    private void update() {
        var n = size;
        if (n <= 0) return;

        var e = this.evi;
        if (e == null || e.length < n)
            e = new double[n];

        var tasks = items;
        computeComponents(tasks, 0, n, at, e);

        var count = 0;
        var sort = SORT;
        var eiPrev = Double.POSITIVE_INFINITY;
        for (var i = 0; i < n; i++) {
            var ei = e[i];
            if (eviValid(ei)) {
                if (!sort) sort = (ei > eiPrev);
                eiPrev = ei;
                count++;
            } else {
                tasks[i] = null;
                e[i] = 0;
            }
        }

        if (count < sizeMin)
            clear();
        else
            commitComponents(e, n, count, sort);
    }

    private void commitComponents(double[] evi, int n, int count, boolean sort) {
        this.evi = evi;

        if (count != n)
            removeNulls();

        if (sort && count > 1)
            sort();

        size = count;
    }


    protected abstract boolean computeComponents(NALTask[] tasks, int from, int to, EviInterval at, double[] evi);

    protected void sort() {
        sort(this);
    }

    protected void sort(IntComparator sortComparator) {
        QuickSort.quickSort(0, size, sortComparator, this::swap); //descending
        if (size > 2)
            shuffleTiered();
    }

    private void shuffleTiered() {
        ArrayUtil.shuffleTiered(i -> evi[i], this::swap, size);
    }

    /**
     * removes weakest tasks having overlapping evidence with stronger ones
     */
    private int filterOverlap() {

        //assert(minComponents >= 1);

        final var s = size;

        var evi = this.evi;
        var tasks = this.items;

        BitMatrixGraph conflictGraph = null;
        var conflicts = 0;
        var trims = false;
        try (var o = new StampOverlapping()) {
            for (var r = 0; r < s-1; r++) { //strongest first
                o.set(tasks[r]);
                for (var c = s - 1; c >= r+1; c--) {  //weakest first
                    if (o.test(tasks[c])) {
                        //OVERLAPS!
                        if (Underlap.separate(tasks, r, c, changed -> {
                            computeComponent(this.items, changed, this.at, this.evi);
                            return eviValid(this.evi[changed]);
                        })) {
                            trims = true; //successful underlap the overlap
                        } else {
                            if (conflictGraph == null) conflictGraph =
                                new BitMatrixGraph.BitSetMatrixGraph(s, false);
                                //new BitMatrixGraph.BitSetRowsMatrixGraph(s, false);
                            conflictGraph.set(r, c, true);
                            conflicts++;
                        }
                    }
                }
            }
            //TODO if there is only one conflict for a given pair of edges,
            //and the overlap time is relatively minimal compared to their ranges,
            //trim the weaker one to a range where no overlap occurrs and remove
            //the conflict
        }

        var sAfter = s;

        if (conflicts == 1) {
            //simple case; find it and remove the weaker item
            if (--sAfter < sizeMin)
                return 0; //failed

            for (var r = 0; r < s; r++) {
                var c = conflictGraph.lastColumn(r, true);
                if (c >= 0) {
                    nullifyNode(conflictGraph, c);
                    break;
                }
            }

        } else if (conflicts > 1) {
            //Dominator Elimination
            var rowOrder = sortDominators(conflictGraph);

            for (var ro = 0; ro < s; ro++) {
                var r = rowOrder[ro];
                var thisValue = evi[r];
                if (thisValue <= 0) continue;

                double otherValue = 0;
                var rN = 0;
                for (var c = s-1; c >= 0; c--) {
                    if (r!=c && conflictGraph.isEdge(r, c)) {
                        otherValue += evi[c];
                        rN++;
                    }
                }
                if (rN == 0)
                    continue;

                if (thisValue > otherValue && (sAfter - rN >= sizeMin)) {
                    //sacrifice the others
                    sAfter -= rN;
                    for (var c = s-1; c >= 0; c--) {
                        if (r != c && conflictGraph.isEdge(r,c)) {
                            nullifyNode(conflictGraph, c);
                            if (--rN <= 0)
                                break; //exit early
                        }
                    }
                } else if (--sAfter >= sizeMin) {
                    nullifyNode(conflictGraph, r); //sacrifice for the others
                } else {
                    clear(); return size = 0; //sacrifice impossible; fail
                }

                if (conflictGraph.isEmpty())
                    break; //done
            }
        }

        assert(sAfter >= sizeMin);

        if (sAfter != s)
            removeNulls();

        assert(size > 0);

        if (trims)
            sort();

        return size;
    }

    private static byte[] sortDominators(BitMatrixGraph g) {
        //TODO if minComponents > 1, pre-test to determine if too many conflicts for any possible resolution
        return new DominatorSorter(g).rowOrder;
    }

    private void nullifyNode(BitMatrixGraph g, int r) {
        nullify(r);
        g.removeNode(r);
    }


    /**
     * warning: time focus is not cleared
     */
    @Override
    public void clear() {
        super.clear();
        evi = null;
        term = null;
        //dur(0);
    }

    public final void clearTime() {
        time(TIMELESS, TIMELESS);
    }


    /**
     * compacts both the items[] and evi[] cache, so that they remain in synch
     */
    @Override
    public final boolean removeNulls() {
        var sizeBefore = size;
        if (sizeBefore == 0)
            return false;

        var evi = this.evi;
        if (evi == null)
            return super.removeNulls(); //just remove inner nulls from the items[]

        var items = this.items;
        var sizeAfter = sizeBefore;

        for (var k = 0; k < sizeBefore; k++) {
            if (items[k] == null || !eviValid(evi[k])) {
                sizeAfter--;
                evi[k] = 0;
                items[k] = null;
            }
        }
        if (sizeBefore == sizeAfter)
            return false; //no change
        else {
            _removeNulls(evi, items, sizeBefore, sizeAfter);
            this.size = sizeAfter;
            return true;
        }
    }

    private static void _removeNulls(double[] evi, NALTask[] items, int sizeBefore, int sizeAfter) {
        var sizeCurrent = sizeBefore;
        for (var i = 0; i < sizeCurrent - 1; ) {
            if (evi[i] == 0) {
                var span = (--sizeCurrent) - i;
                arraycopy(evi, i + 1, evi, i, span);
                arraycopy(items, i + 1, items, i, span);
            } else
                i++;
        }
        Arrays.fill(evi, sizeAfter, sizeBefore, 0);
        Arrays.fill(items, sizeAfter, sizeBefore, null);
    }

    public final TaskEviList add(NALTask... tasks) {
        return add(tasks.length, tasks);
    }

    public final MutableTruthProjection add(int firstN, NALTask[] tasks) {
        ensureCapacity(size() + firstN);
        for (var i = 0; i < firstN; i++)
            addFast(tasks[i]);
        return this;
    }

    public final MutableTruthProjection add(Collection<NALTask> tasks) {
        ensureCapacityForAdditional(tasks.size(), true);
        for (var task : tasks)
            addFast(task);
        return this;
    }

    /** comparator by decreasing evidence */
    @Override public final int compare(int a, int b) {
        return a == b ? 0 : Double.compare(evi[b], evi[a]);
    }

    @Override
    public void swap(int a, int b) {
        if (a != b) {
            ArrayUtil.swap(items, a, b);
            ArrayUtil.swapDouble(evi, a, b);
        }
    }

    public MutableTruthProjection sizeMin(int n) {
        this.sizeMin = n;
        return this;
    }

    public MutableTruthProjection sizeMax(int n) {
        assert(n >= sizeMin);
        this.sizeMax = n;
        return this;
    }

    public final float dur() {
        return at.dur;
    }

    public final MutableTruthProjection dur(float dur) {
        at.dur(dur);
        return this;
    }

    private boolean intermpolate() {
        if (termsEqual()) {
            this.term(term(0));
            return true;
        } else
            return _intermpolate();
    }

    static private final boolean REFINE_TARGET = true;

    private boolean _intermpolate() {
//        preIntermpolate();

        final var s = size; //assert(s >= 2);
        var remain = s;

        var evi = this.evi;

        final int j = 0; //TODO try different J because J=0 may actually be the most incompatible
        var items = this.items;
        var original = items[0].term();
        var A = (Compound) original;

        if (REFINE_TARGET/* && s > 1*/) {
            /* finds the first intermpolation, and any items (index>1)
             *  which can't be intermpolated are nullified */
            var ej = evi[j];
            for (var k = 1; k < s; k++) {
                var B = (Compound) items[k].term();
                var AB = intermpolate(B, A, evi[k], ej);
                if (AB != null && A.opID == AB.opID()) {
                    //intermpolation succeeded

                    if (B.equals(AB))
                        continue; //remains same
                    else {
                        //use intermpolated result between the first and the next compatible item
                        A = (Compound) AB;
                        break;
                    }
                }

                //INCOMPATIBILITY
                nullify(k); //remove incompatible
                if (--remain < sizeMin)
                    return false;
            }
        }

        var eviChanged = false;
        if (remain > 1 && NAL.revision.INTERMPOLATE_DISCOUNT_EVIDENCE) {
            //discount evidence in proportion to dtDiff
            var dur = dur();
            if (dur == 0)
                throw new UnsupportedOperationException();

            DTDiffer diffA = null;
//            DTVector diffA = null;

            var targetEquals = A.equals();
            for (var i = 0; i < s; i++) {
                var source = items[i];
                if (source == null) continue;

                var src = (Compound) source.term();
                if (!targetEquals.test(src)) {

                    var dtFactor = NAL.evi.project.project(
                            (diffA == null ? diffA = DTDiffer.the(A) : diffA).diff(src), dur);
//                    var dtFactor =
//                            (diffA == null ? diffA = new DTVector(target) : diffA).diff(src), dur);

                    if (dtFactor < 1) {
                        var truthDiscounted = source.truth().cloneEviMult(dtFactor, eviMin);
                        if (truthDiscounted == null) {
                            //evi underflow
                            nullify(i);
                            if (--remain < sizeMin) return false;
                        } else {
                            set(i, new MySpecialPuncTermAndTruthTask(A, truthDiscounted, source));
                            eviChanged |= computeComponent(items, i, evi);
                        }
                    }
                }
            }
        }

        if (remain < s)
            removeNulls();

        if (size < sizeMin)
            return false;
        else {
            if (eviChanged && remain > 1)
                sort();

            term(A);
            return true;
        }
    }

    private @Nullable Term intermpolate(Compound source, Compound target, double evi, double ej) {
        return NALTask.taskTerm(new Intermpolate(
                (float) (ej / (ej + evi)), timeRes
            ).get(target, source), punc(), false, true);
    }

    public MutableTruthProjection term(@Nullable Term t) {
        this.term = t;
        return this;
    }

    private static Term unbundle(Term i) {
        var events = new TermList();
        ConjBundle.events(i, events::add);
        return new LightCompound(CONJ.id, events);
    }

    private boolean computeComponent(NALTask[] items, int i, double[] evi) {
        return computeComponent(items, i, at, evi);
    }

    protected final boolean computeComponent(NALTask[] items, int i, EviInterval at, double[] evi) {
        return computeComponents(items, i, i+1, at, evi);
    }

    @Override
    public final Lst<NALTask> sortThis() {
        sort();
        return this;
    }

    @Override
    public final void sort(Comparator<? super NALTask> comparator) {
        throw new UnsupportedOperationException("use sort()");
    }

    private boolean termsEqual() {
        var n = size;
        var x = items;
        return switch (n) {
            case 0, 1 -> true;
            case 2 -> x[0].equalsTerm(x[1]);
            //case 3 -> x[0].equalsTerm(x[1]) && x[0].equalsTerm(x[2]);
            default -> {
                var xEquals = x[0].term().equals();
                for (var i = 1; i < n; i++) {
                    if (!xEquals.test(x[i].term()))
                        yield false;
                }
                yield true;
            }
        };
    }

    @Override
    public void nullify(int index) {
        super.nullify(index);
        evi[index] = 0;
    }

    public final byte punc() {
        //if (isEmpty()) throw new RuntimeException();
        return items[0].punc();
    }


    public void print() {
        print(System.out);
    }


    public void print(PrintStream o) {
        forEachWith((t, oo) -> oo.println(t.proof()), o);
    }

    @Override
    public final long start() {
        return at.s;
    }

    @Override
    public final long end() {
        return at.e;
    }


    /**
     * aka "trimwrap", or "trim". use after filtering cyclic.
     * adjust start/end to better fit the (remaining) task components and minimize temporalizing truth dilution.
     * if the start/end has changed, then evidence for each will need recalculated
     * returns the number of active tasks
     *
     * @param all - true if applying to the entire set of tasks; false if applying only to those remaining active
     */
    private void focus(boolean trim) {

        var n = size; //assert (s > 0);

        var changed = false;
        var qsPrev = this.start();
        if (trim || qsPrev == TIMELESS || qsPrev == ETERNAL) {

            long s, e;

            if (n > 1) {
                long[] u;
                if (evi == null || !NAL.revision.CONCENTRATE_PROJECTION) {
                    u = unionInterval();
                } else {
                    u = concentrate(qsPrev, this.end(),
                        //concentrationStrength()
                        1
                    );
                }
                s = u[0]; e = u[1];
            } else {
                var only = items[0];//evi != null ? items[0] : items[firstValidOrNonNullIndex(0)];
                s = only.start();
                e = only.end();
            }

            changed = time(s, e, timeRes);
        }

        if (changed || (evi == null))
            update();
    }

    private long[] unionInterval() {
        long u0 = Long.MAX_VALUE, u1 = Long.MIN_VALUE;
        var items = this.items;
        var hasEvi = evi != null;
        var s = size;
        for (var i = 0; i < s; i++) {
            if (hasEvi ? valid(i) : nonNull(i)) {
                var t = items[i];
                var ts = t.start();
                if (ts != ETERNAL) {
                    u0 = Math.min(u0, ts);
                    u1 = Math.max(u1, t.end());
                }
            }
        }

        if (u0 == Long.MAX_VALUE)
            u0 = u1 = ETERNAL; //all eternal

        return new long[] { u0, u1 };
    }

    /**
     * Truth Coherency Metric
     * inversely proportional to the statistical variance of the contained truth's frequency components
     * <p>
     * TODO refine, maybe weight by evi
     */
    @Research
    public double coherency() {
        var s = size;
        if (s == 0) return Double.NaN;


        var items = this.items;
        var n = 0;
        double mean = 0;
        for (var i = 0; i < s; i++) {
            if (valid(i)) {
                mean += items[i].freq();
                n++;
            } else
                n--;
        }

        if (n <= 1)
            return 1;

        mean /= n;

        var variance = 0.0;
        for (var i = 0; i < s; i++) {
            if (valid(i))
                variance += sqr(items[i].freq() - mean);
        }
        variance /= n;

        return Math.max(0, 1 - variance);
    }


    /**
     * if not triming, the projection's current range is used raw.  otherwise it will be trimmed to fit the contained tasks.
     * does not force project a singular result.
     */
    @Nullable @Override public final NALTask task() {
        if (!commit())
            return null;

        var size = this.size; if (size == 0) return null; //HACK shouldnt happen

        var y = size == 1 && timeAuto ?
            getFirst() :
            task(term);

        return y != null ? taskPost(y) : null;
    }

//    /** is X > Y? */
//    private boolean stronger(NALTask x, NALTask y) {
//        if (x == y) return false; //identical
//
//        //return x.evi() > y.evi();
//
//        if (x.ETERNAL() || y.ETERNAL())
//            return x.evi() > y.evi();
//        else
//            return (x.range() * x.evi()) > (y.range() * y.evi());
//    }

    @Nullable private static NALTask taskPost(NALTask y) {
        var t = y.term();
        return t instanceof LightCompound l ? taskPostUnwrap(y, l) : y;
    }

    private static NALTask taskPostUnwrap(NALTask y, LightCompound t) {
        //create durable immutable representation
        var u = t.op().the(t.dt(), t.subtermsDirect());
        assert(u.TASKABLE());
        return SpecialTermTask.proxy(y, u).copyMeta(y);
    }

    /**
     * sets the evidence min cut-off (for results, not components)
     */
    public final MutableTruthProjection eviMin(double eviMin) {
        this.eviMin = eviMin;
        return this;
    }

    public void addAll(Iterable<NALTask> i) {
        i.forEach(this::add);
    }

    public void addFirst(int n, Iterable<NALTask> i) {
        for (var nalTask : i) {
            add(nalTask);
            if (--n <= 0) break;
        }
    }

    public Iterable<NALTask> sources() {
        return Iterables.transform(this, MutableTruthProjection::unwrap);
    }

    /** unwraps to the source task */
    public static NALTask unwrap(NALTask x) {
        while (x instanceof TruthProjectionWrapper p) {
            //noinspection CastConflictsWithInstanceof
            x = ((ProxyTask) x).task;
        }
        return x;
    }


    /** marker interface for wrapped task */
    private interface TruthProjectionWrapper { }

    @Deprecated public static final class MySpecialOccurrenceTask extends SpecialOccurrenceTask implements TruthProjectionWrapper{
        public MySpecialOccurrenceTask(NALTask task, long start, long end) {
            super(task, start, end);
            copyMeta(task);
        }
    }

    private static final class MySpecialPuncTermAndTruthTask extends SpecialPuncTermAndTruthTask implements TruthProjectionWrapper {
        MySpecialPuncTermAndTruthTask(Compound a, @Nullable Truth truth, NALTask task) {
            super(task, a, truth);
            copyMeta(task);
        }
    }

    private final static class DominatorSorter implements IntComparator, IntIntProcedure {
        final byte[] rowConflict, rowOrder;

        DominatorSorter(BitMatrixGraph g) {
            var s = g.size();
            rowOrder = new byte[s];
            rowConflict = new byte[s];
            for (byte r = 0; r < s; r++) {
                rowOrder[r] = r;
                rowConflict[r] = (byte)g.rowCardinality(r);
            }
            QuickSort.quickSort(0, s, this, this);
        }

        @Override
        public int compare(int a, int b) {
            var ab = Byte.compare(rowConflict[b], rowConflict[a]); //descending
            return ab != 0 ? ab : Integer.compare(b, a) /* descending */;
        }

        /** swapper */
        @Override public void value(int a, int b) {
            swap(rowOrder, a, b);
            swap(rowConflict, a, b);
        }

        private static void swap(byte[] x, int a, int b) {
            var o = x[b];
            x[b] = x[a];
            x[a] = o;
        }
    }
}