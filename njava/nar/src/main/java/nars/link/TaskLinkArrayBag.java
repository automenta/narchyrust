package nars.link;

import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.op.PriMerge;
import jcog.util.PriReturn;
import nars.NAL;

public class TaskLinkArrayBag extends ArrayBag<TaskLink, TaskLink> {

    public TaskLinkArrayBag(PriMerge m) {
        super(m);
    }

    @Override
    protected float merge(TaskLink existing, TaskLink incoming, float incomingPri) {
        return existing.merge(incoming, merge(), PriReturn.Result);
    }

    @Override
    protected float sortedness() {
        return NAL.tasklinkSortedness;
    }

    @Override
    public TaskLink key(TaskLink value) {
        return value;
    }

//    public Bag<TaskLink, TaskLink> buffered() {
//        return new BufferedBag<>(this, new PriMap<>() {
//            @Override
//            protected float merge(Prioritizable existing, TaskLink incoming, float pri, PriMerge merge) {
//                return ((TaskLink)existing).merge(incoming, merge, PriReturn.Delta);
//            }
//        });
//    }
}