package nars.action;

import nars.Deriver;
import nars.NALTask;
import nars.deriver.reaction.TaskReaction;
import org.jetbrains.annotations.Nullable;

public abstract class TaskTransformAction extends TaskReaction {

	protected abstract @Nullable NALTask transform(NALTask x, Deriver d);

	@Override
	protected final void run(NALTask x, Deriver d) {
		NALTask y = transform(x, d);
		if (y!=null) {
			if (copyMeta())
				copyMeta(x, y);
			react(y, d);
		}
	}

	private static void copyMeta(NALTask x, NALTask y) {
		y.copyMeta(x);
		y.setCreation(x.creation());
	}

	protected boolean copyMeta() {
		return false;
	}


}