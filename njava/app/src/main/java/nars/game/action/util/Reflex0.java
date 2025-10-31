package nars.game.action.util;

import jcog.Is;
import jcog.Log;
import jcog.Research;
import jcog.agent.Agent;
import jcog.agent.SensorBuilder;
import jcog.agent.SensorTensor;
import jcog.data.list.Lst;
import jcog.event.Off;
import jcog.math.FloatMeanEwma;
import jcog.math.FloatSupplier;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.FloatRange;
import jcog.signal.IntRange;
import jcog.tensor.Agents;
import jcog.thing.SubPart;
import nars.*;
import nars.control.Cause;
import nars.game.FocusLoop;
import nars.game.Game;
import nars.game.action.AbstractAction;
import nars.game.action.AbstractGoalAction;
import nars.game.meta.MetaGame;
import nars.game.sensor.Sensor;
import nars.table.BeliefTables;
import nars.table.dynamic.MutableTasksBeliefTable;
import nars.term.Termed;
import nars.truth.MutableTruth;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.Streams.stream;
import static java.lang.Float.NaN;
import static java.lang.Math.round;
import static jcog.Util.*;
import static nars.Op.GOAL;
import static nars.TruthFunctions.c2e;

/**
 * Use Reflex.java
 * TODO remove
 *
 * Low-level/instinct sensorimotor reflexes.
 * NAgent Reinforcement Learning Algorithm Accelerator
 * trains and is trainable in cooperation with other action generators.
 * (multiple instances of this can cooperate with each other)
 * <p>
 * Default timing model:
 * <p>
 * [Previous Dur]			[This Dur]				[Next Dur]
 * prevStart...nowStart	nowStart...nextStart	nextStart...nextEnd
 * * reward				*sensorsNow
 * * actionsPrev			*actionsNow
 *
 *      * TODO use AgentBuilder
 *      * TODO auto-confidence in proportion to NAL beliefs
 *      * TODO overlay belief table like BeliefPrediction?
 */
@Research
@Is("Reinforcement_learning")
@Deprecated public class Reflex0 implements SubPart<Game>, Consumer<Game> {
    private static final Logger logger = Log.log(Reflex0.class);

    /**
     * >=1, TODO constructor param
     */
    private final int trainGoals = 1;

//    @Deprecated
//    private static final boolean temporalSuperSampling = false;

    /**
     * NAR goal training rate, in proportion to action's measured dexterity
     */
    public final FloatRange train = new FloatRange(0.95f, 0, 1);

    /**
     * timestretch factor
     * durs>1 may not work with non-zero action/reward shift, TODO test
     */
    @Deprecated private final IntRange durs = new IntRange(1, 1, 32);

    public final AtomicBoolean enabled = new AtomicBoolean(true);
    private final Game game;

    ///** action DAC low-pass smoothing */
    //public final FloatRange actionMomentum = new FloatRange(0f, 0, 1);

    private final ReflexBuilder builder;

    private final Lst<Consumer<SensorBuilder>> sensorBuilders = new Lst<>();

    @Deprecated private final List<AbstractAction> actions = new Lst<>();
    @Deprecated private final XoRoShiRo128PlusRandom rng = new XoRoShiRo128PlusRandom();
    private List<AbstractGoalAction> actionConcepts;

    public final Game game() { return game; }

    /**
     * TODO potentially mutable
     */
    public Agent agent;
    public double[] actionPrev;
    @Deprecated private int inD, outD;
    private transient SensorTensor sensors;
    private Cause cause;
    private ReflexActionTable[] actionTables;

    private double reward = NaN;

    private transient double[] input;
    private transient float durReflex;

    private transient long now = Long.MIN_VALUE;

    private Off on, onAfter;

    int rewardSumIters;

    private BigDecimal rewardSum = new BigDecimal(0);

