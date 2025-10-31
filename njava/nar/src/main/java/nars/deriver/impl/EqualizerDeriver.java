//package nars.derive.impl;
//
//import jcog.data.list.CircularArrayList;
//import jcog.data.list.Lst;
//import jcog.data.map.MRUMap;
//import jcog.data.map.UnifriedMap;
//import jcog.math.RecycledSummaryStatistics;
//import jcog.signal.IntRange;
//import nars.NAR;
//import nars.Deriver;
//import nars.derive.reaction.Reaction;
//import nars.derive.reaction.ReactionModel;
//import nars.Focus;
//import nars.Premise;
//
//import java.util.Map;
//import java.util.function.Function;
//import java.util.random.RandomGenerator;
//
//public class EqualizerDeriver extends Deriver {
//
//    protected static class PremiseQueue {
//        public final CircularArrayList<Premise> queue;
//
//        public PremiseQueue(int cap) {
//            queue = new CircularArrayList<>(new Premise[cap]);
//        }
//
//        public void accept(Premise p, RandomGenerator rng) {
//            int cap = capacity();
//            while (size() >= cap)
//                queue.pollRandom(rng);
//
//            accept(p);
//        }
//
//        public void accept(Premise p) {
//            queue.addLast(p);
//        }
//
//        public Premise next() {
//            return queue.pollFirst();
//            //queue.pollRandom(rng);
//        }
//
//        public void clear() {
//            queue.clear();
//        }
//
//        public int capacity() {
//            return queue.capacity();
//        }
//
//        public int size() {
//            return queue.size();
//        }
//    }
//
//    /**
//     * breadth
//     */
//    public final IntRange iter = new IntRange(16, 1, 512);
//
//    /**
//     * depth
//     */
//    public final IntRange subIter = new IntRange(iter.intValue()/2, 1, 32);
//
////    public final IntRange cap = new IntRange(32, 1, 512);
//
//    final MRUMap<Premise, Premise> cache;
//
//    final Map<String, PremiseQueue> queue;
//
//    /** separate list of created queues, for fast iteration */
//    final Lst<PremiseQueue> queues = new Lst<>();
//
//    @Deprecated int queueCap = iter.intValue();
//
//    /** TODO more efficient: retain data structure */
//    @Override protected void start(Focus f) {
//        queues.clear();
//        queue.clear();
//    }
//
//    private final Function<? super String, ? extends PremiseQueue> subQueue =
//        s -> {
//            PremiseQueue q = new PremiseQueue(queueCap);
//            queues.add(q);
//            return q;
//        };
//
//    public EqualizerDeriver(ReactionModel model, NAR nar) {
//        super(model, nar);
//        queue = new UnifriedMap<>(16);
//        cache = new MRUMap<>(16*1024);
//    }
//
//    protected String classify(Premise p) {
//        Reaction r = p.reaction();
//        return r == null ? p.getClass().getName() : r.type().toString();
//    }
//
//    @Override
//    protected void acceptPremise(Premise p) {
//        queue.computeIfAbsent(classify(p), subQueue)
//            .accept(cached(p), rng);
//    }
//
//    private Premise cached(Premise x) {
//        var y = cache.get(x);
//        if (y == null) {
//            cache.put(x, x);
//            return x;
//        } else
//            return y;
//    }
//    final RecycledSummaryStatistics queueStats = new RecycledSummaryStatistics();
//
//    @Override
//    protected void next() {
//        int seeds = this.iter.intValue(), subIter = this.subIter.intValue();
//        var ff = focus.sampleUnique(rng);
//        int i = 0;
//        while (i < seeds && ff.hasNext()) {
//            run(ff.next());
//            i++;
//        }
//
//        int nQueues = queues.size();
//        if (nQueues > 0) {
//            queueStats.clear();
//            for (var premiseQueue : queues)
//                queueStats.accept(premiseQueue.size());
//
//
//            int totalIters = seeds * subIter;
//
//            int empties = 0, ran = 0;
//
//            queues.shuffleThis(rng);
//
//            for (int qq = 0; ran < totalIters && empties < nQueues; qq++) {
//                int Q = qq % nQueues;
//
//                var q = queues.get(Q);
//
//                //TODO randFloor for how many to run from Q
//                Premise next = q.next();
//                if (next == null) {
//                    empties++;
//                } else {
//                    ran++;
//                    run(next);
//                }
//            }
//        }
//    }
//
//}