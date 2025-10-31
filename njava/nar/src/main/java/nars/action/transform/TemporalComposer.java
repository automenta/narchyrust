package nars.action.transform;

import nars.NAL;
import nars.NALTask;
import nars.task.proxy.SpecialNegTask;

/** marker interface for temporally-inductive reactions */
public interface TemporalComposer {

    /** make this not necessary but not negating them in the first place */
    @Deprecated static void filterConjSeq(NALTask[] x) {
        if (!NAL.temporal.CONJ_INDUCT_NEG_SEQUENCES) {
            //HACK if any negated conjunction sequences, unneg (unwrap from SpecialNegTask) them.
            for (int i = 0; i < x.length; i++) {
                if (x[i] instanceof SpecialNegTask s && s.task.term().SEQ())
                    x[i] = s.task;
            }
        }
    }

}