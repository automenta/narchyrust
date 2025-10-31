package nars.nal.nal5;

import nars.NAR;
import nars.Narsese;
import nars.Truth;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.Op.ETERNAL;
import static nars.term.util.Testing.assertEq;


public class NAL5Test extends AbstractNAL5Test {

    @Test void revision() throws Narsese.NarseseException {
        test.requireAnyConditions = false; NAR n = test.nar;

        final String x = "((robin --> flying) ==> a)";
        n.believe(x, 1f, 0.9f);
        n.believe(x, 0f, 0.6f);
        Truth t = n.belief(x).truth();
        assertEq(0.85f, 0.92f, t);

//        test
//                .mustBelieve(cycles, x, 0.85f, 0.92f)
//                .believe(x)
//                .believe(x, 0.00f, 0.60f);

    }

    @Test
    void deduction() {


        test.believe("(a ==> b)");
        test.believe("((robin --> flying) ==> a)");
        test.mustBelieve(cycles, "((robin --> flying) ==> b)", 1.00f, 0.81f);

    }

    @Test
    void deductionPosCommon() {
        test.volMax(3)
                .believe("(x ==> z)")
                .believe("(z ==> y)")
                .mustBelieve(cycles, "(x ==> y)", 1.00f, 0.81f);

    }

    @Test
    void deductionNegCommon() {
        test.volMax(4)
                .believe("(x ==> --z)")
                .believe("(--z ==> y)")
                .mustBelieve(cycles, "(x ==> y)", 1.00f, 0.81f);

    }

    @Test
    void exemplification() {
        test.believe("(c ==> a)");
        test.believe("(a ==> b)");
        test.mustBelieve(cycles, "(b ==> c)", 1.00f, 0.45f);
    }

    @Test
    void depVarUniqueness() {

        test
                .volMax(11)
                .believe("f(x,#1)")
                .believe("f(y,#1)")
                .mustBelieve(cycles, "(f(x,#1) ==> f(y,#2))", 1.00f, 0.45f)
                .mustBelieve(cycles, "(f(y,#1) ==> f(x,#2))", 1.00f, 0.45f)
        //both forms
        //.mustBelieve(cycles, "(f(x,#1) ==> f(y,#1))", 1.00f, 0.45f)
        ;

    }

    @Test
    void induction() {
        /*


        '********** induction

        'If robin is a type of bird then robin is a type of animal.
        <<robin --> bird> ==> <robin --> animal>>.

        'If robin is a type of bird then robin can fly.
        <<robin --> bird> ==> <robin --> [flying]>>. %0.80%

        'I guess if robin can fly then robin is a type of animal.
        ''outputMustContain('<<robin --> [flying]> ==> <robin --> animal>>. %1.00;0.39%')

        'I guess if robin is a type of animal then robin can fly.
        ''outputMustContain('<<robin --> animal> ==> <robin --> [flying]>>. %0.80;0.45%')

         (a ==> b).
         (a ==> c). %0.8%
         OUT: <c ==> b>. %1.00;0.39%
         OUT: <b ==> c>. %0.80;0.45%


         */

        test.volMax(3);
        test.confMin(0.3f);
        test.believe("(a ==> b)", 1f, 0.9f);
        test.believe("(a ==> c)", 0.8f, 0.9f);
        test.mustBelieve(cycles, "(c ==> b)", 1.00f, 0.39f);
        test.mustBelieve(cycles, "(b ==> c)", 0.80f, 0.45f);

    }

    @Test
    void induction_dont_repeat() {
        test.volMax(3);
        test.question("(a ==> b)");
        test.mustNotQuestion(cycles, "(a ==>+- a)");
        test.mustNotQuestion(cycles, "(b ==>+- b)");
    }

