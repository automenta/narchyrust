package nars.deriver.impl;

import jcog.Util;
import jcog.data.list.Lst;
import jcog.sort.PrioritySet;
import nars.Deriver;
import nars.NALTask;
import nars.NAR;
import nars.Premise;
import nars.deriver.reaction.ReactionModel;
import nars.focus.util.TaskBagAttentionSampler;
import nars.premise.NALPremise;
import nars.time.clock.RealTime;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.IntFloatHashMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;

/**
 * A Deriver using Reinforcement Learning (Q-learning with eligibility traces)
 * to optimize premise selection. It seeds its premise queue from the Focus
 * and processes premises based on learned state values. Hyperparameters
 * are tunable online.
 */
public class RLTaskBagDeriver extends Deriver {

    private static final int DEFAULT_ITERATIONS = 5;
    private static final boolean ITERATION_TUNER_ENABLED = true;
    private static final float TRACE_THRESHOLD = 1e-6f;

    // --- Tunable Hyperparameters ---
    /** learning rate */
    public float alpha = 0.1f;

    /** discount factor */
    public float gamma = 0.9f;

    /** eligibility trace decay (0 to disable eligibility trace) */
    public float lambda = 0.7f;

    public int queueCapacity = 32;

    /** softmax temperature */
    public double temperature =
        //1;
        0.5f;

    float seedRate = 1/10f;
    // --- End Tunable Hyperparameters ---

    private final PrioritySet<TrackedPremise> queue;
    private final FeatureExtractor<Premise> featureExtractor;
    private final RewardModel rewardModel;
    private final RLModel rl;
    private final DeriverIterationTuner iterationTuner;
    private final TaskBagAttentionSampler taskSampler; // Sampler for seeding

    private TrackedPremise currentPremise = null;

    public RLTaskBagDeriver(ReactionModel model, NAR nar,
                            FeatureExtractor<Premise> featureExtractor,
                            RewardModel rewardModel,
                            RLModel rl) {
        super(model, nar);
        this.featureExtractor = Objects.requireNonNull(featureExtractor, "FeatureExtractor required");
        this.rewardModel = Objects.requireNonNull(rewardModel, "RewardModel required");
        this.rl = Objects.requireNonNull(rl, "RLModel required");
        this.queue = new PrioritySet<>(this.queueCapacity);
        this.taskSampler = new TaskBagAttentionSampler(this); // Initialize sampler

        this.iterationTuner = ITERATION_TUNER_ENABLED && nar.time instanceof RealTime ?
            new DeriverIterationTuner(DEFAULT_ITERATIONS) {
                @Override protected void next(int iter) { runIterations(iter); }
            } : null;
    }

    @Override
    protected final void next() {
        if (iterationTuner != null && focusHasTasks()) {
            iterationTuner.run(); // Uses runIterations via callback
        } else {
            runIterations(DEFAULT_ITERATIONS); // Run directly if tuner disabled
        }
    }

    /** Main RL derivation loop for a specified number of iterations. */
    private void runIterations(int iterations) {
        if (!focusHasTasks()) return; // Don't run if Focus is empty

        // Update tunable parameters if necessary (e.g., from FloatRange)
//        if (queue.capacity() != this.queueCapacity) {
//            queue.capacity(this.queueCapacity);
//        }

        if (this.rl instanceof TableRLModel tableModel)
            tableModel.setLambda(this.lambda);

        // 3. Process premises from the queue for the allowed number of iterations
        var processedCount = 0;
        while (processedCount < iterations) {

            //continuous seeding
            //TODO use a random countdown period rather than calling RNG each iteration
            if (queue.isEmpty() || rng.nextBooleanFast8(seedRate)) {
                if (queueSeeds(1)<=0)
                    break; //nothing
            }

            // Choose action (premise) using RL policy
            var p = rl.chooseAction(queue, rng);
            if (p == null) break;

            queue.remove(p); // Consume the chosen premise
            currentPremise = p; // Mark as currently processed

            // Execute base Deriver logic for this premise
            super.run(p.premise);

            updateRLModelForLastProcessed(p);

            currentPremise = null;

            processedCount++;
        }

        rl.commit();
        queue.clear();

        // 5. Process any deferred actions accumulated during the cycle
        later.run().clear();
    }

    /**
     * Samples tasks from the Focus and adds them as seed premises to the queue.
     *
     * @return
     */
    private int queueSeeds(int seedCount) {
        // Sample 'seedCount' tasks using the TaskBagAttentionSampler
        var seeds = taskSampler.seed(focus, seedCount, rng);
        var count = 0;
        for (int i = 0, size = seeds.size(); i < size; i++) {
            var seedTask = seeds.getAndNull(i); // Efficiently get and remove
            if (seedTask != null) {
                queueSeed(seedTask); // Add the seed task to the RL queue
                count++;
            }
        }
        return count;
    }

