package nars.nal.nal5;

import nars.NAL;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static nars.$.$$;
import static nars.Op.QUESTION;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AIMATests {

    private final NAR n = NARS.tmp();

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 0.05, 0.1/*, 0.25, 0.5*/})
    void testAIMAExample(double truthRes) throws Narsese.NarseseException {

        try (TestNAR t = new TestNAR(n)
                .volMax(5)
                .freqRes((float) truthRes)
                .confRes(0.1)
                .confMin(0.25f)
                .confTolerance(1)) {

            n.believe(
                    "(P ==> Q)",
                    "((L && M) ==> P)",
                    "((B && L) ==> M)",
                    "((A && P) ==> L)",
                    "((A && B) ==> L)",
                    "A",
                    "B");

            t.question("Q");

            t.mustBelieve((int) (NAL.test.TIME_MULTIPLIER * 600),
                    "Q", 1f, 0.5f
            );

            t.run();
        }


    }

    @Test
    void testWeaponsDomain_0() {
        int cycles = 64;
        try (TestNAR t = new TestNAR(n)) {
            t.volMax(21)
                    .confMin(0.5f)
                    .confTolerance(1)
                    .believe("((&&,Weapon(#y),Sells($x,#y,#z),Hostile(#z)) ==> Criminal($x))")
                    .input("Criminal(?x)?")
                    .mustQuestion(cycles, "(&&,Weapon(#y),Sells(?x,#y,#z),Hostile(#z))")
                    .mustNotOutput(cycles, "Criminal", QUESTION)
                    .run();
        }
    }

    @Test
    void testWeaponsDomain() {


//        n.freqResolution.set(0.25f);
//        n.confResolution.set(0.1f);
//        n.confMin.set(0.1f);

//        n.beliefPriDefault.amp(0.25f);
//        n.questionPriDefault.amp(0.5f);

//        n.log();

        int cycles = 100;

        assertEquals(20, $$("((&&,Weapon(#y),Sells($x,#y,#z),Hostile(#z)) ==> Criminal($x))").complexity());

        try (TestNAR t = new TestNAR(n)) {
            t
                    .volMax(22)
                    .believe("((&&,Weapon(#y),Sells($x,#y,#z),Hostile(#z)) ==> Criminal($x))")
                    .believe("Owns(Nono, M1)")
                    .believe("Missile(M1)")
                    .believe("((Missile($x) && Owns(Nono,$x)) ==> Sells(West,$x,Nono))")
                    .believe("(Missile($x) ==> Weapon($x))")
                    .believe("(Enemy($x,America) ==> Hostile($x))")
                    .believe("American(West)")
                    .believe("Enemy(Nono,America)")
                    .question("Criminal(?x)")
                    .mustBelieve(cycles, "Criminal(West)", 1, 0.5f)
                    .run();
        }

    }

//    private static void assertBelief(NAR n, boolean expcted, String x, int time) {
//
//        int metricPeriod = time / 4;
//
//        PairedStatsAccumulator timeVsConf = new PairedStatsAccumulator();
//
//
//        FloatArrayList evis = new FloatArrayList();
//        for (int i = 0; i < time; i += metricPeriod) {
//            n.run(metricPeriod);
//
//            NALTask y = n.belief($.the(x), i);
//            if (y == null)
//                continue;
//
//			float symConf = (float) y.conf();
//            assertTrue(y.POSITIVE() == expcted && y.polarity() > 0.5f);
//
//            evis.add((float) c2w(symConf, 1));
//            timeVsConf.add(i, symConf);
//        }
//
//
//        assertFalse(evis.isEmpty());
//
//
//        for (char c : "ABLMPQ".toCharArray()) {
//            Term t = $.the(String.valueOf(c));
//            Task cc = n.belief(t);
//            System.out.println(cc);
//        }
//        System.out.println(timeVsConf.yStats());
//        System.out.println(
//                SparkLine.renderFloats(evis)
//        );
//    }


}