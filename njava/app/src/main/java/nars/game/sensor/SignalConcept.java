package nars.game.sensor;

import jcog.math.FloatSupplier;
import nars.*;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import nars.concept.util.ConceptBuilder;
import nars.game.util.Perception;
import nars.table.dynamic.SensorBeliefTables;
import nars.table.dynamic.SerialBeliefTable;
import nars.table.dynamic.TaskSeriesSeriesBeliefTable;
import nars.table.dynamic.TruthCurveBeliefTable;
import nars.table.question.QuestionTable;
import nars.task.SerialTask;
import nars.truth.MutableTruth;
import nars.truth.evi.EviInterval;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;


/**
 * primarily a collector for belief-generating time-changing (live) input scalar (1-d real value) signals
 *
 *
 * In vector analysis, a scalar quantity is considered to be a quantity that has magnitude or size, but no motion. An example is pressure; the pressure of a gas has a certain value of so many pounds per square inch, and we can measure it, but the notion of pressure does not involve the notion of movement of the gas through space. Therefore pressure is a scalar quantity, and it's a gross, external quantity since it's a scalar. Note, however, the dramatic difference here between the physics of the situation and mathematics of the situation. In mathematics, when you say something is a scalar, you're just speaking of a number, without having a direction attached to it. And mathematically, that's all there is to it; the number doesn't have an internal structure, it doesn't have internal motion, etc. It just has magnitude - and, of course, location, which may be attachment to an object.
 * http://www.cheniere.org/misc/interview1991.htm#Scalar%20Detector
 */
public class SignalConcept extends TaskConcept implements PermanentConcept {

    public transient SerialTask next;

    private long activationPrev;
    private float changeAccumulated = 0;

//    @Deprecated private long nextRange = 0;

    public SignalConcept(Term term, boolean vector, NAR n) {
        this(term,
            beliefTable(term, true, true, vector, n.conceptBuilder),
            beliefTable(term, false, false, false, n.conceptBuilder),
            n
        );
    }

    public SignalConcept(Term term, BeliefTable beliefTable, BeliefTable goalTable, NAR n) {
        super(term,
            beliefTable,
            goalTable,
            questionTable(term, true, n),
            questionTable(term, false, n)
        );
        n.add(this);
        activationPrev = n.time() - 1;
    }

    static BeliefTable beliefTable(Term term, boolean beliefOrGoal, boolean sensor, NAR n) {
        return beliefTable(term, beliefOrGoal, sensor, false, n.conceptBuilder);
    }

    public static BeliefTable beliefTable(Term term, boolean beliefOrGoal, boolean sensor, boolean vector, ConceptBuilder c) {
        return sensor ?
                new SensorBeliefTables(c.temporalTable(term, beliefOrGoal),
                    (vector ? NAL.signal.VECTOR_BELIEF_TABLES_CLASSIC : NAL.signal.SENSOR_BELIEF_TABLES_CLASSIC) ?
                        sensorTableClassic(term, beliefOrGoal) :
                        sensorTableCurve(term, beliefOrGoal)
                )
                :
                c.beliefTable(term, beliefOrGoal);
    }

    private static TruthCurveBeliefTable sensorTableCurve(Term term, boolean beliefOrGoal) {
        return new TruthCurveBeliefTable(term, beliefOrGoal);
    }

    private static TaskSeriesSeriesBeliefTable sensorTableClassic(Term term, boolean beliefOrGoal) {
        return new TaskSeriesSeriesBeliefTable(term, beliefOrGoal, NAL.signal.SIGNAL_BELIEF_TABLE_SERIES_CAPACITY);
    }

    private static QuestionTable questionTable(Term term, boolean beliefOrGoal, NAR n) {
        return n.conceptBuilder.questionTable(term, beliefOrGoal);
    }

    private final MutableTruth valuePrev = new MutableTruth();

    /** @param w if null uses Perception's default SerialUpdater, otherwise non-null is used to specify a special time for this input */
    public final void input(@Nullable Truth next, float pri, @Nullable EviInterval w, Perception p) {
        var u = updater(w, p);
        input(sensorBeliefs().add(next, u), next, u.w, pri, p);
        valuePrev.set(next);
    }

    private static SerialBeliefTable.SerialUpdater updater(@Nullable EviInterval w, Perception p) {
        return w == null ? p.updater : new SerialBeliefTable.SerialUpdater(w, p.nar());
    }

    private void input(SerialTask next, @Nullable Truth t, EviInterval w, float pri, Perception p) {
        if (next == null) {
            p.remove(this);
        } else {
            next.setUncreated().pri(pri);
            p.put(this, pri, changeAccumulated += changeInc(t, valuePrev), durSince(w));
        }
        this.next = next;
    }

    private float durSince(EviInterval w) {
        return (w.s - activationPrev) / w.dur;
    }

    private float changeInc(@Nullable Truth value, Truth prev) {
        float changeInc;
        if (prev == null) {
            changeInc = 1;
        } else {
            var delta = Math.abs(delta(prev, value));
            changeInc =
                delta < NAL.truth.FREQ_EPSILON ? 0 : 1;
                //delta < NAL.truth.FREQ_EPSILON ? 0 : Util.lerpSafe(delta, 0.5f, 1);
                //delta;
        }
        return changeInc;
    }

    private static float delta(Truth prev, Truth next) {
//        if (prev == next) {
//            if (prev instanceof TruthCurve c) {
//                int d = Math.round(w.dur);
//                long s = w.s, e = w.e;
//                return c.freq(s, e) - c.freq(s - d, e - d);
//            } else
//                return 0;
//        } else
            return prev.freq() - next.freq();
    }


    public final SensorBeliefTables sensorBeliefs() {
        return (SensorBeliefTables) beliefs();
    }

    public final void remember(Focus w, long now) {
        if (this.next!=null) {
            w.remember(this.next);
            activationPrev = now;
            changeAccumulated = 0;
        }
    }


    /**
     * update directly with next value
     */
    public static Function<FloatSupplier, FloatFloatToObjectFunction<Truth>> SET = (conf) ->
        ((prev, next) -> next == next ? $.t(next, conf.asFloat()) : null);
    /**
     * first order difference
     */
    public static Function<FloatSupplier, FloatFloatToObjectFunction<Truth>> DIFF = (conf) ->
        ((prev, next) -> ($.t(next == next ? prev == prev ? (next - prev) / 2f + 0.5f : 0.5f : 0.5f, conf.asFloat())));


    @Nullable public final Truth truth() {
        var v = next;
        return v == null ? null : v.truth();
    }

    public final float freq() {
        var t = truth();
        return t!=null ? t.freq() : Float.NaN;
    }

    public byte punc() {
        var v = next;
        return v != null ? v.punc() : 0;
    }

//    /** reverse-estimates an input value from beliefs (at any target time) */
//    public double predict(long s, long e) {
//        throw new TODO();
//    }

}