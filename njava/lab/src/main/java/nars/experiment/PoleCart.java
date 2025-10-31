package nars.experiment;

import jcog.Is;
import jcog.TODO;
import jcog.Util;
import jcog.activation.ReluActivation;
import jcog.activation.SigLinearActivation;
import jcog.lstm.LSTM;
import jcog.math.FloatSupplier;
import jcog.math.normalize.FloatNormalized;
import jcog.nndepr.MLP;
import jcog.nndepr.RecurrentNetwork;
import jcog.nndepr.optimizer.SGDOptimizer;
import jcog.signal.FloatRange;
import jcog.tensor.LivePredictor;
import jcog.tensor.Predictor;
import nars.NAR;
import nars.Player;
import nars.Term;
import nars.func.TruthPredict;
import nars.game.Game;
import nars.game.action.BiPolarAction;
import nars.game.reward.MultiReward;
import nars.game.sensor.Sensor;
import nars.gui.LSTMView;
import nars.gui.LayerView;
import nars.gui.NARui;
import nars.term.Termed;
import nars.term.atom.Atomic;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Streams.stream;
import static jcog.Util.fma;
import static nars.$.*;
import static org.hipparchus.util.MathUtils.normalizeAngle;
import static spacegraph.SpaceGraph.window;

/**
 * adapted from:
 * http:
 * https:
 * <p>
 * see also: https:
 * https://github.com/tensorflow/tfjs-examples/blob/master/cart-pole/cart_pole.js
 * https://github.com/gyscos/CMANes/blob/master/src/pole/Pole.java
 * https://github.com/openai/gym/blob/master/gym/envs/classic_control/cartpole.py#L66
 */
@Is("Inverted_pendulum") public class PoleCart extends Game {

    /**
     * time downsample; iterations to compute per frame
     */
    public int frameSkip =
        2;
        //1; //BASE
        //3;
        //4;
        //8;

    static final boolean rewardBinary = false;

    static final boolean speedControl = false;
    static final boolean brakeControl = false;

    private static final float fps = 50;

    static final boolean senseVelocities = true;

    private final boolean senseAngleDirect = true;
    private final boolean senseAngleComponents = false;

    static private final float sensorRes =
            0.01f;
    static final float actionRes =
            speedControl ? 1 : sensorRes;

    private static final int sensorDetailSmall =
        //3;
        5;
        //2;
        //1;

    private static final int sensorDetail =
        //3;
        5;
        //2;
        //1; //TODO eliminate (...-->0) term pattern
        //3;
        //4;
        //8;
        //12;
        //16;

    private final boolean rewardMulti = false;
    private final boolean rewardMotionless = false;

    private boolean rewardCenter;

    private final float rewardPower = 1;

    private static final float posMin = -2;
    private static final float posMax = +2;
    private static final double cartMass = 1;
    private static final double poleMass =
            //0.1; //<-- OpenAI Gym parameters
            0.1;
    private static final double poleLength = 1;
    private static final double cartLength = poleLength / 2;
    private static final double poleMassLength = poleMass * cartLength;

    public final FloatRange gravity = new FloatRange(
            9.82f
            //0.1; //<-- interesting
            //4;
            //0;
            , 0, 12);

    private static final double totalMass = cartMass + poleMass;

    private static final int substeps = 2;

    public final FloatRange fricPole = new FloatRange(
            0 //<-- OpenAI Gym parameters
            //0.002f
            , 0, 0.1f);
    public final FloatRange fricCart = new FloatRange(
            0 //<-- OpenAI Gym parameters
            //0.01f
            , 0, 1);

    public final FloatRange forceAmp = new FloatRange(3, 0, 6);
    public final FloatRange forcePower = new FloatRange(1, 0, 4);
    public final FloatRange tau = new FloatRange(
            0.02f, 0.001f, 0.04f //<-- OpenAI Gym parameters
            //0.005f, 0.001f, 0.04f
            //0.03f, 0.01f, 0.08f
            //0.02f, 0.01f, 0.08f
            //0.06f, 0.01f, 0.12f
            //0.019f, 0.001f, 0.05f
//            0.03f, 0.001f, 0.05f
//            0.02f, 0.001f, 0.05f
    );
    private final AtomicBoolean paintQueued = new AtomicBoolean(false);

