package nars.nal.nal5;

import nars.NAR;
import nars.NARS;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * '********** contraposition
 * <p>
 * 'It is unlikely that if robin is not a type of bird then robin can fly.
 * (--(robin --> bird) ==> --(robin --> [flying])). %0.9%
 * <p>
 * 'If robin cannot fly then is robin a type of bird?
 * (--(robin --> [flying]) ==> (robin --> bird))?
 * <p>
 * 'I guess it is unlikely that if robin cannot fly then robin is a type of bird.
 * (--(robin --> [flying]) ==> --(robin --> bird)). %1.00;0.45%
 * <p>
 * <p>
 * P ==> Q   |-   --Q ==> --P
 */
class ConversionAndContrapositionTest extends AbstractNAL5Test {

    private static final int cycles = 64;

    @Override
    protected NAR nar() {
        NARS nn = new NARS.DefaultNAR(0, 0, false);
        nn.then("deriver", N-> nn.deriver(NARS.Rules.nal(1, 6)
            .core().stm().temporalInduction()
            .files("conversion.nal", "contraposition.nal")
        , N).everyCycle(N.main()));
        NAR n = nn.get();
        n.complexMax.set(10);
        return n;
    }

    @Test
    void testConversion() {

        test
                .volMax(3)
                .input("(x==>y)?")
                .input("(y==>x).")
                .mustBelieve(cycles, "(x==>y)", 1.0f, 0.47f)
        ;
    }

    @Test
    void testConversionNeg() {

        test
                .volMax(4)
                .confMin(0.45f)
                .input("(x ==> y)?")
                .input("(--y ==> x).")
                .mustBelieve(cycles, "(x ==> y)", 0.0f, 0.47f)
        ;
    }

    @Test
    void contrapositionPos() {
        test.volMax(4)
                .believe("(A ==> B)", 0.9f, 0.9f)
                .question("(--B ==> A)")
                .mustBelieve(cycles, "((--,B)==>A)", 0f, 0.45f);
    }


    @Test
    void contrapositionNeg() {
        test.volMax(4)
                .question("(--B ==> A)")
            .believe("(A ==> B)", 0.1f, 0.9f)
            .mustBelieve(cycles, " (--B ==> A)",
                    1, 0.45f)
//            .mustNotOutput(cycles, "(--B ==> A)", BELIEF,
//                    0, 0.5f, 0, 1)
        ;
    }

    @Test
    void contraposition_pos_belief() {
        test
                .volMax(5).confMin(0.43f)
                .input("(y ==> x).")
                .input("(--x ==> y)?")
                .mustBelieve(cycles, "(--x ==> --y)", 1, 0.47f)
        ;
    }

    @Test
    void contraposition_pos_belief_dt() {
        test
                .volMax(5).confMin(0.43f)
                .input("(y ==>+10 x).")
                .input("(--x ==>+- y)?")
                .mustBelieve(cycles, "(--x ==>-10 --y)", 1f, 0.47f)
        ;
    }

    @Test
    void contraposition_neg_belief() {
        test
                .volMax(4)
                .input("(--x ==> y)?")
                .input("(y ==> x). %0.1%")
                .mustBelieve(cycles, "(--x ==> y)", 0f, 0.08f)
                .mustBelieve(cycles, "(--x ==> y)", 1f, 0.45f)
        ;
    }

    @Nested
    @Disabled
    public class ImplDisjTest extends AbstractNAL5Test {
        @Test
        void impl_to_disj() {
            test
                    .volMax(6)
                    .input("(x ==> y)?")
                    .input("(--x || y).")
                    .mustBelieve(cycles, "(x ==> y)", 1f, 0.81f)
            ;
        }

        @Test
        void impl_to_disj_neg_subj() {
            test
                    .volMax(6)
                    .input("(--x ==> y)?")
                    .input("(x || y).")
                    .mustBelieve(cycles, "(--x ==> y)", 1f, 0.81f)
            ;
        }

        @Test
        void impl_pos_to_disj() {
            test
                    .volMax(6)
                    .input("(--x || y)?")
                    .input("(x ==> y).")
                    .mustBelieve(cycles, "(--x || y)", 1f, 0.81f)
            ;
        }

