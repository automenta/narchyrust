package nars.term;

import nars.Term;
import nars.term.util.conj.ConjList;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.term.ConjTest.assertEq;
import static nars.term.util.conj.CondDiff.diffFirst;
import static org.junit.jupiter.api.Assertions.*;

@Disabled class ConjBundleTest {

    private static final String c1  = "(x &&     (a-->(b & --c)))";
    private static final String c1n = "(x &&   --(a-->(b & --c)))";
    private static final String c2  = "(x &&+1   (a-->(b & --c)))";
    private static final String c2n = "(x &&+1 --(a-->(b & --c)))";

    @Test
    void CondOfComplex_TopLevel_1() {
        assertCondOf(c1, "x", +1);
    }

    @Test
    void CondOfComplex_TopLevel_2() {
        assertEquals("((a-->((--,c)&&b))&&x)", $$c(c1).toString());

        String r = "(&&,(--,(a-->c)),(a-->b))";
        String s = "(a-->((--,c)&&b))";
        assertEq(s, r);
        assertEquals(s, $$c(r).toString());
        assertEq("((a-->(b & --c))&&x)", "(&&,(--,(a-->c)),(a-->b),x)");
        assertCondOf(c1, s, +1);
    }

    @Test
    void CondOfComplex_InhInner() {
        assertCondOf(c1, "(a-->b)", +1);
        assertCondOf(c1, "--(a-->c)", +1);
        assertCondOf(c1,   "(a-->c)", -1);
    }

    @Test
    void CondOfComplex_InhInnerNeg() {
        assertCondOf("(a-->((--,c)&&b))", "--(a-->c)", +1);
        assertNotCondOf("(a-->((--,c)&&b))", "(a-->c)", +1);
    }
    @Test
    void CondOfComplex_InhInnerNeg2() {
        assertCondOf(c1, "--(a-->c)", +1);
    }

    @Disabled @Test
    void CondOfNegPos() {
        assertCondOf("--(a-->(b&&c))", "--(a-->b)", +1);
        assertCondOf("--(a-->(b&&c))", "(a-->b)", -1);
        assertNotCondOf("--(a-->(b&&c))", "(a-->b)", +1);
    }

    @Disabled
    @Test
    void CondOfNegNeg() {
        assertCondOf("--(a-->(--b&&c))", "(a-->b)", +1);
        assertNotCondOf("--(a-->(--b&&c))", "--(a-->b)", +1);
    }

    @Test
    void SubCondOf() {
        assertCondOf("(a-->(&&,  b,c,d))",        "(a-->(  b &   c))", +1);
        assertCondOf("(a-->(&&,--b,c,d))",        "(a-->(--b &   c))", +1);
        assertCondOf("(a-->(&&,--b,--c,d))",      "(a-->(--b & --c))", +1);
        assertNotCondOf("(a-->(&&,--b,--c,d))", "--(a-->(  b &   c))", +1);
    }

    @Test
    void SeqCondOf() {
        assertCondOf("(x &&+1 (a-->(&&,b,c,d)))",        "x", +1);
        assertCondOf("(x &&+1 (a-->(&&,b,c,d)))",        "(a-->(b&c))", +1);
        assertNotCondOf("(x &&+1 (a-->(&&,b,c,d)))",        "--(a-->d)", +1);
    }

    @Test void falseCond() {
        final String s = "(((x&y)-->a) & ((--x&y)-->a))";
        Term S = $$(s);
        assertEq("false", S);
    }

    @Test
    void MultiCondOf1() {
        assertCondOf("((x & y) --> (a & b))", "(x-->a)", +1);
    }
    @Test
    void MultiCondOf2() {
        assertCondOf("((x & y) --> (a & b))", "(x-->(a & b))", +1);
    }
    @Test
    void MultiCondOf3() {
        assertCondOf("((x & y) --> (--a & b))",    "--(x-->a)", +1);
    }

    @Test
    void CondOfComplex_InhInnerNot() {
        assertNotCondOf(c1, "(a-->c)", +1);
        assertNotCondOf(c1, "(a-->d)", +1);
    }

    @Test void CondWithout_Complex_simple() {
        assertCondWithout(c1, "x", "(a-->((--,c)&&b))");
    }
    @Test void CondWithout_Complex_inh_pos() {
        assertCondWithout("(a-->(b&c))", "(a-->b)", "(a-->c)");
    }

    @Test void CondWithout_Complex_inh_pos_separate() {
        assertCondWithout("((a&&x)-->(b&c))", "(a-->b)", "(((a&&x)-->c)&&(x-->b))");
    }
    @Test void CondWithout_Complex_inh_pos_can_separateSubj() {
        assertCondWithout("((a&&x)-->(b&c))", "((a&&x)-->b)", "((a&&x)-->c)");
    }
    @Test void CondWithout_Complex_inh_pos_can_separatePred() {
        assertCondWithout("((a&&x)-->(b&c))", "(a-->(b&&c))", "(x-->(b&&c))");
    }

    @Test void CondWithout_Complex_inh_pos2() {
        assertCondWithout(c1, "(a-->b)", "((--,(a-->c))&&x)");
    }
    @Test void CondWithout_Complex_inh_pos3() {
        assertCondWithout("(x&&(a-->(&&,b,c,d)))", "(a-->(b&c))", "((a-->d)&&x)");
    }
    @Test void CondWithout_Complex_inh_neg() {
        assertCondWithout(c1, "--(a-->c)", "((a-->b)&&x)");
    }

    @Test void bundleCorrectOrder_by_count() {
        assertEq("(((&&,w,x,y)-->a)&&(w-->b))", "(&&,(x-->a),(y-->a),(w-->a),(w-->b))");
        assertEq("((w-->(&&,a,b,c))&&(x-->c))", "(&&,(x-->c),(w-->c),(w-->a),(w-->b))");
    }

    private static void assertCondWithout(String c, String e, String y) {
        assertEq(y, diffFirst($$c(c), $$(e), false));
    }

    private static void assertCondOf(String conj, String event, int polarity) {
        assertTrue(condOf(conj, event, polarity));
    }
    private static void assertNotCondOf(String conj, String event, int polarity) {
        assertFalse(condOf(conj, event, polarity));
    }

    private static boolean condOf(String conj, String event, int polarity) {
        Compound c = $$c(conj);
        Term e = $$(event);
        return c.condOf(e, polarity);
    }

    @Test void explode1() {
        ConjList c = new ConjList();
        c.add(0L, $$("((((--,add((#1,#2),(--,#3),#4))&&#3)-->(low&&#4))&&(cmp(#1,#2)=-1))"));
        c.inhExplode();
        assertEquals(5, c.size());
    }
    @Test void explode2() {
        ConjList c = new ConjList();
        c.add(0L, $$("((a&&b)-->(x&&y))"));
        c.inhExplode();
        assertEquals("0:(a-->x),0:(a-->y),0:(b-->x),0:(b-->y)", c.toString());
        assertEquals(4, c.size());
    }
}