package nars.nal.nal3;

import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class NAL3Extra extends NAL3Test {


    @Test
    void unionOfOppositesInt() {
        //Coincidentia oppositorum
        test
                .volMax(6)
                .believe("((  x&z)-->a)")
                .believe("((--x&y)-->a)")
                .mustBelieve(cycles, "((y&z)-->a)", 1f, 0.81f)
        ;
    }

    @Test
    void unionOfOppositesExt() {
        //Coincidentia oppositorum

        test
                .volMax(8)
                .believe("(a-->(  x|z))")
                .believe("(a-->(--x|y))")
                .mustBelieve(cycles, "(a-->(y|z))", 1f, 0.81f)
        ;
    }

    @Test
    void drilldown1() {

        test.volMax(10);
        test.believe("((x|y)-->z)");
        test.mustQuestion(cycles, "((|,x,y,?1)-->z)");

    }

    @Disabled @Test
    void composition_on_both_sides_of_a_statement() {

        test.believe("<bird --> animal>", 0.9f, 0.9f);
        test.question("((&,bird,swimmer) --> (&,animal,swimmer))");
        test.mustBelieve(cycles, "((&,bird,swimmer) --> (&,animal,swimmer))", 0.90f, 0.73f);

    }

    @Disabled
    @Test
    void composition_on_both_sides_of_a_statement_2() {

        test.believe("<bird --> animal>", 0.9f, 0.9f);
        test.question("<(|,bird,swimmer) --> (|,animal,swimmer)>");
        test.mustBelieve(cycles, "<(|,bird,swimmer) --> (|,animal,swimmer)>", 0.90f, 0.73f);

    /*<bird --> animal>. %0.9;0.9%
            <(|,bird,swimmer) --> (|,animal,swimmer)>?*/
    }

    @Disabled @Test
    void composition_on_both_sides_of_a_statement2() {

        test.believe("<bird --> animal>", 0.9f, 0.9f);
        test.question("<(-,swimmer,animal) --> (-,swimmer,bird)>");
        test.mustBelieve(cycles, "<(-,swimmer,animal) --> (-,swimmer,bird)>", 0.90f, 0.73f);

    }

    @Disabled @Test
    void composition_on_both_sides_of_a_statement2_2() {

        test.believe("<bird --> animal>", 0.9f, 0.9f);
        test.question("<(~,swimmer,animal) --> (~,swimmer,bird)>");
        test.mustBelieve(cycles, "<(~,swimmer,animal) --> (~,swimmer,bird)>", 0.90f, 0.73f);

    }
    @Disabled @Test
    void compound_composition_one_premise() {

        test.believe("<swan --> bird>", 0.9f, 0.9f);
        test.question("<swan --> (|,bird,swimmer)>");
        test.mustBelieve(cycles, "<swan --> (|,bird,swimmer)>", 0.90f, 0.73f);

    }

    @Disabled @Test
    void compound_composition_one_premise2() {
        test
                .believe("(swan --> bird)", 0.9f, 0.9f)
                .question("((swan&swimmer) --> bird)")
                .mustBelieve(cycles, "((swan&swimmer) --> bird)", 0.90f, 0.73f);
    }

    @Disabled @Test
    void compound_composition_one_premise3() {

        TestNAR tester = test;
        tester.believe("<swan --> bird>", 0.9f, 0.9f);
        tester.askAt(cycles / 2, "<swan --> (swimmer - bird)>");
        tester.mustBelieve(cycles, "<swan --> (swimmer - bird)>", 0.10f, 0.73f);

    }

    @Disabled @Test
    void compound_composition_one_premise4() {

        TestNAR tester = test;
        tester.believe("<swan --> bird>", 0.9f, 0.9f);
        tester.askAt(cycles / 2, "<(swimmer ~ swan) --> bird>");
        tester.mustBelieve(cycles, "<(swimmer ~ swan) --> bird>", 0.10f, 0.73f);

    }



}