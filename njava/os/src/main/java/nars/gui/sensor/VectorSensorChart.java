package nars.gui.sensor;

import jcog.Util;
import jcog.data.list.FastCoWList;
import jcog.func.IntIntToFloatFunction;
import jcog.math.FloatSupplier;
import jcog.signal.FloatRange;
import jcog.signal.IntRange;
import nars.Answer;
import nars.Focus;
import nars.NAL;
import nars.NAR;
import nars.concept.TaskConcept;
import nars.game.Game;
import nars.game.sensor.Sensor;
import nars.game.sensor.SignalConcept;
import nars.game.sensor.VectorSensor;
import nars.gui.NARui;
import nars.sensor.BitmapSensor;
import nars.time.part.DurLoop;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.state.Dragging;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.meta.obj.ObjectSurface;
import spacegraph.space2d.widget.button.ButtonSet;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.video.Draw;

import java.util.Arrays;
import java.util.function.Consumer;

import static java.lang.Math.sqrt;
import static nars.Op.GOAL;

/**
 * displays a CameraSensor pixel data as perceived through its concepts (belief/goal state)
 * monochrome
 */
public class VectorSensorChart extends BitmapMatrixView implements BitmapMatrixView.ViewFunction2D {

    private static final int AFFECT_CONCEPT_BUTTON = 0;
    private static final int OPEN_CONCEPT_BUTTON = 2;
    /**
     * how much evidence to include in result
     */
    public final IntRange truthCapacity = new IntRange(
            NAL.answer.ANSWER_CAPACITY, 1, NAL.answer.ANSWER_CAPACITY);

    public final FloatRange depth = FloatRange.unit(1);

    /**
     * in durs
     */
    public final FloatRange timeShift = new FloatRange(0, -64, +64);

    /**
     * durs around target time
     */
    public final FloatRange window = new FloatRange(1, 0, 4);

    public final transient TaskConcept[][] concept;
    final FastCoWList<Layer> layers = new FastCoWList<>(Layer[]::new);
    private final Sensor sensor;
    private final NAR nar;
    private final FloatSupplier dur;

    final private float[] rgb = new float[3];


    /** max number of displayed concepts before defaulting to low-precision truthCapacity=1 */
    private final static int PRECISE_THRESHOLD = 8;

    //    public final AtomicBoolean beliefs = new AtomicBoolean(true);
//    public final AtomicBoolean goals = new AtomicBoolean(true);
    //public final AtomicBoolean pris = new AtomicBoolean(true);

    private DurLoop on;
    private TaskConcept touchConcept;
    private Consumer<TaskConcept> touchMode = (x) -> {
    };
    final Dragging affect = new Dragging(AFFECT_CONCEPT_BUTTON) {
        @Override
        protected boolean drag(Finger f) {
//            updateTouchedConcept(f);
            var c = touchConcept;
            if (c != null) {
                onTouch(touchConcept);
                return true;
            }
            return false;
        }
    };
    private transient Answer answer;
    private transient int answerTries;

    public VectorSensorChart(VectorSensor v, NAR n) {
        this(v, n::dur, n);
    }

    public VectorSensorChart(VectorSensor v, int w, int h, NAR n) {
        this(v, w, h, n::dur, n);
    }


    public VectorSensorChart(VectorSensor v, Game g) {
        this(v,
                //g::dur
                g::durFocus
                , g.nar()
        );
    }

    public VectorSensorChart(VectorSensor v, int w, int h, Game g) {
        this(v, w, h, g::dur, g.nar());
    }


    public VectorSensorChart(VectorSensor v, FloatSupplier dur, NAR n) {
        this(v,
                v instanceof BitmapSensor b ? b.width : (int) Math.ceil(idealStride(v)),
                v instanceof BitmapSensor b ? b.height : (int) Math.ceil(v.size() / idealStride(v)),
                dur, n);
    }

    public VectorSensorChart(VectorSensor v, int w, int h, FloatSupplier dur, NAR n) {
        super(w, h);
        if (w < 1 || h < 1)
            throw new UnsupportedOperationException("zero dimension");

        if (w * h > PRECISE_THRESHOLD)
            truthCapacity.set(1);

        this.dur = dur;
        this.sensor = v;
        this.nar = n;

        this.concept = new TaskConcept[w][h];


        if (v instanceof BitmapSensor) {
            for (TaskConcept c : v) {
                var C = (BitmapSensor.PixelSignal) c;
                concept[C.x()][C.y()] = c;
            }
        } else {
            int x = 0, y = 0;
            for (TaskConcept c : v) {
                concept[x][y] = c;
                if (w >= h) {
                    if (++x == w) {
                        x = 0;
                        y++;
                    }
                } else {
                    if (++y == h) {
                        y = 0;
                        x++;
                    }
                }
            }
        }

        initLayers();
    }

    public VectorSensorChart(Sensor sensor, SignalConcept[][] matrix, int width, int height, FloatSupplier dur, NAR n) {
        super(width, height);
        this.dur = dur;
        this.sensor = sensor;
        this.concept = matrix;
        this.nar = n;
        initLayers();
    }

