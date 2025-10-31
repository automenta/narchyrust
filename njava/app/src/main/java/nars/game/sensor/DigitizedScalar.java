package nars.game.sensor;

import jcog.Util;
import jcog.data.list.Lst;
import jcog.math.Digitize;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Term;
import nars.game.Game;
import nars.table.eternal.EternalDefaultTable;

import java.util.Iterator;
import java.util.List;

import static nars.Op.BELIEF;

/**
 * manages a set of N 'digit' concepts whose beliefs represent components of an
 * N-ary (N>=1) discretization of a varying scalar (ie: 32-bit floating point) signal.
 * <p>
 * 'digit' here does not necessarily represent radix arithmetic. instead their
 * value are determined by a ScalarEncoder impl
 * <p>
 * expects values which have been normalized to 0..1.0 range (ex: use NormalizedFloat)
 *
 */
public class DigitizedScalar extends DemultiplexedScalarSensor {


    //    public final static ScalarEncoder Mirror = (v, i, indices) -> {
//        assert (indices == 2);
//        return i == 0 ? v : 1 - v;
//    };

    //        public final float defaultTruth() {
//            return 0;
//        }
    public final List<SignalComponent> sensors;

    private final Digitize digitizer;
    private final Term[] states;

    public DigitizedScalar(FloatSupplier input, Digitize d, NAR nar, Term... states) {
        super(input, //$.func(DigitizedScalar.class.getSimpleName(),
                states //TODO refine
                ///*,$.quote(Util.toString(input))*/, $.the(freqer.getClass().getSimpleName())
                //   )
                ,
                nar, (prev, next) -> {
                    if (next < 0 || next > 1)
                        throw new ArithmeticException();//next, 0, 1);
                    return next == next ? $.t(next, nar.confDefault(BELIEF)) : null;
                }
        );


        this.input = input;
        this.sensors = new Lst(states.length); assert (states.length > 1);

        this.digitizer = d;
        this.states = states;

    }

    @Override
    public void start(Game game) {
        super.start(game);

        NAR nar = game.nar;
        int i = 0;
        var defaultFreq = digitizer.defaultTruth();

        for (Term s : states) {
            int ii = i++;
            SignalComponent sc = component(s, () -> {
                float x = digitizer.digit(asFloat(), ii, states.length);
                return Util.equals(x, defaultFreq) ? Float.NaN : x;
            }, nar);

            if (defaultFreq == defaultFreq)
                EternalDefaultTable.add(sc, defaultFreq, nar);

            sensors.add(sc);
        }

    }

    @Override
    public Iterator<SignalComponent> iterator() {
        return sensors.iterator();
    }


//    /**
//     * returns snapshot of the belief state of the concepts
//     */
//    public Truth[] belief(long when, NAR n) {
//        return IntStream.range(0, size())
//                .mapToObj(sensors::get)
//                .map(s -> n.beliefTruth(s, when))
//                .toArray(Truth[]::new);
//    }


}