package nars.action.transform;

import jcog.signal.FloatRange;
import nars.Deriver;
import nars.NALTask;
import nars.action.TaskTransformAction;
import org.jetbrains.annotations.Nullable;

/**
 * TODO options
 * --structure required
 * --op required
 *
 */
public class EternalizeAction extends TaskTransformAction {

    public final FloatRange eviRate = new FloatRange(1, 0, 1);

    static final boolean inputTasks = false;

    {
        taskPunc(true, true, false, false);
        taskEternal(false);

        if (!inputTasks)
            taskInput(false);
    }

    @Override
    protected @Nullable NALTask transform(NALTask x, Deriver d) {
        return NALTask.eternalize(x, eviRate.floatValue(), d.eviMin);
    }
}