    @Test
    void abduction() {

        /*

        '********** abduction

        'If robin is a type of bird then robin is a type of animal.
        <<robin --> bird> ==> <robin --> animal>>.

        'If robin can fly then robin is probably a type of animal.
        <<robin --> [flying]> ==> <robin --> animal>>. %0.8%

        'I guess if robin is a type of bird then robin can fly.
        ''outputMustContain('<<robin --> bird> ==> <robin --> [flying]>>. %1.00;0.39%')

        'I guess if robin can fly then robin is a type of bird.
        ''outputMustContain('<<robin --> [flying]> ==> <robin --> bird>>. %0.80;0.45%')

        (b ==> a).
        (c ==> a). %0.80%
        14
         OUT: <b ==> c>. %1.00;0.39%
         OUT: <c ==> b>. %0.80;0.45%
         */

        test.volMax(3).confMin(0.35f);
        test.believe("(b ==> a)");
        test.believe("(c ==> a)", 0.8f, 0.9f);
        test.mustBelieve(cycles, "(b ==> c)", 1.00f, 0.39f);
        test.mustBelieve(cycles, "(c ==> b)", 0.80f, 0.45f);
    }

    @Test
    void abductionSimpleNeg() {
        test.volMax(4).confMin(0.35f)
                .believe("(a ==> --b)")
                .believe("(a ==> --c)", 0.8f, 0.9f)
                .mustBelieve(cycles, "(--c ==> b)", 0.00f, 0.39f)
                .mustBelieve(cycles, "(--b ==> c)", 0.20f, 0.45f);
    }

    @Test
    void testImplBeliefPosPos() {


        test

                .believe("b")
                .believe("(b==>c)", 1, 0.9f)
                .mustBelieve(cycles, "c", 1.00f, 0.81f);
    }

    @Test
    void testImplBeliefPosNeg() {


        test
                .believe("b")
                .believe("(b ==> --c)", 1, 0.9f)
                .mustBelieve(cycles, "c", 0.00f, 0.81f);
    }

    @Test
    void detachment() {

        test
                .believe("(a ==> b)")
                .believe("a")
                .mustBelieve(cycles, "b", 1.00f, 0.81f);

    }

    @Test
    void detachment2() {

        test.believe("(a ==> b)", 0.70f, 0.90f);
        test.believe("b");
        test.mustBelieve(cycles, "a",
                1f, 0.36f);
        //0.7f, 0.36f /*0.45f*/);


    }

    @Test
    void anonymous_analogy1_depvar() {
        test.volMax(7);
        test.confMin(0.1f);
        test
                .believe("(a:#1 && y)")
                .believe("a:x", 0.80f, 0.9f)
                .mustBelieve(cycles, "(a:x && y)", 0.80f, 0.43f);
//                .mustBelieve(cycles, "y", 0.80f, 0.43f)
    }

    @Test
    void anonymous_analogy1_depvar_neg() {
        test.volMax(8);
        test.confMin(0.3f);
        test
                .believe("(--(#1-->a) && y)")
                .believe("(x-->a)", 0.20f, 0.9f)
                .mustBelieve(cycles, "(--a:x && y)", 0.80f, 0.43f);
        //.mustBelieve(cycles, "y", 0.80f, 0.43f);
    }

    @Test void implCompose_PredPosPos() {
        test.volMax(8)
            .believe("(a ==> b)")
            .believe("(a ==> c)", 0.9f, 0.9f)
            .mustBelieve(cycles, " (a ==> (b&&c))", 0.90f, 0.81f);
    }
    @Test void implCompose_PredPosNeg() {
        test.volMax(6)
                .believe("(a ==>   b)")
                .believe("(a ==> --c)")
                .mustBelieve(cycles, "(a ==> (b && --c))", 1, 0.81f);
    }
    @Test void implCompose_PredNegNeg() {
        test.volMax(7)
                .believe("(a ==> --b)")
                .believe("(a ==> --c)")
                .mustBelieve(cycles, "(a ==> (--b && --c))", 1, 0.81f);
    }

//    @Test
//    void anonymous_analogy_disj_depvar() {
//        test.volMax(8);
//        test.confMin(0.3f);
//        test
//            .believe("--((#1-->a) && y)")
//            .believe("(x-->a)", 0.80f, 0.9f)
//            .mustBelieve(cycles, "--(a:x && y)", 0.20f, 0.43f);
//            //.mustBelieve(cycles, "y", 0.20f, 0.43f);
//    }

//
//    @Test
//    void anonymous_analogy1_neg2() {
//        test
//                .termVolMax(5)
//                .believe("(&&, --x, y, z)")
//                .believe("x", 0.20f, 0.9f)
//                .mustBelieve(cycles, "(&&,y,z)", 0.80f,
//                        0.65f /*0.43f*/);
//    }



