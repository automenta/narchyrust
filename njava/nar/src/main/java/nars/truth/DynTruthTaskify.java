package nars.truth;

import jcog.Research;
import jcog.Util;
import jcog.data.iterator.CartesianProductIndex;
import jcog.data.list.Lst;
import jcog.random.RandomBits;
import jcog.signal.meter.SafeAutoCloseable;
import jcog.sort.QuickSort;
import jcog.util.ArrayUtil;
import nars.*;
import nars.table.question.QuestionTable;
import nars.task.util.TaskList;
import nars.term.Compound;
import nars.term.Neg;
import nars.time.Tense;
import nars.truth.dynamic.DynTruth;
import nars.truth.evi.EviInterval;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.NAL.revision.DYN_DT_DITHER;

/**
 * Dynamic Taskify
 * <p>
 * uses dynamic truth models and recursive dynamic belief evaluation to compute
 * accurately truthed, timed, and evidentially stamped composite/aggregate truths
 * with varying specified or unspecified internal temporal features described by
 * a template target.
 * <p>
 * additionally tracks evidential overlap while being constructed, and provide the summation of evidence after
 */
@Research
public final class DynTruthTaskify extends DynTaskify {
    private final Answer a;

    /** cached value */
    private final float durMatch;

    private final Lst<Component> componentList;
    private transient TaskList[] tasks;
    private transient Component[] component;

    public DynTruthTaskify(DynTruth model, boolean beliefOrGoal, Answer a) {
        this(model, term(a, a.template()), beliefOrGoal, DYN_DT_DITHER ? a.nar.timeRes() : 0, a);
    }

    public DynTruthTaskify(DynTruth model, Compound template, boolean beliefOrGoal, int timeRes, Answer a) {
        super(model, template, beliefOrGoal, timeRes, 0);
        this.a = a;
        this.durMatch = a.dur();
        componentList = new Lst<>(0, new Component[model.componentsEstimate()]);
    }

    @Nullable @Override public final NALTask taskClose() {
        return expand() ? super.taskClose() : null;
    }

