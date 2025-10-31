package nars;

import jcog.TODO;
import jcog.Util;
import jcog.event.ByteTopic;
import jcog.event.Off;
import jcog.event.Topic;
import jcog.memoize.MemoGraph;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.Prioritizable;
import jcog.pri.UnitPri;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.op.PriMerge;
import jcog.random.RandomBits;
import jcog.signal.FloatRange;
import jcog.signal.IntRange;
import jcog.util.ConsumerX;
import nars.action.link.index.BagAdjacentTerms;
import nars.concept.TaskConcept;
import nars.control.Budget;
import nars.control.DefaultBudget;
import nars.eval.Evaluator;
import nars.eval.TaskEvaluation;
import nars.focus.BagForget;
import nars.focus.BasicTimeFocus;
import nars.focus.PriSource;
import nars.focus.TimeFocus;
import nars.focus.util.TaskAttention;
import nars.focus.util.TaskBagAttentionSampler;
import nars.io.DirectTaskInput;
import nars.io.TaskInput;
import nars.link.MutableTaskLink;
import nars.link.TermLinkBag;
import nars.task.CommandTask;
import nars.task.util.PuncBag;
import nars.term.Termed;
import nars.time.Tense;
import nars.util.NARPart;
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import static nars.NAL.TASKBAG_PUNC_OR_SHARED;
import static nars.Op.TIMELESS;
import static nars.term.Functor.isFunc;

public class Focus extends NARPart implements Externalizable, ConsumerX<Task> {



    public final ByteTopic<Task> eventTask = new ByteTopic<>(Op.Punctuation);

    public final PriSource pri;
    public final UnitPri freq = new UnitPri(0.5f);
    /**
     * local metadata
     */
    public final Map meta = new ConcurrentHashMap<>();
    private final FloatRange commitDurs = new FloatRange(1, 0.5f, 16);

    /**
     * ensure at most only one commit is in progress at any time
     */
    private final AtomicBoolean busy = new AtomicBoolean(false);

    public Function<Bag, Consumer<Prioritizable>> updater =
        new BagForget();
        //new BagSustainByVariables();

    /**
     * accepts incoming tasks
     * buffering allows deduplicating same results from different threads without otherwise having to input all into belief tables directly
     */
    public TaskInput input =
        new DirectTaskInput() //low latency, low throughput
        //new MapTaskInput() //provides some dedupe
        //new BagTaskInput(1, 1/3f) //WARNING bag seems to become inconsistent for unknown reason
    ;

    public final Consumer<NALTask> ACTIVATE = this::activate;
    public final BiConsumer<NALTask,TaskConcept> REMEMBER = this::_remember;

    public TimeFocus time = new BasicTimeFocus();
    public Budget budget = new DefaultBudget();

    public transient float durSys = 1;

    private int complexMax = Integer.MAX_VALUE;

    /** timing */
    private volatile long prev, now, commitNext, novelBefore;

    public final Attn attn;

    public Focus(Term id, int capacity) {
        this(id);
        attn.capacity(capacity);
    }


    public Focus(Term id) {
        super(id);
        this.pri = new PriSource(this.id, 1);

        this.attn =
            new TaskBagAttn(this);
            //new ConceptTaskSource();

        input.start(this);
    }

    @Nullable public static TermLinkBag termBag(Termed t, Focus f) {
        var concept = f.nar.concept(t);
        return concept == null ? null : concept.meta(id(f), () -> new TermLinkBag(NAL.premise.TERMLINK_CAP));
    }

    @Nullable public TaskAttention taskBag(Termed t, Focus f, int capacity) {
        var concept = f.nar.concept(t);
        var a = concept == null ? null : concept.meta(idTask(f), this::taskBag);
        if (a!=null) {
            if (capacity >= 0)
                a.setCapacity(capacity);
        }
        return a;
    }

    private TaskAttention taskBag() {
        return new TaskAttention(TASKBAG_PUNC_OR_SHARED, this);
    }

