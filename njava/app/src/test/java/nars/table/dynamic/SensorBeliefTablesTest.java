package nars.table.dynamic;

import jcog.signal.MutableFloat;
import nars.*;
import nars.game.Game;
import nars.game.sensor.LambdaScalarSensor;
import nars.util.RingIntervalSeries;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

class SensorBeliefTablesTest {

    private final NAR n = NARS.shell();

    @Test
    void test1() {

        Game a = new Game("a");

        MutableFloat xx = new MutableFloat(0);
        var x = new LambdaScalarSensor($.atomic("x"), xx);
        a.addSensor(x);

        assertEquals(0, n.time());
        n.add(a);

        SensorBeliefTables xb = (SensorBeliefTables) x.concept.beliefs();

        step(n);
        assertEquals(1, xb.taskCount());
        step(n);
        assertEquals(1, xb.taskCount()); //same but stretch

        xx.set(0.5f);
        step(n);
        step(n);
        assertEquals(4, n.time());

        if (!(xb.sensor instanceof TruthCurveBeliefTable))
            assertEquals(2, xb.taskCount());

        {
            List<NALTask> tt = xb.taskStream().sorted(Comparator.comparing(NALTask::start)).collect(toList());
            assertEquals(2, tt.size());
            assertEquals(2, tt.get(0).range());
            assertEquals(2, tt.get(1).range());

            assertArrayEquals(new long[] { 1 }, tt.get(0).stamp());
//            assertArrayEquals(new long[] { 2 }, tt.get(1).stamp());
            //assertTrue(!Arrays.equals(tt.get(0).stamp(), tt.get(1).stamp()));
        }

        xx.set(0.75f);
        step(n);
        {
            List<Task> tt = xb.taskStream().collect(toList());
            assertEquals(3, tt.size());
        }

        RingIntervalSeries b = (RingIntervalSeries) ((TaskSeriesSeriesBeliefTable) xb.sensor).tasks;
        int head = b.q.head();
//        assertEquals(0, rb.indexNear(head,0));
        assertEquals(0, b.indexNear(head,0));
        assertEquals(0, b.indexNear(head,1));
        assertEquals(0, b.indexNear(head,2));
        assertEquals(1, b.indexNear(head,3));
        assertEquals(1, b.indexNear(head,4));
        assertEquals(2, b.indexNear(head,5));
        assertEquals(2, b.indexNear(head,6));
        assertEquals(2, b.indexNear(head,1000));
        assertEquals(0, b.indexNear(head,-5));
        assertTrue(b.first().start() < b.last().start());

        //stretch another step
        step(n);


        //test truthpolation of projected future signal, which should decay in confidence
		//assertTrue((float) n.beliefTruth(x, n.time() + 10).conf() < n.confDefault(BELIEF) - 0.1f);

//        xb.series.casetTaskCapacity(3);
//
//        xx.set(0);  step(n, xb); //cause wrap-around
//        assertEquals(1, b.indexNear(head,4));
//        assertEquals(0, b.indexNear(head,7)); //wrap-around
//
//




    }

    @Test
    void testEmpty() {
        Game a = new Game("a");

        MutableFloat xx = new MutableFloat(0);
        var x = new LambdaScalarSensor($.atomic("x"), xx);
        a.addSensor(x);

        n.add(a);

        SensorBeliefTables xb = (SensorBeliefTables) x.concept.beliefs();

        step(n); step(n); step(n);
        xx.set(0.75f);
        step(n); step(n);

        xb.print();
        var xs = xb.sensor;
        assertTrue(xs.isEmpty(-2, -1));
        assertTrue(xs.isEmpty(-2, 0));
        assertTrue(xs.isEmpty(0, 0));
        assertFalse(xs.isEmpty(0, 1));
        assertFalse(xs.isEmpty(1, 3));
        assertFalse(xs.isEmpty(4, 7));
        assertTrue(xs.isEmpty(7, 8));
        assertTrue(xs.isEmpty(7, 7));

    }

    private static void step(NAR n) {
        n.run(1);
//        System.out.println("@" + n.time());
//        xb.print();
//        System.out.println();
    }

    @Test
    void testCurveBeliefTable() {
        Game a = new Game("a");

        MutableFloat xx = new MutableFloat(0);
        var x = new LambdaScalarSensor($.atomic("x"), xx);
        a.addSensor(x);

        n.add(a);

        int cap = 8;

        SensorBeliefTables xb = (SensorBeliefTables) x.concept.beliefs();
        TruthCurveBeliefTable tc = new TruthCurveBeliefTable(x.concept.term, true);
        xb.tables.set(0, tc);

        assertTrue(tc.isEmpty());

        xx.set(0.75f); step(n);

        assertFalse(tc.isEmpty());

        xx.set(0.25f); step(n);

        assertEquals(n.time()-1, tc.start());
        assertEquals(n.time(), tc.end());

        assertEquals("$1.0 x. 1 %.75;.90%", task(tc, 1, 1));
        assertEquals("$1.0 x. 2 %.25;.90%", task(tc, 2, 2));
        assertEquals("$1.0 x. 1⋈2 %.50;.90%", task(tc, 1, 2));

        step(n);
        assertEquals("$1.0 x. 1⋈3 %.42;.90%", task(tc, 1, 3));
        assertEquals("$1.0 x. 1⋈3 %.42;.90%", task(tc, 1, 4), "same, just some extra that isn't defined will be excluded");

    }

    private static String task(TruthCurveBeliefTable tc, int s, int e) {
        return tc.task(s, e, 0, null).toString();
    }
}