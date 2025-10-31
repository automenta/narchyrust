package nars.deriver.util;

import nars.Deriver;
import nars.NALTask;
import nars.NAR;
import nars.Term;
import nars.truth.DynTaskify;
import nars.truth.dynamic.DynTruth;
import org.jetbrains.annotations.Nullable;

import static nars.NAL.revision.DYN_DT_DITHER;


public final class DeriverTaskify extends DynTaskify {

    private final Deriver d;

    public DeriverTaskify(DynTruth model, Deriver d, NALTask... components) {
        super(model, null, components[0].BELIEF(),
                DYN_DT_DITHER ? d.timeRes() : 0,
                0);
        this.d = d;
        this.setArray(components);
    }

    @Override
    public @Nullable NALTask task() {

        NALTask y = super.task();
        if (y!=null) {
            float p = (float) priFactor(y);
            if (p!=p)
                return null; //dropped
            y.priMul(p);
        }

        return y;
    }

    private double priFactor(NALTask y) {
        return d.focus.budget.priDerived(y, null, null, d);
    }

    @Override
    public NAR nar() {
        return d.nar;
    }

    @Override
    public double eviMin() {
        return d.eviMin;
    }

    @Override public int complexityMax() { return d.complexMax; }

    @Override
    public boolean accept(Term object, long start, long end) {
        throw new UnsupportedOperationException();
    }

}