package nars.nal.nal1;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class NAL1Test extends NALTest {

    protected static int cycles = 35;

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(1);
        n.complexMax.set(7);
        return n;
    }

    @Test
    void deduction() {
        test

                .believe("(bird --> animal)")
                /*.en("bird is a type of animal.")
                .es("bird es un tipo de animal.")
                .de("bird ist eine art des animal.");*/
                .believe("(robin --> bird)")

                .mustBelieve(cycles, "(robin --> animal)", 0.81f);
    }

    @Test
    void deductionReverseAndAbduction() {
        test
                .input("(b-->c).")
                .input("(a-->b).")
                .mustBelieve(cycles, "(a-->c)", 1f, 0.81f)
                .mustBelieve(cycles, "(c-->a)", 1f, 0.45f)
        ;
    }


    @Disabled @Test
    void revision() {

        String belief = "(bird --> swimmer)";

        test
                .mustBelieve(cycles, belief, 0.87f, 0.91f)
                .believe(belief, 1f, 0.90f)
                .believe(belief, 0.10f, 0.60f)
        ;
    }


    @Test
    void abduction() {
        /*
        <sport --> competition>. 'Sport is a type of competition.
        <chess --> competition>. %0.90% 'Chess is a type of competition.
        ''outputMustContain('<sport --> chess>. %1.00;0.42%') 'I guess sport is a type of chess.
        ''outputMustContain('<chess --> sport>. %0.90;0.45%') 'I guess chess is a type of sport.
         */
        test
            .volMax(3)
                .believe("(sport --> competition)", 1f, 0.9f)
              //.en("sport is a type of competition.");
                .believe("(chess --> competition)", 0.90f, 0.9f)
              //.en("chess is a type of competition.");

                .mustBelieve(cycles, "(chess --> sport)", 1f, 0.42f)
                .mustBelieve(cycles, "(sport --> chess)", 0.9f, 0.45f)

//                .mustBelieve(cycles, "(chess --> sport)", 0.9f, 0.45f)
//                .mustNotBelieve(cycles, "(chess --> sport)", 1f, 0.42f, (s,e)->true)
//              //.en("I guess chess is a type of sport");
//                .mustBelieve(cycles, "(sport --> chess)", 1f, 0.42f)
//                .mustNotBelieve(cycles, "(sport --> chess)", 0.9f, 0.45f, (s,e)->true)
//            /*  .en("I guess sport is a type of chess.")
//	            .en("sport is possibly a type of chess.")
//	            .es("es posible que sport es un tipo de chess.");*/
        ;
    }


    @Test void induction() {
        /*
        <swan --> swimmer>. %0.90%  'Swan is a type of swimmer.
        <swan --> bird>.  'Swan is a type of bird.

        <bird --> swimmer>. %0.90;0.45%  'I guess bird is a type of swimmer.
        <swimmer --> bird>. %1.00;0.42%  'I guess swimmer is a type of bird.
        */
        test
            .volMax(3)
            .believe("(swan --> swimmer)", 0.9f, 0.9f)
            .believe("(swan --> bird)", 1, 0.9f)
            .mustBelieve(cycles, "(bird --> swimmer)", 0.9f, 0.45f)
            .mustNotBelieve(cycles, "(bird --> swimmer)", 1, 0.42f, (s,e)->true)
            .mustBelieve(cycles, "(swimmer --> bird)", 1, 0.4f)
            .mustNotBelieve(cycles, "(swimmer --> bird)", 0.9f, 0.45f, (s,e)->true)
        ;
    }
    @Test
    void exemplification() {
        /*

        'Robin is a type of bird.
        <robin --> bird>.

        'A bird is a type of animal.
        <bird --> animal>.

        'I guess animal is a type of robin.
        ''outputMustContain('<animal --> robin>. %1.00;0.45%')
         */
        test
                .believe("<robin --> bird>", 1f, 0.9f)
                .believe("<bird --> animal>", 1f, 0.9f)
                .mustBelieve(cycles, "<animal --> robin>", 1f, 0.45f)
        ;
    }


    @Test
    void backwardInference() {

        test
                .volMax(3)
                .believe("(bird --> swimmer)", 1.0f, 0.8f)
                .question("(?1 --> swimmer)")
                .mustOutput(cycles, "(?1 --> bird)?")
                .mustOutput(cycles, "(bird --> ?1)?")
        ;
    }

    @Test
    void backwardInference2() {

        test
//                .confMin(0.9f)
                .volMax(3)
                .believe("(x --> y)")
                .question("(x --> z)")
                .mustOutput(cycles, "<y --> z>?")
                .mustOutput(cycles, "<z --> y>?")
        ;
    }




    @Test
    void revisionSim() {
        test.believe("<robin <-> swan>");
        test.believe("<robin <-> swan>", 0.1f, 0.6f);
        test.mustBelieve(cycles, "<robin <-> swan>", 0.87f, 0.91f);
    }

    @Test
    void resemblance() {

        test.confMin(0.8f);
        test.believe("<robin <-> swan>");
        test.believe("<gull <-> swan>");
        test.mustBelieve(cycles, "<gull <-> robin>", 1.0f, 0.81f);

    }

    @Test
    void inheritanceQuestion_and_Belief_ToQuestion() {

        test
            .volMax(3)
            .confMin(0.8f)
            .question("(x --> y)")
            .believe("(y --> z)")
            .mustQuestion(cycles, "(x --> z)")
            //.mustNotOutput(cycles, "(y --> z)", QUESTION)
        ;
    }




}