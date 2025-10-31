package nars.term.util;

import jcog.random.XoRoShiRo128PlusRandom;
import nars.*;
import nars.concept.TaskConcept;
import nars.table.BeliefTables;
import nars.table.eternal.EternalTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.Compound;
import nars.term.atom.Bool;
import nars.term.util.conj.ConjList;
import nars.time.Tense;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.term.util.Testing.assertEq;
import static nars.time.Tense.Eternal;
import static nars.time.Tense.Present;
import static org.junit.jupiter.api.Assertions.*;

public class IntermpolationTest {

    final NAR n = NARS.shell();

    private static long dtDiff(String x, String y) {
        return Intermpolate.dtDiff($$(x), $$(y));
    }

    static void permuteChoose(Compound a, Compound b, String expected) {
        assertEquals(expected, permuteIntermpolations(a, b).toString());
    }

    static Set<Term> permuteIntermpolations(Compound a, Compound b) {
        assertEquals(a.op(),a.op());
        //assertEquals(a.root(), b.root(), () -> "concepts differ: " + a + ' ' + b);

        long ab = Intermpolate.dtDiff(a, b);
        long ba = Intermpolate.dtDiff(b, a);
        assertTrue(ab!=Long.MAX_VALUE, () -> "dtDiff(" + a + "," + b + ")=" + ab);
        assertEquals(ab, ba, () -> "commutivity violation: " + ab + " != " + ba); //commutative

        int dtDither = 1;

        Random rng = new XoRoShiRo128PlusRandom(1);

        Set<Term> ss = new TreeSet();
        int n = 10 * (a.complexity() + b.complexity());
        for (int i = 0; i < n; i++) {
            float r = rng.nextFloat();
            Term y = new Intermpolate(r, dtDither).get(a, b);
            if (!(y instanceof Bool)) {
                assertEquals(a.opID(), y.opID(), () -> a + " + " + b + " @ " + r);
                ss.add(y);
            }
        }

        return ss;
    }

    @Deprecated public static Term intermpolate(Compound a, Compound b, float aProp, NAL nar) {
        return new Intermpolate(aProp, nar.timeRes()).get(a, b);
    }

    @Test void DTSimilarity() {
        assertEquals(Intermpolate.dtDiff(0, 1), Intermpolate.dtDiff(0, -1));
        assertTrue(Intermpolate.dtDiff(0, 1) < Intermpolate.dtDiff(0, 2));
        assertTrue(Intermpolate.dtDiff(1,2) < Intermpolate.dtDiff(1,4));
        assertTrue(Intermpolate.dtDiff(1,2) < Intermpolate.dtDiff(1,-1));

        for (String o : new String[] { "==>", "&&"}) {
            Term  a = $$("(x " + o + "+1 y)");
            Term  b = $$("(x " + o + "+2 y)");
            Term nb = $$("(x " + o + "-2 y)");
            Term  c = $$("(x " + o + "+4 y)");

            long ab = Intermpolate.dtDiff(a, b);
            long ac = Intermpolate.dtDiff(a, c);
            assertTrue(ab < ac);

            long bc = Intermpolate.dtDiff(b, c);
            assertEquals(2, bc, ()->b + " " + c + " -> " + bc);
            long bnb = Intermpolate.dtDiff(b, nb);
            assertEquals(4, bnb, ()->b + " " + nb + " -> " + bnb);
            assertTrue(bc < bnb);
        }
    }

    @Test void conjReverse() {
        assertEquals(2, dtDiff("(x &&+1 y)", "(x &&-1 y)"));
        assertEquals(2, dtDiff("(x &&+1 y)", "(x &&-1 y)"));
        assertEquals(2, dtDiff("((x &&+1 y) ==>+1 z)", "((x &&-1 y) ==>+1 z)"));

    }
    @Test void implReverse() {
        assertEquals(2, dtDiff("(x ==>+1 y)", "(x ==>-1 y)"));
        assertEquals(Long.MAX_VALUE, dtDiff("(x ==>+1 y)", "(y ==>+1 x)"));
    }

    @Test
    void testDTDiffSame() {
        assertEquals(0f, dtDiff("(x ==>+5 y)", "(x ==>+5 y)"));
    }

