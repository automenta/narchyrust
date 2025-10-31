package nars;

import nars.action.memory.Remember;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * holds a set of ranked question/quests tasks
 * top ranking items are stored in the lower indexes so they will be first iterated
 */
public interface TaskTable {

    /**
     * attempt to insert a task.
     */
    void remember(Remember r);

    default boolean tryRemember(Remember r) {
        remember(r);
        return r.stored();
    }

    /**
     * number of items in this collection
     * warning: size()==0 does not necessarily mean that isEmpty(), although this is true for the default implementation
     */
    int taskCount();

    int capacity();

    default boolean isEmpty() {
        return taskCount() == 0;
    }


    default void forEachTask(Consumer<? super NALTask> x) {
        taskStream().forEach(x);
    }

    /**
     * TODO add 'intersects or contains' option
     * note: minT and maxT are raw, ie. ETERNAL,ETERNAL isnt what you want, instead: all = Long.MIN_VALUE, Long.MAX_VALUE
     */
    default void forEachTask(long minT, long maxT, Consumer<? super NALTask> x) {
        Consumer<? super NALTask> y;
        if (minT == Long.MIN_VALUE && maxT == Long.MAX_VALUE) {
            y = x;
        } else {
            y = t -> {
                if (t.intersectsRaw(minT, maxT))
                    x.accept(t);
            };
        }
        forEachTask(y);
    }

    void taskCapacity(int newCapacity);

    /**
     * returns true if the task was removed.
     * implementations should delete the task only when delete is true
     */
    boolean remove(NALTask x, boolean delete);

    void clear();

    /** in dynamic implementations, this will be an empty stream */
    Stream<? extends NALTask> taskStream();

    default NALTask[] taskArray() {
        return taskStream().toArray(NALTask[]::new);
    }

    /** dont call directly.  Answer is the only caller that should use it
     *  TODO add ttl parameter */
    void match(Answer a);


    /** clear and fully deallocate if possible */
    default void delete() {
        clear();
    }

    default void print(PrintStream out) {
        //TODO buffer to StringBuilder first
        this.forEachTask(t -> println(out, t));
        out.println();
    }

    static void println(Appendable out, NALTask t)  {
        try {
            out.append(String.valueOf(t)).append(' ').append(Arrays.toString(t.stamp())).append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    default void print() {
        print(System.out);
    }

    @Nullable default TaskTable ifNotEmpty() {
        return isEmpty() ? null : this;
    }
}