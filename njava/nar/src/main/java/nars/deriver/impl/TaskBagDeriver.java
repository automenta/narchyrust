package nars.deriver.impl;

import jcog.Util;
import jcog.pri.op.PriMerge;
import jcog.signal.IntRange;
import jcog.sort.PrioritySet;
import nars.*;
import nars.deriver.reaction.ReactionModel;
import nars.focus.util.TaskBagAttentionSampler;
import nars.premise.NALPremise;
import nars.time.clock.RealTime;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

/**
 * "Open-NARS for Applications" for NARchy
 * Inspired by ONA's control system.
 * TODO clear bags when Focus.clear() called
 */
public class TaskBagDeriver extends Deriver {

    /** default depth (subpremise evaluations) per seed */
    public static int DEPTH =
        11;
        //9;

    /** default iterations per cycle */
    private static int ITERS = 5;

    /** default batch ("width"), % of iterations */
    private static float BATCH = 1f/ITERS;

    /** depth (batch work per seed) */
    public final IntRange depth = new IntRange(DEPTH, 4, 256);

    public final PremiseSet premises = new PremiseSet();

    private final TaskBagAttentionSampler tasks = new TaskBagAttentionSampler(this);

    public TaskBagDeriver(ReactionModel model, NAR nar) {
        super(model, nar);
    }

    private void _next(int iter) {
        if (focusHasTasks()) {
            __next(iter);
            later.run().clear();
        }
    }

    protected void __next(int iter) {
        var batchDepth = depth.intValue();
        for (var b = iter; b > 0; ) {
            var nextBatchSize = Util.clampSafe((int)(iter * BATCH), 1, b);
            b -= nextBatchSize;
            premises.run(nextBatchSize, batchDepth);
        }
    }

    @Override
    protected void acceptPremise(Premise p) {
        premises.add(p, this.premise);
    }

    @Override
    protected void onAdd(NALTask x) {
        super.onAdd(x);
        premises.onDerived(x);
    }

    public class PremiseSet {
        static final int QUEUE_CAPACITY = NAL.derive.TASKIFY_INLINE ? 64 : 32;
        static final float decayRate =
            (float) (1f/Math.sqrt(QUEUE_CAPACITY));
            //1f/QUEUE_CAPACITY;
            //1/8f;
            //1/6f;
            //1/4f;
            //1/3f;

        protected PrioritySet<Premise> queue = new PrioritySet<>(QUEUE_CAPACITY);

        public FloatFunction<Premise> pri = p -> 1;

        public boolean isEmpty() {
            return queue.isEmpty();
        }

        public void add(Premise x, Premise parent) {
            if (queue.put(x, pri.floatValueOf(x)))
                onDerived(x);
        }

        public void clear() {
            queue.clear();
        }

        public void run(int iters) {
            Premise next;
            while (iters-- > 0 && (next = queue.get(rng)) != null) {
                run(next);
            }
        }

        protected void runSeed(NALPremise.SeedTaskPremise seed, int iter) {
            run(seed);

            if (!isEmpty())
                run(iter);

            clear();
        }

        private static void update(Premise p, NALTask seed) {
            if (p instanceof NALPremise P) {
                var t = p.task();
                if (t != seed)
                    PriMerge.replace.accept(t, seed);
            }
        }

        protected void run(Premise p) {
            TaskBagDeriver.this.run(p);
            queue.priMul(p, decayRate);
        }

        public final void runSeed(NALTask t, int iter) {
            runSeed(NALPremise.the(t), iter);
        }

        protected void run(int seedCount, int itersPerSeed) {
            var seeds = tasks.seed(focus, seedCount, rng);
            for (int i = 0, size = seeds.size(); i < size; i++)
                runSeed(seeds.getAndNull(i), itersPerSeed);
        }

        protected void onDerived(NALTask x) {
            //TODO reward?
        }

        protected void onDerived(Premise p) {
            //TODO reward?
        }

    }

    @Override
    protected void onCommit() {
        if (_next!=null) _next.update();
    }

    @Override
    protected void next() {
        if (_next != null && focusHasTasks()) _next.run();
        else _next(ITERS);
    }

    private boolean focusHasTasks() {
        return !focus.attn.isEmpty();
    }

    public static final boolean ITERATION_TUNER = true;
    private final DeriverIterationTuner _next = ITERATION_TUNER && nar.time instanceof RealTime ? new DeriverIterationTuner(ITERS) {
        @Override protected void next(int iter) { _next(iter); } } : null;

}

