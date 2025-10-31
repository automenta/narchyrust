package nars.action.link;

import jcog.Util;
import jcog.cluster.BagClustering;
import jcog.cluster.BagClustering.Dimensionalize;
import jcog.data.bit.MetalBitSet;
import jcog.data.list.Lst;
import jcog.decide.MutableRoulette;
import jcog.decide.Roulette;
import jcog.math.Intervals;
import jcog.math.LongInterval;
import jcog.pri.AtomicPri;
import jcog.pri.PLink;
import jcog.signal.FloatRange;
import jcog.signal.IntRange;
import jcog.util.ArrayUtil;
import nars.*;
import nars.action.transform.TemporalComposer;
import nars.deriver.reaction.NativeReaction;
import nars.deriver.util.DeriverTaskify;
import nars.task.proxy.SpecialNegTask;
import nars.task.proxy.SpecialTermTask;
import nars.task.util.TaskList;
import nars.term.util.Image;
import nars.term.util.transform.VariableShift;
import nars.truth.dynamic.DynConj;
import nars.truth.dynamic.DynImpl;
import nars.truth.dynamic.DynTruth;
import nars.unify.constraint.TermMatch;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import static java.lang.Math.abs;
import static jcog.Util.sqr;
import static jcog.pri.Prioritized.EPSILON;
import static nars.Op.*;

public class ClusterInduct extends NativeReaction implements TemporalComposer {

    private static final boolean allowVars = true, allowSeq = true;

    /** modes */
    private final boolean conj, disj, impl;


    private static final boolean PRI_IN_CLUSTER_BY_LINK_OR_TASK = true;

//    /** >1 increases the dithering applied to the clustered tasks,
//     *     increasing the probability of parallel sub-conditions */
//    public final IntRange overDither = new IntRange(1, 1, 8);

    public final IntRange condMin = new IntRange(2, 2, 4);

    public final IntRange condMax = new IntRange(3, 2, 16);

    /** output to input cond count ratio: 1.0 is balanced */
    public final FloatRange outputRatio = new FloatRange(1, 0.1f, 128);

//    public final FloatRange outputBatch = new FloatRange(16, 1, 128);

    public final FloatRange forgetRate = new FloatRange(NAL.derive.TERMLINK_and_CLUSTERING_FORGET_RATE, 0, 2);

    /*
    TODO stop adding neighbors if frequency becomes too dilute:
    public final FloatRange freqMin = FloatRange.unit(Util.PHI_min_1f);
    */

    /** when ranking clusters by priority, use priority of the link (true), or the task directly (false) */
    private static final boolean clusterPriorityByLink = true;

    /** TODO per centroid, centroid^0.5 ? */
    public final IntRange clusterIterations = new IntRange(16, 1, 64);

    /**
     * fraction of volMax to stop clustering
     */
    public final FloatRange volFractionLimit = new FloatRange(/*0.9f*/ 1, 0, 1);

    final int centroids, capacity;

    private final Predicate<NALTask> filter;
    private final byte punc;
    final float learningPeriodDurs = 1;

    private final static boolean bagAutoClear =
        false;
        //true;

    public ClusterInduct(byte punc, int centroids, int capacity) {
        this(punc, centroids, capacity, t -> true);
    }

    /**
     * default that configures with belief/goal -> question/quest output mode
     */
    public ClusterInduct(byte punc, int centroids, int capacity, Predicate<NALTask> filter) {
        super();

        single();

        assert(BELIEF_OR_GOAL(punc));
        taskPunc(this.punc = punc);

        taskEternal(false);

        if (!NAL.term.CONJ_INDUCT_IMPL)
            hasNot(PremiseTask, IMPL);

        if (!allowSeq)
            iffNot(PremiseTask, TermMatch.SEQ);

        if (!allowVars)
            hasAny(PremiseTask,
                    Variables //<-- TODO requires multi-normalization (shifting offsets)
                    , false);



        this.filter = filter;
        this.centroids = centroids;
        this.capacity = capacity;

//        condMean = new Ewma().period(CAPACITY/*TODO refine*/).with(Util.mean(condMin.floatValue(), condMax.floatValue()));
        conj = true;
        disj = NAL.temporal.TEMPORAL_INDUCTION_DISJ;
        impl = punc==BELIEF && NAL.temporal.TEMPORAL_INDUCTION_COMPOUND_CLUSTER_IMPL;
    }

