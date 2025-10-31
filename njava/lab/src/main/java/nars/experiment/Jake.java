package nars.experiment;

import jake2.Defines;
import jake2.Jake2;
import jake2.client.CL_input;
import jake2.client.Key;
import jake2.game.EntHurtAdapter;
import jake2.game.PlayerView;
import jake2.game.edict_t;
import jake2.sound.jsound.SND_JAVA;
import jake2.sys.IN;
import jcog.math.normalize.FloatNormalized;
import jcog.signal.buffer.CircularFloatBuffer;
import jcog.signal.wave2d.BrightnessNormalize;
import jcog.signal.wave2d.ColorMode;
import jcog.signal.wave2d.ImageFlip;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.$;
import nars.Player;
import nars.game.Game;
import nars.game.sensor.DemultiplexedScalarSensor;
import nars.game.sensor.FreqVectorSensor;
import nars.gui.sensor.VectorSensorChart;
import nars.sensor.BitmapSensor;
import nars.sensor.PixelBag;
import nars.video.AutoClassifiedBitmap;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import spacegraph.SpaceGraph;
import spacegraph.space2d.meta.obj.ObjectSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.video.GLScreenShot;
import toxi.math.MathUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static jake2.Globals.STAT_FRAGS;
import static jake2.Globals.cl;
import static spacegraph.space2d.container.grid.Containers.grid;

/**
 * Created by me on 9/22/16.
 */
public class Jake extends Game implements Runnable {

    private static final boolean lookPitch = true;
    private static final boolean hearAudio = false;

    private static final int FPS = 40;
    static final int audioBufferSamples = 4096;
    static float timeScale = 2f;
    static float yawSpeed = 10;
    static float pitchSpeed = 5;
    final PlayerData player = new PlayerData();
    final CircularFloatBuffer buf = new CircularFloatBuffer(audioBufferSamples);
    private final GLScreenShot rgb;
    private final GLScreenShot depth;
    /**
     * in degrees
     */
    float pitchAngle = 30;
    float[] bbuf = new float[audioBufferSamples];
    private FreqVectorSensor hear;
    private int lastSamplePos = -1;

    public Jake() {
        super("q");

        rgb = new GLScreenShot(true);
        depth = new GLScreenShot(false);
    }

    /**
     * put maps in /home/me/.jake2/maps
     * http://tastyspleen.net/quake/downloads/maps/individual/?C=M;O=D
     * http://www.lahtela.com/aq2suomi/maps/icity.zip
     */
    protected static String nextMap() {
        return "demo1";
        //return "town";
        //return "campgrounds";
        //return "castle";
        //return "airport";
        //return "icity";
    }

    public static void main(String[] args) {

        new Player(FPS, n -> n.add(new Jake())).start();

    }

