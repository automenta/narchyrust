package nars.eval;

import nars.NAR;
import nars.Task;
import nars.Term;

import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class TaskEvaluation<T extends Task> extends Evaluation implements Consumer<T>, Predicate<Term> {


    protected TaskEvaluation(Evaluator e) {
        super(e, null);
    }

    /** current or last task being computed, if any */
    public abstract T task();

    public abstract NAR nar();

}