    /** Adds a seed task to the premise queue, evaluating its initial RL value. */
    private void queueSeed(NALTask task) {
        var p = NALPremise.the(task); // Create the seed premise
        var state = featureExtractor.apply(p);
        enqueue(new TrackedPremise(p, state), state); // Add to queue, prioritized by value
    }

    private boolean enqueue(TrackedPremise p, int state) {
        return queue.put(p, value(state));
    }

    protected float value(int state) {
        var q = rl.q(state);

        return (float) Util.softmax(q, temperature); //softmax: value's use in PrioritySet (implementing Roulette select) is mathematically equivalent to computing softmax over all Q-values and sampling directly.
    }

    /** Calculates reward and updates the RL model for the previously processed premise. */
    private void updateRLModelForLastProcessed(TrackedPremise lastProcessed) {
        var reward = rewardModel.calculateReward(lastProcessed, lastProcessed.derivedPremises, lastProcessed.derivedTasks);
        var nextBestAction = queue.itemHighest(); // Best available action for next state value estimate
        var nextState = (nextBestAction != null) ? nextBestAction.state : 0; // Default terminal/null state
        var nextValue = (nextBestAction != null) ? rl.q(nextState) : 0;

        rl.update(lastProcessed.state, reward, nextState, nextValue, alpha, gamma);
        // Derived lists in lastProcessed are now implicitly stale and will be GC'd
    }

    @Override
    protected void acceptPremise(Premise p) {
        var state = featureExtractor.apply(p);
        var tracked = new TrackedPremise(p, state);

        // Associate derived premise with the one currently being processed
        if (currentPremise != null) {
            currentPremise.derivedPremises.add(p);
        }

        enqueue(tracked, state);
    }

    @Override
    protected void onAdd(NALTask task) {
        super.onAdd(task);
        // Associate derived task with the one currently being processed
        if (currentPremise != null)
            currentPremise.derivedTasks.add(task);

        // Allow reward model for immediate feedback
        rewardModel.onTaskDerived(currentPremise, task);
    }

    @Override
    protected void onCommit() {
        if (iterationTuner != null) iterationTuner.update();
    }

    private boolean focusHasTasks() {
        var f = this.focus;
        return f != null && !f.attn.isEmpty();
    }

    private static class TrackedPremise {
        final Premise premise;
        final int state;
        final List<Premise> derivedPremises = new Lst<>(2);
        final List<NALTask> derivedTasks = new Lst<>(2);
        TrackedPremise(Premise premise, int state) { this.premise = premise; this.state = state; }
        @Override public int hashCode() { return premise.hashCode(); }
        @Override public boolean equals(Object o) { return this == o || (o instanceof TrackedPremise tp && premise.equals(tp.premise)); }
        @Override public String toString() { return "Tracked(" + premise + ", state=" + state + ")"; }
    }

    public interface FeatureExtractor<X> {
        int apply(X item);

        /** dimensionality */
        int size();
    }

    public static class FeatureExtractorBuilder<X> {
        private final List<Map.Entry<ToIntFunction<X>, Integer>> features = new Lst<>();
        private int totalBits;

        public final FeatureExtractorBuilder<X> add(Predicate<X> feature) {
            return add(x -> feature.test(x) ? 1 : 0, 1);
        }

        public FeatureExtractorBuilder<X> add(ToIntFunction<X> feature, int bits) {
            if (bits <= 0 || bits > 31) throw new IllegalArgumentException("Bits must be 1-31");
            if (totalBits + bits > 31) throw new IllegalArgumentException("Total bits exceed 31 for int state");
            features.add(Map.entry(feature, bits));
            totalBits += bits; return this;
        }
        public FeatureExtractor<X> build() {
            if (features.isEmpty())
                throw new UnsupportedOperationException();

            return new FeatureExtractor<>() {

                final List<Map.Entry<ToIntFunction<X>, Integer>> captured = List.copyOf(features);

                @Override
                public int apply(X item) {
                    int packed = 0, shift = 0;
                    for (var entry : captured) {
                        int bits = entry.getValue();
                        var value = entry.getKey().applyAsInt(item);
                        packed |= (value & ((1 << bits) - 1)) << shift;
                        shift += bits;
                    }
                    return packed;
                }

                @Override
                public int size() {
                    return totalBits;
                }
            };
        }
        public int getTotalBits() { return totalBits; }
    }

