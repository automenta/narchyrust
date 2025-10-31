package nars.game.util;

import com.google.common.collect.Iterables;
import jcog.Log;
import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.path.FromTo;
import jcog.data.graph.search.Search;
import jcog.data.list.Lst;
import jcog.pri.PLink;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PriReferenceArrayBag;
import jcog.pri.op.PriMerge;
import nars.*;
import nars.game.Game;
import nars.game.action.AbstractAction;
import nars.game.reward.Reward;
import nars.subterm.Subterms;
import nars.task.util.TaskList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjTree;
import nars.time.Tense;
import nars.truth.AbstractMutableTruth;
import nars.truth.MutableTruth;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static jcog.Util.emptyIterable;
import static nars.NAL.STAMP_CAPACITY;
import static nars.Op.*;
import static nars.term.atom.Bool.Null;

/**
 * Implication Graph / Compiler
 * a set of plugins that empower and accelerate causal reasoning
 * <p>
 * see: https:
 * see: https:
 */
public enum Impiler {
    ;

    /** concept metadata field key storing impiler node instances */
    private static final String IMPILER_NODE = Impiler.class.getSimpleName();

    public static @Nullable Impiler.ImpilerNode node(Termed x, boolean createIfMissing, NAR nar) {
        @Nullable Concept nn = createIfMissing ? nar.conceptualize(x) : nar.concept(x);
        return nn == null ? null : node(nn, createIfMissing);
    }

    public static @Nullable Impiler.ImpilerNode node(Concept c) {
        return node(c, true);
    }

    public static @Nullable Impiler.ImpilerNode node(Concept c, boolean createIfMissing) {
        return createIfMissing ?
            c.meta(IMPILER_NODE, () -> new ImpilerNode(c.term())) :
            c.meta(IMPILER_NODE);
    }

    public static boolean filter(Term next) {
        return next.IMPL() && !next.hasVars();
    }

    private static float implValue(Task t) {
        return t.priElseZero();
    }

//    public static void graphGML(Iterable<? extends Concept> concepts, PrintStream out) {
//        GraphIO.writeGML(graph(concepts), out);
//    }



    /**
     * creates graph snapshot
     */
//    public static Was graph(Iterable<? extends Concept> concepts) {
//        var g = new Was();
//        g.add(concepts);
//        return g;
//    }

    /**
     * all tasks in NAR memory
     */
    public static void load(NAR n) {
        load(n.concepts(), n).forEach((x)->{ });
    }

    public static Stream<ImpilerNode> load(Stream<? extends Concept> cc, NAR n) {
        return cc
        .filter(c -> filter(c.term()))
        .flatMap(c -> c.beliefs().taskStream())
        .flatMap(t -> load(t, n));
    }

    public static void load(Focus w) {
        load(w.concepts(), w.nar).forEach((x)->{ });
    }



    public static Stream<ImpilerNode> load(Task t, NAR nar) {
        Term tt = t.term();
        Subterms ttt = tt.subterms();
        Term subj = ttt.sub(0), pred = ttt.sub(1);

        Concept sc = nar.conceptualize(subj);
        if (sc != null) {
            Concept pc = nar.conceptualize(pred);
            if (pc != null) {
                if (!sc.term().equals(pc.term())) {
                    ImpilerNode nn = node(sc);
                    ImpilerNode mm = node(pc);
                    nn.add(true, t, pc);
                    mm.add(false, t, sc);
                    return Stream.of(nn, mm);

//                    if (t.isPositive() && pred.CONJ() && !Conj.isSeq(pred.term())) {
//                        //branch parallel conjunction to components
//
//                        //TODO check truth correctness here
//                        for (Term predSub : pred.subterms()) {
//                            Concept pcSub = nar.conceptualize(predSub);
//                            if (pcSub!=null) {
//                                //TODO the forward link?
//                                node(pcSub).add(false, t /* re-use though target will not be the containing conj */, sc);
//                            }
//                        }
//                    }
                }
            }
        }

        return Stream.empty();
    }

