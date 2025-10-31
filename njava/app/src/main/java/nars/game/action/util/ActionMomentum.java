package nars.game.action.util;

import jcog.Util;
import jcog.signal.FloatRange;
import nars.game.Game;

import java.util.function.Consumer;

import static nars.Op.GOAL;

public class ActionMomentum implements Consumer<Game> {
    public final FloatRange momentum = FloatRange.unit(0);

    private double[] prev = null;

    public ActionMomentum(float momentum) {
        this.momentum.set(momentum);
    }

    @Override
    public void accept(Game g) {
        double[] current = g.actions.snapshot();

        if (prev != null && prev.length == current.length) {
            float momentum = this.momentum.asFloat();
            for (int i = 0; i < current.length; i++) {
                var prev = this.prev[i];
                var curr = current[i];
                if (prev == prev) {
                    current[i] = curr == curr ?
                            Util.lerpSafe(momentum, curr, prev)
                            :
                            prev;
                }
            }
        }

        if (prev == null || prev.length != current.length)
            prev = new double[current.length];
        System.arraycopy(current, 0, prev, 0, prev.length);

        g.actions.setSnapshot(current, g.nar.confDefault(GOAL));
    }
}