    public interface RewardModel {
        float calculateReward(TrackedPremise p, List<Premise> dP, List<NALTask> dT);
        default void onTaskDerived(TrackedPremise parent, NALTask derivedTask) {}
    }

    public interface RLModel {
        float q(int state);
        void update(int state, float reward, int nextState, float nextValue, float alpha, float gamma);
        TrackedPremise chooseAction(PrioritySet<TrackedPremise> actions, RandomGenerator rng);
        default void commit() {

        }
    }

    public static class BasicRewardModel implements RewardModel {
        private final float premiseReward, taskReward;
        public BasicRewardModel(float pR, float tR) { this.premiseReward = pR; this.taskReward = tR; }
        @Override public float calculateReward(TrackedPremise p, List<Premise> dP, List<NALTask> dT) {
            return (dP.size() * premiseReward) + (dT.size() * taskReward);
        }
    }

    /** implements Q-learning with eligibility traces:
     Stores Q-values in an IntFloatHashMap (state ID → Q-value).
     Updates Q-values using the TD-error: reward + gamma * nextValue - oldQ.
     Eligibility traces (lambda > 0) distribute credit to prior states, decaying by gamma * lambda.
     Action selection uses the queue’s priority order (softmax-based). */
    public static class TableRLModel implements RLModel {
        private final IntFloatHashMap q, eligibility;

        private float lambda;
        
        public TableRLModel(int capacity) {
            this.q = new IntFloatHashMap(capacity);
            this.eligibility = new IntFloatHashMap(capacity / 4);
        }

        @Override
        public void commit() {
            tracePrune();
        }

        public void setLambda(float l) { this.lambda = Util.unitize(l); }

        @Override public float q(int state) { return q.get(state); }

        @Override public void update(int state, float reward, int nextState, float nextValue, float alpha, float gamma) {
            var oldQ = q(state);
            var tdError = reward + gamma * nextValue - oldQ;
            if (this.lambda == 0) {
                q.put(state, oldQ + alpha * tdError);
            } else {
                eligibility.put(state, 1); // Replacing trace
                eligibility.updateValues((s_i, trace_i)->{
                    var q_i = q.get(s_i);
                    q.put(s_i, q_i + alpha * tdError * trace_i);
                    return trace_i * gamma * this.lambda;
                });


//                var it = eligibility.iterator();
//                while (it.hasNext()) {
//                    it.advance();
//                    int s_i = it.key(); float trace_i = it.value();
//                    float q_i = q.get(s_i);
//                    q.put(s_i, q_i + alpha * tdError * trace_i);
//                    float decayed = trace_i * gamma * this.lambda;
//                    if (decayed < TRACE_THRESHOLD) it.remove(); else it.setValue(decayed);
//                }
            }
        }

        private void tracePrune() {
            eligibility.values().removeIf(z -> z < TRACE_THRESHOLD);
        }

        @Override public TrackedPremise chooseAction(PrioritySet<TrackedPremise> actions, RandomGenerator rng) {
            return actions.get(rng);
        }
    }


    /**
     * Neural (MLP) RL model for Q-learning with eligibility traces.
     * Uses primitive arrays for zero-allocation inference and updates,
     * batch processing for efficiency, and stable numerical methods.
     */
    public static class NeuralRLModel implements RLModel {
        private static final float TRACE_THRESHOLD = 1e-6f;
        private static final float GRADIENT_CLIP = 1.0f; // Prevent exploding gradients
        private static final int BATCH_SIZE = 32; // Max transitions per batch

        // MLP architecture: input (state) -> hidden -> output (Q-value)
        private final int inputSize; // Bits in state (e.g., 9 from FeatureExtractor)
        private final int hiddenSize; // Configurable hidden layer size
        private static final int outputSize = 1; // Single Q-value output
        private final float[] w1; // Input -> Hidden weights [inputSize * hiddenSize]
        private final float[] b1; // Hidden biases [hiddenSize]
        private final float[] w2; // Hidden -> Output weights [hiddenSize * outputSize]
        private final float[] b2; // Output biases [outputSize]
        private final float[] hidden; // Hidden layer activations [hiddenSize]
        private final float[] output; // Output activations [outputSize]
        private final float[] gradW1; // Gradients for w1
        private final float[] gradB1; // Gradients for b1
        private final float[] gradW2; // Gradients for w2
        private final float[] gradB2; // Gradients for b2

