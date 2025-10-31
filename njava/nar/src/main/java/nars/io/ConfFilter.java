package nars.io;

import jcog.signal.FloatRange;
import nars.NALTask;
import nars.Op;
import org.HdrHistogram.ConcurrentDoubleHistogram;
import org.HdrHistogram.DoubleHistogram;

import java.util.concurrent.atomic.AtomicInteger;

public class ConfFilter extends TaskInput {

    private static final double epsilon =
        //1.0e-4;
        //1.0e-5;
        1.0e-9;
        //NAL.truth.CONF_MIN * 2;
        //NAL.truth.CONF_MIN;

//    private static final float rangeDursMax = 1.0E3f;

    /** whether confBelief==confGoal (shared) */
    private static final boolean joint  = false;

    private static final boolean deleteReject = false;

    private static final boolean trace  = false;

    private DoubleHistogram vBelief, vGoal;

    private static final int updatePeriod = 4096, resetPeriod = updatePeriod * 100;

    public final FloatRange percentileThresh = FloatRange.unit(
        //0.01f
        //0.05f
        0.10f
        //0.25f
        //1/3f
        //1/2f
        //0.5f
    );

    volatile double vMinBelief = 0, vMinGoal = 0;

    private final AtomicInteger iterBelief = new AtomicInteger(), iterGoal = joint ? iterBelief : new AtomicInteger();

    private final TaskInput proxy;

    public ConfFilter(TaskInput i) {
        this.proxy = i;
        this.f = proxy.f; //HACK
        reset(true);
        reset(false);
    }

    private DoubleHistogram newHistogram() {
        return new ConcurrentDoubleHistogram(
            (int) (1.0 / epsilon), 5
        );
    }

    @Override
    public void remember(NALTask x) {
        DoubleHistogram vv;
        byte punc = x.punc();
        boolean beliefOrGoal;
        AtomicInteger iter;
        switch (punc) {
            case Op.BELIEF -> { beliefOrGoal = true; vv = this.vBelief; iter = iterBelief; }
            case Op.GOAL -> { beliefOrGoal = false; vv = this.vGoal; iter = iterGoal; }
            default -> { beliefOrGoal = false; vv = null; iter = null; }
        }
        if (vv!=null) {
            double v = value(x);
            boolean reject;

            if (v < Double.POSITIVE_INFINITY) {
                reject = v < (beliefOrGoal ? vMinBelief : vMinGoal);
                vv.recordValue(
                    epsilon + v
                    //Util.max(minConfDiscernable, v)
                );

                long n = iter.incrementAndGet();
                if (n % resetPeriod == 0) {
                    reset(beliefOrGoal);
                } else if (n % updatePeriod == 0) {
                    update(beliefOrGoal);
                }
            } else {
                reject = false;
            }

            if (reject) {
                if (deleteReject)
                    x.delete();
                return; //drop
            }
        }
        proxy.accept(x);
    }

    private double value(NALTask x) {
        return x.conf();
        //return x.evi();
        //return x.ETERNAL() ? Double.POSITIVE_INFINITY : x.conf() * Util.min(x.range()/f.dur(), rangeDursMax); //TODO handle ETERNAL
    }

    private void update(boolean beliefOrGoal) {
        float percentile = percentileThresh.floatValue() * 100;
        if (beliefOrGoal) {
            vMinBelief = valueAtPercentile(percentile, true);
        } else {
            vMinGoal = joint ? vMinBelief : valueAtPercentile(percentile, false);
        }

        if (trace)
            System.out.println("conf_min: B=" + vMinBelief + " G=" + vMinGoal);
//        System.out.println("conf_min: B=" + confMinBelief + " G=" + confMinGoal + "\n" +
//                Str.histogramString(confBelief, 20));
    }

    private double valueAtPercentile(float percentile, boolean beliefOrGoal) {
        return (beliefOrGoal ? vBelief : vGoal).getValueAtPercentile(percentile) - epsilon;
    }

    private synchronized void reset(boolean beliefOrGoal) {
        if (beliefOrGoal) {
            //confBelief.reset(); //DOESNT WORK
            vBelief = newHistogram();
            vMinBelief = 0;
        } else {
            vGoal = joint ? vBelief : newHistogram();
            vMinGoal = 0;
        }

    }

//    @Override
//    public boolean rememberNow(NALTask x, boolean activate) {
//        return proxy.rememberNow(x, activate);
//    }
}
