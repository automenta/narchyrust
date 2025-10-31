package nars.experiment;

import java4k.gradius4k.Gradius4K;
import jcog.Util;
import jcog.data.list.Lst;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.NAR;
import nars.Player;
import nars.Term;
import nars.game.Game;
import nars.game.reward.Reward;
import nars.gui.sensor.VectorSensorChart;
import nars.sensor.BitmapSensor;
import nars.term.atom.Int;
import nars.video.AutoClassifiedBitmap;
import spacegraph.space2d.container.grid.Gridding;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java4k.gradius4k.Gradius4K.*;
import static nars.$.*;
import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 4/30/17.
 */
public class Gradius extends Game {

    /** framerate scale: >= 1 */
    private static final int gameSpeed =
        //2;
        //4;
        5;
        //9;
        //1;
        //4;

    static final int playerSpeed = 1;

    public static final float FPS = 25;

    private final Gradius4K g = new Gradius4K();

    private final boolean speedControl = false;
    static boolean easy = false;

    int lastScore;

    private AutoClassifiedBitmap camAE;



    public static void main(String[] args) {
        //            n.add(new Gradius($.p("g", "r")));
        //            n.add(new Gradius($.p("g", "s")));

        Gradius g = new Gradius(atomic("g"), easy);
        Player p = new Player(g).fps(FPS);
        //p.gameReflex = true;
        p.start();

        p.nar.runLater(()-> window(nars.gui.GameUI.aeUI(g.camAE), 500, 500));


    }

    static class MultiGradius {
        public static void main(String[] args) {
            int n = 3;
            Stream<Game> g = IntStream.range(0, n).mapToObj(x ->
                    new Gradius(p("g", String.valueOf((char)('a' + x)))));
            Player p = new Player(g).fps(FPS);
            p.start();
        }
    }

    public Gradius(Term id) {
        this(id, false);
    }

    public Gradius(Term id, boolean easy) {
        super(id);


        g.DIE_IF_HIT_ENEMY = g.DIE_IF_HIT_ENEMY_BULLET = g.DIE_IF_HIT_WALL = !easy;
        //g.SPEED = g.SPEED / (gameSpeed/3f);

        //cameraMulti();
        senseCamera();


        int gpsDigits =
            3;
            //1;
            //2;
            //5;
        float gpsRes = 0.05f;
        senseGPS(id, gpsDigits, gpsRes);

        actionPushButton(inh(id, "fire"), b -> { g.keys[VK_SHOOT] = b; });

        if (speedControl) {
            action(inh(id, "speed"), s -> {
                g.SPEED = Util.lerp(s, 0.1f, playerSpeed);
            }).freqRes(0.2f);
        }

        actionToggle();
        //actionBipolar();
        //actionAbsolute();

        if (!easy) {
            var alive = reward("alive", Reward.attack(() -> {
                if (g.paused)
                    return Float.NaN;
                else if (g.playerDead > 1)
                    return 0;
                else
                    return +1;
                ////Float.NaN; //+1;
            }, 10)).resolution(0.2f);//.usually(1).strength(0.75f);
        }
//        Exe.runLater(()->{
//            NAL.DEBUG = true;
//            alive.addGuard(true,false);
//        });
//        alive.setDefault($.t(1, nar.beliefConfDefault.floatValue()/2));

        reward("destroy", Reward.release(() -> {

            if (g.paused) return Float.NaN;

            int nextScore = g.score;

            float r = nextScore - lastScore;

            lastScore = nextScore;

            return r > 0 ? 1 : 0;
            //return Util.unitize(r);
            //return r!=0 ? Util.unitize(r) : Float.NaN;
        }, 6)).resolution(0.2f);//.usually(0);
        //destroy.strength(0.8f);
//        destroy.setDefault($.t(0, nar.beliefConfDefault.floatValue()/2));


        //        if (canPause) {
//            actionToggle($$("pause"),
//                    b -> g.paused = b);
//        }


        g.paused = false;
        afterFrame(()->{
            for (int i = 0; i < gameSpeed; i++)
                g.next();
        });
    }

    private void senseGPS(Term id, int gpsDigits, float gpsRes) {
        float width = g.getWidth();
        float height = g.getHeight();
        senseNumberN(inh(id, p(atomic("x"), varDep(1))), () -> g.player[OBJ_X] / width, gpsDigits).freqRes(gpsRes);
        senseNumberN(inh(id, p(atomic("y"), varDep(1))), () -> g.player[OBJ_Y] / height, gpsDigits).freqRes(gpsRes);
    }


