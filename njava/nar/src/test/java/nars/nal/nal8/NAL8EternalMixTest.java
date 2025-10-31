package nars.nal.nal8;

import nars.$;
import nars.Narsese;
import nars.nal.nal7.NAL7Test;
import nars.test.NALTest;
import nars.test.TestNAR;
import nars.time.Tense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.function.LongPredicate;

import static nars.Op.*;

/**
 * NAL8 tests specifically involving one or more eternal input tasks
 */
class NAL8EternalMixTest extends NALTest {

    private static final LongPredicate ZERO = t -> t >= 0;
    private static final int cycles = 30;

    @BeforeEach
    void setTolerance() {
        test.confTolerance(NAL7Test.TEMPORAL_CONF_TOLERANCE);
        test.volMax(15);
    }


    @Test
    void subsent_1() {
        String x2 = "(hold({t002}) &&+5 at({t001}))";
        String x3 = "((hold({t002}) &&+5 at({t001})) &&+5 open({t001}))";

        test
            .volMax(22)
            .confMin(0.35f)
            .input("opened:{t001}. |")
            .input("(((hold({t002}) &&+5 at({t001})) &&+5 open({t001})) &&+5 opened:{t001}).")
            .mustBelieve(cycles, x2, 1, 0.40f, -15)
            .mustBelieve(cycles, x2, 1, 0.73f, ETERNAL)
            .mustBelieve(cycles, x3, 1, 0.81f, ETERNAL)
            .mustNotBelieve(cycles, x2, 1, 0.81f, (s, e) -> s != -15 && s!=ETERNAL)
            .mustNotBelieve(cycles, x3, 1, 0.81f, (s, e) -> s != -15 && s!=ETERNAL);


    }

    @Test
    void conditional_abduction_temporal_vs_eternal() {

        test.volMax(10);

        test.input("at(SELF,{t003}). |");
        test.inputAt(1, "(goto($1) ==>+5 at(SELF,$1)).");

        test.mustBelieve(cycles, "goto({t003})", 1, 0.45f, -5);

    }

    @Test
    void ded_with_indep_var_temporal() {

        test.input("goto({t003}). |");
        test.inputAt(0, "(goto($1) ==>+5 at(SELF,$1)).");

        test.mustBelieve(cycles, "at(SELF,{t003})", 1, 0.81f, 5);

    }


    @Test
    void ded_with_var_temporal2() {

        test.input("goto({t003}). | ");
        test.inputAt(10, "(goto($1) ==>+5 at(SELF,$1)). ");

        test.mustBelieve(cycles, "at(SELF,{t003})", 1, 0.81f, 5);

    }


    @Test
    void goal_deduction_tensed_conseq() {

        test.input("goto(x). |");
        test.inputAt(10, "(goto($1) ==>+5 at(SELF,$1)).");

        test.mustBelieve(cycles, "at(SELF,x)", 1, 0.81f, 5);
    }

    @Test
    void goal_deduction_impl() {

        test.input("x:y! |");
        test.input("(goto(z) ==>+5 x:y).");
        test.mustGoal(cycles, "goto(z)", 1, 0.45f, -5);
    }

    @Test
    void goal_deduction_impl_after() {

        test.input("x:y! |");
        test.input("(goto(z) ==>-5 x:y).");
        test.mustGoal(cycles, "goto(z)", 1, 0.45f, 5);
    }

    @Test
    void goal_deduction_delayed_impl() {

        test.input("x:y!");
        test.inputAt(2, "(goto(z) ==>+1 x:y). |");
        test.mustGoal(cycles, "goto(z)", 1, 0.45f, (t) -> t >= 1);
    }


    @Test
    void goal_deduction_tensed_conseq_noVar() {

        test.inputAt(1, "goto(x). |");
        test.inputAt(10, "(goto(x) ==>+5 at(SELF,x)).");

        test.mustBelieve(cycles, "at(SELF,x)", 1, 0.81f, 6);
    }

    @Test
    void belief_deduction_by_condition() {

        test.input("(open({t001}) ==>+5 opened:{t001}).");
        test.inputAt(10, "open({t001}). |");

        test.mustBelieve(cycles, "opened:{t001}", 1, 0.81f, 15);

    }

