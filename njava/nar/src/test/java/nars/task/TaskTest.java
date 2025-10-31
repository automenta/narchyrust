package nars.task;

import nars.*;
import nars.concept.TaskConcept;
import nars.term.Compound;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static nars.$.*;
import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 11/3/15.
 */
public class TaskTest {

    @Test
    void testTenseEternality() throws Narsese.NarseseException {
        NAR n = new NARS().get();

        String s = "<a --> b>.";

        assertEquals(ETERNAL, ((NALTask)Narsese.task(s, n)).start());

        assertTrue(((NALTask)Narsese.task(s, n)).ETERNAL(), "default is eternal");

        assertEquals(ETERNAL, ((NALTask)Narsese.task(s, n)).start(), "tense=eternal is eternal");



    }

    @Test
    void testInvalidStatementIndepVarTask() {
        NAR t = NARS.shell();
        try {
            t.inputTask("at($1,$2,$3).");
            fail("");
        } catch (Exception e) {
            assertTrue(true);
        }
    }


    @Test
    void testRepeatEvent() throws Narsese.NarseseException {
        NAR n = NARS.shell();

        for (String x : new String[]{
                "((a) ==>+1 (a))",
                "((a) &&+1 (a))",

                /*"((a) &&+1 (a))",*/
        }) {
            Term t = $(x);
            assertInstanceOf(Compound.class, t, () -> x + " :: " + t);
            assertTrue(t.dt() != DTERNAL);

            Task y = task(t, BELIEF, t(1f, 0.9f)).apply(n);

            //y.term().printRecursive();
            assertEquals(x, y.term().toString());

        }


    }

    @Test void InvalidIndepOnlyTask() {
        if (!NAL.ABSTRACT_TASKS_ALLOWED) {
            assertFalse(NALTask.TASKS($$("((--,$1) ==>-2 $1)")));
            assertFalse(NALTask.TASKS($$("($1 ==>-2 $1)")));
            assertFalse(NALTask.TASKS($$("(#1 ==>-2 #1)")));
            assertFalse(NALTask.TASKS($$("(#1,#2)")));
        }
    }



    @Test
    void inputTwoUniqueTasksDef() throws Narsese.NarseseException {
        inputTwoUniqueTasks(new NARS().get());
    }


    private static void inputTwoUniqueTasks(NAR n) throws Narsese.NarseseException {



        NALTask x = n.inputTask("<a --> b>.");
        assertArrayEquals(new long[]{1}, x.stamp());
        n.run();

        NALTask y = n.inputTask("<b --> c>.");
        assertArrayEquals(new long[]{2}, y.stamp());
        n.run();

        n.reset();

        n.input("<e --> f>.  <g --> h>. ");

        n.run(10);

        NALTask q = n.inputTask("<c --> d>.");
        assertArrayEquals(new long[]{5}, q.stamp());

    }


    @Test
    void testDoublePremiseMultiEvidence() throws Narsese.NarseseException {



        NAR d = new NARS().get();

        d.input("<a --> b>.", "<b --> c>.");

        long[] ev = {1, 2};
        d.main().eventTask.on(t -> {

            if (t instanceof DerivedTask && ((DerivedTask)t).parentBelief()!=null) {
                long[] s = ((NALTask)t).stamp();
                assertArrayEquals(ev, s, () -> "all double-premise derived terms have this evidence: "
                        + t + ": " + Arrays.toString(ev) + "!=" + Arrays.toString(s));
            }

//            System.out.println( ((NALTask)t).proof());

        });

        d.run(256);


    }

    @Deprecated public static TaskBuilder task( Term term, byte punct, float freq, float conf) {
        return task(term, punct, t(freq, conf));
    }

    @Deprecated public static TaskBuilder task( Term term, byte punct, Truth truth) {
        return new TaskBuilder(term, punct, truth);
    }

    @Deprecated public static TaskBuilder task( String term, byte punct, float freq, float conf) throws Narsese.NarseseException {
        return task($(term), punct, freq, conf);
    }

    @Test
    void testValid() throws Narsese.NarseseException {
        NAR tt = NARS.shell();
        Task t = task("((&&,#1,(#1 &| #3),(#2 &| #3),(#2 &| a)) =|> b)", BELIEF, 1f, 0.9f).apply(tt);
        assertNotNull(t);
        Concept c = tt.concept(t.term(), true);
        assertNotNull(c);
    }


    @Test void ValidIndepTaskConcept() {
        NAR tt = NARS.shell();
        Concept c = tt.conceptualize($$("(((sx,$1)&|good) ==>+2331 ((sx,$1)&&good))"));
        assertInstanceOf(TaskConcept.class, c);
    }
//    @Test void DiffQueryVarNormalization() throws Narsese.NarseseException {
//        NAR tt = NARS.shell();
//        Term x = assertEq("(?2~?1)", "(?x~?y)");
//        assertEq("(?1~y)", "(?x~y)");
//        assertEq("(?2~?1)", "(?y~?x)");
//        assertEq("(x~?1)", "(x~?y)");
//        assertEquals("((?2~?1)-->z)?",tt.input("((?x~?y)-->z)?").get(0).toStringWithoutBudget());
//        assertEquals("((?2~?1)-->z)?",tt.input("((?y~?x)-->z)?").get(0).toStringWithoutBudget());
//    }

//    @Test void testPostNormalizeImpl() {
//        Term a = $$("(((x &&+1 --$1) &&+1 --$1) ==>+- $1)");
//        assertEq("(((x &&+1 (--,$1)) &&+1 (--,$1)) ==>+- $1)", a);
//        assertEq("", a.normalize());
//    }

    @Test
    void testValidIndep() {
        assertTrue(NALTask.TASKS($$("(($1 &&+4 $1) ==>-2 ((--,angX) &&+8 $1))")));
    }

    @Test void validUnbalancedIndepInQuestion() {
        assertTrue(NALTask.TASKS($$(
        "(((y,\\,x) --> $1) <-> ?1)"
        ), QUESTION, true));
    }

    @Disabled
    @Test
    void ValidTaskTerm() {
        assertTrue(NALTask.TASKS($$("believe(x,(believe(x,(--,(cam(9,$1) ==>-78990 (ang,$1))))&&(cam(9,$1) ==>+570 (ang,$1))))")));
    }
}