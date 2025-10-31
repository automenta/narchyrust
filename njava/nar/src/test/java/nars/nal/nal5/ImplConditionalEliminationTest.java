package nars.nal.nal5;

import org.junit.jupiter.api.Test;

class ImplConditionalEliminationTest extends AbstractNAL5Test {

    private static final int cycles = 50;

    @Test
    void impl_subj_conj_elim_p() {
        test
            .volMax(6).confMin(0.75f)
            .believe("((a && b) ==> x)")
            .believe("(  --b ==> x)")
            .mustBelieve(cycles, "(a ==> x)", 1, 0.81f);
    }

    @Test
    void impl_subj_conj_elim_n() {
        test
            .volMax(6).confMin(0.75f)
            .believe("((a && b) ==> --x)")
            .believe("(  --b ==> --x)")
            .mustBelieve(cycles, "(a ==> x)", 0, 0.81f);
    }

    @Test
    void impl_subj_disj_elim_p() {
        test
            .volMax(8).confMin(0.75f)
            .believe("((a || b) ==> x)")
            .believe("(  --b ==> x)")
            .mustBelieve(cycles, "(a ==> x)", 1, 0.81f);
    }


    @Test
    void compound_decomposition_two_premises1() {

        test.volMax(12)
        .believe("(bird:robin ==> --(animal:robin && (robin-->[flying])))", 1.0f, 0.9f)
        .believe("          (bird:robin ==> (robin-->[flying]))")
        .mustBelieve(cycles, "(bird:robin ==> animal:robin)", 0.00f, 0.81f);

    }

    @Test
    void compound_decomposition_two_premises1_simpler() {

        test
                .volMax(6)
                .confMin(0.75f)
                .believe("(b ==> --(a && r))", 1.0f, 0.9f)
                .believe("          (b ==> r)")
                .mustBelieve(cycles, "(b ==> a)", 0.00f, 0.81f);

    }
}