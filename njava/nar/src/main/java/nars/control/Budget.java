package nars.control;

import jcog.Is;
import nars.Deriver;
import nars.Focus;
import nars.NALTask;
import nars.Truth;
import org.jetbrains.annotations.Nullable;

import static nars.TruthFunctions.c2e;
import static nars.TruthFunctions.confCompose;

/**
 * attention management strategy
 * http://sol.gfxile.net/interpolation/
 */
@Is({"Occam's_razor","Occam's_superglue","Economy","Attention"})
public abstract class Budget  {

    public static double eviParents(@Nullable Truth x, @Nullable Truth y) {
        if (x == null && y == null)
            return 0;
        else if (x == null)
            return y.evi();
        else if (y == null)
            return x.evi();
        else {
            return
                c2e(confCompose(x, y)); //exact

                //Util.max( //stricter
                //Util.min( //relaxed
//                Fuzzy.mean(
//                //plus( //most strict
//                  x.evi(), y.evi()
//                );
        }
    }

//    /**
//     * meta-ockham
//     * 'plurality should be posited with as much as is preferred.'
//     * @param v volume of the item
//*    * @param vMax max volume allowed
//     * @param vTgtPct target volume, in  [0..1] proportion to vMax
//     * @param intensity parameter affecting degree of specificity to the target
//     */
//    public static double simpleIdeal(float v, float vTgtPct, float vMax, float intensity) {
//        return 1 / (1 + intensity * unitizeSafe(Math.abs(v - ((1 - vTgtPct) * vMax))/vMax) );
//    }

//    public static double simple(float vol, int volMax, float power) {
//        //return simpleStep(vol, volMax, power);
//        return simplePow(vol, volMax, power);
//    }
//
//    /** smoothstep */
//    private static double simpleStep(float vol, int volMax, float power) {
//        return Util.smootherstep(1-(power*vol/(volMax+1)));
//    }
//    /**
//     * ockham's razor: 'plurality should not be posited without necessity.'
//     */
//    private static double simplePow(float vol, int volMax, float power) {
//
//        double cost =
//            vol / (volMax + 1); //linear
//            //Math.pow(vol / (volMax + 1), 1/2f); //power
//            //vol < 5 ? 0 : 1; //step
//
//
//        @Deprecated float powerScale =
//            //2; //maps unit input 0..1 to 0..2
//            1;
//
//        double y = Math.pow(1 - unitizeSafe(cost), power * powerScale);
//
//        //System.out.println(vol + "/" + volMax + " @ " + power + " = " + y);
//        return y;
//        //return Math.pow(1 - (clampSafe(vol, 0, volMax) / (volMax + 1)), power);
//        //return 1 - Math.pow(unitizeSafe(vol / ((float) volMax + 1)), (1/power));
//
////        float strength =
////                1; /* linear */
////                //2; /* quadratic */
////                //4;
////                //16;
////        return 1 - Math.pow(unitize(vol / (volMax+1)), 1/strength) * (1 - power);
////        //return 1 - unitize(vol / (volMax+1)) * (1 - power); //linear
//    }
//    //PriMerge.plus;

    /** priority of derived NALTask */
	public final float priDerived(NALTask t, Deriver d) {
        var p = d.premise;
        return priDerived(t, p.task(), p.belief(), d);
	}

	public abstract float priDerived(NALTask xy, @Nullable NALTask x, @Nullable NALTask y, Object d);

    public abstract float priIn(NALTask t, Focus f);

//	/** priority of derived premise (feedback) */
//	public abstract double priPremise(Premise p, Deriver d);

//    /**
//     * input tasklink priority
//     */
//    public abstract double priIn(NALTask t, Focus f);
}