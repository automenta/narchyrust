package nars.task;

import nars.Term;
import nars.Truth;
import nars.task.util.TaskException;
import org.jetbrains.annotations.Nullable;


/** generic NAL Task with stored start,end time */
public final class TemporalTask extends AbstractNALTask {

	private final long start, end;

    public TemporalTask(Term term, byte punc, @Nullable Truth truth, long start, long end, long[] stamp) throws TaskException {
		super(term, punc, truth, start, end, stamp);
		this.start = start;
        this.end = end;
    }

    @Override
    public final long start() {
        return start;
    }

    @Override
    public final long end() {
        return end;
    }

}