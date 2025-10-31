package nars.task;

import com.google.common.collect.Lists;
import jcog.math.LongInterval;
import nars.*;
import nars.concept.TaskConcept;
import nars.table.BeliefTables;
import nars.table.eternal.EternalTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.util.IntermpolationTest;
import nars.test.analyze.BeliefAnalysis;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.truth.proj.IntegralTruthProjection;
import nars.truth.proj.MutableTruthProjection;
import nars.truth.proj.TruthProjection;
import nars.truth.util.Revision;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.*;

import static nars.$.$;
import static nars.NAL.STAMP_CAPACITY;
import static nars.Op.BELIEF;
import static nars.Op.ETERNAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 3/18/16.
 */
public class RevisionTest {

    public static final Term x = $.atomic("x");
    private final NAR n = NARS.shell();

    private static TaskBuilder t(float freq, float conf, long occ) throws Narsese.NarseseException {
        return new TaskBuilder($("a:b"), BELIEF, $.t(freq, conf)).time(0, occ, occ);
    }

    static TaskBuilder t(float freq, float conf, long start, long end) throws Narsese.NarseseException {
        return new TaskBuilder($("a:b"), BELIEF, $.t(freq, conf)).time(0, start, end);
    }

    static NAR newNAR(int fixedNumBeliefs) {
        //TODO
//
//        ConceptAllocator cb = new ConceptAllocator(fixedNumBeliefs, fixedNumBeliefs, 1);
//        cb.beliefsMaxEte = (fixedNumBeliefs);
//        cb.beliefsMaxTemp = (fixedNumBeliefs);
//        cb.beliefsMinTemp = (fixedNumBeliefs);
//        cb.goalsMaxEte = (fixedNumBeliefs);
//        cb.goalsMaxTemp = (fixedNumBeliefs);
//        cb.goalsMinTemp = (fixedNumBeliefs);

        return new NARS()/*.concepts(new DefaultConceptBuilder(cb))*/.get();

    }

    private static void p(NALTask aa) {
        System.out.println(aa.toString(true));
        System.out.println("\tevi=" + aa.evi());
    }

    /**
     * assumes none of the tasks are eternal
     * <p>
     * warning: output task will have zero priority and input tasks will not be affected
     * this is so a merge construction can be attempted without actually being budgeted
     * <p>
     * also cause merge is deferred in the same way
     *
     * @return
     */
    @Deprecated
    static Pair<NALTask, TruthProjection> merge(NALTask[] tasks, int numTasks, int minComponents, boolean dither, NAR n) {

        assert (numTasks >= minComponents);

        //quick 2-ary stamp pre-filter
        var p = new IntegralTruthProjection(LongInterval.Timeless)
                .timeRes(n.timeRes())
                .add(numTasks, tasks)
                .sizeMin(minComponents);
//        boolean ok = p.commit();
//        assertTrue(ok);

        var eviMin = NAL.belief.REVISION_MIN_EVI_FILTER ? n.eviMin() : NAL.truth.EVI_MIN;
        var truth = p.eviMin(eviMin).truth();
        Pair<NALTask, TruthProjection> result = truth == null ? null : pair(
                NALTask.task(p.term, p.punc(), truth, p, stampSample(p, STAMP_CAPACITY)),
                p);


        return numTasks == 2 && Stamp.overlap(tasks[0], tasks[1]) ?
                null : result;

    }

    /**
     * computes a stamp by sampling from components in proportion to their evidence contribution
     */
    @Deprecated private static long[] stampSample(MutableTruthProjection p, int capacity) {

        //removeNulls(); //HACK

        var n = p.size();

        @Nullable long[] s0 = p.stamp(0);
        switch (n) {
            case 0 -> throw new NullPointerException();
            case 1 -> {
                assert (s0.length <= capacity);
                return s0;
            }
            default -> {
                var maxPossibleSize = 0;
                for (var i = 0; i < n; i++) maxPossibleSize += p.stamp(i).length;
                if (maxPossibleSize <= capacity) {
                    var l = new LongArrayList(maxPossibleSize);
                    for (var i = 0; i < n; i++) {
                        for (var s : p.stamp(i)) {
                            var a = l.binarySearch(s);
                            if (a < 0)
                                l.addAtIndex(-a - 1, s);
                        }
                    }
                    return l.toArray(); //should be already sorted
                } else {
                    //sample n-ary
                    // TODO weight contribution by evidence
                    return Stamp.zip(capacity, p::stamp, p, n);
                }
            }
        }
    }