    @Test
    void testDTDiffVariety() {
        assertEquals(0f, dtDiff("(x ==>+5 y)", "(x ==>+- y)"), 0.01f);
        assertEquals(5f, dtDiff("(x ==>+5 y)", "(x ==> y)"), 0.01f);
        assertEquals(10f, dtDiff("(x ==>+5 y)", "(x ==>-5 y)"), 0.01f);
        assertEquals(2f, dtDiff("(x ==>+5 y)", "(x ==>+3 y)"), 0.01f);
        assertEquals(4f, dtDiff("(x ==>+5 y)", "(x ==>+1 y)"), 0.01f);
    }

    @Test
    void testDTImpl1() {
        long a52 = dtDiff("(x ==>+5 y)", "(x ==>+2 y)");
        long a54 = dtDiff("(x ==>+5 y)", "(x ==>+4 y)");
        assertTrue(a52 > a54);
    }

    @Test void SimilarConj() {
        assertTrue(Long.MAX_VALUE!=dtDiff("(&&,a,b,c)","((b&|c)&&a)"));
    }

    @Test
    void testConjSequence1() {
        long a52 = dtDiff("((x &&+5 y) &&+1 z)", "((x &&+2 y) &&+1 z)");
        long a54 = dtDiff("((x &&+5 y) &&+1 z)", "((x &&+4 y) &&+1 z)");
        assertTrue(a52 > a54);
    }

    @Test
    void testConjSequence1_SubEventNeg() {
        long a52 = dtDiff("(--(x &&+5 y) &&+1 z)", "(--(x &&+2 y) &&+1 z)");
        long a54 = dtDiff("(--(x &&+5 y) &&+1 z)", "(--(x &&+4 y) &&+1 z)");
        assertTrue(a52 > a54);
    }
    @Test
    void testConjSequence1_SubEventXternal() {
        long a52 = dtDiff("((x &&+- y) &&+1 z)", "((x &&+- y) &&+2 z)");
        long a54 = dtDiff("((x &&+- y) &&+1 z)", "((x &&+- y) &&+1 z)");
        assertTrue(a52 > a54);
    }

    @Test
    void testDTImplEmbeddedConj() {
        long a = dtDiff("((x &&+1 y) ==>+1 z)", "((x &&+1 y) ==>+2 z)");
        long b = dtDiff("((x &&+1 y) ==>+1 z)", "((x &&+2 y) ==>+1 z)");
        assertTrue(a >= b);
    }

    @Test
    void testDTImplEmbeddedConj_SubjNeg() {
        long a = dtDiff("(--(x &&+1 y) ==>+1 z)", "(--(x &&+1 y) ==>+2 z)");
        long b = dtDiff("(--(x &&+1 y) ==>+1 z)", "(--(x &&+2 y) ==>+1 z)");
        assertTrue(a >= b);
    }

//    @Test
//    void testIntermpolation0() throws Narsese.NarseseException {
//        Compound a = $.$$c("((a &&+3 b) &&+3 c)");
//        Compound b = $.$$c("((a &&+3 b) &&+1 c)");
//        permuteChoose(a, b,
//                "[((a &&+3 b) &&+1 c), ((a &&+3 b) &&+2 c), ((a &&+3 b) &&+3 c)]"
//                //"[((a &&+3 b) &&+1 (c &&+2 c))]"
//        );
//    }
    @Test
    void testIntermpolationNegConj() {
        Compound a = $$c("(x &&+1 y)");
        Compound b = $$c("(x &&+2 y)");
        permuteChoose(a, b,
            "[(x &&+1 y), (x &&+2 y)]"
        );
        permuteChoose((Compound)a.neg(), (Compound)b.neg(),
                "[(--,(x &&+1 y)), (--,(x &&+2 y))]"
        );
    }
    @Test
    void testIntermpolationNegConjInSeq() {
        Compound a = $$c("(--(x &&+1 y) &&+1 c)"); assertEq(a, ConjList.conds(a).term());
        Compound b = $$c("(--(x &&+2 y) &&+1 c)"); assertEq(b, ConjList.conds(b).term());
        Compound c = $$c("(--(x &&+3 y) &&+1 c)"); assertEq(c, ConjList.conds(c).term());

        long ab = Intermpolate.dtDiff(a, b);
        long ac = Intermpolate.dtDiff(a, c);
        assertTrue(ab < ac,
                () -> "fail: " + ab + " < " + ac);

        permuteChoose(a, b,
                "[((--,(x &&+1 y)) &&+1 c), ((--,(x &&+2 y)) &&+1 c)]"
        );
    }
    @Test
    void testIntermpolation0b() {
        Compound a = $$c("(a &&+3 (b &&+3 c))");
        Compound b = $$c("(a &&+1 (b &&+1 c))");
        permuteChoose(a, b,
                "[((a &&+1 b) &&+1 c), ((a &&+1 b) &&+2 c), ((a &&+2 b) &&+1 c), ((a &&+2 b) &&+2 c), ((a &&+2 b) &&+3 c), ((a &&+3 b) &&+2 c), ((a &&+3 b) &&+3 c)]");
    }

