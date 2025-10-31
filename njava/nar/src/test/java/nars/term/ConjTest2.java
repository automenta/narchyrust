package nars.term;

import jcog.data.list.Lst;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.Narsese;
import nars.Term;
import nars.term.util.conj.CondDiff;
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjTree;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static nars.$.*;
import static nars.Op.CONJ;
import static nars.Op.ETERNAL;
import static nars.term.atom.Bool.False;
import static nars.term.atom.Bool.True;
import static nars.term.util.Testing.assertEq;
import static nars.term.util.Testing.assertStable;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;
import static org.junit.jupiter.api.Assertions.*;

/** experimental or unknown edge cases, etc.. TODO */
@Disabled class ConjTest2 {
//    static final Term x = $.the("x");
//    static final Term y = $.the("y");
//    static final Term z = $.the("z");
//    static final Term a = $.the("a");
//    static final Term b = $.the("b");

//    @Test void inlineNegSequence() {
//        if (NAL.SEQUENCE_NEG_FLATTEN) {
//            assertEq("((x &&+1 (--,y)) &&+1 (--,z))",
//                    "(x &&+1 --(y &&+1 z))");
//        }
//    }

    @Deprecated
    static Term conj(Lst<LongObjectPair<Term>> events) {
        int eventsSize = events.size();
        switch (eventsSize) {
            case 0:
                return True;
            case 1:
                return events.getFirst().getTwo();
        }

        ConjBuilder ce = new ConjTree();

        for (LongObjectPair<Term> o : events) {
            if (!ce.add(o.getOne(), o.getTwo())) {
                break;
            }
        }

        return ce.term();
    }




    @Test
    void testMergedDisjunction1() {
        assertFalse($$c("(x &&+1 y)").condOf($$("(x&&y)")));

        //simplest case: merge one of the sequence
        Term a = $$("(x &&+1 y)");
        Term b = $$("(x && y)");
        assertEq(
                "((--,((--,y) &&+1 (--,y)))&&x)",
                CONJ.the(a.neg(), 0, b.neg()).neg()
        );
    }


    @Test void testMergedDisjunction2() {
        //simplest case: merge one of the sequence
        Term a = $$("(x &&+10 y)");
        Term b = $$("(x &&-10 y)");
        assertEq(
                "((y &&+10 x)||(x &&+10 y))",
                //"((--,((--,y) &&+20 (--,y))) &&+10 x)",
                CONJ.the(a.neg(), 0, b.neg()).neg()
        );
    }


    @Test
    void testWrappingCommutiveConjunctionX_3() {

        Term xAndRedundantParallel = $$("(((x &| y) &| z)&&x)");
        assertEquals("(&&,x,y,z)",
                xAndRedundantParallel.toString());


        Term xAndContradictParallel = $$("(((x &| y) &| z)&&--x)");
        assertEquals(False, xAndContradictParallel);


        Term xAndContradictParallelMultiple = $$("(&&,x,y,((x &| y) &| z))");
        assertEquals("(&&,x,y,z)",
                xAndContradictParallelMultiple.toString());


        Term xAndContradict2 = $$("((((--,angX) &&+4 x) &&+10244 angX) && --x)");
        assertEquals(False, xAndContradict2);


        Term xAndContradict3 = $$("((((--,angX) &&+4 x) &&+10244 angX) && angX)");
        assertEquals(False, xAndContradict3);


        Term xParallel = $$("((((--,angX) &&+4 x) &&+10244 angX) &| y)");
        assertEquals(False, xParallel);


        Term xParallelContradiction4 = $$("((((--,angX) &&+4 x) &&+10244 angX) &| angX)");
        assertEquals(False, xParallelContradiction4);


        Term x = $$("((((--,angX) &&+4 x) &&+10244 angX) &| angX)");
        Term y = $$("(angX &| (((--,angX) &&+4 x) &&+10244 angX))");
        assertEquals(x, y);

    }


    @Test
    void testXternalInDternal() {
        assertEq(//"((a&&x) &&+- (a&&y))",
                "((x &&+- y)&&a)",
                "((x &&+- y) && a)");
    }

