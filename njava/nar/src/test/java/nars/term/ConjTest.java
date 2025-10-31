package nars.term;

import jcog.data.list.Lst;
import nars.$;
import nars.Narsese;
import nars.Term;
import nars.io.IO;
import nars.subterm.TmpTermList;
import nars.term.atom.Atom;
import nars.term.util.TermException;
import nars.term.util.Testing;
import nars.term.util.conj.*;
import nars.term.util.transform.Retemporalize;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static nars.$.*;
import static nars.Op.*;
import static nars.Op.terms;
import static nars.term.ConjTest2.conj;
import static nars.term.atom.Bool.*;
import static nars.term.util.Testing.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;
import static org.junit.jupiter.api.Assertions.*;


class ConjTest {

    private static final Term x = Atom.atomic("x");
    private static final Term y = Atom.atomic("y");
    private static final Term z = Atom.atomic("z");
    private static final Term a = Atom.atomic("a");
    private static final Term frA = $$("#1");
    private static final Term frB = $$("--(#1 &&+- #1)");
    private static final Term frAn = frA.neg();
    private static final Term frBn = $$("--(--#1 &&+- --#1)");

    static Term assertEq(String y, String x) {
        return assertEq($$(y), $$(x));
    }

    static Term assertEq(Term y, String x) {
        return assertEq(y, $$(x));
    }

    static void assertEq(String y, Term x) {
        Testing.assertEq(y, x);
        //return assertEq($$$(y), x);
    }

    /**
     * additionally tests inverse
     */
    private static Term assertEq(Term y, Term x) {
        var xy = Testing.assertEq(y, x);


        Map<Term, Term> a = new HashMap();
        x.ANDrecurse(z -> true, (xx, s) -> {
            if ((s == null || s.CONJ()) && (!xx.CONJ())) {
                a.put(xx, p(xx).neg());
                a.put(xx.neg(), p(xx));
            }
            return true;
        });
        var xa = x.replace(a);
        var ya = y.replace(a);
        var xya = Testing.assertEq(ya, xa);


        return xy;
    }

    @Test
    void AryDisjFilter() {
        //(not (a and b and c) and not(a and b) )
        assertEq("(--,(a&&b))", "(--(&&,a,b,c) && --(&&,a,b) )");
    }

    @Test
    void FalseReplace() {

        assertEq(False, CONJ.the(XTERNAL, False, False));
        assertEq(False, "(false &&+- false)");

        assertEq(True, CONJ.the(XTERNAL, True, True));
        assertEq(Null, CONJ.the(XTERNAL, x, Null));

        assertEq(x, CONJ.the(XTERNAL, True, x));

    }

    @Test
    void FalseReplace2() {
        assertEq(False,
                $$("(((--,x) &&+- (--,x))&&(x &&+- x))")
                        .replace($$("(x &&+- x)"), $$("x")));
    }

    @Test
    void FalseReduce1a() {
        assertEq(False, $$("(&&,x,--(#1 &&+- #1),#1)"));
    }

    @Test
    void FalseReduce1b() {
        assertEquals(False, CONJ.the(frA, frB));
        assertEquals(False, CONJ.the(frAn, frBn));
    }

    @Test
    void FalseReduce1bb() {
        assertEquals(False, CONJ.the(frA, frB, x));
        assertEquals(False, CONJ.the(frAn, frBn, x));
    }

    @Test
    void FalseReduce1c() {
        var t = new ConjTree();
        assertTrue(t.add(ETERNAL, frA));
        /*assertFalse(*/
        t.add(ETERNAL, frB);//);
        assertEquals(False, t.term());
    }

    @Disabled @Test
    void ConjTree_Seq_of_Two_Negated_Seq() {
        var t = new ConjTree();
        t.add(1, $$c("(--,(a &&+1 b)"));
        t.add(2, $$c("(--,(c &&+1 d)"));
        assertEq("TODO", t.term());
    }
    private static final Term b = $$("b");
    private static final Term c = $$("c");
    private static final Term abC = $$("(?1 ==> c)");
    private static final Term ab1C = $$("(?1 ==>+1 c)");
    private static final Term ab2C = $$("(?1 ==>+2 c)");
    private static final Term Cab = $$("(c ==> ?1)");
    private static final Term a1a = $$("(a &&+1 a)");
    private static final Term b2b = $$("(b &&+2 b)");
    private static final Term C1ab = $$("(c ==>+1 ?1)");
    private static final Term C2ab = $$("(c ==>+2 ?1)");


    @Test void ImplConj_Eternal() {
        assertEq("((a&&b)==>c)",
            Cond.implConj(a, b, c, 'c', true, abC, abC));
        assertEq("((a||b)==>c)",
            Cond.implConj(a, b, c, 'd', true, abC, abC));
        assertEq("(c==>(a&&b))",
                Cond.implConj(a, b, c, 'c', false, abC, Cab));
        assertEq("(--,(c==>((--,a)&&(--,b))))",
                Cond.implConj(a, b, c, 'd', false, abC, Cab));
    }
    @Test void ImplConj_Seq() {
        assertEq("(c ==>+1 (a &&+1 b))",
                Cond.implConj(a, b, c, 'c', false, C1ab, C2ab));

        assertEq("((b &&+1 a) ==>+1 c)",
                Cond.implConj(a, b, c, 'c', true, ab1C, ab2C));

        assertEq("((a&&b) ==>+1 c)",
                Cond.implConj(a, b, c, 'c', true, ab1C, ab1C));
        assertEq("((a &&+1 b) ==>+1 c)",
                Cond.implConj(a, b, c, 'c', true, ab2C, ab1C));
    }
    @Test void ImplConj_Seq2() {
        assertEq("(((a &&+1 a) &&+1 b) ==>+1 c)",
                Cond.implConj(a1a, b, c, 'c', true, ab2C, ab1C));
    }
    @Test void ImplConj_Seq3() {
        assertEq("(((a&&b) &&+1 a) ==>+1 c)",
                Cond.implConj(a1a, b, c, 'c', true, ab1C, ab2C));
    }
    @Test void ImplConj_Seq4() {
        assertEq("(c ==>+1 (a &&+1 (a&&b)))",
                Cond.implConj(a1a, b, c, 'c', false, C1ab, C2ab));
    }

    @Test
    void FalseReduce1d() {

        var t = new ConjTree();
        assertTrue(t.add(ETERNAL, frB));
        /*assertFalse(*/
        t.add(ETERNAL, frA)/*)*/;
        assertEquals(False, t.term());
    }

    @Test
    void disj_in_conj_reduction() {
        assertEq("L", "((L||R)&&L)");
        assertEq("R", "((L||R)&&R)");
        assertEq("((--,L)&&(--,R))", "((L||(--,R))&&(--,L))"); //and(or(L,not(R)),not(L)) = not(L) and not(R)
        assertEq("(--,R)", "((L||(--,R))&&(--,R))"); //and(or(L,not(R)),not(R)) = not(R)
        assertEq("((--,R)&&L)", "((L||R)&&(--,R))");
        assertEq("((--,R)&&X)", "(((L||(--,R)) && X)&&(--,R))");
    }

    @Test
    void disj_in_conj_seq_reduction2() {
        assertEq("((--,R) &&+561 ((--,R)&&(--,speed)))", "(((L||(--,R)) &&+561 (--,speed))&&(--,R))");
    }

//    @Test
//    void neg_conj_seq1() {
//        if (NAL.term.DISJ_SEQ_SIMPLE) {
//            assertEq("((x &&+1 (--,y)) &&+1 (--,z))",
//                    "(x &&+1 --(y &&+1 z))");
//        }
//    }

    @Test
    void conj_xor() {
        assertEq("((x||y)&&(--,(x&&y)))"
                ,"((x|y) & --(x&y))");
    }

    @Test
    void disj_xor() {
        assertEq("(((--,x)&&y)||((--,y)&&x))"
                ,"((x & --y) | (--x & y))");
    }

    @Test
    void disj_xor_neg() {
        assertEq("(((--,x)&&(--,y))||(x&&y))"
                , "((--x & --y) | (x & y))");
    }

    @Test
    void disj_in_conj_seq_reduction3a() {
        assertEq("(L &&+561 ((--,speed)&&L))",
                "((L &&+561 (--,speed))&&L)");
    }

    @Test
    void disj_in_conj_seq_reduction3b() {

        assertEq("(L &&+561 ((--,speed)&&L))",
                "(((L||(--,R)) &&+561 (--,speed))&&L)");
    }

    @Test
    void disj_in_conj_xternal_reduction_pre() {
        assertEq("(a &&+1 (a&&b))", "(((a||b) &&+1 b)&&a)");
    }

    @Test void disj_in_conj_xternal_reduction_a() {
        assertEq("((a&&b) &&+- a)", CONJ.the($$("((a||b) &&+- b)"), $$("a")));
    }

    @Test void disj_in_conj_xternal_reduction_b() {
        assertEq("((a&&b) &&+- a)", "(((a||b) &&+- b)&&a)");
    }

    @Test void disj_in_conj_xternal_no_reduction() {
        assertEq("((a &&+- b)&&c)", "((a &&+- b)&&c)");
    }

    @Test
    void conjXternalReplace() {
        assertEq("((x &&+1 y) &&+1 z)",
            $$("((x &&+- y) &&+1 z)")
                .replace($$("(x &&+- y)"), $$("(x &&+1 y)"))
        );
    }

    @Test
    void conjXternalReplace2() {
        assertEq("((x &&+1 y) &&+1 (z &&+1 w))",
                $$("(((x &&+- y) &&+1 z) &&+1 w)")
                        .replace($$("(x &&+- y)"), $$("(x &&+1 y)"))
        );
    }
    @Test
    void disj_in_conj_seq_reduction4() {
        var a = $$("(x &&+1 (--y &&+1 --z))");
        var b = $$("(x &&+1 --(y &&+1   z))");
        assertNotEquals(a, b);
        assertEquals(a.seqDur(), b.seqDur());
        assertNotEquals(a.concept(), b.concept());
    }



    @Test
    void conjTreeContradictionElimination00() {
        var x = ConjPar.parallel(DTERNAL, new TmpTermList(
                $$("a"),
                $$("--(a&&b)")
        ), true, terms);
        assertEq("((--,b)&&a)", x);
    }

    @Test
    void conjTreeContradictionElimination_Bundle() {

        var a = new TmpTermList(
                $$("(x-->a)"),
                $$("--(x-->(a&&b))")
        );
        var x = ConjPar.parallel(DTERNAL, a, true, terms);
        assertEq("((--,(x-->b))&&(x-->a))", x);

    }

    @Test
    void conjTreeContradictionElimination_DontBundle() {

        var a = new TmpTermList(
                $$("(x-->a)"),
                $$("--(x-->(a&&b))")
        );
        var x = ConjPar.parallel(DTERNAL, a, true, terms);
        assertEq("((--,(x-->b))&&(x-->a))", x);
    }

    @Test
    void AnotherComplexInvalidConj2() {
        //TODO check what this actually means
        var a = $$("((--,((--,_3) &&+190 (--,_3)))&&((_1,_2)&&_3))");
        assertEq("((_1,_2)&&_3)", a);
    }

    @Test
    void DTChange() {
        final var x = "((a &&+3 b) &&+3 c)";
        var y = $$c(x);
        assertEq(x, y);
        assertEq("((a &&+3 b) &&+2 c)", y.dt(2));
    }

    @Test
    void XternalWithFalseFastFail() {
        assertEq(False, "(((--,((a &&+- b) &&+- c)) &&+- (--,d)) &&+- false)");
        assertEq(False, "(((--,((a &&+- b) &&+- c)) &&+- false) &&+- d)");
    }

    @Test
    void ParallelDisjunctionAbsorb() {
        assertEq("((--,y)&&x)", CONJ.the($$("--(x&&y)"), $$("x")));
        assertEq("((--,y)&&x)", CONJ.the(0, $$("--(x&&y)"), $$("x")));
    }

    @Test
    void disjEteConjOverride1() {
        assertEq("(((--,a)&&c) &&+- ((--,a)&&c))",
                "(&&,((--,a) &&+- (--,a)),c,(--,a))");
        assertEq(False, "(&&,(a &&+- a),c,(--,a))");
    }

