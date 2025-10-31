package nars.action.transform;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.unify.constraint.TermMatch;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InperienceTest {

	@Test void testReify1() {
		assertTimeless("(x && y)", true);
		assertTimeless("/\\(x && y)", true);
		assertTimeless("/\\((x && y))", true);
		assertTimeless("(x &&+1 y)", false);
		assertTimeless("/\\(x &&+1 y)", false);
		//assertEq("", new Inperience.BeliefInperience(BELIEF, 0).reify(NALTask.task($$c("(x &&+1 y)"), BELIEF, $.t(1, 0.9f), ETERNAL, ETERNAL, new long[] { 1 }), $$("I")));
	}

	private static void assertTimeless(String x, boolean timeless) {
		assertEquals(timeless,
			TermMatch.Timeless.test($$(x))
		);
	}

	@Test
	void testReifyBelief() throws Narsese.NarseseException {
		NAR n = NARS.shell();

		assertEq(
				"believe(I,x)",
			//"believe(I,x,1)",
			new Inperience.BeliefInperience(BELIEF, 0).reify(n.inputTask("x. |"), $$("I"))
		);

		//negation is maintained internally to the overall positive belief
		assertEq(
				"believe(I,(--,x))",
			//"believe(I,x,-1)",
			new Inperience.BeliefInperience(BELIEF, 0).reify(n.inputTask("--x. |"), $$("I"))
		);
	}
}