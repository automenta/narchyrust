package nars.term.util.transform;

import nars.Op;
import nars.Term;
import nars.unify.Unify;
import nars.unify.UnifyAny;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.util.Testing.assertEq;
import static nars.term.util.conj.Cond.unifiableCond;
import static org.junit.jupiter.api.Assertions.*;

class UnifiableTest {

    @Test void posConstant() {
        assertUnifiableEvent(
                "(x&&y)",
                "x",
                "x");
    }
    @Test void posConstantFail() {
        assertUnifiableEvent(
                "(x&&y)",
                "z",
                null);
    }
    @Test void posVar() {
        assertUnifiableEvent(
                "((a,b)&&y)",
                "(#1,b)",
                "(a,b)");
    }
    @Test void posVarReverse() {
        assertUnifiableEvent(
                "((#1,b)&&y)",
                "(a,b)",
                "(#1,b)");
    }
    @Test void posVarFail() {
        assertUnifiableEvent(
                "((a,b)&&y)",
                "(#1,a)",
                null);
    }
    @Test void negVar() {
        assertUnifiableEvent(
                "(open($1,#2)&&(--,(#2-->lock)))",
                "(--,(lock1-->lock))",
                "(--,(#2-->lock))");
    }
    @Disabled
    @Test void posVarBundled() {
        assertUnifiableEvent(
                "(((a,b)&&y)-->z)",
                "z(#1,b))",
                "z(a,b)");
    }
    @Disabled @Test void negVarBundled() {
        assertUnifiableEvent(
                "((--(a,b)&&y)-->z)",
                "--z(#1,b))",
                "(--,z(a,b))");
    }
    @Test void posVarBundled2() {
        assertUnifiableEvent(
                "(&&,z(a,b),(y-->z),(c-->z))",
                "(z(#1,b)&&(y-->z))",
                "(z(a,b)&&(y-->z))");
    }
    private static void assertUnifiableEvent(String x, String y, String z) {
        Term X = $$(x);
        Term Y = $$(y);
        Term Z = unifiableCond(
                X,
                Y, false, false, new UnifyAny());

        if (z == null)
            assertNull(Z);
        else {
            assertEq(z, Z);
            assertTrue(Unify.isPossible(Z, Y, Op.Variables, 1));
            assertFalse(Unify.isPossible(Z, X, Op.Variables, 1));
        }
    }
}