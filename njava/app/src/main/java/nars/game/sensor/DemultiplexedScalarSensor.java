package nars.game.sensor;

import jcog.math.FloatSupplier;
import jcog.signal.MutableFloat;
import jcog.signal.NumberX;
import nars.NAR;
import nars.Term;
import nars.Truth;
import nars.game.Game;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

/** accepts a scalar input that is decomposed into components represented via multiple concepts */
public abstract class DemultiplexedScalarSensor extends VectorSensor implements FloatSupplier {

    public final NumberX value = new MutableFloat();
    public FloatSupplier input;
    public final FloatFloatToObjectFunction<Truth> truther;

    protected DemultiplexedScalarSensor(FloatSupplier input, Term[] states, NAR n, FloatFloatToObjectFunction<Truth> truther) {
        super(states);
        this.truther = truther;
        this.input = input;
    }

    @Override public void accept(Game g) {
        value.set(input.asFloat());
        super.accept(g);
    }


    @Override
    public float asFloat() {
        return value.floatValue();
    }

}