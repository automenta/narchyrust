package nars.task;

import nars.NALTask;
import nars.Task;
import org.jetbrains.annotations.Nullable;

public final class DerivedTask extends ProxyTask {

    private final Task parentTask;
    private final @Nullable Task parentBelief;

    public DerivedTask(NALTask y, NALTask task, NALTask belief) {
        super(y);

        this.parentTask = task.the();

        this.parentBelief = belief !=null ? belief.the() : null;

        copyMeta(y);
    }

    private Task parentTask() {
        return parentTask;
    }

    public @Nullable Task parentBelief() {
        return parentBelief;
    }

    @Override
    public void proof(int indent, StringBuilder sb) {
        super.proof(indent, sb);

        //if (task instanceof DerivedTask) {
            Task pt = parentTask();
            if (pt != null)
                pt.proof(indent + 1, sb);
            Task pb = parentBelief();
            if (pb != null)
                pb.proof(indent + 1, sb);
        //}
    }
}