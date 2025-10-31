package nars.nal.nal8;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.nal.nal7.NAL7Test;
import nars.term.Compound;
import nars.test.NALTest;
import nars.test.TestNAR;
import nars.time.Tense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * tests goals involving &,|,~,-, etc..
 */
public abstract class NAL8DecomposeTest extends NALTest {

	public static final int cycles = 20;


	@Override
	protected NAR nar() {
		NAR n = NARS.tmp(5, 6);
		n.complexMax.set(12);
		return n;
	}

	@BeforeEach
	void setTolerance() {
		test.confTolerance(NAL7Test.TEMPORAL_CONF_TOLERANCE);
		test.nar.time.dur(3);
	}

	static class ConjBelief extends NAL8DecomposeTest {

		@Test
		void ConjBeliefNeg() {
			test
				.volMax(5)
				.input("(&&,--a,b).")
				.input("--a.")
				.mustBelieve(cycles, "b", 1f, 0.81f);
		}


		@Disabled
		@Test
		void ConjBeliefWeak() {
			test
				.volMax(3)
				.input("(a && b). %0.75;0.9%")
				.input("a. %0.80;0.9%")
				.mustBelieve(cycles, "b", 0.60f, 0.49f)
				.must(BELIEF, true, (t) -> {
					if ("b".equals(t.term().toString())) {
						float f = t.freq();
						return !Util.equals(f, 0.60f, 0.05f) || (float) t.conf() < 0.2f;
					}
					return true;
				});
		}

		@Disabled
		@Test
		void ConjBeliefWeakNeg() {
			test
				.volMax(4)
				.input("(--a && b). %0.75;0.9%")
				.input("a. %0.20;0.9%")
				.mustBelieve(cycles, "b", 0.60f, 0.49f)
				.must(BELIEF, true, (t) -> {
					if ("b".equals(t.term().toString())) {
						float f = t.freq();
						return !Util.equals(f, 0.60f, 0.05f) || (float) t.conf() < 0.2f;
					}
					return true;
				});
		}
	}

	static class ConjGoal extends NAL8DecomposeTest {
		@Test
		void ConjPos1() {
			test
				.volMax(4)
				.input("(&&, a, --b)! %0.9%")
				.input("a. %0.9%")
				.mustGoal(cycles, "b", 0.0f, 0.73f);
		}

		@Test
		void ConjPos2() {
			test
					.volMax(4)
					.input("(&&, a, --b)! %0.9%")
					.input("a. %0.3%")
					.mustGoal(cycles, "b", 0.0f, 0.80f);
		}

		@Test
		void DisjPos1() {
			test.confMin(0.1f)
					.volMax(4)
					.input("(||, a, --b)! %0.9%")
					.input("a. %0.9%")
					.mustNotOutput(cycles, "b", GOAL, 0.5f, 1, 0.4f, 1f, ETERNAL);
		}
		@Test
		void ConjConditional_correction_conj_ignore() {
			test.volMax(7)
					.input("(&&,a,b,c)!")
					.input("a.")
					.mustNotGoal(cycles, "a", 0f, 1f);
		}
		@Disabled @Test
		void subsent_1_even_simpler_simplerGoalEternal() {

			test
					.input("(open(t1) && opened:t1)!")
					.mustGoal(cycles, "open(t1)", 1, 0.81f, ETERNAL)
					.mustNotOutput(cycles, "open(t1)", GOAL, 0)
			;

		}

		@Disabled @Test
		void Goal_Should() {
			test
					.input("(a && e)!")
					.input("--e.")
					.mustGoal(cycles, "e", 1, 0.81f)
			;
		}

		@Disabled @Test
		void ConjConditional_correction_conj_pos() {
			test.volMax(7)
				.input("(&&,a,b,c)!")
				.input("--a.")
				.mustGoal(cycles, "a", 1f, 0.81f)
				.mustNotGoal(cycles, "a", 0f, 0.5f);
		}
		@Disabled @Test
		void ConjConditional_correction_conj_neg() {
			test.volMax(7)
				.input("(&&,--a,b,c)!")
				.input("a.")
				.mustGoal(cycles, "a", 0f, 0.81f)
				.mustNotGoal(cycles, "a", 0.5f, 1f);
		}

