package nars.task;

import nars.NALTask;
import nars.Op;
import nars.Term;
import nars.Truth;
import org.jetbrains.annotations.Nullable;

/**
 * base class for concrete immutable on-heap NAL Task implementation,
 * with mutable cause[] and initially empty meta table
 */
public abstract sealed class AbstractNALTask extends NALTask permits EternalTask, SerialTask, TemporalTask {

    /*@Stable*/  /*final*/  public long[] stamp;

    private final Term term;
    private final Truth truth;
    private final byte punc;
    private final int hash;

    protected AbstractNALTask(Term term, byte punc, @Nullable Truth truth, long start, long end, long[] stamp) {
        if (Op.BELIEF_OR_GOAL(punc) && truth == null)
            throw new IllegalArgumentException();
        super();
        this.term = term;
        this.punc = punc;
        this.truth = truth;
        this.stamp = stamp;
        this.hash = hashCalculate(start, end); //must be last
    }


    protected int hashCalculate(long start, long end) {
        return hash(
                term,
                truth,
                punc,
                start, end, stamp);
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final long[] stamp() {
        return stamp;
    }


    @Override
    public final @Nullable Truth truth() {
        return truth;
    }

    @Override
    public byte punc() {
        return punc;
    }

    @Override
    public Term term() {
        return term;
    }

    @Override
    public String toString() {
        return appendTo(null).toString();
    }

}