//    /**
//     * MCTS - Monte Carlo Tree Search
//     */
//    public class MCTSPremiseQueue extends PremiseQueue {
//        static final double PremiseReward = 0.01;
//        static final boolean rootNoReward = false;
//        private static final double explore =
//                Math.sqrt(2);
//
//        @Deprecated private static final boolean cacheNodes = false;
//        private static final boolean cacheValues = true;
//
//        private static final boolean timeCosts = false;
//
//        private static final DoubleFunction<Node> nodeUCB = Node::ucb;
//        private final PuncBag taskReward = new PuncBag(
//                0.25f, 0.1f, 0.25f, 0.1f,
//                0, 1
//        );
//        private final int BEAM_WIDTH = 8;
//        /**
//         * probability of executing the current node in the traversal, and not a descendant leaf node
//         */
//        private final float REFRESH_RATE = 0.02f;
//        private final Node root;
//        private final Map<Term, Node> nodes = cacheNodes ?
//                new MRUMap<>(4 * 1024) : null;
//        private final Map<Premise, NodeValue> values = cacheValues ?
//                new MRUMap<>(4 * 1024) : null;
//        private final Node[] featureNodes;
//        private final String[] features = { /* TODO */};
//        private final RankedN<Node> beam = new RankedN<>(new Node[BEAM_WIDTH], n -> (float) n.ucb());
//        private transient double reward;
//        private transient Node currentNode;
//
//        @SuppressWarnings("UnreachableCode")
//        public MCTSPremiseQueue() {
//
//            this.root = new Node(null, null, 0);
//
//            this.featureNodes = new Node[features.length];
//            for (var i = 0; i < featureNodes.length; i++)
//                featureNodes[i] = new Node(null, null, -1);
//        }
//
//        private static long features(Premise premise) {
////            var features = 0L;
////            if (premise.complexity() < 5) features |= 1L << Feature.COMPLEXITY_LOW.ordinal();
////            else if (premise.complexity() < 10) features |= 1L << Feature.COMPLEXITY_MEDIUM.ordinal();
////            else features |= 1L << Feature.COMPLEXITY_HIGH.ordinal();
////            // Add more feature extraction logic here
////            return features;
//            return 0;
//        }
//
//        @Override
//        public void onDerived(NALTask x) {
//            reward += taskReward.apply(x);
//        }
//
//        private Node addRoot(Premise x) {
//            return nodes == null ?
//                new Node(x, root) :
//                nodes.computeIfAbsent(x.term(), X -> new Node(x, root));
//        }
//
//        @Override
//        public void add(Premise x, Premise parent) {
//            premiseAdded();
//
//            if (nodes == null) {
//                newNode(x, parent);
//                return;
//            }
//
//            var xt = x.term();
//            //nodes.computeIfAbsent(xt, X -> newNode(x, parent));
//            var n = nodes.get(xt);
//            if (n == null)
//                nodes.put(xt, n = newNode(x, parent));
//
////            } else {
////                if (n.premise != x) {
////                    Util.nop();
////                }
////                var pn = parent;
////                if (n.parent.premise != pn) {
////                    Util.nop();
////                }
////            }
//        }
//
//        private Node newNode(Premise x, Premise parent) {
//            var parentNode = currentNode;
//            var n = new Node(x, parentNode);
//            if (values != null) {
//                var v = values.get(valueKey(x));
//                if (v != null)
//                    reward(n, v.rewardMean()); //initialize with existing value
//            }
//            return n;
//        }
//
//        private Premise valueKey(Premise x) {
//            return x;
//        }
//
//        private void premiseAdded() {
//            reward += PremiseReward;
//        }
////        private Term valueKey(Premise x) {  return valueKey(x.term());  }
////
////        /** determines how premises map to saved value premises */
////        private Term valueKey(Term x) {
////            return x; //as-is
////            //return nars.term.anon.Anon.the(x);
////        }
//
//        private Node node(Premise n) {
//            var p = nodes.get(n.term());
//            return p == null ? root : p;
//        }
//
//        @Override
//        protected void runSeed(NALPremise.SeedTaskPremise seed, int iter) {
//            this.currentNode = addRoot(seed);
//            TaskBagDeriver.this.run(seed);
//        }
//
//        @Override
//        public void run(int batchSize, int itersPerSeed) {
//            clear();
//            super.run(batchSize, itersPerSeed); //SEEDS loaded
//            run(batchSize * itersPerSeed);
//        }
//
//        @Override
//        public void run(int iterations) {
//            if (root.childCount() == 0)
//                return;
//
//            for (var i = 0; i < iterations; i++) {
//                var n = select(root);
//                if (n == root)
//                    return; //empty
//
//                currentNode = n;
//                reward = 0;
//                var p = n.premise;
//                if (timeCosts) {
//                    runTimed(p);
//                } else {
//                    TaskBagDeriver.this.run(p);
//                }
//                reward(n, reward);
//            }
//        }
//
//        private void runTimed(Premise p) {
//            var start = System.nanoTime();
//            TaskBagDeriver.this.run(p);
//            if (reward > 0) {
//                var end = System.nanoTime();
//                //TODO histogram, percentile
//                //1_000.0 //x 1  uS
//                //10_000.0  //x 10 uS
//                //x 40 uS
//                @Deprecated var divisor = Math.max(1, (end - start) /
//                        //1_000.0 //x 1  uS
//                        //10_000.0  //x 10 uS
//                        40_000.0);
//                reward /= divisor;
//            }
//        }
//
//        private Node select(Node n) {
//            //int beamWidth = this.BEAM_WIDTH;
//            while (n.childCount() > 0 && !rng._nextBooleanFast8(REFRESH_RATE)) {
//                n = n.ucbSample(beam, rng);
//            }
//            return n;
//        }
//
//        /**
//         * backpropagate reward
//         */
//        private void reward(Node n, double reward) {
//            do {
//                n.reward(reward, featureNodes);
//                n = n.parent;
//            } while (n != null);
//        }
//
//        @Override
//        public void clear() {
//            for (var f : featureNodes) f.clearStats();
//
//            if (!cacheNodes) {
//                if (values != null) {
//                    forEachNode(n -> {
//                        var p = n.premise;
//                        if (p == null) return; //root node
//
//                        var k = valueKey(p);
//                        var v = values.get(k);
//                        if (v == null) {
//                            var s = n.snapshotIfNotZero();
//                            if (s != null)
//                                values.put(k, s);
//                        } else
//                            v.merge(n);
//                    });
//                }
//                if (nodes != null)
//                    nodes.clear();
//            }
//
//            root.clear();
//        }
//
//        private void forEachNode(Consumer<Node> o) {
//            root.forEachRecursively(o);
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return root.childCount() == 0;
//        }
//
//        private static class NodeValue {
//            long visits;
//            double reward;
//
//            NodeValue(long visits, double reward) {
//                this.visits = visits;
//                this.reward = reward;
//            }
//
//            protected NodeValue() {
//
//            }
//
//            public double rewardMean() {
//                return reward / (visits + 1);
//            }
//
//            /**
//             * TODO handle numeric overflow
//             */
//            public void merge(NodeValue x) {
//                visits += x.visits;
//                reward += x.reward;
//            }
//
//        }
//
//        private static class Node extends NodeValue {
//            private static final Node[] EmptyNodeArray = new Node[0];
//            final Node parent;
//            final int depth;
//
//            final Lst<Node> children = new Lst<>(EmptyNodeArray);
//
//            /**
//             * virtual parents, as a bitset to feature ID's
//             */
//            final long features;
//            Premise premise;
//
//            Node(Premise p, Node parent) {
//                this(p, parent, parent.depth + 1);
//                parent.children.add(this);
//            }
//
//            Node(Premise p, Node parent, int depth) {
//                this.premise = p;
//                this.parent = parent;
//                this.depth = depth;
//                this.features = p != null ? features(p) : 0L;
//            }
//
//            @Override
//            public String toString() {
//                return "[" + depth + "]:" + premise.toString();
//            }
//
//            /**
//             * TODO check the formula
//             */
//            public double ucb() {
//                return ucb(parent.logVisits());
//            }
//
//            private double logVisits() {
//                return Util.log1p(visits);
//            }
//
//            //            public double ucb0() {
//            //                return reward + explore * Math.sqrt(
//            //                        2 * Util.log1p(totalUpdates) / (visits+1)
//            //                );
//            //            }
//
//            public double ucb(double logParentVisits) {
//                var visits = this.visits + 1;
//                var rewardMean = reward / visits;
//                if (logParentVisits > 0)
//                    rewardMean += explore * Math.sqrt(logParentVisits / visits);
//
//                if (rewardMean != rewardMean)
//                    throw new WTF();
//                return rewardMean;
//            }
//
//            /**
//             * Top-P (Nucleus) Sampling
//             */
//            public Node ucbSample(RankedN<Node> beam, RandomGenerator rng) {
//                beam.clear();
//
//                var parentLogVisits = logVisits();
//                for (var c : children)
//                    beam.add(c, (float) c.ucb(parentLogVisits));
//
//                return beam.getRoulette(rng);
//            }
//
//            public Node ucbMax() {
//                //TODO return children.maxByDouble(Node::ucb);
//
//                var c = children.size();
//                var nc = children.array();
//                Node y = null;
//                var parentLogVisits = logVisits();
//                var max = Double.NEGATIVE_INFINITY;
//                for (var i = 0; i < c; i++) {
//                    var x = nc[i];
//                    var v = x.ucb(parentLogVisits);
//                    if (v > max) {
//                        max = v;
//                        y = x;
//                    }
//                }
//                return y;
//            }
//
//            public int childCount() {
//                return children.size();
//            }
//
//            public void reward(double reward, Node[] featureNodes) {
//                if (rootNoReward && premise == null)
//                    return; //root
//
//                this.visits++;
//                this.reward += reward;
//                if (features != 0) rewardFeatures(reward, featureNodes);
//            }
//
//            private void rewardFeatures(double reward, Node[] featureNodes) {
//                var n = featureNodes.length;
//                for (var i = 0; i < n; i++) {
//                    if ((features & (1L << i)) != 0) {
//                        var fni = featureNodes[i];
//                        fni.visits++;
//                        fni.reward += reward;
//                    }
//                }
//            }
//
//            public void clear() {
//                children.clear();
//                clearStats();
//            }
//
//            public void clearStats() {
//                visits = 0;
//                reward = 0;
//            }
//
//            public NodeValue snapshot() {
//                return new NodeValue(visits, reward);
//            }
//
//            public NodeValue snapshotIfNotZero() {
//                return visits == 0 ? null : snapshot();
//            }
//
//            public void forEachRecursively(Consumer<Node> o) {
//                o.accept(this);
//                children.forEach(c -> c.forEachRecursively(o));
//            }
//        }
//
////        public Map<Feature, Double> getFeatureInsights() {
////            Map<Feature, Double> insights = new EnumMap<>(Feature.class);
////            for (Feature f : Feature.values()) {
////                var node = featureNodes[f.ordinal()];
////                insights.put(f, node.visits > 0 ? node.totalReward / node.visits : 0);
////            }
////            return insights;
////        }
//    }


