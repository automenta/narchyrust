package nars.game.meta;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import jcog.*;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.Lst;
import jcog.math.FloatDifference;
import jcog.math.FloatMeanEwma;
import jcog.math.FloatMeanWindow;
import jcog.math.FloatSupplier;
import jcog.math.normalize.FloatNormalized;
import jcog.pri.bag.impl.ArrayBag;
import jcog.signal.FloatRange;
import nars.*;
import nars.control.DefaultBudget;
import nars.control.Emotion;
import nars.focus.BagForget;
import nars.focus.BasicTimeFocus;
import nars.game.FocusLoop;
import nars.game.Game;
import nars.game.GameTime;
import nars.game.Rewards;
import nars.game.action.AbstractAction;
import nars.game.action.AbstractGoalAction;
import nars.game.reward.LambdaScalarReward;
import nars.game.reward.MultiReward;
import nars.game.reward.Reward;
import nars.game.reward.ScalarReward;
import nars.game.sensor.AbstractSensor;
import nars.game.sensor.SignalComponent;
import nars.game.sensor.VectorSensor;
import nars.memory.HijackMemory;
import nars.task.util.Eternalization;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.time.ScheduledTask;
import nars.time.clock.RealTime;
import nars.truth.util.ConfRange;
import org.HdrHistogram.Histogram;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static java.lang.Math.pow;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static jcog.Fuzzy.polarize;
import static jcog.Util.*;
import static jcog.math.LongInterval.TIMELESS;
import static nars.$.inh;
import static nars.$.p;
import static nars.Op.*;
import static nars.TruthFunctions.c2e;
import static nars.TruthFunctions.e2c;

/**
 * core metavisor
 */
public abstract class SelfMetaGame extends MetaGame {

    private static final boolean METAGAME_TIME_DEPENDENT = true;

    @Deprecated final int subMetaFrameDivisor;

    private final Term SELF;
    public List<SubMetaGame> game;

    private final boolean subMetaRewardMode = false;

    protected SelfMetaGame(NAR nar, float selfMetaDur, int subMetaDurDivisor) {
        super(p(nar.self(), meta), GameTime.durs(selfMetaDur));
        this.subMetaFrameDivisor = subMetaDurDivisor;
        SELF = nar.self();
    }

//    static final Logger logger = Log.logger(SelfMetaGame.class);
//
//    private void load() {
//        byte[] prev = User.the().blob(SelfMetaGame.class.getName());
//        if (prev != null) {
//            try {
//                int numRead = IO.readTasks(prev, what());
//                logger.info("{} read {} tasks ({} bytes)", this, numRead, prev.length);
//            } catch (IOException e) {
//                logger.error("{}", e);
//            }
//        }
//    }
//
//    public void save() {
//
//        DynBytes b = new DynBytes(1024 * 128);
//        what().concepts()
//                .flatMap(Concept::tasks)
//                .filter(Objects::nonNull)
//                .map(x -> NALTask.eternalize(x, nar))
//                .filter(Objects::nonNull)
//                .distinct()
//                .map(IO::taskToBytes)
//                .forEach(b::write);
//
//        User.the().put(SelfMetaGame.class.getName(), b.compact());
//        logger.info("{} saved tasks ({} bytes)", this, b.length);
//    }

    @Override
    protected final void init() {
        metagames();

        super.init();

        initMeta();


        //TODO make this better configurable
        game.forEach(subMeta -> rewards(
            subMetaRewardMode ?
                inh(subMeta.id, happy)
                :
                inh(subMeta.id, SETe.the(happy,dex)),
            subMetaRewardMode ?
                subMeta.game //game happiness only (direct)
                :
                subMeta, //game happiness + submeta dex
        false));

        nar.runLater(this::initMetaEnd);
    }

    @Deprecated protected void initMetaEnd() {
    }

    protected abstract void initMeta();

    protected abstract void initMeta(SubMetaGame subMetaGame);

    protected void eternalization(float min, float max) {
        floatAction(inh(id, "eternalize"), (FloatProcedure) e ->
                ((Eternalization.Flat)nar.eternalization).ete.setLerp(e, min, max));
    }

    @Deprecated protected void fairDeriverShallow() {
        final Histogram h = new Histogram(2);

//        var s = floatAction(inh(nar().self(), "shallow"), (FloatProcedure) e -> {
//            float period = lerpSafe(e, 11, 2);
//            //System.out.println("shallow=" + period);
//            h.recordValue((int)period);
//            FairDeriver.shallow.set(1f/period);
//        });

        afterFrame(() -> {
            if (rng().nextBooleanFast16(0.004f)) {
                System.out.print(Str.histogramString(h, true));
            }
        });
    }



    /**
     * TODO this needs a fix in GameTime.Durs because MetaGame could set the dur longer than the game's dur, and never react to changing curiosity
     */
    @Deprecated
    public void durMeta(float octaves, float subOctaves) {
        FloatMeanEwma target = new FloatMeanEwma(0.3f);
        durOctaves(SELF, ()->(float) (nar.dur() * pow(2, -subOctaves)), octaves + subOctaves, d -> nar.time.dur((float) target.acceptAndGetMean(d)));
    }

    protected void complexMax(int min, int max) {
        action(inh(SELF, "volMax"), (FloatProcedure) r ->
                nar.complexMax.set(lerpInt(r, min, max)));
    }
//
//    private void governor(MLPGovernor governor) {
//        actionUnipolar($.inh(SELF, $.p("exe", "exploration")), (float r) ->
//                governor.explorationRate.setLerp(r, 0.01f, 1f));
//
//    }


