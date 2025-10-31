package nars.term.util.transform;

import nars.NAL;
import nars.Narsese;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.util.conj.Cond;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.term.atom.Bool.True;
import static nars.term.util.Testing.assertEq;
import static nars.term.util.transform.Conceptualization.*;
import static org.junit.jupiter.api.Assertions.*;

class ConceptualizationTest {
    private static final String D = "((((--,(c&&d))&&(--,e)) &&+1 c) &&+1 d)";
    private static final String E = "(((--,a) &&+380 (--,b)) &&+120 ((--,c) &&+2170 (--,((--,b) &&+470 (--,b)))))";
    private static final String G = "(((--,((--,y) &&+90 (--,((--,x) &&+210 (--,x))))) &&+2380 x) &&+2320 x)";
    private static final String J = "(((x ==>+5 y) &&+5 y)==>x)";
    private static final String K = "(((p-->x) &&+10 (p-->x)) &&+100 (p-->y))";
    private static final String L = "((--,((reward:nario-->dur)&&(reward:nario-->shift))) &&+580 (--,(reward:nario-->shift)))";
    private static final String M = "(b &&+1 (c && d))";
    private static final String N = "(a &&+1 (b &&+1 c))";
    private static final String O = "(a &&-1 (b &&+1 c))";
    private static final String P = "((--((tetris-->dex)&&#1) &&+1 (tetris-->dex)) &&+1 #1)";


    private static void assertEqXY(TermTransform f, String y, String x) {
        assertEqXY(f, y, $$(x));
    }

    private static void assertEqXY(TermTransform f, String expect, Term input) {
        assertInstanceOf(Compound.class, input);

        Term actual = f.apply(input);
        assertEq(expect, actual);

        Term actual2 = f.apply(actual);
        assertEquals(actual, actual2, () -> "unstable: " + input + " -> " + actual + " -> " + actual2); //stability test
    }

    private static Term assertCeptualStable(String s) throws Narsese.NarseseException {
        Term c = $(s);
        Term c1 = c.concept();

        Term c2 = c1.concept();
        assertEquals(c1, c2, () -> "unstable: " + c1 + " != " + c2 + " <= " + s);
        return c1;
    }

    @Test
    void conjParSeq_2() {
        assertConcept( "(a &&+- b)", "(a && b)");
        assertConcept( "(a &&+- b)", "(a &&+1 b)");
        assertConcept( "(a &&+- b)", "(a &&-1 b)");
    }
    @Test
    void conjParSeq_3() {
        assertConcept( "(&&,a,b,c)", "(&&, a, b, c)");
        assertConcept( "( &&+- ,a,b,c)", "((a &&+1 b) &&+1 c)");
        assertConcept( "( &&+- ,c,b,a)", "((c &&+1 b) &&+1 a)");
        assertConcept( "((b &&+- c) &&+- a)", "(a &&+1 (b && c))");
    }

    @Test
    void testHybridStrategy_conjSeq() {
        assertConcept("((x &&+- y) &&+- x)", "((x &&+1 y) &&+1 x)");
        assertConcept("((x &&+- y) &&+- (--,x))", "((x &&+1 y) &&+1 --x)");
    }
    @Test
    void testHybridStrategy_delta() {
        assertConcept("Δ((x &&+- y) &&+- x)", "/\\((x &&+1 y) &&+1 x)");
    }

    @Test
    void testHybridStrategyc() {
        assertConcept( "((--,(a &&+- b)) &&+- (--,b))", "((--,(a&&b)) &&+- (--,b))");
    }

    @Disabled @Test
    void D_Hybrid() {
        assertConcept("TODO" /*"(((--,(c &&+- d))&&(--,e)) &&+- (c &&+- d))"*/, D);
    }

    @Test
    void D_Sequence() {
        assertEqXY(Flatten, "( &&+- ,(--,(c&&d)),(--,e),c,d)", D);
    }

    @Test
    void E_Hybrid() {
        assertConcept("((((b ||+- b) &&+- (--,c)) &&+- (--,b)) &&+- (--,a))", E);
    }

    @Test
    void E_Sequence() {
        assertConcept("((y &&+- z) &&+- x)", "((x &&+1 y) &&+1 z)");
        assertConcept("((x &&+- z) &&+- y)", "((y &&+1 x) &&+1 z)");
        assertConcept("((x &&+- y) &&+- y)", "((y &&+1 x) &&+1 y)");
        assertConcept("((x &&+- y) &&+- y)", "((y &&+1 y) &&+1 x)");
    }
    @Test
    void E_Sequence_Parallel_Mix() {
        assertConcept("((&&,x,y,z) &&+- x)", "((&&,x,y,z) &&+1 x)");
    }

    @Test
    void E_Sequence_Factored_xternal() {
        assertConcept("((x &&+- y)&&z)", "(z && (x &&+- y))");
    }

