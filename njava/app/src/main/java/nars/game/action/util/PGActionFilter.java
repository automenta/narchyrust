package nars.game.action.util;

import jcog.Fuzzy;
import jcog.Research;
import jcog.TODO;
import jcog.Util;
import jcog.agent.Agent;
import jcog.signal.FloatRange;
import jcog.tensor.Tensor;
import jcog.tensor.rl.pg.PPO;
import nars.NAR;
import nars.game.Game;
import nars.game.action.AbstractGoalAction;
import nars.game.sensor.ScalarSensor;
import nars.game.sensor.VectorSensor;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static nars.Op.GOAL;

/**
 * TODO
 * For an algorithm that functions best as a filter or enhancement to an existing decision-making process, rather than completely replacing it, we should consider approaches that can learn to make incremental improvements or corrections. Here are some suitable options:
 * <p>
 * Residual Policy Learning (RPL):
 * RPL is designed to learn a residual policy that improves upon an existing base policy. It's well-suited for scenarios where you have a reasonably good baseline policy (in this case, your existing action selection mechanism) and want to refine it.
 * Advantages:
 * <p>
 * Can leverage existing knowledge encoded in the base policy
 * Learns to make targeted improvements
 * Can potentially converge faster than learning from scratch
 * <p>
 * <p>
 * Actor-Critic with Policy Gradient:
 * A lightweight actor-critic model could be used to learn adjustments to the existing actions. The actor would output small deltas to be added to the original actions, while the critic would evaluate the state-action pairs.
 * Advantages:
 * <p>
 * Can learn continuous action adjustments
 * Critic provides value estimation, potentially stabilizing learning
 * <p>
 * <p>
 * Soft Actor-Critic (SAC):
 * SAC is an off-policy algorithm that could learn to make soft adjustments to the existing policy. It's known for its sample efficiency and stability.
 * Advantages:
 * <p>
 * Entropy regularization encourages exploration
 * Off-policy nature allows it to learn from past experiences efficiently
 * <p>
 * <p>
 * Twin Delayed Deep Deterministic Policy Gradient (TD3):
 * TD3 is an improvement over DDPG, designed to work well with continuous action spaces. It could learn to output action adjustments.
 * Advantages:
 * <p>
 * Addresses overestimation bias in value functions
 * Works well with continuous action spaces
 * <p>
 * <p>
 * Proximal Policy Optimization (PPO) with a Custom Objective:
 * A modified version of PPO could be used where the objective function is designed to optimize incremental improvements over the base policy.
 * Advantages:
 * <p>
 * Stable learning process
 * Can be adapted to focus on incremental improvements
 * <p>
 * <p>
 * Meta-Learning Algorithms (e.g., MAML):
 * Meta-learning approaches could be used to learn a "correction function" that quickly adapts to different scenarios or game states.
 * Advantages:
 * <p>
 * Can potentially generalize well to new situations
 * Learns to learn, potentially allowing quick adaptation
 *
 * TODO optional action momentum parameter
 */
@Research
public class PGActionFilter implements Consumer<Game> {

    public final FloatRange strength = FloatRange.unit(1);
    public final AtomicBoolean updateHard = new AtomicBoolean(true);

    public final Agent agent;
    public final double[] actionSuggest, actionNext;

    /** measure of how much this influenced/intervened in the result */
    public final double[] delta;

    private final int inputSize, outputSize;
    private final boolean includeRewardsIfMultiple = true;
    private final boolean includeActionCurrent = false;
    private final boolean includeActionDexterity = false;
    private final Game game;
    private double reward;

//    /** TODO residual reinforcement mode:
//     *     whether to learn an action-delta, or the entire action */
//    public final boolean residual = false;

    public NAR nar() {
        return game.nar;
    }

    public PGActionFilter(Game g, int episodeLen) {
        this.game = g;
        this.inputSize = input(g).length;
        int outputs = g.actions.components.size();
        this.outputSize = includeActionDexterity ? outputs * 2 : outputs;

        var scale = 32;
        @Deprecated int hiddenPolicy = scale * inputSize; //Math.max(outputSize, Fuzzy.mean(inputSize, outputSize));
        @Deprecated int hiddenValue = scale * inputSize;

        var a =
                //new Reinforce(inputSize, outputSize, hiddenPolicy, episodeLen) {
                new PPO(inputSize, outputSize, hiddenPolicy, hiddenValue, episodeLen) {
                    public final FloatRange actionDeltaFactor = new FloatRange(0.0f, 0, 10);

                    @Override
                    protected double reward(double reward) {
                        //penalize action delta
                        var r = super.reward(reward);
                        if (actions.size() < 2) return r;
                        var next = actions.getLast();
                        var prev = actions.get(actions.size()-2);

                        var actionDeltaLoss = next.mse(prev).mul(actionDeltaFactor.asFloat());
                        return r - actionDeltaLoss.scalar();
                    }
                };
                //Agents.BayesZero(inputSize, outputSize, 4, 8);
                //Agents.CMAESZero(inputSize, outputSize);
                //Agents.CMAES(inputSize, outputSize);
                //new ReinforceODE.ReinforceNeuralODE(inputSize, outputSize, hiddenPolicy, 4, episodeLen);
                //new Reinforce(inputSize, outputSize, hiddenPolicy, episodeLen) {
//                //ew SAC(inputSize, outputSize, hiddenPolicy, hiddenValue, episodeLen);
//                //new DDPG(inputSize, outputSize, hiddenPolicy, hiddenValue) {
                //new StreamAC(inputSize, outputSize, hiddenPolicy, hiddenValue);
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

        this.agent =
                //a instanceof AbstractPG aa ? aa.agent() : (Agent)a;
                a.agent();

        delta = new double[outputs];
        actionSuggest = new double[g.actions.components.size()];
        actionNext = new double[g.actions.components.size()];
    }

