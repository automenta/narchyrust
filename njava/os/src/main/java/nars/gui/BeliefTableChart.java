package nars.gui;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES3;
import jcog.Util;
import jcog.pri.op.PriMerge;
import jcog.signal.FloatRange;
import nars.NAR;
import nars.TaskTable;
import nars.term.Termed;
import nars.truth.TruthWave;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToFloatFunction;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Labeled;
import spacegraph.space2d.MenuSupplier;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.grid.Containers;
import spacegraph.space2d.container.unit.Clipped;
import spacegraph.space2d.meta.obj.ObjectSurface;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import java.util.Arrays;

import static nars.TruthFunctions.c2e;
import static nars.TruthFunctions.e2c;


public class BeliefTableChart extends Stacking implements Labeled, MenuSupplier {

    public final BeliefTableChartParams beliefTableChartParams;


    private static final float h = 0.95f;

    private final Termed term;
    private final TruthGrid beliefGrid;
    private final TruthGrid goalGrid;

    /**
     * (if > 0): draw additional projection wave to show truthpolation values for a set of evenly spaced points on the visible range
     */

    private transient float freqResolution;
    private NAR nar;

    @Override
    protected void paintIt(GL2 gl, ReSurface r) {
        gl.glColor3f(0, 0, 0);
        Draw.rect(bounds, gl);
    }

    /**
     * TODO use double not float for precision that may be lost
     *
     * @param y (freq,conf)->y
     */
    private void renderWaveLine(GL2 gl, TruthWave wave, Colorize colorize) {
        gl.glLineWidth(4);
        gl.glBegin(GL.GL_LINE_STRIP);

        wave.forEach((freq, conf, start, end) -> {
            colorize.colorize(gl, freq, conf);
            gl.glVertex2f((xTime(start) + xTime(end))/2, y(freq));
        });

        gl.glEnd();
    }

    public void renderWaveArea(long minT, long maxT, GL2 gl, TruthWave wave, FloatFloatToFloatFunction y, Colorize colorize) {

        gl.glBegin(GL2ES3.GL_QUADS);

        float midY = y.valueOf(0.5f, 0.5f);
//
//        gl.glVertex2f(xTime(minT, minT, maxT), midY);
//        gl.glVertex2f(xTime(minT, minT, maxT), midY);

        wave.forEach((freq, conf, start, end) -> {

            long end1 = end;
            if (start > maxT || end1 < minT)
                return;

            colorize.colorize(gl, freq, conf);

            float Y = y.valueOf(freq, conf);

            float x1 = xTime(start);
            gl.glVertex2f(x1, midY);
            gl.glVertex2f(x1, Y);

            if (start == end1)
                end1 = start + 1;

            float x2 = xTime(end1);
            gl.glVertex2f(x2, Y);
            gl.glVertex2f(x2, midY);

        });

//        gl.glVertex2f(xTime(maxT, minT, maxT), midY);
//        gl.glVertex2f(xTime(maxT, minT, maxT), midY);

        gl.glEnd();
    }

    private BeliefTableChart set(long s, long e) {
        this.beliefTableChartParams.start = s;
        this.beliefTableChartParams.end = e;
        return this;
    }

    @FunctionalInterface
    public interface Colorize {
        void colorize(GL2 gl, float f, float c);
    }
    @Deprecated private static final boolean modeTasksOrHistogram =
            true;
            //false;

    class TruthGrid extends PaintSurface {

        private final TruthWave projected;
        private final TruthWave tasks;
        private final Colorize colorizeLine;
        private final Colorize colorizeFill;
        private static final float taskWidthMin = 0.005f;
        private static final float taskHeightMin = 0.04f;

        private final boolean beliefOrGoal;

        TruthGrid(boolean beliefOrGoal) {
            super();
            this.beliefOrGoal = beliefOrGoal;
            projected = new TruthWave(1);
            tasks = new TruthWave(1024);
            this.colorizeLine = beliefOrGoal ?
                    (gl, f, c) -> {
                        float a = 0.6f + 0.25f * c;
                        float i = 0.25f + 0.6f * c;  //intensity
                        float j = 0; //0.05f * (1 - c);
                        gl.glColor4f(i, j, j, a);
                    }
                    :
                    (gl, f, c) -> {
                        float a = 0.6f + 0.25f * c;
                        float i = 0.25f + 0.6f * c;  //intensity
                        float j = 0; //0.05f * (1 - c);
                        gl.glColor4f(j, i, j, a);
                    };
            this.colorizeFill = beliefOrGoal ?
                (gl, f, c) -> gl.glColor4f(1, 0, 0, colorizeFillAlpha(c))
                :
                (gl, f, c) -> gl.glColor4f(0, 1, 0, colorizeFillAlpha(c));

        }