    private static void separateVariables(NALTask[] n) {

        var nSize = n.length;
        var hasVars = MetalBitSet.bits(nSize);

        var varCounts = varCounts(n, nSize, hasVars);
        if (varCounts == null || Util.max(varCounts) < 2)
            return;

        //TODO sort by max vars first to reduce effort
        //n.sortThisByInt((NALTask x) -> -x.term().vars());

        var v = new VariableShift(vCommon(varCounts));
        for (var i = 0; i < nSize; i++) {
            if (!hasVars.test(i)) continue;

            var X = n[i];
            var x = X.term();
            if (v.offset() > 0) {
                var y = v.apply(x);
                n[i] = SpecialTermTask.proxyUnsafe(X, y).copyMeta(X); //TODO re-use a VariableShift TermTransform
            }
            v.shift(x);
        }

    }

    private static int vCommon(short[] varCounts) {
        var vCommon = 0;
        if (varCounts[0]>1) vCommon |= VAR_PATTERN.bit;
        if (varCounts[1]>1) vCommon |= VAR_QUERY.bit;
        if (varCounts[2]>1) vCommon |= VAR_INDEP.bit;
        if (varCounts[3]>1) vCommon |= VAR_DEP.bit;
        return vCommon;
    }

    @Nullable private static short[] varCounts(NALTask[] n, int nSize, MetalBitSet hasVars) {
        short[] varCounts = null;
        for (var i = 0; i < nSize; i++) {
            if (i == nSize - 1 && varCounts == null)
                break; //final term, no vars yet, leave school early

            var xs = n[i].term().struct();
            if (Op.hasAny(xs, Variables)) {
                hasVars.set(i);
                if (varCounts == null) varCounts = new short[VarTypes];
                if (Op.hasAny(xs, VAR_PATTERN)) varCounts[0]++;
                if (Op.hasAny(xs, VAR_QUERY)) varCounts[1]++;
                if (Op.hasAny(xs, VAR_INDEP)) varCounts[2]++;
                if (Op.hasAny(xs, VAR_DEP)) varCounts[3]++;
            }

        }
        return varCounts;
    }

//    public Stream<MyBagClustering> clusters(NAR n) {
//        return n.focus.stream().map(z -> z.get().local(this));
//    }

    @Override
    protected final void run(Deriver d) {
        var x = d.premise.task();
        if (filter.test(x))
            clusters(d.focus).put(task(x), d);
    }

    private NALTask task(NALTask x) {
        var xt = x.term();
        var yt = Image.imageNormalize(xt);
        if (!xt.equals(yt))
            return SpecialTermTask.proxy(x, yt).copyMetaAndCreation(x);
            //if (!filter.test(x)) return;
        return x;
    }


//    private void addAll(Lst<NALTask[]> zz, Deriver d) {
//        if (zz==null || zz.isEmpty())  return;
//        for (NALTask[] x : zz)
//            add(x, d);
//        zz.clear();
//    }

    private void add(NALTask[] x, boolean implOrConj, Deriver d) {
        if (allowVars) separateVariables(x);

        _addOne(x, d, implOrConj);
        //_addAll(x, d);
    }

    private void _addOne(NALTask[] x, Deriver d, boolean implOrConj) {
//        if (!conj || !impl)
//            throw new TODO("other configurations");

        if (implOrConj) {
            addImpl(x, d);
        } else {
            if (!disj || d.randomBoolean())
                addConj(x, d);
            else
                addDisj(x, d);
        }

//        int success = switch (d.randomInt(disj ? 3 : 2)) {
//            case 0 -> addImpl(x, d);
//            case 1 -> addConj(x, d);
//            case 2 -> addDisj(x, d);
//            default -> throw new UnsupportedOperationException();
//        };
    }

