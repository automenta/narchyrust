package nars.truth;

import jcog.Fuzzy;
import jcog.Util;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.NAL;
import nars.Truth;
import nars.TruthFunctions;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static nars.$.t;
import static nars.NAL.truth.FREQ_EPSILON;
import static org.junit.jupiter.api.Assertions.*;


class TruthTest {

    @Test
    void testRounding() {
        int discrete = 10;
        float precision = 1f / discrete;

        for (float x = 0.19f; x < 0.31f; x += 0.01f / 2) {
            int i = Util.toInt(x, discrete);
            float y = Util.toFloat(i, discrete);
//            System.out.println(x + " -> " + i + " -> " + y + ", d=" + (Math.abs(y - x)));
            assertTrue(
                    Util.equals(
                            x,
                            y,
                            precision / 2f)
            );
        }
    }

    @Test
    void testEternalize() {
        assertEquals(0.47f, TruthFunctions.e2c(t(1, 0.9f).eviEternalized(1)), 0.01f);
        assertEquals(0.31f, TruthFunctions.e2c(t(1, 0.45f).eviEternalized(1)), 0.01f);
        assertEquals(0.09f, TruthFunctions.e2c(t(1, 0.10f).eviEternalized(1)), 0.01f);
    }

    @Test void testPreciseTruthEquality1() {
        assertEquals(t(0.5f, 0.5f), t(0.5f, 0.5f));
        float freqInsignificant = FREQ_EPSILON / 5;
        assertEquals(t(0.5f, 0.5f),
                t(0.5f + freqInsignificant, 0.50f));
    }
    @Test void testPreciseTruthEquality2() {
        float freqSignificant = FREQ_EPSILON;
        PreciseTruth aa = t(0.5f, 0.5f);
        PreciseTruth bb = t(0.5f + freqSignificant, 0.5f);
        assertNotEquals(aa, bb);
    }

    @Test void testPreciseTruthEquality3() {
        double confSignificant = NAL.truth.CONF_MIN;
        PreciseTruth x = t(0.5f, 0.5f);
        assertEquals(x, x);
        assertNotEquals(x, t(0.5f, 0.5f + confSignificant));
        assertEquals(x, t(0.5f, 0.5f + confSignificant/4));
    }

//    @Disabled
//    @Test
//    void testDiscreteTruth_FreqEquality() {
//        Truth a = PreciseTruth.byConf(1, 0.9f);
//        Truth aCopy = PreciseTruth.byConf(1, 0.9f);
//        assertEquals(a, aCopy);
//
//        float ff = 1 - TRUTH_EPSILON / 2.5f;
//        assertEquals(1, Util.round(ff, TRUTH_EPSILON));
//        Truth aEqualWithinThresh = PreciseTruth.byConf(
//                ff /* slightly less than half */,
//                0.9f);
//        assertEquals(a, aEqualWithinThresh);
//        assertEquals(a.hashCode(), aEqualWithinThresh.hashCode());
//
//        Truth aNotWithinThresh = PreciseTruth.byConf(1 - TRUTH_EPSILON * 1, 0.9f);
//        assertNotEquals(a, aNotWithinThresh);
//        assertNotEquals(a.hashCode(), aNotWithinThresh.hashCode());
//
//    }

    @Test
    void testConfEquality() {
        Truth a = PreciseTruth.byConf(1, 0.5f);

        Truth aEqualWithinThresh = PreciseTruth.byConf(1, 0.5f - FREQ_EPSILON / 2.1f /* slightly less than half the epsilon */);
        assertEquals(a.toString(), aEqualWithinThresh.toString());
        //assertEquals(a.hashCode(), aEqualWithinThresh.hashCode());

        Truth aNotWithinThresh = PreciseTruth.byConf(1, 0.5f - FREQ_EPSILON * 1);
        assertNotEquals(a, aNotWithinThresh);
        //assertNotEquals(a.hashCode(), aNotWithinThresh.hashCode());
    }


