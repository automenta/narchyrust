package nars.focus;

import jcog.pri.Prioritizable;
import jcog.pri.bag.Bag;
import jcog.signal.FloatRange;
import nars.NAL;

import java.util.function.Consumer;
import java.util.function.Function;

public class BagForget implements Function<Bag, Consumer<Prioritizable>> {

    /** values < 0 have accelerated forgetting, > 0 have decelerated forgetting (sustain) */
    public final FloatRange forget = new FloatRange(NAL.FORGETTING, 0, +2);

    @Override public final Consumer<Prioritizable> apply(Bag b) {
        return b.forget(forget.floatValue());
    }

}