    private void memoryControl(HijackMemory m) {
        //TODO refine
        //TODO refine
        floatAction(inh(SELF, "memTemp"), m.add::set);
    }

    @Research
    @Is("Energy_proportional_computing")
    public void cpuThrottle(float min, float max) {
        floatAction(inh(SELF, effort), min, max, 1, nar.cpuThrottle::set);

//TODO feedback -> powersave mode
//        ThreadCPUTimeTracker.getCPUTime()
//        reward("lazy", 1, ()->{
//            return 1-nar.loop.throttle.floatValue();
//        });
    }


    public void metagames() {
        List<Game> x = nar.parts(Game.class).filter(g -> !(g instanceof MetaGame)).toList();
//        if (x.size() == 1) {
//            //.. ??
//        }
        this.game = x.stream().map(g -> {
            //logger.info(this -)
            SubMetaGame gMeta = new SubMetaGame(g,
                METAGAME_TIME_DEPENDENT ?
                    GameTime.frames(g, subMetaFrameDivisor)  :
                    GameTime.durs(subMetaFrameDivisor)
            );
            nar.add(gMeta);

            focusPri(g, 0.01f, 1);
            focusPri(gMeta, 0.01f, 1);

            return gMeta;
        }).collect(toList());
    }

    private void focusPri(Game g, float min, int max) {
        floatAction(inh(g.id, freq), min, max, 1, g.focus()::
                //freqAndPri
                freq
        );
    }


//    private void rewardsAggregate() {
//        reward($.inh(happy, SELF), () -> {pri
//            return (float) happiness(nowPercept.s, nowPercept.e, nowPercept.dur, nar);
//            //return (float)happinessSigma(nowPercept.s, nowPercept.e, nowPercept.dur, nar);
//        });
//
//        // /*$.p(SELF, now)*/
//
//        //
////		/** past happiness memory */
////		sense($.inh($.p(SELF, past), happy), () -> {
////			float durProjected = dur();
////			return happiness(Math.round(nowPercept.start - dur() * emotionalMomentumDurs), nowPercept.start, durProjected, nar);
////		});
////
////		/** optimism */
////		reward($.inh(SELF, optimistic), () -> {
////			float dur = nowLoop.dur * 2;
////			float shift = 1 * dur;
////			return happiness(Math.round(nowLoop.start + shift), Math.round(nowLoop.end + shift), dur, nar);
////		});//.conf(0.5f*nar.confDefault(GOAL));;
//
//    }

    private void cpu() {

    }

    public void heapSense() {

        Runtime rt = Runtime.getRuntime();

        FloatSupplier freeHeap = () -> (float) (((double) rt.freeMemory()) / rt.totalMemory());
        sense(inh(SELF, p("free", "heap")), freeHeap);
        sense(inh(SELF, p("free", "heapDelta")), normalizedDelta(freeHeap));
    }

    public void emotionSense() {


        Emotion e = nar.emotion;

        List.of(
//                sense(inh(SELF, "derived"), normalizedDelta(e.derivedTask)),
//                sense(inh(SELF, "premised"), normalizedDelta(e.derivedPremise)),
//        sense($.inh(SELF,$.p( "derive", "task", "drop")), deltaNormalized(e.derivedTaskDrop));
//        sense($.inh(SELF,$.p( "derive", "task", "dup")), deltaNormalized(e.derivedTaskDup));

                sense(inh(SELF, "conceptNew"),
                        normalizedDelta(e.conceptNew)
                ),
//        sense($.inh(SELF, "unceptualize"), deltaNormalized(e.conceptDelete)),

                sense(inh(SELF, "busy"), new FloatNormalized(e.busyVol)),
                sense(inh(SELF, p(lag, "nar")), new FloatNormalized(e.narLoopLag.abs())),
                sense(inh(SELF, p(lag, "loop")), new FloatNormalized(e.durLoopLag))
                //sense(inh(SELF, p("loop", "slow")), new FloatNormalized(e.durLoopSlow)),
        );
          //      .forEach(s -> s.resolution(resSense));
    }

//    public void metaGoals(CreditControlModel model) {
//        for (MetaGoal mg : MetaGoal.values()) {
//            actionUnipolar($.inh(SELF, $.the(mg.name())), x -> {
//                model.want(mg,
//
//                        //x
//                        (x * 2) - 1
//
////					x >= 0.5f ?
////						(float) Util.lerp(Math.pow((x - 0.5f) * 2, 1 /* 2 */), 0, +1) //positive (0.5..1)
////						:
////						//Util.lerp((x) * 2, -0.02f, 0) //negative (0..0.5): weaker
////						0f
//                );
//            });
//        }
//    }

    private FloatNormalized normalizedDelta(FloatSupplier in) {
        return new FloatNormalized(new FloatDifference(in, nar::time)).polar();
    }

    private void confRes(Term label, ConfRange c, float min, float max) {
        floatAction(inh(SELF, p(conf, label)), x ->
            c.set(round(lerpSafe(x, min, max), nar.confRes.floatValue())));
    }

