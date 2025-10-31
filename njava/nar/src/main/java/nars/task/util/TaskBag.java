package nars.task.util;

import jcog.pri.PLink;
import jcog.pri.bag.impl.PriArrayBag;
import jcog.pri.op.PriMerge;
import nars.NAL;
import nars.NALTask;

/** wraps tasks in a PLink, so that the bag's forgetting won't affect it */
public class TaskBag extends PriArrayBag<PLink<NALTask>> {

    public TaskBag(PriMerge merge, int capacity) {
        this(merge);
        capacity(capacity);
    }

    public TaskBag(PriMerge merge) {
        super(merge);
    }

    public TaskBag() {
        this(NAL.taskPriMerge);
    }

    public final void put(NALTask p) {
        put(p, p.priElseZero());
    }

    public PLink<NALTask> put(NALTask p, float pri) {
        return put(new PLink<>(p, pri));
    }

    @Override
    protected float merge(PLink<NALTask> existing, PLink<NALTask> incoming, float incomingPri) {
        return NALTask.mergeInBag(existing.id, incoming.id);
    }

}
