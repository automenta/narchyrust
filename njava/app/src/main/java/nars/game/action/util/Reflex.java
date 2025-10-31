package nars.game.action.util;

import jcog.Is;
import jcog.Log;
import jcog.Research;
import jcog.Util;
import jcog.agent.Agent;
import jcog.agent.SensorBuilder;
import jcog.agent.SensorTensor;
import jcog.data.list.Lst;
import jcog.event.Off;
import jcog.math.FloatMeanEwma;
import jcog.signal.FloatRange;
import jcog.signal.IntRange;
import jcog.tensor.rl.pg.PPO;
import jcog.thing.SubPart;
import nars.*;
import nars.control.Cause;
import nars.game.FocusLoop;
import nars.game.Game;
import nars.game.action.AbstractAction;
import nars.game.action.AbstractGoalAction;
import nars.game.sensor.ScalarSensor;
import nars.game.sensor.Sensor;
import nars.game.sensor.VectorSensor;
import nars.table.BeliefTables;
import nars.table.dynamic.MutableTasksBeliefTable;
import nars.term.Termed;
import nars.truth.MutableTruth;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.google.common.collect.Streams.stream;
import static java.lang.Float.NaN;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static jcog.Util.lerpSafe;
import static jcog.Util.unitizeSafe;
import static nars.Op.GOAL;
import static nars.TruthFunctions.c2e;

/**
 * Low-level/instinct sensorimotor reflexes.
 * NAgent Reinforcement Learning Algorithm Accelerator
 * trains and is trainable in cooperation with other action generators.
 * (multiple instances of this can cooperate with each other)
 *
 *      * TODO use AgentBuilder
 */
@Research
@Is("Reinforcement_learning") public class Reflex implements SubPart<Game>, Consumer<Game> {

    /**
     *  TODO just pass a model as constructor parameter
     *
     *  Table offers more timing flexibility, such as different or multiple durations,
     *      updating a Goal task in an overlay sensor belief table that can compete with other goals.
     *  Direct accesses the current actions and rewards directly from the Game,
     *      and decides the Game's final actions directly.
     * */
    @Deprecated public enum ModelOption { TABLE,  DIRECT }

    public final ReflexModel model;

    public Reflex(Game game) {
        this.model = new DirectStrategy(game);
        game.add(this);
    }

    public Reflex(Game game, TableModel.Builder builder) {
        this.model = new TableModel(builder, game);
        game.add(this);
    }

    @Override
    public void startIn(Game g) {
        model.startIn(g);
    }

    @Override
    public void stopIn(Game g) {
        model.stopIn(g);
    }

    @Override
    public void accept(Game g) {
        model.accept(g);
    }

    /**
     * Shared helper: converts a Truth to a scalar value.
     */
    public static float truthFeedback(Truth t) {
        return (t == null) ? NaN : t.freq();
    }

    public Agent agent() {
        return model.agent;
    }

    /**
     * An abstract base class that implements the common RL loop:
     *  1) senseIn()
     *  2) actionIn()
     *  3) rewardIn()
     *  4) computeNextAction(...)
     *  5) actionOut()
     *
     */
    public static abstract class ReflexModel implements Consumer<Game> {

        protected final Logger logger = Log.log(getClass());

        protected final Game game;
        protected Off onFrame;           // subscription to game frames
        protected Cause cause;           // cause reference in NAR

        // RL agent
        protected Agent agent;
        protected int inDim, outDim;

        // Sensor input
        protected SensorTensor sensors;
        protected double[] input;

        // Actions: previous and computed next action vectors
        protected double[] actionPrev;
        public double[] actionNext;

        // Reward tracking
        protected double reward = NaN;
        protected BigDecimal rewardSum = BigDecimal.ZERO;
        protected int rewardSumIters;

        // Timing: current time and duration interval (e.g. reflex period)
        protected long now = Long.MIN_VALUE;
        protected float durReflex = 1;

        // Enabled flag
        public final AtomicBoolean enabled = new AtomicBoolean(true);

        public ReflexModel(Game game) {
            this.game = game;
        }

        public void startIn(Game g) {
            this.cause = g.nar.causes.newCause(this);
            this.onFrame = g.onFrame(this);
        }

        public void stopIn(Game g) {
            if (onFrame != null) {
                onFrame.close();
                onFrame = null;
            }
        }