    @Test
    void disjEteConjOverride2() {
        assertEq("((c&&#1) &&+- (c&&#1))", "(&&,((--,#6) &&+- (--,#6)),c,(--,#6))");
    }

    @Test
    void ParallelDisjunctionInert() {
        assertEq("((--,(x&&y))&&z)",
                CONJ.the($$("--(x&&y)"), $$("z")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%" /* @ ETE */, "(a &&+1 %)" /* @+1 */, "(% &&+1 a)" /* @ 0 */})
    void disjunctifyEliminate(String p) {
        ConjBuilder c = new ConjTree();
        c.add(p.length() > 1 ? 0 : ETERNAL, $$(p.replace("%", "(--x || y)")));
        c.add(p.length() > 1 ? 0 : 1L, $$(p.replace("%", "--x")));
        assertEq(p.replace("%", "(--,x)"), c.term());
    }

    @Test
    void dusjunctifyInSeq() {
        assertEq("(a &&+1 x)", "(a &&+1 ((x || y)&&x))");
        assertEq("(a &&+1 (--,x))", "(a &&+1 ((--x || y)&&--x))");
        assertEq("(x &&+1 a)", "(((x || y)&&x) &&+1 a)");
        assertEq("((--,x) &&+1 a)", "(((--x || y)&&--x) &&+1 a)");
    }

    @Disabled @Test
    void disjunctifySeq2() {
        ConjBuilder c = new ConjTree();
        c.add(ETERNAL, $$("(--,(((--,(g1-->input)) &&+40 (g1-->forget))&&((g1-->happy) &&+40 (g1-->happy))))"));
        var addTemporal = c.add(1, $$("happy:g1")); //shouldnt cancel since it's temporal, only at 1
        assertTrue(addTemporal);
        assertEq("((--,((--,(g1-->input)) &&+40 (g1-->forget)))&&(g1-->happy))", c.term());
//        assertEq("((--,((--,(_1-->_2)) &&+40 (_1-->_3)))&&(_1-->_4))", c.term().anon());

    }

    @Test
    void ContradictionWTF() {
        assertEq(False, $$("((x(g,1) &&+49 (--,((g,(g,-1))-->((x,/,-1),x))))&&(--,x(g,1)))"));
    }

    @Test
    void DisjCancellation() {
        assertEq("b", "((b||c)&&(b||(--,c)))");
    }

    @Disabled @Test
    void disjunctionSequenceReduce() {
        var y = "((--,((--,tetris(1,11)) &&+330 (--,tetris(1,11))))&&(--,left))";
//
//        //by parsing
//        assertEq(y,
//                "((--,(((--,tetris(1,11)) &&+230 (--,left)) &&+100 ((--,tetris(1,11)) &&+230 (--,left)))) && --left)"
//        );

        //by Conj construction
        for (var w : new long[]{ETERNAL, 0, 1}) {
            ConjBuilder c = new ConjTree();
            c.add(ETERNAL, $$("(--,(((--,tetris(1,11)) &&+230 (--,left)) &&+100 ((--,tetris(1,11)) &&+230 (--,left))))"));
            c.add(w, $$("--left"));
            assertEq(y, c.term());
        }

    }

    @ParameterizedTest
    @ValueSource(strings = {"%" /* @ ETE */, "(a &&+1 %)" /* @+1 */, "(% &&+1 a)" /* @ 0 */})
    @Disabled void disjunctifyReduce(String p) {
        ConjBuilder c =
                //new Conj();
                new ConjTree();
        c.add(p.length() > 1 ? 0 : ETERNAL, $$(p.replace("%", "(--x || y)")));
        c.add(p.length() > 1 ? 0 : 1L, $$(p.replace("%", "x")));
        assertEq(p.replace("%", "(x&&y)"), c.term());
    }

    @Test
    void conjoinify_234u892342() {
        ConjBuilder c = new ConjTree();
        var x = $$("(((--,right) &&+90 (--,rotate)) &&+50 ((--,tetris(1,7))&&(--,tetris(7,4))))");
        assertEquals(CONJ, x.op());
        var y = $$("right");
        c.add(0, x);
        assertEquals(3, c.eventOccurrences());
        c.add(25360, y);
        assertEq("(((--,right) &&+90 (--,rotate)) &&+50 (((--,tetris(1,7))&&(--,tetris(7,4))) &&+25220 right))", c.term());
    }

    @Test
    void ParallelDisjunctionAbsorb0() {
        /* and(not(x), or(x, not(y))) = ¬x ∧ ¬y */
        assertEq("((--,R)&&(--,jump))", "(--R && (R || --jump))");
    }


    @Test
    void SequentialDisjunctionAbsorb3par() {
        assertEq("((--,R)&&(--,jump))", "(--R && (  R || --jump))");
        assertEq("((--,jump)&&R)", "(  R && (--R || --jump))");
    }

    @Disabled @Test
    void SequentialDisjunctionAbsorb3seqA() {
        assertEq("((--,jump)&&R)", "(R && --(R &&+600 jump))");
    }

    @Disabled @Test
    void SequentialDisjunctionAbsorb3seqB() {
        assertEq("((--,R)&&(--,jump))", "(--R && --(--R &&+600 jump))");
    }

    @Test
    void SequentialDisjunctionContradict() {
        var u = CONJ.the(
                $$("(--,((--,R) &&+600 jump))"),
                $$("(--,L)"),
                $$("R"));
        assertEq("((--,L)&&R)", u);
    }

    @Test void DisjunctionParallelReduction0() {
        var x = $$("(&&,(--,((height-->tetris)&&(density-->tetris))),(--,curi(tetris)),(density-->tetris))");
        assertEq("(&&,(--,curi(tetris)),(--,(height-->tetris)),(density-->tetris))", x);
    }

    @Test
    void DisjunctionParallelReduction() {
        assertEq("((--,y)&&x)",
                $$("(&&,(--,(y&&x)),x)")
        );
        assertEq("((--,y)&&x)", //<- maybe --y &| x
                $$("(&&,(--,(y&|x)),x)")
        );
        assertEq("((--,y)&&x)",
                $$("(&|,(--,(y&&x)),x)")
        );
        assertEq("((--,y)&&x)",
                $$("(&|,(--,(y&|x)),x)")
        );
        assertEq("(&&,(--,y),x,z)",
                $$("(&|,(--,(y&|x)),z,x)")
        );

        assertEq("(&&,(--,curi(tetris)),(--,(height-->tetris)),(density-->tetris))",
                $$("(&|,(--,((height-->tetris)&|(density-->tetris))),(--,curi(tetris)),(density-->tetris))")
        );

    }

    @Test
    void DisjunctionParallelReduction2() {

        /* and( not(and(y,x)),z,x) = x ∧ ¬y ∧ z */
        assertEq("(&&,(--,y),x,z)",
                $$("(&&,(--,(y&&x)),z,x)")
        );

    }

    @Test
    void CollapseEteParallel1() {

        assertEq("(&&,a,b,c)", "((&&,a,b)&|c)"); //collapse
        //assertEq("((a&&b)&|c)", "((&&,a,b)&|c)"); //NOT collapse

        assertEq("(&&,a,b,c)", "((&|,a,b)&&c)"); //NOT collapse

    }

    @Test
    void CollapseEteParallel2() {
        assertEq("(&&,a,b,c,d)", "(&|,(&&,a,b),c,d)"); //collapse
//        assertEq("(&|,(a&&b),c,d)", "(&|,(&&,a,b),c,d)"); //NOT collapse
        assertEq("(&&,a,b,c,d)", "(&&,(&|,a,b),c,d)"); //NOT collapse
    }


//    @Test void ConjEternalConj2() {
//        Term a = $$("(--,((--,((--,y)&|x))&&x))"); //<-- should not be constructed
//        Term b = $$("(x &&+5480 (--,(z &&+5230 (--,x))))");
//        Term ab = CONJ.the(a, b);
//        assertTrue(ab.equals(Null) || ab.equals(False));
//    }


    //TODO
//    @Test void SubTimes() {
//        $$("(((--,_1) &&+22 _2) &&+2 (--,_2))").subTimes()
//    }

    @Test
    void CollapseEteParallel3() {

        assertEq("(&&,(--,(c&&d)),a,b)", CONJ.the($$("(--,(c&|d))"), $$("(a&|b)")));
        assertEq("(&&,(--,(c&&d)),a,b)", "((&|,a,b) && --(&|,c,d))"); //NOT collapse


    }

    @Test
    void CollapseEteContainingEventParallel1() {

        assertEq("(a &&+1 (b&&c))", "(a &&+1 (b&&c))");
        assertEq("(a &&+1 (&&,b,c,d))", "(a &&+1 (b&|(c&&d)))");


    }

//    @Test
//    void ConjDistributeEteParallel1() {
//        Term x = $$("((&|,_2(_1),_4(_3),_6(_5))&&(--,(_6(#1)&|_6(#2))))");
//
//        ConjBuilder c = new ConjTree();
//        boolean added = c.add(x.SEQ() ? 0 : ETERNAL, x);
//        assertTrue(added);
//        assertEquals(3, c.eventCount(ETERNAL));
//        assertEq(x, c.term());
//
//    }

    @Test
    void Disj_Simplify_2a() {
        assertEq("((a||b)&&c)", "((a & c) | (b & c))");
    }

    @Test void Disj_Simplify_2aa() {
        assertEq("((a&&b)||c)", "((a | c) & (b | c))");
    }

    @Test
    void Disj_Simplify_2b() {
        assertEq("(--,((x||y)&&a))", "(--(a && x) && --(a && y))");
    }

    @Test
    void Disj_Simplify_2c() {
        assertEq("(&&,(a||b),x,y)", "((&&,x,y,a) || (&&,x,y,b))");
    }

    @Test
    void Disj_Simplify_3() {
        assertEq("((||,x,y,z)&&a)", "(||,(a&&x),(a&&y),(a&&z))");
    }

    @Test
    void Disj_Simplify_3_inSeq() {
        assertEq("(w &&+1 ((||,x,y,z)&&a))", "(w &&+1 (||,(a&&x),(a&&y),(a&&z)))");
    }

    @Test
    void Disj2() {

        assertEq("x", "((a && x)||(--a && x))");
        assertEq("x", "--(--(a && x) && --(--a && x))");
    }

    @Test
    void Disj3a0() {
        assertEq("(--,a)", ConjPar.disjunctive(new TmpTermList(
                        $$("--(a&&x)"), $$("--(a&&y)"), $$("--a")),
                DTERNAL, terms_));
    }

    @Test
    void Disj3a() {

        assertEq("a", "(||,(a && x),(a && y), a)");
    }

    @Test
    void Disj3b() {
        assertEq("((||,x,y,z)&&a)", "(||,(a && x),(a && y), (a&&z))");
    }

    @Test
    void Disj3c() {
        assertEq("(((x||y)&&a)||z)", "(||,(a && x),(a && y), z)");
    }

    @Test
    void Disj3d() {
        assertEq("((||,x,y,(--,z))&&a)", "(||,(a && x),(a && y), (a&&--z))");
    }

    @Test
    void Disj4() {
        /*
        0 = {Neg$NegCached@3442} "(--,(a&&x))"
        1 = {Neg$NegCached@3443} "(--,(a&&y))"
        2 = {CachedCompound$TemporalCachedCompound@3437} "(a&&z)"
        */
        /* (a and x) or (a and y) or not(a and z) */
        assertEq("(||,x,y,(--,a),(--,z))", "(||,(a && x),(a && y), --(a&&z))");
    }

    @Disabled
    @Test
    void Balancing() {

        //Term ct = Conj.conjSeqFinal(Op.terms, 590, $$("((--,(left &&+380 (--,left)))&|(--,left))"), $$("(--,(left &&+280 (--,left)))"));


        var as = "(((#1 &&+3080 #1) &&+16040 (#1 &&+1100 (--,((--,#1) &&+3580 (--,#1))))) &&+6540 (--,#1))";
        var a = $$(as);
        var bs = "(((#1 &&+3080 #1) &&+16040 #1) &&+1100 ((--,((--,#1) &&+3580 (--,#1))) &&+6540 (--,#1)))";
        var b = $$(bs);

        assertEq(a, b);
        //a.printRecursive();
        assertEquals(bs, a.toString());
        assertEquals(bs, b.toString());
    }

    @Disabled @Test
    void distributeCommonFactor() {
        assertEq("((x &&+1 (x&&y)) &&+1 ((--,y)&&x))",
                "(((x &&+1 y) &&+1 --y) && x)");
    }



    @Test
    void negatedSequenceInParallel() {
        var x = $$("((--,((--,destroy) &&+1 fire))&&speed)");
        assertTrue(x.SEQ());
        assertEquals(1, x.seqDur());

    }

    @Test
    void CommutizeRepeatingImpl1() {
        assertEquals(True,
                $$c("(a ==>+1 a)").dt(DTERNAL));
    }
    @Test
    void CommutizeRepeatingImpl2() {
        assertEquals(False,
                $$c("(--a ==>+1 a)").dt(DTERNAL));
    }
    @Test
    void CommutizeRepeatingImpl3() {
        assertEquals(True,
                $$c("(a ==>+1 a)").dt(0));
        assertEquals(False,
                $$c("(--a ==>+1 a)").dt(0));


        assertEquals("(a ==>+- a)",
                $$c("(a ==>+1 a)").dt(XTERNAL).toString());
        assertEquals("((--,a) ==>+- a)",
                $$c("(--a ==>+1 a)").dt(XTERNAL).toString());
    }

    @Test
    void stupidDisjReduction_a() {
        assertEq("rotate", "((right||rotate)&&rotate)");
    }
    @Test
    void stupidDisjReduction_b() {
        assertEq("rotate", CONJ.the(
                $$("(right||rotate)"), $$("rotate")));
    }

    @Test
    void ParallelEvents() {
        var e = ConjList.conds(
                $$("(&&,(checkScore(tetris) &&+40 (--,isRow(tetris,(#1,TRUE)))),isRow(tetris,(#2,(--,TRUE))),cmp(#1,#2,-1))")
        );
        for (var w : e.when) {
            assertTrue(w==ETERNAL || (w >= 0 && w <= 40));
        }
    }

    @Test
    void EventContradictionAmongNonContradictionsRoaring() {
        ConjBuilder c = new ConjTree();
        c.add(ETERNAL, $$("(&&,a,b,c,d,e,f,g,h)"));
        assertEquals(8, c.eventCount(ETERNAL));
        var added = c.add(1, a.neg());
        assertFalse(added);
        assertEquals(False, c.term());
    }

    @Test
    void GroupNonDTemporalParallelComponents() throws Narsese.NarseseException {


        var c1 = $("((--,(ball_left)) &&-270 (ball_right)))");

        assertEquals("((ball_right) &&+270 (--,(ball_left)))", c1.toString());
        assertEquals(
                "(((ball_left)&&(ball_right)) &&+270 (--,(ball_left)))",

                CONJ.the(0, $("(ball_left)"), $("(ball_right)"), c1)
                        .toString());

    }

    @Test
    void DisjConjElim() {
        assertEq("(--,L)", "((R || --L) && --L)");
    }

    @Test
    void EventContradictionWithEternal() {
        ConjBuilder c = new ConjTree();
        c.add(ETERNAL, x);
        var added = c.add(1, x.neg());
        assertFalse(added);
        assertEquals(False, c.term());
    }

    @Test
    void EventNonContradictionWithEternal() {
        ConjBuilder c = new ConjTree();
        c.add(ETERNAL, x);
        var added = c.add(1, y);
        assertTrue(added);
        assertEquals("(x&&y)", c.term().toString());
    }

    @Test
    void ConjParallelWithSeq2() {
        assertEq(False, "((--a &&+5 b)&|a)");
        assertEq("((a&&b) &&+5 (--,a))", "((b &&+5 --a)&|a)");
    }

    @Test
    void ConjEventsWithFalse() throws Narsese.NarseseException {
        assertEquals(
                False,
                conj(
                        new Lst<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(1L, $("a")),
                                pair(2L, $("b1")),
                                pair(2L, False)
                        })));
        assertEquals(
                False,
                conj(
                        new Lst<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(1L, $("a")),
                                pair(1L, $("--a"))
                        })));
    }

    @Test
    void ConjParallelsMixture() {

        assertEq(False, "(((b &&+4 a)&|(--,b))&|((--,c) &&+6 a))");
    }

    @Test
    void ConjEteParaReduction() {
        assertEq("((--,a)&&b)", "(((--,a)&|b)&&b))");
    }

    @Test
    void EventContradictionAmongNonContradictions() {
        ConjBuilder c = new ConjTree();
        c.add(1, x);
        c.add(1, y);
        c.add(1, z);
        assertFalse(c.add(1, x.neg()));
        assertEquals(False, c.term());
    }

    @Test
    void EventContradiction() {
        ConjBuilder c = new ConjTree();
        c.add(1, x);
        assertFalse(c.add(1, x.neg()));
        assertEquals(False, c.term());
    }

    @Test
    void ConjParaEteReductionInvalid() {
        assertEquals(False,
                $$("(((--,a)&&b)&|(--,b)))")
        );
    }

    @Test
    void ConjParaEteReduction2simpler() {
        assertEq("(((--,x)&&y) ==>+1 ((--,x)&&y))", "(((--,x)&|y) ==>+1 (((--,x)&&y)&|y))");
    }


    @Test
    void ConjParaEteReductionInvalid2() {
        assertEquals(False,
                $$("(((--,a)&&(--,b))&|b))")
        );
    }

    @Test
    void ConjParaEteReduction() {
        assertEq("((--,a)&&b)", "(((--,a)&&b)&|b))");
    }

    @Test
    void ConjParallelOverrideEternal() {

        assertEq(
                "(a&&b)",
                "( (a&&b) &| (a&|b) )");

    }

    @Test
    void ConegatedConjunctionTerms4() throws Narsese.NarseseException {
        assertEquals($("((--,(y&&z))&&x)"), $("(x && --(y && z))"));
    }

    @Test
    void ConegatedConjunctionTerms1() {
        assertEq(False, "(&|, #1, (--,#1), x)");
        assertEq(False, "(&&, #1, (--,#1), x)");
    }

    @Test
    void ConjComplexAddRemove() {
        var x = $$("(( ( (x,_3) &| (--,_4)) &| (_5 &| _6)) &&+8 ( ((x,_3) &| (--,_4)) &| (_5 &|_6))))");
        assertEq("((&&,(x,_3),(--,_4),_5,_6) &&+8 (&&,(x,_3),(--,_4),_5,_6))", x);

//        ConjBuilder c = new ConjTree();
//        c.add(x.SEQ() ? 0 : ETERNAL, x);
//        assertEquals(x, c.term());


    }

    @Test
    void Promote2() {
        assertEq("(&&,a,b,c)", "(a&&(b&|c))");
    }

    @Test
    void negatedConjunctionAndNegatedSubterm() throws Narsese.NarseseException {


        assertEquals("(--,x)", $("((--,(x &| y)) &| (--,x))").toString());
        assertEquals("(--,x)", $("((--,(x && y)) && (--,x))").toString());


        assertEquals("(--,x)", $("((--,(x && y)) && (--,x))").toString());
        assertEquals("(--,x)", $("((--,(&&,x,y,z)) && (--,x))").toString());

        assertEquals("((--,(y&&z))&&x)", $("((--,(&&,x,y,z)) && x)").toString());
    }

    @Disabled
    @Test
    void CoNegatedConjunctionParallelEternal1() {

        assertEq(False,
                "(((--,(z&&y))&&x)&|(--,x))");
    }

    @Disabled
    @Test
    void CoNegatedConjunctionParallelEternal2() {
        assertEq(False,
                "(((--,(y&&z))&|x)&&(--,x))");

    }

    @Test
    void theDifferenceBetweenDTERNAL_and_Parallel_dternal_a() {

        assertEq("((a&&b) &&+1 (a&&b))",//"(&&,(a &&+1 b),a,b)",
                "( (a&&b) && (a &&+1 b) )"); //distributed, while sequence is preserved
    }
    @Test
    void dontDistribute() {

        assertEq("((a &&+1 b)&&c)",
                "((a &&+1 b)&&c)");
        assertEq("((a &&+1 b)&&(c=#1))",
                "((a &&+1 b)&&(c=#1))");
        assertEq("(&&,(a &&+1 b),(c=#1),(d=#2))",
                "(&&,(a &&+1 b),(c=#1),(d=#2))");
    }

    @Test
    void theDifferenceBetweenDTERNAL_and_Parallel_dternal_c() {
        //differeng event range, can not align sequences
        assertEq("(((a&&b) &&+1 (a&&b))&&(a &&+2 b))",
                "( ((a&&b) &&+1 (a&&b)) && (a &&+2 b) )");

    }

    @Test
    void commutiveConjInSequence() {
        assertEq("((x&&y) &&+1 w)", "((x && y) &&+1 w)");
        assertEq("(w &&+1 (x&&y))", "(w &&+1 (x && y))");
        assertEq("((a&&b) &&+1 (x&&y))", "((a && b) &&+1 (x && y))");
        assertEq("((a&&b) &&+1 (c&&d))", ConjSeq.conjAppend($$("(a&&b)"), 1, $$("(c&&d)"), terms));
    }

    @Test
    void eteConjInParallel() {
        assertEq("(&&,a,b,z)", "((a && b) &| z)");
        assertEq("((--,(a&&b))&&z)", "(--(a && b) &| z)");
    }

    @Test
    void negatedEteConjInSequenceShouldBeParallel() {
        assertEq("((--,(a&&b)) &&+1 (x&&y))", "(--(a && b) &&+1 (x && y))");
        assertEq("(--,((a&&b) &&+1 (x&&y)))", "--((a && b) &&+1 (x && y))");
    }

    @Test
    void ConjEternalOverride() {

        var y = "(a&|b)";

        ConjBuilder c = new ConjTree();
        c.add(ETERNAL, $$("(a&&b)"));
        c.add(ETERNAL, $$("(a&|b)"));
        assertEq("(a&&b)", c.term());

        assertEq(
                "(a&&b)",
                "( (a&&b) && (a&|b) )");

    }

    @Test
    void ConjPosNegElimination1() throws Narsese.NarseseException {


        assertEquals("((--,b)&&a)", $("(a && --(a && b))").toString());
    }

    @Test
    void ConjPosNegElimination2() throws Narsese.NarseseException {


        assertEquals("((--,a)&&b)", $("(--a && (||,a,b))").toString());
    }
    @Test
    void ConjPosNegEliminationXternal() throws Narsese.NarseseException {
        assertEquals("((--,a)&&b)",
                $("(--a && --(--a &&+- --b))").toString());
        assertEquals("((b ||+- c)&&(--,a))", $("(--a && --(&&+-, --a, --b, --c))").toString());
    }

    @Test
    void XternalRepeatA() {
        assertEq("( &&+- ,x,x,y)", "(&&+-,x,x,y)");
    }

    @Disabled @Test
    void XternalRepeatB() {
        assertEq("(x &&+- x)", "(&&+-,x,x,x)");
    }

    @Test
    void ConjRepeatPosNeg() {
        Term x = $.atomic("x");
        assertEquals(+1, CONJ.the(-1, x, x).dt());
        assertEquals(+1, CONJ.the(+1, x, x).dt());
        assertArrayEquals(IO.termToBytes(CONJ.the(+32, x, x)), IO.termToBytes(CONJ.the(-32, x, x)));
        assertEquals(+1, ((Compound) CONJ.the(XTERNAL, new Term[]{x, x})).dt(-1).dt());
        assertEquals(+1, ((Compound) CONJ.the(XTERNAL, new Term[]{x, x})).dt(+1).dt());
        assertEquals(CONJ.the(-1, x, x), CONJ.the(+1, x, x));
        assertEquals(((Compound) CONJ.the(XTERNAL, new Term[]{x, x})).dt(-1),
                ((Compound) CONJ.the(XTERNAL, new Term[]{x, x})).dt(+1));
    }

    @Test
    void ConjEvents1a() throws Narsese.NarseseException {
        assertEquals(
                "(a &&+16 ((--,a)&&b))",
                conj(
                        new Lst<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(298L, $("a")),
                                pair(314L, $("b")),
                                pair(314L, $("(--,a)"))})
                ).toString()
        );
    }

    @Test
    void ConjEvents1b() throws Narsese.NarseseException {
        assertEquals(
                "((a&&b) &&+1 (--,a))",
                conj(
                        new Lst<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(1L, $("a")),
                                pair(1L, $("b")),
                                pair(2L, $("(--,a)"))})
                ).toString()
        );
    }

    @Test
    void ConjEvents2() throws Narsese.NarseseException {
        assertEquals(
                "((a &&+1 (&&,b1,b2,b3)) &&+1 (c &&+1 (d1&&d2)))",
                conj(
                        new Lst<LongObjectPair<Term>>(new LongObjectPair[]{
                                pair(1L, $("a")),
                                pair(2L, $("b1")),
                                pair(2L, $("b2")),
                                pair(2L, $("b3")),
                                pair(3L, $("c")),
                                pair(4L, $("d1")),
                                pair(4L, $("d2")),
                                pair(5L, True /* ignored */)
                        })).toString());
    }

    @Test
    void ConjPosNeg() throws Narsese.NarseseException {


        assertEquals(False, $("(x && --x)"));
        assertEquals(True, $("--(x && --x)"));
        assertEquals(True, $("(||, x, --x)"));

        assertEquals("y", $("(y && --(&&,x,--x))").toString());
    }

    @Test
    void TrueFalseInParallel() {
        var X = $.atomic("x");
        for (var i : new int[]{XTERNAL, 0, DTERNAL}) {
            assertEquals("x", CONJ.the(i, X, True).toString(), ()->"dt=" + i);
            assertEquals(False, CONJ.the(i, X, False), ()->"dt=" + i);
        }
    }

    /** (a or not(z)) and not(a and x)
        CNF | (¬a ∨ ¬x) ∧ (a ∨ ¬z)

     */
    @Test void ParallelValid3() {
        assertEq("((a||(--,z))&&(--,(a&&x)))", //(a or not (z)) and not (a and x)
                $$("((a||(--,z)) && (--,(a&&x)))"));
    }

    /**
     * a and not (a and x) and not (a and y)
     * CNF | a ∧ ¬x ∧ ¬y
     * DNF | a ∧ ¬x ∧ ¬y
     */
    @Test void ParallelValid3_5() {
        assertEq("(&&,(--,x),(--,y),a)",
                $$("(&&,a,(--,(a&&x)),(--,(a&&y)))"));
    }
    @Test void ParallelValid3_5_aNeg() {
        assertEq("(&&,(--,a),(--,x),(--,y))",
                $$("(&&,(--,a),(--,((--,a)&&x)),(--,((--,a)&&y)))"));

    }

    /** ((a or not(z)) and not(a and x)) and not(a and y)
     DNF | (a ∧ ¬x ∧ ¬y) ∨ (¬a ∧ ¬z)
     CNF | (¬a ∨ ¬x) ∧ (¬a ∨ ¬y) ∧ (a ∨ ¬z)
     */
    @Test void ParallelValid4() {
        assertEq("(&&,(a||(--,z)),(--,(a&&x)),(--,(a&&y)))",
                $$("(&&,(a||(--,z)),(--,(a&&x)),(--,(a&&y)))"));
    }

    @Test
    void ConegatedConjunctionTerms0() throws Narsese.NarseseException {
        assertEq("((--,#1) &&+- #1)", "(#1 &&+- (--,#1))");
        assertEq("(#1 &&+1 (--,#1))", "(#1 &&+1 (--,#1))");
        assertEq(False, "(#1 && (--,#1))");
        assertEq(False, "(#1 &| (--,#1))");
        assertEquals(False, CONJ.the(varDep(1), varDep(1).neg()));


        assertEq("(x)", "(&&, --(#1 && (--,#1)), (x))");

        assertSame(CONJ, $("((x) &&+1 --(x))").op());
        assertSame(CONJ, $("(#1 &&+1 (--,#1))").op());


    }

    @Test
    void CoNegatedJunction() {


        assertEq(False, "(&&,x,a:b,(--,a:b))");

        assertEq(False, "(&&, (a), (--,(a)), (b))");
        assertEq(False, "(&&, (a), (--,(a)), (b), (c))");


        assertEq(False, "(&&,x,y,a:b,(--,a:b))");
    }

    @Test
    void coNegatedDisjunction_seq1() {
        assertEq(True, CondDiff.diffAll($$("((--,left) &&+120 (--,left))"), $$("--left")));

        assertEq(True, "(||,x,a:b,(--,a:b))");

        assertEq(True, "(||,x,y,a:b,(--,a:b))");
    }

    @Test
    void coNegatedDisjunction_seq2() {
        assertEq(False, "(&&,--((--,left) &&+120 (--,left)),--left)");
        assertEq(True, "(||,  ((--,left) &&+120 (--,left)),  left)");
        assertEq("(y==>x)", "(((||,((--,left) &&+120 (--,left)),left) && y) ==> (x&&y))");
    }

    @Disabled @Test
    void coNegatedDisjunction_seq_shortened() {
        assertEq("(--right && --left)", "(&&,--((--,left) &&+120 right),--left)"); //sequence distorted
    }

    @Test
    void coNegatedDisjunction_par() {
        assertEq("(x&&y)", "(--(x && --y) && x)");
        assertEq("(--x && --y)", "(--(x && --y) && --y)");
    }

    @Test
    void InvalidDisj() {
        assertEq("((--,d)&&c)", "((--,(d&&c)) && c)");
    }

    @Disabled @Test
    void InvalidDisjSeq() {

        assertEq("((--,(a&&b))&&c)", "(--(&&,a,b,c) && c)");

        var a = $$("(--,((a &&+1 b)&&c))");
        assertEq("((--,(a &&+1 b))&&c)", CONJ.the(a, $$("c")));

        assertEq("((--,(a &&+1 b))&&c)", "(&&, --((a &&+1 b)&&c), c)");
    }
    @Test
    void InvalidDisjSeq2() {

        var c = $$("((a &&+1 b)&&c)");
        var x = $$("c");
        var y = CondDiff.diffAll(c, x);
        assertEq("(a &&+1 b)", y);

    }

    @Test
    void ConegatedConjunctionTerms0not() {

        assertEq("((--,(y&&z))&&x)", "(x&&--(y &| z))");

        assertEq("((--,(y&&z))&&x)", "(x &| --(y && z))");
    }

    @Test
    void ConjImageIndepVar() {
        var r = "(($1-->(REPRESENT,/,$3))&&($2-->(REPRESENT,/,$4)))";
        assertEq("(REPRESENT($1,$3)&&REPRESENT($2,$4))", $$$(r));
    }

    @Test
    void ConjImageIndepVar2() {
        var s = "((($1-->(REPRESENT,/,$3))&&($2-->(REPRESENT,/,$4)))==>REPRESENT({$1,$2},{$3,$4}))";
        var ss = (Compound) $$$(s);
        assertFalse(ss.NORMALIZED());
        assertEq("((REPRESENT($1,$3)&&REPRESENT($2,$4))==>REPRESENT({$1,$2},{$3,$4}))", ss);
        assertEq("((REPRESENT($1,$2)&&REPRESENT($3,$4))==>REPRESENT({$1,$3},{$2,$4}))", ss.normalize());
    }

    @Test
    void ConegatedConjunctionTerms1not() {

        assertEq("((--,(y &&+1 z))&&x)", "(x&&--(y &&+1 z))");

        assertEq("(x &&+1 (--,(y&&z)))", "(x &&+1 --(y && z))");
    }

    @Test
    void ConegatedConjunctionTerms2() {

        assertEq("((--,(robin-->swimmer))&&#1)", "(#1 && --(#1&&(robin-->swimmer)))"); //and(x, not(and(x,y))) = x and not y
    }

    @Test
    void Demorgan1() {


        assertEq("(--,(p&&q))", "(||, --p, --q)");
    }

    @Test
    void Demorgan2() {


        assertEq("((--,p)&&(--,q))", "(--p && --q)");
    }

    @Test
    void ConjDisjNeg() {
        assertEq("((--,x)&&y)", "(--x && (||,y,x))");
    }

    @Test
    void ConjParallelsMixture2() {
        assertEq("(b ==>+1 (a&&x))", $$("(b ==>+1 (a&&x))"));

        var c = new ConjList();

        c.add(0L, x);

        c.add(1L, x);
        c.add(1L, y);

        c.add(2L, x);
        c.add(2L, y);
        c.add(2L, z);
        assertEquals(6, c.size());
        assertEquals(3, c.eventOccurrences());

        var result = c.paralellize(terms);
        assertEquals(3, c.size());
        assertEquals(3, c.eventOccurrences());

    }


    @Test
    void ConjParallelsMixture3() {

        assertEq("((a &&+1 (b1&&b2)) &&+1 c)", "((a &&+1 (b1&|b2)) &&+1 c)");


    }

    @Test
    void ConjParallelWithNegMix() {
        var x = "((--,(x &| y)) &| (--,y))";
        assertEquals($$(x).toString(), $$($$(x).toString()).toString());

        assertEq("(--,y)",
                x);

        assertEq("(--,y)",
                "((--,(x&|y))&|(--,y))");


        assertEquals("((--,(x&&y)) &&+1 (--,y))", $$("((--,(x &| y)) &&+1 (--,y))").toString());

    }

    @Test
    void ConjParallelWithNegMix2() {
        assertEq("(--,y)", "((--,(x && y)) && (--,y))");
    }
    @Test
    void ConjParallelWithNegMix3() {
        assertEq("(--,y)", "((--,(x &&+1 y)) && (--,y))");
    }

    @Test
    void CommutiveTemporality1() {
        testParse("(at(SELF,b)&&goto(a))", "(goto(a)&&((SELF,b)-->at))");
        testParse("(at(SELF,b)&&goto(a))", "(goto(a)&|((SELF,b)-->at))");
        testParse("(at(SELF,b) &&+5 goto(a))", "(at(SELF,b) &&+5 goto(a))");
    }

    @Test
    void CommutiveTemporality2() {
        testParse("(at(SELF,b)&&goto(a))");
        testParse("(at(SELF,b) &&+5 goto(a))");
        testParse("(goto(a) &&+5 at(SELF,b))");
    }

    @Test
    void CommutiveTemporalityDepVar1() {
        testParse("(goto(#1) &&+5 at(SELF,#1))");
    }

    @Test
    void CommutiveTemporalityDepVar2() {
        testParse("(goto(#1) &&+5 at(SELF,#1))", "(goto(#1) &&+5 at(SELF,#1))");
        testParse("(at(SELF,#1) &&+5 goto(#1))", "(goto(#1) &&-5 at(SELF,#1))");
    }

    @Test
    void WrappingCommutiveConjunctionX_2() {
        var xAndRedundant = $$("((x &&+1 x)&&x)");
        assertEquals("(x &&+1 x)",
                xAndRedundant.toString());
    }



    @Test
    void ConjCommutivity() {

        assertEquivalentTerm("(&&,a,b)", "(&&,b,a)");
        assertEquivalentTerm("(&&,(||,(b),(c)),(a))", "(&&,(a),(||,(b),(c)))");
        assertEquivalentTerm("(&&,(||,(c),(b)),(a))", "(&&,(a),(||,(b),(c)))");
        assertEquivalentTerm("(&&,(||,(c),(b)),(a))", "(&&,(a),(||,(c),(b)))");
    }

    @Test
    void Conjunction1Term() throws Narsese.NarseseException {

        assertEquals("a", $("(&&,a)").toString());
        assertEquals("x(a)", $("(&&,x(a))").toString());
        assertEquals("a", $("(&&,a, a)").toString());

        assertEquals("((before-->x) &&+10 (after-->x))",
                $("(x:after &&-10 x:before)").toString());
        assertEquals("((before-->x) &&+10 (after-->x))",
                $("(x:before &&+10 x:after)").toString());


    }

    @Test
    void EmptyConjResultTerm() {
        ConjBuilder c = new ConjTree();
        assertEquals(True, c.term());
    }

    @Test
    void EmptyConjTrueEternal() {
        ConjBuilder c = new ConjTree();
        c.add(ETERNAL, True);
        assertEquals(True, c.term());
    }

    @Test
    void EmptyConjTrueTemporal() {
        ConjBuilder c = new ConjTree();
        c.add(0, True);
        assertEquals(True, c.term());
    }

    @Test
    void EmptyConjFalseEternal() {
        ConjBuilder c = new ConjTree();
        c.add(ETERNAL, False);
        assertEquals(False, c.term());
    }

    @Test
    void EmptyConjFalseTemporal() {
        ConjBuilder c = new ConjTree();
        c.add(0, False);
        assertEquals(False, c.term());
    }

    @Test
    void EmptyConjFalseEternalShortCircuit() {
        ConjBuilder c = new ConjTree();
        c.add(ETERNAL, $$("x"));
        var addedFalse = c.add(ETERNAL, False);
        assertFalse(addedFalse);
        //boolean addedAfterFalse = c.addAt($$("y"), ETERNAL);
        assertEquals(False, c.term());
    }

    @Test
    void EmptyConjFalseTemporalShortCircuit() {
        ConjBuilder c = new ConjTree();
        c.add(0, $$("x"));
        var addedFalse = c.add(0, False);
        assertFalse(addedFalse);
        //boolean addedAfterFalse = c.addAt($$("y"), 0);
        assertEquals(False, c.term());
    }

    @Test
    void ReducibleDisjunctionConjunction0() {
        assertEq("x", $$("((x||y) && x)"));
    }

    @Test
    void ReducibleDisjunctionConjunction1() {

        for (var dt : new int[]{DTERNAL, 0}) {
            var s0 = "(x||y)";
            var x0 = $$(s0);
            var x = CONJ.the(dt, x0, $$("x"));
            assertEquals("x", x.toString());
        }
    }

    @Test
    void ReducibleDisjunctionConjunction2() {
        assertEq("(x&&y)", $$("((||,x,y,z)&&(x && y))").toString());
    }

    @Test
    void ReducibleDisjunctionConjunction3() {
        assertEquals("(--,x)", $$("((||,--x,y)&& --x)").toString());
    }

    @Test
    void InvalidAfterXternalToNonXternalDT() {
        //String s = "((--,((||,dex(fz),reward(fz))&&dex(fz))) &&+- dex(fz))";
        var x = $$c("((--x &&+1 y) &&+- x)");
        assertEquals(False, x.dt(0));
        assertEquals(False, x.dt(DTERNAL));
//        assertThrows(TermException.class, ()->
//            x.dt(1)
//        );

    }

    @Test
    void InvalidSubsequenceComponent2() {
        var s = $$("(--,((x||y)&&z))");
        assertEq("(&&,(--,x),(--,y),z)", CONJ.the(s, DTERNAL, $$("z")).toString()); //TODO check
    }

    @Test
    void SortingTemporalConj() {
        assertEquals(0, $$("(x &&+1 y)").compareTo($$("(x &&+1 y)")));

        assertEquals(-1, $$("(x &| y)").compareTo($$("(x &&+1 y)")));
        assertEquals(-1, $$("(x &&+1 y)").compareTo($$("(x &&+2 y)")));
        assertEquals(-1, $$("(x &&-1 y)").compareTo($$("(x &&+1 y)")));

        assertEquals(+1, $$("(x &&+2 y)").compareTo($$("(x &&+1 y)")));
        assertEquals(+1, $$("(x &&+10 y)").compareTo($$("(x &&-10 y)")));

        assertEquals(-1, $$("(x &&+1 y)").compareTo($$("(x &&+10 y)")));
    }

    @Test
    void ConjConceptualizationWithFalse() {
        assertEquals(False, $$("((--,chronic(g))&&((--,up)&|false))"));
    }

    @Test
    void ParallelFromEternalIfInXTERNAL() {
        assertEq("((a&&x) &&+- (a&&y))", "((a&&x) &&+- (a&&y))");
    }

