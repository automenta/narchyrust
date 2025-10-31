package nars.nal.nal7;

import nars.NAL;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.TaskConcept;
import nars.term.util.transform.VariableShift;
import nars.test.TestNAR;
import nars.time.Tense;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.Op.VAR_DEP;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 6/8/15.
 */
class TemporalInductionTest {

    final NAR n = NARS.tmp(7);

    private static int getBeliefCount(NAR n) {
        return Tense.occToDT(n.tasks(true, false, false, false).count());
    }

    /**
     * needs STM
     */
    @Test
    void inductionDiffEventsAtom() {
        testInduction("before", "after", 10);
    }

    @Test
    void inductionDiffEventsCompound() {
        testInduction("x:before", "x:after", 10);
    }

    @Test
    void inductionDiffEventsCompoundNear() {
        testInduction("x:before", "x:after", 3);
    }

    @Test
    void inductionDiffEventsNegPos() {
        testInduction("(--,x:before)", "x:after", 4);
    }

    @Test
    void inductionSameEvents() {
        testInduction("x", "x", 3);
    }

    @Test
    void inductionSameEventsNeg() {
        testInduction("--x", "--x", 10);
    }

    @Test
    void inductionSameEventsInvertPosNeg() {
        testInduction("x", "--x", 10);
    }

    @Test
    void inductionSameEventsInvertNegPos() {
        testInduction("--x", "x", 10);
    }

    /**
     * tests that conj and impl induction results dont have diminished
     * confidence as a result of temporal distance, as
     * would ordinarily happen due to using projected belief truth
     * rather than raw belief truth
     */
    private void testInduction(String a, String b, int dt) {
        assert (dt != 0);

        var cycles = dt * 10;
        var bNeg = b.startsWith("--");
        var t = new TestNAR(n)
                .volMax(9).confMin(0.4f)
                .input(a + ". |")
                .inputAt(dt, b + ". |")
                .mustBelieve(cycles, '(' + a + " &&+" + dt + ' ' + b + ')', 1.00f, 0.81f /*intersectionConf*/, 0)
                .mustBelieve(cycles, '(' + a + " ==>+" + dt + ' ' + (bNeg ? b.substring(2) /* unneg */ : b) + ')',
                        bNeg ? 0 : 1.00f,
                        0.45f /*abductionConf*/, 0);

//        if (!a.equals(b)) {
//            t.mustNotOutput(cycles, '(' + a + " &&-" + dt + ' ' + b + ')', BELIEF, 0.5f, 1f, 0, 1); //wrong direction
//        }

//        if (!(a.contains("--") /*NEG*/ && a.equals(b)))
//            t.mustBelieve(cycles, '(' + b + " ==>-" + dt + ' ' + a + ')', 1.00f, 0.45f /*inductionConf*/, dt);

        t.run();
    }

    @Test
    void testTemporalRevision() throws Narsese.NarseseException {

        n.time.dur(1);

//        final Predicate<Term> isTerm = $("a:b").equals();
//        n.eventTask().on(t -> {
//            NALTask T = (NALTask) t;
//            if (T.range()==1 && isTerm.test(T.term()) && T.conf() < 0.85) {
//                assertTrue(false, "unnecessary echo: " + t);
//            }
//        }, BELIEF);

        n.input("a:b. | %1.0;0.9%");
        n.run(5);
        n.input("a:b. | %0.0;0.9%");
        n.run(5);
        n.input("a:b. | %0.5;0.9%");
        n.run(1);


        var c = (TaskConcept) n.conceptualize("a:b");
        assertNotNull(c);


        var b = c.beliefs();
        b.print();
        assertTrue(b.taskCount() >= 3);


        var x = n.belief(c.term(), 5);
        assertTrue(x.toStringWithoutBudget().startsWith("(b-->a). 5"));
        assertTrue(x.NEGATIVE());


        var y = n.belief(c.term(), 0);
        assertTrue(y.toStringWithoutBudget().startsWith("(b-->a). 0"));
        assertTrue(y.POSITIVE());

    }
//
//    @Test
//    void testQuestionProjection() throws Narsese.NarseseException {
//
//        NAR n = NARS.tmp();
//
//
//
//        n.input("a:b. |");
//
//        n.input("a:b? :/:");
//        n.run(5);
//        n.input("a:b? :/:");
//        n.run(30);
//        n.input("a:b? :/:");
//        n.run(250);
//        n.input("a:b? :/:");
//        n.run(1);
//
//
//
//
//
//    }

