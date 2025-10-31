package nars.game.reward;

import jcog.Util;
import jcog.math.Intervals;
import jcog.signal.FloatRange;
import nars.*;
import nars.action.memory.Remember;
import nars.game.Game;
import nars.game.sensor.ScalarSensor;
import nars.table.BeliefTables;
import nars.table.dynamic.MutableTasksBeliefTable;
import nars.table.eternal.EternalDefaultTable;
import nars.task.SerialTask;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.AbstractMutableTruth;
import nars.truth.MutableTruth;
import nars.truth.evi.EviInterval;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;

import static java.lang.Float.NaN;
import static nars.NALTask.taskTerm;
import static nars.Op.*;

/**
 * base class for reward which represents current belief truth as the reward value
 */
public abstract class ScalarReward extends Reward {

    /**
     * each has unique stamp
     */
    private static final int goalCapacity = 1;

    /**
     * for fair ingression of goal evidence per time range
     */
    private static final boolean GOAL_EVI_DIV_BY_RANGE = false;

    /**
     * ideal (goal) reward value
     */
    public final FloatRange freq = new FloatRange(1, 0, 1);
    /**
     * adjustable goal truth
     */
    private final AbstractMutableTruth goalTruth = new MutableTruth(1, 0);

    /**
     * factor to multiple pri() for question inputs
     * avoid distracting from the goal concept.
     * 0 to disable
     *
     * TODO inversely proportional to dexterity
     * TODO move this to separate class that manages questions for all game's rewards
     */
    @Deprecated
    private final FloatRange questionProb = FloatRange.unit(
        0 //DISABLED
        //0.02f
        //0.01f
        //0.002f
    );


    public final void amp(float p) {
        sensor.sensing.amp(p);
    }

    public enum GoalFocus {

        Eternal() {
            @Override
            long[] goalOcc(Game g) {
                return new long[]{ETERNAL, ETERNAL};
            }
        },

        Focus() {
            @Override
            long[] goalOcc(Game g) {
                return g.focus().when();
                //return g.time.startEndArray();
            }
        },

        /**
         * stretches goalOccFocus to include the present
         */
        FocusRadius() {
            @Override
            long[] goalOcc(Game g) {
                var o = Focus.goalOcc(g);
                var now = g.time();

                //full radius: past AND future
                var r = Math.max(Math.abs(o[0] - now), Math.abs(o[1] - now));
                o[0] = now - r;
                o[1] = now + r;

                //half radius: past OR future
                //        if (o[0] > now) o[0] = now;
                //        if (o[1] < now) o[1] = now;

                return o;
            }
        },

        Now() {
            @Override
            long[] goalOcc(Game g) {
                return g.time.startEndArray();
            }
        },

        NowWide() {
            private static final float focusScale =
                2;
                //1;

            @Override
            long[] goalOcc(Game g) {
                return Intervals.range(g.time(), g.durFocus() * focusScale);
            }
        },

        FocusNow() {

            @Override
            long[] goalOcc(Game g) {
                return Intervals.range(g.time(), g.durFocus());
            }
        },

        /** shifted so the left is aligned with now, and right towards the future */
        FocusFuture() {
            @Override
            long[] goalOcc(Game g) {
                var s = g.time();
                return new long[] { s, s + Math.round(g.durFocus()) };
            }
        };

        abstract long[] goalOcc(Game g);
    }

    public GoalFocus goalFocus =
        //GoalFocus.Now;
        //GoalFocus.FocusNow;
        GoalFocus.FocusFuture;
        //GoalFocus.NowWide;
        //GoalFocus.Focus;
        //GoalFocus.Eternal;

    /**
     * feedback beliefs
     */
    public final ScalarSensor sensor = new RewardScalarSensor();

    /**
     * actual current reward value
     */
    public volatile float reward = NaN;

    @Deprecated
    private float _resolution = -1;

    @Nullable
    private SerialTask how, how2;

    private RewardGoalTable out;