    @Test
    void condition_goal_deduction2() {

        test
            .confMin(0.4f)
            .input("a:b! |")
            .inputAt(10, "(( c:d &&+5 e:f ) ==> a:b).")
            .mustGoal(cycles, "( c:d &&+5 e:f)", 1, 0.81f, -5)
            .mustNotOutput(cycles, "( c:d &&+5 e:f)", GOAL, ETERNAL)
        ;
    }

    @Test
    void condition_goal_deduction_interval() {
        test
            .input("a:b! |")
            .input("(( c:d &&+5 e:f ) ==>+5 a:b).")
            .mustGoal(cycles, "( c:d &&+5 e:f)", 1, 0.81f, -10);
    }

    @Test
    void condition_goal_deductionEternal() {

        test
            .input("a:b!")
            .inputAt(4, "(( c:d &&+5 e:f ) ==> a:b).")
            .mustGoal(cycles, "( c:d &&+5 e:f)", 1, 0.81f, ETERNAL)
            .mustNotOutput(cycles, "( c:d &&+5 e:f)", GOAL, t -> t > 0)
            .mustNotQuest(cycles, "((b-->a)&&(--,a))")
            .mustNotQuest(cycles, "((b-->a)&&a)")
        ;
    }

    @Test
    void further_detachment() {

        test
                .input("reachable(SELF,{t002}). |")
                .inputAt(10, "(reachable(SELF,{t002}) &&+5 pick({t002}))!")
                .mustGoal(cycles, "pick({t002})", 1, 0.81f, 5);

    }

    @Test
    void condition_goal_deduction_eternal_belief() {
        test.volMax(18);
        test
                .input("reachable(SELF,{t002})! |")
                .inputAt(5, "((on($1,#2) &&+0 at(SELF,#2)) ==> reachable(SELF,$1)).")
                .mustGoal(cycles, "(on({t002},#1) && at(SELF,#1))", 1, 0.45f, 0)
                .mustNotOutput(cycles, "(at(SELF,#1) && on({t002},#1))", GOAL, t -> t == ETERNAL || t == 5);

    }

    @Test
    void goal_ded_2() {
        test.inputAt(0, "at(SELF,{t001}). |");
        test.inputAt(0, "(at(SELF,{t001}) &&+5 open({t001}))!");
        test.mustGoal(cycles, "open({t001})", 1, 0.43f, 5);
    }

    @Test
    void condition_goal_deduction_3simplerReverse() {

        test

                .inputAt(1, "at:t003! |")
                .input("(at:$1 ==>+5 goto:$1).")

                .mustGoal(cycles, "goto:t003", 1, 0.45f, 6)
                .mustNotOutput(cycles, "goto:t003", GOAL, 0, 1f, 0.3f, 1f, 1L);

    }


    @Test
    void further_detachment_2() {
        test.volMax(19);
        test
                .input("reachable(SELF,{t002}). | %1.0;0.7%")
                .inputAt(3, "((reachable(SELF,{t002}) &&+5 pick({t002})) ==>+7 hold(SELF,{t002})).")
                .mustBelieve(cycles, "(pick({t002}) ==>+7 hold(SELF,{t002}))", 1, 0.63f, t -> t >= 3)
                //.mustBelieve(cycles, "(pick({t002}) ==>+7 hold(SELF,{t002}))", 1, 0.81f, ETERNAL) //via structural reduction

        ;

    }

    @Test
    void goal_deduction_2() {

        test.volMax(10).confMin(0.6f);
        test.input("goto({t001}). |");
        test.inputAt(7, "(goto($1) ==>+2 at(SELF,$1)).");
        test
                .mustBelieve(cycles, "at(SELF,{t001})", 1, 0.81f, 2)
                .mustNotOutput(cycles, "at(SELF,{t001})", BELIEF, 1f, 1, 0.81f, 0.81f,
                        t -> t == 0);
    }


    @Test
    void condition_belief_deduction_2() {

        test
                .volMax(16)
                .input("on({t002},{t003}). |")
                .inputAt(2, "(on({t002},#1) && at(SELF,#1)).")
                .mustBelieve(cycles, "at(SELF,{t003})", 1, 0.23f, 0)
                .mustNotOutput(cycles, "at(SELF,{t003})", BELIEF, 0, 1f, 0, 1f, ETERNAL);
    }


