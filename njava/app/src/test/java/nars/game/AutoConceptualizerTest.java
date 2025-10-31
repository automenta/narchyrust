package nars.game;

import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.func.AutoConceptualizer;
import nars.sensor.BitmapSensor;
import org.junit.jupiter.api.Test;

import java.util.List;

public class AutoConceptualizerTest extends BitmapSensorTest {

    @Test
    void test1() {
        NAR n = NARS.tmp();
        tmp = new MyGame();

        int H = 4;
        int W = 4;
        BitmapSensor<?> c = new BitmapSensor<>(new Bitmap2D() {
            @Override
            public int width() {
                return W;
            }

            @Override
            public int height() {
                return H;
            }

            @Override
            public float value(int x, int y) {
                return (((x + y + n.time())) % 2) > 0 ? 1f : 0f;
            }
        }, $::p);
        tmp.addSensor(c);
        n.add(tmp);

        AutoConceptualizer ac = new AutoConceptualizer($.inh(c.term(), "auto"),
                List.of(c.concepts.iter.order) /* HACK */, true, 2, n);

        n.log();
        for (int i =0; i< 155; i++) {
//            next(n, ac);
            n.run(1);
        }


    }
}