    @Test
    void testTemporalRevisionOfTemporalRelation() throws Narsese.NarseseException {


        n.input("(a ==>+0 b). %1.0;0.7%");
        n.input("(a ==>+5 b). %1.0;0.6%");
        n.run(1);


        //TODO


    }

    @Test
    void testInductionStability() throws Narsese.NarseseException {


        n.input("a:b. |");
        n.run(5);
        n.input("c:d. |");

        n.run(200);


        var before = n.memory.size();
//        int numBeliefs = getBeliefCount(nar);


        n.run(60);


        var after = n.memory.size();
        assertEquals(before, after);


    }



    @Disabled @Test
    void conjInduction_with_range_echos_0() {


        var t = new TestNAR(n);
        n.believe($$("a"), 0, 5);
        n.believe($$("b"), 2, 2);
        /*
             a a a a a a
                 b
             0 1 2 3 4 5
        */
        t.mustBelieve(10, "(a &&+2 (b &&+3 a))", 1, 0.81f, 0, 0);
        t.run();
    }

    @Disabled @Test
    void conjInduction_with_range_echos_1() {


        var t = new TestNAR(n);
        n.believe($$("a"), 0, 5);
        n.believe($$("b"), 2, 3);
        /*
             a a a a a a
                 b b
             0 1 2 3 4 5
        */
        t.mustBelieve(10, "(a &&+2 (b &&+2 a))", 1, 0.81f, 0, 1);
        t.run();
    }
    @Disabled @Test
    void conjInduction_with_range_echos_1shift() {
        var s = 1;
        var t = new TestNAR(n);
        n.believe($$("a"), 0+s, 5);
        n.believe($$("b"), 2+s, 3);
        t.mustBelieve(10, "(a &&+2 (b &&+2 a))", 1, 0.81f, 0+s, 1);
        t.run();
    }

    @Test
    void conjSeqInductionDontFactor() {
        var t = new TestNAR(n);
        n.believe($$("(a &&+1 b)"), 0);
        n.believe($$("c"), 0);
        t.mustBelieve(16, "((a&&c) &&+1 b)", 1, 0.81f, 0);
        t.mustNotBelieve(16, "((a &&+1 b)&&c)");
        t.mustBelieve(16, "((a &&+1 b) ==>-1 c)", 1, 0.45f, 0);
        t.mustBelieve(16, "(c==>(a &&+1 b))", 1, 0.45f, 0);
        t.run();
    }

//    static class PriMeter extends DoubleMeter {
//
//        private final FloatSupplier getter;
//
//        PriMeter(NAR n, String id) {
//            super("pri(" + id + ")", true);
//            Term term = $.$$(id);
//            this.getter = ()->{
//                Concept cc = n.concept(term);
//                if (cc == null)
//                    return 0;
//                return n.concepts.pri(cc, 0);
//            };
//        }
//
//        @Override
//        public DoubleMeter reset() {
//            set(getter.asFloat());
//            return this;
//        }
//
//
//    }
//    /**
//     * higher-level rules learned from events, especially repeatd
//     * events, "should" ultimately accumulate a higher priority than
//     * the events themselves.
//     */
//    @Test
//    void testPriorityOfInductedRulesVsEventsThatItLearnedFrom() {
//        NAR n = NARS.tmp();
//
//        n.beliefPriDefault.set(0.1f);
//
//
//
//        TemporalMetrics m = new TemporalMetrics(1024);
//        n.onCycle(()->m.update(n.time()));
//
//        m.add(new PriMeter(n,"(0)"));
//        m.add(new PriMeter(n,"(1)"));
//        m.add(new PriMeter(n,"(2)"));
//        m.add(new PriMeter(n,"((0) && (1))"));
//        m.add(new PriMeter(n,"((0) ==> (1))"));
//        m.add(new PriMeter(n,"((1) && (2))"));
//        m.add(new PriMeter(n,"((1) ==> (2))"));
//
//
//
//        int loops = 32, eventsPerLoop = 3, delayBetweenEvents = 2;
//        for (int i = 0; i < loops; i++) {
//            for (int j = 0; j < eventsPerLoop; j++) {
//                n.believe($.p(j), Tense.Present);
//                n.run(delayBetweenEvents);
//            }
//        }
//
//
//        m.printCSV4(System.out);
//    }

