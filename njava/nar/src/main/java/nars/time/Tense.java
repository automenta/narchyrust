package nars.time;

import jcog.Is;
import jcog.Util;
import jcog.WTF;
import jcog.math.LongInterval;
import nars.NAL;
import nars.Op;
import nars.Term;
import nars.term.Termlike;

import java.util.random.RandomGenerator;

import static jcog.math.LongInterval.ETERNAL;
import static jcog.math.LongInterval.TIMELESS;


/**
 * tense modes, and various Temporal utility functions and constants
 */
@Deprecated public enum Tense {

    Eternal(":-:"),
    Present("|"),
    Past(":\\:"),
    Future(":/:");

    private final String symbol;

    Tense(String string) {
        symbol = string;
    }

//
//    /**
//     * if (relative) event B after (stationary) event A then order=forward;
//     * event B before       then order=backward
//     * occur at the same time, relative to duration: order = concurrent
//     */
//    public static boolean simultaneous(long a, long b, float tolerance) {
//		return a == ETERNAL || b == ETERNAL || Math.abs(a - b) <= tolerance;
//    }





    /** direction: -1 half-dither round down, 0 = normal dithering , +1 = half-dither round up */
    @Deprecated public static long dither(long x, int dither, int rounding) {
        return dither > 1 && x != 0 && x != ETERNAL && x != Op.TIMELESS ?
                dither(x + rounding * Math.round(dither/4.0), dither) :
                x;
    }

//    private  static long _dither(long x, int dither, double offset) {
//        return _dither(x + offset, dither);
//    }


//    /** TODO test */
//    static long ditherLogarithmic(long t, boolean relative, int dither) {
//        //return Util.round(t, dither);
//		//logarithmic dithering
//		return Util.round(relative && NAL.DT_DITHER_LOGARITHMICALLY && t > dither * dither ? Math.pow(dither, Util.round(Math.log(t) / Math.log(dither), 1f / dither)) : (double) t, dither);
//    }

    public static long[] dither(long[] t, NAL n) {
        return dither(t, n.timeRes());
    }
    
    /** modifies the input array, and returns it */
    public static long[] dither(long[] t, int dt) {
        if (dt > 1) {
            long s = t[0];
            if (s != ETERNAL && s != Op.TIMELESS) {
                long e = t[1];
                if (e == s) {
                    t[0] = t[1] = dither(s, dt); //same value for the point
                } else {
                    t[0] = dither(s, dt/*, -1*/);
                    t[1] = dither(e, dt/*, +1*/);
                }
            }
        }
        return t;
    }

    public static int dither(int t, int dither) {
        return switch (t) {
            case Op.DTERNAL, Op.XTERNAL, 0 -> t;
            default -> dither <= 1 ? t : (int)Util.round(t, dither);
        };
    }

    @Is("Nyquist_frequency")
    public static long dither(long t, int dither) {
        return t == ETERNAL || t == Op.TIMELESS ? t : Util.round(t, dither);
    }


//    public static long[] union(TaskRegion... tt) {
//        switch (tt.length) {
//            case 0: throw new UnsupportedOperationException();
//            case 1: return new long[] { tt[0].start(), tt[0].end() };
//            //TODO will work if it handles eternal
//            case 2:
//                long as = tt[0].start();
//                long bs = tt[1].start();
//                if (as != ETERNAL && bs != ETERNAL) {
//                    return new long[]{Math.min(as, bs), Math.max(tt[0].end(), tt[1].end())};
//                }
//
//        }
//        return union(ArrayIterator.iterable(tt));
//    }

//    public static long[] union(Iterator<? extends TaskRegion> t) {
//        long start = MAX_VALUE, end = Long.MIN_VALUE;
//
//        while (t.hasNext()) {
//            TaskRegion x = t.next();
//            //if (x == null) continue;
//            long xs = x.start();
//            if (xs != ETERNAL) {
//                start = Math.min(xs, start);
//                end = Math.max(x.end(), end);
//            }
//        }
//
//        if (start == MAX_VALUE)
//            start = end = ETERNAL;
//
//        return new long[] { start, end };
//    }
//
//    private static @Nullable long[] intersect(Iterable<? extends TaskRegion> t) {
//        long start = MAX_VALUE, end = Long.MIN_VALUE;
//
//        for (TaskRegion x : t) {
//
//            long xs = x.start();
//            if (xs != ETERNAL) {
//                long xe = x.end();
//                if (start==MAX_VALUE) {
//                    //first
//                    start = xs;
//                    end = xe;
//                } else {
//                    @Nullable LongInterval l = Longerval.intersection(start, end, xs, xe);
//                    if (l==null)
//                        return null;
//
//                    start = Math.max(xs, start);
//                    end = Math.min(xe, end);
//                }
//            }
//        }
//
//        if (start == MAX_VALUE)
//            start = end = ETERNAL;
//
//        return new long[] { start, end };
//    }


