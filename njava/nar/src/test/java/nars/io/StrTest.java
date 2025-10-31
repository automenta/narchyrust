package nars.io;

import jcog.Str;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class StrTest {

    @Test
    void testN2() {
        
        assertEquals("1.0", Str.n2(1.00f));
        assertEquals(".50", Str.n2(0.5f));
        assertEquals(".09", Str.n2(0.09f));
        assertEquals(".10", Str.n2(0.1f));
        assertEquals(".01", Str.n2(0.009f));
        assertEquals("0.0", Str.n2(0.001f));
        assertEquals(".01", Str.n2(0.01f));
        assertEquals("0.0", Str.n2(0.0f));
        
        
    }

    























































}
