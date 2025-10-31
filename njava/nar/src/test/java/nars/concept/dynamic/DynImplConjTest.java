package nars.concept.dynamic;

import jcog.Fuzzy;
import jcog.data.list.Lst;
import nars.*;
import nars.table.dynamic.DynBeliefTable;
import nars.time.Tense;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.Op.BELIEF;
import static nars.truth.dynamic.DynImplConj.DynImplDisjMixSubj;
import static org.junit.jupiter.api.Assertions.*;

abstract class DynImplConjTest extends AbstractDynTaskTest {

    static class DynImplSubjConjTest extends DynImplConjTest {
        @Disabled @Test
        void DynamicImplSubjTemporalExact() {
            testDynamicImplSubjPredTemporalExact(1, false /* reversed */);
        }

        @Disabled @Test
        void DynamicImplSubjNegTemporalExact() {
            testDynamicImplSubjPredTemporalExact(-1, false /* reversed */);
        }

        @ParameterizedTest //,"(--(--a &&+- --b) ==> x)", //HACK
        //"(--(--a &&+- --b) ==>+- x)" //HACK
        //            "((a ||+- b) ==> x)",
        //            "((a ||+- b) ==>+- x)"
        @ValueSource(strings = "((a || b) ==>+- x)")
        void XternalSubj(String s) throws Narsese.NarseseException {
            n.believe("(a ==> x)");
            n.believe("(b ==> x)");
            assertBelief(s,
                Op.ETERNAL, "((a||b)==>x)", 1, 0.81f, n
            );

        }

        @Test
        void DynamicImplSubj() {
            n.believe("(x ==> a)", 1f, 0.9f);
            n.believe("(y ==> a)", 1f, 0.9f);
            //            n.believe("a:y", 1f, 0.9f);
            //            n.believe("a:(--,y)", 0f, 0.9f);
            n.believe("(z ==> a)", 0f, 0.9f);
            n.believe("(w ==> a)", 0f, 0.9f);
            n.believe("(--z ==> a)", 0.75f, 0.9f);
            //            n.believe("a:(--,z)", 1f, 0.9f);
            //            n.believe("x:b", 1f, 0.9f);
            //            n.believe("y:b", 1f, 0.9f);
            //            n.believe("z:b", 0f, 0.9f);
            n.run(2);
            for (long now : new long[]{0, n.time() /* 2 */, Op.ETERNAL}) {

//                Term conjPP = $$("((x && y) ==> a)");
//                assertTrue(isDynamicBeliefTable(conjPP));
//                Term conjPN = $$("((x && z) ==> a)");
//                assertTrue(isDynamicBeliefTable(conjPN));
//                assertEquals($.t(1f, 0.81f), n.beliefTruth(conjPP, now));
//                assertEquals($.t(1f, 0.81f), n.beliefTruth(conjPN, now));
//                {
//                    Term pnnConj = $$("((x && --z) ==> a)");
//                    Truth pnnConjTruth = n.beliefTruth(pnnConj, now);
//                    assertEquals($.t(/* union */ 1f, 0.81f), pnnConjTruth);
//                }

                Term disjPP = $$("((x || y) ==> a)");
                assertTrue(isDynamicBeliefTable(disjPP));
                Term disjPN = $$("((x || z) ==> a)");
                assertTrue(isDynamicBeliefTable(disjPN));
                assertEquals($.t(1f, 0.81f), n.beliefTruth(disjPP, now));
                assertNull(n.beliefTruth(disjPN, now));
                //assertEquals($.t(0f, 0.81f), n.beliefTruth(disjPN, now));


                {
                    Term pnnDisj = $$("((x || --z) ==> a)");
                    Truth pnnDisjTruth = n.beliefTruth(pnnDisj, now);
                    assertTrue(
                        //$.t(/* intersection */ 0.75f, 0.81f),
                        $.t(/* intersection */ 0.88f, 0.61f).equals(
                             pnnDisjTruth, 0.01f));
                }


//                assertEquals($.t(0f, 0.81f), n.beliefTruth($$("((z && w) ==> a)"), now));
                assertEquals($.t(0f, 0.81f), n.beliefTruth($$("((z || w) ==> a)"), now));
//                assertNull(n.beliefTruth($$("((--z && --w) ==> a)"), now));
                assertNull(n.beliefTruth($$("((--z || --w) ==> a)"), now));

                //OR
                Term NppAndNeg = $$("(--(--x && --y) ==> a)");
                assertEquals($.t(1f, 0.81f), n.beliefTruth(NppAndNeg, now));

                Term NppOrPos = $$("((x || y) ==> a)");
                assertEquals($.t(1f, 0.81f), n.beliefTruth(NppOrPos, now));


                //Unknowable cases
                Term NppOrPosNeg = $$("((x || --y) ==> a)");
                assertNull(n.beliefTruth(NppOrPosNeg, now));


                Term NppAndPos = $$("(--(x && y) ==> a)");
                assertNull(n.beliefTruth(NppAndPos, now));


                Term NppOrNeg = $$("((--x || --y) ==> a)");
                assertNull(n.beliefTruth(NppOrNeg, now));


                //                assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((x&z)-->a)")), now));
            }

        }

