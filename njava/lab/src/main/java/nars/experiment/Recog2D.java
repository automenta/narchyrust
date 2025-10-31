package nars.experiment;

import com.jogamp.opengl.GL2;
import jcog.math.v2;
import jcog.signal.FloatRange;
import jcog.signal.wave2d.ScaledBitmap2D;
import nars.*;
import nars.game.Game;
import nars.game.action.GoalAction;
import nars.game.reward.LambdaScalarReward;
import nars.gui.NARui;
import nars.gui.sensor.VectorSensorChart;
import nars.sensor.BitmapSensor;
import nars.term.Termed;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static nars.$.$$;

/**
 * Created by me on 10/8/16.
 */
public class Recog2D extends Game {

    private final Graphics2D g;
    private final int
        w = 12, h = 14, fontSize = 18;
        //w = 8, h = 5, fontSize = 6;

    private BeliefVector outs;

    //effects TODO abstract
    public final FloatRange jitter = new FloatRange(0, 0, 1);
    public final FloatRange jitterDistance = new FloatRange(0, 0, 1);
    float jitterRate = 0.1f;

    /** view rendering offset */
    private v2 t;

//    private final Training train;
    private BitmapSensor<?> sp;

//    boolean mlpLearn = true, mlpSupport = true;

    BufferedImage canvas;

//    public final AtomicBoolean neural = new AtomicBoolean(false);

    int image;
    static final int imageCount = 2;

    int imagePeriod = 64;
    static final int FPS = 16;
    private static final float actionRes =
        1;
        //0.25f;

