package spacegraph.test;

import jcog.table.DataTable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.TsneRenderer;
import tech.tablesaw.api.DoubleColumn;

import static spacegraph.SpaceGraph.window;

public class TsneTest {


    public static void main(String[] args) {
        window(testTsneModel(), 500, 500);
    }

    public static Surface testTsneModel() {
        return TsneRenderer.view(dataExample(), null, 0, 0).widget();
    }

    private static DataTable dataExample() {
        DataTable data =
                new DataTable();
                //new ARFF(new File("/tmp/x.arff"));
        data.addColumns(DoubleColumn.create("a"), DoubleColumn.create("b"), DoubleColumn.create("c"));
        float n = 16;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                data.add(
                        (1+(i^j)%n)/n, Math.cos(i * 0.2), Math.sin(j * 0.2)
                        //Math.cos((i ^ (i-5))/n), Math.sin(i/10f), -0.5f + Math.random()
                );
            }
        }
        return data;
    }

//    @Test
//    void test1() {
//    //public static void main(String[] args) {
//
//
//        int DIM = 4;
//        int N = 128;
//        double[][] x = new double[N][DIM];
//        int j = 0;
//        for (int i = 0; i < N / 2; i++) {
//            x[j++] = new double[]{0, 0, 1 + Math.random() / 2f, 1 + Math.random() / 2f};
//        }
//        for (int i = 0; i < N / 2; i++) {
//            x[j++] = new double[]{1, 0, -1 + Math.random() / 2f, -1 + Math.random() / 2f};
//        }
//
//
//        SimpleTSne t = new SimpleTSne();
//
//        double[][] y = t.reset(x, new TSneConfig(
//                2,
//                false, true
//        ));
//        System.out.println(MatrixOps.doubleArrayToPrintString(y));
//
//        Surface plot = new PaintSurface() {
//
//            @Override
//            protected void paint(GL2 gl, ReSurface surfaceRender) {
//                Draw.bounds(bounds, gl, this::paint);
//            }
//
//            protected void paint(GL2 gl) {
//                t.next();
//
//                double[][] vv = t.Y;
//                if (vv == null)
//                    return;
//                vv = vv.clone();
//
//
//                float h;
//                float scale = 0.01f;
//                float w = h = 1f / vv.length * Math.min(w(), h())*scale;
//                for (int i = 0, yLength = vv.length; i < yLength; i++) {
//                    double[] v = vv[i];
//
//                    float x = (float) (v[0]);
//                    float y = (float) (((v.length > 1) ? v[1] : 0));
//
//                    x *= scale;
//                    y *= scale;
//
//                    Draw.colorHash(gl, i, 0.75f);
//                    Draw.rect(x, y, w, h, gl);
//                }
//            }
//        };
//
//        {
//            window(plot, 800, 800);
//        }
//
//        Util.sleepMS(10000);
//    }

}