        @Test void subjDisjSubj1() throws Narsese.NarseseException {
            n.believe("(a ==> x)"); n.believe("(b ==> x)");
            assertBelief(Op.ETERNAL, "((a||b)==>x)",
                    1, 0.81f,
                    n
            );
        }

        @Disabled @Test void subjInhDisjSubj1() throws Narsese.NarseseException {
            n.believe("((z-->a) ==> x)"); n.believe("((z-->b) ==> x)");
            assertBelief(Op.ETERNAL, "((--,(z-->((--,a)&&(--,b))))==>x)",
                    1, 0.81f,
                    n
            );
        }
        @Disabled @Test void subjInhConjPred1() throws Narsese.NarseseException {
            n.believe("(x==>(z-->a))"); n.believe("(x ==> (z-->b))");
            assertBelief(Op.ETERNAL, "(x==>(z-->(a&&b)))",
                    1, 0.81f,
                    n
            );
        }
        @Disabled @Test void subjInhDisjPred1() throws Narsese.NarseseException {
            n.believe("(x==>--(z-->a))"); n.believe("(x ==> --(z-->b))");
            assertBelief(Op.ETERNAL, "(x==>(z-->((--,a)&&(--,b))))",
                    1, 0.81f,
                    n
            );
        }

        /**
         * subject: &&=union, ||=intersection
         */
        @Disabled @Test
        void DifferenceBetweenIntersectionAndUnionInSubj1() throws Narsese.NarseseException {
            n.believe("(a ==> x)"); n.believe("(b ==> x)");
            assertBelief(Op.ETERNAL, "((a&&b)==>x)",
                    1, 0.81f,
                    n
            );
        }

        @Disabled @Test
        void implDTShift_Subj_eternal_pred_conj() throws Narsese.NarseseException {
            n.believe("(a ==>+2 (x &&+1 y))");
            n.believe("(b ==>+1 (x &&+1 y))");
            assertBelief("((a &&+- b) ==>+- (x &&+- y))", Op.ETERNAL, "((a &&+1 b) ==>+1 (x &&+1 y))",
                    1, 0.81f,
                    n
            );
        }

        @Disabled @Test
        void implDTShift_Subj_eternal() throws Narsese.NarseseException {
            n.believe("(a ==>+2 x)");
            n.believe("(b ==>+1 x)");
            assertBelief("((a &&+- b) ==>+- x)", Op.ETERNAL, "((a &&+1 b) ==>+1 x)",
                    1, 0.81f,
                    n
            );
        }

        @Disabled @Test
        void DifferenceBetweenIntersectionAndUnionInSubj2() throws Narsese.NarseseException {
            n.believe("--(a ==> x)"); n.believe("--(b ==> x)");
            assertBelief(Op.ETERNAL, "((a&&b)==>x)",
                    0, 0.81f,
                    n
            );
        }
    }

    static class DynImplPredConjTest extends DynImplConjTest {
        @Test
        void eligibleDynamicImpl() {
            assertDynamicBeliefTable("((x && y) ==> a)");
            assertDynamicBeliefTable("(a ==> (x && y))");
            assertDynamicBeliefTable("(((x,#1) && y) ==> a)"); //#1 not shared between components
        }