    @Test
    void implCompose_SubjPosPos() {
        test.volMax(11)
            .believe("(x ==> z)")
            .believe("(y ==> z)", 0.9f, 0.81f)
            .mustBelieve(cycles, " ((x||y) ==> z)", 0.97f, 0.66f);
//        test.mustBelieve(cycles, " ((x && y) ==> z)", 1f, 0.73f);
    }
    @Test
    void implCompose_SubjPosPos_different() {
        test.volMax(11).confMin(0.01f)
                .believe("(x ==> z)", 1.0f, 0.9f)
                .believe("(y ==> z)", 0.1f, 0.9f)
                .mustBelieve(cycles, " ((x||y) ==> z)", 0.55f, 0.08f);
    }
    @Test
    void compound_composition_SubjNeg_simple() {
        test.volMax(11);
        test.believe("--(x ==> z)");
        test.believe("--(y ==> z)", 0.9f, 0.81f);
        test.mustBelieve(cycles, "((x || y) ==> z)", 0.03f, 0.66f);
        //        test.mustBelieve(cycles, "((x && y) ==> z)", 0.1f, 0.73f);

    }


    @Test
    void compound_composition_Subj() {


        test.volMax(16)
                .believe("(bird:robin ==> animal:robin)")
                .believe("((robin-->[flying]) ==> animal:robin)", 0.9f, 0.81f)
//                .mustBelieve(cycles, "((bird:robin && (robin-->[flying])) ==> animal:robin)", 1f, 0.73f)
                .mustBelieve(cycles, "((bird:robin || (robin-->[flying])) ==> animal:robin)", 0.97f, 0.66f)
                .mustNotBelieve(cycles, "(((robin-->[flying])&&(robin-->bird))==>(robin-->animal))", 0.95f, 0.73f, (s, e) -> true)
        ;
    }

    @Test
    void compound_composition_SubjNeg() {

        test.volMax(14).confMin(0.65f);
        test.believe("--(bird:robin ==> animal:nonRobin)");
        test.believe("--((robin-->[flying]) ==> animal:nonRobin)", 0.9f, 0.81f);
        test.mustBelieve(cycles, "((bird:robin && (robin-->[flying])) ==> animal:nonRobin)", 0.1f, 0.73f);
        //test.mustBelieve(cycles, "((bird:robin || (robin-->[flying])) ==> animal:nonRobin)", 0f, 0.73f);
    }

    @Test
    void compound_decomposition_one_premise_pos() {


        test.volMax(8);
        test.believe("((robin --> [flying]) && (robin --> swimmer))", 1.0f, 0.9f);
        test.mustBelieve(cycles, "(robin --> swimmer)", 1.00f, 0.81f);
        test.mustBelieve(cycles, "(robin --> [flying])", 1.00f, 0.81f);
    }

    @Test
    void conjunction_decomposition_one_premises() {

        test
                .volMax(8)
                .believe("(&&,(robin --> swimmer),(robin --> [flying]))", 0.9f, 0.9f)
                .mustBelieve(cycles, "(robin --> swimmer)", 0.9f, 0.73f)
                .mustBelieve(cycles, "(robin --> [flying])", 0.9f, 0.73f);

    }

    @Test
    void conjunction_decomposition_one_premises_simple() {
        test.volMax(3)
                .believe("(a && b)")
                .mustBelieve(cycles, "a", 1f, 0.81f)
                .mustBelieve(cycles, "b", 1f, 0.81f)
        ;
    }

    @Test
    void negation0() {

        test
                .mustBelieve(cycles, "(robin --> [flying])", 0.10f, 0.90f)
                .believe("(--,(robin --> [flying]))", 0.9f, 0.9f);


    }

    @Test
    void negation1() {

        test
                .mustBelieve(cycles, "<robin <-> parakeet>", 0.10f, 0.90f)
                .believe("(--,<robin <-> parakeet>)", 0.9f, 0.9f);


    }

