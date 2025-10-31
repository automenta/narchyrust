package nars.term.util.conj;

import jcog.WTF;
import nars.$;
import nars.Op;
import nars.Term;
import nars.term.Compound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConjBuilderTest {

    private static final Term x = $.atomic("x");
    private static final Term y = $.atomic("y");

    @Test
    void testSimpleEternalsNeg() {
        ConjBuilder c = new ConjTree();
        c.add(Op.ETERNAL, x);
        c.add(Op.ETERNAL, y.neg());
        assertEquals("((--,y)&&x)", c.term().toString());
    }

    @Test
    void testSimpleEvents() {
        for (ConjBuilder c : new ConjBuilder[]{new ConjTree(), new ConjList()}) {
            c.add(1, x);
            c.add(2, y);
            assertEquals("(x &&+1 y)", c.term().toString());
            assertEquals(1, c.shift());
            assertEquals(1, c.shiftOrZero());

//            if (c instanceof ConjBuilder)
//                assertEquals(2, ((Conj) c).event.size());
        }
    }

    @Test
    void testEventShiftEternal() {
        for (ConjBuilder c : new ConjBuilder[]{new ConjTree(), new ConjList()}) {
            c.add(Op.ETERNAL, x);
            c.add(1, y);
            assertEquals(1, c.shift());
        }
        for (ConjBuilder c : new ConjBuilder[]{new ConjTree(), new ConjList()}) {
            c.add(Op.ETERNAL, x);
            c.add(Op.ETERNAL, y);
            assertEquals(Op.ETERNAL, c.shift());
            assertEquals(0, c.shiftOrZero());
        }
    }

    @Test
    void testConjBuilder_DontAccept_TIMELESS() {
        for (ConjBuilder c : new ConjBuilder[]{new ConjTree(), new ConjList()}) {
            assertThrows(WTF.class, ()->c.add(Op.TIMELESS, x), ()->c.getClass().getSimpleName());
        }
    }
    @Test void testConjListDecomposeWeird1() {
        Compound x = $.$$c("((((x,#1)-->#2)&&c(#1)) &&+1 #2)");
        int dt = x.dt();
        ConjList c = ConjList.conds(x, 30, dt== Op.DTERNAL, dt== Op.XTERNAL);
        assertTrue(c.size() > 1);

    }
}