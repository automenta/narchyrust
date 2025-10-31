package nars.nal.nal5;

import org.junit.jupiter.api.Test;

class NAL5SpecificDeductionTest extends AbstractNAL5Test {

    private static final int cycles = 50;

    @Test
    void specific_deduction() {
        test
                .confMin(0.75f)
                .volMax(14)
                .believe("<(&&,(robin --> [flying]),(robin --> [withWings])) ==> a>")
                .believe("(robin --> [flying])")
                .mustBelieve(cycles, " <(robin --> [withWings]) ==> a>", 1.00f, 0.81f);
    }

    @Test
    void specific_deduction2() {
        test
                .volMax(15)
                .confMin(0.8f)
                .believe("<(&&,(robin --> [chirping]),(robin --> [flying]),(robin --> [withWings])) ==> a>")
                .believe("(robin --> [flying])")
                .mustBelieve(cycles, " <(&&,(robin --> [chirping]),(robin --> [withWings])) ==> a>", 1.00f, 0.81f);
    }

    @Test
    void specific_deduction_neg() {


        test.volMax(12);
        test.believe("((--(robin-->[swimming]) && (robin --> [withWings])) ==> a)");
        test.believe("--(robin-->[swimming])");
        test.mustBelieve(cycles, "((robin --> [withWings]) ==> a)", 1.00f, 0.81f);

    }


    @Test
    void specific_conj_subj_deduction_pos_simple() {
        test.believe("((x && y) ==> a)");
        test.believe("x");
        test.mustBelieve(cycles, "(y ==> a)", 1.00f, 0.81f);
//        test.mustBelieve(cycles, "(y && a)", 1.00f, 0.81f);
    }

    @Test
    void specific_deduction_neg_simple() {
        test.volMax(6);
        test.believe("((--x && y) ==> a)");
        test.believe("--x");
        test.mustBelieve(cycles, "(y ==> a)", 1.00f, 0.81f);
    }


    @Test
    void specific_conj_deduction_unify_simple() {
        test.volMax(12);
        test.believe("((x($1) && y) ==> a($1))");
        test.believe("x(b)");
        test.mustBelieve(cycles, "(y ==> a(b))", 1.00f, 0.81f);
    }

    @Test
    void specific_disj_deduction_subj_simple() {
        test.volMax(8);
        test.believe("((x || y) ==> a)");
        test.believe("--x");
        test.mustBelieve(cycles, "(y ==> a)", 1.00f, 0.81f);
    }

    @Test
    void specific_disj_deduction_pred_simple() {
        test.volMax(8);
        test.believe("(a ==> (x || y))");
        test.believe("--x");
        test.mustBelieve(cycles, "(a ==> y)", 1.00f, 0.81f);
    }


    @Test
    void specific_conj_deduction_unification_simpler() {

        test
                .volMax(12)
                .confMin(0.75f)
                .believe("(((#x --> flying) && (#x --> withWings)) ==> a)")
                .believe("(robin --> flying)")
                .mustBelieve(cycles, " ((robin-->withWings)==>a)", 1.00f, 0.81f)
        //.mustBelieve(cycles, " (((robin-->flying)&&(robin-->withWings))==>a)", 1.00f, 0.81f)
        ;

    }

    @Test
    void specific_disj_deduction_unification_simpler() {

        test
                .volMax(12)
                .confMin(0.8f)
                .believe("(((#x --> flying) || (#x --> withWings)) ==> a)")
                .believe("--(robin --> flying)")
                .mustBelieve(cycles, " ((robin-->withWings)==>a)", 1.00f, 0.81f)
        //.mustBelieve(cycles, " (((robin-->flying)&&(robin-->withWings))==>a)", 1.00f, 0.81f)
        ;

    }

    @Test
    void specific_conj_deduction_unification() {

        test
                .volMax(14)
                .confMin(0.8f)
                .believe("(((#x --> [flying]) && (#x --> [withWings])) ==> a)")
                .believe("(robin --> [flying])")
                .mustBelieve(cycles, " ((robin-->[withWings])==>a)", 1.00f, 0.81f)
        //.mustBelieve(cycles, " (((robin-->[flying])&&(robin-->[withWings]))==>a)", 1.00f, 0.81f)
        ;

    }

}