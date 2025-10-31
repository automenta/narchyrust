package nars.truth;

import nars.NAL;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MutableTruthTest {

	@Test void distinguishBetweenNaN_and_eviMin() {
		var m = new MutableTruth(0.5f, 0);
		var n = new MutableTruth();
		var q = new MutableTruth(0.5f, NAL.truth.EVI_MIN);
        assertThrows(UnsupportedOperationException.class, ()-> m.equals(n));
		assertThrows(UnsupportedOperationException.class, ()-> q.equals(n));
	}
}