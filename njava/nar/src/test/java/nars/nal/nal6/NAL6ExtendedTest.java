package nars.nal.nal6;

import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.Op.ETERNAL;

/** these usually need more cycles, involving second-layer unisubst etc */
public class NAL6ExtendedTest extends AbstractNAL6Test {

	static int cycles = 100;





	@Test
	void strong_elimination() {

		test.volMax(18);
		test.confMin(0.7f);
		test.believe("((test($a,is,cat) && sentence($a,is,$b)) ==> ($a --> $b))");
		test.believe("test(tim,is,cat)");
		test.mustBelieve(cycles, "((test(tim,is,cat) && sentence(tim,is,$1)) ==> (tim --> $1))", 1.00f, 0.81f);

	}
	@Test
	void variable_elimination6() {
		test
			.volMax(17)
			.confMin(0.6f)
			.believe("flyer:Tweety")
			.believe("((&&, flyer:$x, ($x-->[chirping]), food($x, worms)) ==> bird:$x)")
			.mustBelieve(cycles, "((&&,flyer:Tweety,(Tweety-->[chirping]),food(Tweety,worms)) ==> bird:Tweety)",
				1.0f,
				0.81f);
	}



	@Test
	void variable_elimination6simpler() {

		test
			.volMax(14).confMin(0.8f)
			.believe("((&&, flyer:$x, chirping:$x, food:worms) ==> bird:$x)")
			.believe("flyer:Tweety")
			.mustBelieve(cycles, "((&&, flyer:Tweety, chirping:Tweety, food:worms) ==> bird:Tweety)",
				1.0f,
				0.81f);


	}

	@Test
	void variable_elimination6simplerReverse() {

		test.volMax(20)
			.believe("(bird:$x ==> (&&, flyer:$x, chirping:$x, food:worms))")
			.believe("flyer:Tweety")
			.mustBelieve(cycles, "(bird:Tweety ==> (&&, flyer:Tweety, chirping:Tweety, food:worms))", 1.0f, 0.81f);


	}

	@Test
	void variable_unification3() {


		test.confMin(0.3f);
		test.believe("<<$x --> swan> ==> ($x --> bird)>", 1.00f, 0.80f);
		test.believe("<<$y --> swan> ==> <$y --> swimmer>>", 0.80f, 0.9f);
		test.mustBelieve(cycles, "<<$1 --> swan> ==> (&&,($1 --> bird),($1 --> swimmer))>", 0.80f, 0.72f);
		test.mustBelieve(cycles, "<($1 --> swimmer) ==> ($1 --> bird)>", 1f, 0.37f);
		test.mustBelieve(cycles, "<($1 --> bird) ==> ($1 --> swimmer)>", 0.80f, 0.42f);


	}


	@Test
	void variable_elimination_impl_fwd_pos_pos() {

		test
			.believe("<($x --> bird) ==> <$x --> animal>>")
			.believe("<robin --> bird>")
			.mustBelieve(cycles, "<robin --> animal>", 1.00f, 0.81f);

	}

	@Test
	void variable_elimination_impl_fwd_pos_neg() {

		test
			.believe("(($x --> bird) ==> --($x --> --animal))")
			.believe("(robin --> bird)")
			.mustBelieve(cycles, "(robin --> --animal)", 0.00f, 0.81f);

	}

	@Test
	void variable_elimination_impl_fwd_neg_pos() {

		test
			.believe("(--($x --> --bird) ==> ($x --> animal))")
			.believe("--(robin --> --bird)")
			.mustBelieve(cycles, "(robin --> animal)", 1.00f, 0.81f);

	}

	@Test
	void variable_elimination_impl_rev() {

		test.believe("<($x --> bird) ==> <$x --> animal>>");
		test.believe("<tiger --> animal>");
		test.mustBelieve(cycles * 2, "<tiger --> bird>", 1.00f, 0.45f);

	}
	@Test
	void variable_elimination_impl_pred_conj() {

		test.volMax(13);
		test.believe("(accessibleFromMenu($1, $2) ==> (($1-->Entity) && ($2-->ComputerMenu)))");
		test.believe("(GraphicalComputerMenu-->ComputerMenu)");
		test.mustBelieve(cycles, "(accessibleFromMenu($1, GraphicalComputerMenu) ==> (($1-->Entity) && (GraphicalComputerMenu-->ComputerMenu)))", 1f, 0.81f);
	}

