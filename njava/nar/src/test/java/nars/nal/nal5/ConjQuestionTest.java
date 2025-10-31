package nars.nal.nal5;

import org.junit.jupiter.api.Test;

class ConjQuestionTest extends AbstractNAL5Test {
    @Test
    void ConjQuestion() {
        test.volMax(3)
                .input("(x && y).").input("x?")
                //.mustQuestion(cycles, "(x && y)")
                .mustQuestion(cycles, "y")
        ;
    }

    @Test
    void ConjQuestion_neg() {
        test.volMax(4).input("(--x && y).").input("x?")
                //.mustQuestion(10, "(--x && y)")
                .mustQuestion(cycles, "y")
        ;
    }

    @Test
    void ConjQuest() {
        test.volMax(4).input("(  x && y).").input("x@")
                //.mustQuest(60, "(  x && y)")
                .mustQuest(cycles, "y")
        ;
    }

    @Test
    void ConjQuest_neg() {
        test.volMax(4).input("(--x && y).").input("x@")
                //.mustQuest(60, "(--x && y)")
                .mustQuest(cycles, "y")
        ;
    }

    @Test
    void Conj_Question_Induction_Unify() {
        test
                .volMax(9)
                .confMin(1)
                .input("y(z)?")
                .input("(x(#1) && y(#1)).")
                .mustQuestion(cycles, "(x(z) && y(z))")
                //.mustQuestion(cycles, "x(z)")
        ;
    }

    @Test
    void Conj_Question_Induction_Unify2() {
        test
                .volMax(9)
                .confMin(1)
                .input("y(?1)?")
                .input("(x(z) && y(z)).")
                .mustQuestion(cycles, "x(z)")
        ;
    }

}