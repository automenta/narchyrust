package nars.term;

import nars.NAL;
import nars.Narsese;
import nars.Op;
import nars.Term;
import nars.term.util.Terms;
import nars.term.util.transform.Retemporalize;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.term.atom.Bool.False;
import static nars.term.atom.Bool.True;
import static nars.term.util.Testing.assertEq;
import static nars.term.util.Testing.assertInvalids;
import static org.junit.jupiter.api.Assertions.*;

public class TemporalTermTest {

    @Test
    void SortingTemporalImpl() {
        assertEquals(-1, $$("(x ==>+1 y)").compareTo($$("(x ==>+10 y)")));
        assertEquals(+1, $$("(x ==>+1 y)").compareTo($$("(x ==>-1 y)")));
        assertEquals(-1, $$("(x ==>-1 y)").compareTo($$("(x ==>+1 y)")));
    }

    @Test
    void validInh() {
        assertEq("(x-->(x))", "(x-->(x))"); //valid
        assertEq("x(x)", "((x)-->x)"); //valid
        assertEq("(x-->(x,x))", "(x-->(x,x))"); //valid
    }

    @Test
    void InvalidInh1() {
        assertInvalids("(x-->{x,y})");
    }
    @Test
    void InvalidInh2() {
        assertInvalids("(x<->{x,y})");
        assertInvalids("(x-->(x<->y))");
        assertInvalids("(x<->(x<->y))");

    }



    @Test
    void InvalidStatementsWithConj() {
//        assertTrue(Conj.eventOfRecursivePN($$("((x,y)&&(x,z))"), $$("(x,y)")));
//        assertFalse(Conj.eventOfRecursivePN($$("((x,y)&&(x,z))"), $$("x")));

        //assertInvalidTerms("(  (x && y) " + op + " (a &&+1 --(x &&+1 y)))");
        for (String op : new String[]{"-->", "<->", "==>"}) {
            assertInvalids("((x && y) " + op + " (x || y))");
            assertInvalids("((x && y) " + op + " x)");
            assertInvalids("((x && y) " + op + " (--x && --y))");
            assertInvalids("((x && y) " + op + " (--x && z))");
            assertInvalids("((x && y) " + op + " x)");
            assertInvalids("((x && y) " + op + " (x && --y))");
            assertInvalids("(((x,y)&&(x,z)) " + op + " (x,y))");

            if (!op.equals("==>")) {
                assertInvalids("(  x " + op + " (a &&+1 (x &&+1 y)))");
                assertInvalids("(--x " + op + " (a &&+1 (x &&+1 y)))");
                assertInvalids("(  x " + op + " (a &&+1 --(x &&+1 y)))");
                assertInvalids("(--x " + op + " (a &&+1 --(x &&+1 y)))");
                assertInvalids("(x " + op + " (x &&+1 y))");
                assertInvalids("(--x " + op + " (x &&+1 y))");
                assertInvalids("((x && y) " + op + " (x &&+- y))");
                assertInvalids("((x || y) " + op + " (--x &&+- --y))");
                assertInvalids("((x &&+1 y) " + op + " x)");
                assertInvalids("((x &&+1 y) " + op + " (x&&y))");
                //assertInvalidTerms("((x &&+1 y) " + op + " (x&&z))");
            }

        }
    }

    @Test
    void TemporalNormalization() {
        //order matters here to test interning corruption:
        assertEq("((--,#1) &&+- #1)", $$("(#2 &&+5 (--,#2))").root());
        assertEq("((--,#1) &&+- #1)", $$("(#2 &&+- (--,#2))").normalize());
        assertEq("(#1 &&+5 (--,#1))", $$("(#2 &&+5 (--,#2))").normalize());
    }

    @Test
    void TemporalNormalization2() {
        assertEq(False,
                $$("(((--,$1:(tetris,d)) &&+2656 (--,#3:(tetris,d))) ==>-2656 $1:(tetris,d))"));
        assertEq(True,
                $$("(($1:(tetris,d) &&+2656 (--,#3:(tetris,d))) ==>-2656 $1:(tetris,d))"));
    }

