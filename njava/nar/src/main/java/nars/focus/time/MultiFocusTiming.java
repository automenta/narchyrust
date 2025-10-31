package nars.focus.time;

import jcog.decide.Roulette;
import jcog.signal.FloatRange;
import nars.Deriver;
import nars.NALTask;
import org.jetbrains.annotations.Nullable;

public class MultiFocusTiming extends FocusTiming {

    /**
     * used in relative mode only
     */
    public final FloatRange taskProbability = FloatRange.unit(
            1/8f
            //1/4f
            //1/3f
            //1/2f
            //1
    );

    public final FloatRange focusProbability = FloatRange.unit(1);

//        public final FloatRange wheneverProbability = FloatRange.unit(0);
//
//        public final FloatRange presentProbability = FloatRange.unit(0);

    @Override
    protected int mode(@Nullable NALTask task, Deriver d) {
        if (task!=null) {
            return Roulette.selectRoulette(d.rng,
                taskProbability.floatValue(),
                focusProbability.floatValue()
            );
        } else
            return 1;

//        return Roulette.selectRoulette(d.rng, task != null ?
//            taskProbability.floatValue() : 0,
//            focusProbability.floatValue()
////                    wheneverProbability.floatValue(),
////                    presentProbability.floatValue(),
//        );
    }
}
