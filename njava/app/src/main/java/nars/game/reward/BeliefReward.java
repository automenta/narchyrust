package nars.game.reward;

import nars.Term;
import nars.game.Game;

/** reward detrmined by a belief state  */
public class BeliefReward extends ScalarReward {

    public BeliefReward(Term id, Game g) {
        super(id);
    }

    @Override
    protected float reward(Game a) {
        var w = a.time;
        return sensor.concept.beliefs().freq(w.s, w.e, w.dur, nar());
    }

}