    @Test
    void testRevisionEquivalence() throws Narsese.NarseseException {
        var a = t(1f, 0.5f, 0);
        a.evidence(0);
        var b = t(1f, 0.5f, 0);
        b.evidence(1);


        var x = Revision.revise(a.truth(), b.truth());
        var y = new IntegralTruthProjection(0, 0)
                .add(Lists.newArrayList(a.apply(n), b.apply(n))).truth();
        //assertEquals(x, y);
        assertEquals(x.toString(), y.toString());
    }

    @Test
    void testCoincidentTasks() throws Narsese.NarseseException {
        var t01 = merge(t(1, 0.9f, 0, 0).apply(n), t(1, 0.9f, 0, 0).apply(n), n);
        assertNotNull(t01);
        assertEquals("(b-->a). 0 %1.0;.95%", t01.toStringWithoutBudget());
        assertEquals("[1, 2]", Arrays.toString(t01.stamp()));
    }

    @Test
    void partiallyCoincidentTasks() throws Narsese.NarseseException {
        var t01 = merge(t(1, 0.9f, 0, 0).apply(n), t(1, 0.9f, 0, 1).apply(n), n);
        assertNotNull(t01);
        assertEquals("(b-->a). 0⋈1 %1.0;.93%", t01.toStringWithoutBudget());
        assertEquals("[1, 2]", Arrays.toString(t01.stamp()));
    }

    @Test
    void adjacentTasks() throws Narsese.NarseseException {
        var t01 = merge(
                t(1, 0.9f, 0, 0).apply(n),
                t(1, 0.9f, 1, 1).apply(n), n);
        assertNotNull(t01);
        assertEquals("(b-->a). 0⋈1 %1.0;.90%", t01.toStringWithoutBudget());
        assertEquals("[1, 2]", Arrays.toString(t01.stamp()));
    }

    /**
     * test solutions are optimal in cases where one or more tasks have
     * overlapping evidence.  including cases where the top ranked merge
     * result is best excluded.
     */
    @Test
    void testOverlapConflict() throws Narsese.NarseseException {
        var rr = merge(new NALTask[]{
				t(0, 0.71f, 0, 0).evidence(1, 2).apply(n),
				t(1, 0.7f, 0, 0).evidence(1).apply(n),
				t(1, 0.7f, 0, 0).evidence(2).apply(n)}, 3, 2, false, n
		);
        assertNotNull(rr);
        var t = rr.getOne();

        assertNotNull(t);
        assertEquals("(b-->a). 0 %1.0;.82%", t.toStringWithoutBudget());
        assertEquals("[1, 2]", Arrays.toString(t.stamp()));

    }

    @Test
    void testNonAdjacentTasks() throws Narsese.NarseseException {
//        if (Param.REVISION_ALLOW_DILUTE_UNION) { //HACK requires truth dilution to be enabled, which ideally will be controlled on a per-revision basis. not statically
        var n = NARS.shell();

        var t01 = t(1, 0.9f, 0, 1).apply(n);
        var t02 = t(1, 0.9f, 0, 2).apply(n);
        var t03 = t(1, 0.9f, 0, 3).apply(n);
        var t35 = t(1, 0.9f, 3, 5).apply(n);
        var t45 = t(1, 0.9f, 4, 5).apply(n);
        var t100_105 = t(1, 0.9f, 100, 105).apply(n);

        //evidence density
        var a = merge(t01, t45, n);
        var b = merge(t02, t45, n);
        var c = merge(t03, t45, n);
        assertNotNull(a);
        assertNotNull(b);
        assertTrue(a.evi() < b.evi());
        assertNotNull(c);
        assertTrue(b.evi() < c.evi());

        assertTrue(merge(t03, t35, n).toStringWithoutBudget().startsWith("(b-->a). 0⋈5 %1.0;.9"));
        assertTrue(merge(t02, t35, n).toStringWithoutBudget().startsWith("(b-->a). 0⋈5 %1.0;.9"));

        //assertEquals("((b-->a) &&+100 (b-->a)). 0⋈2 %1.0;.81%", merge(t02, t100_105, n).toStringWithoutBudget());

//        }

    }

