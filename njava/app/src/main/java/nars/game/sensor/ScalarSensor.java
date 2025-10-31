package nars.game.sensor;

import nars.Term;
import nars.Truth;
import nars.game.Game;
import nars.term.Termed;
import nars.truth.evi.EviInterval;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

import static jcog.Util.roundSafe;

public abstract class ScalarSensor extends AbstractSensor {

    public ScalarSignalConcept concept;
    public float value;

    protected ScalarSensor(Term id) {
        super(id);
    }

    @Override
    public void start(Game g) {
        if (concept!=null)
            throw new UnsupportedOperationException();
        g.nar.add(this.concept = conceptNew(g));
        super.start(g);
    }

    @Override
    public final int size() {
        return 1;
    }

    protected ScalarSignalConcept conceptNew(Game g) {
        return new ScalarSignalConcept(term(), g.nar);
    }

    @Override
    public abstract void accept(Game g);

    protected final void accept(float x, Game g) {
        concept.input(updateTruth(roundSafe(this.value = x, resolution()), g), sensing.pri(), when(g), g.perception);
    }

    protected EviInterval when(Game g) {
        return null;
    }

    @Nullable private Truth updateTruth(float nextValue, Game g) {
        return nextValue == nextValue ? truth(nextValue, g) : null;
    }

    @Override
    public final Iterable<? extends Termed> components() {
        return Collections.singleton(concept);
    }

}