//    @Test
//    public void IndepVarWTF() {
//        assertEq("(x1&&$1)", ((ConjBuilder) new ConjTree()).with(ETERNAL, $$("(&&,x1,$1)")).term());
//        assertEq("(x1&&$1)", ((ConjBuilder) new ConjTree()).with(0, $$("(&&,x1,$1)")).term());
//    }


    @Test
    void ConjSorting() throws Narsese.NarseseException {
        var ea = $("(x&&$1)");
        assertEquals("(x&&$1)", ea.toString());
        var eb = $("($1&&x)");
        assertEquals("(x&&$1)", eb.toString());
        var pa = $("(x&|$1)");
        assertEquals("(x&&$1)", pa.toString());
        var pb = $("($1&&x)");
        assertEquals("(x&&$1)", pb.toString());
        var xa = $("($1 &&+- x)");
        assertEquals("(x &&+- $1)", xa.toString());
        var xb = $("(x &&+- $1)");
        assertEquals("(x &&+- $1)", xb.toString());

        assertEquals(ea, eb);
        assertEquals(ea.dt(), eb.dt());
        assertEquals(ea.subterms(), eb.subterms());

        assertEquals(pa, pb);
        assertEquals(pa.dt(), pb.dt());
        assertEquals(ea.subterms(), pa.subterms());
        assertEquals(ea.subterms(), pb.subterms());

        assertEquals(xa, xb);
        assertEquals(xa.dt(), xb.dt());
        assertEquals(ea.subterms(), xa.subterms());
        assertEquals(ea.subterms(), xb.subterms());
    }

    @Test
    void Retemporalization1_a1() {
        assertEq("((--,y)&&x)", $$("((--,(x&&y))&&x)"));
    }
    @Test
    void Retemporalization1_a2() {
        assertEq("((--,y)&&x)", $$("((--,(x &&+1 y))&&x)"));
    }

    @Test
    void Retemporalization1c() {

        assertEq(False, $$("((--,(x &&+1 x))&&x)"));
        assertEq(False, $$("((--,x)&&x)"));
    }

    @Disabled @Test
    void Retemporalization1b() throws Narsese.NarseseException {
        var x = $(
                "a(x,(--,((--,(x &&+1 x)) &&+- x)))"
        );
        assertEquals("a(x,((x &&+1 x) ||+- (--,x)))",
            x.toString());
//        assertEquals("a(x,true)",
//            Retemporalize.retemporalizeXTERNALToDTERNAL.apply(x).toString()
//        );
    }



    @Test
    void CommutiveTemporalityConjEquiv() {


        testParse("(({(row,3)}-->$1) &&+20 (#2-->$1))", "((#1-->$2) &&-20 ({(row,3)}-->$2))");
    }

    @Test
    void CommutiveTemporalityConjEquiv2() {
        testParse("(({(row,3)}-->$1) &&+20 (#2-->$1))", "(({(row,3)}-->$2) &&+20 (#1-->$2))");
    }

    @Test
    void CommutiveTemporalityConj2() {
        testParse("(goto(a) &&+5 at(SELF,b))", "(goto(a) &&+5 ((SELF,b)-->at))");
    }

    @Test
    void CommutiveConjTemporal() throws Narsese.NarseseException {
        var x = $("(a &&+1 b)");
        assertEquals("a", x.sub(0).toString());
        assertEquals("b", x.sub(1).toString());
        assertEquals(+1, x.dt());
        assertEquals("(a &&+1 b)", x.toString());

        var y = $("(a &&-1 b)");
        assertEquals("a", y.sub(0).toString());
        assertEquals("b", y.sub(1).toString());
        assertEquals(-1, y.dt());
        assertEquals("(b &&+1 a)", y.toString());

        var z = $("(b &&+1 a)");
        assertEquals("a", z.sub(0).toString());
        assertEquals("b", z.sub(1).toString());
        assertEquals(-1, z.dt());
        assertEquals("(b &&+1 a)", z.toString());

        var w = $("(b &&-1 a)");
        assertEquals("a", w.sub(0).toString());
        assertEquals("b", w.sub(1).toString());
        assertEquals(+1, w.dt());
        assertEquals("(a &&+1 b)", w.toString());

        assertEquals(y, z);
        assertEquals(x, w);

    }

    @Test
    void ReversibilityOfCommutive() throws Narsese.NarseseException {
        for (var c : new String[]{"&&"/*, "<=>"*/}) {
            assertEquals("(a " + c + "+5 b)", $("(a " + c + "+5 b)").toString());
            assertEquals("(b " + c + "+5 a)", $("(b " + c + "+5 a)").toString());
            assertEquals("(a " + c + "+5 b)", $("(b " + c + "-5 a)").toString());
            assertEquals("(b " + c + "+5 a)", $("(a " + c + "-5 b)").toString());

            assertEquals($("(b " + c + "-5 a)"), $("(a " + c + "+5 b)"));
            assertEquals($("(b " + c + "+5 a)"), $("(a " + c + "-5 b)"));
            assertEquals($("(a " + c + "-5 b)"), $("(b " + c + "+5 a)"));
            assertEquals($("(a " + c + "+5 b)"), $("(b " + c + "-5 a)"));
        }
    }

    @Test
    void CommutiveWithCompoundSubterm() throws Narsese.NarseseException {
        var a = $("(((--,(b0)) &| (pre_1)) &&+10 (else_0))");
        var b = $("((else_0) &&-10 ((--,(b0)) &| (pre_1)))");
        assertEquals(a, b);

        var c = CONJ.the($("((--,(b0)) &| (pre_1))"), 10, $("(else_0)"));
        var d = CONJ.the($("(else_0)"), -10, $("((--,(b0)) &| (pre_1))"));


        assertEquals(b, c);
        assertEquals(c, d);
        assertEquals(a, c);
        assertEquals(a, d);
    }

    @Test
    void ConjEarlyLate() throws Narsese.NarseseException {
        {
            var yThenZ = $("(y &&+1 z)");
            assertEquals("y", yThenZ.sub(Cond.condEarlyLate(yThenZ, true)).toString());
            assertEquals("z", yThenZ.sub(Cond.condEarlyLate(yThenZ, false)).toString());
        }
        var yThenZ = $("(y &| z)");
        assertEquals("y", yThenZ.sub(Cond.condEarlyLate(yThenZ, true)).toString());
        assertEquals("z", yThenZ.sub(Cond.condEarlyLate(yThenZ, false)).toString());

        var zThenY = $("(z &&+1 y)");
        assertEquals("z", zThenY.sub(Cond.condEarlyLate(zThenY, true)).toString());
        assertEquals("y", zThenY.sub(Cond.condEarlyLate(zThenY, false)).toString());

    }

    @Test
    void DTRange() throws Narsese.NarseseException {
        assertEquals(1, $("(z &&+1 y)").seqDur());
    }

    @Test
    void DTRange2() throws Narsese.NarseseException {
        var x = "(x &&+1 (z &&+1 y))";
        var t = $(x);
        assertEq("((x &&+1 z) &&+1 y)", t.toString());
        assertEquals(2, t.seqDur(), () -> t + " incorrect dtRange");
    }

    @Test
    void DTRange3() throws Narsese.NarseseException {
        assertEquals(4, $("(x &&+1 (z &&+1 (y &&+2 w)))").seqDur());
    }

    @Test
    void DTRange4() throws Narsese.NarseseException {
        assertEquals(4, $("((z &&+1 (y &&+2 w)) &&+1 x)").seqDur());
    }

    @Test
    void Commutivity() throws Narsese.NarseseException {

        assertTrue($("(b && a)").COMMUTATIVE());
        assertTrue($("(b &| a)").COMMUTATIVE());
        assertTrue($("(b &&+- a)").COMMUTATIVE());


        var abc = $("((a &| b) &| c)");
        assertEq("(&&,a,b,c)", abc);
        assertTrue(abc.COMMUTATIVE());

//        assertFalse($("(b &&+1 a)").isCommutative());
    }

    @Test
    void InvalidConjunction() {

        var x = $$c("(&&,a,b,c)");
        assertNotNull(x);
        assertThrows(TermException.class, () -> x.dt(-1));
        assertThrows(TermException.class, () -> x.dt(+1));
        assertNotEquals(Null, x.dt(0));
        assertNotEquals(Null, x.dt(DTERNAL));
        assertNotEquals(Null, x.dt(XTERNAL));
    }

    @Test
    void Retermporalization1() {

        var st = "((--,(happy)) && (--,((--,(o))&&(happy))))";
        var t = $$c(st);
        assertEquals("(--,(happy))", t.toString());



    }

    @Test
    void XternalConjCommutiveAllowsPosNeg() {
        var s =
                //"( &&+- ,(--,x),x,y)";
                "( &&+- ,x,(--,x),y)";
        assertEquals(s,
                CONJ.the(XTERNAL, x, x.neg(), y).toString());
//        assertEquals(s,
//                CONJ.the(XTERNAL, y, x, x.neg()).toString());
    }

    @Test
    void Conceptual2() throws Narsese.NarseseException {

        var x = $("((--,(vy &&+- happy)) &&+- (happy &&+- vy))");
        assertInstanceOf(Compound.class, x);

//        Term y = assertEq(
//                "((--,(vy &&+84 happy))&&(vy&|happy))",
//                "((--,(vy &&+84 happy))&&(happy&|vy))");
//        assertEquals(
//
//                "( &&+- ,(--,(vy &&+- happy)),vy,happy)",
//                y.concept().toString());

    }

    //public static final Retemporalize retemporalizeXTERNALToDTERNAL = new RetemporalizeFromTo(XTERNAL, DTERNAL);
    public static final Retemporalize retemporalizeXTERNALToZero = new Retemporalize.RetemporalizeFromTo(XTERNAL, 0);

    @Test
    void Retermporalization2() throws Narsese.NarseseException {
        var su = "((--,(happy)) &&+- (--,((--,(o))&&+-(happy))))";
        var u = $(su);
        assertEquals("(((o) ||+- (--,(happy))) &&+- (--,(happy)))", u.toString());

//        Term ye = retemporalizeXTERNALToDTERNAL.apply(u);
//        assertEquals("(--,(happy))", ye.toString());

        var yz = retemporalizeXTERNALToZero.apply(u);
        assertEquals("(--,(happy))", yz.toString());

    }

    /**
     * conjunction and disjunction subterms which can occurr as a result
     * of variable substitution, etc which don't necessarily affect
     * the resulting truth of the compound although if the statements
     * were alone they would not form valid tasks themselves
     */
    @Test
    void SingularStatementsInConjunction() throws Narsese.NarseseException {
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a<->a),c:d,e:f)"));

        assertEq($("(&&,c:d,e:f)"), "(&&,(a-->a),c:d,e:f)");
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a==>a),c:d,e:f)"));
        assertEq(False, "(&&,(--,(a==>a)),c:d,e:f)");

    }

    @Test
    void SingularStatementsInDisjunction() {
        assertInvalids("(||,(a<->a),c:d,e:f)");
    }

    @Test
    void SingularStatementsInDisjunction2() {
        assertEq("(y-->x)", "(&&,(||,(a<->a),c:d,e:f),x:y)");
        assertEq(False, "(&&,(--,(||,(a<->a),c:d,e:f)),x:y)");


    }

    @Test
    void EternalDisjunctionToParallel() {

        assertEq("(||,x,y,z)", "(||,(||,x,y),z)");
        assertEq("(||,x,y,z)", "(||,--(--x && --y), z)");
        assertEq("(||,x,y,z)", "(--,(((--,x)&|(--,y))&|(--,z)))");
        //assertEq("(--,(&|,(--,x),(--,y),(--,z)))", "(||,--(--x &| --y), z)");
        assertEq("(||,x,y,z)", "(||,--(--x &| --y), z)");

    }

    @Test
    void DisjunctionStackOverflow() {
        assertEq("(||,x,y,z)", "((x || y) || z)");
        assertEq("((y||z)&&(--,x))", "((||,x,y,z) && --x)");

        assertEq("((y||z)&&(--,x))", "(--(&|, --x, --y, --z) && --x)");
        assertEq("x", "((||,x,y,z) && x)");

        assertEq("x", "(--(&|, --x, --y, --z) && x)");

    }

    @Test
    void DisjunctEqual() {
        var pp = p(x);
        assertEquals(pp, DISJ(pp, pp));
    }

    @Test
    void DisjReduction1() {

        Term x = $.atomic("x");
        assertEquals(x, DISJ(x, x));
        assertEquals(x, CONJ.the(x.neg(), x.neg()).neg());
    }

    @Disabled @Test
    void ConjParallelWithSeq1() {
        assertEq("(a &&+5 b)", "((a &&+5 b)&|a)");
    }

    @Test
    void ConjParallelWithSeq3() {
        assertEq("(a &&+5 (a&&b))", "((a &&+5 b)&&a)");
    }

    @Test
    void EmbeddedConjNormalizationN2() throws Narsese.NarseseException {
        var bad = $("(a &&+1 (b &&+1 c))");
        var good = $("((a &&+1 b) &&+1 c)");
        assertEquals(good, bad);
        assertEquals(good.toString(), bad.toString());
        assertEquals(good.dt(), bad.dt());
        assertEquals(good.subterms(), bad.subterms());
    }

    @Test
    void EmbeddedConjNormalizationN2Neg() throws Narsese.NarseseException {
        var alreadyNormalized = $("((c &&+1 b) &&+1 a)");
        var needsNormalized = $("(a &&-1 (b &&-1 c))");
        assertEquals(alreadyNormalized, needsNormalized);
        assertEquals(alreadyNormalized.toString(), needsNormalized.toString());
        assertEquals(alreadyNormalized.dt(), needsNormalized.dt());
        assertEquals(alreadyNormalized.subterms(), needsNormalized.subterms());
    }

    @Test
    void EmbeddedConjNormalizationN3() throws Narsese.NarseseException {

        var ns = "((a &&+1 b) &&+1 (c &&+1 d))";
        var normal = $(ns);

        assertEquals(3, normal.seqDur());
        assertEquals(ns, normal.toString());

        for (var unnormalized : new String[]{
                "(a &&+1 (b &&+1 (c &&+1 d)))",
                "(((a &&+1 b) &&+1 c) &&+1 d)"
        }) {
            var u = $(unnormalized);
            assertEquals(normal, u);
            assertEquals(normal.toString(), u.toString());
            assertEquals(normal.dt(), u.dt());
            assertEquals(normal.subterms(), u.subterms());
        }
    }

    @Test
    void EmbeddedConjNormalizationWithNeg1() throws Narsese.NarseseException {
        var d = "(((d) &&+3 (a)) &&+1 (b))";

        var c = "((d) &&+3 ((a) &&+1 (b)))";
        var cc = $(c);
        assertEquals(d, cc.toString());

        var a = "(((a) &&+1 (b)) &&-3 (d))";
        var aa = $(a);
        assertEquals(d, aa.toString());


        assertTrue(aa.sub(0).subs() > aa.sub(1).subs());
        assertTrue(cc.sub(0).subs() > cc.sub(1).subs());

    }

    @Test
    void EmbeddedConjNormalization2() {
        assertEq("((a &&+1 b) &&+3 (c &&+5 d))", "(a &&+1 (b &&+3 (c &&+5 d)))");
        assertEq("(((t2-->hold) &&+1 (t1-->at)) &&+3 ((t1-->[opened]) &&+5 open(t1)))", "(hold:t2 &&+1 (at:t1 &&+3 ((t1-->[opened]) &&+5 open(t1))))");
    }

    @Test
    void ConjMergeABCShift() throws Narsese.NarseseException {
        /* WRONG:
            $.23 ((a &&+5 ((--,a)&|b)) &&+5 ((--,b) &&+5 (--,c))). 1⋈16 %1.0;.66% {171: 1;2;3;;} ((%1,%2,task(c),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))
              $.50 (a &&+5 (--,a)). 1⋈6 %1.0;.90% {1: 1}
              $.47 ((b &&+5 (--,b)) &&+5 (--,c)). 6⋈16 %1.0;.73% {43: 2;3;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
        */
        var a = $("(a &&+5 (--,a))");
        var b = $("((b &&+5 (--,b)) &&+5 (--,c))");
//        assertEq("((a &&+5 ((--,a)&&b)) &&+5 ((--,b) &&+5 (--,c)))", Op.terms.conjMerge(a, 5, b));
        assertEq("(((a &&+5 (--,a)) &&+5 b) &&+5 ((--,b) &&+5 (--,c)))", ConjSeq.conjAppend(a, 5, b, terms));
    }

    @Test
    void ConjunctionEqual() {
        assertEquals(x, CONJ.the(x, x));
    }

    @Test
    void ImpossibleSubtermWrong() throws Narsese.NarseseException {
        var sooper = $$c("(cam(0,0) &&+3 ({(0,0)}-->#1))");
        var sub = $("cam(0,0)");
        assertTrue(sooper.contains(sub));
        assertFalse(sooper.impossibleSubTerm(sub));


    }

    @Test
    void ValidConjDoubleNegativeWTF() {
        assertEq("(x &&+1 x)", "((x &&+1 x) && x)");
    }

    @Test
    void ValidConjDoubleNegativeWTF2() {
        assertEquals(False, $$("((x &&+1 x) && --x)"));

        assertEq("((--,x) &&+1 x)", "((--x &&+1 x) &| --x)"); //matches at zero

        assertEquals(False, $$("((--x &&+1 x) && x)"));
        assertEquals(False, $$("((x &&+1 --x) && --x)"));
    }

    @Disabled @Test void ValidConjDoubleNegative0() {
        assertEq("x", "(--(--x &&+1 x) &| x)");
        assertEq(False, "((--x &&+1 x) &| (--x &&+1 --x))");
    }

    @Disabled @Test void NegSeqNormalization1() {
        assertEquals(5, $$c("((--,(x2 &&+5 (--,x2)))&&(--,x3))))").seqDur());
    }

    @Disabled @Test void NegSeqConjAppend() {
        //var tmp = $$("((--,x4) &&+5 ((--,(x3 &&+5 ((--,(x2 &&+5 (--,x2)))&&(--,x3)))) &&+10 x1))");

        var a = $$("--x1");
        var b = $$("((--,(x3 &&+5 ((--,(x2 &&+5 (--,x2)))&&(--,x3)))) &&+10 x1)");
        var tmp = ConjSeq.conjAppend(a, 5, b, terms);
        assertTrue(tmp.CONJ());

    }
    @Disabled @Test void NegSeqNormalization3() {
        for (var s : new String[] {
                          "((--,(x3 &&+5 ((--,(x2 &&+5 (--,x2)))&&(--,x3)))) &&+10 x1)",
            "((--,x4) &&+5 ((--,(x3 &&+5 ((--,(x2 &&+5 (--,x2)))&&(--,x3)))) &&+10 x1))",
            "((--,x1) &&+5 ((--,(x3 &&+5 ((--,(x2 &&+5 (--,x2)))&&(--,x3)))) &&+10 x1))",
            "((--,#1) &&+5 ((--,(#3 &&+5 ((--,(#2 &&+5 (--,#2)))&&(--,#3)))) &&+10 #1))"
        }) {
            var x = $$(s);
            assertTrue(x.CONJ(), ()->"unparsed: " + s);
            //assertEquals(s, x.toString());
            //assertEq("", x.normalize());
        }
    }

    @Test
    void Atemporalization2() throws Narsese.NarseseException {

        assertEquals("((--,y) &&+- y)", Retemporalize.retemporalizeAllToXTERNAL.apply($("(y &&+3 (--,y))")).toString());
    }

    @Test
    void MoreAnonFails() {

    }

    @Test
    void CommutizeRepeatingConjunctions() {
        assertEquals("a",
                $$c("(a &&+1 a)").dt(DTERNAL).toString());
        assertEquals(False,
                $$c("(a &&+1 --a)").dt(DTERNAL));

        assertEquals("a",
                $$c("(a &&+1 a)").dt(0).toString());
        assertEquals(False,
                $$c("(a &&+1 --a)").dt(0));

    }

    @Test
    void CommutizeRepeatingConjunctions2() {

        assertEquals("(a &&+- a)",
                $$c("(a &&+1 a)").dt(XTERNAL).toString());
        assertEquals("((--,a) &&+- a)",
                $$c("(a &&+1 --a)").dt(XTERNAL).toString());

    }