    /**
     * for 'sparse' rewards, where a particular value is expected
     * to occurr significantly more frequently than any other value.
     * <p>
     * a value set at construction time that indicates the most 'usual'
     * or 'default' reward value to be expected, so that when it happens, the
     * ordinary behavior of creating temporal signal tasks is elided.
     * <p>
     * at the same time, an eternal 'background' truth, with the usual value
     * will be present so that it resolves the expected default value accurately.
     * <p>
     * this helps to reduce the number of tasks created but also helps to emphasize
     * the important times when the signal is 'unusual'.
     * <p>
     * disabled by default (NaN).
     */
    private float freqUsually = NaN;

    protected ScalarReward(Term id) {
        super(id);
    }

    @Override
    public final double reward() {
        return reward;
    }

    /**
     * sets the ideal (goal) freq
     */
    public final ScalarReward freq(float g) {
        freq.set(g);
        return this;
    }

    @Override
    public final void accept(Game g) {

        sensor.accept(g);

        var pri = goalPri();
        var o = goalOcc(g);

        goalReward(o, pri, g);
        questionReward(o, pri, g);
    }

    private void questionReward(long[] goalOcc, float pri, Game g) {
        var f = g.focus();
        var rng = f.random();
        if (rng.nextFloat() < questionProb.floatValue()) {
            f.remember(rng.nextBoolean() ? how : how2
                .occ(goalOcc[0], goalOcc[1])
                .setCreation(g.time())
                .withPri(pri * /* qr */ strength())
            );
        }
    }

    private void goalReward(long[] goalOcc, float pri, Game g) {
        var t = (SerialTask) rewardGoalTable().setOrAdd(goalOcc,
            goalTruth.freq(freq.floatValue()).evi(goalEvi(goalOcc, g)),
            pri, g.nar
        ).setUncreated();//g.time());
        g.focus().remember(t);//, sensor.concept);
    }

    /** priority for goals and questions */
    private float goalPri() {
        return sensor.pri.pri();
    }

    private double goalEvi(long[] goalOcc, Game g) {
        var e =
            strength() * g.nar.goalConfDefault.evi(); //scale in evi
            //c2e(conf() * g.nar.goalConfDefault.conf()); //scale in conf

        if (GOAL_EVI_DIV_BY_RANGE && goalOcc[0] != ETERNAL) {
            var goalRange = 1 + goalOcc[1] - goalOcc[0];
            var goalDurs = Math.max(goalRange / g.dur(), (float) 1);
            var ee =
                    e / goalRange;
                    //e / Math.sqrt(goalDurs);
            return Math.max(ee, g.nar.eviMin());
        } else
            return e;
    }

    private long[] goalOcc(Game g) {
        return Tense.dither(goalFocus.goalOcc(g), g.nar);
    }

    private RewardGoalTable rewardGoalTable() {
        return out;
    }

    public final ScalarReward resolution(float r) {
        sensor.freqRes(r);
        return this;
    }

    protected abstract float reward(Game a);

    @Override
    public final double happy(long start, long end, float dur) {
        var i = this.sensor;

        Truth actual;
        if (i == null) {
            actual = null;
        } else {
            //TODO ignore Eternal freqUsually if temporals exist in the interval
            actual = beliefTruth(start, end, dur,
                    sensor.concept.sensorBeliefs().sensor
                    //i.concept.beliefs()
            );
//            if (actual == null) {
//                //TODO try again, accept any belief
//                //if (NAL.belief.REWARD_SERIAL_FILTER...)
//                //actual = beliefTruth(start, end, dur, false, rewardBelief);
//            }
        }
        //* Math.min(1, b.conf() / game._confDefaultBelief)

        return actual == null ? Double.NaN :
                1 - i.truther(game).dist(goalTruth, actual);
    }

    @Nullable
    private Truth beliefTruth(long start, long end, float dur, BeliefTable rewardBelief) {
        return rewardBelief.truth(start, end,
//                null,
////                onlySerial ? t -> t instanceof SerialTask || t instanceof EternalTask : null
//               null,
////                t -> Arrays.equals(t.stamp(), rewardBelief.sharedStamp)
                dur, nar());
    }

