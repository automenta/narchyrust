package nars.game.meta;

import jcog.Is;
import jcog.TODO;
import jcog.Util;
import jcog.agent.Agent;
import jcog.data.list.Lst;
import jcog.math.FloatDifference;
import jcog.math.FloatSupplier;
import jcog.math.normalize.FloatNormalized;
import jcog.signal.FloatRange;
import jcog.tensor.rl.pg.*;
import nars.$;
import nars.Op;
import nars.Term;
import nars.control.DefaultBudget;
import nars.focus.BasicTimeFocus;
import nars.focus.PriAmp;
import nars.game.Game;
import nars.game.GameTime;
import nars.game.action.AbstractAction;
import nars.game.action.AbstractGoalAction;
import nars.game.action.util.Reflex0;
import nars.game.reward.LambdaScalarReward;
import nars.task.util.OpPri;
import nars.task.util.PuncBag;
import nars.term.atom.Atomic;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.stream.Stream;

import static jcog.Util.*;
import static nars.$.inh;
import static nars.$.p;
import static nars.Op.*;

/**
 * self metaprogrammer - agent metavisor - homunculus
 */
@Is({"Homunculus", "Metagaming", "Human_biocomputer", "Variety_(cybernetics)"})
public abstract class MetaGame extends Game {

    protected static final float resAction = 0.01f;
    protected static final float resReward = resAction;

    public static final int DEFAULT_DUR_MULTIPLIER =
        1;
        //2; //nyquist
        //16;

    /**
     * action's dynamic range exponent.
     * higher values pack more dynamic range "octaves" or "decibels",
     * thus allow more sensitivity and expressiveness
     */
    @Is("Sound_intensity") static float priActionSharp =
        Float.NaN //disable (linear)
        //6
        //4
        //3
        //2
        //8
        //10
        //20
        //3
        //5
        //8
        //1
        //3
        //4
        ;

    static float priActionMin = PriAmp.EPSILON,
                 priActionMax = 1;

    /** probability of clearing focus. 0 to disable */
    private static final float autoclearProb =
        0;
        //0.01f;

    static final Atomic OP = Atomic.atom("op");
    static final Atomic forget = Atomic.atom("forget");

    protected static final Atomic simple = Atomic.atom("simple");


    protected static final Atomic link = Atomic.atom("link");
    protected static final Atomic term = Atomic.atom("term");
    protected static final Atomic task = Atomic.atom("task");
    protected static final Atomic concept = Atomic.atom("concept");

    static final Atomic clear = Atomic.atom("clear");
    static final Atomic effort = Atomic.atom("effort");

    static final Atomic meta = Atomic.atom("meta");
    static final Atomic focus = Atomic.atom("focus");

//	static final Atomic grow = Atomic.atom("grow");
    /**
     * internal truth frequency precision
     */
    static final Atomic precise = Atomic.atom("precise"); //frequency resolution
//	static final Atomic ignorant = Atomic.atom("ignorant"); //min conf
//	static final Atomic careful = Atomic.atom("careful"); //conf resolution

    static final Atomic CURIOSITY = Atomic.atomic("curi");

    static final Atomic PERCEPTION = Atomic.atomic("perception");

//	static final Atomic belief = Atomic.atom("belief");
//	protected static final Atomic goal = Atomic.atom("goal");
//
//	static final Atomic question = Atomic.atom("question");
//	static final Atomic quest = Atomic.atom("quest");

    protected static final Atomic freq = Atomic.atom("freq");
    protected static final Atomic conf = Atomic.atom("conf");
    public static final Atomic pri = Atomic.atom("pri");

    public static final Atomic lag = Atomic.atom("lag");

    public static final Atomic active = Atomic.atom("active");
    static final Atomic mean = Atomic.atom("mean");

    protected static final Term strength = pri;//SETe.the(conf, pri);

    static final Atomic play = Atomic.atom("play");
    static final Atomic input = Atomic.atom("input");
    static final Atomic dur = Atomic.atom("dur");
    static final Atomic shift = Atomic.atom("shift");
    public static final Atomic happy = Atomic.atom("happy");
    static final Atomic optimistic = Atomic.atom("optimistic");
    static final Atomic dex = Atomic.atom("dex");
    static final Atomic now = Atomic.atom("now");
    static final Atomic past = Atomic.atom("past");
    static final Atomic future = Atomic.atom("future");
    static final Atomic on = Atomic.atom("on");


