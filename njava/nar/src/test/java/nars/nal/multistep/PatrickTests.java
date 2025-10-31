package nars.nal.multistep;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.CONJ;
import static nars.Op.GOAL;

/**
,see Natural_Language_Processing2.md
 */

public class PatrickTests extends NALTest {


    @Test
    void testExample1() {
        /*
        
        

        <(&&,<$1 --> (/,REPRESENT,_,$3)>,<$2 --> (/,REPRESENT,_,$4)>) ==> <(*,(*,$1,$2),(*,$3,$4)) --> REPRESENT>>.
        
        <cat --> (/,REPRESENT,_,ANIMAL)>.
        
        <eats --> (/,REPRESENT,_,EATING)>.

        
        <(*,(*,cat,eats),?what) --> REPRESENT>?
        
         */
        test.freqRes(0.1f).volMax(22)
            .believe("(( ($1-->(REPRESENT,/,$3)) && ($2-->(REPRESENT,/,$4))) ==> REPRESENT({$1,$2},{$3,$4}))")
            .believe("(cat-->(REPRESENT,/,ANIMAL))")
            .believe("(eats-->(REPRESENT,/,EATING))")
            .question( "REPRESENT({cat,eats},?1)")
            //.mustBelieve(2000, "REPRESENT((eats,cat),(EATING,ANIMAL))", 0.9f, 1f, 0.15f, 0.99f);
            .mustBelieve(400, "REPRESENT({cat,eats},{ANIMAL,EATING})", 0.9f, 1f, 0.15f, 0.99f);

    }

    @Test
    void testExample1_NoImages() {

        final String o =
                "REPRESENT((cat&eats),{ANIMAL,EATING})";
                //"REPRESENT({cat,eats},{ANIMAL,EATING})";
        test.freqRes(0.1f).volMax($$(o).complexity()*4)
            .confMin(0.01f)
            .confTolerance(0.8f)
                .believe("(( REPRESENT($1,$3) && REPRESENT($2, $4) ) ==> REPRESENT({$1,$2},{$3,$4}))")
                .believe("REPRESENT(cat,ANIMAL)")
                .believe("REPRESENT(eats,EATING)")
                //.question("REPRESENT({cat,eats},?1)")
                .question("REPRESENT((cat&eats),?1)")
                .mustBelieve(200, o, 1, 0.4f);
    }

    @Test
    void testToothbrush() {
        /*
        <(*,toothbrush,plastic) --> made_of>.
        <(&/,<(*,$1,plastic) --> made_of>,<({SELF},$1) --> op_lighter>) =/> <$1 --> [heated]>>.
        <<$1 --> [heated]> =/> <$1 --> [melted]>>.
        <<$1 --> [melted]> <|> <$1 --> [pliable]>>.
        <(&/,<$1 --> [pliable]>,<({SELF},$1) --> op_reshape>) =/> <$1 --> [hardened]>>.
        <<$1 --> [hardened]> =|> <$1 --> [unscrewing]>>.
        <toothbrush --> object>.
        (&&,<#1 --> object>,<#1 --> [unscrewing]>)!

            >> lighter({SELF},$1) instead of <({SELF},$1) --> op_lighter>

        */


        TestNAR n = test;


        n.confTolerance(0.5f);

//        tt.nar.freqResolution.set(0.25f);
//        tt.nar.confResolution.set(0.02f);

//        n.nar.time.dur(100);
        n.nar.complexMax.set(18);

        n.nar.timeRes.set(5);

        n.input(
                "made_of(toothbrush,plastic).",
                "( ( made_of($1, plastic) && lighter(I, $1) ) ==>+10 <$1 --> [heated]>).",
                "(<$1 --> [heated]> ==>+10 <$1 --> [melted]>).",
                "(<$1 --> [melted]> ==> <$1 --> [pliable]>).",
                "(<$1 --> [pliable]> ==> <$1 --> [melted]>).",
                "(( <$1 --> [pliable]> && reshape(I,$1)) ==>+10 <$1 --> [hardened]>).",
                "(<$1 --> [hardened]> ==> <$1 --> [unscrews]>).",


                "(toothbrush --> [unscrews])! |"

        );

//        test.nar.onTask(t->{
//           System.out.println(t.proof());
//        },GOAL);

        int cycles = 500;
        n.mustGoal(cycles, "lighter(I, toothbrush)", 1f,
                0.2f,

                t -> t >= 0
        );


    }

