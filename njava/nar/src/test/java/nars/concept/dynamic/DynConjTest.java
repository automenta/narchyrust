package nars.concept.dynamic;

import jcog.data.list.Lst;
import nars.*;
import nars.concept.TaskConcept;
import nars.table.BeliefTables;
import nars.table.dynamic.DynTruthBeliefTable;
import nars.term.Compound;
import nars.truth.dynamic.DynConj;
import nars.truth.dynamic.DynTruth;
import nars.truth.proj.IntegralTruthProjection;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static nars.$.*;
import static nars.Op.*;
import static nars.term.util.Testing.assertEq;
import static nars.truth.dynamic.DynConj.Conj;
import static org.junit.jupiter.api.Assertions.*;

class DynConjTest extends AbstractDynTaskTest {

    private static List<String> components(DynTruth model, Compound xyz, long s, long e) {
        List<String> components = new Lst();
        model.decompose(xyz, s, e, (what, whenStart, whenEnd) -> {
            components.add(what + " @ " + whenStart + ".." + whenEnd);
            return true;
        });
        return components;
    }

    static List<String> conjDynComponents(Compound xyz, long s, long e) {
        return components(Conj, xyz, s, e);
    }

    @Test
    void decomposeableXternal() {
        assertFalse(DynConj.condDecomposeable($$("(((--,c)&&#1) &&+- (--,#1))"), true));
    }

    @Test
    void testDynamicConjunction2() throws Narsese.NarseseException {

        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("b:x", 0f, 0.9f);
        long now = n.time();

        assertEquals(t(1f, 0.81f), n.beliefTruth("(a:y && --b:x)", now));

        assertEquals(t(0f, 0.81f), n.beliefTruth($("(b:x && a:y)"), now));

    }

    @Disabled @Test
    void testDynamicConjunctionEternalOverride() throws Narsese.NarseseException {
        n.believe($$("a:x"), 0);
        n.believe($$("a:y"), 0);

        final long now = 0; //n.time();
        assertEquals(t(1, 0.81f), n.beliefTruth($("(a:x && a:y)"), now));

        {
            //temporal evaluated a specific point
            NALTask xy = n.belief($("(a:x && a:y)"), now);
            assertEquals("((x-->a)&&(y-->a))", xy.term().toString());
            assertEquals(t(1, 0.81f), xy.truth());
        }

        {
            Term v = $("(a:x && --a:y)");
            NALTask xAndNoty = n.belief(v, now);
            assertNotNull(xAndNoty);
            assertEquals("(((--,y)&&x)-->a)", xAndNoty.term().toString());
            assertEquals(t(0f, 0.81f), xAndNoty.truth());
        }

        {
            //remain eternal
            NALTask xy = n.belief($("(a:x && a:y)"), ETERNAL);
            assertNotNull(xy);
            assertEquals(0, xy.start()); //exact time since it was what was stored
            assertEquals("((x&&y)-->a)", xy.term().toString());
            assertEquals(t(1f, 0.81f), xy.truth());
        }


        //override or revise dynamic with an input belief
        {
            n.believe($$("--(a:x && a:y)"), 0);
            assertEquals(1, n.concept("(a:x && a:y)").beliefs().taskCount());

            NALTask ttEte = n.answerBelief($("(a:x && a:y)"), now);
//            assertEquals(1, ttEte.stamp().length);


            assertTrue(ttEte.toString().contains("((x&&y)-->a). 0 %"));

            Truth tNow = n.beliefTruth($("(a:x && a:y)"), now);
            assertTrue(
                    t(0.32f, 0.93f /*0.87f*/)
                            //$.t(0.00f, 0.90f)
                            //$.t(0.32f, 0.90f /*0.87f*/)
                            .equals(tNow, 0.01f), () -> "was " + tNow + " at " + now);

        }
        {
            n.believe($$("--(a:x && a:y)"), 0);
            assertTrue(2 <= n.concept("(a:x && a:y)").beliefs().taskCount());

            NALTask ttNow = n.answerBelief($("(a:x && a:y)"), now);
            assertTrue(ttNow.NEGATIVE());
            assertTrue(ttNow.toString().contains("((x&&y)-->a). 0"), ttNow::toString);
        }


//        NALTask tAfterTask = n.belief($("(a:x && a:y)"), now + 2);
//        assertNotNull(tAfterTask);
//        assertTrue(tAfterTask.POSITIVE());
////        assertEquals(now + 2, tAfterTask.start());
////        assertEquals(now + 2, tAfterTask.end());
//
//        Truth tAfter = n.beliefTruth($("(a:x && a:y)"), now + 2);
//        assertNotNull(tAfter);
//        assertTrue(tAfter.POSITIVE());
//        //assertTrue($.t(0.19f, 0.88f).equalsIn(tAfter, n), () -> tAfter.toString());
//
//        Truth tLater = n.beliefTruth($("(a:x && a:y)"), now + 5);
//        assertEquals(tLater.POSITIVE(), tAfter.POSITIVE());
//        assertTrue((float) tLater.conf() < (float) tAfter.conf());
//        //assertTrue($.t(0.19f, 0.79f).equalsIn(tLater, n), () -> tLater.toString());
    }

