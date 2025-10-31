//package nars.game.sensor.attn;
//
//import jcog.signal.FloatRange;
//import nars.Term;
//import nars.Focus;
//import nars.game.sensor.SignalConcept;
//import nars.game.sensor.VectorSensor;
//import nars.time.When;
//
//import java.util.ArrayDeque;
//import java.util.Deque;
//
///**
// * FIFO
// */
//public class QueueVectorSensorAttention extends VectorSensorAttention /*implements Controllable*/ {
//
//    @Deprecated public static final int RATE_MAX_DEFAULT =
//        32;
//        //16;
//        //64;
//
//    final Deque<SignalConcept> queue = new ArrayDeque<>();
//
//    public final FloatRange activateRate = new FloatRange(0.5f, 0, 1);
//
//
//    public QueueVectorSensorAttention(VectorSensor v, Term why) {
//        super(v, why);
//    }
//
//    @Override
//    public void activate(SignalConcept s) {
//        boolean alreadyActive = s.activateIfDeactivated();
//        if (!alreadyActive)
//            queue.addFirst(s);
//
//    }
//
//    @Override
//    public void commit(VectorSensor v, When<Focus> w) {
//        int pending = queue.size();
//        if (pending==0)
//            return;
//
//        int total = v.size();
//        int toActivate = Math.min(pending, activationLimit(pending, total));
//
//        activate(v.priComponent(), toActivate, w.x);
//        trim(total);
//
//        //System.out.println(v + " amp=" + n4(v.pri.amp.floatValue()) + " activated=" + toActivate + ", queue=" + queue.size());
//    }
//
//    protected void activate(float priEach, int toActivate, Focus ww) {
//        for (int i = toActivate; i > 0; i--)
//            pop().remember(priEach, ww);
//    }
//
//    private void trim(int total) {
//        //trim queue to capacity
//        int toRemove = queue.size() - total;
//        while (toRemove-- > 0)
//            pop();
//    }
//
//    private int activationLimit(int pending, int total) {
//        return (int) Math.ceil(total * activateRate.floatValue());
//        //return (int) Math.ceil(total * amp * inputRate.floatValue());
//    }
//
//    SignalConcept pop() {
//        SignalConcept s = queue.removeLast();
//        s.deactivate();
//        return s;
//    }
//
//    public void rateMax(int m) {
//        /* HACK */
//        int s = v.size();
//        if (s <= m) activateRate.set(1);
//        else activateRate.set( ((float)m) / s );
//    }
//
////        @Override
////        public void controlled(Game g) {
////            g.actionUnipolar()
////        }
//}