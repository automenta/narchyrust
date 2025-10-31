//package nars.truth.proj;
//
//import nars.NALTask;
//import nars.truth.evi.EviInterval;
//
///** experimental */
//public class RelativeTruthProjection extends LinearTruthProjection {
//
//    private final long now;
//
//
////
////    public RelativeTruthProjection(int capacity, long now) {
////        this(TIMELESS, TIMELESS, capacity, now);
////    }
////
////    public RelativeTruthProjection(long start, long end, int capacity, long now) {
////        this(start, end, capacity, now, 1);
////    }
//
//    public RelativeTruthProjection(long start, long end, int capacity, long now) {
//        super(start, end, capacity);
//        this.now = now;
//    }
//
//    @Override
//    protected boolean computeComponents(NALTask[] tasks, int from, int to, EviInterval at, double[] evi) {
//        long now = this.now;
//        long S = at.s, E = at.e;
//        double range = S == ETERNAL ? 1 : 1 + E - S;
//        for (int i = from; i < to; i++) {
//            double ei = tasks[i].evi(now, S, E, 0, 0);
//            evi[i] = ei * range;
//        }
//        return true; //TODO return false when unchanged
//    }
//
//}