    @Test
    void testDynamicConjunctionEternalTemporalMix() throws Narsese.NarseseException {

//        String xx = "((e&&x)&&(e&&y))";
//        assertEquals(xx, $$("((x&&y)&&e)").toString());

        n.believe($$("x"), 0);
        n.believe($$("y"), 0);
        n.believe($$("e"), ETERNAL);

        Term xye = $("(&&,x,y,e)");

        NALTask atZero = n.belief(xye, 0);
        assertNotNull(atZero);

        NALTask atOne = n.belief(xye, 1);
        assertNotNull(atOne);

        NALTask atEte = n.belief(xye, ETERNAL);
        assertNotNull(atEte);

        assertEquals(0, atZero.start());

        assertEquals(0, atEte.start());

        assertEquals(0.73f, (float) atZero.conf(), 0.01f);
        assertEquals(0.73f, (float) atEte.conf(), 0.01f);

//        //loose=true
//        assertEquals(1, atOne.start()); //if not moved
//        assertEquals(0.73f, (float) atOne.conf(), 0.2f);
    }

    @Test
    void testDynamicConjunctionTemporalOverride() throws Narsese.NarseseException {
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);

        long now = n.time();
        assertEquals(t(1f, 0.81f), n.beliefTruth($("(a:x && a:y)"), now));

        n.believe($$("--(a:x && a:y)"), now);

