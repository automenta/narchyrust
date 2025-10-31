package nars.game.sensor;

import jcog.math.FloatSupplier;
import nars.Term;
import nars.game.Game;

public class LambdaScalarSensor extends ScalarSensor {

    private final FloatSupplier v;

    public LambdaScalarSensor(Term id, FloatSupplier v) {
        super(id);
        this.v = v;
    }

    @Override
    public void accept(Game g) {
        accept(v.asFloat(), g);
    }



}