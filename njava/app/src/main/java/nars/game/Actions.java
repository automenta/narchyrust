package nars.game;

import com.google.common.collect.Streams;
import jcog.TODO;
import jcog.data.list.FastCoWList;
import jcog.data.list.Lst;
import nars.*;
import nars.focus.PriAmp;
import nars.game.action.AbstractAction;
import nars.game.action.AbstractGoalAction;
import nars.game.action.AbstractGoalAction.MotorFunction;
import nars.game.action.CompoundAction;
import nars.game.action.CompoundAction.CompoundActionComponent;
import nars.term.Termed;
import nars.truth.proj.TruthProjection;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.GOAL;

public class Actions implements Iterable<AbstractAction> {

    /** goal pickup: relative shift time, in durs */
    public static final float GOAL_FOCUS_SHIFT =
        0;
        //-1; //establishes sequential-frame causal dependency
        //-0.5f;

    /**
     * TODO Map<> for O(1) dynamic add/remove
     */
    public /* TODO private */ final FastCoWList<AbstractAction> actions =
            new FastCoWList<>(AbstractAction[]::new);

    public PriAmp pri;

    public Answer goalMatch;

    @Nullable public Consumer<Game> actionProcessor = null;

    public static final boolean curiosityReviseOrReplace = false;

    /** compound actions, for their special deferred initialization */
    @Deprecated transient final List<FocusLoop<Game>> _actions = new Lst<>();

    /** all action components */
    public transient final List<AbstractAction> components = new Lst<>();

    void act(Game g) {
        matchGoal(g);

        pre(g);

        if (actionProcessor !=null)
            actionProcessor.accept(g);

        update(g);
    }

    private void pre(Game g) {
        for (var a : components) a.pre(g);
    }

    private void update(Game g) {
        for (var a : actions) a.accept(g);
        for (var a : _actions) if (a instanceof CompoundAction A) A.accept(g);
    }

    private void matchGoal(Game g) {
        if (goalMatch == null)
            goalMatch = newGoalMatch(g);

        var w = g.time;
        var shift = Math.round(GOAL_FOCUS_SHIFT * w.dur);
        goalMatch
            .time(w.s + shift, w.e + shift)
            .dur(w.dur);
    }

    private static Answer newGoalMatch(Game g) {
        return new Answer(null, true,
                NAL.answer.ACTION_ANSWER_CAPACITY, g.nar).eviMin(0);
    }

    @Nullable public TruthProjection truth(Term term, TaskTable t) {
        return t == null || t.isEmpty() ? null :
            goalMatch
                .clear()
                .match(t)
                .truthProjection();
    }

    //	/** dur sensitivity detection range */
//	public final FloatRange actionDurs = new FloatRange(1, 0, 4);


//    /**
//     * relative time when the desire is actually read (negative values mean past, ex: half duration ago),
//     * determines when desire is answered to produce feedback in the present frame.
//     * represents a natural latency between decision and effect.
//     */
//    public final FloatRange desireShift = new FloatRange(
//            0
//            //-1f
//            //-0.5f
//            //+0.5f
//            , -2, +2);


    /**
     * dexterity = mean(conf(action))
     * evidence/confidence in action decisions, current measurement
     */
    public final double dexterity() {
        var a = actions.size();
        return a == 0 ? 0 :
                dexterityMean()
                //dexterityMeanGeo(a)
        ;
    }

    private double dexterityMean() {
        return actions.meanBy(AbstractAction::dexterity);
    }

    public final double coherency(long start, long end, float dur, NAR nar) {
        return actions.meanBy(x -> x.coherency(start, end, dur, nar));
    }

    /**
     * compares the truth frequencies of the implication pair:
     * (  outcome ==>-dt action)
     * (--outcome ==>-dt action)
     * <p>
     * if both are not defined, return NaN
     * this can be used to test how polarized the belief that an outcome depends on the
     * particular value of this action.  if the two impl freq are similar, this
     * indicates the outcome is expected to depend less on this action's choice
     * than if the freq are different.
     */
    public double opinionation(Term outcome, int dt, long start, long end, float dur, NAR nar) {
        throw new TODO();
    }


//    /**
//     * avg action frustration
//     */
//    @Research
//    public final double frustration(long start, long end, float dur, NAR nar) {
//        return actions.meanBy(rr -> 1 - rr.happy(start, end, dur, nar));
//    }

    public final <A extends AbstractAction> A addAction(A c) {
        var ct = c.term();
        if (actions.OR(ct.equalsTermed()))
            throw new RuntimeException("action exists with the ID: " + ct);
        actions.add(c);
        return c;
    }

    public final int size() {
        return actions.size();
    }

    @Override
    public final Iterator<AbstractAction> iterator() {
        return actions.iterator();
    }

    @Override
    public final void forEach(Consumer<? super AbstractAction> action) {
        actions.forEach(action);
    }

    public final Stream<AbstractAction> stream() {
        return actions.stream();
    }

    public Stream<? extends Concept> components() {
        return stream().flatMap(z -> Streams.stream(z.components()))
                .filter(z -> z instanceof Concept).map(z -> ((Concept)z));
    }

    public <A extends FocusLoop<Game>> A addAction(A a) {
        if (a instanceof AbstractAction aa) {
            addAction(aa);
            components.add(aa);
        } else {
            _actions.add(a);
            for (var c : ((CompoundAction)a).concepts)
                components.add(c);
        }

        return a;
    }

    public Stream<? extends Termed> componentStream() {
        return stream().flatMap(x1 -> Streams.stream(x1.components()));
    }

    public Stream<CompoundAction> compoundActions() {
        return stream()
            .filter(z -> z instanceof CompoundActionComponent zc)
            .map(c -> ((CompoundActionComponent)c).compoundAction())
            .distinct()
        ;
    }

    public synchronized void filter(Consumer<Game> r) {
        if (actionProcessor !=null) throw new UnsupportedOperationException();
        actionProcessor = r;
    }

    public void setSnapshot(double[] action, float conf) {
        int i = 0;
        for (var a : components) {
            if (a instanceof AbstractGoalAction aa)
                aa.goalSet((float) action[i], conf);
            else
                throw new UnsupportedOperationException();
            i++;
        }
    }

    public double[] snapshot() {
        var input = new DoubleArrayList(components.size());
        for (var a : components) {
            if (a instanceof AbstractGoalAction aa) {
                double f = aa.goalInternal();
                //var f = polarize ? Fuzzy.polarize(_f) : _f;
                input.add(f);
            } else
                throw new UnsupportedOperationException();
        }
        return input.toArray();
    }



    public abstract static class ActionControl {
        public boolean enabled = false;

        /** wrap a motor function */
        abstract public MotorFunction motor(MotorFunction m, AbstractGoalAction a, Game g);
    }

    public class Overrides {
        public final Map<ActionControl, Termed> o = new LinkedHashMap<>();

        public void enable(boolean e) {
            o.keySet().forEach(a -> a.enabled = e);
        }
    }

    public final Overrides overrides = new Overrides();

    public static class PushButtonControl extends ActionControl {
        public final char keycode;
        public boolean pressed;

        public PushButtonControl(char keycode) {
            this.keycode = keycode;
        }

        @Override
        public String toString() {
            return "keypress " + (keycode == ' ' ? "SPACE" : String.valueOf(keycode));
        }

        @Override
        public MotorFunction motor(MotorFunction m, AbstractGoalAction a, Game game) {
            return (b,g)->{
                if (enabled)
                    g = $.t(pressed ? 1 : 0, game.nar.confDefault(GOAL));
                return m.apply(b, g);
            };
        }
    }
}