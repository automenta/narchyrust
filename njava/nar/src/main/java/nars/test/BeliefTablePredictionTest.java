package nars.test;

import jcog.Util;
import nars.*;
import nars.table.BeliefTables;
import nars.table.temporal.TemporalBeliefTable;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.impl.block.factory.Comparators;

import java.util.LinkedHashSet;
import java.util.Set;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BeliefTablePredictionTest {
    public static final Term x = $$("x");

    static {
        NAL.DEBUG = true;
    }

    public final NAR n = NARS.tmp();
    final int t0 = 0;
    final int periods = 4;
    final float samplesPerPeriod = 8;

    final int tableCapacity = 128;

    final int volMax = 8;
    final float conf = 0.9f;
    Set<NALTask> derived;
    private static final float period = 16;
    final float freq = 0.5f;
    public final IntToFloatFunction f = t ->
            (float) (Math.sin(t * freq * Math.PI / (period)) + 1) / 2;
            //(float) (Math.sin(t * freq * Math.PI / (period)) + 1) / 2 >= 0.5f ? 1 : 0; //square wave

    /** in periods */
    final float dur = 0.25f;



    public BeliefTablePredictionTest trace(boolean on) {
        if (on && derived == null) derived = new LinkedHashSet();
        else if (!on && derived != null) {
            derived.clear();
            derived = null;
        }
        return this;
    }

    public TemporalBeliefTable restart() {
        n.complexMax.set(volMax);
        n.time.dur((int)Math.ceil(this.dur * period));
        n.confMin.set(0);

        TemporalBeliefTable table = ((BeliefTables) n.conceptualize(x).beliefs()).tableFirst(TemporalBeliefTable.class);

        table.clear();

        assert (tableCapacity >= 1 * periods * Math.max(1, (period / samplesPerPeriod)));
        table.taskCapacity(tableCapacity);

        input(x, conf, f, t0, period, periods, samplesPerPeriod, n);

        if (derived != null) derived.clear();
        n.main().onTask(z -> {
            NALTask Z = (NALTask) z;
            if (Z.BELIEF() && x.equals(Z.term())) {
                if (derived != null) {
                    derived.add(Z);
                }
                double d = divergence(Z);
                if (d > 0.1) {
                    System.out.println("actual: " + f.valueOf((int) Z.mid()));
                    NAR.proofPrint(Z);
                    System.out.println();
                }
            }
        });
//        n.log();

        return table;
    }

    public void printDivergence(Set<NALTask> derived) {
        derived.stream().sorted(Comparators.byDoubleFunction(this::divergence)).forEachOrdered(Z -> {
            System.out.println("actual: " + f.valueOf((int) Z.mid()));
            NAR.proofPrint(Z);
            System.out.println();
        });
    }

    public double divergenceWeighted(NALTask z) {
        double C = z.conf();
        return divergence(z) * C;
    }

    public double divergence(NALTask z) {
        long s = z.start(), e = z.end();
        double F = z.freq();
        double zDiff = 0;
        for (long t = s; t <= e; t++) {
            zDiff += Math.abs(F - f.valueOf((int) t));
        }
        return zDiff;
    }

    private void input(Term x, float conf, IntToFloatFunction f, int t0, float period, int periods, float samplesPerPeriod, NAR n) {

        int t = t0;
        float samplePeriodF = period / samplesPerPeriod;
        int samplePeriod = Math.round(samplePeriodF);
        assert (samplePeriod >= 1);
        assertTrue(Util.equals(samplePeriodF, samplePeriod, 0.01f)); //divisor
        for (int p = 0; p < periods; p++) {
            for (int s = 0; s < samplesPerPeriod; s++) {
                float y = f.valueOf(t);
                n.believe(x, y, conf, t, t + samplePeriod - 1);
                t += samplePeriod;
            }
        }
    }

    public long start() {
        return 0;
    }

    public long end() {
        return (long) Math.ceil(periods * samplesPerPeriod);
    }

    public void run(int cycles) {
        n.run(cycles);
    }
}