    @Test
    void E_Sequence_Factored_seq() {
        assertConcept("((x &&+- y)&&z)", "(z && (x &&+1 y))");
    }


    @Test
    void E_Sequence_Others() {
        assertEqXY(Flatten, "( &&+- ,(b ||+- b),(--,a),(--,b),(--,c))", E);
    }

    @Disabled @Test
    void F_Hybrid() {
        String F =
            "(((--,(b:p&&#1)) &&+1800 (((--,#1)&&(--,#2))||(--,b:p))) &&+1990 (--,(b:p&&#2)))";
        String ff =
            "(((((--,#1) &&+- (--,#2))||(--,(p-->b))) &&+- (--,((p-->b) &&+- #2))) &&+- (--,((p-->b) &&+- #1)))";
        assertConcept(ff, F);
    }

    @Test
    void G_Hybrid() {
        assertConcept("((((--,x) &&+- (--,x)) ||+- y) &&+- (x &&+- x))", G);

    }
    @Test
    void G_Hybrid_b() {
        assertEqXY(Hybrid,
            "(((((--,((tetris,b)-->(5,14))) &&+- low(tetris,b)) ||+- (--,(((tetris,b)-->(#1,#2)) &&+- (cmp(#1,#2)=-1)))) &&+- ((tetris,b)-->(5,14))) &&+- ((--,(((tetris,b)-->(#3,#4)) &&+- (cmp(#3,#4)=1))) &&+- (--,low(tetris,b))))",
            "(((--,low(tetris,b)) &&+5980 (--,(((tetris,b)-->(#3,#4))&&(cmp(#3,#4)=1)))) &&+100 ((--,((((tetris,b)-->(#1,#2))&&(cmp(#1,#2)=-1)) &&+3110 (--,((--,((tetris,b)-->(5,14))) &&+3790 low(tetris,b)))))&&((tetris,b)-->(5,14))))");
    }

    @Test void I_Hybrid_pre() {

        String K = "((a&&b)||c)";
        assertConcept("((a &&+- b) ||+- c)", K);

        String K0 = "(a&&b)";
        assertConcept("(a &&+- b)", K0);

    }

    @Test void I_Hybrid() {
        String I = "(((b||c) &&+180 (a||c)) &&+160 ((a&&b)||c))";
        assertConcept("((((a &&+- b) ||+- c) &&+- (a ||+- c)) &&+- (b ||+- c))", I);
    }

    @Disabled @Test void J_Hybridj() {
        assertConcept("(((x ==>+- y) &&+- y) ==>+- x)", J);
    }

    @Test void L_Hybrid() {
        assertConcept("((--,((nario-->reward)-->(dur&&shift))) &&+- (--,((nario-->reward)-->shift)))", L);
    }
    @Test
    void K_Hybrid() {
        assertConcept("(((p-->x) &&+- (p-->y)) &&+- (p-->x))", K);
    }


    @Test
    void EmbeddedChangedRoot() throws Narsese.NarseseException {
        assertEq("(a ==>+- (b &&+- c))",//"(a ==>+- (b&&c))",
                $("(a ==> (b &&+1 c))").root());
    }

    @Test
    void EmbeddedChangedRootVariations() throws Narsese.NarseseException {
        {
            Term x = $("(a ==> (b &&+1 (c && d)))");
            assertEquals("(a==>(b &&+1 (c&&d)))", x.toString());
            assertEq(
                    "(a==>(&&,b,c,d))",
                    //"(a ==>+- (&&,b,c,d))",
                    //"(a ==>+- ((c&&d) &&+- b))",
                    x.root());
        }
//		{
//			Term x = $("(a ==> (b &&+1 (c && d)))");
//			assertEq("(a ==>+- (&&,b,c,d))", x.root());
//		}
//
//
//		{
//			Term x = $("(a ==> (b &&+1 (c &&+1 d)))");
//			assertEq("(a ==>+- (&&,b,c,d))", x.root());
//		}

//        {
//            Term x = $("(a ==> (b &&+1 --(c &&+1 d)))");
//            assertEquals("(a ==>+- ( &&+- ,b,c,d))", x.root().toString());
//        }

    }

    @Test
    void conceptualizeSequence() {
        assertEq("((2,(g,y)) &&+- (2,(g,y)))",
                $$("((2,(g,y)) &&+260 (2,(g,y)))").root());
    }

//	@Test void conceptualizeImplSeq() {
//		assertEq("(((--,((x-->h) ==>+- ((--,(x-->effort))&&(x-->h))))&&(--,(((x-->h) &&+- (x-->h)) ==>+- (x-->h))))<->((x-->h) &&+- (x-->h)))", $$("(((--,(((x-->h) &&+300 (x-->h)) ==>+8660 (x-->h))) &&+8510 (--,((x-->h) ==>+170 ((--,(x-->effort)) &&+180 (x-->h)))))<->((x-->h) &&+530 (x-->h)))").concept());
//	}