	@Test
	void variable_elimination_conj() {
		test
			.volMax(7).confMin(0.42f)
			.believe("((#x --> bird) && (#x --> swimmer))")
			.believe("(swan --> bird)", 0.90f, 0.9f)
			.mustBelieve(cycles, "(swan --> swimmer)", 0.90f, 0.43f);
	}



	@Test
	void variable_elimination5() {


		test
			.volMax(16)
			.believe("({Tweety} --> [withWings])")
			.believe("((($x --> [chirping]) && ($x --> [withWings])) ==> ($x --> bird))")
			.mustBelieve(cycles,
				"((({Tweety}-->[chirping])&&({Tweety}-->[withWings]))==>({Tweety}-->bird))",
				1.00f,
				0.81f
			);
	}


	@Test
	void variable_elimination5_neg() {
		test.confMin(0.6f);
		test.volMax(16);
		test.believe("({Tweety} --> [withWings])");
		test.believe("((($x --> [chirping]) && ($x --> [withWings])) ==> --($x --> nonBird))");
		test.mustBelieve(cycles,
			"((({Tweety}-->[chirping])&&({Tweety}-->[withWings]))==>({Tweety}-->nonBird))",
			//"({Tweety}-->[chirping])==>({Tweety}-->nonBird))",
			0.00f,
			//0.81f
			0.81f
		);

	}



	@Disabled
	@Test
	void multiple_variable_elimination3() {

		test.believe("((#x --> lock) && (key:$y ==> open($y,#x)))");
		test.believe("({lock1} --> lock)");
		test.mustBelieve(cycles, "(($1 --> key) ==> open($1,{lock1}))", 1.00f, 0.81f);

	}


	@Test
	void multiple_variable_elimination4() {
		test.volMax(16)
			.believe("(&&,open(#y,#x),(#x --> lock),(#y --> key))")
			.believe("({lock1} --> lock)")
			.mustBelieve(cycles, "(&&,({lock1} --> lock),(#1-->key),open(#1,{lock1}))", 1.00f,
				0.45f
				//0.81f
				//0.66f
				//0.43f
			);
	}



	@Test
	void variable_unification5() {

		test.volMax(14);
		test.confMin(0.75f);
		test.believe("<(&&,($x --> flyer),($x --> [chirping])) ==> ($x --> bird)>");
		test.believe("<($y --> [withWings]) ==> ($y --> flyer)>");
		test.mustBelieve(cycles, "((($1 --> [chirping]) && ($1 --> [withWings])) ==> ($1 --> bird))",
			1.00f,
			0.81f
		);

	}

