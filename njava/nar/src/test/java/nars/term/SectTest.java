package nars.term;

import nars.Term;
import nars.term.util.Terms;
import nars.term.util.Testing;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.Op.CONJ;
import static nars.term.atom.Bool.*;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** intersection / diff terms */
class SectTest {
    @Test
    void testSectCommutivity() {
        Testing.assertEquivalentTerm("(&,a,b)", "(&,b,a)");
        Testing.assertEquivalentTerm("(a & b)", "(b & a)"); //test for alternate syntax
        Testing.assertEquivalentTerm("(|,a,b)", "(|,b,a)");
    }

    @Test
    void testSectWrap() {
        Testing.assertEquivalentTerm("(|,(b&a),c)", "(|,(b&a),c)");
        Testing.assertEquivalentTerm("(&,(b|a),c)", "(&,(b|a),c)");
        Testing.assertEquivalentTerm("(|,a,b,c)", "(|,(b|a),c)");
    }
    @Disabled @Test
    void CoNegatedDifference() {


//        {
//            NAR n = NARS.shell();
//            n.believe("X", 1.0f, 0.9f);
//            n.believe("Y", 0.5f, 0.9f);
//            tryDiff(n, "(X~Y)", "%.50;.81%");
//            tryDiff(n, "((--,Y)~(--,X))", "%.50;.81%");
//            tryDiff(n, "(Y~X)", "%0.0;.81%");
//            tryDiff(n, "((--,X)~(--,Y))", "%0.0;.81%");
//        }
//        {
//            NAR n = NARS.shell();
//            n.believe("X", 1.0f, 0.9f);
//            n.believe("Y", 0.75f, 0.9f);
//            tryDiff(n, "(X~Y)", "%.25;.81%");
//
//            tryDiff(n, "((--,Y)~(--,X))", "%.25;.81%");
//
//            tryDiff(n, "(Y~X)", "%0.0;.81%");
//            tryDiff(n, "((--,X)~(--,Y))", "%0.0;.81%");
//        }

        assertEq("((--,Y)&&X)", "(X-Y)");
        assertEq("(X||(--,Y))", "(X~Y)");



//        assertEq("(Y~X)", "((--,X)~(--,Y))");
//        assertEq("(X~Y)", "((--,Y)~(--,X))");

//        assertEq("(A-->(Y-X))", "(A-->((--,X)~(--,Y)))");
//        assertEq("(A-->(Y-X))", "(A-->((--,X)-(--,Y)))");
//
//        assertEq("((Y-X)-->A)", "(((--,X)-(--,Y))-->A)");
//        assertEq("((Y~X)-->A)", "(((--,X)~(--,Y))-->A)");

    }

    @Disabled @Test void SectDiffEquivAndReductions() {
        Testing.assertEquivalentTerm("(&,b,--a)", "(b - a)");
        Testing.assertEquivalentTerm("(|,b,--a)", "(b ~ a)");

        Testing.assertEquivalentTerm("(&,b,a)", "(b - --a)");

        Testing.assertEquivalentTerm("--(b~a)", "--(b~a)"); // 1 - (b * (1-a)) hyperbolic paraboloid

        Testing.assertEquivalentTerm("(&,c, --(b & --a))", "(c - (b-a))");
        Testing.assertEquivalentTerm("(c ~ (b-a))", "(c ~ (b-a))"); //different types, unchanged

        Testing.assertEquivalentTerm("((b ~ c) ~ (b ~ a))", "((b ~ c) ~ (b ~ a))"); // (b * (1-c)) * (1-(b * (1-a)))
    }

    //    @Test
//    void testSectConceptualization() {
//
//        assertEq("((a==>b)&x)", "((a==>b) & x)");
//        assertEq("((a ==>+1 b)&x)", "((a==>+1 b) & x)");
//        assertEq("((a ==>+- b)&x)", $$("((a==>+1 b) & x)").concept());
//
//
////        TermTest.assertEq(Bool.Null, "((a==>+1 b) & (a ==>+2 b))");
////        TermTest.assertEq(Bool.Null, "(&, (a==>b),(a ==>+2 b),(c==>d))");
////        TermTest.assertEq("(((a ==>+2 b)-->d)&(a ==>+1 b))", "((a==>+1 b) & ((a ==>+2 b)-->d))");
////        TermTest.assertEq(Bool.Null, "(((a ==> b)-->d) & ((a ==>+2 b)-->d))");
////        TermTest.assertEq(Bool.Null, "(&, (a==>b),(a ==>+2 b),((c==>d)-->e))");
//
//
//    }


    @Disabled @Test void SectDiff() {
        Term t = CONJ.the($$("(--,(?2~(|,(--,(?2~?1)),?2,?3)))"), $$("?2"), $$("?3"));
        assertEquals(t, t);
    }

    @Test void recursiveSect() {
        assertEq(True, "(b-->(b&c))");
        assertEq(False, "(b-->(--b&c))");
        assertEq(True, "((a&b)-->a)");
    }