    private static NALTask merge(NALTask t01, NALTask t45, NAR n) {
        return merge(new NALTask[]{t01, t45}, 2, 2, false, n).getOne();
    }


//
//    static void print(@NotNull List<Task> l, int start, int end) {
//
//        System.out.println("INPUT");
//        for (Task t : l) {
//            System.out.println(t);
//        }
//
//        System.out.println();
//
//        System.out.println("TRUTHPOLATION");
//        for (long d = start; d < end; d++) {
//            Truth a1 = new FocusingLinearTruthPolation(d, d, 1).addAt(l).truth();
//            System.out.println(d + ": " + a1);
//        }
//    }

    @Test
    void testRevisionInequivalenceDueToTemporalSeparation() throws Narsese.NarseseException {

        var A = t(1f, 0.5f, +1).evidence(1).apply(n);
        var B = t(0f, 0.5f, -1).evidence(2).apply(n);
        MutableTruthProjection ab = new IntegralTruthProjection(-1,+1);
        ab.dur(1);
        ab.add(A, B);
        var pt = ab.truth(0,0);
        assertNotNull(pt);
        assertEquals(0.5f, pt.freq(), 0.01f);

        @Nullable Truth rt = Revision.revise(A.truth(), B.truth());

        assertEquals(pt.freq(), rt.freq(), 0.01f);
        var ptConf = (float) pt.conf();
        assertTrue(ptConf < (float) rt.conf());

    }

    @Test
    void testRevisionEquivalence2Instant() throws Narsese.NarseseException {
        var a = t(1f, 0.5f, 0);
        var b = t(0f, 0.5f, 0);
        assertEquals(
                Revision.revise(a.truth(), b.truth()).toString(),
                new IntegralTruthProjection(0, 0).add(Lists.newArrayList(a.apply(n), b.apply(n))).truth().toString()
        );
    }

    @Test
    void testPolation1() throws Narsese.NarseseException {

        var a = t(1f, 0.9f, 3).apply(n);
        var b = t(0f, 0.9f, 6).apply(n);

        assertEquals(0.5f, a.pri());
        assertEquals(0.5f, b.pri());

        //single projection
        {
            final var steps = 10;
            final float dur = 1;
            var pri = new float[steps];
            var evi = new double[steps];
            for (var i = 0; i < steps; i++) {
                var tt = new IntegralTruthProjection(i, i);
                tt
                        .dur(dur)
                        .add(new NALTask[] { a });
                var ti = tt
                        .task();
                var p = ti.pri();
                assertTrue(p > 0 && p <= 0.5);
                pri[i] = p;
                evi[i] = ti.evi();
                System.out.println(i + " " + ti);
            }
            assertTrue(pri[3] > pri[0], "a decrease in priority as a result of evidence loss");
            assertTrue(pri[3] > pri[6], "a decrease in priority as a result of evidence loss");
        }
        {
            final var steps = 10;
            final float dur = 1;
            var pri = new float[steps];
            var evi = new double[steps];
            for (var i = 0; i < steps; i++) {
                var tt = new IntegralTruthProjection(i, i);
                tt
                        .dur(dur)
                        .add(a, b);
                var ti = tt.task();
                var p = ti.pri();
                assertTrue(p > 0 && p <= 0.5);
                pri[i] = p;
                evi[i] = ti.evi();
                System.out.println(i + " " + ti);
            }
            assertEquals(pri[6], pri[3]);
            assertTrue(pri[3] > pri[0], "a decrease in priority as a result of evidence loss");
        }


        var ab2 = new IntegralTruthProjection(3, 3).add(Lists.newArrayList(a, b)).truth();
        assertTrue(ab2.freq() > 0.75f);
		assertTrue((float) ab2.conf() >= 0.75f);

        {
            TruthProjection p5 = new IntegralTruthProjection(6, 6).add(List.of(a, b));
            var ab5 = p5.truth();
            assertTrue(ab5.freq() < 0.4f);
			assertTrue((float) ab5.conf() >= 0.5f);
        }
    }

