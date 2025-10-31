package nars.term;

import nars.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.term.util.Testing.assertCond;
import static nars.term.util.Testing.assertNotCond;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CondOfTest {

    @Test
    void CondOfSimple() {
        //assertFalse(Conj.eventOf($$("x"), $$("x")));
        assertCond("(x &&+1 --x)", "x");
        assertCond("(x &&+1 --x)", "--x");
        assertCond("(x && y)", "x");
        assertCond("(x &&+1 y)", "x");
        assertCond("((x && y) &&+1 z)", "z");
    }

    @Test
    void CondOfSimple2() {
        assertCond("((x&&y) &&+1 z)", "(x&&y)");
    }

    @Test
    void CondOfSubSeq() {
        assertCond("(z &&+1 (x &&+1 y))", "(x &&+1 y)");
        assertCond("(z &&+1 (x &&+1 (y &&+1 z)))", "(x &&+1 y)");
        assertCond("(z &&+1 (x &&+1 y))", "(z &&+1 x)");
        assertCond("(z &&+1 (x &&+1 y))", "(z &&+2 y)");
        assertFalse($$c("(z &&+1 (x &&+2 y))").condOf($$("(x &&+1 y)")));
    }
    @Test
    void CondOfSubSeqFactored() {
        assertCond("(z && (x &&+1 y))", "(x &&+1 y)");
        assertCond("(z && (x &&+1 (y &&+1 z)))", "(x &&+1 y)");
        assertCond("(z && (x &&+1 y))", "(z && x)");
        assertCond("(z && (x &&+1 y))", "(z && y)");
        assertFalse($$c("(z && (x &&+2 y))").condOf($$("(x &&+1 y)")));
    }
    @Test
    void CondOfSubSeqFactoredPartial() {
        assertCond("(z && (x &&+1 y))",
                "(x&&z)");
    }
    @Test
    void CondOfSubSeqFactoredPartial2() {
        assertCond("(z && (x &&+1 y))",
                "((z && x) &&+1 y)");
    }

    @Test
    void CondOfSubSeqFactoredPartial3() {
        assertNotCond("(z && (x &&+1 y))",
                "((z && w) &&+1 y)");
    }

    @Test
    void CondOfXternal1() {
        assertCond("(x &&+- y)", "x");
        assertCond("(x &&+- y)", "y");
    }

    @Test
    void CondOfXternal3a() {
        assertCond("((x&&z) &&+- y)", "x");
        assertCond("((x &&+1 z) &&+- y)", "x");
    }
    @Test
    void CondOfXternal3b() {
        assertCond("((x&&z) &&+- y)", "(x&&z)");
        assertNotCond("((x&&z) &&+- y)", "(x &&+1 z)");
        assertNotCond("((x &&+1 z) &&+- y)", "(x&&z)");
    }

    @Test
    void CondOfXternal4() {
        assertCond("((x &&+1 z) &&+- y)", "x");
        assertCond("((a:(b&c) &&+1 z) &&+- y)", "a:b");
    }

    @Test
    void CondOfXternal5() {
        assertCond("((x &&+- z) && y)", "(x &&+- z)");
        assertCond("((x &&+- z) && y)", "y");
        assertCond("((x &&+- z) &&+- y)", "(x &&+- z)");
        assertCond("((x &&+- z) &&+1 y)", "(x &&+- z)");

        assertCond("((x &&+- z) && y)", "(x && y)");
    }

    @Test
    void CondOfXternal2() {
        assertCond("((x &&+1 z) &&+- y)", "(x &&+1 z)");
        assertNotCond("((z &&+1 x) &&+- y)", "(x &&+1 z)");
        assertCond("((z &&+1 x) &&+- y)", "(x &&+- y)");
    }

    @Test
    void CondOfXternalSuper1() {
        assertCond("((x && z) &&+- (a && b))", "a");
    }
    @Test
    void CondOfXternalSuper2() {
        assertCond("((x && z) &&+- (a && b))", "(x && z)");
    }
    @Test
    void CondOfXternalSuper3() {
        assertCond("((x && z) &&+- (a && b))", "(z &&+- a)");
    }
    @Test
    void CondOfXternalSuper4() {
        assertCond("((x && z) &&+- (a && b))", "((x && z) &&+- a)");
    }

    @Test
    void CondOfXternalSuper5() {
        assertCond("((x && z) &&+- (a && b))", "(x &&+- z)");
    }

    @Test
    void CondOfXternalSuperNot() {
        assertNotCond("((x && z) &&+- (a && b))", "(z && a)");
    }


    @Test
    void ConjEventOfXternalDisj1() {
        assertTrue(
            $$c("(--x &&+- --x)").condOf($$("x"), 0)
        );
    }

    @Test
    void ConjEventOfXternalDisj2() {
        assertTrue(
            $$c("(--x &&+- --x)").condOf($$("x"), 0)
            //$$c("(--(--x &&+- --x))").condOf($$("x"), 0)
        );
//        assertTrue(
//            $$c("(--(--x &&+- --x) &&+- --(--x &&+- --x))").condOf($$("x"), 0)
//        );
    }

    @Test
    void SequentialDisjunctionAbsorb2() {
        Compound x = $$c("(--R &&+600 jump)");
        assertTrue(x.condOf($$("--R")));
        assertFalse(x.condOf($$("R")));
    }

    @Test void eventOf_Misc() {
        _assertCond($$("(x && y)"), $$("x"));
        _assertCond($$("(x && y)"), $$("y"));
        //_assertNotEventOf($$("(x&&y)"), $$("(x&&y)")); //equal

        _assertNotCond($$("(x &&+- y)"), $$("(x&&y)")); //component-wise, this is contained

        _assertCond($$("(x &&+- y)"), $$("x"));
        _assertCond($$("(x &&+- y)"), $$("y"));

        _assertCond($$("(x &&+1 y)"), $$("x"));
        _assertCond($$("(x &&+1 y)"), $$("y"));

        _assertCond($$("(&&,x,y,z)"), $$("x"));
    }

    @Test void eventOf_Misc2() {
        assertCond("(&&,x,y,z)","(x&&y)");
    }

    @Test void eventOf_Misc3() {
        _assertCond($$("((&&,x,y) &&+1 w)"), $$("w"));
        _assertCond($$("((&&,x,y) &&+1 w)"), $$("(x && y)"));
    }
    @Test void eventOf_SubSeq() {
        var xy = $$("((x &&+4120 (y&&z)) &&+1232 --y)");
        _assertCond(xy, $$("(z &&+1232 --y)"));
        _assertNotCond(xy, $$("(w &&+1232 --y)")); //wrong start term
        _assertNotCond(xy, $$("(z &&+1232 w)")); //wrong end term
        _assertNotCond(xy, $$("(z &&+1231 --y)")); //different sequencing
    }

    @Deprecated private static void _assertCond(Term xy, Term x) {
        assertCond(xy.toString(), x.toString());
    }

    @Deprecated private static void _assertNotCond(Term xy, Term x) {
        assertNotCond(xy.toString(), x.toString());
    }
}