package nars.action.resolve;

import nars.Deriver;
import nars.NALTask;
import nars.Term;
import nars.focus.time.TaskWhen;
import nars.task.proxy.SpecialNegTask;
import nars.term.Neg;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public abstract class TaskResolver {

    /**
     * main implementation method
     * NOTE some impl may non-uniformly assume that it will use resolveTerm(x)
     */
    public abstract @Nullable NALTask resolveTask(Term x, byte punc, Object/*Supplier<long[]>*/  occ, Deriver d, @Nullable Predicate<NALTask> filter);

    public/* final */ NALTask resolveTask(Term x, byte punc, TaskWhen when, Deriver d) {
        return resolveTask(x, punc, when.whenAbsolute(d), d);
    }

    @Nullable
    @Deprecated
    protected /* final */ NALTask resolveTask(Term x, byte punc, long[] w, Deriver d) {
        return resolveTask(x, punc, w, d, null);
    }

    /**
     * accepts and wraps negated terms for lookup
     */
    @Nullable
    public /* final */ NALTask resolveTaskPolar(Term x, byte punc, long s, long e, Deriver d, @Nullable Predicate<NALTask> filter) {
        boolean neg = x instanceof Neg;
        if (neg) x = x.unneg();
        NALTask y = resolveTask(x, punc, new long[]{s, e}, d, filter);
        return y != null && neg ? SpecialNegTask.neg(y) : y;
    }

}