    @Test
    void testIntermpolationOrderMismatch() {
        Compound a = $$c("(c &&+1 (b &&+1 a))");
        Compound b = $$c("(a &&+1 (b &&+1 c))");
        permuteChoose(a, b, "[((a &&+1 b) &&+1 c), (b &&+1 (a&|c)), ((a&|c) &&+1 b), ((c &&+1 b) &&+1 a)]");
    }

    @Test
    void testIntermpolationOrderPartialMismatch() {
        Compound a = $$c("(a &&+1 (b &&+1 c))");
        Compound b = $$c("(a &&+1 (c &&+1 b))");
        permuteChoose(a, b, "[((a &&+1 b) &&+1 c), ((a &&+1 c) &&+1 b), (a &&+2 (b&|c)), (a &&+1 (b&|c))]");
    }

    @Test
    void testIntermpolationImplSubjOppositeOrder() {
        Compound a = $$c("((x &&+2 y) ==> z)");
        Compound b = $$c("((y &&+2 x) ==> z)");
        permuteChoose(a, b, "[((x&&y)==>z), ((y &&+2 x)==>z), ((y &&+1 x)==>z), ((x &&+1 y)==>z), ((x &&+2 y)==>z)]");
    }

    @Test
    void testIntermpolationImplSubjOppositeOrder2() {
        Compound a = $$c("((x &&+1 y) ==>+1 z)");
        Compound b = $$c("((y &&+1 x) ==>+1 z)");
        permuteChoose(a, b,
                "[((x&&y) ==>+1 z), ((y &&+1 x) ==>+1 z), ((x &&+1 y) ==>+1 z)]");
    }
    @Test
    void testIntermpolationImplDirectionMismatch() {
        Compound a = $$c("(a ==>+1 b)");
        Compound b = $$c("(a ==>-1 b))");
        permuteChoose(a, b, "[(a==>b), (a ==>-1 b), (a ==>+1 b)]");
    }


    @Test
    void testIntermpolationImplSubjImbalance() {
        Compound a = $$c("((x &&+1 y) ==> z)");
        Compound b = $$c("(((x &&+1 y) &&+1 x) ==> z)");
        permuteChoose(a, b, "[(((x &&+1 y) &&+1 x)==>z), ((x &&+1 y)==>z)]");
    }

    @Test
    void testIntermpolationOrderPartialMismatch2() {
        Compound a = $$c("(a &&+1 (b &&+1 (d &&+1 c)))");
        Compound b = $$c("(a &&+1 (b &&+1 (c &&+1 d)))");
        String expected = "[((a &&+1 b) &&+1 (d &&+1 c)), ((a &&+1 b) &&+1 (c &&+1 d))]";
        permuteChoose(a, b, expected);
    }

    @Test
    void testIntermpolationOrderMixDternalPrePre() {
        Compound x = $$c(
                //"(a &&+1 (b && c))"
                "(a &&+1 (b &&+1 c))"
        );
        Term y = x.concept();
        assertEq("((b &&+- c) &&+- a)", y);
    }
    @Test
    void testIntermpolationOrderMixDternalPre() {
        Compound a = $$c("(a &&+1 (b &&+1 c))");
        Compound b = $$c("(a &&+1 (b && c))");
        permuteChoose(a, b, "[((a &&+1 b) &&+1 c), (a &&+1 (b&&c))]");
    }