    @Test
    void conceptualizeSequence_orderingcary() {
        assertEq("(a &&+- b)", $$("(a &&+1 b)").root());
        assertEq("(a &&+- b)", $$("(b &&+1 a)").root());
    }


    @Test
    void N_Hybrid() {
        assertConcept("((b &&+- c) &&+- a)", N);
    }

    @Test
    void O_Hybrid() {
        assertEq("((b &&+1 c) &&+1 a)", O);
        assertEq("((a &&+1 b) &&+1 c)", "(a &&+1 (b &&+1 c))");
        assertConcept("((a &&+- c) &&+- b)", O);
    }

    @Test void temporal_in_inh() {
        assertConcept("((x ==>+- y)-->a)", "((x==>y)-->a)");
    }
    @Test void temporal_in_prod() {
        assertConcept("((x ==>+- y),(x &&+- y))", "((x ==>+1 y),(x &&+1 y))");
        assertConcept("((x ==>+- y),(x &&+- y))", "((x==>y),(x&&y))");
    }

    @Test
    void conceptualizeSequence2() {
        assertConcept("(--,((x &&+- x) &&+- x))", "(--,((x &&+79 x) &&+90 x))");
    }
    @Test
    void conceptualizeSequence2b() {
        assertConcept("((--,((x &&+- x) &&+- x)) &&+- (--,y))", "((--,((x &&+79 x) &&+90 x)) &&+154 --y)");
    }

    private static void assertConcept(String a, Term b) {
        assertEqXY(NAL.conceptualization, a, b);
    }

    private static void assertConcept(String a, String b) {
        assertConcept(a, $$$(b));
    }

//    @Test
//    void conceptualizeSequence2ci() {
//        assertEqXY(Hybrid,"((--,(x ==>+- x)) &&+- (--,y))",
//                "((--,(x ==>+90 x)) &&+154 --y)");
//    }

    @Test
    void conceptualizeSequence3() {
        assertEq("((tetris-->(&&,(--,density),R,rotate)) &&+- (tetris-->((--,density)&&rotate)))",
                $$("(((--,(tetris-->density))&&(tetris-->rotate)) &&+50 (&&,(--,(tetris-->density)),(tetris-->R),(tetris-->rotate)))").root()
        );

    }

    @Disabled @Test
    void Conceptualize_Not_NAL3_seq() {
        assertEq("(x-->(a &&+- a))", $$("(x-->(a &&+1 a))").root());
    }

    @Test
    void StableNormalizationAndConceptualizationComplex() {
        String s = "(((--,checkScore(#1))&&#1) &&+14540 ((--,((#1)-->$2)) ==>+17140 (isRow(#1,(0,false),true)&&((#1)-->$2))))";
        Term t = $$(s);
        assertEquals(s, t.toString());
        assertEquals(s, t.normalize().toString());
        assertEquals(s, t.normalize().normalize().toString());
//		String c = "( &&+- ,((--,((#1)-->$2)) ==>+- (isRow(#1,(0,false),true)&&((#1)-->$2))),(--,checkScore(#1)),#1)";
//		assertEquals(c, t.concept().toString());
//		assertEquals(c, t.concept().concept().toString());
//		Termed x = new DefaultConceptBuilder((z) -> {
//		}).apply(t);
//		assertTrue(x instanceof TaskConcept);
    }

    @Test
    void conjRoot1() throws Narsese.NarseseException {

        assertEq("(x &&+- y)", $("(x && y)").root());
        assertEq("(x &&+- y)", $("(x &&+1 y)").root());
        assertEq("(x &&+- y)", $("(x &&-1 y)").root());
        assertEq("(x &&+- y)", $("(x &&+- y)").root());

        assertEq("(x &&+- x)", $("(x &&+1 x)").root());
        assertEq("(x &&+- x)", $("(x &&-1 x)").root());
        assertEq("(x &&+- x)", $("(x &&+- x)").root());

        assertEq("((--,x) &&+- x)", $("(x &&+1 --x)").root());
        assertEq("((--,x) &&+- x)", $("(x &&-1 --x)").root());
        assertEq("((--,x) &&+- x)", $("(x &&+- --x)").root());
    }

    @Test
    void implRoot1() throws Narsese.NarseseException {

        assertConcept("(x ==>+- y)", $("(x ==> y)").root());
        assertConcept("(x ==>+- y)", $("(x ==>+1 y)").root());
        assertConcept("(x ==>+- y)", $("(x ==>-1 y)").root());
        assertConcept("(x ==>+- y)", $("(x ==>+- y)").root());
    }
    @Test
    void implRoot2() throws Narsese.NarseseException {

        assertConcept("(x ==>+- x)", $("(x ==>+1 x)").root());
        assertConcept("(x ==>+- x)", $("(x ==>-1 x)").root());
        assertConcept("(x ==>+- x)", $("(x ==>+- x)").root());
    }
    @Test
    void implRoot3() throws Narsese.NarseseException {

        assertEqXY(Hybrid,"((--,x) ==>+- x)", $("(--x ==>+1 x)").root());
        assertEqXY(Hybrid,"((--,x) ==>+- x)", $("(--x ==>-1 x)").root());
        assertEqXY(Hybrid,"((--,x) ==>+- x)", $("(--x ==>+- x)").root());
    }