    /** "residual reinforcement learning" */
    private void residualMean(Tensor mean, Game g) {
        int n = actionSuggest.length;
        var mm = mean.array();
        for (var i = 0; i < n; i++) {
            float res = g.actions.components.get(i).resolution();
            var si = actionSuggest[i];
            if (si==si)
                mm[i] = Util.clampSafePolar(Util.round(mm[i] + Fuzzy.polarize(si),
                        res*2 /* x2 since in polar mode */));
        }
    }

    private static double feedback(double before, double after, boolean hard, float strength, Game g) {
        double feedback;
        if (hard) {
            feedback = g.rng().nextBoolean(strength) ? after : before;
        } else {
            feedback = Util.lerpSafe(strength, before, after); //TODO maybe modulate strength by (normalized)? conf=dexterity
        }
        return feedback;
    }

    @Override
    public void accept(Game g) {
        var input = input(g);

        var actionPrev =
            actions(g, false, false, false);

        //var rewardPrev = this.reward;
        var reward = reward(g);

        pre(g);

        agent.apply(null, actionPrev, (float) reward, input, actionNext);

        commit(actionNext, g);

        this.reward = reward;
    }

    private double[] input(Game g) {
        var input = new DoubleArrayList();

        for (var sensor : g.sensors.sensors) {
            if (sensor instanceof ScalarSensor ss) {
                input.add(ss.value);
            } else if (sensor instanceof VectorSensor vv) {
                input.addAll(vv.values());
            } else
                throw new UnsupportedOperationException();
        }

        if (includeActionCurrent)
            actions(input, includeActionDexterity, true, true, g);
        if (includeRewardsIfMultiple && g.rewards.size()>1)
            rewards(input, g);

        return input.toArray();
    }

    private void rewards(DoubleArrayList input, Game g) {
        g.rewards.forEach(r -> input.add(r.rewardElseZero()));
    }

    private double[] actions(Game game, boolean dexterity, boolean currentOrPrev, boolean polarize) {
        var input = new DoubleArrayList();
        actions(input, dexterity, currentOrPrev, polarize, game);
        return input.toArray();
    }

    private void actions(DoubleArrayList input, boolean dexterity, boolean currentOrPrev, boolean polarize, Game g) {
        int n = 0;
        for (var action : g.actions.components) {
            if (action instanceof AbstractGoalAction aa) {
                var _f = action(aa, currentOrPrev, g);
                var f = polarize ? Fuzzy.polarize(_f) : _f;
                input.add(f);
                n++;
            } else
                throw new UnsupportedOperationException();
        }
        if (dexterity) {
            if (!currentOrPrev)
                throw new TODO("prev");

            double[] dexterities = new double[n];
            int j = 0;
            for (var action : g.actions.components) {
                if (action instanceof AbstractGoalAction aa)
                    dexterities[j++] = aa.dexterity();
                else
                    throw new UnsupportedOperationException();
            }
            Util.normalize(dexterities);
            input.addAll(dexterities);
        }

    }

    private float action(AbstractGoalAction aa, boolean currentOrPrev, Game g) {
        if (currentOrPrev)
            return aa.goalInternal();

        //TODO cache these values they will be uniform for all actions
        int dur = Math.round(g.dur());
        long s = g.time() - dur;
        return aa.concept.sensorBeliefs().freq(s, s + dur, 0 /*g.nar().dur()*/, g.nar());
    }

    private double reward(Game game) {
        return game.happiness();
    }

    private void pre(Game game) {
        int index = 0;
        for (var a : game.actions.components) {
            if (!(a instanceof AbstractGoalAction aa))
                throw new UnsupportedOperationException();

            actionSuggest[index++] = Util.roundSafe(aa.goalInternal(), a.resolution());
        }
    }

    private void commit(double[] actionNext, Game g) {
        var index = 0;
        var s = strength.asFloat();
        var hard = updateHard.getOpaque();
        var conf = g.nar.confDefault(GOAL);
        for (var a : g.actions.components) {
            if (!(a instanceof AbstractGoalAction aa))
                throw new UnsupportedOperationException();

            float res = a.resolution();
            double before = actionSuggest[index];
            if (before!=before)
                Util.nop();
            double next = actionNext[index];
            delta[index] = next - before;

            aa.goalSet( Util.roundSafe((float) feedback(before, next, hard, s, g), res), conf );
            index++;
        }
    }

    public double reward() {
        return reward;
    }

}