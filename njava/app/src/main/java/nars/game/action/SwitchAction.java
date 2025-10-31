package nars.game.action;

import jcog.decide.Decide;
import jcog.decide.DecideSoftmax;
import jcog.math.Digitize;
import nars.NAR;
import nars.Term;
import nars.Truth;
import nars.game.Game;
import nars.game.sensor.DigitizedScalar;
import nars.game.sensor.SignalConcept;

import java.util.function.IntPredicate;

@Deprecated public class SwitchAction extends DigitizedScalar {

    static final float EXP_IF_UNKNOWN = 0;

    private final Decide decider;
    final float[] exp;
    private final IntPredicate action;

    public SwitchAction(NAR nar, IntPredicate action, Term... states) {
        super(null, Digitize.BinaryNeedle, nar, states);
        this.input = value::floatValue;
        this.decider =
                //new DecideEpsilonGreedy(0.05f, nar.random());
                new DecideSoftmax(0.5f, nar.random());
        this.action = action;
        exp = new float[states.length];


    }

    protected int decide(long start, long end, NAR n) {
        for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
            SignalConcept x = sensors.get(i);
            Truth g = x.goals().truth(start, end, n);

            exp[i] = g != null ? q(g) : EXP_IF_UNKNOWN;
        }

        return decider.applyAsInt(exp);

    }

    /** truth -> decidability */
    public static float q(Truth g) {
        //return g.expectation();
        return g.freq();
    }

    @Override
    public void accept(Game g) {

        var w = g.time;

        int d = decide(w.s, w.e, g.nar);

        if (d!=-1 && action.test(d))
            value.set((d + 0.5f)/exp.length);
        else
            value.set(Float.NaN);

        super.accept(g);
    }
}