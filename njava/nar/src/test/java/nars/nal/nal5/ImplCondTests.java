package nars.nal.nal5;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ImplCondTests extends AbstractNAL5Test {

    private static final int cycles = 70;

    @Test
    void deduction_disj_A() {
        test.volMax(8).confMin(0.7f)
                .believe("(a ==> x)")
                .believe("((x || y) ==> z)")
                .mustBelieve(cycles, "(a ==> z)", 1.00f, 0.81f);
    }
    @Test
    void deduction_conj_A() {
        test.volMax(8).confMin(0.7f)
                .believe("(a ==> (x && y))")
                .believe("(x ==> z)")
                .mustBelieve(cycles, "(a ==> z)", 1.00f, 0.81f);
    }
    @Test
    void deduction_conj_B() {
        test.volMax(8).confMin(0.7f)
                .believe("(a ==> (--x && y))")
                .believe("(--x ==> z)")
                .mustBelieve(cycles, "(a ==> z)", 1.00f, 0.81f);
    }
    @Test
    void deduction_disj_A_dont() {
        test.volMax(8).confMin(0.7f)
                .believe("(a ==> x)")
                .believe("(--x ==> z)")
                .mustNotBelieve(cycles, "(a ==> z)");
    }
    @Test
    void deduction_disj_A_neg() {
        test.volMax(8).confMin(0.8f)
                .believe("(a ==> --x)")
                .believe("((--x || y) ==> z)")
                .mustBelieve(cycles, "(a ==> z)", 1.00f, 0.81f);
    }

    @Test
    void deduction_disj_B() {
        test.volMax(8).confMin(0.8f)
                .believe("(a ==> (x && y))")
                .believe("(x ==> z)")
                .mustBelieve(cycles, "(a ==> z)", 1.00f, 0.81f);
    }

    @Test
    void deduction_conj_disj_dont_1() {
        test.volMax(8)
                .believe("(a ==> x)")
                .believe("((x && y) ==> z)")
                .mustNotBelieve(cycles, "(a ==> z)");
    }

    @Test
    void deduction_conj_disj_dont_2() {
        test.volMax(8).confMin(6)
                .believe("(a ==> --x)")
                .believe("((x && y) ==> z)")
                .mustNotBelieve(cycles, "(a ==> z)");
    }

    @Test
    void deduction_conj_disj_dont_3() {
        test.volMax(8)
                .believe("(a ==> --x)")
                .believe("((x || y) ==> z)")
                .mustNotBelieve(cycles, "(a ==> z)");
    }


    @Test
    void deduction_conj_disj_dont_4() {
        test.volMax(8)
                .believe("(a ==> (--x && y))")
                .believe("(x ==> z)")
                .mustNotBelieve(cycles, "(a ==> z)");
    }

    @Test
    void deduction_conj_disj_dont_5() {
        test.volMax(8)
                .believe("(a ==> (x && y))")
                .believe("(--x ==> z)")
                .mustNotBelieve(cycles, "(a ==> z)");
    }



    @Test
    void induction1() {
        test.volMax(8).confMin(0.4f)
            .believe("(x ==> a)")
            .believe("((x || y) ==> z)")
            .mustBelieve(cycles, "(a ==> z)", 1.00f, 0.81f);
    }

    @Test
    void induction2() {
        test.volMax(8).confMin(0.4f)
            .believe("(--x ==> a)")
            .believe("((--x || y) ==> z)")
            .mustBelieve(cycles, "(a ==> z)", 1.00f, 0.81f);
    }

    @Test void induction_question_fwd_a() {
        test.volMax(8).confMin(0.99f)
                .input("(--x ==> a)?")
                .input("((--x || y) ==> z).")
                .mustQuestion(cycles, "(  a ==> z)")
                .mustQuestion(cycles, "(--a ==> z)")
                .mustNotQuestion(cycles, "(--a ==>+- z)")
                .mustNotQuestion(cycles, "(  a ==>+- z)")
        ;
    }

    @Disabled @Test void conditional_intersection_1() {
        test.volMax(11)
            .believe("((&&,a,b,c)   ==> z)")
            .believe("((&&,  b,c,d) ==> z)")
            .mustBelieve(cycles, "((&&,b,c) ==> z)", 1, 0.81f)
            .mustBelieve(cycles, "((&&,(a||d),b,c) ==> z)", 1, 0.81f)
        ;
    }
    @Disabled
    @Test void conditional_intersection_1a() {
        test.volMax(13)
                .believe("((y-->(&&,a,b,c))   ==> z)")
                .believe("((y-->(&&,  b,c,d)) ==> z)")
                .mustBelieve(cycles, "((y-->(&&,b,c)) ==> z)", 1, 0.81f)
                .mustBelieve(cycles, "((y-->(&&,(a||d),b,c)) ==> z)", 1, 0.81f)
        ;
    }
    @Disabled @Test void conditional_intersection_2() {
        test.volMax(14)
            .believe("(--(&&,a,b,c)   ==> z)")
            .believe("(--(&&,  b,c,d) ==> z)")
            .mustBelieve(cycles, "(--(&&,b,c) ==> z)", 1, 0.81f)
        ;
    }


    @Test
    void conditional_induction_described() {
        test.volMax(11);
        test.confMin(0.7f);
        test.believe("<(&&,(robin --> [chirping]),(robin --> [flying])) ==> a>");
        test.believe("<(robin --> [flying]) ==> (robin --> [withBeak])>", 0.9f, 0.9f);
        test.mustBelieve(cycles, "<(&&,(robin --> [chirping]),(robin --> [withBeak])) ==> a>",
                1.00f, 0.73f);
    }

    @Test
    void conditional_induction_conj() {
        test.volMax(5).confMin(0.2f)
                .believe("((x && a) ==> z)")
                .believe("(a ==> y)", 0.9f, 0.9f)
                .mustBelieve(cycles, "((x && y) ==> z)", 0.95f, 0.73f);
    }
//    @Test
//    void conditional_multi_deduction_conj() {
//        test.volMax(5).confMin(0.2f)
//            .believe("(z ==> (x && a))")
//            .believe("(a ==> y)", 0.9f, 0.9f)
//            .mustBelieve(cycles, "(z ==> (x && y))", 0.95f, 0.73f);
//    }

    @Test
    void conditional_induction_conj_mismatch() {
        test.volMax(8).confMin(0.2f)
                .believe("((x && --a) ==> z)")
                .believe("(a ==> y)", 0.9f, 0.9f)
                .mustNotBelieve(cycles, "((x && y) ==> z)");
    }

    @Test
    void conditional_induction_conj_mismatch2() {
        test.volMax(5).confMin(0.2f)
                .believe("((x && a) ==> z)")
                .believe("(--a ==> y)", 0.9f, 0.9f)
                .mustNotBelieve(cycles, "((x && y) ==> z)");
    }

    @Test
    void conditional_induction_conj_neg() {
        test.volMax(7).confMin(0.2f)
                .believe("((x && --a) ==> z)")
                .believe("(--a ==> y)", 0.9f, 0.9f)
                .mustBelieve(cycles, "((x && y) ==> z)", 0.95f, 0.73f);
    }

    @Test
    void conditional_induction_disj() {
        test.volMax(12).confMin(0.2f)
                .believe("((x || a) ==> z)")
                .believe("(a ==> y)", 0.9f, 0.9f)
                .mustBelieve(cycles, "((x || y) ==> z)", 0.95f, 0.73f);

    }

    @Test
    void conditional_induction_weak_conj() {
        test.volMax(5).confMin(0.2f)
                .believe("((x && a) ==> z)")
                .believe("(y ==> a)", 0.9f, 0.9f)
                .mustBelieve(cycles, "((x && y) ==> z)", 0.95f, 0.42f);
    }

    @Disabled
    @Test
    void conditional_induction_inh_conj() {
        test.volMax(9).confMin(0.7f)
                .believe("(((x && a)-->d) ==> z)")
                .believe("((a-->d) ==> (y-->d))", 0.9f, 0.9f)
                .mustBelieve(cycles, "(((x && y)-->d) ==> z)", 1.00f, 0.73f);
    }

    @Test
    void conditional_induction_conj_neg_event() {
        test.volMax(7).confMin(0.7f)
                .believe("((x && --y) ==> z)")
                .believe("(--y ==> a)", 0.9f, 0.9f)
                .mustBelieve(cycles, "((a && x) ==> z)", 0.95f, 0.73f);
    }

    @Test
    void conditional_induction_conj_neg_condition() {
        test.volMax(9).confMin(0.3f)
                .believe("((x && y) ==> z)")
                .believe("(y ==> a)", 0.1f, 0.9f)
                .mustBelieve(cycles, "((--a && x) ==> z)", 0.95f, 0.73f);
    }


}