package nars.task.util;

import jcog.math.Intervals;
import nars.NALTask;
import nars.Term;
import nars.truth.Stamp;

import static nars.Op.ETERNAL;

/** stamp pair-wise comparison modes */
public enum StampPairOverlap {

    Strict() {
        @Override
        public boolean overlapping(NALTask a, NALTask b) {
            return Stamp.overlapsAny(a, b);
        }
    },

    /** loose, but extends exclusion area to prevent loopy overlap in temporal
     *  inductions and their components,
     *  by including the internal span of temporal terms in the temporal intersection test */
    SemiStrict() {

        @Override
        public boolean overlapping(NALTask a, NALTask b) {
            return a.term().TEMPORAL() || b.term().TEMPORAL() ?
                semiStrict(a, b) :
                Loose.overlapping(a, b);
        }

        private static boolean semiStrict(NALTask a, NALTask b) {
            return Strict.overlapping(a, b) && temporalIntersect(a, b);
        }

        /** TODO test IMPL case */
        private static boolean temporalIntersect(NALTask a, NALTask b) {
            long as = a.start(); if (as == ETERNAL) return true;
            long bs = b.start(); if (bs == ETERNAL) return true;
            return Intervals.intersectsRaw(
                adjust(a.term().unneg(), as, a.end()),
                adjust(b.term().unneg(), bs, b.end()));
        }

        private static long[] adjust(Term term, long... se) {
            switch (term.op()) {
                case IMPL -> {
                    int d = term.subUnneg(0).seqDur() + term.dt() + term.sub(1).seqDur();
                    if (d > 0) se[1] += d; //expand after
                    else if (d < 0) se[0] -= d; //expand before
                }
                case CONJ, DELTA -> se[1] += term.seqDur();
            }
            return se;
        }
    },

    Loose() {
        @Override
        public boolean overlapping(NALTask a, NALTask b) {
            return Stamp.overlap(a, b);
        }
    }

    ;

    abstract public boolean overlapping(NALTask a, NALTask b);
}
