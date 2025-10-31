package nars.game.sensor;

import nars.Truth;
import nars.game.FocusLoop;
import nars.game.Game;
import nars.truth.Truther;
import org.jetbrains.annotations.Nullable;

/** TODO range (yMin, yMax) metadata */
public interface Sensor extends FocusLoop<Game> {

    default Truther truther() { return null; }

    default Truther truther(Game g) {
        Truther t = truther();
        return t == null ? g.truther() : t;
    }

    /** warning: the result here could be a shared MutableTruth instance */
    @Nullable default Truth truth(float x, Game g) {
        return truther(g).truth(x, resolution());
    }

    Sensor freqRes(float r);


}