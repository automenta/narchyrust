package nars.game.action;

import jcog.Util;
import nars.NAL;
import nars.NAR;
import nars.Term;
import nars.Truth;
import nars.game.Game;
import nars.game.sensor.ScalarSensor;
import nars.game.sensor.ScalarSignalConcept;
import nars.game.sensor.SignalConcept;
import nars.table.BeliefTables;
import nars.table.eternal.EternalDefaultTable;
import nars.truth.AbstractMutableTruth;
import nars.truth.MutableTruth;
import nars.truth.evi.EviInterval;
import nars.truth.proj.MutableTruthProjection;
import nars.truth.proj.TruthProjection;
import nars.truth.util.Revision;
import org.jetbrains.annotations.Nullable;

import static nars.Op.GOAL;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 * <p>
 * Goal/Belief Feedback Loop:
 * <patham9_> when a goal task is processed, the following happens: In order to decide on whether it is relevant for the current situation, at first it is projected to the current time, then it is revised with previous "desires", then it is checked to what extent this projected revised desire is already fullfilled (which revises its budget) , if its below satisfaction threshold then it is pursued, if its an operation it is additionally checked if
 * <patham9_> executed
 * <patham9_> the consequences of this, to give examples, are a lot:
 * <patham9_> 1 the system wont execute something if it has a strong objection against it. (example: it wont jump down again 5 meters if it previously observed that this damages it, no matter if it thinks about that situation again or not)
 * <patham9_> 2. the system wont lose time with thoughts about already satisfied goals (due to budget shrinking proportional to satisfaction)
 * <patham9_> 3. the system wont execute and pursue what is already satisfied
 * <patham9_> 4. the system wont try to execute and pursue things in the current moment which are "sheduled" to be in the future.
 * <patham9_> 5. the system wont pursue a goal it already pursued for the same reason (due to revision, it is related to 1)
 * <p>
 *  TODO goal truth bias parameter
 */
public abstract class AbstractGoalAction extends ScalarSensor implements AbstractAction {


    /**
     * current belief state
     */
    private final AbstractMutableTruth beliefTruth = new MutableTruth();

    /**
     * current action state, including curiosity
     */
    public final AbstractMutableTruth goalTruth = new MutableTruth();

    /**
     * current action state excluding curiosity
     */
    public final AbstractMutableTruth goalTruthOrganic = new MutableTruth();

    /** current goal value, in [0..+1] */
    protected float goal = Float.NaN;

    private static final boolean DEX_SMOOTH = false;

    public boolean trace = false;

    /**
     * updated only if tracing is enabled
     */
    @Nullable private transient TruthProjection goalReason;

    private float priGoal = Float.NaN;

    private double dex = 0;

    /** current curiosity goal */
    @Nullable private final AbstractMutableTruth curi = new MutableTruth();


    protected AbstractGoalAction(Term id) {
        super(id);
    }

//    private static Truth truth(Lst<NALTask> t, float dur, Game g) {
//        if (t.isEmpty())
//            return null;
//
//        Moment a = g.actions.goalMatch;
//        IntegralTruthProjection p = new IntegralTruthProjection(a.start(), a.end(), t.size());
//        p.dur(dur);
//        p.addAll((Collection<? extends NALTask>) t);
//        return truth(p);
//    }

    private static @Nullable Truth truth(@Nullable TruthProjection t) {
        if (t instanceof MutableTruthProjection mt)
            mt.curve = false;

        return t != null ? t.truth() : null;
    }


    @Override
    public void pre(Game g) {
        if (computeBeliefTruth()) beliefTruth(g);

        var truthDur = g.focus().durSys;
        goalTruth(g,
            truthDur > 0 ?
                    new float[] { truthDur, truthDur/2, 0 } :
                    new float[] { 0 }
            //truthDur, 0
        );
    }

    public void goalSet(float f, float c) {
        this.goalTruth.freq(this.goal = Util.unitize(f)).conf(c);
    }

    @Override
    public void accept(Game g) {
        update(beliefTruth.ifIs(), goalTruth.ifIs(), g);
        goal(goal(g), g);
    }

    protected Truth goal(Game g) {
        return g.truther().truth(goal(goal), resolution());
    }

    protected float goal(float m) {
        return m;
    }

