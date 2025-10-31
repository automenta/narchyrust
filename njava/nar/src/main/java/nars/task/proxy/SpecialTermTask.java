package nars.task.proxy;

import nars.NALTask;
import nars.Term;
import nars.task.ProxyTask;
import nars.term.Neg;
import org.jetbrains.annotations.Nullable;

/**
 * accepts a separate target as a facade to replace the apparent content target of
 * a proxied task
 */
public class SpecialTermTask extends ProxyTask {

    private final Term term;

    protected SpecialTermTask(NALTask X, Term term) {
        super((X instanceof SpecialTermTask S) ? S.task : X);
        this.term = term;
    }

    @Nullable public static NALTask proxy(NALTask x, Term t) {
        return proxy(x, t, false);
    }

    @Nullable public static NALTask proxy(NALTask X, Term y, boolean safe) {
        return proxy(X, y, true, safe);
    }

    @Nullable private static NALTask proxy(NALTask X, Term y, boolean validate, boolean safe) {

        Term z;
        if (validate) {
            z = taskTerm(y, X.punc(), false, safe);
            if (z==null)
                return null;

            if (z instanceof Neg && !(X instanceof SpecialNegTask)) {
                z = z.unneg();
                if (X.BELIEF_OR_GOAL())
                    return SpecialPuncTermAndTruthTask.proxy(z, X.punc(), X.truth().neg(), X);
            }

        } else {
            z = y;
        }

        return proxyUnsafe(X, z);
    }

    /** use with caution, because it elides validatation
     *  allows naming with unnormalized terms, etc */
    public static NALTask proxyUnsafe(NALTask X, Term y) {
        Term x = X.term();
        return x.equals(y) ? X : new SpecialTermTask(X, y);
    }

    @Override public final Term term() {
        return term;
    }

}