//    @Test
//    void ConjRepeatXternalEllipsisDontCollapse() {
//
//        assertEq("((%1..+ &&+- %1..+)&&(%2..+ &&+- %2..+))", $$("((%1..+ &&+- %1..+) && (%2..+ &&+- %2..+))"));
//
//        //construct by changed dt to XTERNAL from DTERNAL
//        assertEq("((%1..+ &&+- %1..+) &&+- (%2..+ &&+- %2..+))",
//                $$c("((%1..+ &&+- %1..+) && (%2..+ &&+- %2..+))").dt(XTERNAL));
//
//        //construct directly
//        assertEq("((%1..+ &&+- %1..+) &&+- (%2..+ &&+- %2..+))",
//                "((%1..+ &&+- %1..+) &&+- (%2..+ &&+- %2..+))");
//
//
//    }


    @Test
    void DisjunctionInnerDTERNALConj() {



        assertEq("((x &&+1 (--,x))&&(--,y))", "((x &&+1 --x) && --y)");
        var x = $$("((x &&+1 --x) && --y)");
        assertEq("(((--,y)&&x) &&+1 ((--,x)&&(--,y)))"
                //"((x &&+1 (--,x))&&(--,y))"
                , x);
        ConjBuilder c = new ConjTree();
        c.add(x.SEQ() ? 0 : ETERNAL, x);
        assertEq(x, c.term());

//        assertEquals(1, xc.eventCount(0));
//        assertEquals(1, xc.eventCount(1));
//        assertEquals(1, xc.eventCount(ETERNAL));


        //            assertTrue(xc.removeEventsByTerm($$("x"), true, false));
//            assertEq("((--,x)&&(--,y))", xc.term());
    }

    @Test
    void ConjOneNonEllipsisDontRepeat() {
        assertEq("x", "(&&,x)");
        assertEq("x", "(&&+- , x)");
    }


    @Test
    void ConjEternalConj1() {
        //construction method 1
        assertEq(False, CONJ.the($$("(((left-->g) &&+270 (--,(left-->g))) &&+1070 (right-->g))"), $$("(&&,(up-->g),(left-->g),(destroy-->g))")));
    }


    @Test
    void ConjEternalConj2Pre() {
        //(not ((not y)and x) and x) == x and y
        //https://www.wolframalpha.com/input/?i=(not+((not+y)and+x)+and+x)

        //construction method 1:
        var a = $$("((--,((--,y)&|x))&&x)");
        assertEq("(x&&y)", a);
    }


    @Test
    void SequenceInnerConj_Normalize_to_Ete() {

        var xy_wz = $$("((x &| y) &&+1 (w &| z))");
        assertEq("((x&&y) &&+1 (w&&z))", xy_wz);

    }

    //    @Test
