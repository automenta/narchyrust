package nars.table.question;

import nars.Answer;
import nars.NALTask;
import nars.action.memory.Remember;

import java.util.stream.Stream;

/** read-only */
public class SingleQuestionTable<T extends NALTask> implements QuestionTable {

    public final T task;

    public SingleQuestionTable(T t) {
        if (!t.QUESTION_OR_QUEST()) throw new UnsupportedOperationException();
        this.task = t;
    }

    @Override
    public void remember(Remember r) {
        if (r.input == task) {
            r.store(task);
        } else {
            //ignore
        }
    }

    @Override
    public int taskCount() {
        return 1;
    }

    @Override
    public int capacity() {
        return 1;
    }

    @Override
    public void taskCapacity(int newCapacity) {
        //ignore
    }

    @Override
    public boolean remove(NALTask x, boolean delete) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public Stream<? extends NALTask> taskStream() {
        return Stream.of(task);
    }

    @Override
    public void match(Answer a) {
        a.test(task);
    }
}