		@Disabled @Test
		void ConjConditional_correction_disj_ignore() {
			test.volMax(9)
				.input("(||,a,b,c)!")
				.input("a.")
				.mustNotGoal(cycles, "a", 0f, 1f)
			;
		}
		@Disabled @Test
		void ConjConditional_correction_disj_correct() {
			test.volMax(9)
					.input("(||,a,b,c)!")
					.input("--a.")
					.mustGoal(cycles, "a", 1f, 0.81f)
			;
		}

		@Test
		void ConjPos_AntiCond() {
			test
				.volMax(3)
				.confMin(0)
				.goal("(a && b)", Tense.Eternal, 1, 0.45f)
				.believe("a", Tense.Eternal, 0.35f, 0.94f)
				.mustNotGoal(cycles, "b", 0f, 0.5f);
		}


	}



	static class DisjGoal extends NAL8DecomposeTest {

		@Test
		void DisjConditionalDecompose() {
			test.volMax(6)
				.input("(||,a,b)!")
				.input("--a.")
				.mustGoal(cycles, "b", 1f, 0.81f)
				.mustNotGoal(cycles, "b", 0f, 0.5f)
			;
		}

		@Test
		void DisjConditionalDecompose_3ary() {
			test.volMax(12)
				.input("(||,a,b,c)!")
				.input("--a.")
				.mustGoal(cycles, "(b||c)", 1f, 0.81f)
				.mustNotOutput(cycles, "(b||c)", GOAL, 0f, 0.5f, 0f, 1f)
			;
		}

		@Test
		void DisjConditionalDecompose_opposite_neg_2ary() {
			test.volMax(6)
				.input("(a || b).")
				.input("--a!")
				.mustOutputNothing(cycles)
//				.mustGoal(cycles, "b", 1f, 0.81f)
//				.mustNotOutput(cycles, "b", GOAL, 0f, 0.5f, 0f, 1f)
			;
		}

		@Test
		void DisjConditionalDecompose_opposite_neg_3ary() {
			test.volMax(7)
				.input("(||,a,b,c).")
				.input("--a!")
				.mustOutputNothing(cycles)
//				.mustGoal(cycles, "(b||c)", 1f, 0.81f)
//				.mustNotOutput(cycles, "(b||c)", GOAL, 0f, 0.5f, 0f, 1f)
			;
		}

		@Test
		void goal_DisjConditionalDecompose_belief() {
			test.volMax(6)
				.input("(||,a,b).")
				.input("a!")
				.mustOutputNothing(cycles)
			;
		}
		@Test
		void goal_DisjConditionalDecompose_belief_temporal_before_nothing() {
			test.volMax(6)
				.input("--(--a &&+1 --b).")
				.input("a!")
				.mustOutputNothing(cycles)
			;
		}


		@Test
		void goal_DisjConditionalDecompose_belief_temporal_before_nothing2() {
			test.volMax(6)
				.input("--(--a &&+1 --b).")
				.input("b!")
				.mustOutputNothing(cycles)
			;
		}

		@Disabled @Test
		void goal_DisjConditionalDecompose_belief_temporal_before_other() {
			test.volMax(6)
					.input("--(--a &&+1 --b).")
					.input("--b!")
					.mustGoal(cycles, "a", 1f, 0.81f)
			;
		}

//		@Test
//		void DisjOpposite() {
//
//			//produces output from structural deduction
//			test
//				.volMax(4)
//				.input("(||,a,--b)!")
//				.input("a.")
//				.mustNotOutput(cycles, "b", GOAL, 0f, 1f, 0f, 1f, t -> true)
//			;
//		}

		@Test
		void DisjNeg() {
			test
				.volMax(4)
				.input("(a || --b)!")
				.input("--a.")
				.mustGoal(cycles, "b", 0, 0.81f)
				.mustNotOutput(cycles, "b", GOAL, 0.5f, 1f, 0f, 1f, t -> true)

			;
		}