    protected MetaGame(Term id, GameTime t) {
        super(id, t);



        if (DEFAULT_DUR_MULTIPLIER != 1)
            focus().time =  new DurMultiplierTiming();

        if (autoclearProb > 0) {
            afterFrame(() -> {
                if (rng().nextBooleanFast16(autoclearProb))
                    focus().clear();
            });
        }
    }

    @Override
    protected void init() {
        super.init();
        {
            //HACK experimental 'seed' defaults
            ((DefaultBudget) focus().budget).puncSeed.set(0.5f, 0.1f, 1f, 0.2f);
            sensors.pri.amp(0.25f);
        }
    }

    public LambdaScalarReward rewards(Term id, Game g, boolean normalized) {
        FloatSupplier f = () -> {
//            float tDur =
//                dur();
//                //Math.max(1, t.e - t.s);
            float happinessDurs = nars.NAL.signal.HAPPINESS_DUR_ZERO ? 0 : nar.dur();
            int d = Math.round(happinessDurs);
            long now = time();
//            long now = //t.s;
//                       Fuzzy.mean(t.s, t.e);
            return (float) g.happiness(
                    now - d, now, d
                    //t.e - d , t.e, tDur
                    //t.s - d, t.s, tDur
                    //t.s - d, t.e, tDur
            );
        };
        var r = normalized ? rewardNormalized(id, f) : reward(id, f);
        r.resolution(resReward);
        //r.goalFocus = ScalarReward.GoalFocus.Eternal;
        return r;
    }

    public void durOctaves(Term id, FloatSupplier baseDur, float octaves, FloatProcedure durSetter) {
        floatAction(inh(id, dur), x -> {
            double d = baseDur.asFloat() * Math.pow(2, x * octaves);
            durSetter.value((float) d);
        });
    }

    public AbstractAction priAction(PriAmp node) {
        return priAction(inh(node.id, pri), node::amp);
    }

    public AbstractAction priAction(Term id, FloatProcedure action) {
        return floatAction(id, x -> action.value(expUnitSafe(x)));
    }

    public AbstractAction priActionLinear(Term id, FloatProcedure action, float min, float max) {
        return floatAction(id, x -> action.value(lerpSafe(x, min, max)));
    }

    public AbstractAction priAction(Term id, FloatProcedure action, float min, float max, float sharpness) {
        return floatAction(id, x -> action.value(expUnitSafe(x, sharpness, min, max)));
    }

    public static float expUnitSafe(float x) {
        return expUnitSafe(x, priActionSharp, priActionMin, priActionMax);
    }

    public static float expUnitSafe(float x, float sharpness, float min, float max) {
        return lerpSafe(
            (sharpness==sharpness) ?
                (float) expUnit(x, sharpness) //EXPONENTIAL
                :
                x //LINEAR
        , min, max);
    }


    protected AbstractAction actFloat(Term t, FloatRange r) {
        return floatAction(t, 1, r);
    }

    @Deprecated
    protected AbstractAction floatAction(Term t, float exp, FloatRange r) {
        return floatAction(t, r.min, r.max, exp, r::set);
    }
    public AbstractGoalAction floatAction(Term t, float exp, FloatProcedure r) {
        return floatAction(t, 0, 1, exp, r);
    }

    public AbstractGoalAction floatAction(Term t, FloatProcedure r) {
        return floatAction(t, 0, 1, 1, r);
    }

    protected AbstractGoalAction floatAction(Term t, float min, float max, float exp, FloatProcedure r) {
        boolean EXP = exp != 1;
        var a = action(t, x -> {
            if (x == x) r.value(lerp(EXP ? (float) Math.pow(x, exp) : x, min, max));
        });
        a.freqRes(resAction);
        return a;
    }