    @Test
    void testIntermpolationWrongOrderSoDternalOnlyOption() {
        Compound a = $$c("(((right-->tetris) &&+2 (rotCW-->tetris)) &&+1 (tetris-->[happy]))");

        Compound b = $$c("(((tetris-->[happy]) &&+1 (right-->tetris)) &&+2 (rotCW-->tetris))");

        ConjList ae = ConjList.conds(a);
        ConjList be = ConjList.conds(b);
        assertEquals(
            ae.sortThisByValue().toItemString(),
            be.sortThisByValue().toItemString()
        );

        permuteChoose(a, b, "[(((tetris-->[happy])&&(right-->tetris)) &&+2 (rotCW-->tetris)), (((tetris-->[happy]) &&+1 (right-->tetris)) &&+2 (rotCW-->tetris)), ((right-->tetris) &&+2 ((tetris-->[happy])&&(rotCW-->tetris))), (((right-->tetris) &&+2 (rotCW-->tetris)) &&+1 (tetris-->[happy]))]");

    }

    @Disabled
    @Test
    void testIntermpolationOrderMixDternal2() {
        Compound a = $$c("(a &&+1 (b && c))");
        Compound b = $$c("(a &&+1 (b &&+1 c))");
        permuteChoose(a, b, "[((a &&+1 b) &&+1 c), (a &&+1 (b && c)]");
    }
    @Test
    void testIntermpolationOrderMixDternal3() {
        Compound a = $$c("(a &&+1 (b &&+1 (c &&+1 d)))");
        Compound b = $$c("(a &&+1 (b &&+1 (c&&d)))");
        permuteChoose(a, b, "[((a &&+1 b) &&+1 (c&&d)), ((a &&+1 b) &&+1 (c &&+1 d))]");
    }

    @Test
    void testIntermpolationOrderMixDternal2Reverse() {
        Compound a = $$c("(a &&+1 (b &&+1 (c &&+1 d)))");
        Compound b = $$c("((a && b) &&+1 (c &&+1 d))");
        permuteChoose(a, b, "[(((a&&b) &&+1 c) &&+1 d), ((a &&+1 b) &&+1 (c &&+1 d))]");
    }

    @Test
    void testIntermpolationOrderPartialMismatchReverse() {
        Compound a = $$c("(a &&+1 (b &&+1 c))");
        Compound b = $$c("(b &&+1 (a &&+1 c))");
        assertEquals(2f, Intermpolate.dtDiff(a,b));
        permuteChoose(a, b, "[((b &&+1 a) &&+1 c), ((a &&+1 b) &&+1 c)]");
    }

    @Test
    void testIntermpolationOrderPartialMismatchDternal1a() {
        Compound a = $$c("(a &&+1 (b &&+1 ((&&,c,d,e,f) &&+1 x)))");
        Compound b = $$c("(a &&+1 ((&&,b,c,d,e,f) &&+1 x)))");
        assertEquals(5, Intermpolate.dtDiff(a, b), 0.01f);
        //assertEquals(?, Intermpolate.dtDiffPct(a, b), 0.01f);
    }
    @Test
    void testIntermpolationOrderPartialMismatchDternal1b() {
        Compound a = $$c("(b &&+1 ((&&,c,d,e,f) &&+1 x))");
        Compound b = $$c("((&&,b,c,d,e,f) &&+1 x))");
        assertEquals(0.5f, Intermpolate.dtDiff(a, b), 0.01f);
    }

    @Test
    void testIntermpolationOrderPartialMismatchReverse2() {
        Compound a = $$c("(b &&+1 (a &&+1 (c &&+1 d)))");
        Compound b = $$c("(a &&+1 (b &&+1 (c &&+1 d)))");
        permuteChoose(a, b,
            "[((b &&+1 a) &&+1 (c &&+1 d)), ((a &&+1 b) &&+1 (c &&+1 d))]"
        );
    }
    @Test
    void conjXternalOuter1() {
        Compound a = $$c("((x &&+1 y) &&+- z)");
        Compound b = $$c("((x &&+2 y) &&+- z)");
        permuteChoose(a, b,
        "[((x &&+1 y) &&+- z), ((x &&+2 y) &&+- z)]"
        );
    }
    @Disabled @Test void innerImpl() {
        Compound a = $$c("(((--,R(tetris,a)) ==>+100 ((tetris,a)-->(#1,#2)))&&(cmp(#1,#2)=1))");
        Compound b = $$c("(((--,R(tetris,a)) ==>+200 ((tetris,a)-->(#1,#2)))&&(cmp(#1,#2)=1))");
        assertEq(
            "(((--,R(tetris,a)) ==>+150 ((tetris,a)-->(#1,#2)))&&(cmp(#1,#2)=1))",
                new Intermpolate(0.5f, 1).get(a, b)
        );
    }

