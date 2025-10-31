package nars.truth;

import jcog.Fuzzy;
import nars.*;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * compact chart-like representation of a belief state at each time cycle in a range of time.
 * useful as a memoized state snapshot of a belief table
 * stored in an array of float quadruplets for each task:
 * 1) start
 * 2) end
 * 3) freq
 * 4) conf
 * 5) quality
 */
public class TruthWave {

    /** controls the precision of the evaluation */
    private static final int answerTries = 3;

    private static final int ENTRY_SIZE = 4;
    public TaskTable table;

    /**
     * start and stop interval (in cycles)
     */
    private long start, end;

    /**
     * sequence of triples (freq, conf, start, end) for each task; NaN for eternal
     */
    private float[] truth;
    private int size;


    public TruthWave(int initialCapacity) {
        resize(initialCapacity);
        clear();
    }

    public TruthWave(TaskTable b) {
        this(b.taskCount());
        this.table = b;
        set(b, Long.MIN_VALUE, Long.MAX_VALUE);
        //TODO update range
    }


    private void load(@Nullable Truthed t, long s, long e, int index) {
        load(t, start, end, s, e, this.truth, index);
    }

    private static void load(@Nullable Truthed t, long absStart, long absEnd, long start, long end, float[] array, int index) {
        float F, C = 0;
        if (t != null) {
            F = t.freq(start, end);
            if (F==F)
                C = (float) t.conf();
        } else {
            F = Float.NaN;
        }
        load(F, C, absStart, absEnd, start, end, array, index);
    }

    private static void load(float F, float C, long absStart, long absEnd, long start, long end, float[] array, int index) {
        double range = absEnd - absStart;
        array[index++] = pos(start, absStart, range);
        array[index++] = pos(end, absStart, range);
        array[index++] = F;
        array[index] = C;
    }

    private static float pos(long t, long absStart, double range) {
        return t == Op.ETERNAL ? Float.NaN :
                (float) (((t - absStart)) / range);
    }

    public void clear() {
        size = 0;
        start = end = Op.ETERNAL;
        table = null;
    }

    private void resize(int cap) {
        truth = new float[ENTRY_SIZE * cap];
    }

    /**
     * clears and fills this wave with the data from a table
     */
    public void set(TaskTable b, long minT, long maxT) {
        clear();
        this.table = b;
        int s = b.taskCount();
        if (s == 0) return;

        this.start = minT;
        this.end = maxT;

        size(s);

        this.size = 0;
        b.forEachTask(minT, maxT, this::eachTask);
    }

    private void eachTask(NALTask x) {
        long xs = x.start();
        if (xs > end) return; //OOB
        long xe = x.end();
        if (xe < start) return; //OOB

        if (x.truth() instanceof TruthCurve c)
            load(c, Math.max(start, xs), Math.min(end, xe));
        else
            load(x, xs, xe);
    }

    private void load(TruthCurve c, long xs, long xe) {
        c.forEach(xs, xe, true, t -> load(t.truth, t.start, t.end));
    }

    private void load(Truthed x, long xs, long xe) {
        load(x, xs, xe, (size++) * ENTRY_SIZE);
    }


    private void size(int s) {
        if (capacity() < s) resize(s);
    }



    /**
     * fills the wave with evenly sampled points in a time range
     */
    public void project(TaskTable table, long minT, long maxT, int points, Term term, float durMatch, NAR nar) {

        clear();
        this.start = minT;
        this.end = maxT;
        if (minT == maxT)
            return;

        size(points);

        float dt;
        int dth;
        long tStart;
        if (points <= 1) {
            dt = 0;
            dth = 0;
            tStart = (minT + maxT) / 2;
        } else {
            dt = (float) ((maxT - minT) / ((double) (points - 1) ));
            dth = Math.round(dt/2);
            tStart = minT;
        }


        float[] data = this.truth;
        int j = 0;
        Answer a = new Answer(term, true, answerTries, nar).dur(durMatch);

        for (int i = 0; i < points; i++) {
            long t = tStart + Math.round(i * dt);
            long s = t - dth, e = t + dth;
            Truth tr = a.clear().ttl(answerTries).time(s, e).match(table).truth(false);
            if (tr != null) {
                long mid = Fuzzy.mean(s, e);
                load(tr, minT, maxT, mid, mid, data, (j++) * ENTRY_SIZE);
            }

        }
        this.size = j;

    }

    public boolean isEmpty() {
        return size == 0;
    }

    private long start() {
        return start;
    }

    private long end() {
        return end;
    }

    public final void forEach(TruthWaveVisitor v) {
        int n = this.size;
        float[] t = this.truth;
        int j = 0;
        long start = this.start;
        double totalRange = this.end - start;
        for (int i = 0; i < n; i++) {
            float s = t[j++];
            float e = t[j++];
            float f = t[j++];
            float c = t[j++];
            long S = start + Math.round(totalRange * s);
            long E = start + Math.round(totalRange * e);
            v.onTruth(f, c, S, E);
        }
    }

    private int capacity() {
        return truth.length / ENTRY_SIZE;
    }

    @Override
    public String toString() {
        return start() + ".." + end() + ": " + Arrays.toString(truth);
    }


    @FunctionalInterface
    public interface TruthWaveVisitor {
        void onTruth(float f, float c, long start, long end);
    }
}