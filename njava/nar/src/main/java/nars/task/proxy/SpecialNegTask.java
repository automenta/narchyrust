package nars.task.proxy;

import nars.NALTask;
import nars.Term;
import nars.Truth;
import nars.task.ProxyTask;
import org.jetbrains.annotations.Nullable;

/** term and truth value are inverted, so the semantics remain the same */
public final class SpecialNegTask extends ProxyTask {

    private SpecialNegTask(NALTask x) {
        super(x);
        assert(!(x instanceof SpecialNegTask) && x.BELIEF_OR_GOAL());
        copyMeta(x);
        setCreation(x.creation());
    }

    public static NALTask neg(NALTask x) {
        return x instanceof SpecialNegTask s ? s.task : new SpecialNegTask(x);
    }

    @Override
    public Term term() {
        return task.term().neg();
    }

    @Override @Nullable
    public Truth _truth() {
        return truth(super._truth(), true);
    }

    @Override
    public float freq(long start, long end) {
        return 1 - task.freq(start, end);
    }

    /** elides constructing a negated Truth instance */
    @Override public double evi() {
        return task.evi();
    }

}