    private void senseCamera() {
        int
            //sw = 8, sh = 8, features = 8;
            sw = 8, sh = 8, features = 10;
            //sw = 8, sh = 8, features = 6;
            //sw = 8, sh = 8, features = 5;
            //sw = 8, sh = 16, features = 9;
            //sw = 4, sh = 8, features = 7;
            //sw = 4, sh = 4, features = 16;

        int downscale =
            //4;
            2;
            //1;

        Supplier<BufferedImage> gi = () -> g.image;
        var c0 =
                new ScaledBitmap2D(gi, g.image.getWidth()/downscale, g.image.getHeight()/downscale).crop(0, 0.02f /* erase top border */, 1, 1);
                //new MonoBufImgBitmap2D(gi);

        //c0.mode(ColorMode.Hue);

        //PixelBag cc = new PixelBag(c0, 64, 64);
        //cc.Z = cc.minZoom;

        camAE = new AutoClassifiedBitmap(
                //$.inh(p(varDep(3), id), p(varDep(1), varDep(2))),
                //inh( p("see", id), p(varDep(3), p(varDep(1), varDep(2)))),
                inh( id, p(varDep(3), p(varDep(1), varDep(2)))),
                c0,
                sw, sh,
                features, this
                );

        //camAE.learnTiles = false;
        //camAE.learnRandom = camAE.tileCount();
        //((Autoencoder)camAE.ae).noise.set(0.01f);

        camAE.freqRes(
            //0.02f
            0.1f
            //0.02f
            //0.5f
            //1
        );

    }

    private void cameraMulti() {
        int dx = 2, dy = 2;
        int px = 24, py = 24;
//        int dx = 3, dy = dx;
//        int px = 12, py = px;


        assert px % dx == 0 && py % dy == 0;

//        {
//            PixelBag retina = new PixelBag(new MonoBufImgBitmap2D(() -> g.image), px, py) {
//                @Override
//                protected float missing() {
//                    return 0;
//                }
//            };
////            retina.addActions(id,this, false, false, true);
////            onFrame(()->{
////               retina.setXRelative(g.player[OBJ_X] / g.getWidth());
////                retina.setYRelative(g.player[OBJ_Y] / g.getHeight());
////            });
//            Bitmap2DSensor sensor = new Bitmap2DSensor(id, new BrightnessNormalize(retina), nar);
//            addCamera(sensor);
//            retina.addActions(id, this);
//            window(new VectorSensorView(sensor, this).withControls(), 400, 400);
//        }

        List<BitmapSensor> cams = new Lst();
        //AttNode camGroup = addGroup(cams)


//            Atomic big = Atomic.atom("C");
//            Atomic small = Atomic.atom("c");

        Supplier<BufferedImage> cc = () -> g.image;

        for (int i = 0; i < dx; i++) {
            for (int j = 0; j < dy; j++) {
                int ii = i, jj = j;
                cams.add(senseCamera((x, y) ->
                    //$.inh(id, $.p(Int.the(ii), Int.the(jj), $.p($.the(x), $.the(y)))),
                    //$.inh($.p(id, Int.the(ii), Int.the(jj)), $.p($.the(x), $.the(y))),
                    inh(p(id, Int.i(ii), Int.i(jj)), p(the(x), the(y))),

                    new ScaledBitmap2D(cc, px, py)
                        .crop(
                            (float) i / dx, (float) j / dy,
                            (float) (i + 1) / dx, (float) (j + 1) / dy)
                         )
//                            .model(
//                                //new QueueVectorSensorAttention(0.5f)
//                                new ChunkedVectorSensorAttention(1f, 2)
//                            )
                    );
            }
        }

        cams.forEach(c->c.freqRes(0.1f));

        window(new Gridding(
                cams.stream().map(c -> new VectorSensorChart(c, this).withControls())), 400, 400);
    }

    @Override
    protected void stopping(NAR nar) {
        g.paused = true;
        super.stopping(nar);
    }

    void actionToggle() {
        //TODO boundary feedback


        Term left =  inh(id, p("go", p(-1,0)));
        Term right =  inh(id, p("go", p(+1,0)));
        Term down =  inh(id, p("go", p(0,+1)));
        Term up =  inh(id, p("go", p(0,-1)));
        boolean[] k = g.keys;
        actionToggle(left, right,
              b -> k[VK_LEFT] = b,
              b -> { k[VK_RIGHT] = b; }
        );
        actionToggle(down, up,
              b -> k[VK_DOWN] = b,
              b -> { k[VK_UP] = b;   }
        );
    }

    private void actionAbsolute() {
        boolean[] k = g.keys;
        actionPIDStep(inh(id, "x"), ()->g.playerX, action -> {
            switch (action) {
                case -1 -> { k[VK_LEFT] = true; k[VK_RIGHT] = false; }
                case 0  -> k[VK_LEFT] = k[VK_RIGHT] = false;
                case +1 -> { k[VK_LEFT] = false; k[VK_RIGHT] = true; }
            }
        }, 13, 243, ()->g.SPEED*2).freqRes(0.02f);

        actionPIDStep(inh(id, "y"), ()->g.playerY, action -> {
            switch (action) {
                //reversed: up -, down +
                case +1 -> { k[VK_DOWN] = true; k[VK_UP] = false; }
                case 0  -> k[VK_DOWN] = k[VK_UP] = false;
                case -1 -> { k[VK_DOWN] = false; k[VK_UP] = true; }
            }
        }, 11, 220, ()->g.SPEED*2).freqRes(0.02f);

    }

    @Deprecated void actionBipolar() {
        float thresh = 0.1f;
        actionBipolar(inh(p(id, "x"), varDep(1)), g.keys,
                VK_LEFT, VK_RIGHT, thresh);
        actionBipolar(inh(p(id, "y"), varDep(1)), g.keys,
                VK_UP,   VK_DOWN,  thresh);
    }




}