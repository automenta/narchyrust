package nars.deriver.util;

import jcog.exe.flow.Feedback;
import jcog.signal.meter.FastCounter;
import nars.*;
import nars.term.atom.Bool;
import nars.term.util.TermException;
import org.jetbrains.annotations.Nullable;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

public enum DerivationFailure {

    Null {
        @Override
        public void record(NAR n) {
            is(n.emotion.deriveFailNullTerm);
        }
    },

    VolMax {
        @Override
        public void record(NAR n) {
            is(n.emotion.deriveFailVolLimit);
        }
    },

    Taskable,

    /** term contains query vars, applies only to belief and goals */
    QueryVar,

    /** term contains xternal, applies only to belief and goals */
    Xternal;

    protected void is(FastCounter deriveFailNullTerm) {
        Feedback.is(name(), deriveFailNullTerm);
    }

    public static @Nullable DerivationFailure failure(Term x, byte punc, int volMax) {

        if (x instanceof Bool) return Taskable;

        x = x.unneg();

        if (!x.TASKABLE())
            return Taskable;

        if (x.complexity() > volMax)
            return VolMax;

        if (x.hasAny(Op.VAR_PATTERN))
            throw new TermException("Termify result contains VAR_PATTERN", x);

        if (punc == BELIEF || punc == GOAL) {
            if(x.hasVarQuery())
                return QueryVar;
            else if(x.TEMPORAL_VAR())
                return Xternal;
        }

        return null; //OK
    }

    /** default misc failure */
    public void record(NAR n) {
        n.emotion.deriveFail.increment(/*() ->
                rule + " |\n\t" + d.xy + "\n\t -> " + c1e
        */);
    }

    public final void record(Deriver d) {
        if (NAL.DEBUG)
            record(d.nar);
    }
}