    @Test
    void InvalidStatementsWithConj2() {
//		assertInvalidTerms("((((0,7)||(--,speed)) &&+- (--,low))-->((((--,(0,7))&&speed)||low)&&(--,density)))");
        assertInvalids("(  (x && y) --> (a &&+1 --(x &&+1 y)))"); //TODO
    }

    @Disabled
    @Test
    void InvalidInh_ConjComponent() {
        assertInvalids("((x-->r)-->(r&&c))");
        assertInvalids("((x-->r)-->((--,r)&&c))");
    }

    @Disabled @Test
    void InvalidInheritanceOfEternalTemporalNegated() throws Narsese.NarseseException {
        assertEquals(False, $("(--(a &&+1 b) --> (a && b))"));
        assertEquals(False, $("(--(a &&+- b) --> (a && b))"));
        assertEquals(False, $("((a &&+1 b) --> --(a && b))"));
        assertEquals(False, $("((a &&+- b) --> --(a && b))"));

    }

    @Test
    void Atemporalization3a() throws Narsese.NarseseException {

        assertEquals(
                "(--,((x &&+- $1) ==>+- ((--,y) &&+- $1)))",
                Retemporalize.retemporalizeAllToXTERNAL.apply($("(--,(($1&&x) ==>+1 ((--,y) &&+2 $1)))")).toString());
    }

    @Test
    void Atemporalization3b() {

        if (NAL.term.IMPL_IN_CONJ) {
            Compound x = $$c("((--,(($1&&x) ==>+1 ((--,y) &&+2 $1))) &&+3 (--,y))");
            Term y = Retemporalize.retemporalizeAllToXTERNAL.apply(x);
            assertEquals("((--,((x &&+- $1) ==>+- ((--,y) &&+- $1))) &&+- (--,y))", y.toString());
        }
    }



    @Disabled
    @Test /* TODO decide the convention */ void Atemporalization5() throws Narsese.NarseseException {
        for (String s : new String[]{"(y &&+- (x ==>+- z))", "((x ==>+- y) &&+- z)"}) {
            Term c = $(s);
            assertInstanceOf(Compound.class, c);
            assertEquals("((x &&+- y) ==>+- z)",
                    c.toString());
            assertEquals("((x &&+- y) ==>+- z)",
                    c.root().toString());


        }
    }

    @Test
    void Atemporalization6() {
        Compound x0 = $$c("(($1&&x) ==>+1 ((--,y) &&+2 $1)))");
        assertEquals("((x&&$1) ==>+1 ((--,y) &&+2 $1))", x0.toString());

    }

    @Test
    void CommutiveTemporalityDepVar0() throws Narsese.NarseseException {
        Term t0 = $("((SELF,#1)-->at)").term();
        Term t1 = $("goto(#1)").term();
        Term[] a = Terms.commute(t0, t1);
        Term[] b = Terms.commute(t1, t0);
        assertEquals(
                Op.terms.subterms(a),
                Op.terms.subterms(b)
        );
    }

    @Test
    void parseTemporalRelation() throws Narsese.NarseseException {

        assertEquals("(x ==>+5 y)", $("(x ==>+5 y)").toString());
        assertEquals("(x &&+5 y)", $("(x &&+5 y)").toString());

        assertEquals("(x ==>-5 y)", $("(x ==>-5 y)").toString());

        assertEquals("((before-->x) ==>+5 (after-->x))", $("(x:before ==>+5 x:after)").toString());
    }

    @Test
    void temporalEqualityAndCompare() throws Narsese.NarseseException {
        assertNotEquals($("(x ==>+5 y)"), $("(x ==>+0 y)"));
        assertNotEquals($("(x ==>+5 y)").hashCode(), $("(x ==>+0 y)").hashCode());
        //assertNotEquals($("(x ==> y)").hashCode(), $("(x ==>+0 y)").hashCode());

        assertEquals($("(x ==>+0 y)"), $("(x ==>-0 y)"));
        assertNotEquals($("(x ==>+5 y)"), $("(y ==>-5 x)"));


        assertEquals(0, $("(x ==>+0 y)").compareTo($("(x ==>+0 y)")));
        assertEquals(-1, $("(x ==>+0 y)").compareTo($("(x ==>+1 y)")));
        assertEquals(+1, $("(x ==>+1 y)").compareTo($("(x ==>+0 y)")));
    }

