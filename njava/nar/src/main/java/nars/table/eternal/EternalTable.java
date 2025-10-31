package nars.table.eternal;

import jcog.Util;
import jcog.data.list.Lst;
import jcog.sort.LockedFloatSortedArray;
import jcog.util.ArrayUtil;
import nars.*;
import nars.action.memory.Remember;
import nars.task.SerialTask;
import nars.truth.Stamp;
import nars.truth.proj.IntegralTruthProjection;
import nars.truth.proj.MutableTruthProjection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static nars.BeliefTable.eternalOriginality;


/**
 * Created by me on 5/7/16.
 */
public class EternalTable extends LockedFloatSortedArray<NALTask> implements BeliefTable {

    /** initializes with zero capacity. */
    public EternalTable() {
        super();
        items = NALTask.EmptyNALTaskArray; //HACK
    }

    @Override
    public final void forEachTask(long minT, long maxT, Consumer<? super NALTask> x) {
        forEachTask(x);
    }

    @Override
    public final void forEachTask(Consumer<? super NALTask> x) {
        forEach(x);
    }

    @Override
    public final int taskCount() {
        return size();
    }

    @Override
    public final Stream<? extends NALTask> taskStream() {
        return stream();
    }

    @Override
    public void match(Answer a) {
        if (size == 0)
            return; //empty
        long r = lock.readLock();
        try {
            //lock.read(() -> whileEach(a));
            whileEach(a);
        } finally {
            lock.unlockRead(r);
        }
    }

    @Override
    public void taskCapacity(int c) {
        assert (c >= 0);

        Lst<Task> trash = null;

        long l = lock.writeLock();
        try {
            if (this.capacity() == c)
                return;

            int excess = size - c;
            if (excess > 0)
                trash = compress(c, excess, trash);

            capacity(c);
        } finally {
            lock.unlock(l);
        }

        if (trash != null)
            trash.forEach(Task::delete);
    }

    private Lst<Task> compress(int c, int excess, Lst<Task> trash) {
        for (int e = 0; e < excess; e++) {
            if (!compressFast(null))
                break; //stuck
        }

        excess = size - c;
        if (excess > 0) {
            //evict
            trash = new Lst<>(excess);
            do {
                trash.addFast(removeLast());
            } while (--excess > 0);
        }
        return trash;
    }

    @Override
    public NALTask[] taskArray() {

        //long l = lock.readLock();
//        try {
        int s = this.size;
        if (s == 0)
            return NALTask.EmptyNALTaskArray;
        else {
            NALTask[] list = this.items;
            return Arrays.copyOf(list, Math.min(s, list.length));
        }
//        } finally {
//            lock.unlock(l);
//        }

    }

    /**
     * for ranking purposes.  returns negative for descending order
     */
    @Override
    public final float floatValueOf(NALTask w) {
        return (float) -eternalOriginality(w);
    }

    /** weakest to weakest, first valid is taken */
    private boolean compressFast(@Nullable Remember r) {
        int s = size();

        NAR nar = r!=null ? r.nar() : null;
        EternalReviser ar = new EternalReviser(nar!=null ? nar.timeRes() : 1);

        for (int a = s - 1; a >= 1; a--) {
            NALTask A = get(a);
            ar.set(A);
            for (int b = a - 1; b >= 0; b--) {
                NALTask B = get(b);
                NALTask AB = ar.apply(B);
                if (AB != null) {
                    //assert(a > b); //remove the higher index, A, first and B remains at same index
                    removeFast(a);
                    removeFast(b);
                    if (!contains(AB)) {
                        insert(AB);
//                        if (r!=null)
//                            r.focus.activate(new Remember(AB, r.concept));
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public final Truth truth() {
        NALTask s = first();
        return s != null ? s.truth() : null;
    }

    @Override
    public boolean remove(NALTask x, boolean delete) {

        if (cantContain(x))
            return false;

        Task removed = remove(x);

        if (removed != null) {
            if (delete)
                removed.delete();
            return true;
        } else {
            return false;
        }
    }


    @Override
    public final void remember(Remember r) {
        NALTask x = r.input;
        if (cantContain(x))
            return;

        long l = lock.readLock();
        try {
            int s = size;
            if (s > 0) {
                //scan list for existing equal task //TODO containment can be quickly tested by assuming the tasks are sorted by conf, and whether the conf is in the current range
                NALTask[] ii = items;
                int existing = ArrayUtil.indexOf(ii, x, 0, Math.min(s, ii.length));
                if (existing!=-1) {
                    r.store(ii[existing]);
                    return;
                }
            }

            int c = capacity();
            if (s >= c) {
                l = Util.readToWrite(l, lock);

                if (c == 0) {
                    if (s > 0)
                        super.delete(); //clear();
                    return;
                }

                if (s > 1)
                    compressFast(r);
                int excess = 1 + size() - c;
                if (excess > 0) {
                    //compression failed; evict
                    NALTask weakestPresent = last();
                    if (weakestPresent != null) {
                        if (!stronger(x, weakestPresent)) {
                            //rejected
                            r.unstore(x);
                            if (--excess <= 0)
                                return;
                        } else {
                            evict(excess, r);
                            //continue...
                        }
                    }
                }
            }

            l = Util.readToWrite(l, lock);

            if (capacity() > 0) { //check again in case it got deleted while waiting for lock
                insert(x);
                r.store(x);
            }

        } finally {
            if (l != 0)
                lock.unlock(l);
        }

    }

    private void evict(int n, Remember r) {
        for (int i = 0; i < n; i++)
            r.unstore(removeLast());
    }

    private static final class EternalReviser implements Function<NALTask,NALTask> {

        long[] xStamp;
        private NALTask x;

        MutableTruthProjection l;
        final int dtDither;

        /** ditherDT used in intermpolation */
        EternalReviser(int dtDither) {
            this.dtDither = dtDither;
        }

        void set(NALTask x) {
            this.x = x;
            this.xStamp = x.stamp();
        }

        @Override
        public NALTask apply(NALTask y) {
            return Stamp.overlapsAny(xStamp, y.stamp()) ? null : projection(y);
        }

        private NALTask projection(NALTask y) {
            if (l == null) {
                l = new IntegralTruthProjection(2);
                l.add(x);
            }

            //l.removeIf(z -> z != x); //remove all but 'x'
            l.add(y);
            return l.task();
        }

    }

    private static boolean stronger(NALTask a, NALTask b) {
        return eternalOriginality(b) <= eternalOriginality(a);
    }

    private static boolean cantContain(NALTask input) {
        return input instanceof SerialTask || !input.ETERNAL();
    }


}