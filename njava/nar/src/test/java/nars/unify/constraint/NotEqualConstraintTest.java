package nars.unify.constraint;

import nars.Term;
import nars.term.util.Terms;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NotEqualConstraintTest {


    @Test
    void testNeqRComConj() {
        assertEqRCom("left", "((--,left)&&(--,rotate))");
        assertEqRCom("--left", "((--,left)&&(--,rotate))");
        assertEqRCom("x", "(x&&y)");
        assertEqRCom("--x", "(--x&&y)");

//        assertEqRCom("(left&&rotate)", "((--,left)&&(--,rotate))");
//        assertEqRCom("(--left&&rotate)", "((--,left)&&(--,rotate))");
//        assertEqRCom("(--left && --rotate)", "((--,left)&&(--,rotate))");
    }

    /* TODO  neqICom different form neqRCom
    @Test
    void notNeqRComConj() {
        assertNotEqICom("(x && y)", "(&&, x, y, z)");
    } */

    static void assertNotEqRCom(String a, String b) {
        assertEqRCom(a, b, false);
    }
    static void assertEqRCom(String a, String b) {
        assertEqRCom(a, b, true);
    }
    static void assertEqRCom(String a, String b, boolean isTrue) {
        Term A = $$(a);
        Term B = $$(b);
        Supplier<String> msg = () -> a + " " + b + " " + (isTrue ? "!eqRCom" : "eqRCom");

        assertEquals(isTrue, (A.equals(B) || Terms.rCom(A, B)), msg);

        assertEquals(isTrue, (B.equals(A) || Terms.rCom(B, A)), msg);
    }

}