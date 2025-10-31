package nars.test.impl;

import jcog.Str;
import jcog.Util;
import jcog.data.graph.BitMatrixGraph;
import jcog.data.list.Lst;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.sort.RankedN;
import nars.*;
import nars.memory.SimpleMemory;
import nars.term.atom.Atomic;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.jetbrains.annotations.Nullable;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static java.lang.Math.abs;
import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * creates a graph of statements with random truth values.  ensures the graph is connected.
 * runs the graph in a NAR and detect significant divergences from the input.
 */
public abstract class IntegrityTest implements Runnable {

    public final BitMatrixGraph connectivity;
    private final boolean beliefOrGoal;
    boolean logDiff;
    boolean logConcepts;

    final int cycleBatch = 64;
    final int batches = 100;

    protected final NAR nar;
    final List<Term> edges = new Lst();
    final List<NALTask> edgeTasks = new Lst();

    protected IntegrityTest(boolean beliefOrGoal, BitMatrixGraph connectivity, NAR nar) {
        this.nar = nar;
        this.beliefOrGoal = beliefOrGoal;
        this.connectivity = connectivity;

    }

    public static BitMatrixGraph connectivity(int N, float edgeDensityMin) {
        assert (N > 2);
        BitMatrixGraph g = new BitMatrixGraph.BitSetRowsMatrixGraph(N, false);
        int totalEdges = N * (N - 1);
        int minEdges = (int) Math.ceil(edgeDensityMin * totalEdges);

        int edges = 0;

        Random rng = new XoRoShiRo128PlusRandom(0);
        boolean connected = false;
        //TODO safety exit
        do {
            int a = rng.nextInt(N), b = rng.nextInt(N);
            if (a == b) continue;
            if (g.setIfChanged(a, b, true))
                edges++;
            if (!connected)
                connected = g.dfsCount(0) >= N;
        } while (
                edges < minEdges //dense enough?
                        ||
                        !connected
        );
        return g;
    }

    public static void main(String[] args) {

        boolean logNAR = false;
        boolean logTasks = false;


        NAL.DEBUG = true;

        NAR n = NARS.tmp(8);

        int volMax = 16;
        n.complexMax.set(volMax);

        ((SimpleMemory)n.memory).capacity(4096);

        if (logNAR)
            n.log();

        n.time.dur(10);

        new StatementTest(connectivity(8, 0.5f),
//                IMPL, true, true, true
//                IMPL, true, true, false
//                INH, true, true, true
                INH, true, true, false
                //INH, false, true, true
                , n).run();

        if (logTasks)
            n.tasks().sorted(Comparators.byFloatFunction(t -> -t.pri())).forEach(System.out::println);
    }



    private void addEdge(boolean beliefOrGoal, NAR nar, int i, int j, Term edge) {
        edges.add(edge);

        NALTask task = NALTask.taskEternal(edge, beliefOrGoal ? BELIEF : GOAL, edgeTruth(edge, i, j), nar);
        budget(task);
        edgeTasks.add(task);

        nar.input(task);
//                nar.run(1); //HACK
    }

    public boolean rngBool() {
        return random().nextBoolean();
    }

    public Random random() {
        return (Random)nar.random(); //TODO better
    }

    protected void budget(NALTask task) {
        task.pri(nar);
    }

    /**
     * undirected edge term
     */
    @Nullable
    public abstract Term ungeTerm(int i, int j);

    /**
     * directed edge term
     */
    @Nullable
    public abstract Term digeTerm(int i, int j);

    public abstract Truth edgeTruth(Term edgeTerm, int i, int j);

    public static Term vertexTerm(int x) {
        return Atomic.atomic((char) ('a' + x));
    }

