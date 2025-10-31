package nars.game.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import jcog.TODO;
import jcog.event.Off;
import jcog.pri.PLink;
import jcog.pri.Prioritized;
import jcog.pri.op.PriMerge;
import jcog.sort.RankedN;
import jcog.sort.Top;
import jcog.sort.TopFilter;
import nars.*;
import nars.concept.PermanentConcept;
import nars.deriver.reaction.NativeReaction;
import nars.deriver.reaction.TaskReaction;
import nars.link.TaskLink;
import nars.link.TermLinkBag;
import nars.task.util.ImplTaskify;
import nars.task.util.TaskBag;
import nars.term.Compound;
import nars.term.Termed;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import static nars.Op.*;
import static nars.deriver.reaction.NativeReaction.PremiseTask;

/** batch implication applyer */
public enum Implyer { ;

    private static final boolean
        targetAllowTemporable = true,
        targetAllowVar = true,
        allowLoops = true,
        /** true is more expensive */
        consumeTryOnlyIfTaskCreated = true;

    public static void start(NARS.Rules d, boolean onLink, boolean onBelief, boolean onGoal, int maxTasks, int triesPerTask, int nodeCapacity) {
        d.add(new ImplCacheRemember(nodeCapacity));

        if (onLink)
            d.add(new ImplCacheApplyLink(maxTasks, nodeCapacity, triesPerTask));

        if (onBelief || onGoal)
            d.add(new ImplCacheApplyTask(onBelief, onGoal, triesPerTask, maxTasks, nodeCapacity));
    }

    private static @Nullable Implyer.ImplCacheNode node(@Nullable Concept c, Focus f, int capacity, boolean createIfMissing) {
        if (c == null) return null;
        var key = key(f);
        return createIfMissing ?
                c.meta/*Soft*/(key, z -> new ImplCacheNode(Math.max(1,capacity/2))) :
                c.meta(key);
    }

    /** TODO use an instance to be specific? */
    private static String key(Focus f) {
        return f.key(Implyer.class, Implyer.class.getSimpleName(), f.id);
    }

    private static boolean validTarget(Term x) {
        var s = x.struct();
        return     !hasAny(s, targetAllowVar ? VAR_INDEP.bit : Variables)
                && hasAny(s, AtomicConstant)
                && x.CONCEPTUALIZABLE()
                && (targetAllowTemporable || !x.TEMPORALABLE());
    }

    private static void filterTarget(NativeReaction r) {
        r.hasAny(PremiseTask, AtomicConstant);

        int structFilter = 0;
        structFilter |= targetAllowTemporable ? IMPL.bit : Temporals;
        structFilter |= targetAllowVar ? VAR_INDEP.bit : Variables;
        r.hasNone(PremiseTask, structFilter);
    }

    private static void update(NALTask impl, boolean subjOrPred, int capacity, Focus f) {
        var n = node(impl, subjOrPred, capacity, f);
        if (n != null)
            n.add(impl, subjOrPred);
    }

    private static @Nullable ImplCacheNode node(NALTask impl, boolean subjOrPred, int capacity, Focus f) {
        return node(f.nar.concept(impl.term().subUnneg(subjOrPred ? 0 : 1)),
                f, capacity, true);
    }

    public static void update(Predicate<Term> target, int capacity, Focus f) {
        ((Focus.TaskBagAttn)f.attn).bag.forEach(BELIEF, c -> {
            var x = c.term();
            if (x.IMPL() && (target.test(x.subUnneg(0)) || target.test(x.sub(1))))
                update(c, capacity, f);
        });
    }

    private static void update(NALTask impl, int capacity, Focus f) {
        if (!impl.BELIEF()) throw new UnsupportedOperationException();

        var t = impl.term();

        Term subj, pred;
        if (validTarget(subj = t.subUnneg(0)) && validTarget(pred = t.sub(1))) {
            if (allowLoops || !loop(subj, pred)) {
                update(impl, true, capacity, f);
                update(impl, false, capacity, f);
            }
        }
    }

    private static boolean loop(Term subj, Term pred) {
        return subj.complexity() >= pred.complexity() ? condOfPN(subj, pred) : condOfPN(pred, subj);
        //return condOfPN(subj, pred) || condOfPN(pred, subj);
    }

    private static final String STORE = Implyer.class.getSimpleName() + " _store";

    public static Off onTasks(int capacity, Focus f) {
        return f.onTask(STORE, t -> {
            if (t.term().IMPL())
                update((NALTask)t, capacity, f);
        }, BELIEF);
    }

    private static boolean condOfPN(Term subj, Term pred) {
        return subj instanceof Compound sc && sc.condOf(pred, 0);
    }

    public enum ImplyerActive { ;

