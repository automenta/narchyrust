package nars;

import jcog.TODO;
import jcog.math.LongInterval;
import jcog.random.RandomBits;
import jcog.sort.FloatRank;
import jcog.sort.RankedN;
import nars.table.BeliefTables;
import nars.table.dynamic.DynTruthBeliefTable;
import nars.task.proxy.SpecialTermTask;
import nars.task.util.TaskList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.util.DTDiffer;
import nars.time.Moment;
import nars.truth.DynTruthTaskify;
import nars.truth.evi.EviInterval;
import nars.truth.proj.IntegralTruthProjection;
import nars.truth.proj.MutableTruthProjection;
import nars.truth.proj.SingletonTruthProjection;
import nars.truth.proj.TruthProjection;
import nars.util.Timed;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import static jcog.math.LongInterval.ETERNAL;
import static jcog.math.LongInterval.TIMELESS;
import static nars.NAL.answer.DT_SPECIFIC;

/**
 * heuristic task ranking for matching of evidence-aware truth values may be computed in various ways.
 * designed to be reusable
 * <p>
 * Analogous to a database query planner/optimizer
 */
public final class Answer implements Timed, Predicate<NALTask>, AutoCloseable {

    /**
     * dummy ranker for use before adding 2nd task.
     */
    private static final FloatRank<NALTask> NullRanker = (x, min) -> 0;
    private final static boolean rangeSpecific = NAL.answer.RANGE_SPECIFIC > 0;
    public final NAR nar;
    public final RankedN<NALTask> tasks;
    public final EviInterval when;
    /**
     * system-wide duration used for truth calculations
     */
    public float durTruth;
    /**
     * distinguishes between true=belief/goal, false=question/quest
     */
    private final boolean beliefOrQuestion;
    public Predicate<? super NALTask> filter;
    /**
     * 'time to live' # of tries remain
     */
    public int ttl;
    /**
     * min mean evi for generated tasks
     */
    public double eviMin = NAL.truth.EVI_MIN;
    private RandomBits rng;
    private @Nullable Term template;
    private float depth;
    public int level;

    /**
     * unspecified time/duration
     */
    public Answer(@Nullable Term template, boolean beliefOrQuestion, int capacity, NAR nar) {
        this(template, beliefOrQuestion, TIMELESS, TIMELESS, 0f, capacity, nar);
    }

    public Answer(@Nullable Term template, boolean beliefOrQuestion, long start, long end, float dur, int capacity, NAR nar) {
        this(template, beliefOrQuestion, new EviInterval(start, end, dur), capacity, nar);
    }

    /**
     * TODO filter needs to be more clear if it refers to the finished task (if dynamic) or a component in creating one
     */
    public Answer(@Nullable Term template, boolean beliefOrQuestion, EviInterval when, int capacity, NAR n) {
        this.when = when;
        this.beliefOrQuestion = beliefOrQuestion;
        this.nar = n;
        this.durTruth = n.dur();
        this.tasks = new RankedN<>(new NALTask[capacity], NullRanker);
        template(template);
        depth(beliefOrQuestion ? n.answerDepthBeliefGoal.asFloat() : NAL.answer.ANSWER_DEPTH_QUESTION);
    }

    public static FloatRank<NALTask> rankDTIntermpolate(FloatRank<NALTask> f, Compound term, float dur) {
        var power = DT_SPECIFIC;
        if (power == 0) return f;

        var is = intermpolateStrength(term, dur);
        return is == null ? f : is.mulUnitPow(f, power);
    }

    @Nullable
    private static DTDiffer intermpolateStrength(Compound template, float dur) {
        var d = DTDiffer.the(template);
        return d == DTDiffer.DTDifferZero ? null : d.dur(dur);
    }

    private FloatRank<NALTask> rankRange(FloatRank<NALTask> f, float power) {
        if (power == 0) return f;

        long eRange = when.rangeElse(-1);
        return eRange < 0 ? f :
                f.mulUnitPow(new RangeRank(eRange), power);
    }

    public final Answer match(TaskTable t) {
        if (t instanceof BeliefTables tt)
            matchTables(tt);
        else
            matchTable(t);
        return this;
    }

    private void matchTables(BeliefTables tt) {
        var ta = tt.tablesArray();
        if (plan(ta))
            matchTablesPlan(ta);
        else
            matchTablesSimple(ta);
    }

    /**
     * TODO maybe add a complexity threshold, ex: >3
     */
    private boolean plan(BeliefTable[] ta) {
        return false;
        //return dynTruthTables(ta) > 0;
    }

    private static int dynTruthTables(BeliefTable[] ta) {
        int d = 0;
        for (var t : ta) {
            if (t instanceof DynTruthBeliefTable)
                d++;
            else if (t == null)
                break;
        }
        return d;
    }

    private void matchTable(TaskTable t) {
        if (t instanceof DynTruthBeliefTable dt)
            matchDynTruthTable(dt);
        else
            t.match(tableTTL(depth, t));
    }

    private Answer tableTTL(float depth, TaskTable t) {
        ttl = Math.max(1, (int) (depth * t.capacity()));
        return this;
    }