    final Sensor dx;
    final Sensor x;
    final Sensor angX, angY;
    final Sensor angVel;


    private final Sensor angle;


    private final float posSpeedMax = (posMax - posMin) * 1;
    private final double angSpeedMax = 8;

    private double pos;
    private double posDot;
    private double ang = 0.2;
    private double angDot;
    private double impulse;
    private float forceL, forceR;
    private JPanel panel;
    private Dimension offDimension;
    private Graphics offGraphics;


    private boolean keyboard;
    private float forceKeyL, forceKeyR;


    PoleCart(Term id) {
        this(id, false);
    }

    public PoleCart(String id, boolean visible) {
        this($$(id), visible);
    }

    PoleCart(Term id, boolean visible) {
        super(id);

        FloatSupplier R = //mode == 0 ?
                this::reward
                //: this::reward1
                ;

        if (rewardMulti)
            reward(MultiReward.ewma(p(id, "balance"), R, 0.01f)/*.resolution(0.05f)*/);
        else {
            reward("balanced", R);//.resolution(0.04f);
            //r.rewardFreqOrConf.set(false);
        }

        if (rewardCenter) {
            reward("center", () -> (float) (
                    Math.pow(1 - 2 * Math.abs(0.5 - Util.normalize(pos, posMin, posMax)), 0.5f)
            ));
        }

        if (rewardMotionless) {
            reward("still", () -> {
                //TODO refine
                return (float) (1 / (1 +
                        //Math.abs(angDot)/angSpeedMax
                        +4 * Math.abs(posDot) / (posSpeedMax)
                ));
            });
        }

        beforeFrame(() -> {
            double tau = this.tau.floatValue();
            for (int j = 0; j < frameSkip; j++) {
                for (int i = 0; i < substeps; i++)
                    this.update(tau / substeps);
            }
            if (paintQueued.weakCompareAndSetAcquire(false, true))
                panel.repaint();
        });


        x = senseNumberN(
                //inh(p("x", id), varDep(1))
                inh(id, p("x", varDep(1)))
                , () -> (float) (pos - posMin) / (posMax - posMin), sensorDetailSmall);
        if (senseVelocities) {
            dx = senseNumberN(
                    //inh(p("dx", id), $.varDep(1))
                    inh(id, p("dx", varDep(1)))
                    ,
                    new FloatNormalized(() -> (float) posDot).polar(), sensorDetailSmall
            );
        }

//        Term _angX = inh(p($.the("ang"), $.the("x"), id), $.varDep(1));
//        Term _angY = inh(p($.the("ang"), $.the("y"), id), $.varDep(1));
        Term _angX = inh(id, p(atomic("ang"), atomic("x"), varDep(1)));
        Term _angY = inh(id, p(atomic("ang"), atomic("y"), varDep(1)));
        Term _ang = inh(id, p("ang", varDep(1)));
        if (senseAngleComponents) {
            angX = senseNumberN(_angX,
                    () -> (float) (0.5f + Math.cos(ang) / 2), sensorDetail);
            angY = senseNumberN(_angY,
                    () -> (float) (0.5f + Math.sin(ang) / 2), sensorDetail);
        } else
            angX = angY = null;


        FloatSupplier angle = () -> (float) (ang + Math.PI / 2);
        if (senseAngleDirect) {
            //if (sensorDetail == 1)
                //this.angle = sense($.inh(id, "ang"), angle);

            this.angle = senseAngle(_ang, angle);

        } else
            this.angle = null;

//		,
//			() -> (float) (0.5f + 0.5f * Math.cos(angle)), digitization);


        if (senseVelocities) {
            FloatSupplier angVel = () -> (float) angDot;
            this.angVel = senseNumberN(
                    inh(id, p("d", "ang", varDep(1))
                    ), new FloatNormalized(angVel).polar(), sensorDetailSmall);
        }

        sensors.sensors.forEach(x -> x.freqRes(sensorRes));

        actionBipolar();
        //actionBipolarVelocity();
        //initUnipolar();

        if (brakeControl) {
            float brakeRate = 0.25f;
            actionPosHalf(inh(id, "brake"), x -> {
                double f = 1 - x * brakeRate;
                this.posDot *= f;
                //this.angDot *= f;
                return x;
            });
        }
        if (speedControl) {
            var speedAction = action(inh(id, "speed"), s -> {
                if (keyboard) throw new TODO();
                this.forceAmp.setLerp(s);
            }).freqRes(actionRes);
        }


        panel = new JPanel(new BorderLayout()) {
            final Stroke stroke = new BasicStroke(4);
            private final double[] xs = {-2.5, 2.5, 2.5, 2.3, 2.3, -2.3, -2.3, -2.5};
            private final double[] ys = {-0.4, -0.4, 0., 0., -0.2, -0.2, 0, 0};
            private final transient int[] pixxs = new int[8];
            private final transient int[] pixys = new int[8];
            private Image offImage;

            {
                setIgnoreRepaint(true);
            }

            @Override
            public void paint(Graphics g) {
                update(g);
            }

            @Override
            public void update(Graphics g) {

                //paintQueued.set(false);

                Dimension d = panel.getSize();


                if (offGraphics == null
                        || d.width != offDimension.width
                        || d.height != offDimension.height) {
                    offDimension = d;
                    offImage = panel.createImage(d.width, d.height);
                    offGraphics = offImage.getGraphics();
                }


                float clearRate =
                        //0.5f; //transparent fade
                        1;

                offGraphics.setColor(new Color(0, 0, 0, clearRate));
                offGraphics.fillRect(0, 0, d.width, d.height);

                for (int i = 0; i < 8; i++) {
                    pixxs[i] = px(d, xs[i]);
                    pixys[i] = py(d, ys[i]);
                }
                Color trackColor = Color.GRAY;
                offGraphics.setColor(trackColor);
                offGraphics.fillPolygon(pixxs, pixys, 8);


                Color cartColor = Color.ORANGE;
                offGraphics.setColor(cartColor);
                offGraphics.fillRect(px(d, pos - 0.2), py(d, 0), pixDX(d, 0.4), pixDY(d, -0.2));

                ((Graphics2D) offGraphics).setStroke(stroke);


                offGraphics.drawLine(px(d, pos), py(d, 0),
                        px(d, pos + Math.sin(ang) * poleLength),
                        py(d, poleLength * Math.cos(ang)));


                if (impulse != 0) {
                    int signAction = impulse > 0 ? 1 : impulse < 0 ? -1 : 0;
                    int tipx = px(d, pos + 0.2 *
                            //signAction
                            impulse * 0.8f //TODO normalize to max force range
                    );
                    Color arrowColor = new Color(0.1f, 0.5f + Util.tanhFast(Math.abs((float) impulse * 0.2f)) / 2f, 0.1f);
                    int tipy = py(d, -0.1);
                    offGraphics.setColor(arrowColor);
                    offGraphics.drawLine(px(d, pos), py(d, -0.1), tipx, tipy);
                    offGraphics.drawLine(tipx, tipy, tipx - 4 * signAction, tipy + 4);
                    offGraphics.drawLine(tipx, tipy, tipx - 4 * signAction, tipy - 4);
                }


                g.drawImage(offImage, 0, 0, panel);


                paintQueued.setRelease(false);
            }

        };


        if (visible) {
            JFrame f = new JFrame();
            f.setContentPane(panel);
            f.setSize(800, 300);
            f.setVisible(true);

            f.addKeyListener(new KeyAdapter() {

                @Override
                public void keyReleased(KeyEvent e) {
                    if (keyboard) {
                        forceKeyL = forceKeyR = 0;
                    }
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_SPACE -> {
                            keyboard = !keyboard;
                            System.out.println("manualOverride=" + keyboard);
                        }
                        case KeyEvent.VK_LEFT -> {
                            if (keyboard) {
                                forceKeyL = +1;
                                forceKeyR = 0;
                            }
                        }
                        case KeyEvent.VK_RIGHT -> {
                            if (keyboard) {
                                forceKeyR = +1;
                                forceKeyL = 0;
                            }
                        }
                        case KeyEvent.VK_DOWN -> forceAmp.add(-0.5f);
                        case KeyEvent.VK_UP -> forceAmp.add(0.5f);
                    }
                }
            });
        }

    }

    public static void main(String[] args) {
        Player p = new Player(new PoleCart($$("p"), true));
        p.fps(fps);

        boolean mini = true;
        if (mini) {
            //p.focusLinks = 384;
            //p.freqRes = 0.06f;
            //p.complexMax = 15;
            //p.confMin = p.confRes = 0.0001;
            p.arith = false;
            //p.nalStructural = false;
        }
        p.start();
    }

    public enum PoleCartExt {
        ;
        static final boolean sensorPredict = true;
        static final boolean actionPredict = false;
        static final boolean rl = true;
        static final boolean metaRL = false;

        public static void main(String[] arg) {

            int N = 1;
            Player P = new Player(fps, n -> {
                for (int i = 0; i < N; i++) {
                    Atomic baseName = atomic(PoleCart.class.getSimpleName());
                    PoleCart g = new PoleCart(N > 1 ? p(baseName, the(i)) : baseName, true);

                    n.add(g);

                    n.runLater(() -> window(NARui.beliefCharts(
                            g.sensors.components().toList(), n), 900, 900
                    ));
                }
            });
            //P.volMax = 16;
//        P.controlLerp();
//        P.answerQuestionsFromTaskLinks = true;

            P.subMetaReflex = metaRL;
            P.gameReflex = rl;
            //P.deltaIntro = true;
//        P.selfMetaReflex = P.subMetaReflex = true;


            P.start();
//        P.the(Game.class).forEach(g -> {
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                File f = new File("/tmp/" + g.id + ".nal");
//                try {
//                    PrintStream p = new PrintStream(new FileOutputStream(f));
//                    g.vocabulary.forEach(v ->
//                        p.println("//" + v)
//                    );
//                    long c = g.knowledge().filter(z -> !(z instanceof SerialTask)).sorted(
//                            Comparator.comparingDouble((NALTask z) -> z.BELIEF_OR_GOAL() ? z.conf() : 0)
//                                    .thenComparingInt(NALTask::volume)
//                                    .thenComparing(NALTask::term))
//                            .peek(p::println).count();
//                    System.err.println(f.toPath() + "\n\t" + g + " " + c + " tasks");
//                    p.close();
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//            }));
//        });


            P.the(PoleCart.class).forEach(p -> {
                Iterable<? extends Termed> predicting = concat(
                        p.angle.components(),
                        //p.angX.components(), p.angY.components(),
                        p.x.components()
                        //ang.sensors.stream().flatMap(x->Streams.stream(x.components())).collect(toList()),
//                    p.ang.components(),
                );
                if (senseVelocities) {
                    predicting = concat(predicting,
                            p.angVel.components(), p.dx.components()
                    );
                }

                if (sensorPredict) {
                    var sensorStream = stream(predicting);
                    var actionStream = p.actions.componentStream();
                    p.nar.add(new TruthPredict(
                            () -> Stream.concat(sensorStream, actionStream).iterator()
                            ,
                            predicting,
                            4,
                            () -> p.nar.dur() * 4,
                            5,

                            mlpPredictor()
                            //recurrentPredictor()
                            //ntmPredictor()

              /*.adapt(
                            freq -> (freq - 0.5) * 2
                            , value -> (value / 2) + 0.5
                    )*/

//                            (i,o)-> {
//                                final LSTM x = new LSTM(i,o,8);
////                                x.alpha(5f);
//                                x.randomize(0.1f, ThreadLocalRandom.current());
//                                return x;
//                            }

                            , p,
                            true) {

                        @Override
                        protected void starting(NAR n) {
                            super.starting(n);
                            n.runLater(() -> {
                                Util.sleepMS(2000);
                                Predictor P = this.predictor();
                                switch (P) {
                                    case LSTM l -> window(NARui.get(new LSTMView(l), LSTMView::run, n), 400, 400);
                                    case MLP m ->
                                            window(NARui.get(new LayerView(Stream.of(m.layers)), LayerView::run, n), 400, 400);
                                    case RecurrentNetwork r ->
                                            window(NARui.get(new LSTMView.RecurrentView(r), LSTMView.RecurrentView::run, n), 400, 400);
                                    case null, default -> {
                                    }
                                }
                            });
                        }
                    });


                }
                if (actionPredict) {
                    p.nar.add(new TruthPredict(
                            p.actions, //Iterables.concat(predicting, p.actions.actions),
                            p.actions,
                            8,
                            () -> 3 * p.dur(),
                            6,
                            (i, o) -> new LSTM(i, o, 2).alpha(0.04f),
                            //								new LivePredictor.MLPPredictor(0.1f).adapt(
                            //									freq -> (freq-0.5)*2,
                            //									value -> (value/2)+0.5
                            //								),
                            p,
                            false));
                }
            });

        }
    }

    private static IntIntToObjectFunction<Predictor> ntmPredictor() {
        return (i, o) -> new LivePredictor.NTMPredictor(i, o, 2);
    }

