package nars.term;

import nars.term.anon.AnonCompound;
import org.junit.jupiter.api.Test;

import static nars.$.$$c;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AnonCompoundTest {

    @Test void test1() {
        assertAnonCompound("(a-->b)");
        assertAnonCompound("(a-->(c,(--,d),#1))");
    }

    private static void assertAnonCompound(String t) {
        Compound x = $$c(t);
        Compound y = AnonCompound.anon(x);
        //if (x == y) return;

        assertInstanceOf(AnonCompound.class, y);
        assertEquals(t, y.toString());
        assertEquals(x, y);
        assertEquals(y, x);
    }
}