    public OpPri opPri(Term root, OpPri pri, float min, float max, boolean delta, boolean sim) {
        //TODO normalize their effect?
        //TODO check any other misssing ops that may name tasks
        Lst<FloatRange> o = pri.op;
        var controlled = new UnifiedSet<>();
        controlled.add(opPri(IMPL, root, min, max, o));
        controlled.add(opPri(CONJ, root, min, max, o));
        //opPri(INH, root, min, max, o),
        //nalSim ? opPri(SIM, root, min, max, o) : null,
        if (delta)
            controlled.add(opPri(DELTA, root, min, max, o));
        if (sim)
            controlled.add(opPri(SIM, root, min, max, o));

        Op[] others = Stream.of(Op.values())
            .filter(j -> j.taskable && !controlled.contains(j))
            .toArray(Op[]::new);

        floatAction(inh(root, p(OP, "other")), v -> {
            for (Op k : others)
                o.get(k.id).setLerp(v, min, max);
        });

        return pri;
    }

    private Op opPri(Op x, Term root, float min, float max, Lst<FloatRange> o) {
        var y = o.get(x.id);
        floatAction(inh(root, p(OP, x.atom)), v -> y.setLerp(v, min, max));
        return x;
    }


    /**
     * 4 punctuation controlled by 2 actions:
     *    belief to goal ratio, belief/goal to question/quest ratio
     * <p>
     * the min and max priority range effectively determines a temporal "bandwidth"
     *
     * for the aggregate bag item lifespan
     *
     * @param amp add extra action for controlling the max amplitude.
     */
    public void puncPriBasic(Term root, boolean amp, boolean qq, float ampMin, float ampMax, PuncBag... pri) {
        afterFrame(new Runnable() {

            private static final Term BELIEF_AND_GOAL = $.quote(".!"), QUESTION_AND_QUEST = $.quote("?@");

            /** belief/goal axis:   0=belief/question-like, 1=goal/quest-like */
            final FloatRange BG = FloatRange.unit(0.5f);

            /** belief/question axis: 0=belief/goal-like, 1=question/quest-like */
            final FloatRange QQ = FloatRange.unit(0);

            /** amplification */
            final FloatRange AMP = FloatRange.unit(1);

            {
                assert (pri.length > 0);

                if (amp)
                    floatAction(inh(root, p(MetaGame.pri, "amp")), AMP::set);

                floatAction(inh(root, p(MetaGame.pri, BELIEF_AND_GOAL)), BG::set);

                if (qq)
                    floatAction(inh(root, p(MetaGame.pri, QUESTION_AND_QUEST)), QQ::set);
            }

            @Override
            public void run() {
                float goalness = BG.floatValue(), questionness = QQ.floatValue();

                float BB = val(1 - goalness, 1 - questionness);
                float GG = val(goalness, 1 - questionness);
                float QQ = val(1 - goalness, questionness);
                float qq = val(goalness, questionness);

                //NORMALIZE
                double normalizationDivisor = BB + GG + QQ + qq;
                BB/=normalizationDivisor; GG/=normalizationDivisor; QQ/=normalizationDivisor; qq/=normalizationDivisor;

                float x = AMP.asFloat();
                float pMin = x * ampMin, pMax = x * ampMax;
                BB = lerpSafe(BB, pMin, pMax);
                GG = lerpSafe(GG, pMin, pMax);
                QQ = lerpSafe(QQ, pMin, pMax);
                qq = lerpSafe(qq, pMin, pMax);

                for (var punc : pri)
                    punc.set(BB, GG, QQ, qq);
            }

            private float val(float goalness, float questionness) {
                return goalness + questionness;

                // Soft geometric mean with offset
                //float offset = 0.01f;  // prevents total zero-out, adjustable
                //return (float)Math.sqrt((goalness + offset) * (questionness + offset));

                // Soft maximum with controllable sharpness
                //float sharpness = 3.0f;  // adjustable, higher = sharper transition
                //return (float)(Math.log(Math.exp(goalness * sharpness) +
                //        Math.exp(questionness * sharpness)) / sharpness);

                //euclidean
                //return Fuzzy.and(goalness,questionness);

                //return (float) Math.sqrt(sqr(goalness)+sqr(questionness));
            }
        });
    }


    public void puncPri(Term r, float min, float max, PuncBag... pp) {
        assert(pp.length>0);

        float exp =
            1;
            //priActionSharp;

        floatAction(inh(r, punc(BELIEF)), exp, v ->
            { for (var p : pp) p.belief.setLerp(v, min, max); } ) ;
        floatAction(inh(r, punc(QUESTION)), exp, v ->
            { for (var p : pp) p.question.setLerp(v, min, max); });
        floatAction(inh(r, punc(GOAL)), exp, v ->
            { for (var p : pp) p.goal.setLerp(v, min, max); });
        floatAction(inh(r, punc(QUEST)), exp, v ->
            { for (var p : pp) p.quest.setLerp(v, min, max); });
    }

