package nars.experiment;

import jcog.math.FloatSupplier;
import jcog.signal.wave2d.Bitmap2D;
import jcog.signal.wave2d.LensDistortion2D;
import jcog.signal.wave2d.ScaledBitmap2D;
import jcog.signal.wave2d.WrappedBitmap2D;
import nars.$;
import nars.Player;
import nars.Term;
import nars.experiment.pacman.PacmanGame;
import nars.experiment.pacman.maze.Maze;
import nars.game.Game;
import nars.game.GameTime;
import nars.game.reward.Reward;
import nars.game.sensor.AbstractSensor;
import nars.gui.sensor.VectorSensorChart;
import nars.sensor.BitmapSensor;
import nars.sensor.PixelBag;
import nars.video.AutoClassifiedBitmap;
import nars.video.SwingBitmap2D;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;

import static nars.$.$$;
import static spacegraph.SpaceGraph.window;

public class Pacman extends Game {

    private final PacmanGame g = new PacmanGame();

    static float fps =
        50;
        //30;
        //25;
        //50;
        //10;

    private final AutoClassifiedBitmap ae;

    /** prevents pacman getting stuck (ex: if maze is mostly emptied). 0 to disable */
    float resetProb =
        //0;
        1/700f;

    static final boolean autoencoder = true;
    static final boolean lensDistortion = false;
    static final boolean brightnessNormalize = true;

    private BitmapSensor see;

    public Pacman(String id) {
        //super($$(id), GameTime.fps(fps));
        super($$(id), GameTime.durs(1));
        int w, h;
        if (autoencoder) {
            //higher-res pre-code
            //w = h = 200;
            //w = h = 128;
            w = h = 96;
            //w = h = 64;
        } else {
            w = h = 64;
        }

        var gView = new ScaledBitmap2D(new SwingBitmap2D(g.view), w, h);

        var cam0 = new WrappedBitmap2D(gView) {
            @Override
            public void updateCenter() {
                setCenter(
                    (float) (w * (1 - g.player.x / g.maze.width) - 2),
                    (float) (h * (1 - g.player.y / g.maze.height) - 2)
                );
            }
        };
        Bitmap2D b = lensDistortion ? new LensDistortion2D(cam0) : cam0;
        //b = brightnessNormalize ? new BrightnessNormalize(b) : b;

        if (autoencoder) {

//            gView.mode(ColorMode.Hue);

            b = new PixelBag(b, b.width(), b.height());
            ((PixelBag)b).setZoom(0.7f);
            //((PixelBag)b).setMaxZoom(2);


            var states =
                7;
                //12;
            ae = new AutoClassifiedBitmap(this.id, b,
                //8, 8, 20,
                8, 8, states,
                //16, 16, states,
                //4, 4, states,
            this);

            ae
                .freqRes(0.04f);
            //ae.learnRandom = ae.tileCount();

        } else {
            ae = null;
            onFrame(b::updateBitmap);

            AbstractSensor c = senseCamera((x, y) ->
                            //$.inh($.p(id, "see"), $.p(x, y)),
                            $.inh(id, $.p("see", $.p(x, y))),
                    b/*, 0*/);
//                .model(
//                        //new QueueVectorSensorAttention(0.5f)
//                        new ChunkedVectorSensorAttention(1f, 2)
//                );
            this.see = (BitmapSensor)c;
            c.freqRes(0.02f);
        }

//        for (MonoBufImgBitmap2D.ColorMode cm : new MonoBufImgBitmap2D.ColorMode[]{
//                MonoBufImgBitmap2D.ColorMode.R,
//                MonoBufImgBitmap2D.ColorMode.G,
//                MonoBufImgBitmap2D.ColorMode.B
//        }) {
//            Bitmap2DSensor c = senseCamera(
//                    (x,y)->$.func((Atomic)id, $.the(cm.name()), $.the(x), $.the(y)),
//                    camScale.filter(cm)
//            );
//
//            VectorSensorView v = new VectorSensorView(c, this);
////            onFrame(v::update);
//            gg.add(v/*.withControls()*/);
//            c.resolution(0.1f);
//        }
        /*, 0*/

        //        SpaceGraph.window(gg, 300, 300);

        afterFrame((G)->{
            if (G.rng().nextBoolean(resetProb))
                g.reset();
        });

        boolean[] keys = g.keys;

        Term L = $.inh(id, $.p("go", $.p(-1, 0)));
        Term R = $.inh(id, $.p("go", $.p(+1, 0)));
        Term U = $.inh(id, $.p("go", $.p(0, +1)));
        Term D = $.inh(id, $.p("go", $.p(0, -1)));
        actionToggle(L, R, (BooleanProcedure)
            p -> toggleKey(keys, p, 0, 1, 2, 3, Maze.Direction.left),
            p -> toggleKey(keys, p, 1, 0, 2, 3, Maze.Direction.right)
        );
        actionToggle(U, D, (BooleanProcedure)
            p -> toggleKey(keys, p, 2, 3, 0, 1, Maze.Direction.up),
            p -> toggleKey(keys, p, 3, 2, 0, 1, Maze.Direction.down)
        );
//        actionTriState($.p(id,$.p($.the("x"), $.varQuery(1))), (dh) -> {
//            switch (dh) {
//                case +1:
//                    g.keys[1] = true;
//                    g.keys[0] = false;
//                    break;
//                case -1:
//                    g.keys[0] = true;
//                    g.keys[1] = false;
//                    break;
//                case 0:
//                    g.keys[0] = g.keys[1] = false;
//                    break;
//            }
//        });
//
//        actionTriState($.p(id,$.p($.the("y"), $.varQuery(1))), (dh) -> {
//            switch (dh) {
//                case +1:
//                    g.keys[2] = true;
//                    g.keys[3] = false;
//                    break;
//                case -1:
//                    g.keys[3] = true;
//                    g.keys[2] = false;
//                    break;
//                case 0:
//                    g.keys[2] = g.keys[3] = false;
//                    break;
//            }
//        });


        reward("alive", ()->{
            return g.dying ? 0 : 1;
        });//.usually(1);

        beforeFrame(g::update);

        //TODO multiple reward signals: eat, alive, dist->ghost (cheat)
        final FloatSupplier s = () -> {

            int nextScore = g.score;

            float r = (nextScore - lastScore);
//            if(r == 0)
//                return Float.NaN;


            lastScore = nextScore;
            return r > 0 ? +1 : 0;
            //            else if (r < 0) return 0;
//            else
//                return 0.5f;
            //return Float.NaN;

        };
        reward("score",
            //s
            Reward.release(s, 3)
        ).resolution(0.25f);//.usually(0);

    }

    private boolean toggleKey(boolean[] keys, boolean pressed, int a, int b, int c, int d, Maze.Direction dir) {
        if (pressed) {
            keys[a] = true;
            keys[b] = false;
            keys[c] = keys[d] = false;
            return !g.player.walled(dir);
        } else {
            keys[a] = false;
            return false;
        }
    }

    int lastScore;


    public static void main(String[] args) {

        final Pacman pac = new Pacman("pac");

        Player p = new Player(
                pac
                //new Pacman("(pac,p)"),new Pacman("(pac,q)")
            ).fps(fps).start();


        p.nar.runLater(()->{
            window(nars.gui.GameUI.aeUI(pac.ae), 500, 500);

            p.the(Pacman.class).forEach(a -> {
                if (a.see!=null)
                    window(new VectorSensorChart(a.see, a).withControls()
                            , 400, 400);
            });

        });

    }

}