        public static Runnable run(Collection<? extends Termed> concepts, boolean focusOrConcepts, int maxTasksPerConcept, int triesPerTask, int nodeCapacity, Focus f) {
            if (concepts.isEmpty())
                return ()->{ }; //HACK
            else if (focusOrConcepts) {
                var conceptTerms = Sets.newHashSet(Iterables.transform(concepts, Termed::term));
                return () -> taskifyFocusTasks(conceptTerms, maxTasksPerConcept, triesPerTask, nodeCapacity, f);
            } else
                return () -> taskifyConcepts(concepts, maxTasksPerConcept, triesPerTask, f, nodeCapacity);
        }

        private static void taskifyFocusTasks(java.util.Set<Term> concepts, int maxTasksPerConcept, int triesPerTask, int nodeCapacity, Focus f) {
            throw new TODO();
//            if (maxTasksPerConcept < 1 || triesPerTask < 1) throw new UnsupportedOperationException();
//
//            var i = new ImplTaskify(f);
//            f.tasks.model.stream(true, true, false, false)
//                .filter(cond -> concepts.contains(cond.term()))
//                .forEach(cond -> {
//                    var n = node(cond.term(), nodeCapacity, f);
//                    if (n != null)
//                        taskify(i, cond, n, cond::BELIEF, 0.5f, maxTasksPerConcept, triesPerTask, f);
//                });
        }

        private static void taskifyConcepts(Iterable<? extends Termed> concepts, int maxTasksPerConcept, int triesPerTask, Focus f, int nodeCapacity) {
            if (maxTasksPerConcept < 1 || triesPerTask < 1) throw new UnsupportedOperationException();

            var i = new ImplTaskify(f);
            var rng = f.random();
            for (var x : concepts) {
                taskifyConcept(i,
                        x instanceof PermanentConcept pc ? (Concept)pc : f.nar.concept(x),
                        rng.nextBoolean() ? BELIEF : GOAL,
                        maxTasksPerConcept, triesPerTask, nodeCapacity, f);
            }
        }

        private static void taskifyConcept(ImplTaskify i, Concept c, byte punc, int maxTasks, int triesPerTask, int nodeCapacity, Focus f) {
            var n = node(c, nodeCapacity, f);
            if (n != null)
                taskify(i, null, n, () -> punc == BELIEF, 0.5f, maxTasks, triesPerTask, f);
        }

    }





    private static class ImplCacheRemember extends TaskReaction {

        private final int nodeCapacity;

        ImplCacheRemember(int nodeCapacity) {
            super(true, false, false, false);
            is(PremiseTask, IMPL);
            this.nodeCapacity = nodeCapacity;
        }

        @Override
        protected void run(NALTask impl, Deriver d) {
            update(impl, nodeCapacity, d.focus);
        }

    }

    private static class ImplCacheApplyTask extends TaskReaction {

        private final int maxTasks, nodeCapacity, triesPerTask;

        ImplCacheApplyTask(boolean belief, boolean goal, int triesPerTask, int maxTasks, int nodeCapacity) {
            this.triesPerTask = triesPerTask;
            if (!belief && !goal)
                throw new UnsupportedOperationException("either or both: belief, goal");

            this.maxTasks = maxTasks;
            this.nodeCapacity = nodeCapacity;

            single(belief, goal,false,false);

            filterTarget(this);
        }

        @Override
        protected void run(NALTask t, Deriver d) {
            //TODO ensure that the Reaction Predicates can be used to completely elide the need to validTarget(x)
            if (validTarget(t.term())) {
                var n = node(t.term(), nodeCapacity, d);
                if (n != null)
                    taskify(t, d, n, null/*cond::BELIEF*/, maxTasks, triesPerTask);
            }
        }

    }

    private static void taskify(ImplTaskify it, @Nullable NALTask cond, ImplCacheNode n, @Nullable BooleanSupplier beliefOrGoal, float probFwd, int maxTasks, int triesPerTask, Object x) {
        int cf = n.fwd.size(), cr = n.rev.size();
        var s = cf + cr;
        if (s <= 0) return;

        var ft = cf == 0 ? 0 : (cr == 0 ? maxTasks : Math.round(maxTasks * probFwd));
        var rt = maxTasks - ft;
        n.taskify(it, cond,false, beliefOrGoal, rt, triesPerTask, x);
        n.taskify(it, cond,true, beliefOrGoal, ft, triesPerTask, x);
    }

    private static class ImplCacheApplyLink extends NativeReaction {

        final int triesPerTask;

        final int maxTasks, nodeCapacity;

        ImplCacheApplyLink(int tasks, int nodeCapacity, int triesPerTask) {
            this.triesPerTask = triesPerTask;
            maxTasks = tasks;
            this.nodeCapacity = nodeCapacity;
            tasklink();
            taskEqualsBelief();

            filterTarget(this);
            //TODO valid implication conditions
        }