    @Test
    void testToothbrushSimpler() {


        TestNAR tt = test;

        tt.confTolerance(0.9f);

        tt.nar.freqRes.set(0.25f);
//        tt.nar.confResolution.setAt(0.05f);


        tt.nar.time.dur(100);
        tt.nar.complexMax.set(18);
        tt.nar.timeRes.set(10);
        tt.nar.confMin.set(0.05f);

//        tt.nar.focus().onTask(t -> {
//            if (t.term().SIM())
//                tt.nar.proofPrint((NALTask)t);
//        }, QUESTION);

        tt.input(
                "(toothbrush-->plastic).",
                "( ( ($1-->plastic) && ($1-->lighter) ) ==>+10 hot:$1).",
                "(hot:$1 ==>+10 pliable:$1).",
//                "(molten:$1 ==> pliable:$1).",
                //"(pliable:$1 ==> molten:$1).",
                //"( (pliable:$1 && ($1-->reshape)) ==>+10 hard:$1).",
                "(pliable:$1 ==>+10 (hard:$1 && --hot:$1)).", //cool-off
                //"(pliable:$1 ==>+10 hard:$1).", //cool-off
                "(hard:$1 ==> unscrews:$1).",
                "$1.0 unscrews:toothbrush! |"
        );

        int cycles = 200;
//        tt.mustGoal(cycles, "hot:toothbrush", 1f, 0.5f, (t) -> t >= 0);
//
        tt.mustGoal(cycles, "hard:toothbrush", 1, 0.5f, (t) -> t >= 0);
        tt.mustGoal(cycles, "pliable:toothbrush", 1, 0.5f, (t) -> t >= 0);
//        tt.mustGoal(cycles, "molten:toothbrush", 1f, 0.5f, (t) -> t >= 0);
        //tt.mustNotQuestion(cycles, "(((--,(?1-->(plastic&&?2))) &&+- (?3-->(lighter&&?4))) ==>+- (?1-->(plastic&&?2)))");
        tt.mustGoal(cycles, "lighter:toothbrush", 1,
                0.3f,
                t -> t >= 0);

    }

    /**
    ,TODO
     */
    @Disabled
    @Test
    void testConditioningWithoutAnticipation() throws Narsese.NarseseException {
        /*
        <a --> A>. | <b --> B>. | %0% <c --> C>. %0%
        8
        <b --> B>. | <a --> A>. | %0% <c --> C>. %0%
        8
        <c --> C>. | <a --> a>. | %0% <b --> B>. %0%
        8
        <a --> A>. | <b --> B>. | %0% <c --> C>. %0%
        100
        <b --> B>. | <a --> A>. | %0% <c --> C>. %0%
        100
        <?1 =/> <c --> C>>? 

        Expected result: (also in OpenNARS syntax)
        For appropriate Interval target "time", "time2",
        <(&/,<a --> A>,time) =/> <c --> C>>.
        and
        <(&/,<b --> B>,time) =/> <c --> C>>.
        needs to be reduced in frequency, making
        <(&/,<a --> A>,time,<b --> B>,time2) =/> <c --> C>>.
        the strongest hypothesis based on the last two inputs where neither a nor b "leaded to" c.
         */

        NAR n = NARS.tmp();
        n.beliefPriDefault.pri(0.01f);
        n.complexMax.set(16);


        n.inputAt(0, "  A:a. |    --B:b. |    --C:c. |");
        n.inputAt(8, "  B:b. |    --A:a. |    --C:c. |");
        n.inputAt(16, "  C:c. |    --A:a. |    --B:b. |");
        n.inputAt(24, "  A:a. |    --B:b. |    --C:c. |");
        n.inputAt(124, "  B:b. |    --A:a. |    --C:c. |");

        n.run(224);
        //n.clear();

        n.input("       $0.9 (?x ==>   C:c)?");


        n.run(2000);

        /*
        Expected result: (also in OpenNARS syntax)
        For appropriate Interval target "time", "time2",
        <(&/,<a --> A>,time) =/> <c --> C>>.
        and
        <(&/,<b --> B>,time) =/> <c --> C>>.
        needs to be reduced in frequency, making
        <(&/,<a --> A>,time,<b --> B>,time2) =/> <c --> C>>.
        the strongest hypothesis based on the last two inputs where neither a nor b "leaded to" c.
         */

    }

