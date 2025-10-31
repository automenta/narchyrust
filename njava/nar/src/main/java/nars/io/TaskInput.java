package nars.io;

import jcog.data.array.IntComparator;
import jcog.data.list.Lst;
import jcog.pri.PLink;
import jcog.pri.bag.impl.ArrayBag;
import jcog.sort.QuickSort;
import nars.*;
import nars.action.memory.Remember;
import nars.concept.TaskConcept;
import nars.task.SerialTask;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static jcog.util.ArrayUtil.swap;
import static nars.term.atom.Bool.Null;

public abstract class TaskInput implements Consumer<NALTask> {

    protected Focus f;

    @Override
    public final void accept(NALTask x) {
        if (x instanceof SerialTask s)
            f.activate(s);
        else
            remember(x);
    }

    public void accept(NALTask x, TaskConcept c) {
        if (x instanceof SerialTask s)
            throw new UnsupportedOperationException();
        new Remember(x, c, f);
    }

    protected abstract void remember(NALTask x);



    /**
     * remember immediately
     */
    public final Remember rememberNow(NALTask x, boolean activate) {
        Remember r = new Remember(f, activate);
        r.input(x);
        return r;
    }

    public final void rememberNow(NALTask x) {
        rememberNow(x, true);
    }


    /**
     * called on cycle start, or if focus changes
     */
    public void start(Focus f) {
        this.f = f;
    }

    /**
     * flush, called on cycle end or anytime before that
     */
    public void commit() { }

    protected void rememberAll(Map<NALTask, NALTask> map) {
        rememberAll(new RememberAll(map));
    }

    protected void rememberAll(ArrayBag<?, PLink<NALTask>> toDrain, int n, RememberAll r) {
        rememberAll(r.drain(toDrain, n));
    }

    private void rememberAll(RememberAll r) {
        r.commit(f);
    }


    /** batch impl */
    protected static final class RememberAll extends Remember implements IntComparator, IntIntProcedure {

        private final Lst<Term> concepts = new Lst<>(Op.EmptyTermArray);
        private final Lst<NALTask> tasks = new Lst<>(NALTask.EmptyNALTaskArray);
        private transient Term[] tmpConcepts;
        private transient NALTask[] tmpTasks;

        RememberAll() {

        }

        RememberAll(Stream<NALTask> t) {
            t.forEach(tasks::add);
        }

        public final void commit(Focus f) {
            focus(f);
            if (!tasks.isEmpty()) {
                if (NAL.REMEMBER_BATCHED)
                    commitBatched();
                else
                    commitDirect();
            }
            close();
        }

        private void commitDirect() {
            tasks.clear(new Remember(focus)::rememberNext);
        }

        /** pre-sorts the input, for batching tasks common to concepts.
         *  WARNING: can affect attention dynamics */
        private void commitBatched() {
            try {

                NAR nar = nar();
                for (NALTask xx : tasks)
                    concepts.add(xx.term().concept());

                int n = concepts.size(); assert(tasks.size()==n);
                if (n == 0)
                    return;

                tmpTasks = tasks.array();
                tmpConcepts = concepts.array();
                if (n > 1)
                    QuickSort.quickSort(0, n, this, this);


                Term cc = Null;
                for (int i = 0; i < n; i++) {
                    Term nc = tmpConcepts[i];

                    //update concept
                    if (!cc.equals(nc))
                        concept = nar.conceptualizeTask(cc = nc);

                    if (concept!=null) {
                        //assert(tmpTasks[i].term().concept().equals(concept.term()));
                        input(tmpTasks[i]);
                        input = stored = null;
                    }
                }

            } finally {
                concept = null;

                concepts.clear();
                tmpConcepts = null;

                tasks.clear();
                tmpTasks = null;

                close();
            }
        }

        public final RememberAll drain(ArrayBag<?, PLink<NALTask>> bag, int n) {
            tasks.ensureCapacity(n);
            bag.pop(n, false, x -> tasks.add(x.id));
            return this;
        }

        public RememberAll(Map<?, NALTask> m) {
            int n = m.size();
            tasks.ensureCapacity(n);
            MapBuffer.clear(m, tasks::add, Integer.MAX_VALUE /*n*/);
            //m.values().removeIf(z -> { tasks.add(z); return true; });
        }

        /** swapper */
        @Override public void value(int a, int b) {
            swap(tmpConcepts, a, b);
            swap(tmpTasks, a, b);
        }

        /** sorter */
        @Override public int compare(int a, int b) {
            int t =
                tmpConcepts[a].compareTo(tmpConcepts[b]);
            return t != 0 ? t :
                compare(tmpTasks[a], tmpTasks[b]);
        }

        private static int compare(NALTask A, NALTask B) {
            int p = Byte.compare(A.punc(), B.punc());
            if (p!=0) return p;
    //        int s = Long.compare(A.start(), B.start());
    //        if (s!=0) return s;
    //
    //        int e = Long.compare(A.end(), B.end());
    //        if (e!=0) return e;

            return Integer.compare(
                    System.identityHashCode(A),
                    System.identityHashCode(B));
        }
    }
}