package nars.concept.dynamic;

import nars.*;
import nars.concept.TaskConcept;
import nars.term.Compound;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.Op.ETERNAL;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.*;

class DynConjFactoredTest extends AbstractDynTaskTest {
    @Test
    void testDynamicConjunctionFactoredEte() throws Narsese.NarseseException {
        testDynamicConjunctionFactored(true);
    }

    @Test
    void testDynamicConjunctionFactoredTemporal() throws Narsese.NarseseException {
        testDynamicConjunctionFactored(false);
    }

    private void testDynamicConjunctionFactored(boolean ete) throws Narsese.NarseseException {

        if (ete) {
            n.believe($("x"), ETERNAL);
        } else {
            n.believe($("x"), 0, 4);
        }

        n.believe($("y"), 0);
        n.believe($("z"), 2);

        {
            NALTask t = n.answerBelief($$("(x&&y)"), 0);
            assertNotNull(t);
            assertTrue(t.POSITIVE());
        }
        {
            NALTask t = n.answerBelief($$("(x&&z)"), 2);
            assertNotNull(t);
            assertTrue(t.POSITIVE());
        }

        {
            Compound xyz = $$c("(x && (y &&+2 z))");
            assertEquals(
                    //"[x @ 0..2, y @ 0..0, z @ 2..2]",
                    "[(x&&y) @ 0..0, (x&&z) @ 2..2]",
                    //"[x @ 0..0, y @ 0..0, x @ 2..2, z @ 2..2]",
                    //"[x @ 0..2, y @ 0..0, z @ 2..2]",
                    DynConjTest.conjDynComponents(xyz, 0, 0).toString());
            assertEquals(
                    "[x @ 0..4, y @ 0..2, z @ 2..4]",
                    //"[(x&&y) @ 0..2, (x&&z) @ 2..4]",
                    DynConjTest.conjDynComponents(xyz, 0, 2).toString());

            NALTask t = n.answerBelief(xyz, 0, 0);
            assertNotNull(t);
            assertEq("((y &&+2 z)&&x)" /*xyz*/ /*"((x&&y) &&+2 (x&&z))"*/,
                    t.term());
            assertEquals(1, t.freq(), 0.05f);
            assertEquals(0.81f, (float) t.conf(), 0.4f);
        }

        int dur = 0; //TODO test other durations
        @Deprecated TaskConcept cc = (TaskConcept) n.conceptualize($("(&&, x, y, z)"));
        @Deprecated TaskTable xtable = cc.beliefs();
        {
            Term xyz = $("((x && y) &&+2 (x && z))");
//            assertEq("((y &&+2 z)&&x)"/*"((x&&y) &&+2 (x&&z))"*/, xyz);
            NALTask t = n.answer(xtable, 0, 0, xyz, null, dur, NAL.answer.ANSWER_CAPACITY);
            assertEquals(1f, t.freq(), 0.05f);
            assertEquals(0.81f, (float) t.conf(), 0.4f);
        }
        {
            @Nullable Term template = $("((x && y) &&+2 (x && z))");
            NALTask t = n.answer(xtable, 0, 0, template, null, dur, NAL.answer.ANSWER_CAPACITY);
            assertEquals(1f, t.freq(), 0.05f);
            assertEquals(0.81f, (float) t.conf(), 0.4f);
        }


    }

    @Test
    void testDynamicConjunctionFactoredWithAllTemporalEvidence1() throws Narsese.NarseseException {


        xyzSetup();
        Term xyz = $("((x&&y) &&+4 (x&&z))");
        NALTask t = n.answerBelief(xyz, 0, 0);
        assertNotNull(t);
        assertEquals(1, t.freq(), 0.05f);
        assertEquals(0.73f, (float) t.conf(), 0.1f);
        assertEq(xyz, t.term());
    }

    @Test
    void testDynamicConjunctionFactoredWithAllTemporalEvidence2() throws Narsese.NarseseException {
        xyzSetup();
        Term xyz = $(
                //"((y &&+2 z)&&x)"
                "((x&&y) &&+2 (x&&z))"
        );
        NALTask t = n.answerBelief(xyz, 0);
        assertNotNull(t);
        assertEq(xyz, t.term());
        assertEquals(1, t.freq(), 0.05f);
        assertEquals(0.73f, (float) t.conf(), 0.1f);
    }

    @Test
    void testDynamicConjunctionFactoredWithAllTemporalEvidence3() throws Narsese.NarseseException {
        xyzSetup();
        //Term xyz = $("((x&&y) &&+2 z))");
        Term xyz = $("(&&+- ,x,y,z)");
        NALTask t = n.answerBelief(xyz, 0);
        assertNotNull(t);
        assertEquals(1f, t.freq(), 0.01f);
        assertEquals(0.73f, (float) t.conf(), 0.01f);
        assertEq("((y &&+2 z)&&x)", t.term());
        assertEquals(3, t.stamp().length);

    }

    @Test
    void testDynamicConjunctionFactoredWithAllTemporalEvidence4() throws Narsese.NarseseException {
        xyzSetup();
        //Term xyz = $("((x&&y) &&+2 z))");
        Term xyz = $("(&&+- ,x,--y,z)");
        NALTask t = n.answerBelief(xyz, 0);
        assertNotNull(t);
        assertEquals(0, t.freq(), 0.01f);
        assertEquals(0.73f, (float) t.conf(), 0.01f);
        assertEq("(((--,y) &&+2 z)&&x)", t.term());


    }

    private void xyzSetup() throws Narsese.NarseseException {
        n.believe($("x"), 0, 2);
        n.believe($("y"), 0);
        n.believe($("z"), 2);
//        n.time.dur(8);
    }
}