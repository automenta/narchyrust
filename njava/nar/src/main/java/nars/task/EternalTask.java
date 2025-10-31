package nars.task;

import nars.Term;
import nars.Truth;
import org.jetbrains.annotations.Nullable;

public final class EternalTask extends AbstractNALTask {

	public EternalTask(Term term, byte punc, @Nullable Truth truth, long[] stamp) {
		super(term, punc, truth, ETERNAL, ETERNAL, stamp);
	}

    @Override
    public final long start() {
        return ETERNAL;
    }

    @Override
    public final long end() {
        return ETERNAL;
    }

}