        @Test
        void impl_neg_to_disj() {
            test
                    .volMax(6)
                    .input("(--x || --y)?")
                    .input("(x ==> --y).")
                    .mustBelieve(cycles, "(--x || --y)", 1f, 0.81f)
            ;
        }
    }
//	@Test
//	void contraposition_pos_belief_dt_conj_subj() {
//		test
//				.volMax(6).confMin(0.45f)
//				.input("(--(x &&+- z) ==>+- y)?")
//				.input("(y ==>+1 --(x &&+1 z)).")
//				.mustBelieve(cycles, "(--(x &&+1 z) ==>-2 y)", 1f, 0.47f)
//		;
//	}
//	@Test
//	void contraposition_pos_belief_dt_conj_pred() {
//		test
//				.volMax(6).confMin(0.45f)
//				.input("(--x ==>+- (y &&+- z))?")
//				.input("((y &&+1 z) ==>+1 --x).")
//				.mustBelieve(cycles, "(--x ==>-2 (y &&+1 z))", 1f, 0.47f)
//		;
//	}
//	@Test
//	void contraposition_pos_belief_dt_conj_subj_pred() {
//		test
//				.volMax(8).confMin(0.45f)
//				.input("(--(x &&+- w) ==>+- (y &&+- z))?")
//				.input("((y &&+1 z) ==>+2 --(x &&+5 w)).")
//				.mustBelieve(cycles, "(--(x &&+5 w) ==>-8 (y &&+1 z))", 1f, 0.47f)
//		;
//	}
//	@Test
//	void contraposition_pos_belief_dt_conj_subj_pred2() {
//		test
//				.volMax(8).confMin(0.45f)
//				.input("(--(x &&+- w) ==>+- (y &&+- z))?")
//				.input("((y &&+1 z) ==>-2 --(x &&+5 w)).")
//				.mustBelieve(cycles, "(--(x &&+5 w) ==>-4 (y &&+1 z))", 1f, 0.47f)
//		;
//	}
//
//	/** tests occurrence shifting */
//	@Test void contraposition_pos_belief_dt_temporal() {
//		test
//			.volMax(4)
//			.input("(--x ==> y)?")
//			.input("(y ==>+2 --x). |")
//			.mustBelieve(cycles, "(--x ==>-2 y)", 1f, 0.47f, 2)
//		;
//	}
//	/** tests occurrence shifting */
//	@Test void contraposition_pos_belief_dt_temporal_auto() {
//		test
//				.volMax(4)
//				.confTolerance(0.2f)
//				.input("(y ==>+2 --x). |")
//				.mustBelieve(cycles, "(--x ==>-2 y)", 1f, 0.47f, 2)
//		;
//	}
//	/** tests occurrence shifting */
//	@Test void contraposition_pos_belief_dt_temporal_specific() {
//		test
//				.volMax(4)
//				.confTolerance(0.2f)
//				.input("(--x ==>+2 y)? |")
//				.input("(y ==>+2 --x). |")
//				.mustBelieve(cycles, "(--x ==>-2 y)", 1f, 0.47f, 2)
//		;
//	}
//
//	/** tests occurrence shifting */
//	@Test void contraposition_pos_belief_dt_temporal_subj_conj() {
//		test
//				.volMax(7)
//				.confTolerance(0.2f)
//				.input("(--x ==> (y &&+1 z))?")
//				.input("((y &&+1 z) ==>+2 --x). |")
//				.mustBelieve(cycles, "(--x ==>-3 (y &&+1 z))", 1f, 0.47f, 3)
//		;
//	}
//	/** tests occurrence shifting */
//	@Test void contraposition_pos_belief_dt_temporal_subj_pred_conj() {
//		test
//				.volMax(8)
//				.confTolerance(0.2f)
//				.input("(--(x &&+5 w) ==> (y &&+1 z))?")
//				.input("((y &&+1 z) ==>+2 --(x &&+5 w)). |")
//				.mustBelieve(cycles, "(--(x &&+5 w) ==>-8 (y &&+1 z))", 1f, 0.47f, 3)
//		;
//	}
////	@Test
////	void contraposition() {
////		test.volMax(10);
////		test.believe("(--(robin --> bird) ==> (robin --> [flying]))", 0.1f, 0.9f);
////		test.mustBelieve(cycles, " (--(robin --> [flying]) ==> (robin --> bird))",
////			0.1f, 0.42f /*0.36f*/);
////		//0f, 0.45f);
////	}


}