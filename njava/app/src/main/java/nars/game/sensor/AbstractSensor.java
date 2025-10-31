package nars.game.sensor;

import nars.NAL;
import nars.Term;
import nars.focus.PriAmp;
import nars.truth.Truther;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractSensor implements Sensor {
    public final PriAmp pri;
    public final Term id;

    public final Sensing sensing = new Sensing();

    protected AbstractSensor(Term id) {
        this.id = id;
        this.pri = new PriAmp(this);
    }

    @Override
    public final Term term() {
        return id;
    }

    @Override public final AbstractSensor freqRes(float v) {
        sensing.freqRes = v;
        return this;
    }

    @Override public final float resolution() {
        return sensing.freqRes;
    }

    @Override public final Truther truther() { return sensing.truther; }

    public final Sensor truther(@Nullable Truther t) {
        sensing.truther = t;
        return this;
    }

    public abstract int size();


//    private float pri(float pri, Truth prev, Truth next, float fRes) {
//        float priIfNoChange =
//                //ScalarValue.EPSILON; //min pri used if signal value remains the same
//                pri * pri;
//
//        if (prev == null)
//            return pri;
//        else {
////            float fDiff = next!=null ? Math.abs(next.freq() - prev.freq()) : 1f;
////            return Util.lerp(fDiff, priIfNoChange, pri);
//            if (next == null || Math.abs(next.freq()-prev.freq()) > fRes)
//                return pri;
//            else
//                return priIfNoChange;
//        }
//    }



    /** sensor parameter object */
    public final class Sensing {

        public float freqRes = NAL.truth.FREQ_EPSILON;

        @Nullable public Truther truther;

        /** sets priority post-filter */
        public void amp(float a) {
            pri.amp(a);
        }

        public float pri() {
            return pri.pri();
        }

    }
}