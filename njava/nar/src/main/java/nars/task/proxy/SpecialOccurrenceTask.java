package nars.task.proxy;

import nars.NALTask;
import nars.task.ProxyTask;

public class SpecialOccurrenceTask extends ProxyTask {

    private final long start, end;

    public SpecialOccurrenceTask(NALTask task, long start, long end) {
        super(task instanceof SpecialOccurrenceTask s ? s.task : task);
        this.start = start;
        this.end = end;
    }

    @Override
    public final long start() {
        return start;
    }

    @Override
    public final long end() {
        return end;
    }

}