        @Override protected void run(Deriver d) {
            var p = TaskLink.parentLink(d.premise);
            var x = p.from();
            if (!validTarget(x)) return; //HACK TODO use PRED's

            var n = node(x, nodeCapacity, d); if (n == null) return;

            var b = p.priPunc(BELIEF) +  p.priPunc(QUESTION);
            var g = p.priPunc(GOAL)   +  p.priPunc(QUEST);
            var bOrG = (b + g > Prioritized.EPSILON ? b/(b+g) : 0.5f);

            taskify(null, d, n, ()->d.rng.nextBoolean(bOrG), maxTasks, triesPerTask);
        }
    }

    private static void taskify(@Nullable NALTask cond, Deriver d, ImplCacheNode n, @Nullable BooleanSupplier bg, int maxTasks, int triesPerTask) {
        taskify(new ImplTaskify(d.focus), cond, n, bg, 0.5f, maxTasks, triesPerTask, d);
    }

    @Nullable private static ImplCacheNode node(Term x, int capacity, Deriver d) {
        return node(x, capacity, d.focus);
    }
    @Nullable private static ImplCacheNode node(Term x, int capacity, Focus f) {
        return node(f.nar.concept(x), capacity, f);
    }
    @Nullable private static ImplCacheNode node(Concept c, int capacity, Focus f) {
        return node(c, f, capacity, false);
    }

    private static final class ImplCacheNode {

        private static final PriMerge priMerge =
            TermLinkBag.Merge;
            //PriMerge.max;

        final TaskBag fwd, rev;

        ImplCacheNode(int capacityEach) {
            this(capacityEach, capacityEach);
        }

        ImplCacheNode(int fwdCapacity, int revCapacity) {
            fwd = new TaskBag(priMerge, fwdCapacity);
            rev = new TaskBag(priMerge, revCapacity);
        }

        public void add(NALTask x, boolean fwdOrReverse) {
            var b = table(fwdOrReverse);
            b.put(new PLink<>(x, pri(x)));
            b.commit();
        }

        /** TODO refine */
        private float pri(NALTask x) {
            return (float) x.conf();

            /* task priority */
            //return x.priElseZero();

//            /* absolute truth: range x conf */
//            long maxRangeDurs = 64;
//            float dur = d.focus.durSys;
//            float y = (float) (
//                (x.evi() * Math.min((x.ETERNAL() ? Long.MAX_VALUE : x.range()/dur), maxRangeDurs))
//                    / (maxRangeDurs/2) /* estimate */
//            );
//            return y;

//            /* temporal truth */
//            long now = d.nar.time();
//            int dur = (int)d.focus.durSys;
//            float ete = d.nar.eternalization.floatValueOf(x);
//            Truth t = x.truth(now-dur/2, now+dur/2, dur, ete, 0);
//            return (float)(Prioritized.EPSILON + t.conf());

//            /* task priority and simplicity */
//            return x.priElseZero() / (1 + x.volume()/4f);
        }

        public void print(PrintStream out) {
            fwd.print(out);
            rev.print(out);
        }

        private TaskBag table(boolean fwdOrRev) {
            return fwdOrRev ? fwd : rev;
        }

        private static final FloatFunction<NALTask> value = a -> {
            return (float) a.conf();
            //return a.priElseZero();
//                Truth y = x.truth(m, nar);
//                return y == null ? 0 : (float) y.conf();
        };

        private void taskify(ImplTaskify it, @Nullable NALTask cond, boolean fwdOrRev, @Nullable BooleanSupplier beliefOrGoal, int maxTasks, int triesPerTask, Object x) {
            if (maxTasks <= 0) return;

            TopFilter<NALTask> tasks = null;
            var iter = table(fwdOrRev).sampleUnique(rng(x));
            var tries = triesPerTask * maxTasks;
            var condBelief = cond != null && cond.BELIEF();
            for (var i = 0; i < tries && iter.hasNext(); ) {
                var t = it.task(iter.next().id, cond, fwdOrRev, beliefOrGoal!=null ? beliefOrGoal.getAsBoolean() : condBelief, x);
                if (t != null) {
                    if (tasks==null) tasks = maxTasks == 1 ? new Top<>(null) : new RankedN<>(new NALTask[maxTasks], value);
                    tasks.add(t);
                    if (consumeTryOnlyIfTaskCreated) i++;
                }
                if (!consumeTryOnlyIfTaskCreated) i++;
            }
            if (tasks!=null)
                update(tasks, x);
        }
    }

    private static RandomGenerator rng(Object x) {
        return x instanceof Deriver d ? d.rng : ((Focus) x).random();
    }

    /** ctx is either a Deriver or a Focus */
    private static void update(Iterable<NALTask> tasks, Object ctx) {
        for (var t : tasks) {
            if (ctx instanceof Deriver d)
                d.add(t, false);
            else
                ((Focus) ctx).remember(t);
        }
    }

}