    @Test
    void testXternalInSequence() {
        assertEq("((x &&+- y) &&+1 a)", "((x &&+- y) &&+1  a)");
        assertEq("(a &&+1 (x &&+- y))", "(a &&+1 (x &&+- y))");

        assertEquals(0, $$("(x &&+- y)").seqDur());
        assertEquals(2, $$("((y &&+2 z) &&+- x)").seqDur());


    }
    @Test
    void testXternalInSequence2() {
        assertEq("((x &&+- y)&&a)", "((x &&+- y) &&  a)");
        //assertEq("((x &&+- y)&&a)", "((x &&+- y) &|  a)");
    }

    @Test
    void testConjSeqWtf() {
        Term t = CONJ.the(606,
                $$$("((tetris(#1,13) &&+42 (tetris(#1,13)&&(tetris-->#2)))&&(--,tetris(#3,#_f)))"),
                $$$("(tetris-->#2)"));
        assertEq("(((tetris(#1,13) &&+42 (tetris(#1,13)&&(tetris-->#2)))&&(--,tetris(#3,#_f))) &&+606 (tetris-->#2))",
                t);

//        Term u = t.anon();
//        assertEq("(((_2(#1,_1) &&+42 (_2(#1,_1)&&(_2-->#2)))&&(--,_2(#3,#_f))) &&+606 (_2-->#2))", u);
//        assertEquals(t.volume(), u.volume());

    }


    @Test
    void testDisjunctionInnerDTERNALConj2_simple() {

        assertEq("(x&&y)", "(x && (y||--x))");
        assertEq("(x&&y)", "(x && (y||(--x &&+1 --x)))");

        assertEq("(x&&y)", "(x && (y||--(x &&+1 x)))");

        assertEq("((--,y)&&(--,z))", CondDiff.diffAll($$("((x||(--,z))&&(--,y))"), $$("x")));
        assertEq("((y||z)&&x)", "(x && (y||(--x && z)))"); //and(x, or(y,and(not(x), z)))) = x & y

    }

    @Test
    void testDisjunctionInnerDTERNALConj2() {
        Term x = $$("((x &&+1 --x) && --y)");
        Term xn = x.neg();
        assertEq("((--,(x &&+1 (--,x)))||y)", xn);

        assertEq(
                //"((||,(--,(x &&+1 (--,x)) ),y)&&x)",
                //"x",
                "(x&&y)",
                CONJ.the(xn, $$("x"))
        );

    }


    @Test
    void testAnotherComplexInvalidConj() {
        String a0 = "(((--,((--,_2(_1)) &&+710 (--,_2(_1))))&&(_3-->_1)) &&+710 _2(_1))";
        Term a = $$(a0);
        assertEq(a0, a);
        Term b = $$("(--,(_2(_1)&&(_3-->_1)))");
        assertEq("((_3-->_1) &&+710 _2(_1))", CONJ.the(0, a, b));

    }


    @Test
    void disjunctionSequence_vs_Eternal_Cancellation() {

        for (long t : new long[]{0, 1, ETERNAL}) {
            ConjBuilder c = new ConjTree();
            c.add(t, $$("--(x &&+50 x)"));
            c.add(t, $$("x"));
            Term cc = c.term();
            assertEquals(t == ETERNAL ? False : $$("(x &&+50 (--,x))"), cc, () -> t + " = " + cc);
        }
    }

    @Test
    void xternal_disjunctionSequence_Reduce() {
        ConjBuilder c = new ConjTree();
        c.add(ETERNAL, $$("--(x &&+- y)"));
        c.add(ETERNAL, $$("x"));
        assertEq("((--,y)&&x)", c.term());
    }

    @Test
    void disjunctionSequence_vs_Eternal_Cancellation_mix() {
        {
            //disj first:
            ConjBuilder c = new ConjTree();
            c.add(1, $$("--(x &&+50 x)"));
            c.add(ETERNAL, $$("x"));
            assertEq(False, c.term());
        }
        {
            //eternal first:
            ConjBuilder c = new ConjTree();
            c.add(ETERNAL, $$("x"));
            c.add(1, $$("--(x &&+50 x)"));
            assertEq(False, c.term());
        }


    }


