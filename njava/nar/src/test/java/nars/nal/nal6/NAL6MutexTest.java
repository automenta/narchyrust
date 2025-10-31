package nars.nal.nal6;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;

@Disabled class NAL6MutexTest extends AbstractNAL6Test {

	private static final int cycles = 50;

	@Test
	void preDuctionMutex() {
		test
				.volMax(6)
				.believe("(x ==>   z)")
				.believe("(y ==> --z)")
				.mustBelieve(cycles, "--(x&&y)", 1f, 0.81f)
		;
	}
	@Test
	void postDuctionMutex() {
		test
				.volMax(6)
				.believe("(  z ==> x)")
				.believe("(--z ==> y)")
				.mustBelieve(cycles, "--(x&&y)", 1f, 0.45f)
		;
	}
	@Disabled
	@Test
	void testMutexAbduction() {
		test
			.volMax(5)
			.believe("(--(x && y) ==> z)")
			.believe("(x && z)")
			.mustBelieve(cycles, "y", 0f, 0.45f)
			.mustNotOutput(cycles, "y", BELIEF, 0.1f, 1f, 0, 1)
		;
	}

	@Disabled @Test
	void testMutexAbductionNeg() {
		test
			.volMax(5)
			.believe("(--(x && y) ==> --z)")
			.believe("(x && --z)")
			.mustBelieve(cycles, "y", 0f, 0.45f)
			.mustNotOutput(cycles, "y", BELIEF, 0.1f, 1, 0, 1)
		;
	}
	@Test
	void testMutexPrecondition_Eternal() {
		test
				.volMax(8)
				.believe("((&&,a,--b,c) ==> z)")
				.believe("((&&,--a,b,c) ==> z)")
				.mustBelieve(cycles, "((&&,--(a && b),c) ==> z)", 1f, 0.81f)
		;
	}
	@Test
	void testMutexPrecondition_Seq() {
		test
				.volMax(8)
				.believe("(((&&,a,--b) &&+1 c) ==> z)")
				.believe("(((&&,--a,b) &&+1 c) ==> z)")
				.mustBelieve(cycles, "((--(a && b) &&+1 c) ==> z)", 1f, 0.81f)
		;
	}

	@Test
	void testMutexPrecondition2() {
		test
				.volMax(10)
				.believe("((&&,a,--b,c,d) ==> z)")
				.believe("((&&,--a,b,c,d) ==> z)")
				.mustBelieve(cycles, "((&&,--(a && b),c,d) ==> z)", 1f, 0.81f)
		;
	}

	@Test
	void testConjunctionContradictionInduction() {

		test
			.believe("((x && y) ==> z)")
			.believe("((x && --y) ==> z)")
			.mustBelieve(cycles, "(x ==> z)", 1.00f,
				0.81f);
		//0.45f);

	}

	@Test
	void testMutexBelief() {
		test
			.confMin(0.7f)
			.volMax(3)
			.believe("--(x && y)")
			.believe("x")
			.mustBelieve(cycles, "y", 0f, 0.81f)
//                .mustNotOutput(cycles, "x", BELIEF, 0f, 0.5f, 0, 1f, ETERNAL)
//                .mustNotOutput(cycles, "y", BELIEF, 0.5f, 1f, 0, 1f, ETERNAL)
		;
	}

	@Test
	void testMutexConjBeliefInduction() {
		test
			.volMax(6)
			.confMin(0.4f)
			.believe("(x && --y)")
			.believe("(--x && y)")
			.mustBelieve(cycles, "(x && y)", 0f, 0.81f)
			//.mustBelieve(cycles, "(--x && --y)", 0f, 0.81f)
//			.mustBelieve(cycles, "(x ==> y)", 0f, 0.45f)
//			.mustBelieve(cycles, "(y ==> x)", 0f, 0.45f)
		;
	}
	@Test
	void testMutexInhSectBeliefInduction() {
		test
				.volMax(9)
				.confMin(0.4f)
				.believe("(a --> (x && --y))")
				.believe("(a --> (--x && y))")
				.mustBelieve(cycles, "(a --> (x && y))", 0f, 0.81f)
				//.mustBelieve(cycles, "(a --> (--x && --y))", 0f, 0.81f)
		;
	}

	@Test
	void testMutexConjBeliefInduction2() {
		test
			.volMax(8)
			.believe("(&&,x,--y,a)")
			.believe("(&&,--x,y,a)")
			.mustBelieve(cycles, "(--(x && y) && a)", 1f, 0.81f)
		;
	}
//    @Test void MutexConjImplBeliefInduction() {
//        test.nar.termVolumeMax.setAt(12);
//        test
//                .believe("((x && --y) ==> z)")
//                .believe("((--x && y) ==> z)")
//                .mustBelieve(cycles, "(--(x && y) ==> z)", 1f, 0.81f)
//        ;
//    }


//        @Test
//        void testMutexDiffGoal1NegNAary() {
//            test
//                    .input("--((&,a,b,--c)-->g)!")
//                    .input("((a&b)-->g).")
//                    .mustGoal(cycles, "(c-->g)", 1f, 0.81f);
//        }

	@Test
	void mutexConj() {
		test.volMax(11).confMin(0.7f)
			.believe("(&&, x, --y, a)")
			.believe("(&&, --x, y, b)")
			.mustBelieve(cycles, "(--(x && y) && (a || b))", 1f, 0.81f);
	}
	@Test
	void mutexConj2() {
		test.volMax(13).confMin(0.7f)
				.believe("(&&, x, --y, (a))")
				.believe("(&&, --x, y, {b})")
				.mustBelieve(cycles, "(--(x && y) && ((a) || {b}))", 1f, 0.81f);
	}
	@Test
	void mutexConj3() {
		test.volMax(17).confMin(0.7f)
				.believe("(&&, x, --y, (a), w)")
				.believe("(&&, --x, y, {b}, w)")
				.mustBelieve(cycles, "(--(x && y) && (((a)&&w) || ({b}&&w)))", 1f, 0.81f);
	}
	@Test
	void mutexConj4() {
		test.volMax(17).confMin(0.7f)
				.believe("(&&, x, --y, a, b)")
				.believe("(&&, --x, y, c, d)")
				.mustBelieve(cycles, "(--(x && y) && ((a&&b) || (c&&d)))", 1f, 0.81f);
	}
	@Disabled @Test
	void mutexConj5() {
		test.volMax(17).confMin(0.7f)
				.believe("(&&, --x, --y, a, b)")
				.believe("(&&,   x,   y, c, d)")
				.mustBelieve(cycles, "( (x<=>y) && ((a&&b) || (c&&d)))", 1f, 0.81f);
	}


	//    @Test void MutexSwapPos() {
//        test.nar.termVolumeMax.setAt(14);
//        test
//                .believe("--(x && y)")
//                .believe("its(x,a)")
//                .mustBelieve(cycles, "(its(x,a)<->its(--y,a))", 1f, 0.45f)
//                //.mustBelieve(cycles, "its(--y,a)", 1f, 0.81f)
//        ;
//    }
//
//    @Test void MutexSwapNeg() {
//        test
//                .believe("--(x && y)")
//                .believe("its(--x,a)")
//                .mustBelieve(cycles, "(its(--x,a)<->its(y,a))", 1f, 0.45f)
//                //.mustBelieve(cycles, "its(y,a)", 1f, 0.81f)
//        ;
//    }

}
