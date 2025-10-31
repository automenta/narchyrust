package nars.truth;

import jcog.math.ImmLongInterval;
import jcog.math.Intervals;
import jcog.math.LongInterval;
import nars.NAL;
import nars.NALTask;
import nars.Truth;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

public class MutableTruthInterval extends PlainMutableTruth implements LongInterval {

    public long s = TIMELESS, e = TIMELESS;

    @Override
    public String toString() {
        return super.toString() + "@" + new ImmLongInterval(s,e);
    }

    @Nullable
    public final Truth project(NALTask x, float dur, float ete, double eviMin) {
        return x.truth(s, e, dur, ete, eviMin);
        //return x.truthRelative(s, e, dur, now, ete, eviMin);
    }

    @Override
    public MutableTruthInterval clone() {
        return clone(s, e);
    }

    public MutableTruthInterval clone(long s, long e) {
        if (this.s == s && this.e == e)
            return this;

        MutableTruthInterval t = new MutableTruthInterval();
        t.occurr(s,e).freq(freq()).evi(evi());
        return t;
    }

    @Override
    public MutableTruthInterval clear() {
        super.clear();
        s = e = TIMELESS;
        return this;
    }

    @Override
    public final long start() {
        return s;
    }

    @Override
    public final long end() {
        return e;
    }

//    public final MutableProjectedTruth occurrConc(long s, long e) {
//        MutableProjectedTruth y;
////        if (NAL.answer.PROJECTION_OCC_FINETUNE)
////            y = cloneFineTune(s, e, now, dur);
////        else
//            y = clone(s,e);
//
//        return y.ditherTime(ditherDT);
//    }

//    /** experimental untested
//     * */
//    private MutableProjectedTruth cloneFineTune(long s, long e, long now, float dur) {
//        final long s0 = this.s, e0 = this.e;
//
//        final double evi0 = this.evi();
//        if (evi0 > 0 && s0 != ETERNAL && s != ETERNAL) {
//            //fine-tune reproject
//            //TODO apply eternalization?
//            //TODO range dilution?
//            final double mult = TruthFunctions.projectMid(
//                    now, s0, e0, s, e, dur, 0 /* TODO ETE */);
//            return (MutableProjectedTruth) clone(s, e).evi(evi0 * mult);
//        }
//        return this;
//    }

    public final MutableTruthInterval ditherTime(int dt) {
        long s = this.s;
        return dt > 1 && s != ETERNAL && s != TIMELESS ?
            occurr(
                //Tense.dither(s, dt, -1), Tense.dither(e, dt, +1)
                Tense.dither(s, dt), Tense.dither(e, dt)
            ) : this;
    }

    public final MutableTruthInterval occurr(long s, long e) {
//        if (s == ETERNAL) {
//            assert(e ==ETERNAL);
//            occEternal();
//        } else {
        if (s > e)
            throw new IllegalArgumentException();

        this.s = s;
        this.e = e;
//        }
        return this;
    }

//    private MutableTruthInterval occEternal() {
//        long s0 = this.s;
//        if (s0 != ETERNAL && this.s != TIMELESS)
//            throw new UnsupportedOperationException("became eternal");
//
//        this.s = this.e = ETERNAL;
//        return this;
//    }

//    public final void occurrUnion(long as, long ae, long bs, long be) {
//        occurr(Math.min(as, bs),  Math.max(ae, be));
//    }

    @Deprecated public long[] startEndShadow() {
        return s == ETERNAL ? startEndArray() : null;
    }

//    public final MutableProjectedTruth occurrEternal() {
//        return occurr(ETERNAL, ETERNAL);
//    }

    public final void occurr(long[] se) {
        assert(se.length==2);
        occurr(se[0], se[1]);
    }

    /** decays evidence according to shifted temporal distance */
    public boolean reoccurr(long ns, long ne, long now, NAL n) {
        long ps = this.s, pe = this.e;
        if (ns == ps && ne == pe) return true; //unchanged

        if (is()) {
            //assert (sp != ETERNAL && sn != ETERNAL);

            double tFactor =
                NAL.revision.PROJECT_REL_OCC_SHIFT ? reprojectFactor(
                        ps, pe, ns, ne,
                        /*n.eternalization.floatValueOf(null),*/ /*now,*/ n.dur()) : 1;

            double rangeFactor =
                NAL.revision.PROJECT_REL_OCC_RANGE_DIFF ? reprojectRange(ns, ne, pe, ps) : 1;

            double eFactor = tFactor * rangeFactor;
            if (eFactor!=1) {
                double ee = eFactor * evi();
                if (ee < n.eviMin()) //TODO d.eviMin
                    return false;
                evi(ee);
            }
        }

        occurr(ns, ne);
        return true;
    }

    private static double reprojectRange(long sn, long en, long ep, long sp) {
        return Math.min(1, ((double) (1 + ep - sp)) / (1 + en - sn));
    }

//    private static double reprojectFactor(long from, long to, float eternalization, long now) {
//        return lerpSafe(eternalization,
//                reprojectFactor(from, to, now), 1);
//    }

    public static double reprojectFactor(long fs, long fe, long ts, long te, float dur) {
        return reprojectFactor(Intervals.diffSep(ts, fs, fe), dur); //ignore te
        //return reprojectFactor(fs, ts, dur); //ignore end's
        //return reprojectFactor(Fuzzy.mean(fs, fe), Fuzzy.mean(ts, te), dur);
    }

    public static double reprojectFactor(long delta, /*long now,*/ float dur) {
        //return
//            NAL.truth.REPROJECT_OCC_DIFF_TRIANGULAR ?
//            TruthFunctions.project(from, to, now, 0 /*n.dur()*/)
//            :
            /* TODO incorrect?
                instead it needs to recalculate the truth from the reprojected inputs */
        return NAL.evi.project.project(delta, dur);
    }
}