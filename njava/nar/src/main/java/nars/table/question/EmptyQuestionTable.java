package nars.table.question;

import nars.Answer;
import nars.NALTask;
import nars.action.memory.Remember;

import java.util.function.Consumer;
import java.util.stream.Stream;

public class EmptyQuestionTable implements QuestionTable {

    @Override
    public Stream<? extends NALTask> taskStream() {
        return Stream.empty();
    }

    @Override
    public void match(Answer a) {

    }

    @Override
    public void remember(/*@NotNull*/ Remember r) {

    }


    @Override
    public void clear() {

    }

    @Override
    public void forEachTask(Consumer<? super NALTask> x) {

    }

    @Override
    public boolean remove(NALTask x, boolean delete) {
        return false;
    }


    @Override
    public void taskCapacity(int newCapacity) {

    }


    @Override
    public int taskCount() {
        return 0;
    }

    @Override
    public int capacity() {
        return 0;
    }

}