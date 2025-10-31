package nars.nal.nal2;


import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class NAL2Test extends NALTest {

    private static final int cycles = 50;


    @Override
    protected NAR nar() {
        NAR n = new NARS().withNAL(1,2).get();
        n.confMin.set(0.2f);
        n.complexMax.set(7);
        return n;
    }


    /* Handled by parser, this copula is just syntactic sugar
    @Test
    public void instanceToInheritance() throws InvalidInputException {
        test()
        .believe("<Tweety -{- bird>")
        .mustBelieve(cycles,"<{Tweety} --> bird>",1.0f,0.9f)
        .run();
    }*/

    /* Handled by parser, this copula is just syntactic sugar
    @Test
    public void propertyToInheritance() throws InvalidInputException {
        test().believe("<raven -]- black>")
        .mustBelieve(cycles,"<raven --> [black]>",1.0f,0.9f)
        .run();
    }*/

    /* Handled by parser, this copula is just syntactic sugar
    @Test
    public void instancePropertyToInheritance() throws InvalidInputException {
        test().believe("<Tweety {-] yellow>") 
        .mustBelieve(cycles,"<{Tweety} --> [yellow]>",1.0f,0.9f)
        .run();
    }
*/



//    @Test
//    void setDefinition3() {
//
//
//        test.volMax(5);
//        test.believe("<{Birdie} <-> {Tweety}>");
//        test.mustBelieve(cycles, "<Birdie <-> Tweety>", 1.0f, 0.9f);
//        test.mustBelieve(cycles, "<{Tweety} --> {Birdie}>", 1.0f, 0.9f);
//
//    }
//
//    @Test
//    void setDefinition4() {
//
//
//        test.volMax(5);
//        test.confMin(0.8f);
//        test.believe("<[bright] <-> [smart]>");
//        test.mustBelieve(cycles, "<bright <-> smart>", 1.0f, 0.9f);
//        test.mustBelieve(cycles, "<[bright] --> [smart]>", 1.0f, 0.9f);
//
//    }

    @Test
    void structureTransformation() {

        TestNAR tester = test;
        tester.believe("<Birdie <-> Tweety>", 0.9f, 0.9f);
        tester.question("<{Birdie} <-> {Tweety}>");
        tester.mustBelieve(cycles, "<{Birdie} <-> {Tweety}>", 0.9f, 0.73f /*0.9f*/);

    }

    @Test
    void structureTransformation2() {

        TestNAR tester = test;
        tester.believe("<bright --> smart>", 0.9f, 0.9f);
        tester.question("<[bright] --> [smart]>");
        tester.mustBelieve(cycles, "<[bright] --> [smart]>", 0.9f, 0.73f /*0.9f*/);

    }

    @Test
    void structureTransformation3() {
        /*
        <bright <-> smart>. %0.9;0.9%
        <{bright} --> {smart}>?
         */

        test.believe("<bright --> smart>", 0.9f, 0.9f);
        test.question("<{bright} --> {smart}>");
        test.mustBelieve(cycles, "<{bright} --> {smart}>", 0.9f, 0.73f /*0.9f*/);

    }



    @Test
    void testUnion() {

        test.confMin(0.8f);
        test
                .volMax(5)
                .believe("a:{x}.")
                .believe("a:{y}.")
                .mustBelieve(cycles, "a:{x,y}", 1, 0.81f);

    }

    @Test
    void testSetDecomposePositive() {
        test.confMin(0.8f);
        test
                .believe("({x,y}-->c)")
                .mustBelieve(cycles, "({x}-->c)", 1, 0.81f)
                .mustBelieve(cycles, "({y}-->c)", 1, 0.81f)
        ;
    }

    @Test
    void testInstToNonSet() {
        test
                .believe("({x}-->c)")
                .mustBelieve(cycles, "(x-->c)", 1, 0.81f)
        ;
    }
    @Test
    void testPropToNonSet() {
        test
                .believe("(c --> [x])")
                .mustBelieve(cycles, "(c-->x)", 1, 0.81f)
        ;
    }

    @Test
    void testSetDecomposeNegativeExt() {

        test
                .believe("<{--x,y}-->c>")
                .mustBelieve(cycles, "({--x}-->c)", 1, 0.81f)
                .mustBelieve(cycles, "({y}-->c)", 1, 0.81f)
        ;
    }

    @Test
    void testSetDecomposeNegativeInt() {

        test
                .believe("<c-->[--x,y]>")
                .mustBelieve(cycles, "(c-->[--x])", 1, 0.81f)
                .mustBelieve(cycles, "(c-->[y])", 1, 0.81f)
        ;
    }

    @Test
    void testIntersectDiffUnionOfCommonSubtermsPre() {
        test
                .volMax(5)
                .believe("<{x,y}-->c>")
                .mustBelieve(cycles, "<{x}-->c>", 1, 0.81f)
                .mustBelieve(cycles, "<{y}-->c>", 1, 0.81f)
        ;
    }
    @Test
    void testIntersectDiffUnionOfCommonSubterms() {
        test
                .volMax(6)
                .confMin(0.8f)
                .believe("<{x,y}-->c>")
                .believe("<{x,z}-->c>")
                .mustBelieve(cycles, "<{x,y,z}-->c>", 1, 0.81f)
                .mustBelieve(cycles, "<{x}-->c>", 1, 0.81f)
                .mustBelieve(cycles, "<{y}-->c>", 1, 0.81f)
                .mustBelieve(cycles, "<{z}-->c>", 1, 0.81f)
        ;
    }


    @Test
    void set_operations() {

        test
                .volMax(8)
                .confMin(0.75f)
                .believe("<planetX --> {Mars,Pluto,Venus}>", 0.9f, 0.9f)
                .believe("<planetX --> {Pluto,Saturn}>", 0.7f, 0.9f)
                .mustBelieve(cycles, "<planetX --> {Mars,Pluto,Saturn,Venus}>", 0.97f, 0.81f)
                .mustBelieve(cycles, "<planetX --> Pluto>", 0.63f, 0.81f)

        ;


    }

    @Test
    void set_operationsSetExt_union() {

        TestNAR tester = test;
        tester.believe("<planetX --> {Mars,Pluto,Venus}>", 0.9f, 0.9f);
        tester.believe("<planetX --> {Pluto,Saturn}>", 0.1f, 0.9f);
        tester.mustBelieve(cycles, "<planetX --> {Mars,Pluto,Saturn,Venus}>", 0.91f, 0.81f);
    }

    @Test
    void set_operationsSetExt_unionNeg() {

        TestNAR tester = test;
        tester.believe("<planetX --> {Earth}>", 0.1f, 0.9f);
        tester.believe("<planetX --> {Mars}>", 0.1f, 0.9f);
        tester.mustBelieve(cycles, "<planetX --> {Earth,Mars}>", 0.19f, 0.81f);
    }


    @Test
    void set_operationsSetInt_union_2_3_4() {

        TestNAR tester = test;
        tester.believe("<planetX --> [marsy,earthly,venusy]>", 1.0f, 0.9f);
        tester.believe("<planetX --> [earthly,saturny]>", 0.1f, 0.9f);
        tester.mustBelieve(cycles, "<planetX --> [marsy,earthly,saturny,venusy]>", 0.1f, 0.81f);
    }

    @Test
    void set_operationsSetInt_union1_1_2_3() {

        test.volMax(6);
        test.confMin(0.8f);
        test.believe("<planetX --> [marsy,venusy]>", 1.0f, 0.9f);
        test.believe("<planetX --> [earthly]>", 0.1f, 0.9f);
        test.mustBelieve(cycles, "<planetX --> [marsy,earthly,venusy]>", 0.1f, 0.81f);

    }

    @Test
    void set_operations2_difference() throws Narsese.NarseseException {
        assertEquals("{Mars,Venus}", $.diff($.$("{Mars,Pluto,Venus}"), $.$("{Pluto,Saturn}")).toString());

        test.confMin(0.8f);
        test.believe("(planetX --> {Mars,Pluto,Venus})", 0.9f, 0.9f);
        test.believe("(planetX --> {Pluto,Saturn})", 0.1f, 0.9f);
        test.mustBelieve(cycles, "(planetX --> {Mars,Venus})", 0.81f ,0.81f);

    }


    @Test
    void set_operations3_difference() {

        test.confMin(0.8f);
        test.believe("<planetX --> [marsy,earthly,venusy]>", 1.0f, 0.9f);
        test.believe("<planetX --> [earthly,saturny]>", 0.1f, 0.9f);
        test.mustBelieve(cycles, "<planetX --> [marsy,earthly,saturny,venusy]>", 0.1f, 0.81f);
        test.mustBelieve(cycles, "<planetX --> [marsy,venusy]>", 0.90f, 0.81f);
    }

    @Test
    void set_operations4() {

        test.volMax(7);
        test.confMin(0.8f);
        test.believe("([marsy,earthly,venusy] --> planetX)", 1.0f, 0.9f);
        test.believe("([earthly,saturny] --> planetX)", 0.1f, 0.9f);
        test.mustBelieve(cycles, "<[marsy,earthly,saturny,venusy] --> planetX>", 1.0f, 0.81f);
        test.mustBelieve(cycles, "<[marsy,venusy] --> planetX>", 0.90f, 0.81f);

    }

    @Test
    void set_operations5Half() {
        test.volMax(9);
        test.confMin(0.8f);
        test.believe("<{Mars,Pluto,Venus} --> planetX>", 1.0f, 0.9f);
        test.mustBelieve(cycles, "<{Mars,Venus} --> planetX>", 1.0f, 0.81f);
    }

    @Test
    void set_operations5() {

        test.volMax(9);
        test.confMin(0.8f);
        test.believe("<{Mars,Pluto,Venus} --> planetX>", 1.0f, 0.9f);
        test.believe("<{Pluto,Saturn} --> planetX>", 0.1f, 0.9f);
        test.mustBelieve(cycles, "<{Mars,Pluto,Saturn,Venus} --> planetX>", 0.1f, 0.81f);
        test.mustBelieve(cycles, "<{Mars,Venus} --> planetX>", 0.9f, 0.81f);
    }

    @Test void questionDecomposition1() {
        test.volMax(6);
        test.confMin(0.8f);
        test.believe("<{Mars,Pluto,Venus} --> planetX>", 1.0f, 0.9f);
        test.question("<{Pluto} --> planetX>");
        test.mustBelieve(cycles, "<{Pluto} --> planetX>", 1, 0.81f);
    }

//    @Test void testDepVarLift() { //single premise
//        test.believe("(#1-->x)", 1.0f, 0.9f);
//        test.mustBelieve(cycles, "({#1}-->x)", 1, 0.81f);
//    }
//    @Test void testDepVarCollect() {
//        test.believe("(#1-->x)", 1.0f, 0.9f);
//        test.believe("(a-->x)", 1.0f, 0.9f);
//        test.mustBelieve(cycles, "({#1,a}-->x)", 1, 0.81f);
//    }
}