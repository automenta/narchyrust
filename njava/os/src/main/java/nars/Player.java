package nars;

import jcog.Log;
import jcog.TODO;
import jcog.Util;
import jcog.exe.Loop;
import jcog.io.Serials;
import jcog.lab.Experiment;
import jcog.lab.NumberSensor;
import jcog.signal.FloatRange;
import jcog.tensor.Agents;
import nars.action.decompose.DecomposeTerm;
import nars.action.link.ClusterInduct;
import nars.action.link.STMLinker;
import nars.action.link.TermLinking;
import nars.action.transform.*;
import nars.control.DefaultBudget;
import nars.control.DeriverExec;
import nars.deriver.impl.TaskBagDeriver;
import nars.deriver.reaction.Reactions;
import nars.focus.BasicTimeFocus;
import nars.focus.time.ActionTiming;
import nars.func.Factorize;
import nars.game.Game;
import nars.game.action.AbstractGoalAction;
import nars.game.action.CompoundAction;
import nars.game.action.util.ActionMomentum;
import nars.game.action.util.Reflex;
import nars.game.action.util.Reflex0;
import nars.game.meta.MetaGame;
import nars.game.meta.SelfMetaGame;
import nars.game.reward.Reward;
import nars.game.sensor.ScalarSensor;
import nars.game.sensor.SignalConcept;
import nars.game.sensor.VectorSensor;
import nars.game.util.Conjyer;
import nars.game.util.Implyer;
import nars.gui.NARui;
import nars.gui.ReflexUI;
import nars.io.ConfFilter;
import nars.memory.CaffeineMemory;
import nars.memory.HijackMemory;
import nars.memory.Memory;
import nars.memory.TierMemory;
import nars.premise.NALPremise;
import nars.task.util.Eternalization;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.time.Time;
import nars.time.clock.RealTime;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.slf4j.Logger;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.BitmapLabel;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.Math.round;
import static java.util.Collections.EMPTY_LIST;
import static nars.$.inh;
import static nars.NAL.truth.FREQ_RES_DEFAULT;
import static nars.Op.*;
import static nars.deriver.reaction.MutableReaction.PremiseTask;
import static spacegraph.SpaceGraph.window;

/**
 * builder for NAR's that play games
 */
public class Player implements AutoCloseable {

    public boolean ADVANCED = false;

    public boolean selfMetaReflex = ADVANCED;
    public boolean subMetaReflex;
    //    public boolean subMetaReflexMerged = false;

    public boolean gameReflex;


    public boolean premiseAuto = selfMetaReflex;


    /** higher-order logic (HOL) functions */
    public final boolean HOL = false;

    public float metaMomentum =
            //0.9f
            0 //disabled
    ;

    private static final Logger logger = Log.log(Player.class);

    /**
     * constructed by calling start()
     */
    public NAR nar;

    private Loop loop;

    public double ramGB =
            Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024.0);

    public int threads = jcog.Config.INT("THREADS", Util.concurrencyExcept(1));

    /**
     * fundamental system framerate
     * TODO auto-determine by estimating from maximum required FPS of the game durations'
     */
    private final float fps = 50;


    boolean beliefConfControl, goalConfControl;


    /**
     * thread affinity, can improve latency
     */
    public boolean exeAffinity = threads > 3;

    /**
     * holds procedures for the adding of Game's to the NAR.
     *  TODO make this process fully dynamic so that games can be added and removed freely
     */
    @Deprecated
    public Consumer<NAR> ready = n -> {
    };

    public boolean meta = jcog.Config.IS("meta", true);

    public boolean inperienceGoals = true;
    public boolean inperienceBeliefs;
    public boolean inperienceQuestions;

    /**
     * global simplicity  control
     * warning: may interfere with other simplicity controls
     */
    private final boolean complexMaxControl = false;

    private final boolean durMeta = false;

    /**
     * default SelfMetaGame Focus freq
     */
    public static float selfFreqDefault = 0.02f;

    private final boolean selfFreq = false;

    public boolean gamePri = true;

    public boolean autoReward = true;
    boolean autoRewardConf = true, autoRewardPri = true;

    public boolean motivation;
    public boolean xor;

//    public boolean answerQuestionsFromTaskLinks;

//    public boolean goalSpread = Config.IS("goalspread", false);

    public boolean actionRewardQuestion;

    public boolean clusterBelief = true;
    public boolean clusterGoal = true;
    public boolean temporalInduction = !clusterBelief;
    public boolean stmLinker = !clusterBelief;

    /**
     * clustering output ratio, proportional to temporal 'connectedness'
     */
    public float clusterRate = jcog.Config.FLOAT("cluster_rate",
        0.5f //restricted
        //1 //BALANCED
        //1.5f
        //Util.PHIf
        //2
        //0.5f
    );

    public int clusterCap =
        //64
        160
        //32
        //48
        //256
        //384
        //512
        //128
        //96
        //64
    ;

    public boolean arith = true;

    public boolean explode = HOL;
    public boolean factorize = HOL;
    public boolean abbreviation = HOL;
    public boolean ifThenElse; //TODO replace with a Java-impl Reaction


    public boolean goalInduction;

    /**
     * TODO split into separate 'dur' and 'shift' flags
     */
    boolean timeFocus =
        true;
        //false;

    /**
     * temporal perceptual bandwidth/horizon, in multiples of base dur
     */
    public float durMax = jcog.Config.FLOAT("durMax",
            32
            //16
            //48
            //64
            //128
            //8
            //4
            //1
            //15
            //32
            //64
            //8
            //96
            //128
            //256
            //512
            //1 //DISABLED
            //2
            //4
            //64
            //512
            //1024
            //6
            //4.5f
            //3
            //7
            //3.5f
            //2
            //1
            //0
            //3
            //8
            //10
            //16
    );

    /**
     * temporal horizon period through which focus can scan. >=0, 0 disables
     */
    public float durShift = jcog.Config.FLOAT("durShift",
            16 * durMax
            //32 * durMax
            //8 * durMax
            //4 * durMax
            //2 * durMax
            //25 * durMax
            //50 * durMax
    );

//    static final boolean answerLogging = true;

    public boolean commentary;

    public boolean encourager;


    /**
     * ability for a submeta to pause its game
     */
    public boolean pausing;

    public boolean uiTop = true;
    public boolean uiStats;

