package nars.io;

import jcog.Util;
import jcog.pri.PLink;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.BufferedBag;
import jcog.signal.FloatRange;
import nars.Focus;
import nars.NAL;
import nars.NALTask;
import nars.task.util.TaskBag;

/* lossy */
public final class BagTaskInput extends DirectTaskInput {

    private final RememberAll all = new RememberAll();

    /**
     * units: tasklinkbag capacities (not this's capacity) per duration
     */
    public final FloatRange rate = new FloatRange(0.5f, 0, 1.0f);

    /**
     * units: max tasks per tasklink
     */
    public final FloatRange capacityFactor;

    final Bag<?, PLink<NALTask>> bag =
        new TaskBag(NAL.taskPriMerge);
        //new BufferedBag<>(new TaskBag());

    public BagTaskInput(float capacityFactor, float inputRate) {
        this.capacityFactor = new FloatRange(capacityFactor, 0, 4);
        this.rate.set(inputRate);
    }

    @Override
    public void start(Focus f) {
        Focus fPrev = this.f;
        super.start(f);

        if (fPrev != f)
            clear();

        bag.capacity(_capacity(f));
    }

    private int _capacity(Focus f) {
        return f.attn.capacity() * capacityFactor.intValue();
    }

    public void clear() {
        bag.clear();
    }

    @Override
    public void remember(NALTask t) {
        bag.put(new PLink<>(t, t.priElseZero()));
    }

    @Override
    public void commit() {
//            try (var __ = nar.emotion.derive_time_Input_Derivation.time()) {

        if (bag.isEmpty())
            return;

        //bag.commit(null); //necessary?

        /* capacities/dur */
        float rate = this.rate.floatValue();

        /* in durs */
        float dtDurs = f.updateDT();

        int n = Util.clampSafe(Math.round(dtDurs * rate * f.attn.capacity()), 1, bag.capacity());

        if (bag instanceof BufferedBag bb)
            bb.commit(null);

        var b = bag instanceof BufferedBag bb ? bb.bag : bag;
        rememberAll((ArrayBag<?, PLink<NALTask>>) b, n, all);
    }

    public BagTaskInput rate(float v) {
        rate.set(v);
        return this;
    }
}

//    final PriArrayBag<NALTask> bag = new PriArrayBag<>(NAL.taskPriMerge) {
//        @Override
//        protected float merge(NALTask existing, NALTask incoming, float incomingPri) {
//            return NALTask.mergeInBag(existing, incoming);
//        }
//
//        @Override
//        protected int histogramBins(int s) {
//            return 0;
//        }
//
////            @Override
////            public void onReject(NALTask t) {
////                BagBuffered.this.onReject(t);
////            }
//    };
//        private void onReject(Task t) {
////            nar.emotion.derivedTaskDrop.increment();
//        }
