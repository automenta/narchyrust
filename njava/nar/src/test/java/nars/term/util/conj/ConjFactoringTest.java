package nars.term.util.conj;

import nars.Term;
import nars.term.Compound;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.Op.*;
import static nars.term.atom.Bool.False;
import static nars.term.util.Testing.assertCond;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.*;

@Disabled class ConjFactoringTest {

	private static final Term w = $$("w");
	private static final Term x = $$("x");
	private static final Term y = $$("y");
	private static final Term z = $$("z");

	@Test
	void ContainsEventFactored0() {
		assertCond("(z&&(x &&+1 y))", "z");
	}
	@Test
	void ContainsEventFactored1() {
		assertCond("(z&&(x &&+1 y))", "(x&&z)");
		assertCond("(z&&(x &&+1 y))", "(y&&z)");
		assertCond("(z&&(x &&+1 (y &&+1 w)))", "(z&&w)");
	}

	@Test
	void CondTime_Factored() {
		Compound c = $$c("(z&&(x &&+1 y))");
		assertEquals(0, c.when( $$("(x&&z)"), true));
		assertTrue(c.condFirst( $$("(x&&z)")));
		assertEquals(1, c.when( $$("(y&&z)"), true));
		assertFalse(c.condFirst( $$("(y&&z)")));
	}

	@Test
	void DontFactorDisj() {
		ConjBuilder c = new ConjTree();
		c.add(0, $$("(a||b)"));
		c.add(4, $$("(--a&&b)"));
		Term cc = c.term();
		assertEq("((a||b) &&+4 ((--,a)&&b))", cc);
	}

	@Test
	void FactorizeParallelConjETE() {
		Term x = $$("(a&|b)");
		Term y = $$("(b&|c)");
		ConjBuilder c = new ConjTree();
		c.add(ETERNAL, x);
		assertEquals(2, c.eventCount(ETERNAL));
		c.add(ETERNAL, y);
		assertEquals(3, c.eventCount(ETERNAL));
		assertEquals(1, c.eventOccurrences());
//        c.factor();
//        assertEquals(3, c.eventCount(ETERNAL)); //unchanged
//        assertEquals(1, c.eventOccurrences());
		assertEq("(&&,a,b,c)", c.term());
	}

	@Test
	void DistributedParallelIntoTemporals3() {
		assertEq("((x&&y) &&+- x)", "(((x && y) &&+- x)&&x)");
	}

	@Test void InnerDistribute_pos_a() {
		assertEq("((y&&z) &&+1 z)", "((z&&y) &&+1 z)");
	}

	@Test void InnerDistribute_pos_b() {
		assertEq("(((--,z)&&y) &&+1 (--,z))", "(--z && (y &&+1 --z))");
	}
	@Test void InnerDistribute_neg_a() {
		assertEq("(((--,z)&&y) &&+1 (--,z))", "(((--,z)&&y) &&+1 (--,z))");
	}

	@Test
	void ContainsEventFactored2() {

		assertFalse($$c("(z&&(x &&+1 y))").condOf($$("(x&&y)")));

	}

	private static final String yss =
		"((x&&y) &&+1 (x&&z))"
		//"((y &&+1 z)&&x)"
	;

	@Test void SimpleSeq() {
		assertEq(yss, "((x&&y) &&+1 (x&&z))");
	}

	@Test void SimpleSeq_Manual() {
		ConjTree t = new ConjTree();
		t.add(ETERNAL, x);
		t.add(1, y);
		t.add(2, z);
		assertEq(yss, t.term());
	}

	@Test
	void SimpleSeq_bundled() {
		assertEq("(((y-->a) &&+1 (z-->a))&&(x-->a))",
				"((a:y &&+1 a:z)&&a:x)");
	}

	@Test
	void SimpleSeq2() {
		String y = "(&&,(y &&+1 z),(--,w),x)";
		assertEq(y, "((&&,(--,w),x,y) &&+1 (&&,(--,w),x,z))");

		ConjTree t = new ConjTree();
		t.add(ETERNAL, x);
		t.add(ETERNAL, w.neg());
		t.add(1, $$(y));
		t.add(2, z);
		Term z = t.term();
		assertEq(y, z);
	}
	@Test
	void FactoredElimination() {
		//TODO
		//test that the eternal component is not eliminated while its dependent temporal component remains
		//may need Conj.distribute() method for exhaustive, unfactored comparison
		//test that implication construction returns the same result whether conj-containing input is factored or not

		assertEq("((c &&+1 d)&&x)", "((x&|c) &&+1 (x&|d))"); //sanity pre-test
		assertEquals($$("((c &&+1 d),x)").complexity() + 2, $$("((x&|c),(x&|d))").complexity()); //factored form results in 2 volume savings


		//the (c&&x) case reduces to 'c' because it occurs at the same time point as (b&&x)
		assertEq("(((a &&+1 b)&&x)==>((c &&+1 d)&&x))",
			"((x&&(a &&+1 b)) ==> (x&&(c &&+1 d)))"
		);


		assertEq("(((a &&+1 b)&&x)==>((c &&+1 d)&&x))",
			"((x&&(a &&+1 b)) ==> ((x&&c) &&+1 (x&&d)))");
	}