//    public float focusSamplingExponent =
//            ArrayBag.SHARP_DEFAULT
//            //4
//            //1
//            //2
//            //3
//    ;

    public boolean cpuThrottle = true; //selfMetaReflex;

    /**
     * -1 for auto-size to heap (-Xmx)
     */
    public double conceptsMax = -1;

    public float beliefConf = jcog.Config.FLOAT("beliefconf",
            0.9f
            //0.98f
            //0.95f
            //0.75f
            //Util.PHI_min_1f
            //0.5f
    );
    public float goalConf = jcog.Config.FLOAT("goalconf", beliefConf);

    public boolean eternalizationControl =
        selfMetaReflex;
        //false;

    public boolean answerDepthBeliefGoalControl =
        selfMetaReflex;

    public float eternalization = jcog.Config.FLOAT("eternalization",
            //1/100f
            //1/40f
            1/20f
            //1/10f
            //1/8f
            //1/5f
            //1/2f
            //0 //DISABLED
    );

    int complexMin = 12;

    public int complexMax = jcog.Config.INT("complexity",
            //32
            28
            //22
            //26
            //40
            //64
            //40
            //80
            //90
            //19
            //22
            //28
            //36
    );

    /**
     * in milliseconds
     */
    public int timeRes = jcog.Config.INT("dt",
            4      //250hz
            //5    //200hz
            //10   //100hz
            //20   //50hz
            //8    //125hz
            //40   //25hz
    );

    public float freqRes = jcog.Config.FLOAT("df",
            FREQ_RES_DEFAULT
            //0.02f
            //0.005f
            //NAL.truth.TRUTH_EPSILON //highest precision
    );

    /**
     * initial min confidence
     * TODO auto-calculate based on durMax,durShift, etc ?
     */
    public double confMin =
        //1E-8;
        1E-7;
        //1E-9;
        //1E-6;
        //5E-5;
        //4E-4;
        //2E-5;
        //1E-6;
        //1E-4;
        //1E-6;
        //5E-4;
        //1E-3;
        //5E-6;
        //5E-6;
        //2E-5;
        //NAL.truth.CONF_MIN; //minimum

    public double confRes = jcog.Config.DOUBLE("dc",
        confMin
        //1E-3
        //1E-4
        //4E-4
        //CONF_RES_DEFAULT //coarse
        //NAL.truth.FREQ_EPSILON //high precision
        //NAL.truth.CONF_MIN //highest
        //Util.mean(NAL.truth.TRUTH_EPSILON, 0.01f) //mid
    );

    public boolean confMinControl;
    public double confMinMax =
        1E-3;
        //Util.min(CONF_MAX, confMin * 10);

    public boolean confFilter;

    public boolean varIntro = true;


    public boolean nalProcedural = jcog.Config.IS("PROCEDURAL", true);

    public boolean nalStructural = jcog.Config.IS("STRUCTURAL", true);

    /** untested, prolly not entirely working */
    public boolean nalDiff = jcog.Config.IS("DIFF", false);

    public boolean nalAnalogy = jcog.Config.IS("ANALOGY", nalStructural /*HOL*/);

    public boolean nalSets;

    public boolean nalDelta = jcog.Config.IS("DELTA", true);
    public boolean deltaGoal; //nalDelta;


    public static final int hijackMemory_Reprobes = 8;

    private static final boolean printDeriver = true;

    enum FocusForgetting {
        Forget, ForgetOnly, Balanced, BalancedSoft
    }

    public FocusForgetting focusForget = null; //!ADVANCED ? null : FocusForgetting.Forget;
        //FocusForgetting.BalancedSoft;
        //FocusForgetting.Balanced;
        //FocusForgetting.ForgetOnly;

    public boolean focusClear
            //advanced;
            ;
        //true;
        //focusForget == FocusForgetting.Off;

    public boolean simplicityControl = ADVANCED;
    public boolean certaintyControl = ADVANCED;

    public boolean derivePri = true; //ADVANCED;
    public boolean derivePriSimple = false;

    public boolean opPri = ADVANCED;

    public boolean inputBagPri = false;

    public boolean puncSeed = false, puncSeedBasic = true;

    public float ampMin =
        0.01f
        //0.1f
        //0.01f
        //0.25f
        //0.05f
        //0.02f
        //0.01f
        //0.01f
        ;


    public boolean
        curiosityControl,
        perceptionControl = true,

        implBeliefify = true,
        implGoalify = implBeliefify,

        implyerGame = true,
        implyer ,
        implyerAggressive,

        conjyer,

        focusSharp;

    /**
     * special complexMax for metagames; set to 0 to disable
     */
    private final int complexMaxMeta =
        18;
        //15;
        //14;
        //12;
        //11;
        //9;
        //0; //disabled

    /**
     * how many system durs in self-meta dur
     */
    int selfMetaDurs =
        //2;
        //8;
        //3;
        4;
        //1;

    /**
     * NOTE this specifies the 'game frame divisor', not durs.
     */
    int subMetaDurs = selfMetaDurs;


    public int focusConcepts = 512;

    float metaScale = 1/2f;
    int metaFocusConcepts = (int)(focusConcepts * metaScale);
    int subMetaFocusConcepts = metaFocusConcepts;

    public Player() {
    }

    public Player(Game... partsOrGames) {
        this(Stream.of(partsOrGames));
    }

    public Player(Collection<Game> parts) {
        this();
        add(parts);
    }

    public Player(Stream<Game> parts) {
        this();
        add(parts);
    }

    @Deprecated
    public Player(float fps, Consumer<NAR> ready) {
        this();
        ready(ready);
        fps(fps);
    }

    public synchronized Player ready(Consumer<NAR> r) {
        if (this.nar != null)
            r.accept(nar);
        else
            this.ready = this.ready.andThen(r);
        return this;
    }

    public Player add(Game... p) {
        return add(Stream.of(p));
    }


    public Player add(Collection<Game> p) {
        return add(p.stream().parallel());
    }

    private final CopyOnWriteArrayList<Game> games = new CopyOnWriteArrayList<>();

    public Player add(Stream<Game> p) {
        p.forEach(games::add);
        return this;
    }


//    public void control(ControlModel m) {
//
//        this.control = m;
//
////        if (!meta) {
////            n.emotion.want(MetaGoal.Question, -0.005f);
////            n.emotion.want(MetaGoal.Believe, 0.01f);
////            n.emotion.want(MetaGoal.Goal, 0.05f);
////        }
//    }

