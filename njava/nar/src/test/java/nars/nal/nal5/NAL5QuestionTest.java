package nars.nal.nal5;

import nars.NAR;
import nars.NARS;
import nars.term.Compound;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static nars.$.$$$;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NAL5QuestionTest {

    private static final int cycles = 500;

    private final NAR nar = NARS.tmp(5, 6);

    @ParameterizedTest
    @ValueSource(strings = {"", ",y", ",y,z", ",y,z,w"/*,",y,z,w,q"*/})
    void testDepVarInIndepImpl(String args) {
        try (TestNAR t = new TestNAR(nar)) {
            t.volMax(15)
                        .confMin(0.74f)
                        .input("f(#x" + args + ").")
                        .input("(f($x" + args + ") ==> g($x" + args + ")).")
                        .mustBelieve(cycles, "g(#1" + args + ")", 1f, 0.81f).run();
        }
    }

    @Test
    void testDepVarInIndepImpl3() {
        Compound c = (Compound) $$$("f(#2,#1)");
        assertFalse(c.NORMALIZED());
        assertEq("f(#1,#2)", c.normalize());

        try (TestNAR t = new TestNAR(nar)) {
            t
                        .confMin(0.75f)
                        .volMax(12)
                        .input("f(#x,#y).")
                        .input("(f($x,#y) ==> g($x,#y)).")
                        .mustBelieve(cycles, "g(#1,#2)", 1f, 0.81f).run();
        }
    }


}