package nars.truth;

import jcog.util.ObjectLongLongPredicate;
import nars.*;
import nars.task.util.StampOverlapping;
import nars.task.util.TaskList;
import nars.task.util.TruthComputer;
import nars.term.Compound;
import nars.term.util.TermTransformException;
import nars.time.Tense;
import nars.truth.dynamic.DynTruth;
import org.jetbrains.annotations.Nullable;

public abstract class DynTaskify extends TaskList implements AutoCloseable, ObjectLongLongPredicate<Term>, TruthComputer {
    public final DynTruth model;

    /** occurrence time of the generated task, set by the model */
    private long start = TIMELESS, end = TIMELESS;

    /**
     * TODO change this to an int field that caches nar.dtDither()
     */
    public final int timeRes;
    public final boolean beliefOrGoal;

    @Deprecated @Nullable
    public final Compound template;

    private static final boolean ditherOcc = NAL.revision.DYNTASKIFY_OCC_DITHER;

    protected DynTaskify(DynTruth model, @Nullable Compound template, boolean beliefOrGoal, int timeRes, int capacity) {
        super(capacity == 0 ? NALTask.EmptyNALTaskArray : new NALTask[capacity], 0);
        this.template = template;
        this.model = model;
        this.timeRes = timeRes;
        this.beliefOrGoal = beliefOrGoal;
    }

    @Override
    public void close() {
        delete();
    }

    public NALTask taskClose() {
        var z = task();
        close();
        return z;
    }

    @Nullable
    public NALTask task() {
        assert (size > 0);
        if (noOverlap(NAL.answer.DYN_UNDERLAP)) {
            occUnion(); //default. may be overridden in DynTruth computeTruth
            return taskTry(model.recompose(template, this));
        }
        return null;
    }

    private @Nullable NALTask taskTry(Term x) {
        if (taskTermValid(x)) {
            return task(x);
        } else {
            if (NAL.test.DEBUG_EXTRA)
                throw new TermTransformException(this + " " + model + " fault", template, x);
            return null;
        }
    }

    protected boolean noOverlap(boolean separate) {
        var tasks = this.items;
        var s = size;
        try (var overlap = new StampOverlapping()) {
            for (var r = 0; r < s-1; r++) { //strongest first
                overlap.set(tasks[r]);
                for (var c = s - 1; c > r; c--) {  //weakest first
                    if (overlap.test(tasks[c])) {
                        //overlap: require successful separation, else fail
                        if (!separate || !Underlap.separate(tasks, r, c, null))
                            return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean noOverlap0() {
        var n = size;
        var tasks = this.items;

        if (identiticalElements(n, tasks))
            return false;

        try (var aa = new StampOverlapping()) {
            for (var a = 0; a < n - 1; a++) {
                aa.set(tasks[a]);
                for (var b = a + 1; b < n; b++) {
                    if (aa.test(tasks[b]))
                        return false;
                }
            }
        }
        return true;
    }

    private static boolean identiticalElements(int n, NALTask[] items) {
        for (var a = 0; a < n -1; a++) {
            for (var b = a + 1; b < n; b++) {
                if (items[a] == items[b])
                    return true;
            }
        }
        return false;
    }


    private byte punc() {
        return items[0].punc();
    }


    private boolean taskTermValid(@Nullable Term x) {
        if (x == null || !x.unneg().TASKABLE())
            return false;
        var t = template;
        return t == null ||
               (NAL.revision.TEMPLATE_MATCH_STRICT ? t.equalsRoot(x) : t.opID() == x.opID());
    }

    @Override
    public final Truth computeTruth() {
        var t = model.truth(this);
        return (t == null || t.evi() < eviMin()) ?
            null :
            t;
    }

    public final void occ(long s, long e) {
        this.start = s; this.end = e;
    }

    /** default time mode; TODO elide when recalculated by model */
    public final void occUnion() {
        if (size() == 1) {
            var first = getFirst();
            occ(first.start(), first.end());
        } else {
            var s = earliestStart();
            if (s == ETERNAL)
                occ(ETERNAL, ETERNAL);
            else
                occ(s, latestEnd());
        }
    }

    public abstract double eviMin();

    @Override
    public final long start() {
        throw new UnsupportedOperationException("use startEndArray()");
    }

    @Override
    public final long end() {
        throw new UnsupportedOperationException("use startEndArray()");
    }


    @Override
    public final long[] startEndArray() {
        return Tense.dither(new long[]{ start, end }, timeRes);
    }

    public abstract NAR nar();

    /**
     * earliest start time, excluding ETERNALs.  returns ETERNAL if all ETERNAL
     */
    public long earliestStart() {
        var w = minValue(t -> {
            var ts = t.start();
            return ts == ETERNAL ? TIMELESS : ts;
        });
        return w == TIMELESS ? ETERNAL : w;
    }


    public int complexityMax() {
        return Integer.MAX_VALUE;
    }

    @Nullable public final Truth taskTruth(int i) {
        return get(i).truth();
    }

}