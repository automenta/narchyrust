package nars.nal.nal3;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static nars.Op.GOAL;

public class NAL3DecomposeGoalTest {

    public static final int cycles = 100;

    @Nested
    class NAL3DecomposeDoubleGoalTest extends NAL3Test {
        @Test
        void quest_induction_nal3() {
            test.volMax(6)
                    .goal("x:a")
                    .goal("x:b")
                    .mustQuest(cycles, "x:(a &   b)")
                    .mustQuest(cycles, "x:(a & --b)")
                    .mustGoal(cycles, "x:(a&b)", 1f, 0.81f)
            ;
        }

        @Disabled @Test
        void reduceConjLike() {
            test
                .volMax(7)
                .input("--((a&b)-->g)!")
                .input("(a-->g).")
                .mustGoal(cycles, "(a-->g)", 0f, 0.81f) //divide
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f) //desire
                .mustNotOutput(cycles, "(a-->g)", GOAL, 0.5f, 1f,0,1, t->true)
            ;
        }
        @Disabled @Test
        void reduceConjLike_weaker() {
            test
                .volMax(7)
                .input("--((a&b)-->g)!")
                .input("(a-->g). %0.75%")
                .mustGoal(cycles, "(a-->g)", 0f, 0.61f) //divide
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f) //desire
                .mustNotOutput(cycles, "(a-->g)", GOAL, 0.5f, 1f,0,1, t->true)
            ;
        }
        @Disabled @Test
        void reduceConjLikeNeg() {
            test
                .volMax(7)
                .input("--((b & --a)-->g)!")
                .input("--(a-->g).")
                .mustGoal(cycles, "(a-->g)", 1f, 0.81f) //divide
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f) //desire
                .mustNotOutput(cycles, "(a-->g)", GOAL, 0f, 0.5f,0,1, t->true)
            ;
        }
        @Disabled @Test
        void decompose_Conj_Goal_neg_decompose_pos_nal3() {
            test.volMax(5)
                .input("(x-->(a&b))! %0.1;0.9%")
                .input("(x-->b). %0.9;0.9%")
                .mustGoal(cycles, "(x-->a)", 0.19f, 0.74f)
                .mustNotGoal(cycles, "(x-->b)", 0.2f, 1f);
        }

        @Test
        void UnionGoalDoubleDecomposeSubj() {
            test.volMax(7).confMin(0.75f)
                .input("((a|b)-->g)!")
                .input("--(a-->g).")
                .mustGoal(cycles, "(b-->g)", 1, 0.81f);
        }
        @Test
        void UnionGoalDoubleDecomposeSubjNothing() {
            test.volMax(7)
                .input("((a|b)-->g)!")
                .input("(a-->g).")
                .mustOutputNothing(cycles);
        }
//        @Test
//        void UnionGoalDoubleDecomposeSubjNeg() {
//            test
//                .termVolMax(7)
//                .confMin(0.75f)
//                .input("((--a|b)-->g)!")
//                .input("(a-->g).")
//                .mustGoal(cycles, "(b-->g)", 1f, 0.81f)
//                .mustGoal(cycles, "(b-->g)", 0f, 0.81f)
//            ;
//        }
        @Disabled
        @Test
        void UnionGoalDoubleDecomposePred() {
            test
                .volMax(3)
                .input("(g-->(a|b))!")
                .input("--(g-->a).")
                .mustGoal(cycles, "(g-->b)", 1f, 0.81f)
                .mustNotOutput(cycles, "(g-->a)", GOAL)
            ;
        }


    }

    @Nested
    @Disabled
    class NAL3DecomposeSingleGoalTest extends NAL3Test {


//        @Test
//        void UnionSinglePremiseDecomposeGoal1Pos() {
//            test
//                .termVolMax(7)
//                .confMin(0.75f)
//                .input("((a|b)-->g)!")
//                .mustGoal(cycles, "(a-->g)", 1f, 0.81f)
//                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
//        }

        @Test
        void IntersectionSinglePremiseDecomposeGoal1Pos() {
            test
                .volMax(6)
                .confMin(0.75f)
                .input("((a&b)-->g)!")
                .mustGoal(cycles, "(a-->g)", 1f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f);
        }


        @Test
        void IntersectionPosIntersectionSubGoalSinglePremiseDecompose() {
            test
                .input("((a&b)-->g)!")
                .mustGoal(cycles, "(a-->g)", 1f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 1f, 0.81f)
            ;
        }

        @Test
        void IntersectionPosIntersectionPredGoalSinglePremiseDecompose() {
            test
                .input("(g-->(a&b))!")
                .mustGoal(cycles, "(g-->a)", 1f, 0.81f)
                .mustGoal(cycles, "(g-->b)", 1f, 0.81f)
            ;
        }

        @Test
        void NegIntersection2BeliefSinglePremiseDecompose() {
            test
                .input("--((a&b)-->g).")
                .mustBelieve(cycles, "(a-->g)", 0f, 0.81f)
                .mustBelieve(cycles, "(b-->g)", 0f, 0.81f)
            ;
        }

        @Test
        void NegIntersection2GoalSinglePremiseDecompose() {

            test
                .input("--((a&b)-->g)!")
                .mustGoal(cycles, "(a-->g)", 0f, 0.81f)
                .mustGoal(cycles, "(b-->g)", 0f, 0.81f)
            ;
        }
        @Test
        void NegIntersection3GoalSinglePremiseDecompose() {

            test
                .volMax(12)
                .input("--((&,a,b,c)-->g)!")
                .mustGoal(cycles, "(a-->g)", 0f, 0.73f)
                .mustGoal(cycles, "(b-->g)", 0f, 0.73f)
                .mustGoal(cycles, "(c-->g)", 0f, 0.73f)
            ;
        }

//        @Test
//        void NegUnionBeliefSinglePremiseDecompose() {
//            test
//                .input("--((a|b)-->g).")
//                .mustBelieve(cycles, "(a-->g)", 0f, 0.81f)
//                .mustBelieve(cycles, "(b-->g)", 0f, 0.81f)
//            ;
//        }


//        @Test
//        void NegUnionGoalSinglePremiseDecompose() {
//            test
//                .input("--((a|b)-->g)!")
//                .mustGoal(cycles, "(a-->g)", 0f, 0.81f)
//                .mustNotOutput(cycles, "(a-->g)", GOAL, 0.1f, 1f, 0, 1)
//                .mustGoal(cycles, "(b-->g)", 0f, 0.81f)
//                .mustNotOutput(cycles, "(b-->g)", GOAL, 0.1f, 1f, 0, 1)
//            ;
//        }

//        @Test
//        void IntersectionConditionalDecomposeGoalNeg() {
//            test
//                    .input("--((a|b)-->g)!")
//                    .input("--(a-->g).")
//                    .mustGoal(cycles, "(b-->g)", 0f, 0.81f);
//        }

    }



}