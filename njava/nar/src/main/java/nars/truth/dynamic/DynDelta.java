package nars.truth.dynamic;

import jcog.util.ObjectLongLongPredicate;
import nars.*;
import nars.term.Compound;
import nars.truth.DynTaskify;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.TruthFunctions.delta;

/** TODO 2-ary delta between two concepts at different times
 *      canonically sort the pair's order since the reverse
 *      will be redundant (1-d).
 */
public class DynDelta extends DynTruth {

    public static final DynTruth the = new DynDelta();

    private DynDelta() { }

    public boolean ditherComponentOcc() { return false; }

    @Override public Truth truth(DynTaskify d) {
        NALTask S = d.get(0), E = d.get(1);
        if (!S.term().equals(E.term()))
            return null; //inequal content TODO avoid entirely

        var se = d.startEndArray();
        long a = se[0], b = se[1];
        var times = times(a, b); //HACK recompute

        var C = pairConf(S, times[0], times[1]) * pairConf(E, times[2], times[3]);
        if (C <= NAL.truth.CONF_MIN) return null;

        d.occ(a, b);

        return $.t(delta(S.freq(), E.freq()), C);
    }

    @Override
    public boolean decompose(Compound superterm, long s, long e, ObjectLongLongPredicate<Term> each) {
        assert(superterm.DELTA());
        if (s == e) return false; //point or ETERNAL
        var ab = times(s, e);
        var x = superterm.sub(0);
        return each.accept(x, ab[0], ab[1]) && each.accept(x, ab[2], ab[3]);
    }

    private static long[] times(long startA, long endB) {
        var dh = (endB - startA) /2;
        var endA = startA + dh;
        var startB = endB - dh;

        if (endA == startB) {
            //prevent temporal overlap
            endA--;
            if (startA > endA) startA--; //HACK for point
        }
        assert(endA < startB);
        return new long[]{ startA, endA, startB, endB };
    }

    @Override
    public Term recompose(Compound superterm, DynTaskify d) {
        return superterm;
        //return DELTA.the(d.getFirst().term());
    }

    @Override
    public int componentsEstimate() {
        return 2;
    }

    @Override
    public @Nullable Predicate<NALTask> preFilter(int component, DynTaskify d) {
        return notEternal;
    }

    private static final Predicate<NALTask> notEternal = z -> !z.ETERNAL();

}