    /** Scope
     *    true: Global (shared by all Focus's),
     *    false: per Focus, Focus-local */
    public static final boolean global = false;
    private static final String idGlobalTerm = BagAdjacentTerms.class.getSimpleName();
    private static final String idGlobalTask = TaskAttention.class.getSimpleName();

    private static String id(Focus f) {
        return global ? idGlobalTerm : f.key(BagAdjacentTerms.class, idGlobalTerm, f.id);
    }
    private static String idTask(Focus f) {
        return global ? idGlobalTask : f.key(TaskAttention.class, idGlobalTask, f.id);
    }

    @Override
    protected void starting(NAR nar) {

        durSys = nar.dur();

        if (time.dur() <= 0) time.dur(durSys); //HACK for dur=0

        commitNext = nar.time() - 1; //force start ASAP
        super.starting(nar);

        nar.pri.add(pri);
    }

    public void clear() {
        attn.clear();
    }

    /** concepts in links */
    public Stream<Concept> concepts() {
        return attn.concepts();
    }

    /** terms in links */
    public final Stream<Term> terms(Predicate<Term> filter) {
        return concepts()
            //.unordered()
            .map(x -> x.term)
            .filter(filter);
    }

    @Override
    public final void writeExternal(ObjectOutput objectOutput) {
        throw new TODO();
    }

    @Override
    public final void readExternal(ObjectInput objectInput) {
        throw new TODO();
    }

    @Override
    protected void stopping(NAR nar) {
        nar.pri.remove(pri);
        super.stopping(nar);
    }

    /**
     * perceptual duration (cycles)
     */
    public final float dur() {
        return time.dur();
    }

    public void log(Task t) {
        eventTask.emit(t, t.punc());
        nar.emotion.busyVol.add(t.term().complexity());
    }

    @Deprecated
    public final RandomGenerator random() {
        return nar.random();
    }

    public final void remember(NALTask x) {
        input.accept(x);
    }
    private void _remember(NALTask x, TaskConcept c) {
        input.accept(x, c);
    }

    private void run(CommandTask t) {
        if (!isFunc(t.term()))
            throw new UnsupportedOperationException();

        new CommandEvaluation(t);
    }

    public final Off onTask(Consumer<Task> listener, byte... punctuations) {
        return eventTask.on(listener, punctuations);
    }

    public final boolean commit(long now) {
        if (now >= this.commitNext) {
            if (Util.enterAlone(busy)) {
                try {
                    _commit(now);
                    return true;
                } finally {
                    Util.exitAlone(busy);
                }
            }
        }
        return false;
    }

    private void _commit(long now) {
        commitTime(now);

        input.commit();

        attn.commit();
    }


    private void commitTime(long now) {
        this.prev = this.now;
        this.now = now;
        this.durSys = nar.dur();

        var durCommit = Math.max(1, durSys * commitDurs.floatValue());
        this.commitNext = now + Math.round(durCommit);

        float durNovel =
            durCommit;
            //durSys;
            //dur();
        long cycNovel = Math.max(1, Tense.occToDT(Math.round(durNovel * NAL.belief.NOVEL_DURS)));
        novelBefore = now - cycNovel;
    }

    public Off log() {
        return log((Predicate) null);
    }

    public Off log(@Nullable Predicate<?> pred) {
        return logTo(System.out, pred);
    }

    public Off logTo(Appendable out, @Nullable Predicate<?> includeValue) {
        return logTo(out, null, includeValue);
    }

    private Off logTo(Appendable out, @Nullable Predicate<String> includeKey, @Nullable Predicate includeValue) {
        return eventTask.on(new TaskLogger(includeValue, out, includeKey));
    }

    //@Override
    public final float pri() {
        return pri.pri();
    }

    public final float freq() {
        return freq./*pri*/asFloat();
    }

    public final Focus pri(float freq, float pri) {
        freq(freq);
        this.pri.pri(pri);
        return this;
    }