//    public void controlCreditAnalysis() {
//        control(new CreditAnalysisModel());
//    }
//
//    public void control(CreditControlModel.Governor g) {
//        control(new CreditControlModel(g));
//    }
//
//    public void controlMLP() {
//        control(new MLPGovernor());
//    }
//
//    public void controlLerp() {
//        control(new LERPGovernor());
//    }

    public final synchronized Player start() {
        init();

        loop = nar.startFPS(fps);

        //nar.time.dur(loop.periodMS()*sysDurFactor);

        var tracePath = jcog.Config.get("traceGames", null);
        if (tracePath != null)
            traceGames(tracePath, true, true, true, 1);

        return this;
    }

    /**
     * traces game numerics to TSV file
     */
    protected void traceGames(String path, boolean gameHappiness, boolean metagameSensors, boolean metagameActions, float durPeriod) {
        logger.warn("trace {}", path);
        FileOutputStream f;
        try {
            f = new FileOutputStream(path);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        var e = new Experiment<>(Player.this, new Experiment.CSV(f, '\t'))
                    .sense(NumberSensor.of("time", (Player x) -> x.nar.time()))
                    .sense(NumberSensor.of("concepts", (Player x) -> x.nar.memory.size()));

        //e.sense(NumberSensor.of("derivedTask", (Player x)-> x.nar.emotion.derivedTask.getAsDouble()));
        //e.sense(NumberSensor.of("busy", (Player x)-> x.nar.emotion.busyVol.getAsDouble()));

        //game (& metagame) rewards
        if (gameHappiness)
            gamesAndMetaGames().forEach(g ->
                e.sense(NumberSensor.of(g.id + "_happy", (Player x) -> g.happiness())));

        //metagame sensors & actions
        nar.parts(MetaGame.class).forEach(mg -> {
            if (metagameSensors) {
                mg.sensors.stream().forEach(gs -> {
                    if (gs instanceof ScalarSensor ss)
                        experimentSensor(e, mg, ss.concept);
                    else if (gs instanceof VectorSensor vs) {
                        vs.forEach(vgs -> experimentSensor(e, mg, vgs));
                    } else {
                        //TODO
                    }
                });
            }

            if (metagameActions) {
                mg.actions.forEach(ga -> {
                    if (ga instanceof AbstractGoalAction aga)
                        e.sense(NumberSensor.of(mg.id + "_" + ga.term(),
                                () -> aga.goalTruth.freq()));
                    else {
                        //TODO
                        throw new TODO();
                    }
                });
            }
        });

        nar.onDur(() -> e.record(this), durPeriod);
    }

    private static void experimentSensor(Experiment<Player> e, MetaGame g, SignalConcept s) {
        //TODO ss.concept.beliefs().truth(...)?
        e.sense(NumberSensor.of(g.id + "_" + s.term(), s::freq));
    }

//    @Deprecated public synchronized Player runCycles(int cycles) {
//        init();
//
//        nar.run(cycles);
//
//        return this;
//    }

    public synchronized void init() {
        ensureStopped();
        if (nar == null) __init();
    }

    private void ensureStopped() {
        if (loop != null) throw new RuntimeException("already running");
    }

    public synchronized Player stop() {
        //nar.synch();
//        nar.runLater(()->{
        gamesAndMetaGames().forEach(nar::remove); //HACK
//            nar.stop();
//        });
        return this;
    }


    private void __init() {
        var n = this.nar = nar();

        n.timeRes.set(timeRes);

        n.complexMax.set(complexMax);

        n.freqRes.set(freqRes);
        n.confRes.set(confRes);

        n.beliefConfDefault.set(beliefConf);
        n.goalConfDefault.set(goalConf);

        initEternalization(n);

        n.confMin.set(confMin);


        games.forEach(n::add); //ADD GAMES

        if (meta)
            n.add(meta());

        if (commentary) {
            throw new TODO();
            //foreach game focus: n.add(new Commentary(focus));
        }

        if (confFilter) {
            gamesAndMetaGames().forEach(g ->
                g.focus().input = new ConfFilter(g.focus().input));
        }


        //n.runLater(() ->
        n.parts(Game.class).forEach(g -> {
            if (complexMaxMeta > 0 && g instanceof MetaGame)
                g.focus().complexMax(complexMaxMeta);
//            ((ArrayBag) ((BagFocus) g.focus()).bag).sharp = focusSamplingExponent;
        });

        var summaryPrint = true;
        if (summaryPrint) {
            n.runLater(() ->
                    gamesAndMetaGames().forEach(g -> {
                        System.out.println(g.id);
                        System.out.println("vocabulary:\t" + g.vocabulary);
                        System.out.println("   complex:\t" + g.vocabularyComplexityStatistics());
                        System.out.println();
                    })
            );
        }

        if (gameReflex)
            games().forEach(g -> reflex(g));

        if (uiStats) {
            n.runLater(() -> {
                var stats = new BitmapLabel(String.valueOf((char) 0x00D8));
                window(stats, 400, 600);
                var s = new StringBuilder(16 * 1024);
                Loop.of(() -> {
                    //stats.viewMin(stats.view().w, stats.view().h);
                    s.setLength(0);

                    n.stats(false, true, s);

                    stats.text(s.toString()); //TODO avoid toString() and read from CharSequence directly
                }).fps(0.25f);
            });
        }

        if (uiTop) {
            n.runLater(() -> {
                window(NARui.top(n), 1024, 800);


//                n.runLater(() -> {
//                    List<SubPart> rr = n.subPartStream().filter(pp -> pp instanceof Reflex).toList();
//                    if (!rr.isEmpty())
//                        window(new Surfaces(Iterables.transform(rr,
//                                pp -> NARui.reflexUI((Reflex) pp))), 800, 600);
//                });

                //        if (exeChart) {
                //            Exe.runLater(() -> n.runLater(() -> {
                //                window(NARui.exeChart((WorkerExec) n.exe), 800, 800);
                //            }));
                //        }

//                List<Reflex> l = n.parts(Reflex.class)
//                        .collect(toList());
//                if (!l.isEmpty()) {
//                    window(new Gridding(
//                        l.stream().map(NARui::reflexUI).collect(toList())
//                    ), 500, 500);
//                }

            });

        }

//        if (uiGames) {
//            nar.runLater(() -> {
//                SpaceGraph.window(new Gridding(nar.parts(Game.class).map(NARui::gameUI).collect(toList())), 1024, 768);
//            });
//        }

        var timeLimitCyc = jcog.Config.INT("TIMELIMIT", -1);
        if (timeLimitCyc >= 0) {
            nar.runAt(timeLimitCyc, this::exit);
        }

        if (jcog.Config.IS("rewardTrace", false)) {
            float rewardTraceDurs = 400;
            nar.onDur(new RewardTrace(), rewardTraceDurs);
        }

        //nar.synch();
        ready.accept(n);


    }

    private static void reflex0(Game g) {
        var r = new Reflex0(
                Agents::PPO,
                //Agents::VPG,
                //Agents::CMAES,
                //PolicyAgent::DQN,
                //ValuePredictAgent::DQN_NTM,
                //ValuePredictAgent::DQN_LSTM,
                //ValuePredictAgent::DQN1,
                g,
                1
                //1,2
                //1, 2, 4
        );
        reflexUI(r);
        //g.actions.curiosity.enable.set(false);
        //g.actions.curiosityReviseOrReplace = true;
    }

    private static void reflex(Game g) {
        g.runLater(()->{
            var r =
                    new Reflex(g); //DIRECT
//                    new Reflex(g, new Reflex.TableModel.Builder("agent",
//                            (i,o)->new Reinforce(i, o, i*o, 6).agent(),
//                            true//history.length > 1
//                            ,g.rewards.size() > 1,
//                            1
//                    ));
            window(nars.gui.ReflexUI.reflexUI(r, r.agent(), g::afterFrame), 800, 600);
        });
    }

    private static void reflexUI(Reflex0 r) {
        window(ReflexUI.reflexUI(r), 900, 900);
    }

    private void initEternalization(NAR n) {
        var ete =
                new Eternalization.Temporals()
                //new Eternalization.TemporalsAndVariables()
                //new Eternalization.Flat()
                //new Eternalization.Derived()
                ;

        n.eternalization = ete.set(eternalization);
    }

    public final Stream<Game> games() {
        return gamesAndMetaGames().filter(g -> !(g instanceof MetaGame));
    }

    public final void games(Consumer<Game> each) {
        games().forEach(each);
    }

    public final Stream<Game> gamesAndMetaGames() {
        return nar.parts(Game.class);
    }

//    static final float metaFreq = 0.02f; // / meta.game.size();

    public SelfMetaGame meta() {

        var meta = new SelfMetaGame(nar, selfMetaDurs, subMetaDurs) {

            @Override
            protected void initMeta() {

                heapSense();

                emotionSense();

                if (cpuThrottle) {
                    autoThrottle();
                    //cpuThrottle(0.05f, 1);
                }

                //fairDeriverShallow();

                if (eternalizationControl) {
                    float eternalMin = 0, eternalMax = 0.5f;
                    eternalization(eternalMin, eternalMax);
                }
                if (answerDepthBeliefGoalControl) {
                    float min = 0.01f, max = 1f;
                    floatAction(inh(id, "answerDepth"), (FloatProcedure) e ->
                        nar.answerDepthBeliefGoal.setLerp(e, min, max));
                }

                if (beliefConfControl)
                    conf(punc(BELIEF), nar.beliefConfDefault, 0.1f, 0.9f);

                if (goalConfControl)
                    conf(punc(GOAL), nar.goalConfDefault, 0.1f, 0.9f);

                if (encourager) {
                    var enc = new FloatRange(0.5f, 0, 1);
                    var e = encouragement(enc);
                    nar.runLater(() -> { //HACK
                        window(new Gridding(NARui.beliefChart(e.sensor, nar),
                                        new PushButton(":)", () -> enc.set(1)),
                                        new PushButton(":(", () -> enc.set(0))),
                                800, 800);
                    });
                }

                if (confMinControl)
                    confMin(confMin, confMinMax);

                //truthPrecision(true, false);

                //conf(nar.goalConfDefault, 0.25f, 0.75f);

                if (complexMaxControl && complexMax > complexMin)
                    complexMax(complexMin, complexMax);


                if (durMeta)
                    durMeta(3, 2);

                if (selfFreq)
                    action(inh(id, "freq"), (float x) ->
                            focus().freq.setLerp(x, 0.01f, 0.1f)
                    );
                else
                    focus().freq(selfFreqDefault);

//                if (nar.control.model instanceof CreditControlModel) {
//                    metaGoals((CreditControlModel) nar.control.model);
////            CreditControlModel.Governor governor = ((CreditControlModel) nar.control.model).governor;
//                    //if (governor instanceof MLPGovernor) governor((MLPGovernor)governor); //probably isnt always necessary
//                }
                //        if (nar.memory instanceof HijackMemory) {
                //            memoryControl((HijackMemory)nar.memory);
                //        }

                //overderive(DERIVE, derivePri, 1, 1.5f);
                //opPri(SELF, derivePri.opPri);

                if (premiseAuto) premiseAuto(premiseAutoPri);
            }

            private void premiseAuto(PremisePriControl p) {
                new PremisePri(p)
                    .add("var", p.varIntro)
                    .add("nal", p.nalPremise)
                    .add("decompose", p.decomposing)
                    .add("clusterInduct", p.clusterInduct)
                    .add("termlink", p.termLinking)
                    .add("evaluate", p.evaluate)
                    //TODO others
                    .commit();
            }

            private class PremisePri {
                static final float min = 0.01f, max = 1;
                final Map<String,FloatRange> p = new LinkedHashMap<>();
                private final PremisePriControl c;

                public PremisePri(PremisePriControl c) {
                    this.c = c;
                }

                public PremisePri add(String s, FloatRange r) {
                    if (r == c.other) throw new UnsupportedOperationException();
                    p.put(s, r);
                    return this;
                }
                public void commit() {
                    p.forEach((n, r)->{
                       premisePriAction(n, r, min, max);
                    });
                    afterFrame(()->{
                        double probSum = 0;
                        for (FloatRange r : p.values())
                            probSum += r.getAsDouble();
                        double remain = 1 - probSum/p.size();
                        c.other.set(Util.lerp(Util.clamp(remain, 0, 1), min, max));
                    });
                }
                private void premisePriAction(String other, FloatRange p, float min, float max) {
                    floatAction(inh($.p(id, "premise"), other), x -> p.setLerp(x, min, max));
                }
            }


            @Override
            protected void initMetaEnd() {
                if (selfMetaReflex) {
                    focus().freq(0.001f);
                    reflex(this);
                }
            }

            @Override
            protected void initMeta(SubMetaGame m) {
                var g = m.game;

                if (actionRewardQuestion)
                    m.actionRewardQuestion(g);

                if (perceptionControl)
                    m.perception(g, 1/50f, 1/2f);

                if (curiosityControl) {
                    m.curiosity(g,
                        NAL.CURIOSITY_RATE_ON, NAL.CURIOSITY_RATE_ON * 2,
                        NAL.CURIOSITY_RATE_OFF, NAL.CURIOSITY_RATE_OFF / 2
                    );
                }

                //senseGameRewards();

                var f = g.focus();
                var budget = (DefaultBudget) f.budget;

                if (opPri)
                    m.opPri(f.id, budget.opPri,
                            //0.01f
                            //0.1f
                            //0.5f
                            //ampMin/ampMax /* HACK: re-use the same dynamic range: */
                            ampMin,
                            1,
                            nalDelta,
                            nalDiff
                    );

                if (puncSeed) {
                    if (!NAL.TASKBAG_PUNC_OR_SHARED)
                        throw new TODO();

                    var psid =
                            $.p(f.id, "seed");
                            //f.id;
                    var puncPriMin =
                        ampMin;

                    if (puncSeedBasic) {
                        m.puncPriBasic(
                            psid,
                            false, true, puncPriMin, 1,
                            budget.puncSeed
                            //budget.puncSeed, budget.puncLink //JOINT
                            //budget.puncSeed
                        );
                    } else {
                        m.puncPri(psid, puncPriMin, 1,
                            budget.puncSeed
                            //budget.puncLink, budget.puncSeed //JOINT
                        );
                    }
                }

                if (derivePri) {
                    var pdID = $.p(f.id, "derivePri");
                    float pMin = ampMin, pMax = 1;
                    if (derivePriSimple) {
                        m.floatAction(inh(pdID, pri),
                            p -> budget.puncDerived.set(Util.lerpSafe(p, pMin, pMax)));
                    } else {
                        m.puncPriBasic(pdID, true, false,
                            pMin, pMax,
                            budget.puncDerived);
//                        m.puncPri(pdID,
//                          pMin, pMax,
//                          budget.puncDerived);
                    }
                }

//                if (linkPriAutoReward) {
//                    //TODO normalize happiness
//                    float activationBase = ((DefaultBudget)g.focus().budget).linkActivation.asFloat();
//                    float divisor = 10f;
//                    g.onFrame(G -> {
//                        ((DefaultBudget)G.focus().budget).linkActivation.setLerp(
//                            (float)G.happiness(),
//                                activationBase/divisor, activationBase
//                        );
//                    });
//                }
                if (inputBagPri) {
                    m.linkPri(concept, budget.inputActivation, ampMin, 1);
                }

                if (simplicityControl)
                    m.simplicity(budget, ampMin, 2);

                if (certaintyControl)
                    m.certain(budget, ampMin, 2);

                if (focusForget != null) {
                    switch (focusForget) {
                        case Forget -> m.focusSustainBasic(f, 0, 1);
                        case ForgetOnly -> m.focusSustainBasic(f, -0.3f, -0.01f);
                        case Balanced -> m.focusSustainBasic(f, -1, 1);
                        case BalancedSoft -> m.focusSustainBasic(f, -0.5f, +0.5f);
                    }
                } else {
                    //var u = ((BagFocus)metaGame.game.focus()).updater;
                    //((BagSustain)u).sustain.set(); //some constant release
                }

                if (focusClear) {
                    m.focusClearStochastic /* focusClearPWM*/(f,
                            2, 128);
                }

                m.senseSimple(inh(f.id, $.p(concept, simple)), f.attn. _bag, z -> z.get().term().complexity());
//                m.senseSimple(inh(f.id, $.p(task, simple)), f.tasks, z -> z.id.complexity());

                if (f.attn instanceof Focus.TaskBagAttn)
                    m.sensePunc(f);

                if (focusSharp)
                    m.focusSharp(
                            2, 5
                            //0.5f, 4
                    );

                //memoryControlSimple(w, -0.9f, 0.9f);

                //memoryControlPrecise(w, false, true, false);
                //memoryControlPrecise(w, true, false, true);


                //memoryPreAmp(w, true, false);

                //curiosityShift();
//                if (curiosityControl)
//                    g.curiosityRate();
                //curiosityStrength();


//                {
//
//                    OpPri op = new OpPri();
//                    PuncPri punc = new PuncPri();
//                    w.taskLinkPri = t -> op.floatValueOf(t) * punc.floatValueOf(t) * t.pri();
//                    Term root = w.id; /* $.p(w.id, pri)*/
//                    opPri(root, op);
//                    puncPri(root, punc);
//
//                }


//				if (w.inBuffer instanceof PriBuffer.BagTaskBuffer)
//					floatAction($.inh(gid, input), ((PriBuffer.BagTaskBuffer) (w.inBuffer)).valve);

                var t = (BasicTimeFocus) f.time;
                if (timeFocus)
                    m.timeFocus(f.id, t,
                        nar::dur,
                        durMax, durShift,
                1f/subMetaDurs
                    );
                else {
                    m.game.onFrame(()->{
                        var rng = rng();
                        t.shiftDurs((rng.nextFloat() * 2 - 1) * durShift);
                        t.dur(rng.nextFloat() * durMax * nar.dur());
                    });

                }


                {
                    //RESET
                    float amp = 1;
                    g.actions.pri.amp(amp);
                    g.sensors.pri.amp(amp);
                    g.rewards.pri.amp(amp);
                }

                if (gamePri) {
                    if (autoReward)
                        m.autoReward(
                            autoRewardConf, autoRewardPri
                            //true, true //priority of derivations will be double-discounted due to conf loss
                            //false, true
                        );
                    if (!autoReward/* || !autoRewardPri*/)
                        m.priRewards();

                    var individualActionPri = ADVANCED;
                    if (individualActionPri) {
                        g.actions.compoundActions().forEach(c -> m.priAction(c.pri));

                        for (var a : g.actions)
                            if (a instanceof CompoundAction.CompoundActionComponent) {
                                /* ignore; the entire set of concepts is setup above */
                            } else if (a instanceof AbstractGoalAction aga)
                                m.priAction(aga.pri);
                            else
                                throw new TODO();
                    } else
                        m.priActions();

                    m.priSensors(
                            true, true, false
                            //false, true, true
                    );
                }

                if (metaMomentum != 0)
                    m.actions.filter(new ActionMomentum(metaMomentum));

//                if (rewardConf) {
//                    final float confBase = g.nar.confDefault(GOAL);
//                    g.rewardConf(confBase / 4, confBase);
//                }

                Reward dexReward = m.dexReward();
                    //.strength(0.5f);

                Reward gameReward = m.rewards(inh(/*g.*/id, happy), g, false);

                if (subMetaReflex) {
                    //m.focus().freq(0.001f); //deprioritize

                    {
                        reflex(m);
                    }

                    {
//                        var R = m.reflex(new Predicate<>() {
//                            final Set<String> incl = java.util.Set.of(
//                                    "(op",
//                                    "derive)",
//                                    "simple",
//                                    "certain",
//                                    ".!", "?@",
//                                    //"pri",
//                                    "link"
//                            );
//                            @Override
//                            public boolean test(AbstractAction a) {
//                                //return true;
//                                return incl.stream().anyMatch(match ->
//                                        a.term().toString().contains(match)
//                                );
//                            }
//                        });
//                        reflexUI(R);
                        }

                    }

                if (pausing)
                    m.pausing();

            }


        };

        //self.logActions();

        if (metaMomentum != 0) {
            meta.actions.filter(new ActionMomentum(metaMomentum));
        }


        nar.runLater(()->{
            games().forEach(g -> g.capacity(focusConcepts));
            meta.capacity(metaFocusConcepts);
            meta.game.forEach(g -> g.capacity(subMetaFocusConcepts));
        });


//        meta.afterFrame(() -> {
//            meta.focus().freq(metaFreq);
//                        //.freqAndPri(p);
//        });

        return meta;
    }

    private volatile boolean shutdown;

    public final void exit() {
        shutdown = true;
        System.exit(0);
    }

    public Time clock = new RealTime.MS();

    /**
     * TODO automatically compute this based on the maximum framerate of any game, up to a system hard limit (ex: 100 fps)
     */
    public Player fps(float fps) {
        //this.fps = Math.min(60, fps);
        clock = new RealTime.MS().durFPS(fps);
        return this;
    }

    private NAR nar() {

        var n = new NARS()
                .time(clock)
                .memory(memory())
                .get();

        n.focus.remove(n.main().pri(0, 0)); //HACK disables default context

        n.exe.exe = new ForkJoinPool(Math.max(1, threads/2), ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
        derivers(n);

        return n;
    }

    private void derivers(NAR n) {
        var r = rules().compile(n);
        r.print();
        n.runLater(()->
            n.add(new DeriverExec(r, threads) {
                @Override
                protected Deriver deriver(NAR n) {
                    var d = super.deriver(n);
                    if (premiseAuto) {
                        if (!(d instanceof Deriver))
                            throw new UnsupportedOperationException();
                        ((TaskBagDeriver) d).premises.pri = premiseAutoPri;
                    }
                    return d;
                }
            })
        );
    }


    private final PremisePriControl premiseAutoPri = premiseAuto ? new PremisePriControl() : null;
//
//    private Exec multiExec() {
//        Function<NAR,ReactionModel> model =
//                n ->rules().compile(n).print();
//
//        return new WorkerExec(threads, exeAffinity, model) {
//
//            @Override
//            protected Deriver deriver(Focus f) {
//                var d = super.deriver(f);
//                if (premiseAuto) {
//                    ((TaskBagDeriver.PrioritySetPremiseQueue) ((TaskBagDeriver)d).queue).pri = premiseAutoPri;
//                }
//                return d;
//            }
//        };
////        return new FiberExec(threads, model);
//    }

    /**
     * TODO parameters
     */
    private Memory memory() {

        /* curve TODO refine */
        int compoundFactor;
        if (ramGB <= 0.25f) compoundFactor = 6 * 4;
        else if (ramGB <= 0.5f) compoundFactor = 12 * 4;
        else compoundFactor = 24 * 8;

        var compoundCap = compoundFactor * 1024 * ramGB;

        return memoryHijackAtomCompoundSplit(1, (int) compoundCap);

        //return memoryHijackTier();

        //return memoryCaffeine();

        //return memoryCaffeineSoft();


//        int largeThresh = Math.max(1,Util.sqrtInt(complexMax));
        //				ramGB >= 0.75 ?
        //new HijackMemory((int) Math.round(ramGB * 64 * 1024), 3)

//					CaffeineMemory.soft()

    }


    private CaffeineMemory memoryCaffeine() {
        var concepts = (int) round(ramGB * 10 * 1024);
        //return new CaffeineMemory(concepts);
        var complexMean = complexMax / 2;
        return new CaffeineMemory(CaffeineMemory.ComplexityWeigher, (long) concepts * complexMean);
    }

    /**
     * dangerous, can lose hardwired concepts briefly during temporary eviction
     */
    private CaffeineMemory memoryCaffeineSoft() {
        return CaffeineMemory.soft();
    }

    /**
     * TODO support changing volume
     */
    private TierMemory memoryHijackTier() {
        var reprobes = 7;

        var chunkSize = 3;
        var chunks = (int) Math.ceil((float) (complexMax - 1) / chunkSize);
        var m = new Memory[chunks + 1];
        m[0] = new HijackMemory((int) round(ramGB * 2 * 1024), Math.max(3, reprobes / 2));
        for (var i = 0; i < chunks; i++) {
            //TODO optional complexity curve
            var c = (conceptsMax <= 0 ? ramGB * 32 * 1024 : conceptsMax) / chunks;
            m[i + 1] = new HijackMemory((int) round(c), reprobes);
        }
        return new TierMemory(
                k -> {
                    var kv = k.complexity();
                    return kv == 1 ? 0 : (kv - 1) / chunkSize;
                }, m
        );
    }


    private TierMemory memoryHijackAtomCompoundSplit(int atomCap, int compoundCap) {
        return new TierMemory(
                k -> k instanceof Atomic ? 0 : 1,
                new HijackMemory((int) round(ramGB * atomCap * 1024), 5),
                new HijackMemory((int) round(conceptsMax <= 0 ? compoundCap : conceptsMax),
                        hijackMemory_Reprobes)
        );
    }

    private TierMemory memoryHybridTier(int thresh) {
        return new TierMemory(
                k -> k.complexity() < thresh ? 0 : 1,
                new HijackMemory((int) round(conceptsMax <= 0 ? ramGB * 32 * 1024 : conceptsMax), 4),
                CaffeineMemory.soft()
        );
    }

//	public void addClock(NAR n) {
//
//		Atom FRAME = Atomic.atom("frame");
//
//		n.parts(Game.class).forEach(g -> g.onFrame(() -> {
//			long now = n.time();
//			int X = g.iterationCount();
//			int radix = 16;
//			Term x = $.pRecurse(false, $.radixArray(X, radix, Integer.MAX_VALUE));
//
//			Term f = $.funcImg(FRAME, g.id, x);
//			Task t = new SerialTask(f, BELIEF, $.t(1f, n.confDefault(BELIEF)),
//				now, Math.round(now + g.durLoop()),
//				new long[]{n.time.nextStamp()});
//			t.pri(n.priDefault(BELIEF) * g.what().pri());
//
//			g.what().accept(t);
//		}));
//	}

//	public void addFuelInjection(NAR n) {
//		n.parts(What.class).filter(w -> w.inBuffer instanceof PriBuffer.BagTaskBuffer).map(w -> (PriBuffer.BagTaskBuffer) w.inBuffer).forEach(b -> {
//			MiniPID pid = new MiniPID(0.007f, 0.005, 0.0025, 0);
//			pid.outLimit(0, 1);
//			pid.setOutMomentum(0.1);
//			float ideal = 0.5f;
//			n.onDur(() -> b.valve.set(pid.out(ideal - b.load(), 0)));
//		});
//
//	}

    public Reactions rules() {
        var d = new NARS.Rules();

        d.core(varIntro);

        if (temporalInduction)
            d.temporalInduction();

        //d = Derivers.nal(nalMin, nalMax);
        if (nalStructural)
            d.structural();

        if (nalProcedural)
            d.procedural();
        if (nalAnalogy)
            d.analogy();
        if (nalSets)
            d.sets();
        if (nalDelta) {
            //Δ
            d.files("delta.induction.nal");
            //d.files("delta.induction.extra.nal");

            if (deltaGoal)
                d.files("delta.goal.nal");
        }
        if (nalDiff)
            d.diff();

        var a = new ActionTiming();

        if (stmLinker)
            d.add(new STMLinker(1, true, true, false, false)/*.taskVol(3, 9)*/
//                .isNotAny(PremiseTask, IMPL).isNotAny(PremiseBelief, IMPL)
            );

        if (clusterBelief)
            d.add(clusterInduction(BELIEF, clusterRate, clusterCap));

        if (clusterGoal)
            d.add(clusterInduction(GOAL, clusterRate, clusterCap));

//        if (taskResolveAction)
//            d.add(new TaskResolve(a, TaskResolver.AnyTaskResolver));
//
//
//        if (beliefResolveAction)
//            d.add(new BeliefResolve(true, true, true, true, a,
//                    TaskResolver.AnyTaskResolver)); //extra belief resolver for present focus

        if (motivation)
            d.files("motivation.nal");

        if (xor)
            d.files("xor.nal");

//        if (goalSpread) {
//            var punc = new PuncBag(0, 0, 1 / 2f, 1 / 8f, 0, 1);
//            final var l = new LinkFlow(
//                    LinkFlows.fromEqualsFromOrTo,
//                    //LinkFlows.fromEqualsFrom,
//                    //LinkFlows.fromEqualsTo,
//                    x -> {
//                        //float[] f = ((TaskLink)x).priGet(punc);
//                        var t = x.task();
//                        var p = t.punc();
//                        var v = punc.apply(p) * t.priElseZero();
//                        if (v <= Prioritized.EPSILON)
//                            return null;
//                        var f = new float[4];
//                        //float bias = -0.01f * v/4;
//                        //Arrays.fill(f, bias);
//                        f[switch(p) {
//                            case BELIEF -> 0;
//                            case QUESTION -> 1;
//                            case GOAL -> 2;
//                            case QUEST -> 3;
//                            default -> -1;
//                        }] = v;
//
//                        //transition matrix
////                    final float f3 = f[3];
////                    f[2] += f3 * 1/3f; //Q -> g
////                    f[1] += f3 * 1/3f; //Q -> q
////                    f[3] *= 1/3f;
//
//                        return f;
//                    }
//                    ,
//                    PriMerge.plus
//            );
//            l.taskPunc(GOAL, QUEST);
//            l.spread.set(
//                    //1
//                    2
//                    //8
//            );
//            d.add(l);
//
//            //d.add(new LinkFlow(0f, 0, 0.1f, 0f, LinkFlows.edgeToEdge).neq(TheTask,TheBelief));
//
//            //TODO impl->impl link creation from results of adjacents
//
//            //d.add(new xxTermLinking.PremiseTermLinking(new CachedAdjacenctTerms(EqualTangent.the, false)));
//
//        }


        if (ifThenElse)
            d.files("if.nal");


        if (goalInduction)
            d.files("induction.goal.nal");

        var inperienceLevels =
            0;
            //1;

        if (inperienceBeliefs) {
            d.addAll(new Inperience.BeliefInperience(BELIEF, inperienceLevels).timelessOnly().hasNot(PremiseTask, IMPL, CONJ));
        }
        if (inperienceGoals) {
            d.add(
                new Inperience.BeliefInperience(GOAL, inperienceLevels)
            );
        }
        if (inperienceQuestions) {
            d.addAll(
                    //.timelessOnly().hasNot(PremiseTask, IMPL,CONJ)
                    new Inperience.QuestionInperience(QUESTION)
                    //.timelessOnly().hasNot(PremiseTask, IMPL,CONJ)
                    , new Inperience.QuestionInperience(QUEST)
                    //.timelessOnly().hasNot(PremiseTask, IMPL,CONJ)
            );
        }



//        if (eternalizeImpl)
//            d.addAll(new EternalizeAction()
//                    .isAny(TheTask, Op.or(IMPL))
//            );


        if (implBeliefify)
            d.add(new ImplTaskifyAction(true));

        if (implGoalify)
            d.add(new ImplTaskifyAction(false));

        var implyerNodeCapacity = 64;
        if (implyer) {
            int maxTasks = 1, triesPerTask = 1;
            Implyer.start(d,
                false, true, true
                //true, false, false
                //true, true, true
                //true, false, true
                , maxTasks, triesPerTask, implyerNodeCapacity
            );
        }

        if (implyer && implyerAggressive) {
            var metagamesAlso = true;
            this.ready(e ->
                (metagamesAlso ? gamesAndMetaGames() : games()).forEach(g ->
                    Implyer.onTasks(implyerNodeCapacity, g.focus())
                ));
        }

        if (conjyer) {
            int maxTasks = 1, triesPerTask = 1;
            Conjyer.start(d, maxTasks, triesPerTask, implyerNodeCapacity);
        }

        if (implyerGame) {

            var sensorPower =
                0;
                //1;
                //2;
                //3;
                //4;

            var actionPower =
                2;

            var rewardPower =
                4;

            var triesPerTask = 1;

            ready(n -> games().forEach(g -> {
                implyerGame(g, implyerNodeCapacity, sensorPower, actionPower, rewardPower, triesPerTask);
            }));
        }

        if (arith)
            d.add(new Arithmeticize.ArithmeticIntroduction());

        if (factorize)
            d.add(new Factorize.FactorIntroduction());

        if (explode)
            d.addAll(new ExplodeAction());

//        if (answerLogging) {
//            //.log(true).apply(false)
//
//            //d.add(new AnswerQuestionsFromBeliefTable(a, true, true)); //extra belief resolver for present focus
//            d.add(new AnswerQuestionsFromConcepts.AnswerQuestionsFromTaskLinks(new PresentFocus())
//                    //.log(true).apply(true)
//            );
//
//        }

//        if (answerQuestionsFromTaskLinks) {
//            d.add(new AnswerQuestionsFromConcepts.AnswerQuestionsFromTaskLinks(new PresentTiming()).taskComplex(2, 16));
            //d.add(new AnswerQuestionsFromBeliefTable(a, true, false)) //extra belief resolver for present focus
            //d.add(new TermLinking.PremiseTermLinking(new FirstOrderIndexer())) //<- slow
//        }

        if (abbreviation) {
            d.addAll(
                    new Abbreviate.AbbreviateRecursive(9, 16),
                    new Abbreviate.UnabbreviateRecursive());
            //TODO re-abbreviate: abbreviate unabbreviated instances terms recursively which also appear in the term abbreviated
//                //.add(new Abbreviation.AbbreviateRoot(4, 9))
//                //.add(new Abbreviation.UnabbreviateRoot())
        }
        return d;
    }


    /** TODO a lighter weight variation that samples tasks from focus taskbag directly, rather than lookup from the concepts here */
    private static void implyerGame(Game g, int nodeCap, int sensorPower, int actionPower, int rewardPower, int triesPerTask) {
        var focusOrConcepts =
                false;
        //true;

        boolean sensors = sensorPower > 0, rewards = rewardPower > 0, actions = actionPower > 0;

        var f = g.focus();
        List<? extends Termed>
                S = sensors ? g.sensors.components().toList() : EMPTY_LIST,
                R = rewards ? g.rewards.components().toList() : EMPTY_LIST,
                A = actions ? g.actions.components().toList() : EMPTY_LIST;

        var targets = new UnifiedSet<Term>();

        for (var x : S) targets.add(x.term());
        var updateSensors = Implyer.ImplyerActive.run(S, focusOrConcepts, sensorPower, triesPerTask, nodeCap, f);

        for (var x : A) targets.add(x.term());
        var updateActions = Implyer.ImplyerActive.run(A, focusOrConcepts, actionPower, triesPerTask, nodeCap, f);

        for (var x : R) targets.add(x.term());
        var updateRewards = Implyer.ImplyerActive.run(R, focusOrConcepts, rewardPower, triesPerTask, nodeCap, f);

        targets.trimToSize();
        Predicate<Term> inTargets = targets::contains;
        g.beforeFrame(() -> {
            Implyer.update(inTargets, nodeCap, f);
            updateSensors.run();
            updateActions.run();
            updateRewards.run();
        });
    }



    /**
     * determines cluster 'sharpness'.  higher values are less sharp,
     * including more tasks between which n-pairings can be made.
     * >= 3, probably
     */
    private static final float tasksPerCentroid =
            4;
    //6;
    //16;
    //12;
    //8;
    //32;

    private static ClusterInduct clusterInduction(byte punc, float rate, int cap) {
        var c = new ClusterInduct(punc, round(cap / tasksPerCentroid), cap);
        c.outputRatio.set(rate);
        //c.condMax.set(3);
        return c;
    }

    public final <C> Stream<C> the(Class<C> c) {
        return nar.parts(c);
    }

    @Override public void close() {
        nar.runLater(() -> {
            stop();
            nar.delete();
            nar = null;
        });
    }

    /**
     * mean reward over entire existence (or until statistics are reset)
     */
    public double rewardMean() {
        return games().mapToDouble(Game::rewardMean).average().getAsDouble();
    }

    private static class PremisePriControl implements FloatFunction<Premise> {
        public final FloatRange
            varIntro = FloatRange.unit(1),
            nalPremise = FloatRange.unit(1),
            termLinking = FloatRange.unit(1),
            decomposing = FloatRange.unit(1),
            clusterInduct = FloatRange.unit(1),
            evaluate = FloatRange.unit(1),
            other = FloatRange.unit(1)
        ;

        @Override public float floatValueOf(Premise x) {
            FloatRange y;
            if (x instanceof NALPremise)
                y = nalPremise;
            else {
                var xc = x.reactionType();
                if (xc == VariableIntroduction.class)
                    y = varIntro;
                else if (xc == TermLinking.class)
                    y = termLinking;
                else if (xc == ClusterInduct.class)
                    y = clusterInduct;
                else if (xc == Evaluate.class)
                    y = evaluate;
                else if (DecomposeTerm.class.isAssignableFrom(xc))
                    y = decomposing;
                else {
                    //System.out.println(xc);
                    y = other;
                }
            }
            return y.floatValue();

            //return x instanceof NALPremise n ? (n.task.priElseZero()/2+0.5f) : 0.5f;
//            if (x.reactionType()== VariableIntroduction.class)
//                return 0.1f;
//            else return 1;
            //return x.reactionType()==null ? 1 : 0.5f;
            //return 1;
        }
    }


    /**
     * see: https://agarwl.github.io/rliable/assets/slides_mlc.pdf
     */
    @Deprecated
    private class RewardTrace implements Runnable {

        final Map<String, Object> result = new TreeMap();
        final DoubleSummaryStatistics rewards = new DoubleSummaryStatistics();
        //DoubleSummaryStatistics rewardsNow = new DoubleSummaryStatistics();

        {
            games().forEach(g -> g.onFrame(() -> {
                var h = g.happiness();
                if (h == h) {
                    synchronized (this) {
                        rewards.accept(h);
                        //rewardsNow.accept(h);
                    }
                }
            }));
        }

        /**
         * TODO Config
         */
        PrintStream out;

        {
            var file = jcog.Config.get("rewardtracefile", null);
            if (file == null)
                out = System.out;
            else {
                try {
                    out = new PrintStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                //if (file!=null)  result.put("file", file);
                result.put("rewardMean", rewards.getAverage());

                jcog.Config.forEach(result::put);
                result.put("games", games().map(z -> z.id.toString()).toList());

                //result.put("clocktime",...)
                //result.put("machine",...)

                out.println(Serials.jsonNode(result).toPrettyString());
                //out.println("rewards: " + rewards);
                out.close();
            }));
        }

//        private long lastTime = nar.time();

        @Override
        public synchronized void run() {
//            if (shutdown)
//                return;
//            long now = nar.time();
//            long tDelta = now - lastTime;
//            if (tDelta > 0) {
//                //double rSum = games().mapToDouble(g -> g.rewards.rewardStat.getSum()).sum();
//                //double rDelta = rSum - lastReward;
//                //double rMean = rDelta / tDelta;
//
//                synchronized(this) {
//                    out.println(now + "," + rewardsNow.getAverage());
//                    rewardsNow = new DoubleSummaryStatistics(); //reset
//                }
//                out.flush();
//
//                //lastReward = rSum;
//                lastTime = now;
//            }

        }

        private Stream<Game> games() {
            return gamesAndMetaGames().filter(g -> !(g instanceof MetaGame));
        }

    }

}