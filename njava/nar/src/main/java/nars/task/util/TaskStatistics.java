package nars.task.util;

import jcog.Util;
import jcog.event.Off;
import nars.Concept;
import nars.NALTask;
import nars.NAR;
import nars.Task;
import nars.util.NARPart;
import org.hipparchus.stat.Frequency;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by me on 10/31/16.
 */
public class TaskStatistics extends NARPart implements Consumer<Task> {
    private final AtomicInteger i = new AtomicInteger(0);

    private final Frequency clazz = new Frequency();
    private final Frequency complexity = new Frequency();
    private final Frequency rootOp = new Frequency();
    private final Frequency punc = new Frequency();
    private final Frequency eviLength = new Frequency();

    private final Frequency freq = new Frequency();
    private final Frequency conf = new Frequency();
    private final Frequency pri = new Frequency();

    private Off on;

    public TaskStatistics() {

    }

    public TaskStatistics(NAR n) {
        super(n);
    }

    @Override
    protected void starting(NAR nar) {
        on = nar.main().eventTask.on(this);
    }

    @Override
    protected void stopping(NAR nar) {
        on.close();
        on = null;
    }

    public TaskStatistics addAll(NAR nar) {
        nar.tasks().forEach(this);
        return this;
    }

    public TaskStatistics addAll(Concept c) {
        c.tasks(true, true, true, true).forEach(this);
        return this;
    }

    public TaskStatistics addAll(Iterable<NALTask> c) {
        c.forEach(this);
        return this;
    }

    @Override public void accept(Task _t) {

        if (!(_t instanceof NALTask t) || _t.isDeleted())
            return;

        i.incrementAndGet();
        
        complexity.addValue(t.complexity());
        rootOp.addValue(t.op());
        clazz.addValue(t.getClass().toString());
        punc.addValue(t.punc());
        eviLength.addValue(t.stamp().length);

        if (t.BELIEF_OR_GOAL()) {
            freq.addValue(Util.round(t.freq(), 0.1f));
			conf.addValue(Util.round((float) t.conf(), 0.1f));
        }
        pri.addValue(Util.round(t.pri(), 0.1f));

    }

    private void print(PrintStream out) {
        out.println("-------------------------------------------------");
        out.println("Total Tasks:\n" + i.get());

        out.println("\npunc:\n" + punc);
        out.println("\nrootOp:\n" + rootOp);
        out.println("\nvolume:\n" + complexity);
        out.println("\nevidence:\n" + eviLength);
        out.println("\nclass:\n" + clazz);

        out.println("\nfreq:\n" + freq);
        out.println("\nconf:\n" + conf);
        out.println("\npri:\n" + pri);

    }


    public void print() {
        print(System.out);
    }
}