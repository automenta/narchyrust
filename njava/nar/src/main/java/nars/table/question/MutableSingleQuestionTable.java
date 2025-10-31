package nars.table.question;

import nars.task.SerialTask;

public class MutableSingleQuestionTable extends SingleQuestionTable<SerialTask> {

    public MutableSingleQuestionTable(SerialTask serialTask) {
        super(serialTask);
    }

    public final MutableSingleQuestionTable creation(long w) {
        task.setCreation(w);
        return this;
    }

    public final MutableSingleQuestionTable occ(long s, long e) {
        task.occ(s, e);
        return this;
    }

    public final MutableSingleQuestionTable pri(float p) {
        task.pri(p);
        return this;
    }
}