package nars.experiment;

import nars.$;
import nars.Player;
import nars.gui.sensor.VectorSensorChart;
import nars.term.atom.Atomic;
import spacegraph.SpaceGraph;

import static nars.$.$$;
import static nars.experiment.Tetris.tetris_height;
import static nars.experiment.Tetris.tetris_width;

public class Chimera {

    static final float FPS = 50;

    public static void main(String... args) {   //potential boredom without the down button.
//        System.setProperty("tetris.fall.rate", "5");
//        System.setProperty("tetris.can.fall", "true");
//        //survival only.
//        System.setProperty("tetris.use.density", "false");
//        //dot
//        System.setProperty("tetris.easy", "true");
//            reduce con io
        System.setProperty("avg.err", "false");

        new Player(FPS, n -> {


            var g = new Gradius($.atomic("g"));
            n.add(g);

            ArkaNAR a = new ArkaNAR($$("noid"), true, false, 20, 16);
            a.ballSpeed.set( 0.7f * a.ballSpeed.floatValue() );
            n.add(a);


            n.runLater(()-> SpaceGraph.window(
                new VectorSensorChart(a.cc, a).withControls(), 800, 800));

            var x = new NARio();
            n.add(x);

            var t = new Tetris(Atomic.atom("tetris"), tetris_width, tetris_height);
            n.add(t);
            n.runLater(()-> SpaceGraph.window(new VectorSensorChart(t.vision, t).withControls(), 400, 800));

        }).start();

    }
}