    public static void impile(Game g) {
//        String key = "impiler_" + g.id;

        Runnable x = new Runnable() {

            final int dursAhead = 4;

            private ImpilerDeduction ii;
            final Focus w = g.focus();
            final NAR n = w.nar;


            private double dur;
            private long now;
            private int dtDither;

            @Override
            public void run() {

                dur = g.dur();
                now = g.time();
                dtDither = n.timeRes();

                load(w);

                ii = new ImpilerDeduction(n);
                for (AbstractAction a : g.actions)
                    predict(n.goalPriDefault.pri(), a, false);
                for (Reward r : g.rewards)
                    predict(n.beliefPriDefault.pri(), r, true);
            }

            public void predict(float pri, Termed X, boolean beliefOrGoal) {

                byte punc = beliefOrGoal ? BELIEF : GOAL;


                LongToObjectFunction<Truth> projector = ii.projector(X, beliefOrGoal, false);
                if (projector == null)
                    return;


                Term x = X.term();

                double durOffset = 0.5f;
                for (int d = 0; d < dursAhead; d++) {
                    long ww = now + Math.round((d + durOffset) * dur);
                    Truth truth = projector.apply(Math.round(ww));
                    if (truth != null) {

                        long windw = Math.round(dur / 2);
                        long[] se = {ww - windw, ww + windw};
                        Tense.dither(se, dtDither);

                        NALTask y = NALTask.taskUnsafe(x, punc, truth, se[0], se[1], n.evidence());
                        y.pri(pri);

                        w.remember(y);
                    }
                }
            }
        };

        g.onFrame(x);


    }


    private static class ImpilerNode extends MapNodeGraph.AbstractNode<Term, Task> {

        @Deprecated static final int CAP = 16; //TODO curve

        final Bag<Task, ImplPLink> tasks = new PriReferenceArrayBag<>(PriMerge.max);

        ImpilerNode(Term id) {
            this(id, (int) (Math.ceil((float)CAP) / (1 + Util.sqrt(id.complexity()))));
        }

        ImpilerNode(Term id, int cap) {
            super(id);
            tasks.capacity(cap);
        }

        final void add(boolean direction, Task t, Concept target) {
            ImplPLink x = new ImplPLink(t, implValue(t), direction, target);
            ImplPLink y = tasks.put(x);
            if (y != null)
                tasks.commit();
        }

        @Override
        public String toString() {
            return id + ":" + tasks.toString();
        }

        @Override
        public Iterable<FromTo<MapNodeGraph.AbstractNode<Term, Task>, Task>> edges(boolean in, boolean out) {
            assert (in ^ out);
            if (tasks.isEmpty())
                return Collections.EMPTY_LIST;

            return Iterables.filter(Iterables.transform(tasks, (tLink) -> {

                boolean dir = tLink.direction;
                if ((!out || !dir) && (!in || dir))
                    return null;

                if (tLink.target.isDeleted()) {
                    tLink.delete();
                    return null;
                }

                var other = node(tLink.target, true);

                Task task = tLink.id;

                return out ? edge(this, task, other) : edge(other, task, this);

            }), Objects::nonNull);
        }

        private static final class ImplPLink extends PLink<Task> {

            /**
             * true = out, false = in
             */
            private final boolean direction;

            /**
             * TODO weakref?
             */
            private final Concept target;

            ImplPLink(Task task, float p, boolean direction, Concept target) {
                super(task, p);
                this.direction = direction;
                this.target = target;
            }
        }

    }

    /** instance of an impiler deduction search */
    static class ImpilerDeduction extends Search<Term, NALTask> {

        final int recursionMin = 2;
        final int recursionMax = 4;
        static final int volPadding = 2;

        private final NAR nar;


        /** temporary buffer to collect results */
        private transient @Nullable List<NALTask> in;

        private final transient int volMax;
        private final transient double eviMin;
        private final transient long now;
        private final transient float dur;

        private transient long start;
        private transient boolean forward;
        private final transient ConjBuilder cc = new ConjTree();


        ImpilerDeduction(NAR nar) {
            this.nar = nar;
            this.volMax = nar.complexMax() - volPadding;
            this.eviMin = nar.eviMin();
            this.now = nar.time();
            this.dur =
                //0; //?
                nar.dur();
        }


