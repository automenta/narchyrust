package nars.io;

import jcog.Util;
import nars.Focus;
import nars.NALTask;

import static nars.NALTask.i;

public final class PuncBagTaskInput extends DirectTaskInput {

    public final BagTaskInput[] b;

    public PuncBagTaskInput(float capFactor, float rate) {
        b = Util.arrayOf(i -> new BagTaskInput(capFactor, rate), new BagTaskInput[4]);
    }

    @Override
    public final void remember(NALTask t) {
        b[i(t.punc())].accept(t);
    }

    @Override
    public void start(Focus f) {
        super.start(f);
        for (BagTaskInput bb : b) bb.start(f);
    }

    @Override
    public void commit() {
        for (BagTaskInput bb : b) bb.commit();
    }

    public final PuncBagTaskInput rate(float v) {
        for (BagTaskInput bb : b) bb.rate.set(v);
        return this;
    }
}
