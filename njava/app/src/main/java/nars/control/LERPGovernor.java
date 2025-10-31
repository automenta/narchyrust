//package nars.control;
//
//import jcog.Util;
//import jcog.data.DistanceFunction;
//import jcog.data.list.Lst;
//import jcog.util.ArrayUtil;
//import nars.NAR;
//import nars.control.model.CreditControlModel;
//
//import static jcog.Util.lerpSafe;
//
///**
// * applies simple LERP memory to each computed value
// */
//public class LERPGovernor implements CreditControlModel.Governor {
//
//    double[] w = new double[MetaGoal.values().length];
//    float[] f = ArrayUtil.EMPTY_FLOAT_ARRAY;
//    float explorationRate = 0.25f;
//    float momentum = 0.8f;
//
//    @Override
//    public void accept(double[] want, Lst<Cause> cc, NAR n) {
//
//        Cause[] c = cc.array();
//        int ww = Math.min(c.length, cc.size());
//        if (f.length != ww)
//            f = new float[ww];
//
//        //reduce effective momentum by the change in metagoal vector
//        double[] wNext = want;
//        float wDiff = (float) Math.sqrt(DistanceFunction.distanceCartesianSq(w, wNext));
//        float m = momentum * Math.max(0, 1 - wDiff);
//        System.arraycopy(wNext, 0, w, 0, w.length);
//
//        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
//        for (int i = 0; i < ww; i++) {
//            Cause ci = c[i];
//
//            float r = ci.value;
//
//            float v;
//            //unchanged, hold existing value
//            v = /*r == r ?*/ (f[i] = lerpSafe(m, r, f[i])) /*: f[i]*/;
//
//            min = Math.min(v, min);
//            max = Math.max(v, max);
//        }
//
////                    System.out.println(min + "\t" + max);
//
//        if (Util.equals(min, max)) {
//            for (int i = 0; i < ww; i++)
//                c[i].pri(explorationRate); //flat
//        } else {
//            min -= explorationRate * (max - min);
//            for (int i = 0; i < ww; i++)
//                c[i].pri(Util.normalizeSafe(f[i], min, max) * 2);
//        }
//    }
//
//}