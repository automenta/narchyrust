package nars.term;

import nars.*;
import nars.term.atom.Bool;
import nars.term.util.Testing;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.DTERNAL;
import static nars.Op.XTERNAL;
import static nars.term.atom.Bool.False;
import static nars.term.atom.Bool.Null;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * tests specific to implication compounds TODO
 */
class ImplTest {
    @Test
    void InvalidImpl1() {
        assertEq(False, "(--y =|> y)");
    }

    @Test
    void InvalidImpl2() {
        assertEq(False, "(--(x &| y) =|> y)");
    }

    @Test
    void InvalidImpl3() {
        assertEq(False, "(--(--x &| y) =|> y)");
    }

    @Test
    void ReducibleImplFactored() {
        assertEq("((x&&y)==>z)", "((x &| y) ==> (y &| z))");
    }

//    @Test
//    void toomuchReduction() {
//        /** this took some thought but it really is consistent with the system */
//        assertEq("((b &&+60000 c)=|>(#1 &&+60000 (b&|#1)))",
//                "((b &&+60000 c)=|>((#1 &&+60000 b)&&(c &&+60000 #1)))");
//    }


    @Test
    void ReducibleImplFactored2a() {
        assertEq("((x&&y)==>z)", "((y && x) ==> (y && z))");
    }
    @Test
    void ReducibleInhImplFactored2a() {
        assertEq("(((a-->x)&&(a-->y))==>(a-->z))",
                "((x:a && y:a) ==> (y:a && z:a))");
    }

    @Test
    void ReducibleImplFactored2b() {
        assertEq("((&&,a,x,y)==>z)", "((&&, x, y, a) ==> (y && z))");
    }
    @Test
    void ReducibleImplFactored2c() {
        assertEq("((y &&+1 x)==>(z &&+1 y))", "((y &&+1 x)==>(z &&+1 y))");
    }

    @Test
    void ReducibleImplFactoredPredShouldRemainIntact() {

        for (String cp : new String[]{"&&", " &&+- "}) {
            assertEq("((x&&y) ==>+1 (y" + cp + "z))", "((y&&x) ==>+1 (y" + cp + "z))");
            assertEq("(a ==>+1 (b &&+1 (y" + cp + "z)))", "(a ==>+1 (b &&+1 (y" + cp + "z)))");
        }


    }

    /**
     * the + and - versions have distinct meanings that must be maintained
     */
    @Test
    void TemporalRepeatDoesNotNormalization() {
        assertEq("(x ==>-2 x)", "(x ==>-2 x)");
        assertEq("(x ==>+2 x)", "(x ==>+2 x)");
    }

    @Test
    void ReducibleImpl() {

        assertEq("(--,((--,x)==>y))", "(--x ==> (--y && --x))");

        assertEq("(x==>y)", "(x ==> (y &| x))");
        assertEq("(--,((--,$1)==>#2))", "((--,$1)==>((--,$1)&|(--,#2)))");
    }

    @Test
    void ReducibleImpl2() {
        assertEq(Bool.True, "((y &| x) ==> x)");
    }

    @Test
    void ReducibleImplConjCoNeg() {
        assertEq(False, "((y && --x) ==> x)");

        for (String i : new String[]{"==>"/*, "=|>"*/}) {
            for (String c : new String[]{"&&"}) {
                assertEq(False, "(x " + i + " (y " + c + " --x))");
                assertEq(False, "(--x " + i + " (y " + c + " x))");
                assertEq(False, "((y " + c + " --x) " + i + " x)");
                assertEq(False, "((y " + c + " x) " + i + " --x)");
            }
        }
    }


    @Test
    void ReducibleImplParallelNeg() {
        assertEq("(--,((--,x)==>y))", "(--x ==> (--y && --x))");
    }

    @Test
    void ReducibleImplParallelNeg2() {
        assertEq(Bool.True, "((--y && --x) ==> --x)");
    }

    @Test
    void InvalidCircularImpl() throws Narsese.NarseseException {
        assertNotEquals(Null, $("(x(intValue,(),1) ==>+10 ((--,x(intValue,(),0)) &| x(intValue,(),1)))"));
        assertEq("(--,(x(intValue,(),1)==>x(intValue,(),0)))", "(x(intValue,(),1) ==> ((--,x(intValue,(),0)) &| x(intValue,(),1)))");
    }

    @Test
    void InvalidCircularImpl2() {
        assertEq("(--,(x(intValue,(),1)==>x(intValue,(),0)))", "(x(intValue,(),1) ==> ((--,x(intValue,(),0)) &| x(intValue,(),1)))");
    }

