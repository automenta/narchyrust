package nars.deriver;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.action.decompose.Decompose1;
import nars.action.decompose.DecomposeCond;
import nars.action.resolve.Answerer;
import nars.action.resolve.TaskResolve;
import nars.action.transform.Arithmeticize;
import nars.deriver.reaction.ReactionModel;
import nars.deriver.reaction.Reactions;
import nars.focus.time.NonEternalTiming;
import nars.func.Factorize;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeReactionBuilderTest {

	@Disabled
	@Test void DecomposeOneTwo() {
		NAR n = NARS.shell();
		Reactions r = new Reactions().addAll(
				new Decompose1(),
				new DecomposeCond()
			);
		assertEquals(2, r.size());
		ReactionModel d = r.compile(n);

		d.print();

		String dw = d.what.toString();
		assertTrue(dw.contains("{0}"));
		assertTrue(dw.contains("{1}"));
	}
	@Disabled @Test void ArithmeticizeFactorize() throws Narsese.NarseseException {
		NAR n = NARS.shell();
		Reactions r = new Reactions().addAll(
			new TaskResolve(new NonEternalTiming(), Answerer.AnyTaskResolver),
			new Arithmeticize.ArithmeticIntroduction0(),
			new Factorize.FactorIntroduction()
		);
		assertEquals(3, r.size());
		ReactionModel d = r.compile(n);

		d.print();

		String dw = d.what.toString();
		assertTrue(dw.contains("{0}"));
		assertTrue(dw.contains("{1}"));
		n.log();
		n.believe("((1,2),a)");
		n.run(500);
	}
}