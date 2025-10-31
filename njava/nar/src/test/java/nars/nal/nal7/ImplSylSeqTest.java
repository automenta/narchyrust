package nars.nal.nal7;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$c;
import static nars.Op.BELIEF;
import static nars.term.util.impl.ImplSyl.implSylDT;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ImplSylSeqTest extends AbstractNAL7Test {

    private static final int cycles = 5;

    @Test
    void impl_deduction_1() {
        assertEquals(3147 /*TODO check*/, implSylDT(
                $$c("((--,(x-->(nar,lag))) ==>-2707 (((x-->conceptualize) &&+2427 (x-->premised))&&(--,(x-->(loop,lag)))))"),
                $$c("((--,(x-->(loop,lag))) ==>-440 (freq(($1,d),meta)<->freq($1,a)))"),
                -2707, -1, -440, -1, 'i'
        ));

        test.volMax(3)
            .input("(a ==>+1 b).")
            .input("(b ==>+2 c).")
            .mustBelieve(cycles, "(a ==>+3 c)", 1, 0.81f)
            .mustNotBelieve(cycles, "(a ==> c)")
            .mustNotBelieve(cycles, "(a ==>-3 c)")
        ;
    }

    @Test
    void impl_deduction_1_subcond_a_later() {

        test.volMax(5)
                .input("(a ==>+1 (x &&+1 b)).")
                .input("(b ==>+2 c).")
                .mustBelieve(cycles, "(a ==>+4 c)", 1, 0.81f)
                .mustNotBelieve(cycles, "(a ==>+3 c)")
                .mustNotBelieve(cycles, "(a ==>-4 c)")
                .mustNotBelieve(cycles, "(a ==> c)")
        ;
    }
    @Test
    void impl_deduction_1_subcond_a_earlier() {
        test.volMax(5)
                .input("(a ==>+1 (b &&+1 x)).")
                .input("(b ==>+2 c).")
                .mustBelieve(cycles, "(a ==>+3 c)", 1, 0.81f)
                .mustNotBelieve(cycles, "(a ==>-3 c)")
                .mustNotBelieve(cycles, "(a ==> c)")
        ;
    }
    @Test
    void impl_deduction_1_subcond_b_early() {
        test.volMax(5)
                .input("(a ==>+1 b).")
                .input("((b &&+1 x) ==>+2 c).")
                .mustBelieve(cycles, "(a ==>+4 c)", 1, 0.81f)
                .mustNotBelieve(cycles, "(a ==>+3 c)")
                .mustNotBelieve(cycles, "(a ==> c)")
        ;
    }

    @Test
    void impl_deduction_1_subcond_b_late() {

        test.volMax(5)
                .input("(a ==>+1 b).")
                .input("((x &&+1 b) ==>+2 c).")
                .mustBelieve(cycles, "(a ==>+3 c)", 1, 0.81f)
                .mustNotBelieve(cycles, "(a ==>+4 c)")
                .mustNotBelieve(cycles, "(a ==> c)")
        ;
    }

    @Test
    void impl_deduction_1_subcond_b_post_neg() {

        test.volMax(6)
                .input("(a ==>+1 --b).")
                .input("((x &&+1 --b) ==>+2 c).")
                .mustBelieve(cycles, "(a ==>+3 c)", 1, 0.81f)
                .mustNotBelieve(cycles, "(a ==>+4 c)")
                .mustNotBelieve(cycles, "(a ==> c)")
        ;
    }

    @Test
    void impl_deduction_subj_seq() {
        test.volMax(7)
                .input("((a &&+5 x) ==>+1 b).")
                .input("((b &&+1 x) ==>+2 c).")
                .mustBelieve(cycles, "((a &&+5 x) ==>+4 c)", 1, 0.81f)
                .mustNotBelieve(cycles, "((a &&+5 x) ==>+3 c)")
                .mustNotBelieve(cycles, "((a &&+5 x) ==> c)")
        ;
    }
    @Test
    void impl_deduction_pred_seq() {
        test.volMax(7)
                .input("(a ==>+1 b).")
                .input("((b &&+1 x) ==>+2 (c &&+5 x)).")
                .mustBelieve(cycles, "(a ==>+4 (c &&+5 x))", 1, 0.81f)
                .mustNotBelieve(cycles, "(a ==>+3 (c &&+5 x))")
                .mustNotBelieve(cycles, "(a ==> (c &&+5 x))")
        ;
    }

    @Test
    void impl_exemplification_1_subcond_a() {
        test.volMax(5)
            .input("(a ==>+1 (x &&+1 b)).")
            .input("(b ==>+2 c).")
            .mustBelieve(cycles, "(c ==>-4 a)", 1, 0.45f)
            .mustNotBelieve(cycles, "(c ==>-3 a)")
            .mustNotBelieve(cycles, "(c ==> a)")
        ;
    }


    @Test
    void impl_induction_pred_seq_equal() {
        test.volMax(5)
                .input("(a ==>+2 (y &&+1 x)).")
                .input("(b ==>+1 (y &&+1 x)).")
                .mustBelieve(cycles, "(a ==>+1 b)", 1, 0.45f)
                .mustBelieve(cycles, "(b ==>-1 a)", 1, 0.45f)
                .mustNotBelieve(cycles, "(a ==>-1 b)")
                .mustNotBelieve(cycles, "(b ==>+1 a)")
                .mustNotBelieve(cycles, "(a ==>+2 b)")
                .mustNotBelieve(cycles, "(a ==> b)")
        ;
    }

    @Test
    void impl_induction_subj_seq_equal() {

        test.volMax(5)
                .input("((x &&+1 y) ==>+2 a).")
                .input("((x &&+1 y) ==>+1 b).")
                .mustBelieve(cycles, "(b ==>+1 a)", 1, 0.45f)
                .mustBelieve(cycles, "(a ==>-1 b)", 1, 0.45f)
                .mustNotBelieve(cycles, "(a ==>+2 b)")
                .mustNotBelieve(cycles, "(a ==>+1 b)")
                .mustNotBelieve(cycles, "(b ==>-1 a)")
                .mustNotBelieve(cycles, "(a ==> b)")
        ;
    }


    @Test
    void impl_induction_subj_subcond() {
        String a = "(x &&+1 y)";
        String c = "x";
        assertInduction(a, "b", c, "d");
    }


    private void assertInduction(String a, String b, String c, String d) {
        test.volMax(8)
                .input("(" + a + " ==>+1 " + b + ").")
                .input("(" + c + " ==>+1 " + d + ").")
                .mustBelieve(cycles, "(" + d + " ==>+1 " + b + ")", 1, 0.45f)
                .mustBelieve(cycles, "(" + b + " ==>-1 " + d + ")", 1, 0.45f)
                .mustNotBelieve(cycles, "(" + b + " ==>+1 " + d + ")")
                .mustNotBelieve(cycles, "(" + d + " ==>-1 " + b + ")")
                .mustNotBelieve(cycles, "(" + b + " ==>+2 " + d + ")")
                .mustNotBelieve(cycles, "(" + b + " ==> " + d + ")")
                .mustNotBelieve(cycles, "(" + d + " ==> " + b + ")")
        ;
    }


    @Test
    void impl_induction_pred_subcond() {
        //(B ==> X), (A ==> Y), cond(Y,  X), --seq(Y), --var({X,Y,A,B})  |- implSyl(B,A,1,-1,p), (Belief:InductionPP)

        test.volMax(5)
            .input("(a ==>+1 (y &&+1 x)).")
            .input("(b ==>+1 x).")
            .mustBelieve(cycles, "(a ==>+1 b)", 1, 0.45f)
            .mustBelieve(cycles, "(b ==>-1 a)", 1, 0.45f)
            .mustNotBelieve(cycles, "(a ==>+2 b)")
            .mustNotBelieve(cycles, "(a ==> b)")
        ;
    }

    @Test
    void impl_deduction_1_subcond_aNeg() {

        test.volMax(6)
                .input("(a ==>+1 (x &&+1 --b)).")
                .input("(--b ==>+2 c).")
                .mustBelieve(cycles, "(a ==>+4 c)", 1, 0.81f)
                .mustNotBelieve(cycles, "(a ==>+3 c)")
                .mustNotBelieve(cycles, "(a ==> c)")
        ;
    }

    @Test
    void impl_deduction_2() {
        test.volMax(9)
                .input("((a &&+1 a) ==>+2 (b &&+3 b)).")
                .input("((b &&+3 b) ==>+5 (c &&+7 c)).")
                .mustBelieve(cycles, "((a &&+1 a) ==>+10 (c &&+7 c))", 1, 0.81f)
                .mustNotBelieve(cycles, "((a &&+1 a) ==> (c &&+7 c))")
        ;
    }
    @Test
    void impl_induction_abduction_subj_2() {
        test.volMax(9)
            .input("((a &&+1 a) ==>+2 (b &&+3 b)).")
            .input("((c &&+5 c) ==>+7 (b &&+3 b)).")
            .mustBelieve(cycles, "((a &&+1 a) ==>-10 (c &&+5 c))", 1, 0.45f)
            .mustBelieve(cycles, "((c &&+5 c) ==>+4 (a &&+1 a))", 1, 0.45f)
            .mustNotBelieve(cycles, "((c &&+5 c) ==>+9 (a &&+1 a))")
            .mustNotBelieve(cycles, "((c &&+5 c) ==>+5 (a &&+1 a))")
            .mustNotBelieve(cycles, "((a &&+1 a) ==>-5 (c &&+5 c))")
        ;
    }

    @Test
    void impl_induction_abduction_pred_2() {
        test.volMax(9)
            .input("((b &&+1 b) ==>+2 (a &&+3 a)).")
            .input("((b &&+1 b) ==>+7 (c &&+11 c)).")
            .mustBelieve(cycles, "((a &&+3 a) ==>+2 (c &&+11 c))", 1, 0.45f)
            .mustBelieve(cycles, "((c &&+11 c) ==>-16 (a &&+3 a))", 1, 0.45f)
            .mustNotBelieve(cycles, "((c &&+11 c) ==>-8 (a &&+3 a))")
            .mustNotBelieve(cycles, "((a &&+3 a) ==>+4 (c &&+11 c))")
            .mustNotBelieve(cycles, "((a &&+3 a) ==>-6 (c &&+11 c))")
        ;
    }
    @Test
    void impl_induction_abduction_subj_1() {
        test.volMax(3)
                .input("(a ==>+1 x).")
                .input("(a ==>+2 y).")
                .mustBelieve(cycles, "(x ==>+1 y)", 1, 0.45f)
                .mustBelieve(cycles, "(y ==>-1 x)", 1, 0.45f)
                .mustNotBelieve(cycles, "(x ==> y)")
                .mustNotBelieve(cycles, "(y ==> x)")
        ;
    }
    @Test
    void impl_induction_abduction_pred_1() {
        test.volMax(3)
            .input("(x ==>+2 a).")
            .input("(y ==>+1 a).")
            .mustBelieve(cycles, "(x ==>+1 y)", 1, 0.45f)
            .mustBelieve(cycles, "(y ==>-1 x)", 1, 0.45f)
            .mustNotBelieve(cycles, "(x ==> y)")
            .mustNotBelieve(cycles, "(y ==> x)")
        ;
    }

    @Test
    void impl_pred_conj_sequence1() {
        test
            .input("(x ==> (a &&+1 b)).")
            .input("(x ==> (c &&+1 d)).")
            .mustNotOutput(cycles, "(x ==>-4 ((a &&+1 b) &&+2 (c &&+1 d)))", BELIEF)
            .mustNotOutput(cycles, "(x==>((c &&+1 d) &&+1 (a &&+1 b)))", BELIEF)
            .mustBelieve(cycles, "(x ==>((a&&c) &&+1 (b&&d)))", 1, 0.81f)
            .mustBelieve(cycles, "((c &&+1 d) ==>-1 (a &&+1 b))", 1, 0.45f)
        ;
    }

    @Test
    void impl_pred_disj_sequence1() {
        test.volMax(12)
            .input("(x ==> (a &&+1 b)).")
            .input("(x ==> (c &&+1 d)).")
            .mustBelieve(cycles,
                    "(x==>((a&&c) &&+1 (b&&d)))"
                    //"(x==>((a &&+1 b)||(c &&+1 d)))"
                    , 1, 0.81f)
        ;
    }

    @Test
    void impl_pred_conj_sequence2a() {
        test.volMax(16).confMin(0.79f)
            .believe("(x ==>+1 (a &&+1 b))")
            .believe("(x ==>+2 (c &&+1 d))")
            .mustBelieve(cycles, "(x ==>+1 ((a &&+1 (b&&c)) &&+1 d))", 1, 0.81f)
            .mustNotBelieve(cycles, "( x==>+1 ((a &&+1 b) &&+1 (c &&+1 d)))")
        ;
    }

    @Test
    void impl_pred_conj_sequence2b() {
        test.volMax(16).confMin(0.79f)
            .believe("(x ==>+2 (a &&+1 b))")
            .believe("(x ==>+1 (c &&+1 d))")
            .mustBelieve(cycles, "(x ==>+1 ((c &&+1 (a&&d)) &&+1 b))", 1, 0.81f)
        ;
    }
    @Test
    void impl_pred_conj_sequence2aa() {
        test.volMax(16).confMin(0.79f)
                .believe("((x &&+1 e) ==>+1 (a &&+1 b))")
                .believe("((x &&+1 e) ==>+2 (c &&+1 d))")
                .mustBelieve(cycles, "((x &&+1 e) ==>+1 ((a &&+1 (b&&c)) &&+1 d))", 1, 0.81f)
                .mustNotBelieve(cycles, "((x &&+1 e) ==>+1 ((a &&+1 b) &&+1 (c &&+1 d)))")
        ;
    }
    @Test
    void impl_pred_conj_sequence2aaa() {
        test.volMax(16).confMin(0.79f)
                .believe("(x ==>+1 (a &&+1 b))")
                .believe("(x ==>-1 (c &&+1 d))")
                .mustBelieve(cycles, "(x ==>-1 (((c &&+1 d) &&+1 a) &&+1 b))", 1, 0.81f)
        ;
    }

    @Disabled
    @Test
    void impl_subj_conj_sequence2a() {
        test.volMax(16).confMin(0.79f)
                .believe("((a &&+1 b) ==>+2 x)")
                .believe("((c &&+1 d) ==>+1 x)")
                .mustBelieve(cycles, "(((a &&+1 (b&&c)) &&+1 d) ==>+1 x)", 1, 0.81f)
        ;
    }
}