    @Test void implSeqConcept1() throws Narsese.NarseseException {
        assertEqXY(Hybrid,"((x &&+- y) ==>+- a)",
                $("((x &&+1 y) ==>+- a)").concept());
        assertEqXY(Hybrid,"((x &&+- y) ==>+- a)",
                $("((x&&y) ==>+- a)").concept());

    }

    @Test
    void P_Hybrid() {
        assertEqXY(Hybrid,
                //"((--,((tetris-->dex)&&#1)) &&+- ((tetris-->dex)&&#1))"
                "((--,((tetris-->dex) &&+- #1)) &&+- ((tetris-->dex) &&+- #1))"
                , P);
    }

//    @Disabled @Deprecated static class ConceptualizationSequenceTest {
//        @Test
//        void P_Sequence() {
//            assertEqXY(Sequence, "((--,((tetris-->dex)&&#1)) &&+- ((tetris-->dex) &&+- #1))", P);
//        }
//        @Test
//        void M_Sequence() {
//            assertEqXY(Sequence, "((c&&d) &&+- b)", M);
//            //assertEqXY(Sequence,"(((--,rotate(tetris,(speed,tetris,/)))==>speed(tetris,a)) &&+- ((--,rotate(tetris,(speed,tetris,/)))==>speed(tetris,a)))", z);
//        }
//        @Test
//        void N_Sequence() {
//            assertEqXY(Sequence, "((((--,x) &&+- (--,x)) ||+- y) &&+- (x &&+- x))", G);
//            assertEqXY(Sequence, "(((--,(c&&d))&&(--,e)) &&+- (c &&+- d))",D);
//            assertEqXY(Sequence, "((((b ||+- b) &&+- (--,c)) &&+- (--,b)) &&+- (--,a))", E);
//            assertEqXY(Sequence, "((b &&+- c) &&+- a)", N);
//        }
//
//        @Test
//        void O_Sequence() {
//            assertEqXY(Sequence, "((a &&+- c) &&+- b)", O);
//        }
//
//
//        @Test
//        void K_Sequence() {
//            assertEqXY(Sequence, "(((p-->x) &&+- (p-->y)) &&+- (p-->x))",K);
//        }
//
//
//        /**
//         * not fully working
//         */
//        @Deprecated
//        public static final Untemporalize Sequence = new Untemporalize() {
////        @Override
////        protected boolean flatten() {
////            return false;
////        }
//
//            @Override
//            protected Term transformConjSeq(Compound x) {
//                TermList xx = seqEvents(x, false, false);
//
//                xx.replaceAll(this);
//
//                int s = xx.subs();
//
//                if (s > 2) xx.reverseThis(); //HACK put sequence start outside
//                Term y;
//                if (/*!flatten() ||*/ s > 2 || ((x.structureSubs() & CONJ.bit) != 0) || (s == 2 && xx.sub(0).equalsPN(xx.sub(1)))) {
//                    y = ConjSeq.xSeq(xx);
//                } else {
//                    y = CONJ.the((Subterms) xx);
//                }
//                xx.delete();
//                return y;
//            }
//
//        };
//
//    }
    @Test
    void Q_Hybrid_0() {
        assertEqXY(Hybrid,
                "(noid(7,4) &&+- (--,(noid-->L)))",
                "(noid(7,4)&&(--,(noid-->L)))");
        assertEqXY(Hybrid,
                "(noid(7,4) ==>+- noid(14,13))",
                "(noid(7,4) ==>+- noid(14,13))");
        assertEqXY(Hybrid,
                "((--,(noid-->L)) ==>+- noid(14,13))",
                "((--,(noid-->L)) ==>+- noid(14,13))");
    }
    @Test
    void Q_Hybrid() {
        assertEqXY(Hybrid,
                "((x &&+- y) ==>+- z)",
                "((x &&+- y) ==>+- z)");
        assertEqXY(Hybrid,
                "(((--,y) &&+- x) ==>+- z)",
                "(((--,y) &&+- x) ==>+- z)");
    }

    @Test void Q_Hybrid3() {
        String A = "(((k,g)-->(3,6))||(--,(g-->destroy)))";
        String B = "(g-->((--,alive)&&destroy))";
        Term a = $$(A), b = $$(B);
        assertInstanceOf(Neg.class, a); assertTrue(a.unneg().CONJ());
        assertTrue(b.INH());
        assertTrue(Cond.commonSubCond(a.unneg(), b, true, 0));
        Term aImplB = $$("(" + A + " ==>+- " + B + ")");
        assertTrue(aImplB.IMPL());
        Term aImplBConcept = aImplB.concept();
        assertTrue(aImplBConcept.IMPL());
        assertEq("((((k,g)-->(3,6)) ||+- (--,(g-->destroy))) ==>+- (g-->((--,alive)&&destroy)))",
                aImplBConcept);
    }


