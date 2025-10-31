package nars.term;

import nars.Narsese;
import nars.Term;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.Op.*;
import static nars.Op.or;
import static org.junit.jupiter.api.Assertions.*;

/**
 * test target hash and structure bits
 */
class TermHashTest {

    @Test
    void testStructureIsVsHas() throws Narsese.NarseseException {

        assertTrue(inh("a", "b").hasAny(ATOM));
        assertTrue(inh(p("a"), $("b"))
                .hasAny(or(ATOM, PROD)));

        assertFalse(inh(p("a"), $("b"))
                .isAny(or(SIM, PROD)));
        assertNotSame(PROD, inh(p("a"), $("b"))
                .op());

        assertSame(INH, inh("a", "b").op());
        assertTrue(inh("a", "b").hasAny(INH));
        assertTrue(inh("a", "b").hasAny(ATOM));
        assertFalse(inh("a", "b").hasAny(SIM));
    }

    @Test
    void testHasAnyVSAll() throws Narsese.NarseseException {
        @Nullable Term iii = impl(inh("a", "b"), $("c"));
        assertTrue(iii.hasAll(or(IMPL, INH)));
        assertFalse(iii.hasAll(or(IMPL, SIM)));
        assertTrue(iii.hasAny(or(IMPL, INH)));

    }














}