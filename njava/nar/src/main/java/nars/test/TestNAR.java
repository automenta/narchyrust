package nars.test;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import jcog.Log;
import jcog.data.list.Lst;
import jcog.event.ByteTopic;
import nars.*;
import nars.term.util.TermException;
import nars.test.condition.DerivedTaskCondition;
import nars.test.condition.LambdaTaskCondition;
import nars.test.condition.NARCondition;
import nars.test.condition.TaskCondition;
import nars.time.Tense;
import nars.util.NARPart;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;
import org.slf4j.Logger;

import java.io.StringWriter;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

import static java.lang.Float.NaN;
import static nars.$.$$;
import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * TODO use a countdown latch to provide early termination for successful tests
 * TODO impl Focus
 */
public class TestNAR extends NARPart {

    private static final Logger logger = Log.log(TestNAR.class);
    private static final int maxSimilars = 2;
    public final Focus focus;
    /**
     * holds must (positive) conditions
     */
    private final Lst<NARCondition> succeedsIfAll = new Lst();
    /**
     * holds mustNot (negative) conditions which are tested at the end
     */
    private final Lst<NARCondition> failsIfAny = new Lst();
    /**
     * -1 = failure,
     * 0 = hasnt been determined yet by the end of the test,
     * (1..n) = success in > 1 cycles,
     * +1 = success in <= 1 cycles
     */
    public float score;
    public float freqTolerance = NAL.test.TEST_EPSILON;
    public NAR nar;
    /**
     * enable this to print reports even if the test was successful.
     * it can cause a lot of output that can be noisy and slow down
     * the test running.
     * TODO separate way to generate a test report containing
     * both successful and unsuccessful tests
     */
    StringWriter trace;
    /**
     * safety check to find tests with no conditions
     */
    public boolean requireAnyConditions = true;
    private float confTolerance = NAL.test.TEST_EPSILON;
    private boolean finished;
    private boolean exitOnAllSuccess = true;
    private boolean report = NAL.DEBUG;
    private int cycleEnd;

    public TestNAR(NAR nar) {
        this.focus = nar.main();

        this.nar = nar; //HACK

        nar.add(this);
    }

    private FluentIterable<NARCondition> conditions() {
        return (FluentIterable<NARCondition>) Iterables.concat(succeedsIfAll, failsIfAny);
    }

    public TestNAR confTolerance(float t) {
        this.confTolerance = t;
        return this;
    }

    public void freqTolerance(float f) {
        this.freqTolerance = f;
    }

    public void run() {
        long cycleEnd = this.cycleEnd;
            //infer final cycle
            for (NARCondition oc : conditions()) {
                long oce = oc.getFinalCycle();
                if (oce < Long.MAX_VALUE)
                    cycleEnd = Math.max(oce, cycleEnd);
            }
        if (cycleEnd == Long.MIN_VALUE)
            throw new UnsupportedOperationException("could not infer final cycle");

        run(cycleEnd);
    }

    public void run(long cycleEnd) {

        if (requireAnyConditions && conditions().isEmpty())
            fail("no conditions tested");

        score = -cycleEnd; //default score
        score = Math.min(-1, cycleEnd);

        nar.onCycle((Consumer) new StopIfAnyFail());
        if (exitOnAllSuccess)
            nar.onCycle((Consumer) new StopIfAllSucceed());

        long startTime = nar.time();

        runUntil(cycleEnd);

        long endTime = nar.time();
        int runtime = Math.max(0, (int) (endTime - startTime));

        boolean success = _success();

        if (success) {
            score = -runtime;
        }

        if (trace != null)
            logger.trace("{}", trace.getBuffer());

        if (report || !success)
            report(this.toString(), logger);

        assertTrue(success);
    }

    public boolean _success() {
        return succeedsIfAll.AND(NARCondition::getAsBoolean)
                &&
                !failsIfAny.OR(NARCondition::getAsBoolean);
    }

    public void report(String id, Logger logger) {
        logger.info("{}\n", id);

        for (NARCondition t : conditions())
            t.log(logger);

        nar.stats(true, true, logger);
    }

    private TestNAR runUntil(long finalCycle) {
        while (!finished && nar.time() < finalCycle)
            nar.run();

        return this;
    }

