package nars;

import jcog.Util;
import nars.gui.BeliefTableChart;
import nars.test.BeliefTablePredictionTest;
import nars.time.clock.CycleTime;

import static spacegraph.SpaceGraph.window;

public class BeliefTablePredictionsUI {
    public static void main(String[] args) {
        BeliefTablePredictionTest t = new BeliefTablePredictionTest();

        long range = t.end() - t.start();

        t.restart();

        BeliefTableChart c = new BeliefTableChart(t.x);
        c.beliefTableChartParams.projections = 256;
        window(c, 500, 500);

        double extent = 2;

        while (true) {

            long now = Math.round(((t.n.random().nextFloat() - 0.5f) * 2) * extent * range + 1);
            ((CycleTime)t.n.time).set(now);

            t.run(64);

            c.update(t.n,  Math.round(-extent * range), Math.round(t.end() + extent * range));
            Util.sleepMS(10);
        }

    }
}