    public VectorSensorChart(BitmapSensor sensor, Focus w) {
        this(sensor, ((FloatSupplier) (w::dur)).times(0.5f), w.nar);
    }

    public VectorSensorChart(BitmapSensor sensor, FloatSupplier dur, NAR n) {
        this(sensor, sensor.concepts.matrix, sensor.height, sensor.width, dur, n);
    }

    static void blend(float v, float[] color, float[] rgb) {
        rgb[0] += v * color[0];
        rgb[1] += v * color[1];
        rgb[2] += v * color[2];
    }

    static float idealStride(VectorSensor v) {
        return (float) Math.floor(sqrt(v.size()));
    }

    @Deprecated
    protected void initLayers() {
        /* beliefs */
        layers.add(new ColoredLayer(1f, 1f, 1f) {
            long as, ae;

            private final XYConcept xyc = new XYConcept() {
                @Override
                protected float floatValue(int x, int y, TaskConcept c) {
                var bb = c.beliefs();
                if (!bb.isEmpty()) {
                    var b = answer.clear().match(bb).truth(false);
                    if (b != null)
                        return b.freq(as, ae);
                }
                return Float.NaN;
                }
            };

            {
                opacity.set(0.8f);
            }

            @Override
            public void update(VectorSensorChart v) {
                as = answer.start(); ae = answer.end(); //HACK
                update(v, xyc);
                //Util.normalize(value);
            }

        });
        /* goals */
        layers.add(new Layer() {

            public final MutableBoolean normalize = new MutableBoolean(false);
            public final MutableBoolean freqOrExp = new MutableBoolean(true);
            private final XYConcept xyc = new XYConcept() {
                @Override
                protected float floatValue(int x, int y, TaskConcept c) {
                    var gg = c.goals();
                    if (gg.isEmpty()) return 0.5f;

                    var g = answer.clear().ttl(answerTries).match(gg).truth(false);
                    return g != null ?
                            (float) (freqOrExp.booleanValue() ? g.freq(answer.when) : g.expectation()) :
                            0.5f;
                }
            };

            {
                opacity.set(0.5f);
            }

            @Override
            public void blend(float vv, float opacity, float[] rgbTarget) {
                var v = (vv - 0.5f) * 2 * opacity;

                //TODO adjustable sensitivity curve
                //v = Util.tanhFast(v * 3);

                if (v <= 0)
                    rgbTarget[0] -= v;
                else
                    rgbTarget[1] += v;

                //blue highlight near 0.5:
                //rgbTarget[2] += 0.25f * Math.pow(1 - Math.abs(v), 4);
            }

            @Override
            public void update(VectorSensorChart v) {

                update(v, xyc);

                if (normalize.booleanValue()) {
                    //balance bipolar normalize around 0.5
                    var minmax = Util.minmax(value);
                    float min = minmax[0], max = minmax[1];
                    if (max - min > NAL.truth.FREQ_EPSILON /*Float.MIN_NORMAL*/) {
                        var max5 = max - 0.5f;
                        var min5 = 0.5f - min;
                        if (max5 > min5) {
                            min = 0.5f - max5;
                        } else if (min5 > max5) {
                            max = 0.5f + min5;
                        }
                        Util.normalize(value, min, max);
                    }
                }
            }
        });

    }

    public void onConceptTouch(Consumer<TaskConcept> c) {
        touchMode = c;
    }

    @Override
    public boolean updateTouch(Finger finger) {
        if (super.updateTouch(finger)) {

            updateTouchedConcept(finger);

            var c = this.touchConcept;
            if (c != null && finger.releasedNow(OPEN_CONCEPT_BUTTON) && !finger.dragging(OPEN_CONCEPT_BUTTON)) {
                NARui.conceptWindow(c, nar);
            } else {
                finger.test(affect);
            }
            return true;
        }

        return false;
    }

    private void updateTouchedConcept(Finger finger) {
        touchConcept = finger == null ? null :
                concept(touchPix.x, /*height() - 1 - */touchPix.y);
    }

    private @Nullable TaskConcept concept(int x, int y) {
        return x >= 0 && y >= 0 && x < width() && y < height() ?
                concept[x][y] : null;
    }

    public int height() {
        return concept.length;
    }

    public int width() {
        return concept[0].length;
    }

    void onTouch(TaskConcept touchConcept) {
        touchMode.accept(touchConcept);
    }

    @Override
    protected void starting() {
        super.starting();
        depth.set(nar.answerDepthBeliefGoal.asFloat());
        on = nar.onDur(this::accept);
    }

    @Override
    protected void stopping() {
        on.close();
        this.on = null;
        super.stopping();
    }

