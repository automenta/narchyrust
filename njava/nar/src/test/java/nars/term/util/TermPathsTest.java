package nars.term.util;

import nars.$;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.util.TermPaths.pathExact;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TermPathsTest {

    @Test
    void test1() {
        assertArrayEquals(new byte[] { 0, 1}, pathExact($$("xor:(%1,%2)"), $.varPattern(2)) );
    }

    @Test
    void testCantCommute() {
        assertNull( pathExact($$("xor:{%1,%2}"), $.varPattern(2)) );
    }
}