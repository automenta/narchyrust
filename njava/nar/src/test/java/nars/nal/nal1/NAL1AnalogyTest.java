package nars.nal.nal1;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.QUESTION;
import static nars.nal.nal1.NAL1Test.cycles;

class NAL1AnalogyTest extends NALTest {
    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(6 /* for analogy */);
        n.complexMax.set(7);
        return n;
    }

    @Test
    void analogy() {
        test
            .believe("<gull <-> swan>")
            .believe("<swan --> swimmer>")
            .mustBelieve(cycles, "<gull --> swimmer>", 1.0f, 0.81f);
    }

    @Test void analogy_subj() {
        //          #R[(M --> P) (S <-> M) |- (S --> P) :pre ((:!= S P)) :post (:t/analogy :d/strong :allow-backward)]
        test
                .volMax(3)
                .believe("(m<->s)")
                .believe("(m-->p)")
                .mustBelieve(cycles, "(s-->p)", 1, 0.81f)
        //.mustNotQuestion(cycles, "(p<->s)")
        ;
    }
    @Test void analogy_pred() {
        test
            .believe("(a<->b)")
            .believe("(x-->a)")
            .mustBelieve(cycles, "(x-->b)", 1.0f, 0.81f);
    }


    @Test
    void analogyNeg() {
        test
            .believe("(p1 --> p2)", 0.1f, 0.9f)
            .believe("(p2 <-> p3)", 1, 0.9f)
            .mustBelieve(cycles, "(p1 --> p3)", 0.1f, 0.81f);

    }

    @Test void analogy_question() {
        test
            .believe("(a<->b)")
            .question("(x-->a)")
            .mustQuestion(cycles, "(x-->b)");
    }

    @Test void analogy_quest() {
        test
                .believe("(a<->b)")
                .quest("(x-->a)")
                .mustQuest(cycles, "(x-->b)");
    }

    @Test
    void comparison_question() {
        test
                .believe("<swan --> swimmer>", 0.9f, 0.9f)
                .askAt(0, "<swan --> bird>")
                .mustQuestion(cycles, "<bird <-> swimmer>");
    }

    @Test
    void comparison_question_2() {
        test.askAt(0,"<sport --> competition>")
            .believe("<chess --> competition>", 0.9f, 0.9f)
            .mustQuestion(cycles, "<chess <-> sport>");
    }

    @Test
    void comparisonPosPos() {
        test
            .believe("(swan-->swimmer)")
            .believe("(swan-->dinosaur)")
            //.mustBelieve(cycles, "((swan-->swimmer) <-> (swan-->dinosaur))", 1, 0.45f)
            .mustBelieve(cycles, "(dinosaur <-> swimmer)", 1, 0.45f)
        ;
    }
    @Test
    void comparisonPosPos_prod() {
        test.volMax(11)
            .believe("((swan,x)-->swimmer)")
            .believe("((swan,x)-->dinosaur)")
            .mustBelieve(cycles, "(dinosaur <-> swimmer)", 1, 0.45f)
        ;
    }

    @Test
    void comparisonPosNeg() {
        test
                .believe("(swan-->swimmer)", 0.9f, 0.9f)
                .believe("(swan-->dinosaur)", 0, 0.9f)
                .mustBelieve(cycles, "(dinosaur <-> swimmer)", 0f, 0.42f);
    }