    @Test
    void testSequentialDisjunctionAbsorb3() {
        Term[] a = {
            $$("(--,((--,R) &&+600 jump))"),
            $$("(--,L)"),
            $$("(--,R)")
        };

        assertEq("(&&,(--,((--,R) &&+600 jump)),(--,L),(--,R))", CONJ.the(a));
        assertEq("(((--,L)&&(--,R)) &&+600 (--,jump))", CONJ.the(0,a));
    }

    @Test
    void testDisj_Factorize_2b() {
        assertEq("((a&&b)||(c&&d))", "((a&&b)||(c&&d))");
        assertEq("(((a&&b)||(c&&d))&&x)", "(||,(&&,x,a,b),(&&,x,c,d))");
    }

    /** TODO check */
    @Disabled
    @Test
    void testDisj5() {
        /* (a and x) or (a and y) or (not (a) and z) =
              (¬a ∨ x ∨ y) ∧ (a ∨ z)
              ((||,x,y,(--,a))&&(a||z))
        * */

        assertEq("((||,x,y,(--,a))&&(a||z))", "(||,(a && x),(a && y), (--a&&z))");

        assertEq("((||,x,y,(--,a))&&(a||z))", "((||,x,y,(--,a))&&(a||z))"); //pre-test
    }


    @Test
    void testDisjuncSeq1() {


        //simple case of common event
        Term c = $$("((a &&+1 b) || (a &&+2 b))");
        //assertEq("(a &&+1 (--,((--,b) &&+1 (--,b))))", c);
        assertEq("((a &&+1 b)||(a &&+2 b))", c);
    }

    @Test
    void factorizeInProducts1() {
        assertEq("((&&,b,c,d)||a)", "(( (a||b) && (a||c)) && (a||d))");
    }

    @Test
    void factorizeInProducts2() {

        /* https://github.com/Horazon1985/ExpressionBuilder/blob/master/test/logicalexpression/computationtests/GeneralLogicalTests.java#L109 */
        //LogicalExpression logExpr = LogicalExpression.build("(a|b)&(a|c)&x&(a|d)");
        //LogicalExpression expectedResult = LogicalExpression.build("(a|b&c&d)&x");
        /*
            | (a ∨ b) ∧ (a ∨ c) ∧ x ∧ (a ∨ d)                 | ((b ∧ c ∧ d) ∨ a) ∧ x
        DNF | (a ∧ x) ∨ (b ∧ c ∧ d ∧ x)                       | (a ∧ x) ∨ (b ∧ c ∧ d ∧ x)
        CNF | (a ∨ b) ∧ (a ∨ c) ∧ (a ∨ d) ∧ x                 | (a ∨ b) ∧ (a ∨ c) ∧ (a ∨ d) ∧ x
        ANF | (a ∧ x) ⊻ (b ∧ c ∧ d ∧ x) ⊻ (a ∧ b ∧ c ∧ d ∧ x) | (a ∧ x) ⊻ (b ∧ c ∧ d ∧ x) ⊻ (a ∧ b ∧ c ∧ d ∧ x)
        NOR | (a ⊽ b) ⊽ (a ⊽ c) ⊽ (a ⊽ d) ⊽ ¬x | (a ⊽ b) ⊽ (a ⊽ c) ⊽ (a ⊽ d) ⊽ ¬x
        NAND| (a ⊼ x) ⊼ (b ⊼ c ⊼ d ⊼ x) | (a ⊼ x) ⊼ (b ⊼ c ⊼ d ⊼ x)
        AND | ¬(¬a ∧ ¬b) ∧ ¬(¬a ∧ ¬c) ∧ ¬(¬a ∧ ¬d) ∧ x | ¬(¬a ∧ ¬b) ∧ ¬(¬a ∧ ¬c) ∧ ¬(¬a ∧ ¬d) ∧ x
        OR  | ¬(¬a ∨ ¬x) ∨ ¬(¬b ∨ ¬c ∨ ¬d ∨ ¬x) | ¬(¬a ∨ ¬x) ∨ ¬(¬b ∨ ¬c ∨ ¬d ∨ ¬x)
        (assuming NAND and NOR are n-ary operators)
         */
        Term x = $$("(( (a||b) && (a||c)) && x)");
        Term y = $$("(a||d)");
        assertEq("(((&&,b,c,d)||a)&&x)", CONJ.the(x, y));
        assertEq("(((&&,b,c,d)||a)&&x)", "((( (a||b) && (a||c)) && x) && (a||d))");
    }