        @Disabled
        @Test
        void ineligibleDynamicImpl1() {
            assertNotDynamicTable("(((x,#1) && (y,#1)) ==> a)"); //depvar shared between terms
            assertNotDynamicTable("((#1 && (y,#1)) ==> a)"); //raw depvar componnet
            assertNotDynamicTable("(((x,$1) && y) ==> (a,$1))"); //indepvar shared between subj and impl

            assertNotDynamicTable("(((x,#1) && (y,#2)) ==> z(#2))"); //depvar imbalance, unnormalizable in some cases
            assertNotDynamicTable("(((x,#1) && (y,#2)) ==> z(#1))"); //depvar imbalance, unnormalizable in some cases
        }


        @Disabled @Test
        void eligibleDynamicImpl2() {
            assertNotDynamicTable("(((x,#1) && (y,#2)) ==> z)"); //depvar unique between subj components
            assertNotDynamicTable("(((x,#1) && (y,#2)) ==> (z,#3))"); //depvar unique between subj components
            assertNotDynamicTable("(((x,#1) && (y,#2)) ==> (z,#1))"); //depvar unique between subj components
        }

        /** depvar shared between subj and impl disqualifies most strategies */
        @Disabled @Test void eligibleDynamicImpl3() {
            String t = "(--((x,#1) && y) ==> (a,#1))";
//            assertFalse(
//                    DynConj.condDecomposeable($$c(s.unneg(), true, 2 /* because of TruthFunctions.mix non-associativity */)
            var y = dynTable(n, $$c(t), true);
            assertNotSame(DynImplDisjMixSubj, y); //depvars disqualify it.
            //assertSame(DynImplContra.DynImplContraposition, y);
        }

        @Disabled @Test
        void eligibleDynamicImpl4() {
            assertDynamicBeliefTable("(((x,#1) && (y,#1)) ==> (a,#1))"); //all share the variable so it could be
        }



        @Test
        void DynamicImplPredTemporalExact() {
            testDynamicImplSubjPredTemporalExact(2, true);
        }

        @Test
        void DynamicImplPredNegTemporalExact() {
            testDynamicImplSubjPredTemporalExact(-2, false);
        }




        @ParameterizedTest @ValueSource(strings={
                "(x ==>+- (a && b))",
                "(x ==>+- (a &&+- b))",
                "(x ==>+- (a &&+- b))"})
        void XternalPred(String s) throws Narsese.NarseseException {
            n.believe("(x ==> a)"); n.believe("(x ==> b)");
            assertBelief(s, Op.ETERNAL, "(x==>(a&&b))",
                1, 0.81f,
                n
            );
        }

//    @Test
//    void ReconstructImplConjTemporal() {
//        //((((left-right)-->fz) &&+6555 (left-->fz))==>(right-->fz)). -280⋈41620 %.14;.24%
//        //((--,(left-->fz)) ==>+300 (right-->fz)). 7460⋈44240 %.40;.43%
//        //DONT produce: ((((left-right)-->fz) &&+6555 (left-->fz))&&(--,(left-->fz)))
//    }

//
//    @Test
//    void DynamicConjunctionFactoredInImpl() throws Narsese.NarseseException {
//        NAR n = n;
//        n.believe($("(x==>a)"), ETERNAL);
//        n.believe($("(y ==>+2 a)"), 0);
//        n.believe($("(z =|> a)"), 2);
//        n.time.dur(8);
//
//        {
//            Compound xyz = $("((x &&+2 z)==>a)");
//            assertEquals(
//                    "[(x ==>+2 a) @ 0..0, (z==>a) @ 0..0]",
//                    components(DynamicStatementTruth.ImplSubjConj, xyz, 0, 0).toString());
//        }
//        {
//            Compound xyz = $("((x &&+2 z) ==>+- a)");
//            assertEquals(
//                    "[(x ==>+- a) @ 0..0, (z ==>+- a) @ 0..0]",
//                    components(DynamicStatementTruth.ImplSubjConj, xyz, 0, 0).toString());
//        }
//        {
//
//            Compound xyz = $("((x && (y &&+2 z))==>a)");
//            assertEquals(
//                    "[((x&&y) ==>+2 a) @ 0..0, ((x&&z)==>a) @ 0..0]",
//                    //"[(x ==>+2 a) @ 0..0, (y ==>+2 a) @ 0..0, (z==>a) @ 0..0]",
//                    components(DynamicStatementTruth.ImplSubjConj, xyz, 0, 0).toString());
//            Task t = n.answer(xyz, BELIEF, 0);
//            assertNotNull(t);
//            assertEquals(xyz, t.term());
//        }
//    }

