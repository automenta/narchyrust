package nars.nal.nal3;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;

@Disabled class NAL3ComposeTest extends NAL3Test {
    @Test
    void compound_composition_two_premises() {

        test.believe("(swan --> swimmer)", 0.9f, 0.9f);
        test.believe("(swan --> bird)", 0.8f, 0.9f);
        //tester.mustBelieve(cycles, "(swan --> (bird | swimmer))", 0.98f, 0.81f);
        test.mustBelieve(cycles, "(swan --> (bird & swimmer))", 0.72f, 0.81f);

    }
    @Disabled @Test
    void compound_composition_two_premises_recurse() {
        test.believe("(swan --> (bird & swimmer))", 0.9f, 0.9f);
        test.believe("(swan --> reptile)", 0.2f, 0.9f);
        test.mustBelieve(cycles, "(swan --> ((bird & swimmer) - reptile) )", 0.72f, 0.81f);
    }
    @Disabled @Test
    void compound_composition_two_premises_recurse2() {
        test.believe("(swan --> (bird & swimmer))", 0.9f, 0.9f);
        test.believe("(swan --> (reptile & swimmer))", 0.2f, 0.9f);
        test.mustBelieve(cycles, "(swan --> ((bird&swimmer) - (reptile&swimmer)) )", 0.72f, 0.81f);
    }

    @Test
    void compound_composition_two_premises2() {

        test.volMax(8);
        test.believe("(sport --> competition)", 0.9f, 0.9f);
        test.believe("(chess --> competition)", 0.8f, 0.9f);
        //test.mustBelieve(cycles, "((chess & sport) --> competition)", 0.72f, 0.81f);
        test.mustBelieve(cycles, "((chess | sport) --> competition)", 0.98f, 0.81f);
    }

    @Test
    void compound_composition_two_premises_subj_intersection_and_union() {

        test.volMax(8);
        test.believe("(sport --> competition)", 0.9f, 0.9f);
        test.believe("(chess --> competition)", 0.8f, 0.9f);
        //test.ask("((chess|sport) --> competition)");
        test.mustBelieve(cycles, "((chess | sport) --> competition)", 0.98f, 0.81f);
        //test.mustBelieve(cycles, "((chess & sport) --> competition)", 0.72f, 0.81f);
    }

    @Test
    void compound_composition_two_premises_pred_intersection_and_union() {
        test.volMax(8);
        test.believe("(competition --> sport)", 0.9f, 0.9f);
        test.believe("(competition --> chess)", 0.8f, 0.9f);
//        test.ask("(competition --> (chess|sport))");
        //test.mustBelieve(cycles, "(competition --> (chess | sport))", 0.98f, 0.81f);
        test.mustBelieve(cycles, "(competition --> (chess & sport))", 0.72f, 0.81f);
    }

    @Test
    void intersectionComposition() {
        test
                .believe("(swan --> bird)")
                .believe("(swimmer--> bird)")
                .mustBelieve(cycles, "((swan|swimmer) --> bird)", 1f, 0.81f);
    }

    @Test
    void intersectionCompositionWrappedInProd() {
        test
                .volMax(10)
                .believe("((swan) --> bird)")
                .believe("((swimmer)--> bird)")
                .mustBelieve(cycles, "(((swan)|(swimmer)) --> bird)", 1f, 0.81f);
    }

    @Disabled
    @Test
    void composeConjSimilarity_dont_introduce_xternal() {
//        test.nar.onCycle(()->{
//            ((TaskLinkWhat)test.nar.what()).links.links.print();
//        });
        test
            .volMax(5)
            .believe("(x --> a)")
            .believe("(x --> b)")
            .believe("(y <-> x)")
            .mustBelieve(cycles, "(y <-> (a&&b))", 1f, 0.81f)
            .mustNotOutput(cycles, "(y <-> (a &&+- b))", BELIEF);
    }

    @Test void composeQuestion_pred() {
        test
                .volMax(8)
                .question("(x-->a)")
                .believe("(x-->b)")
                .mustQuestion(cycles,"(x-->(a&b))")
                .mustQuestion(cycles,"(x-->(a|b))")
        ;
    }
    @Test void composeQuest_pred() {
        test
                .volMax(8)
                .quest("(x-->a)")
                .believe("(x-->b)")
                .mustQuest(cycles,"(x-->(a&b))")
                .mustQuest(cycles,"(x-->(a|b))")
        ;
    }
    @Disabled @Test void composeQuestion_sim() {

        test
                .volMax(8)
                .believe("(x-->(a&&b))")
                .question("(x-->a)")
                .mustQuestion(cycles,"(a<->b)")
        ;
    }
}