	@Test void disjunctionInSeqElim1() {
		String x = "(((--,speed) &&+110 (--,(vy-->1)))&&(--,z))";
		assertEq(x, x);
	}
	@Test void disjunctionInSeqElim1b() {
		assertEq("((((--,speed) &&+110 (--,(vy-->1)))&&(--,z)))",
				"(((--z && (--,speed)) &&+110 ((--,(vy-->1))&&--z)))");
	}
	@Test void disjunctionInSeqElim1c() {
		String x = "(((--,speed) &&+110 (--,(vy-->1)))&&(--,z))";
		assertEq(x,
				"(((--speed||z) &&+110 (--,(vy-->1))) && --z)");
	}
	@Test void disjunctionInSeqElim1d() {
		assertEq("(c &&+110 (c&&d))",
			"((((--,(a &&+43 b))||c) &&+110 d)&& c))");
	}
	@Test void disjunctionInSeqElim1_x() {
		assertEq("(((a||z) &&+- b) && --z)",
				"(((a||z) &&+- b) && --z)");
	}
	@Test void disjunctionInSeqElim1_x2a() {
		assertEq("((x &&+- y)&&z)",
				"((x &&+- y) && z)");
	}
	@Test void disjunctionInSeqElim1_x2b() {
		assertEq("(((--,a) &&+- (--,b))&&(--,z))",
				"(((--a||z) &&+- --b) && --z)");
	}

	@Test void disjunctionInSeqElim1_x3() {
		assertEq("((c&&d) &&+- c)",
				"((((--,(a &&+43 b))||c) &&+- d)&& c))");
	}
	@Test void disjunctionInSeqElim2() {
		assertEq("((--,(a &&+1 b))&&(--,c))",
				"(((--,(a &&+1 b))||c) && --c)");
	}
	@Test void disjunctionInSeqElim3() {
		assertEq("(((--,(a &&+1 b)) &&+2 d)&&(--,c))",
		   	  "((((--,(a &&+1 b))||c) &&+2 d)&&(--,c)))");
	}
	@Test void disjunctionInSeqElimSeq() {

		assertEq("(--,c)", "((--,(a&&c)) && --c)");
		assertEq("(--,c)"
				,"((--,(a &&+1 c)) && --c)");

//		assertEq("(((--,a) &&+2 d)&&(--,c))",
//				"(((--,(a &&+1 c)) &&+2 d)&&(--,c)))");
	}
	@Test void disjunctionInSeqElim2_neg() {

		assertEq("c", "(((--,(a && b))||c) && c)");
		assertEq("(c&&d)", "((((--,(a && b))||c)||d) && (c&&d))");

		assertEq("c", "(((--,(a &&+1 b))||c) && c)");

		assertEq("(c &&+3 (c&&d))", "((((--,(a &&+1 b))||c) &&+2 d)&&c))");
	}

	@Test
	void ConjEternalConjEternalConj() {

		Term a = $$("((x &&+7130 --x)&&y)");
		Term b = $$("z");
		assertEq(
			"(&&,(x &&+7130 (--,x)),y,z)", //NOT: "(((x &&+7130 --x)&&y)&&z)",
			CONJ.the(a, b)
		);
	}

	@Test
	void DisjInSeqPartialReduction() {

		assertEq("(((--,jump) &&+320 jump)&&(--,R))",
			$$("(((--,jump) &&+320 (R||jump))&&(--,R))"));
		assertEq("((jump &&+320 (--,jump))&&(--,R))",
			$$("((jump &&+320 (R||--jump))&&(--,R))"));

		assertEq("(((--,jump) &&+320 jump)&&R)",
			$$("(((--,jump) &&+320 (--R||jump))&&R)"));
		assertEq("((jump &&+320 (--,jump))&&R)",
			$$("((jump &&+320 (--R||--jump))&&R)"));
	}

