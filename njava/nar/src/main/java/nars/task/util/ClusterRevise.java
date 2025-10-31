package nars.task.util;

import jcog.Util;
import jcog.cluster.KMeansPlusPlus;
import jcog.data.DistanceFunction;
import jcog.data.list.Lst;
import jcog.math.Intervals;
import jcog.math.LongInterval;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.sort.RankedN;
import nars.Focus;
import nars.NAL;
import nars.NALTask;
import nars.action.memory.Remember;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.util.DTVector;
import nars.truth.proj.IntegralTruthProjection;
import nars.truth.proj.MutableTruthProjection;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.MIN_VALUE;

/**
 * implements lossy belief table compression
 * applies strategies to ensure a belief table remains under capacity:
 *    --cluster revision
 *    --victim eviction
 */
public class ClusterRevise implements DistanceFunction {

    private final TemporalBeliefTable table;
    private final Remember r;

    /**
     * # of tasks merged at once.  merge rate.
     * >=2; 2 is most precision with least throughput.
     */
    private static final int MERGE_CAPACITY = 2;

    /** especially helpful to be larger for temporally variable terms */
    private static final int MERGE_NEIGHBORS_CANDIDATES =
        MERGE_CAPACITY+1;

    /** >=1 */
    private static final int MERGE_ITER =
        //1;
        2;

    private static final float MERGE_CENTROID_FACTOR =
        1;
        //1/MERGE_LOOPS;
        //1/(float) Math.sqrt(MERGE_LOOPS);


    /**
     * compression eligibility pre-filter. reduces # of tasks considered in compression by including only the N worst victims.
     * eden space. protects some non-victims from merging or eviction
     */
    private static final float ELITISM =
        0; //all tasks are eligible for revision
        //1/2f;
        //1/3f;
        //0.25f;
        //0.5f;
        //0.1f;


    private final VictimRank victimRank;

    private final int dtDims;

    private final DTVector dtv;
    private final int targetSize;
    private final int capacity;
    private final MutableTruthProjection t;
    private final Lst<NALTask> victims;

    float fcFactor;

    public ClusterRevise(TemporalBeliefTable t, Remember r, int targetSize, int capacity) {
        this(t, r, time(r, t), targetSize, capacity);
    }

    /**
     * @param targetSize  target/ideal size
     * @param capacity    maximum allowed size, maintained by evictions if necessary
     */
    public ClusterRevise(TemporalBeliefTable table, Remember r, long now, int targetSize, int capacity) {
        //assert(capacity>=targetSize);

        this.table = table;
        this.r = r;
        this.capacity = capacity;
        this.targetSize = targetSize;

        victims = new Lst<>(capacity);

        var firstTask = table.firstTask();
        if (firstTask != null) {

            victimRank = victimRank(now);

            victimLoad();

            //test the need for compression here in case any have been removed, or the situation has changed due to other threads
            var excess = excess();
            if (excess > 0) {
                victimSort();

                if (victims.size() > 2) {
                    t = newProjection(r);
                    dtDims = DTVector.count(firstTask.term());
                    dtv = dtv();
                    fcFactor =
                            (float)Math.pow(victims.size(), -0.5); //1/sqrt(..)
                            //1f/victims.size();

                    cluster();

                } else {
                    t = null;
                    dtDims = -1;
                    dtv = null;
                }

                evict();
                victims.delete();
            } else {
                t = null;
                dtDims = -1;
                dtv = null;
            }
        } else {
            //HACK
            //this should only happen rarely.
            //table deleted? TODO why
            dtDims = -1;
            victimRank = null;
            dtv = null;
            t = null;
        }

    }

    private @NotNull MutableTruthProjection newProjection(Remember r) {
        final MutableTruthProjection t;
        t = new IntegralTruthProjection(MERGE_NEIGHBORS_CANDIDATES)
                .sizeMin(2)
                .sizeMax(MERGE_CAPACITY)
                .timeRes(r.timeRes())
                .dur(r.dur());
        return t;
    }

    private void victimSort() {
        /* sort: most victimized first  */
        victims.sortThisByDouble(Z -> -victimRank.applyAsDouble(Z), true);

        if (ELITISM > 0)
            victims.removeAbove(Math.round(victims.size() * (1 - ELITISM)));
    }

    private @Nullable DTVector dtv() {
        return dtDims > 0 ? new DTVector(dtDims, Clustering.dimsOffset) : null;
    }