//    private static IntIntToObjectFunction<Predictor> recurrentPredictor() {
//        return (i, o) -> Agents.recurrentBrain(i, o, Fuzzy.mean(i, o), 4);
//    }

    private static IntIntToObjectFunction<Predictor> mlpPredictor() {
        return (i, o) -> {
            MLP m = new MLP(i,
                    new MLP.LinearLayerBuilder(o /*Fuzzy.mean(i, o)*/,
                            ReluActivation.the
                            //LeakyReluActivation.the
                    ),
//                    new MLP.Dense(o, SigmoidActivation.the)

                    //new MLP.Layer( 1 * (i + o), TanhActivation.the),
                    //                            new MLP.Layer( Math.max(i/2,o*2), TanhActivation.the),
                    //new MLP.Layer( Math.max(i/2,o*2), TanhActivation.the),
                    //                            new MLP.Layer( (int)Util.mean(i, o), TanhActivation.the),

                    //new MLP.LinearLayerBuilder((i + o), new SigLinearActivation()),
                    //new MLP.LinearLayerBuilder(o, new SigLinearActivation())
                    new MLP.LinearLayerBuilder(o,
                            //SigmoidActivation.the
                            SigLinearActivation.the
                    )
                    //new MLP.LinearLayerBuilder(o)

                    //                            new MLP.Layer( 1 * (i + o), TanhActivation.the),
                    //                            new MLP.Layer( o, SigmoidActivation.the)
                    //new MLP.Layer( (i + o), SigmoidActivation.the)
                    //new MLP.Layer( (x + y), TanhActivation.the),
            ).optimizer(
                    //new AdamOptimizer()
                    //new SGDOptimizer(0)
                    new SGDOptimizer(0.9f)
            );

            m.clear();
            return m;
        };
    }

    public final PoleCart frameSkip(int frameSkip) {
        this.frameSkip = frameSkip;
        return this;
    }

    private static int px(Dimension d, double v) {
        return (int) Math.round((v + 2.5) / 5.0 * d.width);
    }

    private static int py(Dimension d, double v) {
        return (int) Math.round(d.height - (v + 0.5f) / 2.0 * d.height);
    }

    private static int pixDX(Dimension d, double v) {
        return (int) Math.round(v / 5.0 * d.width);
    }

    private static int pixDY(Dimension d, double v) {
        return (int) Math.round(-v / 2.0 * d.height);
    }

    public void force(float f) {
        forceL = f < 0 ? -f : 0;
        forceR = f > 0 ? +f : 0;
    }

    private void actionBipolar() {
        this.actionBipolar(id,
                new BiPolarAction.Analog()
                //new BiPolarAction.PWM()
        , f -> {
            if (keyboard)
                f = forceKeyL > 0 ? -forceKeyL : +forceKeyR;
            force(f);
            return f;
        }).freqRes(actionRes);
    }

