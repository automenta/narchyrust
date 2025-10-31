package nars.concept.dynamic;

import nars.*;
import nars.truth.PreciseTruth;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.NAL.test.TEST_EPSILON;
import static nars.Op.ETERNAL;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.*;

/** assumes DYNAMIC_IMPL=true */
class DynImplTest extends AbstractDynTaskTest {

    @Test void Tables() {
        assertDynamicBeliefTable("(x==>y)");
        assertDynamicBeliefTable("(y==>x)");
        assertDynamicBeliefTable("(--y==>x)");
        assertDynamicBeliefTable("(--x==>y)");
    }

    @Test
    void testEternalPosPos() throws Narsese.NarseseException {

        n.input("x.");
        n.input("y.");

        assertTruthEq(1, 0.45f, "(x==>y)");
        assertTruthEq(1, 0.45f, "(y==>x)");
//        assertEquals(
//                $.t(0, 0.31f),  //contraposition
//                /*null*/ //no evidence
//                n.beliefTruth("(--x==>y)", ETERNAL));
    }

    @Test
    void testEternalPosPosSeqEte() throws Narsese.NarseseException {
        n.input("(x &&+1 z).");
        n.input("y.");
        assertTruthEq(1, 0.45f, "((x &&+1 z)==>y)");
    }

    @Test
    void testEternalNegPosSeqEte() throws Narsese.NarseseException {
        n.input("--(x &&+1 z).");
        n.input("y.");

        assertTruthEq(1, 0.45f, "(--(x &&+1 z)==>y)");
    }

    @Test
    void testEternalNegPosSeqTmp() throws Narsese.NarseseException {
        n.input("--(x &&+1 z). |");
        n.run(2);
        n.input("y. |");
        assertTruthEq(1, 0.45f, "(--(x &&+1 z) ==>+1 y)");
    }

    @Test
    void testEternalPosPosSeqTmp() throws Narsese.NarseseException {
        n.input("(x &&+1 z). |");
        n.run(2);
        n.input("y. |");
        assertTruthEq(1, 0.45f, "((x &&+1 z) ==>+1 y)");
    }

    @Test
    void testEternalNegPos() throws Narsese.NarseseException {
        n.input("--x.");
        n.input("y.");
        assertTruthEq(1, 0.45f, "(--x==>y)");
        assertTruthEq(0, 0.45f, "(y==>x)");
        assertNull(n.beliefTruth("(x==>y)", ETERNAL)); //no evidence
        assertNull(n.beliefTruth("(--y==>x)", ETERNAL)); //no evidence
    }

    @Test
    void testEternalPosNeg_defined() throws Narsese.NarseseException {
        n.input("x.");
        n.input("--y.");
        assertTruthEq(0, 0.45f, "(x==>y)");
    }

    @Test
    void testEternalPosNeg_undefined() throws Narsese.NarseseException {
        n.input("x.");
        n.input("--y.");
        assertNull(n.beliefTruth("(--x==>y)", ETERNAL)); //no evidence
    }

    @Test
    void testEternalNegNeg() throws Narsese.NarseseException {
        n.input("--x.");
        n.input("--y.");
        assertNull(n.beliefTruth("(x==>y)", ETERNAL)); //no evidence

        assertTruthEq(0, 0.45f, "(--x==>y)");
        assertEq("((--,x)==>y)", n.belief("(--x==>y)", ETERNAL).term());
    }

    @Test
    void testEternalPosConjPosPos() throws Narsese.NarseseException {
        n.input("x1.");
        n.input("x2.");
        n.input("y.");
        assertTruthEq(1, 0.81f, "(x1&&x2)");
        assertTruthEq(1, 0.42f, "((x1&&x2)==>y)");
        assertTruthEq(1, 0.42f, "(y==>(x1&&x2))");
    }

    @Test
    void testEternalPosConjPosNeg() throws Narsese.NarseseException {
        n.input("x1.");
        n.input("--x2.");
        n.input("y.");

        assertTruthEq(1, 0.45f, "(--x2==>y)");
        assertNull(n.beliefTruth("(  x2==>y)", ETERNAL));

        assertTruthEq(1, 0.42f, "(y==>(x1 && --x2))");
        assertTruthEq(0, 0.42f, "(y==>(x1 &&   x2))");

        //assertEquals($.t(1, 0.42f), n.beliefTruth("((x1 && --x2)==>y)", ETERNAL));

    }