    public double rewardMean(boolean reset) {
        synchronized(this) {
            if (rewardSumIters == 0)
                return 0;
            double mean = rewardSum.multiply(new BigDecimal(1.0/rewardSumIters)).doubleValue();
            if (reset) {
                rewardSum = new BigDecimal(0);
                rewardSumIters = 0;
            }
            return mean;
        }
    }

    public double rewardCurrent() {
        return reward;
    }

    public Reflex0 addMeta(Game g) {
        NAR n = g.nar;
        IntIntToObjectFunction<Agent> metaAgent =
            //Agents::BayesZero;
            Agents::StreamAC;
            //Agents::CMAESZero;
            //Agents::PPO;
            //Agents::ReinforceLiquid;
            //Agents::DQevolving;
            //Agents::CMAES;
            //Agents::VPG;
            //Agents::DDPG;

        var metaGame = MetaGame.meta(this, () -> {
            return (float) this.rewardCurrent(); //same as game reward:
        });

        var actionMomentum =
            0;
            //0.9f;
        if (actionMomentum > 0)
            metaGame.actions.actionProcessor = new ActionMomentum(actionMomentum);

        n.add(metaGame);

        var history =
            1;
            //2;
        return new Reflex0(metaAgent, metaGame, history);
    }


    public static final class ReflexBuilder {

        public final static ReflexBuilder RANDOM = new ReflexBuilder("Random", Agents::Random, false, false, 1);

        String name;
        private final IntIntToObjectFunction<Agent> agent;
        private final float[] history;

        private static final boolean senseSensors = true;

        final boolean senseActions;


        /** especially important to be true if multiple rewards are present,
         *  then at least as sensors they can be separated -
         *  since RL/MDP must scalarize them as the reward signal. */
        final boolean senseRewards;
        private final List<AbstractGoalAction> actions = new Lst<>();


        public ReflexBuilder(IntIntToObjectFunction<Agent> agent, boolean senseActions, boolean senseRewards, float[] history) {
            this(null, agent, senseActions, senseRewards, history);
        }

        public ReflexBuilder(@Nullable String name, IntIntToObjectFunction<Agent> agent, boolean senseActions, boolean senseRewards, float... history) {
            if (history.length < 1)
                throw new UnsupportedOperationException();

            this.name = name;
            this.agent = agent;
            this.history = history;

            //TODO validate history for uniqueness, and sortedness
            this.senseActions = senseActions; //history.length > 1;
            this.senseRewards = senseRewards;
        }

        @Override
        public String toString() {
            return (name!=null ? name : agent) +
                    "_H=" + Arrays.toString(history);
        }

        /**
         * TODO abstract history model, with non-uniform timesteps
         */
        void add(Game g, Reflex0 r) {

            var actions = g.actions.stream()
                    .filter(z -> z instanceof AbstractGoalAction)
                    .map(z -> (AbstractGoalAction)z)
                    .filter(r::includeAction).toList();
            actions.forEach(r.actions::add);

            this.actions.addAll(actions);


            r.sensorBuilders.add(s -> {
                if (senseSensors)
                    r.sensors(s, g.sensors.stream().filter(r::includeSensor), 0, history);

                if (senseRewards)
                    r.sensors(s, g.rewards.stream()//.filter(r::includeReward)
                        ,NAL.temporal.GAME_REWARD_SHIFT_DURS, history);

                if (senseActions)
                    r.sensors(s, actions.stream(),
                        NAL.temporal.GAME_ACTION_SHIFT_DURS, history);
            });
        }

        public List<AbstractGoalAction> actions() {
            return actions;
        }
    }

    protected boolean includeAction(AbstractAction action) {
        return true;
    }
    protected boolean includeSensor(Sensor action) {
        return true;
    }

    @Deprecated public Reflex0(IntIntToObjectFunction<Agent> agentBuilder, Game g, float... history) {
        this(new ReflexBuilder(agentBuilder,
                true//history.length > 1
                ,g.rewards.size() > 1 || history.length > 1,
                history
                ), g);
    }

