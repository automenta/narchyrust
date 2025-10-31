package nars.nal.nal5;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ImplQuestionsTest extends AbstractNAL5Test {


    @Disabled @Test
    void oppositeImplicationQuestion() {
        test.volMax(4)
                .believe("(x ==> y)")
                .mustQuestion(cycles, "(  y ==> x)")
                .mustQuestion(cycles, "(--y ==> x)")
        ;
    }

    @Disabled
    @Test
    void testImplSubj_Questioned() {
        test
                .volMax(3)
                .input("(x ==> y)?")
                .mustQuestion(cycles, "x")
                .mustQuestion(cycles, "y")
        ;
    }

    @Disabled @Test
    void testImplSubj_Questioned_indepVar() {
        test
                .volMax(9)
                .input("(x($1) ==> y($1))?")
                .mustQuestion(cycles, "x(?1)")
                .mustQuestion(cycles, "y(?1)")
        ;
    }

    @Test
    void Impl_Question_Induction_Unify() {
        test
                .volMax(9)
                .confMin(0.8f)
                .input("y(z)?")
                .input("(x($1) ==> y($1)).")
                .mustQuestion(cycles, "x(z)")
                //.mustQuestion(cycles, "(x(z) ==> y(z))")
        ;
    }

    /** involves second-layer termlinking */
    @Test void Impl_Question_Decomposition_Unify() {
        test
                .volMax(9)
                .confMin(0.8f)
                .input("(x($1) ==> y($1))?")
                .input("y(z).")
                .mustQuestion(cycles, "(x(z) ==> y(z))")
                .mustQuestion(cycles, "x(z)")
                //.mustNotQuestion(cycles, "(y($1) ==> y($1))")
        ;
    }

    @Test
    void Conj_Question_Decomposition_Unify() {
        test
                .volMax(9)
                .confMin(0.8f)
                .input("(x(#1) &&+1 y(#1))?")
                .input("y(z).")
                .mustQuestion(cycles, "(x(z) &&+1 y(z))")
                //.mustQuestion(cycles, "x(z)")
                //.mustNotQuestion(cycles, "(y($1) &&+- y($1))")
        ;
    }

    @Test
    void testImplQuestion_Conj() {
        test
            .volMax(13)
            .confMin(1)
            .input("(a && y(z))?")
            .input("(x(z) ==> y(z)).")
            .mustQuestion(cycles, "(a && x(z))")
            .mustQuestion(cycles, "x(z)")
          //.mustQuestion(cycles, "(x(z) ==> (a && y(z)))")
        ;
    }

    @Test
    void deduction_fwd_question_a() {
        test.volMax(3)
            .question("(a ==> x)")
            .believe( "(x ==> z)")
            .mustQuestion(cycles, "(a ==> z)");
    }

    @Test
    void deduction_fwd_question_b() {
        test.volMax(3)
            .question("(x ==> z)")
            .believe( "(a ==> x)")
            .mustQuestion(cycles, "(a ==> z)")
//            .mustNotQuestion(cycles, "(a ==>+- z)")
//            .mustNotQuestion(cycles, "(a ==> x)")
        ;
    }
    @Test
    void exemplificatoin_fwd_question_b() {
        test.volMax(3)
                .question("(x ==> z)")
                .believe( "(a ==> x)")
                .mustQuestion(cycles, "(z ==> a)")
        ;
    }

    @Test void induction_question_fwd_b() {
        test.volMax(8).confMin(0.99f)
                .input("((--x || y) ==> z)?")
                .input("(--x ==> a).")
                .mustQuestion(cycles*3, "(  a ==> z)")
                .mustQuestion(cycles*3, "(--a ==> z)")
//                .mustNotQuestion(cycles, "(--a ==>+- z)")
//                .mustNotQuestion(cycles, "(  a ==>+- z)")
        ;
    }

    @Test
    void deduction_fwd_question_b_neg() {
        test.volMax(4)
                .question("(--x ==> z)")
                .believe( "(a ==> x)")
                .mustQuestion(cycles, "(a ==> z)")
        ;
    }

    @Test
    void deduction_rev_question_b() {
        test.volMax(3)
                .question("(a ==> z)")
                .believe( "(x ==> z)")
                .mustQuestion(cycles, "(a ==> x)");
    }
    @Test
    void deduction_rev_question_a() {
        test.volMax(3)
                .question("(a ==> z)")
                .believe( "(a ==> x)")
                .mustQuestion(cycles, "(x ==> z)");
    }

    @Test
    void inductionSylQuestion1() {
        test.volMax(3)
                .believe("(a ==> x)")
                .question("(a ==> y)")
                .mustQuestion(cycles, "(x ==> y)")
                .mustQuestion(cycles, "(y ==> x)")
        ;
    }
    @Test
    void inductionSylQuestion2() {
        test.volMax(3)
                .believe("(x ==> a)")
                .question("(y ==> a)")
                .mustQuestion(cycles, "(x ==> y)")
                .mustQuestion(cycles, "(y ==> x)")
        ;
    }

    @Test
    void implBeliefToQuestionDecomposition() {
        test.volMax(8)
                .believe("((a||b) ==> x)")
                .question("b")
                .mustQuestion(cycles, "x");
    }

    @Test
    void implBeliefToQuestionDecomposition2() {
        test.volMax(8)
                .believe("(x==>(a||b))")
                .question("b")
                .mustQuestion(cycles, "x");
    }

    @Test
    void implQuestionDecompose_component_disj() {
        test.volMax(10).confMin(0.99f)
                .question("(x==>a)")
                .believe("(x==>(a||b))")
                .mustQuestion(cycles*3, "(x==>b)");
    }
    @Test
    void implQuestionDecompose_disj_component() {
        test.volMax(8)
                .question("(x==>(a||b))")
                .believe("(x==>a)")
                .mustQuestion(cycles*3, "(x==>b)");
    }
}