        private static float colorizeFillAlpha(float c) {
            //return 0.1f + 0.5f * c;
            return 0.25f + 0.5f * (c * c);
        }


        void update(TaskTable table, int projections, NAR nar) {
            //BeliefTable table = (BeliefTable) c.table(beliefOrGoal ? BELIEF : GOAL);

            if (table.isEmpty()) {
                projected.clear();
                tasks.clear();
            } else {

//            int dither = Math.max(1,
//                    (int) Math.round(((double) (end - start)) / (projections)));
                long projStart = beliefTableChartParams.start; //Util.round((double)start, dither);
                long projEnd = beliefTableChartParams.end; //Math.max(Util.round((double)end, dither), Util.round((double)start + 1, dither));

                //float dur = (end-start) * projectDurs.floatValue() / projections;

                projected.project(table, projStart, projEnd, projections, term.term(), 0, nar);
                tasks.set(table, beliefTableChartParams.start, beliefTableChartParams.end);
            }

            if (!modeTasksOrHistogram)
                histogramUpdate(tasks);

        }

        @Override
        protected void paint(GL2 _gl, ReSurface reSurface) {
            Draw.bounds(bounds, _gl, this::doPaint);
        }

        void doPaint(GL2 gl) {
            if (modeTasksOrHistogram) {
                renderTasks(gl, tasks, colorizeFill);
            } else {
                histogramRender(gl, tasks);
            }

            renderWaveLine(gl, projected, colorizeLine);

            renderPresentLine(gl);
        }

        private void renderPresentLine(GL2 gl) {
            float x = xTime(beliefTableChartParams.now);
            gl.glColor3f(0.5f, 0.5f, 0.5f);
            gl.glLineWidth(2f);
            Draw.linf(x, 0, x, 1, gl);
        }

//        private void renderNodes(GL2 gl, TruthWave tasks) {
//            TaskTable table = tasks.table;
//            if (table instanceof BeliefTables)
//                ((BeliefTables)table).tables.forEach(b -> renderBeliefTable(gl, b));
//            else
//                renderBeliefTable(gl, table);
//        }
//
//        private void renderBeliefTable(GL2 gl, TaskTable t) {
////            if (t instanceof RTreeBeliefTable)
////                renderRTreeBeliefTable(gl, ((RTreeBeliefTable)t));
//        }

//        private void renderRTreeBeliefTable(GL2 gl, RTreeBeliefTable t) {
//
//            if (t.isEmpty())
//                return;
//
//            gl.glLineWidth(2f);
//            gl.glColor4f(0.5f, 0.5f, 0.5f, 0.75f);
//
////            float fEps = nar.freqResolution.floatValue()/2;
//
//            t.streamNodes().filter(Objects::nonNull).map(n -> (TaskRegion) n.bounds()).filter(b -> b != null && !(b instanceof Task) && b.intersects(start, end)).forEach(b -> {
//                float x1 = xTime(b.start());
//                float x2 = xTime(b.end());
//                float y1 = b.freqMin();
//                float y2 = b.freqMax();
//                Draw.rectStroke(x1, y1, x2 - x1, y2 - y1, gl);
//            });
//        }

        float[][] h;

        /** ideally odd */
        int horRes = 31;

        /** ideally odd */
        int maxVertRes = 9;
        int vertRes;

        private void histogramUpdate(TruthWave wave) {

            boolean e2c = true;
            var merge =
                PriMerge.plus;
                //PriMerge.max;

            vertRes = Math.min(maxVertRes, Math.round(1/freqResolution));
            float dt = (float)((double)(end()-BeliefTableChart.this.start()))/horRes;

            if (h == null)
                h = new float[horRes][vertRes];

            for (int x = 0; x < horRes; x++)
                Arrays.fill(h[x], 0);

            wave.forEach((freq, conf, s, e) -> {
                double xs = xTime(s), xe = xTime(e);
                int xhs = Util.clampSafe((int)Math.round(xs * (horRes-1)), 0, horRes-1),
                    xhe = Util.clampSafe((int)Math.round(xe * (horRes-1)), 0, horRes-1);
                int yh = Math.round(freq * (vertRes - 1));
                float value = e2c ? (float) c2e(conf) : conf;
                for (int x = xhs; x <= xhe; x++)
                    h[x][yh] = (float) merge.valueOf(h[x][yh], value);
            });

            //pre-normalize
            float max = 0;
            for (int x = 0; x < horRes; x++) {
                float[] hx = h[x]; for (int y = 0; y < vertRes; y++) {
                    float hxy = hx[y];
                    if (e2c) {
                        hxy = (float) e2c(hxy);
                        hx[y] = hxy;
                    }
                    max = Math.max(max, hxy);
                }
            }

            //NORMALIZE
            if (max > 0) {
                for (int x = 0; x < horRes; x++) {
                    float[] hx = h[x];
                    for (int y = 0; y < vertRes; y++)
                        hx[y] = hx[y] / max;
                }
            }
        }