    @Test
    void Q_Hybrid2() {
        assertConcept("((x &&+- y) ==>+- x)",
                "((x&&y) ==>+- x)");
    }
    @Test void R_Hybrid() {
//        assertEq(Null, "((((x,1)&&z) &&+- (--,w))-->(((x,1)||z)&&(--,(1,/,dAng))))");
//        assertEq(Null,"(((a&&z)&&c)-->((a||z)&&d))");
        assertEquals(Null, $$("((&&,(forget||(--,clear)),(--,(premises,x)),happy)<->(((premises,y)&&(--,forget)) &&+- (--,grow)))"));
        assertEquals(Null, $$("(((a&&z) &&+- c)-->((a||z)&&d))"));
//        assertEqO(Hybrid,"","(((a&&z) &&+- c)-->((a||z)&&d))");
    }

    @Test
    void conjRoot_depVar_2() {
        assertEq("(--,((tetris-->dex) &&+- #1))", $$("(--,((tetris-->dex)&&#1))").root());
        assertEq("((--,(tetris-->dex)) &&+- #1)", $$("  (--(tetris-->dex)&&#1)").root());

        assertEq("((--,(x &&+- #1)) &&+- (x &&+- #1))",
                $$("(--(x&&#1) &&+1 (x&&#1))").root());
        assertEq("(((--,x) &&+- x) &&+- #1)",
                $$("((--x&&#1) &&+1 (x&&#1))").root());

    }

    @Test
    void conjRoot2() {
        assertEq("( &&+- ,(--,(tetris-->(a&&b))),x,y)",
                $$("(&&,(--,(tetris-->(a&&b))),x,y)").root());
    }


    @Test
    void M_Hybrid() {
        assertConcept("((c&&d) &&+- b)", M);
    }


    @Test
    void ConjSeqConceptual2_Hybrid() {
        assertConcept("(x-->((--,(a&&b))&&c))",
                "(x-->((--,(a&&b))&&c))");
    }

    private static final Retemporalize Xternal = Retemporalize.retemporalizeAllToXTERNAL;

    @Disabled @Test
    void ConjSeqConceptual2_Xternal() {
        assertEqXY(Xternal, "((--,((x-->a) &&+- (x-->b))) &&+- (x-->c))", "(x-->((--,(a&&b))&&c))");
    }

    @Test void Inner_Temporal_1() {
        assertConcept("a(x,(y &&+- z))", "a(x,(y &&+1 z))");
    }

    @Test void Inner_Temporal_1b() {
        assertEqXY(Xternal, "a(x,(y &&+- z))", "a(x,(y &&+1 z))");
    }

    @Test
    void Inner_Temporal_Sim1() {
        assertEqXY(Xternal, "((((a)&&x)-->y)<->y(a))", "((((a)&&x)-->y)<->y(a))");
    }
    @Test
    void Inner_Temporal_Sim2() {
        assertEqXY(Xternal, "TODO",
                "(((((--,(tetris,c))&&(tetris,b))-->meta)-->happy)<->(meta(tetris,a)-->happy))");
    }

    @Test
    void ConjSeqConceptual5() {
        assertConcept("((||,(1-->ang),(--,z),(--,ang))&&(--,(2-->ang)))",
                "((--,(&&,(--,(1-->ang)),ang,z))&&(--,(2-->ang)))");

        //TODO ((grid,#1,13) &&+440 (--,((||,(--,(grid,#1,#1)),rotate)&&left))) .concept()
        //TODO ((&&,(x-->curi),(--,left),(--,rotate))&&(--,((--,rotate) &&+819 (--,left))))
    }

    @Test
    void ConceptualizationWithoutConjReduction2a() {
        assertEqXY(Hybrid,"(&&,x,y,z)", "((x &&+1 y) &&+1 z)");
        assertEqXY(Hybrid,"(&&,x,y,z)", "((x &&+1 z) &&+1 y)");

    }

    @Test
    void ConceptualizationWithoutConjReduction() {
        String s = "((--,((y-->#1) &&+345 (#1,zoom))) &&+1215 (--,((#1,zoom) &&+10 (y-->#1))))";
        assertEqXY(Hybrid,"((--,((y-->#1)&&(#1,zoom))) &&+- (--,((y-->#1)&&(#1,zoom))))",
                s);
    }


    @Test
    public void ConjConceptualizationWithNeg1() {
        String s = "(--,((--,(right &&+12 #1)) &&+24 #1))";
        Term t = $$(s);
        assertEquals(s, t.toString());
        assertEq("(--,((--,(right &&+12 #1)) &&+24 #1))", t.normalize().toString());
    }