    public Recog2D() {
        super($$("x"));

        canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        g = (Graphics2D) canvas.getGraphics();

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    @Override
    protected void init() {
        super.init();

        int sh = 9;
        int sw = 7;
        sp = senseCamera(
                //id,
                (x,y)->$.inh(id, $.p(x, y)),
                /*new BrightnessNormalize*/(
                        new ScaledBitmap2D(() -> canvas, sw, sh)/*, 0.8f*/)
                );


        outs = new BeliefVector(this, imageCount,
                //$.inst($.the( ii), $.the("x"))
                ii -> $.inh(id, id.toString() + $.the(ii))
//                ii -> $.the(id.toString() + ii)
        );
//        train = new Training(
//                sp.src instanceof PixelBag ?
//                    new FasterList(sensors).with(((PixelBag) sp.src).actions) : sensors,
//                outs, nar);
//        train = null;

        //return Util.clamp(2 * -(error / maxImages - 0.5f), -1, +1);
        LambdaScalarReward r = reward("correct", () -> {
            double error = 0;
            double pcSum = 0;
            for (int i = 0; i < imageCount; i++) {
                BeliefVector.Neuron ni = this.outs.neurons[i];
                ni.update();
                double pc = ni.predictedConf;
                if (pc==pc) {
                    //double pc = 1;
                    pcSum += pc;
                    error += //ni.error * pc;
                             ni.error;
//                    System.out.print("\t" + i + " " + ni.error);
                }
            }
//            System.out.println();

            return (float) (1 - error/imageCount);
            //return (float) Math.pow(1 - error / pcSum, 1);
            //return Util.clamp(2 * -(error / maxImages - 0.5f), -1, +1);
        });//.compose(new FloatAveragedWindow(16, 0.1f)));
        //nar.runLater(()-> r.addGuard(true, false));

//        reward("specific", () -> {
//            float[] expectation = Util.normalizeCartesian(Util.map(
//                    (IntToFloatFunction) ((int i) -> {
//                        float v = this.outs.neurons[i].predictedFreq * this.outs.neurons[i].predictedConf;
//                        return v==v? v : 0;
//                    }),
//                    new float[this.outs.neurons.length]), 0.000001f);
//            return Util.max(expectation) - Util.min(expectation);
//        }).conf(nar.confDefault(GOAL)/2);

//        Param.DEBUG = true;
//        nar.onTask(t ->{
//            NALTask T = (NALTask) t;
//            if (!T.ETERNAL() && t.term().equals(r.term())) {
//                System.out.println(T.proof());
//            }
//        }, GOAL);

        nar.runLater(()-> SpaceGraph.window(conceptTraining(outs, nar), 800, 600));
    }

    Surface conceptTraining(BeliefVector tv, NAR nar) {
        //Plot2D p;
//        int history = 256;

        int bound = tv.concepts.length;
        Gridding g = new Gridding(

//                p = new Plot2D(history, Plot2D.Line).addAt("Reward", () ->
//                        reward
//
//                ),


                new AspectAlign(new VectorSensorChart(sp, this), AspectAlign.Align.Center, sp.width, sp.height),

                new Gridding(beliefTableCharts(nar, List.of(tv.concepts), 16)),

                new Gridding(IntStream.range(0, bound).mapToObj(i -> new VectorLabel(String.valueOf(i)) {
                    @Override
                    protected void paintIt(GL2 gl, ReSurface r) {
                        var c = tv.concepts[i];
                        BeliefVector.Neuron nn = tv.neurons[i];

                        float freq;

                        Truth t = c.concept.beliefs().truth(time, nar);
                        if (t != null) {
							//float conf = (float) t.conf();
                            freq = t.freq();
                        } else {
//                            conf = nar.confMin.floatValue();
                            float defaultFreq =
                                    0.5f;

                            freq = defaultFreq;
                        }


                        Draw.colorBipolar(gl,
                                2f * (freq - 0.5f)


                        );

                        //float m = 0.5f * conf;

                        Draw.rect(bounds, gl);

                        if (tv.verify) {
                            float error = nn.error;
                            if (error != error) {


                            } else {


                            }
                        }


                    }
                }).toArray(Surface[]::new)));

        int[] frames = {0};
        beforeFrame(() -> {

            if (frames[0]++ % imagePeriod == 0) {
                nextImage();
            }

            redraw();


            outs.expect(image);


//            if (neural.get()) {
//                train.update(mlpLearn, mlpSupport);
//            }

            //p.update();

        });

        return g;
    }

    @Deprecated
    public Surface beliefTableCharts(NAR nar, Collection<? extends Termed> terms, long window) {
        long[] btRange = new long[2];
        onFrame(() -> {
            long now = nar.time();
            btRange[0] = now - window;
            btRange[1] = now + window;
        });
        return NARui.beliefCharts(terms.stream(), nar);
    }

    protected int nextImage() {

        image = nar.random().nextInt(imageCount);


        return image;
    }

    private void redraw() {
        g.clearRect(0, 0, w, h);
        FontMetrics fontMetrics = g.getFontMetrics();

        String s = String.valueOf((char) ('0' + image));

        Rectangle2D sb = fontMetrics.getStringBounds(s, g);
        double sbX = sb.getCenterX();

        v2 t = new v2((float) (w / 2 - sbX), h);

        var rng = nar.random();

        boolean saccade = rng.nextFloat() < jitterRate;
        float jitterx = jitter.floatValue();
        float jittery = jitterx;
        float jitterDistance = this.jitterDistance.floatValue();
        t.added( (float)(saccade ? (rng.nextFloat() - 0.5f)*2 * sb.getWidth() * jitterDistance : jitterx),
                 (float)(saccade ? (rng.nextFloat() - 0.5f)*2 * sb.getHeight()* jitterDistance : jittery));
        g.drawString(s, t.x, t.y);
        this.t = t;
    }

    public static void main(String[] arg) {

        var p = new Player(new Recog2D());

        //p.gameReflex = true;

        p.fps(FPS*2).start();

    }

//    public static class Training {
//        private final List<Sensor> ins;
//        private final BeliefVector outs;
//        private final MLPMap trainer;
//        private final NAR nar;
//
//        private final float learningRate = 0.3f;
//
//        /**
//         * Introduction of the momentum rate allows the attenuation of oscillations in the gradient descent. The geometric idea behind this idea can probably best be understood in terms of an eigenspace analysis in the linear case. If the ratio between lowest and largest eigenvalue is large then performing a gradient descent is slow even if the learning rate large due to the conditioning of the matrix. The momentum introduces some balancing in the update between the eigenvectors associated to lower and larger eigenvalues.
//         * <p>
//         * For more detail I refer to
//         * <p>
//         * http:
//         */
//        private final float momentum = 0.6f;
//
//        public Training(java.util.List<Sensor> ins, BeliefVector outs, NAR nar) {
//
//            this.nar = nar;
//            this.ins = ins;
//            this.outs = outs;
//
//
//            this.trainer = new MLPMap(ins.size(), new int[]{(ins.size() + outs.states) / 2, outs.states}, nar.random(), true);
//            trainer.layers[1].setIsSigmoid(false);
//
//        }
//
//
//        float[] in(float[] i, long when) {
//            int s = ins.size();
//
//            if (i == null || i.length != s)
//                i = new float[s];
//            for (int j = 0, insSize = ins.size(); j < insSize; j++) {
//                float b = nar.beliefTruth(ins.get(j), when).freq();
//                if (b != b)
//                    b = 0.5f;
//                i[j] = b;
//            }
//
//            return i;
//        }
//
//        protected void update(boolean train, boolean apply) {
//            float[] i = in(null, nar.time());
//
//            float errSum;
//            if (train) {
//                float[] err = trainer.put(i, outs.expected(null), learningRate);
//
//                errSum = Util.sumAbs(err) / err.length;
//                System.err.println("  error sum=" + errSum);
//            } else {
//                errSum = 0f;
//            }
//
//            if (apply/* && errSum < 0.25f*/) {
//                float[] o = trainer.get(i);
//                for (int j = 0, oLength = o.length; j < oLength; j++) {
//                    float y = o[j];
//
//                    float c = nar.confDefault(BELIEF) * (1f - errSum);
//                    if (c > 0) {
//                        nar.believe(
//                                outs.concepts[j].target(),
//                                Tense.Present, y, c);
//                    }
//
//                }
//
//            }
//        }
//    }
//

    /**
     * Created by me on 10/15/16.
     */
    static class BeliefVector {

        static class Neuron {

            public float predictedFreq = 0.5f;
            public float predictedConf = 0;

            public float expectedFreq;

            public float error;

            Neuron() {
                expectedFreq = Float.NaN;
                error = 0;
            }

            public void expect(float expected) {
                this.expectedFreq = expected;
                update();
            }

            public void actual(float f, float c) {
                this.predictedFreq = f;
                this.predictedConf = c;
                update();
            }

            protected void update() {
                float a = this.predictedFreq;
                float e = this.expectedFreq;
                if (e != e) {
                    this.error = 0;
                } else if (a != a) {
                    this.error = 0.5f;
                } else {
                    this.error = Math.abs(a - e);
                }
            }
        }

        public float[] expected(float[] output) {
            output = sized(output);
            for (int i = 0; i < concepts.length; i++)
                output[i] = expected(i);
            return output;
        }

        public float[] actual(float[] output) {
            output = sized(output);
            for (int i = 0; i < concepts.length; i++)
                output[i] = actual(i);
            return output;
        }

        float[] sized(float[] output) {
            if (output == null || output.length != states) {
                output = new float[states];
            }
            return output;
        }


        Neuron[] neurons;
        GoalAction[] concepts;

        final int states;


        boolean verify;


        BeliefVector(Game a, int maxStates, IntFunction<Term> namer) {

            this.states = maxStates;
            this.neurons = new Neuron[maxStates];
            this.concepts = IntStream.range(0, maxStates).mapToObj(i -> {
                        Term tt = namer.apply(i);

                        Neuron n = neurons[i] = new Neuron();

                        return a.action(tt, x -> {


                            n.actual(x, 0.5f);
//                                    x != null ? x.freq() : Float.NaN,
//                                    x != null ? (float) x.conf() : 0);


                        }).freqRes(actionRes);


                    }


            ).toArray(GoalAction[]::new);


        }

        public float expected(int i) {
            return neurons[i].expectedFreq;
        }


        public float actual(int state) {
            return neurons[state].predictedFreq;
        }


        void expect(IntToFloatFunction stateValue) {
            for (int i = 0; i < states; i++)
                neurons[i].expect(stateValue.valueOf(i));
        }

        public void expect(int onlyStateToBeOn) {
            expect(ii -> ii == onlyStateToBeOn ? 1f : 0);
        }


    }
}