package nars.nal.nal5;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.Op.ETERNAL;
import static nars.term.atom.Bool.False;
import static nars.term.util.Testing.assertEq;

class ConditionalEliminationTest extends AbstractNAL5Test {

    @Test void conditional_elimination_subj() {
        test.volMax(11);
        test.confMin(0.4f);
        test.believe("((&&,a,b) ==> c)", 0.9f, 0.9f);
        test.believe("((&&,a,d) ==> c)");
        test.mustBelieve(cycles, "(b ==> d)", 1.00f, 0.42f);
        test.mustBelieve(cycles, "(d ==> b)", 0.90f, 0.45f);
        //test.mustBelieve(cycles, "((a&&b) ==> d)", 1.00f, 0.42f);
        //test.mustBelieve(cycles, "((a&&d) ==> b)", 0.90f, 0.45f);
    }
    @Test void conditional_elimination_pred() {
        test.volMax(11);
        test.confMin(0.4f);
        test.believe("(c ==> (a&&b))", 0.9f, 0.9f);
        test.believe("(c ==> (a&&d))");
        test.mustBelieve(cycles, "(b ==> d)", 1.00f, 0.42f);
        test.mustBelieve(cycles, "(d ==> b)", 0.90f, 0.45f);
        test.mustBelieve(cycles, "((a&&b) ==> d)", 1.00f, 0.42f);
        test.mustBelieve(cycles, "((a&&d) ==> b)", 0.90f, 0.45f);
    }

    @Test
    void conditional_elimination2() {


        test.volMax(18);
        test.confMin(0.4f);
        test.believe("((&&,(robin --> [flying]),(robin --> [withWings])) ==> (robin --> [living]))", 0.9f, 0.9f);
        test.believe("((&&,(robin --> [flying]),a) ==> (robin --> [living]))");
        test.mustBelieve(cycles, "(a ==> (robin --> [withWings]))",
                1.00f, 0.42f);
        test.mustBelieve(cycles, "((robin --> [withWings]) ==> a)",
                0.90f, 0.42f /*0.45f*/);

    }



    @Test
    void conditional_elmimination4() {
        test.volMax(5).confMin(0.7f)
                .believe("((x && y) ==> z)")
                .believe("(a ==> y)", 0.9f, 0.9f)
                .mustBelieve(cycles, "((x && a) ==> z)", 1f, 0.73f);
    }


    @Disabled
    @Test
    void InhElimination_0() {
        test
                .volMax(9)
                .believe("((a&&b)-->(a&&c))")
                .mustBelieve(cycles, "(b-->c)", 1f, 0.81f);
    }

    @Test
    void ImplElimination_0() {
        test
                .volMax(9)
                .believe("((a&&b) ==>+1 (a&&c))")
//                .mustBelieve(cycles, "((a&&b) ==>+1 c)", 1f, 0.81f)
                .mustBelieve(cycles, "(b ==>+1 c)", 1f, 0.81f)
                .mustNotBelieve(cycles, "(b ==> c)")
                .mustNotBelieve(cycles, "((a&&b) ==> c)")
        ;
    }

    @Test
    void ImplElimination_1() {
        test
                .volMax(9)
                .believe("((a&&b)==>x)")
                .believe("((a&&c)==>x)")
                .mustBelieve(cycles, "(b==>c)", 1f, 0.45f)
                .mustBelieve(cycles, "(c==>b)", 1f, 0.45f)
        ;
    }

    @Test
    void ImplElimination_1_disj() {
        test
                .volMax(9)
                .believe("((a||b)==>x)")
                .believe("((a||c)==>x)")
                .mustBelieve(cycles, "(b==>c)", 1f, 0.45f)
                .mustBelieve(cycles, "(c==>b)", 1f, 0.45f)
        ;
    }

    @Test
    void conditional_abduction3_semigeneric3() {


        test.volMax(14);
        test.confMin(0.4f);
        test.believe("((&&,(R --> [f]),(R --> [w])) ==> (R --> [l]))", 0.9f, 0.9f);
        test.believe("((&&,(R --> [f]),(R --> b)) ==> (R --> [l]))");
        test.mustBelieve(cycles, "((R --> b) ==> (R --> [w]))", 1f, 0.42f /*0.36f*/);
        test.mustBelieve(cycles, "((R --> [w]) ==> (R --> b))", 0.90f, 0.45f);
    }

    @Test
    void conditional_abduction2_viaMultiConditionalSyllogism() {

        test
                .volMax(15).confMin(0.4f)
                .believe("<(&&,(robin --> [withWings]),(robin --> [chirping])) ==> a>")
                .believe("<(&&,(robin --> [flying]),(robin --> [withWings]),(robin --> [chirping])) ==> a>")
                .mustBelieve(cycles, "(robin --> [flying])",
                        1.00f, 0.45f
                )
                .mustNotOutput(cycles, "(robin --> [flying])", BELIEF, 0f, 0.5f, 0, 1, ETERNAL);
    }

    @Test
    void conditional_abduction3_generic() {

        test.volMax(14);
        test.confMin(0.35f);
        test.believe("((&&,(R --> [f]),(R --> [w])) ==> (R --> [l]))", 0.9f, 0.9f);
        test.believe("((&&,(R --> [f]),(R --> b)) ==> (R --> [l]))");
        test.mustBelieve(cycles, "((R --> b) ==> (R --> [w]))", 1f, 0.42f);
        test.mustBelieve(cycles, "((R --> [w]) ==> (R --> b))", 0.90f, 0.45f);
    }


    @Test
    void conditional_deduction3() {
        test.volMax(11);
        test.confMin(0.8f);
        test.believe("<(&&,a,(robin --> [living])) ==> b>");
        test.believe("<(robin --> [flying]) ==> a>");
        test.mustBelieve(cycles, " <(&&,(robin --> [flying]),(robin --> [living])) ==> b>",
                1.00f, 0.81f);

    }

    @Test
    void impl_decompose_subcond_disj_1() {
        assertEq(False, "(a ==> false)");

        test.volMax(8).confMin(0.03f);
        test.believe("(a ==> (x || y))", 0.9f, 0.9f);
        test.believe("(a ==> x)", 0.1f, 0.9f);
        test.mustNotBelieve(cycles, "a");
        test.mustBelieve(cycles, "(a ==> y)", 1.00f, 0.81f);
//        test.mustBelieve(cycles, "(a ==> (x && y))", 0.1f, 0.81f / 9f);
    }

}