    @Disabled
    @Test
    public void ConjConceptualizationWithNeg2() {
        //TODO
        String s = "(--,((--,(right &&+12 #1)) &&+24 #1))";
        Term t = $$(s);
        assertEq("(--,((--,(right&&#1)) &&+- #1))", t.root().toString());
        assertEq("((--,(right&&#1)) &&+- #1)", t.unneg().concept().toString());
    }

    @Test
    void StableConceptualization6a() {
        assertEqXY(Hybrid,
                "(x(isRow,(8,false),true) &&+- x($1,#2))",
                "(x($1,#2) &&+290 x(isRow,(8,false),true))");
        assertEqXY(Hybrid,
                "((x(isRow,(8,false),true) &&+- x($1,#2)) ==>+- (((checkScore,#2)&&($1,#2))-->x))",
                "((x($1,#2) &&+290 x(isRow,(8,false),true)) ==>+1 (x(checkScore,#2)&&x($1,#2)))");
    }

    @Test
    void ConjSeqConceptual3() throws Narsese.NarseseException {
        String x0 = "((--,((--,a) &&+1 (--,b))) &&+1 a)";
        {
            assertEq("(((--,a) &&+1 (--,b)) &&+1 a)",
                    CONJ.the($("((--,a) &&+1 (--,b))"), +1, $("a")));
            assertEq(x0,
                    CONJ.the($("((--,a) &&+1 (--,b))").neg(), +1, $("a")));
        }

        String x = "(--," + x0 + ")";
        {

            Term y = $(x);
            assertEq(x, y);

            assertEq("(((--,a)&&(--,b)) ||+- (--,a))", y.root());
        }
    }

    @Test
    void ConjSeqConceptual4() throws Narsese.NarseseException {
        {
            Term t = $("((--,((--,(--a &&+1 --b)) &&+1 a)) &&+1 a)");
            assertEq("((--,((--,((--,a) &&+1 (--,b))) &&+1 a)) &&+1 a)", t);

            Term r = t.root();
            assertEq("((((--,a) &&+- (--,b)) ||+- (--,a)) &&+- a)", r);

            Term c = t.concept();
            assertInstanceOf(Compound.class, c);
            assertEquals(r, c);
        }
    }

    @Disabled
    @Test
    void ConjParallelConceptual() {


        for (int dt : new int[]{ /*XTERNAL,*/ 0, DTERNAL}) {
            assertEquals("( &&+- ,a,b,c)",
                    CONJ.the(dt, $$("a"), $$("b"), $$("c")).concept().toString(), () -> "dt=" + dt);
        }


//        assertEquals(
//                "( &&+- ,(bx-->noid),(y-->noid),#1)",
//                $$("(--,(((bx-->noid) && (y-->noid)) && #1))")
//                        .concept().toString());
        assertEquals(
                "(x,(--,( &&+- ,a,b,c)))",
                $$("(x,(--,(( a && b) && c)))")
                        .concept().toString());
    }



    @Test
    void InhConj() {
        Term a = $$("(x-->(y&&z))");
        Term ac = a.concept();
        assertEq("(x-->(y&&z))", ac);
//        assertEq("(x-->(y&&z))", $$("(x-->(y &&+1 z))").concept());
        assertEq("((x-->((--,score)&&rotate))-->(plan,z,/))",
                $$("((x-->((--,score)&&rotate))-->(plan,z,/))").concept());
    }

    /**
     * TODO make consistent with new conceptualization
     */
    @Disabled
    @Test
    void ConceptualizeNAL3() {
        //direct inh subterm
        assertEq("(x-->(a &&+- b))", $$("(x-->(a&&b))").root());
        assertEq("(x-->(a &&+- b))", $$("(x-->(a&&b))").concept());

        assertEq(//TODO "(x-->(a||b))",
                "(--,(x-->((--,a) &&+- (--,b))))",
                $$("(x-->(a||b))").root());
        assertEq(//TODO "(x-->(a||b))",
                "(x-->((--,a) &&+- (--,b)))",
                $$("(x-->(a||b))").concept());


        assertEq("(x-->( &&+- ,a,b,c))", $$("(x-->(&&,a,b,c))").root());

        //direct sim subterm
        assertEq("((a &&+- b)<->x)", $$("(x<->(a&&b))").root());
        assertEq("((a &&+- b)<->x)", $$("(x<->(a&&b))").concept());
        assertEq("((a &&+- b)<->(c &&+- d))", $$("((c&&d)<->(a&&b))").root());

        //indirect inh subterm (thru product)
        assertEq("x((a &&+- b))", $$("x((a&&b))").root());
        assertEq("x((a ||+- b))", $$("x((a||b))").root());
        assertEq(//"x(( &&+- ,(--,b),(--,c),a))",
                "x(((b ||+- c) &&+- a))",
                $$("x((a&&(b||c)))").root());

        assertEq("(a-->(x &&+- y))", $$("(a-->(x && y))").concept());
        assertEq("((x &&+- y)-->a)", $$("((x && y)-->a)").concept());
        assertEq("((x &&+- y)<->a)", $$("((x && y)<->a)").concept());
        assertEq("(a-->( &&+- ,x,y,z))", $$("(a-->(&&,x,y,z))").concept());

        assertEq("(a-->((--,x) &&+- (--,y)))", $$("(a-->(x || y))").concept());


    }

