package nars.nal.nal5;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.Op.ETERNAL;

@Disabled class ConditionalAbductionTest extends AbstractNAL5Test {

    private static final int cycles = 100;

    @Test
    void conditional_abduction2() {
        test.volMax(6).confMin(0.43f)
                .believe("((x && y) ==> z)")
                .believe("(y ==> z)")
                .mustBelieve(cycles, "x", 1.00f, 0.45f);
    }

    @Test
    void abduction_without_variable_elimination() {

        test
                .volMax(13).confMin(0.43f)
                .believe("(open(x,lock1) ==> (x --> key))", 1.00f, 0.90f)
                .believe("(((lock1 --> lock) && open(x,lock1)) ==> (x --> key))", 1.00f, 0.90f)
                .mustBelieve(cycles, "lock:lock1", 1.00f, 0.45f)
        ;
    }

    @Test
    void abduction_neg_without_variable_elimination() {

        test
                .volMax(14)
                .confMin(0.4f)
                .believe("(open(x,lock1) ==> (x --> key))", 1.00f, 0.90f)
                .believe("((--(lock1 --> lock) && open(x,lock1)) ==> (x --> key))", 1.00f, 0.90f)
                .mustBelieve(cycles, "lock:lock1", 0.00f, 0.45f)
        ;
    }

    @Disabled
    @Test
    void conditional_abduction2_depvar_2() {
        test.volMax(6).confMin(0.43f)
                .believe("((x && y) ==> #1)")
                .believe("(y ==> #1)")
                .mustBelieve(cycles, "x", 1.00f, 0.45f);
    }

    @Disabled
    @Test
    void conditional_abduction2_depvar() {
        test.confMin(0.43f);
        test.volMax(5);
        test
                .believe("((x && y) ==> z)")
                .believe("(y ==> #1)")
                .mustBelieve(cycles, "x", 1.00f, 0.45f);
    }

    @Disabled
    @Test
    void conditional_antiAbduction_viaMultiConditionalSyllogism_simple_a() {
        test.volMax(6)
                .confMin(0.43f)
                .believe("(x ==> --y)")
                .believe("((z && x) ==> y)")
                .mustBelieve(cycles, "z", 0.00f, 0.45f);
    }

    @Disabled
    @Test
    void conditional_antiAbduction_viaMultiConditionalSyllogism_simple_b() {
        test.volMax(6).confMin(0.43f)
                .believe("(x ==> y)")
                .believe("((z && x) ==> --y)")
                .mustBelieve(cycles, "z", 0.00f, 0.45f);
    }

    @Test
    void conditional_abduction_viaMultiConditionalSyllogism_simple() {
        test.volMax(5).confMin(0.43f)
                .believe("(x ==> y)")
                .believe("((z && x) ==> y)")
                .mustBelieve(cycles, "z", 1.00f, 0.45f);
    }

    @Test
    void conditional_abduction_viaMultiConditionalSyllogismSimple_NegPredicate() {
        test.volMax(6).confMin(0.43f)
                .believe("(x ==> --y)")
                .believe("((z && x) ==> --y)")
                .mustBelieve(cycles, "z", 1.00f, 0.45f);
    }

    @Test
    void conditional_abduction_viaMultiConditionalSyllogismSimple_Predicate_Polarity_Mismatch() {
        test.volMax(6).confMin(0.43f)
                .believe("(x ==> y)")
                .believe("((z && x) ==> --y)")
                .mustNotBelieve(cycles, "z", 1.00f, 0.45f, (s, e) -> true);
    }

    @Test
    void conditional_abduction_viaMultiConditionalSyllogismSimple_NegSubCondition_The() {
        test
                .volMax(6).confMin(0.43f)
                .believe("(x ==> y)")
                .believe("((--z && x) ==> y)")
                .mustBelieve(cycles, "--z", 1.00f, 0.45f);
    }

    @Test
    void conditional_abduction_viaMultiConditionalSyllogismSimple_NegSubCondition_Other() {
        test.volMax(6).confMin(0.43f)
                .believe("(--x ==> y)")
                .believe("((z && --x) ==> y)")
                .mustBelieve(cycles, "z", 1.00f, 0.45f);
    }

    @Test
    void conditional_abduction_viaMultiConditionalSyllogismSimple2() {

        test
                .volMax(7).confMin(0.43f)
                .believe("((&&,x1,x2) ==> y)")
                .believe("((&&,x1,x2,z) ==> y)")
                .mustBelieve(cycles, "z", 1.00f, 0.45f);

    }

    @Test
    void conditional_abduction_viaMultiConditionalSyllogism() {

        test
                .volMax(11).confMin(0.43f)
                .believe("(flying:robin ==> bird:robin)")
                .believe("((swimmer:robin && flying:robin) ==> bird:robin)")
                .mustBelieve(cycles, "swimmer:robin", 1.00f, 0.45f);

    }

    @Test
    void conditional_abduction2_viaMultiConditionalSyllogism_simpler() {

        test
                .volMax(6).confMin(0.43f)
                .believe("((&&,robinWings,robinChirps) ==> a)")
                .believe("((&&,robinFlies,robinWings,robinChirps) ==> a)")
                .mustBelieve(cycles, "robinFlies",
                        1.00f, 0.45f
                )
                .mustNotOutput(cycles, "robinFlies", BELIEF, 0f, 0.5f, 0, 1, ETERNAL);
    }


}