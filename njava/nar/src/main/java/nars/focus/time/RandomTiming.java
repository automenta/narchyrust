//package nars.focus.time;
//
//import nars.Deriver;
//import nars.NALTask;
//
//public class RandomTiming implements Timing {
//
//    final Timing[] f;
//
//    public RandomTiming(Timing... f) {
//        this.f = f;
//        assert (f.length > 1);
//    }
//
//    private Timing f(Deriver d) {
//        return f[d.rng.nextInt(f.length)];
//    }
//
//    @Override
//    public long[] whenRelative(NALTask task, Deriver d) {
//        return f(d).whenRelative(task, d);
//    }
//
//    @Override
//    public long[] whenAbsolute(Deriver d) {
//        return f(d).whenAbsolute(d);
//    }
//}