    public static Game meta(Reflex0 r, FloatSupplier reward) {
        var statPeriod = 1024;

        var rewardWeight = 1;
        var policyLossWeight = //0 to disable
            0.6f;
        var valueLossWeight = //0 to disable
            0.3f;
        var cycleTimeWeight = //0 to disable
            0;
            //0.4f;

        boolean learningRateControl = true;
        float learningRateDivisor = 100;

        if (policyLossWeight + rewardWeight <= 0)
            throw new IllegalStateException();

        int frameRateDivisor = 2;

        Game game = r.game(); Agent a = r.agent;
        Term i = p("meta", $.atomic(a.toString()));
        Game g = new Game(i, GameTime.frames(game, frameRateDivisor));

        if (rewardWeight > 0)
            g.reward(inh(i, "reward"), reward).weight(rewardWeight);


        if (a instanceof VPG.PGAgent pga) {
            if (cycleTimeWeight > 0) {
                var cycleTimeNormalized = new FloatNormalized(() -> (float) (pga.cycleTimeNS / 1.0e6), 0, statPeriod);
                g.reward(inh(i, "cycleTime"), cycleTimeNormalized.oneMinus()).weight(cycleTimeWeight);
            }

            AbstractPG pg = pga.pg;

            //TODO optimizer choice for next iteration
            if (pg instanceof StreamAC s){
                g.action(inh(i, "policyLr"), s.policyLr);
                g.action(inh(i, "valueLr"), s.valueLr);
                g.action(inh(i, "gamma"), s.gamma);
                g.action(inh(i, "lambda"), s.lambda);



                g.sense(inh(i, "entropy"), new FloatNormalized(() -> (float) (s.entropy), 0, statPeriod));


                g.sense(inh(i, "policyTracesL1"), new FloatNormalized(() -> (float)s.policyTraces.normL1(), 0, statPeriod));
                g.sense(inh(i, "policyLoss"), new FloatNormalized(() -> (float) (s.policyLoss), 0, statPeriod));
                g.sense(inh(i, "valueTracesL1"), new FloatNormalized(() -> (float)s.valueTraces.normL1(), 0, statPeriod));
                g.sense(inh(i, "valueLoss"), new FloatNormalized(() -> (float) (s.valueLoss), 0, statPeriod));

                g.sense(inh(i, "td"), new FloatNormalized(() -> (float) Math.abs(s.tdErr), 0, statPeriod).oneMinus());
                g.reward("happiness", ()->(float)game.happiness());
            }

            if (pg instanceof AbstractReinforce ar){

                g.sense(inh(i, "entropy"), new FloatNormalized(() -> (float) Math.abs(ar.entropyCurrent), 0, statPeriod));

                g.action(inh(i, "gamma"), ar.gamma);

                g.action(inh(i, "lambda"), ar.lambda);

                g.action(inh(i, "exploreBonus"), ar.exploreBonus);

                //g.action(inh(i, "paramNoise"), vpg.paramNoise);

                //g.action(inh(i, "actionNoise"), vpg.actionNoise);

                if (learningRateControl) {
                    var policyLearning = ar.policyLearning;
                    float alphaMin = policyLearning.asFloat() / learningRateDivisor;
                    float alphaMax = policyLearning.asFloat();
                    var alpha = Util.lerpLog(alphaMin, alphaMax);
                    g.action(inh(i, "learningRate"), x -> {
                        ar.policyLearning.set((float) alpha.valueOf(x));
                    });
                }

                if (cycleTimeWeight > 0) {
                    int episodeMax = 32, epochsMax = 1;
                    g.action(inh(i, "episodeLen"), (float x) -> ar.episode.setLerp(x, 2, episodeMax));
                    if (epochsMax > 1)
                        g.action(inh(i, "epochs"), (float x) -> ar.epochs.setLerp(x, 1, epochsMax));
                }

                var policyLossTerm = inh(i, "policyLoss");
                var valueLossTerm = inh(i, "valueLoss");
                var policyLossDeltaTerm = inh(i, "policyLossDelta");
                var valueLossDeltaTerm = inh(i, "valueLossDelta");

                FloatSupplier policyLoss = () -> (float) Math.abs(ar.policyLoss);
                FloatSupplier policyLossNorm = new FloatNormalized(policyLoss, 0, statPeriod);
                FloatSupplier policyLossDelta = new FloatNormalized(new FloatDifference(policyLoss, g::time), 0, statPeriod);

                g.sense(policyLossDeltaTerm, policyLossDelta);
                //g.sense(policyLossTerm, policyLossNorm);

                if (policyLossWeight > 0) {
                    g.reward(policyLossTerm, policyLossNorm.oneMinus()).weight(policyLossWeight);
                    //g.reward(policyLossDeltaTerm, policyLossDelta.oneMinus()).weight(lossWeight);
                } else {
                    g.sense(policyLossTerm, policyLossNorm);
                    //g.sense(policyLossDeltaTerm, policyLossDelta);
                }

                if (ar instanceof VPG vpg) {
                    FloatSupplier valueLoss = () -> (float) Math.abs(vpg.valueLoss);
                    FloatSupplier valueLossNorm = new FloatNormalized(valueLoss, 0, statPeriod);
                    FloatSupplier valueLossDelta = new FloatNormalized(new FloatDifference(valueLoss, g::time), 0, statPeriod);

                    g.sense(valueLossDeltaTerm, valueLossDelta);
                    //g.sense(valueLossTerm, valueLossNorm);

                    if (policyLossWeight > 0) {
                        g.reward(valueLossTerm, valueLossNorm.oneMinus()).weight(valueLossWeight);
                        //g.reward(valueLossDeltaTerm, valueLossDelta.oneMinus()).weight(lossWeight);
                    } else {
                        g.sense(valueLossTerm, valueLossNorm);
                        //g.sense(valueLossDeltaTerm, valueLossDelta);
                    }
                }

                if (ar instanceof PPO o) {
                    g.action(inh(i, "epsilon"), x -> {
                        o.proximal.setLerp(x, 0.01f, 0.5f);
                    });
                }
            } else if (pg instanceof DDPG) {
                throw new TODO();
            }
        }
        //TODO optional: copy r's sensor's?
        return g;
    }

