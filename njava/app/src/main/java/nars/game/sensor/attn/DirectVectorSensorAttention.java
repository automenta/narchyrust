//package nars.game.sensor.attn;
//
//import com.google.common.collect.Iterators;
//import jcog.signal.FloatRange;
//import nars.Term;
//import nars.Focus;
//import nars.game.Game;
//import nars.game.sensor.SignalConcept;
//import nars.game.sensor.SignalComponent;
//import nars.game.sensor.VectorSensor;
//import nars.time.When;
//import nars.truth.Truther;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Iterator;
//
//public class DirectVectorSensorAttention extends VectorSensorAttention {
//
//    public final FloatRange updateRate = FloatRange.unit(1);
//    public final FloatRange activateRate = FloatRange.unit(1);
//
//    @Nullable private transient Iterator<SignalComponent> updateLoop, activateLoop;
//
//    public DirectVectorSensorAttention(VectorSensor v, Term why) {
//        super(v, why);
//    }
//
//
//    @Override public void input(When<Focus> ww, Truther truther, float resolution, float sleepDurs, Game g) {
//        int n = v.size();
//        int pixelsToUpdate = Math.round(updateRate.floatValue() * n);
//        if (pixelsToUpdate == 0) return;
//        if (updateLoop == null) updateLoop = Iterators.cycle(v::inputIterator);
//
//        int pixelsUpdated = 0, i = 0;
//        for (Iterator<SignalComponent> ii = updateLoop; i < n && ii.hasNext(); i++ ) {
//            if (input(ii.next(), ww, truther, resolution, sleepDurs, g)) {
//                if (++pixelsUpdated >= pixelsToUpdate)
//                    break; //done for this cycle
//            }
//        }
//    }
//
//    @Override
//    public void commit(VectorSensor v, When<Focus> w) {
//        int n = v.size();
//        int pixelsToActivate = Math.round(activateRate.floatValue() * n);
//        if (pixelsToActivate == 0) return;
//        if (activateLoop == null) activateLoop = Iterators.cycle(v::inputIterator);
//
//        float priEach = v.priComponent();
//
//        //commit() will clean the tables; so even if no active signals, they still may need cleaned
//        Focus f = w.x;
//
//        int pixelsActivated = 0, i = 0;
//        for (Iterator<SignalComponent> ii = activateLoop; i < n && ii.hasNext(); i++) {
//            SignalConcept s = ii.next();
//            s.remember(priEach, f);
//            if (++pixelsActivated >= pixelsToActivate)
//                break; //done for this cycle
//
//        }
//    }
//
//
//}