    /**
     * these are experimental cases involving contradictory or redundant events in a conjunction of
     * parallel and dternal sub-conjunctions
     * TODO TO BE DECIDED
     */
//    @Disabled
//    private class ConjReductionsTest {


    @Test
    void testConjParaEteReduction2() throws Narsese.NarseseException {
        String o = "(((--,tetris(isRow,2,true))&|tetris(isRowClear,8,true)) ==>-807 (((--,tetris(isRow,2,true))&&tetris(isRowClear,8,true))&|tetris(isRowClear,8,true)))";
        String q = "((((--,(isRow,2,true))&&(isRowClear,8,true))-->tetris) ==>-807 (((--,(isRow,2,true))&&(isRowClear,8,true))-->tetris))";
        Term oo = $(o);
        assertEquals(q, oo.toString());
    }


    @Test
    void testConjNearIdentity() {
        assertEq(True, "( (a&&b) ==> (a&|b) )");

        assertEq(
                "((X,x)&|#1)",
                "( ((X,x)&&#1) &| ((X,x)&|#1) )");

        assertEq("((--,((X,x)&&#1))&|(--,((X,x)&|#1)))", "( (--,((X,x)&&#1)) &| (--,((X,x)&|#1)) )");
    }

    @Test
    void disj_in_conj_common_seq_extraction_fwd2() {

        assertEquals(1, $$("((x &&+1 y) || (x &&+1 z))").seqDur());

        Term a = $$("((x &&+1 y) || (x &&+1 z))");
        assertEq("(x &&+1 (y||z))", a);
        assertEquals(1, a.seqDur());
    }
    @Test
    void disj_in_conj_common_seq_extraction_fwd5() {
        Term a = $$("((x &&+1 y) && (x &&+2 z))");
        assertEquals(2, a.seqDur());
    }

    @Disabled
    @Test
    void disj_in_conj_common_seq_extraction_rev2() {
        //reverse
        Term b = $$("((y &&+1 x) || (z &&+1 x))");
        assertEq("((y||z) &&+1 x)", b);
    }

    @Test
    void disj_in_conj_common_seq_extraction_fwd3() {
        Term a = $$("((x &&+1 (y &&+1 z)) || (x &&+1 (y &&+1 w)))");
        assertEq("((x &&+1 y) &&+1 (w||z))", a);
        assertEquals(2, a.seqDur());
    }

    @Test
    void disj_in_conj_common_seq_extraction_fwd3_partial() {
        //dt differ in last segment, so can not factor out y
        Term a = $$("((x &&+1 (y &&+1 z)) || (x &&+1 (y &&+2 w)))");
        assertEq(
                "(x &&+1 ((y &&+2 w)||(y &&+1 z)))"
                //"(x &&+1 (--,((--,(y &&+2 w))&&(--,(y &&+1 z)))))"
                , a);
        assertEquals(3, a.seqDur());
    }
    @Test
    void theDifferenceBetweenDTERNAL_and_Parallel_dternal_b() {
        //distributed, while sequence is preserved; same event range -> dt=0
        assertEq("((a&&b) &&+1 (a&&b))",
                "( ((a&&b) &&+1 (a&&b)) && (a &&+1 b) )");
    }
    @Test
    void ConjParallelsMixture2b() {
        assertEq("((&&,a,b2,b3) &&+1 (c&&b1))",
                "(((a &&+1 b1)&|b2)&|(b3 &&+1 c))");
    }
    @Test
    void _Not_A_Sequence() {
        Term x = $$("(((--,((--,believe(z,rotate)) &&+4680 (--,believe(z,rotate))))&|(--,left))&&(right &&+200 (--,right)))");
        assertTrue(x.complexity() > 5); //something
//        assertTrue(Conj.isSeq(x));
//        assertEquals(1, Conj.seqEternalComponents(x.subterms()).cardinality());
    }

