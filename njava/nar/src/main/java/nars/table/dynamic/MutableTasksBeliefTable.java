package nars.table.dynamic;

import nars.NAL;
import nars.Term;
import nars.action.memory.Remember;
import nars.task.SerialTask;
import nars.util.RingIntervalSeries;
import org.jetbrains.annotations.Nullable;


/**
 * overlay table with N mutable recycled tasks (stored in a sequence or near sequence)
 * that can be manipulated directly in access orders such as FIFO or LIFO
 */
public class MutableTasksBeliefTable extends TaskSeriesSeriesBeliefTable {

    @Deprecated public MutableTasksBeliefTable(Term c, boolean beliefOrGoal, int cap) {
        super(c, beliefOrGoal, cap);
    }

    private SerialTask first() { return tasks.first(); }
    private SerialTask last()  { return tasks.last(); }
    private SerialTask pollFirst()   { return tasks.poll(); }

    @Override
    public void remember(Remember r) {
        //throw new UnsupportedOperationException();
        //DROP?
    }

    public final SerialTask setOrAdd(float freq, double evi, long start, long end, float pri, NAL n) {
        return _setOrAdd(setOrAddNext(), freq, evi, start, end, pri, n);
    }

    @Nullable private SerialTask setOrAddNext() {
        int c = tasks.capacity();
        if (c == 1)
            return first();
        else
            return tasks.size() >= c ? pollFirst() : null; //re-use oldest
    }

    public final SerialTask set(int i, float freq, double evi, long start, long end, float pri, NAL n) {
        return _setOrAdd(get(i), freq, evi, start, end, pri, n);
    }

    private SerialTask _setOrAdd(@Nullable SerialTask x, float freq, double evi, long start, long end, float pri, NAL n) {
        if (x == null) {
            var y = task(start, end, freq, evi, pri, n);
            tasks.add(y);
            return y;
        } else {
            x.set(start, end, freq, evi, pri);
            return x;
        }
    }

    @Nullable public SerialTask get(int i) {
        return ((RingIntervalSeries<SerialTask>) tasks).peek(i);
    }

    public final SerialTask task(long start, long end, float f, double evi, float pri, NAL n) {
        return taskSerial(start, end, f, evi, n).withPri(pri);
    }

}