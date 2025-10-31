package nars.action;

import nars.*;
import nars.task.proxy.SpecialTermTask;
import nars.task.proxy.SpecialTruthTask;
import nars.term.util.TermCodec;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class TaskTermTransformAction extends TaskTransformAction {
    protected Supplier<TermCodec<?,?>> codec = ()->null;

    @Nullable public abstract Term apply(Term x, Deriver d);

    /** sets anon codec */
    public TaskTermTransformAction anon() {
        codec = () -> new TermCodec.AnonTermCodec(false, true);
        return this;
    }

    @Override
    protected final NALTask transform(NALTask t, Deriver d) {
        try (TermCodec c = this.codec.get()) {
            var x0 = t.term();
            var x = c!=null ? c.encodeSafe(x0) : x0;
            var y = apply(x, d);

            if (y == null) return null;

            var y0 = c!=null ? c.decode(y) : y;

            return valid(x0, y0, d) ? task(t, y0, d) : null;
        }
    }

    private static boolean valid(Term x0, Term y, Deriver d) {
        var yu = y.unneg();
        return yu.complexity() <= d.complexMax &&
                !x0.equals(yu) &&
                yu.TASKABLE();
    }

    @Nullable
    protected final NALTask task(NALTask x, Term y, Deriver d) {
        var xx = SpecialTermTask.proxy(x, y, true);
        if (xx == null) return null;
        if (x.QUESTION_OR_QUEST()) return xx;
        else {
            var t = truthOf(x, y);
            return t == null || t.evi() < d.eviMin ?
                    null :
                    SpecialTruthTask.proxy(xx, t);
        }
    }

    @Nullable protected Truth truthOf(NALTask x, Term y) {
        return TruthFunctions.weak(x.truth());
    }


}