    private FloatRank<NALTask> rank() {
        var rank = beliefOrQuestion ? eviStrength() : priRelevant();
        var b = rangeSpecific ? rankRange(rank, NAL.answer.RANGE_SPECIFIC) : rank;
        var x = this.template;
        return x == null || !x.TEMPORALABLE() ? rank : rankDTIntermpolate(rank, (Compound) x, durTruth);
    }

    public final Answer clear() {
        tasks.clear();
        return this;
    }

    public final float depth() {
        return depth;
    }

    public final Answer depth(float depth) {
        this.depth = depth;
        return this;
    }

    public final Answer ttl(int ttl) {
        this.ttl = ttl;
        return this;
    }

    /**
     * temporally-relevant evidence metric
     */
    private FloatRank<NALTask> eviStrength() {
        return when;
    }

    private FloatRank<NALTask> priRelevant() {
        var start = when.start();
        if (start == ETERNAL || start == TIMELESS)
            return (t, m) -> t.pri();
        else {
            var end = when.end();
            return (t, m) -> {
                var pri = t.pri(); // * t.originality();
                return (/*pri == pri && */pri > m) ?
                        (float) (pri / (1.0 + t.timeBetweenTo(start, end) / durTruth))
                        :
                        Float.NaN;
            };
        }
    }

    public Answer template(@Nullable Term t) {
        if (t instanceof Neg) throw new IllegalArgumentException();
        this.template = t;
        return this;
    }

    @Nullable
    public final NALTask task(boolean occSpecific) {
        return task(tasks.capacity(), occSpecific);
    }

    /**
     * gets or creates one 'best' task
     *
     * @param revisionCap if <=1, revision is not attempted.
     */
    @Nullable
    public final NALTask task(int revisionCap, boolean occSpecific) {
        var n = tasks.size();
        if (n == 0) return null;

        var first = tasks.first();

        if (first.QUESTION_OR_QUEST())
            return n == 1 ? first : taskSample();
        else {
            if (n == 1 && (!occSpecific || when.contains(first)))
                return first;

            var p = occSpecific ? truthProjection() : truthAnyTime();

            if (p != null) {
                if (p instanceof MutableTruthProjection m) {
                    m.sizeMax(revisionCap);
                    if (occSpecific)
                        first = m.getFirst(); //update if projected
                }
                var y = p.task();
                p.delete();

                //return y;
                return y == null || Objects.equals(first, y) ? first : tasks.rank.better(first, y);
            }
        }
        return null;
    }

    @Nullable
    public final NALTask taskSample() {
        return tasks.getRoulette(random());
    }

    public final @Nullable Truth truth() {
        return truth(true);
    }
    public final @Nullable Truth truth(boolean allowCurve) {
        var p = truthProjection();
        if (p == null) return null;

        if (!allowCurve && p instanceof MutableTruthProjection m) m.curve = false;

        var y = p.truth();
        p.delete();
        return y;
    }

    /**
     * projects for the Answer's target time interval
     */
    @Nullable
    public final TruthProjection truthProjection() {
        return truthProjection(when);
    }

    /**
     * projects for the time of the matched tasks,
     * whenever it might be
     */
    private TruthProjection truthAnyTime() {
        return truthProjection(LongInterval.Timeless);
    }

    @Nullable
    private TruthProjection truthProjection(LongInterval when) {
        return switch (tasks.size()) {
            case 0 -> null;
            case 1 -> truth1(when);
            default -> truthN(when);
        };
    }

    private SingletonTruthProjection truth1(LongInterval when) {
        return SingletonTruthProjection.the(
                    tasks.first(), when,
                    nar.timeRes(), durTruth)
                .eviMin(eviMin);
    }

    private TruthProjection truthN(LongInterval when) {
        var t = tasks;
        int n = t.size();
        return new IntegralTruthProjection(when)
                .dur(durTruth)
                .timeRes(nar.timeRes())
                .freqRes(nar.freqRes.floatValue())
                .term(template)
                .eviMin(eviMin)
                .with(Arrays.copyOf(t.items, n), n);
    }

    private void invalidateRanker() {
        tasks.rank = NullRanker;
    }

    public final boolean isEmpty() {
        return tasks.isEmpty();
    }

    /**
     * consume a limited 'tries' iteration. also applies the filter.
     * returns whether further tests are allowed
     */
    @Deprecated
    @Override
    public final boolean test(NALTask t) {
        if (ttl < 0)
            return false;

        testForce(t); //return value ignored

        return ttl > 0;
    }

    /**
     * bypasses ttl check. use carefully.  returns whether task was added, which is different from test(x)
     */
    public final boolean testForce(NALTask t) {
        ttl--;
        return accept(t) && taskAdd(t);
    }

    private boolean accept(NALTask t) {
        var f = this.filter;
        return f == null || f.test(t);
    }

    /**
     * lazy-initialize ranker only before adding the 2nd item
     */
    private boolean taskAdd(NALTask t) {
        if (tasks.rank == NullRanker)
            tasks.rank(rank());
        return tasks.add(t);
    }

    /**
     * match duration
     */
    @Override
    public final float dur() {
        return when.dur;
    }

