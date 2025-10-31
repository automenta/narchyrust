package nars.focus;

import jcog.pri.op.PriMerge;
import jcog.signal.FloatRange;

import static jcog.pri.op.PriMerge.plus;

/** PriNode + Post-Processor (ex: Amplifier) */
public class PriAmp extends PriNode {

	private final PriMerge mode =
		PriMerge.and;
		//plus;

	private final FloatRange amp = new FloatRange(mode==plus ? 0 : /*and*/ 1, 0, 1f);

	public PriAmp(Object id) {
		super(id);
	}

	@Override
	protected float in(double p) {
		double x = amp.doubleValue();
		return (float) mode.valueOf(x, p);
    }

	public PriAmp amp(float a) {
		amp.set(a);
		return this;
	}


}