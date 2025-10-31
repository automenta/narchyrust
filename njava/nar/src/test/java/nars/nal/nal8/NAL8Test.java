package nars.nal.nal8;

import nars.*;
import nars.nal.nal7.NAL7Test;
import nars.term.util.conj.CondMatch;
import nars.test.NALTest;
import nars.time.Tense;
import nars.unify.UnifyAny;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static nars.$.$$;
import static nars.Op.*;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NAL8Test extends NALTest {

	public static final int cycles = 30;

	@BeforeEach
	void setTolerance() {
		test.confTolerance(NAL7Test.TEMPORAL_CONF_TOLERANCE);
		test.volMax(12);
		test.confMin(0.005f);
	}

	@Test
	void subsent_1_even_simpler_simplerBeliefTemporal() {

		test
			.volMax(9)
			.input("(open(t1) &&+5 (t1-->[opened])). |")
			.mustBelieve(cycles, "open(t1)", 1.0f, 0.81f, 0)
			.mustBelieve(cycles, "(t1-->[opened])", 1.0f, 0.81f, 5)
			.mustNotOutput(cycles, "open(t1)", BELIEF, (t) -> t != 0)
			.mustNotOutput(cycles, "(t1-->[opened])", BELIEF, 0.5f, 1, 0, 0.8f, (t) -> t != 5)
		;
	}

	@Test
	void subsent_1_even_simpler_simplerGoalTemporal() {

		test
			.volMax(9)
			.input("(open(t1) &&+5 opened(t1))! |")
			.mustGoal(cycles, "open(t1)", 1.0f, 0.81f, 0)
			.mustGoal(cycles, "opened(t1)", 1.0f, 0.81f, 5)
			.mustNotOutput(cycles, "open(t1)", GOAL, t -> t != 0)
			.mustNotOutput(cycles, "opened(t1)", GOAL, t -> t != 5)
		;
	}


	@Test
	void testCorrectGoalOccAndDuration() throws Narsese.NarseseException {
        /*
        $1.0 (happy-->dx)! 1751 %.46;.15% {1753: _gpeß~Èkw;_gpeß~Èky} (((%1-->%2),(%3-->%2),neqRCom(%1,%3)),((%1-->%3),((Induction-->Belief),(Weak-->Goal),(Backwards-->Permute))))
            $NaN (happy-->noid)! 1754⋈1759 %1.0;.90% {1748: _gpeß~Èky}
            $1.0 (dx-->noid). 1743⋈1763 %.46;.90% {1743: _gpeß~Èkw}
         */

		test
			.input(NALTask.taskUnsafe($.$("(a-->b)"), GOAL, $.t(1f, 0.9f), 10, 20, new long[]{100}).<Task>withPri(0.5f))
			.input(NALTask.taskUnsafe($.$("(c-->b)"), BELIEF, $.t(1f, 0.9f), 5, 25, new long[]{101}).<Task>withPri(0.5f))
			.mustGoal(cycles, "(a-->c)", 1f, 0.4f,

				x -> x >= 5 && x <= 25
			)
		;

	}
	@Test void correctGoalShift1() {
		/*
		TaskRel wrong time
		$.07 (agent-->action1)! 5320⋈5331 %0.0;.11% {5327: 1;5;6}
			$.38 (agent-->happy)! 6385⋈6449 %1.0;.50% {6417: 1}
			$.03 ((--,(agent-->action1)) ==>-4 (agent-->happy)). 5316⋈5327 %1.0;.22% {: 5;6}

		$0.0 R:a! 5330⋈5750 %.36;.04% {5751: ÒHtÍÜROk.;ÓHtÍÜROk.;ÚHtÍÜROk.}
			$.06 near:a! 9732⋈10156 %1.0;.50% {9744: ÒHtÍÜROk.}
			$0.0 ((--,R:a) ==>+10 near:a). 5700⋈5780 %.64;.25% {: ÓHtÍÜROk.;ÚHtÍÜROk.}
		 */
		test
			.inputAt(4,"(--y ==>-1 x). |")
			.inputAt(6, "x! |")
			.mustGoal(cycles, "y", 0, 0.6f, 7)
			.mustNotOutput(cycles, "y", GOAL, 0, 1, 0, 1, (s,e)->s==5 && e==5)
		;
	}

	@Disabled @Test
	void simpleConjBeliefAndGoalPP() {
		test
			.input("x! |")
			.input("(x && y).")
			.mustGoal(cycles, "y", 1.0f, 0.81f, 0);
	}

	@Test void testWhatIsFormingThisIntersectionGoal() {
		test
			.volMax(18)
			.input("  possesses(user,#1)!")
			.input("--possesses(--user,#1)!")
//			.mustNotOutput(cycles, "((((--,user),#1)&&(user,#1))-->possesses)", QUEST)
			.mustNotOutput(cycles, "((((--,user),#1)&&(user,#1))-->possesses)", GOAL);
	}

//	@Test
//	void simpleConjBeliefAndGoalNP() {
//		test
//			.input("--x! |")
//			.input("(--x && y).")
//			.mustGoal(cycles, "y", 1.0f, 0.81f, 0);
//	}

	@Test
	@Disabled void firstGoalConjunctionEvent() {

		test
			.volMax(19)
			.input("(hold(SELF,{t002}) &&+5 (at(SELF,{t001}) && open({t001})))! |")
//			.believe("--hold(SELF,{t002}). |")
			.mustGoal(cycles, "hold(SELF,{t002})", 1.0f, 0.81f, 0)
			.mustNotOutput(cycles, "hold(SELF,{t002})", GOAL, ETERNAL);
	}



	@Test
	void subbelief_2() throws Narsese.NarseseException {

		{
			Term t = $.$("(hold(SELF,{t002}) &&+5 (at(SELF,{t001}) &&+5 open({t001})))");
			assertEquals(2, t.subs());
			assertEquals(10, t.seqDur());
		}

		test
			.volMax(9)
			.input("(h &&+2 (a &&+2 o)). |")
			.mustBelieve(cycles, "h", 1.0f, 0.73f, 0)
			.mustBelieve(cycles, "(a &&+2 o)", 1.0f, 0.81f, 2)
		;
	}

	@Test
	void subbelief_2easy() {
		test
			.input("(a:b &&+5 x:y). |")
			.mustBelieve(cycles, "a:b", 1.0f, 0.81f, (t -> t == 0))
			.mustBelieve(cycles, "x:y", 1.0f, 0.81f, (t -> t == 5))
		;
	}

	@Test
	void eternal_deduction_1_pos_belief_pos_conc() {
		test.input("pick:t2.");
		test.input("(pick:t2 ==> hold:t2).");
		test.mustBelieve(cycles, "hold:t2", 1.0f, 0.81f, ETERNAL);
	}

	@Test
	void eternal_deduction_1_pos_belief_neg_conc() {
		test.input("pick:t2.");
		test.input("--(pick:t2 ==> hold:t2).");
		test.mustBelieve(cycles, "hold:t2", 0.0f, 0.81f, ETERNAL);
	}

	@Test
	void eternal_deduction_1_pos_belief_maybe_conc() {
		test.input("pick:t2.");
		test.input("(pick:t2 ==> hold:t2). %0.50%");
		test.mustBelieve(cycles, "hold:t2", 0.5f, 0.81f, ETERNAL);
	}

	@Test
	void eternal_deduction_1_maybe_belief_maybe_conc() {

		test.input("pick:t2. %0.50%");
		test.input("(pick:t2 ==> hold:t2). %0.50%");
		test.mustBelieve(cycles, "hold:t2", 0.5f, 0.4f, ETERNAL);
	}

	@Test
	void eternal_deduction_1_maybe_goal_certain_conc() {
		int cycles = NAL8Test.cycles + 10; //HACK
		test.confMin(0.25f);
		test.volMax(8);
		test.input("pick:t2! %0.50%");
		test.input("(pick:t2 ==> hold:t2).");
		test.input("(pick:t2 ==> --donthold:t2).");
		test.mustGoal(cycles, "hold:t2", 1f, 0.29f, ETERNAL);
		test.mustGoal(cycles, "donthold:t2", 0f, 0.29f, ETERNAL);
		test.mustBelieve(cycles, "((--,(t2-->donthold))==>(t2-->hold))", 1f, 0.45f, ETERNAL);
		test.mustBelieve(cycles, "     ((t2-->hold)==>(t2-->donthold))", 0f, 0.45f, ETERNAL);
	}

	@Test
	void eternal_deduction_1_neg_belief_pos_conc() {
		test.input("--pick:t2.");
		test.input("(--pick:t2 ==> hold:t2).");
		test.mustBelieve(cycles, "hold:t2", 1.0f, 0.81f, ETERNAL);
	}

	@Test
	void eternal_deduction_1_neg_belief_neg_conc() {
		test.input("--pick:t2.");
		test.input("--(--pick:t2 ==> hold:t2).");
		test.mustBelieve(cycles, "hold:t2", 0.0f, 0.81f, ETERNAL);
	}

	@Test
	void temporal_deduction_1() {

		test.input("pick:t2. |");
		test.inputAt(2, "(pick:t2 ==>+5 hold:t2).");
		test.mustBelieve(cycles, "hold:t2", 1.0f, 0.81f, 5);

	}

	@Test
	void temporal_abduction_subj_conj_1() {
		test.input("hold:t2. |");
		test.inputAt(2, "((x &&+1 pick:t2) ==>+1 hold:t2).");
		test.mustBelieve(cycles, "(x &&+1 pick:t2)", 1.0f, 0.45f, -2);
	}

	@Test
	void subbelief_2medium() {
		test
			.volMax(16)
			.input("(a:b &&+5 (c:d &&+5 x:y)). |")
			.mustBelieve(cycles, "a:b", 1.0f, 0.73f, 0)
//			.mustBelieve(cycles, "c:d", 1.0f, 0.73f, 5)
//			.mustBelieve(cycles, "x:y", 1.0f, 0.73f, 10)
			.mustNotOutput(cycles, "a", BELIEF)
			.mustNotOutput(cycles, "b", BELIEF)
			.mustNotOutput(cycles, "c", BELIEF)
			.mustNotOutput(cycles, "d", BELIEF)
		;
	}


	@Test
	void testDesiredConjPos() {
		test
			.believe("x")
			.goal("(x&&y)")
			.mustGoal(cycles, "y", 1f, 0.81f);
	}

	@Test
	void testDesiredConjNeg() {
		test.believe("--x")
			.goal("(--x && y)")
			.mustGoal(cycles, "y", 1f, 0.81f);
	}


	@Test
	void goal_prePP() {
		test
			.goal("x")
			.believe("(x==>y)")
			.mustGoal(cycles, "y", 1f, 0.45f)
			.mustNotOutput(cycles, "y", GOAL, 0f, 0.5f, 0, 1, t -> true);
	}

	@Test
	void goal_preNP() {
		test
			.goal("--x")
			.believe("(--x==>y)")
			.mustGoal(cycles, "y", 1f, 0.45f)
			.mustNotOutput(cycles, "y", GOAL, 0f, 0.5f, 0, 1, t -> true);
	}
	@Test
	void goal_preNN() {
		test
			.goal("--x")
			.believe("(--x==>--y)")
			.mustGoal(cycles, "y", 0f, 0.45f)
			.mustNotOutput(cycles, "y", GOAL, 0.5f, 1f, 0, 1, t -> true)
			;
	}
	@Test
	void goal_prePN() {
		test
			.goal("x")
			.believe("(x==> --y)")
			.mustGoal(cycles, "y", 0f, 0.45f)
			.mustNotOutput(cycles, "y", GOAL, 0.5f, 1f, 0, 1, t -> true)
			;
	}
	@Test
	void goal_prePP_temporal() {
		test
			.goal("x", 1, 0.9f, 9, 14)
			.believe("(x ==>+1 y)", 1, 0.9f, 4, 6)
			.mustGoal(cycles, "y", 1, 0.34f, 10, 12)
		;
	}
	@Test
	void goal_postPP_delay() {

		test
			.input("y! |")
			.believe("(x ==>+1 y)")
			.mustGoal(cycles, "x", 1f, 0.81f, -1)
			.mustNotOutput(cycles, "x", GOAL, 0f, 0.5f, 0, 1, t -> true)
			;
	}

	@Disabled @Test
	void goal_postNP() {
		//B, (X ==> A), --is(B,"==>"), --isVar(X) |- --unisubst(X,A,B,"$"), (Goal:PostNP,
		test
			.input("--y!")
			.believe("(x ==> y)")
			.mustGoal(cycles, "x", 0f, 0.81f)
			.mustNotOutput(cycles, "x", GOAL, 0.5f, 1f, 0, 1, t -> true)
		;
	}
	@Disabled @Test
	void goal_postNP2() {
		test
				.input("--y!")
				.believe("(--x ==> y)")
				.mustGoal(cycles, "x", 1f, 0.81f)
				.mustNotOutput(cycles, "x", GOAL, 0f, 0.5f, 0, 1, t -> true)
		;
	}
	@Test
	void postPN() {
		test
			.input("y!")
			.believe("(x ==> --y)")
			.mustGoal(cycles, "x", 0f, 0.81f)
			.mustNotOutput(cycles, "x", GOAL, 0.5f, 1f, 0, 1, t -> true)
		;
	}

	@Test
	void postDisj1() {
		test
			.input("a!")
			.believe("(x ==> (||,a,b,c))")
			//.mustBelieve(cycles, "(x==>a)", 1f, 0.81f)
			.mustGoal(cycles, "x", 1f, 0.81f)
			.mustNotOutput(cycles, "x", GOAL, 0f, 0.5f, 0, 1, t -> true)
		;
	}
	@Test
	void postConjInh() {
		test
				.input("(a-->y)!")
				.believe("(x ==> ((a&b)-->y))")
				.mustGoal(cycles, "x", 1f, 0.81f)
				.mustNotOutput(cycles, "x", GOAL, 0f, 0.5f, 0, 1, t -> true)
		;
	}
	@Test
	void postDisjInh() {
		test
				.input("(a-->y)!")
				.believe("(x ==> ((a|b)-->y))")
				.mustGoal(cycles, "x", 1, 0.81f)
				.mustNotOutput(cycles, "x", GOAL, 0f, 0.5f, 0, 1, t -> true)
		;
	}

	@Test
	void preDisj1() {
		test
				.input("a.")
				.believe("((a|b) ==> x)")
				.mustBelieve(cycles, "x", 1, 0.81f)
				.mustNotOutput(cycles, "x", GOAL, 0f, 0.5f, 0, 1, t -> true)
		;
	}

//        test
//            .input("y! %0.50%")
//            .believe("(x ==> --y)")
//            .mustGoal(cycles, "x", 0.5f, 0.81f)
//            .mustGoal(cycles, "x", 0f, 0.81f);

//    }
//    @Test
//    void testGoalConjunctionDecomposeNeg() {
//        test
//                .goal("(x &&+3 y)", Tense.Present, 0f, 0.9f)
//                .mustNotOutput(cycles, "x", GOAL, 0);
//    }

	@Disabled
	@Test
	void testGoalConjunctionDecomposeViaStrongTruth() {

		test
			.goal("(&&, x, y, z, w)", Tense.Present, 1f, 0.9f)
			.believe("w", Tense.Present, 0.9f, 0.9f)
			.mustGoal(cycles, "w", 0.9f, 0.81f, 0)
		;
	}

	@Disabled
	@Test
	void testGoalConjunctionDecomposeViaStrongTruthNeg() {

		test
			.goal("(&&, x, y, z, --w)", Tense.Present, 1f, 0.9f)
			.believe("w", Tense.Present, 0.1f, 0.9f)
			.mustGoal(cycles, "w", 0.9f, 0.81f, 0)
		;
	}

	@Test
	void testStrongNegativePositiveInheritance() {

		test
			.goal("--(A-->B)")
			.believe("(B-->C)")
			.mustGoal(cycles, "(A-->C)", 0f, 0.81f)
		;
	}

	@Test
	void testStrongNegativeNegativeInheritance() {

		test
			.goal("--(A-->B)")
			.believe("--(B-->C)")
			.mustNotOutput(5, "(A-->C)", GOAL, 0f, 1f, 0f, 1f, ETERNAL)
		;
	}

	@Test
	void testConditionalGoalConjunctionDecomposePositivePostconditionGoal() {

		test
			.goal("y", Tense.Present, 1f, 0.9f)
			.believe("(x &&+3 y)", Tense.Present, 1f, 0.9f)
			.mustBelieve(cycles, "x", 1f, 0.81f, 0)
			.mustBelieve(cycles, "y", 1f, 0.81f, 3)
			.mustGoal(cycles, "x", 1f, 0.81f, t -> t == -3);
	}


	@Disabled @Test
	void testConditionalGoalConjunctionDecomposePositiveGoalBefore() {

		test
			.goal("x", Tense.Present, 1f, 0.9f)
			.inputAt(2, "(x &&+3 y). |")
			.mustGoal(cycles, "y", 1f, 0.45f /*0.81f*/, 3)
		;
	}

	@Test
	void testConditionalGoalConjunctionDecomposePositiveGoalAfter() {

		test
			.goal("y", Tense.Present, 1f, 0.9f)
			.inputAt(2, "(x &&+3 y). |")
			.mustGoal(cycles, "x", 1f, 0.81f, 2/*-3*/)
		;
	}


	@Test
	void testConjSeqGoalDecomposeForward() {
		test
			.goal("(x &&+3 y)", Tense.Present, 1f, 0.9f)
			.believe("x", Tense.Present, 1f, 0.9f)
			.mustGoal(cycles, "y", 1f, 0.81f, (t) -> t == 3)
			.mustNotOutput(cycles, "y", GOAL, t -> t != 3);
	}

	@Test
	void testConjParGoalDecomposeForward() {

		test
			.volMax(3)
			.goal("(x && y)", Tense.Present, 1f, 0.9f)
			.believe("x", Tense.Present, 1f, 0.9f)
			.mustGoal(cycles, "y", 1f, 0.81f, 0)
			.mustNotOutput(cycles, "y", GOAL, t -> t != 0);
	}

	@Test
	void testConjSeqGoalNegDecomposeForward() {


		test
			.volMax(4)
			.goal("(--x &&+3 y)", Tense.Present, 1f, 0.9f)
			.believe("x", Tense.Present, 0f, 0.9f)
			.mustGoal(cycles, "y", 1f, 0.81f, 3)
			.mustNotOutput(cycles, "y", GOAL, ETERNAL)
			.mustNotOutput(cycles, "y", GOAL, 0)
		;
	}

	@Test
	void conditionalDisjDecomposePos() {

		test.volMax(5)
			.goal("(x || y)", 1f, 0.9f, 0, 0)
			.believe("--x", 1f, 0.9f, 2, 2)
			.mustGoal(cycles, "y", 1f, 0.81f, 0)
		;
	}

	@Test
	void conditionalDisjDecomposeNeg() {

		test.volMax(7)
			.goal("(--x || y)", 1f, 0.9f, 0, 0)
			.believe("x", 1f, 0.9f, 0, 0)
			.mustGoal(cycles, "y", 1f, 0.45f, 0)
		;
	}

	@Test
	void testInhibition() {


		test
			.volMax(4).confMin(0.7f)
			.goal("reward")
			.believe("(  good ==> reward)", 1, 0.9f)
			.believe("(--bad  ==> reward)", 1, 0.9f)
			.mustGoal(cycles, "good", 1.0f, 0.81f)
			.mustNotOutput(cycles, "good", GOAL, 0f, 0.9f, 0f, 1f, ETERNAL)
			.mustGoal(cycles, "bad", 0.0f, 0.81f)
			.mustNotOutput(cycles, "bad", GOAL, 0.1f, 1f, 0f, 1f, ETERNAL)
		;

	}

	@Test
	void testInhibitionInverse() {

		test.volMax(3).confMin(0.25f)
			.goal("--reward")
			.believe("(good ==> reward)", 1, 0.9f)
			.believe("(bad ==> reward)", 0, 0.9f)
			.mustGoal(cycles, "bad", 1.0f, 0.81f)
			//mustGoal(cycles, "good", 0.0f, 0.81f)
			.mustNotOutput(cycles, "bad", GOAL, 0f, 0.9f, 0f, 1f, ETERNAL)
		;
	}


	@Test
	void testInhibition0() {
		test
			.volMax(3)
			.goal("reward")
			.believe("(bad ==> --reward)", 1, 0.9f)
			.mustNotOutput(cycles, "bad", GOAL, 0.5f, 1f, 0f, 1f, ETERNAL);
	}

	@Test
	void testInhibition1() {
		test.volMax(3)
			.goal("reward")
			.believe("(good ==> reward)", 1, 0.9f)
			.mustGoal(cycles, "good", 1.0f, 0.81f)
			.mustNotOutput(cycles, "good", GOAL, 0.0f, 0.7f, 0.5f, 1f, ETERNAL)
		;
	}

	@Test
	void testInhibitionReverse() {
		test.volMax(3)
			.goal("reward")
			.believe("(reward ==> good)", 1, 0.9f)
			.mustGoal(cycles, "good", 1.0f, 0.45f)
			.mustNotOutput(cycles, "good", GOAL, 0.0f, 0.5f, 0.0f, 1f, ETERNAL);
	}



	@Disabled @Test
	void testInheritanceDecompositionTemporalGoal() {
		test
			.inputAt(0, "(((in)&(left))-->cam)! |")
			.mustGoal(cycles, "cam(in)", 1f, 0.81f, 0)
			.mustGoal(cycles, "cam(left)", 1f, 0.81f, 0);

	}

	@Disabled @Test
	void testInheritanceDecompositionTemporalBelief() {

		test
			.inputAt(0, "(((in)&(left))-->cam). |")
			.mustBelieve(cycles, "cam(in)", 1f, 0.81f, 0)
			.mustBelieve(cycles, "cam(left)", 1f, 0.81f, 0);

	}

	@Test
	@Disabled
	void disjunctionBackwardsQuestionEternal() {


		test
			.inputAt(0, "(||, x, y)?")
			.believe("x")
			.mustBelieve(cycles, "(&&, (--,x), (--,y))", 0f, 0.81f, ETERNAL);
	}


	@Test
	void questParComponent() {
		test
			.input("(a && b).")
			.input("a@")
			.mustOutput(cycles, "b", QUEST, 0, 1f);
	}
	@Test
	void questSeqComponent() {
		test
			.input("(x &&+1 y).")
			.input("y@")
			.mustOutput(cycles, "x", QUEST, 0, 1);
	}
	@Test
	void questImplPred() {
		test
			.input("(x ==> y).")
			.input("y@")
			.mustOutput(cycles, "x", QUEST, 0, 1);
	}
	@Test
	void questImplSubjP() {
		test
				.input("(  y ==> x).")
				.input("y@")
				.mustOutput(cycles, "x", QUEST, 0, 1);
	}
	@Test
	void questImplSubjN() {
		test
				.input("(--y ==> x).")
				.input("y@")
				.mustOutput(cycles, "x", QUEST, 0, 1);
	}



	@Test
	void testGoalImplComponentEternal() {
		test.volMax(6)
			.confMin(0.7f)
			.input("happy!")
			.input("(in ==> (happy && --out)).")
			//.mustBelieve(cycles, "(in ==> happy)", 1f, 0.81f)
			.mustGoal(cycles, "in", 1f, 0.73f);
	}

	@Test
	void testGoalImplComponentEternalSubjNeg() {
		test.volMax(7)
			.confMin(0.4f)
			.input("happy!")
			.input("(--in ==> (happy && --out)).")
			.mustGoal(cycles, "in", 0f, 0.42f);
	}

	@Test
	void testConjDecomposeWithDepVar() {

		test
			.input("(#1 && --out)! |")
			.mustGoal(cycles, "out", 0f, 0.81f, 0);
	}

	@Test
	void testPredictiveImplicationTemporalTemporal() {
        /*
        wrong timing: should be (out)! @ 16
        $.36;.02$ (out)! 13 %.35;.05% {13: 9;a;b;t;S;Ü} ((%1,(%2==>%3),belief(negative),time(decomposeBelief)),((--,unisubst(%2,%3,%1)),((AbductionPN-->Belief),(DeductionPN-->Goal))))
            $.50;.90$ (happy)! 13 %1.0;.90% {13: Ü}
            $0.0;.02$ ((out) ==>-3 (happy)). 10 %.35;.05% {10: 9;a;b;t;S} ((%1,(%2==>((--,%3)&&%1073742340..+)),time(dtBeliefExact),notImpl(%1073742340..+)),(unisubst((%2 ==>+- (&&,%1073742340..+)),(--,%3),(--,%1)),((DeductionN-->Belief))))
        */

		test
			.volMax(3)
			.inputAt(0, "(out ==>-3 happy). |")
			.inputAt(13, "happy! |")
			.mustGoal(cycles, "out", 1f, 0.45f, (t) -> t > 13)
			.mustNotOutput(cycles, "out", GOAL, 3);
	}
	@Test
	void testPredictiveImplicationTemporalEternal() {
		test
				.volMax(10)
				.inputAt(0, "(((1,0)&&up) ==>+1 (1,1)).")
				.inputAt(1, "(1,1)! |")
				.inputAt(1, "(1,0). |")
				.mustGoal(cycles, "((1,0)&&up)", 1f, 0.81f, (t) -> t==0)
				.mustGoal(cycles, "up", 1f, 0.45f, (t) -> t==0)
				;
	}

	@Test
	void testPredictiveImplicationTemporalTemporalOpposite() {

		test
			.volMax(3)
			.inputAt(0, "(happy ==>-3 out). |")
			.inputAt(2, "happy! |")
			.mustGoal(cycles, "out", 1f, 0.45f, (t) -> t == -1);


	}
	@Test
	void testPredictiveImplicationTemporalTemporal_Fwd_Unification() {
		test
				.volMax(7)
				.inputAt(0, "(x:$1 ==>+1 y:$1). |")
				.inputAt(2, "x:a! |")
				.mustGoal(cycles, "y:a", 1f, 0.45f, 3);
	}
	@Test
	void testPredictiveImplicationTemporalTemporal_Rev_Unification() {
		test
				.volMax(7)
				.inputAt(0, "(x:$1 ==>+1 y:$1). |")
				.inputAt(2, "y:a! |")
				.mustGoal(cycles, "x:a", 1f, 0.81f, 1);
	}

	@Disabled
	@Test
	void testConjDecomposeSequenceEmbedsAntiGoalNeg() {

		test
			.input("(a &&+1 (x &&+1 y)).")
			.input("--x!")
			.mustGoal(cycles, "(a &&+2 y)", 0f, 0.4f, t -> t == ETERNAL);
	}

	@Disabled
	@Test
	void testConjDecomposeSequenceEmbedsAntiGoalPos() {

		test
			.input("(a &&+1 (--x &&+1 y)).")
			.input("x!")
			.mustGoal(cycles, "(a &&+2 y)", 0f, 0.4f, t -> t == ETERNAL);
	}

	@Test
	void testPredictiveImplicationTemporalTemporalNeg() {

		test
			.inputAt(0, "(--out ==>-3 happy). |")
			.inputAt(5, "happy! |")
			.mustGoal(cycles, "out", 0f, 0.45f, /*~*/8);

	}


	@Test
	void testPredictiveEquivalenceTemporalTemporalNeg() {

		test
			.volMax(4)
			.inputAt(0, "(--out ==>-3 happy). |")
			.inputAt(0, "(happy ==>+3 --out). |")
			.inputAt(5, "happy! |")
			.mustGoal(cycles, "out", 0f, 0.45f, 8)
			.mustNotOutput(cycles, "out", GOAL, 3);
	}


	@ParameterizedTest
	@ValueSource(ints = {-4, -3, +0, +3, +4})
	void implDecomposeGoalPredicate1(int dt) {
		testDecomposeGoalPredicateImplSubjPred(dt, "a/b");
	}

	@ParameterizedTest
	@ValueSource(ints = {-4, -3, +0, +3, +4})
	void implDecomposeGoalPredicate2(int dt) {
		testDecomposeGoalPredicateImplSubjPred(dt, "(a &&+1 a2)/b");
	}

	@ParameterizedTest
	@ValueSource(ints = {-3, +0, +3})
	void implDecomposeGoalPredicate2swap(int dt) {
		testDecomposeGoalPredicateImplSubjPred(dt, "(a2 &&+1 a)/b");
	}

	@ParameterizedTest
	@ValueSource(ints = {-4, -3, +0, +3, +4})
	void implDecomposeGoalPredicate3(int dt) {
		testDecomposeGoalPredicateImplSubjPred(dt, "(a &&+1 --a)/b");
	}

	@ParameterizedTest
	@ValueSource(ints = {-4, -3, +0, +3, +4})
	void implDecomposeGoalPredicate4(int dt) {
		testDecomposeGoalPredicateImplSubjPred(dt, "(a &&+1 a)/b");
	}


	private void testDecomposeGoalPredicateImplSubjPred(int dt, String sj) {

		int when = 2;

		int goalAt = //Math.max(when,
			when - dt - $$(sj).seqDur();

		String[] subjPred = sj.split("/");
		assertEquals(2, subjPred.length);

		int start = 1;
		test
			.volMax(6)
			.inputAt(start, '(' + subjPred[0] + " ==>" + ((dt >= 0 ? "+" : "-") + Math.abs(dt)) + ' ' + subjPred[1] + "). |")
			.inputAt(when, "b! |")
			.mustGoal(cycles, subjPred[0], 1f, 0.81f, t -> t == goalAt)
			.mustNotGoal(cycles, subjPred[0], (s,e) -> s != goalAt)
			.mustNotGoal(cycles, "b", (s,e) -> s != when)
//			.mustNotOutput(cycles, "b", GOAL, 0, 0.9f, 0, 0.3f)
			.mustNotQuestion(cycles, "(a ==>+4 a)")
			.mustNotQuestion(cycles, "(a==>b)")
		;
	}

	@Test
	void conjDecomposeGoalAfter_ete_tmp() {

		test
			.volMax(3)
			.inputAt(1, "(a &&+3 b).")
			.inputAt(5, "b! |")

			.mustGoal(cycles, "a", 1f, 0.81f, t -> t == 2)
//                .mustNotOutput(cycles, "a", GOAL, t -> t!=2)
		;
	}

	@Test
	void conjDecomposeGoalAfter_tmp_tmp() {

		test
			.volMax(3)
			.inputAt(1, "(a &&+3 b). |")
			.inputAt(5, "b! |")

			.mustGoal(cycles, "a", 1f, 0.3f, t -> t == 2)
			.mustNotOutput(cycles, "a", GOAL, ETERNAL);
	}

	@Test
	void conjDecomposeGoalAfterParallel() {

		test
			.volMax(6)
			.inputAt(1, "(a &&+3 (b&&c)).")
			.inputAt(5, "b! |")
			.mustGoal(cycles, "(a &&+3 c)", 1f, 0.3f, t -> t == 2);
	}

	@Test
	void conjDecomposeGoalAfterNeg() {

		test
			.volMax(4)
			.inputAt(1, "(a &&+3 --b).")
			.inputAt(5, "b! |")
			.mustGoal(cycles, "a", 0f, 0.3f, t -> t == 2)
			.mustNotGoal(cycles, "a", 0.5f, 1f, 0, 1)
			.mustNotOutput(cycles, "a", GOAL, ETERNAL);
	}

	@Test
	void conjDecomposeGoalAfterNegSeq() {

		test
			.input("((x &&+3 y) &&+1 --b).")
			.inputAt(10, "--b! |")
			.mustGoal(cycles, "(x &&+3 y)", 1f, 0.81f, t -> t == 6);
	}

	@Test
	void conjDecomposeGoalAfterPosNeg() {

		test
			.inputAt(3, "(--a &&+3 b). |")
			.inputAt(6, "b! |")
			.mustGoal(cycles, "a", 0f, 0.81f, t -> t == 3)
			.mustNotOutput(cycles, "a", GOAL, ETERNAL);
	}

	@Test
	void implDecomposeGoalAfterPosNeg() {

		test
			.inputAt(0, "(--a ==>+1 b). |")
			.inputAt(1, "b! |")
			.mustGoal(cycles, "a", 0f, 0.81f, t -> t >= 0);

	}

	@Test
	void conjDecomposeGoalAfterNegNeg() {

		test
			.volMax(4)
			.inputAt(3, "(a &&+3 --b). |")
			.inputAt(6, "--b! |")
			.mustGoal(cycles, "a", 1f, 0.5f, t -> t == 3)
			.mustNotOutput(cycles, "a", GOAL, t -> t != 3);
	}

	@Test
	void implDecomposeGoalBeforeTemporalEte() {

		test
			.inputAt(1, "(x ==>-1 y).")
			.inputAt(2, "y! |")
			.mustGoal(cycles, "x", 1f, 0.45f, 3);

	}

	@Test
	void implDecomposeGoalBeforeTemporalSameTerm() {
		test
			.volMax(3)
			.confMin(0.2f)
			.inputAt(1, "(x ==>-1 x).")
			.inputAt(2, "x! |")
			.mustGoal(cycles, "x", 1f, 0.45f, 3)
			.mustGoal(cycles, "x", 1f, 0.81f, 1);
	}

	@Test
	void implDecomposeGoalBeforeTemporalSameTermNegated() {
		test
			.inputAt(1, "(--x ==>-1 x).")
			.inputAt(2, "x! |")
			.mustGoal(cycles, "x", 0f, 0.45f, t -> t == 3)
//                .mustNotOutput(cycles, "x",  GOAL,  t -> t != 3)
		;
	}

	@Test
	void implDecomposeGoalBeforeTemporalImpl() {

		test
			.inputAt(1, "(x ==>-1 y). |")
			.inputAt(2, "y! |")
			.mustGoal(cycles, "x", 1f, 0.81f, 3);
	}

	@Test
	void inhGoalN() {
		test
				.input("--y:x!")
				.input("z:y.")
				.mustGoal(cycles, "z:x", 0f, 0.81f);
	}

	@Test
	void inhGoalP() {

		test
			.input("b:a!")
			.input("c:b.")
			.mustGoal(cycles, "c:a", 1f, 0.81f);
	}


	@Disabled
	@Test
	void questImplDt() {
		test
			.inputAt(0, "(a,b).")
			.inputAt(0, "a. |")
			.inputAt(4, "b@ |")
			.mustOutput(cycles, "(b ==>-4 a)", QUESTION, 0f, 1, 0f, 1f, 4);
	}


	@Test
	void testConjResultGoal() {
		test.volMax(13)
			.input("done!")
			.input("((happy &&+20 y) &&+2 ((--,y) &&+1 done)). |")
			.mustGoal(cycles, "((happy &&+20 y) &&+2 (--,y))", 1, 0.81f, 0)
			.mustNotGoal(cycles, "(happy &&+20 done)")
		;
	}

	@Disabled
	@Test
	void testSimilarityGoalPosBelief() {
		test.volMax(3).goal("(it<->here)")
			.believe("(here<->near)")
			.mustGoal(cycles, "(it<->near)", 1f, 0.45f);
	}

	@Disabled
	@Test
	void testSimilarityGoalNegBelief() {
		test.volMax(3).goal("--(it<->here)")
			.believe("(here<->near)")
			.mustGoal(cycles, "(it<->near)", 0f, 0.45f);
	}

	@Disabled
	@Test
	void testGoalByConjAssociationPosPos() {

		test.goal("a")
			.believe("(b &&+1 (a &&+1 c))")
			.mustGoal(cycles, "(b &&+2 c)", 1f, 0.45f)
			.mustNotOutput(cycles, "(b &&+2 c)", GOAL, 0f, 0.5f, 0f, 1f, x -> true);
	}

	@Disabled
	@Test
	void testGoalByConjAssociationNegPos() {

		test.goal("--a")
			.believe("(b &&+1 (a &&+1 c))")
			.mustGoal(cycles, "(b &&+2 c)", 0f, 0.45f, (t) -> t == ETERNAL)
			.mustNotOutput(cycles, "(b &&+2 c)", GOAL, 0.5f, 1f, 0f, 1f, x -> true);
	}

	@Disabled
	@Test
	void testGoalByConjAssociationPosNeg() {

		test.goal("a")
			.believe("(b &&+1 (--a &&+1 c))")
			.mustGoal(cycles, "(b &&+2 c)", 0f, 0.45f, (t) -> t == ETERNAL)
			.mustNotOutput(cycles, "(b &&+2 c)", GOAL, 0.5f, 1f, 0f, 1f, x -> true);
	}

	@Disabled
	@Test
	void testGoalByConjAssociationNegNeg() {

		test.goal("--a")
			.believe("(b &&+1 (--a &&+1 c))")
			.mustGoal(cycles, "(b &&+2 c)", 1f, 0.45f, (t) -> t == ETERNAL)
			.mustNotOutput(cycles, "(b &&+2 c)", GOAL, 0f, 0.5f, 0f, 1f, x -> true);
	}

	//    @Test void GoalBeliefDecomposeTimeRangingRepeat() {
//        /*
//        $.03 vel(fz,move)! 1536601075540⋈1536601112860 %.57;.03% {1536601090589: } ((%1,%2,eventOf(%2,%1)),(conjDropIfLatest(%2,%1),((Desire-->Goal))))
//            //belief timing ignored
//
//            $1.0 vel(fz,move)! 1536601075540⋈1536601112860 %.57;.15% {1536601088494: }
//                    37S
//
//            $.09 (vel(fz,move) &&+460 vel(fz,move)). 1536601089440⋈1536601089580 %.39;.82% {1536601090008: ÖqdUçëípß;ÖqdUçëíqc;ÖqdUçëíqO} ((%1,%2,(--,is(%2,"==>")),(--,is(%1,"==>"))),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief),(Relative-->Time),(VarIntro-->Also))))
//                    140ms, at the correct start
//         */
//        test
//            .input("x! +0..+100")
//            .input("(x &&+5 x). +20..+30")
//            .mustGoal(cycles, "x", 1f, 0.1f, (t) -> t > 20 /* dropping 2nd event */)
//            .mustNotOutput(cycles, "x", GOAL, 0f, 0.5f, 0f, 1f, (long s, long e) -> (e-s > 10));
//
//    }

	@Test
	void testGoalBeliefDecomposeTimeRangingDiffer() {
		test
			.volMax(3)
			.input("x! +0..+100")
			.input("(y &&+5 x). +20..+30")
			.mustGoal(cycles, "y", 1f, 0.8f, (a, b) ->
				(a == 15 && b == 25)
			)
			.mustNotOutput(cycles, "y", GOAL, 0f, 0.5f, 0f, 1f)
//				(s, e) -> (s!=0 || e!=10))
//			.mustNotOutput(cycles, "x", GOAL, (t) -> true /* shouldnt drop first event */)
//			.mustNotOutput(cycles, "x", GOAL, 0f, 0.5f, 0f, 1f, (s, e) -> (e - s != 10))
		;


	}


	@Test
	void decomposeConjunctionGoalBeliefRelative() {
        /*
        wrong: output should be relative to belief
        $.01 ((Y-->trackXY),"+")! 2428⋈2432 %1.0;.20% {3101: Gæ;IM;IÂ} ((%1,%2,eventOfNeg(%1,%2)),(conjDropIfEarliest(%1,(--,%2)),((DeductionPN-->Belief),(DesirePN-->Goal))))
            $.50 ((--,((X-->trackXY),"+")) &&+8 (--,((Y-->trackXY),"+")))! 2420⋈2424 %0.0;.20% {3101: IM;IÂ}
            $.49 ((X-->trackXY),"+"). 2340⋈2576 %0.0;.90% {2340: Gæ}
        */
		test.nar.time.dur(16);

		test
			.inputAt(0, "(y &&+5 x)! |")
			.inputAt(2, "y. |")
			.mustGoal(cycles, "x", 1f, 0.1f, (t) -> t >= 7)
			.mustNotOutput(cycles, "x", GOAL, (t) -> t < 7)
		;
	}

	@Test
	void testRepeatConjDropWTF() {
        /* wrong:
        $.02 reward(trackXY)! 172⋈216 %1.0;.50% {11461: 9;b;3e} ((%1,%2,eventOfNeg(%2,%1)),(conjDropIfLatest(%2,(--,%1)),((DesireN-->Goal),(TaskInBelief-->Time))))
            $1.0 reward(trackXY)! 172⋈276 %1.0;.95% {626: }
            $.32 ((--,reward(trackXY)) &&+172 (--,reward(trackXY))). 172⋈216 %1.0;.65% {969: 9;b;3e©} ((%1,%2,eventOfNeg(%1,%2)),(conjWithout(%1,(--,%2)),((StructuralDeduction-->Belief))))
            */
		test.confTolerance(0.01f);
		test
			.inputAt(0, "(--x &&+2 --x).")
			.inputAt(2, "x! |")
			.mustNotOutput(cycles, "x", GOAL, 0f, 0.5f, 0, 1f, t -> true)
		;

	}

	@Test
	void testRepeatConjDropWTF_invert() {
        /* wrong:
        $.02 reward(trackXY)! 172⋈216 %1.0;.50% {11461: 9;b;3e} ((%1,%2,eventOfNeg(%2,%1)),(conjDropIfLatest(%2,(--,%1)),((DesireN-->Goal),(TaskInBelief-->Time))))
            $1.0 reward(trackXY)! 172⋈276 %1.0;.95% {626: }
            $.32 ((--,reward(trackXY)) &&+172 (--,reward(trackXY))). 172⋈216 %1.0;.65% {969: 9;b;3e©} ((%1,%2,eventOfNeg(%1,%2)),(conjWithout(%1,(--,%2)),((StructuralDeduction-->Belief))))
            */
		test.confTolerance(0.01f);
		test
			.inputAt(0, "(x &&+2 x).")
			.inputAt(2, "--x! |")
			.mustNotOutput(cycles, "x", GOAL, 0.5f, 1, 0, 1f, t -> true)
		;

	}


	@Test
	void testNotEventOfNeg() {
    /*
    NO
    $.01 ((--,(reward-->trackXY)) &&+15 (reward-->trackXY))! 5811⋈5817 %1.0;.01% {5814: 1;d.;dÇ;dÓ;dá;dë;d÷;dþ;e7;Cù;D6;DQ;DÀ;DÇ;DÎ;DÖ}
                X, C, eventOf(C,X), --eventOfNeg(C,X)   |- C, (Goal:DesireWeak, Time:BeliefAtTask)
        $.50 (reward-->trackXY)! %1.0;.90% {0: 1}
        $1.0 ((--,(reward-->trackXY)) &&+15 (reward-->trackXY)). 5811⋈5816 %.03;.87% {5814: d.;dÇ;dÓ;dá;dë;d÷;dþ;e4;e7;Cù;D6;DQ;DÀ;DÇ;DÎ;DÖ}
        */
		test
			.believe("(--x &&+1 x)")
			.goal("x")
			.mustNotOutput(cycles, "(--x &&+1 x)", GOAL);
	}

	@Test
	void testSubIfWTF() {
        /*
        $.57 good! 144⋈152 %.73;.13% {966: 1;Ð;Ô;Ø} B, (C ==> A), --is(A,"#"),--is(C,"#"), --is(B,"==>") |- unisubst(C,A,B), (Belief:Post, Goal:PostStrong, Time:TaskMinusBeliefDT)
            $1.0 good! %1.0;.90% {0: 1}
            $.48 (better ==>+8 better). 152⋈160 %.73;.20% {699: Ð;Ô;Ø} B, A, --is(A,"==>") |- polarize((polarize(A,belief) ==> B),task), (Belief:InductionDepolarized, Time:BeliefRelative, Also:VarIntro)
            */

		test
			.input("good! |")
			.input("(better ==>+1 better). |")
			.mustNotOutput(cycles, "good", GOAL, 0, 1, 0, 1, t -> true);
	}


	@Test
	void condition_goal_deduction_2_neg_event() {
		test
			.volMax(13)
			.input("--on(x,{t003}).")
			.input("(--on(x,#1) && at(SELF,#1))!")
			.mustGoal(cycles, "at(SELF,{t003})", 1.0f, 0.81f, ETERNAL);
	}

	@Test
	void anonymous_analogy_condition_belief_deduction_2_entire_variable() {
		test
			.input("x.")
			.input("(#1 && g(#1)).")
			.mustBelieve(cycles, "(x && g(x))", 1.0f, 0.81f, ETERNAL);
	}

	@Test
	void anonymous_analogy_neg_condition_belief_deduction_2_entire_variable() {
		test
			.volMax(8)
			.input("--x.")
			.input("(#1 && g(#1)).")
			.mustBelieve(cycles, "(--x && g(--x))", 1.0f, 0.81f, ETERNAL);
	}

//    @Test
//    void condition_belief_deduction_temporal() {
//        test
//                .input("(x ==>+1 y)")
//                .input("((x ==> y) && z).")
//                .mustBelieve(cycles, "((x ==>+1 y) && z)", 1.0f, 0.81f, ETERNAL);
//    }

	@Disabled @Test
	void condition_goal_disjunction_2_neg_conj_no_var_simple() {
		test
			.volMax(10)
			.input("on(t2,t3).")
			.input("--(on(t2,t3) && at(t3))!")
			.mustGoal(cycles, "at(t3)", 0.0f, 0.81f, ETERNAL)
			.mustNotOutput(cycles, "at(t3)", GOAL, 0.1f, 1f, 0, 1, t -> true)
			.mustNotOutput(cycles, "on(t2,t3)", GOAL, 0f, 0.9f, 0, 1, t -> true)
		;
	}

	@Test
	void condition_goal_disjunction_2_neg_conj_var_simple() {
		test
			.volMax(10)
			.input("on(t2,t3).")
			.input("--(on(t2,#1) && at(#1))!")
			.mustGoal(cycles, "at(t3)", 0.0f, 0.81f, ETERNAL)
			//.mustGoal(cycles, "on(t2,t3)", 1.0f, 0.81f, ETERNAL)
			.mustNotOutput(cycles, "at(t3)", GOAL, 0.1f, 1f, 0, 1, t -> true)
			.mustNotOutput(cycles, "on(t2,t3)", GOAL, 0f, 0.9f, 0, 1, t -> true)
		;
	}

	@Test
	void condition_goal_conjunction_2_neg_conj_var_simple_pos() {
		test
			.volMax(11)
			.input("on(t2,t3).")
			.input("(on(t2,#1) && at(#1))!")
			.mustGoal(cycles, "at(t3)", 1.0f, 0.81f, ETERNAL)
			.mustNotOutput(cycles, "at(t3)", GOAL, 0f, 0.9f, 0, 1, t -> true)
			.mustNotOutput(cycles, "on(t2,t3)", GOAL, 0.1f, 1f, 0, 1, t -> true)
		;
	}

	@Test
	void condition_goal_disj() {
		test
			.volMax(12)
			.input("--on(t2,t3).")
			.input("(on(t2,#1) || at(#1))!")
			.mustGoal(cycles, "at(t3)", 1.0f, 0.81f, ETERNAL)
			.mustNotOutput(cycles, "at(t3)", GOAL, 0f, 0.9f, 0, 1, t -> true)
			.mustNotOutput(cycles, "on(t2,t3)", GOAL, 0f, 1f, 0, 1, t -> true)
		;
	}

	@Test
	void condition_goal_conjunction_2_neg_conj_var_simple_neg() {
		test
			.volMax(11)
			.input("--at(t2).")
			.input("(--on(t2,#1) && at(#1))!")
			.mustGoal(cycles, "at(t3)", 1.0f, 0.81f)
			.mustNotOutput(cycles, "at(t3)", GOAL, 0f, 0.9f, 0, 1, t -> true)
			.mustNotOutput(cycles, "at(#1)", GOAL, 0f, 1f, 0, 1, t -> true)
			.mustNotOutput(cycles, "on(t2,t3)", GOAL, 0.1f, 1f, 0, 1, t -> true)
			.mustNotOutput(cycles, "on(t2,#1)", GOAL, 0f, 1f, 0, 1, t -> true)
		;
	}

	@Test
	void condition_goal_disjunction_2_neg_conj_var_simple_neg() {
		test
			.volMax(11)
			.input("--on(t2,t3).")
			.input("--(--on(t2,#1) && at(#1))!")
			.mustGoal(cycles, "at(t3)", 0.0f, 0.81f, ETERNAL)
			.mustNotOutput(cycles, "at(t3)", GOAL, 0.1f, 1f, 0, 1, t -> true)
			.mustNotOutput(cycles, "on(t2,t3)", GOAL, 0f, 0.9f, 0, 1, t -> true)
		;
	}

	@Test
	void condition_goal_deduction_2_neg_conj() {
		test
			.volMax(14)
			.input("on({t002},{t003}).")
			.input("--(on({t002},#1) && at(SELF,#1))!")
			.mustGoal(cycles, "at(SELF,{t003})", 0.0f, 0.81f, ETERNAL);
	}

	@Test
	void condition_goal_deduction_2_ete_belief() {
		conditionalGoalDeduction(true);
	}

	@Test
	void condition_goal_deduction_2_temporal_belief() {
		conditionalGoalDeduction(false);
	}

	private void conditionalGoalDeduction(boolean eteBelief) {
		test
			.volMax(12)
			.input("on({t002},{t003})." + (eteBelief ? "" : " |"))
			.input("(on({t002},#1) && at(SELF,#1))!")
			.mustGoal(cycles, "at(SELF,{t003})", 1.0f, 0.81f, eteBelief ? ETERNAL : 0);
	}

	@Test
	void condition_goal_deductionWithVariableEliminationOpposite() {

		test
			.volMax(14)
			.input("goto({t003}). |")
			.input("(goto(#1) &&+5 at(SELF,#1))!")
			.mustGoal(2 * cycles, "at(SELF,{t003})", 1.0f, 0.81f, (t) -> t >= 5)
		;
	}

	@Test
	void UnifyGoalSeqConclusionPos() {
		test
			.volMax(10)
			.believe("(f(#x) &&+1 g(#x))")
			.goal("g(x)")
			.mustGoal(cycles, "f(x)", 1f, 0.81f);
	}

	@Test
	void UnifyGoalSeqConclusionNeg() {
		assertEq("f(x)",
			CondMatch.match($.$$c("(f(#x) &&+1 --g(#x))"), $$("--g(x)"), false, true, false, false, new UnifyAny())
		);
		test
			.volMax(10)
			.believe("(f(#x) &&+1 --g(#x))")
			.goal("--g(x)")
			.mustGoal(cycles, "f(x)", 1f, 0.81f);
	}
	@Test
	void implPredConjTemporal1() {
		test
			.volMax(9)
			.believe("(z ==>+4 x)")
			.believe("(z ==>+2 y)")
			.mustBelieve(cycles, "(z ==>+2     (y &&+2   x))", 1f, 0.81f)
			.mustBelieve(cycles, "(z ==>+2 --(--y &&+2 --x))", 1f, 0.81f)
		;
	}

	@Test void testCouldntP() {
		//    X, C, --conjSequence(C), condOf(C,--X)   |-  --condWithoutAll(C, --X), (Goal:ExemplificationPP, Time:Either)
		test
				.volMax(4)
				.goal("x")
				.believe("(--x && y)")
				.mustGoal(cycles, "y", 0f, 0.45f)
				.mustNotGoal(cycles, "y", 0.5f, 1f)
		;
	}
	@Test void testCouldntN() {
		test
				.volMax(4)
				.goal("--x")
				.believe("(x && y)")
				.mustGoal(cycles, "y", 0f, 0.45f)
				.mustNotGoal(cycles, "y", 0.5f, 1f)
		;
	}
//	@Test void testCouldntDisjP() {
//		test
//				.volMax(4)
//				.goal("x")
//				.believe("--(x && y)")
//				.mustGoal(cycles, "y", 1f, 0.45f)
//				.mustNotGoal(cycles, "y", 0f, 0.5f)
//		;
//	}

}