//    @Nullable private final MRUMap<Premise, Premise> cache;
//    private static final boolean cachePremises = false;
//        this.cache = cachePremises ? new MRUMap<>(8*1024) : null;

//    private Premise premise(Premise p) {
//        if (!cachePremises)
//            return p;
//        var q = cache.putIfAbsent(p,p);
//        return q == null ? p : q;
//    }


//    /** fairly samples the graph of premises */
//    protected class GraphPremiseQueue extends PremiseQueue {
//        private final Map<Premise, ArrayHashSet<Premise>> graph = new UnifriedMap<>();
//        private final ArrayHashSet<Premise> roots = new ArrayHashSet<>();
//
//        final boolean rootsRandom = true;
//        float childFactor = 6;
//
//        public void clear() {
//            graph.clear();
//            roots.clear();
//        }
//
//        public void run(int iter) {
//            if (roots.isEmpty()) return;
//
//            if (rootsRandom)
//                runRandom(iter);
//            else
//                runRoundRobin(iter);
//        }
//
//        private void runRoundRobin(int iter) {
//            //round-robin
//            int r = 0;
//            for (int i = 0; i < iter; i++) {
//                if (roots.size() <= r) r = 0; //reached end
//                traverse(roots.get(r++));
//            }
//        }
//
//        private void runRandom(int iter) {
//            for (int i = 0; i < iter; i++)
//                traverse(roots.get(rng));
//        }
//
//        private void traverse(Premise x) {
//            var children = graph.get(x);
//            int nChildren = children!=null ? children.size() : 0;
//            if (nChildren == 0 || executeSelfOrChild(x, nChildren))
//                TaskBagDeriver.this.run(x);
//            else
//                traverse(children.get(rng));
//        }
//
//        /** TODO tune */
//        private boolean executeSelfOrChild(Premise x, int nChildren) {
//            return rng.nextBooleanFast16(1f/(1 +
//                //(int)Math.pow(nChildren, childFactor)
//                nChildren * childFactor
//            ));
//        }
//
//        private ArrayHashSet<Premise> premiseNode(Premise x) {
//            return graph.computeIfAbsent(x, (X)-> new ArrayHashSet<>(1));
//        }
//
//        public void add(Premise x, Premise parent) {
//            if(parent instanceof NALPremise.SeedTaskPremise)
//                roots.add(x);
//            else
//                premiseNode(parent).add(x);
//        }
//
//        @Override
//        public boolean isEmpty() {
//            throw new TODO();
//        }
//    }
//
//    public class ListPremiseQueue extends PremiseQueue {
//        protected final Lst<Premise> queue = new Lst<>();
//        private final boolean pop;
//
//        public ListPremiseQueue(boolean pop) {
//            this.pop = pop;
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return queue.isEmpty();
//        }
//
//        @Override
//        public void add(Premise x, Premise parent) {
//            queue.add(x);
//        }
//
//        @Override public void clear() {
//            queue.clear();
//        }
//
//        @Override
//        public void run(int iters) {
//            Premise next;
//            while ((next = (pop ? queue.removeRandom(rng) : queue.get(rng))) != null) {
//                TaskBagDeriver.this.run(next);
//                if (--iters <= 0)
//                    break;
//            }
//        }
//
//    }
//    public class ListPremiseQueue2 extends PremiseQueue {
//        protected final Lst<Premise> queue = new Lst<>();
//        final java.util.Set<Premise> p = new UnifiedSet<>();
//
//        @Override
//        public boolean isEmpty() {
//            return queue.isEmpty();
//        }
//
//        @Override
//        public void add(Premise x, Premise parent) {
//            if (p.add(x))
//                queue.add(x);
//        }
//
//        @Override public void clear() {
//            queue.clear();
//            p.clear();
//        }
//
//        @Override
//        public void run(int iters) {
//            for (int i = 0; i < iters; ) {
//                var next = queue.removeRandom(rng);
//                if (next == null) {
//                    if (p.isEmpty())
//                        break; //completely empty
//                    else {
//                        queue.addAll(p); //reload
//                        p.clear();
//                    }
//                } else {
//                    TaskBagDeriver.this.run(next);
//                    i++;
//                }
//            }
//        }
//
//    }
//
//
//    public class BagPremiseQueue extends PremiseQueue {
//        protected final PLinkArrayBag<Premise> queue = new PLinkArrayBag<>(
//                PriMerge.min,
//                new ArrayBag.PlainListArrayBagModel<>());
//
//        static final int CAPACITY = 32;
//        static final float decayRate =
//            //1/2f;
//            //1/3f;
//            //1/4f;
//            1/8f;
//            //0.1f;
//
//        public BagPremiseQueue() {
//            queue.setCapacity(CAPACITY);
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return queue.isEmpty();
//        }
//
//        @Override
//        public void add(Premise x, Premise parent) {
//            queue.put(new PlainLink<>(x, pri(x)));
//        }
//
//        protected float pri(Premise x) {
//            return 1;
//            //return x instanceof NALPremise n ? (n.task.priElseZero()/2+0.5f) : 0.5f;
//            //return x.reactionType()==null ? 1 : 0.5f;
//        }
//
//        @Override public void clear() {
//            queue.clear();
//        }
//
//        @Override
//        public void run(int iters) {
//            queue.commit(null);
//            PriReference<Premise> next;
//            while ((next = queue.sample(rng)) != null) {
//                Premise p = next.get();
//
//                run(p);
//
//                queue.put(new PlainLink<>(p, next.pri() * decayRate));
//                queue.commit(null);
//
//                if (--iters <= 0)
//                    break;
//            }
//            //System.out.println(queue.size());
////            synchronized(TaskBagDeriver.class) {
////                queue.print();
////                System.out.println();
////                System.out.println();
////            }
//        }
//
//        protected void run(Premise p) {
//            TaskBagDeriver.this.run(p);
//        }
//
//    }
//    /** pair of queues for 2 classes of premise: 'seeds' and 'leaves' */
//    abstract public class TieredPremiseQueue extends PremiseQueue {
//        protected final ArraySet<Premise> seed = new ArrayHashSet<>();
//        protected final Lst<Premise> leaf = new Lst<>();
//
//        transient NALPremise root;
//        private final static boolean seedPop = true, leafPop = true;
//
//        /** weight ratio */
//        private final float leafWeight;
//
//        public TieredPremiseQueue(float leafWeight) {
//            this.leafWeight = leafWeight;
//        }
//
//        @Override
//        public void add(Premise x, Premise root) {
//            (seed(x) ? seed : leaf).add(x);
//        }
//
//        @Override
//        public boolean isEmpty() {
//            throw new TODO();
//        }
//
//        abstract protected boolean seed(Premise premise);
//
//        @Override public void clear() {
//            seed.clear();
//            leaf.clear();
//        }
//
//
//        @Override
//        public void run(int iters) {
//            for (int i = 0; i < iters; i++) {
//                int leafCount = leaf.size(), seedCount = seed.size();
//
//                if (seedCount == 0 && leafCount == 0) {
//                    if (i < iters-1 && root!=null) { //at least one more iteration remaining
//                        TaskBagDeriver.this.run(root); //recycle
//                        continue;
//                    } else
//                        return; //Focus is empty, done
//                }
//
//                TaskBagDeriver.this.run(next(seedProb(seedCount, leafCount)));
//            }
//        }
//
//        /** @return seed=true, leaf=false */
//        private boolean seedProb(int seedCount, int leafCount) {
//            if (seedCount == 0) return false;
//            if (leafCount == 0) return true;
//            float seedOrLeaf;
//            //raw:
//            //seedOrLeaf = (seedCount == 0 ? false : (..)) //TODO
//
//            //ratio of weighted queue sizes:
//            seedOrLeaf = seedCount / (seedCount + leafCount * leafWeight);
//
//            //ratio of weighted log queue sizes:
//            //float sc = (float)Math.log(1+seedCount), lc = (float)Math.log(1+leafCount);
//            //seedOrLeaf = sc / (sc + lc * leafWeight);
//
//            return rng.nextBooleanFast8(seedOrLeaf);
//        }
//
//        @Nullable
//        private Premise next(boolean seedOrLeaf) {
//            return seedOrLeaf ?
//                    (seedPop ? seed.removeRandom(rng) : seed.get(rng)) :
//                    (leafPop ? leaf.removeRandom(rng) : leaf.get(rng));
//        }
//
//    }
//    public class ScoringPremiseQueue extends PrioritySetPremiseQueue {
//        static final double TaskBonus = 0.25;
//        static final double QuestionBonus = 0.15;
//        static final double PremiseBonus = 0;
//        double score;
//
//        final Scoring scoring = new UCBScoring();
//
//        public ScoringPremiseQueue(Deriver d) {
//            super(d);
//        }
//
//        protected int key(Premise p) {
////            Class c = p.reactionType();
////            if (c == null) c = p.getClass();
////            return c.getSimpleName();
//            //return Integer.toString(p.reactionID());
//            return p.reactionID();
//        }
//        //new EWMAScoring();
//        //new HistogramScoring();
//
//        @Override
//        protected float pri(Premise x) {
//            var s = scoring.get(key(x));
//            if (s != s) return 1; //default
//            else {
////                float S = Util.lerp((float)(temperature * priMean),
////                        (float) s, 1);
//                var S = (float) s;
//                return S;
//            }
//        }
//
//        @Deprecated
//        private double mean() {
//            return this.scoring.mean();
//        }
//
//        @Override
//        protected void run(Premise p) {
//            score = 0;
//            super.run(p);
//            scoring.put(key(p), score);
////            var pp = p.parent;
////            double parentScore = score;
////            while (pp!=null) {
////                if (pp instanceof NALPremise.SeedTaskPremise) break;
////                parentScore *= parentRate;
////                scoring.add(key(pp), parentScore);
////                Premise ppNext = pp.parent;
////                pp = ppNext;
////            }
//        }
//
//        @Override
//        public void add(Premise x, Premise parent) {
//            super.add(x, parent);
//            score += PremiseBonus;
//        }
//
//        @Override
//        public void onDerived(NALTask x) {
//            super.onDerived(x);
//            score += x.BELIEF_OR_GOAL() ? TaskBonus : QuestionBonus;
//        }
//
//        abstract static class Scoring<K> {
//            abstract public double get(K key);
//
//            abstract public void put(K key, double score);
//
//            abstract public void add(K key, double score);
//
//            abstract public void clear();
//
//            public void print() {
//
//            }
//
//            public abstract double mean();
//        }
//
//        static class UCBScoring extends Scoring<Integer> {
//            private final UCBPrioritizer<Integer, Integer> prioritizer = new UCBPrioritizer<>() {
//                @Override
//                protected Integer type(Integer x) {
//                    return x;
//                }
//            };
//            //0.4f;
//            //0.1f;
//            //0.5f;
//            //0;
//            /**
//             * 0 disables softmax
//             */
//            float softmaxTemperature =
//                    0.25f;
//
//            @Override
//            public double get(Integer x) {
////                if (ThreadLocalRandom.current().nextFloat() < 0.001f)
////                    System.out.println(prioritizer.status());
//
//                var p = (float) prioritizer.pri(x) + Float.MIN_NORMAL;
//                //System.out.println(x + " " +  p);
//
//                var t = softmaxTemperature;
//                if (t > Float.MIN_NORMAL) {
//                    p = (float) Util.softmax(p, t);
//                }
//
//                //stats.accept(p);
//                return p;
//            }
//
//            @Override
//            public void put(Integer x, double score) {
//                prioritizer.update(x, score);
//            }
//
//            @Override
//            public void clear() {
//                prioritizer.clear();
//            }
//
//            @Override
//            public void add(Integer key, double score) {
//                throw new UnsupportedOperationException();
//            }
//
//            @Override
//            public double mean() {
//                throw new UnsupportedOperationException();
//            }
//        }
//
//        /**
//         * crude, doesn't credit parents
//         */
//        static class HistogramScoring extends Scoring<String> {
//            private static final float precision = 100.0f;
//            final Map<String, Histogram> scores = new HashMap<>();
//
//            private static float _score(float M) {
//                return M / precision;
//            }
//
//            private static long _score(double s) {
//                return Math.round(s * precision);
//            }
//
//            @Override
//            public double get(String x) {
//                var s = scores.get(x);
//                return s == null ? Double.NaN : _score((float) s.getMean());
//            }
//
//            @Override
//            public void clear() {
//                throw new TODO();
//            }
//
//            @Override
//            public double mean() {
//                return this.scores.values().stream().mapToDouble(AbstractHistogram::getMean).average().getAsDouble();
//            }
//
//            @Override
//            public void put(String x, double s) {
//                var S = scores.computeIfAbsent(x, X -> new Histogram(2));
//                if (S != null) S.recordValue(_score(s));
//            }
//
//            @Override
//            public void add(String key, double score) {
//                throw new TODO();
//            }
//        }
//
//        /**
//         * crude, doesn't credit parents
//         */
//        static class EWMAScoring extends Scoring<String> {
//            final Map<String, FloatMeanEwma> scores = new HashMap<>();
//            final Map<String, FloatMeanEwma> accum = new HashMap();
//            final float alpha = 0.1f;
//            final float alphaChildren = alpha;
//
//            @Override
//            public void print() {
//                synchronized (EWMAScoring.class) {
//                    scores.forEach((k, e) -> {
//                        System.out.println(k + "\t" + n4(e.mean()));
//                    });
//                }
//            }
//
//            @Override
//            public double mean() {
//                return this.scores.values().stream()
//                        .mapToDouble(FloatMeanEwma::mean).average()
//                        .orElse(0.5f);
//            }
//
//            @Override
//            public void clear() {
//                accum.clear();
//                scores.clear();
//            }
//
//            @Override
//            public double get(String x) {
//                var s = scores.get(x);
//                if (s == null) return Double.NaN;
//                return s.mean();
//            }
//
//            @Override
//            public void add(String key, double score) {
//                accum.computeIfAbsent(key, z -> new FloatMeanEwma(alphaChildren))
//                        .accept(score);
//            }
//
//            @Override
//            public void put(String x, double s) {
//                var aa = accum.get(x);
//                var accumMean = aa == null ? 0 : aa.mean();
//                scores.computeIfAbsent(x, X -> new FloatMeanEwma(alpha))//.reset(1))
//                        .accept(s + accumMean);
//            }
//
//        }
//    }