    @Test
    void ImplInImpl2() {
        assertEq("(((--,(in))&&(happy))==>(out))",
                "((--,(in)) ==> ((happy)  ==> (out)))");
    }

    @Test
    void ImplInImplDTemporal() {
        assertEq("(((--,(in)) &&+1 (happy)) ==>+2 (out))", "((--,(in)) ==>+1 ((happy) ==>+2 (out)))");
    }

    @Disabled /** TODO helpful? */ @Test void implicationInSubj() {
        assertEq("((R==>P)==>Q)", "((R==>P)==>Q)"); //unchanged
        assertEq("((--,(R==>P))==>Q)", "(--(R==>P)==>Q)"); //unchanged
    }

    @Test
    void implicationInPred() {
        assertEq("((P&&R)==>Q)", "(R==>(P==>Q))");
        assertEq("((R &&+2 P) ==>+1 Q)", "(R ==>+2 (P ==>+1 Q))");
        assertEq("(((S &&+1 R) &&+2 P) ==>+1 Q)", "((S &&+1 R) ==>+2 (P ==>+1 Q))");
        assertEq("((x&&y) ==>+1 z)", "(x==>(y ==>+1 z))");
        assertEq("((x &&+1 y)==>z)", "(x ==>+1 (y==>z))");
        assertEq("((x &&+1 y) ==>+1 z)", "(x ==>+1 (y ==>+1 z))");
    }

    @Test
    void ImplInImplA_pred() {
        assertEq("((a&&b)==>c)", "(a==>(b==>c))");
    }

    @Test void ImplInImplDTernal_pred_neg_inner() {
        assertEq("(((--,b)&&a)==>c)", "(a==>(--b==>c))");
    }
    @Test void ImplInImplDTernal_pred_neg_outer() {
        assertEq("(--,((a&&b)==>c))", "(a==>--(b==>c))");
    }
    /** TODO helpful? */ @Test @Disabled void ImplInImpl_subj() {
        assertEq("(a==>(b&&c))", "((a==>b)==>c)");
    }
    /** TODO helpful? */ @Test @Disabled void NegImplInImplDTernal_subj() {
        assertEq("(a==>((--,b)&&c))", "(--(a==>b)==>c)");
    }

    @Disabled @Test
    void ImplInConjPos() {
        String s = "((c==>a)&&a)";
        assertEquals(s, s);
    }

    @Test
    void ImplInConjNeg() throws Narsese.NarseseException {
        if (NAL.term.IMPL_IN_CONJ) {
            String s = "((--,(c==>a))&&(--,a))";
            assertEquals(

                    s,
                    $(s).toString());
        }
    }

    @Disabled
    @Test
    void ImplInConj2xPos() throws Narsese.NarseseException {
        String s = "((c==>a)&&(d==>a))";
        assertEquals(
                s,
                $(s).toString());
    }

    @Test
    void ImplInConj2xNeg() throws Narsese.NarseseException {
        if (NAL.term.IMPL_IN_CONJ) {
            String s = "((--,(c==>a))&&(--,(d==>a)))";

            assertEquals(

                    s,
                    $(s).toString());
        }
    }


    @Test
    void implSubjSimultaneousWithTemporalPred() {
        Term x = $$("((--,(tetris-->happy))==>(tetris(isRow,(2,true),true) &&+5 (tetris-->happy)))");
        assertEquals(
                "((--,(tetris-->happy))==>(tetris(isRow,(2,true),true) &&+5 (tetris-->happy)))",
                x.toString());
    }



    @Test
    void ImplXternalDternalPredicateImpl() {

        assertEq("((x &&+1 y) ==>+- z)", "(x ==>+1 (y ==>+- z))");
        assertEq("((x &&+- y) ==>+- z)", "(x ==>+- (y ==>+- z))");
        assertEq("((x &&+1 (y&&z)) ==>+1 w)", "((x &&+1 y) ==> (z ==>+1 w))");

        assertEq("(((x &&+1 y) &&+1 z) ==>+1 w)", "((x &&+1 y) ==>+1 (z ==>+1 w))");

        //assertEq("((x &&+- y) ==>+1 z)", "(x ==>+- (y ==>+1 z))");
        //assertEq("(((x &&+1 y) &&+- z) ==>+1 w)", "((x &&+1 y) ==>+- (z ==>+1 w))");
    }

    @Test
    void implicationInPred_Collapse() {
        assertEq(Bool.True, "(R==>(P==>R))");
    }

    @Test
    void implicationInPred_Reduce() {
        assertEq("(R==>P)", "(R==>(R==>P))");
    }

