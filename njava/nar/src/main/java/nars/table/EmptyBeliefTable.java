package nars.table;

import nars.Answer;
import nars.BeliefTable;
import nars.NALTask;
import nars.action.memory.Remember;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class EmptyBeliefTable implements BeliefTable {

    public static final BeliefTable Empty = new EmptyBeliefTable();

    protected EmptyBeliefTable() {

    }

    @Override
    public Stream<? extends NALTask> taskStream() {
        return Stream.empty();
    }

    @Override
    public void match(Answer a) {
        //nothing
    }

    @Override
    public int capacity() {
        return 0;
    }

    @Override
    public NALTask[] taskArray() {
        return NALTask.EmptyNALTaskArray;
    }

    @Override
    public void forEachTask(Consumer<? super NALTask> x) {

    }

    @Override
    public boolean remove(NALTask x, boolean delete) {
        return false;
    }


    @Override
    public void forEachTask(long minT, long maxT, Consumer<? super NALTask> x) {

    }

    @Override
    public void taskCapacity(int newCapacity) {

    }


    @Override
    public int taskCount() {
        return 0;
    }


    @Override
    public void remember(/*@NotNull*/ Remember r  /*@NotNull*/) {

    }

    @Override
    public void print(/*@NotNull*/ PrintStream out) {

    }

    @Override
    public void clear() {

    }

}