    private static long time(@Nullable Remember r, TemporalBeliefTable t) {
        //return r != null && /* HACK */ r.nar() != null ? r.time() : t.mid() /* guess */;
        return r.time();
    }

    private VictimRank victimRank(long now) {
        var v = victimRank();
        v.now = now;
        return v;
    }

    /** occurrence of temporal terms matters less, so preserve by their strength */
    private VictimRank victimRank() {
        return dtDims == 0 ?
                //new Far()
                new FarWeak(1)
                :
                //new Far()
                new FarWeak(0.5f)

//            new FarWeak(
//                dtDims == 0 ? 1.5f : 0.5f
//            )

        //new Far()
        ;
    }

    //private static final double F_FACTOR = 1/8f, C_FACTOR=1/8f;

    /**
     * related: DT_SPECIFICITY in Answer strength
     */
    private static final float dtFactor = NAL.answer.DT_SPECIFIC;

    @Override
    public double distance(double[] a, double[] b) {
        var dOcc = Intervals.timeBetweenRaw(a,b); //Intervals.timeBetweenPct(a,b);          //Intervals.diffTotal(a, b);
        var dRange = Intervals.diffRangeRaw(a, b);
        var dFreq = Math.abs(a[2] - b[2]);
        var dConf = Util.pctDiff(a[3], b[3]);
        var dDT = dDT(a, b);

        return
            /*Math.sqrt*/(Util.sqr(dOcc) + Util.sqr(dRange) + Util.sqr(dFreq * fcFactor) + Util.sqr(dConf * fcFactor) + Util.sqr(dDT * dtFactor));
            //(dOcc + (df * fcFactor) + (dc * fcFactor)) * (1 + ddt * dtFactor);
            //(dOcc + (df * fcFactor) + (dc * fcFactor)) * (1 + ddt * dtFactor);
            //Math.sqrt(Util.sqr(dOcc) + Util.sqr(df * fcFactor) + Util.sqr(dc * fcFactor)) * (1 + ddt * dtFactor);
    }

    private double dDT(double[] a, double[] b) {
        var dims = a.length;
        return dims <= Clustering.dimsOffset ? 0 :
            DistanceFunction.normalizedCartesian(a, b, Clustering.dimsOffset, dims);
    }

    private KMeansPlusPlus<NALTask> clustering(int centroids) {
        return new Clustering(centroids);
    }

    /** TODO tune */
    private static int centroids(int size) {
        //System.out.println(size + " -> " + c + " (" + ((float)size)/c  + " each)");
        return Math.max(2, (int)Math.ceil(
            //size / ((0.5f + MERGE_NEIGHBORS_CANDIDATES) * MERGE_CENTROID_REDUCTION)
            size * MERGE_CENTROID_FACTOR / (MERGE_NEIGHBORS_CANDIDATES)
        ));
    }

    /** stores the 'radius' of the victims */
    private double range;

    private void cluster() {
        try (var k = clustering(centroids(victims.size()))) {

            updateRange();

            cluster(k);

            mergeCentroids(k);

        } catch (DTVector.ConceptShapeException e) {
            clusterException(e);
        }
    }

    private void updateRange() {
        range = victimRange()/2.0;
        if (range < Float.MIN_NORMAL) range = 1;
    }

    long victimRange() {
        long min = MAX_VALUE, max = MIN_VALUE;
        for (var t : victims) {
            min = Math.min(min, t.start());
            max = Math.max(max, t.end());
        }
        return max - min;
    }

    private void evict() {
        /* use actual size because reinsertions may have caused their own merges making the forced eviction here unnecessary */
        var toRemove = table.taskCount() - capacity;
        if (toRemove <= 0)
            return;

        for (int i = 0, n = victims.size(); toRemove > 0 && i < n; i++) {
            var v = victims.getAndNull(i);
            if (v != null && !v.isDeleted()) {
                if (table.remove(v, true))
                    toRemove--;
            }
        }

        if (toRemove > 0)
            table.evictForce(toRemove, victimRank);
    }

    @Deprecated private static void clusterException(DTVector.ConceptShapeException e) {
        //HACK
        if (NAL.DEBUG)
            throw e;
//        else {
//            //HACK nuke it from orbit to be sure
////                boolean removed = map.keySet().removeIf(
////                        z -> ((NALTask) z).isDeleted());
//            //map.clear();
//        }
    }