    public TestNAR input(String... s) {
        finished = false;
        for (String x : s)
            try {
                nar.input(x);
            } catch (Narsese.NarseseException e) {
                fail(e::toString);
            }
        return this;
    }

    public TestNAR input(Task... s) {
        finished = false;
        for (Task x : s) {
            if (x.pri() == 0 || x.isDeleted())
                throw new RuntimeException("input task has zero or deleted priority");
            nar.input(x);
        }
        return this;
    }

    /**
     * warning may not work with time=0
     */

    public TestNAR inputAt(long time, String s) {
        finished = false;
        nar.inputAt(time, s);
        return this;
    }

    public TestNAR inputAt(long time, Task... t) {
        finished = false;
        nar.inputAt(time, t);
        return this;
    }

    public TestNAR believe(String t, Tense tense, float f, float c) {
        finished = false;
        nar.believe(t, tense, f, c);
        return this;
    }

    public TestNAR believe(String x, float f, float c, long s, long e) {
        finished = false;
        nar.believe(x, f, c, s, e);
        return this;
    }

    public TestNAR goal(String x, float f, float c, long s, long e) {
        finished = false;
        nar.want(x, f, c, s, e);
        return this;
    }

    public TestNAR goal(String t, Tense tense, float f, float c) {
        finished = false;
        try {
            nar.want($.$(t), f, c, tense);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public TestNAR goal(String s) {
        nar.want(s);
        return this;
    }

    public TestNAR log() {
        nar.log();
        report = true;
        return this;
    }

    public TestNAR logDebug() {
        if (!NAL.DEBUG) {
            logger.warn("WARNING: debug mode enabled statically");
            NAL.DEBUG = true;
        }
        return log();
    }

    /**
     * fails if anything non-input is processed
     */

    public TestNAR mustOutputNothing(int cycles) {
        //TODO use LambaTaskCondition
        requireAnyConditions = false;
        focus.eventTask.on(c -> {
            if (c instanceof NALTask && !((NALTask) c).isInput())
                fail(() -> c + " output, but must not output anything");
        });

        cycleEnd = Math.max(cycleEnd, cycles);
        return this;
    }

    public TestNAR dur(int newDur) {
        nar.time.dur(newDur);
        return this;
    }

    public void stop() {
        finished = true;
    }

    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate occ) {
        return mustEmit(focus.eventTask, cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, (s, e) -> occ.test(s) && occ.test(e));
    }

    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long occ) {
        return mustOutput(cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, occ, occ);
    }

    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long start, long end) {
        return mustOutput(cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, (s, e) -> start == s && end == e);
    }

    public TestNAR mustOutput(long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time) {
        return mustEmit(focus.eventTask, cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, time);
    }

    public TestNAR mustOutput(long cyclesAhead, String task) {
        try {
            return mustEmit(focus.eventTask, cyclesAhead, task);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }

    private TestNAR mustEmit(ByteTopic<Task> c, long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time) {
        try {
            return mustEmit(c, cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, time, true);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }

    private TestNAR mustEmit(ByteTopic<Task> c, long cyclesAhead, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time, boolean must) throws Narsese.NarseseException {
        long now = time();
        cyclesAhead = Math.round(cyclesAhead * NAL.test.TIME_MULTIPLIER);
        return mustEmit(c, now, now + cyclesAhead, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, time, must);
    }

    private TestNAR mustEmit(ByteTopic<Task> c, long cycleStart, long cycleEnd, String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate time, boolean mustOrMustNot) throws Narsese.NarseseException {


        if (freqMin == -1)
            freqMin = freqMax;

        Term term = $.$(sentenceTerm);
        int tv = term.complexity();
        int tvMax = focus.complexMax();
        if (tv > tvMax)
            throw new TermException("condition term volume (" + tv + ") exceeds volume max (" + tvMax + ')', term);


        float hf = freqTolerance, hc = confTolerance;

        return must(c, punc, mustOrMustNot,
                new DerivedTaskCondition(nar,
                        cycleStart, cycleEnd,
                        term, punc,
                        freqMin - hf, freqMax + hf,
                        confMin - hc, confMax + hc, time));


    }

    public TestNAR must(byte punc, Predicate<NALTask> tc) {
        return must(punc, true, tc);
    }

    public TestNAR mustNot(byte punc, Predicate<NALTask> tc) {
        return must(punc, false, tc);
    }

    public TestNAR must(byte punc, boolean mustOrMustNot, Predicate<NALTask> tc) {
        return must(punc, mustOrMustNot, new LambdaTaskCondition(tc));
    }

    public TestNAR must(byte punc, boolean mustOrMustNot, TaskCondition tc) {
        return must(focus.eventTask, punc, mustOrMustNot, tc);
    }

    public TestNAR must(ByteTopic c, byte punc, boolean mustOrMustNot, TaskCondition tc) {

        c.on(x -> tc.test((NALTask) x), punc);

        if (report && tc instanceof DerivedTaskCondition)
            ((DerivedTaskCondition) tc).similars(maxSimilars);

        finished = false;

        if (mustOrMustNot) {
            succeedsIfAll.add(tc);
        } else {
            exitOnAllSuccess = false;
            failsIfAny.add(tc);
        }

        return this;
    }

    public final long time() {
        return nar.time();
    }

    private TestNAR mustEmit(ByteTopic<Task> c, long cyclesAhead, String task) throws Narsese.NarseseException {
        NALTask t = Narsese.task(task, nar);


        String termString = t.term().toString();
        float freq, conf;
        if (t.truth() != null) {
            freq = t.freq();
            conf = (float) t.conf();

        } else {
            freq = conf = NaN;
        }

        return mustEmit(c, cyclesAhead, termString, t.punc(), freq, freq, conf, conf, (s, e) -> s == t.start() && e == t.end());
    }

    public TestNAR mustOutput(long cyclesAhead, String term, byte punc, float freq, float conf) {
        return mustOutput(cyclesAhead, term, punc, freq, freq, conf, conf, ETERNAL);
    }

    public TestNAR mustBelieve(long cyclesAhead, String term, float freqMin, float freqMax, float confMin, float confMax) {
        return mustBelieve(cyclesAhead, term, freqMin, freqMax, confMin, confMax, ETERNAL);
    }

    public TestNAR mustBelieve(long cyclesAhead, String term, float freqMin, float freqMax, float confMin, float confMax, long when) {
        return mustOutput(cyclesAhead, term, BELIEF, freqMin, freqMax, confMin, confMax, when);
    }

    public TestNAR mustBelieve(long cyclesAhead, String term, float freq, float confidence, Tense t) {
        return mustOutput(cyclesAhead, term, BELIEF, freq, freq, confidence, confidence, nar.time(t));
    }

    /**
     * tests for any truth value at the given occurrences
     */

    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, long occ) {


        mustNotOutput(cyclesAhead, term, punc, 0.0f, 1.0f, 0.0f, 1.0f, occ);
        return this;
    }

    public TestNAR mustNotQuestion(long cyclesAhead, String term) {
        return mustNotOutput(cyclesAhead, term, QUESTION);
    }

    public TestNAR mustNotQuest(long cyclesAhead, String term) {
        return mustNotOutput(cyclesAhead, term, QUEST);
    }

    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc) {
        return mustNotOutput(cyclesAhead, term, punc, (t) -> true);
    }

    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, LongPredicate occ) {


        mustNotOutput(cyclesAhead, term, punc, 0.0f, 1.0f, 0.0f, 1.0f, occ);
        return this;
    }

    public TestNAR mustNotBelieve(long cyclesAhead, String term, float freqMin, float freqMax) {
        return mustNotOutput(cyclesAhead, term, BELIEF, freqMin, freqMax);
    }

    public TestNAR mustNotGoal(long cyclesAhead, String term, float freqMin, float freqMax) {
        return mustNotOutput(cyclesAhead, term, GOAL, freqMin, freqMax);
    }

    public TestNAR mustNotGoal(long cyclesAhead, String term, float freqMin, float freqMax, float confMin, float confMax) {
        return mustNotOutput(cyclesAhead, term, GOAL, freqMin, freqMax, confMin, confMax);
    }

    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, float freqMin, float freqMax) {
        return mustNotOutput(cyclesAhead, term, punc, freqMin, freqMax, 0, 1);
    }

    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, float freqMin, float freqMax, float confMin, float confMax) {
        return mustNotOutput(cyclesAhead, term, punc, freqMin, freqMax, confMin, confMax, t -> true);
    }

    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, float freqMin, float freqMax, float confMin, float confMax, long occ) {
        LongPredicate badTime = (l) -> l == occ;
        return mustNotOutput(cyclesAhead, term, punc, freqMin, freqMax, confMin, confMax, badTime);
    }

    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongPredicate badTimes) {
        return mustNotOutput(cyclesAhead, term, punc, freqMin, freqMax, confMin, confMax, (s, e) -> badTimes.test(s) || (s != e && badTimes.test(e)));
    }

    public TestNAR mustNotOutput(long cyclesAhead, String term, byte punc, float freqMin, float freqMax, float confMin, float confMax, LongLongPredicate timeFilter) {
        if (freqMin < 0 || freqMin > 1.0f || freqMax < 0 || freqMax > 1.0f || confMin < 0 || confMin > 1.0f || confMax < 0 || confMax > 1.0f || freqMin != freqMin || freqMax != freqMax)
            throw new UnsupportedOperationException();

        try {
            return mustEmit(focus.eventTask,
                    cyclesAhead,
                    term, punc,
                    freqMin, freqMax, confMin, confMax,
                    timeFilter, false);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }

    }

    public TestNAR mustBelieve(long cyclesAhead, String term, float freq, float confidence, long when) {
        return mustOutput(cyclesAhead, term, BELIEF, freq, freq, confidence, confidence, when);
    }

    public TestNAR mustBelieveAtOnly(long cyclesAhead, String term, float freq, float confidence, long startTime) {
        return mustBelieveAtOnly(cyclesAhead, term, freq, confidence, startTime, startTime);
    }

    public TestNAR mustBelieveAtOnly(long cyclesAhead, String term, float freq, float confidence, long startTime, long endTime) {
        mustBelieve(cyclesAhead, term, freq, confidence, startTime);
        return mustNotBelieve(cyclesAhead, term, (s, e) -> s != startTime || e != endTime);
    }

    public TestNAR mustNotBelieve(long cyclesAhead, String term, float freq, float confidence, LongLongPredicate occTimeAbsolute) {
        return mustNotOutput(cyclesAhead, term, BELIEF, freq, freq, confidence, confidence, occTimeAbsolute);
    }

    public TestNAR mustNotBelieve(long cyclesAhead, String term) {
        return mustNotBelieve(cyclesAhead, term, (s, e) -> true);
    }

    public TestNAR mustNotGoal(long cyclesAhead, String term) {
        return mustNotGoal(cyclesAhead, term, (s, e) -> true);
    }

    public TestNAR mustNotBelieve(long cyclesAhead, String term, LongLongPredicate occTimeAbsolute) {
        return mustNotOutput(cyclesAhead, term, BELIEF, 0, 1, 0, 1, occTimeAbsolute);
    }

    public TestNAR mustNotGoal(long cyclesAhead, String term, LongLongPredicate occTimeAbsolute) {
        return mustNotOutput(cyclesAhead, term, GOAL, 0, 1, 0, 1, occTimeAbsolute);
    }

    public TestNAR mustBelieve(long cyclesAhead, String term, float freq, float confidence, long start, long end) {
        return mustOutput(cyclesAhead, term, BELIEF, freq, freq, confidence, confidence, start, end);
    }

    public TestNAR mustBelieve(long cyclesAhead, String term, float freq, float confidence) {
        return mustBelieve(cyclesAhead, term, freq, confidence, ETERNAL);
    }

    public TestNAR mustBelieve(long cyclesAhead, String term, float confidence) {
        return mustBelieve(cyclesAhead, term, 1.0f, confidence);
    }

    public TestNAR mustGoal(long cyclesAhead, String goalTerm, float freq, float conf) {
        return mustOutput(cyclesAhead, goalTerm, GOAL, freq, conf);
    }

    public TestNAR mustQuestion(long cyclesAhead, String qt) {
        return mustOutput(cyclesAhead, qt, QUESTION);
    }

    public TestNAR mustQuestion(long cyclesAhead, String qt, LongLongPredicate when) {
        return mustOutput(cyclesAhead, qt, QUESTION, 0, 1, 0, 1, when);
    }

    public TestNAR mustQuest(long cyclesAhead, String qt) {
        return mustOutput(cyclesAhead, qt, QUEST);
    }

    public TestNAR mustOutput(long cyclesAhead, String qt, byte punc) {
        return mustOutput(cyclesAhead, qt, punc, 0, 1, 0, 1, t -> true);
    }

    public TestNAR mustGoal(long cyclesAhead, String goalTerm, float freq, float conf, long occ) {
        return mustOutput(cyclesAhead, goalTerm, GOAL, freq, freq, conf, conf, occ);
    }

    public TestNAR mustGoal(long cyclesAhead, String goalTerm, float freq, float conf, LongPredicate occ) {
        return mustOutput(cyclesAhead, goalTerm, GOAL, freq, freq, conf, conf, occ);
    }

    public TestNAR mustGoal(long cyclesAhead, String goalTerm, float freq, float conf, LongLongPredicate occ) {
        return mustOutput(cyclesAhead, goalTerm, GOAL, freq, freq, conf, conf, occ);
    }

    public TestNAR mustGoal(long cyclesAhead, String goalTerm, float freq, float conf, long start, long end) {
        return mustOutput(cyclesAhead, goalTerm, GOAL, freq, freq, conf, conf, start, end);
    }

    public TestNAR mustBelieve(long cyclesAhead, String goalTerm, float freq, float conf, LongPredicate occ) {
        return mustOutput(cyclesAhead, goalTerm, BELIEF, freq, freq, conf, conf, occ);
    }

    public TestNAR mustBelieve(long cyclesAhead, String goalTerm, float freq, float conf, LongLongPredicate occ) {
        return mustOutput(cyclesAhead, goalTerm, BELIEF, freq, freq, conf, conf, occ);
    }

    public TestNAR question(String questionString) {
        nar.question($$(questionString));
        return this;
    }

    public TestNAR quest(String questString) {
        nar.quest($$(questString));
        return this;
    }

    public TestNAR askAt(int i, String term) {
        try {
            nar.inputAt(i, term + '?');
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * TODO make this throw NarseseException
     */

    public TestNAR believe(String termString) {
        try {
            nar.believe(termString);
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        } /*catch (RuntimeException e) {
            throw new RuntimeException(e);
        }*/
        return this;
    }

    public TestNAR believe(String termString, float freq, float conf) {
        nar.believe(termString, freq, conf);
        return this;
    }

    public final TestNAR volMax(int i) {
        nar.complexMax.set(i);
        return this;
    }

    public final TestNAR confMin(float c) {
        nar.confMin.set(c);
        return this;
    }

    public final TestNAR freqRes(float r) {
        nar.freqRes.set(r);
        return this;
    }

    @Override
    protected void starting(NAR nar) {
        this.nar = nar;
        nar.focus.add(focus);
    }

    @Override
    protected void stopping(NAR nar) {
        nar.focus.remove(focus);
        this.nar = null;
    }

    public TestNAR confRes(double r) {
        nar.confRes.set(r);
        return this;
    }


//    static class TestNARResult implements Serializable {
//        public final boolean error;
//        final boolean success;
//
//        TestNARResult(boolean success, boolean error) {
//            this.success = success;
//            this.error = error;
//        }
//        //TODO long wallTimeNS;
//        //TODO etc
//    }

    abstract static class IfEveryCycle implements Consumer<NAR>, BooleanSupplier, Runnable {
        final int testPeriodCycles;
        int cycle;

        IfEveryCycle() {
            this(1);
        }

        IfEveryCycle(int testPeriodCycles) {
            this.testPeriodCycles = testPeriodCycles;
        }

        @Override
        public final void accept(NAR nar) {

            if (++cycle % testPeriodCycles == 0) {
                if (getAsBoolean())
                    run();
            }

        }

        public abstract boolean getAsBoolean();
    }

    private final class StopIfAllSucceed extends IfEveryCycle {
        @Override
        public void run() {
            stop();
        }

        @Override
        public boolean getAsBoolean() {
            return succeedsIfAll.AND(BooleanSupplier::getAsBoolean);
        }
    }

    private final class StopIfAnyFail extends IfEveryCycle {
        @Override
        public void run() {
            stop();
        }

        @Override
        public boolean getAsBoolean() {
            return failsIfAny.OR(BooleanSupplier::getAsBoolean);
        }
    }
}