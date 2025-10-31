package nars.truth;

import jcog.Fuzzy;
import nars.$;
import nars.NAL;
import nars.Truth;
import nars.TruthFunctions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static java.lang.Float.MIN_NORMAL;
import static java.util.Arrays.asList;
import static jcog.Util.sqr;
import static nars.NAL.truth.CONF_MIN;
import static nars.TruthFunctions.mix2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** experiments with alternate truth functions */
class TruthFunctions2Test {

//	@Test
//	void unionFair() {
//		assertEquals($.t(1f, 0.81f), TruthFunctions2.unionFair($.t(1,0.9f), $.t(1, 0.9f), MIN_NORMAL));
//		assertEquals($.t(0.88f, 0.58f), TruthFunctions2.unionFair($.t(0.75f,0.9f), $.t(0.5f, 0.9f), MIN_NORMAL));
//		assertNull(TruthFunctions2.unionFair($.t(1f,0.9f), $.t(0f, 0.9f), MIN_NORMAL)); //full frequency distortion
//	}

	@Test
	void divide() {
		assertDivide(1f, 1, 1, 0.81f);
		assertDivide(0.5f, 1, 0.5f, 0.81f);
		assertDivide(0f, 1, 0f, 0.81f);
		assertDivide(0.5f, 0.7f, 0.72f, 0.81f);
		//assertDivide(1f, 0.5f, 1, 0.4f);
	}

	private static void assertDivide(float xy, float x, float y, float yc) {
		Truth z = TruthFunctions.divide($.t(xy, 0.9f), $.t(x, 0.9f), MIN_NORMAL);
		assertTrue($.t(y, yc).equals(z, 0.02f), ()->z!=null ? z.toString() : "null");
	}

	@Test
	void divide_unsure() {
		PreciseTruth u = $.t(1, 0.81);
		for (var t : asList(
				TruthFunctions.divide($.t(0.9f, 0.9f), $.t(0.9f, 0.9f), CONF_MIN),
				TruthFunctions.divide($.t(0.5f, 0.9f), $.t(0.5f, 0.9f), false, false, CONF_MIN),
				TruthFunctions.divide($.t(0.3f, 0.9f), $.t(0.3f, 0.9f), false, false, CONF_MIN),
				TruthFunctions.divide($.t(0.1f, 0.9f), $.t(0.1f, 0.9f), false, false, CONF_MIN))) {
			assertEquals(u, t/*.immutable()*/);
		}
	}


	@Test
	void divide_AND_OR() {
		//float x = 0.2f, y = 0.8f;
		float x = 1, y = 1;
		float c = 0.9f;
		/* && */ assertDivide(x, y, c, true);
		// /* || */ assertDivide(x, y, c, false);
	}

	private static void assertDivide(float x, float y, float c, boolean andor) {
		float xy = andor ? Fuzzy.and(x, y) : Fuzzy.or(x, y);
		float X = andor ? xy / y : (1-xy) / (1-y);
		assertDivide(xy, c, y, c, X, sqr(c));
	}

	private static void assertDivide(float xy, double xyc, float y, double yc, float x, double xc) {
		assertEquals($.t(x, xc),
				TruthFunctions.divide($.t(xy, xyc), $.t(y, yc), true, true, CONF_MIN)/*.immutable()*/);
	}


	private static double alignment(double x, double y) {
		return Fuzzy.alignment(x, y, NAL.truth.FREQ_EPSILON);
	}

	@Test
	void freqAlignment() {

		//same polarity
		assertEquals( 0, alignment(0, 1));
		assertEquals( 1, alignment(1, 1));
		assertEquals( 1, alignment(0, 0));
		assertEquals( 1, alignment(0.5, 0.5));
		assertEquals( 0.75, alignment(0, 0.25));

		//different polarities (across mid-point)
		assertEquals( 0, alignment(0.25, 0.75));
		assertEquals( 0.1, alignment(0.25, 0.7), 0.01);

		assertTrue( alignment(0.7f, 0.6f) > 0.5f);

		assertTrue( alignment(0.45f, 0.55f) < 0.6f);


		assertTrue(alignment(0.7f, 0.6f) > alignment(0.45f, 0.55f));

		assertTrue( alignment(0.7f, 0.6f) < alignment(1f, 0.9f) ); //??

		//fractional self-similarity
		assertEquals(alignment(1f, 0.75f), alignment(0.75f, 0.5f + 0.25f/2), 0.01f);

	}

	@Disabled
	@Test void demandMaybeMaybe() {
		assertEquals("%.50;.46%", TruthFunctions.post($.t(0.5f, 0.9f), $.t(0.5f, 0.9f), true, true, 0).toString());
	}

