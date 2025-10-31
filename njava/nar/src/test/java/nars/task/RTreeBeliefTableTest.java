//package nars.task;
//
//import jcog.data.list.Lst;
//import jcog.math.MultiStatistics;
//import jcog.pri.Prioritized;
//import jcog.tree.rtree.HyperIterator;
//import jcog.tree.rtree.node.RNode;
//import nars.$;
//import nars.NAR;
//import nars.NARS;
//import nars.concept.TaskConcept;
//import nars.table.BeliefTables;
//import nars.table.eternal.EternalTable;
////import nars.table.temporal.RTreeBeliefTable;
//import nars.table.temporal.TemporalBeliefTable;
//import nars.task.util.TaskRegion;
//import nars.term.Term;
//import nars.term.atom.Atomic;
//import nars.Truth;
//import nars.truth.func.TruthFunctions;
//import org.eclipse.collections.api.block.function.primitive.FloatFunction;
//import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
//import org.jetbrains.annotations.NotNull;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//import static java.util.stream.Collectors.toList;
//import static jcog.Str.n4;
//import static nars.Op.BELIEF;
//import static nars.task.TaskTest.task;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//class RTreeBeliefTableTest {
//
//    private static final LongToFloatFunction stepFunction = (t) -> (Math.sin(t) / 2f + 0.5f) >= 0.5 ? 1f : 0f;
//
//    static final Term x = Atomic.atom("x");
//    private static void testAccuracy(int dur, int period, int end, int cap, LongToFloatFunction func) {
//
//        NAR n = NARS.shell();
//
//        n.time.dur(dur);
//
//
//        TaskConcept c = (TaskConcept) n.conceptualize(x);
//        @NotNull BeliefTables cb = (BeliefTables) (c.beliefs());
//
//        cb.tableFirst(EternalTable.class).setTaskCapacity(0);
//        cb.tableFirst(TemporalBeliefTable.class).setTaskCapacity(cap);
//
//
//        System.out.println("points:");
//        long time;
//        long start = n.time();
//        while ((time = n.time()) < end) {
//            float f = func.valueOf(time);
//            System.out.print(time + "=" + f + '\t');
//            n.input(task(x, BELIEF, f, 0.9f).time(time).withPri(0.5f).apply(n));
//            n.run(period);
//            c.beliefs().print();
//            System.out.println();
//
//
//        }
//        System.out.println();
//        System.out.println();
//
//
//        MultiStatistics<NALTask> m = new MultiStatistics<NALTask>()
//                .classify("input", NALTask::isInput)
////                .classify("derived", (t) -> t instanceof DerivedTask)
//
//                .value("pri", Prioritized::pri)
//                .value2D("truth", (t) -> new float[]{t.freq(), (float) t.conf()})
//                .value("freqErr", (t) -> Math.abs(((t.freq() - 0.5f) * 2f) - func.valueOf(t.mid())))
//                .add(c.beliefs().taskStream().collect(toList()));
//
//        System.out.println();
//        m.print();
//        System.out.println();
//
//        c.beliefs().print();
//
//
//        //CSVOutput csv = new CSVOutput(System.out, "time", "actual", "approx");
//
//        double errSum = 0;
//        for (long i = start; i < end; i++) {
//            float actual = func.valueOf(i);
//
//            Truth actualTruth = n.beliefTruth(x, i);
//            float approx, err;
//            if (actualTruth != null) {
//                approx = actualTruth.freq();
//                err = Math.abs(approx - actual);
//            } else {
//                approx = Float.NaN;
//                err = 1f;
//            }
//
//            errSum += err;
//
//            //csv.out(i, actual, approx);
//
//        }
//        double avgErr = errSum / (end - start + 1);
//        System.out.println();
//        System.out.println(n4(avgErr) + " avg freq err per point");
//        assertTrue(avgErr < 0.4f);
//    }
//
//
//    @Test
//    void testAccuracyFlat() {
//
//        testAccuracy(1, 1, 20, 8, (t) -> 0.5f);
//    }
//
//    @Test
//    void testAccuracySineDur1() {
//
//        testAccuracy(1, 1, 20, 8, (t) -> (float) (Math.sin(t / 5f) / 2f + 0.5f));
//    }
//
//    @Test
//    void testAccuracySineDur1Ext() {
//        testAccuracy(1, 1, 50, 8, (t) -> (float) (Math.sin(t / 1f) / 2f + 0.5f));
//    }
//
//    @Test
//    void testAccuracySineDur() {
//        testAccuracy(2, 2, 50, 8, (t) -> (float) (Math.sin(t / 5f) / 2f + 0.5f));
//    }
//
//    @Test
//    void testAccuracySawtoothWave() {
//
//        testAccuracy(1, 3, 15, 5, stepFunction);
//    }
//
//    @Test
//    void testAccuracySquareWave() {
//        testAccuracy(1, 1, 7, 5, stepFunction);
//    }
//
//    @Disabled
//    @Test
//    void testHyperIterator() {
//        NAR n = NARS.shell();
//
//        n.time.dur(1);
//
//        Term term = $.p("x");
//
//        TaskConcept c = (TaskConcept) n.conceptualize(term);
//        BeliefTables cb = (BeliefTables) (true ? c.beliefs() : c.goals());
//
//        int cap = 16;
//
//        RTreeBeliefTable table = cb.tableFirst(RTreeBeliefTable.class);
//        table.setTaskCapacity(cap);
//
//
//        int horizon = 50;
//        int maxRange = 8;
//
//        //populate table randomly
//        for (int i = 0; i < cap; i++) {
//            long start = n.random().nextInt(horizon);
//            long end = start + n.random().nextInt(maxRange);
//            TemporalBeliefTableTest.add(table, term, 1f, 0.9f, start, end, n);
//        }
//        table.print();
//
//
//        List<TaskRegion> shouldBeAscendingTimes = seek(table, -1, -1);
//        print("shouldBeAscendingTimes", shouldBeAscendingTimes);
//        List<TaskRegion> shouldBeDescendingTimes = seek(table, horizon + 1, horizon + 1);
//        print("shouldBeDescendingTimes", shouldBeDescendingTimes);
//        List<TaskRegion> shouldOscillateOutwardFromMidpoint = seek(table, horizon / 2, horizon / 2);
//        print("shouldOscillateOutwardFromMidpoint", shouldOscillateOutwardFromMidpoint);
//    }
//
//    @Deprecated
//    static List<TaskRegion> seek(RTreeBeliefTable table, long s, long e) {
//        int c = table.capacity();
//        List<TaskRegion> seq = new Lst(c);
//        //double dur = table.tableDur((s+e)/2);
//        table.read(t -> {
//            HyperIterator<TaskRegion> h = new BeliefStrengthHyperIterator(c, s, e);
//
//            h.add(table.root());
//            h.bfs();
//            h.values.forEach(seq::add);
//        });
//        return seq;
//    }
//
//    /** for use only in temporal belief tables; eternal tasks not supported since i dont know how to directly compare them with temporals for the purposes of this interface */
//    public static FloatFunction<TaskRegion> beliefStrength(long targetStart, long targetEnd) {
//        return t -> beliefStrength(t, targetStart, targetEnd);
//    }
//    static float beliefStrength(TaskRegion t, long qStart, long qEnd) {
//        return (float)(TruthFunctions.c2w(t.confMean()) * t.range() / (1.0 + t.minTimeTo(qStart, qEnd)));
//    }
//
//    static void print(String msg, List<TaskRegion> seq) {
//        System.out.println(msg);
//        int i = 0;
//        for (TaskRegion t : seq)
//            System.out.println("\t" + (i++) + ": " + t);
//        System.out.println();
//    }
//
//    @Test
//    void testSplitOrdering() {
//        NAR n = NARS.shell();
//
//        n.time.dur(1);
//
//        Term term = $.p("x");
//
//        TaskConcept c = (TaskConcept) n.conceptualize(term);
//        BeliefTables cb = (BeliefTables) (true ? c.beliefs() : c.goals());
//
//        int cap = 64;
//
//        TemporalBeliefTable table = cb.tableFirst(TemporalBeliefTable.class);
//        table.setTaskCapacity(cap);
//
//
//        int horizon = 50;
//        int maxRange = 8;
//
//        //populate table randomly
//        for (int i = 0; i < cap; i++) {
//            long start = n.random().nextInt(horizon);
//            long end = start + n.random().nextInt(maxRange);
//            TemporalBeliefTableTest.add(table, term, n.random().nextFloat(), n.random().nextFloat() * 0.8f + 0.1f, start, end, n);
//        }
//        table.print();
//
//        //table.read(t -> t.root().streamNodesRecursively().forEach(System.out::println));
//    }
//
//
//    private static final class BeliefStrengthHyperIterator extends HyperIterator<TaskRegion> {
//        private final long s, e;
//
//        public BeliefStrengthHyperIterator(int c, long s, long e) {
//            super(8, new TaskRegion[Math.min(c, 32)]);
//            this.s = s;
//            this.e = e;
//        }
//
//        @Override
//        protected float rankNode(RNode<TaskRegion> o, float min) {
//            return beliefStrength((TaskRegion)(o.bounds()), s, e);
//        }
//
//        @Override
//        protected float rankItem(TaskRegion o, float min) {
//            return beliefStrength(o, s, e);
//        }
//    }
//}