    public Reflex0(ReflexBuilder b, Game g) {
        this.builder = b;

        this.game = g;

        g.add(this);
    }

    private static float truthFeedback(Truth t) {
        return t.freq();
        //return t.expectation();
    }

    private void sensors(SensorBuilder s, Stream<? extends FocusLoop> ins, float tOffset, float[] history) {
        ((Stream<? extends Termed>) ins.flatMap(x -> stream(x.components()))).forEach(t -> {
            var c = new ConceptRef(t);
            for (int p = 0; p < history.length; p++) {
                float start = -history[p] + tOffset;
                s.in(new SensorInputter(c, start, p));
            }
        });
    }

    @Override
    public void startIn(Game g) {

        this.cause = g.nar.causes.newCause(this);

        builder.add(g, this);

        var s = new SensorBuilder();
        for (var sb : sensorBuilders)
            sb.accept(s);

        this.sensors = s.sensor();

        this.inD = this.sensors.volume();
        this.input = new double[inD];

        actionConcepts = builder.actions();
        assert(!actionConcepts.isEmpty());

        this.actionTables = actionConcepts.stream().map(new Function<AbstractGoalAction, MutableTasksBeliefTable>() {
            int from, to = -1;

            @Override
            public MutableTasksBeliefTable apply(AbstractGoalAction a) {

                to = from;

                MutableTasksBeliefTable t = new ReflexActionTable(a, from, to, game);

                ((BeliefTables) a.concept.goals()).add(t);

                from = to + 1;

                return t;
            }
        }).toArray(ReflexActionTable[]::new);

        actionPrev = new double[outD = (int) Stream.of(actionTables).count()/*mapToInt(ReflexActionTable::poles).sum()*/];
        assert (inD > 0 && outD > 0);

        setAgent(builder.agent.value(inD, outD));


//        //HACK
//        if (agent instanceof AbstractPG.PGAgent p) {
//            double[] actionRes = actionConcepts.stream().mapToDouble(AbstractGoalAction::resolution).toArray();
//            if (p.pg instanceof AbstractPG R) {
//                R.actionFilter = a -> {
//                    for (int i = 0; i < a.length; i++) {
//                        var ri = actionRes[i];
//                        a[i] = Fuzzy.polarize(Util.round(Fuzzy.unpolarize(a[i]), ri));
//                    }
//                };
//            }
//        }

        //assert (this.on == null);
        this.on = game.onFrame(this);
    }

    @Override
    public void stopIn(Game g) {
        this.on.close();
        this.on = null;
    }

//	private float sensorValueIfMissing() {
//		//return 0.5f;
//		return noise();
//	}

    private void setAgent(Agent a) {
        if (agent != a) {
            this.agent = a;
            logger.info("{} {} in={} out={}", agent, game, inD, outD);
        }
    }

    /**
     * for missing values
     */
    @Deprecated private float missing() {
        return NaN;
        //return rng.nextFloat();
    }

//	private void _feedback(long prev, long now, double[] z) {
//		int k = 0;
//		NAR n = game.nar();
//
//		for (AbstractGoalActionConcept s : actions) {
//			Truth t = s.beliefs().truth(prev, now, durReflex, n);
//			float Y = t != null ? truthFeedback(t) : Float.NaN;
//			float y = Y == Y ? Y : noise();
//			if (actionDiscretization > 1) {
//				//DIGITIZE
//				for (int d = 0; d < actionDiscretization; d++) {
//					float yd = (d + 0.5F) / actionDiscretization;
//					z[k++] = (float)pow(1 - abs(yd - y), digitizedActionSpecificity);
//				}
//				Util.normalizeSubArraySum1(z, k-actionDiscretization, k);
//			} else {
//				z[k++] = y;
//				//z[k++] = polarize(y);
//			}
//		}
//
//		if (this.nothingAction)
//			z[z.length - 1] =
//					(float) (1 - Util.sum(z, 0, z.length-1)/(z.length-1));
//					//(1 - Util.max(fb)) * 1f / (1 + Util.variance(fb)); //HACK TODO estimate better
//
//
//	}


