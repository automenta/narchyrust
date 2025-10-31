package nars.term.util.transform;

import nars.Op;
import nars.Term;
import nars.term.builder.InterningTermBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static nars.$.$$;
import static nars.Op.CONJ;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MapSubstTest {

	@Test
	void interlock_InNonCommutve_2() {
		assertEq("(b,a)", $$("(a,b)").replace(Map.of(
			$$("a"), $$("b"),
			$$("b"), $$("a"))));
	}
	@Test
	void interlock_InNonCommutve_2_3() {
		assertEq("(b,a,c)", $$("(a,b,c)").replace(Map.of(
			$$("a"), $$("b"),
			$$("b"), $$("a"))));
	}

	@Test
	void interlock_identical_InCommutve_2() {
		Term aAndB = CONJ.the($$("a"), $$("b"));
		Term BandA = aAndB.replace(Map.of(
			$$("a"), $$("b"),
			$$("b"), $$("a")));
		assertEquals(aAndB, BandA);
		if (Op.terms instanceof InterningTermBuilder)
			assertSame(aAndB, BandA);
	}

	@Test
	void interlock_identical_InCommutve_2_3() {
		Term aAndB = CONJ.the($$("a"), $$("b"), $$("c"));
		Term bAndA = aAndB.replace(Map.of(
			$$("a"), $$("b"),
			$$("b"), $$("a")));
		assertEquals(aAndB, bAndA);
		if (Op.terms instanceof InterningTermBuilder)
			assertSame(aAndB, bAndA);
	}
}