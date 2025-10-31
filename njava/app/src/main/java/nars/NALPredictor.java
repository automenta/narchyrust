package nars;

import jcog.TODO;
import jcog.math.FloatSupplier;
import jcog.signal.FloatRange;
import jcog.tensor.Predictor;
import nars.game.Game;
import nars.game.sensor.ScalarSensor;

import java.util.Random;

/** general purpose vector -> vector associative memory.
 *  uses an embedded NAR and (TODO optional) eternal reasoning.
 *
 *  TODO use NALData
 *  */
public class NALPredictor implements Predictor {
    final NAR nar;
    private final int inputs;
    private final Term[] xt, yt;

    /** TODO IntegratingScalarSensor: only for DeltaPredictor ? */
    private final DifferentiableScalarSensor[] x, y;

    public NALPredictor(int inputs, int outputs) {
        this(inputs, outputs, NARS.tmp());
    }

    public NALPredictor(int inputs, int outputs, NAR nar) {
        this.nar = nar;
        this.inputs = inputs;

        this.x = new DifferentiableScalarSensor[inputs];
        xt = new Term[inputs];
        this.y = new DifferentiableScalarSensor[outputs];
        yt = new Term[inputs];
//        this.game = new Game($.the("xy")) {
        {
            for (int i = 0; i < inputs; i++) {
                x[i] = new DifferentiableScalarSensor(xt[i] = x(i));
            }
            for (int i = 0; i < inputs; i++) {
                y[i] = new DifferentiableScalarSensor(yt[i] = y(i));
            }
        }
//        };
    }

    @Override
    public double[] put(double[] x, double[] y, float pri) {
        throw new TODO();
    }

    @Override
    public double[] get(double[] x) {
        return new double[0];
    }

    static class DifferentiableScalarSensor extends ScalarSensor {

        final FloatSupplier x = new FloatRange(0.5f, 0, 1);

        DifferentiableScalarSensor(Term id) {
            super(id);
        }

        public void addDelta(float d) {
            x.plus(d);
        }

        @Override
        public void accept(Game g) {
            accept(x.asFloat(), g);
        }

    }


    /** TODO cache */
    private Term x(int i) {
        return $.atomic("x" + i);
    }
    /** TODO cache */
    private Term y(int i) {
        return $.atomic("y" + i);
    }


    @Override
    public void clear(Random rng) {
        nar.main.clear();
    }
}