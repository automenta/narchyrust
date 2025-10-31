package nars.game.util;

import nars.Truth;
import nars.game.action.AbstractGoalAction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.jetbrains.annotations.Nullable;

public class UnipolarMotor implements AbstractGoalAction.MotorFunction {

    private final boolean freqOrExp;
    private final FloatToFloatFunction ifGoalMissing;
    private final FloatToFloatFunction update;
    private float goalPrev;

    public UnipolarMotor(boolean freqOrExp, FloatToFloatFunction ifGoalMissing, FloatToFloatFunction update) {
        this.freqOrExp = freqOrExp;
        this.ifGoalMissing = ifGoalMissing;
        this.update = update;
        this.goalPrev =
            Float.NaN;
            //0.5f;
    }

    @Override
    public float apply(@Nullable Truth b, @Nullable Truth g) {
        float goal = goal(g);

        //goal = Util.round(goal, res) //??

        goalPrev = goal;

        return goal==goal ? update.valueOf(goal) : Float.NaN;
    }

    private float goal(@Nullable Truth g) {
        return g == null ? ifGoalMissing.valueOf(goalPrev) : (float) (freqOrExp ? g.freq() : g.expectation());
    }
}