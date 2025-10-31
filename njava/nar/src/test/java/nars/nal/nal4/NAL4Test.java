package nars.nal.nal4;

import nars.NAR;
import nars.NARS;
import nars.action.transform.ImageAlign;
import nars.action.transform.ImageUnfold;
import nars.nal.nal7.NAL7Test;
import nars.term.util.Image;
import nars.test.NALTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.Op.*;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NAL4Test extends NALTest {


	private static final int cycles = 24;

	@BeforeEach
	void setTolerance() {
		test.confTolerance(NAL7Test.TEMPORAL_CONF_TOLERANCE); //for NAL4 Identity / StructuralReduction option
	}

	@Override
	protected NAR nar() {
		NARS nn = new NARS.DefaultNAR(0, 0, false);
		nn.then("deriver", N-> nn.deriver(NARS.Rules.nal(1, 6)
						.core().stm().temporalInduction()
						.addAll(new ImageUnfold(true),
								new ImageAlign.ImageAlignBidi())
				, N).everyCycle(N.main()));
		NAR n = nn.get();
		n.complexMax.set(10);
		return n;
	}


	@Test
	void structural_transformation_dont() {

		test
			.mustNotOutput(cycles, "(reaction --> (acid,base))", BELIEF, ETERNAL)
			.believe("(acid --> (reaction,/,base))")
		;
	}

	@Test
	void structural_transformationExt_forward_repeats2() {
		test
			.believe("((a,b,a) --> bitmap)", 1, 0.9f)
			.mustBelieve(cycles, "(b --> (bitmap,a,/,a))", 1, 0.9f)
			.mustBelieve(cycles, "(a --> (bitmap,/,b,/))", 1, 0.9f)
		;
	}

	@Test
	void structural_transformationExt_forward_repeats_sim() {
		test
			.volMax(16)
			.believe("((a,b,a) --> bitmap)", 1, 0.9f)
			.believe("(a <-> b)", 0.0f, 0.9f)
			.mustBelieve(cycles, "((bitmap,/,b,/) <-> (bitmap,a,/,a))", 0.0f, 0.81f)
		;
	}

	@Test
	void structural_transformationExt_only_first_layer() {

		test
			.mustOutputNothing(cycles)
			.believe("acid(reaction,/,base)", 1, 0.9f)
//                .mustBelieve(CYCLES, "((reaction,\\,/,base) --> acid)", 1, 0.9f)
//                .mustBelieve(CYCLES, "((reaction,acid,\\,/) --> base)", 1, 0.9f)
//                .mustNotOutput(CYCLES, "((acid,/,base) --> reaction)", BELIEF, ETERNAL)
		;
	}

	@Test
	void structural_transformationExt_forward_repeats2numeric() {
		test
			.believe("((0,1,0) --> bitmap)", 1, 0.9f)
			.mustBelieve(cycles, "(1 --> (bitmap,0,/,0))", 1, 0.9f)
			.mustBelieve(cycles, "(0 --> (bitmap,/,1,/))", 1, 0.9f)
			.mustNotOutput(cycles, "(bitmap --> (0,1,0))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
		;
	}

	@Test
	void structural_transformationExt_forward_repeats2numeric_temporal() {
		test
			.input("((0,1,0) --> bitmap). |")
			.mustBelieve(cycles, "(1 --> (bitmap,0,/,0))", 1, 0.9f, 0)
			.mustBelieve(cycles, "(0 --> (bitmap,/,1,/))", 1, 0.9f, 0)
			.mustNotOutput(cycles, "(bitmap --> (0,1,0))", BELIEF, 1f, 1f, 0.9f, 0.9f, t -> true)
		;
	}

	@Test
	void structural_transformationExt_forward_repeats3() {
//		test.nar.onTask(t -> {
//			assertEquals(0, ((NALTask)t).creation(), ()->
//					"creation must be t=0" + t);
//		});
		test
			.believe("((0,1,0,1) --> bitmap)", 1, 0.9f)
			.question("(1 --> (bitmap, 0,/,0,/))")
			.mustBelieve(cycles, "(1 --> (bitmap, 0,/,0,/))", 1, 0.9f)
			.question("(0 --> (bitmap, /,1,/,1))")
			.mustBelieve(cycles, "(0 --> (bitmap, /,1,/,1))", 1, 0.9f);
	}

	@Test
	void structural_transformationExt_reverse() {
		test
			.mustBelieve(cycles, "reaction(acid,base)", 1, 0.9f)
			.believe("(acid --> (reaction,/,base))", 1, 0.9f)
			.question("reaction(acid,base)")
		;
	}

	@Test
	void structural_transformationExt() {

		test
			.mustBelieve(cycles, "(acid --> (reaction,/,base))", 1, 0.9f)
			.mustBelieve(cycles, "(base --> (reaction,acid,/))", 1, 0.9f)
			.mustNotOutput(cycles, "(reaction --> (acid,base))", BELIEF, ETERNAL)
			.believe("((acid,base) --> reaction)", 1, 0.9f)
		;
	}
	@Disabled @Test void testSimpleImageQueryVar() {
		//<<$1 --> (helpful_to /1 nars)> <=> ?1>?
		//System.out.println($$("(((y,\\,a) --> $1) <-> ((y,\\,b) --> $1)))"));
		test.freqRes(0.1f).volMax(22)
				.believe("(y-->(a,x))")
				.believe("(y-->(b,x))")
				.question( "(((y,\\,x) --> $1) <-> ?1)")
				.mustBelieve(10,"((y-->(a,$1))<->(y-->(b,$1)))", 1, 0.45f);
	}
	@Test
	void structural_transformationInt_0() {
		test
			.mustBelieve(cycles, "((reaction,\\,base) --> acid)", 1, 0.9f)
			.mustBelieve(cycles, "((reaction,acid,\\) --> base)", 1, 0.9f)
			.mustNotOutput(cycles, "((acid,base) --> reaction)", BELIEF, ETERNAL)
			.believe("(reaction --> (acid,base))", 1, 0.9f)
		;
	}


	@Test
	void structural_transformationInt_reverse() {
		test
			.mustBelieve(cycles, "(neutralization --> (acid,base))", 1, 0.9f)
			.mustNotOutput(cycles, "(neutralization --> (acid,/,\\,base))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
			.believe("((neutralization,\\,base) --> acid)", 1, 0.9f)
		;
	}

	@Test
	void structural_transformation_DepVar1() {
		test
			.mustBelieve(cycles, "(base --> (reaction,#1,/))", 1, 0.9f)
			.mustBelieve(cycles, "(#1 --> (reaction,/,base))", 1, 0.9f)
			.believe("reaction(#1,base)", 1, 0.9f);
	}

	@Test
	void structural_transformation_DepVar2() {
		test.mustBelieve(cycles, "(acid --> (reaction,/,#1))", 1, 0.9f).
			mustBelieve(cycles, "(#1 --> (reaction,acid,/))", 1, 0.9f).
			believe("reaction(acid,#1)", 1, 0.9f);
	}

	@Test
	void structural_transformation_one_arg() {
		test.mustBelieve(cycles, "(acid --> (reaction,/))", 1, 0.9f);
		//test.mustNotOutput(cycles, "(acid --> (reaction,/))", BELIEF, 0, 1, 0, 1, t->true);
		test.believe("reaction(acid)", 1, 0.9f);
	}

	@Test
	void structural_transformation6() {
		test
			.mustBelieve(cycles, "(neutralization --> (acid,base))", 1, 0.9f) //en("Something that can be neutralized by an acid is a base.");
			.mustNotOutput(cycles, "((acid,base) --> neutralization)", BELIEF, 1f, 1, 0.9f, 0.9f, ETERNAL)
			.believe("((neutralization,acid,\\) --> base)", 1, 0.9f) //en("Something that can neutralize a base is an acid.");
		;
	}

	@Test
	void structural_transformation6_temporal() {
		test
			.mustNotOutput(cycles, "((acid,base) --> neutralization)", BELIEF, 1f, 1, 0.9f, 0.9f, t -> true)
			.mustBelieve(cycles, "(neutralization --> (acid,base))", 1, 0.9f, 0) //en("Something that can be neutralized by an acid is a base.");
			.input("((neutralization,acid,\\) --> base). |") //en("Something that can neutralize a base is an acid.");
		;
	}

	@Test
	void structural_transformationInt() {
		test
			.mustBelieve(cycles, "((neutralization,\\,base) --> acid)", 1, 0.9f)
			.mustBelieve(cycles, "((neutralization,acid,\\) --> base)", 1, 0.9f)
			.believe("(neutralization --> (acid,base))")
		;
	}

	@Test
	void structural_transformation_int_neg_belief() {
		test
			.confMin(0.89f)
			.believe("--(x --> (acid,base))")
			.mustBelieve(cycles, "((x,\\,base) --> acid)", 0, 0.9f)
			.mustBelieve(cycles, "((x,acid,\\) --> base)", 0, 0.9f)
		;
	}

	@Test
	void structural_transformationInt_neg_focus() {
		test
			.believe("(nothing --> (--acid,--base))")
			.mustBelieve(cycles, "((nothing,--\\,--base) --> acid)", 1, 0.9f)
			.mustBelieve(cycles, "((nothing,--acid,--\\) --> base)", 1, 0.9f)
		;
	}

	@Test
	void imageAlign1() {
		test
			.confMin(0.8f).volMax(15)
			.believe("(neutralization --> (acid,base))")
			.believe("((acid,base) --> reaction)")
			.mustBelieve(cycles, "((neutralization,\\,base) --> (reaction,/,base))", 1, 0.81f)
			.mustBelieve(cycles, "((neutralization,acid,\\) --> (reaction,acid,/))", 1, 0.81f)

		;
	}

	@Test
	void imageAlign2() {
		test
			.volMax(11)
			.confMin(0.81f)
			.believe("(neutralization --> (acid,base))")
			.believe("(sulfuric_acid --> acid)")
			//.mustBelieve(cycles, "((neutralization,\\,base)<->sulfuric_acid)", 1, 0.45f)
            .mustBelieve(cycles, "((neutralization,\\,base) --> sulfuric_acid)", 1, 0.81f)
			.mustBelieve(cycles, "(((neutralization,\\,base)&sulfuric_acid)-->acid)", 1, 0.81f)
		;
	}

    @Test
    void imageAlign3() {
		assertEq("neutralization(sulfuric_acid,base)",
				Image.imageNormalize($$c("(sulfuric_acid-->(neutralization,/,base))")));
        test
            .volMax(6)
            .confMin(0.75f)
            .believe("((acid,base) --> neutralization)")
            .believe("(sulfuric_acid --> acid)")
            .mustBelieve(cycles, "neutralization(sulfuric_acid,base)", 1, 0.81f)

        ;
    }

	@Test
	void imageAlignTo() {
		test
			.volMax(10)
			.confMin(0.81f)
			.believe("((x,y) --> z)")
			.believe("(x --> (z,y))")
			.mustBelieve(cycles, "(x --> ((z,y)&(z,/,y)))", 1, 0.81f)
			.mustBelieve(cycles, "(((x,\\,y)&&(x,y))-->z)", 1, 0.81f)
		;
	}
	@Test
	void imageAlignTo_in_Impl() {
		test
				.volMax(14)
				.confMin(0.4f)
				.believe("((x,y) --> z)")
				.believe("(a ==> (x --> ((z,y)&(z,/,y))) )")
				.mustBelieve(cycles, "a", 1, 0.45f)
		;
	}

	@Disabled @Test
	void CompositionFromProductInh() {
		test
			.mustBelieve(cycles, "((drink,soda) --> (drink,acid))", 1, 0.81f)
			.believe("(soda --> acid)", 1, 0.9f)
			.question("((drink,soda) --> ?death)");
	}

	@Disabled
	@Test
	void CompositionFromProductSim() {

		test
			.mustBelieve(cycles, "((soda,food) <-> (deadly,food))", 1, 0.81f)
			.believe("(soda <-> deadly)", 1, 0.9f)
			.question("((soda,food) <-> #x)")
		;
	}


	@Disabled @Test
	void NeqComRecursiveConstraint() {

        /*
        SHOULD NOT HAPPEN:
        $.02;.09$ ((o-(i-happy))-->happy). 497⋈527 %.55;.18% {497⋈527: æ0IáËÑþKn;æ0IáËÑþKM;æ0IáËÑþKÄ;æ0IáËÑþKÉ;æ0IáËÑþKÌ} (((%1-->%2),(%1-->%3),neqCom(%2,%3)),((%3-->%2),((Abduction-->Belief),(Weak-->Goal),(Backward-->Permute))))
            $.04;.75$ happy(L). 497⋈512 %.55;.75% {497⋈512: æ0IáËÑþKÄ}
            $.05;.53$ ((L)-->(o-(i-happy))). 527 %.54;.53% {527: æ0IáËÑþKn;æ0IáËÑþKM;æ0IáËÑþKÉ;æ0IáËÑþKÌ} Dynamic
        */

		test
			.volMax(10)
			.mustNotOutput(cycles, "((o-(i-happy))-->happy)", BELIEF, ETERNAL)
			.believe("happy(L)", 1f, 0.9f)
			.believe("((L)-->(o-(i-happy)))", 1f, 0.9f)
		;
	}


	@Disabled
	@ValueSource(bytes = {QUESTION, QUEST})
	@ParameterizedTest
	void TransformQuestionSubj(byte punc) {
		test
			.volMax(6)
			.input("((a,b)-->?4)" + (char) punc)
			.mustOutput(cycles, "(b-->(?1,a,/))", punc)
			.mustOutput(cycles, "(a-->(?1,/,b))", punc)
		;
	}

	@Disabled
	@ValueSource(bytes = {QUESTION, QUEST})
	@ParameterizedTest
	void TransformQuestionPred(byte punc) {
		test
			.volMax(6)
			.input("(x --> (a,b))" + (char) punc)
			.mustOutput(cycles, "b(x,a,\\)", punc)
			.mustOutput(cycles, "a(x,\\,b)", punc)
		;
	}

	@Test
	void Normalize0() {
		test
			.mustBelieve(cycles, "(cat-->(likes,/,[blue]))", 1f, 0.9f)
			.mustNotOutput(cycles, "(likes-->(cat,[blue]))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
			.believe("likes(cat,[blue])")
		;
	}

	@Test
	void Normalize1() {
		String input = "((likes,cat,\\)-->[blue])";
		assertEquals("(likes-->(cat,[blue]))", Image.imageNormalize($$(input)).toString());

		test
			.mustBelieve(cycles, "(likes-->(cat,[blue]))", 1f, 0.9f)
			.mustNotOutput(cycles, "((cat,[blue])-->likes)", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
			.believe(input)
		;
	}

	@Test
	void Normalize1a() {

		test
			//.mustBelieve(CYCLES, "((cat,[blue])-->likes)", 1f, 0.9f)
			.mustNotOutput(cycles, "(likes-->(cat,[blue]))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
			.believe("([blue] --> (likes,cat,/))")
		;
	}

	@Disabled
	@Test
	void Normalize1aQ() {

		test
			.mustQuestion(cycles, "((cat,[blue])-->likes)")
			.mustNotOutput(cycles, "(likes-->(cat,[blue]))", QUESTION, 1f, 1f, 0.9f, 0.9f, ETERNAL)
			.question("([blue] --> (likes,cat,/))")
		;
	}

	@Disabled
	@Test
	void Normalize1b() {
		test
			.mustOutputNothing(cycles)
			.believe("((likes,cat,/)-->[blue])")

		;
	}

	@Test
	void Normalize2() {
		test
			.mustBelieve(cycles, "likes(cat,[blue])", 0.9f)
			.mustNotOutput(cycles, "(likes-->(cat,[blue]))", BELIEF, 1f, 1f, 0.9f, 0.9f, ETERNAL)
			.believe("([blue] --> (likes,cat,/))")
		;
	}

	@Disabled
	@Test
	void QuestionAnswering() {
		test
			.mustOutput(cycles, "((0,1)-->(1,1))", QUESTION)
			.input("((0,1)-->?1)?")
			.input("((1,1)-->x).");
	}

	@ValueSource(bytes = {QUESTION, QUEST})
	@ParameterizedTest
	@Disabled
	void TransformRawQuestionSubj(byte punc) {
		test
			.mustOutput(cycles, "(b-->(?1,a,/))", punc)
			.mustOutput(cycles, "(a-->(?1,/,b))", punc)
			.input("(a,b)" + (char) punc)
		;
	}

	@ParameterizedTest
	@ValueSource(strings = {"-->", "<->"})
	void composition_on_both_sides_of_a_statement_2(String op) {
		test
			
			.mustBelieve(cycles, "((bird,plant)" + op + "(animal,plant))", 1, 0.81f) //en(" The relation between bird and plant is a type of relation between animal and plant.")
			.believe("(bird" + op + "animal)", 1, 0.9f) //en("Bird is a type of animal.");
			.question("((bird,plant)" + op + "(animal,plant))")
		;
	}

	@Test
	void composition_on_both_sides_of_a_statement_2_neg() {
		test
			.volMax(12)
			.mustBelieve(cycles, "(((x|y),plant) --> (animal,plant))", 1, 0.81f)
			.believe("((x|y)-->animal)", 1, 0.9f)
			.question("(((x|y),plant) --> (animal,plant))")
		;
	}

	@Disabled @Test
	void one_element_unwrap() {
		test
			.volMax(6)
			.believe("((x)-->(y))", 1, 0.9f)
			.question("(x --> y)")
			.mustBelieve(cycles, "(x --> y)", 1, 0.81f)
		;
	}

	@Test
	void one_element_wrap() {
		test
			.volMax(5)
			.believe("(x-->y)", 1, 0.9f)
			.question("((x) --> (y))")
			.mustBelieve(cycles, "((x) --> (y))", 1, 0.81f)
		;
	}

	@Test
	void composition_on_both_sides_of_a_statement_2_alternating_position() {

		test
			.believe("(bird-->animal)", 1, 0.9f) //en("Bird is a type of animal.");
			.question("((bird,plant) --> (plant,animal))")
			.mustNotBelieve(cycles, "((bird,plant) --> (plant,animal))")
			//.mustBelieve(cycles, "((bird,plant) --> (plant,animal))", 1, 0.81f) //en("The relation between bird and plant is a type of relation between animal and plant.");
		;
	}


	@Test
	void composition_on_both_sides_of_a_statement_3() {

		test
			
			.believe("(bird-->animal)") //en("Bird is a type of animal.");
			.question("((wtf,bird,plant) --> (wtf,animal,plant))")
			.mustBelieve(cycles, "((wtf,bird,plant) --> (wtf,animal,plant))", 1, 0.81f) //en("The relation between bird and plant is a type of relation between animal and plant.");
		;
	}



}