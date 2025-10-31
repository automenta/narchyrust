package nars.focus.time;

import jcog.math.Intervals;
import jcog.signal.FloatRange;
import nars.Deriver;
import nars.NALTask;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import static nars.Op.ETERNAL;

/** delegates to the temporal focus of the deriver's Focus */
public class FocusTiming implements TaskWhen {

    public final FloatRange shiftRandom = new FloatRange(1, 0, 2);

    private long[] when(long[] x, Deriver d) {
        float r = shiftRandom.asFloat();
        return r > 0 ? whenRandom(x, r, d) : x;
    }

    private static long[] whenRandom(long[] x, float r, Deriver d) {
        return Tense.shiftRandom(x, r * d.dur(), d.rng);
    }

    @Override
    public long[] whenAbsolute(Deriver d) {
        return when(whenFocus(d), d);
    }

    @Override
    public long[] whenRelative(NALTask t, Deriver d) {
        long[] w = when(when(t, d), d);
//        if (expandToDur && w[0]!=ETERNAL) {
//            long durW = w[1] - w[0];
//            int durFocus = (int)d.focus.dur();
//            if (durW < durFocus) {
//                long mid = Fuzzy.mean(w[0], w[1]);
//                w[0] = mid - durFocus/2;
//                w[1] = mid + durFocus/2;
//            }
//        }
        return w;
    }

    private long[] when(NALTask task, Deriver d) {
        return when(task, d, mode(task, d));
    }

    protected int mode(NALTask task, Deriver d) {
        return
            1
            //0
        ;
    }

    /** TODO use interface or enum */
    @Deprecated private static long[] when(@Nullable NALTask task, Deriver d, int mode) {
        return switch (mode) {
            //task
            case 0 -> whenTask(task);

            //focus
            case 1 -> whenFocus(d);
                      //rangeShrink(whenFocus(d), task, d);

            //whenever
            case 2 -> whenEver();

            //present
            case 3 -> whenPresent(d);

            default -> throw new UnsupportedOperationException();
        };
    }

    private static long[] whenFocus(Deriver d) {
        return d.focus.when();
    }

    private static long[] whenTask(@Nullable NALTask task) {
        return task.startEndArray();
    }

    private static long[] whenPresent(Deriver d) {
        return Intervals.range(d.now(), d.dur() /*task.range()*/);
    }

    private static long[] whenEver() {
        return new long[]{ETERNAL, ETERNAL};
    }

//    private long[] rangeShrink(long[] r, @Nullable NALTask task, Deriver d) {
//        long rs = r[0];
//        if (task != null && rs!=ETERNAL && !task.ETERNAL()) {
//            long tr = task.range()-1;
//            long rr = r[1] - rs;
//            if (tr < rr) {
//                long mid =
//                    d.rng.nextLong(rs, rs + (rr-tr)); //RANDOM subrange within bounds
//                    //d.rng.nextLong(se[0], se[1]); //RANDOM subrange
//                    //Fuzzy.mean(se[0], se[1]); //MID
//
//                return Intervals.range(mid, tr);
//            }
//        }
//        return r;
//    }



//    /** surround the task's occurrence with the focus duration */
//    private static long[] whenTask(NALTask t, float minRange) {
//        long tRange = t.range()-1;
//        return tRange >= minRange ?
//                t.startEndArray() :  //task range wider
//                LongInterval.range(t.mid(), minRange); //focus duration wider
//
//    }

}