    @Test void Multiple_Seq_dtZero() {
        assertEq("TODO", "((--,(b &&+1 d)) &&+0 (b &&+2 d))");
    }
    @Test
    void testSequenceInEternal() {
        assertEq(
                "((#DAY2 &&+1 #DAY1)&&#NIGHT)", //FACTORED
                //"((#DAY2&&#NIGHT) &&+1 (#DAY1&&#NIGHT))",
                //"((#NIGHT &&+1 #DAY1)&&(#DAY2 &&+1 #NIGHT))",
                CONJ.the(
                        $$$("(#NIGHT &&+1 #DAY1)"),
                        $$$("(#DAY2 &&+1 #NIGHT)")
                ));
    }

    @Disabled
    static class CanWeAbolishDTeq0 {

        private final Random rng = new XoRoShiRo128PlusRandom(1);

        @Test
        void PromoteEternalToParallel3() {


            Term x = assertEq(//"((b&&c)&|(x&&y))",
                    //"((b&&c)&|(x&&y))",
                    "(&|,b,c,x,y)",
                    "((b&&c)&|(x&&y))");

            Term y = $$("(&|,(b&&c),x)");
            assertEquals("(&&,b,c,x)", y.toString());

//            assertEquals("y", Conj.diffOne(x, y).toString());

            //ConjCommutive.the(DTERNAL, $$("(a&|b)"), $$("(b&|c)"));

            assertEq("((a&|b)&&(b&|c))", "((a&|b)&&(b&|c))");
            assertEq("((a&|b)&&(c&|d))", "((a&|b)&&(c&|d))");
            assertEq("(&|,a,b,c,d)", "((a&&b)&|(c&&d))");
        }

        @Test
        void misc() {

            assertEq("(&|,(--,(c&|d)),a,b)", "((&&,a,b) &| --(&|,c,d))");
            Term xy_xz = $$("((x &| y) &| (w &| z))");
            assertEq("(&|,x,y,w,z)", xy_xz);
            assertEq("((a&|b)==>x)", "((a &| b) ==> x)");
            assertEq("((a&|b) ==>+- x)", "((a &| b) ==>+- x)"); //xternal: unaffected
            assertEq("((a&|b) &&+- c)", "((a&|b) &&+- c)"); //xternal: unaffected
        }

        @Test
        void ValidConjParallelContainingTermButSeparatedInTime0() {
            for (String s : new String[]{
                    "(x &&+100 (--,(x&|y)))",
                    "(x &&+100 ((--,(x&|y))&|a))"
            })
                assertStable(s);
        }

        @Test
        void ValidConjParallelContainingTermButSeparatedInTime() {
            for (String s : new String[]{
                    "((--,(&|,(--,L),(--,R),(--,angVel)))&|(--,(x&|y)))",
                    "(x &&+100 ((--,(&|,(--,L),(--,R),(--,angVel)))&|(--,(x&|y))))",
                    "(x &&+100 ((--,(x&|y))&|(--,z)))",
                    "(x &&+100 (--,(x&|y)))"
            })
                assertStable(s);


        }

        @Test
        void ConjEventConsistency3ary() {
            for (int i = 0; i < 100; i++) {
                assertConsistentConj(3, 0, 7);
            }
        }

        @Test
        void ConjEventConsistency4ary() {
            for (int i = 0; i < 100; i++) {
                assertConsistentConj(4, 0, 11);
            }
        }

        @Test
        void ConjEventConsistency5ary() {
            for (int i = 0; i < 300; i++) {
                assertConsistentConj(5, 0, 17);
            }
        }

        @Deprecated
        private void assertConsistentConj(int variety, int start, int end) {
            Lst<LongObjectPair<Term>> x = newRandomEvents(variety, start, end);

            Term y = conj(x.clone());

            //if (!x.equals(z)) {
            //    Term y2 = conj(x.clone());
            //}

            //assertEquals(x, z);
        }

        private Lst<LongObjectPair<Term>> newRandomEvents(int variety, int start, int end) {
            Lst<LongObjectPair<Term>> e = new Lst<>();
            long earliest = Long.MAX_VALUE;
            for (int i = 0; i < variety; i++) {
                long at = (long) rng.nextInt(end - start) + start;
                earliest = Math.min(at, earliest);
                e.add(pair(at, atomic(String.valueOf((char) ('a' + i)))));
            }

            long finalEarliest = earliest;
            e.replaceAll((x) -> pair(x.getOne() - finalEarliest, x.getTwo()));
            e.sortThisByLong(LongObjectPair::getOne);
            return e;
        }
    }

}