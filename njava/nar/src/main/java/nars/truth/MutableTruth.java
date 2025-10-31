package nars.truth;

import jcog.Util;

import java.lang.invoke.VarHandle;

public non-sealed class MutableTruth extends AbstractMutableTruth {

	private static final VarHandle FREQ = Util.VAR(MutableTruth.class, "freq", float.class);
	@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
	private volatile float freq;

	private static final VarHandle EVI = Util.VAR(MutableTruth.class, "evi", double.class);
	@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
	private volatile double evi;

	public MutableTruth(float f, double evi) {
		super(f, evi);
	}

	public MutableTruth() {
		super();
	}

	public MutableTruth(Truthed t) {
		super(t);
	}

	@Override
	protected void _freq(float f) {
		FREQ.setRelease(this, f);
	}

	@Override
	protected void _evi(double e) {
		EVI.setRelease(this, e);
	}

	@Override
	public final float freq() {
		return (float) FREQ.getAcquire(this);
	}

	@Override
	public final double evi() {
		return (double) EVI.getAcquire(this);
	}

}