package nars.deriver.util;

import nars.*;
import nars.eval.TaskEvaluation;
import nars.task.proxy.SpecialPuncTermAndTruthTask;
import nars.task.proxy.SpecialTermTask;
import nars.term.atom.Bool;
import org.jetbrains.annotations.Nullable;

import java.util.random.RandomGenerator;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.term.atom.Bool.True;

/** TODO impl Premise */
public abstract class NALTaskEvaluation extends TaskEvaluation<NALTask> {

    public int solutionsRemain;
    public int triesRemain;
    public final Deriver deriver;
    protected transient NALTask task;

    protected NALTaskEvaluation(Deriver d) {
        super(d.nar.evaluator);
        this.deriver = d;
    }

    private static @Nullable NALTask booleanAnswer(NALTask x, Bool y, Deriver d) {

        assert(y!=Null);

        byte punc = x.punc();
        boolean puncQuestion = punc == QUESTION;

        if (puncQuestion || punc == QUEST) {
            //conver to an answering belief/goal now that the absolute truth has been determined

            byte answerPunc = puncQuestion ? BELIEF : GOAL;

            return SpecialPuncTermAndTruthTask.proxy(
                x.term(),
                answerPunc,
                $.t(y == True ? 1 : 0, d.nar.confDefault(answerPunc)),
                x);

        } else
            return null; //throw new WTF();

    }

    @Override
    public NALTask task() {
        return task;
    }

    public void apply(NALTask x, int solutions, int tries) {
        clear();
        this.solutionsRemain = solutions;
        this.triesRemain = tries;
        this.task = x;
        eval(x.term());
    }

    @Override
    protected RandomGenerator random() {
        return deriver.rng;
    }

    @Override
    public NAR nar() {
        return deriver.nar;
    }

    @Override
    public boolean test(Term y) {
        if (y.TASKABLE() /*&& !y.hasAny(BOOL)*/ /* HACK - inner booleans */ && y!=task.term()) {
            NALTask task = this.task;
            NALTask z;
            if (y instanceof Bool)
                z = booleanAnswer(task, (Bool) y, deriver);
            else if (y.unneg().complexity() <= deriver.complexMax)
                z = SpecialTermTask.proxy(task, y, true);
            else
                z = null;

            if (z != null) {
                accept(z);
                if (--solutionsRemain <= 0)
                    return false;
            }
        }

        return --triesRemain > 0;
    }


}