package nars.task.util;

import nars.term.Termed;
import nars.util.SoftException;

/**
 * reports problems when constructing a Task
 */
public final class TaskException extends SoftException {

    private final Termed task;


    public TaskException(String message, Termed t) {
        super(message);
        this.task = t;
//        if (t instanceof Task)
//            ((Task) t).delete();
    }

    
    @Override
    public String getMessage() {
        String m = super.getMessage();
        return ((m!=null) ? m + ": " : "") + (task!=null ? task.toString() : "");
    }

}
