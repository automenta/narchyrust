package nars.table;

import com.google.common.base.Joiner;
import jcog.TODO;
import jcog.data.list.Lst;
import nars.*;
import nars.action.memory.Remember;
import nars.concept.TaskConcept;
import nars.table.dynamic.MutableTasksBeliefTable;
import nars.table.dynamic.SerialBeliefTable;
import nars.table.temporal.NavigableMapBeliefTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.Termed;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.TruthFunctions.c2e;
import static nars.task.TaskTest.task;
import static org.junit.jupiter.api.Assertions.*;

class TemporalBeliefTableTest {

    static final Term ab = $$("a:b");
    final NAR n = NARS.shell();

    private static void assertScan(TemporalBeliefTable table, int center, String log) {

        if (table instanceof NavigableMapBeliefTable) {
            TimeLogger l = new TimeLogger();
            ((NavigableMapBeliefTable) table).scanNear(center, l);
            assertEquals(log, l.toString().trim());
        } else {
            //..
        }
    }

    private static String taskTableStr(BeliefTable table) {
        StringBuilder sb = new StringBuilder(1024);
        table.taskStream().forEach(z -> TaskTable.println(sb, z));
        return sb.toString();
    }

    private static String assertSeq(int capacity, int w, int range, String seqExpect, BeliefTable r) {
        if (range != 1)
            throw new TODO();

        NAR n = NARS.shell();

        TaskConcept X = (TaskConcept) n.conceptualize(ab);
        r.taskCapacity(capacity);
        if (r instanceof MutableTasksBeliefTable M)
            M.sharedStamp = n.evidence();

        Term x = X.term();

        for (int i = 0; i < capacity; i++)
            add(r, x, i, i, n);

        List<NALTask> l = new Lst<>(capacity);
        StringBuilder s = new StringBuilder(capacity * 6);
        Predicate<NALTask> each = z -> {
            s.append(z.start());
            if (z.range() != 1)
                s.append("..").append(z.end());
            s.append(" ");
            l.add(z);
            return true;
        };

        if (r instanceof SerialBeliefTable M)
            M.whileEachRadial(0, capacity, w, true, each);
        else if (r instanceof NavigableMapBeliefTable N)
            N.scanNear(w, each);
        else
            throw new TODO();

        var seq = s.toString().trim();
        //System.out.println(seq);

        assertEquals(capacity, l.size(), "skips");
        assertEquals(capacity, new HashSet<>(l).size(), "repeats");

        if (seqExpect != null)
            assertEquals(seqExpect, seq);

        return seq;
    }

    static void testSubsumeContainedEvent(boolean forward, BeliefTable t) {

        NAR n = NARS.shell();
        TaskConcept AB = (TaskConcept) n.conceptualize(ab);
        t.taskCapacity(4);

        long as = 0;
        long ae = 3;
        long bs = 1;
        long be = 2;
        long sameStamp = 1;

        Task x = add(t, AB, 1f, 0.9f, forward ? as : bs, forward ? ae : be, sameStamp, n);
        assertNotNull(x);

        Task y = add(t, AB, 1f, 0.9f, forward ? bs : as, forward ? be : ae, sameStamp, n);
        assertNotNull(y);

        assertEquals(1, t.taskCount());
        assertEquals(4, t.taskStream().findFirst().get().range()); //HACK task range = bounds range + 1

//		if (t instanceof RTreeBeliefTable)
//			assertEquals(3, ((RTreeBeliefTable) t).bounds().range(0));
    }

    static NALTask add(TaskTable r, Termed x, float freq, float conf, long start, long end, long evi, NAR n) {
        NALTask a = task(x.term(), BELIEF, freq, conf).time(start, start, end).evidence(evi).apply(n);
        a.pri(0.5f);
        return insert(r, a, n);
    }

    private static NALTask insert(TaskTable t, NALTask a, NAR n) {
        var r = new Remember(n.main());
        r.input(a);
        t.remember(r);
        return a;
    }

    static NALTask add(TaskTable r, Termed x, long start, long end, NAR n) {
        if (r instanceof MutableTasksBeliefTable b)
            return b.setOrAdd(1, c2e(0.9), start, end, 0.5f, n);
        else
            return add(r, x, 1, 0.9f, start, end, n);
    }

    static NALTask add(TaskTable r, Termed x, float freq, float conf, long start, long end, NAR n) {
        return add(r, x, freq, conf, start, end, n.evidence()[0], n);
    }

