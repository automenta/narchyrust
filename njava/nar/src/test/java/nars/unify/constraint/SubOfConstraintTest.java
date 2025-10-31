package nars.unify.constraint;

import nars.$;
import nars.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.subterm.util.SubtermCondition.Cond;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubOfConstraintTest {

    static final SubConstraint c = new SubConstraint(Cond, $.varPattern(1), $.varPattern(2), +1);

    @Test
    void CondOf_1() {

        assertCondOf($$("(  x&&y)"), $$("x"));
        assertCondOf($$("(--x&&y)"), $$("--x"));
        assertNotCondOf($$("(x&&y)"), $$("--x"));
        assertNotCondOf($$("(x&&y)"), $$("z"));

        assertCondOf($$("(x &&+1 (y&|z))"), $$("(y&|z)"));
        assertCondOf($$("(x &&+1 (y&|z))"), $$("x"));
        assertCondOf($$("(x &&+1 (y&|z))"), $$("y"));

    }

    private static void assertNotCondOf(Term c, Term x) {
        assertFalse(condOf(c, x));
    }

    private static void assertCondOf(Term c, Term x) {
        assertTrue(condOf(c, x));
    }

    private static boolean condOf(Term c, Term x) {
        return SubOfConstraintTest.c.valid(c, x, null);
    }

//    @Test
//    void testLastEventOf_Commutive() {
//
//        SubOfConstraint c = new SubOfConstraint($.varPattern(1), $.varPattern(2), EventLast, +1);
//        assertTrue(!c.invalid(
//                (Term) $$("(x&&y)"),
//                (Term) $$("x")));
//        assertTrue(!c.invalid(
//                (Term) $$("(x&|y)"),
//                (Term) $$("x")));
//        assertTrue(c.invalid(
//                (Term) $$("(x&|y)"),
//                (Term) $$("z")));
//
//        assertTrue(c.invalid(
//                (Term) $$("((--,((_1-->_2)&|(_3-->_2)))&|_4(_2))"),
//                (Term) $$("((_1-->_2)&&(_3-->_2))")));
//
////        assertTrue(!c.invalid(
////                (Term)$$("(((_1-->_2)&|(_3-->_2))&|_4(_2))"),
////                (Term)$$("((_1-->_2)&|(_3-->_2))")));
////        assertTrue(!c.invalid(
////                (Term)$$("(((_1-->_2)&|(_3-->_2))&|_4(_2))"),
////                (Term)$$("((_1-->_2)&&(_3-->_2))")));
//    }

//    @Test
//    void testLastEventOf_Seq() {
//
//        SubOfConstraint c = new SubOfConstraint($.varPattern(1), $.varPattern(2), EventLast, +1);
//        assertTrue(c.invalid(
//                (Term) $$("(x &&+1 y)"),
//                (Term) $$("x")));
//        assertTrue(!c.invalid(
//                (Term) $$("(x &&+1 y)"),
//                (Term) $$("y")));
//
//
//        {
//            Term a = $$("(x &&+1 (y&&z))");
//            Term b = $$("y");
//            assertEq("(x &&+1 z)", Conj.withoutEarlyOrLate(a, b, false));
//            assertNull( Conj.withoutEarlyOrLate(b, a, false));
//            assertTrue(!c.invalid(
//                    a,
//                    b));
//        }
//
//        assertTrue(!c.invalid(
//                (Term) $$("(x &&+1 (y&|z))"),
//                (Term) $$("y")));
//        assertTrue(!c.invalid(
//                (Term) $$("(x &&+1 (y&|z))"),
//                (Term) $$("(y&|z)")));
//
////        assertTrue(!c.invalid(
////                (Term) $$("(x &&+1 (y&&z))"),
////                (Term) $$("(y&&z)")));
////
//
//    }
//
//    @Test
//    void testLastEventOfNeg_Commutive() {
//
//        SubOfConstraint c = new SubOfConstraint($.varPattern(1), $.varPattern(2), EventLast, -1);
//        assertTrue(!c.invalid(
//                (Term) $$("(--x&&y)"),
//                (Term) $$("x")));
//
//    }
}