    private void mergeCentroids(KMeansPlusPlus<NALTask> k) {
        var mergesRemain = excess();
        if (mergesRemain <= 0)
            return;

        var clusters = k.clusterCount;
        var mergeImmediate = clusters <= mergesRemain;
        var mergeRanked = mergeImmediate ? null : new RankedN<>(new PotentialMerge[mergesRemain],
                m -> (float) m.value());

        //breadth first: attempt merge once in each cluster
        for (var l = 0; l < MERGE_ITER; l++) {
            for (var c = 0; c < clusters; c++)
                mergeCentroid(centroidIterator(k, c), mergeRanked, mergeImmediate);

            if (!mergeImmediate && merge(mergeRanked))
                break; //done
        }
        t.delete();
    }

    private int excess() {
        return table.taskCount() - targetSize;
    }

    private void mergeCentroid(Iterator<NALTask> cc, RankedN<PotentialMerge> mergeRanked, boolean mergeImmediate) {
        if (cc!=null) {
            var m = merge(cc);
            if (m != null) {
                if (mergeImmediate)
                    mergeImmediate(m);
                else
                    mergeRanked.add(m);
            }
        }
    }

    /** returns true if done */
    private boolean merge(RankedN<PotentialMerge> mm) {
        if (!mm.isEmpty()) {
            for (var m : mm)
                mergeImmediate(m);
            mm.clear();
            return table.taskCount() <= targetSize;
        }
        return false;
    }

    @Nullable private PotentialMerge merge(Iterator<NALTask> centroid) {
        t.clear();
        t.clearTime();
        for (var n = 0; centroid.hasNext() && n < MERGE_NEIGHBORS_CANDIDATES; n++) {
            var x = centroid.next();
            if (!x.isDeleted())
                t.add(x);
        }
        return merge();
    }

    private @Nullable PotentialMerge merge() {
        if (t.size() > 1) {
            t.timeAuto = true; //force revision
            var merged = t.task();
            if (merged!=null)
                return new PotentialMerge(t, merged);
        }
        return null;
    }

    private static @Nullable Iterator<NALTask> centroidIterator(KMeansPlusPlus<NALTask> n, int centroid) {
        var cn = n.valueCount(centroid);
        return cn < 2 ? null : (cn > MERGE_NEIGHBORS_CANDIDATES ?
            n.valueIteratorShuffled(centroid, ThreadLocalRandom.current()) :
            n.valueIterator(centroid));
    }


    private static class PotentialMerge {
        private final NALTask[] inputs;
        private final NALTask merged;

        PotentialMerge(MutableTruthProjection t, NALTask merged) {
            this.inputs = t.sourcesArray();
            this.merged = merged;
        }

        public double value() {
            return value(merged) / value(inputs);
        }

        private static double value(NALTask[] xx) {
            double s = 0;
            for (var x : xx)
                s += value(x);
            return s;
        }

        private static double value(NALTask x) {
            return x.evi() * x.range();
        }
    }

    private void mergeImmediate(PotentialMerge m) {
        removeMergeComponents(m.inputs);

        var f = r.focus;
        var x = m.merged;
        if (NAL.revision.INPUT_MERGE_IMMEDIATE) {
            //IMMEDIATE: will not count this as a merge if could not input
            inputMerged(f, x);
        } else {
            //NORMAL: assumes successful eventual input
            f.remember(x);
        }
    }

    private static void inputMerged(Focus f, NALTask x) {
        f.input.rememberNow(x, false).stored();
    }

    /**
     * remove components of the merge before adding the merge,
     * to avoid their presence being adjacent to the resulting merge
     */
    private void removeMergeComponents(NALTask[] inputs) {
        var toRemove = new Lst<NALTask>(inputs.length);
        for (var s : inputs) {
            s.delete();
                /*var removed = */victims.removeFirstInstance(s);
//                if (!removed) {
//                    if (NAL.test.DEBUG_EXTRA)
//                        throw new WTF(); //HACK TEMPORARY
//                }
            toRemove.add(s);
        }
        if (!toRemove.isEmpty())
            table.removeAll(toRemove);
    }