        // Eligibility traces
        private final float[] traceW1; // Eligibility traces for w1
        private final float[] traceB1; // Eligibility traces for b1
        private final float[] traceW2; // Eligibility traces for w2
        private final float[] traceB2; // Eligibility traces for b2

        // Batch storage
        private final IntArrayList batchStates = new IntArrayList(BATCH_SIZE);
        private final FloatArrayList batchRewards = new FloatArrayList(BATCH_SIZE);
        private final IntArrayList batchNextStates = new IntArrayList(BATCH_SIZE);
        private final FloatArrayList batchNextValues = new FloatArrayList(BATCH_SIZE);
        private final FloatArrayList batchAlphas = new FloatArrayList(BATCH_SIZE);

        private float lambda; // Eligibility trace decay

        /**
         * @param inputSize Number of input bits (from FeatureExtractor)
         * @param hiddenSize Size of hidden layer
         * @param rng Random generator for initialization
         */
        public NeuralRLModel(int inputSize, int hiddenSize, RandomGenerator rng) {
            this.inputSize = inputSize;
            this.hiddenSize = hiddenSize;

            // Initialize arrays
            w1 = new float[inputSize * hiddenSize];
            b1 = new float[hiddenSize];
            w2 = new float[hiddenSize * outputSize];
            b2 = new float[outputSize];
            hidden = new float[hiddenSize];
            output = new float[outputSize];
            gradW1 = new float[inputSize * hiddenSize];
            gradB1 = new float[hiddenSize];
            gradW2 = new float[hiddenSize * outputSize];
            gradB2 = new float[outputSize];
            traceW1 = new float[inputSize * hiddenSize];
            traceB1 = new float[hiddenSize];
            traceW2 = new float[hiddenSize * outputSize];
            traceB2 = new float[outputSize];

            // Initialize weights and biases (Xavier initialization)
            {
                float scale = (float) Math.sqrt(6.0 / (inputSize + hiddenSize));
                xavier(w1, rng, scale);
                xavier(b1, rng, scale);
            }
            {
                float scale = (float) Math.sqrt(6.0 / (hiddenSize + outputSize));
                xavier(w2, rng, scale);
                xavier(b2, rng, scale);
            }
        }

        private void xavier(float[] w1, RandomGenerator rng, float scale) {
            for (int i = 0; i < w1.length; i++) w1[i] = (rng.nextFloat() * 2 - 1) * scale;
        }

        public void setLambda(float lambda) {
            this.lambda = Util.unitize(lambda);
        }

        @Override
        public float q(int state) {
            forward(state);
            return output[0];
        }

        @Override
        public void update(int state, float reward, int nextState, float nextValue, float alpha, float gamma) {
            // Add to batch
            batchStates.add(state);
            batchRewards.add(reward);
            batchNextStates.add(nextState);
            batchNextValues.add(nextValue);
            batchAlphas.add(alpha);

            // Process batch if full
            if (batchStates.size() >= BATCH_SIZE) {
                processBatch(gamma);
                batchStates.clear();
                batchRewards.clear();
                batchNextStates.clear();
                batchNextValues.clear();
                batchAlphas.clear();
            }
        }

        @Override
        public TrackedPremise chooseAction(PrioritySet<TrackedPremise> actions, RandomGenerator rng) {
            return actions.get(rng); // Reuse PrioritySet's softmax-based selection
        }

        /**
         * Forward pass: state -> Q-value.
         * Zero-allocation using primitive arrays.
         */
        private void forward(int state) {
            // Convert state to binary input
            float[] input = new float[inputSize];
            for (int i = 0; i < inputSize; i++) {
                input[i] = (state >> i) & 1;
            }

            // Input -> Hidden
            Arrays.fill(hidden, 0);
            for (int h = 0; h < hiddenSize; h++) {
                for (int i = 0; i < inputSize; i++) {
                    hidden[h] += input[i] * w1[i * hiddenSize + h];
                }
                hidden[h] += b1[h];
                hidden[h] = relu(hidden[h]); // Stable ReLU
            }

            // Hidden -> Output
            Arrays.fill(output, 0);
            for (int o = 0; o < outputSize; o++) {
                for (int h = 0; h < hiddenSize; h++) {
                    output[o] += hidden[h] * w2[h * outputSize + o];
                }
                output[o] += b2[o];
            }
        }

