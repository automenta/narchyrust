package nars.term.anon;

import nars.Term;
import nars.term.util.Explode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$c;
import static nars.func.MathFuncTest.assertEval;
import static nars.term.util.Testing.assertEq;

class ExplodeTest {
    static final String x = "((a,(b,c)) --> (b,c))";

    @Test void test1() {
        assertExplode(x, 2, 2, "(((a,#1)-->#1)&&((b,c)=#1))");
    }

    @Disabled
    @Test void testN() {
        assertExplode(x, Integer.MAX_VALUE, 1, "(&&,((#2,#1)-->#1),((b,c)=#1),(a=#2))");
    }

    @Test void testSePP() {
        assertExplode("((x&&y) &&+1 (x&&y))", Integer.MAX_VALUE, 2,
                "(((x&&y)=#1)&&(#1 &&+1 #1))");
    }

    @Test void testSePN() {
        assertExplode("((x&&y) &&+1 --(x&&y))", Integer.MAX_VALUE, 2, "(((x&&y)=#1)&&(#1 &&+1 (--,#1)))");
    }

    private static void assertExplode(String x, int componentsMax, int copiesMin, String y) {
        Term xy = new Explode($$c(x), componentsMax, copiesMin, Integer.MAX_VALUE).outEqXY;
        assertEq(y, xy);
        assertEval(x, xy);
    }
}