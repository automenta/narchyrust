package nars.focus.util;

import jcog.TODO;
import jcog.data.map.MRUMap;
import jcog.event.Off;
import nars.*;
import nars.unify.UnifyAny;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static nars.Op.BELIEF;
import static nars.Op.ETERNAL;

/** question & answer-collecting context with per answer callback.
 *  optional duplicate filtering */
abstract public class Questioning implements Consumer<Task>, AutoCloseable {

    private final Focus f;

    final MRUMap<NALTask, NALTask> questions;

    final QuestionMatching match =
        new UnifyQuestionMatching();
        //new StampQuestionMatching();

    private final Off off;

    interface QuestionMatching {
        void match(NALTask a);

        /** called when the set of active questions changes */
        default void update() { }
    }

    /** match by stamp component */
    static class StampQuestionMatching implements QuestionMatching {
        //final LongHashSet...
        @Override
        public void match(NALTask a) {
            throw new TODO();
        }
    }

    /** more exhaustive test for all results
     *  optional: compilation, term cache */
    class UnifyQuestionMatching implements QuestionMatching {
        @Override
        public void match(NALTask a) {
            if (questions.isEmpty()) return;

            Term A = a.term();
            UnifyAny u = new UnifyAny(ThreadLocalRandom.current());
            for (NALTask q : questions.keySet()) {
                if (u.unifies(q.term(), A))
                    tryAnswer(q, a);
            }
        }
    }

    static private final int SEEN_CAPACITY = 1024;
    /** duplicate filtering */
    @Nullable final MRUMap<NALTask,NALTask> seen = new MRUMap<>(SEEN_CAPACITY);

    protected Questioning(Focus f, int qCapacity) {
        this.f = f;
        this.questions = new MRUMap<>(qCapacity);
        off = f.onTask(this);
    }

    @Override public void close() {
        clear();
        off.close();
    }

    private void tryAnswer(NALTask question, NALTask answer) {
        if (seen==null || seen.put(answer,answer)==null)
            answer(question, answer);
    }

    /** called on answer */
    abstract protected void answer(NALTask question, NALTask answer);

    public final void ask(Term question) {
        ask(NALTask.task(question, Op.QUESTION, null, ETERNAL, ETERNAL, f.nar.evidence()));
    }

    public void ask(NALTask question) {
        if (!question.QUESTION_OR_QUEST())
            throw new UnsupportedOperationException();

        if (question.QUEST())
            throw new TODO();

        synchronized(this) {
            if (questions.put(question, question) == null)
                match.update();
        }

        f.accept(question);

        answerImmediate(question);
    }

    /** match possibly already-known answer, ex: memory task resolve */
    private void answerImmediate(NALTask question) {
        var a = f.nar.answer(question.term(), BELIEF, question.start(), question.end());
        if (a!=null) {
            tryAnswer(question, a);
        }
    }

    public synchronized boolean unask(NALTask question) {
        boolean removed = questions.remove(question) != null;
        if (removed)
            match.update();
        return removed;
    }

    public synchronized void clear() {
        questions.clear();
        match.update();
    }

    @Override
    public final void accept(Task X) {
        if (X instanceof NALTask x && x.BELIEF()) {
            match.match(x);
        }
    }
}
