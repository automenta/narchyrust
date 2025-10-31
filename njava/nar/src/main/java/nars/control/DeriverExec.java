package nars.control;

import jcog.Log;
import jcog.Util;
import jcog.data.list.Lst;
import jcog.math.FloatMeanEwma;
import jcog.pri.PLink;
import jcog.random.RandomBits;
import jcog.util.ArrayUtil;
import nars.Deriver;
import nars.Focus;
import nars.NAR;
import nars.deriver.impl.TaskBagDeriver;
import nars.deriver.reaction.ReactionModel;
import nars.util.NARPart;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.nanoTime;

/** Threadpool of Derivers
 *  TODO use AffinityThreadPool?
 * */
public class DeriverExec extends NARPart {

    private final ReactionModel rules;

    private final ExecutorService workerExecutor;
    protected final DeriverThread[] thread;

    /** Throttle in [0..1], default=1.0 => no sleep; 0 => paused */
    private /*volatile*/ float throttle = 1;

    private /*volatile*/ boolean running;

    private double sleepMSFactor;

    private static final int HASH_SEED = 123456789;
    private static final float DRIFT_RATE = 0.1f; // How quickly weights drift toward target each update

    private static final WeightedFocusSampler nullSampler = new WeightedFocusSampler(ArrayUtil.EMPTY_FLOAT_ARRAY, new Focus[0]);


    public DeriverExec(ReactionModel rules, int threads) {
        this.rules = rules;
        this.workerExecutor = Executors.newFixedThreadPool(threads);
        this.thread = new DeriverThread[threads];
    }

    @Override
    protected void starting(NAR n) {

        int threads = threads();
        for (var i = 0; i < threads; i++)
            this.thread[i] = new DeriverThread(i, deriver(n));

        // Register update to be called periodically or based on specific triggers
        update();
        n.onDur(this::update);

        // Start workers
        running = true;
        for (var w: this.thread)
            workerExecutor.execute(w);
    }

    @Override
    protected void stopping(NAR nar) {
        running = false;
        workerExecutor.shutdownNow();
        Arrays.fill(thread, null);
    }

    protected Deriver deriver(NAR n) {
        return new TaskBagDeriver(rules, n);

//        var feat = new RLTaskBagDeriver.FeatureExtractorBuilder<Premise>()
//            //TODO tune
//            .add(p -> p instanceof SeedTaskPremise x)
//            .add(p -> p instanceof NALPremise)
//            .add(p -> p instanceof DoubleTaskPremise)
//            .add(p -> p.task()!=null ? NALTask.i(p.task().punc()) : 0, 2)
//            .add(p -> {
//                if (!(p instanceof NativePremise x)) return 0;
//                var r = x.reactionType();
//                if (r == TermLinking.class) return 1;
//                if (r == BeliefResolve.class) return 2;
//                if (r == VariableIntroduction.class) return 3;
//                if (r == Evaluate.class) return 4;
//                if (DecomposeTerm.class.isAssignableFrom(r))
//                    return 5;
//                if (r == STMLinker.class || r == ClusterInduct.class || TemporalInduction.class.isAssignableFrom(r))
//                    return 6;
//                return 7; //other NativePremise
//            }, 3)
//        ;
//        var rew = new RLTaskBagDeriver.BasicRewardModel(0.05f, 1);
//        var f = feat.build();
//        var model =
//            //new RLTaskBagDeriver.TableRLModel(256);
//            new RLTaskBagDeriver.NeuralRLModel(f.size(), f.size()*4, new XoRoShiRo128PlusRandom(1));
//        return new RLTaskBagDeriver(rules, n, f, rew, model);
    }



    public double throttle() {
        return throttle;
    }

    private synchronized void update() {
        throttle = nar.throttle();

        {
            //update threads throttles
            int w = thread.length;
            float runningIdeal = w * throttle;
            int running = (int) runningIdeal;

            //int partialSleep = (throttleSleepRemainder > 0) ? nar.random().nextInt(running) : -1;
            for (int i = 0; i < w; i++)
                thread[i].myThrottle = throttle(i, running, runningIdeal);
        }
        var links = links();

        var n = links.size();
        if (n == 0)
            updateSampler(nullSampler);
        else {
            var f = new Focus[n];
            var p = new float[n];
            for (var i = 0; i < n; i++) {
                var l = links.get(i);
                f[i] = l.id;
                p[i] = l.priElseZero();
            }
            update(p, f);
        }
    }

    private static float throttle(int i, int running, float runningIdeal) {
        if (i >= running)
            return 0; //fully asleep
        else if (i == running - 1)
            return 1 - Math.max((float) 0, runningIdeal - running); //fraction
        else
            return 1; //fully awake
    }

    private Lst<PLink<Focus>> links() {
        var f = nar.focus;
        var links = new Lst<PLink<Focus>>(f.size());
        for (var x : f)
            if (x.priElseZero() > 0)
                links.add(x);
        return links;
    }

    protected void update(float[] p, Focus[] f) {
        updateSampler(new WeightedFocusSampler(p, f));
    }

    private void updateSampler(WeightedFocusSampler shared) {
        for (var w : this.thread)
            w.sampler = shared;
    }


    public int threads() {
        return thread.length;
    }

    private static class WeightedFocusSampler {

        private final float[] cumulative;
        private final Focus[] f;
        private final int n;

        WeightedFocusSampler(float[] weights, Focus[] f) {
            this.f = f;
            this.n = weights.length;
            cumulative = new float[n];
            float sum = 0;
            for (var i = 0; i < n; i++) {
                sum += weights[i];
                cumulative[i] = sum;
            }
            if (sum > 0)
                for (var i = 0; i < n; i++)
                    cumulative[i] /= sum;
        }