    public boolean expand() {
        if (model.decompose(template, a.start(), a.end(), this)) {
            component = componentList.toArrayRecycled();
            componentList.delete();
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        closeTasks();
        closeComponents();
        super.close();
    }

    /** add a component, called only by DynTruth.decompose */
    public final boolean accept(Term subterm, long start, long end) {
        if (timeRes > 1 && model.ditherComponentOcc()) {
            start = Tense.dither(start, timeRes);
            end   = Tense.dither(end, timeRes);
        }
        componentList.add(new Component(subterm, start, end, durMatch));
        return true;
    }

    @Override
    public NAR nar() {
        return a.nar;
    }

    private boolean commit() {
        var ord = ord();
        if (ord == null) return false;

        return commitIterative(ord);
        //return commitTiered(ord);
    }

    private boolean commitIterative(byte[] ord) {
        var n = a.nar;
        var cn = ord.length;
        float subDepth = subDepth(a, cn);
        var subCapacity = subCapacity(cn);
        var eviMin = eviMin(a);

        var rng = a.rng();

        var projectComponents = model.projectComponents(a.template());

        var filterOuter = filterOuter();

        int levelNext = a.level + 1;

        this.tasks = new TaskList[cn];
        ensureCapacity(cn);

        var cc = component;
        for (var i : ord) {
            var t = cc[i];
            if (!(stage0(t, n) &&
                  t.match(filter(i, filterOuter), subCapacity, subDepth, eviMin, rng, levelNext, n) &&
                  stage2(i, projectComponents)))
                return false;
        }
        return true;
    }

    /** returns eviMin threshold for valid answers */
    private double eviMin(Answer a) {
        return _eviMin(a.eviMin);
    }

    private static double _eviMin(double e) {
//        if (dynRecursionIgnorance > 0) {
//            //var m = e * (1 + dynRecursionIgnorance);
//            var m = c2e(1/(1/e2c(e) * (1-dynRecursionIgnorance)));
//            return m;
//        }
        return e;
    }

    /** combines filterOuter, and model's preFilter (filterInner) */
    private @Nullable Predicate<NALTask> filter(byte i, Predicate<NALTask> filterOuter) {
        var filterInner = model.preFilter(i, this);
        return Util.and(filterInner, filterOuter);
    }

    private @Nullable Predicate<NALTask> filterOuter() {
//        if (dynRecursionIgnoranceComponents) {
//            var eviComponentMin = _eviMin(a.eviMin);
//            return t -> t.evi() >= eviComponentMin;
//        }
        return null;
    }

    private boolean commitTiered(byte[] ord) {
        return stage0(ord) && stage1(ord) && stage2(ord);
    }

    private boolean stage0(byte[] ord) {
        var n = a.nar;
        for (var b : ord) {
            if (!stage0(component[b], n))
                return false;
        }
        return true;
    }

    private boolean stage0(Component cb, NAR n) {
        return cb.table(beliefOrGoal, n);
    }

    private boolean stage1(byte[] ord) {
        var n = a.nar;
        var cn = ord.length;
        float subDepth = subDepth(a, cn);
        var subCapacity = subCapacity(cn);
        var eviMin = eviMin(a);

        var rng = a.rng();

        var filterOuter = filterOuter();

        int levelNext = a.level + 1;

        var cc = component;
        for (var i : ord) {
            if (!cc[i].match(filter(i, filterOuter), subCapacity, subDepth, eviMin, rng, levelNext, n))
                return false;
        }
        return true;
    }

    private boolean stage2(byte[] ord) {

        var projectComponents = model.projectComponents(a.template());

        int n = ord.length;

        this.tasks = new TaskList[n];
        ensureCapacity(n);

        for (byte b : ord)
            if (!stage2(b, projectComponents))
                return false;

        return true;
    }

    private boolean stage2(byte ord, boolean projectComponents) {
        var z = component[ord].tasks(projectComponents);
        if (z != null) {
            tasks[ord] = z;
            size++;
            return true;
        } else
            return false;
    }

    @Nullable private byte[] ord() {
        var cn = this.component.length;
        return switch (cn) {
            case 0 -> null;
            case 1 -> ArrayUtil.BYTE_ARRAY_ONLY_ZERO;
            default -> ordN(cn);
        };
    }

    private byte[] ordN(int cn) {
        var ord = ArrayUtil.byteOrdinals(cn);
        QuickSort.sort(ord, this::complexity /* ascending */);
        return ord;
    }

    private int complexity(int j) {
        return component[j].x.complexity();
    }

    private float subDepth(Answer a, int components) {
        var superDepth = a.depth();
        //return superDepth / components;
        return superDepth * NAL.answer.ANSWER_DEPTH_DECAY;
        //return superDepth;
    }

    /** determines effort, recursively, in sub matches */
    private int subCapacity(int components) {
        var superCap = a.tasks.capacity();
        return superCap; //inherit from super
        //return Math.max(1, superCap - 1); //decreasing one per level
        //return Math.max(1, superCap/components); //decreasing per # components
        //return 1;
    }

    /** 0 to disable */
    public static final int ANSWER_PERMUTE_TRIES =
        //0; //DISABLED
        5;

    @Override
    public final @Nullable NALTask task() {
        return commit() ?
            (ANSWER_PERMUTE_TRIES>0 && canPermute() ?
                taskPermute(ANSWER_PERMUTE_TRIES) :
                    taskFirst())
           : null;
    }

    private boolean canPermute() {
        var options = 0;
        for (var task : tasks) {
            if ((options = Math.max(options, task.size())) > 1)
                break;//going to need permute
        }
        return options > 1;
    }

    public NALTask taskFirst() {
        var items = this.items;
        var t = this.tasks;
        var n = t.length;
        for (var i = 0; i < n; i++)
            items[i] = t[i].getFirst();
        return taskNext();
    }

    private @Nullable NALTask taskNext() {
        return super.task();
    }

    /** remaining permutions
     *  TODO iteration limit parameter, and shuffled permutation order
     *  TODO shuffle?
     */
    @Nullable private NALTask taskPermute(int tries) {
        if (tries <= 0)
            return null;

        var x = this.tasks;
        var y = this.items;
        var i = new CartesianProductIndex(taskCounts(x, x.length));
        while (tries-- > 0 && i.hasNext()) {
            permute(y, x, i.next());

            var z = taskNext();
            if (z != null)
                return z;

        }
        return null;
    }

    private static void permute(NALTask[] y, TaskList[] x, int[] p) {
        int n = x.length;
        for (var j = 0; j < n; j++)
            y[j] = x[j].get(p[j]);
    }

    static private int[] taskCounts(TaskList[] tasks, int n) {
        var counts = new int[n];
        for (var i = 0; i < n; i++)
            counts[i] = tasks[i].size();
        return counts;
    }

    @Override
    public double eviMin() {
        return a.eviMin;
    }


    private static final class Component implements SafeAutoCloseable {
        /** un-negated */
        final Term x;

        /** target start, end; may not equal the actual result's occurrence */
        //final long s, e;
        final EviInterval when;

        private final boolean neg;

        @Nullable transient TaskTable table;
        @Nullable transient Answer match;

        Component(Term x, long s, long e, float durMatch) {
            when = new EviInterval(s, e, durMatch);
            this.x = x.unneg();
            this.neg = x instanceof Neg;
        }

        @Override public void close() {
            if (match!=null) {
                match.close();
                match = null;
            }
            table = null;
        }
        boolean table(boolean beliefOrGoal, NAR n) {
            var table = n.table(x, beliefOrGoal);
            if (table != null && !table.isEmpty()) {
                this.table = table;
                return true;
            } else
                return false;
        }

        boolean match(Predicate<NALTask> filter, int capacity, float depth, double eviMin, RandomBits rng, int level, NAR nar) {
            var t = this.table;
            this.table = null; //release
            boolean beliefOrQuestion = !(t instanceof QuestionTable);
            var a = new Answer(x, beliefOrQuestion, when, capacity, nar);
            a.level = level;
            a
                .random(rng)
                .filter(filter)
                .eviMin(eviMin)
                .depth(depth)
                .match(t);

            if (a.isEmpty()) {
                a.close();
                return false; //no chance
            } else {
                this.match = a;
                return true;
            }
        }

        @Nullable public TaskList tasks(boolean projectComponents) {
            var a = this.match;
            this.match = null; /* detach: to help GC if possible */
            var z = a.tasks(projectComponents);
            return neg && z != null ? z.neg() : z;
        }


    }

    /** prefer the answer's specific term, but if necessary, provide this table's temporal generic */
    private static Compound term(Answer a, Term tableTemplate) {
        var template = a.template();
        return (Compound) (template != null ? template : tableTemplate);
    }


    private void closeComponents() {
        if (component!=null) {
            for (var c : component) c.close();
            //Arrays.fill(component, null);
            component = null;
        }
    }

    private void closeTasks() {
        if (tasks!=null) {
            for (var t : tasks) if (t!=null) t.delete();
            tasks = null;
        }
    }

//    /** tests components
//     *  TODO dont test current component?
//     * */
//    @Deprecated private boolean noOverlap(NALTask t) {
//        var overlap = new StampOverlapping().set(t);
//        for (var c : components) {
//            var m = c.match;
//            if (m !=null)
//                for (var cc : m.tasks)
//                    if (overlap.test(cc))
//                        return false;
//        }
//        return true;
//    }

}