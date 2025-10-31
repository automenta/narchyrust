package nars.concept.dynamic;

import nars.*;
import nars.table.BeliefTables;
import nars.table.dynamic.DynTruthBeliefTable;
import nars.truth.PreciseTruth;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.*;

@Disabled class DynSectTest {

    private final NAR n = NARS.shell();

    private NAR dummyBeliefs() {
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("a:z", 0f, 0.9f);
        n.believe("x:b", 1f, 0.9f);
        n.believe("y:b", 1f, 0.9f);
        n.believe("z:b", 0f, 0.9f);
        return n;
    }

    private NAR dummyGoals(float xFreq, float yFreq, float zFreq) {
        n.want($$("a:x"), xFreq, 0.9f);
        n.want($$("a:y"), yFreq, 0.9f);
        n.want($$("a:z"), zFreq, 0.9f);
        return n;
    }

    @Test
    void dyn_inh_sect_beliefs1() throws Narsese.NarseseException {


        NAR n = dummyBeliefs();
        NALTask k = n.answer($("((x|y)-->a)"), BELIEF, 0);
        assertEquals("(((--,x)&&(--,y))-->a)", k.term().neg().toString());
        assertEquals(1, k.truth().freq());
//            assertEquals(0f, k.truth().freq());
//            assertEquals("(((--,x)&&(--,y))-->a)", k.term().toString());

    }
    @Test
    void dyn_inh_sect_beliefs2() throws Narsese.NarseseException {
        NAR n = dummyBeliefs();
        NALTask k = n.answer($("(--,(((--,x)&&y)-->a))"), BELIEF, 0);
        assertEquals("(((--,x)&&y)-->a)", k.term().neg().toString());
        assertEquals(1f, k.truth().freq());
    }
    @Disabled
    @Test
    void dyn_inh_sect_beliefs3() throws Narsese.NarseseException {
        NAR n = dummyBeliefs();
        NALTask k = n.answer($("((x-y)-->a)"), BELIEF, 0);
        assertEquals("(((--,y)&&x)-->a)", k.term().toString());
        assertEquals(0f, k.truth().freq());
    }

    @Test
    void testDynamicIntersectionAtZero() throws Narsese.NarseseException {
        testDynamicIntersectionAt(0);
    }
    @Test
    void testDynamicIntersectionAtEternal() throws Narsese.NarseseException {
        testDynamicIntersectionAt(ETERNAL);
    }



    void testDynamicIntersectionAt(long now) throws Narsese.NarseseException {

        NAR n = dummyBeliefs();

        assertNotNull(((BeliefTables) n.conceptualize($("((x|y)-->a)")).beliefs())
                .tableFirst(DynTruthBeliefTable.class));

        assertTruth("((x&y)-->a)", now, 1, 0.85f);
        assertTruth("((x|y)-->a)", now, 1, 0.85f);
        assertTruth("((x&z)-->a)", now, 0, 0.85f);
        assertTruth("((x|z)-->a)", now, 1, 0.85f);

        assertTruth("(b --> (x|y))", now, 1, 0.81f);
        assertTruth("(b --> (x|z))", now, 1, 0.81f);
        assertTruth("(b --> (x&z))", now, 0, 0.81f);

    }

    private void assertTruth(String tt, long now, float f, float c) throws Narsese.NarseseException {
        Truth t = n.beliefTruth(tt, now);
        PreciseTruth e = $.t(f, c);
        assertTrue(e.equals(t, 0.1f), ()->tt + " @ " + now + " => " + t + " , expected=" + e);
    }

    @Test void dyn_inh_sect_goals_1() throws Narsese.NarseseException {

        NAR n = dummyGoals(1, 1, 0);

        NALTask k = n.answer($("((--x && --y)-->a)"), GOAL, 0);
        assertNotNull(k);
        assertEquals("(((--,x)&&(--,y))-->a)", k.term().toString());
        assertEquals(0f, k.truth().freq(), 0.01f);
        assertEquals(0.81f, k.truth().conf(), 0.01f);
    }
    @Test void dyn_inh_sect_goals_2() throws Narsese.NarseseException {
        NAR n = dummyGoals(1, 1, 0);
        Term kt = $("((x|y)-->a)");
        NALTask k = n.answer(kt, GOAL, 0);
        assertNotNull(k);
        Term ku = k.term();
        assertEquals("(((--,x)&&(--,y))-->a)", ku.neg().toString());
        assertEquals(1, k.truth().freq());
    }
    @Test void dyn_inh_sect_goals_3() throws Narsese.NarseseException {
        NAR n = dummyGoals(1, 1, 0);
        NALTask k = n.answer($("(--,(((--,x)&&y)-->a))"), GOAL, 0);
        assertEquals("(((--,x)&&y)-->a)", k.term().neg().toString());
        assertEquals(1, k.truth().freq());
    }
    @Test void dyn_inh_sect_goals_4() throws Narsese.NarseseException {
        NAR n = dummyGoals(1, 1, 0);
        NALTask k = n.answer($("((x & --z)-->a)"), GOAL, 0);
        assertEquals(1f, k.truth().freq());
        assertEquals("(((--,z)&&x)-->a)", k.term().toString());
//            assertEquals(0f, k.truth().freq());
//            assertEquals("(((--,x)&&(--,y))-->a)", k.term().toString());

    }

}