    @Test void TemporalInduction_Conj_Disj() {
        var cycles = 30;
        var t = new TestNAR(n);
        t.volMax(6);
        t.believe("x");
        t.believe("y");
        t.mustBelieve(cycles, "(x&&y)", 1, 0.81f);
        //t.mustBelieve(cycles, "(x||y)", 1f, 0.81f);
        t.mustBelieve(cycles, "(x ==> y)", 1, 0.45f);
        t.mustBelieve(cycles, "(y ==> x)", 1, 0.45f);
        t.run();
    }

    @Test void TemporalInduction_Conj_Impl_VarShift_pre() {
        //new VariableShift(varCommonBits).shift(aa).apply(b.term())
        assertEq("y(#2)",
                new VariableShift(VAR_DEP.bit).shift($$("x(#1)")).apply($$("y(#1)").term()));
    }

    @Test void TemporalInduction_Conj_Impl_VarShift() {
        final var cycles = 150;
        var t = new TestNAR(n);
        t.confMin(0.43f);
        t.volMax(11);
        t.believe("x(#1)");
        t.believe("y(#1)");
        //t.mustBelieve(cycles, "((#1)-->(x&&y))", 1f, 0.81f);
        t.mustBelieve(cycles, "(x(#1)&&y(#2))", 1, 0.81f);
//        t.mustBelieve(cycles, "(--x(#1) && y(#2))", 0, 0.81f);
//        t.mustBelieve(cycles, "(x(#1) && --y(#2))", 0, 0.81f);
        //t.mustBelieve(cycles, "--(x(#1)||y(#2))", 0, 0.81f);
        t.mustBelieve(cycles, "(x(#1) ==> y(#2))", 1, 0.45f);
        t.mustBelieve(cycles, "(y(#1) ==> x(#2))", 1, 0.45f);
        t.run();
    }
    @Test void TemporalInduction_PosNeg_Impl() {
        var cycles = 50;
        var t = new TestNAR(n);
        t.confMin(0.4f);
        t.volMax(9);
        t.believe("x");
        t.believe("--y");
        t.mustBelieve(cycles, "(x ==> --y)", 1f, 0.45f);
        t.mustBelieve(cycles, "(--y ==> x)", 1f, 0.45f);
        t.mustNotOutput(cycles, "(--x ==> y)", BELIEF, 0.0f, 1, 0, 1);
        t.mustNotOutput(cycles, "(  x ==> y)", BELIEF, 0.5f, 1, 0, 1);
        t.mustNotOutput(cycles, "(  y ==> x)", BELIEF, 0.0f, 1, 0, 1);
        t.run();
    }

    @Test void TemporalInduction_Temporal_Impl_Bidirectional() {


        var cycles = 10;
        var t = new TestNAR(n);
        t.confMin(0.39f);
        t.volMax(3);
        t.inputAt(0, "a. | %1.0;0.9%");
        t.inputAt(1, "b. | %0.9;0.9%");
        t.mustBelieve(cycles, "(a ==>+1 b)", 0.9f, 0.45f, 0);

        if (NAL.temporal.TEMPORAL_INDUCTION_IMPL_BIDI) //HACK
            t.mustBelieve(cycles, "(b ==>-1 a)", 1.0f, 0.4f, 1);
        else {
            //TODO t.mustNotBelieve..
        }

        t.run();
    }
}