    @Test
    void testRevisionEquivalence4() throws Narsese.NarseseException {
        var a = t(0f, 0.1f, 3).evidence(1).apply(n);
        var b = t(0f, 0.1f, 4).evidence(2).apply(n);
        var c = t(1f, 0.1f, 5).evidence(3).apply(n);
        var d = t(0f, 0.1f, 6).evidence(4).apply(n);
        var e = t(0f, 0.1f, 7).evidence(5).apply(n);

        for (var i = 0; i < 15; i++) {
            System.out.println(i + " " + new IntegralTruthProjection(i, i).add(Lists.newArrayList(a, b, c, d, e)).truth());
        }

    }

    @Test
    void testTemporalProjectionInterpolation() throws Narsese.NarseseException {


        var maxBeliefs = 12;
        var n = newNAR(maxBeliefs);


        var b = new BeliefAnalysis(n, "<a-->b>");
        b.believe(0.5f, 1.0f, 0.85f, 5);
        b.believe(0.5f, 0.0f, 0.85f, 10);
        b.believe(0.5f, 1.0f, 0.85f, 15);
        b.run(1);

        assertTrue(3 <= b.size(true));

        var period = 1;
        var loops = 20;

        Set<Task> tops = new HashSet();
        for (var i = 0; i < loops; i++) {


            b.run(period);

            var now = b.nar.time();

            Task tt = n.belief(b.concept().term(), now);
            tops.add(tt);

            System.out.println(now + " " + tt);

        }

        assertTrue(3 <= tops.size(), "all beliefs covered");

        b.print();

    }

    @Test
    void testTemporalProjectionConfidenceAccumulation2_1() {
        testConfidenceAccumulation(2, 1f, 0.1f);
    }

    @Test
    void testTemporalProjectionConfidenceAccumulation2_5() {
        testConfidenceAccumulation(2, 1f, 0.5f);
    }

    @Test
    void testTemporalProjectionConfidenceAccumulation2_9() {

        testConfidenceAccumulation(2, 1f, 0.9f);
        testConfidenceAccumulation(2, 0.5f, 0.9f);
        testConfidenceAccumulation(2, 0f, 0.9f);
    }

    @Test
    void testTemporalProjectionConfidenceAccumulation3_1_pos() {
        testConfidenceAccumulation(3, 1f, 0.1f);
    }

    @Test
    void testTemporalProjectionConfidenceAccumulation3_1_neg() {
        testConfidenceAccumulation(3, 0f, 0.1f);
    }

    @Test
    void testTemporalProjectionConfidenceAccumulation3_1_mid() {
        testConfidenceAccumulation(3, 0.5f, 0.1f);
    }

    @Test
    void testTemporalProjectionConfidenceAccumulation3_5() {
        testConfidenceAccumulation(3, 1f, 0.5f);
    }

    @Test
    void testTemporalProjectionConfidenceAccumulation3_9() {
        testConfidenceAccumulation(3, 1f, 0.9f);
    }

    private static void testConfidenceAccumulation(int repeats, float freq, float inConf) {
        var maxBeliefs = repeats * 4;

        var n = newNAR(maxBeliefs);


        var outConf = (float) TruthFunctions.e2c(TruthFunctions.c2e(inConf) * repeats);

        BeliefAnalysis b = null;
        try {
            b = new BeliefAnalysis(n, "<a-->b>");
        } catch (Narsese.NarseseException e) {
            fail(e);
        }
        long at = 5;
        for (var i = 0; i < repeats; i++) {
            b.believe(0.5f, freq, inConf, at);
        }

        b.run(1);

        b.print();
        assertTrue(repeats <= b.size(true));

        @Nullable Truth result = n.beliefTruth(b, at);
        assertEquals(freq, result.freq(), 0.25f);
		assertEquals(outConf, (float) result.conf(), 0.25f);
    }

    @Test
    void testTemporalRevection() throws Narsese.NarseseException {


        var maxBeliefs = 4;
        var n = newNAR(maxBeliefs);


        var b = new BeliefAnalysis(n, "<a-->b>");


        b.believe(0.5f, 0.0f, 0.85f, 5);
        n.run();
        b.believe(0.5f, 0.95f, 0.85f, 10);
        n.run();
        b.believe(0.5f, 1.0f, 0.85f, 11);
        n.run();

        b.print();
        assertTrue(3 <= b.size(true));


        b.believe(0.5f, 1.0f, 0.99f, 12);


        n.run(3);
        b.print();

        assertEquals(4, b.size(true));

        b.print();

//        assertEquals(5, b.wave().start());
//        assertEquals(12, b.wave().end());
//
//        assertEquals(5, b.wave().start());
//        assertEquals(11, b.wave().end());

    }