    public void confMin(double confMinMin, double confMinMax) {
        var f = lerpLog(confMinMin, confMinMax);
        floatAction(inh(SELF, "ignore"), x -> {
            //double cMin = lerpSafe(x, confMinMin, confMinMax);
            double cMin = f.valueOf(x);
            //System.out.println(cMin);
            nar.confMin.set(cMin);
        });
    }

    public void conf(Term id, ConfRange c, float confMin, float confMax) {
        floatAction(inh(SELF, p(id, conf)), x -> c.set(lerpSafe(x, confMin, confMax)));
    }

    /** sets throttle from total demand freq of the subgames */
    protected void autoThrottle() {
        var momentum = 0.5f;
        afterFrame(()->{
            var max = game.stream()
                    .flatMap(z -> Stream.of(z, z.game)) //all subgames and their meta's
                    .mapToDouble(g -> g.focus().freq())
                    .max()
                    //.average()
                    .orElse(Double.NaN);
            //TODO + meta freq?

            if (max!=max)
                return;

            double demand = max;

            double minThrottle = 0.01;
            nar.cpuThrottle.setMomentum((float)Math.max(minThrottle, demand), momentum);
        });
    }

//    private void confMin(double min, double max) {
//        actionUnipolar($.inh($.p(conf, "min"), SELF), x -> {
//            nar.confMin.set(Util.round(lerpSafe(x, min, max), nar.confResolution.floatValue()));
//        });
//    }

    protected void truthPrecision(boolean freq, boolean conf) {
        assert (freq || conf);

        floatAction(inh(SELF, precise), x -> {
            float b, fRes, cRes;
            //double cMin;
            switch (x) {
                case float v when v >= 0.8f -> {
                    fRes = 0.0125f;
                    cRes = 0.01f;
                    //cMin = NAL.truth.CONF_MIN;
                    b = 1f;
                }
                case float v when v >= 0.6f -> {
                    fRes = 0.025f;
                    cRes = 0.01f;
                    //cMin = 0.005;
                    b = 0.75f;
                }
                case float v when v >= 0.4f -> {
                    fRes = 0.05f;
                    cRes = 0.01f;
                    //cMin = 0.01;
                    b = 0.50f;
                }
                case float v when v >= 0.2f -> {
                    fRes = 0.1f;
                    cRes = 0.02f;
                    //cMin = 0.02;
                    b = 0.25f;
                }
                default -> {
                    fRes = 0.2f;
                    cRes = 0.04f;
                    //cMin = 0.02;
                    b = 0;
                }
            }
            if (freq)
                nar.freqRes.set(fRes);
            if (conf)
                nar.confRes.set(cRes);

//            return b; //TODO
        });
    }

//    private void confPrecision() {
//        actionUnipolar($.inh(SELF, ignorant), (i) -> {
//            nar.confMin.conf(Util.lerp(i, NAL.truth.CONF_MIN, nar.beliefConfDefault.get() / 8f));
//        });
//    }

//    private DoubleStream happinesses(long start, long end, float dur, NAR nar) {
//        return nar.parts(MetaGame.class)
//                .filter(g -> g != SelfMetaGame.this)
//                .filter(Part::isOn)
//                .mapToDouble(g -> g.happiness(start, end, dur));
//    }

//	@NotNull
//	private Stream<What> others() {
//		return nar.what.stream()
//			.map(What.Much::the)
//			.filter(w -> w != this.what());
//	}


//    float happiness(long start, long end, float dur, NAR nar) {
//        //System.out.println(Joiner.on(", ").join(happinesses(start, end, dur, nar).iterator()));
//        return (float) happinesses(start, end, dur, nar)
//                .filter(x -> x == x) //not NaN
//                /*.map(gg ->
//                    (((gg - 0.5f) * 2f
//                        * g.what().pri() //weighted by current priority
//                    ) / 2f) + 0.5f
//                )*/
//                .average().orElse(Float.NaN);
//    }
//
//    private DoubleStream happinessesFinite(long start, long end, float dur, NAR nar) {
//        return happinesses(start, end, dur, nar).filter(x -> x == x);
//    }
//
//    public double happinessAverage(long start, long end, float dur, NAR nar) {
//        return happinessesFinite(start, end, dur, nar).average().orElse(Double.NaN);
//    }
//
//    public double happinessSigma(long start, long end, float dur, NAR nar) {
//        return happinessesFinite(start, end, dur, nar).reduce(1, (x, y) -> x * y);
//    }

