package nars.truth.util;

import nars.NALTask;
import nars.Truth;
import nars.truth.PreciseTruth;
import nars.truth.Truthed;
import nars.truth.proj.IntegralTruthProjection;
import org.jetbrains.annotations.Nullable;

/**
 * Truth/Task Revision & Projection (Revection)
 */
public enum Revision {
    ;


    public static Truth revise(@Nullable Truth a, @Nullable Truth b) {
        if (a == null) return b;
        else if (b == null) return a;
        else return revise(a, b, 0);
    }

    /**
     * classic eternal revision
     */
    private static @Nullable Truth revise(Truthed a, Truthed b, float minEvi) {

        double ae = a.evi(), be = b.evi();
        var e = ae + be;

        return e <= minEvi ?
                null :
                PreciseTruth.byEvi(
                        (ae * a.freq() + be * b.freq()) / e,
                        e
                );
    }


    public static NALTask mergeIntersect(NALTask existing, NALTask incoming, float freqRes, double confRes, long[] stamp) {
        var c = new IntegralTruthProjection(2);
        c.add(existing);
        c.add(incoming);
        c.freqRes = freqRes;
        c.confRes = confRes;
        //TODO c.eviMin()

        var t = c.truth();
        if (t == null)
            return null; //shouldnt happen

        var y = NALTask.clone(existing, existing.term(), t, existing.punc(), c.start(), c.end(), stamp, false);

//        if (NAL.DEBUG && !y.term().concept().equals(existing.term().concept())) throw new UnsupportedOperationException(); //merge result has different concept

//        if (existing.equals(y)) return existing;
//        if (incoming.equals(y)) return incoming;

        c.fund(y, c.eviPrioritizer());

        return y;
    }
}