    @Test
    void testSequenceIntermpolation1() {

        var a = $.$$c("(((--,(dx-->noid)) &&+4 ((--,(by-->noid))&&(happy-->noid))) &&+11 (bx-->noid))");
        var b = $.$$c("(((bx-->noid) &&+7 (--,(dx-->noid))) &&+4 ((--,(by-->noid))&&(happy-->noid)))");
//        Term ar = a.root();
//        Term br = b.root();
//        assertEquals(ar, br);
//        assertEquals(a.concept(), b.concept());

        TreeSet<Term> outcomes = new TreeSet();

        var misses = 0;
        for (var i = 0; i < 10; i++) {
            var c = IntermpolationTest.intermpolate(a, b, 0.5f, n);
            if (c != null) {
                outcomes.add(c);
            } else
                misses++;
        }

        for (var outcome : outcomes) {
            System.out.println(outcome);
        }
        assertFalse(outcomes.isEmpty());
    }

    @Test
    void testSequenceIntermpolationInBeliefTable() throws Narsese.NarseseException {

        n.complexMax.set(17);

        var a = $("(((--,(dx-->noid)) &&+4 ((--,(by-->noid))&&(happy-->noid))) &&+11 (bx-->noid))");
        var b = $("(((bx-->noid) &&+7 (--,(dx-->noid))) &&+4 ((--,(by-->noid))&&(happy-->noid)))");
//        assertEquals(a.root(), b.root());
//        assertEquals(a.concept(), b.concept());


        var out = new StringBuilder();
        n.main().eventTask.on(t -> out.append(t).append('\n'));

        Task at = n.believe(a, Tense.Present, 1f, n.confDefault(BELIEF));
        n.believe(b, Tense.Present);
        ((BeliefTables) n.concept(a).beliefs()).tableFirst(EternalTable.class).taskCapacity(1);
        ((BeliefTables) n.concept(a).beliefs()).tableFirst(TemporalBeliefTable.class).taskCapacity(1);
        n.input(at);


        n.run(1);

        /*
        $.50 (((--,(dx-->noid)) &&+4 ((--,(by-->noid))&&(happy-->noid))) &&+11 (bx-->noid)). 0⋈15 %1.0;.90% {0: 1}
        $.50 (((bx-->noid) &&+7 (--,(dx-->noid))) &&+4 ((--,(by-->noid))&&(happy-->noid))). 0⋈11 %1.0;.90% {0: 2}
          >-- should not be activated: $.50 (((--,(dx-->noid)) &&+4 ((--,(by-->noid))&&(happy-->noid))) &&+11 (bx-->noid)). 0⋈15 %1.0;.90% {0: 1}
        $.50 (((--,(dx-->noid)) &&+4 ((--,(by-->noid))&&(happy-->noid))) &&+7 ((--,(by-->noid))&&(happy-->noid))). 0⋈15 %1.0;.95% {0: 1;2}
        $.26 ((--,(dx-->noid)) &&+4 ((--,(by-->noid))&&(happy-->noid))). 0⋈4 %1.0;.81% {1: 1;;}
        $.31 ((--,(dx-->noid)) &&+15 (bx-->noid)). 0⋈15 %1.0;.81% {1: 1;;}
         */

    }

    @Test
    void eternalBeliefRevision() {
        testEternalRevision(1, true);
    }

    @Test
    void eternalGoalRevision() {
        testEternalRevision(1, false);
    }

    private static void testEternalRevision(int delay1, boolean beliefOrGoal) {


        var n = newNAR(6);


        var b = new BeliefAnalysis(n, x)
                .input(beliefOrGoal, 1, 0.9f).run(1);

        assertEquals(1, b.size(beliefOrGoal));

        b.input(beliefOrGoal, 0.0f, 0.9f).run(1);

        b.run(delay1);


        b.table(beliefOrGoal).print();
        assertEquals(3, b.size(beliefOrGoal));

        n.run(delay1);

        assertEquals(3, b.size(beliefOrGoal), "no additional revisions");


    }

