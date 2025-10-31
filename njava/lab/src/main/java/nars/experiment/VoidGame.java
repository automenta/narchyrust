package nars.experiment;

import nars.game.Game;

/** goal is to do nothing.  see if it learns to set throttle to zero (energy efficience), out of boredom
 *  TODO
 * */
public abstract class VoidGame extends Game {

    protected VoidGame(String id) {
        super(id);
    }

}