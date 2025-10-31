package nars.term.buffer;

import nars.Term;
import nars.term.atom.Atomic;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.util.transform.TermTransform;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TermBufferTest {

    private static final Term A = $$("a");
    private static final Term B = $$("b");
    private static final Term C = $$("c");

    private static class MyTermBuffer extends TermBuffer {
        @Override
        public Term term(int volMax) {
            var t = super.term(volMax);
            var volExpected = volMax - volRemain;
            var volActual = t.complexity();
            assertTrue((t == Null || volActual == volExpected), "incorrect volume calculation: actually " + volActual + " but measured " + volExpected);
            return t;
        }
    }

    @Test
    void testSimple() {
        assertEquals("(a,b)", new MyTermBuffer()
                .appendCompound(PROD, A, B).term().toString());
    }
    @Test
    void testNeg() {
        var l0 = new MyTermBuffer().appendCompound(PROD, A, B, B);
        var l1 = new MyTermBuffer().appendCompound(PROD, A, B, B.neg());

        var code = l1.code;
        var code1 = l0.code;
        assertEquals(code1.length()+1, code.length()); //only one additional byte for negation
        assertEquals(l0.sub.termCount(), l1.sub.termCount());
        assertEquals(l0.sub.termToId, l1.sub.termToId);

        assertEquals("(a,b,(--,b))", l1.term().toString());
        assertEquals("((--,a),(--,b))", new MyTermBuffer()
                .appendCompound(PROD, A.neg(), B.neg()).term().toString());
    }
    @Test
    void testTemporal() {
        assertEquals("(a==>b)", new MyTermBuffer()
                .appendCompound(IMPL, A, B).term().toString());

        assertEquals("(a ==>+1 b)", new MyTermBuffer()
                .appendCompound(IMPL, 1, A, B).term().toString());
    }

    private static final TermTransform nullTransform = new RecursiveTermTransform() {

    };
    private static final TermTransform atomToCompoundTransform = new RecursiveTermTransform() {

        final Term cmp = $$("(x,y)");

        @Override
        public Term applyAtomic(Atomic atomic) {
            return "_1".equals(atomic.toString()) ? cmp : atomic;
        }
    };

    @Test
    void testTransform1() {
        assertLazyTransforms("((_1) ==>+- (_1))");
    }

    @Test void Transform2() {
        var x = "((_1) ==>+- _1)";
        assertEquals("(((x,y)) ==>+- (x,y))",
                atomToCompoundTransform.apply($$(x)).toString());

    }
    @Test
    void testNestedCompound() {
        var expected = $$("(a,(b,c))");

        // Create a separate TermBuffer for the inner compound
        var BC = new MyTermBuffer();
        BC.appendCompound(PROD, B, C);
        var bc = BC.term();
        assertEq("(b,c)", bc);

        // Create another TermBuffer for the outer compound, including the inner term
        var ABC = new MyTermBuffer();
        ABC.appendCompound(PROD, A, bc);

        assertEq("(a,(b,c))", ABC.term());
    }

    @Test
    void testCompoundInCompound() {
        var buffer = new MyTermBuffer().compoundStart(PROD.id)
                    .subsStart((byte) 2).append(A);
        assertEquals("(a,{b,c})", buffer.compoundStart(SETe.id).appendSubterms(B, C).compoundEnd()
                    .subsEnd()
                .compoundEnd().term().toString());
    }

    @Test void EmptyProd() {
        assertLazyTransforms("x(intValue,(),3)");
    }
    @Test void AtomFunc() {
        assertLazyTransforms("x(a)");
    }

    private static void assertLazyTransforms(String x) {
        assertEquals(x, nullTransform.apply($$(x)).toString());
    }


}