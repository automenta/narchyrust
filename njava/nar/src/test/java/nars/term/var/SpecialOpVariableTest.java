package nars.term.var;

import nars.$;
import org.junit.jupiter.api.Test;

import static nars.Op.VAR_DEP;
import static org.junit.jupiter.api.Assertions.*;

class SpecialOpVariableTest {

    @Test
    void test1() {
        Variable i = $.varIndep(1);
        Variable d = $.varDep(1);
        SpecialOpVariable s = new SpecialOpVariable(i, VAR_DEP);
        UnnormalizedVariable u = new UnnormalizedVariable(VAR_DEP, "#$1");

        assertEquals("#$1", s.toString());
        assertNotEquals(s, i);
        assertNotEquals(s, d);

        assertArrayEquals(s.bytes(), u.bytes());
        assertEquals(s, u);


    }
}