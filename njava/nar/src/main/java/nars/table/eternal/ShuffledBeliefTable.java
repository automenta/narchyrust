package nars.table.eternal;

import nars.Answer;
import nars.NALTask;
import nars.table.dynamic.DynBeliefTable;

public class ShuffledBeliefTable extends DynBeliefTable {
	public final NALTask[] tasks;

	public ShuffledBeliefTable(NALTask... tasks) {
		super(tasks[0].term().concept(), tasks[0].BELIEF());
		this.tasks = tasks;
		//TODO verify all tasks have the same concept and same punctuation (either belief or goal)
	}

	@Override
	public void match(Answer a) {
		//try one at a time
		a.test(tasks[a.rng().nextInt(tasks.length)]);
	}
}