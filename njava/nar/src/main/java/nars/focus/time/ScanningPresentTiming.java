//package nars.focus.time;
//
//import jcog.signal.FloatRange;
//import nars.Deriver;
//import nars.time.Tense;
//
//public class ScanningPresentTiming extends PresentTiming {
//    public final FloatRange durs = new FloatRange(32, 0, 128);
//
//    @Override
//    public long[] whenAbsolute(Deriver d) {
//        long[] now = super.whenAbsolute(d);
//
//        shiftSimple(now, d);
////        shiftStretch(now, d);
//
//        Tense.dither(now, d.ditherDT);
//
//        return now;
//    }
//
//    private void shiftStretch(long[] now, Deriver d) {
//        now[0] += shift(d);
//        now[1] += shift(d);
//        if (now[0] > now[1]) {
//            long x = now[1];
//            now[1] = now[0];
//            now[0] = x;
//        }
//    }
//
//    private void shiftSimple(long[] now, Deriver d) {
//        long cyclesShift = shift(d);
//        now[0] += cyclesShift;
//        now[1] += cyclesShift;
//    }
//
//
//    private long shift(Deriver d) {
//        return Math.round(d.random.nextGaussian() * d.dur() * durs.doubleValue());
//    }
//}