//    void SubtimeFirst_of_Sequence() {
//        Term subEvent = $$("(((--,_3(_1,_2))&|_3(_4,_5)) &&+830 (--,_3(_8,_9)))").eventFirst();
//        assertEq("((--,_3(_1,_2))&&_3(_4,_5))", subEvent);
//        assertEquals(
//                20,
//                $$("((_3(_6,_7) &&+20 ((--,_3(_1,_2))&&_3(_4,_5))) &&+830 (--,_3(_8,_9)))").
//                        subTimeFirst(subEvent)
//        );
//
//    }
    @Test
    void conjWTFF() {
        ConjBuilder x = new ConjTree();
        x.add(ETERNAL, $$("a:x"));
        x.add(0, $$("a:y"));
        assertEq("((x-->a)&&(y-->a))", x.term());
    }

    @Test
    void conjWTFF2() {
        assertEq(False,
                $$("(&&, ((--,#1) &&+232 (--, (tetris --> curi))),(--, right),right)")
        );
    }

    @Test
    void ParallelizeDTERNALWhenInSequence() {
        assertEq("((a&&b) &&+1 c)", "((a&&b) &&+1 c)");
        assertEq("((a&&b) &&+- c)", "((a&&b) &&+- c)"); //xternal: unaffected

        assertEq("(c &&+1 (a&&b))", "(c &&+1 (a&&b))");
    }

    @Test
    void ParallelizeImplAFTERSequence() {
        assertEq("((a &&+1 b)==>x)", "((a &&+1 b) ==> x)");
        //assertEq("(x =|> (a &| b))","(x ==> (a &| b))");
        //assertEq("(x =|> (a &&+1 b))","(x ==> (a &&+1 b))");
    }

    @Test
    void ElimDisjunctionDTERNAL_WTF() {
        assertEq("(--,right)", "((--,(left&&right))&&(--,right))");
    }

    @Test
    void SimpleEternals() {
        ConjBuilder c = new ConjTree();
        c.add(ETERNAL, x);
        c.add(ETERNAL, y);
        assertEquals("(x&&y)", c.term().toString());
        assertEquals(1, c.eventOccurrences());
//        assertEquals(byte[].class, c.event.get(ETERNAL).getClass());
    }


