package nars.game.action;

import nars.Term;
import nars.Truth;
import nars.game.Game;
import org.jetbrains.annotations.Nullable;


public class GoalAction extends AbstractGoalAction {

    public/*final*/ MotorFunction motor;

    public GoalAction(Term term, MotorFunction m) {
        super(term);
        setMotor(m);
    }

    public void setMotor(MotorFunction m) {
        this.motor = m;
    }

    @Override
    protected final void update(@Nullable Truth belief, @Nullable Truth goalTruth, Game g) {
        goal = motor.apply(belief, goalTruth);
    }

}