    @Test
    void testTruthOscillation() {

        var n = NARS.shell();


        var b = new BeliefAnalysis(n, x);


        b.believe(1.0f, 0.9f, Tense.Present);
        b.run(1);


        b.run(1);


        b.believe(0.0f, 0.9f, Tense.Present);
        b.run(1);


        b.run(1);


        b.print();
        assertEquals(2, b.size(true));

        var offCycles = 2;
        b.believe(1.0f, 0.9f, Tense.Present).run(offCycles)
                .believe(0.0f, 0.9f, Tense.Present);

        for (var i = 0; i < 16; i++) {


            n.run(1);

        }


    }

    @Test
    void testTruthOscillation2() {


        var maxBeliefs = 16;
        var n = newNAR(maxBeliefs);


        var b = new BeliefAnalysis(n, x);


        var period = 8;
        var loops = 4;
        for (var i = 0; i < loops; i++) {
            b.believe(1.0f, 0.9f, Tense.Present);


            b.run(period);


            b.believe(0.0f, 0.9f, Tense.Present);

            b.run(period);

            b.print();
        }

        b.run(period);

        b.print();


    /*
    Beliefs[@72] 16/16
    <a --> b>. %0.27;0.98% [1, 2, 3, 4, 6] [Revision]
    <a --> b>. %0.38;0.98% [1, 2, 3, 4, 6, 7] [Revision]
    <a --> b>. %0.38;0.98% [1, 2, 3, 4, 5, 6] [Revision]
    <a --> b>. %0.23;0.98% [1, 2, 3, 4, 6, 8] [Revision]
    <a --> b>. %0.35;0.97% [1, 2, 3, 4] [Revision]
    <a --> b>. %0.52;0.95% [1, 2, 3] [Revision]
    <a --> b>. 56+0 %0.00;0.90% [8] [Input]
    <a --> b>. 48+0 %1.00;0.90% [7] [Input]
    <a --> b>. 40+0 %0.00;0.90% [6] [Input]
    <a --> b>. 32+0 %1.00;0.90% [5] [Input]
    <a --> b>. 24+0 %0.00;0.90% [4] [Input]
    <a --> b>. 16+0 %1.00;0.90% [3] [Input]
    <a --> b>. 8+0 %0.00;0.90% [2] [Input]
    <a --> b>. 0+0 %1.00;0.90% [1] [Input]
    <a --> b>. %0.09;0.91% [1, 2] [Revision]
    <a --> b>. 28-20 %0.00;0.18% [1, 2, 3] [((%1, <%1 </> %2>, shift_occurrence_forward(%2, "=/>")), (%2, (<Analogy --> Truth>, <Strong --> Desire>, <ForAllSame --> Order>)))]
     */





    /*for (int i = 0; i < 16; i++) {
        b.printEnergy();
        b.print();
        n.frame(1);
    }*/


    }

    @Test
    void testRevision3Eternals() throws Narsese.NarseseException {
        var n = newNAR(6);

        n.input("a. %1.0;0.5%",
                "a. %0.0;0.5%",
                "a. %0.1;0.5%");
//        n.run(1);
        TaskTable taskTable = n.conceptualize("a").beliefs();
        var t = n.answer(taskTable, ETERNAL, ETERNAL, null, null, 0, NAL.answer.ANSWER_CAPACITY);
        assertEquals(0.37f, t.freq(), 0.02f);
		assertEquals(0.75f, (float) t.conf(), 0.02f);
    }