        /**
         * predicate: &&=intersection, ||=union
         */
        @Test
        void DifferenceBetweenIntersectionAndUnionInPred1() throws Narsese.NarseseException {
            n.believe("(x ==> a)"); n.believe("(x ==> b)");
            assertBelief(Op.ETERNAL, "(x==>(a&&b))",
                    1, 0.81f,
                    n
            );
        }
        @Test
        void DifferenceBetweenIntersectionAndUnionInPred2() throws Narsese.NarseseException {
            n.believe("--(x ==> a)"); n.believe("(x ==> b)");
            assertBelief(Op.ETERNAL, "(x==>(a&&b))",
                    0, 0.81f,
                    n
            );
        }
        @Test
        void DifferenceBetweenIntersectionAndUnionInPred3() throws Narsese.NarseseException {
            n.believe("--(x ==> a)");
            n.believe("(x ==> b)");
            assertBelief(Op.ETERNAL, "(x==>((--,a)&&b))",
                    1, 0.81f,
                    n
            );
        }
        @Test
        void DifferenceBetweenIntersectionAndUnionInPred4() throws Narsese.NarseseException {
            n.believe("--(x ==> a)");
            n.believe("(x ==> b)");
            //"(x==>(a||b))":
            assertBelief(Op.ETERNAL, "(x==>((--,a)&&(--,b)))",
                    0, 0.81f,
                    n
            );
        }


        @Test
        void implDTShiftPred_eternal() throws Narsese.NarseseException {
            n.believe("(x ==>+1 a)"); n.believe("(x ==>+2 b)");
            assertBelief("(x ==>+- (a &&+- b))", Op.ETERNAL, "(x ==>+1 (a &&+1 b))",
                    1, 0.81f,
                    n
            );
        }

        @Test
        void implDTShiftPred_temporal() {
            n
                    .believe("(x ==>+1 a)", 1f, 0.9f, 1, 1);
            n
                    .believe("(x ==>+2 b)", 1f, 0.9f, 1, 1);

            //temporal, but same time, so effectively no projection necessary
            assertBelief("(x ==>+- (a &&+- b))", 1, "(x ==>+1 (a &&+1 b))",
                    1, 0.81f,
                    n
            );
        }
        @Test
        void implDTShiftPred_temporal_subj_conj() {
            n
                    .believe("((x &&+1 y) ==>+1 a)", 1f, 0.9f, 1, 1);
            n
                    .believe("((x &&+1 y) ==>+2 b)", 1f, 0.9f, 1, 1);
            assertBelief("((x &&+- y) ==>+- (a &&+- b))", 1,
                    "((x &&+1 y) ==>+1 (a &&+1 b))",
                    1, 0.81f,
                    n
            );
        }

        @Test
        void implDTShiftPred_temporal_subj_conj_neg_dt() {
            n
                    .believe("((x &&+1 y) ==>-2 a)", 1f, 0.9f, 1, 1);
            n
                    .believe("((x &&+1 y) ==>-1 b)", 1f, 0.9f, 1, 1);
            assertBelief("((x &&+- y) ==>+- (a &&+- b))", 1, "((x &&+1 y) ==>-2 (a &&+1 b))",
                    1, 0.81f,
                    n
            );
        }

        @Test
        void implDTShiftPred_temporal_subj_conj_neg_subj() {
            n
                    .believe("(--(x &&+1 y) ==>-2 a)", 1f, 0.9f, 1, 1);
            n
                    .believe("(--(x &&+1 y) ==>-1 b)", 1f, 0.9f, 1, 1);
            assertBelief("(--(x &&+- y) ==>+- (a &&+- b))", 1,
                    "((--,(x &&+1 y)) ==>-2 (a &&+1 b))",
                    1, 0.81f,
                    n
            );
        }

