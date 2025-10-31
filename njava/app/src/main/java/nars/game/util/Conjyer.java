package nars.game.util;

import jcog.event.Off;
import jcog.pri.PLink;
import jcog.sort.RankedN;
import jcog.sort.Top;
import jcog.sort.TopFilter;
import nars.*;
import nars.deriver.reaction.TaskReaction;
import nars.link.TermLinkBag;
import nars.task.util.TaskBag;
import nars.term.Compound;
import nars.term.util.conj.CondDiff;
import nars.truth.Stamp;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.TruthFunctions.neg;

/**
 * Batch reverse-indexed conjunction decomposer for OpenNARS, inspired by Implyer
 * UNTESTED
 *
 * TODO disjunctions
 * TODO goal conditions
 */
public enum Conjyer {
    ;

    private static final boolean ALLOW_VAR = false;

    /**
     * Start registering conjunction decomposition
     */
    public static void start(NARS.Rules d, int maxTasks, int triesPerTask, int nodeCapacity) {
        d.add(new ConjCacheRemember(nodeCapacity));
        d.add(new ConjCacheApplyTask(maxTasks, triesPerTask, nodeCapacity));
    }

    /**
     * Validate a conjunction term
     */
    private static boolean validConj(Term x) {
        var s = x.struct();
        return x.CONJ() && !hasAny(s, ALLOW_VAR ? VAR_INDEP.bit : Variables);
    }

    /**
     * Cache a conjunction task in its conditions
     */
    private static void update(NALTask conj, int capacity, Focus f) {
        var ct = conj.term();
        if (validConj(ct))
            ((Compound) ct).conds(cond -> {
                var condNode = node(cond, capacity, f, true);
                if (condNode!=null)
                    condNode.add(conj);
            }, true, false);
    }

    /**
     * Get or create a cache node
     */
    @Nullable private static ConjCacheNode node(Term cond, int capacity, Focus f, boolean createIfMissing) {
        var c = f.nar.concept(cond.unneg());
        if (c == null) return null;
        else {
            var k = f.key(Conjyer.class, Conjyer.class.getSimpleName(), f.id);
            return createIfMissing ?
                c.meta(k, z -> new ConjCacheNode(capacity)) :
                c.meta(k);
        }
    }

    /**
     * Listener for conjunction tasks
     */
    public static Off onTasks(int capacity, Focus f) {
        return f.onTask("Conjyer_store", (Task t) -> {
            if (t.term().CONJ())
                update((NALTask) t, capacity, f);
        }, BELIEF);
    }

    /**
     * Reaction to cache conjunctions
     */
    private static class ConjCacheRemember extends TaskReaction {
        private final int nodeCapacity;

        ConjCacheRemember(int nodeCapacity) {
            super(true, false, false, false);
            is(PremiseTask, CONJ);
            this.nodeCapacity = nodeCapacity;
        }

        @Override
        protected void run(NALTask conj, Deriver d) {
            update(conj, nodeCapacity, d.focus);
        }
    }

    /**
     * Reaction to decompose conjunctions
     */
    private static class ConjCacheApplyTask extends TaskReaction {
        private final int maxTasks, triesPerTask, nodeCapacity;

        ConjCacheApplyTask(int maxTasks, int triesPerTask, int nodeCapacity) {
            this.maxTasks = maxTasks;
            this.triesPerTask = triesPerTask;
            this.nodeCapacity = nodeCapacity;
            single(true, false, false, false); // Beliefs only //TODO Goals
            taskEternal(false);
            //hasAny(PremiseTask, CONJ);
        }

        @Override
        protected void run(NALTask cond, Deriver d) {
            var n = node(cond.term(), nodeCapacity, d.focus, false);
            if (n != null)
                taskify(n, cond, d);
        }

        private void taskify(ConjCacheNode n, NALTask cond, Deriver d) {
            var f = d.focus;
            var ct = new ConjTaskify(f /*, new EviIntegral(cond.start, cond.end, ..) */);
            TopFilter<NALTask> tasks = maxTasks == 1 ? new Top<>(null) : new RankedN<>(new NALTask[maxTasks], t -> (float) t.conf());
            var tries = triesPerTask * maxTasks;

            var dur = f.dur();
            var nar = f.nar;
            var eviMin = nar.eviMin();
            var timeRes = nar.timeRes();
            var iter = n.conjs.sampleUnique(d.rng);
            for (var i = 0; i < tries && iter.hasNext(); i++) {
                var conj = iter.next().id;
                if (!NAL.premise.OVERLAP_DOUBLE_MODE.overlapping(conj, cond)) {
                    var y = ct.decompose(conj, cond, dur, eviMin, timeRes);
                    if (y!=null)
                        tasks.add(y);
                }
            }

            for (var t : tasks)
                d.add(t, false);
        }
    }

    /**
     * Taskification for conjunction decomposition
     */
    private static class ConjTaskify {
        //private final EviInterval evi;
        private final Focus focus;

        ConjTaskify(Focus f) {
            this.focus = f;
            //var se = f.when();
            //this.evi = new EviInterval(se[0], se[1], f.dur());
        }

        @Nullable public NALTask decompose(NALTask conj, NALTask cond, float dur, double eviMin, int timeRes) {

            var condTerm = cond.term();
            var conjTerm = conj.term();

            var condTruth = cond.truth();

            if (condTruth.NEGATIVE()) {
                condTruth = condTruth.neg();
                condTerm = condTerm.neg();
            }

            //compute offset to shift
            var CT = (Compound) conjTerm;
            var condOcc = CT.when(condTerm, true);
            if (condOcc == XTERNAL) {
                //try the negation
                condTruth = condTruth.neg();
                condTerm = condTerm.neg();
                condOcc = CT.when(condTerm, true);
                if (condOcc == XTERNAL)
                    return null; //throw new WTF();
            }

            if (condOcc == DTERNAL) condOcc = 0; //HACK

            var decomposed = CondDiff.diffAny(conjTerm, condTerm, false);
            if (!decomposed.TASKABLE() || decomposed.equals(conjTerm)) return null;

            var cs = cond.start();

            var conjTruth = conj.truth(
                    cs, cond.end(),
                    dur, focus.nar.eternalization.floatValueOf(conj), eviMin);
            if (conjTruth == null || conjTruth.evi() < eviMin) return null;

            var truth =
                neg(TruthFunctions.Divide.truth(conjTruth.neg(), condTruth, eviMin));
                //NALTruth.Divide.truth(conjTruth, condTruth, eviMin);
            if (truth == null) return null;

            var oy = cs - condOcc;
            var r = Math.min(conj.range(), cond.range()) - 1;
            return NALTask.task(decomposed, BELIEF, truth,
                oy, oy + r,
                Stamp.zip(conj, cond)
            );
        }

    }

    /**
     * Cache node for conjunctions
     */
    private static class ConjCacheNode {
        private final TaskBag conjs;

        ConjCacheNode(int capacity) {
            this.conjs = new TaskBag(TermLinkBag.Merge, capacity);
        }

        public void add(NALTask conj) {
            conjs.put(new PLink<>(conj, (float) conj.conf()));
            conjs.commit();
        }
    }
}