package nars.truth.proj;

import nars.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.*;

class TruthProjectionTest {

    private static final AtomicInteger serial = new AtomicInteger();
    private static final Term x = $$("x");

    @Test
    void testEqualEvidenceInSubInterval_of_Task() {
        //TODO repeat test for TruthletTask to test trapezoidal integration
        NALTask T = t(1, 0.5f, 0, 4);
        for (int s = 0; s <=4; s++)
            for (int e = s; e<=4; e++ )
                assertEquals(T.truth(), T.truth(s, e, 0, 0, NAL.truth.EVI_MIN));
        assertTrue( (float) T.truth(2, 10, 0, 0, NAL.truth.EVI_MIN).conf() < (float) T.truth(2, 5, 0, 0, NAL.truth.EVI_MIN).conf());
        assertEquals(T.truth(), T.truth(0, 4, 0, 0, NAL.truth.EVI_MIN));
        assertEquals(T.truth(), T.truth(0, 4, 0, 0, NAL.truth.EVI_MIN));
        assertEquals(T.truth(), T.truth(2, 3, 0, 0, NAL.truth.EVI_MIN));

//        Truth wa = T.truth(2, 5, 1);
//        Truth w = Truth.weaker(T.truth(), wa);
//        assertNotEquals(T.truth(), w);
    }

    @Test
    void testEvidenceIntegration_ConservedSingleTask_Full_Duration() {
        int W = 10;
        for (int i = W-1; i >=1; i--)  {
            IntegralTruthProjection t = new IntegralTruthProjection(0, W);
            NALTask T = t(1, 0.5f, 0, i);
            t.add(T);
            Truth tt = t.truth(0, i);
            assertNotNull(tt);
//            System.out.println(T + "\t" +
//                    T.eviAvg(0, i, 1) + " eviInteg\t => " + tt + ";evi=" + tt.evi());

            /*assertEquals(T.freq(), tt.freq());
            assertEquals(T.evi() * ((float)i)/W, tt.evi(), 0.1f); //percentage of evidence being occluded
            assertEquals(T.conf(), tt.conf(), 0.01f);*/
        }
    }
//
//    @Test
//    public void testEvidenceIntegration_ConservedSingleTask_Half_Duration() {
//        float conf = 0.5f;
//        NALTask t = t(1, conf, 0, 10);
//
//        //monotonically decrease evidence further away from task, regardless of observation time
//        for (long now : new long[] { -10, -5, 0, 5, 10, 15}) {
//            assertTrue(t.eviRelative(1, now) == t.eviRelative(9, now));
//            assertTrue(t.eviRelative(-1, now) > t.eviRelative(-2, now));
//            assertTrue(t.eviRelative(10, now) > t.eviRelative(11, now));
//            assertTrue(t.eviRelative(11, now) > t.eviRelative(12, now));
//        }
//        //monotonically decrease evidence further away from task, regardless of tgt time
//        for (long tgt : new long[] { -10, -5, 0, 5, 10, 15}) {
//            double a = t.eviRelative(tgt, 0);
//            double b = t.eviRelative(tgt, 20);
//            assertTrue(a > b);
//        }
//
//
//        LinearTruthProjection p = new LinearTruthProjection(0, 10);
//        p.add(t);
//        @Nullable Truth tt = p.truth();
//        assertEquals(1, tt.freq());
//        assertEquals(conf, tt.conf());
//    }


    private static NALTask t(float freq, float conf, long start, long end) {
        long[] stamp = { serial.getAndIncrement() };
        return t(freq, conf, start, end, stamp);
    }

    private static NALTask t(float freq, float conf, long start, long end, long... stamp) {
        return NALTask.taskUnsafe(x, BELIEF, $.t(freq, conf), start, end, stamp);
    }

    @Test void testDisjointProjection() {
        MutableTruthProjection t = new IntegralTruthProjection(3);
        /*
        0 = {SerialTask@7258} "$.38 ("-aä0èÊÔÈUI"-->happy). 39665⋈39687 %.50;.25%"
        1 = {SerialTask@7259} "$.38 ("-aä0èÊÔÈUI"-->happy). 39604⋈39626 %.50;.25%"
        2 = {SerialTask@7260} "$.38 ("-aä0èÊÔÈUI"-->happy). 39458⋈39480 %.50;.25%"
         */
        t.time(39690, 39710);
        t.dur(21);
        t.add(t(0.5f, 0.25f, 39665,39687));
        t.add(t(0.5f, 0.25f, 39604,39626));
        t.add(t(0.5f, 0.25f, 39458,39480));
        NALTask y = t.task();
        assertTrue(y.range() > 1);

    }

    @Test void dominatorOutweighed() {
        MutableTruthProjection t = new IntegralTruthProjection(3);
        t.add(t(1, 0.5f, 0, 1, 1L, 2L));
        t.add(t(1, 0.4f, 0, 1, 1L));
        t.add(t(1, 0.4f, 0, 1, 2L));
        NALTask y = t.task();
        assertTrue(y.conf() > 0.5f);
        assertEquals(2, y.stamp().length);
    }
    @Test void secondaryDominatorOutweighed() {
        MutableTruthProjection t = new IntegralTruthProjection(3);
        t.add(t(1, 0.5f, 0, 1, 3L));
        t.add(t(1, 0.4f, 0, 1, 1L, 2L, 4L)); //<- eliminate
        t.add(t(1, 0.38f, 0, 1, 1L));
        t.add(t(1, 0.38f, 0, 1, 2L));
        NALTask y = t.task();
        assertEquals("[1, 2, 3]", Arrays.toString(y.stamp()));
        assertTrue(y.conf() > 0.6f);
    }
    @Test void dominatorNotOutweighed() {
        //dominator is stronger
        MutableTruthProjection t = new IntegralTruthProjection(3);
        t.add(t(1, 0.6f, 0, 1, 1L, 2L));
        t.add(t(1, 0.4f, 0, 1, 1L));
        t.add(t(1, 0.4f, 0, 1, 2L));
        NALTask y = t.task();
        assertEquals(0.6f, y.conf());
    }
    @Test void monotonicSanity() {
        MutableTruthProjection t = new IntegralTruthProjection(3);
        t.add(t(1, 0.4f, 0, 1, 1L));
        t.add(t(1, 0.3f, 0, 1, 1L));
        t.add(t(1, 0.2f, 0, 1, 1L));
        NALTask y = t.task();
        assertEquals(0.4f, y.conf());
    }

    @Test void monotonicSanity2() {
        MutableTruthProjection t = new IntegralTruthProjection(4);
        t.add(t(1, 0.4f, 0, 1, 1L));
        t.add(t(1, 0.3f, 0, 1, 1L));
        t.add(t(1, 0.2f, 0, 1, 1L));
        t.add(t(1, 0.1f, 0, 1, 1L));
        NALTask y = t.task();
        assertEquals(0.4f, y.conf());
    }
    @Test void monotonicSanity3() {
        MutableTruthProjection t = new IntegralTruthProjection(4);
        t.add(t(1, 0.4f, 0, 1, 1L));
        t.add(t(1, 0.3f, 0, 1, 1L));
        t.add(t(1, 0.2f, 0, 1, 1L));
        t.add(t(1, 0.1f, 0, 1, 2L));
        NALTask y = t.task();
        assertTrue(0.41f < y.conf(), y::toString);
        assertEquals(2, y.stamp().length);
    }

}