		@Test
		void DisjNeg2() {
			test
				.volMax(4)
				.input("(||,--a, b)!")
				.input("a.")
				.mustGoal(cycles, "b", 1f, 0.81f);
		}
		@Test
		void DisjNeg2_weak() {
			test
					.volMax(4)
					.input("(||,--a, b)! %0.75;0.9%")
					.input("a.")
					.mustGoal(cycles, "b",
							1, 0.38f
							//0.75f, 0.81f
					);
		}
		@Test
		void DisjNeg3() {
			test
				.volMax(5)
				.input("(||,--a,--b)!")
				.input("a.")
				.mustGoal(cycles, "b", 0f, 0.81f);
		}


		@Test void disjNAL6_DecomposeGoal() {
//        $ .04 (agent-->action2)! 335⋈351 %1.0;.07% {366: 1;c;d}
			//        $.38 (agent-->happy)! 3665⋈3729 %1.0;.50% {3697: 1}
			//        $.01 (agent-->((--,happy)&&(--,action2))). 335⋈351 %.73;.21% {: c;d}
			test
				.inputAt(3, "happy!")
				.inputAt(2, "--(happy||action2). %.73;.90%")
				//.inputAt(2, "((--,happy)&&(--,action2)). %.73;.90%")
				.mustNotOutput(cycles,"action2", GOAL, 0, 1, 0, 1, ETERNAL)
			;
		}

		@Test void decompose_temporal_disj() {
//        $ .04 (agent-->action2)! 335⋈351 %1.0;.07% {366: 1;c;d}
			//        $.38 (agent-->happy)! 3665⋈3729 %1.0;.50% {3697: 1}
			//        $.01 (agent-->((--,happy)&&(--,action2))). 335⋈351 %.73;.21% {: c;d}
			test
				.inputAt(1, "action! |")
				.inputAt(1, "((--,happy) &&+1 (--,action)). | %.73;.90%")
				.mustOutput(cycles,"happy", GOAL, 1f, 1f, 0.76f, 0.76f, 0)
			;
		}
	}


//    @Test
//    void _Pos_GoalInDisj2_AlternateSuppression_1() {
//        test
//                .input("(||,x,y).")
//                .input("x!")
//                .mustGoal(cycles, "y", 0f, 0.45f);
//    }
//    @Test
//    void _Pos_AntiGoalInDisj2_AlternateSuppression_1() {
//        test
//                .input("(||,--x,y).")
//                .input("x!")
//                .mustGoal(cycles, "y", 1f, 0.45f);
//    }
//    @Test
//    void _Neg_GoalInDisj2_AlternateSuppression_1() {
//        test
//                .input("(||,--x,y).")
//                .input("--x!")
//                .mustGoal(cycles, "y", 0f, 0.45f);
//    }
//    @Test
//    void _Pos_GoalInDisj3_AlternateSuppression_1() {
//        test
//                .input("(||,x,y,z).")
//                .input("x!")
//                .mustGoal(cycles, "(||,y,z)" /* == (&&,--y,--z) */, 0f, 0.15f);
//    }



	@Disabled
	static class ConjStrongDeduction extends NAL8DecomposeTest {
		@Test
		void Bpp() {
			posBelief(test.volMax(5).input("(a && b).").input("(b ==> c)."));
		}
        @Test
        void Bnp() {
            posBelief(test.volMax(5).input("(a && --b).").input("(--b ==> c)."));
        }
        @Test
        void Bpn() {
            negBelief(test.volMax(5).input("(a && b).").input("(b ==> --c)."));
        }

		@Test
		void Gpp() {
			posGoal(test.volMax(5).input("(a && b)!").input("(c ==> b)."));
		}

		@Test
		void Gpn() {
			posGoal(test.volMax(5).input("(a && --b)!").input("(c ==> --b)."));
		}