        // --------------------------------------------------------
        //  Consumer<Game> Method (Main Loop)
        // --------------------------------------------------------
        @Override
        public void accept(Game g) {
            if (agent == null || !enabled.get() || !updateTime()) return;

            // 1) Read sensor inputs.
            senseIn();

            // 2) (Optionally) process previous action information.
            actionIn();

            // 3) Measure reward.
            rewardIn();

            // 4) Compute the next action using the strategy-specific method.
            computeNextAction(input, reward);

            // 5) Commit the new action.
            actionOut();
        }

        protected boolean updateTime() {
            this.now = game.time();
            this.durReflex = game.dur();
            return true;
        }

        // --------------------------------------------------------
        //  Step 1: senseIn()
        // --------------------------------------------------------
        protected void senseIn() {
            if (sensors != null)
                sensors.update().writeTo(input);
        }

        // --------------------------------------------------------
        //  Step 2: actionIn()
        // --------------------------------------------------------
        protected void actionIn() {
            // Default is no action—in ReflexStrategy, this is overridden.
        }

        // --------------------------------------------------------
        //  Step 3: rewardIn()
        // --------------------------------------------------------
        protected void rewardIn() {
            var r = _reward();
            this.reward = r;
            if (Double.isFinite(r)) {
                rewardSum = rewardSum.add(BigDecimal.valueOf(r));
            }
            rewardSumIters++;
        }


        public final FloatRange rewardMomentum = FloatRange.unit(0);
        public final FloatMeanEwma rewardEwma = new FloatMeanEwma();

        private double _reward() {
            var h = game.happiness();

            float m = rewardMomentum.asFloat();
            if (m > 0) {
                rewardEwma.alpha(1-m);
                return rewardEwma.acceptAndGetMean(h);
            }

            return h;
        }

        // --------------------------------------------------------
        //  Step 4: computeNextAction() is abstract.
        // --------------------------------------------------------
        protected abstract void computeNextAction(double[] input, double reward);

        // --------------------------------------------------------
        //  Step 5: actionOut()
        // --------------------------------------------------------
        protected void actionOut() {
            // Default is a no-op.
        }

        // --------------------------------------------------------
        //  Public helper
        // --------------------------------------------------------
        public double rewardMean(boolean reset) {
            if (rewardSumIters == 0) return 0;
            var mean = rewardSum.doubleValue() / rewardSumIters;
            if (reset) {
                rewardSum = BigDecimal.ZERO;
                rewardSumIters = 0;
            }
            return mean;
        }

        public double rewardCurrent() {
            return reward;
        }
    }

    // ============================================================
    //  1) ReflexStrategy
    // ============================================================
    /**
     * A Reflex-based RL approach that creates and updates belief tables (one per action).
     * It overrides the timing update to enforce a minimum interval before updating.
     *
     *  * Default timing model:
     *  * [Previous Dur]			[This Dur]				[Next Dur]
     *  * prevStart...nowStart	nowStart...nextStart	nextStart...nextEnd
     *  * * reward				*sensorsNow
     *  * * actionsPrev			*actionsNow
     */
    public static final class TableModel extends ReflexModel {

        public final FloatRange train = new FloatRange(0.95f, 0, 1);
        private final IntRange durs = new IntRange(1, 1, 32);
        private final Builder builder;
        private final Lst<Consumer<SensorBuilder>> sensorBuilders = new Lst<>();
        private List<AbstractGoalAction> actionConcepts;
        private ReflexActionTable[] actionTables;

        private static final Logger log = Log.log(TableModel.class);

        private final static float minEviFactor = 3;

        public TableModel(Builder builder, Game g) {
            super(g);
            this.builder = builder;
        }

        @Override
        public void startIn(Game g) {
            super.startIn(g);

            var sb = new SensorBuilder();
            builder.add(g, this);
            for (var c : sensorBuilders)
                c.accept(sb);

            sensors = sb.sensor();
            inDim = sensors.volume();
            input = new double[inDim];

            // Build action references.
            actionConcepts = builder.actions();
            outDim = actionConcepts.size();
            actionPrev = new double[outDim];
            actionNext = new double[outDim];

            // Initialize the RL agent.
            this.agent = builder.agent.value(inDim, outDim);
            log.info("ReflexStrategy agent: {} with inDim={}, outDim={}", agent, inDim, outDim);

            // Create a belief table (ReflexActionTable) for each action.
            actionTables = new ReflexActionTable[outDim];
            for (var i = 0; i < outDim; i++) {
                var a = actionConcepts.get(i);
                var c = a.concept;
                var table = new ReflexActionTable(a, i, g);
                ((BeliefTables) c.goals()).add(table);
                actionTables[i] = table;
            }
        }