    @Override
    protected ScalarSignalConcept conceptNew(Game g) {
        var n = g.nar;
        var c = new ScalarSignalConcept(id,
            SignalConcept.beliefTable(id, true, true, false, n.conceptBuilder),
            new BeliefTables(),
        n);

        ((BeliefTables) c.goals()).add(

                n.conceptBuilder.temporalTable(id, false)

                //new NavigableMapBeliefTable()
            /*{
                @Override
                public void remember(Remember r) {
                    if (capacity() <= (1+taskCount()) && !map.containsKey(r.input))
                        Util.nop(); //full
                    super.remember(r);
                    if (r.stored==null) {
                        System.err.println("rejected: " + r.input);
                        System.err.println("\t" + this.summary());
                    }
                }

                @Override
                protected int merge(MutableTruthProjection t, @Nullable NALTask victim, boolean removeVictim) {
                    return super.merge(t, victim, removeVictim);
                }
            }
             */
        );

        return c;
    }

    protected abstract void update(@Nullable Truth beliefTruth, @Nullable Truth goalTruth, Game g);

    public EternalDefaultTable goalDefault(Truth t, NAR n) {
        return EternalDefaultTable.add(concept, t, GOAL, n);
    }

    @Nullable
    public final TruthProjection reason() {
        if (!trace) throw new UnsupportedOperationException("trace must be enabled");
        return goalReason;
    }

    @Override public final float priGoal() {
        return priGoal;
    }

    @Override
    public final double dexterity() {
        return dex;
    }

    @Override
    public double coherency(long start, long end, float dur, NAR nar) {
        return concept.goals().coherency(start, end, dur, nar);
    }

    /**
     * whether to supply a belief truth measurement during update
     */
    protected boolean computeBeliefTruth() {
        return false;
    }

    protected final void goal(@Nullable Truth nextFeedback, Game g) {
        @Nullable EviInterval w = g.time.addDurs(NAL.temporal.GAME_ACTION_SHIFT_DURS);
        concept.input(nextFeedback, sensing.pri(), w, g.perception);

//        if (NAL.signal.ACTION_GOAL_LINK && concept.next!=null)
//            g.focus().activate(concept.term(), GOAL,
//                concept.next.priElseZero() / Util.max(1, ACTION_SLEEP_DURS_MAX));
    }

    @Nullable
    private AbstractMutableTruth beliefTruth(Game g) {
        return beliefTruth.set(truth(g.actions.truth(concept.term, concept.beliefs())));
    }

    /**
     * @param truthDurs a monotonically decreasing array of durations
     */
    private AbstractMutableTruth goalTruth(Game g, float... truthDurs) {
        assert(truthDurs.length > 0);

        var goals = concept.goals();
        var term = concept.term;

        Truth t = null;
        for (int i = 0, n = truthDurs.length; i < n; i++) {
            var truthDur = truthDurs[i];

            var ti = goalTruth(
                g.actions.truth(term, goals)
            );

            if (i == 0)
                dex = ti!=null ? ti.conf() : 0; //set dexterity based on the first truthDur

            if (ti==null)
                break; //give up

            t = ti; //refine
        }

        goalTruthOrganic.set(t);

        return goalTruth.set(truth(g, t));
    }

    private Truth truth(Game g, Truth organicTruth) {
        var curiosityTruth = curiosity(g);
        return g.actions.curiosityReviseOrReplace ?
            Revision.revise(curiosityTruth, organicTruth) :
            (curiosityTruth.is() ? curiosityTruth : organicTruth);
    }

    private AbstractMutableTruth curiosity(Game g) {
        g.curiosity.curiosity(curi, g);
        return curi;
    }

    @Nullable private Truth goalTruth(@Nullable TruthProjection x) {
        Truth organicTruth;

        if (x != null) {
            organicTruth = truth(x);
            priGoal = (float) x.priWeighted();
        } else {
            organicTruth = null;
            priGoal = 0; //Double.NaN;
        }

        if (trace) {
            goalReason = x;
        } else {
            if (x != null) x.delete();
            goalReason = null;
        }
        return organicTruth;
    }

    public float goalInternal() {
        var gt = goalTruth;
        return gt.is() ? gt.freq() : Float.NaN;
    }

    /**
     * determines the feedback belief when desire or belief has changed in a MotorConcept
     * implementations may be used to trigger procedures based on these changes.
     * normally the result of the feedback will be equal to the input desired value
     * although this may be reduced to indicate that the motion has hit a limit or
     * experienced resistence
     *
     * @param desired  current desire - null if no desire Truth can be determined
     * @param believed current belief - null if no belief Truth can be determined
     * @return truth of a new feedback belief, or null to disable the creation of any feedback this iteration
     */
    @FunctionalInterface
    public interface MotorFunction {

        float apply(@Nullable Truth believed, @Nullable Truth desired);

    }

}