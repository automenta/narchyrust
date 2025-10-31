package nars.nal.nal7;

import com.google.common.math.PairedStatsAccumulator;
import nars.*;
import nars.action.link.STMLinker;
import nars.action.transform.TemporalInduction;
import nars.deriver.reaction.ReactionModel;
import nars.time.Tense;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.DTERNAL;
import static nars.Op.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * tests the time constraints in which a repeatedly inducted
 * conj/impl belief can or can't "snowball" into significant confidence
 */
class RuleInductionTest {

    @Disabled
    @Test
    void test1() {

        NAR n = NARS.shell();
        n.complexMax.set(8);

        ReactionModel d = new NARS.Rules().core().stm().temporalInduction()
            .addAll(
                new TemporalInduction.ConjInduction(0, 0),
                new STMLinker(1, true, false, false, false)
            )
            .compile(n);

//        d.print();

        int dur = 2;
        n.time.dur(dur);



        int period = dur * 4;
        int dutyPeriod = period / 2;
        Term aConjB = $$("(a &&+" + dutyPeriod + " --a)");
        Term aConjB_root = aConjB.concept();
        Term aImpB = $$("(a ==>+" + dutyPeriod + " --a)");

        PairedStatsAccumulator aConjB_exp = new PairedStatsAccumulator();

//        Histogram aConjB_dt = new Histogram(4);

        //                aConjB_dt.recordValue(dt); //cant accept -dt
        n.main().eventTask.on(_t -> {
            NALTask t = (NALTask)_t;
            if (!t.isInput() && t.term().root().equals(aConjB_root)) {
                long start = t.start();
				int dt = Math.abs(t.term().dt());

//                aConjB_dt.recordValue(dt); //cant accept -dt

                assertEquals(start, t.end());
                assertNotEquals(ETERNAL, start);
                assertNotEquals(DTERNAL, dt);
            }
        });

        PairedStatsAccumulator aImpB_exp = new PairedStatsAccumulator();
        int loops = 10;
        for (int i = 0; i < loops; i++) {


            n.believe("a", Tense.Present, 1, 0.9f);
//            n.believe("b", Tense.Present, 0, 0.9f);
            n.run(dutyPeriod);
//            n.believe("b", Tense.Present, 1, 0.9f);
            n.believe("a", Tense.Present, 0, 0.9f);
            n.run(period - dutyPeriod);

            long now = n.time();

            System.out.println("\n" + now);
            aConjB_exp.add(now, observe(n, aConjB, now));

        }


        {
            System.out.println("<" + aConjB + " @ " + n.time() + '>');
            System.out.println("expectation vs. time: \t" + aConjB_exp.yStats());
            System.out.println("\tslope=" + aConjB_exp.leastSquaresFit().slope());


//            System.out.println("dt:");
//            Texts.histogramPrint(aConjB_dt, System.out);

            System.out.println("</>\n");
        }

//        double aConjB_pearsonCorrelationCoeff = aConjB_exp.pearsonsCorrelationCoefficient();
//        assertTrue(aConjB_pearsonCorrelationCoeff > 0.4f,
//                () -> aConjB + " confidence increases smoothly: correlation quality=" + aConjB_pearsonCorrelationCoeff);
//        assertTrue(aConjB_exp.leastSquaresFit().slope() > 0, () -> aConjB + " confidence increases");
//

    }


    private static double observe(NAR n, Term x, long now) {
        NALTask nb = n.belief(x, now);
        Truth xTruth = nb != null ? nb.truth(now, now, 0, 0, NAL.truth.EVI_MIN) : null;

        System.out.println(x + "\t" + xTruth);
        n.conceptualize(x).beliefs().print();
        System.out.println();


        return xTruth != null ? xTruth.expectation() : 0;
    }
}