    @Test
    void condition_belief_deduction_2_neg() {

        test
                .input("(--,on({t002},{t003})). |")
                .inputAt(2, "((--,on({t002},#1)) && at(SELF,#1)).")
                .mustBelieve(cycles, "at(SELF,{t003})", 1, 0.43f, 0)
                .mustNotOutput(cycles, "at(SELF,{t003})", BELIEF, 0, 1f, 0, 1f, ETERNAL);
    }

    @Test
    void condition_belief_deduction_2_easier() {

        test
                .input("on(t002,t003). |")
                .inputAt(2, "(on(t002,#1) && at(SELF,#1)).")
                .mustBelieve(cycles*10, "at(SELF,t003)", 1, 0.43f, 0)
                .mustNotOutput(cycles, "at(SELF,t003)", BELIEF, 0, 1, 0, 1f, ETERNAL);
    }

    @Disabled
    @Test
    void condition_belief_deduction_2_dternal() {


        test

                .input("on:(t002,t003). |")
                .inputAt(10, "(on:(t002,#1) && at(SELF,#1)).")
                .mustBelieve(cycles * 4, "at(SELF,t003)", 1, 0.43f, 0)

        ;
    }

    @Test
    void temporal_goal_detachment_1() {

        test
                .input("hold. |")
                .input("( hold &&+5 (at &&+5 open) )!")
                .mustGoal(cycles, "(at &&+5 open)", 1, 0.5f, 5)
                .mustNotOutput(cycles, "(at &&+5 open)", GOAL, (t)->t!=5 && t!=ETERNAL)
        ;
    }

//    @Test
//    void temporal_goal_detachment_2() {
//
//
//        test
//                .input("hold! |")
//                .inputAt(2, "( hold &&+5 eat ).")
//                .mustGoal(cycles, "eat", 1f, 0.81f, 5)
//        ;
//    }

    @Test
    void temporal_goal_detachment_3_valid() {

        test
                .input("use! |")
                .inputAt(2, "( hold &&+5 use ).")
                .mustGoal(cycles, "hold", 1f, 0.81f, t -> t == -5)
                .mustNotOutput(cycles, "use", GOAL, ETERNAL)
                .mustNotOutput(cycles, "hold", GOAL, ETERNAL)
        ;
    }

    @Test
    void temporal_goal_detachment_3_valid_negate() {

        test
                .input("--use! |")
                .inputAt(1, "( hold &&+5 --use ).")
                .mustGoal(cycles, "hold", 1f, 0.81f, -5)
                .mustNotOutput(cycles, "hold", GOAL, 0, 1f, 0, 1, (t)->t > -5)
                .mustNotOutput(cycles, "use", GOAL, ETERNAL)
        ;
    }

    @Test
    void detaching_condition0() {
        test.volMax(13);
        TestNAR tester = test;

        tester.input("( ( hold:t2 &&+1 (att1 &&+1 open:t1)) ==>+1 opened:t1).");
        int when = 2;
        tester.inputAt(when, "hold:t2. |");

        String result = "((att1 &&+1 open:t1) ==>+1 opened:t1)";
        tester.mustBelieve(cycles, result, 1, 0.44f, t -> t >= when);

    }

    @Test
    void detaching_condition() {

        test
                .volMax(24)
                .input("( ( hold(SELF,{t002}) &&+5 (at(SELF,{t001}) &&+5 open({t001}))) ==>+5 opened:{t001}).")
                .inputAt(10, "hold(SELF,{t002}). |")
                .mustBelieve(cycles * 1, "opened:{t001}",
                        1, 0.45f, 25)
                .mustBelieve(cycles * 1, "((at(SELF,{t001}) &&+5 open({t001})) ==>+5 opened:{t001})",
                        1, 0.81f, t -> (t >= 10));

    }

    @Test
    void subgoal_1_abd() {

        test.volMax(24);
        test.input("opened:{t001}. |");
        test.input("((hold(SELF,{t002}) &&+5 ( at(SELF,{t001}) &&+5 open({t001}))) ==>+5 opened:{t001}).");

        test.mustBelieve(cycles, "( hold(SELF,{t002}) &&+5 ( at(SELF,{t001}) &&+5 open({t001})))",
                1, 0.45f,
                -15);

    }

