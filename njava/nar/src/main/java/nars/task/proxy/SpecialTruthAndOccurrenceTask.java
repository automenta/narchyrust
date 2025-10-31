package nars.task.proxy;

import nars.NAL;
import nars.NALTask;
import nars.Truth;
import nars.task.util.TaskException;
import org.jetbrains.annotations.Nullable;

/**
 * accepts replacement truth and occurrence time for a proxied task
 */
public class SpecialTruthAndOccurrenceTask extends SpecialOccurrenceTask {

    public static NALTask proxy(NALTask t, @Nullable Truth tr, long s, long e) {
        if (e < s)
            throw new TaskException("reversed occurrence for proxy: " + s + ".." + e, t);
        if ((tr == null) != t.QUESTION_OR_QUEST())
            throw new TaskException("invalid truth/non-truth for proxy", t);

        if (tr!=null) tr = immutable(tr);

        boolean oSame = eqOcc(t, s, e), tSame = eqTruth(t, tr);
        if (oSame && tSame)
            return t;

        if (t instanceof SpecialTruthAndOccurrenceTask st) {
            t = st.task; //unwrap

            //check if the existing proxy is equivalent
            oSame = eqOcc(t, s, e); tSame = eqTruth(t, tr);
            if (oSame && tSame)
                return t;
        }

        if (!oSame && !tSame)
            return new SpecialTruthAndOccurrenceTask(t, tr, s, e);
        else if (!oSame) {
            if (t instanceof SpecialOccurrenceTask st)  {
                t = st.task; //unwrap
                if (eqOcc(t, s, e))
                    return t;
            }
            return new SpecialOccurrenceTask(t, s, e);
        } else {
            if (t instanceof SpecialTruthTask st) {
                t = st.task; //unwrap
                if (eqTruth(t, tr))
                    return t;
            }
            return new SpecialTruthTask(t, tr);
        }
    }

    private static boolean eqTruth(NALTask t, @Nullable Truth tr) {
        return //Objects.equals(tr, t.truth());
                tr.equals(t.truth(), NAL.truth.FREQ_EPSILON, NAL.truth.CONF_MIN);
    }

    private static boolean eqOcc(NALTask t, long start, long end) {
        return t.start() == start && t.end() == end;
    }


    /**
     * either Truth, Function<Task,Truth>, or null
     */
    private final Truth truth;

    private SpecialTruthAndOccurrenceTask(NALTask task, Truth truth, long start, long end) {
        super(task, start, end);
        this.truth = immutable(truth);
    }

    @Override
    public @Nullable Truth _truth() {
        return truth;
    }

}