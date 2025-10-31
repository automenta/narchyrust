package nars.nal.nal1;

import nars.*;
import nars.term.atom.Bool;
import nars.test.TestNAR;
import nars.test.impl.DeductiveMeshTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.DoubleSummaryStatistics;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

import static nars.Op.BELIEF;
import static nars.Op.ETERNAL;
import static nars.term.Functor.f;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 5/24/16.
 */
public class QuestionTest {

    final NAR nar = NARS.tmp(1);

    private static final int cycles = 250;

    @Test
    void whQuestionUnifyQueryVar() throws Narsese.NarseseException {
        testQuestionAnswer(cycles, "(bird-->swimmer)", "(?x --> swimmer)", "(bird-->swimmer)");
    }

    @Test
    void yesNoQuestion() throws Narsese.NarseseException {
        testQuestionAnswer(cycles, "(bird-->swimmer)", "(bird-->swimmer)", "(bird-->swimmer)");
    }

    @Test
    void testTemporalExact() throws Narsese.NarseseException {
        testQuestionAnswer(cycles,
                "((a &&+1 b) &&+1 c)",
                "((a &&+1 b) &&+1 c)",
                "((a &&+1 b) &&+1 c)");
    }

    /**
     * question to answer matching
     */
    private void testQuestionAnswer(int cycles, String belief, String question, String expectedSolution) throws Narsese.NarseseException {
        AtomicInteger ok = new AtomicInteger(0);


        Term expectedSolutionTerm = $.$(expectedSolution);


        nar.main().eventTask.on(a -> {
            if (a.punc() == BELIEF && a.term().equals(expectedSolutionTerm))
                ok.incrementAndGet();
        });

        nar.believe(belief, 1.0f, 0.9f);
        nar.question(question);



        nar.run(cycles);


        assertTrue(ok.get() > 0);


    }


    /**
     * tests whether the use of a question guides inference as measured by the speed to reach a specific conclusion
     */
    @Disabled @Test
    void questionDrivesInference() {

        int[] dims = {3, 2};
        final int timelimit = 2400;

//        TaskStatistics withTasks = new TaskStatistics();
//        TaskStatistics withoutTasks = new TaskStatistics();
        DoubleSummaryStatistics withTime = new DoubleSummaryStatistics();
        DoubleSummaryStatistics withOutTime = new DoubleSummaryStatistics();

        IntFunction<NAR> narProvider = (seed) -> {
            NAR d = NARS.tmp(1);
            //d.random().setSeed(seed);
            d.complexMax.set(7);
            d.freqRes.set(0.25f);
            return d;
        };

        BiFunction<Integer, Integer, TestNAR> testProvider = (seed, variation) -> {
            NAR n = narProvider.apply(seed);
            TestNAR t = new TestNAR(n);
            switch (variation) {
                case 0, 1 -> new DeductiveMeshTest(t, dims, timelimit);
            }
            return t;
        };

        for (int i = 0; i < 1 /* seed doesnt do anything right now 10 */; i++) {
            int seed = i + 1;

            TestNAR withQuestion = testProvider.apply(seed, 0);
            withQuestion.run();
            withTime.accept(withQuestion.time());
//            withTasks.addAll(withQuestion.nar);

            TestNAR withoutQuestion = testProvider.apply(seed, 1);
            withoutQuestion.run();
            withOutTime.accept(withoutQuestion.time());
//            withoutTasks.addAll(withoutQuestion.nar);
        }

//        withTasks.print();
//        withoutTasks.print();

        assertNotEquals(withTime, withOutTime);
//        System.out.println("with: " + withTime);
//        System.out.println("withOut: " + withOutTime);


    }


    @Test
    @Disabled
    void testMathBackchain() throws Narsese.NarseseException {
        NAR n = NARS.tmp(1);


        n.add(f("odd", a -> {
            if (a.subs() == 1 && a.sub(0).ATOM()) {
                try {
                    return $.intValue(a.sub(0)) % 2 == 0 ? Bool.False : Bool.True;
                } catch (NumberFormatException ignored) {

                }
            }
            return null;
        }));
        n.complexMax.set(24);
        n.input(
                "({1,2,3,4} --> number).",
                "((({#x} --> number) && odd(#x)) ==> ({#x} --> ODD)).",
                "((({#x} --> number) && --odd(#x)) ==> ({#x} --> EVEN)).",
                "({#x} --> ODD)?",
                "({#x} --> EVEN)?"


        );
        n.run(2500);

    }

    @Disabled
    @Test
    void testDeriveQuestionOrdinary() {
        new TestNAR(NARS.tmp(1))
                .question("((S | P) --> M)")
                .believe("(S --> M)")
                .mustQuestion(512, "(P --> M)").run();
    }

    @Disabled
    @Test
    void testDeriveQuestOrdinary() {
        new TestNAR(nar)
                .quest("((S | P) --> M)")
                .believe("(S --> M)")
                .mustQuest(256, "(P --> M)").run();
    }

    @Disabled
    @Test
    void testExplicitEternalizationViaQuestion() {
        /*ETERNAL*/
        /*temporal*/
        new TestNAR(nar)
                .inputAt(1, "x. | %1.00;0.90%")
                .inputAt(4, "x. | %0.50;0.90%")
                .inputAt(7, "x. | %0.00;0.90%")
                .inputAt(8, "$1.0 x?")
                .mustBelieve(64, "x", 0.5f, 0.73f /*ETERNAL*/)
                .mustBelieve(64, "x", 0.5f, 0.9f, t -> t == 4 /*temporal*/).run();
    }

    @Disabled
    @Test
    void testExplicitEternalizationViaQuestionDynamic() {
        new TestNAR(nar)
                .inputAt(1, "x. | %1.00;0.90%")
                .inputAt(4, "y. | %1.00;0.90%")
                .inputAt(1, "$1.0 (x &&+3 y)? |")
                .inputAt(1, "$1.0 (x &&+3 y)?")


                .mustBelieve(64, "(x &&+3 y)", 1f, 0.45f, t -> t == ETERNAL)
                .mustBelieve(64, "(x &&+3 y)", 1f, 0.81f, t -> t == 1).run();
    }

}