    @Test
    void TransformedImplDoesntActuallyOverlap() {
        String X = "(((#1 &&+7 (a,c)) &&+143 (a,c)) ==>+- (a,c))";
        Compound x = $$c(X);
        assertEquals(X, x.toString());
        assertEquals("(((#1 &&+7 (a,c)) &&+143 (a,c)) ==>+7 (a,c))",
                x.dt(7).toString());
    }

//    @Test void InvalidIntEventTerms() {
//
//        assertEq(Null, "(/ && x)");
////        assertEq(Null, "(1 && x)");
////        assertEq(Null, "(1 &&+1 x)");
////        assertEq(Null, "(1 ==> x)");
////        assertEq(Null, "(x ==> 1)");
//
////        assertEq(Null, "(--,1)");
////        assertEq(Null, "((--,1) && x)");
//    }


    @Test
    void ImplRootRepeat() throws Narsese.NarseseException {
        Term h = $("(x ==>+1 x)");
        assertEquals("(x ==>+- x)", h.root().toString());
    }

    @Test
    void ImplRootNegate() throws Narsese.NarseseException {
        Term i = $("(--x ==>+1 x)");
        assertEquals("((--,x) ==>+- x)", i.root().toString());

    }

    @Disabled
    @Test
    void EqualsAnonymous3() throws Narsese.NarseseException {


        assertEquals(Retemporalize.retemporalizeAllToXTERNAL.apply($("(x && (y ==> z))")),
                Retemporalize.retemporalizeAllToXTERNAL.apply($("(x &&+1 (y ==>+1 z))")));


        assertEquals("((x &&+1 z) ==>+1 w)",
                $("(x &&+1 (z ==>+1 w))").toString());

        assertEquals(Retemporalize.retemporalizeAllToXTERNAL.apply($("((x &&+- z) ==>+- w)")),
                Retemporalize.retemporalizeAllToXTERNAL.apply($("(x &&+1 (z ==>+1 w))")));
    }


    @Test
    void ImplTransformMaintainsTiming_via_Realign() {
        assertEq(
                "((c-->a) ==>+3 (a-->d))",
                $$("(($1-->a) ==>-1 ((c-->$1) &&+4 (a-->d)))").replace(varIndep(1), $$("c"))
        );
        assertEq(
                "((a-->d) ==>+3 (c-->a))",
                $$("(((a-->d) &&+4 (c-->$1)) ==>-1 ($1-->a))").replace(varIndep(1), $$("c"))
        );
    }

    @Test
    void ConjTransformMaintainsTiming() {
        assertEq(
                "((x-->a) &&+3 (z-->a))",
                $$("((x-->a) &&+1 ((y-->b) &&+2 (z -->a)))").replace($$("b"), $$("y"))
        );
    }




    @Test void xternal1() {
        assertEq(
                "(clear(tetris,c) ==>+- ((tetris,c)-->(dex&&happy)))",
                //"(clear(tetris,c) ==>+- (dex(tetris,c) &&+- happy(tetris,c)))",
                Retemporalize.retemporalizeAllToXTERNAL.applyCompound(
                        $$c("(clear(tetris,c)==>((tetris,c)-->(dex&&happy)))")
                )
        );
    }

    @Test void xternal2() {
        //TODO check
        assertEq(
            "(((((--,(tetris,a))&&(--,(tetris,d)))-->meta)-->happy) &&+- (\"-MþóUØIönd\"-->((--,(lag,dur))&&(--,conceptualize))))",
            //"(((((--,(tetris,a))&&(--,(tetris,d)))-->meta)-->happy) &&+- ((--,(\"-MþóUØIönd\"-->(lag,dur))) &&+- (--,(\"-MþóUØIönd\"-->conceptualize))))",
            Retemporalize.retemporalizeAllToXTERNAL.applyCompound(
                $$c("(((((--,(tetris,a))&&(--,(tetris,d)))-->meta)-->happy)&&(\"-MþóUØIönd\"-->((--,(lag,dur))&&(--,conceptualize))))")
            )
        );
    }

    @Test
    void SameWithDiffDT() {
        String s = "((x &&+1 y)<~>(x &&+2 y))";
        assertEq(s, s);

        assertEq(True, "((x &&+1 y)<~>(x &&+1 y))");
    }

}