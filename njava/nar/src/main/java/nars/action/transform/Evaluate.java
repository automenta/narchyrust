package nars.action.transform;

import jcog.signal.IntRange;
import nars.Deriver;
import nars.NALTask;
import nars.Op;
import nars.deriver.reaction.TaskReaction;
import nars.deriver.util.NALTaskEvaluation;

/** evaluates functor-containing tasks
 * TODO add pure evaluation memoization
 * */
public class Evaluate extends TaskReaction {

	public final IntRange resultMax = new IntRange(1, 1, 16);
	public final IntRange triesMax = new IntRange(2, 1, 16);

	public Evaluate() {
		super();
		hasAll(PremiseTask, Op.FuncBits, true);
	}

	@Override
	protected void run(NALTask x, Deriver d) {
		new MyTaskEvaluation(d).apply(x, resultMax.intValue(), triesMax.intValue());
	}

	private class MyTaskEvaluation extends NALTaskEvaluation {
		MyTaskEvaluation(Deriver d) {
			super(d);
		}

		@Override public void accept(NALTask y) {
			react(y, deriver);
		}
	}
}