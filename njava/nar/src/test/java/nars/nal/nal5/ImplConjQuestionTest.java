package nars.nal.nal5;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ImplConjQuestionTest extends AbstractNAL5Test {
    @Test
    void NonConjInImplSubj_Question() {
        test
                .volMax(3).confMin(0.85f)
                .input("(x==>a).")
                .input("x?")
                .mustQuestion(cycles, "a")
        ;
    }

    @Test
    void NonConjInImplSubj_Quest() {
        test
                .volMax(3).confMin(0.85f)
                .input("(x==>a).")
                .input("x@")
                .mustQuest(cycles, "a")
        ;
    }

    @Test
    void ConjInImplSubj_Question() {
        test
                .volMax(5).confMin(0.85f)
                .input("((x && y)==>a).")
                .input("x?")
                .mustQuestion(cycles, "y")
                .mustNotQuestion(cycles, "(y==>a)")
        ;
    }

    @Test
    void ConjInImplSubj_Question_auto() {
        test
                .volMax(5).input("((x && y)==>a)?")
                .mustQuestion(cycles, "(y==>a)")
        ;
    }

    @Test
    void ConjInImplPred_Question() {
        test.volMax(5).confMin(0.9f)
                .input("(a ==> (x && y)).")
                .input("x?")
                .mustQuestion(cycles, "a")
        //.mustQuestion(cycles, "(a==>x)")
        ;
    }

    @Test
    void ConjInImplPred_Quest() {
        test.volMax(5).confMin(0.9f)
                .input("(a ==> (x && y)).")
                .input("x@")
                .mustQuest(cycles, "a")
        //.mustQuestion(cycles, "(a==>x)")
        ;
    }

    @Test
    void testImplSubj_and_ConditionsQuestioned_fwd() {
        test.volMax(3)
                .question("x")
                .input("(x ==> y).")
                .mustQuestion(cycles, "y")
        ;
    }

    @Test
    void testImplSubj_and_ConditionsQuestioned_rev() {
        test.volMax(3)
                .question("y")
                .input("(x ==> y).")
                .mustQuestion(cycles, "x")
        ;
    }

    @Disabled
    @Test
    void testImplNegSubjQuestioned() {
        test.volMax(4)
                .input("(--x ==> y)?")
                .mustQuestion(cycles, "x")
                .mustQuestion(cycles, "y")
        ;
    }

    @Disabled
    @Test
    void testImplConjSubjQuestioned() {
        test
                .volMax(3)
                .input("(x ==> y)?")
                .mustQuestion(cycles, "x")
                .mustQuestion(cycles, "y")
        ;
    }

    @Test
    void ImplDisjPredQustion_A() {
        test.volMax(8).confMin(0.8f)
                .input("(a ==> (x||y))?")
                //.input("(a ==> x).")
                .mustQuestion(cycles, "(a==>x)")
                .mustNotQuestion(cycles, "(a ==> (x||y))")
        ;
    }

    @Test
    void ImplDisjPredQustion_B() {
        test.volMax(8).confMin(0.8f)
                .input("(a ==> (x||y)).")
//                .input("(a ==> x)?")
                .mustBelieve(cycles, "(a==>x)", 1f, 0.81f)
//                .mustNotQuestion(cycles, "(a ==> (x||y))")
        ;
    }

    @Test
    void ImplSubjConjQuestion_A() {
        test.volMax(7).confMin(0.8f)
                .input("((x && y) ==> a)?")
                //.input("(x ==> a).")
                .mustQuestion(cycles, "(y==>a)")
                .mustNotQuestion(cycles, "((x&&y)==>a)")
        ;
    }

    @Test
    void ImplSubjConjQuestion_B() {
        test.volMax(7).confMin(0.9f)
                .input("((x && y) ==> a).")
                .input("(x ==> a)?")
                .mustQuestion(cycles, "(y==>a)")
                .mustNotQuestion(cycles, "(y ==>+- a)")
                .mustNotQuestion(cycles, "(x ==>+- a)")
                .mustNotQuestion(cycles, "((x&&y)==>a)")
                .mustNotQuestion(cycles, "((x&&y) ==>+- x)")
        ;
    }


    @Test
    void ImplSubjdisjQuestion_A() {
        test.volMax(8).confMin(0.8f)
                .input("((x || y) ==> a)?")
                //.input("(x ==> a).")
                .mustQuestion(cycles, "(y==>a)")
                .mustNotQuestion(cycles, "((x||y)==>a)")
        ;
    }

    @Test
    void ImplSubjDisjQuestion_B() {
        test.volMax(8).confMin(0.8f)
                .input("((x || y) ==> a).")
                .input("(x ==> a)?")
                .mustQuestion(cycles, "y")
                .mustNotQuestion(cycles, "(y==>a)")
                .mustNotQuestion(cycles, "((x||y)==>a)")
        ;
    }

    @Test
    void conjPreconditionDecompositionToImpl_BackChaining_Question() {
        test
                .question("((x&&y)==>z)")
                .mustQuestion(cycles, "(x&&y)")
                .mustQuestion(cycles, "(x==>z)")
                .mustQuestion(cycles, "(y==>z)")
//                .mustQuestion(cycles, "(x==>(y&&z))")
//                .mustQuestion(cycles, "(y==>(x&&z))")
        ;
    }

    @Test
    void conjPreconditionDecompositionToImpl_BackChaining_Question_neg() {
        test
                .volMax(6)
                .question("(--(x&&y)==>z)")
                .mustQuestion(cycles, "(x&&y)")
                .mustQuestion(cycles, "(--x ==> z)")
                .mustQuestion(cycles, "(--y ==> z)")
        ;
    }

    @Test
    void conjPostconditionDecompositionToImpl_BackChaining_Question() {
        test
                .question("(z==>(x&&y))")
//                .mustQuestion(cycles, "(x&&y)")
                .mustQuestion(cycles, "(z ==> x)")
                .mustQuestion(cycles, "(z ==> y)")
        ;
    }
}