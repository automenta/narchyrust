package nars.truth;

import nars.Truth;

public non-sealed class PlainMutableTruth extends AbstractMutableTruth {
    private float freq;
    private double evi;

    public PlainMutableTruth(float f, double e) {
        super(f, e);
    }

    public PlainMutableTruth(Truth t) {
        super(t);
    }

    public PlainMutableTruth() {
        super();
    }

    @Override
    protected void _freq(float f) {
        freq = f;
    }

    @Override
    protected void _evi(double e) {
        evi = e;
    }

    @Override
    public float freq() {
        return freq;
    }

    @Override
    public double evi() {
        return evi;
    }
}