        /** TODO use a BitmapMatrix */
        private void histogramRender(GL2 gl, TruthWave wave) {
            var h = this.h;
            if (h == null)
                return;

            float rw = 1f/(horRes-1), rh = 1f/(vertRes-1);
            for (int x = 0; x < horRes; x++) {
                float[] hx = h[x];
                for (int y = 0; y < vertRes; y++) {
                    float hxy = hx[y];
                    if (beliefOrGoal)
                        gl.glColor4f(hxy, 0, 0, 0.5f);
                    else
                        gl.glColor4f(0, hxy, 0, 0.5f);

                    Draw.rect(x * rw, (y - 0.5f) * rh, rw, rh, gl);
                }
            }
        }

        private void renderTasks(GL2 gl, TruthWave wave, Colorize colorize) {

            float ph = Math.max(taskHeightMin, freqResolution);

            wave.forEach((freq, conf, s, e) -> {

                float start = xTime(s);
                if (start > 1)
                    return;

                float end = xTime(e + 1);
                if (end < 0)
                    return;

                colorize.colorize(gl, freq, conf);

                float yBottom = BeliefTableChart.y(freq) - ph / 2;
                float width = end - start;
                if (width < taskWidthMin) {
                    //point-like
                    float w = taskWidthMin; //visible width
                    float center = (end + start) / 2;
//                    float yMid = freq;
                    float thick = taskWidthMin/2;
                    //Draw.rectFrame(center, yMid, w, thick, ph, gl);
                    Draw.rectCross(center - w / 2, yBottom, w, ph, thick, gl);
                } else {
                    //solid
                    Draw.rect(start, yBottom, width, ph, gl);
                }

            });
        }


    }

    private static float y(float y) {
        //TODO map to space within some margin
        return ((y - 0.5f) * h) + 0.5f;
    }

    public long start() {
        return beliefTableChartParams.start;
    }
    public long end() {
        return beliefTableChartParams.end;
    }

    private float xTime(long x) {
        //o = Util.clampSafe(o, start, end);
        long s = start();
        return (float) (((double) (x - s)) / (end() - s));
    }


    public BeliefTableChart(Termed x) {
        this(x, new BeliefTableChartParams());
    }

    public BeliefTableChart(Termed term, BeliefTableChartParams p) {
        super();
        this.beliefTableChartParams = p;
        this.term = term;

        add(new Clipped(beliefGrid = new TruthGrid(true)));
        add(new Clipped(goalGrid = new TruthGrid(false)));
    }

    public BeliefTableChart update(NAR nar) {

        long now = this.beliefTableChartParams.now = nar.time();

        //TODO different time modes
        float narDur = nar.dur();
        double visDurs = this.beliefTableChartParams.rangeDurs.doubleValue();
        long start = now - Math.round(visDurs * narDur);
        long end = now + Math.round(visDurs * narDur);
        if (end == start) end = start + 1;

        return update(nar, start, end);
    }

    public BeliefTableChart update(NAR nar, long s, long e) {
        set(s,e);
        @Nullable TaskTable beliefs = nar.table(term, true);
        @Nullable TaskTable goals = nar.table(term, false);
        if (beliefs == null && goals == null)
            return this;
        this.freqResolution = nar.freqRes.floatValue();

        this.nar = nar;

        if (beliefs!=null)
            beliefGrid.update(beliefs, beliefTableChartParams.projections, nar);
        if (goals!=null)
            goalGrid.update(goals, beliefTableChartParams.projections, nar);

        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + term + ']';
    }


    @Override
    public Surface label() {
        return new VectorLabel(term.toString());
    }

    @Override
    public Surface menu() {
        /* TODO */
        return Containers.row(new ObjectSurface(this), 0.9f,
                PushButton.iconAwesome("search-plus").clicked(() -> NARui.conceptWindow(term, nar /* TODO */)));
    }


    public static class BeliefTableChartParams {
        public final FloatRange rangeDurs = new FloatRange(32, 0.5f, 2048f);
        //public final FloatRange projectDurs = new FloatRange(1, 0, 32);
        public long start;
        public long end;
        public long now;
        public int projections = 15;

        public BeliefTableChartParams() {
        }
    }
}