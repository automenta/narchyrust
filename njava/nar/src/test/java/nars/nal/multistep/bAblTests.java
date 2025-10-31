package nars.nal.multistep;

import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;

/**
 * see bAbl.nal
 */
class bAblTests extends NALTest {


    @Disabled
    @Test
    void test1() {

        test
                .volMax(6)
                .believe("in(john,playground)")
                .believe("in(bob,office)")
                .question("in(john,?where)")
                .mustBelieve(500, "in(john,playground)", 1f, 0.73f)
        ;


    }

    @Test
    void test2() {


        String solution = "inside(football,playground)";

        TestNAR t = test;

        t.confMin(0.1f);
        t.freqRes(0.25f);
        t.nar.complexMax.set(20);

        int cyc = 150;
        t
                .inputAt(0, "((holds(#who,$what) &&+1 inside(#who,$where)) ==>+1 inside($what,$where)).")
                .inputAt(1, "holds(john,football). |")
                .inputAt(2, "inside(john,playground). |")
                //.inputAt(2,"inside(bob,office).")
                //.inputAt(2,"inside(bob,kitchen).")
                .inputAt(2, "inside(football,?where)?")
                .mustOutput(cyc, solution, BELIEF,
                        1f, 1f, 0.2f, 0.99f, 3)
                .mustNotOutput(cyc, solution, BELIEF,
                        0, 1, 0, 1, (s, e) -> s != 3 && e != 3);
        /*
        $.44 inside(football,playground). 3 %1.0;.54% {64: 1;2;4}
    $.50 inside(john,playground). 2 %1.0;.90% {2: 4}
    $.43 (inside(john,playground) ==>+1 inside(football,playground)). 1 %1.0;.60% {25: 1;2;4}
      $.41 ((holds(john,football) &&+1 inside(john,playground)) ==>+1 inside(football,playground)). 2 %1.0;.68% {10: 1;2;4}
        $.40 ((holds(john,football) &&+1 ((john,$1)-->$2)) ==>+1 ((football,$1)-->$2)). 1 %1.0;.81% {20: 1;2}
          $.50 holds(john,football). 1 %1.0;.90% {1: 2}
          $.35 ((holds(#1,$2) &&+1 ((#1,$3)-->$4)) ==>+1 (($2,$3)-->$4)). %1.0;.90% {44: 1}
        $.50 inside(john,playground). 2 %1.0;.90% {2: 4}
      $.50 holds(john,football). 1 %1.0;.90% {1: 2}
	(  B ==> C),B,--is(B,"--"), neq(B,C)        |-   C,  (Belief:PrePPX, Time:BeliefRel):	preBelief
	({punc({"!",".","?","@"}),equalRoot(task,belief),TermMatcher(taskTerm,2),(--,(double))}==>("nars.action.transform.VariableIntroduction","envo3p"))
	X, (  C==>A),    is(C,"&&")                 |-     unisubst((  C==>A), chooseUnifiableSubEvent(C,    X),     X, novel), (Belief:ConductPPX, Time:Belief):	anonymous_analogy
	(  C ==> A), X        |-   (conjWithout(C,  X) ==>   A), (Belief:ConductPP, Time:Belief):	specific_deduction
	(  C==>A), X, is(C,"&&")                 |-     unisubst((  C==>A), chooseUnifiableSubEvent(C,    X),     X, novel), (Belief:ConductPP, Time:Belief):	anonymous_analogy
	B, (  B ==> C),--is(B,"--"), neq(B,C)       |-   C,  (Belief:PrePP, Time:TaskRel):	preBelief

         */

    }

    /**
     * TODO find a better problem representation, this one isnt good
     */
    @Disabled
    @Test
    void test19() {


        TestNAR t = test;
        t.confTolerance(0.9f);
        t.nar.complexMax.set(35);
        t.nar.freqRes.set(0.25f);
        t.nar.beliefPriDefault.pri(0.1f);
        t.nar.questionPriDefault.pri(0.8f);

        t.input("((&&, start($1,$2), at( $1,$B,$C), at( $B,$2,$C2) ) ==> ( path( id,$C,id,$C2)   && chunk( $1,$2,$B) )).")
                .input("((&&, start($1,$2), at( $1,$B,$C), at( $2,$B,$C2) ) ==> ( path( id,$C,neg,$C2)  && chunk( $1,$2,$B) )).")
                .input("((&&, start($1,$2), at( $B,$1,$C), at( $B,$2,$C2) ) ==> ( path( neg,$C,id,$C2)  && chunk( $1,$2,$B) )).")
                .input("((&&, start($1,$2), at( $B,$1,$C), at( $2,$B,$C2) ) ==> ( path( neg,$C,neg,$C2) && chunk( $1,$2,$B) )).")
                .input("at(kitchen,hallway,south).")
                .input("at(den,hallway,west).")
                .input("start(den,kitchen).")
                .input("$0.9 path(?a,?b,?c,?d)?")
                .mustBelieve(2500, "path(id,west,neg,south)", 1f, 0.75f);


    }


}