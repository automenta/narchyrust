package nars.task;

import nars.*;
import nars.concept.TaskConcept;
import nars.table.BeliefTables;
import nars.table.dynamic.DynTruthBeliefTable;
import nars.test.TestNAR;
import nars.test.analyze.BeliefAnalysis;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.Op.IMPL;
import static nars.task.RevisionTest.x;
import static nars.term.util.Testing.assertEq;
import static nars.time.Tense.Present;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 7/5/15.
 */
class BeliefTableTest {

    final NAR n = NARS.tmp();

    private static void assertDuration(NAR n, String c, long start, long end) throws Narsese.NarseseException {
        TaskConcept cc = (TaskConcept) n.conceptualize(c);
        assertNotNull(cc, () -> c + " unconceptualized");


        assertNotNull(((BeliefTables) cc.beliefs()).tableFirst(DynTruthBeliefTable.class));

        NALTask t = n.belief(cc, start, end);
        assertNotNull(t);
        assertEquals(start, t.start());
        assertEquals(end, t.end(), ()-> t + " but end should be: " + end + '\n' + t.proof());
    }



    @Test
    void testEternalBeliefRanking() {


        BeliefAnalysis b = new BeliefAnalysis(n, x);

        b.believe(1.0f, 0.5f);
        b.print();

        TaskTable beliefs = b.concept().beliefs();

        assertEquals(0.5, (float) n.answer(beliefs, Op.ETERNAL, Op.ETERNAL, null, null, 0, NAL.answer.ANSWER_CAPACITY).conf(), 0.001);
        Truth bt = n.beliefTruth(b, n.time());
        assertNotNull(bt);
		assertEquals(0.5, (float) bt.conf(), 0.001);
        assertEquals(1, beliefs.taskCount());

        b.believe(1.0f, 0.5f);
        n.run();
        b.print();
        //assertEquals(3 /* revision */, beliefs.taskCount());
        assertEquals(0.669, (float) n.answer(beliefs, Op.ETERNAL, Op.ETERNAL, null, null, 0, NAL.answer.ANSWER_CAPACITY).conf(), 0.01);

        b.believe(1.0f, 0.5f);
        n.run();
        b.print();
        //assertEquals(5, beliefs.taskCount());
        assertEquals(0.75, (float) n.answer(beliefs, Op.ETERNAL, Op.ETERNAL, null, null, 0, NAL.answer.ANSWER_CAPACITY).conf(), 0.001);
		assertEquals(0.75, (float) n.beliefTruth(b, n.time()).conf(), 0.01);

        b.believe(1.0f, 0.5f);
        n.run();
        b.print();
        assertEquals(0.79, (float) n.answer(beliefs, Op.ETERNAL, Op.ETERNAL, null, null, 0, NAL.answer.ANSWER_CAPACITY).conf(), 0.2);
        //assertEquals(7, beliefs.taskCount());

    }

    @Test
    void testPolation0() {

        int dur = 1;


        n.time.dur(dur);

        BeliefAnalysis b = new BeliefAnalysis(n, x);

        long[] timing = {0, 2, 4};
        float[] freqPattern = {0, 0.5f, 1f};
        assertEquals(timing.length, freqPattern.length);
        int k = 0;
        float conf = 0.9f;
        for (int i = 0; i < freqPattern.length; i++) {
            b.believe(0.5f, freqPattern[k], conf, timing[k]);
            k++;
        }
        int c = freqPattern.length;
        assertEquals(c, b.size(true));

        BeliefTable table = b.concept().beliefs();

        b.print();
        int spacing = 4;
        int margin = spacing * (c / 2);
        for (int i = -margin; i < spacing * c + margin; i++)
            System.out.println(i + "\t" + table.truth(i, i, n));


        for (int i = 0; i < c; i++) {
            long w = timing[i];
            Truth truth = table.truth(w, w, n);
            float fExpected = freqPattern[i];
            assertEquals(fExpected, truth.freq(), 0.1f, () -> "exact truth @" + w + " == " + fExpected);

            NALTask match = n.answer(table, w, w, null, null, 0, NAL.answer.ANSWER_CAPACITY);
            assertEquals(fExpected, match.freq(), 0.1f, () -> "exact belief @" + w + " == " + fExpected);
        }


        for (int i = 1; i < c - 1; i++) {
            float f = (freqPattern[i - 1] + freqPattern[i] + freqPattern[i + 1]) / 3f;
            long w = timing[i];
            assertEquals(f, table.truth(w, w, n).freq(), 0.1f, () -> "t=" + w);
        }


    }

