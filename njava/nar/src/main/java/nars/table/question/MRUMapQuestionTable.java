package nars.table.question;

import jcog.TODO;
import jcog.data.iterator.ArrayIterator;
import jcog.data.map.MRUMap;
import nars.Answer;
import nars.NALTask;
import nars.Task;
import nars.action.memory.Remember;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * unsorted, MRU policy.
 * this impl sucks actually
 * TODO make one based on ArrayHashSet
 */
public class MRUMapQuestionTable extends MRUMap<NALTask, NALTask> implements QuestionTable {


    public MRUMapQuestionTable() {
        super(0);
    }

    @Override
    public synchronized void taskCapacity(int newCapacity) {
        super.setCapacity(newCapacity);
    }

    @Override
    public final int taskCount() {
        return size();
    }

    @Override
    public void remember(/*@NotNull*/ Remember r) {
        NALTask t = r.input;
        synchronized (this) {
            NALTask u = merge(t, t, (prev, next) -> {
				r.store(prev);
				return next;
            });
        }

    }
    @Override
    public void match(Answer a) {
        //sample(m.nar.random(), size(), m::tryAccept);
        throw new TODO();
    }

    @Override
    public Stream<? extends NALTask> taskStream() {
        return ArrayIterator.stream(taskArray());
    }

    public NALTask[] taskArray() {

        int s = size();
        if (s == 0) {
            return NALTask.EmptyNALTaskArray;
        } else {
            synchronized (this) {
                return values().toArray(new NALTask[s]);
            }
        }

    }

    @Override
    public void forEachTask(Consumer<? super NALTask> x) {
        for (NALTask y : taskArray()) {
            if (y == null)
                continue;
            if (y.isDeleted()) {
                remove(y, false);
            } else {
                x.accept(y);
            }
        }
    }

    @Override
    public synchronized boolean remove(NALTask x, boolean delete) {
        Task r = remove(x);
        if (r != null) {
            if (delete)
                r.delete();
            return true;
        }
        return false;
    }

    @Override
    public synchronized void clear() {
        super.clear();
    }

}