    /**
     * TODO prevent duplicate add
     */
    protected LambdaScalarReward encouragement(FloatRange enc) {
        LambdaScalarReward e = reward(inh(SELF, "encouragement"), () -> {
            float v = enc.asFloat();
            if (Util.equals(v, 0.5f, nar.freqRes.floatValue()))
                return Float.NaN; //neutral

            enc.setLerp(0.01f, v, 0.5f); //fade to 0.5
            return v;
        });

//        Exe.runLater(() -> { //HACK
//            e.usually(0.5f);
//        });

        return e;
    }

//    public void exeControl(Focus f, boolean puncProb) {
//            if (f.budget instanceof DefaultBudget) {
//
//            }
////        actionUnipolar($.inh(exe.SELF, "subCycles"), (i) ->{
////            //long subCycleNS = Math.round(Util.lerpSafe(i, 0.05, 0.45) * exe.nar.loop.periodNS());
////            //System.out.println(Texts.timeStr(subCycleNS));
////            //exe.subCycleNS = subCycleNS;
////            exe.subCycles = lerpSafe(i, 2, 8);
////        });
//
////        Term SELF = exe.nar.self();
//
////        if (exe.deriveParam!=null)
////            bagDeriver(SELF, exe.deriveParam);
//
//
////        float gainMin = 0.5f, gainMax = 1.5f;
//
////        workerUnipolar($.inh($.p(SELF,"derive"), "detail"), (e, v) -> {
////            ((DefaultDerivePri) (e.deriver.derivePri)).complexityAbsCost.setProportionally(1-v);
////        });
////        workerUnipolar($.inh($.p(SELF,"derive"), "detailRel"), (e, v) -> {
////            ((DefaultDerivePri) (e.deriver.derivePri)).complexityRelCost.setProportionally(1-v);
////        });
//
//
//    }

//    private void bagDeriver(Term x, BagDeriver.BagDeriverParam p) {
//
//        /* TODO iterations limits needs auto-tuned for system's CPU speed */
//        floatAction(inh(p("derive", x), "depth"),
//                (float v) -> p.iterations.set(Math.round(lerpSafe(v, 64, 256))));
//
//        floatAction(inh(p("derive", x), "breadth"),
//                (float v) -> p.tasklink.set(1f / Util.lerpSafe(v, 16, 4)));
//
////        actionUnipolar($.inh(DERIVE, "bounce"),
////                (float v) -> p.bounce.setLerp(v, 0.0f, 0.9f));
//
////        //TODO DISABLE automatically when output = Direct
////        actionUnipolar($.inh(DERIVE, "output"),
////                (float v) -> p.out.setLerp(v, 0.01f, 1));
//
//
//    }

//    private void simplicity(Term DERIVE, DefaultDerivePri derivePri) {
//        //        actionUnipolar($.inh("certainty", SELF_DERIVE), (v) -> {
////            //pri.simplicity.setProportionally(v);
////            pri.eviLossCost.set(lerpSafe(v, 0.1f, 1.0f));
////        });
//
////        int volMaxOriginal = nar.volMax();
//        actionUnipolar($.inh(DERIVE, "simple"), v -> {
//            //nar.volMax.setLerp(v, volMaxOriginal, 24);
//
//            derivePri.simplicity.setLerp(v);
//            //derivePri.complexityCost.setLerp(v);
//
//            //derivePri.complexityRelCost.set(0);
////            derivePri.complexityAbsCost.set(lerpSafe(v, 0f, 0.5f));
//
//        });
//    }

//    private void overderive(Term DERIVE, DefaultDerivePri derivePri, float minGain, float maxGain) {
//        /** "overderive": afterburner distortion pedal */
//        actionUnipolar($.inh(DERIVE, "gain"), v->{
//            derivePri.nalPri.set(lerpSafe(v, minGain, maxGain));
//        });
//    }
//
//    private void workerUnipolar(Term id, ObjectFloatProcedure<WorkerExec.WorkPlayLoop> ww) {
//        WorkerExec e = (WorkerExec) nar.exe;
//        actionUnipolar(id, (rate) -> {
//            e.loops.forEach(w -> ww.value(w, rate));
//        });
//    }


    public class SubMetaGame extends MetaGame {

        /** long-term ambition TODO describe better */
        private static final int DEX_CONTRACTION_ITERS =
            //2;
            //4;
            16;
            //256;

        private static final boolean HAPPINESS_NORMALIZED = false;

        /**
         * the game that this controls
         */
        public final Game game;

        protected static final Logger logger = Log.log(SubMetaGame.class);

        public SubMetaGame(Game g, GameTime timing) {
            super(p(g.id, meta), timing);
            this.game = g;
        }

        public void actionRewardQuestion(Game G) {
            G.afterFrame(()->{
                var rng = G.nar().random();
                FocusLoop _s = G.sensors.sensors.get(rng);
                Term s;
                if (_s instanceof VectorSensor)
                    s = ((Termed)new Lst<>(_s.components()).get(rng)).term();
                else
                    s = _s.term();

                Reward _r = G.rewards.rewards.get(rng);

                if (rng.nextBoolean())
                    s = s.neg();

                Term r = _r.term(); //TODO goal term
                Term q = CONJ.the(XTERNAL, s, r);

                long[] startend = G.focus().when(); long start = startend[0], end = startend[1];
                final byte punc =
                        //QUESTION;
                        QUEST;
                Task p = NALTask.task(q, punc, null, start, end, nar.evidence());
                p.pri(0.25f);
//                        System.out.println(p);
                G.focus().accept(p);
            });
        }

        @Override
        protected final void init() {

            super.init();

            initMeta(this);
        }

        /** @param adjustPri whether to adjust reward belief and goal priorities */
        @Research
        public void autoReward(boolean adjustConf, boolean adjustPri) {
            assert (adjustConf || adjustPri);

            Rewards gameRewards = game.rewards;
            var n = gameRewards.size();
            /** steadfastness */
            float minStrength =
                //1f/n;
                0.01f;
                //0.001f;
                //Math.max(0.01f, 0.1f/n);
                //0.5f/n;

            if (minStrength == 1)
                return;

            Term pred = strength;
//                    priBelief && conf ? MetaGame.strength :
//                    (priBelief ? MetaGame.pri : MetaGame.conf);

            for (Reward r : gameRewards) {

                float initialStrength = r.strength();

                if (!(initialStrength > minStrength * 2))
                    continue; //too weak, dont control

                priActionLinear(inh(r.id, pred), s -> {
                    if (s!=s) return;

                    if (adjustConf)
                        r.conf(lerpSafe(s,
                            minStrength, initialStrength /* use initial strength as max */
                        ));

                    if (adjustPri) {
                        /* scales reward priority */
                        //TODO optimal curve
                        //TODO factor out to Reward.amp(float a)
                        float p =
                            expUnitSafe(s);
                            //s;

                        if (r instanceof ScalarReward X) {
                            X.amp(p);
                        } else if (r instanceof MultiReward X) {
                            X.amp(p);
                        } else {
                            throw new TODO();
                        }
                    }

                }, 0, 1);
            }
        }