    @Override
    public Iterable<? extends Termed> components() {
        return Collections.singleton(sensor);
        //return List.of(id);
        //return sensor.components();
    }

    /**
     * TODO not completely working yet
     */
    public final ScalarReward usually(float freq) {
        this.freqUsually = freq;
        return this;
    }

    @Override
    public void start(Game g) {
        super.start(g);

        var nar = g.nar;

        sensor.start(g);
        ((BeliefTables) sensor.concept.goals()).addFirst(out = new RewardGoalTable(nar));


        how = how(term(), QUEST, nar);
        how2 = how(taskTerm(IMPL.the($.varQuery(1), XTERNAL, term()), QUESTION), QUESTION, nar);


        if (_resolution > 0)
            sensor.freqRes(_resolution);

        if (freqUsually == freqUsually) {
            nar.runLater(() -> { //HACK
                Truth t = $.t(freqUsually, nar().beliefConfDefault.conf() * NAL.signal.REWARD_USUALLY_CONF_FACTOR);

                //for (Termed c : components()) {
                //TODO assert that it has no eternal tables already
                EternalDefaultTable.add(sensor.concept, t, nar);
                //}
            });
        }
    }

    private static SerialTask how(Term t, byte p, NAR nar) {
        var now = nar.time();
        return new SerialTask(t, p, null, now, now, nar.evidence());
    }

    private final class RewardGoalTable extends MutableTasksBeliefTable {

        RewardGoalTable(NAR nar) {
            super(ScalarReward.this.id, false, goalCapacity);
            if (NAL.signal.STAMP_SHARING)
                sharedStamp = nar.evidence();
        }

        @Override
        public void remember(Remember r) {
            if (r.input.ETERNAL()) {
                //HACK prevent eternal duplicate store in eternalbelieftable
                var first = tasks.first();
                if (first != null && Arrays.equals(first.stamp(), r.input.stamp() /*r.input.equals(first)*/))
                    r.store(first);
            }
        }

        @Override
        public Truth taskTruth(float f, double evi) {
            return new MutableTruth(f, evi);
        }

        SerialTask setOrAdd(long[] se, AbstractMutableTruth goalTruth, float pri, NAL n) {
            return setOrAdd(goalTruth.freq(), goalTruth.evi(),
                    se[0], se[1],
                    pri, n);
        }

    }

    private final class RewardScalarSensor extends ScalarSensor {

        RewardScalarSensor() {
            super(ScalarReward.this.id);
        }

        @Override
        public void accept(Game g) {
            accept(reward(
                ScalarReward.this.reward = ScalarReward.this.reward(g)), g);
        }

        /**
         * @param x current reward frequency
         */
        private float reward(float x) {
            if (x == x && freqUsually == freqUsually &&
                    Util.equals(x, freqUsually, nar().freqRes.asFloat())) {
                return NaN; //masked; absorbed by EternalTable (and also cancels any ongoing Serial stretch) */
            } else
                return x;
        }

        /**
         * prevent reward and action occurring simultaneously
         * which can confuse action prediction.
         */
        @Override
        protected EviInterval when(Game g) {
            return g.time.addDurs(NAL.temporal.GAME_REWARD_SHIFT_DURS);
        }
    }

//    public void addGuard(boolean log, boolean forget) {
//
//        ((BeliefTables) sensor.goals()).add(0, new EmptyBeliefTable() {
//            @Override
//            public void remember(Remember r) {
//                NALTask i = r.input;
//
//                float diff = abs(i.freq() - goalTruth.freq());
//                if (diff >= 0.5f) {
//                    if (log) {
//                        //logger.info("goal contradicts reward:\n{}", i.proof());
//                        System.out.print("goal contradicts reward\t");
//                        r.nar().proofPrint(i);
//                        System.out.println();
//                    }
//                    if (forget) {
//                        r.unstore(i);
//                    }
//                } else if (diff < 0.25f) {
//                    //good
////                    if (log) {
////                        //logger.info("goal contradicts reward:\n{}", i.proof());
////                        System.out.print("goal supports reward\t"); r.nar().proofPrint(i);
////                    }
//                }
//
//            }
//        });
//    }


}