    public final Focus freq(float f) {
        freq.pri(f);
        return this;
    }

    @Nullable
    public <X> X local(Object key) {
        return (X) meta.get(key);
    }

    public <K, X> X local(K key, X value) {
        var v2 = meta.put(key, value);
        return (X)(v2 != null ? v2 : value);
    }

    public <K, X> X local(K key, Function<K, ? extends X> build) {
        return (X) meta.computeIfAbsent(key, build);
    }

    public <C> Stream<C> localInstances(Class<? extends C> valueClass) {
        return meta.entrySet().stream()
                .map(i -> ((Map.Entry)i).getValue())
                .filter(valueClass::isInstance);
    }

    /**
     * novelty filter for: activation, logging, etc.
     * determines whether a previously-stored task is novel, and can be re-activated now
     *
     * @return TIMELESS if it should not be reactivated, or the current time to set its
     * re-creation time as if it should be.
     */
    public boolean novelTime(NALTask x) {
        long creation;
        if (NAL.TASK_ACTIVATE_ALWAYS || ((creation = x.creation()) == TIMELESS || creation <= novelBefore)) {
            x.setCreation(time()); //initialize or re-activate
            return true;
        } else
            return false;
    }

    @Override
    public final void accept(Task x) {
        if (x instanceof NALTask X)
            remember(X); //rememberNow(X);
        else if (x instanceof CommandTask C)
            run(C);
        else
            throw new UnsupportedOperationException();
    }

    public final void acceptAll(Task... t) {
        for (Task x : t) accept(x);
    }

    public final void acceptAll(Stream<? extends Task> t) {
        t.forEach(this);
    }

//    /**
//     * where threads can store threadlocal
//     */
//    private final ThreadLocal<Map> threadLocal = ThreadLocal.withInitial(HashMap::new);
//
//    public final <X> X threadLocal(Object key) {
//        return (X) threadLocal.get().get(key);
//    }
//
//    public final <X> X threadLocal(Object key, Function<Focus, X> ifMissing) {
//        //return (X) threadLocal.get().computeIfAbsent(key, k -> ifMissing.apply(this));
//
//        var m = threadLocal.get();
//        Object v = m.get(key);
//        if (v == null)
//            m.put(key, v = ifMissing.apply(this));
//        return (X) v;
//    }

    public final Focus dur(float dur) {
        time.dur(dur);
        return this;
    }

    public final long[] when() {
        return time.when(this);
    }


    public int complexMax() {
        return Math.min(complexMax, nar.complexMax());
    }

    /** time since last update, in durs */
    public final float updateDT() {
        return (now - prev)/durSys;
    }


    /** time of the latest update */
    public final long time() {
        return now;
    }

    /** current attention priority of the focus within the running system */
    public final float focusPri() {
        PLink<Focus> f = nar.focus.get(id);
        return f!=null ? f.pri() : Float.NaN;
    }

    /** utility class for caching a shared key constructed from 2 components: prefix and id */
    public String key(Object key, String prefix, Object id) {
        String k = local(key);
        return k != null ? k : local(key, prefix + "," + id);
    }

    /** sets a specific max volume for this Focus.  volMax(Integer.MAX_VALUE) to disable */
    public final void complexMax(int v) {
        complexMax = v;
    }

    public Off onTask(String key, Consumer<Task> each, byte... punc) {
        Off on;
        if ((on = (Off) meta.get(key)) == null) {
            meta.put(key, on = onTask(each, punc));
        }
        return on;
    }

    /** TODO just accept: Term,Term,float parameters; don't involve TaskLink's */
    public boolean link(@Deprecated MutableTaskLink l) {
        var bag = termBag(l.from(), this);
        return bag != null && bag.put(new PLink<>(l.to(), l.priElseZero())) != null;
    }

    /**
     * TODO histogram priority to adaptively determine ANSI color by percentile
     */
    static class TaskLogger implements Consumer<Task> {