//    @Test
//    void RoaringBitmapNeededManyEventsAtSameTime() {
//        ConjBuilder b = new ConjTree();
//        for (int i = 0; i < Conj.ROARING_UPGRADE_THRESH - 1; i++)
//            b.add(1, $.the(String.valueOf((char) ('a' + i))));
//        assertEquals("(&&,a,b,c,d,e,f,g)", b.term().toString());
////        assertEquals(1, b.event.size());
////        assertEquals(byte[].class, b.event.get(1).getClass());
//
////        ConjBuilder b = new ConjTree();
////        for (int i = 0; i < Conj.ROARING_UPGRADE_THRESH + 1; i++)
////            c.add(1, $.the(String.valueOf((char) ('a' + i))));
////        assertEquals("(&&,a,b,c,d,e,f,g,h,i)", c.term().toString());
////        assertEquals(1, c.event.size());
////        assertEquals(RoaringBitmap.class, c.event.get(1).getClass());
//    }

    @Test
    void SimpleEventsNeg() {
        ConjBuilder c = new ConjTree();
        c.add(1, x);
        c.add(2, y.neg());
        assertEquals("(x &&+1 (--,y))", c.term().toString());
    }

    @Test
    void invalidXternal65() {
        var x0 = $$("((--,(x||(--,y)))&&x)");
        assertEq(False, x0);
    }