    @Test
    void temporal_deduction_2() {

        test.volMax(24);
        TestNAR tester = test;
        tester.input("((hold(SELF,{t002}) &&+5 (at(SELF,{t001}) &&+5 open({t001}))) ==>+5 opened:{t001}).");
        tester.inputAt(2, "hold(SELF,{t002}). | ");

        //tester.mustQuestion(cycles, "(at(SELF,{t001}) &&+5 open({t001}))");
        tester.mustBelieve(cycles, "((at(SELF,{t001}) &&+5 open({t001})) ==>+5 opened:{t001})",
                1, 0.81f,
                t -> true
                //2 + 5
        );

    }


    @Test
    void implSubstitutionViaSimilarity() {

        test
                .input("(a:b<->c:d).")
                .input("(c:d ==>+1 e:f). |")
                .mustBelieve(cycles, "(a:b ==>+1 e:f)", 1, 0.81f, 0)
                .mustNotOutput(cycles, "(a:b ==>+1 e:f)", BELIEF, ETERNAL);
    }

    @Test
    void implSubstitutionViaSimilarityReverse() {

        test

                .input("(a:b<->c:d).")
                .input("(e:f ==>+1 c:d). |")
                .mustBelieve(cycles, "(e:f ==>+1 a:b)", 1, 0.4f, 0)
                .mustNotOutput(cycles, "(e:f ==>+1 a:b)", BELIEF, ETERNAL);
    }


    @Test
    void DesiredConjDelayed() {

        test
                .believe("x", Tense.Present, 1, 0.9f)
                .goal("(x &&+3 y)")
                .mustGoal(cycles, "y", 1, 0.81f, (t) -> t >= 3)
                .mustNotOutput(cycles, "y", GOAL, ETERNAL);
    }

    @Test
    void DesiredConjDelayedNeg() {

        test
                .believe("x", Tense.Present, 0, 0.9f)
                .goal("(--x &&+3 y)")
                .mustGoal(cycles, "y", 1f, 0.81f, x -> x >= 3)
                .mustNotOutput(cycles, "y", GOAL, ETERNAL);
    }

    @Test
    void BelievedImplOfDesireDelayed() {

        test
                .goal("x", Tense.Present, 1f, 0.9f)
                .believe("(x ==>+3 y)")
                .mustGoal(cycles, "y", 1f, 0.81f, 3)

        ;
    }

    @Test
    void GoalConjunctionDecomposeSuffix() {

        test
                .goal("(x &&+3 y)", Tense.Eternal, 1f, 0.9f)
                .inputAt(4, "x. |")
                .mustGoal(cycles, "y", 1f, 0.81f, (4 + 3))
                .mustNotOutput(cycles, "y", GOAL, 3)

        ;
    }

    @Test
    void NegatedImplicationS() {

        test
                
                .goal("R")
                .input("((--,a:b) ==> R). |")
                .mustGoal(cycles, "a:b", 0.0f, 0.81f, ZERO);
    }

//    @Test
//    void InductionAntigoalNP() {
//
//        test
//
//                .input("--S! |")
//                .input("(S ==> R).")
//                .mustGoal(cycles, "R", 0.0, 0.45f, 0);
//    }
//    @Test
//    void InductionAntigoalNN() {
//
//        test
//
//                .input("--S! |")
//                .input("--(S ==> R).")
//                .mustGoal(cycles, "R", 1, 0.45f, 0);
//    }
//    @Test
//    void InductionAntigoalP() {
//
//        test
//
//                .input("S! |")
//                .input("(--S ==> R).")
//                .mustGoal(cycles, "R", 0.0, 0.45f, 0);
//    }
//    @Test
//    void InductionAntigoalPN() {
//
//        test
//
//                .input("S! |")
//                .input("--(--S ==> R).")
//                .mustGoal(cycles, "R", 1, 0.45f, 0);
//    }
//
//    @Test
//    void DeductionAntigoalPos() {
//
//        test
//
//                .input("R! |")
//                .input("(S ==> --R).")
//                .mustGoal(cycles, "S", 0.0, 0.81f, 0);
//    }
//
//    @Test
//    void DeductionAntigoalNeg() {
//
//        test
//
//                .input("--R! |")
//                .input("(S ==> R).")
//                .mustGoal(cycles, "S", 0.0, 0.81f, 0);
//    }