    /** this should be called by Game:
     *      --AFTER: sensors input, rewards input
     *      --BEFORE: actions are finalized
     */
    @Override public final void accept(Game g) {
        if (!enabled.getOpaque() || !updateTime())
            return;

        int gDur = round(g.dur());
        int rDur = round(Math.max((float) 0, durReflex - 1));

        long nowStart = this.now;

        senseIn(); //sensors first, since it might include previous cycle's actionIn not to be overwritten yet

        long ais = nowStart + round(gDur * NAL.temporal.GAME_ACTION_SHIFT_DURS);

        long rs = nowStart + round(gDur * NAL.temporal.GAME_REWARD_SHIFT_DURS);

        long aos = nowStart;

        actionIn(ais, ais + rDur, actionPrev);

        rewardIn(rs, rs + rDur);

        act();

        actionOut(aos, aos + rDur);
    }

    private void actionIn(long s, long e, double[] actions) {
        //System.out.println("actions_in\t" + ts(s,e));

        var nar = game.nar();
        for (ReflexActionTable table : actionTables)
            table.actionIn(s, e, actions, nar);
    }

    private void rewardIn(long s, long e) {
        double r = reward(s, e);

        this.reward = r;

        synchronized (this) {
            if (r==r)
                rewardSum = rewardSum.add(new BigDecimal(r));
            rewardSumIters++;
        }
    }

    private double reward(long s, long e) {
        return game.happiness(s, e,
                NAL.signal.HAPPINESS_DUR_ZERO ? 0 : durReflex);
    }


    private void senseIn() {
        sensors.update().writeTo(input);
    }

    //private final Ewma rewardNormalized = new Ewma(1,0.01f);

    private void act() {
        agent.act(actionPrev, (float)reward, input, agent.actionNext);
    }

    private void actionOut(long s, long e) {
        //System.out.println("actions_out\t" + ts(s,e));
        float train = this.train.floatValue();
		if (train <= Float.MIN_NORMAL)
			return; //disabled
        float pri =
                game.nar.priDefault(GOAL); // * train;
                //Prioritizable.EPSILON;
                //0;
                // /actionTables.length;

        var f = game.focus();
        double[] a = agent.actionNext;
        double eviMin = game.nar.eviMin();
        for (ReflexActionTable t : actionTables)
            t.goal(a, s, e, train, pri, eviMin, f);
    }

    /**
     * DIGITAL -> ANALOG
     */
    private double dac(double[] x, int i) {
        double xi = x[i];
        if (xi != xi) {
			//xi = noise(); //HACK
			throw new UnsupportedOperationException();
		} else
            return unitize(xi);
    }

    private boolean updateTime() {
        var NOW = game.time;
        long now = NOW.start();
        long nowPrev = this.now;
        float rDur = game.dur() * durs.intValue();
        if (now < nowPrev + rDur)
            return false;

        //System.out.println(">" + NOW);
        this.now = now;
        this.durReflex = rDur;

        for (var t : actionTables)
            t.stretchDurs = rDur;

        return true;
    }

    @Deprecated
    public void afterFrame(Runnable runnable) {
        game.afterFrame(runnable);
    }



    private static String ts(long s, long e) {
        return "[" + s + ".." + e + "]";
    }

    private float scalar(Truth t) {
        return t != null ?
                t.freq() :
                0.5f /*NaN*/ /*noise()*/;
    }

    private class ReflexActionTable extends MutableTasksBeliefTable {

        final FloatMeanEwma dex = new FloatMeanEwma().period(4, 16).with(0);
        private final AbstractAction action;

        /** action vector index range */
        final int from, to;