    @Test void recursiveImplParPP() { assertEq("(b==>c)", "(b==>(  b & c))"); }
    @Test void recursiveImplParPPdt() { assertEq("(b ==>+1 c)", "(b==>(  b &&+1 c))"); }
    @Test void recursiveImplParPN() {
        assertEq(False, "(b==>(--b & c))");
    }
    @Test void recursiveImplParSP() {
        assertEq(True, "((  b & c)==>b)");
    }
    @Test void recursiveImplParSN() {
        assertEq(False, "((--b & c)==>b)");
    }

    @Test void SimSect() {
        ConjTest.assertEq("((y&&z)<->x)", "((y&&z)<->x)");
        ConjTest.assertEq("((y&&z)<->x)", $$c("((y&&z)<->x)").concept());
    }

    @Test void SimSectInvalidEmbeddedDT_Recursive() {
        assertEq(Null, "((y &&+1 z)<->x)");
        assertEq(Null, "((y &&+- z)<->x)");
        assertEq(Null, "((a&&(y &&+- z))<->x)");
    }

    @Disabled @Test void factorCommonSectConj() {
        assertEq("(a-->c)", "((a&b)-->(b&c))");
    }

    @Disabled @Test void factorCommonSectDisj() {
        assertEq("(a-->c)", "((a|b)-->(b|c))");
    }

    @Test void factorCommonSectMix() {
        //TODO
        //??
        //assertEq("(a-->c)", "((a|b)-->(b&c))");
        //assertEq("(a-->c)", "((a&b)-->(b|c))");
    }

    @Disabled @Test void TooComplexSectDiff() {
//        assertEq("", "(a --> --(x-y))");

        /*
                           x-y  =            x*(1-y)
                        --(x-y) =         1-(x*(1-y))
                  (&,x,--(x-y)) =     x * (1-x*(1-y))
                (&,--y,--(x-y)) = (1-y) * (1-x*(1-y))
         */

        Term n3b = $$("(&, (--,(x-y)), (--,y), x)");
        Term n2 = $$("(&, (--,(&,(--,(x-y)),(--,y),x)), (--,(x-y)), (--,y), x)");
        Term n1 = $$("(y-(&,(--,(&,(--,(x-y)),(--,y),x)),(--,(x-y)),(--,y),x))");

        Term n = $$("(a-->(y-(&,(--,(&,(--,(x-y)),(--,y),x)),(--,(x-y)),(--,y),x)))");

    }

    @Disabled
    @Test void InvalidTemporal1() {
        String a = "(x &&+1 y)";
        String b = "(x &&+2 y)";
        //        assertEq(Null, '(' + a + '|' + b + ')');
//        assertEq(Null, '(' + a + '&' + b + ')');
        assertEq(Null, '{' + a + ',' + b + '}');
        assertEq(Null, '[' + a + ',' + b + ']');
//        assertEq(Null, '(' + a + "<->" + b + ')');

        //one is negated
//        assertEq(Null, '(' + a + "| --" + b + ')');
//        assertEq(Null, '(' + a + "& --" + b + ')');
        assertEq(Null, '{' + a + ",--" + b + '}');
        assertEq(Null, '[' + a + ",--" + b + ']');
//        assertEq(Null, '(' + a + "<-> --" + b + ')');

        //3-ary
//        assertEq(Null, "(|," + a + ',' + b + ',' + c + ')');
//        assertEq(Null, "(&," + a + ',' + b + ',' + c + ')');
        String c = "z";
        assertEq(Null, '{' + a + ',' + b + ',' + c + '}');
        assertEq(Null, '[' + a + ',' + b + ',' + c + ']');

    }

    @Test void recursiveInhInvalid2() {
        assertTrue(Terms.rCom($$("(--x && (y))"), $$("(y)")));

        assertEq(True, "((--x && (y))-->(y))");
        assertEq(False, "((--(y) && x)-->(y))");
        assertEq(True, "((y)-->(--x && (y)))");
        assertEq(False, "((y)-->(--(y) && x))");

        assertEq(True, "(((--,(cat,blue))&&(tom,sky))-->(tom,sky))");
    }
    @Test void recursiveInhInvalid3() {
        assertEq(Null, "(((--,(1-->(d,x)))&&(1-->dAng))-->((--,2)&&1))");
    }

    @Test void recursiveInhInvalid4() {
        Compound a = $$c("(&&,(--,(((#1,0),x)&&((5,3),y))),(--,((#2,3),z)),cmp(#1,#2,1))");
        Term b = $$("((5,3),y)");
        assertTrue(a.containsRecursively(b));
        assertTrue(a.subterms().containsRecursively(b));

        assertEq(Null, "((((4,0),x)&&((5,3),y))-->(&&,(--,(((#1,0),x)&&((5,3),y))),(--,((#2,3),z)),cmp(#1,#2,1)))");
    }

//    @Test
//    void testInvalidTemporal3() {
//        assertEq(Null, "((a==>+1 b)~(a ==>+2 b))");
//        //TermTest.assertEq("((--,(c ==>+2 d))&(a ==>+1 b))", "((X &&+837 Y)~(--,(Y &&+1424 X)))");
//    }
//    @Test void ValidTemporal1() {
//        assertEq("((x &&+1 y)|(x &&+2 z))", "((x &&+1 y)|(x &&+2 z))");
//    }
}