    /**
    ,TODO
     */
    @Test
    @Disabled
    void testPixelImage() throws Narsese.NarseseException {


        NAR n = NARS.tmp();


        n.complexMax.set(60);
        n.beliefPriDefault.pri(0.05f);
        n.questionPriDefault.pri(0.9f);

        n.input("<#x --> P>. %0.0;0.25%");


        String image1 =
                """
                        <p_1_1 --> P>. | %0.5;0.9%
                        <p_1_2 --> P>. | %0.5;0.9%
                        <p_1_3 --> P>. | %0.6;0.9%
                        <p_1_4 --> P>. | %0.6;0.9%
                        <p_1_5 --> P>. | %0.5;0.9%
                        <p_2_1 --> P>. | %0.5;0.9%
                        <p_2_2 --> P>. | %0.5;0.9%
                        <p_2_3 --> P>. | %0.8;0.9%
                        <p_2_4 --> P>. | %0.5;0.9%
                        <p_2_5 --> P>. | %0.5;0.9%
                        <p_3_1 --> P>. | %0.6;0.9%
                        <p_3_2 --> P>. | %0.8;0.9%
                        <p_3_3 --> P>. | %0.9;0.9%
                        <p_3_4 --> P>. | %0.5;0.9%
                        <p_3_5 --> P>. | %0.5;0.9%
                        <p_4_1 --> P>. | %0.5;0.9%
                        <p_4_2 --> P>. | %0.5;0.9%
                        <p_4_3 --> P>. | %0.7;0.9%
                        <p_5_4 --> P>. | %0.6;0.9%
                        <p_4_4 --> P>. | %0.5;0.9%
                        <p_4_5 --> P>. | %0.6;0.9%
                        <p_5_1 --> P>. | %0.5;0.9%
                        <p_5_2 --> P>. | %0.5;0.9%
                        <p_5_3 --> P>. | %0.5;0.9%
                        <p_5_5 --> P>. | %0.5;0.9%
                        <example1 --> name>. |""";


        n.input(image1.split("\n"));


		n.question(CONJ.the($("P:p_2_3"), $("P:p_3_2"), $("P:p_3_4"), $("P:p_4_3"), $("name:example1")));


        n.run(6000);

        n.clear();


        String image2 =
                """
                        <p_1_1 --> pixel>. | %0.5;0.9%
                        <p_1_2 --> pixel>. | %0.5;0.9%
                        <p_1_3 --> pixel>. | %0.6;0.9%
                        <p_1_4 --> pixel>. | %0.6;0.9%
                        <p_1_5 --> pixel>. | %0.5;0.9%
                        <p_2_1 --> pixel>. | %0.5;0.9%
                        <p_2_2 --> pixel>. | %0.5;0.9%
                        <p_2_3 --> pixel>. | %0.8;0.9%
                        <p_2_4 --> pixel>. | %0.5;0.9%
                        <p_2_5 --> pixel>. | %0.5;0.9%
                        <p_3_1 --> pixel>. | %0.6;0.9%
                        <p_3_2 --> pixel>. | %0.5;0.9%
                        <p_3_3 --> pixel>. | %0.5;0.9%
                        <p_3_4 --> pixel>. | %0.8;0.9%
                        <p_3_5 --> pixel>. | %0.5;0.9%
                        <p_4_1 --> pixel>. | %0.5;0.9%
                        <p_4_2 --> pixel>. | %0.5;0.9%
                        <p_4_3 --> pixel>. | %0.7;0.9%
                        <p_5_4 --> pixel>. | %0.6;0.9%
                        <p_4_4 --> pixel>. | %0.5;0.9%
                        <p_4_5 --> pixel>. | %0.6;0.9%
                        <p_5_1 --> pixel>. | %0.5;0.9%
                        <p_5_2 --> pixel>. | %0.5;0.9%
                        <p_5_3 --> pixel>. | %0.5;0.9%
                        <p_5_5 --> pixel>. | %0.5;0.9%
                        <example2 --> name>. |""";

        n.input(image2.split("\n"));


		n.question(CONJ.the($("P:p_2_3"), $("P:p_3_2"), $("P:p_3_3"), $("P:p_3_4"), $("P:p_4_3"), $("name:example2")));
        n.run(6000);


    }