    @Test
    void DepvarWTF() {

        /*
            $.03 (((--,#1)&&(--,#1))==>b). %1.0;.45% {2: 1;3} ((%1,%2,(--,is(%1,"==>"))),(((--,%1) ==>+- %2),((AbductionN-->Belief),(TaskRelative-->Time),(VarIntro-->Also))))
              $.25 a. %0.0;.90% {0: 3}
              $.25 ((--,a)==>b). %1.0;.90% {0: 1}
         */
        assertEq("((--,#1)==>x)", "(((--,#1)&&(--,#1))==>x)");
    }

    @Test
    void Implicit_DTERNAL_to_Parallel() {
        assertEq("((x&&y)==>z)", "((x&&y)==>z)"); //unchanged
        assertEq("((x&&y) ==>+- z)", "((x&&y) ==>+- z)"); //unchanged

        assertEq("((x&&y)==>z)", "((x&&y)=|>z)");  //temporal now
        assertEq("((x&&y) ==>+1 z)", "((x&&y) ==>+1 z)");
        assertEq("(z==>(x&&y))", "(z=|>(x&&y))");
    }


    @Test
    void Elimination1() {
        assertEq(
                "(--,((left &&+60 left) ==>+5080 left))",
                "((left &&+60 left) ==>-60 (left &&+5140 (--,left)))"
        );
    }

    @Test
    void Elimination2() {
        assertEq(
                False,
                "((--,(left &&+2518 left))==>left)"
        );
    }


    @Test
    void Elimination3() {


        assertEq("(b ==>+1 (a&&x))", $$("(b ==>+1 (a&&x))"));

        Compound x1 = $.$$c("((a &&+5 b) ==>+- (b &&+5 c))");
        Term y1 = x1.dt(0);
        assertEq("((a &&+5 b) ==>+5 c)", y1);
    }

    @Test
    void Elimination4() {
        Compound x2 = $.$$c("((a &&+5 b) ==>+1 (b &&+5 c))");
        assertEq("((a &&+5 b) ==>+5 c)", x2.dt(0));
        assertEq("((a &&+5 b) ==>+5 c)", x2.dt(DTERNAL));
        assertEq("((a &&+5 b) ==>+- (b &&+5 c))", x2.dt(XTERNAL));

    }


    /**
     * test repeat that may appear in a Mapped subterms
     */
    @Test
    void ValidRepeatImplWithIndep() {
        {
            String x = "(($1 &&+5 b) ==>+1 ($1 &&+5 b))";
            assertEquals(x, $$(x).toString());
        }

        {
            String x = "(($1 &&+5 b),($1 &&+5 b))";
            assertEquals(x, $$(x).toString());
        }
    }









        /*
            (&,(&,P,Q),R) = (&,P,Q,R)
            (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)


            if (term1.op(Op.SET_INT) && term2.op(Op.SET_INT)) {


            if (term1.op(Op.SET_EXT) && term2.op(Op.SET_EXT)) {

         */



    @Test
    void ImplicationConjCommonSubtermsReduce() {
        assertEq("((&&,a,b,c)==>d)", "((&&, a, b, c) ==> (&&, a, d))");
        assertEq("((a&&d)==>(b&&c))", "((&&, a, d) ==> (&&, a, b, c))");
        Testing.assertInvalids("((&&, a, b, c) ==> (&&, a, b))");
        assertEq("((a&&b)==>c)", "((&&, a, b) ==> (&&, a, b, c))");
        assertEq(Bool.True, "((&&, a, b, c) ==> a)");

        assertEq("(a==>(b&&c))", "(a ==> (&&, a, b, c))");
    }

    @Test
    void ImplCommonSubterms() {

        assertEq("(((--,isIn($1,xyz))&&(--,(($1,xyz)-->$2)))==>(y-->x))",
                "(((--,isIn($1,xyz))&&(--,(($1,xyz)-->$2)))==>((--,(($1,xyz)-->$2))&&(y-->x)))");
    }

    @Test
    void ImplCommonSubterms2() {
        assertEq(Bool.True, "((tetris(isRowClear,7,true)&&tetris(7,14))==>tetris(7,14))");


        assertEq(Bool.True, "((tetris(isRowClear,7,true)&&tetris(7,14))=|>tetris(7,14))");

        assertEq("((tetris(isRowClear,7,true)&&tetris(7,14)) ==>+10 tetris(7,14))",
                "((tetris(isRowClear,7,true)&&tetris(7,14)) ==>+10 tetris(7,14))");
    }

