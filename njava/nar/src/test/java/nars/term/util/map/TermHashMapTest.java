package nars.term.util.map;

import nars.$;
import nars.term.atom.Anom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TermHashMapTest {
    @Test
    void test1() {
        TermHashMap m = new TermHashMap();

        m.put($.atomic("x"), "a");
        assertNotNull(m.other);
        assertEquals(1, m.size());
        assertFalse(m.isEmpty());
        m.put($.varDep(1), "v");
        assertNotNull(m.id);
        assertEquals(2, m.size());
        m.put($.varDep(1), "v");
        assertEquals(2, m.size()); 

        assertEquals("{#1=v, x=a}", m.toString());

        assertEquals("v", m.remove($.varDep(1)));
        assertEquals(1, m.size());
        assertNull(m.remove($.varDep(1)));

        assertEquals("a", m.remove($.atomic("x")));
        assertEquals(0, m.size());

        
        m.put($.atomic("x"), "a");

        m.clear();

        assertEquals(0, m.size());
        assertTrue(m.isEmpty());



    }

    @Test
    void testNegAnonKeys() {

        TermHashMap m = new TermHashMap();
        m.put(Anom.anom(1), "p");
        m.put(Anom.anom(1).neg(), "n");
        assertEquals(2, m.size());
        assertEquals("p", m.get(Anom.anom(1)));
        assertEquals("n", m.get(Anom.anom(1).neg()));
        assertTrue(m.other.isEmpty()); //no need to instantiate other for neg
    }

}