    private void _addAll(NALTask[] x, Deriver d) {
        var success = 0;
        if (conj)
            success += addConj(x, d);

        if (disj /*&& (!conj || success>0)*/)
            success += addDisj(x, d);

        if (impl && punc==BELIEF)
            success += addImpl(x, d);


//        condMean.accept(
//                //successs > 0 ? x.length : 0
//                success
//        );
    }

    private int addImpl(NALTask[] x, Deriver d) {
        return taskifyAdd(implComponents(x, d), DynImpl.DynImplInduction, d);
    }

    private int addDisj(NALTask[] x, Deriver d) {
        TemporalComposer.filterConjSeq(x);
        return taskifyAdd(x, DynConj.Disj, d);
    }

    private int addConj(NALTask[] x, Deriver d) {
        TemporalComposer.filterConjSeq(x);
        return taskifyAdd(x, DynConj.Conj, d);
    }

    private static NALTask[] implComponents(NALTask[] x, Deriver d) {
        NALTask[] y;
        if (x.length > 2) {
            /* split x[] into two temporally-distinct sets, and CONJ each. */
            y = implComponentsSplit(x, d); if (y == null) return null;
        } else
            y = x.clone();


        if (!NAL.temporal.TEMPORAL_INDUCTION_IMPL_BIDI) {
            /* sort by time */
            if (y[0].start() > y[1].start())
                ArrayUtil.reverse(y);
        }

        //unwrap negated predicate
        if (y[1] instanceof SpecialNegTask s) y[1] = s.task;

        if (NAL.temporal.TEMPORAL_INDUCTION_POLARITY_STOCHASTIC_IMPL_SUBJ) {
            var subjTask = y[0];
            if (!d.rng.nextBooleanFast16(subjTask.freq(d.focus.time.when(d.focus))))
                y[0] = SpecialNegTask.neg(subjTask);
        }

        return y;
    }

    private static NALTask[] implComponentsSplit(NALTask[] x, Deriver d) {
        implComponentsSplitSort(x, d);

        NALTask[] subj, pred;
        pred = new NALTask[x.length/2];
        subj = new NALTask[x.length - pred.length];

        System.arraycopy(x, 0, subj, 0, subj.length);
        System.arraycopy(x, subj.length + 0, pred, 0, pred.length);

        var subjTask = taskify(subj, DynConj.Conj, d);
        if (subjTask == null) return null;
        var predTask = taskify(pred, DynConj.Conj, d);
        if (predTask == null) return null;

        return new NALTask[] { subjTask, predTask };
    }

    /* strategies:
        - sort by occurrence, ex: midpoint (if tasks are not parallel-ish)
        - sort by confidence
        - sort by complexity
        - other tiebreaker cases
     */
    private static void implComponentsSplitSort(NALTask[] x, Deriver d) {
        var parallelish = parallelish(x);
        if (parallelish)
            implComponentSplitSortConf(x, d); //TODO other strategies???
        else
            implComponentSplitSortTemporal(x, d);
    }

    private static boolean parallelish(NALTask[] x) {
        double variance = 0;
        double mean = 0;
        var n = 0 ;
        var smallestRange = Long.MAX_VALUE;
        for (var z : x) {
            var m = occ(z); if (m == ETERNAL) continue;
            mean += m;
            n++;
            var r = z.range();
            if (r < smallestRange) smallestRange = r;
        }
        if (n != 0) {
            mean/=n;
            for (var z : x) {
                var m = occ(z); if (m == ETERNAL) continue;
                variance += sqr(m - mean);
            }
        }

        return variance <= smallestRange;
    }

    private static long occ(NALTask z) {
        return z.start();
        //return z.mid();
    }

    private static void implComponentSplitSortTemporal(NALTask[] x, Deriver d) {
        Comparator<NALTask> c = Comparator.comparingLong(LongInterval::mid);
        if (d.rng.nextBoolean()) c = c.reversed();
        Arrays.sort(x, c);
    }
    @Deprecated private static void implComponentSplitSortConf(NALTask[] x, Deriver d) {
        var c = Comparator.comparingDouble((NALTask z) -> z.evi());
        if (d.rng.nextBoolean()) c = c.reversed();
        Arrays.sort(x, c);
    }
    private static int taskifyAdd(NALTask[] x, DynTruth m, Deriver d) {
        var y = taskify(x, m, d);
        if (y != null) {
            d.add(y, false);
            return x.length;
        } else
            return 0;
    }