    @Override
    protected void init() {
        super.init();

        //int px = 64, py = 48;
        int ipw = 128, iph = 96;
        //int spw = 8, sph = 6;
        int spw = 16, sph = 12;

        ScaledBitmap2D src = new ScaledBitmap2D(rgb, ipw, iph);
        src.mode(ColorMode.Hue);

        PixelBag rgbVision = new PixelBag(
                //new BrightnessNormalize(
                        new ImageFlip(src, false, true)
                //)
        , ipw, iph);
        rgbVision.setZoom(0);

        //TODO AE?
        BitmapSensor depthVision = senseCamera("depth",
            new BrightnessNormalize(
                new ImageFlip(new ScaledBitmap2D(depth, ipw / 4, iph / 4), false, true
                )
            )
        );


        int aeStates = 6;
        AutoClassifiedBitmap rgbAE = new AutoClassifiedBitmap(
                $.atomic("gray"), rgbVision, spw, sph,
                aeStates, this);
        rgbAE.alpha(0.03f);
        //rgbAE.resolution(0.05f);
        //rgbAE.ae.normalize.set(false);
        rgbAE.learnRandom = 16;

        BitmapMatrixView rgbView = new BitmapMatrixView(rgbVision);
        afterFrame(rgbView::updateIfShowing);


        DemultiplexedScalarSensor vx = senseNumberBi($.inh(id, $.p($.atomic("v"), $.atomic("x"))), new FloatNormalized(() -> player.velX));
        DemultiplexedScalarSensor vy = senseNumberBi($.inh(id, $.p($.atomic("v"), $.atomic("y"))), new FloatNormalized(() -> player.velY));
        DemultiplexedScalarSensor vz = senseNumberBi($.inh(id, $.p($.atomic("v"), $.atomic("z"))), new FloatNormalized(() -> player.velZ));

        int angles = 8;
        DemultiplexedScalarSensor ang = senseAngle(angles,
                () -> MathUtils.radians(player.yaw + 180),
                a -> $.inh(id, $.p($.atomic("ang"), $.the(a))));

        ang.freqRes(0.1f);

        //SpaceGraph.(column(visionView, camAE.newChart()), 500, 500);
//        }


        //BitmapMatrixView depthView = new BitmapMatrixView(depthVision.src);
        //onFrame(depthView::updateIfShowing);

        //                new VectorSensorChart(ang, this)
        /*,depthView*/
        nar.runLater(() -> SpaceGraph.window(grid(
                grid(
                        new VectorSensorChart(vx, this),
                        new VectorSensorChart(vy, this),
                        new VectorSensorChart(vz, this),
                        new VectorSensorChart(ang, this)
//                new VectorSensorChart(ang, this)
                ),
                rgbView, nars.gui.GameUI.aeUI(rgbAE),
                new VectorSensorChart(depthVision, this.focus()).withControls()/*,depthView*/), 500, 500));

        if (hearAudio) {
            hear = new FreqVectorSensor(8, 4096, 35, 2048, f -> $.inh($.the(f), "hear"), nar);
            hear.freqRes(0.1f);
            sensors.addSensor(hear);

            //WaveBitmap hearView = new WaveBitmap(hear.buf, 300, 64);
            //onFrame(hearView::update);
            //spectrogram(hear.buf, 0.1f,512, 16),

            SpaceGraph.window(grid(new VectorSensorChart(hear, this).withControls(),
                    //spectrogram(hear.buf, 0.1f,512, 16),
                    new ObjectSurface(hear)), 400, 400);

        } else
            hear = null;



        actionToggle(
                $.inh(id, "fore"), $.inh(id, "back"),
                (BooleanProcedure) x -> CL_input.in_forward.state = x ? 1 : 0,
                (BooleanProcedure) x -> CL_input.in_back.state = x ? 1 : 0
        );

//        actionPushButtonMutex(
//                $.the("strafeLeft"), $.the("strafeRight"),
//                x -> CL_input.in_moveleft.state = x ? 1 : 0,
//                x -> CL_input.in_moveright.state = x ? 1 : 0
//        );

//        actionToggle(
//            $.inh(id, "left"), $.inh(id, "right"),
//            ()->cl.viewangles[Defines.YAW] += yawSpeed,
//            ()->cl.viewangles[Defines.YAW] -= yawSpeed
//        );
        actionBipolar(
                $.inh(id, $.p("turn", $.varDep(1))),
                s -> {
                    cl.viewangles[Defines.YAW] += s * yawSpeed;
                    return s;
                }
        );


        actionPushButton($.inh(id, "jump"), (BooleanProcedure) x1 -> CL_input.in_up.state = x1 ? 1 : 0);

        actionPushButton($.inh(id, "fire"), (BooleanProcedure) x -> CL_input.in_attack.state = x ? 1 : 0);

        if (lookPitch) {
            actionToggle(
                    $.inh(id, "lookUp"), $.inh(id, "lookDown"),
                    () -> cl.viewangles[Defines.PITCH] = Math.min(+pitchAngle / 2, cl.viewangles[Defines.PITCH] + pitchSpeed),
                    () -> cl.viewangles[Defines.PITCH] = Math.max(-pitchAngle / 2, cl.viewangles[Defines.PITCH] - pitchSpeed)
            );
        }

        afterFrame(player::update);

        reward("health", () -> player.health);
        //rewardNormalized($.inh(id, "health"), () -> player.health/*.nanIfZero()*/);
//        reward($.inh(id, "alive"), () -> player.alive ? 1 : 0);
        //rewardNormalizedPolar($.inh(id, "health"), new FloatDifference(() -> player.health, nar::time)/*.nanIfZero()*/).strength(0.5f);

        reward("speed", new FloatNormalized(() -> player.speed).ewma(10)).conf(0.25f);

        reward("frags",
                new FloatNormalized(() -> Math.min(player.damageInflicted.getAndSet(0), 5f)));


//        ()->{
//            player.update();
//
//            float nextState = player.health * 4f + player.speed / 2f + player.frags * 2f;
//
//            float delta = nextState - state;
//
//            state = nextState;
//
//            return delta;
//        });


        new Thread(this).start();

    }