    @Test
    void conditional_belief_disj_strong() {
        test.volMax(8);
        test.believe("((x || y) ==> a)");
        test.believe("x");
        test.mustBelieve(cycles, "a", 1.00f, 0.81f);
    }
    @Test
    void conditional_goal_disj_strong() {
        test.volMax(8);
        test.believe("((x || y) ==> a)");
        test.goal("x");
        test.mustGoal(cycles, "a", 1.00f, 0.45f);
    }
    @Test
    void conditional_goal_disj_strong_unify() {
        test.volMax(14);
        test.believe("((x($b) || y) ==> a($b))");
        test.goal("x(z)");
        test.mustGoal(cycles, "a(z)", 1.00f, 0.45f);
    }
    @Test
    void testPosPosImplicationConc() {


        test
                .input("x. %1.0;0.90%")
                .input("(x ==> y).")
                .mustBelieve(cycles, "y", 1.0f, 0.81f)
                .mustNotOutput(cycles, "y", BELIEF, 0f, 0.5f, 0, 1, ETERNAL);

    }

    @Test
    void testImplNegPos() {

        test
                .input("--x.")
                .input("(--x ==> y).")
                .mustBelieve(cycles, "y", 1.0f, 0.81f)
        // .mustNotOutput(cycles, "((--,#1)==>y)", BELIEF, 0f, 0.5f, 0, 1, ETERNAL)
        //.mustNotOutput(cycles, "y", BELIEF, 0f, 0.5f, 0, 1, ETERNAL)
        ;
    }

    @Test
    void testImplNegNeg() {

        test

                .input("--x.")
                .input("(--x ==> --y).")
                .mustBelieve(cycles, "y", 0.0f, 0.81f)
        //.mustNotOutput(cycles, "y", BELIEF, 0.5f, 1f, 0.1f, 1, ETERNAL)
        ;
    }