    @Test
    void ImplicationTerm2() {

        test
                .input("R! |")
                .input("(--S ==> R).")
                .mustGoal(cycles, "S", 0.0f, 0.81f, 0);

    }

    @Disabled @Test
    void NegatedImplicationTerm3() {

        test
                .input("R. |")
                .input("((--,a:b) && R)!")
                .mustGoal(cycles, "a:b", 0.0f, 0.81f, ZERO);
    }


    @Disabled
    @Test
    void disjunctionBackwardsQuestionTemporal() {

        test
                .inputAt(0, "(||, x, y)?")
                .believe("x", Tense.Present, 1f, 0.9f)
                .mustBelieve(cycles, "(&&, (--,x), (--,y))", 0, 0.81f, 0);
    }

    @Test
    void GoalImplComponentTemporal() {

        test
                .input("happy! |")
                .input("((--,in) ==>+1 (happy &&-1 (--,out))).")
                .mustGoal(cycles, "in", 0, 0.42f, 0);
    }

    @Disabled
    @Test
    void GoalImplComponentWithVar() {

        test.nar.runAt(cycles * 4, () -> {
            try {
                test.nar.concept($.$("c($1)")).print();
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }
        });

        test
                .input("((a($x) &&+4 b($x)) ==>-3 c($x)).")
                .inputAt(0, "cx! |")

                .mustGoal(cycles * 5, "bx", 1f, 0.73f,
                        (t) -> t >= 3 /* early since cx is alrady active when this gets derived */);
    }

    @Test
    void PredictiveImplicationTemporalEternal() {

        test
                .inputAt(0, "(out ==>-3 happy).")
                .inputAt(13, "happy! |")
                .mustGoal(cycles, "out", 1f, 0.81f, 16)
                .mustNotOutput(cycles, "out", GOAL, 3);
    }

    @Test
    void PredictiveImplicationEternalTemporal() {

        test
                .inputAt(0, "(out ==>-3 happy). |")
                .inputAt(13, "happy!")
                .mustGoal(cycles, "out", 1f, 0.81f, t->t>=10); //13 as if it were present
    }


    @Test
    void deriveNegInhGoalTemporal() {

        test
                .confMin(0.75f)
                .confMin(3)
                .input("b:a! |")
                .input("c:b.")
                .input("--y:x!  |")
                .input("z:y.")
                .mustGoal(cycles, "c:a", 1, 0.81f, 0)
                .mustGoal(cycles, "z:x", 0, 0.81f, 0);
    }

    @Test
    void StrongUnificationDeductionPP() {

        test
            .input("(y ==>+1 x).")
            .input("y. |")
            .mustBelieve(cycles, "x", 1, 0.81f, 1)
            .mustNotOutput(cycles, "x", BELIEF, t -> t != 1)
//            .mustNotBelieve(cycles, "((y ==>+1 x)==>y)")
            .mustNotBelieve(cycles, "(((y ==>+1 #1) &&+1 #1)==>y)")
            .mustNotBelieve(cycles, "((y &&+1 x) ==>+1 x)")
        ;
    }
    @Test
    void StrongUnificationDeductionPN() {

        test
                .confMin(0.75f)
                .input("(--Y ==>+1 x).")
                .input("--Y. |")
                .mustBelieve(cycles, "x", 1f, 0.81f, 1)
                .mustNotOutput(cycles, "x", BELIEF, (t)->t!=1)
        ;
    }
    @Test
    void StrongUnificationAbductionPP() {

        test
                .input("(X ==>+1 Y).")
                .input("Y. |")
                .mustBelieve(cycles, "X", 1f, 0.45f, -1);
    }

    @Test
    void StrongUnificationAbductionPN() {

        test
                .input("(--X ==>+1 Y).")
                .input("Y. |")
                .mustBelieve(cycles, "X", 0, 0.45f, -1);
    }

}