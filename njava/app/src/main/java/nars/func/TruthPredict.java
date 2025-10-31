package nars.func;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.math.FloatMeanEwma;
import jcog.math.FloatSupplier;
import jcog.signal.FloatRange;
import jcog.tensor.LivePredictor;
import jcog.tensor.Predictor;
import nars.*;
import nars.game.Game;
import nars.table.BeliefTables;
import nars.table.dynamic.MutableTasksBeliefTable;
import nars.term.Termed;
import nars.truth.MutableTruth;
import nars.util.NARPart;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import static java.lang.Math.round;
import static jcog.Util.map;
import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/**
 * numeric prediction support
 * TODO configuration parameters for projections
 * --number
 * --time width (duty cycle %)
 * --confidence fade factor
 */
public class TruthPredict extends NARPart implements Runnable {

    /**
     * multiplier to the learned target's average measured evidence,
     * and normalized (divided) among # of futures
     * (TODO normalize by futures factors integral which is a curve determined by eviSustain)
     */
    public final FloatRange eviFactor = new FloatRange(1, 0, 2);

    public final FloatRange eviSustain = new FloatRange(1, 0, 1);
    /**
     * evidence level meter
     */
    final FloatMeanEwma evi;
    final boolean beliefOrGoal; //TODO
    private final LivePredictor predictor;
    /**
     * duration, in cycles
     */
    private final FloatSupplier dur;
    /**
     * belief tables where predictions are written to
     */
    private final MyMutableTasksBeliefTable[] predicting;

    /**
     * true = all concept predictions share same stamp
     * false = each concept has a unique stamp
     */
    private static final boolean stampShared = false;

    //    private final Cause why;
    int futures;

    /** if not high frequency, updates between periods are elided.  otherwise sub-period updates are calculated */
    private boolean updatesBetweenPeriods = false;

    public TruthPredict(Iterable<? extends Termed> in, Iterable<? extends Termed> out, int history, FloatSupplier dur, int futures, IntIntToObjectFunction<Predictor> m, Game g, boolean beliefOrGoal) {
        this(in, out, history, dur, futures, m, g.nar, beliefOrGoal);
        g.afterFrame(this::next);
    }

    public TruthPredict(Iterable<? extends Termed> in, Iterable<? extends Termed> out, int history, FloatSupplier dur, int futures, IntIntToObjectFunction<Predictor> m, NAR n, boolean beliefOrGoal) {
        this(Iterables.toArray(in, Termed.class),
             Iterables.toArray(out, Termed.class),
             history, dur, futures, m, true, beliefOrGoal,
             n);

    }

    /**
     * @param outBG whether the prediction inputs are beliefs (true) or goals (false)
     * @param outBG whether the prediction results are beliefs (true) or goals (false)
     * TODO for pasts, use Sensors like configured in Reflex for more flexible historical 'lenses'
     */
    public TruthPredict(Termed[] in, Termed[] out, int pasts, FloatSupplier dur, int futures, IntIntToObjectFunction<Predictor> m, boolean inBG, boolean outBG, NAR nar) {
        super();
        this.nar = nar;
        this.beliefOrGoal = outBG;

        this.futures = futures;
        this.dur = dur;
        this.predicting = new MyMutableTasksBeliefTable[out.length];

        eviSustain.set(1 - 1f / (1 + pasts)); //TODO refine using log(..)/..

        var sharedStamp = stampShared ? nar.evidence() : null;
        for (int i = 0, n = out.length; i < n; i++)
            addTable(futures, outBG, nar, nar.conceptualize(out[i].term()), i, sharedStamp);

        evi =
            new FloatMeanEwma(0.5f);
            //new FloatAveragedWindow(out.length * 2, 0.5f).mode(FloatAveragedWindow.Mode.Mean);

        predictor = new LivePredictor(new LivePredictor.DenseFramer(
            x(in, inBG),
            pasts,
            dur,
            x(out, outBG)
        ), m);
    }

    private void addTable(int futures, boolean beliefOrGoal, NAR nar, Concept c, int i, long[] sharedStamp) {
        ((BeliefTables) ((beliefOrGoal ? c.beliefs() : c.goals())))
            .add(predicting[i] = new MyMutableTasksBeliefTable(
                    c.term(), futures, beliefOrGoal, sharedStamp != null ? sharedStamp : nar.evidence()));
    }