    @Test void memorizePreconditionParallel() {
        /*
        <(&/,(&&,<a --> #A>,<a2 --> #A>),+10,(^pick,{SELF},a),+10) =/> <a --> B>>.
        100
        <a --> A>. |
        <a2 --> A>. |
        10
        <a --> B>!
        ''outputMustContain('(^pick,{SELF},a). :!110: %1.00;0.90%')
         */
        test
            .believe("(((#A:a && #A:b) &&+1 pick(#A)) ==>+1 c)")
            .input("x:a. |")
            .input("x:b. |")
            .inputAt(3, "c! |")
            .mustOutput(10, "pick(x)", GOAL, 1f, 1f, 0.05f, 0.81f,
                    t->t>=1)
            ;
    }

    /** https://github.com/opennars/ALANN2018/blob/master/Examples/Shape_world */
    @Disabled @Test void ShapeWorld() throws Narsese.NarseseException {
        String s = 
                "<<($1,$2) --> larger> ==> <($2,$1) --> smaller>>.\n" +
                "<<($1,$2) --> smaller> ==> <($2,$1) --> larger>>.\n" +
                "<<($1,$2) --> above> ==> <($2,$1) --> below>>.\n" +
                "<<($1,$2) --> next_to> ==> <($2,$1) --> next_to>>.\n" +
                //"<<($1,$2) --> next_to> <=> <($2,$1) --> next_to>>.\n" +
                "<<($1,$2) --> over> ==> <($2,$1) --> under>>.\n" +
                "<<($1,$2) --> under> ==> <($2,$1) --> over>>.\n" +
                "<<($1,$2) --> outside> ==> <($2,$1) --> inside>>.\n" +
                "<<($1,$2) --> inside> ==> <($2,$1) --> contains>>.\n" +
                "<<($1,$2) --> contains> ==> <($1,$2) --> larger>>. \n" +
                "<<($1,$2) --> on> ==> <($2,$1) --> under>>. \n" +
                "<<({$1},{$2}) --> inside> ==> <({$2},{$1}) --> contains>>.\n" +
                "<({box},floor) --> on>.\n" +
                "<({toy},{box}) --> inside>.\n" +
                "<({ball},{box}) --> on>.\n" +
                "<{?1} --> (on,/,floor)>?\n" +
                "<{?1} --> (on,{ball},/)>?\n" +
                "<{?1} --> (under,/,{ball})>?\n" +
                "<{?1} --> (contains,/,{toy})>?\n" +
                "<{box} --> (larger,/,{toy})>?\n" +
                "<{toy} --> (smaller,/,{box})>?\n" +
                "<?1 --> (on,/,(on,/,floor))>?"
                ;
        NAR n = NARS.tmp();
        n.log();
        n.input(s);
        n.run(1000);
        
    }
    @Disabled @Test void overlap1() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.log();
        n.input("(?1-->x)?");
        n.input("(a-->x).");
        n.input("((a,b)-->x).");
//        n.main().onTask(z -> {
//            if (z.BELIEF() && ((NALTask)z).truth().conf()>=0.89f)
//                Util.nop();
//        });
        n.run(1000);
    }

}