    @Test
    void testLinearTruthpolation() {
        n.time.dur(5);
        n.inputAt(10, "x. |");
        n.run(11);

        assertConf(0.90f, 10);
        assertConf(0.86f, 9);
        assertConf(0.86f, 11);
        assertConf(0.82f, 8);
        assertConf(0.82f, 12);
        assertConf(0.78f, 7);
    }

    private void assertConf(float c, int w) {
        Truth t = n.beliefTruth($$("x"), w, w, 5);
        assertNotNull(t);
        assertEquals(c, (float) t.conf(), 0.1f);
    }

    @Test
    void testStableDurationDithering() {

        int dur = 2;
        n.timeRes.set(dur);
        n.time.dur(dur);
        long cycles = dur * 10;

        TestNAR t = new TestNAR(n);

        t.confTolerance(0.1f);
        t.inputAt(0, "x. |");
        t.inputAt(dur*2+1, "y. |");
        t.mustBelieve(cycles, "(x &&+4 y)", 1f, 0.81f, s -> true  /* TODO test occ = 0..3 */);
        t.mustBelieve(cycles, "(x ==>+4 y)", 1f, 0.45f, s -> true  /* TODO test occ = 0..3 */);
        t.mustBelieve(cycles, "(y ==>-4 x)", 1f, 0.45f, s -> true  /* TODO test occ = 0..3 */);
        t.mustNotOutput(cycles, "(x &&+5 y)", BELIEF);
        t.mustNotOutput(cycles, "(x ==>+5 y)", BELIEF);
        t.mustNotOutput(cycles, "(x ==>+6 y)", BELIEF);
        t.mustNotOutput(cycles, "(y ==>-5 x)", BELIEF);
        t.mustNotOutput(cycles, "(y ==>-6 x)", BELIEF);
        // || tt.end() > 0 && tt.start() > dur)
        n.main().eventTask.on(_tt -> {
            NALTask tt = (NALTask) _tt;
            if (!tt.isInput() && (tt.start() % dur != 0 || tt.end() % dur != 0)) // || tt.end() > 0 && tt.start() > dur)
                fail(()->tt + " is not aligned");
        });
        t.run();
    }

    @Test
    void testTemporalUnion() throws Narsese.NarseseException {





        n.time.dur(2);
        int a = 1;
        n.inputAt(a, "a:x. |");
        int b = 2;
        n.inputAt(b, "a:y. |");
        n.run(b + 1);


        for (String t : new String[]{"a:(x|y)", "a:(x&y)", "a:(x~y)", "a:(y~x)"}) {
            assertDuration(n, t, a, b);
        }


    }

//    @Test
//    void testDurationIntersection() {
//        /*
//        WRONG: t=25 is not common to both; 30 is however
//        $.12 ((happy|i)-->L). 25 %.49;.81% {37: b;k} (((%1-->%2),(%3-->%2),task("."),notSet(%3),notSet(%1),neqRCom(%3,%1)),(((%1|%3)-->%2),((Intersection-->Belief))))
//            $.25 (i-->L). 30 %.84;.90% {30: k}
//            $.22 (happy-->L). 20⋈30 %.58;.90% {20: b}
//        */
//
//    }


//    @Test
//    void testBestMatchConjSimple() {
//
//    }

    @Test
    void testBestAnswerImplSimple() throws Narsese.NarseseException {


        n.believe("(a ==> b)", Present, 1f, 0.9f);
        n.believe("(a ==>+5 b)", Present, 1f, 0.9f);
        n.believe("(a ==>-5 b)", Present, 1f, 0.9f);

        long when = 0;

        {
            int dt = 3;
            Term tt = IMPL.the($.$("a"), +dt, $.$("b"));
            assertEq("(a ==>+3 b)", n.answer(tt, BELIEF, when).term()); //, ()->tt + " -> " + n.answer(tt, BELIEF, when)
        }

        {
            int dt = -3;
            Term tt = IMPL.the($.$("a"), +dt, $.$("b"));
            assertEq("(a ==>-2 b)", n.answer(tt, BELIEF, when).term()); //, ()->tt + " -> " + n.answer(tt, BELIEF, when)
        }

        {
            int dt = 0;
            Term tt = IMPL.the($.$("a"), +dt, $.$("b"));
            assertEq("(a ==>+3 b)", n.answer(tt, BELIEF, when).term()); //, ()->tt + " -> " + n.answer(tt, BELIEF, when)
        }

        //TODO
        Task bwd = n.answer($.impl($.$("a"), -5, $.$("b")), BELIEF, when);
        assertEquals("(a ==>-2 b)", bwd.term().toString());


        Task x = n.answer($.impl($.$("a"), Op.DTERNAL, $.$("b")), BELIEF, when);
        System.out.println(x);


    }


}