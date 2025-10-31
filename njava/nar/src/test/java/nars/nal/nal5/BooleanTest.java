package nars.nal.nal5;

import com.google.common.base.Joiner;
import nars.*;
import nars.concept.TaskConcept;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.IntFunction;

import static nars.$.$$;
import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * NAL5 Boolean / Boolean Satisfiability / Boolean Conditionality
 */
class BooleanTest {

    final NAR n = NARS.tmp(6);

    private void testSAT2Individual(int i, int j) throws Narsese.NarseseException {


        n.freqRes.set(0.05f);
        n.confRes.set(0.05f);
        n.confMin.set(0.02f);
        n.complexMax.set(7);


        String[] outcomes = {
                "a",
                "b",
                "c",
                "d"};


        n.believe("( (--i && --j) ==> " + outcomes[0] + ')');
        n.believe("( (--i && j) ==> " + outcomes[1] + ')');
        n.believe("( (i && --j) ==> " + outcomes[2] + ')');
        n.believe("( (i && j) ==> " + outcomes[3] + ')');

        Term I = $.$("i").negIf(i == 0);
        Term J = $.$("j").negIf(j == 0);


        n.believe(CONJ.the(I, J));


        n.run(64);

        //System.out.println(i + " " + j + ' ');
        final float confThresh = 0.7f;
        for (int k = 0, outcomesLength = outcomes.length; k < outcomesLength; k++) {
            String s = outcomes[k];
            Concept dc = n.conceptualize(s);
            assertNotNull(dc);
            @Nullable NALTask t = n.belief(dc, n.time());
            Truth b = t != null ? t.truth() : null;

            //System.out.println("\t" + dc.term() + '\t' + s + '\t' + b + '\t' + outcomes[k]);


            int ex = -1, ey = -1;
            switch (k) {
                case 0 -> {
                    ex = 0;
                    ey = 0;
                }
                case 1 -> {
                    ex = 0;
                    ey = 1;
                }
                case 2 -> {
                    ex = 1;
                    ey = 0;
                }
                case 3 -> {
                    ex = 1;
                    ey = 1;
                }
            }
            boolean positive = ((ex == i) && (ey == j));
            if (positive == (b == null)) {
                if (positive)
                    fail("unrecognized true case");
                else if (b.conf() > confThresh)
                    fail("invalid false impl subj deriving a pred");
            }


            if (positive && b.NEGATIVE() && b.conf() > confThresh) fail(() -> "wrong true case:\n" + t.proof());
            if (!positive && b != null && b.POSITIVE() && b.conf() > confThresh)
                fail(() -> "wrong false case:\n" + t.proof());

        }


    }

    static void testSATRandom(boolean beliefOrGoal, boolean temporalOrEternal, int dim) {

        int volMax = 5;
        int c = dim * 100;
        int cRemoveInputs = c * 1 / 4;

        IntFunction<Term> termizer =
                //(i)->$$("x" + i);
                (i) -> $.inh($.the(i), "x");

        NAR n = NARS.tmp(/*6,8*/);
        n.complexMax.set(volMax);

        Set<NALTask> inputs = new LinkedHashSet();

        boolean[] b = new boolean[dim];
        Term[] t = new Term[dim];
        for (int i = 0; i < dim; i++) {
            b[i] = n.random().nextBoolean();
            Term what = (t[i] = termizer.apply(i)).negIf(!b[i]);
            Tense when = temporalOrEternal ? Tense.Present : Tense.Eternal;
            inputs.add(beliefOrGoal ?
                    n.believe(what, when, 1f, 0.9f) :
                    n.want(what, 1f, 0.9f, when));
            if (temporalOrEternal)
                n.run(1); //stagger input temporally
        }
        Truth[] r = new Truth[dim];
        for (int i = 0; i < c; i++) {

            n.run();

            if (i == 0 || i == cRemoveInputs) {
                float bConf = n.confDefault(BELIEF);
                //get all structural transformed inputs (ex: images)
                n.tasks().filter(z -> (beliefOrGoal ? z.BELIEF() : z.GOAL()) && (float) z.conf() >= bConf).forEach(inputs::add);
                for (NALTask z : inputs) {
                    n.concept(z).remove(z);
                    z.delete();
                }
            }

            if (i >= cRemoveInputs) {
                for (int j = 0; j < dim; j++) {
                    long when =
                            //temporalOrEternal ? n.time() : ETERNAL;
                            ETERNAL;

                    Truth tj = beliefOrGoal ? n.beliefTruth(t[j], when) : n.goalTruth(t[j], when);

                    if (i == c - 1) {
                        //last cycle:
                        int J = j;

                        assertNotNull(tj, () -> t[J] + " without truth @ " + when + "\n" +
                                Joiner.on("\n").join(((TaskConcept) n.concept(t[J])).table(beliefOrGoal ? BELIEF : GOAL).taskStream().iterator()));
                    }

                    if (tj != null) {
                        r[j] = tj;
                        assertEquals(b[j], r[j].POSITIVE());
                    }
                }
            }
        }
        for (int i = 0; i < dim; i++) {
//            System.out.println(t[i] + " " + b[i] + ' ' + r[i]);
            int ii = i;
            n.concept(t[i])
                    .tasks()
                    .filter(z -> beliefOrGoal ? z.BELIEF() : z.GOAL())
                    .forEach(z -> {
                        System.out.println(z);
                        assertEquals(b[ii], z.POSITIVE(), z::proof);
                    });
        }


    }

