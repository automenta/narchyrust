package nars.term;

import nars.Term;
import nars.term.util.conj.CondDiff;
import nars.term.util.conj.ConjList;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static nars.$.$$;
import static nars.term.atom.Bool.True;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.*;

class CondDiffTest {

    static void assertConjDiffAll(String inc, String exc, String expect, boolean excludeNeg) {
        Term exclude = $$(exc);
        Term x = CondDiff.diffAll($$(inc), exclude);
        if (excludeNeg)
            x = CondDiff.diffAll(x, exclude.neg());
        assertEq(expect, x);
    }

    @Test
    void testConjDiff() {

        assertConjDiffAll("(--x && --y)", "x", "(--,y)", true);
        assertConjDiffAll("(--x &&+1 --y)", "x", "(--,y)", true);
        assertConjDiffAll("((--x &&+1 z) &&+1 --y)", "x", "(z &&+1 (--,y))", true);
        assertConjDiffAll("((x &&+1 z) &&+1 --y)", "x", "(z &&+1 (--,y))", true);
        assertConjDiffAll("((--x &&+1 z) &&+1 --y)", "--x", "(z &&+1 (--,y))", true);
        assertConjDiffAll("((x &&+1 z) &&+1 --y)", "--x", "(z &&+1 (--,y))", true);
        assertConjDiffAll("(--x && --y)", "x", "((--,x)&&(--,y))", false);
        assertConjDiffAll("(--x &&+1 --y)", "x", "((--,x) &&+1 (--,y))", false);
        assertConjDiffAll("((--x &&+1 z) &&+1 --y)", "x", "(((--,x) &&+1 z) &&+1 (--,y))", false);

        assertConjDiffAll("(x && y)", "x", "y", false);

//        assertConjDiff("--x", "x", "(--,x)", false);
//        assertConjDiff("x", "--x", "x", false);
//        assertConjDiff("--x", "x", "true", true);
//        assertConjDiff("--x", "--x", "true", false);
//        assertConjDiff("--x", "--x", "true", true);
//        assertConjDiff("x", "--x", "true", true);
    }

    @Test
    void testConjDiff_factored_seq() {
        assertEq("b",
                CondDiff.diffFirst($$("(x&&(a &&+1 b))"),
                        $$("(a&&x)")));
    }
    @Test
    void testConjDiff_factored_seq_some() {
        assertEq("(x &&+1 x)", CondDiff.diffFirst($$("(x&&(a &&+1 b))"),
                $$("(&&,a,b)")));
    }

    @Test
    void testConjDiff_factored_seq_all() {
        assertEq(True, CondDiff.diffFirst($$("(x&&(a &&+1 b))"),
                $$("(&&,x,a,b)")));
    }

    @Disabled
    @Test
    void testConjDiffOfNegatedConj() {
        assertConjDiffAll("--(x && y)", "x", "(--,y)", false);
        assertConjDiffAll("--(x && --y)", "x", "y", false);
        assertConjDiffAll("--(--x &&+1 --y)", "x", "y", true);
    }

    @Test
    void conjWithout1() {
        ConjTest.assertEq("c",
                CondDiff.diffFirst($$("(&&,--a,--b,c)"), $$("(--a && --b)"), false));
    }

    @Test
    void testcondWithoutAllParallel1() {
        ConjTest.assertEq("(a&&b)", CondDiff.diffAll(
                $$("(&&,a,b,c)"),
                $$("(&&,c,d,e)")));
    }
    @Test
    void testcondWithoutAllParallel2() {

        ConjTest.assertEq("(a&&b)", CondDiff.diffAll(
                $$("(&|,a,b,c)"),
                $$("(&|,c,d,e)")));
    }
    @Test
    void testcondWithoutAllParallel5() {

//        assertEq("(a&&b)",
//                Conj.diffAll(
//                        $$("(&&,a,b,c)"),
//                        $$("(&|,c,d,e)")));

        ConjTest.assertEq("(a&&b)", CondDiff.diffAll(
                $$("(&&,a,b,--c)"),
                $$("(&&,--c,d,e)")));
    }

