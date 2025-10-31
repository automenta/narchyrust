//package nars.game.sensor.attn;
//
//import nars.Term;
//import nars.Focus;
//import nars.game.Game;
//import nars.game.sensor.SignalConcept;
//import nars.game.sensor.SignalComponent;
//import nars.game.sensor.VectorSensor;
//import nars.time.When;
//import nars.truth.Truther;
//
//public abstract class VectorSensorAttention {
//    protected final VectorSensor v;
//    public final Term why;
//
//    protected VectorSensorAttention(VectorSensor v, Term why) {
//        this.v = v;
//        this.why = why;
//    }
//
//    public abstract void commit(VectorSensor v, When<Focus> w);
//
//    public void input(When<Focus> ww, Truther truther, float resolution, float sleepDurs, @Deprecated Game g) {
//        for (var ii = v.inputIterator(); ii.hasNext(); )
//            input(ii.next(), ww, truther, resolution, sleepDurs, g);
//    }
//
//    protected final boolean input(SignalComponent s, When<Focus> ww, Truther truther, float resolution, float sleepDurs, @Deprecated Game g) {
//        if (s.input(truther.truth(s.value(g), resolution), why, sleepDurs, ww)) {
//            activate(s);
//            return true;
//        } else {
//            return false;
//        }
//
//    }
//
//}