    @Test
    void testSAT2Individual00() throws Narsese.NarseseException {
        testSAT2Individual(0, 0);
    }

    @Test
    void testSAT2Individual01() throws Narsese.NarseseException {
        testSAT2Individual(0, 1);
    }

    @Test
    void testSAT2Individual10() throws Narsese.NarseseException {
        testSAT2Individual(1, 0);
    }


    @Test
    void testSAT2Individual11() throws Narsese.NarseseException {
        testSAT2Individual(1, 1);
    }

    @Test void XOR_PP() throws Narsese.NarseseException {
        xorTest(true, true);
    }
    @Test void XOR_PN() throws Narsese.NarseseException {
        xorTest(true, false);
    }
    @Test void XOR_NP() throws Narsese.NarseseException {
        xorTest(false, true);
    }
    @Test void XOR_NN() throws Narsese.NarseseException {
        xorTest(false, false);
    }

    private void xorTest(boolean xp, boolean yp) throws Narsese.NarseseException {

        n.confMin.set(0.75f);
        n.complexMax.set(7);
        n.believe("((  x &&   y) ==> --z)");
        n.believe("((--x &&   y) ==>   z)");
        n.believe("((  x && --y) ==>   z)");
        n.believe("((--x && --y) ==> --z)");
        n.run(2);

        assertNull(n.beliefTruth("z", ETERNAL));

        n.believe(CONJ.the($$("x").negIf(!xp), $$("y").negIf(!yp)) );
        n.run(25);

        Truth z = n.beliefTruth("z", ETERNAL);
        assertNotNull(z);
        assertTrue(z.negIf(xp == yp).freq() > 0.75f);
    }

    /** TODO */
    @Disabled
    @Test
    void testConditionalImplication() {
        boolean[] booleans = {true, false};
        Term x = $.atomic("x");
        Term y = $.atomic("y");
        Term[] concepts = {x, y};

        for (boolean goalSubjPred : booleans) {


            for (boolean subjPolarity : booleans) {
                for (boolean predPolarity : booleans) {
                    for (boolean goalPolarity : booleans) {

                        Term goal = (goalSubjPred ? x : y).negIf(!goalPolarity);
                        Term condition = $.impl(x.negIf(!subjPolarity), y.negIf(!predPolarity));

                        NAR n = NARS.tmp();
                        n.want(goal);
                        n.believe(condition);
                        n.run(128);

                        System.out.println(goal + "!   " + condition + '.');
                        for (Term t : concepts) {
                            if (!t.equals(goal.unneg()))
                                System.out.println("\t " + t + "! == " + n.goalTruth(t, ETERNAL));
                        }
                        System.out.println();

                    }
                }

            }

        }
    }

//    @Disabled
//    @Test
//    void testXOREternal() throws Narsese.NarseseException {
//        //classic XOR example
//
//        NAR n = NARS.tmp();
//        //n.log();
//        n.volMax.set(8);
//        n.believe("--(  x &&   y)");
//        n.believe("  (  x && --y)");
//        n.believe("  (--x &&   y)");
//        n.believe("--(--x && --y)");
//        n.run(1600);
//
//        Concept a = n.concept("(x && y)");
//
//        Concept b = n.concept("(x && --y)");
//
//        Concept c = n.concept("(--x && y)");
//
//        Concept d = n.concept("(--x && --y)");
//
//
////        for (Concept x : new Concept[]{ a,b,c,d}) {
////            x.print();
////            x.beliefs().forEachTask(t -> System.out.println(t.proof()));
////            System.out.println();
////        }
//
//    }

    @Disabled
    @Test
    void SATRandomBeliefEternal() {
        testSATRandom(true, false, 4);
    }

    @Disabled
    @Test
    void SATRandomBeliefTemporal() {
        testSATRandom(true, true, 3);
    }

    @Disabled
    @Test
    void SATRandomGoalEternal() {
        testSATRandom(false, false, 4);
    }

    @Disabled
    @Test
    void SATRandomGoalTemporal() {
        testSATRandom(false, true, 6);
    }
}