        /**
         * Process a batch of transitions, updating weights and eligibility traces.
         */
        private void processBatch(float gamma) {
            int size = batchStates.size();
            Arrays.fill(gradW1, 0);
            Arrays.fill(gradB1, 0);
            Arrays.fill(gradW2, 0);
            Arrays.fill(gradB2, 0);

            for (int b = 0; b < size; b++) {
                int state = batchStates.get(b);
                float reward = batchRewards.get(b);
                int nextState = batchNextStates.get(b);
                float nextValue = batchNextValues.get(b);
                float alpha = batchAlphas.get(b);

                // Forward pass for current state
                forward(state);
                float oldQ = output[0];

                // Compute TD error
                float tdError = reward + gamma * nextValue - oldQ;

                // Backward pass: compute gradients
                computeGradients(state, tdError);

                // Update eligibility traces
                if (lambda == 0) {
                    // No traces: update weights directly
                    updateWeights(alpha);
                } else {
                    // Update traces
                    updateTraces();
                    // Apply trace-based updates
                    applyTraceUpdates(alpha, tdError);
                    // Decay traces
                    decayTraces(gamma);
                }
            }
        }

        /**
         * Compute gradients for a single transition.
         */
        private void computeGradients(int state, float tdError) {
            float[] input = new float[inputSize];
            for (int i = 0; i < inputSize; i++) {
                input[i] = (state >> i) & 1;
            }

            // Output layer gradients
            for (int o = 0; o < outputSize; o++) {
                gradB2[o] += tdError;
                for (int h = 0; h < hiddenSize; h++) {
                    gradW2[h * outputSize + o] += tdError * hidden[h];
                }
            }

            // Hidden layer gradients
            float[] hiddenGrad = new float[hiddenSize];
            for (int h = 0; h < hiddenSize; h++) {
                float grad = 0;
                for (int o = 0; o < outputSize; o++) {
                    grad += tdError * w2[h * outputSize + o];
                }
                hiddenGrad[h] = grad * reluDerivative(hidden[h]);
            }

            // Input -> Hidden gradients
            for (int h = 0; h < hiddenSize; h++) {
                gradB1[h] += hiddenGrad[h];
                for (int i = 0; i < inputSize; i++) {
                    gradW1[i * hiddenSize + h] += hiddenGrad[h] * input[i];
                }
            }
        }

        /**
         * Update weights using gradients.
         */
        private void updateWeights(float alpha) {
            for (int i = 0; i < w1.length; i++) {
                w1[i] += alpha * clip(gradW1[i]);
            }
            for (int i = 0; i < b1.length; i++) {
                b1[i] += alpha * clip(gradB1[i]);
            }
            for (int i = 0; i < w2.length; i++) {
                w2[i] += alpha * clip(gradW2[i]);
            }
            for (int i = 0; i < b2.length; i++) {
                b2[i] += alpha * clip(gradB2[i]);
            }
        }

        /**
         * Update eligibility traces.
         */
        private void updateTraces() {
            // Replacing trace
            System.arraycopy(gradW1, 0, traceW1, 0, traceW1.length);
            System.arraycopy(gradB1, 0, traceB1, 0, traceB1.length);
            System.arraycopy(gradW2, 0, traceW2, 0, traceW2.length);
            System.arraycopy(gradB2, 0, traceB2, 0, traceB2.length);
        }

        /**
         * Apply trace-based weight updates.
         */
        private void applyTraceUpdates(float alpha, float tdError) {
            traceUpdate(w1, alpha, tdError, traceW1);
            traceUpdate(b1, alpha, tdError, traceB1);
            traceUpdate(w2, alpha, tdError, traceW2);
            traceUpdate(b2, alpha, tdError, traceB2);
        }
        /**
         * Decay eligibility traces.
         */
        private void decayTraces(float gamma) {
            decay(traceW1, gamma);
            decay(traceB1, gamma);
            decay(traceW2, gamma);
            decay(traceB2, gamma);
        }

        private static void traceUpdate(float[] w, float alpha, float tdError, float[] traceW1) {
            for (int i = 0; i < w.length; i++)
                w[i] += alpha * tdError * clip(traceW1[i]);
        }



        private void decay(float[] trace, float gamma) {
            for (int i = 0; i < trace.length; i++) {
                trace[i] *= gamma * lambda;
                if (Math.abs(trace[i]) < TRACE_THRESHOLD) trace[i] = 0;
            }
        }

        /**
         * Stable ReLU activation.
         */
        private static float relu(float x) {
            return Math.max(0, x);
        }

        /**
         * Derivative of ReLU.
         */
        private static float reluDerivative(float x) {
            return x > 0 ? 1 : 0;
        }

        /**
         * Clip gradients to prevent exploding.
         */
        private static float clip(float x) {
            return Util.clampSafePolar(x, GRADIENT_CLIP);
        }
    }
}