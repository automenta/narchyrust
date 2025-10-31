package nars.task.proxy;

import nars.NALTask;
import nars.Term;
import nars.Truth;
import nars.task.ProxyTask;
import nars.term.Neg;
import org.jetbrains.annotations.Nullable;

public class SpecialPuncTermAndTruthTask extends ProxyTask {

    private final Term term;
    private final Truth truth;
    private final byte punc;

    /** use with caution */
    public SpecialPuncTermAndTruthTask(NALTask task, Term term, Truth truth) {
        this(task, term, task.punc(), truth);
    }

    @Deprecated
    private SpecialPuncTermAndTruthTask(NALTask task, Term term, byte punc, Truth truth) {
        super(task);
        this.term = term;
        this.truth = immutable(truth);
//        if (this.truth == null)
//            throw new NullPointerException(); //TEMPORARY
        this.punc = punc;
    }


    public static SpecialPuncTermAndTruthTask proxy(Term term, byte punc, Truth truth, NALTask task) {

        Term y = taskTerm(term, punc);

        boolean neg = y instanceof Neg;
        if (neg) {
            if (truth!=null) truth = truth.neg();
            y = y.unneg();
        }

        //TODO other equality tests

        return y!=null ? new SpecialPuncTermAndTruthTask(task, y, punc, truth) : null;
    }


    @Override
    public float freq(long start, long end) {
        return truth.freq(start, end);
    }

    @Override
    public byte punc() {
        return punc;
    }

    @Override
    public @Nullable Truth _truth() {
        return truth;
    }

    @Override
    public Term term() {
        return term;
    }
}