    @Nullable private static NALTask taskify(NALTask[] x, DynTruth t, Deriver d) {
        //if (x == null) return null; //HACK
        return new DeriverTaskify(t, d, x).taskClose();
    }

    private MyBagClustering clusters(Focus f) {
        return f.local(this, s -> new MyBagClustering());
    }

    protected float pri(NALTask t) {
        var p = t.pri();
        //float p = (float) t.conf();
        //var p = (float)(Util.max(t.pri(), EPSILON) * t.conf());
//        if (!NAL.temporal.CONJ_INDUCT_NEG_SEQUENCES && t.term().SEQ())
//            p *= t.freq();


        return p;

        //return (float) Fuzzy.meanGeo(t.polarity(), t.conf());

//        return (float) ((0.5f + 0.5f * t.polarity())
//                    * (0.5f + 0.5f * t.conf()))
//                        //* (0.5f + 0.5f * t.pri())
//                ;

        //return t.pri() * (0.5f + t.polarity()*0.5f);

//        float vMax = d.volMax;
//        return Math.max(0, ((vMax + 1) - t.volume()))/ vMax * (0.5f + t.polarity() / 2);

        //return 1/Util.sqrt(t.volume());
        //return (0.5f + t.polarity()/2) / Util.sqrt(t.volume());
        //return t.polarity();
            //t.priElseZero()
            //t.priElseZero() * t.polarity()
            //t.priElseZero() * Math.pow(t.originality(), 2)
            //t.priElseZero() / t.volume()
            //t.priElseZero() / Math.sqrt(t.volume())
            //1
//                //(1 + 0.5 * t.priElseZero()) *
//                (t.evi()) *
//                (1 + (t.range()-1)/d.nar.dur()) *
            // * (t.polarity())
            //(1 + 1f/t.volume())
            //* (1 + 0.5 * t.originality())
        //);
//                 * (1/(1f+t.volume()))
    }

//    /** average conditions represented in clustered tasks, per iteration */
//    @Deprecated final Ewma condMean;

    private class MyBagClustering extends BagClustering<NALTask> {


        static final boolean CENTROID_SAMPLE_ROULETTE_OR_FLAT = true;

        final AtomicBoolean busy = new AtomicBoolean(false);
        volatile long nextLearn = Long.MIN_VALUE;

        MyBagClustering() {
            super(model, centroids, capacity, NAL.taskPriMerge);
            net.realloc(capacity);
        }

        /**
         * called by only one thread at a time:
         * TODO adjust update rate according to parameter
         * TODO move to MyBagClustering
         */
        private void update(Deriver d) {
            if (size() < condMin.intValue())
                return;

            var now = d.now();
            if (now < nextLearn)
                return;

            ClusterInduct.this.now = now;
            nextLearn = now + (long) (Math.ceil(learningPeriodDurs * d.nar.dur()));

            if (!bagAutoClear)
                forget(forgetRate.asFloat());

            net.random = d.rng;
            learn(clusterIterations.intValue());
            next(d.rng.nextFloor(outputRatio.floatValue() *
                bag.size() / condMax.intValue()
                //net.clusterCount()
            ), d);
            net.random = null;

            if (bagAutoClear) {
                bag.clearSoft();
                //bag.clear();
                //net.clear();
            }
        }


        void put(NALTask x, Deriver d) {
//            if (d.ctx.compiled)
//                _put(x,d);
//            else
                d.later.once(Tuples.pair(this, x), () -> _put(x, d));
        }

        void _put(NALTask x, Deriver d) {
            var X = new PLink<>(x, pri(x));
            var Y = put(X);
            //boolean insertedNew = Y == X;

            if (Util.enterAlone(busy)) {
                try {
                    update(d);
                } finally {
                    Util.exitAlone(busy);
                }
            }
        }



//        @Nullable private Lst<NALTask[]> next(Deriver d) {
//
////            float ob = outputBatch.floatValue();
////            if (!d.randomBoolean(1/ob))
////                return null;
//
//            float batchSizeProb =
//                outputRatio.floatValue() / condMean.meanFloat();
//                //outputRatio.floatValue() * ob;
//
//            int batchSize = d.rng.floor(batchSizeProb);
//
//            return next(batchSize, d);
//        }