        /**
         * Override updateTime() to enforce that updates occur only after a minimum interval.
         * The interval is computed as: game.dur() * durs.intValue().
         */
        @Override
        protected boolean updateTime() {
            long now = game.time();
            if (now < this.now + (long) (durReflex * durs.intValue()))
                return false;

            this.now = now;
            this.durReflex = durReflex * durs.intValue(); //HACK

            for (var t : actionTables)
                t.stretchDurs = durReflex;

            return true;
        }

        @Override
        protected void actionIn() {
            var s = now - round(durReflex);
            var e = now;
            for (var t : actionTables)
                t.actionIn(s, e, actionPrev, game.nar);
        }

        @Override
        protected void computeNextAction(double[] input, double reward) {
            // ReflexStrategy computes its next action via its agent.
            agent.act(actionPrev, (float) reward, input, agent.actionNext);
        }

        @Override
        protected void actionOut() {
            var trainVal = train.floatValue();
            if (trainVal <= 0f) return;

            var pri = game.nar.priDefault(GOAL);
            var eviMin = game.nar.eviMin() * minEviFactor;
            var s = now;
            var e = now + round(durReflex);

            var focus = game.focus();
            var actionNext = agent.actionNext;
            System.arraycopy(actionNext, 0, this.actionNext, 0, this.actionNext.length);
            for (var t : actionTables)
                t.goal(actionNext, s, e, trainVal, pri, eviMin, focus);


            // Update actionPrev for the next cycle.
            System.arraycopy(actionNext, 0, actionPrev, 0, this.actionNext.length);
        }

        /* Allows ReflexBuilder to attach sensor-creation routines. */
        void addTable(Consumer<SensorBuilder> c) {
            sensorBuilders.add(c);
        }

        private class ReflexActionTable extends MutableTasksBeliefTable {

            final FloatMeanEwma dex = new FloatMeanEwma().period(4, 16).with(0);
            private final AbstractAction action;
            final int index;
            float stretchDurs;

            ReflexActionTable(AbstractAction a, int index, Game g) {
                super(a.term(), false, 1);
                this.action = a;
                this.index = index;
                sharedStamp = g.nar.evidence();
            }

            @Override
            public Truth taskTruth(float f, double evi) {
                return new MutableTruth(f, evi);
            }

            void actionIn(long s, long e, double[] actions, NAR nar) {
                if (!(action instanceof AbstractGoalAction ga)) return;
                var t = ga.concept.beliefs().truth(s, e, stretchDurs, nar);
                var val = (t != null ? truthFeedback(t) : Double.NaN);
                actions[index] = val;
            }

            void goal(double[] newActions, long s, long e, float strength, float pri, double eviMin, Focus f) {
                var a = newActions[index];
                if (Double.isFinite(a)) {
                    var freq = Util.roundSafe(unitizeSafe(a), action.resolution());
                    var dexMean = dex.acceptAndGetMean(action.dexterity());
                    var evi = max(eviMin, strength * c2e(dexMean));
                    f.remember(setOrAdd((float) freq, evi, s, e, pri, f.nar));
                } else {
                    tasks.clear();
                }
            }
        }

        /**
         * The ReflexBuilder collects configuration for the ReflexStrategy.
         */
        public static final class Builder {
//            public static final ReflexBuilder RANDOM =
//                    new ReflexBuilder("Random", Agents::Random, false, false, 1);

            String name;
            final IntIntToObjectFunction<Agent> agent;
            final float[] history;
            final boolean senseActions;
            final boolean senseRewards;
            private final List<AbstractGoalAction> actions = new Lst<>();

