package nars.task.util;

import jcog.pri.UnitPri;
import nars.NALTask;
import nars.Task;
import nars.Term;
import nars.Truth;

/** primitive partial task definition: without term, punctuation */
public class ProtoTask implements TaskRegion {
	public final Truth truth;
	public final UnitPri pri;
	public final long creation, start;
	public long end;
	public final long[] stamp;
	public Term why;

	public ProtoTask(NALTask t) {
		this.truth = t.truth();
		this.pri = new UnitPri(t.pri());
		this.stamp = t.stamp();
		this.creation = t.creation();
		this.start = t.start();
		this.end = t.end();
	}

	public void merge(Task x) {
		//TODO
	}

	@Override
	public float freqMin() {
		return truth.freq();
	}

	@Override
	public float freqMax() {
		return truth.freq();
	}

	@Override
	public float confMin() {
		return (float) truth.conf();
	}

	@Override
	public float confMax() {
		return (float) truth.conf();
	}

	@Override
	public long start() {
		return start;
	}

	@Override
	public long end() {
		return end;
	}

}