        @Deprecated
        private void pausing0() {
            float playThresh = 0.25f;
            actionPushButton(inh(game.id, play), new BooleanProcedure() {

                private final AtomicInteger autoResumeID = new AtomicInteger();
                private volatile ScheduledTask autoResume;
                private volatile Runnable resume;

                @Override
                public synchronized void value(boolean e) {
                    //enableAction = n.actionToggle($.func(enable, n.id), (e)->{
                    //TODO integrate and threshold, pause for limited time
                    if (e) {
                        tryResume();
                    } else {
                        tryPause();
                    }
                }

                void tryPause() {

                    if (resume == null) {

                        resume = game.pause();
                        NAR n = nar();

                        int a = autoResumeID.get();

                        final long autoResumePeriod = 256;

                        autoResume = n.runAt(Math.round(n.time() + autoResumePeriod * n.dur()), () -> {
                            if (autoResumeID.get() == a)
                                tryResume();
                            //else this one has been cancelled
                        });

                    }
                }

                void tryResume() {

                    if (resume != null) {
                        autoResumeID.getAndIncrement();
                        resume.run();
                        autoResume = null;
                        resume = null;
                    }

                }
            }, () -> playThresh);

        }

        public AbstractAction pausing() {
            final int MAX_SLEEP_DURS = 80, MIN_WAKE_DURS = 20;
            return actionPushButton(inh(game.id, on), new BooleanPredicate() {

                long mustWakeAt = Long.MIN_VALUE;
                long maySleepAt = Long.MIN_VALUE;

                {
                    nar.onDur(() -> {
                        if (mustWakeAt > Long.MIN_VALUE) {
                            long now = nar.time();
                            if (now >= mustWakeAt) {
                                mustWakeAt = Long.MIN_VALUE;
                                maySleepAt = now + Math.round(game.dur() * MIN_WAKE_DURS);
                                game.enable(true); //force restart
                            }
                        }
                    });
                }

                @Override
                public boolean accept(boolean on) {
                    if (on) {
                        mustWakeAt = Long.MIN_VALUE;
                    } else {
                        long now = nar.time();
                        if (now < maySleepAt)
                            on = true;//force wake
                        else {
                            if (mustWakeAt == Long.MIN_VALUE) {
                                //start sleep
                                mustWakeAt = now + Math.round(game.dur() * MAX_SLEEP_DURS);
                            } else {
                                //continue sleep
                            }
                        }
                    }

                    game.enable(on);

                    return on;
                }
            });
            //pauser.goalDefault($.t(1, 0.0001f), nar);
            //        Reward enableReward = reward("enable", () -> enabled.getOpaque() ? +1 : 0f);
        }

//        /** not necessary if the game rewards are the metagame's rewards */
//        private void senseGameRewards() {
//            //sense game's rewards individually
//            if (game.rewards.size() > 1) {
//                game.rewards.forEach(r -> {
//                    if (r instanceof ScalarReward)
//                        sense(r.id, () -> ((ScalarReward) r).reward);
//                });
//            }
//        }

//        public void focusShare(Focus from, float pct) {
//            //                //this.what().accept(new EternalTask($.inh(aid,this.id), BELIEF, $.t(1f, 0.9f), nar));
//            focusShare(from, focus(), pct);
//        }
//
//        private void focusShare(Focus from, Focus to, float pct) {
//            onFrame(() -> {
//                int links = (int) Math.ceil(pct * ((BagFocus) to).capacity());
//                from.sample(random(), links, t -> {
//                    to.link(((AtomicTaskLink) t).clone());
//                });
//            });
//        }

//        public void timeFocus0(Term id, BasicTimeFocus focus, FloatSupplier baseDur, float octaves, float range) {
//            durOctaves(id, baseDur, octaves, focus::dur);
//
//            floatAction(inh(id, "shift"), x ->
//                focus.shift.setLerp(x, -range, +range)
//            );
//
////            if (focus instanceof FuzzyTimeFocus) {
////                floatAction(inh(id, "whenever"), x -> {
////                    ((FuzzyTimeFocus) focus).jitter.setLerp(x, 0, 1);
////                });
////            }
//        }

        public void focusSharp(float min, float max) {
            floatAction(inh(game.id, "sharp"), (float s) -> {
                float ss = lerpSafe(s, min, max);
                ((ArrayBag) game.focus().attn._bag).sharp = ss;
            });
        }

