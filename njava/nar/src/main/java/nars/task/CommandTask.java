package nars.task;

import nars.Task;

import static nars.Op.COMMAND;

public abstract class CommandTask extends Task {

	@Override
	public final byte punc() {
		return COMMAND;
	}

}
