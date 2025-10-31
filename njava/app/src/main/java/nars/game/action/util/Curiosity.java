package nars.game.action.util;

import jcog.noise.SimplexNoise;
import jcog.random.RandomBits;
import jcog.signal.FloatRange;
import nars.NAL;
import nars.game.Game;
import nars.truth.AbstractMutableTruth;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import static nars.Op.GOAL;

public class Curiosity {

    public final FloatRange curiosityRateOn = FloatRange.unit(NAL.CURIOSITY_RATE_ON);
    public final FloatRange curiosityRateOff = FloatRange.unit(NAL.CURIOSITY_RATE_OFF);

//    /**
//     * curiosity period, in game durs. set to zero to disable curiosity
//     */
//    public final FloatRange curiosityDurs = new FloatRange(
//            NAL.CURIOSITY_DURS, 1, 1000);
//    /**
//     * time to disabling an enabled curiosity goal, in curiosity periods
//     */
//    public final FloatRange curiosityDutyCycle = new FloatRange(
//            NAL.CURIOSITY_DUTY_CYCLE, 0, 2);

    /**
     * TODO make a FloatRange
     * TODO tune according to strongest frequency in simplex noise?
     */
    @Deprecated
    public static final double curiosityNoiseFreq =
        //1/20f;
        1/15f;
        //1/10f;
        //1/100f;
        //0.5f;
        //1/1000f;

    private final SimplexNoise curiosityNoise = new SimplexNoise(ThreadLocalRandom.current().nextLong() /*id.hashCode()*/);

    public final AtomicBoolean enabled = new AtomicBoolean(true);

    //private int curiosityState = 0;

    public void curiosity(AbstractMutableTruth curi, Game g) {
        if (!enabled.getOpaque()) {
            curi.clear();
            return;
        }

        var rng = g.rng();
        if (curi.is()) {
            if (rng.nextBoolean(curiosityRateOff.floatValue())) {
                //DISABLE
                curi.clear();
            }
        } else {
            if (rng.nextBoolean(curiosityRateOn.floatValue())) {
                //ENABLE
                curi.conf(g.nar.confDefault(GOAL));
            }
        }

        if (curi.is())
            curi.freq(curiosityFreq(curi, g));
    }

//    public void curiosity(MutableTruth curi, Game g) {
//        var rng = g.rng();
//        float curiosityPeriod = curiosityDurs.floatValue();
//        boolean on = curi.is();
//
//        if (!on) {
//            if (curiosityState >= 0) {
//                double curiConf = g.nar.confDefault(GOAL);
//                curi.conf(curiConf);
//                on = true;
//                curiosityState = +curiosityInterval(curiosityPeriod * curiosityDutyCycle.floatValue(), rng);
//            } else {
//                curiosityState++;
//            }
//        }
//        if (on) {
//            if (curiosityState <= 0) {
//                curi.clear();
//                curiosityState = -curiosityInterval(curiosityPeriod * (1 - curiosityDutyCycle.floatValue()), rng);
//            } else {
//                curi.freq(curiosityFreq(curi, g));
//                curiosityState--;
//            }
//        }
//    }

    private static int curiosityInterval(float p, RandomBits rng) {
        return Math.max(1, rng.nextFloor(p*2)); //mean: p
        //return Math.max(1, rng.nextInt(p));      //mean: p/2
        //return Math.max(1, (int) rng.nextGaussian(p, curiosityStdDev));
    }

    private double curiosityFreq(Object x, Game g) {
        return noise(
            curiosityTime(x, g),
            System.identityHashCode(x)
        );
    }

    private double noise(float t, float x) {
        return curiosityNoise.noise(t, x);
    }

    private static float curiosityTime(Object x, Game g) {
        return (float) (curiosityNoiseFreq * (((double) g.time.s) / g.dur()));
    }

}
