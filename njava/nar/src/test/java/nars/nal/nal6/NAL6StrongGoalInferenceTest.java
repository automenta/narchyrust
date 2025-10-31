package nars.nal.nal6;

import nars.time.Tense;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.GOAL;

class NAL6StrongGoalInferenceTest extends AbstractNAL6Test {

    @Test
    void demandOpposite() {
        test.volMax(5).input("b!").input("(c ==> --b).").mustGoal(cycles, "c", 0f, 0.81f);
    }

    @Test
    void quest_induction_nal6() {
        test.volMax(5)
                .goal("a")
                .goal("b")
                .mustQuest(cycles, "(a&b)")
                .mustGoal(cycles, "(a&b)", 1f, 0.81f)
        ;
    }



//    @Test
//    void quest_induction_2() {
//        test.volMax(5)
//            .goal("x:a")
//            .believe("x:(a & b)")
//            .mustQuest(cycles, "x:b")
//        ;
//    }

    @Test
    void Goal_Match_ImplSubj() {
        test
            .confMin(0.4f).volMax(3)
            .believe("(x ==> y)")
            .goal("x", Tense.Eternal, 1.00f, 0.90f)
            .mustGoal(cycles, "y", 1.00f, 0.45f)
            .mustNotOutput(cycles, "y", GOAL, 0.00f, 0.5f, 0, 0.9f, t -> true)
        ;
    }
    @Test
    void Goal_Match_ImplPred() {
        test
            .confMin(0.8f).volMax(3)
            .believe("(y ==> x)")
            .goal("x")
            .mustGoal(cycles, "y", 1.00f, 0.81f)
            .mustNotOutput(cycles, "y", GOAL, 0.00f, 0.5f, 0, 0.9f, t -> true)
        ;
    }

    @Disabled
    @Test
    void Goal_Match_ImplPredConj() {
        test
                .confMin(0.75f).volMax(5)
                .believe("(y ==> (a&&b))")
                .goal("a")
                .mustGoal(cycles, "y", 1.00f, 0.81f)
                .mustNotOutput(cycles, "y", GOAL, 0.00f, 0.5f, 0, 0.9f, t -> true)
        ;
    }

    @Disabled @Test
    void PosGoal_Match_ImplPred_Maybe() {
        test
            .volMax(3)
            .believe("(y ==> x)", 0.5f, 0.9f)
            .goal("x", Tense.Eternal, 1.00f, 0.9f)
            .mustGoal(cycles, "y", 0.5f, 0.45f)
        ;
    }
    @Test
    void MaybeGoal_Match_ImplPred_Maybe() {

        test
                .volMax(3)
                .believe("(y ==> x)", 0.5f, 0.9f)
                .goal("x", Tense.Eternal, 0.5f, 0.90f)
                .mustGoal(cycles, "y", 1, 0.81f)
        ;
    }
    @Test
    void MaybeBelief_Match_ImplSubj_Maybe() {
        test
                .volMax(3)
                .believe("(y ==> x)", 0.5f, 0.9f)
                .believe("y", 0.5f, 0.90f)
                .mustBelieve(cycles, "x", 0.5f, 0.4f)
        ;
    }

    @Test
    void GoalMatchSubjOfImplWithVariable_Pos() {
        test
                .confMin(0f)
                .believe("(x($1)==>y($1))")
                .goal("x(a)", Tense.Eternal, 1.00f, 0.90f)
                .mustGoal(cycles, "y(a)", 1.00f, 0.45f)
                .mustNotOutput(cycles, "y(a)", GOAL, 0.00f, 0.5f, 0, 0.9f, t -> true)
        ;
    }

    @Test
    void GoalMatchSubjOfImplWithVariable_PosWeak() {
        test
                .confMin(0f)
                .believe("(x($1)==>y($1))", 0.75f, 0.9f)
                .goal("x(a)", Tense.Eternal, 1.00f, 0.90f)
                .mustGoal(cycles, "y(a)", 0.75f, 0.45f)
//            .mustGoal(cycles, "y(a)", 0.25f, 0.1f)
        ;
    }

    @Test
    void GoalMatchNegatedSubjOfImplWithVariable() {
        test
                .confMin(0f)
                .believe("(--x($1)==>y($1))")
                .goal("x(a)", Tense.Eternal, 0.00f, 0.90f)
                .mustGoal(cycles, "y(a)", 1.00f, 0.45f)
                .mustNotOutput(cycles, "y(a)", GOAL, 0f, 0.5f, 0, 0.9f, t -> true)
        ;
    }

    @Test
    void GoalMatchNegatedSubjOfImplWithVariable_Neg() {
        test
                .confMin(0)
                .believe("(--x($1) ==> --y($1))")
                .goal("x(a)", Tense.Eternal, 0.00f, 0.90f)
                .mustGoal(cycles, "y(a)", 0.00f, 0.45f)
                .mustNotOutput(cycles, "y(a)", GOAL, 0.5f, 1f, 0, 0.9f, t -> true)
        ;
    }

    @Test
    void GoalMatchSubjOfImplWithVariableNeg() {
        test
                .confMin(0)
                .believe("--(x($1)==>y($1))")
                .goal("x(a)", Tense.Eternal, 1.00f, 0.90f)
                .mustGoal(cycles, "y(a)", 0.00f, 0.45f)
                .mustNotOutput(cycles, "y(a)", GOAL, 0.5f, 1f, 0, 0.9f, t -> true)
        ;
    }

    @Test
    void GoalMatchPredOfImplWithVariable() {
        test
                .believe("(x($1)==>y($1))")
                .goal("y(a)", Tense.Eternal, 1.00f, 0.90f)
                .mustGoal(cycles, "x(a)", 1.00f, 0.81f);
    }
}