//package nars.control;
//
//import jcog.Util;
//import jcog.activation.TanhActivation;
//import jcog.agent.SensorBuilder;
//import jcog.agent.SensorTensor;
//import jcog.data.list.Lst;
//import jcog.signal.FloatRange;
//import jcog.nn.MLP;
//import jcog.pri.Prioritized;
//import jcog.signal.Tensor;
//import jcog.signal.tensor.ArrayTensor;
//import jcog.util.ArrayUtil;
//import nars.NAR;
//import nars.control.model.CreditControlModel;
//
//import java.util.Random;
//
//public class MLPGovernor implements CreditControlModel.Governor {
//
//    private final ArrayTensor wants = new ArrayTensor(MetaGoal.values().length);
//
//    static final int history = 0;
//
//    public final FloatRange learningRate = new FloatRange(0.01f, 0, 0.25f);
//    public final FloatRange explorationRate = new FloatRange(0.25f, 0, 1f);
//
//    float[] f = ArrayUtil.EMPTY_FLOAT_ARRAY;
//    float[] g = ArrayUtil.EMPTY_FLOAT_ARRAY;
//    float[] fNorm = ArrayUtil.EMPTY_FLOAT_ARRAY;
//    public Predictor[] predictor = new Predictor[0];
//
//    @Deprecated
//    static final int dims = MetaGoal.values().length;
//
//
//    /**
//     * value vector -> priority for an individual cause, independently learned
//     * 1 hidden layer to be able to solve ~XOR problem
//     * "A limitation of single layer perceptron is that it is only capable of separating data points with a single line."
//     * http://www.mlopt.com/?p=160
//     */
//    public static final class Predictor {
//
//        private final ArrayTensor value;
//
//        final SensorTensor in;
//        private final MLP mlp;
//
//        Predictor(Tensor want, int history) {
//
//            in = new SensorBuilder().history(history).in(want).in(value = new ArrayTensor(1)).sensor();
//
//            mlp = new MLP(
//                    in.volume(),
//                    new MLP.Layer(in.volume(), TanhActivation.the),
//                    new MLP.Layer(1, TanhActivation.the)
//            );
//
//
////				this.h = history;
////				if (history > 0) {
////					this.history = new TensorRing(new ArrayTensor(history), 1, history);
////					for (int i = 0; i < history; i++) this.history.setSpin(0);
////				} else
////					this.history = null;
//        }
//
//        public float learn(float v, float specificLearningRate) {
//            double[] out = mlp.put(in.update(), new double[]{v}, specificLearningRate);
//            value.setAt(0, v);
//
//            //return unitizeSafe(out[0]);
//            return (float) Util.clampSafe(out[0], -1, +1);
//        }
//    }
//
//    private void allocate(NAR n, int ww) {
//        f = new float[ww];
//        fNorm = new float[ww];
//        g = new float[ww];
//        predictor = new Predictor[ww];
//        Random rng = n.random();
//        for (int p = 0; p < ww; p++)
//            (predictor[p] = new Predictor(wants, history)).mlp.randomize(rng);
//    }
//
//    @Override
//    public void accept(double[] want, Lst<Cause> cc, NAR n) {
//
//        Cause[] c = cc.array();
//        int ww = Math.min(c.length, cc.size());
//        if (f.length != ww)
//            allocate(n, ww);
//
//        wants.setAll(want);
//        Util.normalizeCartesian(want, Prioritized.EPSILON);
//
//        //2. learn
//        float inMin = Float.POSITIVE_INFINITY, inMax = Float.NEGATIVE_INFINITY;
//        for (int i = 0; i < ww; i++) {
//            Cause ci = c[i];
//            float v = ci.value;
//            f[i] = v;
//            inMin = Math.min(v, inMin);
//            inMax = Math.max(v, inMax);
//        }
//
//        float range = inMax - inMin;
//        if (range < Prioritized.EPSILON) {
////                        float flat = 1f/ww;
////                        for (int i = 0; i < ww; i++)
////                            c[i].pri(flat);
//        }
//
//        {
//
//
//            //double errTotal = 0;
//            float outMin = Float.POSITIVE_INFINITY, outMax = Float.NEGATIVE_INFINITY;
////				double priSum = 0;
//            float specificLearningRate =
//                    learningRate.floatValue();
////						learningRate * Math.max(0.1f,
////							//Math.abs(0.5f-fNorm[i])*2f
////							Math.abs(f[i]) / Math.max(Math.abs(min), Math.abs(max)) //extremeness
////						);
//
//            float inRange = Math.max(Math.abs(inMin), Math.abs(inMax));
//
//            for (int i = 0; i < ww; i++) {
//
//                float fI = f[i];
//
//                fNorm[i] = fI >= 0 ?
//                        Util.normalizeSafe(fI, 0, +inRange)
//                        :
//                        Util.normalizeSafe(fI, -inRange, 0);
//
//
//                Predictor P = this.predictor[i];
//                float pIn = fNorm[i];
//                float pOut = P.learn(pIn, specificLearningRate);
//                g[i] = pOut;
////					priSum += ();
//                if (pOut < outMin) outMin = pOut;
//                if (pOut > outMax) outMax = pOut;
//                //Util.lerpSafe(pri, explorationRate, 1f)
//
//                //errTotal += P.mlp.errorAbs();
//            }
//            //double errAvg = errTotal / ww;
//
//            //System.out.println(inMin + " " + inMax + " | " + outMin + " " + outMax);
//            float priRange = outMax - outMin;
//            float epsilon = Prioritized.EPSILON * ww;
//            if (priRange < epsilon || outMax < epsilon) {
//                //flat
//                for (int i = 0; i < ww; i++)
//                    c[i].pri(0.5f);
//            } else {
//                //float priAvg = (float)( priSum / ww);
//                float xp = this.explorationRate.floatValue() * outMax;
//                for (int i = 0; i < ww; i++) {
//                    //float p = (g[i] - outMin) / priRange;
//                    float p = g[i];
//                    c[i].pri(Math.max(xp, p));
//                }
//            }
//
//
//            //if(PRINT_AVG_ERR && n.random().nextFloat() < 0.03f)
//            //System.out.println(this + ":\t" + errAvg + " avg err");
//        }
//        //System.out.println(n4(min) + " " + n4(max) + "\t" + n4(nmin) + " " + n4(nmax));
//
//
//    }
//
//
//}