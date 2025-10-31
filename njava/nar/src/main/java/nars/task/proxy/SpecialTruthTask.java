package nars.task.proxy;

import nars.NAL;
import nars.NALTask;
import nars.Truth;
import nars.task.ProxyTask;
import org.jetbrains.annotations.Nullable;

public class SpecialTruthTask extends ProxyTask {

	private final Truth truth;

	SpecialTruthTask(NALTask task, Truth truth) {
		super(task instanceof SpecialTruthTask s ? s.task : task);
		this.truth = immutable(truth);
	}

	@Override
	public float freq(long start, long end) {
		return truth.freq(start, end);
	}
	
	@Override
	public Truth _truth() {
		return truth;
	}

	public static NALTask proxy(NALTask t, Truth truthNext) {
		return proxy(t, truthNext, null);
	}

	public static NALTask proxy(NALTask t, Truth truthNext, @Nullable NAL nal) {
		truthNext = immutable(truthNext);

		if (truthNext.equals(t.truth()))
			return t;

		if (t instanceof SpecialTruthTask s) {
			//unwrap
			t = s.task;
			if (truthNext.equals(t.truth()))
				return t;
		}

		return new SpecialTruthTask(t, truthNext);
	}


}