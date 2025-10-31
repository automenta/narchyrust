package nars.truth;

import jcog.Str;
import nars.NAL;
import nars.Truth;
import nars.truth.util.Revision;
import org.junit.jupiter.api.Test;

import static nars.$.t;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TruthTest2 {

	@Test
	void testTruthRevision() {
		Truth d = Revision.revise(PreciseTruth.byConf(1f, 0.1f), PreciseTruth.byConf(1f, 0.1f));
		assertEquals(1f, d.freq(), 0.01f);
		assertEquals(0.18f, (float) d.conf(), 0.01f);

		Truth a = Revision.revise(PreciseTruth.byConf(1f, 0.3f), PreciseTruth.byConf(1f, 0.3f));
		assertEquals(1f, a.freq(), 0.01f);
		assertEquals(0.46f, (float) a.conf(), 0.01f);

		Truth b = Revision.revise(PreciseTruth.byConf(0f, 0.3f), PreciseTruth.byConf(1f, 0.3f));
		assertEquals(0.5f, b.freq(), 0.01f);
		assertEquals(0.46f, (float) b.conf(), 0.01f);

		Truth c = Revision.revise(PreciseTruth.byConf(1f, 0.9f), PreciseTruth.byConf(1f, 0.9f));
		assertEquals(1f, c.freq(), 0.01f);
		assertEquals(0.95f, (float) c.conf(), 0.01f);
	}

	@Test void ditheringSanity() {
		final float confRes = 0.1f;
		int steps = 99;
		float step = 0.01f;
		int subSteps = 9;
		float subStep = 0.001f;
		for (int i = 1; i < steps; i++) {
			for (int j = 1; j < subSteps; j++) {
				float c = step * i + (subStep) * j;
				Truth p = t(1f, c).dither(NAL.truth.FREQ_EPSILON, NAL.truth.FREQ_EPSILON);
				System.out.println(p + "\t" + Str.n2(c) + '\t' + Str.n4(c)+ '\t' + c );
			}
		}
	}


}