    @Disabled
    @Test void ImplCommonSubterms3() {

        assertEq(Bool.True, "((x(intValue,(),0)&&x(setAt,0))==>x(intValue,(),0))");
        assertEq("x(setAt,0)", "((x(intValue,(),0)==>x(intValue,(),0)) && x(setAt,0))");
        assertEq("((x(setAt,0)==>x(intValue,(),0))&&x(intValue,(),0))",
                "((x(setAt,0)==>x(intValue,(),0)) && x(intValue,(),0))");

    }


    /**
     * https://www.wolframalpha.com/input/?i=%28%28x+and+y%29+implies+x%29
     */
    @Test
    void ImplicationTrue2() {
        assertEq(Bool.True, "((&&,a,b) ==> a)");
    }

    @Test
    void ImplicationTrue3() {
        assertEq(Bool.True, "((&&,x1,$1) ==> $1)");
    }

    @Test
    void ImplicationNegatedPredicate() {
        assertEq("(--,(P==>Q))", "(P==>(--,Q))");
        assertEq("((--,P)==>Q)", "((--,P)==>Q)");
    }

    @Test
    void ImplicationInequality() throws Narsese.NarseseException {

        assertNotEquals(
                $("(r ==> s)"),
                $("(s ==> r)"));
    }
    @Test void AllowEternalImplRecursive() {
        assertEq("(x ==>+1 (x-->y))", "(x ==>+1 (x-->y))"); //OK, temporal

        assertEq("(x==>(x-->y))", "(x ==> (x-->y))"); //OK, eternal/parallel
    }

    @Test
    void implChainReduction1() {
        assertEq("((x&&y)==>z)", "(x ==> (y ==> z))");

        assertEq("((P&&R) ==>+- Q)", "(R==>(P ==>+- Q))");
        //assertEq("(R ==>+- (P==>Q))", "(R ==>+- (P==>Q))"); //unchanged
    }
    @Test void implChainReduction2() {
        assertEq("((x &&+- y)==>z)", "(x ==>+- (y ==> z))");
        assertEq("((x &&+- y) ==>+1 z)", "(x ==>+- (y ==>+1 z))");
    }
    @Test void implConcept1() {
        String x = "((--,((tetris,c)-->((--,$1)&&happy))) ==>+20140 ((tetris,c)-->((--,$1)&&happy)))";
        String y = "((--,((tetris,c)-->((--,$1)&&happy))) ==>+- ((tetris,c)-->((--,$1)&&happy)))";
        assertEq(y, $$(x).concept());
    }
    @Test void implDTERNALValidSeqEqual() {
        assertEq("((x &&+1 y)==>(x &&+1 y))", "(  (x &&+1 y)==>(x &&+1 y))");
    }
    @Test void implDTERNALValidSeqEqualN() {
        assertEq("((--,(x &&+1 y))==>(x &&+1 y))", "(--(x &&+1 y)==>(x &&+1 y))");
        assertEq("((x &&+- y)==>((--,y) &&+- z))", "((x &&+- y)==>(--y &&+- z))");
    }
    @Test void implDTERNALValidSeqEqual2() {
        assertEq("((x &&+- y)==>(x &&+- y))", "(  (x &&+- y)==>(x &&+- y))");
    }
    @Test void implDTERNALInValidSeq1() {
        assertEq(False, "((x &&+1 y)==>(--y &&+1 z))");
    }


    @Test void impl_looseVariableInSubjectUnneg() {
        if (NAL.term.NORMALIZE_IMPL_SUBJ_NEG_VAR) {
            assertLooseVar("(?1 ==> x)", "((--,?1) ==> x)", Op.QUESTION);
            assertLooseVar("(#1 ==> x)", "((--,#1) ==> x)", Op.BELIEF);

            assertLooseVar("(?1 ==> #2)", "((--,?1) ==> #2)", Op.QUESTION);
            assertLooseVar("(--?1 ==>+1 ?1)", "((--,?1) ==>+1 ?1)", Op.QUESTION);
        }
    }

    @Test void conj_looseVariableInSubjectUnneg() {
        assertLooseVar("(?1 && x)", "((--,?1) && x)", Op.QUESTION);
    }

    private static void assertLooseVar(String y, String x, byte punc) {
        assertEquals($$(y), NALTask.taskTerm($$(x), punc));
    }

    @Test void deltaImplInPredicate() {
        //(("-ØôiéjÚÓÞì"-->premised) ==>-94545 Δ(("-ØôiéjÚÓÞì"-->premised) ==>-180 (nario-->freq)))
        assertEq(Null, "(a ==> /\\(b==>c))");
        assertEq(Null, "(  /\\(b==>c)==>a)");
        assertEq(Null, "(--/\\(b==>c)==>a)");
    }
}