    private void cluster(KMeansPlusPlus<NALTask> k) {
        var centroids = k.clusterCount;
        var iters = 1 + (int) Math.ceil(
            Math.sqrt(centroids) * NAL.answer.CLUSTER_REVISE_PRECISION);

        k.cluster(victims, iters);

        //k.sortClustersByVariance();
        k.sortClustersRandom();
        //n.clusters.sortThisByFloat(z -> (float) z.center[2]); //sort by increasing (center) conf

    }

    @Nullable private void victimLoad() {
        table.removeIfInternal(x -> {
            if (x.isDeleted()) return true;

            victims.add(x);
            return false;
        }, MIN_VALUE, MAX_VALUE);
    }


    private abstract static class VictimRank implements DoubleFunction<NALTask> {
        protected long now;

        protected long timeTo(NALTask x) {
            return x.timeTo(now, true,
                    //LongInterval.MEAN
                    LongInterval.MIN
                    //LongInterval.MAX
            );
//            return Fuzzy.mean(
//                x.timeTo(now, true, LongInterval.MIN),
//                x.timeTo(now, false, LongInterval.MAX)
//            );
            //abs(now - x.mid());
            //x.timeMeanDuringOrTo(now);
            //x.timeMeanTo(now);
        }
    }

    private static class Weak extends VictimRank {
        @Override
        public double doubleValueOf(NALTask x) {
            //return (float) -nalTask.evi();
            //return (float) ((1 - x.conf()) / x.range());
            return (-x.evi() / x.range());
        }
    }
//    /** TODO needs refined */
//    private static class FarWeak extends Victimizer {
//
//
//        @Override
//        public float rank(NALTask x, float min) {
//
//            long rHalf = x.range()/2; //TODO maybe /4, etc
//            double xEvi = x.eviInteg(
//                //now, now,
//                now - rHalf, now + rHalf,
//                    dur, 0);
//            double d = -xEvi;
//
//            //double d = (1+far(x)) * (1/(1 + (x.evi() * (x.range()/dur))));///*1 + 0.5f * */ (1 - x.conf()));
//
//            //double d = (1+far(x)) * (/*1 + 0.5f * */ (1 - x.conf()));
//            //double d = far(x) + 0.5f * (1 - x.conf());
//            //double d = far(x) / (1 + x.conf());
////            final double r = x.range()/dur;
//
//            //final double dist = 1 + Math.max(0, (d - rangeImportance * r));
//            //final double dist = d / (1 + r * rangeImportance);
////            return (float) (d / (1 + x.conf() * r/(r+d)));
//            //return (float) (d / (1 + x.conf() * r));
//            return (float) d;
//            //return (float)(dist / (1 + x.evi()));
//        }
//
//
//    }

    private static class Far extends VictimRank {
        @Override
        public double doubleValueOf(NALTask x) {
            return timeTo(x);
        }
    }

    /**
     * TODO this may be too intolerant of weak present-time tasks, such as goals. tweak
     */
    private static class FarWeak extends VictimRank {
        private final float distFactor;

        private FarWeak(float distFactor) {
            this.distFactor = distFactor;
        }

        @Override
        public double doubleValueOf(NALTask x) {
            double dist = timeTo(x);

            return -x.evi() / (1 + dist/x.range() * distFactor);
            //return -x.eviInteg() / (1 + Util.log1p(dist) * distFactor);
            //return -x.eviInteg() / (1 + Util.sqr(dist) * distFactor); //to match quadratic EviCurve
            //return -x.eviInteg() / (1 + dist * distFactor);
            //return (1 + dist * distFactor) / (1 + x.eviInteg());
            //return 1 / (1 + x.evi() * (x.range() / (1.0 + dist * distFactor)));
        }
    }


    private class Clustering extends KMeansPlusPlus<NALTask> {
        private static final int dimsOffset = 4;

        public Clustering(int centroids) {
            super(centroids, dimsOffset + dtDims, ClusterRevise.this, new XoRoShiRo128PlusRandom());
        }

        @Override public void coord(NALTask x, double[] c) {
            var now = victimRank.now;
            c[0] = (x.start() - now)/range;
            c[1] = (x.end() - now)/range;

            var t = x.truth();
            c[2] = t.freq();
            c[3] = t.conf();

            var dim = c.length;
            if (dim > dimsOffset && !dtv.set(x, c))
                fail(x);
        }

        private void fail(NALTask x) {
            /* HACK contaminated map due to conceptualization inconsistency */
            x.delete();
            table.clear();
            throw new DTVector.ConceptShapeException();
        }
    }
}