	@Disabled @Test void demandMaybeMaybe2() {

		assertEquals("%.50;.40%", TruthFunctions.post($.t(0f, 0.9f), $.t(0.5f, 0.9f), true, true, 0).toString());

		assertEquals("%.50;.40%", TruthFunctions.post($.t(1f, 0.9f), $.t(0.5f, 0.9f), true, true, 0).toString());
		assertEquals("%.50;.40%", TruthFunctions.post($.t(0.5f, 0.9f), $.t(1f, 0.9f), true, true, 0).toString());

	}


	/** mix(A,B,C) == mix(B,C,A) == mix(C,A,B) = ... */
	@Disabled @Test void testMixAssociativity() {
		PreciseTruth x = $.t(0.9f, 0.9f);
		mixAssociativity(x, x, x);

		PreciseTruth y = $.t(0.7f, 0.9f);
		mixAssociativity(x, x, y);
	}

	private static void mixAssociativity(Truth a, Truth b, Truth c) {
		var abc = mix2(mix2(a, b, false, 0), c, false, 0).toString();
		var acb = mix2(mix2(a, c, false, 0), b, false, 0).toString();
		var bca = mix2(mix2(b, c, false, 0), a, false, 0).toString();
		var bac = mix2(mix2(b, a, false, 0), c, false, 0).toString();
		var cab = mix2(mix2(c, a, false, 0), b, false, 0).toString();
		var cba = mix2(mix2(c, b, false, 0), a, false, 0).toString();
		assertEquals(abc, bca);
		assertEquals(abc, cab);
		assertEquals(abc, acb);
		assertEquals(abc, bac);
		assertEquals(abc, cba);
	}

//    @Test
//    void testIntersectionX() {
//        assertEquals("%1.0;.67%", intersectionX($.t(1f, 0.5f), $.t(1f, 0.5f), 0).toString());
//        assertEquals("%.50;.25%", intersectionX($.t(1f, 0.5f), $.t(0f, 0.5f), 0).toString());
//
//
//        assertEquals(
//                intersectionX($.t(1f, 0.5f), $.t(0.75f, 0.5f), 0),
//                intersectionX($.t(0f, 0.5f), $.t(0.25f, 0.5f), 0).neg());
//        assertEquals("%.88;.56%", intersectionX($.t(1f, 0.5f), $.t(0.75f, 0.5f), 0).toString());
//        assertEquals("%.13;.56%", intersectionX($.t(0f, 0.5f), $.t(0.25f, 0.5f), 0).toString());
//
//    }

//    @Test
//    void testDifferenceX() {
//        assertEquals("%0.0;.67%", differenceX($.t(1f, 0.5f), $.t(1f, 0.5f), 0).toString());
//        assertEquals("%.50;.25%", differenceX($.t(1f, 0.5f), $.t(0.5f, 0.5f), 0).toString());
//        assertEquals("%1.0;.67%", differenceX($.t(1f, 0.5f), $.t(0f, 0.5f), 0).toString());
//        assertEquals("%0.0;.67%", differenceX($.t(0f, 0.5f), $.t(1f, 0.5f), 0).toString());
//
//
//    }

//    /**
//     * --(A && B), B |- --A
//     * <p>
//     * alternate names: "interdeduction" "deductersection"
//     * return neg(deduct(intersect(neg(v1), v2), 1f));
//     */
//	@Deprecated public static @Nullable Truth reduceConj(Truth a, Truth b, double minConf) {
//		Truth ab = intersect(a, true, b, false, minConf);
//		if (ab == null) return null;
//		float f = ab.freq();
//		double c = f * ab.conf();
//		return c < minConf ? null : tt(1 - f, c);
//	}
//	@Disabled @Test
//	void reduceConj() {
//			PreciseTruth xy = $.t(0, 0.9);
//			PreciseTruth x = $.t(0.5f, 0.9);
//			assertEquals($.t(0.5f, 0.41).toString(), TruthFunctions.reduceConj(xy, x, MIN_NORMAL).toString());
//
//		//TODO needs recalibrated
////		{
////			PreciseTruth x = $.t(0.5f, 0.9f);
////			assertEquals($.t(0.63f, 0.3f), TruthFunctions.reduceConj($.t(0.25f, 0.9f), x, MIN_NORMAL));
////			assertEquals($.t(0.44f, 0.46f), TruthFunctions.reduceConj($.t(0.25f, 0.9f), $.t(0.75f, 0.9f), MIN_NORMAL));
////		}
//	}

}