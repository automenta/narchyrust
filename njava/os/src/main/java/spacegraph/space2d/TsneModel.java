package spacegraph.space2d;

import com.jujutsu.tsne.TSne;
import com.jujutsu.tsne.TSneConfig;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.data.set.ArrayHashSet;
import jcog.data.set.ArraySet;
import jcog.signal.FloatRange;
import jcog.signal.IntRange;
import jcog.table.DataTable.Instance;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.util.MutableRectFloat;

import java.util.HashMap;

//TODO maxGain parameter for SimpleTsne
//TODO dynamic perplexity control
public class TsneModel implements Graph2D.Graph2DUpdater<Instance> {

    final TSne s = new TSne();

    public final IntRange iters = new IntRange(1, 0, 64);

    public final FloatRange nodeScale = new FloatRange(5, 0, 25);
    public final FloatRange nodeExp = new FloatRange(2, 0, 5);

    public final FloatRange perplexity = s.perplexity;
    public final FloatRange alpha = s.alpha;

    float nodeMinScale = 0.01f;
    int scaleColumn;
    float margin = 0.1f;

    /** columns selector for the dimensions input to tSNE */
    private final MetalBitSet vectorCols;

//    private double[][] previousY;
//    private HashMap<Instance, double[]> instanceCoordinates;

    @Deprecated public TsneModel(MetalBitSet vectorCols, int scaleColumn) {
        this.vectorCols = vectorCols;
        this.scaleColumn = scaleColumn;
    }

    final TSneConfig config = new TSneConfig(
             2,
            false, true
    );

    final ArraySet<NodeVis<Instance>> xx = new ArrayHashSet<>();
    final ArrayHashSet<NodeVis<Instance>> nn = new ArrayHashSet<>();

    private double[][] X = new double[0][0];

    @Override public void update(Graph2D<Instance> g, float dtS) {
        int iters = this.iters.getAsInt();
        if (iters < 1)
            return; //paused

        nn.clear();
        g.forEachValue(nn::add);

        if (nn.isEmpty() && xx.isEmpty())
            return;

        boolean needsReset = s.X == null || !nn.equals(xx);

        if (needsReset) {
//            // Store current coordinates before reset
//            int rows = nn.size();
//            if (previousY != null && instanceCoordinates == null) {
//                instanceCoordinates = new HashMap<>(rows);
//                int xn = xx.size();
//                for (int i = 0; i < xn; i++)
//                    instanceCoordinates.put(xx.get(i).id, previousY[i]);
//            }

            xx.clear();
            xx.addAll(nn);


            int d = vectorCols.cardinality();

//            if (rows > 0) {
//
//                //TODO write an overridable extractor method
////                    int cols = xx.get(0).id.data.size()-1;
//                if (X.length != rows /*|| X[0].length != cols*/)
//                    X = new double[rows][];
//
//                int j = 0;
//                for (NodeVis<Instance> i : xx) {
//                    if (X[j] == null || X[j].length!=d)
//                        X[j] = new double[d];
//
//                    i.id.toDoubleArray(vectorCols, X[j]);
//                    j++;
//                }
//            } else {
//                X = new double[0][0];
//            }

            var m = new HashMap<>();
            for (var i : xx)
                m.put(i, i.id.toDoubleArray(vectorCols, new double[d]));
            s.reset(m, config);
            X = s.X;

//            // Apply stored coordinates to matching instances after reset
//            if (instanceCoordinates != null) {
//                for (int i = 0; i < rows; i++) {
//                    Instance instance = xx.get(i).id;
//                    double[] storedCoord = instanceCoordinates.get(instance);
//                    if (storedCoord != null) {
//                        s.Y[i] = storedCoord.clone();
//                    }
//                }
//                instanceCoordinates = null; // Clear the stored coordinates
//            }
        }


//        double gcx = g.cx(), gcy = g.cy();

        double[][] Y = s.next(iters);



//        previousY = Y; // Store current Y for potential future restore
        int j = 0;

        if (xx.isEmpty()) return;

        //double magnify = g.radius(); ///Math.sqrt(n+1);// * g.radius();// / (float) Math.sqrt(n+1);
        float W = g.w(), H = g.h();
        float nodeScale = this.nodeScale.floatValue();

        float xMin = Float.POSITIVE_INFINITY, xMax = Float.NEGATIVE_INFINITY,
                yMin = Float.POSITIVE_INFINITY, yMax = Float.NEGATIVE_INFINITY;
        int jMax = xx.size();
        for (NodeVis<Instance> i : xx) {
            double[] Yj = Y[j];
            float x = (float) Yj[0], y = (float) Yj[1];
            if (x == x && y == y) {
                xMin = Math.min(x, xMin);
                xMax = Math.max(x, xMax);
                yMin = Math.min(y, yMin);
                yMax = Math.max(y, yMax);
            }
            j++;
            if (j == jMax)
                break;
        }

        @Deprecated float m = margin * Math.min(W, H); //margin
        float Wm = W-2*m, Hm = H-2*m;
        float nodeExp = this.nodeExp.asFloat();

        j = 0;
        for (NodeVis<Instance> i : xx) {
            double[] Yj = Y[j];
            float xy0 = (float) Yj[0], xy1 = (float) Yj[1];
            if (xy0!=xy0 || xy1!=xy1) {
//                //HACK
//                xy0 = (xMin+xMax)/2; //ThreadLocalRandom.current().nextFloat(xMin, xMax);
//                xy1 = (yMin+yMax)/2; //ThreadLocalRandom.current().nextFloat(yMin, yMax);
                continue;
            }
            float x = g.x() + Util.normalize(xy0, xMin, xMax)*Wm + m;
            float y = g.y() + Util.normalize(xy1, yMin, yMax)*Hm + m;

            float s = scaleColumn >= 0 ? ((Number) xx.get(j).id.data.get(scaleColumn)).floatValue() : 1;

            float scaleBase = nodeMinScale + nodeScale * Math.min((float) 1, s);
            float scale = (float) Math.pow(scaleBase, nodeExp); //customized: first column as size TODO normalize

            MutableRectFloat<Instance> I = i.m;
            I.setXYWH(x, y, scale, scale);
            I.commitLerp(1);
//                double z = (Yj[2] = Util.clamp(Yj[2], -0.5, +0.5)); //narrow but room to maneuver
//                s.gains[j][2] = 0; //clear z gains
//                //Arrays.fill(s.gains[j], 0.0); //reset gains


            i.color(1,1,1,1);

            //i.pos(i.bounds.clamp(g.bounds));
//                Yj[0] = i.left()/scale;
//                Yj[1] = i.top()/scale;
            I.commitLerp(1f);
            j++;
        }

    }

}