        ReflexActionTable(AbstractAction a, int from, int to, Game g) {
            super(a.term(), false, trainGoals);
            this.action = a;
            sharedStamp = g.nar.evidence();
            this.from = from; this.to = to;
        }

        @Override
        public Truth taskTruth(float f, double evi) {
            return new MutableTruth(f, evi);
            //return PreciseTruth.byEvi(f, evi);
        }

        void adc(double freq, double[] action) {
            action[from] = freq;
//            int poles = poles();
//            assertUnitized(freq);
//
//            switch (poles) {
//                case 1 -> action[from] = freq;
//                case 2 -> {
//
//
//                    var digitizer =
//                            Digitize.FuzzyNeedle;
//                    //Digitize.BinaryNeedle;
//
//                    float FREQ = (float) freq;
//                    for (int d = 0; d < poles; d++)
//                        action[from + d] = digitizer.digit(FREQ, d, poles);
//
////                    //contrast exponent curve
////                    if (actionContrast!=1) {
////                        for (int d = 0; d < actionDigitization; d++)
////                            y[j + d] = Math.pow(y[j + d], actionContrast);
////                    }
//
//                    //normalize so each action's components sum to 1
//                    normalize(action, from, to, 0, sum(action, from, to));
//
//                    //normalize to max component value
//                    //Util.normalize(action, from, to, 0, Util.max(from, to, action));
//
//                }
//                default -> throw new UnsupportedOperationException();
//            }
        }

        double dac(double[] action) {
            return action[from];
//            return switch (poles()) {
//                case 1 -> action[from];
//                case 2 ->
//                    undigitizeWeightedMean(action, from, poles());
//                    //undigitizeAbsNorm(action, from, poles());
//                default -> throw new UnsupportedOperationException();
//            };
        }

//        static double undigitizeAbsNorm(double[] y, int i, int poles) {
//            assert(poles==2); //TEMPORARY
//            double l = y[i], r = y[i+1];
//            double min = Util.min(l, r);
//            l += -min;
//            r += -min;
//            return (l*0 + r*1)/(l+r);
////            double a = Util.max(Math.abs(y[i]), Math.abs(y[i+1]));
////
////            double l = Util.normalize(y[i], -a, +a);
////            double r = Util.normalize(y[i+1], -a, +a);
////            return (l*0 + r*1)/(2);
//
//        }
        /**
         * digital -> analog
         */
        static double undigitizeWeightedMean(double[] y, int i, int poles) {
            double x = 0, sum = 0;
            for (int d = 0; d < poles; d++) {
                double D = y[i + d];
                //D = Fuzzy.unpolarize(D);
//                D = Util.unitize(D);
                D = (D < 0) ? 0 : D; //D = Math.max(0, D);

                //D = Math.max(0, D)/max;
                //D = Util.normalize(D, min, max);
                float value = ((float) d) / (poles - 1);
                x += value * D;
                sum += D;
            }
            if (sum > Float.MIN_NORMAL)
                x /= sum;
            else
                x = 0.5f;
            return x;
        }
//        public double[] joinSoftmax(double[] y, double[] tgt) {
//            final DecideSoftmax decide = new DecideSoftmax(0.1f, rng);
//            for (int i = 0, k = 0; k < tgt.length; ) {
//                int index = decide.applyAsInt(Util.toFloat(y, i, i + actionDigitization));
//                tgt[k++] = ((float) index) / (actionDigitization - 1);
//                i += actionDigitization;
//            }
//            return tgt;
//        }

//        private int poles() {
//            return to-from;
//        }

        public void goal(double[] action, long s, long e, float strength, float pri, double eviMin, Focus f) {
            double a = dac(action);
            if (a == a)
                goal(unitizeSafe(a), s, e, strength, pri, eviMin, f);
            else
                tasks.clear();
        }

