package nars.action.transform;

import nars.Deriver;
import nars.NALTask;
import nars.action.TaskTransformAction;
import nars.task.util.ImplTaskify;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

/**
 * implBeliefify and implGoalify
 */
public class ImplTaskifyAction extends TaskTransformAction {



    public final float fwd;
    final boolean beliefOrGoal;

    public ImplTaskifyAction(boolean beliefOrGoal) {
        this(0.5f, beliefOrGoal);
    }

    public ImplTaskifyAction(float fwd, boolean beliefOrGoal) {
        this.fwd = fwd;
        this.beliefOrGoal = beliefOrGoal;

        is(PremiseTask, IMPL);
        hasAny(PremiseTask, or(VAR_INDEP, VAR_DEP), false);

        //TODO other conditions

//        if (singlePremiseMode) {
//            //more aggressive
         single(true,false,false,false);
//        } else {
//            //more careful
//            taskPunc(true, false,false,false);
//            hasBeliefTask(false);
//            Variable a = $.varPattern(1);
//            Variable b = $.varPattern(2);
//            Variable c = $.varPattern(3);
//            taskPattern(IMPL.the(a, XTERNAL, b));
//            beliefPattern(c);
//            constrain(new EqualPosOrNeg((Variable) (fwd ? taskPattern.sub(0) : taskPattern.sub(1)), c));
//        }
    }

    @Override
    protected @Nullable NALTask transform(NALTask x, Deriver d) {
        return ImplTaskify.taskify(x, d.rng.nextBooleanFast8(fwd), beliefOrGoal, d);
    }

    @Override public boolean autoPri() {
        return false;
    }

}