//    @Test void comparisonStructuralPosSubj() {
//        test.volMax(7)
//            .believe("((b-->a) <-> (c-->a))")
//            .mustBelieve(cycles, "(b<->c)", 1, 0.45f);
//    }
//    @Test void comparisonStructuralPosPred() {
//        test.volMax(7)
//            .believe("((a-->b) <-> (a-->c))")
//            .mustBelieve(cycles, "(b<->c)", 1, 0.45f);
//    }

    @Test
    void inh_to_sim() {
        test.volMax(3)
            .believe("(x-->y)")
            .believe("(y-->x)")
            .mustBelieve(cycles, "(x<->y)", 1, 0.81f)
        ;
    }

    @Test
    void variable_elimination_analogy_substIfUnify_Question() {

        test
                .volMax(7)
                .confMin(0.9f)
                .believe("((bird --> $x) <-> (swimmer --> $x))")
                .question("(bird --> swan)")
                .mustQuestion(cycles, "(swimmer --> swan)")
                .mustNotOutput(cycles, "swimmer", QUESTION)
                .mustNotOutput(cycles, "swan", QUESTION)
        ;

    }

    @Disabled @Test void inhAnalogyWTF() {
        test
                .volMax(7)
                .believe("((a-->x) --> (b-->x))")
                .mustBelieve(cycles, "(a-->b)", 1f, 0.45f);
    }


    @Disabled @Test void sim_compose() {
        test
                .volMax(7)
                .believe("(x --> (a --> b))")
                .believe("(x --> (b --> a))")
                .mustBelieve(cycles, "(x --> (a<->b))", 1f, 0.81f);
    }

    @Disabled
    @Test
    void comparisonStructuralNeg() {
        test
                .believe("--((a-->b) <-> (a-->c))")
                .mustBelieve(cycles, "(b<->c)", 0.0f, 0.45f);
    }


    @Disabled @Test
    void whatDifferenceDoesItMake() {
        test
                .believe("(x <-> y)")
                .believe("(x <-> z)")
                .mustQuestion(cycles, "(x <-> (y && --z))")
                .mustQuestion(cycles, "(x <-> (z && --y))");
    }


    /**
     * fair "variable" var-shifting to get #1 to unify with #2
     */
    @Test
    void VarShifts_Belief() {
        test
                .volMax(11)
                .believe("(((a)-->#1)-->z)")
                .believe("((x(#1) && y(#2))-->z)")
//                .mustBelieve(cycles, "x(#1)", 1f, 0.81f)
//                .mustBelieve(cycles, "y(#1)", 1f, 0.81f)
                .mustBelieve(cycles, "((x(a) && y(#1))-->z)", 1f, 0.43f) //anonymous analogy
                .mustBelieve(cycles, "((x(#1) && y(a))-->z)", 1f, 0.43f) //anonymous analogy
//                .mustBelieve(cycles, "((y(a) && x(y))-->z)", 1f, 0.43f) //anonymous analogy
        //.mustBelieve(cycles, "(x(a) && y(x))", 1f, 0.43f) //anonymous analogy //??
        ;
    }

    /**
     * fair "variable" var-shifting
     */
    @Test
    void VarShifts_Question() {
        test
                .volMax(9)
                .confMin(0.8f)
                .believe("((a)-->#1)")
                .question("(x(#1) && y(#2))")
//                .mustQuestion(cycles, "x(#1)")
//                .mustQuestion(cycles, "y(#1)")
                .mustQuestion(cycles, "(x(a) && y(#1))")
                .mustQuestion(cycles, "(x(#1) && y(a))")
        ;
    }

    @Test
    void inheritanceToSimilarity() {
        test
                .input("(a-->c).")
                .input("(c-->a).")
                .mustBelieve(cycles, "(a<->c)", 1f, 0.81f)
        ;
    }


    @Test
    void inheritanceToSimilarity_neg() {


        test
                .believe("(swan --> bird)")
                .believe("(bird --> swan)", 0.1f, 0.9f)
                .mustBelieve(cycles, "(bird <-> swan)", 0.1f, 0.81f);

    }



    /**
     * ReduceConjunction
     */
    @Disabled @Test
    void inheritanceToSimilarity2() {

        test
                .volMax(3)
                .believe("(bird <-> swan)", 0.1f, 0.9f)
                .question("(bird --> swan)")
                .mustBelieve(cycles, "(bird --> swan)", 0.32f, 0.81f);
    }

    @Disabled @Test
    void similarity_question_to_inh() {

        test
                .volMax(3)
                .question("(bird <-> swan)")
                .mustQuestion(cycles, "(bird --> swan)")
                .mustQuestion(cycles, "(swan --> bird)")
        ;
    }
    /**
     * tests that although the task and belief do not temporally intersect, the belief can still be used to derive the projected result
     * adapted from: NAL1Test
     */
    @Test
    void temporalAnalogy_Project() {
        test
            .believe("<gull <-> swan>", 1f, 0.9f, 0, 1)
            .believe("<swan --> swimmer>", 1f, 0.9f, 4, 5)
            .mustBelieve(cycles, "<gull --> swimmer>",
                1.0f, 0.17f, (s, e) -> s == 4 && e == 5);
    }

    @Disabled @Test void sim_belief_decompose() {
        test
            .volMax(3)
            .believe("(x <-> y)")
            .mustBelieve(cycles, "(x --> y)", 1f, 0.81f)
            .mustBelieve(cycles, "(y --> x)", 1f, 0.81f)
        ;
    }

    @Test void sim_goal_decompose() {
        test
                .volMax(3)
                .goal("(x <-> y)")
                .mustGoal(cycles, "(x --> y)", 1f, 0.81f)
                .mustGoal(cycles, "(y --> x)", 1f, 0.81f)
        ;
    }
    @Test void sim_question_decompose() {
        test
                .volMax(3)
                .question("(x <-> y)")
                .mustQuestion(cycles, "(x --> y)")
                .mustQuestion(cycles, "(y --> x)")
        ;
    }

}