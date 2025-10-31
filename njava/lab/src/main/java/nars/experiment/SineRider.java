package nars.experiment;

import jcog.Fuzzy;
import nars.Player;
import nars.game.Game;
import spacegraph.space2d.widget.meter.Plot2D;

import static nars.$.$$;
import static spacegraph.SpaceGraph.window;
import static spacegraph.space2d.container.grid.Containers.col;

/** https://gist.github.com/jnorthrup/ec13fb706a27549b8b09c48e3b017103 */
public class SineRider extends Game {

    public static void main(String[] args) {
        new Player(new SineRider("s")).fps(30).start();
    }

    private static final int TOP = +1, BOTTOM = -1, OTHER = 0;
    private static final float THRESH =
            //0.9f;
            1/3f;
            //2/3f;
            //0.75f;
            //0.5f;

    float freq;

    int t = 0;
    int act = 0;
    float x = 0;

    public SineRider(String id) {
        this(id, 0.05f);
    }

    public SineRider(String id, float freq) {
        super(id);

        this.freq = freq;
        sense("x", ()->{
            x = (float) Math.sin(freq * (2*Math.PI) * (t++));
            return Fuzzy.unpolarize(x);
        });

        actionTriState($$("y"), a -> {
            //System.out.println(a);
           act = a;
           return true;
        });

        var R = reward("correct", ()->{
            return switch (act) {
                case TOP -> (x > +THRESH) ? 1 : 0;
                case BOTTOM -> (x < -THRESH) ? 1 : 0;
                case OTHER -> (x < +THRESH && x > -THRESH) ? 1 : 0;
                default -> Float.NaN; //unknown
            };
        });

        var p = new Plot2D(256).addSeries("x", () -> x);
        var q = new Plot2D(256).addSeries("y", () -> act);
        var r = new Plot2D(256).addSeries("r", () -> R.reward);
        onFrame(()->{
            p.update();
            q.update();
            r.update();
        });
        window(col(p,q,r), 500, 500);
    }
}