package nars.game;

import jcog.signal.wave2d.ArrayBitmap2D;
import nars.*;
import nars.concept.TaskConcept;
import nars.sensor.BitmapSensor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class BitmapSensorTest {

    @Test
    void test1() {
        int w = 2, h = 2;

        NAR n = NARS.tmp();
        float[][] f = new float[w][h];


        //n.log();

        Term aPixel = $$("x(0,0)");
        //Term why = c.in.why.why;

        tmp = new MyGame();

        BitmapSensor c = new BitmapSensor(new ArrayBitmap2D(f), $.atomic("x"));
        tmp.addSensor(c);

        n.add(tmp);

//        int[] causesDetected = {0};
//        tmp.focus().onTask(t -> {
//            if (t.term().equals(aPixel)) {
////               if (t.why() == null || t.why() instanceof Int) //ignore revisions
////                    assertEquals(why, t.why());
//                if (((NALTask) t).stamp().length <= 1)
//                    causesDetected[0]++;
//            }
//        });

        {
            Assertions.assertEquals(0L, n.time());
            //System.out.println("all 0");
            for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 0f;
            n.run(1);
            Assertions.assertEquals(1L, n.time());
            assertEquals(c, f, n.time(), n);
        }


        n.run(3);


        {
            //System.out.println("all 1");
            for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 1f;
            n.run(3);
            assertEquals(c, f, n.time(), n);
        }


        {
            //System.out.println("all 0");
            for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 0f;
            n.run(1);
            assertEquals(c, f, n.time(), n);
        }

        n.run(3);

        {
            //System.out.println("all 1");
            for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 1f;
            n.run(1);
            assertEquals(c, f, n.time(), n);
        }

//        assertTrue(causesDetected[0] > 0);



    }

    MyGame tmp;

//    void next(NAR n, AbstractSensor c) {
//        //tmp.prev(n.time() - 2);
//        tmp.now(n.time() - 1 );
//        tmp.next(); //update
//        //c.accept(tmp);
//    }

    private static final float tolerance = 0.47f;

    static void assertEquals(BitmapSensor c, float[][] f, long when, NAR n) {

        for (int i = 0; i < c.width; i++) {
            for (int j = 0; j < c.height; j++) {
                TaskConcept p = c.get(i, j);
                Truth t = n.beliefTruth(p, when);
                if (t == null || Math.abs(f[i][j] - t.freq()) > tolerance) {
                    System.err.println("pixel " + p + " incorrect @ t=" + n.time() + " for " + when + ": = " + t);
                    n.beliefTruth(p, when);
                    p.beliefs().print(System.out);
                }
                assertNotNull(t, ()->p.term + " is null");
                Assertions.assertEquals(f[i][j], t.freq(), tolerance, ()->p + " has inaccurate result @ t=" + n.time());
            }
        }
    }

     static class MyGame extends Game {
        MyGame() {
            super("tmp");
        }


    }

}