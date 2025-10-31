package nars.subterm;

import nars.$;
import nars.Narsese;
import nars.Op;
import nars.Term;
import nars.term.atom.Atomic;
import nars.term.util.Terms;
import nars.term.util.Testing;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 11/12/15.
 */
class CachedSubtermsTest {

    @Test
    void testSubtermsEquality() throws Narsese.NarseseException {

        Term a = $.$("(a-->b)");
        Term b = $.impl(Atomic.atomic("a"), Atomic.atomic("b"));
        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());

        Testing.assertEq(a.subterms(), b.subterms());


        assertNotEquals(0, a.compareTo(b));
        assertNotEquals(0, b.compareTo(a));

        /*assertTrue("after equality test, subterms vector determined shareable",
                a.subterms() == b.subterms());*/


    }

    @Test
    void testSortedTermContainer() throws Narsese.NarseseException {
        Term aa = $.$("a");
        Term bb = $.$("b");
        Subterms a = Op.terms.subterms(aa, bb);
        assertTrue(a.subtermsSorted());
        Subterms b = Op.terms.subterms(bb, aa);
        assertFalse(b.subtermsSorted());
        Subterms s = Op.terms.subterms(Terms.commute(b.arrayShared()));
        assertTrue(s.subtermsSorted());
        assertEquals(a, s);
        assertNotEquals(b, s);
    }


}