    @Test
    void Conceptualize_4() {
        assertEq("(--,((--,(fz-->(ang,0))) ==>-870 (fz-->(ang,15))))", "((--,(fz-->(ang,0))) ==>-870 (--,(fz-->(ang,15))))");
    }

    @Disabled @Test
    void temporallyDistinctSim() {
        assertEq(True, "((a &&+1 b) <-> (a &&+2 b))");
    }

    @Test
    void ConjWithTwoImpls() {
        Term c = $$("((x ==>+1 y) && (x ==>+2 y))");
        assertEq("((x ==>+1 y)&&(x ==>+2 y))", c);
        assertEq("((x==>y) &&+- (x==>y))", c.root());
    }

    @Test
    @Disabled void ConjWithTwoImpls2() {
        final String x = "(&&,((--,rotate(tetris,(speed,tetris,/))) ==>+384 speed(tetris,a)),((--,rotate(tetris,(speed,tetris,/))) ==>+392 speed(tetris,a)),((--,rotate(tetris,(speed,tetris,/))) ==>+416 speed(tetris,a)))";
        Term y = $$(x);
        assertEq(x, y);
        assertEquals(CONJ, y.normalize().op());
        Term z = y.root();
        assertEquals(CONJ, z.op());
        assertConcept( "(((--,rotate(tetris,(speed,tetris,/)))==>speed(tetris,a)) &&+- ((--,rotate(tetris,(speed,tetris,/)))==>speed(tetris,a)))", z);

    }

    @Disabled
    @Test
    void ConjWithTwoConj() {
        Term c = $$("((x &&+1 y) && (x &&+2 y))");
        assertEq("((x &&+1 y) &&+1 y)", /*"((x &&+1 y)&&(x &&+2 y))",*/ c);
        assertEq("((x&&y) &&+- (x&&y))", c.root());
    }

    @Test
    void ConjRepeatOrdering1() {
        assertConcept("(((x&&y) &&+- x) &&+- x)", "((x &&+1 (x&&y)) &&+1 x)");
        assertConcept("(((x&&y) &&+- x) &&+- x)", "((x &&+- (x&&y)) &&+1 x)");
    }
    @Test
    void ConjRepeatOrdering2() {
        assertConcept("((x &&+- y) &&+- x)", "(x &&+1 (x&&y))");
    }
    @Test
    void ConjRepeatOrdering3() {
        assertConcept("(((x&&y) &&+- x) &&+- x)", "((x &&+1 (x&&y)) &&+- x)");
    }

    @Test
    void StableConceptualization2() throws Narsese.NarseseException {
        Term c1 = assertCeptualStable("((a&&b)&&do(that))");
        assertEq("(&&,do(that),a,b)", c1.toString());
    }

    @Test
    void StableConceptualization4() throws Narsese.NarseseException {
        Term c1 = assertCeptualStable("((--,((#1-->y)&&(#1-->neutral)))&&(--,(#1-->sad)))");
        assertEq("(#1-->((--,(y&&neutral))&&(--,sad)))", c1.toString());
    }

    @Test void testUhm() {
        Term x = $$("((--,((((ang,x,#1)&&#3)-->#2)&&(#3-->#4))) &&+13210 (--,(#3-->(#2&&#4))))");
        assertEq("((--,((((ang,x,#1)&&#3)-->#2)&&(#3-->#4))) &&+- (--,(#3-->(#2&&#4))))", x.concept());
        assertEq("((--,((((ang,x,#1)&&#3)-->#2)&&(#3-->#4))) &&+- (--,(#3-->(#2&&#4))))", x.concept().concept());
    }
    @Test void testUhm2() {
        Term x = $$("((fz-->((1,ang,/)&&\"-\")) ==>+- ((ang,fz)-->1))");
        assertEq(x, x.concept());
    }
    @Test void testUhm3() {
        {
            Term x = $$("((($1-->\"+\")||(--,($2-->$3))) ==>+2830 ((--,($1-->\"+\"))&&($2-->$3)))");
            assertEq("((($1-->\"+\") ||+- (--,($2-->$3))) ==>+- ((--,($1-->\"+\")) &&+- ($2-->$3)))", x.concept());
        }

//        {
//            Term x = $$("((($1-->\"+\")||(--,($2-->$3))) ==>+- ((--,($1-->\"+\"))&&($2-->$3)))");
//            assertEq(x, x.concept());
//        }

    }
    @Test void testUhm4() {
        assertEqXY(Hybrid,
        "((--,((tetris,b)-->((--,shift)&&$1))) ==>+- ((tetris,b)-->((--,shift)&&$1)))",
        "((--,((tetris,b)-->((--,shift)&&$1))) ==>+1060 ((tetris,b)-->((--,shift)&&$1)))");
    }