    @Test
    void testcondWithoutAllParallel4() {
        ConjTest.assertEq("a", CondDiff.diffAll($$("(&&,a,b,c)"), $$("(b&&c)")));
        ConjTest.assertEq("b", CondDiff.diffAll($$("(&&,a,b,c)"), $$("(a&&c)")));
        ConjTest.assertEq("(b&&c)", CondDiff.diffAll($$("(&&,a,b,c)"), $$("a")));
        ConjTest.assertEq("(a&&c)", CondDiff.diffAll($$("(&&,a,b,c)"), $$("b")));


    }

    @Test void ConjLazyRemoveIf() {
        ConjList c = ConjList.conds($$("((&|,c,f) &&+1 g)"));
        assertEquals(3, c.size());

        assertFalse(
                c.removeIf((when, what) -> when == 1 && "c".equals(what.toString()))
        );
        assertEquals(3, c.size());

        assertTrue(
                c.removeIf((when, what) -> when == 0 && "c".equals(what.toString()))
        );
        assertEquals(2, c.size());
        assertEquals(0, c.when(0));
        ConjTest.assertEq("f", c.get(0));
        assertEquals(1, c.when(1));
        ConjTest.assertEq("g", c.get(1));
        ConjTest.assertEq("(f &&+1 g)", c.term());
    }

    @Test
    void testcondWithoutAllParallel3() {
        ConjTest.assertEq("(f &&+1 g)", CondDiff.diffAll(
                $$("((c && f) &&+1 g)"),
                $$("(&&,c,d,e)")));
    }

    @Test
    void testcondWithoutAllSequence() {
        ConjTest.assertEq("(y &&+1 z)", CondDiff.diffAll($$("((x &&+1 y) &&+1 z)"),
                $$("x")));
        ConjTest.assertEq("(x &&+2 z)", CondDiff.diffAll($$("((x &&+1 y) &&+1 z)"),
                $$("y")));
    }

    @Disabled @Test
    void testcondWithoutAllSequence1b() {
        ConjTest.assertEq("z", CondDiff.diffAll($$("((x &&+1 y) &&+1 z)"),
                $$("(x &&+1 y)")));
        ConjTest.assertEq("z", CondDiff.diffAll($$("((x &&+3 y) &&+3 z)"),
                $$("(x &&+3 y)")));
        ConjTest.assertEq("z", CondDiff.diffAll($$("(z &&+2 (x &&+3 y))"),
                $$("(x &&+3 y)")));
        ConjTest.assertEq("(z &&+8 w)", CondDiff.diffAll($$("((z &&+2 (x &&+3 y)) &&+3 w)"),
                $$("(x &&+3 y)")));
        ConjTest.assertEq("(z &&+8 w)", CondDiff.diffFirst($$("((z &&+2 (x &&+3 y)) &&+3 w)"), $$("(x &&+3 y)"), false));

    }

    @Test void WithoutSome() {
        ConjTest.assertEq("(a&&c)", CondDiff.diffAll($$("(&&,a,b,c)"), $$("(b&&w)")));
    }

    @Disabled @Test
    void testcondWithoutAllSequence2() {
        Term a = $$("((x &&+1 y) &&+1 z)");
        Term b = $$("(x &&+2 z)");
        assertTrue(ConjList.conds(a).contains(ConjList.conds(b)));
        assertEquals("101", ConjList.conds(a).contains(ConjList.conds(b), 0, Term::equals).toString());
        ConjTest.assertEq("y", CondDiff.diffAll(a, b));

    }

    @Test void condWithoutAllParallel6() {

        ConjTest.assertEq("z", CondDiff.diffAll(
                $$("((x &&+1 y) &&+1 z)"),
                $$("(&&,x,y)")));
    }

    @Test void diffAnyRepeat1() {
        assertEquals("[(y &&+2 x), (x &&+1 y)]",
            diffAny(
                $$("((x &&+1 y) &&+2 x)"),
                $$("x"),
    false).toString());
    }

    @Test void diffAnyRepeat1_PN() {
        assertEquals("[(y &&+2 (--,x)), (x &&+1 y)]",
                diffAny(
                        $$("((x &&+1 y) &&+2 --x)"),
                        $$("x"),
                        true).toString());
    }

    private static Set<Term> diffAny(Term c, Term e, boolean pn) {
        Set<Term> s = new TreeSet();
        for (int i = 0; i < 32; i++)
            s.add(CondDiff.diffAny(c, e, pn));
        return s;
    }
}