            public Builder(@Nullable String name,
                           IntIntToObjectFunction<Agent> agent,
                           boolean senseActions,
                           boolean senseRewards,
                           float... history) {
                if (history.length < 1) {
                    throw new UnsupportedOperationException("History must have at least 1 entry");
                }
                this.name = name;
                this.agent = agent;
                this.history = history;
                this.senseActions = senseActions;
                this.senseRewards = senseRewards;
            }

            public List<AbstractGoalAction> actions() {
                return actions;
            }

            @Override
            public String toString() {
                return (name != null ? name : agent.toString()) 
                       + "_H=" + Arrays.toString(history);
            }

            /**
             * Incorporate game sensors and actions.
             */
            void add(Game g, TableModel s) {
                var allActions = g.actions.stream()
                        .filter(a -> a instanceof AbstractGoalAction)
                        .map(a -> (AbstractGoalAction) a)
                        .filter(s::includeAction)
                        .toList();
                actions.addAll(allActions);
                if (actions.isEmpty())
                    throw new UnsupportedOperationException();

                s.addTable(sensorBuilder -> {
                    // Add sensors from game sensors.
                    s.sensors(sensorBuilder, g.sensors.stream().filter(s::includeSensor),
                            0, history);
                    // Optionally add rewards.
                    if (senseRewards) {
                        s.sensors(sensorBuilder, g.rewards.stream(),
                                NAL.temporal.GAME_REWARD_SHIFT_DURS, history);
                    }
                    // Optionally add actions.
                    if (senseActions) {
                        s.sensors(sensorBuilder, allActions.stream(),
                                NAL.temporal.GAME_ACTION_SHIFT_DURS, history);
                    }
                });
            }
        }

        // ------------------------------------------------------------
        //  Utility methods for sensor construction.
        // ------------------------------------------------------------
        private boolean includeAction(AbstractAction action) {
            return true;
        }
        private boolean includeSensor(Sensor sensor) {
            return true;
        }

        void sensors(SensorBuilder sb,
                     Stream<? extends FocusLoop> loops,
                     float offset, float[] hist) {
            loops.flatMap(x -> stream(x.components()))
                 .forEach((t) -> {
                     var c = new TermedConcept((Termed)t);
                     for (var p = 0; p < hist.length; p++) {
                         var start = -hist[p] + offset;
                         sb.in(new SensorReader(c, start, p));
                     }
                 });
        }

        private static final class TermedConcept implements Termed {
            public final Termed t;
            @Nullable
            private Concept c;

            TermedConcept(Termed t) { this.t = t; }
            @Override
            public Term term() { return t.term(); }

            private @Nullable Concept concept(boolean resolveIfMissing, NAR nar) {
                if ((c == null || c.isDeleted()) && resolveIfMissing) {
                    c = nar.conceptualizeDynamic(t);
                }
                return c;
            }
        }

        private class SensorReader implements jcog.math.FloatSupplier {
            final TermedConcept c;
            final float start;
            final int index;

            SensorReader(TermedConcept c, float start, int p) {
                this.c = c;
                this.start = start;
                this.index = p;
            }

            @Override
            public float asFloat() {
                var cc = c.concept(index == 0, game.nar);
                if (cc == null) return NaN;
                var dur = durReflex;
                var s = now + round(start * dur);
                var e = s + round(dur);
                var t = cc.beliefs().truth(s, e, durReflex, game.nar);
                return (t != null ? t.freq() : 0.5f);
            }
        }
    }

    /**
     * A policy-gradient–based filter approach.
     * This strategy avoids creating any belief tables.
     */
    public static final class DirectStrategy extends ReflexModel {
        public final FloatRange strength = FloatRange.unit(1);
        public final AtomicBoolean updateHard = new AtomicBoolean(true);

        private double[] localInput;
        public double[] localActionPrev;
        private double[] localActionNext;
        private double[] actionDelta;

        public DirectStrategy(Game g) {
            super(g);
            g.actions.filter(this);
        }

