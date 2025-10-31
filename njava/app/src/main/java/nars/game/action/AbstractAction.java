package nars.game.action;

import jcog.TODO;
import nars.NAR;
import nars.game.FocusLoop;
import nars.game.Game;

public interface AbstractAction extends FocusLoop<Game> {


//     * TODO exclude input tasks from the calculation */
//    abstract public float dexterity(long start, long end, NAR n);

    /**
     * estimates the organic (derived, excluding curiosity) goal confidence for the given time interval
     */
    double dexterity();

    default float priGoal() { return 0; /* TODO */ }

    /** leverage / ability to change the signal; ratio: dexterity / belief_evi */
    default double malleability() {
        throw new TODO();
    }

    /** degree of freq alignment among the action's held goals at a particular time */
    default double coherency(long start, long end, float dur, NAR nar) { throw new TODO(); }

    /** degree of freq alignment between the action's beliefs and goals at a particular time */
    default double reliability(long start, long end, float dur, NAR nar) { throw new TODO(); }

    void pre(Game g);
}