    @Test
    void testIntermpolationConj2OrderSwap() {
        Compound a = $$c("(a &&+1 b)");
        Compound b = $$c("(b &&+1 a)");
        Compound c = $$c("(b &&+2 a)");
        permuteChoose(a, b, "[(a&&b), (b &&+1 a), (a &&+1 b)]");
        permuteChoose(a, c, "[(a&&b), (b &&+2 a), (b &&+1 a), (a &&+1 b)]"); //not within dur

    }



    @Test
    void testImplSimple() {
        Compound a = $$c("(a ==>+4 b)");
        Compound b = $$c("(a ==>+2 b))");
        permuteChoose(a, b, "[(a ==>+2 b), (a ==>+3 b), (a ==>+4 b)]");
    }
    @Test
    void testIntermpolationImplDirectionDternalAndTemporal() {
        Compound a = $$c("(a ==>+1 b)");
        Compound b = $$c("(a ==> b))");
        permuteChoose(a, b, "[(a==>b), (a ==>+1 b)]");
    }

    @Test
    void testIntermpolation0invalid() {
        Compound a = $$c("(a &&+3 (b &&+3 c))");
        Compound b = $$c("(a &&+1 b)");
        try {
            Set<Term> p = permuteIntermpolations(a, b);
            fail("");
        } catch (Error e) {
            assertTrue(true);
        }
    }

    @Test
    void testIntermpolationConjSeq_2_ary() {
        Compound f = $$c("(a &&+1 b)");
        Compound g = $$c("(a &&-1 b)");
        permuteChoose(f, g, "[(a&&b), (b &&+1 a), (a &&+1 b)]");
    }
    @Test
    void testIntermpolationConjSeq_3_ary_2_inner_different() {
        Compound f = $$c("((a &&+1 b) &&+1 c)");
        Compound g = $$c("((a &&-1 b) &&+1 c)");
        permuteChoose(f, g, "[((a&&b) &&+1 c), ((b &&+1 a) &&+1 c), ((a &&+1 b) &&+1 c)]");
    }
    @Test
    void testIntermpolationConjSeq_3_ary_2_outer_different() {
        Compound f = $$c("((a &&+1 b) &&+1 c)");
        Compound g = $$c("((a &&+1 b) &&+2 c)");
        permuteChoose(f, g,
                "[((a &&+1 b) &&+1 c), ((a &&+1 b) &&+2 c)]");
    }

    @Test
    void testIntermpolationConjSeqFactored() {
        Compound f = $$c("(x && (a &&+1 b))");
        Compound g = $$c("(x && (a &&-1 b))");
        permuteChoose(f, g, "[((b&&x) &&+1 (a&&x)), ((a&&x) &&+1 (b&&x)), (&&,a,b,x)]");
    }
    @Test
    void testIntermpolationConjSeqFactored_2simple() {
        Compound f = $$c("(a &&+1 b)");
        Compound g = $$c("(a &&+2 b)");
        permuteChoose(f, g, "[(a &&+1 b), (a &&+2 b)]");
    }

    @Test
    void testIntermpolationConjSeqFactored_2() {
        Compound f = $$c("(x && (a &&+1 b))");
        Compound g = $$c("(x && (a &&+2 b))");
        permuteChoose(f, g, "[((a&&x) &&+1 (b&&x)), ((a&&x) &&+2 (b&&x))]");
    }

    @Test
    void testIntermpolationConjSeq2() {
        Compound h = $$c("(a &&+1 b)");
        Compound i = $$c("(a && b)");
        permuteChoose(h, i, "[(a&&b), (a &&+1 b)]");
    }

    @Test void intermpolationConjSeq3() throws Narsese.NarseseException {
        //TODO
        assertEquals(10, Intermpolate.dtDiff(
            $.$("((--,((--,(tetris-->left)) &&+170 (--,(tetris-->left))))&&(--,isRow(tetris,(3,TRUE))))"),
            $.$("((--,((--,(tetris-->left)) &&+160 (--,(tetris-->left))))&&(--,isRow(tetris,(3,TRUE))))")
        ));
    }

    @Test
    void testIntermpolationConjInImpl2b() {

        Compound h = $$c("(x==>(a &&+1 b))");
        Compound i = $$c("(x==>(a && b))");

        permuteChoose(h, i, "[(x==>(a&&b)), (x==>(a &&+1 b))]");
    }