    public void perception(Game g, float min, float max) {
        float sharp = 2; //more dexterity in lower values //TODO use reciprocal sclae?
        floatAction(inh(g.id, PERCEPTION), x -> {
            g.perception.perceive.set(lerpSafe(Math.pow(x, sharp), min, max));
        });
    }

    public class DurMultiplierTiming extends BasicTimeFocus {

        public final FloatRange durMultiplier = new FloatRange(DEFAULT_DUR_MULTIPLIER, 0, 16);

        {
            onFrame(()-> focus().dur(durMultiplier.asFloat() * MetaGame.this.dur()));
        }

    }

    //	private float dur(int initialDur, float d) {
//		return Math.max(1, ((d + 0.5f) * 2 * initialDur));
//	}

    //    public GoalActionConcept[] dial(Game a, Atomic label, FloatRange var, int steps) {
//        GoalActionConcept[] priAction = actionDial(
//                $.inh(id, $.p(label, $.the(-1))),
//                $.inh(id, $.p(label, $.the(+1))),
//                var,
//                steps);
//        return priAction;
//    }

    //    /** creates a base agent that can be used to interface with external controller
//     *  it will be consistent as long as the NAR architecture remains the same.
//     *  TODO kill signal notifying changed architecture and unwiring any created WiredAgent
//     *  */
//    public Agenterator agent(FloatSupplier reward, IntIntToObjectFunction<Agent> a) {
//        AgentBuilder b = new AgentBuilder(reward);
//        for (MetaGoal m : MetaGoal.values()) {
//            b.out(5, i->{
//                float w;
//                switch(i) {
//                    default:
//                    case 0: w = -1; break;
//                    case 1: w = -0.5f; break;
//                    case 2: w = 0; break;
//                    case 3: w = +0.5f; break;
//                    case 4: w = +1; break;
//                }
//                nar.emotion.want(m, w);
//            });
//        }
//
//        for (Why c : why) {
//
//            b.in(() -> {
//                float ca = c.amp();
//                return ca==ca ? ca : 0;
//            });
//
////            for (MetaGoal m : MetaGoal.values()) {
////                Traffic mm = c.credit[m.ordinal()];
////                b.in(()-> mm.current);
////            }
//            //TODO other data
//        }
//
//        for (How c : nar.how) {
//            b.in(() -> {
//                PriNode cp = c.pri;
//                return Util.unitize(cp.priElseZero());
//            });
//            //TODO other data
//        }
//
//        return b.get(a);
//    }


//    private static NAgent metavisor(NAgent a) {
//
////        new NARSpeak.VocalCommentary( nar());
//
//        //
////        nar().onTask(x -> {
////           if (x.isGoal() && !x.isInput())
////               System.out.println(x.proof());
////        });
//
//        int durs = 4;
//        NAR nar = nar();
//
//        NAgent m = new NAgent($.func("meta", id), FrameTrigger.durs(durs), nar);
//
//        m.reward(
//                new SimpleReward($.func("dex", id),
//                        new FloatNormalized(new FloatFirstOrderDifference(nar()::time,
//                                a::dexterity)).relax(0.01f), m)
//        );
//
////        m.actionUnipolar($.func("forget", id), (f)->{
////            nar.memoryDuration.setAt(Util.lerp(f, 0.5f, 0.99f));
////        });
////        m.actionUnipolar($.func("awake", id), (f)->{
////            nar.conceptActivation.setAt(Util.lerp(f, 0.1f, 0.99f));
////        });
//        m.senseNumber($.func("busy", id), new FloatNormalized(() ->
//                (float) Math.log(1 + m.nar().emotion.busyVol.getMean()), 0, 1).relax(0.05f));
////
//
////        actionUnipolar($.inh(this.nar.self(), $.the("deep")), (d) -> {
////            if (d == d) {
////                //deep incrases both duration and max target volume
////                this.nar.time.dur(Util.lerp(d * d, 20, 120));
////                this.nar.termVolumeMax.setAt(Util.lerp(d, 30, 60));
////            }
////            return d;
////        });
//
////        actionUnipolar($.inh(this.nar.self(), $.the("awake")), (a)->{
////            if (a == a) {
////                this.nar.activateConceptRate.setAt(Util.lerp(a, 0.2f, 1f));
////            }
////            return a;
////        });
//
////        actionUnipolar($.prop(nar.self(), $.the("focus")), (a)->{
////            nar.forgetRate.setAt(Util.lerp(a, 0.9f, 0.8f)); //inverse forget rate
////            return a;
////        });
//
////        m.actionUnipolar($.func("curious", id), (cur) -> {
////            curiosity.setAt(lerp(cur, 0.01f, 0.25f));
////        });//.resolution(0.05f);
//
//
//        return m;
//    }
//    public static AgentBuilder newController(NAgent a) {
//        NAR n = a.nar;
//
//        Emotion ne = n.emotion;
//        Arrays.fill(ne.want, 0);
//
//        AgentBuilder b = new AgentBuilder(
//
//                HaiQae::new,
//
//                () -> a.enabled.get() ? (0.1f + a.dexterity()) * Util.tanhFast(a.reward) /* - lag */ : 0f)
//
//                .in(a::dexterity)
//                .in(a.happy)
//
//
//
//
//                .in(new FloatNormalized(
//
//                        new FloatFirstOrderDifference(n::time, () -> n.emotion.deriveTask.getValue().longValue())
//                ).relax(0.1f))
//                .in(new FloatNormalized(
//
//                                new FloatFirstOrderDifference(n::time, () -> n.emotion.premiseFire.getValue().longValue())
//                        ).relax(0.1f)
//                ).in(new FloatNormalized(
//                                n.emotion.busyVol::getSum
//                        ).relax(0.1f)
//                );
//
//
//        for (MetaGoal g : values()) {
//            final int gg = g.ordinal();
//            float min = -2;
//            float max = +2;
//            b.in(new FloatPolarNormalized(() -> ne.want[gg], max));
//
//            float step = 0.5f;
//
//            b.out(2, (w) -> {
//                float str = 0.05f + step * Math.abs(ne.want[gg] / 4f);
//                switch (w) {
//                    case 0:
//                        ne.want[gg] = Math.min(max, ne.want[gg] + str);
//                        break;
//                    case 1:
//                        ne.want[gg] = Math.max(min, ne.want[gg] - str);
//                        break;
//                }
//            });
//        }
//
//
//
//
//
//
//
//
//
//
//
//
//        return b;
//    }

}