        @Override
        public void startIn(Game g) {
            super.startIn(g);

            // Build input from available sensors.
            localInput = buildInput(g);
            inDim = localInput.length;

            // The output dimension is the number of actions.
            outDim = g.actions.components.size();
            localActionPrev = new double[outDim];
            localActionNext = new double[outDim];
            actionDelta = new double[outDim];

            // TODO specifyc Agent via (inputsize,outputSize)->Agent builder function as constructor parameter
            var scale = 5 * Math.max(inDim, outDim);

            this.agent =
                    new PPO(inDim, outDim, scale, scale, 8).agent();
                    //new Reinforce(inDim, outDim, scale, 8).agent();
                    //new ReinforceDNC(inDim, outDim, scale, 8, 8, 16, 8, DNCMemory.EraseMode.SCALAR).agent();
                    //new ReinforceODE.ReinforceNeuralODE(inDim, outDim, scale, 4, 8).agent();
                    //new StreamAC(inDim, outDim, scale, scale).agent();
                    //new DDPG(inDim, outDim, scale, scale).agent();
            //Agents.BayesZero(inputSize, outputSize, 4, 8);
            //Agents.CMAESZero(inputSize, outputSize);
            //Agents.CMAES(inputSize, outputSize);
            //new Reinforce(inputSize, outputSize, hiddenPolicy, episodeLen) {
//                //ew SAC(inputSize, outputSize, hiddenPolicy, hiddenValue, episodeLen);
            //new DDPGAuto(inputSize, outputSize, hiddenPolicy, hiddenValue);
//                //new VPG(inputSize, outputSize, hiddenPolicy, hiddenValue, episodeLen) {
//                //new ReinforceODE.ReinforceNeuralODE(inputSize, outputSize, hiddenPolicy, 5, episodeLen) {
//                    @Override
//                    protected double[] sampleAction(Tensor mean, Tensor sigma, FloatRange actionNoise) {
//                        if (residual)
//                            residualMean(mean, g);
//                        return super.sampleAction(mean, sigma, actionNoise);
//                    }
//                };

            this.actionPrev = localActionPrev;
            this.actionNext = localActionNext;

            logger.info("Reflex agent: {} with inDim={}, outDim={}", agent, inDim, outDim);
        }

        @Override
        protected void senseIn() {
            // Rebuild input from sensors every cycle.
            this.input = localInput = buildInput(game);
        }

        @Override
        protected void actionIn() {
            // Fetch the current action levels.
            var arr = actions(game);
            System.arraycopy(arr, 0, localActionPrev, 0, outDim);
        }

        @Override
        protected void computeNextAction(double[] input, double reward) {
            agent.apply(null, localActionPrev, (float) reward, input, localActionNext);
        }

        @Override
        protected void actionOut() {
            commit(localActionNext, game);
            System.arraycopy(localActionNext, 0, localActionPrev, 0, outDim);
        }

        private double[] buildInput(Game g) {
            var result = new DoubleArrayList();
            for (var sensor : g.sensors.sensors) {
                switch (sensor) {
                    case ScalarSensor ss -> result.add(ss.value);
                    case VectorSensor vs -> result.addAll(vs.values());
                    case null, default -> throw new UnsupportedOperationException("Unhandled sensor type: " + sensor);
                }
            }
            if (g.rewards.size() > 1) {
                for (var r : g.rewards)
                    result.add(r.rewardElseZero());
            }
            return result.toArray();
        }

        /** current actions */
        private double[] actions(Game g) {
            var arr = new double[g.actions.components.size()];
            var i = 0;
            for (var act : g.actions.components) {
                if (act instanceof AbstractGoalAction ga) {
                    arr[i++] = ga.goalInternal();
                } else {
                    throw new UnsupportedOperationException("Only AbstractGoalAction supported");
                }
            }
            return arr;
        }

        private void commit(double[] next, Game g) {
            var s = strength.asFloat();
            var hard = updateHard.get();
            var conf = g.nar.confDefault(GOAL);
            var i = 0;
            for (var act : g.actions.components) {
                if (!(act instanceof AbstractGoalAction aa)) throw new UnsupportedOperationException();

                var res = aa.resolution();
                var prev = localActionPrev[i];
                var newVal = next[i];
                actionDelta[i] = newVal - prev;
                var nextI = hard
                        ? (g.rng().nextBoolean(s) ? newVal : prev)
                        : lerpSafe(s, prev, newVal);
                nextI = Util.roundSafe((float) nextI, res);

                aa.goalSet((float) nextI, conf);

                i++;
            }
        }
    }

    public void afterFrame(Runnable runnable) {
        model.game.afterFrame(runnable);
    }

    public double reward() {
        return model.reward;
    }
}