    /** TODO switch to use !Term.SEQ() which has alternate semantics for XTERNAL */
    @Deprecated public static boolean parallel(int dt) {
        return switch (dt) {
            case 0, Op.DTERNAL, Op.XTERNAL -> true;
            default -> false;
        };
    }


    /** safely transform occ (64-bit) to dt (32-bit) */
    public static int occToDT(long occ) {
        if (occ == ETERNAL)
            return Op.DTERNAL;
        else if (occ == Op.TIMELESS)
            return Op.XTERNAL;
        else
            return Util.longToInt(occ);
    }

//    public static int occToDT(long occ, int dither) {
//        return occToDT(dither(occ, dither));
//    }


//    /** computes an ideal range of time for a merge or revision of tasks.
//     * assumes that at least one of the items is non-eternal.
//     * */
//    public static long[] union(Iterable<? extends TaskRegion> tasks) {
//        long[] u = Tense.union(tasks.iterator());
////        long unionRange = u[1] - u[0];
////        float rangeThreshold = Param.REVISION_UNION_THRESHOLD;
////        if (unionRange > Math.ceil(rangeThreshold * Util.max(t -> t.start()==ETERNAL ?  0 : t.range(), tasks))) {
////
////            //too sparse: settle for more potent intersection if exists
////
////            if (rangeThreshold < 1f) {
////                long[] i = Tense.intersect(tasks);
////                if (i != null)
////                    return Tense.dither(i, dtDither);
////            }
////
////            if (!Param.REVISION_ALLOW_DILUTE_UNION)
////                return null;
////            //else: resort to dilute union
////        }
//
//        //TODO handle cases where N>2 and a mix of union/intersect is valid
//
//        return u;
//    }

    public static void assertDithered(LongInterval x, int d) {
        if (d < 1)
            throw new WTF("dtDither < 1");

        long s = x.start();
        long e = x.end();
        if (s != ETERNAL) {
            if (s == Op.TIMELESS)
                throw WTF.WTF(x + " has start=TIMELESS");

            if (d > 1) {
                if ((dither(s, d) != s) || (e!=s && dither(e, d) != e))
                    throw WTF.WTF(x + " has non-dithered occurrence");
            }
        } else {
            if (e!= ETERNAL)
                throw WTF.WTF(x + " start=ETERNAL but end!=ETERNAL");
        }
    }

    public static void assertDithered(Term t, int d) {
        if (d > 1) {
            t.ANDrecurse(Termlike::TEMPORALABLE, xx -> {
                int zdt = xx.dt();
                if (zdt != dither(zdt, d))
                    throw WTF.WTF(t + " contains non-dithered DT in subterm " + xx);
            });
        }
    }

    public static String dtStr(int dt) {
		return dt == Op.DTERNAL ? "ETE" : String.valueOf(dt);
    }

    public static long[] shiftRandom(long[] x, float range, RandomGenerator rng) {
        if (range <= 0) return x;
        long s = x[0];
        if (s==ETERNAL || s==TIMELESS) return x;
        float _o =
                rng.nextFloat(-range/2, +range/2); //uniform distribution
                //(float) (radius * rng.nextGaussian()); //normal dist

        int o = Math.round(_o);
        return o == 0 ? x : new long[]{s + o, x[1] + o};
    }


//    public static long[] intersection(TaskRegion[] t) {
//        //HACK
//        long[] u = Tense.union(t);
//        if (u[0] == ETERNAL)
//            return u;
//
//
//        //find the min range
//        long minRange = Long.MAX_VALUE;
//        for(TaskRegion x : t) {
//            if (x == null || x.task().isEternal())
//                continue;
//            long r = x.range();
//            if (r < minRange)
//                minRange = r;
//        }
//        long range = minRange-1;
//        if (u[1]-u[0] > range) {
//            //shrink range around estimated center point
//            long mid = (u[1] + u[0])/2L;
//            return new long[] { mid - range/2L, mid + range/2L };
//        }
//        return u;
//    }


    @Override
    public String toString() {
        return symbol;
    }


}