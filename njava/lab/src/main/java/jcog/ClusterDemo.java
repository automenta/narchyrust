package jcog;

import jcog.cluster.KMeansPlusPlus;
import jcog.data.DistanceFunction;
import jcog.data.list.Lst;
import jcog.exe.Loop;
import jcog.random.XoRoShiRo128PlusRandom;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.meter.ScatterPlot2D;
import spacegraph.video.Draw;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.stream.IntStream;

import static spacegraph.SpaceGraph.window;

public class ClusterDemo {
    public static void main(String[] args) {

        int W = 16, H = 16;
        int C = 4;
        float maxDensity = 1/3f;
        int iterationsMax = 3;

        ArrayDeque<int[]> points = new ArrayDeque();
        int maxPoints = (int)Math.ceil((W*H)*maxDensity);

        DistanceFunction d =
            //DistanceFunction::distanceManhattan;
            DistanceFunction::distanceCartesian;

        KMeansPlusPlus<int[]> n = new KMeansPlusPlus<>(C, 2, d, new XoRoShiRo128PlusRandom()) {
            @Override public void coord(int[] x, double[] y) {
                y[0] = x[0]; y[1] = x[1];
            }
        };


        BitmapMatrixView b = new BitmapMatrixView(W, H, (x, y, i) -> {
            boolean point = contains(points, x, y);

            var centroid = n.nearest(new int[]{x, y});

            float c = centroid / ((float) C);
            return Draw.colorHSB(c, point ? 0.5f : 1f, point ? 1 : 0.5f);
        });
        ScatterPlot2D<double[]> s = new ScatterPlot2D<>(new ScatterPlot2D.SimpleXYScatterPlotModel<>() {
            @Override
            public void coord(double[] c, float[] target) {
                target[0] = (float) c[0] / W;
                target[1] = (float) c[1] / H;
            }

            @Override
            public float width(double[] o, int n) {
                return 0.1f;
            }

            @Override
            public float height(double[] o, int n) {
                return 0.1f;
            }
        });
        Loop.of(() -> {

            boolean learn = true;
            final int pn = points.size();
            if (b.touchState > 0) {
                int tx = b.touchPix.x, ty = b.touchPix.y;
                if (tx >= 0 && ty >= 0) {
                    if (!contains(points, tx, ty)) {
                        points.addLast(new int[]{tx, ty});
                        if (pn > maxPoints)
                            points.removeFirst();
                    }
                }
            }

            if (learn) {
                if (pn > 1) {
                    n.cluster(new Lst<>(points), iterationsMax);
                    n.sortClusters();
                    s.set(IntStream.range(0, Math.min(n.clusterCountMax, pn)).mapToObj(n::coordCluster));
                    for (var c : n.clusters) {
                        System.out.println(c);
                    }
                    System.out.println();
                }

                b.updateIfShowing();
            }
        }).fps(8);

        window(new Splitting(b, 0.5f, s).horizontal(), 1200, 600);
    }


    private static boolean contains(ArrayDeque<int[]> points, int x, int y) {
        int[] xy = {x, y};
        for (int[] p : points) {
            if (Arrays.equals(p, xy))
                return true;
        }
        return false;
    }
}