        private void next(int batchSize, Deriver d) {
            if (batchSize == 0)
                return;

            var condMin = ClusterInduct.this.condMin.intValue();

            var centroid = centroidPrioritized(d.rng, condMin);
            if (centroid == null)
                return;

            int complexMax = d.complexMax - 1;
            for (var i = 0; i < batchSize; i++) {
                var implOrConj = d.randomBoolean(
                    1/2f
                    //1/(1 + x.length/2f )
                );
                var condMax = implOrConj ? 2 : ClusterInduct.this.condMax.intValue();
                var cc = next(net.values(centroid.getAsInt()), condMin, complexMax, condMax, implOrConj, d);
                if (cc != null)
                    add(cc, implOrConj, d);
            }
        }

        private @Nullable IntSupplier centroidPrioritized(RandomGenerator rng, int condMin) {
            var n = net.clusters.size();
            var pri = new float[n];
            var count = 0;
            for (var centroid = 0; centroid < n; centroid++) {
                var s = net.valueCount(centroid);
                if (s >= condMin) {
                    pri[centroid] = centroidPri(centroid, s);
                    count++;
                }
            }
            return count <= 0 ? null : () -> Roulette.selectRoulette(rng, pri);
        }

        private float centroidPri(int centroid, int s) {
            var priMean = net.valueSum(centroid, clusterPriorityByLink ?
                AtomicPri::pri :
                x -> x.id.priElseZero()) / s;
            return (float) (EPSILON + priMean);
        }

        private int centroidRandom(RandomGenerator rng, int condMin) {
            var n = net.clusters.size();
            var centroid = rng.nextInt(n);
            var tries = n;
            do {
                if (net.valueCount(centroid) >= condMin)
                    return centroid;

                centroid++; if (centroid >= n) centroid = 0; //probe forward
            } while (--tries > 0);
            return -1;
        }

        /** TODO ConjList could allow failed adds */
        private final TaskList neighbors = new TaskList();
        private final UnifiedSet<NALTask> neighborSet = new UnifiedSet<>(4);

        @Nullable private NALTask[] next(Lst<PLink<NALTask>> values, int condMin, int complexMax, int clusterMax, boolean implOrConj, Deriver d) {
            if (values.size() < condMin)
                return null;

            try {
                neighbors(values, clusterMax, implOrConj, complexMax, d);
                return neighbors(condMin);
            } finally {
                neighbors.clear();
                neighborSet.clear();
            }
        }

        private @Nullable NALTask @Nullable [] neighbors(int condMin) {
            return neighbors.size() < condMin ? null : neighbors.toArray();
        }

        private void neighbors(Lst<PLink<NALTask>> values, int clusterMax, boolean implOrConj, int complexMax, Deriver d) {
            var volMarginEach = 1;
            var volThresh = (int) Math.floor(volFractionLimit.floatValue() * (d.complexMax - 1) - clusterMax);

            int dupes = 0, maxDupes = condMax.intValue() * 2;

            var b = CENTROID_SAMPLE_ROULETTE_OR_FLAT ?
                centroidRoulette(values, d.rng) :
                values.clone().shuffleThis(d.rng).iterator();

            while (b.hasNext()) {
                var c = b.next();
                var t = c.id;

                if (neighborSet.contains(t)) {
                    if (++dupes > maxDupes)
                        break;
                    else
                        continue;
                }


                var cNeg = negateComponent(t, implOrConj, d);
                var ccv = t.complexity();

                if (ccv + (cNeg ? 1 : 0) < (neighbors.size() > 1 ? volThresh : complexMax)) { //check again

                    var _t = t;
                    if (cNeg) {
                        t = SpecialNegTask.neg(t).copyMeta(t);
                        ccv++;
                    }


                    var volCost = ccv - volMarginEach;
                    if (volThresh - volCost < 0)
                        continue; //adding would be too large

                    volThresh -= volCost;

                    neighbors.add(t);
                    neighborSet.add(_t);

                    if (neighbors.size() >= clusterMax)
                        break; //done

                }
            }
        }

