package nars.task;

import nars.NALTask;
import nars.Term;
import nars.Truth;
import nars.truth.AbstractMutableTruth;
import org.jetbrains.annotations.Nullable;

/** implementations are immutable but will usually have a different hash and
  * equality than the origin task. hashcode here is calculated lazily.
 * the only mutable components are the hashcode and the cyclic status which is optionally inherited from the source.
 * */
public class ProxyTask extends NALTask {

    public final NALTask task;

    private int hash;

    protected ProxyTask(NALTask task) {


//        if (task instanceof ProxyTask) {
//            //System.out.println(task.getClass() + " may be unwrapped for " + getClass());
//            throw new TaskException(task.getClass() + " -> double proxy -> " + getClass(), task);
//        }
        this.task = task;

        float p = task.pri();
//        if (p!=p) {
//            if (NAL.DELETE_PROXY_TASK_TO_DELETED_TASK)
//                pri.delete();
//            else
//                pri.pri(Prioritizable.EPSILON);
//        } else
            pri(p);

    }

    /** immutable */
    protected static Truth immutable(Truth truth) {
        return truth instanceof AbstractMutableTruth m ? m.immutable() : truth;
    }

//    protected boolean preValidated() {
//        return true;
//    }


    @Override
    public String toString() {
        return appendTo(null).toString();
    }


    @Override
    public Term term() {
        return task.term();
    }


    /** for hash consistency, this assumes that the involved values will not change after the initial calculation.
     * if values will change, call invalidate() to reset hash
     * */
    @Override public final int hashCode() {
        int h = this.hash;
        return h != 0 ? h :
            (this.hash = hash(term(), truth(), punc(), start(), end(), stamp()));
    }
    protected void invalidate() {
        this.hash = 0;
    }

    @Override
    public long start() {
        return task.start();
    }

    @Override
    public long end() {
        return task.end();
    }

    @Override
    public long[] stamp() {
        return task.stamp();
    }

    @Override
    public final @Nullable Truth truth() {
        return immutable(_truth());
    }

    @Nullable protected Truth _truth() {
        return task.truth();
    }

    @Override
    public byte punc() {
        return task.punc();
    }



}