    @Disabled @Test
    void testIntermpolationInner() {
        Compound a = $$c("(x --> (a &&+1 b))");
        Term aRoot = a.root();
        assertEq("(x-->(a&&b))", aRoot);
        Compound b = $$c("(x --> (a && b))");
        permuteChoose(a, b, "[(x-->(a&&b)), (x-->(a &&+1 b))]");
    }


    @Test
    void testEmbeddedIntermpolation() {
        n.time.dur(8);

        Compound a0 = $$c("(b ==>+6 c)");
        Compound b0 = $$c("(b ==>+10 c)");

        Term c0 = intermpolate(a0, b0, 0.5f, n);
        assertEquals("(b ==>+8 c)", c0.toString());


        Compound a = $$c("(a, (b ==>+6 c))");
        Compound b = $$c("(a, (b ==>+10 c))");

        Term c = intermpolate(a, b, 0.5f, n);
        assertEquals("(a,(b ==>+8 c))", c.toString());

        {

            assertEquals("(a,(b ==>+6 c))",
                    intermpolate(a, b, 1f, n).toString());
            assertEquals("(a,(b ==>+10 c))",
                    intermpolate(a, b, 0f, n).toString());


        }
    }

    @Disabled @Test void testConceptualizationIntermpolationPresent() throws Narsese.NarseseException {
        testConceptualizationIntermpolation(Present);
    }
    @Disabled @Test void testConceptualizationIntermpolationEternal() throws Narsese.NarseseException {
        testConceptualizationIntermpolation(Eternal);
    }

    private void testConceptualizationIntermpolation(Tense t) throws Narsese.NarseseException {

//        n.time.dur(8);

        int a = 2;
        int b = 4;
        int ab = 3; //expected

        assertEquals(ab, new Intermpolate(0.5f, n.timeRes()).chooseDT(a,b));

        n.believe("((a ==>+" + a + " b)-->[pill])", t, 1f, 0.9f);
        n.believe("((a ==>+" + b + " b)-->[pill])", t, 1f, 0.9f);
        n.run(1);


        assertEquals("(a ==>+- a)", $$("(a ==>+- a)").concept().toString());
        assertEquals("((a ==>+- b)-->[pill])", $$("((a ==>+- b)-->[pill])").concept().toString());
        String abpill = "((a==>b)-->[pill])";
        assertEquals("((a ==>+- b)-->[pill])", $$(abpill).concept().toString());

        TaskConcept cc = (TaskConcept) n.conceptualize(abpill);
        assertNotNull(cc);

//        cc.beliefs().print();

        String correctMerge = "((a ==>+" + ab + " b)-->[pill])";

        {
            //Part 1: ??
            long when = t == Present ? 0 : Op.ETERNAL;
            Task m = n.answer(cc.beliefs(), when, when, null, null, 0, NAL.answer.ANSWER_CAPACITY);
            assertEquals(correctMerge, m.term().toString());
        }

        BeliefTables tables = (BeliefTables) cc.beliefs();

        {
            //Part 2: Answer Intermpolation
            NALTask ct = n.answer(tables, 0, 0, null, null, 0, NAL.answer.ANSWER_CAPACITY);
            assertNotNull(ct);

            assertEquals(correctMerge, ct.term().toString());
        }

        {
            //Part 3:  belief table compression intermpolation
            BeliefTable table = tables.tableFirst(t == Present ? TemporalBeliefTable.class : EternalTable.class);
            assertEquals(2, table.taskCount());
            assertEquals(2, tables.taskCount());
            table.taskCapacity(1);

            assertEquals(1, table.taskCount());
            assertEquals(correctMerge, table.taskStream().findFirst().get().term().toString());
        }
    }
//
//    @Test void intermpolateSeqReverse() {
//        Term a = intermpolate($$("((--,(1-->x)) &&+100 (--,(2-->x)))"), $$("((--,(2-->x)) &&+145 (--,(1-->x)))"), 0.1f, 1);
//        Term b = intermpolate($$("((--,(1-->x)) &&+100 (--,(2-->x)))"), $$("((--,(2-->x)) &&+145 (--,(1-->x)))"), 0.5f, 1);
//        Term c = intermpolate($$("((--,(1-->x)) &&+100 (--,(2-->x)))"), $$("((--,(2-->x)) &&+145 (--,(1-->x)))"), 0.9f, 1);
//        assertEq("", a);
//        assertEq("", b);
//        assertEq("", c);
//
//
//    }
}