        private static Iterator<PLink<NALTask>> centroidRoulette(Lst<PLink<NALTask>> b, RandomGenerator rng) {
            IntToFloatFunction weight = xx -> {
                var x = b.get(xx);
                return x == null ? 0 : EPSILON +
                    ((PRI_IN_CLUSTER_BY_LINK_OR_TASK ? x : x.id).priElseZero()) //pri by task
                ;
            };
            return new MutableRoulette(b.size(), weight, rng).iterator(b::get);
        }

        public void print() {
//            net.clusters.forEach(c -> {
//                System.out.println(c);
//            });
            bag.print();
        }

    }

    private static boolean negateComponent(NALTask t, boolean implOrConj, Deriver d) {
        if (!implOrConj && !NAL.temporal.TEMPORAL_INDUCTION_CONJ_NEG_SEQ && t.term().SEQ())
            return false;
        return NAL.temporal.TEMPORAL_INDUCTION_POLARITY_STOCHASTIC_CONJ ?
                !d.rng.nextBooleanFast16(t.freq()) : t.NEGATIVE();
    }

    private transient long now = TIMELESS;

    abstract private class Dimensionalize2 extends Dimensionalize<NALTask> {
        Dimensionalize2() { super(2); }
        protected Dimensionalize2(int dims) { super(dims); }
        @Override public void accept(NALTask t, double[] c) {
            c[0] = t.start() - now; c[1] = t.end() - now;
        }
    }
    abstract private class Dimensionalize3Conf extends Dimensionalize2 {
        Dimensionalize3Conf() { super(3); }
        @Override public void accept(NALTask t, double[] c) {
            c[0] = t.start() - now; c[1] = t.end() - now;
            c[2] = t.evi();
        }
    }
    private final Dimensionalize<NALTask> ratio = new Dimensionalize2() {
        @Override public double distance(double[] a, double[] b) {
            return Intervals.
                    dSepNorm(a, b);
                    //dSepFraction(a, b);
        }
    };

    private final Dimensionalize<NALTask> diffTotal = new Dimensionalize2() {
        @Override public double distance(double[] a, double[] b) {
            return Intervals.diffTotal(a,b);
            //return LongInterval.diffTotalNorm(a,b);
        }
    };

    private final Dimensionalize<NALTask> diffMinTimePlusRangeDiff = new Dimensionalize2() {
        private final static double rangeDiff =
            1;
            //1/4f;

        @Override public double distance(double[] a, double[] b) {
            return Intervals.diffSep(a, b)
                    +
                    rangeDiff * Intervals.diffRangeRaw(a, b);
        }
    };

    private final Dimensionalize<NALTask> diffTotalWithConf = new Dimensionalize3Conf() {
        @Override public double distance(double[] a, double[] b) {
            double aRange = a[1] - a[0], bRange = b[1] - b[0];
            var tScale = 1 + Math.min(aRange, bRange);

            return
                   (1 + Math.sqrt(sqr(Intervals.diffMid(a,b)/tScale) + sqr(abs(aRange - bRange)/tScale)))
                     * (1 + Util.pctDiff(a[2], b[2]));

            //(1 + Intervals.diffTotal(a,b)) *
            //(1 + Intervals.diffTotalPct(a,b)) *
            //(1 + Intervals.diffMid(a,b)) * (1 + Intervals.diffRangeRaw(a,b)) *

        }
    };
    /** stratifies by range scale */
    private final Dimensionalize<NALTask> diffMidRange = new Dimensionalize2() {
        private static final float RANGE_FACTOR =
            1/4f;
            //1/20f;
            //1/10f;
            //1/5f;
            //1/3f;

        @Override public double distance(double[] a, double[] b) {
            return (1 + Intervals.diffMid(a,b)) *
                   (1 + Intervals.diffRangeRaw(a, b) * RANGE_FACTOR);
        }
    };

    private final Dimensionalize<NALTask> model =
        //diffMinTimePlusRangeDiff;
        diffTotalWithConf;
        //diffTotal;
        //diffMidRange;
        //ratio;

}