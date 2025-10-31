package nars.truth;

import jcog.math.Intervals;
import nars.NALTask;
import nars.truth.proj.MutableTruthProjection;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntPredicate;

import static jcog.math.LongInterval.ETERNAL;

/**
 * utilities for separating stamp overlap
 */
public enum Underlap {
    ;

    /**
     * trim y (weaker) to not overlap temporally with x (stronger)
     *
     * @return whether trim was successful
     * <p>
     * 4 cases:
     * 1  x contains y: impossible
     * 2  y contains x: longer of either prefix or suffix
     * 3     y leads x: prefix
     * 4     x leads y: suffix
     * <p>
     * TODO when range 2: somehow use both prefix and suffix instead of just either
     */
    public static boolean separate(NALTask[] tasks, int x, int y, @Nullable IntPredicate ifChanged) {
        var X = tasks[x];
        var xs = X.start();
        if (xs == ETERNAL) return false;
        var Y = tasks[y];
        var ys = Y.start();
        if (ys == ETERNAL) return false;
        long xe = X.end(), ye = Y.end();
        if (Intervals.containsRaw(xs, xe, ys, ye)) return false;

        var ps = order(xs, xe, ys, ye);

        long s, e;
        if (ps) {
            s = ys;
            e = xs - 1;
        } else {
            s = xe + 1;
            e = ye;
        }
        if (e < s)
            return false; //impossible

        return separated(y, tasks, s, e, ifChanged);
    }

    private static boolean order(long xs, long xe, long ys, long ye) {
        long prefixLen, suffixLen;
        if (Intervals.containsRaw(ys, ye, xs, xe)) {
            prefixLen = xs - ys;
            suffixLen = ye - xe;
        } else {
            if (ys < xs) {
                prefixLen = xs - ys;
                suffixLen = 0;
            } else if (ye > xe) {
                suffixLen = ye - xe;
                prefixLen = 0;
            } else throw new UnsupportedOperationException();
        }

        assert (prefixLen > 0 || suffixLen > 0);
        return prefixLen >= suffixLen;
    }

    private static boolean separated(int y, NALTask[] tasks, long s, long e, @Nullable IntPredicate ifChanged) {
        var y0 = tasks[y];

        var y1 = new MutableTruthProjection.MySpecialOccurrenceTask(y0, s, e);
        tasks[y] = y1;
        if (ifChanged!=null && !ifChanged.test(y)) {
            //fail; undo
            tasks[y] = y0;
            return false;
        }

        return true;
    }
}