//    private void actionBipolarVelocity() {
//        this.actionBipolar(id, new FloatToFloatFunction() {
//
//            final MiniPID pid = new MiniPID(
//                    1, 1, 1
//            );
//
//            final double maxForce = 1;
//
//            @Override
//            public float valueOf(float v) {
//                pid.outRange(-maxForce, +maxForce);
//                double f = pid.out(posDot, v);
//                force((float) f);
//                return (float) posDot;
//            }
//        });//.resolution(0.1f);
//    }

    public final AtomicBoolean patrickMode = new AtomicBoolean(false);

    protected void update(double dt) {
        if (patrickMode.getOpaque()) {
            updatePatrick(dt);
        } else {
            updateClassic(dt);
        }
    }

    /** patrick's simplified cartpole
     *  https://github.com/opennars/OpenNARS-for-Applications/blob/master/src/system_tests/Cartpole_Test.h
     * */
    protected void updatePatrick(double dt) {
        double force = Util.powAbs((forceR - forceL), forcePower.floatValue())
                * forceAmp.floatValue() / 4;
        double impulse = this.impulse =
            //force;
            force * dt;


        pos = Util.clamp(pos + posDot * dt, posMin, posMax);
        ang += angDot * dt;
        ang = normalizeAngle(ang, 0);

        posDot += impulse;

        @Deprecated double angPatrick = ang - Math.PI / 2;

        //sway
        double reverse = angPatrick > 0 ? 1 : -1;
        angDot += impulse * reverse * 0.9;

        //gravity
        angDot += 0.2 * Math.cos(angPatrick);

        //cart friction
        posDot *= 0.9f;
        //pole friction
        angDot *= 0.99f;

        //max. velocities given by air density
        angDot = Util.clamp(angDot, -angSpeedMax, +angSpeedMax);
        posDot = Util.clamp(posDot, -posSpeedMax, +posSpeedMax);

    }

    protected void updateClassic(double dt) {

        double force = Util.powAbs((forceR - forceL), forcePower.floatValue())
                * forceAmp.floatValue();
        double impulse = this.impulse =
            force;
            //force * dt;

        double sinA = Math.sin(ang);
        double cosA = Math.cos(ang);
        double angleDotSq = Util.sqr(angDot);

        //TODO these should be affected by dt
        double fricCart = this.fricCart.doubleValue();
        double fricPole = this.fricPole.doubleValue();

        double common = (impulse + poleMassLength * angleDotSq * sinA
                - fricCart * (posDot < 0 ? -1 : +1)) / totalMass;


        pos = fma(posDot, dt, pos);


        if (pos >= posMax || pos <= posMin) {
            pos = Util.clamp((float) pos, posMin, posMax);
            posDot = -1 /* restitution */ * posDot;
        }

        float gravity = PoleCart.this.gravity.asFloat();
        double angAcc = (gravity * sinA - cosA * common
                - fricPole * this.angDot / poleMassLength) /
                (cartLength * (4/3.0 - poleMass * Util.sqr(cosA) / totalMass));

        double posDotNext = common - poleMassLength * angAcc * cosA / totalMass;
        posDot = Util.clamp(posDot + posDotNext * dt, -posSpeedMax, +posSpeedMax);

        ang += this.angDot * dt;
        this.angDot = Util.clamp(this.angDot + angAcc * dt,
                -angSpeedMax, +angSpeedMax);


//		if (drawFinished.compareAndSet(true, false))
        //SwingUtilities.invokeLater(panel::repaint);

    }

    private float reward1() {
        return (float) Math.pow(1 - (Math.cos(ang) / 2 + 0.5), 2);
    }

    private float reward() {
        final double a = normalizeAngle(ang, 0);
        if (rewardBinary)
            return Math.cos(a) > 0 ? 1 : 0;

        float rewardAngular = (float) Math.max(0, 1 - Math.abs(a)/(Math.PI));

        //float rewardUniform = rewardLinear / 2 + 0.5f;

        //return rewardUniform;
        //return Util.sqr(rewardUniform);
        return (float) Math.pow(rewardAngular, rewardPower);
    }


    private float reward00() {
        float rewardLinear = (float) Math.cos(ang);

        float rewardUniform = rewardLinear / 2 + 0.5f;

        //return rewardUniform;
        //return Util.sqr(rewardUniform);
        return (float) Math.pow(rewardUniform, rewardPower);
    }

    public void initUnipolar() {
        var L = action(
            inh(id, "L"), a -> (forceL = keyboard ? forceKeyL : a));
        var R = action(
            inh(id, "R"), a -> (forceR = keyboard ? forceKeyR : a));

//        L.goalDefault($.t(0, 0.00001f), nar);
//        R.goalDefault($.t(0, 0.00001f), nar);
    }

    public void reset() {
        pos = posDot = ang = angDot = 0;
    }


}