package nars.nal.nal3;

import nars.$;
import nars.Term;
import nars.subterm.util.SubtermCondition;
import nars.unify.constraint.SubConstraint;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Disabled abstract class NAL3DecomposeBeliefTest extends NAL3Test {

    @Disabled
    static class NAL3DecomposeBeliefSingle_Test extends NAL3DecomposeBeliefTest {
        @Test
        void diff_compound_decomposition_single3_intersect() {
            test.volMax(8);
            test.believe("((dinosaur & --ant) --> [strong])", 0.9f, 0.9f);
            test.mustBelieve(cycles, "(dinosaur --> [strong])", 0.90f, 0.73f);
            test.mustBelieve(cycles, "(ant --> [strong])", 0.10f, 0.73f);
        }


        @Test
        void diff_compound_decomposition_single() {

            test.believe("(robin --> (bird && --swimmer))", 0.9f, 0.9f);
            test.mustBelieve(cycles, "(robin-->bird)", 0.90f, 0.73f);

        }


        @Test
        void sect_compound_decomposition_single2() {

            test.believe("((dinosaur && ant) --> youth)", 0.9f, 0.9f);
            test.mustBelieve(cycles, "(dinosaur --> youth)", 0.9f, 0.81f);

        }

        @Test
        void testNegIntersection3BeliefSinglePremiseDecompose() {
            test
                    .volMax(12)
                    .input("--((&,a,b,c)-->g).")
                    .mustBelieve(cycles, "(a-->g)", 0f, 0.73f)
                    .mustBelieve(cycles, "(b-->g)", 0f, 0.73f)
                    .mustBelieve(cycles, "(c-->g)", 0f, 0.73f)
            ;
        }

    }

    static class NAL3DecomposeBeliefDouble_Test extends NAL3DecomposeBeliefTest {
        @Test
        void sect_compound_decomposition_double_pred_union() {
        /*
        <robin --> (|,bird,swimmer)>. 'Robin is a type of bird or a type of swimmer.
        <robin --> swimmer>. %0.00% 'Robin is not a type of swimmer.
        |-
        (robin-->bird). %1.00;0.81% 'Robin is a type of bird.
        */
            test.believe("(robin --> (bird|swimmer))");
            test.believe("--(robin --> swimmer)");
            test.mustBelieve(cycles, "(robin --> bird)", 1f, 0.81f);
            test.mustNotOutput(cycles, "(robin --> bird)", BELIEF, 0f, 0.5f, 0, 1)
            ;
        }

        @Test
        void sect_compound_decomposition_double_pred_diff() {
        /*
        <robin --> swimmer>. %0.00% 'Robin is not a type of swimmer.
        <robin --> (-,mammal,swimmer)>. %0.00% 'Robin is not a nonswimming mammal.
        |-
        ''outputMustContain('<robin --> mammal>. %0.00;0.81%') 'Robin is not a type of mammal.
        */
            test.believe("--(robin --> (mammal & --swimmer))");
            test.believe("--(robin --> swimmer)");
            test.mustBelieve(cycles, "(robin --> mammal)", 0f, 0.81f);
            test.mustNotOutput(cycles, "(robin --> mammal)", BELIEF, 0.5f, 1f, 0, 1)
            ;
        }


//    @Test
//    void testDifference() {
//
//        TestNAR tester = test;
//        tester.believe("<swan --> bird>", 0.9f, 0.9f);
//        tester.believe("<dinosaur --> bird>", 0.7f, 0.9f);
//        tester.mustBelieve(cycles, "bird:(swan ~ dinosaur)", 0.27f, 0.81f);
//        tester.mustBelieve(cycles, "bird:(dinosaur ~ swan)", 0.07f, 0.81f);
//    }

        @Test
        void testArity1_Decomposition_IntersectExt() {
            test
                    .believe("(a-->(b&&c))", 0f, 0.9f)
                    .believe("(a-->b)")
                    .mustBelieve(cycles, "(a-->c)", 0f, 0.81f);
        }

        @Test
        void testArity2_Decomposition_IntersectExt() {
            test
                    .believe("(a-->(&&,b,c,d))", 0f, 0.9f)
                    .believe("(a-->(&&,b,c))")
                    .mustBelieve(cycles, "(a-->d)", 0f, 0.81f);
        }

        @Disabled
        @Test
        void testSubjPred_Self_Factor_arity2() {
            test

                    .believe("((&&,a,b)-->(&&,a,c))", 1f, 0.9f)
                    .mustBelieve(cycles, "(b-->c)", 1f, 0.81f)
                    .mustNotOutput(cycles, "((a&&b)-->(a&&c))", QUESTION) //we already know this why ask it
//            .mustQuestion(cycles, "((a&&b)-->(&&,a,c,?1))") //nal3.guess
//            .mustQuestion(cycles, "((&&,a,b,?1)-->(a&&c))")
                    .mustNotOutput(cycles, "((a &&+- b)-->(&&,a,c,?1))", QUESTION) //+- not helpful
                    .mustNotOutput(cycles, "((&&,a,b,?1)-->(a &&+- c))", QUESTION)

            ;
        }

        @Disabled
        @Test
        void testSubjPred_Self_Factor_arity3() {
            test
                    .believe("((&&,x,b,y)-->(&&,x,c,y))", 1f, 0.9f)
                    .mustBelieve(cycles, "(b-->c)", 1f, 0.81f)
            ;
        }

        @Test
        void testArity1_Decomposition_Intersect_3_2() {
            test
                    .volMax(9)
                    .believe("(x-->(&&,a,b,c))", 0.9f, 0.9f)
                    .believe("(x-->(&&,a,b))", 0.9f, 0.9f)
                    .mustBelieve(cycles, "(x-->c)", 0.91f, 0.80f);
        }


        @Test
        void testArity1_Decomposition_Intersect_3_2_neg() {
            test
                    .volMax(11)
                    .believe("(x-->(&&,--a,--b,c))", 0.9f, 0.9f)
                    .believe("(x-->(&&,--a,--b))", 0.9f, 0.9f)
                    .mustBelieve(cycles, "(x-->c)", 0.91f, 0.8f);
        }

        @Test
        void testArity1_Decomposition_Intersect_3_2_neg_2() {


            test
                    .volMax(12)
                    .believe("(x-->(&&,--a,--b,c))", 0.25f, 0.9f)
                    .believe("(x-->(&&,--a,--b))", 0.5f, 0.9f)
                    .mustBelieve(cycles, "(x-->c)", 0.63f, 0.51f)
                    .mustNotBelieve(cycles, "(x-->(&&,--a,--b,c))", 0.5f, 0.81f)
            ;
        }

        @Test
        void testArity1_Decomposition_Union_3_2() {
            test
                    .confMin(0.6f)
                    .volMax(10)
                    .believe("(x-->(||,a,b,c))", 0.9f, 0.9f)
                    .believe("(x-->(||,a,b))", 0.1f, 0.9f)
                    .mustBelieve(cycles, "(x-->c)", 0.81f, 0.74f);
        }

        @Test
        void compound_decomposition_two_premises_union() {

            test.volMax(8);
            test.believe("<robin --> (bird || swimmer)>", 0.9f, 0.9f);
            test.believe("<robin --> swimmer>", 0.9f, 0.9f);
            test.mustBelieve(cycles, "(robin-->bird)", 0.09f, 0.15f);

        }

        @Test
        void testArity1_Decomposition_Union() {
            test
                    .volMax(8)
                    .believe("((b||c)-->a)", 0.9f, 0.9f)
                    .believe("(b-->a)", 0.1f, 0.9f)
                    .mustBelieve(cycles, "(c-->a)", 0.81f, 0.74f);
        }

        @Test
        void testArity1_Decomposition_Union2() {


            test
                    .volMax(8)
                    .believe("(a-->b)", 0.25f, 0.9f)
                    .believe("(a-->(b||c))", 0.25f, 0.9f)
                    .mustBelieve(cycles, "(a-->c)", 0.19f, 0.76f, ETERNAL);
        }

        @Test
        void testDisjoint2_int() {


            test
                    .volMax(6).confMin(0.8f)
                    .believe("--(x-->(RealNumber&&ComplexNumber))")
                    .believe("(x-->RealNumber)")
//                .mustBelieve(cycles, "(x-->ComplexNumber)", 0f, 0.45f) //single decomposition
                    .mustBelieve(cycles, "(x-->ComplexNumber)", 0f, 0.81f); //double decomposition

        }

        @Test
        void testDisjoint2_int_weak() {


            test
                    .volMax(6)
                    .believe("--(x-->(RealNumber&&ComplexNumber))")
                    .believe("(x-->RealNumber)", 0.75f, 0.9f)
                    .mustBelieve(cycles, "(x-->ComplexNumber)", 0 /*0.25f*/, 0.61f); //double decomposition

        }

        @Test
        void testDisjoint2_ext() {


            test
                    .volMax(6)
                    .believe("--((RealNumber&&ComplexNumber)-->x)")
                    .believe("(RealNumber-->x)")
//                .mustBelieve(cycles, "(ComplexNumber-->x)", 0f, 0.45f) //single decomposition
                    .mustBelieve(cycles, "(ComplexNumber-->x)", 0, 0.81f); //double decomposition

        }

        @Disabled
        @Test
        void testDisjoint2Learned() {
            test
                    .volMax(6)
                    .believe("--(x-->ComplexNumber)")
                    .believe("(x-->RealNumber)")
                    .mustBelieve(cycles, "(x-->(RealNumber-ComplexNumber))", 1f, 0.81f);

        }

        @Test
        void testDisjoint3() {

            test
                    .volMax(8)
                    .believe("--(x-->(&,RealNumber,ComplexNumber,Letter))")
                    .believe("(x-->RealNumber)")
                    .mustBelieve(cycles, "(x-->(ComplexNumber&&Letter))", 0f, 0.81f)
            ;

        }

        @Test
        void testDisjointWithVarPos() {


            test

                    .volMax(8)
                    .believe("(#1-->(RealNumber&ComplexNumber))")
                    .believe("(x-->RealNumber)")
                    .mustNotBelieve(cycles, "(x-->((--,ComplexNumber)&&RealNumber))")
                    .mustNotBelieve(cycles, "(x-->ComplexNumber)", 0, 0.81f, (s, e) -> true)
                    .mustBelieve(cycles, "(x-->ComplexNumber)", 1f, 0.81f)
            ;

        }

        @Test
        void testDisjointWithVarNeg() {


            test
                    .believe("--(#1-->(RealNumber&ComplexNumber))")
                    .believe("(x-->RealNumber)")
                    .mustBelieve(cycles, "(x-->ComplexNumber)", 0f, 0.81f)
            ;

        }


        @Disabled
        @Test
        void testDifferenceQuestion() {
            test
                    .volMax(8)
                    .believe("((x&&y)-->a)")
                    .mustQuestion(cycles, "((x&&--y)-->a)")
                    .mustQuestion(cycles, "((y&&--x)-->a)")
            ;
        }

        @Disabled
        @Test
        void testDifferenceQuest() {
            test

                    .volMax(8)
                    .goal("((x&&y)-->a)")
                    .mustQuest(cycles, "((x&&--y)-->a)")
                    .mustQuest(cycles, "((y&&--x)-->a)")
            ;
        }


        @Test
        @Disabled
        void DecomposeWTF() {
    /* wrong:
    $.05 (0-->x). 1 %1.0;.54% {419: 1;2;3©} (S --> M), X, is(S,"|"), subOf(S,X)   |-         (X --> M), (Belief:StructuralDeduction,Goal:StructuralDeduction)
        $.09 (((2-1)|(--,0))-->x). 1 %1.0;.60% {127: 1;2;3} (P --> M), (S --> M), notSetsOrDifferentSets(S,P), neq(S,P) |- ((polarizeTask(P) | polarizeBelief(S)) --> M), (Belief:IntersectionDepolarized)
        $.19 ((2-1)-->x). 1⋈2 %1.0;.76% {84: 2;3}
     */


            {
                Term cn = $$("((_2-_1)|(--,_3))");
//        Term cp = $$("((_2-_1)|_3)");
                Term xp = $$("_3");
//        Term xn = $$("(--,_3)");
//        assertFalse(cn.contains(xp));
//        assertFalse(cp.contains(xn));
//        assertTrue(cn.contains(xn));
//        assertTrue(cp.contains(xp));

                assertFalse(
                        new SubConstraint(SubtermCondition.Subterm, $.varDep(1), $.varDep(2))
                                .valid(cn, xp, null)
                );
            }

            test
                    .volMax(9)
                    .believe("( ((a2-a1) & --a0) --> x)")
                    .mustBelieve(cycles, "(a0-->x)", 0, 0.73f)
                    .mustNotOutput(cycles, "(a0-->x)", BELIEF, 0.5f, 1f, 0, 0.99f, (t) -> true)

            ;
        }


    }


}