    @Test
    void testTruthHash() {
        assertEquals(PreciseTruth.byConf(0.5f, 0.5f).hashCode(), PreciseTruth.byConf(0.5f, 0.5f).hashCode());
        assertNotEquals(PreciseTruth.byConf(1, 0.5f).hashCode(), PreciseTruth.byConf(0.5f, 0.5f).hashCode());
        assertNotEquals(PreciseTruth.byConf(0.51f, 0.5f).hashCode(), PreciseTruth.byConf(0.5f, 0.5f).hashCode());
        assertNotEquals(PreciseTruth.byConf(0.506f, 0.5f).hashCode(), PreciseTruth.byConf(0.5f, 0.5f).hashCode());

        assertEquals(PreciseTruth.byConf(0, 0.01f).hashCode(), PreciseTruth.byConf(0, 0.01f).hashCode());


        assertEquals(PreciseTruth.byConf(
                Truth.freq(0.504f, 0.01f),
                (float) Truth.conf(0.5f, 0.01f)
        ).hashCode(), PreciseTruth.byConf(
                Truth.freq(0.5f, 0.01f),
                (float) Truth.conf(0.5f, 0.01f)
        ).hashCode());
        assertEquals(PreciseTruth.byConf(
                Truth.freq(0.004f, 0.01f),
                (float) Truth.conf(0.01f, 0.01f)
        ).hashCode(), PreciseTruth.byConf(
                Truth.freq(0, 0.01f),
                (float) Truth.conf(0.01f, 0.01f)
        ).hashCode());
        assertNotEquals(PreciseTruth.byConf(
                Truth.freq(0.006f, 0.01f),
                (float) Truth.conf(0.01f, 0.01f)
        ).hashCode(), PreciseTruth.byConf(
                Truth.freq(0, 0.01f),
                (float) Truth.conf(0.01f, 0.01f)
        ).hashCode());


    }

//    @Test
//    void testTruthHashUnhash() {
//        XorShift128PlusRandom rng = new XorShift128PlusRandom(2);
//        for (int i = 0; i < 1000; i++)
//            hashUnhash(rng.nextFloat(), rng.nextFloat() * (1f - TRUTH_EPSILON * 2));
//    }

//    private static void hashUnhash(float f, float c) {
//        Truth t = PreciseTruth.byConf(f, c);
//        Truth u = PreciseTruth.byConf(t.hashCode());
//        assertNotNull(u, () -> t + " unhased to null via hashCode " + t.hashCode());
//        assertEquals(t, u);
//    }


    @Test
    void testExpectation() {
        assertEquals(0.75f, PreciseTruth.byConf(1, 0.5f).expectation(), 0.01f);
        assertEquals(0.95f, PreciseTruth.byConf(1, 0.9f).expectation(), 0.01f);
        assertEquals(0.05f, PreciseTruth.byConf(0, 0.9f).expectation(), 0.01f);
    }

    static void printTruthChart() {
        float c = 0.9f;
        for (float f1 = 0f; f1 <= 1.001f; f1 += 0.1f) {
            for (float f2 = 0f; f2 <= 1.001f; f2 += 0.1f) {
                Truth t1 = PreciseTruth.byConf(f1, c);
                Truth t2 = PreciseTruth.byConf(f2, c);
//                System.out.println(t1 + " " + t2 + ":\t" +
//                        TruthFunctions.comparison(t1, t2, TRUTH_EPSILON));
            }
        }
    }

    @Test
    void testTruthPolarity() {
        assertEquals(0f, t(0.5f, 0.9f).polarity(), 0.01f);
        assertEquals(1f, t(0f, 0.9f).polarity(), 0.01f);
        assertEquals(1f, t(1f, 0.9f).polarity(), 0.01f);
        assertEquals(1f, t(1f, 0.5f).polarity(), 0.01f);
    }

//    @Disabled
//    @Test
//    void testEvidenceHorizonDistortion() {
//        Truth a = t(1f, 0.9f);
//        double eviA = a.evi();
//        Truth b = t(1f, 0.9f);
//        double eviB = b.evi();
//        float eviABintersect = TruthFunctions.c2w(0.81f);
//        double eviABintersectRaw = eviA * eviB;
//        double eviABintersectRawToConf = w2c(eviA * eviB);
//        System.out.println();
//    }


//    @Test void PostFn() {
//        /* bad
//        $ .02 (right-->trackXY)! 3492⋈3500 %.28;.09% {3492: 1;36;3 A;3 K;3N;3O}
//            $.17 (trackXY-->happy)! %1.0;.90% {0: 1}
//            $.01 ((right-->$1) ==>-4 ($1-->happy)). 3486⋈3494 %.28;.34% {3490: 36;3A;3K;3N;3O} */
//
//        assertNull(TruthFunctions2.post(t(1, 0.9f), t(0.28f, 0.34f), true, 0));
//
//        assertTrue(
//                TruthFunctions2.post(t(1, 0.9f), t(0.75f, 0.9f), true, 0).expectation()
//                >
//                TruthFunctions2.post(t(1, 0.9f), t(0.65f, 0.9f), true, 0).expectation()
//        );
////        assertTrue(
////                TruthFunctions2.post(t(0.75f, 0.9f), t(0.75f, 0.9f), true, 0).expectation()
////                        >
////                        TruthFunctions2.post(t(1, 0.9f), t(0.65f, 0.9f), true, 0).expectation()
////        );
//    }

    /** a sanity test, but an algebraic proof would suffice instead */
    @Test void AssociativityOfOrFunction() {
        Random rng = new XoRoShiRo128PlusRandom(1);
        for (int i = 0; i < 100; i++) {
            float a = rng.nextFloat(), b = rng.nextFloat(), c = rng.nextFloat();
            assertEquals(
                Fuzzy.or(Fuzzy.or(a, b), c),
                Fuzzy.or(Fuzzy.or(a, c), b),
                0.01f
            );
            assertEquals(
                Fuzzy.or(Fuzzy.or(a, b), c),
                Fuzzy.or(Fuzzy.or(a, c), b),
                0.01f
            );
        }
    }
}