        public void timeFocus(Term id, BasicTimeFocus t, FloatSupplier durSys, float durMax, float durShift, float updateRate) {
            if (durMax >= durShift) throw new UnsupportedOperationException("durShift >> durMax");
            if (durMax<=1 && durShift <=0) throw new UnsupportedOperationException();
            if (updateRate > 1) throw new UnsupportedOperationException();

            FloatRange dur = t.dur, shift = t.shift;
            float[] durDelta = {0}, shiftDelta = {0};

            if (durMax > 1)
                durControl(inh(id, MetaGame.dur), d -> durDelta[0] = (d - dur.asFloat()) * updateRate, durSys, durMax).freqRes(resAction);

            if (durShift > 0)
                shiftControl(inh(id, MetaGame.shift), s -> shiftDelta[0] = (s - shift.asFloat()) * updateRate, durMax, durShift).freqRes(resAction);

            //smooth transition to the target time at a rate (ex: inversely proportional to the game frames per meta frame)
            game.onFrame(()->{
                dur.add(durDelta[0]);
                shift.add(shiftDelta[0]);
            });
        }

        public AbstractGoalAction shiftControl(Term id, FloatConsumer shiftSet, float durMax, float shiftDurs) {
            var a = floatAction(id, x -> {
                //x = (float) Math.pow(x, 3); //cubic curve
                double shift = lerp(x, -shiftDurs, +shiftDurs);
                shiftSet.accept((float)shift);
            });

            nar.runLater(()-> nar.runLater(() ->
                logger.info("{} durShift=+-{}\n{}", game.id, shiftDurs, timeControlAnalysis(durMax, shiftDurs, game.dur())
            )));

            return a;
        }

        private String timeControlAnalysis(float durMax, float shiftDurs, float durGame) {
            float durSys = nar.dur();
            float confDefault = nar.confDefault(BELIEF);
            return DoubleStream.of(1, durMax/2, durMax, shiftDurs).mapToObj(t -> {
                    var eFactor = NAL.evi.project.project((long) (t * durGame), durSys);
                    var confDecayed = e2c(c2e(confDefault) * eFactor);
                    return "@" + t +
                            "durs:\teviFactor=" + Str.n(eFactor, 6) +
                            "\tconfFactor: " + Str.n2(confDefault) + "% ->\t" + Str.n(confDecayed,8) + "%\t" +
                            (confDecayed > nar.confMin.floatValue() ? "" : "UNDERFLOW");
                })
                .collect(joining("\n"));
        }


        public AbstractGoalAction durControl(Term id, FloatProcedure durSet, FloatSupplier durSys, float durMax) {
            assert (durMax > 1);

            //FloatToFloatFunction f = lerpLog(1, durMax);
            FloatToFloatFunction f = x -> x;

            nar.runLater(()->{
//                var durMin = dur(durSys, 0, base, octaves);
//                var durMax = dur(durSys, 1, base, octaves);
                double durMin = durSys.getAsDouble();

                String durMinStr, durMaxStr;
                String units;
                if (nar.time instanceof RealTime rt) {
                    units = "";
                    durMinStr = rt.unitsToTimeString(durMin);
                    durMaxStr = rt.unitsToTimeString(durMax * durMin);
                } else {
                    units = "cycles";
                    durMinStr = Str.n4(durMin);
                    durMaxStr = Str.n4(durMax * durMin);
                }

                logger.info("{} dur={}..{}, {}",
                        game.id,
                        //base, octaves,
                        durMinStr, durMaxStr,
                        units);
            });

            return floatAction(id, x ->
                //durSetter.value((float) dur(durSys, x, base, octaves))
                    {
                        var d = durSys.asFloat();
                        durSet.value(lerp(f.valueOf(x), d, durMax * d));
                    }
            );
        }

//        private double dur(FloatSupplier durSys, float x, float base, float octaves) {
//            return durSys.getAsDouble() * pow(base, x * octaves);
//        }

//        private void memoryPreAmp(GraphBagFocus w, boolean punc, boolean op) {
//            PuncPri byPunc = punc ? puncPri(game.id, new PuncPri()) : null;
//            OpPri byOp = op ? opPri(game.id, new OpPri()) : null;
//
//            FloatRange amp = new FloatRange(1, 0.1f, 1);
////            actionUnipolar($.inh(game.id, "amp"), (float a) -> amp.setLerp(Util.sqr(a)));
//
//            w.priTask = t -> (float) (
//                amp.doubleValue() *
//                t.priElseZero() *
//                (byPunc!=null ? byPunc.floatValueOf(t) : 1) *
//                (byOp!=null ? byOp.floatValueOf(t) : 1)
//            );
//        }


        public void focusClearHysterical(Focus w) {
            float minDurations = 2;
            actionPushButton(inh(game.id, clear),
                    debounce(w::clear, minDurations));
        }

        public void focusClearStochastic(Focus w, float periodDursMin, float periodDursMax) {
            action(inh(game.id, clear), x -> {
                if (rng().nextBoolean(1f/lerpSafe(x, periodDursMax, periodDursMin))) {
                    w.clear();
                    return 1;
                }
                return 0;
            });
        }
        public void focusClearPWM(Focus w, float periodDursMin, float periodDursMax) {
            action(inh(game.id, clear), new FloatToFloatFunction() {

                long prev = TIMELESS; //start

                @Override
                public float valueOf(float rate) {
                    if (rate!=rate) return 0; //N/A

                    long now = game.time(); //w.now();
                    if (prev==TIMELESS)
                        prev = now;

                    rate = polarize(rate);
                    if (rate > 0) {

                        float dur = game.dur();
                        float periodMax = dur * periodDursMax;
                        float periodMin = dur * periodDursMin;

                        float period = lerp(rate, periodMax, periodMin);

                        boolean clearing = now - prev >= period;
                        if (clearing) {
                            w.clear();
                            prev = now;
                            return 1;
                        }
                    }
                    return 0;
                }
            });

        }

