//package nars.derive;
//
//import jcog.Util;
//import jcog.exe.flow.Feedback;
//import jcog.util.ArrayUtil;
//import nars.NAL;
//import nars.NAR;
//import nars.Term;
//import nars.control.Cause;
//import nars.control.Why;
//import nars.derive.impl.BagDeriver;
//import nars.derive.reaction.ReactionModel;
//import nars.time.part.DurLoop;
//import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.WeakHashMap;
//
//import static jcog.Util.lerpSafe;
//
//public class OptiBagDeriver extends BagDeriver {
//
//    private static final float UPDATE_DURS = 4;
//    static final float momentum =
//            0.99f;
//            //0.95f;
////            0.85f;
////            0.75f;
////            0.5f;
//            //-0.05f;
//
//    static {
//        NAL.causeCapacity.set(16);
//    }
//
//    static final boolean trace = false;
//
//    static final FeedbackUpdate u = new FeedbackUpdate();
//
//    public OptiBagDeriver(ReactionModel reactionModel, NAR nar) {
//        super(reactionModel, nar);
//        nar.add(u);
//    }
//
//    @Override
//    protected void next() {
//        Feedback.set(feedback);
//        super.next();
//    }
//
//    static final class Stats<X> implements ShortProcedure {
//        final X x;
//        float[] value = ArrayUtil.EMPTY_FLOAT_ARRAY;
//
//        Stats(X x) {
//            this.x = x;
//        }
//
//        @Override
//        public String toString() {
//            return x + "=" + Arrays.toString(value);
//        }
//
//
//        private void ensureCapacity(int s) {
//            if (value.length < s) value = Arrays.copyOf(value, s);
//        }
//
//        public void absorb(Stats v, int n) {
//            assert(this!=v);
//
//            float[] vy = v.value;
//            ensureCapacity(n);
//            n = Math.min(vy.length, n);
//            float[] vx = this.value;
//            for (int i = 0; i < n; i++) {
//                //TODO atomic
//                vx[i] += vy[i];
//                vy[i] = 0;
//            }
//        }
//
//        /** add(s) */
//        @Override public void value(short s) {
//            //TODO atomic
//            ensureCapacity(s+1);
//            value[s]++;
//        }
//    }
//
//
//
//    static final WeakHashMap<Thread,Map<String,Stats>> subStats = new WeakHashMap();
//
//    static final Feedback<Map<String,Stats>, Object, Object> feedback = new Feedback<>() {
//
//
////        CreditControlModel model = new CreditControlModel((x,y,z)->{}) {
////
////        };
//
//        @Override
//        public void accept(Map<String,Stats> ctx, Object cause, Object result) {
//            //ctx.learn((short)result.ordinal(), cause, 1);
//            if (cause!=null) {
//
//                if (trace)
//                    System.out.println(Thread.currentThread().getName() + "\t" + cause + " " + result);
//
//                Why.each((Term)cause, ctx.computeIfAbsent((String) result, Stats::new));
//
//                //Why.eval(why, strength, n.control.why.array(), (w, p, CC)->learn(CC[w].id, what, p));
//                ///model.learn
//            }
//        }
//
//        @Override
//        public Map<String,Stats> newContext() {
//            Map<String,Stats> m = new HashMap<>();
//            synchronized(subStats) {
//                subStats.put(Thread.currentThread(), m);
//            }
//            return m;
//        }
//    };
//
//    private static class FeedbackUpdate extends DurLoop {
//        private final Map<String,Stats> totals = new HashMap();
//
//        {
//            durs(UPDATE_DURS);
//        }
//
//        private double amp(String x) {
//            return switch(x) {
//                case "derive." -> +0.2;
//                case "derive!" -> +0.3;
//                case "derive?" -> +0.05;
//                case "derive@" -> +0.05;
//                case "derive.premise" -> -0.01;
////            case "derive.NALTask.invalid" -> -0.2;
////            case "derive.premise.invalid" -> -0.2;
////            case "derive.NALTask.failTaskTerm" -> -0.5;
////            case "derive.NALTask.failTaskify" -> -0.5;
////            case "derive.NALTask.equalParent" -> -0.5;
//                default -> 0;
//            };
//        }
//
//        double[] value = ArrayUtil.EMPTY_DOUBLE_ARRAY;
//        double[] valuePrev = ArrayUtil.EMPTY_DOUBLE_ARRAY;
//        double[] pri = ArrayUtil.EMPTY_DOUBLE_ARRAY;
//
//        @Override
//        public void accept(NAR N) {
//            Cause[] cc = nar.control.why.array();
//            int n = Math.min(cc.length, nar.control.why.size());
//            if (n == 0) return;
//
//            if (value.length!=n) {
//                value = new double[n]; //restart
//                valuePrev = new double[n];
//                pri = new double[n];
//            }
//
//            subStats.values().forEach(m -> m.forEach((k, v) -> totals.computeIfAbsent(k, Stats::new).absorb(v, n)));
//
//            //pivot to calculate cause scores
//
//            System.arraycopy(value, 0, valuePrev, 0, n);
//            Arrays.fill(value, 0);
//
//            totals.forEach((x, xStat) -> {
//                double a = amp(x);
//                if (a == 0) return;
//
//                float[] xValue = xStat.value;
//
//                float[] minmax = Util.minmax(xValue);
//                float min = minmax[0], max = minmax[1];
//                if (!Util.equals(min, max)) {
//                    double r = max - min;
//                    for (int c = 0; c < n; c++) {
//                        double vxc = (xValue[c] - min) / r;
//                        value[c] += a * vxc;
//                        xValue[c] = 0;
//                    }
//                }
//            });
//            for (int c = 0; c < n; c++) {
//                value[c] = lerpSafe(momentum, value[c], valuePrev[c]);
//            }
//
//            System.arraycopy(value, 0, pri, 0, n);
////            Util.normalizeCartesian(pri, n, Double.MIN_NORMAL);
//            Util.normalize(pri, n);
//
//            for (int i = 0; i < n; i++) {
//                Cause cci = cc[i];
//                cci.pri = (float) pri[i];
//                cci.value = (float) value[i];
//            }
//
//        }
//    }
//}