        @Test
        void implDTShiftPred_temporal_projecting() {
            n.believe("(x ==>+1 a)", 1, 0.9f, 1, 1);
            n.believe("(x ==>+2 b)", 1, 0.9f, 2, 2);

            //projection must be involved, so confidence should be lower
            assertBelief("(x ==>+- (a &&+- b))", 1, 2,
                    "(x ==>+1 (a &&+1 b))",
                    1, 0.76f,
                    n
            );
        }

    }

    static void testDynamicImplSubjPredTemporalExact(int mode, boolean truthIntersectOrUnion) {
        assert (mode != 0);
        List<String> todo = new Lst();

        int[] ii = {1, 0, 2, -2, -1, 3, -3, Op.DTERNAL, Op.XTERNAL};
        int[] oo = {1, 0, 2, -2, -1, 3, -3, Op.DTERNAL, Op.XTERNAL};
        for (int outer : oo) {
            for (int inner : ii) {


                int XA, YA, XY;
                if (inner != Op.XTERNAL && outer != Op.XTERNAL && inner != Op.DTERNAL && outer != Op.DTERNAL) {
                    XA = inner >= 0 ? outer + inner : outer - inner;
                    YA = inner >= 0 ? XA - inner : XA + inner;
                    XY = XA - outer;
                } else if (inner == Op.XTERNAL || outer == Op.XTERNAL) {
                    todo.add(inner + " " + outer); //throw new TODO();
                    continue;
                } else {
                    if (inner == Op.DTERNAL) {
                        if (outer == Op.DTERNAL) {
                            XA = YA = XY = Op.DTERNAL;
                        } else {
                            XY = 0; // DTERNAL; ?
                            XA = YA = outer;
                        }
                    } else {
                        todo.add(inner + " " + Op.DTERNAL); //throw new TODO();
                        continue;
                        //                        XA = XY = inner;
                        //                        YA = DTERNAL;
                        //                        xy = "((x &&+1 y)==>a)"; //override
                    }
                }
                String x, y, xy;

                switch (mode) {
                    case +1 -> {
                        x = dtdt("(x ==>" + dtStr(XA) + " a)");
                        y = dtdt("(y ==>" + dtStr(YA) + " a)");
                        xy = dtdt("((x &&" + dtStr(XY) + " y) ==>" + dtStr(YA) + " a)");
                    }
                    case -1 -> {
                        x = dtdt("(x ==>" + dtStr(XA) + " a)");
                        y = dtdt("(y ==>" + dtStr(YA) + " a)");
                        xy = dtdt("((--,((--,x) &&" + dtStr(XY) + " (--,y))) ==>" + dtStr(YA) + " a)");
                    }
                    case 2 -> {
                        x = dtdt("(a ==>" + dtStr(XA) + " x)");
                        y = dtdt("(a ==>" + dtStr(YA) + " y)");
                        xy = dtdt("(a ==>" + dtStr(YA) +
                                (XA <= YA ?
                                        " (x &&" + dtStr(XY) + " y))" :
                                        " (y &&" + dtStr(XY) + " x))"));
                    }
                    case -2 -> {
                        x = dtdt("(a ==>" + dtStr(XA) + " x)");
                        y = dtdt("(a ==>" + dtStr(YA) + " y)");
                        xy = dtdt("(a ==>" + dtStr(YA) +
                                //" ((--,x) &&" + dtStr(XY) + " (--,y)))"
                                (XA <= YA ?
                                        " ((--,x) &&" + dtStr(XY) + " (--,y)))" :
                                        " ((--,y) &&" + dtStr(XY) + " (--,x)))")
                        );
                    }
                    default -> throw new UnsupportedOperationException();
                }


                Term pt_p = $$(xy);
                assertEquals(xy, pt_p.toString());
                assertEquals(x, $$(x).toString());
                assertEquals(y, $$(y).toString());


                testImpl(mode, outer, inner, x, y, xy, pt_p, truthIntersectOrUnion);
            }
        }

//        System.err.println("TODO:");
//        for (String s : todo) {
//            System.err.println(s);
//        }

        //assert (todo.isEmpty());

        //Term p_tp = $$("((x && y) ==>+1 a)");
        //Term pttp = $$("((x &&+1 y) ==>+1 a)");
    }

