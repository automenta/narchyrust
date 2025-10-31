package nars.focus.time;

import nars.Deriver;
import nars.NALTask;

/** simply matches the task time exactly. */
public class TaskTiming extends PresentTiming {

    @Override
    public long[] whenRelative(NALTask t, Deriver d) {
        return t.startEndArray();
    }

}