        NALTask u = n.belief($("(a:x && a:y)"), now);
        assertNotNull(u);
        Truth tt = u.truth();
        assertTrue(t(0.32f, 0.93f).equals(tt, 0.01f), tt::toString);
    }

    @Test
    void testIntermpolationInhConjDuality() throws Narsese.NarseseException {
        var t = new IntegralTruthProjection(2);
        t.dur(1);
        t.add(n.inputTask("(x:a&&x:b)."),
              n.inputTask("((a-->x) &&+2 (b-->x))."));
        Truth T = t.truth();
        assertEq("((a-->x) &&+1 (b-->x))", t.term);
        assertEquals("%1.0;.82%", T.toString());
    }

    @Test
    void testDynamicConjunction3() throws Narsese.NarseseException {

        n.believe("a:x", 1, 0.9f);
        n.believe("a:y", 1, 0.9f);
        n.believe("a:z", 1, 0.9f);


        TaskConcept cc = (TaskConcept) n.conceptualize($("(&&, a:x, a:y, a:z)"));
        Truth now = n.beliefTruth(cc, n.time());
        assertNotNull(now);
        assertTrue(t(1f, 0.73f).equals(now, 0.1f), () -> now + " truth at " + n.time());


        {
            TaskConcept ccn = (TaskConcept) n.conceptualize($("(&&, a:x, a:w)"));
            Truth nown = n.beliefTruth(ccn, n.time());
            assertNull(nown);
        }


        Concept ccn = n.conceptualize($("(&&, a:x, (--, a:y), a:z)"));

        {
            NALTask t = n.belief(ccn.term());
            assertNotNull(t);
            assertEquals(0f, t.freq());
        }

        assertInstanceOf(TaskConcept.class, ccn);
        Truth nown = n.beliefTruth(ccn, n.time());
        assertEquals("%0.0;.73%", nown.toString());

        n.clear();


        n.believe("a:y", 0, 0.95f);

        //n.concept("a:y").print();
        NALTask ay = n.belief($$("a:y"));
        assertTrue(ay.freq() < 0.5f);

//        for (int i = 0; i < 4; i++) {
//            Task y = n.belief(n.conceptualize($("(&&, a:x, a:y, a:z)")), n.time());
//            Truth yt = y.truth();
//            assertTrue(yt.freq() < 0.4f, () -> y.proof());
//        }

    }

    @Test
    void testDynamicConjunctionEternalBeliefs() throws Narsese.NarseseException {

        n.believe($("x"));
        n.believe($("y"));
        n.believe($("--z"));


        for (long w : new long[]{ETERNAL, 0, 1}) {
            assertEquals(t(1, 0.81f), n.truth($("(x && y)"), BELIEF == BELIEF, w, w, 0));
            assertEquals(t(0, 0.81f), n.truth($("(x && --y)"), BELIEF == BELIEF, w, w, 0));
            assertEquals(t(1, 0.81f), n.truth($("(x && --z)"), BELIEF == BELIEF, w, w, 0));
        }
    }

    @Test
    void testDynamicConjunctionEternalGoals() throws Narsese.NarseseException {

        n.want($("x"));
        n.want($("y"));
        n.want($("--z"));

        for (long w : new long[]{ETERNAL, 0, 1}) {
            assertEquals(t(1, 0.81f), n.truth($("(x && y)"), GOAL == BELIEF, w, w, 0));
            assertEquals(t(0, 0.81f), n.truth($("(x && --y)"), GOAL == BELIEF, w, w, 0));
            assertEquals(t(1, 0.81f), n.truth($("(x && --z)"), GOAL == BELIEF, w, w, 0));
        }
    }

    @Test
    void testDynamicConjunction2Temporal() throws Narsese.NarseseException {
        float dur = 8;

        n.believe($("x"), 0);
        n.believe($("y"), 4);
        n.time.dur(dur);
        TaskConcept cc = (TaskConcept) n.conceptualizeDynamic($("(x && y)"));

        TaskTable xtable = cc.beliefs();


        {
            @Nullable Term template = $("(x &&+- y)");
            NALTask x = n.answer(xtable, 0, 4, template, null, dur, NAL.answer.ANSWER_CAPACITY);
            assertEq("(x &&+4 y)", x.term());
            assertEquals(0.81f, (float) x.conf(), 0.05f);
        }

        @Nullable Term template2 = $("(x &&+4 y)");
        assertEquals(0.81f, (float) n.answer(xtable, 0, 0, template2, null, dur, NAL.answer.ANSWER_CAPACITY).conf(), 0.05f);
        @Nullable Term template1 = $("(x &&+6 y)");
        assertEquals(0.74f, (float) n.answer(xtable, 0, 0, template1, null, dur, NAL.answer.ANSWER_CAPACITY).conf(), 0.07f);
        @Nullable Term template = $("(x &&+2 y)");
        assertEquals(0.75f, (float) n.answer(xtable, 0, 0, template, null, dur, NAL.answer.ANSWER_CAPACITY).conf(), 0.07f);

//TODO
//        {
//            NALTask tt = xtable.task(0, 0, $("(x &&-32 y)"), null, dur, NAL.answer.ANSWER_CAPACITY, n);
//            //since no such result is really valid, the task returned should be the most likely
//            assertEquals($("(x &&+4 y)"), tt.term());
//            //assertEquals(0.3f, (float) tt.conf(), 0.2f);
//        }
        {
            Term x2y = $("(x &&+2 y)");
            NALTask tt = n.answer(xtable, 0, 0, x2y, null, dur, NAL.answer.ANSWER_CAPACITY);
            assertEquals($("(x &&+4 y)"), tt.term());
            assertEquals(0.79f, (float) tt.conf(), 0.05f);
        }
        {
            Term x0y = $(
                    //"(x&&y)"
                    "(x &&+- y)"
            );
            NALTask tt = n.answer(xtable, 0, 0, x0y, null, dur, NAL.answer.ANSWER_CAPACITY);
            assertEquals($("(x &&+4 y)"), tt.term());
            //assertEq("(x&&y)", p.term());
            assertEquals(0.79f, (float) tt.conf(), 0.1f);
        }


        //TODO test dur = 1, 2, ... etc

    }

    @Disabled @Test
    void testDynamicConceptValid1() throws Narsese.NarseseException {
        Term c =

                CONJ.the(XTERNAL, $("(--,($1 ==>+- (((joy-->fz)&&fwd) &&+- $1)))"),
                        $("(joy-->fz)"), $("fwd")).normalize();

        assertInstanceOf(Compound.class, c, c::toString);
        assertTrue(NALTask.TASKS(c), () -> c + " should be a valid task target");
    }


    @Test
    void testDynamicConjunctionXYZ() throws Narsese.NarseseException {

        n.believe("x", 1f, 0.50f);
        n.believe("y", 1f, 0.50f);
        n.believe("z", 0f, 0.81f);

        assertEquals(
                "%0.0;.20%", n.beliefTruth(
                        n.conceptualize($("(&&,x,y,z)")
                        ), n.time()).toString()
        );
        {
            NALTask bXYZ = n.belief($("(&&,x,y,z)"), n.time());
            assertEquals("(&&,x,y,z)", bXYZ.term().toString());
            assertEquals(3, bXYZ.stamp().length);
        }
        {

            NALTask bXY = n.belief($("(x && y)"), n.time());
            assertEquals("(x&&y)", bXY.term().toString());
            assertEquals(2, bXY.stamp().length);
        }
        {

            NALTask bXY = n.belief($("(x && y)"), ETERNAL);
            assertEquals("(x&&y)", bXY.term().toString());
            assertEquals(2, bXY.stamp().length);
        }

        assertEquals(
                "%0.0;.41%", n.beliefTruth(
                        n.conceptualize($("(y && z)")
                        ), n.time()).toString()
        );
        assertEquals(
                "%1.0;.25%", n.beliefTruth(
                        n.conceptualize($("(x && y)")
                        ), n.time()).toString()
        );
    }

    @Test
    void testDynamicConjConceptWithNegations() throws Narsese.NarseseException {

        for (String s : new String[]{
                "((y-->t) &&+1 (t-->happy))",
                "(--(y-->t) &&+1 (t-->happy))",
                "((y-->t) &&+1 --(t-->happy))",
                "(--(y-->t) &&+1 --(t-->happy))",
        }) {
            Concept c = n.conceptualize($(s));
            assertNotNull(((BeliefTables) c.beliefs()).tableFirst(DynTruthBeliefTable.class));
            assertNotNull(((BeliefTables) c.goals()).tableFirst(DynTruthBeliefTable.class));
        }

    }

    @Test
    void seq3() throws Narsese.NarseseException {

        n.believe($("x"), 0);
        n.believe($("y"), 1);
        n.believe($("z"), 2);
        n.time.dur(8);

        Term xyz = $("((x &&+1 y) &&+1 z)");
        NALTask t = n.answerBelief(xyz, 0, 0);
        assertNotNull(t);
        assertEquals(xyz, t.term());
        assertEquals(1f, t.freq(), 0.05f);
        assertEquals(0.73f, (float) t.conf(), 0.1f);
    }




    @ParameterizedTest
    @ValueSource(strings = {"(x &&+1 x)", "(x &&+- x)"})
    @Disabled
    void testDynamicConjunction_collapseToRevisionOnIntersect(String s) throws Narsese.NarseseException {

        Term x = $("x");
        n.believe(x, 1f, 0.9f, 0, 2);
        n.believe(x, 0.5f, 0.9f, 0, 2);
        n.time.dur(8);


        Term xyz = $(s);
        NALTask t = n.answerBelief(xyz, 0, 2);
        assertNotNull(t, () -> s + " -> null");
        assertEq("x", t.term());
        assertEquals(2, t.stamp().length);
        assertEquals(0, t.start());
        assertEquals(2, t.end());
        assertEquals(0.5f, t.freq(), 0.05f);
        assertEquals(0.81f, (float) t.conf(), 0.1f);

    }

    @Test
    void CoNegatedXternal() throws Narsese.NarseseException {
        n.time.dur(1);

        n.believe($("x"), 0, 0);
        n.believe($("--x"), 2, 2);

        float conf = 0.59f;

        //try all combinations.  there is only one valid result
        int results = 0;
        for (int i = 0; i < 16; i++) {
            NALTask t = n.answerBelief($("(x &&+- --x)"), 0, 2);
            if (t == null) continue;

            assertEquals(1, t.range());
            assertEquals(0.81f, t.conf(), 0.01f);

            String s = t.term().toString();
            switch (s) {
                case "(x &&+2 (--,x))" -> {
                    assertEquals(1, t.freq(), 0.4f);
                    assertEquals(conf, (float) t.conf(), 0.5f);
                    results++;
                }
                case "((--,x) &&+2 x)" -> {
                    assertEquals(0, t.freq(), 0.4f);
                    assertEquals(conf, (float) t.conf(), 0.5f);
                    results++;
                }
                default -> fail(s);
            }
        }

        assertTrue(results > 0);
    }

    @Test
    void depVarContent() {
        assertFalse(isDynamicBeliefTable("(x && #1)"), "no way to decompose");
    }

    @Test
    void depVarContent2() {
        String s = "(&&,x,y,#1)";
        assertTrue(DynConj.condDecomposeable($$(s), true));
        assertTrue(isDynamicBeliefTable(s), "decomposeable two ways, paired with either x or y");
    }

    @Test
    void conjSeqWithDepVar() throws Narsese.NarseseException {

        n.believe($("(x && #1)"), 0);
        n.believe($("y"), 1);
        //n.believe($("(y && #1)"), 2);
        n.time.dur(8);

        Task t = n.answerBelief($$("((x&&#1) &&+1 y)"), 0);
        assertNotNull(t);
    }

    @Test
    void conjSeqWithDepVarSeq() throws Narsese.NarseseException {

        n.believe($("(x &&+1 #1)"), 0);
        n.believe($("y"), 2);
        n.time.dur(8);

        Task T = null;
        //try because there are 2 solutions, one will be null
        for (int i = 0; i < 16; i++) {
            Task t = n.answerBelief($$("((x &&+1 #1) &&+1 y)"), 0);
            if (t != null) {
                T = t;
                break;
            }
        }
        assertNotNull(T);
    }

    @Test
    void testEviDilution() {

        n.believe("x", 0.75f, 0.50f, 0, 0);
        n.believe("y", 0.25f, 0.50f, 1, 1);
        n.believe("z", 0.25f, 0.50f, 2, 2);

        Term aa = $$("(x & --y)");
        NALTask a = n.answerBelief(aa, 0, 1);
        assertNotNull(a);
        assertEq("(x &&+1 (--,y))", a.term());
        assertEquals(0, a.start());
        assertEquals(0, a.end());
        assertEquals(1, a.range());
        assertEquals(1, a.term().seqDur());
        assertTrue(a.conf() < /*Util.sqr(0.5f)* */0.9f);

        Term bb = $$("(x & --z)");
        NALTask b = n.answerBelief(bb, 0, 2);
        assertNotNull(b);
        assertEq("(x &&+2 (--,z))", b.term());
        assertEquals(1, b.range());
        assertEquals(2, b.term().seqDur());

        assertEquals(b.freq(), a.freq());
        //assertTrue((float) b.conf() < (float) a.conf());
        //assertTrue(b.pri() < a.pri());

//        assertEquals("", a.toStringWithoutBudget());
//        assertEquals("", b.toStringWithoutBudget());


    }

//    @Test public void testDynamicIntersectionInvalidCommon() throws Narsese.NarseseException {
//        //TODO
//        n.believe("(x&&+1):y", 0.75f, 0.50f);
//        n.believe("(x&&+2):z", 0.25f, 0.50f);
//        Term xMinY = $("(x(x ~ y)");
//        Term yMinX = $("(y ~ x)");
//        assertEquals(DynamicTruthBeliefTable.class, n.conceptualize(xMinY).beliefs().getClass());
//        assertNull(
//                "%.56;.25%", n.beliefTruth(xMinY, n.time()).toString()
//        );
//    }

}