    private static void testImpl(int mode, int outer, int inner, String x, String y, String xy, Term pt_p, boolean truthIntersectOrUnion) {
        String cccase = Tense.dtStr(inner) + '\t' + Tense.dtStr(outer) + "\t\t" + x + '\t' + y + '\t' + xy;
        //System.out.println(cccase);

        for (float xf : new float[]{1, 0}) {
            for (float yf : new float[]{1, 0}) {
                NAR n = NARS.shell();
                //n.log();
                n.believe(x, xf, 0.9f);
                n.believe(y, yf, 0.9f);

                //assertNotNull(((BeliefTables) n.conceptualizeDynamic(pt_p).beliefs()).tableFirst(DynTruthBeliefTable.class));

                //match first then concept(), tests if the match was enough to conceptualize

                NALTask task = n.answer(pt_p, BELIEF, 0);
                assertNotNull(task);

                assertEquals(pt_p.toString(), task.term().toString(), cccase);
                assertEquals(2, task.stamp().length, cccase);

                Truth truth = n.truth(pt_p, BELIEF == BELIEF, 0, 0, 0);
                assertEquals(truth, task.truth(), cccase);


                float fxy = truthIntersectOrUnion ? Fuzzy.and(xf, yf) : Fuzzy.or(xf, yf);
                if (mode == -2) {
                    //negated pred
                    fxy = 1f - fxy;
                }
                assertEquals(fxy, task.freq(), 0.01f, () -> cccase + "\n\tin: " + task);

				assertEquals(0.81f, (float) task.conf(), 0.01f, cccase);
            }
        }
    }

    private static String dtdt(String xy) {
        xy = xy.replace(" ==>+0 ", "==>");
        xy = xy.replace(" &&+0 ", "&&");
        xy = xy.replace("x && y", "x&&y");
        xy = xy.replace(" ==> ", "==>");
        xy = xy.replace("(--,((--,x)&&(--,y)))", "(x||y)");
        xy = xy.replace("(--,((--,x) && (--,y)))", "(x||y)");
        xy = xy.replace("((--,x) && (--,y))", "((--,x)&&(--,y))");
        return xy;
    }

    private static String dtStr(int dt) {
        return switch (dt) {
            case Op.DTERNAL -> "";
            case Op.XTERNAL -> "+-";
            default -> (dt >= 0 ? "+" : "") + (dt);
        };
    }

    private static Task assertBelief(long when, String answerTermExpected, float freqExpected, float confExpected, NAR n) {
        return assertBelief(when, when, answerTermExpected, freqExpected, confExpected, n);
    }

    private static Task assertBelief(long s, long e, String answerTermExpected, float freqExpected, float confExpected, NAR n) {
        return assertBelief(answerTermExpected, s, e, answerTermExpected, freqExpected, confExpected, n);
    }

    private static Task assertBelief(String inputTerm, long when, String answerTermExpected, float freqExpected, float confExpected, NAR n) {
        return assertBelief(inputTerm, when, when, answerTermExpected, freqExpected, confExpected, n);
    }

    private static Task assertBelief(String inputTerm, long start, long end, String answerTermExpected, float freqExpected, float confExpected, NAR n) {
        Term x = $$(inputTerm);
        assertInstanceOf(DynBeliefTable.class, dynTable(n, x, true));

        NALTask t = n.answer(x, BELIEF, start, end);
        assertNotNull(t, ()->x + " answers null @ " + start);
        assertEquals(answerTermExpected, t.term().toString());

        assertEquals(start, t.start());
        assertEquals(end, t.end());

        assertTrue(1 < t.stamp().length);
        assertEquals(freqExpected, t.truth().freq(), 0.01f);
		assertEquals(confExpected, (float) t.truth().conf(), 0.01f);
        return t;
    }

    void assertNotDynamicTable(String t) {
        assertFalse(isDynamicBeliefTable(t));
    }

}