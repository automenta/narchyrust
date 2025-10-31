package nars.func;

import nars.NAR;
import nars.NARS;
import nars.Term;
import nars.action.link.STMLinker;
import nars.deriver.impl.TaskBagDeriver;
import nars.deriver.reaction.Reactions;
import nars.term.Compound;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.Op.BELIEF;
import static nars.term.atom.Bool.Null;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberTest {

	private final NAR n = NARS.shell();
	{
		Reactions r = NARS.Rules.nal(7, 8, "nal2.member.nal");
        new TaskBagDeriver(
                ((NARS.Rules) r.add(new STMLinker(1, true, false, false, false))).core().stm().temporalInduction().compile(n),
				n).everyCycle(n.main());
	}

	@Test
	void Member1_true() {
		assertEquals(
			Set.of($$("member(a,{a,b})")),
			FunctorTest.eval($$c("member(a,{a,b})"), n));
	}

	@Test
	void Member_one_element_const() {
		assertEquals(
			Set.of($$("a")),
			FunctorTest.eval($$c("(member(#x,{a}) && #x)"), n));
	}
	@Test
	void Member_invalid_recursive() {
		assertEquals(
			Set.of(Null),
			FunctorTest.eval($$c("member(#x,{a(#x)})"), n));
	}

	@Test
	void Member_invalid_recursive_NAL() {
		TestNAR t = new TestNAR(n);
		int cycles = 32;
		String invalidMember = "member(#1,{a(#1)})";
		t.believe(invalidMember);
		t.mustNotOutput(cycles, invalidMember, BELIEF);
        t.run();
    }

	@Test
	void Member_one_element_var_reduce_to_equals() {
		assertEquals(
			Set.of($$("(#x=#y)")),
			FunctorTest.eval($$c("member(#x,{#y})"), n));
	}

	@Disabled
	@Test void Member1_product_arg() {
		assertEquals(
			Set.of($$("member(a,(a,b))")),
			FunctorTest.eval($$c("member(a,(a,b))"), n));

	}

	@Test void Member1_false() {
		assertEquals(
			Set.of($$("--member(c,{a,b})")),
			FunctorTest.eval($$c("member(c,{a,b})"), n));

	}

	@Test void Member1_generator() {
		assertEquals(
			Set.of($$("member(a,{a,b})"), $$("member(b,{a,b})")),
			FunctorTest.eval($$c("member(#x,{a,b})"), n));
	}

	@Test void member_unwrap1() {
		assertEquals(
			Set.of($$("a"), $$("b")),
			FunctorTest.eval($$c("(member(#x,{a,b}) && #x)"), n));
	}
	@Test void member_unwrap2() {
		assertEquals(
			Set.of($$("(a)"),$$("(b)")),
			FunctorTest.eval($$c("(member(#x,{a,b}) && (#x))"), n));
	}

	@Test void member_can_not_unwrap_due_to_var() {
		assertEquals(
			Set.of($$("(member(f(#x),{a,b}) && (#x))")),
			FunctorTest.eval($$c("(member(f(#1),{a,b}) && (#1))"), n));
	}

	@Test void MultiTermute1() {
		Term s = $$(
			"(((--,(x-->((--,#2)&&y))) &&+4710 (member(#1,{z,#2})&&(x-->#1))) &&+4000 member(#1,{2,3}))");
		assertEquals(Set.of(
			$$("((--,(x-->((--,2)&&y))) &&+4710 (x-->2))"),
			$$("((--,(x-->((--,3)&&y))) &&+4710 (x-->3))")
		), FunctorTest.eval((Compound)s, n));
	}



	@Test void Member_Combine_Rule() {


		TestNAR t = new TestNAR(n);
		t.volMax(30);
		t.believe("(member(#1,{a,b}) && (x(#1), y(#1)))");
		t.believe("(member(#1,{c,d}) && (x(#1), y(#1)))");
		int cycles = 100;
		t.mustBelieve(cycles, "(member(#1,{a,b,c,d}) && (x(#1), y(#1)))", 1, 0.81f);
		t.run(cycles);
	}

	@Test void Member_Diff_Rule() {
		TestNAR t = new TestNAR(n);
		t.volMax(20);
		t.believe("  (member(#1,{a,b,c}) && (x(#1), y(#1)))");
		t.believe("--(member(#1,{b,d  }) && (x(#1), y(#1)))");
		int cycles = 100;
		t.mustBelieve(cycles, "(member(#1,{a,c}) && (x(#1), y(#1)))", 1, 0.81f);
		t.run(cycles);
	}

	@ParameterizedTest
 	@ValueSource(strings = {"&&", "==>"}) void member_Budgeting(String op) {
		TestNAR t = new TestNAR(n);

        double[] priSum = {0};
		n.main().eventTask.on((x)-> priSum[0] += x.pri());

		String belief = "(member(#x,{a,b}) " + op + " #x)";

        int cycles = 2;
        t.mustBelieve(cycles, belief,1f,0.9f); //the belief itself
		t.mustBelieve(cycles,"a",1f,0.9f);
		t.mustBelieve(cycles,"b",1f,0.9f);

        final double initPri = 0.5f;
        t.input("$" + initPri + " " + belief + ".");


		t.run(cycles);

		assertEquals(initPri, priSum[0], 0.01);
	}

	@Test void member_unwrap_2d() {
		assertEquals(
			Set.of($$("(a,c)"),$$("(a,d)"),$$("(b,c)"),$$("(b,d)")),
			FunctorTest.eval($$c("(&&, member(#x,{a,b}), member(#y,{c,d}), (#x,#y))"), n));
	}
}