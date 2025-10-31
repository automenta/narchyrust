package nars.task.util;

import jcog.Str;
import jcog.TODO;
import jcog.Util;
import jcog.math.ImmLongInterval;
import jcog.math.Intervals;
import nars.NAL;
import nars.Op;



public final class TasksRegion extends ImmLongInterval implements TaskRegion {

    /** discrete truth encoded extrema.  for fast and reliable comparison.
     *
     * uses the system-wide minimum Truth epsilon and encodes a freq,conf pairs in one 32-bit int.
     *
     * a and b are the corners
     */
    private final int a;
    private final int b;

    @Deprecated public TasksRegion(long start, long end, float freqMin, float freqMax, float confMin, float confMax) {
        this(start, end,
            freqI(freqMin), freqI(freqMax),
            confI(confMin), confI(confMax)
            );
    }

    private static final int _hashDiscreteness =
            (int) Math.round(1.0 / NAL.truth.FREQ_EPSILON);
    static { assert(_hashDiscreteness <= Short.MAX_VALUE); }
    public static final short hashDiscreteness = (short)_hashDiscreteness;

    public static int confI(float conf) {
        return Util.toInt(conf, hashDiscreteness/*hashDiscretenessFine*/);
    }

    public static int freqI(float freq) {
        return Util.toInt(freq, hashDiscreteness);
    }

    TasksRegion(long start, long end, int freqIMin, int freqIMax, int confIMin, int confIMax) {
        super(start, end);
        a = (freqIMin << 16) | confIMin;
        b = (freqIMax << 16) | confIMax;
    }

    private static float freqF(int h) {
        return Util.toFloat(freqI(h), hashDiscreteness);
    }
    private static float confF(int c) {
        return Util.toFloat(confI(c), hashDiscreteness/*hashDiscretenessFine*/);
    }
    private static int confI(int h) {
        return h & 0xffff;
    }
    private static int freqI(int h) {
        return h >> 16;
    }

    @Override public final float freqMin() { return freqF(a); }
    @Override public final float freqMax() { return freqF(b); }
    @Override public final float confMin() {
        return confF(a);
    }
    @Override public final float confMax() {
        return confF(b);
    }

    @Override public final int confMinI() { return confI(a); }
    @Override public final int confMaxI() { return confI(b); }
    @Override public final int freqMinI() { return freqI(a); }
    @Override public final int freqMaxI() { return freqI(b); }

    @Override
    public int hashCode() {
        throw new TODO();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TasksRegion r))
            return false; //throw new TODO();
        return a == r.a && b == r.b && s == r.s && e == r.e;
    }

    @Override public String toString() {

        int decimals = 3;
        return '@' +
                Intervals.tStr(s, e) +
                '[' +
                Str.n(freqMin(), decimals) +
                ".." +
                Str.n(freqMax(), decimals) +
                Op.VALUE_SEPARATOR +
                Str.n(confMin(), decimals) +
                ".." +
                Str.n(confMax(), decimals) +
                Op.TRUTH_VALUE_MARK + ']';
    }




}