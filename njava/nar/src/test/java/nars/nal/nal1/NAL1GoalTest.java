package nars.nal.nal1;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static nars.Op.GOAL;

public class NAL1GoalTest extends NALTest {

	private static final int cycles = 100;

	@Override
	protected NAR nar() {
		return NARS.tmp(1);
	}

	@Test
	void questFromInhSiblingGoal_ext() {
		test
			.input("(a-->c)!")
			.input("(b-->c).")
//			.mustNotGoal(cycles, "(a-->b)", 0f, 1f, 0.4f, 0.45f)
			.mustQuest(cycles, "(b-->c)")
		;
	}

    @Nested
    class Deduction extends NALTest {

		@Test
		void deductionPositiveGoalPositiveBelief() {
			test
					.input("(a-->c)! %0.81;0.9%")
					.input("(a-->b). %0.90;0.9%")
					.mustGoal(cycles, "(b-->c)", 0.81f, 0.42f);
		}

		@Test
		void deductionPositiveGoalNegativeBelief() {
			test
					.input("(a-->c)!")
					.input("(a-->b). %0.2%")
					.mustGoal(cycles, "(b-->c)", 1f, 0.81f);//1f, 0.08f)
		}


		@Test
		void deductionNegativeGoalPositiveBelief() {
			test
					.input("--(a-->c)!")
					.input("(a-->b).")
					.mustGoal(cycles, "(b-->c)", 0f, 0.81f)
			;
		}


		@Test
		void deductionNegativeGoalNegativeBelief() {
			test
					.input("(a-->c)! %0.10;0.90%")
					.input("(a-->b). %0.20;0.90%")
					.mustGoal(cycles, "(b-->c)", 0.82f, 0.81f)
			;
		}


	}

    @Nested
    @Disabled
    class DeductionInverse extends NALTest {
		@Test
		void deductionPositiveGoalPositiveBelief() {
			test
					.input("(a-->b)! %0.90;0.9%")
					.input("(a-->c). %0.45;0.9%")
					.mustGoal(cycles, "(b-->c)", 0.5f, 0.81f)
					.mustNotGoal(cycles, "(b-->c)", 0, 0.48f)
					.mustNotGoal(cycles, "(b-->c)", 0.52f, 1f)

					.mustNotGoal(cycles, "(a-->b)", 0, 0.88f)
					.mustNotGoal(cycles, "(a-->b)", 0.92f, 1f)
					;
		}

		@Test
		void deductionPositiveGoalPositiveBelief_2() {
			test

					.input("(b-->c)! %0.90;0.9%")
					.input("(a-->c). %0.45;0.9%")
					.mustGoal(cycles, "(a-->b)", 0.5f, 0.81f)
					.mustNotGoal(cycles, "(a-->b)", 0, 0.48f)
					.mustNotGoal(cycles, "(a-->b)", 0.52f, 1f)

					.mustNotGoal(cycles, "(b-->c)", 0, 0.88f)
					.mustNotGoal(cycles, "(b-->c)", 0.92f, 1f)

			;
		}

	}

	@Disabled
	@Test
	void deductionNegativeGoalPositiveBeliefSwap() {
		//(B --> C), (A --> B), neqRCom(A,C)    |- (A --> C), (Belief:DeductionX)
		test
			.input("--(nars --> stupid)!")
			.input("(derivation --> nars).")
			.mustGoal(cycles, "(derivation-->stupid)", 0f, 0.81f)
			.mustNotOutput(cycles, "(stupid-->derivation)", GOAL, 0, 1, 0.5f, 1, (t) -> true)
		;
	}

	@Test
	void abductionNegativeGoalPositiveBelief() {
		test
			.goal("--(nars --> stupid)")
			.believe("(human --> stupid)")
			.mustGoal(cycles, "(nars --> human)", 0f, 0.45f)
			.mustGoal(cycles, "(human --> nars)", 0f, 0.45f);
	}

	@Test
	void inductionNegativeGoalPositiveBelief() {
		test
			.goal("--(human --> stupid)")
			.believe("(nars --> stupid)")
			.mustGoal(cycles, "(nars --> human)", 0f, 0.45f)
			.mustGoal(cycles, "(human --> nars)", 0f, 0.45f);
	}

	@Disabled @Test
	void intersectionGoalPosBeliefPos() {
		test
			.input("(a-->c)!")
			.input("(c-->a).")
			.mustGoal(cycles, "(a<->c)", 1f, 0.45f)
		;
	}

	@Disabled @Test
	void intersectionGoalPosBeliefNeg() {
		test
			.input("(a-->c)!")
			.input("(c-->a). %0.25;0.90%")
			.mustGoal(cycles, "(a<->c)", 1f, 0.2f)
		;
	}

//	@Test
//	void intersectionGoalNegBeliefNeg() {
//		test
//			.input("--(c-->a)!")
//			.input("--(a-->c).")
//			.mustGoal(cycles, "(a<->c)", 0f, 0.81f)
//		;
//	}
	@Test
	void resemblanceGoalPosBeliefPos() {
		test
			.volMax(3).confMin(0.7f)
			.input("(m<->p)!")
			.input("(m<->s).")
			.mustGoal(cycles, "(p<->s)", 1f, 0.81f)
		;
	}
	@Test
	void reduce_sim_Goal1() {
		test
			.input("(s<->p)!")
			.mustGoal(cycles, "(s-->p)", 1f, 0.81f)
		;
	}
	@Test
	void reduce_sim_Goal2() {
		test
			.input("(s<->p)! %0.25;0.9%")
			.mustGoal(cycles, "(s-->p)", 0.25f, 0.81f)
		;
	}


//	@Test
//	void resemblanceGoalPosBeliefNeg() {
//		test
//			.input("(m<->p)!")
//			.input("--(s<->m).")
//			.mustGoal(cycles, "--(p<->s)", 1f, 0.81f)
//		;
//	}

	@Test
	void goalSpread1() {
		test.volMax(3)
			.input("(x --> y)!")
			.input("(y <-> z).")
			.mustGoal(cycles, "(x-->z)", 1.0f, 0.45f);
	}
	@Test
	void goalSpread_Outer() {
		test.volMax(7)
				.input("(a:x --> a:y)!")
				.input("(a:y <-> a:z).")
				.mustGoal(cycles, "(a:x-->a:z)", 1.0f, 0.45f);
	}
	@Test
	void goalSpread_Outer_Inner() {
		test.volMax(7)
				.input("(a:x --> b:x)!")
				.input("(x <-> y).")
				.mustGoal(cycles, "(a:y-->b:y)", 1.0f, 0.45f);
	}
	@Test
	void testGoalSimilaritySpreading() {
		test.volMax(3)
			.input("R!")
			.input("(G <-> R).")
			.mustGoal(cycles, "G", 1.0f, 0.81f);
	}

	@Disabled
	@Test
	void testGoalSimilaritySpreadingNeg() {
		test.volMax(3)
			.input("R!")
			.input("--(G <-> R).")
			.mustGoal(cycles, "G", 0.0f, 0.4f);
	}

//    @Test
//    void testGoalSimilaritySpreadingNegInsideNeg() {
//        test
//                .input("--R!")
//                .input("--(G <-> --R).")
//                .mustGoal(cycles, "G", 0.0f, 0.4f);
//    }

	@Disabled @Test
	void testGoalSimilaritySpreadingParameter() {
		test.volMax(4);
		test
				.input("R(x)!")
				.input("(x <-> y).")
				.mustGoal(cycles, "R(y)", 1.0f, 0.4f);
	}

}