    private LongToFloatFunction[] x(Termed[] in, boolean beliefOrGoal) {
        return map(c -> truth(c, beliefOrGoal), LongToFloatFunction[]::new, in);
    }

    @Override public final void run() {
        next(null);
    }

    private transient long lastUpdate = Long.MIN_VALUE;

    public synchronized void next(Game g) {

//        float durSys = nar.dur();

        /* in durs */
        var period = this.dur.asFloat();

        var now = g!=null ? g.time() : nar.time();
        if (!updatesBetweenPeriods && lastUpdate != Long.MIN_VALUE && now - lastUpdate < period)
            return; //TOO EARLY

        lastUpdate = now;

        var predictStart =
            now;
            //Math.round(now + durSys);

        predictStart(predictStart);

        predict(g, predictStart, period);

    }

    /** consider using EviInterval that holds a specific (predictStart,predictEnd) interval,
     *  not a 'predictStart' time point and a 'period' */
    private void predict(Game g, long predictStart, float period) {
        var rng = g.random();
        var beliefPriMax = nar.priDefault(beliefOrGoal ? BELIEF : GOAL);
        var fade = eviSustain.floatValue();
        var eviMin = nar.eviMin();

        for (var t = 0; t < futures; t++) {

            var eviMax = this.evi.mean() * eviFactor.doubleValue();
            var evi = eviMax * Math.pow(fade, t);
            if (evi < eviMin)
                continue;

            var pri = (float) (beliefPriMax * (evi / eviMax));

            //output start, end
            long outStart = round(predictStart + t * period);
            long outEnd = round(outStart + period);

            long inStart = round(predictStart + (t-1) * period);

            var p = predictor.get(inStart);
            for (var c = 0; c < p.length; c++)
                predict(c, t, outStart, outEnd, p, evi, pri, rng);
        }
    }

    private void predictStart(long predictStart) {
        //temporarily disable tables to avoid influencing the prediction HACK
        tablesEnable(false);

        predictor.put(predictStart);

        //re-activate tables
        tablesEnable(true);
    }

    private void tablesEnable(boolean e) {
        for (var t : predicting) t.enabled(e);
    }

    private void predict(int concept, int i, long s, long e, double[] p, double evi, float pri, RandomGenerator rng) {
        float F;
        double E;

        var f = p[concept];
        if (Double.isFinite(f)) {
            F = (float) Util.unitizeSafe(f);
            E = evi;
        } else {
            //HACK TODO
            F = rng.nextFloat();// TV snow signal
            E = NAL.truth.EVI_MIN; //non-zero to be careful
        }

        predicting[concept].set(i, F, E, s, e, pri, nar);
    }

    /**
     * excludes Prediction tasks  when evaluating truth for learning
     * TODO
     */
    private final Predicate<NALTask> filter = null;

    private LongToFloatFunction truth(Termed x, boolean beliefOrGoal) {

        //TODO return new ObjectToFloatFunction<EviInterval>() { //and share the EviInterval instance for truth()
        return new LongToFloatFunction() {

            final Term xx = x.term();
            private Concept c;
            private BeliefTable table;

            @Override
            public float valueOf(long when) {
                if (c == null || c.isDeleted()) {
                    c = nar.conceptualizeDynamic(x);
                    table = beliefOrGoal ? c.beliefs() : c.goals();
                }

                var dur = TruthPredict.this.dur.asFloat();

                long start = when, end = round(start + dur);

                //TODO filter predictions (PredictionTask's) from being affecting the calculation.  Use a LongHashSet to store the prediction stamps

                return accept(table.answer(
                        start, end,
                        xx, dur /*gameDur*/, nar).truth(false));
            }

            private float accept(@Nullable Truth t) {
                float f;
                double e;
                if (t == null) {
                    f = Float.NaN;
                    e = 0;
                } else {
                    f = truth(t.freq());
                    e = t.evi();
                }

                evi.accept(e);

                return f;
            }

            private float truth(float freq) {
                return freq;
            }
        };
    }

    public final Predictor predictor() {
        return predictor.p;
    }

    private static final class MyMutableTasksBeliefTable extends MutableTasksBeliefTable {

        MyMutableTasksBeliefTable(Term t, int capacity, boolean beliefOrGoal, long[] sharedStamp) {
            super(t, beliefOrGoal, capacity);
            this.sharedStamp = sharedStamp;
        }

        @Override
        public Truth taskTruth(float f, double evi) {
            return new MutableTruth(f, evi);
        }

    }



}