    private static void testAddRememberMisc(BeliefTable r) {
        NAR n = NARS.shell();
        TaskConcept X = (TaskConcept) n.conceptualize(ab);
        r.taskCapacity(4);

        assertEquals(0, r.taskCount());

        Term x = X.term();
        float freq = 1;
        float conf = 0.9f;
//        int creationTime = 1;
        int start = 1, end = 1;

        /* insert first task */
        NALTask a = add(r, x, freq, conf, start, end, n);
        assertEquals(1, r.taskCount());

        /* insert duplicate, and check that only 1 task is still present */
        insert(r, a, n);
        assertEquals(1, r.taskCount());

        Task b = add(r, x, 0f, 0.5f, 1, 1, n);
        assertEquals(2, r.taskCount());

        Task c = add(r, x, 0.1f, 0.9f, 2, 2, n);
        assertEquals(3, r.taskCount());

        Task d = add(r, x, 0.1f, 0.9f, 3, 4, n);
        assertEquals(4, r.taskCount());

//        System.out.println("at capacity");
//        r.print(System.out);

        Task e = add(r, x, 0.3f, 0.9f, 3, 4, n);

//        System.out.println("\nat capacity?");
//        r.print(System.out);

//        assertEquals(4, r.taskCount());

        System.out.println("after capacity compress inserting " + e.toString(true));
//        r.print(System.out);
    }

    @Test
    @Disabled
    void ordering_different_sizes() {
        TestNAR t = new TestNAR(n);

        for (int w = 0; w < 5; w += 2)
            for (int i = 0; i < 5; i++)
                t.believe("x", 1, 0.5f, i - w / 2, i + w / 2);

        var ii = n.concept("x").beliefs().taskStream().iterator();
        System.out.println(Joiner.on("\n").join(ii));
        //TODO test
    }

    @Test
    void scan1() throws Narsese.NarseseException {
        TestNAR n = new TestNAR(this.n);

        BeliefTables b = (BeliefTables) this.n.conceptualize("x").beliefs();
        b.tablesArrayForStore();
        var table = b.tableFirst(TemporalBeliefTable.class);


        n.believe("x", 1, 0.5f, 0, 0);
        n.believe("x", 1, 0.5f, 1, 1);
        n.believe("x", 1, 0.5f, 3, 3);
        n.believe("x", 1, 0.5f, 7, 7);

        assertScan(table, 1, "1..1 0..0 3..3 7..7");
        assertScan(table, 3, "3..3 1..1 0..0 7..7");
    }

//	@Disabled
//	@Test void reviseImpl_PP() {
//		TemporalBeliefTable r = new NavigableMapBeliefTable();
//		NAR n = NARS.shell();
//		TaskConcept X = (TaskConcept) n.conceptualize(ab);
//		r.taskCapacity(1);
//		add(r, $$("(x ==>+1 y)"), 1f,0.5f,0, 1, n);
//		assertEquals(1, r.taskCount());
//		add(r, $$("(x ==>+5 y)"), 1f,0.5f,0, 1, n);
//
//		assertEquals(0, r.taskCount());
//
//		Concept c = n.concept("(x ==> (y &&+- y))");
//		assertNotNull(c);
//		assertEquals(1, c.tasks(true,false,false,false).count());
//		String rt = c.tasks(true, false, false, false).findFirst().get().toString();
//		assertTrue(rt.contains("(x ==>+1 (y &&+4 y))"));
//
//	}
//
//	@Disabled @Test void reviseImpl_PN() {
//		reviseImpl_PN(true);
//	}
//	@Disabled @Test void reviseImpl_NP() {
//		reviseImpl_PN(false);
//	}
//
//	private static void reviseImpl_PN(boolean fwd) {
//		TemporalBeliefTable r = new NavigableMapBeliefTable();
//		NAR n = NARS.shell();
//		r.taskCapacity(1);
//		add(r, $$("(x ==>+1 y)"), fwd ? 1 : 0,0.5f,0, 1, n);
//		assertEquals(1, r.taskCount());
//		add(r, $$("(x ==>+5 y)"), fwd ? 0 : 1,0.5f,0, 1, n);
//
//		assertEquals(0, r.taskCount());
//
//		Concept c = n.concept("(x ==> ((--,y) &&+- y))");
//		assertNotNull(c);
//		assertEquals(1, c.tasks(true,false,false,false).count());
//		String rt = c.tasks(true, false, false, false).findFirst().get().toString();
//		assertTrue(rt.contains(fwd ?
//				"(x ==>+1 (y &&+4 (--,y)))" :
//				"(x ==>+1 ((--,y) &&+4 y))"));
//	}


//	@Test void SeriesBasicOperations() {
//		testAddRememberMisc(new SeriesBeliefTable(ab, true, new RingBufferTaskSeries<>(8)));
//	}

//	@Test
//	void RTreeSubsumeContainedEvent() {
//		testSubsumeContainedEvent(true, new RTreeBeliefTable());
//	}
//
//	@Test
//	void RTreeSubsumeContainedByEvent() {
//		testSubsumeContainedEvent(false, new RTreeBeliefTable());
//	}