        /** onMin < onMax, offMin > offMax */
        public void curiosity(Game g, float onMin, float onMax, float offMin, float offMax) {
            if (onMin >= 1 || onMax >= 1 || onMin <= 0 || onMax <= 0 || onMin > onMax)
                throw new WTF();
            if (offMin >= 1 || offMax >= 1 || offMin <= 0 || offMax <= 0 || offMin < offMax)
                throw new WTF();
            floatAction(inh(g.id, CURIOSITY /*p(CURIOSITY, "on")*/), x -> {
                g.curiosity.curiosityRateOn.set(lerpSafe(x, onMin, onMax));
                g.curiosity.curiosityRateOff.set(lerpSafe(x, offMin, offMax));
            });
        }

//        /** controls curiosity duty cycle (period once enabled) */
//        public void curiosityDuty(Game g, float dutyMin, float dutyMax) {
//            floatAction(inh(g.id, p(CURIOSITY, "duty")), x ->
//                g.curiosity.curiosityDutyCycle.set(lerpSafe(x, dutyMin, dutyMax))
//            );
//        }

//        public void focusGrow(BagFocus w, float min, float max) {
//            actionUnipolar($.inh(game.id, grow), (float x) -> {
//                ((BasicActivator) w.activator).feedback.set(lerpSafe(x, min, max));
//            });
//        }

        public void simplicity(DefaultBudget B, float min, float max) {
            floatAction(inh(game.id, simple), x ->
                B.simple.setLerp(x, min, max));
        }
//        public void volTolerance(DefaultBudget B, float max) {
//            floatAction(inh(game.id, simple),
//                x->B.volTolerance.setLerp(x, max, 1)
//            );
//        }
//        public void volIdeal(DefaultBudget B, float min, float max) {
//            floatAction(inh(game.id, simple), x ->
//                B.volIdeal.setLerp(x, min, max));
//        }

    public AbstractGoalAction linkPri(Atomic t, FloatRange r, float min, float max) {
        return floatAction(inh(game.id, p(link, t)), x ->
                r.setLerp(x, min, max));
    }

//        public void focusAmp(BagFocus w, float min, float max) {
//            actionUnipolar(inh(game.id, amp), (float x) -> {
//                ((BasicActivator) w.budget).amp.set(lerpSafe(x, min, max));
//            });
//        }

        public void focusSustainBasic(Focus w, float min, float max) {
            if (!(w.updater instanceof BagForget))
                w.updater = new BagForget(); //HACK

            floatAction(inh(game.id, forget), min, max, 1,
                x -> ((BagForget) w.updater).forget.set(x));
        }

//        private void memoryControlPrecise(BagFocus w, boolean base, boolean byPunc, boolean byOp) {
//
//            BagFocus.BagSustainByOp u = new BagFocus.BagSustainByOp();
//            w.updater = u;
//
//            if (base)
//                focusSustainBasic(w, -1, +1);
//
//            if (byPunc) {
//                priAction(punc(BELIEF), u.belief::set);
//                priAction(punc(GOAL), u.goal::set);
//                priAction(punc(QUESTION), u.question::set);
//                priAction(punc(QUEST), u.quest::set);
//            }
//            if (byOp) {
//                /* Op.ATOM.atom conflicts with BELIEF */
//                //TODO fix the labeling:
//                actFloat(inh(MetaGame.pri, p(OP, the("atom"))), u.atom);
//                actFloat(inh(MetaGame.pri, p(OP, INH.atom)), u.inh);
//                actFloat(inh(MetaGame.pri, p(OP, SIM.atom)), u.sim);
//                actFloat(inh(MetaGame.pri, p(OP, IMPL.atom)), u.impl);
//                actFloat(inh(MetaGame.pri, p(OP, CONJ.atom)), u.conj);
//                actFloat(inh(MetaGame.pri, p(OP, PROD.atom)), u.prod);
//            }
////                if (byCmpl) {
////                    floatAction($.inh(gid, $.p(forget, "simple")), u.simple);
////                    floatAction($.inh(gid, $.p(forget, "complex")), u.complex);
////                }
//
//
//            //		WhatThe.BagDecayByVolume updater = new WhatThe.BagDecayByVolume();
//            //		((WhatThe) w).updater = updater;
//            //		floatAction($.inh(gid, forget), updater.decayRate.subRange(0.5f, 1.5f));
//            //		floatAction($.inh(gid, Atomic.the("forgetCmpl")),  -1 /* dead zone half */, +1, 1, updater.volumeFactor::set);
//
//
//        }

        public <Y> void senseSimple(Term id, Iterable<Y> src, FloatFunction<Y> compl) {
            sense(id, () -> {
                double vSum = 0;
                int n = 0;
                for (Y t : src) {
                    vSum += compl.floatValueOf(t);
                    n++;
                }

                if (n == 0) {
                    return 0.5f; //Float.NaN;
                } else {
                    double mean = vSum / n;
                    double meanNormalized = Math.min(1, mean / nar.complexMax.intValue());
                    return 1 - (float)meanNormalized;
                }
            });
        }