//    @Disabled
//    @Test
//    void invalidXternal67() {
//        //TODO test
//        Term y = CONJ.build(
//                $$("(--,((--,((x||(--,y)) &&+- y))&&x))"),
//                $$("x")
//        );
//        assertEq("x", y);
//        //assertEq(, y.root());
//    }


    @Disabled
    @Test
    void invalidXternal66() {
        //TODO test
        var x = $$("((--,((x||(--,y)) &&+- y))&&x)");
        assertEq(False, x);
    }

    @Test
    void internalDisjCausingStackOverflow() {


        assertEq("c", CondDiff.diffFirst($$("((--,b) &&+- c)"), $$("--b"), false));
        //assertEquals(2, ConjList.events($$("((--,b) &&+- c)")).size());
    }

    @Test
    void internalDisjCausingStackOverflow2() {
        assertEq("((--,b)&&(--,c))", $$("( --(--b && c) && --b)"));
    }

    @Test
    void internalDisjCausingStackOverflow3() {
        assertEq("((--,b)&&(--,c))", $$("( --(--b &&+- c) && --b)"));
    }

    @Test
    void internalDisjCausingStackOverflow4() {

        assertEq("((--,b)&&c)", $$("( (--b && c) && --b)"));
        assertEq("(((--,b)&&c) &&+- (--,b))", $$("( (--b &&+- c) && --b)"));
    }

    @Test
    void internalDisjCausingStackOverflow5a() {

        assertEq("((--,b)&&(--,c))", $$("(--(--b &&   c) && --b)"));
    }

    @Test
    void internalDisjCausingStackOverflow5b1() {
        assertEq("((--,b)&&(--,c))", $$("(--(--b &&+- c) && --b)"));
    }

    @Test
    void internalDisjCausingStackOverflow5b2() {
        assertEq("((--,c)&&b)", $$("(--(b &&+- c) && b)"));
    }

    @Test
    void internalDisjCausingStackOverflow5c() {

        assertEq("(((--,b)&&(--,c)) &&+- ((--,b)&&d))", $$("(&&, (--(--b &&+- c) &&+- d), (--,b))"));
    }

    @Test
    void internalDisjCausingStackOverflowX() {
        //[_4, ((_2 ||+- (--,_3)) &&+- _4), ((--,_2) &&+- _4), (--,_2), (--,_3)]
        //[d, ((b ||+- (--,c)) &&+- d), ((--,b) &&+- d), (--,b), (--,c)]

        assertEq(//"(&&,((b ||+- (--,c)) &&+- d),((--,b) &&+- d),(--,b))"
                "((--,b)&&d)"
                , $$("(&&, (--(--b &&+- c) &&+- d), ((--,b) &&+- d), (--,b))"));
    }
    @Test
    void internalDisjCausingStackOverflowX_2() {

        assertEq(//"(&&,((b ||+- (--,c)) &&+- d),((--,b) &&+- d),(--,b),d)",
                "((--,b)&&d)",
                $$("(&&, d, (--(--b &&+- c) &&+- d), ((--,b) &&+- d), (--,b))"));

        //assertEq("TODO", $$("(&&, d, (--(--b &&+- c) &&+- d), ((--,b) &&+- d), (--,b), (--,c))"));

    }

    @Disabled @Test
    void wtftwf() {
        var c = new ConjList();
        c.add(ETERNAL, $$("(--,((--x &&+220 x) &&+60 x))"));
        c.add(ETERNAL, $$("--x"));
        assertEq("(--,x)", c.term());
    }

    @Test
    void collapsibleEquivalentMutex() {
        //((x && y) || (--x && y))    	 |-   y
        assertEq("y", "((x && y) || (--x && y))");
    }

    @Test
    void sequenceContradiction_pre() {
        assertEq(False, "(&&,a,b,(--a &&+1 b))");
    }
    @Test
    void sequenceContradiction_pre_neg() {
        assertEq(False, "(&&,--a,b,(a &&+1 b))");
    }
    @Test
    void sequenceContradiction() {
        ConjBuilder c = new ConjList();
        c.add(ETERNAL, $$("a"));
        c.add(ETERNAL, $$("b"));
        c.add(0, $$("(--a &&+1 b)"));
        assertEq(False, c.term());
    }

    @Test
    void anotherReduction() {

        var y = $$("((a||b)&&a)");
        assertEq("a", y);

        //((not ((a||b)&&a))&&(not b)) = (--a && --b)  https://www.wolframalpha.com/input/?i=%28%28not+%28%28a%7C%7Cb%29%26%26a%29%29%26%26%28not+b%29%29
        assertEq("((--,a)&&(--,b))", "((--,((a||b)&&a))&&(--,b))");


        assertTrue(((Compound) $$c("(0||1)").unneg()).condOf($$("0"), -1));
        assertFalse(((Compound) $$c("(0||1)").unneg()).condOf($$("0"), +1));

        var x = $$("((0||1)&&0)");
        assertEq("0", x);

        assertEq("((--,0)&&(--,1))", "((--,((0||1)&&0))&&(--,1))");
    }

    @Test
    void inhBundleConjParallel1() {
        assertEq("((x-->a)&&(x-->b))", "((x-->a) && (x-->b))");
    }


    @Test void moveNegsInside() {
        /*
         * !(P(x) ^ !Q(x))
         * |-
         * !P(x) v Q(x)
         */
        assertEq("(--P | Q)", "--(P & --Q)");
    }

    @Disabled @Test void invalidSeqEte1() {
        assertEq(False, $$("((--,((x-->a) &&+20 (x-->a)))&&(x-->a))"));
        assertEq(False, $$("((--,((x-->a) &&+20 (x-->a)))&&(x-->a))"));
    }