	@Test
	void variable_unification5_neg() {


		test.volMax(16);
		test.believe("<(&&,($x --> flyer),($x --> [chirping])) ==> --($x --> nonBird)>");
		test.believe("<($y --> [withWings]) ==> ($y --> flyer)>");
		test.mustBelieve(cycles,
				"((($1 --> [chirping]) && ($1 --> [withWings])) ==> --($1 --> nonBird))",
			1.00f, 0.81f);

	}
	@Test
	void conditional_implication_disj_neg() {
		test.volMax(16);
		test.believe("(((a --> flyer)||(x --> [chirping])) ==> --(x --> nonBird))");
		test.believe("((y --> [withWings]) ==> (a --> flyer))");
		test.mustBelieve(cycles,
				"((y --> [withWings]) ==> --(x --> nonBird))",
				1.00f,
				0.81f
		);

	}
	@Test
	void variable_elimination_deduction() {

		test
			.confMin(0.75f)
			.volMax(17)
			.believe("((&&,(#1 --> lock),open($2,#1)) ==> ($2 --> key))")
			.believe("(lock1 --> lock)")
			.mustBelieve(cycles, "((open($1,lock1)&&(lock1-->lock))==>($1-->key))", 1.00f, 0.81f)
			//.mustBelieve(cycles, "(open($1,lock1)==>($1-->key))", 1.00f, 0.81f)
		;

	}
	@Test
	void variable_elimination_deduction_neg_cond() {

		test
			.volMax(14).confMin(0.8f)
			.believe("((&&,--(#1 --> lock),open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f)
			.believe("(lock1 --> lock)", 0.00f, 0.90f)
			.mustBelieve(cycles, "((open($1,lock1) && --(lock1-->lock))==>($1-->key))", 1.00f, 0.81f)
		;

	}

	@Test
	void multiple_variable_elimination() {


		test.volMax(16);
		test.confMin(0.8f);
		test.believe("((($x --> key) && ($y --> lock)) ==> open($x, $y))");
		test.believe("({lock1} --> lock)");
		test.mustNotOutput(cycles,"({lock1}-->lock)", BELIEF, 1, 1, 0, 0.89f, ETERNAL);
//		test.mustNotOutput(cycles,"((($1-->key)&&($2-->lock))==>open($1,$2))", BELIEF, 1, 1, 0, 0.89f, ETERNAL);
		test.mustBelieve(cycles, "((({lock1}-->lock)&&($1-->key))==>open($1,{lock1}))", 1.00f, 0.81f);


	}


	@Test
	void multiple_variable_elimination2() {

		test.volMax(16);
		TestNAR tester = test;
		tester.believe("(lock:$x ==> (key:#y && open(#y,$x)))");
		tester.believe("lock:{lock1}");
		tester.mustBelieve(cycles, "((#1-->key) && open(#1,{lock1}))", 1.00f, 0.81f);

	}

	@Test
	void variable_elimination_deduction_neg_conc() {

		test
			.volMax(13)
			.believe("((&&,(#1 --> lock),open($2,#1)) ==> ($2 --> key))", 0.00f, 0.90f)
			.believe("(lock1 --> lock)", 1.00f, 0.90f)
			.mustBelieve(cycles, "((open(lock,lock1)&&(lock1-->lock))==>($1-->key))", 0.00f, 0.81f)
//			.mustBelieve(cycles, "(open($1,lock1)==>($1-->key))", 0.00f, 0.73f)
		;
	}

	@Test
	void variable_elimination_deduction_neg_condition() {

		test
			.volMax(14)
			.believe("((&&, --(#1 --> lock), open($2,#1)) ==> ($2 --> key))")
			.believe("--(lock1 --> lock)")
			.mustBelieve(cycles, "((open($1,lock1)&&--(lock1-->lock))==>($1-->key))", 1.00f, 0.81f)
//			.mustBelieve(cycles, "(open($1,lock1)==>($1-->key))", 1.00f, 0.73f)
		;
	}



	@Test
	void recursionSmall() {

		test.volMax(8);
		test.confMin(0.5f);
		test.nar.freqRes.set(0.25f);
		test.nar.confRes.set(0.05f);
		test
			.believe("num:x", 1.0f, 0.9f)
			.believe("( num:$1 ==> num($1) )", 1.0f, 0.9f)
			.mustBelieve(cycles, "num(x)", 1.0f, 1.0f, 0.81f, 1.0f)
			.mustBelieve(cycles, "num((x))", 0.99f, 1.0f, 0.50f, 1.0f)
			.mustBelieve(cycles*2, "num(((x)))", 0.99f, 1.0f, 0.25f, 1.0f)


		;
	}

	@Test
	void recursionSmall1() {


		test.volMax(10);
		test.confMin(0.5f);
		test.nar.freqRes.set(0.2f);
		test
			.believe("num(x)", 1.0f, 0.9f)
			.believe("( num($1) ==> num(($1)) )", 1.0f, 0.9f)
			.question("num(((x)))")
			.mustBelieve(cycles, "num((x))", 1.0f, 1.0f, 0.8f, 1.0f)
			.mustBelieve(cycles, "num(((x)))", 1.0f, 1.0f, 0.5f /*0.66f*/, 1.0f);


	}

}