        private void goal(double f, long s, long e, float strength, float pri, double eviMin, Focus g) {
            float freq = (float) roundSafe(f, action.resolution());

            double dexMean = this.dex.acceptAndGetMean(action.dexterity());
            double evi = Math.max(eviMin, strength * c2e(dexMean));

            g.remember(setOrAdd(freq, evi, s, e, pri, g.nar));
        }

        void actionIn(long s, long e, double[] actions, NAR nar) {
            var t = ((AbstractGoalAction)action).concept
                    .beliefs().truth(s, e, durReflex, nar);
            adc(t != null ? truthFeedback(t) : missing(), actions);
        }

//		private double f(float momentum, double next, MutableTasksBeliefTable t) {
//			if (momentum <= 0) return next;
//
//			SerialTask prev = t.last();
//			return momentum > 0 ? lerpSafe(momentum, next, prev != null ? prev.freq() : next) : next;
//		}

    }

//    private class SensorInput implements Consumer<Termed> {
//        private final float[] timeLens;
//        private final SensorBuilder s;
//        private final float offset;
//        private Concept c;
//
//        SensorInput(float[] timeLens, SensorBuilder s, float offset) {
//            this.timeLens = timeLens;
//            this.s = s;
//            this.offset = offset;
//        }
//
//        @Override
//        public void accept(Termed t) {
//
//            float pastpast = 0; //start at now
//
//            for (int p = 0; p < timeLens.length; p++) {
//                float pastEnd = pastpast;
//                float pastStart = timeLens[p];
//
//                s.in(new SensorHistoryInput(
//                        t, pastStart, pastEnd, p));
//
//                pastpast = pastStart;
//            }
//        }
//
//        public final int size() {
//            return s.size();
//        }
//
//        private class SensorHistoryInput implements FloatSupplier {
//            final int p;
//            private final Termed t;
//
//            /**
//             * in durs ago
//             */
//            private final float pastStart, pastEnd;
//
//            SensorHistoryInput(Termed t, float pastStart, float pastEnd, int p) {
//                this.t = t;
//                this.p = p;
//                assert (pastStart > pastEnd);
//                this.pastStart = pastStart;
//                this.pastEnd = pastEnd;
//            }
//            @Override
//            public float asFloat() {
//                Concept C = c;
//                if (p == 0) {
//                    if ((C == null || C.isDeleted()))
//                        c = C = game.nar.conceptualizeDynamic(t);
//                }
//
//                return scalar(C != null ?
//                        input(C, pastStart, pastEnd, offset) :
//                        null);
//            }
//
//
//        }
//
//
//    }

    private class ConceptRef implements Termed {
        public final Termed t;
        @Nullable private Concept c;

        ConceptRef(Termed t) {
            this.t = t;
        }
        private @Nullable Concept concept(boolean resolveIfMissing) {
            return (resolveIfMissing && missingConcept()) ? (c = _concept()) : c;
        }

        private @Nullable Concept _concept() {
            return game.nar.conceptualizeDynamic(t);
        }

        private boolean missingConcept() {
            return (c == null || c.isDeleted());
        }

        @Override
        public Term term() {
            return t.term();
        }
    }

    private class SensorInputter implements FloatSupplier {
        final int p;

        /** in durs */
        private final float start;
        private final ConceptRef c;

        SensorInputter(ConceptRef c, float start, int p) {
            this.c = c;
            this.p = p;
            this.start = start;
        }

        @Override
        public float asFloat() {
            return scalar(truth(c.concept(p==0)));
        }

        private @Nullable Truth truth(@Nullable Concept C) {
            return C != null ?
                input(C, start) :
                null;
        }

        /**
         * past is number of durations ago to begin (left-aligned) sensed frame
         */
        private Truth input(Concept c, float offsetDurs) {
            float dur = durReflex;
            long s = now + round(offsetDurs * dur);
            long e = s + round(dur);
            //System.out.println(c.term + " " + ts(s, e));
            return c.beliefs().truth(s, e, durReflex, game.nar);
        }
    }
}