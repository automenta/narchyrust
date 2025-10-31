package nars.term.util;

import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.$.$$c;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DTDifferTest {
    
    @Test
    void testSubPath_Impl() {
        var d = (DTDiffer.DTDifferSubExhaustive) DTDiffer.the($$c(
            "((((--,x)&&(--,y)) &&+- z) ==>+- z)"
        ));
        assertArrayEquals(new byte[] { 0 }, d.path);
        assertEquals($$("(((--,x)&&(--,y)) &&+- z)"), d.a);
    }

}