        public void sensePunc(Focus f) {
            //var linkTemplate = INH.the(f.id, p(link, $.varDep(1)));
            var taskPriTemplate = INH.the(f.id, p(pri, $.varDep(1)));

//            var linkSensor = new PuncVectorSensor.LinkPuncSensor(linkTemplate, f);
//            sensors.addSensor(linkSensor);
////            sense(linkTemplate.replace($.varDep(1), mean), () -> (float) linkSensor.mean);

            var taskSensor = new PuncVectorSensor.TaskBagPuncPriSensor(taskPriTemplate, f);
            sensors.addSensor(taskSensor);
//            sense(taskTemplate.replace($.varDep(1), mean), () -> (float) taskSensor.mean);



            var bag = ((Focus.TaskBagAttn)f.attn).bag;

            //MEAN BELIFE/GOAL CONF:
            var beliefConf = INH.the(f.id, p(conf, punc(BELIEF)));
            var goalConf = INH.the(f.id, p(conf, punc(GOAL)));
            sensors.addSensor(new VectorSensor(beliefConf, goalConf) {
                final SignalComponent[] c = new SignalComponent[2];

                float beliefConfMean, goalConfMean;
                {
                    c[0] = component(beliefConf, () -> beliefConfMean, nar);
                    c[1] = component(goalConf,   () -> goalConfMean,   nar);
                }

                @Override
                public void accept(Game g) {
                    float bc = 0, gc = 0, bn = 0, gn = 0;
                    for (var x : bag) {
                        var t = x.id;
                        switch (t.punc()) {
                            case '.' -> { bc += (float) t.conf(); bn++; }
                            case '!' -> { gc += (float) t.conf(); gn++; }
                        }
                    }
                    beliefConfMean = bn > 0 ? bc / bn : 0;
                    goalConfMean = gn > 0 ? gc / gn : 0;
                    super.accept(g);
                }

                @Override
                public Iterator<SignalComponent> iterator() {
                    return ArrayIterator.iterate(c);
                }
            });
        }

        public LambdaScalarReward dexReward() {
            /* aggregate dexterity through submeta's dur (which may be larger than game dur). otherwise this is like a pointwise sample */
            FloatSupplier dex = () -> (float) game.dexterity();

            //FloatMeanEwma dexMean = new FloatMeanEwma();
            final FloatMeanWindow[] dexMean = {new FloatMeanWindow(1)};

            game.afterFrame(() -> {
                int durRatio = Math.max(1, (int) Math.ceil(SubMetaGame.this.dur() / game.dur()));

                if (dexMean[0].capacity() != durRatio)
                    dexMean[0] = new FloatMeanWindow(durRatio);

                dexMean[0].acceptAndGetMean(dex.asFloat());
            });

            var dexNorm = new FloatNormalized(()-> (float) dexMean[0].mean())
                    .period(0, DEX_CONTRACTION_ITERS)
                    .minLimit(0, 0)
                    .range(0, NAL.truth.CONF_MIN);

            //TODO this needs to be sampled each game frame, esp when the self runs at a lower duration it will not capture all the dex
            var r = reward(inh(game.id, MetaGame.dex), dexNorm);
            r.resolution(resReward);
            //r.goalFocus = ScalarReward.GoalFocus.Eternal;
            return r;
        }



        /** global reward priority control */
        public void priRewards() { priAction(game.rewards.pri); }

        /** global action priority control */
        public void priActions() {
            priAction(game.actions.pri);
        }

        @Deprecated public void priSensors() {
            priAction(game.sensors.pri);
        }

        public void priSensors(boolean individualSensorPri, boolean vectorSensorPri, boolean vectorSensorRate) {

            for (FocusLoop s : game.sensors.sensors) {
                if (s instanceof VectorSensor v) {
                    if (vectorSensorPri)
                        priAction(v.pri);

                    if (vectorSensorRate && v.size() > 16) {
                        throw new UnsupportedOperationException();
//                        FloatProcedure updater;
//                        if (v.model instanceof DirectVectorSensorAttention dvsa)
//                            updater = r -> dvsa.activateRate.setLerp(r, 0, 1);
//                        else if (v.model instanceof QueueVectorSensorAttention qvsa)
//                            updater = r -> qvsa.activateRate.setLerp(r, 0, 1);
//                        else if (v.model instanceof AdaptiveVectorSensorAttention qasa)
//                            updater = r -> qasa.activateRate.setLerp(r, 0, 1);
//                        else
//                            updater = null;
//
//                        if (updater!=null)
//                            floatAction(inh(v.id, "rate"), updater);
                    }

                } else if (s instanceof AbstractSensor ss) {
                    if (individualSensorPri)
                        priAction(ss.pri);
                } else
                    throw new UnsupportedOperationException();
            }

        }

    public void certain(DefaultBudget B, float min, float max) {
            floatAction(inh(game.id, "certain"),
                z -> B.certain.setLerp(z, min, max));
            //TODO conviction control
        }


//        public void amp(DefaultBudget B, float min, float max) {
//            floatAction($.inh(game.id, "amp"),
//                    z -> B.amp.setLerp(z, min, max));
//        }

//        public void rewardConf(float rConfMin, float rConfMax) {
//            if (game.rewards.size() > 1) {
//                game.rewards.forEach(r -> {
//                    floatAction(inh(r.id, "conf"), (c) -> {
//                        ((ScalarReward) r).goalTruth.conf(lerpSafe(c, rConfMin, rConfMax));
//                    }).resolution(0.1f);
//                });
//            }
//        }
    }


}