    private void accept(NAR n) {

        if (!showing())
            return;

        var baseDur = dur(n);

        var now = n.time() + Math.round((baseDur * timeShift.floatValue()));

        var truthCapacity = this.truthCapacity.intValue();

        this.answerTries = (int) Math.ceil(truthCapacity);


        if (answer == null)
            answer = new Answer(null, true, truthCapacity, nar);
        else
            answer.clear();

        var w = this.window.floatValue();

        long windowRadius = Math.round(baseDur * w / 2);
        long start = now - windowRadius, end = now + windowRadius;
        answer.time(start, end, durMatch());
        answer.depth(depth.floatValue());

        answer.durTruth = w * baseDur;

        for (var l : layers)
            l.init(this);

        update();
    }

    private static float dur(NAR n) {
        return n.dur();
    }

    protected float durMatch() {
        return dur.asFloat();
    }

    @Override
    public int color(int x, int y, int i) {

        Arrays.fill(rgb, 0);
        for (var l : layers)
            l.blend(l.value(x, w, y), l.opacity.floatValue(), rgb);

        return Draw.rgbInt(
                Util.unitize(rgb[0]), Util.unitize(rgb[1]), Util.unitize(rgb[2])
                /*, 0.5f + 0.5f * p*/);
    }

    public Splitting withControls() {
        return new Splitting(this, 0.1f, new Splitting(new ObjectSurface(layers), 0.5f, new CameraSensorViewControls()).resizeable()).resizeable();
    }

    public abstract static class Layer {
        public final FloatRange opacity = new FloatRange(0.75f, 0, 1);
        float[] value;

        public final void init(VectorSensorChart v) {
            if (value == null)
                value = new float[v.w * v.h];
            update(v);
        }

        public abstract void blend(float v, float opacity, float[] rgbTarget);

        public abstract void update(VectorSensorChart v);

        protected void update(VectorSensorChart v, IntIntToFloatFunction f) {
            var w = v.w;
            var h = v.h;
            var i = 0;
            for (var y = 0; y < h; y++) {
                for (var x = 0; x < w; x++) {
                    value[i++] = f.value(x, y);
                }
            }
        }

        public float value(int x, int w, int y) {
            return value[y * w + x];
        }
    }

    public abstract static class ColoredLayer extends Layer {

        private final float[] color;

        protected ColoredLayer(float r, float g, float b) {
            this.color = new float[]{r, g, b};
        }

        @Override
        public void blend(float v, float opacity, float[] rgb) {
            VectorSensorChart.blend(v * opacity, color, rgb);
        }

    }

    /**
     * TODO use DurSurface
     */
    class CameraSensorViewControls extends Gridding {

        private DurLoop on;

//        /** the procedure to run in the next duration. limits activity to one
//         * of these per duration. */
//        private final AtomicReference<Runnable> next = new AtomicReference<>();

//        @Override
//        protected void starting() {
//            super.starting();
////            on = view.nar.onDur(this::commit);
//        }

        CameraSensorViewControls() {
            super();

            /** TODO use MutableEnum */
            set(new ButtonSet<>(ButtonSet.Mode.One,
                            new CheckBox("Pri+", () -> {
                                onConceptTouch((c) -> {
                                    //TODO
                                });
//                            next.set(()-> {
//                                        //view.nar.activate(c, 1f)
//                                        throw new TODO();
//                                    }
//                            );
//                        });
                            }),


                            goalCheckBox("Goal-", 0f),
                            goalCheckBox("Goal-+", 0f, 1f),
                            goalCheckBox("Goal~", 0.5f),
                            goalCheckBox("Goal+-", 1f, 0f),
                            goalCheckBox("Goal+", 1f)
                    ), new ObjectSurface(view)
                    //TODO attn node plot: supply/demand
                    //new FloatSlider("Supply", view.sensor.attn.supply)
            );
        }

//        protected void commit() {
//            Runnable next = this.next.getAndSet(null);
//            if (next!=null) {
//                next.run();
//            }
//        }

        @Override
        protected void stopping() {
            on.close();
            on = null;
            super.stopping();
        }

        CheckBox goalCheckBox(String s, /* TODO */ float value) {
            return goalCheckBox(s, value, value);
        }

        /**
         * from,to allows specifying a transition, ex: (--x &&+1 x) or (x &&+1 --x) if they differe
         */
        CheckBox goalCheckBox(String s, /* TODO */ float fromValue, float toValue) {
            return new CheckBox(s, () -> {
                if (fromValue != toValue) {
                    onConceptTouch((c) -> {
                        //TODO
                    });
                } else {
                    onConceptTouch(c -> {
                        //next.set(() ->
                        long start, end;
                        final var now = nar.time();
                        var d = (int) Math.ceil(durMatch() / 2);
                        start = now - d;
                        end = now + d;
                        nar.want(c.term(), toValue, nar.confDefault(GOAL), start, end);
                        //);
                    });
                }
            });
        }

    }

    abstract class XYConcept implements IntIntToFloatFunction {

        protected abstract float floatValue(int x, int y, TaskConcept c);

        @Override
        public float value(int x, int y) {
            var cx = concept[x];
            if (cx != null) {
                var c = cx[y];
                if (c != null) {
                    var b = floatValue(x, y, c);
                    if (b == b) return b;
                }
            }
            return 0.5f; //black
            //noise();
        }

    }
}