    @Test void imageConj() {
        String s = "((tetris,a)-->((b&&c),/,pri))";
        assertConcept(s, s);
    }
    @Test
    void Atemporalization4() {
        assertConcept("((x &&+- $1) ==>+- (y &&+- $1))",
                "((x && $1) ==>+- (y && $1))");
    }
    @Test
    void Atemporalization5() {
        assertConcept("(--,((x &&+- $1) ==>+- ((--,y) &&+- $1)))",
                "(--,(($1&&x) ==>+1 ((--,y) &&+2 $1)))");
    }
    @Test void DisjInImplComplex0() {
        assertEqXY(Hybrid,
                "(((#1 &&+- #1) ||+- pa) ==>+- $2)",
                "((||,(#1 &&+60 #1),pa) ==>+960 $2)");
    }

    @Test void impl1() {
//        assertEquals(0, $$$("Δ((#1-->dur) &&+45 (#1-->simple)))").seqDur());

        String x = "((Δ((#1-->dur) &&+45 (#1-->simple)) &&+7079 (--,(nario-->$3))) ==>+45 (nario-->$3))";
        assertEquals(x, $$$(x).toString());
        assertEqXY(Hybrid,
                "((Δ((#1-->dur) &&+- (#1-->simple)) &&+- (--,(nario-->$2))) ==>+- (nario-->$2))",
                x);
    }

    @Test void DisjWithSeq_3ary() {
        assertEqXY(Hybrid,
                "(||+- ,(#1 &&+- #1),x,y)",
                "(||,(#1 &&+60 #1),x,y)");
    }

    @Test void DisjInImplComplex1() {
        assertEqXY(Hybrid,
                "((||+- ,(#1 &&+- #1),pa,(--,x)) ==>+- $2)",
                "((||,(#1 &&+60 #1),pa,(--,x)) ==>+960 $2)");

        assertEqXY(Hybrid,
                "((||+- ,(#1 &&+- #1),pa,(--,$2)) ==>+- $2)",
                "((||,(#1 &&+60 #1),pa,(--,$2)) ==>+960 $2)");
    }

    /** TODO */
    @Disabled @Test void SeqInPar() {
        assertEqXY(Hybrid,
        "",
        "(&&,((#1-->2) &&+1040 (#1-->0)),((#2,p)=#1),(#1-->3))");
    }
    @Test void ConjWTF_0() {
        assertEqXY(Hybrid,
                "((--,(pa &&+- pr)) &&+- (--,pa))",
                "((--,pa) &&+78 (--,(pa&&pr)))");
    }
    @Disabled @Test void ConjWTF_1() {

        assertEq("(((--,(pa &&+- pr)) &&+- (--,pa))&&(--,(pa &&+- pr)))", CONJ.the(DTERNAL,
                $$("(--,(pa &&+- pr))"),
                    $$("((--,(pa &&+- pr)) &&+- (--,pa))")));

        assertEqXY(Hybrid,
                "(((--,(pa &&+- pr)) &&+- (--,pa))&&(--,(pa &&+- pr)))",
                "(((--,pa) &&+78 (--,(pa&&pr)))&&(--,(pa&&pr)))");
    }
    @Test void ConjWTF_2() {
        assertEqXY(Hybrid,
                "TODO",
                "((--,(p-->((--,(\"+\"&&\"-\"))&&(--,balanced))))&&((p-->((--,balanced)&&\"+\")) &&+4000 (p-->balanced))). 28920⋈31460 %.07;.25%\" -> {TemporalTask@8596} \"$.59 ((--,(p-->((--,(\"+\"&&\"-\"))&&(--,balanced))))&&((p-->((--,balanced)&&\"+\")) &&+4000 (p-->balanced)))");
    }

    /**
     * has a bug, dont use
     */
    @Deprecated
    private static final Untemporalize Flatten = new Untemporalize() {
//        @Override
//        protected boolean flatten() {
//            return true;
//        }

        @Override
        protected Term transformConjSeq(Compound x) {
            TermList xx = seqEvents(x, true, false);
            int s = xx.subs();
            Term y = s > 2 || ((x.structSubs() & CONJ.bit) != 0) || (s == 2 && xx.sub(0).equalsPN(xx.sub(1))) ?
                CONJ.the(XTERNAL, (Subterms) xx) :
                CONJ.the((Subterms) xx);
            xx.delete();
            return y;
        }
    };


}