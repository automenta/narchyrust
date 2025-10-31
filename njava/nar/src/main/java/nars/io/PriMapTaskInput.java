package nars.io;

import jcog.TODO;
import jcog.pri.PriMap;
import nars.NALTask;

import static nars.NAL.taskPriMerge;

/**
 * should be equivalent or better than MapTaskInput
 */
public final class PriMapTaskInput extends AbstractMapTaskInput {

    final PriMap<NALTask> tasks = new PriMap();

    public PriMapTaskInput(float capacityProportion) {
        super(capacityProportion);
    }

    @Override
    public void remember(NALTask t) {
        tasks.put(t, taskPriMerge, null);
    }

    @Override
    protected int size() {
        return tasks.size();
    }

    @Override
    protected void _drain() {
        throw new TODO();
    }

    @Override
    public void clear() {
        tasks.clear();
    }
}