        private final @Nullable Predicate includeValue;
        private final Appendable out;
        String previous;

        TaskLogger(@Nullable Predicate includeValue, Appendable out, @Nullable Predicate<String> includeKey) {
            this.includeValue = includeValue;
            this.out = out;
            previous = null;
            Topic.all(this, (k, v) -> {
                if (includeValue == null || includeValue.test(v)) {
                    outputEvent(out, previous, k, v);
                    previous = k;
                }
            }, includeKey);
        }

        private static void outputEvent(Appendable out, String previou, String chan, Object v) {

            try {

                if (!chan.equals(previou)) {
                    out.append(chan).append(": ");

                } else {

                    int n = chan.length() + 2;
                    for (int i = 0; i < n; i++)
                        out.append(' ');
                }

                if (v instanceof Object[] va) {
                    v = Arrays.toString(va);
                } else if (v instanceof Task tv) {
                    v = tv.toString(true);
//                    float tvp = tv.priElseZero();
//                    v = ansi()
//                            .a(tvp >= 0.25f ?
//                                    Ansi.Attribute.INTENSITY_BOLD :
//                                    Ansi.Attribute.INTENSITY_FAINT)
//                            .a(tvp > 0.75f ? Ansi.Attribute.NEGATIVE_ON : Ansi.Attribute.NEGATIVE_OFF)
//                            .fg(budgetSummaryColor(tv))
//                            .a(
//                                    v
//                            )
//                            .reset()
//                            .toString();
                }

                out.append(v.toString()).append('\n');
            } catch (IOException e) {
                logger.error("outputEvent", e);
            }

        }

//        static Ansi.Color budgetSummaryColor(Prioritized tv) {
//            return switch ((int) (tv.priElseZero() * 5)) {
//                default -> Ansi.Color.DEFAULT;
//                case 1 -> Ansi.Color.MAGENTA;
//                case 2 -> Ansi.Color.GREEN;
//                case 3 -> Ansi.Color.YELLOW;
//                case 4 -> Ansi.Color.RED;
//            };
//        }

        @Override
        public void accept(Task v) {
            if (includeValue == null || includeValue.test(v)) {
                outputEvent(out, previous, "task", v);
                previous = "task";
            }
        }


    }

    private class CommandEvaluation extends TaskEvaluation<Task> {

        private final CommandTask task;

        CommandEvaluation(CommandTask t) {
            super(new Evaluator(nar::axioms));
            this.task = t;
            eval(t.term());
        }

        @Override
        public CommandTask task() {
            return task;
        }

        @Override
        public NAR nar() {
            return nar;
        }

        @Override
        public void accept(Task x) {
            Focus.this.accept(x);
        }

        @Override
        public boolean test(Term term) {
            return false; //first only
        }
    }

//    @Override
//    public void remember(NALTask x, Premise premise) {
//        super.remember(x, premise);
//
//        float reflect = budget.deriveTaskReflect.floatValue();
//        if (reflect > 0 && premise instanceof NALPremise n) {
//            float p = x.priElseZero();
//            float pReflect = p * reflect;
//            if (pReflect > Prioritized.EPSILON) {
//                byte xPunc = x.punc();
//                if (premise instanceof NALPremise.DoubleTaskPremise d) {
//                    //activate(d.from(), xPunc, pReflect/2);
//                    //activate(d.to(), xPunc, pReflect/2);
//                    activate(d.from(), xPunc, pReflect/2);
//                } else {
//                    activate(n.from(), xPunc, pReflect);
//                }
//            }
//        }
//    }

    public final BiFunction<MemoGraph, NALTask, TaskConcept> PRE_REMEMBER =
        (G, _x) ->
                G.share(_x, (GG, __x) ->
                        GG.share(__x.term().concept(), this::conceptualize));


    private TaskConcept conceptualize(Termed x) {
        return nar.conceptualizeTask(x);
    }

    public void activate(NALTask t) {
        if (novelTime(t)) {
            attn.activate(t);
            log(t);
        }
    }