    @Test
    void testTemporal1() throws Narsese.NarseseException {
        n.input("x. |");
        n.run(2);
        n.input("y. |");
        {
            NALTask t = n.belief($$("(x ==>+- y)"), 1, 2);
            assertNotNull(t);
            //System.out.println(t);
            assertEquals(0, t.start());
            assertEquals(0, t.end());
            assertEquals(2, t.stamp().length);
            assertEq("(x ==>+2 y)", t.term());
            Truth tt = t.truth();
            assertEquals(1, tt.freq(), 0.01f);
			assertEquals(0.25, (float) tt.conf(), 0.20f);
        }
        //assertEquals("(x==>y). 0 %1.0;.37%", n.belief($$("(x==>y)"), 0, 0).toStringWithoutBudget());
        assertEquals("(x ==>+2 y). 0 %1.0;.45%", n.belief($$("(x ==>+- y)"), 0, 0).toStringWithoutBudget());
        assertEquals("(x ==>+2 y). 0 %1.0;.45%", n.belief($$("(x==>y)"), 0, 0).toStringWithoutBudget());
        //assertEquals("(x ==>+1 y). 0 %1.0;.42%", n.belief($$("(x ==>+1 y)"), 0, 0).toStringWithoutBudget());
    }

    @Test void WeakPolarity() throws Narsese.NarseseException {
        n.input("x. %0.2%");
        n.input("y.");
        {
            Task t = n.belief($$("(x==>y)"));
            assertNotNull(t);
            assertEquals("(x==>y). %1.0;.09%",t.toStringWithoutBudget());
        }
        {
            Task t = n.belief($$("(--x==>y)"));
            assertNotNull(t);
            assertEquals("((--,x)==>y). %1.0;.36%",t.toStringWithoutBudget());
        }
    }

    @Test
    void testPolarityPreference() throws Narsese.NarseseException {
        n.input("x. %0.05%");
        n.input("x. %0.95%");
        n.input("y.");

//        var results = new HashBag();
        for (int i = 0; i < 10; i++) {
            assertNotNull(n.belief($$("(x==>y)")));
        }
//        System.out.println(results.toStringOfItemToCount());
    }

    @Test void compareDynamicAndDerivedImplTruth() {

        float xf = 0.9f, xc = 0.8f;
        long xw = 0;
        long yw = 3;

        Term xy = $$("(x ==>+3 y)");

        Concept XY = n.conceptualizeDynamic(xy);
        assertNotNull(XY);

        n.inputAt(xw, "x. | %" + xf + ";" + xc + "%"); //0.90;0.8%");
        n.inputAt(yw, "y. | %0.70;0.9%");
        n.run(10);

        /*
            $0.0 (x ==>+3 y). 0 %.70;.39% {3: 1;2}
            $0.0 (y ==>-3 x). 3 %.90;.34% {3: 1;2}
         */


        BeliefTable t = XY.beliefs();
        float dur = n.dur();
        NALTask xy0 =
                n.answer(t, 0, 0, xy, null, dur, NAL.answer.ANSWER_CAPACITY);
        assertNotNull(xy0);
            //((BeliefTables) t).tableFirst(DynamicTruthTable.class)
        assertTrue(xy0.toString().endsWith("(x ==>+3 y). 0 %.70;.38%"));

    }

    @Test
    void testInductionNegativeImplSubj() throws Narsese.NarseseException {

        n.input("p. | %0.8;0.9%");
        n.input("n. | %0.2;0.9%"); //n ~= --p
        n.run(1);
        n.input("x. | %0.7;0.9%");
        n.run(1);

        PreciseTruth P = $.t(0.7f, 0.36f);
        Truth a = n.beliefTruth("(  p ==>+1 x)", 0);
        Truth b = n.beliefTruth("(--n ==>+1 x)", 0);
        assertTrue(P.equals(a, TEST_EPSILON*2));
        assertTrue(P.equals(b, TEST_EPSILON*2));

        assertTruthEq(0.7f, 0.09f, "(  n ==>+1 x)", 0);
        //assertTruthEq(0.7f, 0.09f, "(--p ==>+1 x)", 0);
    }

    private void assertTruthEq(float f, float c, String t) throws Narsese.NarseseException {
        assertTruthEq(f, c, t, ETERNAL);
    }

    private void assertTruthEq(float f, float c, String t, long when) throws Narsese.NarseseException {
        Truth truth = n.beliefTruth(t, when);
        assertTrue($.t(f, c).equals(truth, TEST_EPSILON), t);
    }


}