    @Test
    void testRevision2EternalImpl() throws Narsese.NarseseException {
        var n = newNAR(3)
                .input("(x ==> y). %1.0;0.9%",
                        "(x ==> y). %0.0;0.9%");

        n.run(1);

        var c = (TaskConcept) n.conceptualize("(x ==> y)");
        //c.print();
        var t = n.answer(c.term(), BELIEF, ETERNAL);
        assertEquals(0.5f, t.freq(), 0.01f);
		assertEquals(0.947f, (float) t.conf(), 0.01f);
    }

//    /**
//     * test that budget is conserved during a revision between
//     * the input tasks and the result
//     */
//    @Test
//    void testRevisionBudgeting() {
//        NAR n = newNAR(6);
//
//        BeliefAnalysis b = new BeliefAnalysis(n, x);
//
//        assertEquals(0, b.priSum(), 0.01f);
//
//        b.believe(1.0f, 0.5f).run(1);
//
//        Bag<?, TaskLink> tasklinks = b.concept().tasklinks();
//
//        assertEquals(0.5f, b.beliefs().match(ETERNAL, null, n).truth().conf(), 0.01f);
//
//        System.out.println("--------");
//
//        float linksBeforeRevisionLink = tasklinks.priSum();
//
//        b.believe(0.0f, 0.5f).run(1);
//
//        System.out.println("--------");
//
//        b.run(1);
//        tasklinks.commit();
//
//        System.out.println("--------");
//
//        System.out.println("Beliefs: ");
//        b.print();
//        System.out.println("\tSum Priority: " + b.priSum());
//
//
//        float beliefAfter2;
//        assertEquals(1.0f, beliefAfter2 = b.priSum(), 0.1f /* large delta to allow for forgetting */);
//
//
//        assertEquals(0.71f, b.beliefs().match(ETERNAL, null, n).truth().conf(), 0.06f);
//
//        b.print();
//
//
//        assertEquals(3, b.size(true));
//
//
//        assertEquals(beliefAfter2, b.priSum(), 0.01f);
//
//
//    }

    @Test
    void testRevision2TemporalImpl() throws Narsese.NarseseException {
        var n = newNAR(3)
                .input("(x ==> y). | %1.0;0.9%",
                       "(x ==> y). | %0.0;0.9%");

        n.run(1);

        var c = n.concept($("(x ==> y)"));
        assertEquals(2, c.beliefs().taskCount());

        var tt = n.belief($("(x ==> y)"), 0);
        assertNotNull(tt);
        var t = tt.truth();
        assertEquals(0.5f, t.freq(), 0.01f);
		assertEquals(0.947f, (float) t.conf(), 0.01f);
    }

    @Test
    void testRevision2TemporalImpl_Different_Times() throws Narsese.NarseseException {
        var n = newNAR(3);
        n.input("(x ==> y). | %1.0;0.9%");
        n.inputAt(5, "(x ==> y). | %0.0;0.9%");

        n.run(6);

        var template = $("(x ==> y)");
        {
            var tt = n.belief(template, 0);
            assertEquals(0, tt.start()); assertEquals(0, tt.end());
            var t = tt.truth();
            assertEquals(1, t.freq(), 0.01f);
            assertEquals(0.9f,(float) t.conf(), 0.01f);
        }

        {
            var tt = n.belief(template, 0, 5);
            var t = tt.truth();
            assertEquals(0, tt.start()); assertEquals(5, tt.end());
            assertEquals(0.5f, t.freq(), 0.01f);
            assertTrue(0.90 > (float) t.conf());
        }

//        {
//            NALTask tt = n.belief(template, -3, -1);
//            Truth t = tt.truth();
//            assertEquals(0, tt.start()); assertEquals(0, tt.end());
//            assertEquals(1, t.freq(), 0.01f);
//            assertEquals(0.90, (float) t.conf(), 0.01f);
//        }

    }

    @Test
    public void testMergeTruthDilution() {
        //presence or increase of empty space in the union of between merged tasks reduces truth proportionally


        var a = n.believe(x, 1, 1f);

        assertTrue(a.eviMean(1, 1, 0, 0) > a.eviMean(1, 2, 0, 0));
        assertEquals(0, a.eviMean(2, 2, 0, 0));

        var a2 = n.believe(x, 1, 1f);
        var b = n.believe(x, 2, 1f);
        var c = n.believe(x, 3, 1f);
//        Task d = n.believe(x, 8, 1f);
        var aa = merge(a, a2, n);
        p(aa);
		assertTrue((float) aa.conf() > (float) a.conf());
        var ab = merge(a, b, n);
        p(ab);
        assertEquals((float) ab.conf(), (float) a.conf());
//        if (Param.REVISION_ALLOW_DILUTE_UNION) {
        var ac = merge(a, c, n);
        p(ac);
		assertTrue((float) ac.conf() < (float) ab.conf(), () -> ac + " must have less conf than " + ab);
//        }
//        Task ad = Revision.merge(a, d, n);
//        p(ad);
//        assertTrue(ad.conf() < ac.conf());
    }


}