//    @Test void normalizeChangesOp2() {
//        String a =  "(((--,((--,(((#1,b),#2)-->#3)) &&+360 (--,(((#1,c),#2)-->#3)))) &&+56580 (want(\"-cîÑñ6iZCY\",(((tetris,a),meta)-->#4))&&(((tetris,c),meta)-->#4))) &&+3780 (&&,(((--,(\"-cîÑñ6iZCY\",happy(b,meta)))&&(\"-cîÑñ6iZCY\",happy((tetris,a),meta)))-->want),(--,want(\"-cîÑñ6iZCY\",happy((tetris,a),meta))),happy((tetris,c),meta)))";
//        String aa = "TODO";
//        Term b = $$$(a);
//        assertEq(a, b);
//        assertEq(aa, b.normalize());
//    }

    @Test void normalizeChangesOp() {
        var a = "(--,(#1-->(((#2,((#2,6)-->$3)),#4)-->$3)))";
        var b = "(--,(#1-->add((#2,#5),#4)))";
        var c = "(--,(#1-->#4)))";
        final var y = "(&&,(--,(#1-->(((#2,((#2,6)-->$3)),#4)-->$3))),(--,(#1-->add((#2,#5),#4))),(--,(#1-->#4)))";

        var y0 = $$$("(&&," + a + "," + b + "," + c + ")");

        assertEq(y, y0);

        var x = new ConjList(3);
        x.add(0L, $$$(a));
        x.add(0L, $$$(b));
        x.add(0L, $$$(c));
        assertEq(y, x.term());

    }
    @Test void SequenceInParallel_Conj() {
        assertEq("((pa&&#1) &&+60 (pa&&#1))", "((#1 &&+60 #1)&&pa)");
        assertEq("((&&,(--,$2),pa,#1) &&+60 (&&,(--,$2),pa,#1))", "(&&,(#1 &&+60 #1),pa,(--,$2))");
    }

    @Disabled @Test void SequenceInParallel_Disj() {
        assertCanonical("((#1 &&+60 #1)||pa)");
        assertCanonical("(||,(#1 &&+60 #1),pa,(--,$2))");
    }

    @Test void Factored_Sequence_Conflict() {
        var x = $$("((b-->(x&&z)) &&+770 (--,a))");
        var y = $$("--(b-->z)");
        assertEq(False, CONJ.the(x, y));
    }
    @Test void Factored_Sequence_Conflict2() {
        assertEq(False, "(((b-->(x&&z)) &&+770 (--,a))&&(--,(b-->z)))");
    }
    @Test void Factored_Sequence_Conflict3() {
        //non-sequence simpler case:
        assertEq(False, "((b-->(x&&z)) && (--,(b-->z)))");
    }
    @Test void Factored_Sequence_Conflict_3ary() {
        assertEq(False, "(&&,((--,(c-->(a&&b))) &&+620 x),(c-->a),(c-->b))");
    }

    @Test void Factored_Sequence_Distribute() {
        assertEq("(b &&+1 (b&&d))", "((b &&+1 d)&&b)");
    }
    @Test void Multiple_Seq_in_Parallel() {
        assertEq(Null, "((b &&+1 d)&&(a &&+2 c))");
        assertEq(Null, "((--,(b &&+1 d))&&(b &&+2 d))");
        assertEq(Null, "((--,(b &&+1 d))&&(a &&+2 c))");
    }

    @Test void sequenceContradiction_more() {
        assertEq(False,
                "((((x-->(p&&c)) &&+1210 (b-->p)) &&+30 Δ(x-->b))&&(--,(x-->p)))");
    }
    @Test void sequenceContradiction_more2() {
        assertEq(False,
                "((((x-->(p&&c)) &&+1210 (b-->p)) &&+30 Δ(x-->b))&&(--,(x-->(p&&c))))");
    }

    @Test void obviousSubElimination0() {
        assertEq(
                "(&&,a,(b||d),(c||d))" //CNF
                //"((&&,a,b,c)||(a&&d))" //DNF
                ,"(((&&,a,b,c)||d)&&a)");
    }

    @Test void obviousSubElimination1() {
        assertEq("(a&&d)",
                "(((&&,(--,a),b,c)||d)&&a)");
    }

    /** (((a and b and c) or d) and not a) */
    @Test void obviousSubElimination2() {
        assertEq("((--,a)&&d)",
            "(((&&,a,b,c)||d) && --a)");
    }

    @Test void obviousElimination3_par() {
        assertEq("((--,b)&&(--,h))",
                "(--(b && --(h && c)) && --h)");
    }
    @Disabled @Test void obviousElimination3_seq() {
        assertEq("((--,b)&&(--,h))",
            "((--,(b &&+740 (--,(h &&+800 c))))&&(--,h))");
    }

    @Disabled @Test void obviousElimination4_par_p() {
        assertEq("((--,(b-->w))&&(--,(b-->z)))",
                "(&&,(b-->(--w && --z)), --(b-->w))");
    }

    @Disabled @Test void obviousElimination4_par_n1() {
        assertTrue($$c("(b-->(--w && --z))").condOf($$("--(b-->w)")));
        assertFalse($$c("(b-->(--w && --z))").condOf($$("(b-->w)")));

        assertEq("((--,w)&&z)",
                "(--(--w && --z) && --w)");
        assertEq("((--,(b-->w))&&(b-->z))",
                "(--(b-->(--w && --z)) && --(b-->w))");
    }

    @Disabled @Test void obviousElimination4_par_n2() {
        assertEq(False,
                "(&&,--(b-->(--w && --z)), --(b-->w),--(b-->z))");
    }
    @Disabled @Test void obviousElimination4_seq() {
        assertEq(False,
                "(&&,(x &&+190 --(b-->(--w && --z))), --(b-->w),--(b-->z))");
    }

    @Disabled @Test void obviousElimination6_seq() {
        assertEq(False,
                "(((((tetris,d)-->(L&&speed)) &&+80 ((tetris,d)-->(L&&speed))) &&+2340 speed(tetris,d))&&(--,L(tetris,d)))");
    }

    /** (not(not(y) and x) and x) */
    @Test void DisjReductionOutwardCancel1() {
        assertEq("(x&&y)", "(--(--y && x) && x)");
    }

    @Test void DisjReductionOutwardCancel2() {
        assertEq("(x&&y)", "(--(--y &&+1 x) && x)");
    }

    /** x and (not (x and not y)) */
    @Test void DisjReductionOutwardCancel_tree_par() {
        //construction method 2:
        var c = new ConjTree();
        c.add(ETERNAL, x);
        c.add(ETERNAL, $$("(--,((--,y)&&x))"));
        assertEq("(x&&y)", c.term());
    }

    @Test void DisjReductionOutwardCancel_tree_seq() {
        var c = new ConjTree();
        c.add(ETERNAL, x);
        var notNotYThenX = $$("--(--y &&+1 x)");
        assertTrue(c.add(0, notNotYThenX));
        assertEq("(x&&y)", c.term());
    }

    @Test void DisjReductionOutwardCancel_tree_seq_neg() {
        var c = new ConjTree();
        c.add(ETERNAL, x.neg());
        var notNotYThenNotX = $$("--(--y &&+1 --x)");
        assertTrue(c.add(0, notNotYThenNotX));
        assertEq("((--,x)&&y)", c.term());
    }

    @Test void seqAndParContradiction() {
        assertEq(False, "((--,(x&&y))&&(x &&+1 y))");
    }
    @Test void seqAndParBundledContradiction() {
        assertEq(False, "((--,((x&&y)-->a))&&((x-->a) &&+1 (y-->a)))");
    }

    @Test
    void ConjEternalConj2a() {
        //construction method 2
        ConjBuilder xy = new ConjTree();
        xy.add(0, $$("((left-->g) &&+270 (--,(left-->g)))"));
        xy.add(ETERNAL, $$("(left-->g)"));
        assertEquals(False, xy.term());
    }

    @Test
    void ConjEternalConj2() {
        //construction method 2
        ConjBuilder xy = new ConjTree();
        xy.add(0, $$("(((left-->g) &&+270 (--,(left-->g))) &&+1070 (right-->g))"));
        xy.add(ETERNAL, $$("(&&,(up-->g),(left-->g),(destroy-->g))"));
        assertEquals(False, xy.term());
    }
    @Test
    void ConjEternalConj2_easy() {
        //construction method 2
        ConjBuilder xy = new ConjTree();
        xy.add(0, $$("((b &&+20 --b) &&+10 c)"));
        xy.add(ETERNAL, $$("(&&,a,b,d)"));
        assertEquals(False, xy.term());
    }

    @Disabled @Test void obviousElimination5_seq() {
        assertEq(False,
                "((((tetris,c)-->((--,(0,3))&&(--,(2,3)))) &&+2800 (--,((--,((tetris,c)-->(4,15))) &&+3010 (--,dense(tetris,c)))))&&(--,((tetris,c)-->((--,(4,15))&&(--,dense)))))");
    }

    @Test void imageNormalizeSeqSubterms() {
        var a = "(holds(john,football) &&+1 inside(john,playground))";
        var b = "((john-->(holds,/,football)) &&+1 (john-->(inside,/,playground)))";
        assertEq(a, b);

        //test XTERNAL
        assertEq(
            a.replace("+1", "+-"),
            b.replace("+1", "+-")
        );
    }
    @Test void normalizeWithDelta1() {
        var s =
            "(Δ(a &&+1 b) &&+1 #2)";
            //"Δ((z &&+340 (--,#2)) &&+35 #2)";
            //"(Δ((z &&+1(--,#2)) &&+1 #2) &&+1 #5)";
            //"((Δ(((tetris-->(#1,add(#1,4))) &&+340 (--,#2)) &&+35 #2) &&+2915 (--,#5)) &&+375 #5)";
            //"((z &&+2915 (--,#5)) &&+375 #5)";
        var x = $$$(s);
        //System.out.println(x);
        assertEq("(Δ(a &&+1 b) &&+1 #2)", x);

//        ConjList el = new ConjList();
//        el.add(0L, x);
//        assertEquals(3, el.size());

        var y = x.normalize();
        assertEq("(Δ(a &&+1 b) &&+1 #1)", y);
    }

    @Test void normalizeWithDelta2() {
        var x = $$$("((Δ((z &&+340 (--,#2)) &&+35 #2) &&+2915 (--,#5)) &&+375 #5)");
        var y = x.normalize();
        assertEq("((Δ((z &&+340 (--,#1)) &&+35 #1) &&+2915 (--,#2)) &&+375 #2)", y);
    }

    @Test void seqInPar() {
        assertEq("((a&&c) &&+1 (b&&c))",
                "(&&,    (a &&+1 b), c)");
        assertEq("((&&,a,c,d) &&+1 (&&,b,c,d))",
                "(&&,    (a &&+1 b), c, d)");
    }

    /** TODO */
    @Test void negSeqInPar() {
        assertEq(Null,
                "(&&,(--,(a &&+1 b)),c)");
    }
    @Test void orPair2() {
        assertEq("((y&&z)||x)",
                "((||,x,y)&&(||,x,z))");
    }
    @Test void orPair3() {
        assertEq("(((a||b)&&(y||z))||x)",
                "((||,x,y,z)&&(||,x,a,b))");
    }
    @Test void orPair3var() {
        assertEq("(((a||#1)&&(y||#2))||x)",
                "((||,x,y,#1)&&(||,x,a,#2))");
    }
    @Test void disjdisj2() {
        //((A||B) && (A||C)) to (A || (B && C))
        assertEq("((b&&c)||a)",
                "((a||b) && (a||c))");
        assertEq("((b&&c)||(--,a))",
                "((--a||b) && (--a||c))");
    }
    @Test void disjdisj3() {
        assertEq("((&&,b,c,d)||a)",
                "(&&,(a||b),(a||c),(a||d))");
    }
}