    @Override
    public void run() {


        IN.mouse_avail = false;
        Jake2.run(new String[]{
                //"+god 1",
                "vid_width 800",
                "vid_height 600",
                "+cl_gun 0",
                "+lookspring 1",
                "+s_impl jsound",
                "+timescale " + timeScale,

                "+allow_download 0",

                //"+deathmatch 1",
                //"+dmflags 1024",
                "+map " + nextMap()
                //"+connect tastyspleen.net"

        }, g -> {
            rgb.update(g);
            depth.update(g);
            if (hearAudio)
                soundUpdate();
        });


        /*
        Outer Base		base1.bsp
        Installation		base2.bsp
        Comm Center	base3.bsp
        Lost Station		train.bsp
        Ammo Depot	bunk1.bsp
        Supply Station	ware1.bsp
        Warehouse		ware2.bsp
        Main Gate		jail1.bsp
        Destination Center	jail2.bsp
        Security Complex	jail3.bsp
        Torture Chambers	jail4.bsp
        Guard House	jail5.asp
        Grid Control		security.bsp
        Mine Entrance	mintro.asp
        Upper Mines		mine1.bsp
        Borehole		mine2.bsp
        Drilling Area		mine3.bsp
        Lower Mines		mine4.bsp
        Receiving Center	fact1.bsp
        Sudden Death	fact3.bsp
        Processing Plant	fact2.bsp
        Power Plant		power1.bsp
        The Reactor		power2.bsp
        Cooling Facility	cool1.bsp
        Toxic Waste Dump	waste1.bsp
        Pumping Station 1	waste2.bsp
        Pumping Station 2	waste3.bsp
        Big Gun		biggun.bsp
        Outer Hangar	hangar1.bsp
        Comm Satelite	space.bsp
        Research Lab	lab.bsp
        Inner Hangar	hangar2.bsp
        Launch Command	command.bsp
        Outlands		strike.bsp
        Outer Courts	city1.bsp
        Lower Palace	city2.bsp
        Upper Palace	city3.bsp
        Inner Chamber	boss1.bsp
        Final Showdown	boss2.bsp


        Read more: http:
        Under Creative Commons License: Attribution Non-Commercial No Derivatives
        Follow us: @CheatCodes on Twitter | CheatCodes on Facebook

         */

    }

    protected void soundUpdate() {
        byte[] b = SND_JAVA.dma.buffer;
        if (b == null)
            return;
        int sample = SND_JAVA.SNDDMA_GetDMAPos();
        if (sample != lastSamplePos) {


            //System.out.println(sample + " " + SND_MIX.paintedtime);

            int samples = 1024;
            if (buf.available() < samples * 2)
                buf.skip(samples * 2);

            int from = (sample - samples * 4) % b.length;
            int to = (sample - samples * 2) % b.length;

            while (from < 0) from += b.length;
            while (to < 0) to += b.length;

            if (from <= to) {
                buf.writeS16(b, from, to, 1);
            } else {
                buf.writeS16(b, from, b.length, 1);
                buf.writeS16(b, 0, to, 1);
            }


            hear.input(buf.peekLast(bbuf));

            lastSamplePos = sample;
        }
    }

    public static class PlayerData {
        final AtomicInteger damageInflicted = new AtomicInteger();
        public float health;
        public float velX;
        public float velY;
        public float velZ;
        public float speed;
        public int weaponState;
        public short frags;
        public float yaw;
        //        public boolean alive;
        edict_t player;

        protected synchronized void update() {

            edict_t p = PlayerView.current_player;
            if (player == null || player != p) {
                if (p == null)
                    return;
                player = p;
                p.hurt = new EntHurtAdapter() {
                    @Override
                    public void hurt(edict_t self, edict_t other, int damage) {
                        if (self == player && other != player) {
                            System.err.println("hurt " + other + " (" + damage + ")");
                            damageInflicted.addAndGet(damage);
                        }
                    }

                    @Override
                    public String getID() {
                        return "player_hurt";
                    }
                };
            }


            if (p.deadflag > 0) {


                Key.Event(Key.K_ENTER, true, 0);
                return;
            }

            health = p.health/100f;


//            alive = p.deadflag==0;

            weaponState = p.client.weaponstate;

            frags = p.client.ps.stats[STAT_FRAGS];


            yaw = p.s.angles[1];

            float[] v = p.velocity;
            velX = v[0];
            velY = v[1];
            velZ = v[2];
            speed = (float) Math.sqrt(velX * velX + velY * velY + velZ * velZ);

        }


    }

}