        private static int binarySearch(float[] cumWeights, float value) {
            int low = 0, high = cumWeights.length - 1;
            while (low < high) {
                var mid = (low + high) >>> 1;
                if (value > cumWeights[mid])
                    low = mid + 1;
                else
                    high = mid;
            }
            return low;
        }

        @Nullable
        Focus next(RandomBits rng) {
            return switch (n) {
                case 0 -> null;
                case 1 -> f[0];
                default -> f[binarySearch(cumulative, rng.nextFloatFast16())];
            };
        }
    }

    private final class DeriverThread implements Runnable {
        final FloatMeanEwma periodNS = new FloatMeanEwma().period(16).reset(/* initialize: 1ms */ 1E6);

        private final int id;
        private final Deriver d;
        private final RandomBits rng;
        private static final Logger logger = Log.log(DeriverThread.class);
        float myThrottle = 1;

        public WeightedFocusSampler sampler;

        long _periodNS;


        public DeriverThread(int workerId, Deriver d) {
            this.id = workerId;
            this.d = d;
            this.rng = d.rng;
            periodUpdate();
        }

        private void _run() {
            try {
                var f = sampler.next(rng);
                if (f != null)
                    d.next(f);
            } catch (Throwable t) {
                logger.error("run", t);
            }
        }

        @Override
        public void run() {
            while (running) {
                float t = myThrottle;
                if (t>=1 || (awake(t) && !profiled()))
                    _run();
            }
        }

        private static final float profileProb = 1/32f;

        private boolean profiled() {
            if (rng._nextBooleanFast8(profileProb)) {
                long s = nanoTime();

                _run();

                profiled(nanoTime() - s);
                return true;
            }
            return false;
        }

        private boolean awake(float t) {
            if (rng._nextBooleanFast8(t))
                return true;
            Util.sleepNS(_periodNS);
            return false;
        }

        protected void profiled(long timeNS) {
            periodNS.accept(timeNS);
            periodUpdate();
        }

        private void periodUpdate() {
            _periodNS = (long) periodNS.mean();
        }

    }


    /** Focus->Worker locality optimization - untested */
    static class AffinityDeriverExec extends DeriverExec {
        private boolean trace;
        private float[][] affinityWeights;

        public AffinityDeriverExec(ReactionModel rules, int threads) {
            super(rules, threads);
        }

        @Override protected void update(float[] pri, Focus[] f) {
            super.update(pri, f);

            var focusCount = f.length;
            if (focusCount == 0)
                return;

            var nThreads = threads();

            // Initialize or resize affinityWeights if necessary
            if (affinityWeights == null
                    || affinityWeights.length != nThreads
                    || affinityWeights[0].length != focusCount) {
                affinityWeights = new float[nThreads][focusCount];
                // Start with equal distribution among threads for each focus
                for (var t = 0; t < nThreads; t++)
                    Arrays.fill(affinityWeights[t], 1.0f / nThreads);
            } else if (affinityWeights[0].length != focusCount) {
                // Resize inner arrays if focusCount changed
                for (var t = 0; t < nThreads; t++) {
                    affinityWeights[t] = Arrays.copyOf(affinityWeights[t], focusCount);
                    // Initialize new entries evenly
                    for (var i = 0; i < focusCount; i++) {
                        if (affinityWeights[t][i] == 0)
                            affinityWeights[t][i] = 1.0f / nThreads;
                    }
                }
            }

            var H = new float[nThreads][focusCount];
            var sumH = new float[focusCount];
            for (var i = 0; i < focusCount; i++) {
                var sum = 0f;
                for (var t = 0; t < nThreads; t++)
                    sum += (H[t][i] = stableHash(t, f[i]));
                sumH[i] = (sum > 0) ? sum : 1; // avoid division by zero
            }

            var targetWeights = new float[nThreads][focusCount];
            for (var i = 0; i < focusCount; i++) {
                for (var t = 0; t < nThreads; t++)
                    targetWeights[t][i] = pri[i] * (H[t][i] / sumH[i]);
            }

            for (var t = 0; t < nThreads; t++) {
                for (var i = 0; i < focusCount; i++) {
                    var current = affinityWeights[t][i];
                    var target = targetWeights[t][i];
                    affinityWeights[t][i] = current + DRIFT_RATE * (target - current);
                }
            }

            for (var t = 0; t < nThreads; t++)
                thread[t].sampler = new WeightedFocusSampler(affinityWeights[t], f);

            if (trace)
                measureAffinitySkew();
        }


        /**
         * Compute and log an "affinity skew" metric as an indication of how unevenly
         * the processing of focuses is distributed among threads, compared to a random baseline.
         */
        private void measureAffinitySkew() {
            if (affinityWeights == null) return;

            final var threads = threads();
            final var focusCount = affinityWeights[0].length;
            var totalSkew = 0f;
            // For each focus, compute the deviation from a uniform distribution across threads.
            for (var i = 0; i < focusCount; i++) {
                var sum = 0f;
                for (var t = 0; t < threads; t++) {
                    sum += affinityWeights[t][i];
                }
                var uniform = sum / threads;
                for (var t = 0; t < threads; t++) {
                    totalSkew += Math.abs(affinityWeights[t][i] - uniform);
                }
            }
            // Average skew per focus-thread assignment
            var avgSkew = totalSkew / (focusCount * threads);
            logger.info("Average Affinity Skew: {}", avgSkew);
        }

        private static float stableHash(int threadId, Focus focus) {
            return (Util.hashCombine(threadId, focus.term().hashCode()) /
                    (float)0x7fffffff) + 1e-9f;
        }

    }
}