	@Test
	void Distribute_seq_Complex() {
		{
			String s = "((--,(((_6(_1,((--,_4(_2,_3)) &&+60 (--,_5)))&&(--,_5)) &&+43 (--,_5)) &&+72 (--,_7)))||_8)";
			Term x = $$(s);
			assertEq(s, x);
		}

		{
			Term x = $$("(_5 && ((--,(((_6(_1,((--,_4(_2,_3)) &&+60 (--,_5)))&&(--,_5)) &&+43 (--,_5)) &&+72 (--,_7)))||_8))");
			assertEq("_5", x);
		}
	}

	@Test
	void Dternalize() {
		assertEq("((a &&+3 b)&&c)"
			/*"((a&|c) &&+3 (b&|c))"*/, $$c("((a &&+3 b) &&+3 c)").dt(DTERNAL));
	}
	@Test
	void EventNonContradictionWithEternal2() {
		ConjBuilder c = new ConjTree();
		c.add(ETERNAL, x);
		c.add(1, y);
		c.add(2, z);
		assertEq("((y &&+1 z)&&x)", c.term());

	}
	@Test
	void FactorizeEternalConj1() {
		ConjBuilder c = new ConjTree();
		c.add(1, $$("(a&&x)"));
		c.add(2, $$("(b&&x)"));
//        assertTrue(c.eventCount(ETERNAL) == 0);
//        assertTrue(c.eventOccurrences() == 2);
//        c.factor();
		assertEq("((a &&+1 b)&&x)", c.term());
//        assertTrue(c.eventCount(ETERNAL) == 1);
//        assertTrue(c.eventOccurrences() == 3);
		assertEquals(1, c.shift());
	}

	@Test
	void FactorizeEternalConj2() {
		ConjBuilder c = new ConjTree();
		c.add(1, $$("(a&&(x&&y))"));
		c.add(2, $$("(b&&(x&&y))"));
        assertEquals(0, c.eventCount(ETERNAL));
        assertEquals(2, c.eventOccurrences());
//        c.factor();
//        assertTrue(c.eventCount(ETERNAL) == 2);
//        assertTrue(c.eventOccurrences() == 3);
//        assertEq("(x&&y)", c.term(ETERNAL));
//        assertEq("a", c.term(1));
//        assertEq("b", c.term(2));
		assertEq("(&&,(a &&+1 b),x,y)", c.term());
	}

	@Test
	void SequenceAutoFactor() {
		Term xyz = $$("((x &| y) &&+2 (x &| z))");
		assertEq("((y &&+2 z)&&x)", xyz);

		assertEquals($$("(y &&+2 z)").seqDur(), xyz.seqDur());
		assertEquals(2, xyz.seqDur());
	}
	@Test
	void SequenceInnerConj_Normalize_to_Ete_factored() {


		//factored due to repeat x
		Term xy_xz = $$("((x &| y) &&+1 (x &| z))");
		assertEq("((y &&+1 z)&&x)", xy_xz);


	}

	@Test
	void SequentialFactor() {
		assertEq("((y &&+1 z)&&x)", "((x&&y) &&+1 (x&&z))");
	}
	@Test
	void WrappingCommutiveConjunction() {


		Term xEternal = $$("((((--,angX) &&+4 x) &&+10244 angX) && y)");
		assertEquals(
			"((((--,angX) &&+4 x) &&+10244 angX)&&y)",
			//"((((--,angX)&|y) &&+4 (x&|y)) &&+10244 (y&|angX))",
			xEternal.toString());
	}
	@Test
	void WrappingCommutiveConjunctionX() {

		Term xFactored = $$("((x&&y) &&+1 (y&&z))");
		assertEquals("((x &&+1 z)&&y)", xFactored.toString());


		Term xAndContradict = $$("((x &&+1 x)&&--x)");
		assertEquals(False,
			xAndContradict);

	}

	@Disabled
	@Test
	void FactorFromEventSequence() {
		Term yParallel1 = $$("((((--,angX) &&+4 x) &&+10244 angX) &| y)");
		String yParallel2Str = "((((--,angX)&|y) &&+4 (x&|y)) &&+10244 (angX&|y))";
		Term yParallel2 = $$(yParallel2Str);
		assertEquals(yParallel1, yParallel2);
		assertEquals(yParallel2Str, yParallel1.toString());
	}

	@Disabled
	@Test
	void FactorFromEventParallel() {
		Term yParallelOK = $$("(((a&&x) &| (b&&x)) &| (c&&x))");
		assertEquals("", yParallelOK.toString());


		Term yParallelContradict = $$("((a&&x) &| (b&&--x))");
		assertEquals(False, yParallelContradict);
	}

}