    @Test
    void testAbductionNegPosImplicationPred() {
        test

                .input("y. %1.0;0.90%")
                .input("(--x ==> y).")
                .mustBelieve(cycles, "x", 0.0f, 0.45f)
                .mustNotOutput(cycles, "x", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
        ;
    }

    @Disabled
    @Test
    void testAbductionPosNegImplicationPred() {

        test
                .input("y. %1.0;0.90%")
                .input("--(x ==> y).")
                .mustBelieve(cycles, "x", 0.0f, 0.45f)
                .mustNotOutput(cycles, "x", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
        ;
    }

    /* will be moved to NAL multistep test file!!

    
    @Test public void deriveFromConjunctionComponents() { 
        TestNAR test = test();
        test.believe("(&&,<a --> b>,<b-->a>)", Eternal, 1.0f, 0.9f);

        
        test.mustBelieve(70, "<a --> b>", 1f, 0.81f);
        test.mustBelieve(70, "<b --> a>", 1f, 0.81f);

        test.mustBelieve(70, "<a <-> b>", 1.0f, 0.66f);
        test.run();
    }*/

    @Disabled
    @Test
    void testAbductionNegNegImplicationPred() {

        /*
        via contraposition:
        $.32 x. %1.0;.30% {11: 1;2} ((%1,%2,time(raw),belief(positive),task("."),time(dtEvents),notImpl(%2)),((%2 ==>+- %1),((Induction-->Belief))))
            $.21 ((--,y)==>x). %0.0;.47% {1: 2;;} ((((--,%1)==>%2),%2),(((--,%2) ==>+- %1),((Contraposition-->Belief))))
              $.50 ((--,x)==>y). %0.0;.90% {0: 2}
            $.50 y. %1.0;.90% {0: 1}
         */
        test
                .input("y. %1.0;0.90%")
                .input("--(--x ==> y).")
                .mustBelieve(cycles, "x", 1.0f, 0.45f)
                .mustNotOutput(cycles, "x", BELIEF, 0.0f, 0.5f, 0, 1, ETERNAL)
        ;
    }

    @Test
    void testDeductionPosNegImplicationPred() {
        test
                .believe("y")
                .believe("(y ==> --x)")
                .mustBelieve(cycles, "x", 0.0f, 0.81f)
                .mustNotOutput(cycles, "x", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
        ;
    }

    @Test
    void specificDeductionSubjConj() {
        test
            .volMax(8)
            .believe("((a && b) ==> x)")
            .believe("a")
            .mustBelieve(cycles, "(b ==> x)", 1, 0.81f)
        ;
    }
    @Test
    void specificDeductionSubjDisj() {
        test
            .volMax(8)
            .believe("((a || b) ==> x)")
            .believe("--a")
            .mustBelieve(cycles, "(b ==> x)", 1, 0.81f)
        ;
    }
    @Test
    void specificDeductionPredConj() {
        test
                .volMax(8)
                .believe("(x ==> (a&&b))")
                .believe("a")
                .mustBelieve(cycles, "(x ==> b)", 1, 0.81f)
        ;
    }
    @Test
    void specificDeductionPredDisj() {
        test
                .volMax(8)
                .believe("(x ==> (a||b))")
                .believe("--a")
                .mustBelieve(cycles, "(x ==> b)", 1, 0.81f)
        ;
    }
    @Disabled @Test
    void specificDeductionSubjDisj_Structural() {
        test
                .volMax(8)
                .believe("((a||b) ==> x)")
                .mustBelieve(cycles, "(b ==> x)", 1f, 0.81f)
        ;
    }

    @Disabled
    @Test
    void implConjNeutralize2() {
        test.volMax(10)
                .believe("((x && y) ==> z)")
                .believe("((x && --y) ==> --z)")
                .mustBelieve(cycles, "((y ==> z) && (--y ==> --z))", 1f, 0.81f)
        ;
    }

    @Test
    void implConjInvariant_1() {
        assertEq("x", "((x&&y)||(x&&--y))");
        test.volMax(20)
                .believe("((x &&   y) ==> z)")
                .believe("((x && --y) ==> z)")
                .mustBelieve(cycles, "(x ==> z)", 1f, 0.81f)
        ;
    }

    @Test
    void implPre_Symmetric_Abduction() {
        test.volMax(3)
                .believe("(a ==> z)")
                .believe("(b ==> z)")
                .mustBelieve(cycles, "(a ==> b)", 1f, 0.45f)
                .mustBelieve(cycles, "(b ==> a)", 1f, 0.45f)
        ;
    }

    @Test
    void implPre_Assymmetric_Abduction() {
        test.volMax(10)
                .believe("(a ==>   z)")
                .believe("(b ==> --z)")
                .mustBelieve(cycles, "(a ==> b)", 0f, 0.45f)
//            .mustBelieve(cycles, "(b ==> a)", 0f, 0.45f)
//            .mustBelieve(cycles, "(--a ==> b)", 1f, 0.45f)
                .mustBelieve(cycles, "(--b ==> a)", 1f, 0.45f)
        ;
    }

    @Disabled
    @Test
    void questionComponentShortCircuit() {
        test.volMax(3)
                .input("--x.")
                .input("(x && y)?")
                .mustBelieve(cycles, "(x && y)", 0f, 0.45f)
        ;
    }

    @Disabled
    @Test
    void questionComponentShortCircuit_neg() {
        test.volMax(4)
                .input("x.")
                .input("(--x && y)?")
                .mustBelieve(cycles, "(--x && y)", 0f, 0.45f)
        ;
    }

    @Disabled
    @Test
    void questionComponentShortCircuit_arity3() {
        test.volMax(8)
                .input("--x.")
                .input("(&&,w,x,#1)?")
                .mustBelieve(cycles, "(&&,w,x,#1)", 0f, 0.81f)
        ;
    }

    @Test
    void EquivEmulationNegatingBoth() {
        test.volMax(5)
                .believe("(x==>y)")
                .believe("(y==>x)")
                .mustBelieve(cycles, "(--x ==> --y)", 1f, 0.81f)
                .mustBelieve(cycles, "(--y ==> --x)", 1f, 0.81f)
        ;
    }


    @Test
    void oppositeImplicationRevision() {
        test.volMax(5)
                .believe("(x ==>+1 --x)")
                .believe("(--x ==>+1 x)")
                .mustNotBelieve(cycles, "x", (s, e) -> true)
                .mustBelieve(cycles, "(x ==>+2 x)", 1f, 0.81f)
                .mustBelieve(cycles, "(--x ==>+2 --x)", 1f, 0.81f)
        ;
    }


}