package nars.term;

import nars.Narsese;
import nars.Term;
import nars.term.atom.Atomic;
import nars.term.util.transform.VariableTransform;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 8/28/15.
 */
class VariableTest {


	private static void testVariableSorting(String a, String b) {
//		Compound A = raw(a);
		Compound B = raw(b);

//		Term NA = A.normalize();
		Term NB = B.normalize();
//		System.out.println(A + "\t" + B);
//		System.out.println(NA + "\t" + NB);

		assertEq(a, NB);
	}

	private static Compound raw(String a) {
		try {
			return (Compound) Narsese.term(a, false);
		} catch (Narsese.NarseseException e) {
			fail(e);
			return null;
		}
	}

	@Test
	void testPatternVarVolume() throws Narsese.NarseseException {

		assertEquals(0, Narsese.term("$x").complexityConstants());
		assertEquals(1, Narsese.term("$x").complexity());

		assertEquals(0, Narsese.term("%x").complexityConstants());
		assertEquals(1, Narsese.term("%x").complexity());

		assertEquals(Narsese.term("<x --> y>").complexity(),
			Narsese.term("<%x --> %y>").complexity());

	}

	@Test
	void testNumVars() throws Narsese.NarseseException {
		assertEquals(1, Narsese.term("$x").vars());
		assertEquals(1, Narsese.term("#x").vars());
		assertEquals(1, Narsese.term("?x").vars());
		assertEquals(1, Narsese.term("%x").vars());

		assertEquals(2, $("<$x <-> %y>").vars());
	}

	@Test
	void testBooleanReductionViaHasPatternVar() throws Narsese.NarseseException {
		assertEquals(0, $("<a <-> <$1 --> b>>").varPattern());

		assertEquals(1, $("<a <-> <%1 --> b>>").varPattern());

		assertEquals(2, $("<%2 <-> <%1 --> b>>").varPattern());

	}

	@Test
	void testNormalizeUnitCompound() {
		assertEq("(#1)", $$("(#2)").normalize());
		assertEq("(#1)", $$("(#1)").normalize());
	}

	@Disabled @Test
	void testNormalizeNegs() {
		assertEq("(--,#1)", $$("(--,#2)").normalize());
		assertEq("a(#1)" /*"a((--,#1))"*/, $$("a((--,#2))").normalize());
		assertEq("(--,#1)", $$("(--,#1)").normalize());
//		assertEq("(--,#1)", $$("(--,#x)").normalize());

//		assertEq("((--,#1)&&(--,#2))", $$("((--,#3) && (--,#2))").normalize());
	}

	/**
	 * tests target sort order consistency
	 */
	@Test
	void testVariableSubtermSortAffect0() {

		assertEquals(-1, varIndep(1).compareTo(varIndep(2)));


		Term k1 = inh(varIndep(1), Atomic.atomic("key"));
		Term k2 = inh(varIndep(2), Atomic.atomic("key"));
		Term l1 = inh(varIndep(1), Atomic.atomic("lock"));
		Term l2 = inh(varIndep(2), Atomic.atomic("lock"));
		assertEquals(-1, k1.compareTo(k2));
		assertEquals(+1, k2.compareTo(k1));
		assertEquals(-1, l1.compareTo(l2));
		assertEquals(+1, l2.compareTo(l1));

		assertEquals(l1.compareTo(k1), -k1.compareTo(l1));
		assertEquals(l2.compareTo(k2), -k2.compareTo(l2));

// TODO correct:
//		assertEquals(-1, k1.compareTo(l2));
//		assertEquals(+1, k2.compareTo(l1));
//		assertEquals(+1, l2.compareTo(k1));
//		assertEquals(-1, l1.compareTo(k2));


	}

	/**
	 * tests target sort order consistency
	 */
	@Test
	void testVariableSubtermSortAffectNonComm() {

		testVariableSorting("(($1-->key),($2-->lock))", "(($2-->key),($1-->lock))");

	}

	/**
	 * tests target sort order consistency
	 */
	@Test
	void testVariableSubtermSortAffect1() {

		testVariableSorting(
			"((($1-->key)&&($2-->lock))==>open($1,$2))",
			"((($1-->key)&&($2-->lock))==>open($1,$2))"
		);
		testVariableSorting(
			"((($1-->key)&&($2-->lock))==>open($1,$2))", //"((($1-->lock)&&($2-->key))==>open($2,$1))",
			"((($1-->lock)&&($2-->key))==>open($2,$1))"
		);

		testVariableSorting(
			"(open($1,$2)==>(($1-->key)&&($2-->lock)))",
			"(open($1,$2)==>(($1-->key)&&($2-->lock)))"
		);
		testVariableSorting(
			"(open($1,$2)==>(($1-->key)&&($2-->lock)))",
			"(open($2,$1)==>(($1-->lock)&&($2-->key)))"
		);
	}

	@Test
	void VariableTransform1() {
        Term y = VariableTransform.indepToQueryVar.apply($$("(_3(#1,_1) &&+1 _2(#1,$2))"));
		assertEq("(_3(#1,_1) &&+1 _2(#1,?$2))", y);
		assertEq("(_3(#1,_1) &&+1 _2(#1,?2))", y.normalize());
	}

	@Test
	void NormalizationComplex() {
		assertEq("(#1,$2)", $$("(#1,$1)").normalize());
		assertEq("(#1,?2)", $$("(#1,?1)").normalize());
		assertEq("(#1,($2==>($2)))", $$("(#1,($1==>($1)))").normalize());
		assertEq("(($1==>($1)),#2)", $$("(($1==>($1)),#1)").normalize());
	}

	/** TODO ? */
	@Disabled @Test
	void NormalizationComplex2() {
		Compound x = (Compound) $$$("((($1-->Investor)==>(possesses($1,#2)&&({#2}-->Investment))) ==>+- ({#1}-->Investment))");
		assertFalse(x.NORMALIZED());
		assertEq("((($1-->Investor)==>(possesses($1,#2)&&({#2}-->Investment))) ==>+- ({#3}-->Investment))", x.normalize());
	}

	@Test
	void testInhMultipleDep() {

		String s = "(x(#1)-->x(#2))";
		assertEq(s, s);


		String r = "(x(#y)-->x(#z))";
		assertEq(s, r);

	}

	@Test void normalizationInh() {
		assertEquals(-1, $$$("a(x,#1)").compareTo($$$("b(x,#2)")));
		assertEquals(-1, $$$("a(x,#2)").compareTo($$$("b(x,#1)")));
		assertEquals(+1, $$$("b(x,#1)").compareTo($$$("a(x,#2)")));
		assertEquals(+1, $$$("b(x,#2)").compareTo($$$("a(x,#1)")));
		assertEquals("(a(x,#1) &&+3 b(x,#2))", $$$("(a(x,#1) &&+3 b(x,#2))").normalize().toString());
		assertEquals("(a(x,#1) &&+3 b(x,#2))", $$$("(a(x,#2) &&+3 b(x,#1))").normalize().toString());
	}
}