    @Test
    void compression1() throws Narsese.NarseseException {
        TestNAR t = new TestNAR(n);

        BeliefTables b = (BeliefTables) n.conceptualize("x").beliefs();
        b.tablesArrayForStore(); //force un-lazy

        var table = b.tableFirst(TemporalBeliefTable.class);

        t.believe("x", 1, 0.5f, 0, 0);
        t.believe("x", 1, 0.5f, 1, 1);
        t.believe("x", 1, 0.5f, 2, 2);
        t.believe("x", 1, 0.5f, 3, 3);
        assertEquals(4, table.taskCount());

        table.taskCapacity(3);
        assertTrue(table.taskCount() <= 3);
        //System.out.println( taskTableStr(table) );
    }

    @Test
    void addMerge() throws Narsese.NarseseException {
        TestNAR t = new TestNAR(n);
        BeliefTables b = (BeliefTables) n.conceptualize("x").beliefs();
        b.tablesArrayForStore(); //force un-lazy

        var table = b
                .tableFirst(TemporalBeliefTable.class);
        table.taskCapacity(16);

        long[] stamp = n.evidence();
        /* intersect merge */
        {

            n.input(NALTask.task($$("x"), BELIEF, $.t(1, 0.5f), 0, 1, stamp));
            assertEquals(1, table.taskCount());
            n.input(NALTask.task($$("x"), BELIEF, $.t(1, 0.5f), 1, 2, stamp));

            assertEquals(1, table.taskCount());
            assertEquals("0..2", table.range().toString());
        }

        {
            /* contains merge */
            n.input(NALTask.task($$("x"), BELIEF, $.t(1, 0.5f), -1, 3, stamp));
            assertEquals(1, table.taskCount());
            assertEquals("-1..3", table.range().toString());
        }

        {
            /* containedBy merge */
            n.input(NALTask.task($$("x"), BELIEF, $.t(1, 0.5f), 0, 0, stamp));
            assertEquals(1, table.taskCount());
        }

    }

    //	@Test void RTreeBasicOperations() {
//		testAddRememberMisc(new RTreeBeliefTable());
//	}

    @Disabled @Test
    void SkipListBasicOperations() {
        testAddRememberMisc(new NavigableMapBeliefTable());
    }

    @Disabled @Test
    void scanNearSimple_NavigableMap() {
        int capacity = 16;

        NavigableMapBeliefTable m = new NavigableMapBeliefTable();
        m.taskCapacity(capacity);

        assertSeq(capacity, 8, 1, null, m);
    }

    /**
     * linear sequence of tasks uniform range
     */
    @Test
    void scanNearSimple_Series_1() {
        int capacity = 16;
        assertSeq(capacity, 8, 1,
                "8 7 9 6 10 5 11 4 12 3 13 2 14 1 15 0", new MutableTasksBeliefTable(ab, true, capacity));
    }

    @Test
    void scanNearSimple_Series_2() {
        int capacity = 16;
        assertSeq(capacity, 7, 1,
                "7 6 8 5 9 4 10 3 11 2 12 1 13 0 14 15", new MutableTasksBeliefTable(ab, true, capacity));
    }

    private static final class TimeLogger implements Predicate<NALTask> {

        final StringBuilder sb = new StringBuilder(128);

        @Override
        public boolean test(NALTask t) {
            sb.append(t.start()).append("..").append(t.end()).append(" ");
            return true;
        }

        public String toString() {
            return sb.toString();
        }
    }

//	@Test
//	void testProjection() throws Narsese.NarseseException {
//		RTreeBeliefTable r = new RTreeBeliefTable();
//
//		NAR nar = NARS.shell();
//		Term ab = $.$("a:b");
//		TaskConcept AB = (TaskConcept) nar.conceptualize(ab);
//		r.setTaskCapacity(4);
//
//		add(r, AB, 1f, 0.9f, 0, 1, nar);
//		add(r, AB, 0f, 0.9f, 2, 3, nar);
//
//
//		assertEquals("%.83;.92%" /*"%1.0;.90%"*/, r.truth(0, 0, ab, null, nar).toString());
//		assertEquals("%.67;.93%" /*"%1.0;.90%"*/, r.truth(1, 1, ab, null, nar).toString());
//		assertEquals("%1.0;.90%", r.truth(0, 1, ab, null, nar).toString());
//
//		assertEquals("%0.0;.90%", r.truth(2, 3, ab, null, nar).toString());
//		assertEquals("%0.0;.90%", r.truth(3, 3, ab, null, nar).toString());
//
//		assertEquals("%.50;.90%", r.truth(1, 2, ab, null, nar).toString());
//
//		assertEquals("%.33;.87%", r.truth(4, 4, ab, null, nar).toString());
//		assertEquals("%.35;.85%", r.truth(4, 5, ab, null, nar).toString());
//		assertEquals("%.38;.83%", r.truth(5, 5, ab, null, nar).toString());
//		assertEquals("%.40;.79%", r.truth(6, 6, ab, null, nar).toString());
//		assertEquals("%.39;.79%", r.truth(5, 8, ab, null, nar).toString());
//		assertEquals("%.44;.67%", r.truth(10, 10, ab, null, nar).toString());
//
//	}

}