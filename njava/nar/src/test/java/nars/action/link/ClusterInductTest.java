package nars.action.link;

import nars.*;
import nars.action.resolve.Answerer;
import nars.action.resolve.TaskResolve;
import nars.deriver.impl.TaskBagDeriver;
import nars.deriver.reaction.Reactions;
import nars.focus.time.NonEternalTiming;
import nars.test.TestNAR;
import nars.time.Tense;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClusterInductTest {

    private final NAR n = NARS.shell();

    @Test
    void SameTruthSameTime() throws Narsese.NarseseException {
        int ccap = 3;
        conjCluster(n, ccap*2, BELIEF);

        for (int i = 0; i < ccap*2; i++)
            n.believe($.atomic("x" + i), Tense.Present);
        n.run(64);

        Concept c = n.concept($.$(
                "(&&,x0,x1)"
        ));
        assertNotNull(c);
        TaskTable b = c.beliefs();
//        assertEquals(1, b.taskCount());

        assertStamp("[1, 2]", b);
    }

    @Test
    void Neg() throws Narsese.NarseseException {
        int ccap = 2;
        conjCluster(n, ccap*2, BELIEF);

        for (int i = 0; i < ccap; i++)
            n.believe($.atomic("x" + i).neg(), 1);

        for (int i = 0; i < ccap; i++) //HACK these extra tasks shouldnt be necessary.
            n.believe($.atomic("x" + (i+ccap)).neg(), 10);

        n.run(32);

        Concept c = n.concept($.$("(&&,--x0,--x1)"));
        assertNotNull(c);
        assertStamp("[1, 2]", c.beliefs());

        assertNotNull(n.concept($.$("(&&,--x2,--x3)")));
        assertNotNull(n.concept($.$("((--,x0)==>x1)")));
        assertNotNull(n.concept($.$("((--,x1)==>x0)")));
        //TODO others
    }
    @Test
    void VarSeparation() throws Narsese.NarseseException {
        conjCluster(n, 3, BELIEF);

        n.believe($.p("x",$.varDep(1)), 1);
        n.believe($.p("y",$.varDep(1)), 1);
        n.believe($.p("z",$.varDep(1)), 1);

        n.run(32);

        Concept c = n.concept($.$("(&&,(x,#1),(z,#2))"));
        assertNotNull(c);
        assertStamp("[1, 3]", c.beliefs());

//        assertNotNull(n.concept($.$("(&&,--x2,--x3)")));
//        assertNotNull(n.concept($.$("((--,x0)==>x1)")));
//        assertNotNull(n.concept($.$("((--,x1)==>x0)")));
        //TODO others
    }
    private static ClusterInduct conjCluster(NAR n, int ccap, byte punc) {
        return conjCluster(n, 2, 2, ccap, punc);
    }

    private static ClusterInduct conjCluster(NAR n, int centroids, int condMax, int ccap, byte punc) {
        ClusterInduct c = new ClusterInduct(punc, centroids, ccap, NALTask::isInput);
        c.condMax.set(condMax);
		Deriver e = new TaskBagDeriver(new Reactions().addAll(
            c, new TaskResolve(new NonEternalTiming(), Answerer.AnyTaskResolver)).compile(n)
        , n);
        n.onCycle(()-> e.next(n.main()));
        return c;
    }

    private static void assertStamp(String stamp, TaskTable b) {
        NALTask the = b.taskStream().findFirst().get();
        float p = the.pri();
        assertEquals(p, p);
        assertEquals(stamp, Arrays.toString(the.stamp()));
     }

    @Test void DimensionalDistance1() {

        n.time.dur(4);

        conjCluster(n, 8, BELIEF);

        n.inputAt(1, "$1.0 x. |");
        n.inputAt(2, "$1.0 y. |");
        n.inputAt(1, "$0.1 z. |");
        n.inputAt(3, "$0.1 w. |");


        n.run(2);
        //TODO
//        assertEquals(1, n.concept($.$("")).beliefs().size());

    }
    @Test void ConjGoal() {
        int cycles = 32;

        TestNAR t = new TestNAR(n);

        t.volMax(7);
        n.time.dur(1);

        ClusterInduct c = conjCluster(n, 8, GOAL);

        n.inputAt(1, "x! |");
        n.inputAt(2, "y! |");

        n.inputAt(6, "a! |");
        n.inputAt(7, "b! |");

        t.mustGoal(cycles, "(x &&+1 y)", 1, 0.9f*0.9f, (s,e)->s==1 && e==1);
        t.mustGoal(cycles, "(a &&+1 b)", 1, 0.9f*0.9f, (s,e)->s==6 && e==6);
        t.run();
    }
    
}