    @Override
    public final RandomGenerator random() {
        var r = this.rng;
        return r == null ? nar.random() : r;
    }

    public Answer random(RandomBits r) {
        this.rng = r;
        return this;
    }

    @Override
    public RandomBits rng() {
        if (rng == null)
            this.rng = new RandomBits(nar._random());
        return rng;
    }

    @Override
    public void close() {
        tasks.delete();
        filter = null;
        rng = null;
    }

    public final Answer eviMin(double eviMin) {
        this.eviMin = Math.max(NAL.truth.EVI_MIN, eviMin);
        return this;
    }

    @Override
    public final long time() {
        return nar.time();
    }

    public final Answer time(long start, long end) {
        if (when.set(start, end))
            invalidateRanker(); //HACK because beliefStrength() may clone

        return this;
    }

    public final Moment time(long start, long end, float dur) {
        var timeChanged = when.set(start, end);
        var durChanged = when.durIfChanged(dur);
        if (timeChanged || durChanged)
            invalidateRanker(); //HACK because beliefStrength() may clone
        return when;
    }

    public final @Nullable Term template() {
        return template;
    }

    public final @Nullable NALTask sample() {
        return isEmpty() ? null : tasks.getRoulette(random());
    }

    @Nullable
    public final TaskList tasks(boolean occSpecific) {
        try {
            var s = tasks.size();
            if (s != 0) {
                var x = task(occSpecific);
                var xx = x != null;
                var yCap = (xx ? 1 : 0) + (occSpecific ? 0 : s);
                var y = new TaskList(yCap);

                if (xx)
                    y.add(x);

                if (!occSpecific) {
                    var taskItems = tasks.items;
                    for (var i = 0; i < s; i++)
                        y.addIfNotContainsInstance(taskItems[i]);
                }
                if (!y.isEmpty())
                    return y;
            }
            return null;
        } finally {
            close();
        }
    }

    /**
     * rewrites matching task content to new term
     */
    public void replace(Term x, Term y) {
        var items = tasks.items;
        var n = items.length;
        for (var i = 0; i < n; i++) {
            var z = items[i];
            if (z == null) break; //done
            items[i] = SpecialTermTask.proxyUnsafe(z, y).copyMetaAndCreation(z);
        }
    }

    public final Answer filter(@Nullable Predicate<? super NALTask> filter) {
        this.filter = filter;
        return this;
    }

    public final Answer dur(float dur) {
        var d = when.dur;
        if (d != dur) {
            when.dur(dur);
            invalidateRanker(); //HACK because beliefStrength() may clone
        }
        return this;
    }

    public final long start() {
        return when.s;
    }

    public final long end() {
        return when.e;
    }

    private void matchTablesSimple(BeliefTable[] ta) {
        for (var t : ta) {
            if (t == null) break;
            matchTable(t);
        }
    }

    private void matchDynTruthTable(DynTruthBeliefTable t) {
        //if (NAL.answer.DYN_DEPTH_DROPOUT) if (!rng().nextBoolean(depth())) return;

        var earlyExit = false;
        var beliefOrGoal = t.beliefOrGoal;
        for (var m : t.model) {
            if (depth > m.levelMax())
                continue;
            NALTask y;
            try (var Y = new DynTruthTaskify(m, beliefOrGoal, this)) {
                y = Y.taskClose();
            }
            //noinspection ConstantValue
            if (y != null && test(y) && earlyExit)
                break;
        }
    }

    private void matchTablesPlan(BeliefTable[] ta) {
        new AnswerPlan(ta);
    }

    /**
     * TODO dtSpecificity for ==> and &&
     */
    private record RangeRank(long range) implements FloatRank<NALTask> {

        @Override
        public float rank(NALTask x, float min) {
            //weight by difference in target ranges, to filter different scales
            long s = x.start();
            if (s == ETERNAL) return 1; //entirely non-specific  TODO discount to prefer temporal?

            long xRange = 1 + (x.end() - s);
            long eRange = 1 + range;

            /* rangeRatio always >=1 */
            //assert(eRange>0 && xRange>0);
            double rangeRatio =
                    eRange > xRange ? ((double) eRange) / xRange : ((double) xRange) / eRange;
            //(float)Math.max(((double)xRange)/eRange, ((double)eRange)/xRange);
            //float rangeDiff =
            //Math.abs(xRange - eRange);
            //float y = (float) (1 / (1 + ((double) rangeDiff) / eRange));
            float y = (float) (1 / (/*1+*/rangeRatio));
            return y;
        }
    }

    /**
     * TODO
     */
    private class AnswerPlan {

        List<DynTruthTaskify> dyns;

        public AnswerPlan(BeliefTable[] ta) {
            throw new TODO();
//            dyns = new Lst<>(ta.length /* TODO shorter */);
//            for (var t : ta) {
//                if (t == null) break;
//                if (t instanceof DynTruthBeliefTable dt) {
//                    var dtt = new DynTruthTaskify(dt, Answer.this);
//                    //TODO expand recursively, build the tree, etc.
//                    if (dtt.expand())
//                        dyns.add(dtt);
//                } else
//                    matchTable(t);
//            }
        }
    }
}