		@Test
		void Gpn2() {
			test.volMax(5).input("(a && b)!").input("(c ==> --b).")
				.mustGoal(cycles, "c", 0f, 0.73f) //after decomposing the conj to 'b!' , 73% not 81%
				.mustNotOutput(cycles, "c", GOAL, 0.1f, 1f, 0, 1)
			;
		}

		//        @Test void disjP() {
//            pos(test.termVolMax(5).input("(a || b)!").input("(c ==> b)."));
//        }
		private void posBelief(TestNAR t) {
			t.mustBelieve(cycles, "c", 1f, 0.81f)
            .mustNotOutput(cycles, "c", BELIEF, 0, 0.9f, 0, 1)
			;
		}
        private void negBelief(TestNAR t) {
            t.mustBelieve(cycles, "c", 0f, 0.81f)
                .mustNotOutput(cycles, "c", BELIEF, 0.1f, 1f, 0, 1)
            ;
        }

		private void posGoal(TestNAR t) {
			t.mustGoal(cycles, "c", 1f, 0.81f)
				.mustNotOutput(cycles, "c", GOAL, 0, 0.9f, 0, 1)
			;
		}

//		private void negGoal(TestNAR t) {
//			t.mustGoal(cycles, "c", 0f, 0.81f)
//				.mustNotOutput(cycles, "c", GOAL, 0.1f, 1f, 0, 1)
//			;
//		}

	}

	static class DoublePremiseDecompose_ConjDisjGoal_to_Belief extends NAL8DecomposeTest {

		//        @Test
//        void decompose_Conj_BeliefPosPos() {
//            test
//                    .termVolMax(3)
//                    .input("(a && b). %0.9;0.9%")
//                    .input("b. %0.9;0.9%")
//                    .mustBelieve(cycles, "a", 0.81f, 0.66f);
//        }
//        @Test
//        void decompose_Conj_BeliefPosNeg() {
//            test
//                    .termVolMax(4)
//                    .input("(a && --b). %0.9;0.9%")
//                    .input("b. %0.1;0.9%")
//                    .mustBelieve(cycles, "a", 0.81f, 0.66f)
//                    .mustNotBelieve(cycles, "a", 0.91f, 0.07f, (s,e)->true);
//
//        }
		@Test
		void decompose_Conj_BeliefNegPos() {
			test
				.confMin(0.3f)
				.volMax(3)
				.input("(a && b). %0.1;0.9%") //== (--a || --b)
				.input("b. %0.9;0.9%")
				.mustBelieve(cycles, "a", 0.11f, 0.81f);
		}
		@Disabled @Test
		void decompose_Conj_BeliefPosNeg() {
			test
					.volMax(3)
					.input("(a && b). %0.9;0.9%") //== (--a || --b)
					.input("b. %0.1;0.9%")
					.mustBelieve(cycles, "a", 0.99f, 0.74f);
		}
		@Test
		void decompose_Conj_Goal_neg_decompose_pos() {

			test.volMax(3);
			test.input("(a && b)! %0.1;0.9%");
			test.input("b. %0.9;0.9%");
			test.mustGoal(cycles, "a", 0.1f/0.9f, 0.81f);
			test.mustNotGoal(cycles, "b", 0.9f, 1f);
		}


		@Test void GoalDisjDeduction() {
			test
				.input("(x||y)!")//eternal
				.input("--x. |") //temporal
				.mustGoal(cycles, "y", 1, 0.81f,  0);
		}
		@Test
		void decompose_Conj_Goal_neg_decompose_neg() {
			//adapted form nal3 test
			test.volMax(6);
			test.input("(--a || b)! %0.9;0.9%");
			test.input("b. %0.1;0.9%");
			test.mustGoal(cycles, "a", 0.1f, 0.73f);
		}

