package nars.game.action;

import jcog.Util;
import nars.Term;
import nars.Truth;
import nars.game.Game;
import nars.game.sensor.AbstractSensor;
import nars.term.Termed;
import nars.truth.AbstractMutableTruth;
import nars.truth.MutableTruth;
import org.jetbrains.annotations.Nullable;

import static java.util.Collections.EMPTY_LIST;
import static nars.Op.SETe;

/**
 * a vector-determined action
 */
public abstract class CompoundAction extends AbstractSensor {

    public final CompoundActionComponent[] concepts;

//    private float momentum = 0;

    private CompoundAction(Term id, CompoundActionComponent[] concepts) {
        super(id);
        this.concepts = concepts;
        for (var c : this.concepts) c._action = this; //HACK
    }

    protected CompoundAction(Term... ids) {
        this(SETe.the(ids),
                Util.map(CompoundActionComponent::new,
                        new CompoundActionComponent[ids.length], ids)
        );
    }

    @Override
    public final Iterable<? extends Termed> components() {
        return EMPTY_LIST;
        //return ArrayIterator.iterable(concepts);
    }

    @Override
    public final void start(Game g) {
        super.start(g);

        g.nar.causes.newCause(term());

//        for (CompoundActionComponent a : concepts) {
//            //a.sensing = sensing;
//            g.actions.addAction(a);
//        }
    }

//    /**
//     * TODO in terms of low-pass filter frequency (game durs)
//     */
//    public CompoundAction momentum(float m) {
//        this.momentum = m;
//        return this;
//    }

    /**
     * to be called by 'accept()'
     */
    protected final void set(Game g, Truth... t) {
        if (t.length != concepts.length)
            throw new ArrayIndexOutOfBoundsException();

        var freqRes = resolution();
        for (var i = 0; i < t.length; i++)
            set(t, i, freqRes);
    }

    private void set(Truth[] t, int i, float freqRes) {
        var c = concepts[i];

        var cs = c.sensing;
        cs.freqRes = sensing.freqRes;
        cs.truther = sensing.truther;

        var componentTruth = c.truth;
        componentTruth.set(t[i]); //c.setLerp(t[i], momentum);
        componentTruth.freqRes(componentTruth.freq(), freqRes);
    }

    public void pre(Game g) {
        for (var c : concepts)
            c.pre(g);
    }

    public static final class CompoundActionComponent extends AbstractGoalAction {

        /**
         * feedback
         */
        private final AbstractMutableTruth truth = new MutableTruth().clear();
        private CompoundAction _action;

        CompoundActionComponent(Term id) {
            super(id);
            //poles=1;
        }

        public CompoundAction compoundAction() {
            return _action;
        }

        @Override
        protected Truth goal(Game g) {
            return truth;
        }

        @Override
        protected void update(@Nullable Truth beliefTruth, @Nullable Truth goalTruth, Game g) {

        }

    }
}