        public @Nullable LongToObjectFunction<Truth> projector(Termed x, boolean beliefOrGoal, boolean forwardOrReverse) {
            List<NALTask> t = get(x, forwardOrReverse);
            if (t.isEmpty())
                return null;

            return when -> {


                double F = 0, E = 0;

                //TODO use DynTaskify and SpeciaTermTruthOccTask's that apply the impl predicate as the facade content term -- enables stamp overlap check, etc

                for (NALTask impl : t) {
                    Term ii = impl.term();
                    int dt = ii.DT();

                    Term subj = ii.sub(0);
                    long subjCondStart = when - (subj.seqDur() + dt);
                    Truth subjCondTruth = beliefOrGoal ?
                        nar.beliefTruth(subj, subjCondStart) : nar.goalTruth(subj, subjCondStart);

                    if (subjCondTruth!=null) {

                        Truth implTruth = impl.truth();
                        //boolean neg = implTruth.isNegative();
                        //Truth c = NALTruth.Deduction.apply(sTruth, implTruth.negIf(neg), 0); //TODO correct truth func for goal


                        boolean invert;
                        Truth c;

                        if (!forwardOrReverse) {
                            invert = beliefOrGoal && implTruth.NEGATIVE();
                            c = beliefOrGoal ?
                                //    B, (  X ==> C), --is(X,"--"), --is(B,"==>"), --isVar(C)       |-   unisubst(C,X,B,"$"),  (Belief:DeductionRecursivePP, Time:TaskEvent)
                                TruthFunctions.Pre.truth(subjCondTruth, implTruth.negIf(invert))
                                :
                                //    B, (  X ==> C), --is(B,"==>"), --isVar(C)   |-  unisubst(C,X,polarizeTask(B),"$"),  (Goal:DesireWeakDPX, Time:TaskEvent)
                                TruthFunctions.PreWeak.truth(implTruth, subjCondTruth);
                        } else {
                            //TODO test
                            invert = false;
                            c = beliefOrGoal ?
                                //B, (X ==> A), --is(B,"==>"), --isVar(X) |- unisubst(X,A,B,"$"), (Belief:PostWeakPP, Time:TaskEvent)
                                TruthFunctions.PostWeak.truth(subjCondTruth, implTruth)
                                :
                                //B, (X ==> A), --is(B,"==>"), --isVar(X) |-   unisubst(X,A,B,"$"), (Goal:PostPP, Time:TaskEvent)
                                TruthFunctions.Post.truth(implTruth, subjCondTruth);
                        }

                        if (c != null) {

                            double cf = c.freq();
                            if (invert)
                                cf = 1 - cf;
                            double ce = c.evi();
                            E += ce;
                            F += cf * ce;
                        }
                    }
                }
                return E >= eviMin ?
                    PreciseTruth.byEvi((F / E), E)
                    :
                    null;
            };
        }

        public List<NALTask> get(Termed x, boolean forward) {
            return get(x, now, forward);
        }

        /**
         * get the results
         */
        public /* synchronized */ List<NALTask> get(Termed target, long when, boolean forward) {

            this.in = null; //reset for repeated invocation

            Term t = target.term();
            if (t.IMPL())
                target = t.sub(forward ? 0 /* subj */ : 1 /* pred */);

    //        Term target1 = target.unneg();
            this.forward = forward;
            this.start = when;

            ImpilerNode rootNode = node(target, true, nar);
            if (rootNode != null) {
                bfs(rootNode);
                clear(); //clear search log

                if (in != null)
                    return in;
            }

            return Collections.EMPTY_LIST;
        }

        /**
         * out only
         */
        @Override
        protected Iterable<FromTo<MapNodeGraph.AbstractNode<Term, NALTask>, NALTask>> search(MapNodeGraph.AbstractNode<Term, NALTask> n, List<BooleanObjectPair<FromTo<MapNodeGraph.AbstractNode<Term, NALTask>, NALTask>>> path) {
            if (path.size() >= recursionMin && !deduce(path))
                return emptyIterable; //boundary

            //path may have grown?
            return path.size() + 1 > recursionMax ? emptyIterable : n.edges(!forward, forward);

        }

        @Override
        protected boolean go(List<BooleanObjectPair<FromTo<MapNodeGraph.AbstractNode<Term, NALTask>, NALTask>>> path, MapNodeGraph.AbstractNode<Term, NALTask> next) {
            return true;
        }