        @Test
        void decompose_Conj_Goal_neg_decompose_neg_3_ary() {
            test.volMax(6);
            test.input("(||, --a, b, c)! %0.9;0.9%");
            test.input("b. %0.1;0.9%");
            test.mustGoal(cycles, "(--a || c)", 0.9f, 0.73f);
        }
		@Test
		void decompose_Conj_Goal_neg_decompose_neg_3_ary_2() {

            assertTrue(((Compound) $.$$c("(||, --a, b, c, d)").unneg()).condOf($$("(b && d)"), -1));
            assertTrue(((Compound) $.$$c("(||, --a, b, c, d)").unneg()).condOf($$("a"), +1));
            assertTrue(((Compound) $.$$c("(||, --a, b, c, d)").unneg()).condOf($$("b"), -1));

			test.volMax(8);
			test.input("(||, --a, b, c, d)! %0.9;0.9%");
			test.input("(b && d). %0.1;0.9%");
			test.mustGoal(cycles, "(--a || c)", 0.81f, 0.66f);
		}

		@Test void goalEventInBeliefConjBefore_Temporal() {
			/*
			$.21 (write,b):1! 13470⋈13780 %1.0;.01% {21991: qÏDsÞ÷giÚ;CÏDsÞ÷giÚ;FÏDsÞ÷giÚ}
				$.75 happy:b! 28280⋈28680 %1.0;.50% {28490: qÏDsÞ÷giÚ}
				$.31 ((write,b):1&&happy:b). 12500⋈12810 %.32;.25% {: CÏDsÞ÷giÚ;FÏDsÞ÷giÚ}
			 */
			final int gs = 28280, ge = 28680;
			final int bs = 12500, be = 12810;

			int gr = ge-gs, br = be - bs;
			test.goal("y", 1, 0.5f, gs, ge);
			test.believe("(x && y)", 0.32f, 0.25f, bs, be);
			test.mustGoal(cycles,"x", 1, 0.02f,
				(s,e) -> s==gs && e==gs+Math.min(gr,br)
			);
		}
		@Test void goalEventInBeliefConjDuring_Temporal() {
			final int gs = 28280, ge = 28680;
			final int bs = 28400, be = 28500;
			test.goal("y", 1, 0.5f, gs, ge);
			test.believe("(x && y)", 1f, 0.5f, bs, be);
			test.mustGoal(cycles,"x", 1, 0.25f,
					(s,e) -> s==bs && e==be
			);
		}

	}
	static class DoublePremiseDecompose_Goal_to_ConjDisjBelief extends NAL8DecomposeTest {

		@Test
		void pp() {
			test.volMax(6)
				.input("b!")
				.input("(a &&+1 b).")
				.mustGoal(cycles, "a", 1f, 0.45f);
		}
		@Test
		void pp_factored() {
			test.volMax(9)
					.input("b!")
					.input("(x && (a &&+1 b)).")
					.mustGoal(cycles, "(x&&a)", 1f, 0.45f);
		}
		@Test
		void pp_wrong() {
			test
					.volMax(6)
					.input("--b!")
					.input("(a &&+1 b).")
					.mustGoal(cycles, "--a", 1f, 0.81f);
					//.mustNotGoal(cycles, "a");
		}
		@Test
		void np() {
			test.volMax(6)
					.input("--b!")
					.input("(a &&+1 --b).")
					.mustGoal(cycles, "a", 1f, 0.81f);
		}

		@Test void pn() {
			test
				.volMax(6)
				.input("b!")
				.input("--(--a &&+1 --b).") //(a ||+1 b)
				.mustNotGoal(cycles, "a");
		}

		@Test void nn() {
			test
				.volMax(6)
				.input("--b!")
				.input("--(--a &&+1 --b).")  //(a ||+1 b)
				.mustGoal(cycles, "a", 1f, 0.81f);
		}

	}
	@Disabled static class Quest_Induction_Test extends NAL8DecomposeTest {
		@Test void qp() {
			test
			.volMax(6)
			.input("x!")
			.input("(x && y).")
			.mustQuest(cycles, "(x&&y)");
		}
		@Test void qn() {
			test
				.volMax(6)
				.input("x!")
				.input("(--x && y).")
				.mustQuest(cycles, "(--x&&y)");
		}
	}
}