    public DoubleSummaryStatistics error() {
        int capacity = 4;
        boolean includeInputs = false;

//        double freqErrThresh = NAL.truth.TRUTH_EPSILON;

        DoubleSummaryStatistics s = new DoubleSummaryStatistics();

        boolean beliefsOrGoals = edgeTasks.getFirst().BELIEF();
        for (NALTask x : edgeTasks) {
            @Nullable Concept c = nar.concept(x);
            double ee;
            if (c == null) {
                ee = 1; //missing, full error
            } else {
                //measure truth
                double xConf = x.conf();

                BeliefTable table = beliefsOrGoals ? c.beliefs() : c.goals();

                Predicate<NALTask> filter = includeInputs ? null : z -> !z.isInput();

                long start = x.start();
                long end = x.end();
                @Nullable Term template = x.term();
                Answer a = nar.match(table, start, end, template, filter, 0, capacity);

                RankedN<NALTask> yy = a.tasks;

                NALTask y = a.task(true);

                if (y == null) {
                    assert (yy.isEmpty());
                    ee = 1; //full loss
                    error(x, Float.NaN, null, Float.NaN, 0, 0);
                } else if (!x.equals(y)) {
                    double[] meanVariance = Util.variance(yy.stream().mapToDouble(NALTask::freq));
                    double variance = meanVariance[1];
                    double yFreq =
                        y.freq();
                        //meanVariance[0]; //TODO needs weighted by evi
                    double confRatio = y.conf() / xConf;
                    double freqErr = abs(x.freq() - yFreq);

//                    if (abs(freqErr) < freqErrThresh && abs(confRatio - 1) < 0.01f)
//                        continue;

                    error(x, x.freq(), y, yFreq, confRatio, variance);
                    ee = freqErr * confRatio;

                } else
                    continue;

            }
            s.accept(ee);
        }
        return s;
    }

    @Override
    public final void run() {
        int N = connectivity.size();
        for (int i = 0; i < N; i++) {

            for (int j = i + 1; j < N; j++) {
                boolean fwd = connectivity.isEdge(i, j), rev = connectivity.isEdge(j, i);
                if (fwd && rev) {
                    Term uij = ungeTerm(i, j);
                    if (uij != null) {
                        addEdge(beliefOrGoal, nar, i, j, uij);
                        continue;
                    }
                }

                if (fwd) {
                    Term dij = digeTerm(i, j);
                    if (dij != null) addEdge(beliefOrGoal, nar, i, j, dij);
                }

                if (rev) {
                    Term dji = digeTerm(j, i);
                    if (dji != null) addEdge(beliefOrGoal, nar, j, i, dji);
                }

            }
        }

        NAR n = nar;
        for (int i = 0; i < batches; i++) {
            n.run(cycleBatch);

            System.out.println(n.time() + "\t" + " " +
                    edgeTasks.size() + " edges\terr=" +
                    error().toString());
        }

        if (logConcepts)
            n.concepts().forEach(Concept::print);
    }

    protected abstract void error(NALTask x, double xFreq, @Nullable NALTask y, double yFreq, double confRatio, double variance);

    abstract static class AbstractTest extends IntegrityTest {


        protected AbstractTest(boolean beliefOrGoal, BitMatrixGraph connectionTemplate, NAR n) {
            super(beliefOrGoal, connectionTemplate, n);
        }


        @Override
        protected void error(NALTask x, double xFreq, @Nullable NALTask y, double yFreq, double confRatio, double variance) {

            double err = abs(xFreq - yFreq);
            boolean bad = err > 0.5 && confRatio > 0.5;
            boolean print = false;
            if (logDiff) {
                if (y == null)
                    System.out.println("FORGOT " + x);
                else {
                    print = true;
                }
            }

            if (print || bad)
                System.err.println(Str.n4(err) + "+-" + Str.n4(variance) + "\t" + Str.n2(xFreq) + "=?=" + Str.n2(yFreq) + "\t" +
                        "\t" + x.truth() + "\t" + y + "\t c*" + Str.n2(confRatio));

            if (bad) {
                System.out.println(y.proof());
                NAR.proofAppend(y, System.err);
                fail();
            }

        }
    }

    static class StatementTest extends AbstractTest {

        private final Op op;
        private final boolean fwd, rev;
        final float conf =
                //.5f;
                0.9f;

        StatementTest(BitMatrixGraph connectionTemplate, Op o, boolean beliefOrGoal, boolean fwd, boolean rev, NAR nar) {
            super(beliefOrGoal, connectionTemplate, nar);
            assert(!o.commutative);
            this.op = o;

            this.fwd = fwd; this.rev = rev;
        }

        @Override
        public Term ungeTerm(int i, int j) {
            assert (i < j);
            return null;
        }

        @Override
        public Term digeTerm(int i, int j) {
            if ((fwd && (i < j)) || (rev && (j < i)))
                return op.the(vertexTerm(i), vertexTerm(j));

            return null;
        }

        @Override
        public Truth edgeTruth(Term edgeTerm, int i, int j) {
            //return $.t(nar.random().nextFloat(), nar.confDefault(BELIEF));
            float N = this.connectivity.size();
            return $.t(
                //i < j ? (j-i)/N : 1 - (i-j)/N,
                1 - abs(i - j)/N,
                //nar.confDefault(BELIEF)
                conf
            );
        }
    }
}