        /**
         * returns whether to continue further
         */
        boolean deduce(List<BooleanObjectPair<FromTo<MapNodeGraph.AbstractNode<Term, NALTask>, NALTask>>> path) {

            int n = path.size();

            TaskList pathTasks = new TaskList(n);

            Term iPrev = null;
            int volEstimate = 1 + n / 2; //initial cost estimate
            for (int i = 0, pathSize = path.size(); i < pathSize; i++) {
                BooleanObjectPair<FromTo<MapNodeGraph.AbstractNode<Term, NALTask>, NALTask>> ii = path.get(forward ? i : (n - 1 - i));
                NALTask e = ii.getTwo().id();
                Term ee = e.term();
                volEstimate += ee.complexity();
                if (volEstimate > volMax)
                    return false; //estimated path volume exceeds limit


                if (i > 0 && !links(ee.subUnneg(0), iPrev.sub(1))) //TODO this pair may only apply to reverse direction.  forward may need a slightly different comparison
                    return false; //path is not continuous

                pathTasks.add(e);


                for (int k = 0; k < i; k++)
                    if (Stamp.overlap(e, pathTasks.get(k)))
                        return false;

                iPrev = ee;
            }


            AbstractMutableTruth tAccum = null;

            long offset = start;

            long nowStart = Math.round(now - dur);
            long nowEnd = Math.round(now + dur);

            float[] xFactor = new float[pathTasks.size()];
            for (int i = 0, pathTasksLength = pathTasks.size(); i < pathTasksLength; i++) {
                NALTask e = pathTasks.get(i);
                Truth tCurr = e.truth(nowStart, nowEnd, 0, 0, NAL.truth.EVI_MIN);
                if (tCurr == null)
                    return false; //too weak

                Term ee = e.term();
                int edt = ee.dt();
                if (edt == DTERNAL) edt = 0;

                if (forward) {
                    offset += ((Compound)ee).seqDurSub(0, false) + edt;
                } else {
                    offset += -((Compound)ee).seqDurSub(1, false) - edt;
                }

                if (tAccum == null) {
                    tAccum = new MutableTruth(tCurr);
                } else {

                    if (ee.sub(0) instanceof Neg) {
                        tAccum.negThis(); //negate incoming truth to match the negated precondition
                    }

                    if (i == n-1 && e.NEGATIVE()) {
                        //negate so that the deduction belief truth is positive because the final implication predicate will be inverted
                        tCurr = tCurr.neg();
                    }

                    Truth tNext = TruthFunctions.Deduction.truth(tAccum, tCurr);
                    if (tNext == null)
                        return false;

                    tAccum.set(tNext);
                }
                xFactor[i] = (float) tCurr.evi();
            }

            if (tAccum.evi() < eviMin)
                return false;

            cc.clear();

            Term before = Null, next = Null;
            offset = 0;
            long range = Long.MAX_VALUE;
            int zDT = 0;
            for (int i = 0, pathTasksLength = pathTasks.size(); i < pathTasksLength; i++) {
                NALTask e = pathTasks.get(i);


                long es = e.start();
                if (es != ETERNAL)
                    range = Math.min(range, e.end() - es);

                int ees = forward ? 0 : 1;

                Term ee = e.term();
                before = ee.sub(1 - ees);
                if (forward) before = before.negIf(e.NEGATIVE());

                int dt = switch (ee.dt()) {
                    //HACK
                    case DTERNAL -> 0;
                    case XTERNAL -> throw new UnsupportedOperationException();
                    default -> ee.dt();
                };

                next = ee.sub(ees);
                if (!forward) next = next.negIf(e.NEGATIVE());

                zDT = dt;

                if (i == 0)
                    cc.add(0, forward ? next : before);

                offset += (forward ? next : before).seqDur() + dt;

                if (i != n - 1) {
                    if (!cc.add(offset, (forward ? before : next)))
                        return false;
                }

            }

            Term ccc = cc.term();
            if (ccc == Null) return false;

            Term implication = IMPL.the(ccc, zDT, forward ? before : next);
            if (implication instanceof Bool || implication.complexity() > volMax)
                return false;

            if (range == Long.MAX_VALUE)
                range = 0; //all eternal

            long finalStart = start, finalEnd = start + range;

            Truth T = tAccum.dither(nar);
            if (T == null)
                return false;

            NALTask z = NALTask.task(implication, BELIEF, T,
                    finalStart, finalEnd, Stamp.zip(STAMP_CAPACITY, pathTasks::stamp, null /* ()->{
                        throw new TODO();
                    }*/, n));

            Util.normalize(xFactor);
            pathTasks.fund(z, x->xFactor[x]);

            if (in == null) in = new Lst<>(4);
            in.add(z);

            logger.
                trace
                //info
                ("{} ", z);


            return true; //continue
        }

        private static boolean links(Term x, Term y) {
            return
                x.equals(y)
                //||
                //(Conj.eventOf(y, x))
                ;
        }

        private static final Logger logger = Log.log(ImpilerDeduction.class);

    }
}