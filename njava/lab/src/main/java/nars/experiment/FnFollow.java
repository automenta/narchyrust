package nars.experiment;

import jcog.Fuzzy;
import nars.game.Game;
import spacegraph.space2d.widget.meter.Plot2D;

import static nars.$.$$;
import static spacegraph.SpaceGraph.window;
import static spacegraph.space2d.container.grid.Containers.col;

public class FnFollow extends Game {

    float freq = 0.1f;
    //0.75f;
    //0.5f;
    int t = 0;
    float y = 0;
    float x = 0;

    public FnFollow(String id) {
        super(id);

        sense("x", () -> {
            return x = Fuzzy.unpolarize((float) Math.sin(freq * (2 * Math.PI) * (t++)));
        });

        action($$("y"), a -> {
            //System.out.println(a);
            y = a;
        });

        var R = reward("correct", () -> {
            return 1-Math.abs(x-y);
        });

        var p = new Plot2D(256).addSeries("x", () -> x);
        var q = new Plot2D(256).addSeries("y", () -> y);
        var r = new Plot2D(256).addSeries("r", () -> R.reward);
        onFrame(() -> {
            p.update();
            q.update();
            r.update();
        });
        window(col(p, q, r), 500, 500);
    }

    public static void main(String[] args) {
        RLPlayer.run(new FnFollow("s"), 100);
    }

}