package nars.deriver.reaction;

import nars.Deriver;
import nars.NALTask;
import org.jetbrains.annotations.Nullable;

/**
 * responds with zero or more resolved premises in response to input task
 *
 * ex: replies answers to question tasks
 * TODO  'AnswerQuestionsFromProlog' and 'AnswerQuestionsFromExhaustiveMemorySearch'
 */
public abstract class TaskReaction extends NativeReaction {

	protected TaskReaction() {
		this(true,true,true,true);
	}

	protected TaskReaction(boolean b, boolean g, boolean q, boolean Q) {
		if (self())
			single(b, g, q, Q);
		else
			singleNonSelf(b, g, q, Q);
	}

	protected final void react(@Nullable NALTask y, Deriver d) {
		if (y!=null) d.add(y, autoPri());
	}

	protected boolean autoPri() {
		return true;
	}

	protected boolean self() {
		return true;
	}

	@Override
	protected final void run(Deriver d) {
		run(d.premise.task(), d);
	}

	protected abstract void run(NALTask x, Deriver d);

}