    public abstract class Attn {
        /** Bag storing PriReference<Concept> or PriReference<NALTask> */
        public final Bag<? extends Termed, PriReference<? extends Termed>> _bag;

        protected Attn(Bag bag) {
            _bag = bag;
        }

        public abstract boolean activate(NALTask t);
        public abstract void sample(TaskBagAttentionSampler sampler, int iter, PuncBag punc, RandomBits rng);
        public abstract Stream<Concept> concepts();

        public final void clear() {
            _bag.clear();
        }

        public final void capacity(int capacity) {
            _bag.setCapacity(capacity);
        }

        public final void commit() {
            _bag.commit(updater.apply(_bag));
        }

        public final int capacity() {
            return _bag.capacity();
        }

        public final boolean isEmpty() {
            return _bag.isEmpty();
        }


    }

    private class ConceptAttn extends Attn {
        /** capacity */
        final static int tasksPerConcept = 64;

        public final Bag<Term, PLink<Concept>> concepts;

        /**
         * capacity of the links bag
         */
        public final IntRange conceptCapacity = new IntRange(0, 0, 2048) {
            @Override protected void changed(int next) {
                concepts.setCapacity(next);
            }
        };

        ConceptAttn() {
            super(this.concepts = /*new BufferedBag<>*/(new ArrayBag<>(PriMerge.plus) {
                @Override public Term key(PLink<Concept> value) {
                    return value.id.term;
                }
            }));
            //this.concepts = new BufferedBag<>(concepts);
        }

        @Override
        public Stream<Concept> concepts() {
            return concepts.stream().map(x -> x.id);
        }


        @Override public final boolean activate(NALTask t) {
            return activateConcept(t, budget.priIn(t, Focus.this));
        }

        public boolean activateConcept(Termed t, float pri) {
            var c = nar.conceptualize(t);
            if (c!=null) {
                if (t instanceof NALTask tt) {
                    var tb = taskBag(c, Focus.this, tasksPerConcept);
                    if (tb != null)
                        tb.accept(tt);
                }
                return concepts.put(new PLink<>(c, pri))!=null;
            }
            return false;
        }

        @Override public void sample(TaskBagAttentionSampler sampler, int iter, PuncBag punc, RandomBits rng) {
            for (int i = 0; i < iter; i++)
                sampleTask(sampleConcept(rng), punc, rng, sampler);
        }

        @Nullable public Concept sampleConcept(RandomGenerator rng) {
            var c = concepts.sample(rng);
            return c!=null ? c.id : null;
        }

        private void sampleTask(@Nullable Concept c, PuncBag punc, RandomBits rng, TaskBagAttentionSampler sampler) {
            if (c != null) {
                var tb = taskBag(c, Focus.this, -1);
                if (tb != null) {
                    tb.commit();
                    tb.seed(sampler, 1, punc, rng);
                }
            }
        }

    }

    public class TaskBagAttn extends Attn {
        public final TaskAttention bag;

        protected TaskBagAttn(Focus f) {
            super(/*new BufferedBag<>*/((Bag)(bag = new TaskAttention(false, f) {
//                @Override
//                protected float pri(NALTask t) {
//                    return activationRate(t);
//                }
            }).model));
        }

        @Override
        public boolean activate(NALTask t) {
            bag.accept(t);
            return true;
        }

        @Override
        public Stream<Concept> concepts() {
            return terms(/*Term::CONCEPTUALIZABLE*/s -> true)
                    .map(nar::concept)
                    .filter(Objects::nonNull);
        }

        public final Stream<Term> terms(Predicate<Term> filter) {
            return bag.stream()
                    .unordered()
                    .map(x -> x.id.term())
                    .distinct()
                    .filter(filter);
        }

        @Override
        public void sample(TaskBagAttentionSampler sampler, int iter, PuncBag punc, RandomBits rng) {
            bag.seed(sampler, iter, punc, rng);
        }


    }



}