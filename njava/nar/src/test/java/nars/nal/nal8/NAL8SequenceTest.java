package nars.nal.nal8;

import jcog.data.list.Lst;
import nars.*;
import nars.test.NALTest;
import nars.time.Tense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/**
 * test precision of sequence execution (planning)
 */
public class NAL8SequenceTest extends NALTest {

    public static final int cycles = 5;

    @BeforeEach
    void init() {
        test.volMax(19); test.confTolerance(0.7f /* HACK */);
    }


    @Test
    void Goal_After_Simple() {

        test.volMax(3)
                .input("(z &&+1 x)!")
                .input("z.")
                .mustGoal(cycles, "x", 1, 0.81f) 
        ;
    }
    @Test
    void Goal_After_Simple_Neg() {
        test.volMax(4)
                .input("(--z &&+1 x)!")
                .input("--z.")
                .mustGoal(cycles, "x", 1, 0.81f)
        ;
    }

    @Test
    void Should_Pos_Par() {
        test.volMax(6)
            .input("(a && b)!")
            .input("--a.")
            .mustGoal(cycles, "a", 1, 0.81f)
        ;
    }

    @Test
    void Should_Neg_Par() {
        test.volMax(6)
            .input("--(a && b)!")
            .input("a.")
            .mustGoal(cycles, "--a", 1, 0.81f)
        ;
    }
    @Test
    void Should_Neg_Seq() {
        test.volMax(6)
            .input("--(a &&+1 b)!")
            .input("a.")
            .mustGoal(cycles, "--a", 1, 0.81f)
        ;
    }
    @Test
    void Goal_After_Simple_EventNeg() {

        test
                .input("(z &&+1 x)!")
                .input("--z.")
                .mustGoal(cycles, "z", 1, 0.81f)
                .mustNotGoal(cycles, "x")
                .mustNotGoal(cycles, "(z &&+1 x)")
        ;
    }

    @Test
    void Goal_After_Parallel1() {
        test
                .input("((w&&z) &&+1 x)!")
                .input("z.")
                .mustGoal(cycles, "(w &&+1 x)", 1, 0.81f) 
        ;
    }
    @Test
    void Goal_After_Parallel1_EventNeg() {
        test
            .input("((w&&z) &&+1 x)!")
            .input("--z.")
            .mustGoal(cycles, "z", 1, 0.81f)
            .mustNotGoal(cycles, "w")
            .mustNotGoal(cycles, "x")
            .mustNotGoal(cycles, "(z &&+1 x)")
        ;
    }

    @Test
    void Goal_Before_Mid_ParEvent() {

        test
                .input("(x && y)!")
                .input("(z &&+1 (x && y)).")
                .mustGoal(cycles, "z", 1, 0.81f)
        ;
    }
    @Test
    void Goal_Before_Mid_ParEvent_Neg() {

        test
                .input("--(x && y)!")
                .input("(z &&+1 --(x && y)).")
                .mustGoal(cycles, "z", 1, 0.81f)
        ;
    }

    @Test
    void Goal_Before_Mid_ComplexEvent() {

        test
                .input("(x &&+1 y)!")
                .input("(z &&+1 (x &&+1 y)).")
                .mustGoal(cycles, "z", 1, 0.81f) 
        ;
    }

    @Test
    void Goal_Before_Mid_ComplexEvent2() {

        test
                .volMax(14)
                .confMin(0.75f)
                .input("(x &&+1 y)!")
                .input("(z &&+1 ((x &&+1 y) &&+1 w)).")
                .mustGoal(cycles, "z", 1, 0.81f) 
        ;
    }

    @Test
    void Goal_Before_Mid_ComplexSequence2() {

        test
                .input("c!")
                .input("(a &&+1 (b &&+1 (c &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 b)", 1, 0.81f) 
        ;
    }

    @Test
    void StartSequenceBefore_2ary() {

        test
                .input("b!")
                .input("((a&&b) &&+1 c).")
                .mustGoal(cycles, "a", 1, 0.81f) 
        ;
    }

    @Test
    void Goal_Before_Mid_ComplexSequence() {

        test
                .input("c!")
                .input("(a &&+1 (b &&+1 ((c&&f) &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 (b &&+1 f))", 1, 0.81f) 
        ;
    }

    @Test
    void Goal_Before_Without_AntiGoal_P() {
        test
                .input("c!")
                .input("(--c &&+1 (a &&+1 (b &&+1 c))).")
                .mustGoal(cycles, "(a &&+1 b)", 1, 0.81f)
        ;
    }
    @Test
    void Goal_Before_Without_AntiGoal_N() {
        test
                .input("--c!")
                .input("(c &&+1 (a &&+1 (b &&+1 --c))).")
                .mustGoal(cycles, "(a &&+1 b)", 1, 0.81f)
        ;
    }

    @Test
    void Goal_Before_MidUni() {

        test
                .volMax(24)
                .input("c(x)!")
                .input("(a &&+1 (b(#1) &&+1 ((&&,a,b,c(#1),d(x,y)) &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 (b(x) &&+1 (&&,a,b,d(x,y))))", 1, 0.81f)
        ;
    }

    @Test
    void Goal_Before_Mid_Alternate_Sort() {

        test
                .input("f!")
                .input("(a &&+1 (b &&+1 ((c&&f) &&+1 (d &&+1 e)))).")
                .mustNotOutput(cycles, "((a &&+1 b) &&+1 (c &&+1 d))", GOAL, 0, 1, 0, 0.81f)
                .mustGoal(cycles, "(a &&+1 (b &&+1 c))", 1, 0.81f) 
        ;
    }

    @Test
    void Goal_Before_Mid_EventNegative() {

        test
                .input("--c!")
                .input("(a &&+1 (b &&+1 (--c &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 b)", 1, 0.81f) 
        ;
    }

    @Test
    void Goal_Before_FinalEvent() {
        test
                .input("e!")
                .input("(a &&+1 (b &&+1 (c &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 (b &&+1 (c &&+1 d)))", 1, 0.81f) 
                .mustNotGoal(cycles, "(a &&+1 (b &&+1 (c &&+1 d)))", 0, 0.5f)
        ;
    }

    @Test
    void Goal_Not_Before_FinalEvent_Disj_Confirm() {
        test
            .input("--e!")
            .input("(a &&+1 e).")
            .mustGoal(cycles, "a", 0, 0.81f)
        ;
    }

    @Test
    void Goal_Not_After_Not() {
        test
            /* (--a ||+1 --e) */
            .input("--(a &&+1 e)!")
            .input("a.")
            .mustGoal(cycles, "e", 0, 0.81f)
        ;
    }

    @Test
    void Goal_Not_After_Not_2() {
        test
                /* (--a ||+1 --e) */
                .input("--(a &&+1 e)!")
                .input("e.")
                .mustGoal(cycles, "a", 0, 0.81f)
        ;
    }


    @Test
    void Goal_Before_FinalEvent_Disj_Confirm_NP() {
        test
                .input("--e!")
                .input("--(--a &&+1 --e).") //(a ||+1 e)
                .mustGoal(cycles, "a", 1, 0.81f)
                .mustNotGoal(cycles, "e")
        ;
    }
    @Test
    void Goal_Before_FinalEvent_Disj_ConfirmPN() {
        test
                .input("e!")
                .input("--(a &&+1 e).") //(--a ||+1 --e)
                .mustGoal(cycles, "a", 0, 0.81f)
                .mustNotGoal(cycles, "e")
        ;
    }
    @Test
    void GoalNot_Before_Mid_EventNegative() {
        test
                .input("--e!")
                .input("(d &&+1 e).")
                .mustGoal(cycles, "d", 0, 0.81f) 
        ;
    }

    /**
     * tests Demand truth function's inversion
     */
    @Test
    void GoalNot_Before_Mid_EventNegative2() {
        test
                .input("--e!")
                .input("(a &&+1 (b &&+1 (c &&+1 (d &&+1 e)))).")
                .mustGoal(cycles, "(a &&+1 (b &&+1 (c &&+1 d)))", 0, 0.81f)
                .mustNotGoal(cycles, "(a &&+1 (b &&+1 (c &&+1 d)))", 0.5f, 1)
        ;
    }


    @Test
    void UnifyConclusionSequenceOutcomeSimpler() {

        test
                .input("e(x)!")
                .input("((a,#1) &&+1 (b &&+1 e(#1))).")
                .mustGoal(cycles, "((a,x) &&+1 b)", 1, 0.81f) 
        ;
    }

    @Test
    void UnifyConclusionSequenceOutcome() {
        test
                .input("e(x)!")
                .input("((a,#1) &&+1 (b &&+1 (c &&+1 (d &&+1 e(#1))))).")
                .mustGoal(cycles, "((a,x) &&+1 (b &&+1 (c &&+1 d)))", 1, 0.81f) 
        ;
    }

    @Test
    void BeliefDeduction_MidSequenceDTernalComponent() {

        test
                .input("c.")
                .input("(a &&+1 (b &&+1 ((c&&f) &&+1 (d &&+1 e)))).")
                .mustBelieve(cycles, "(a &&+1 (b &&+1 (f &&+1 (d &&+1 e))))", 1, 0.81f)
        ;
    }

    @Test
    void BeliefDeduction_MidSequenceDTernalComponentWithUnification() {

        test
                .volMax(26)
                .input("c(x).")
                .input("(a &&+1 (b(#1) &&+1 ((&&,a,b,c(#1),d(x,#1)) &&+1 (d &&+1 e(#1))))).")
                .mustBelieve(cycles, "(a &&+1 (b(x) &&+1 ((&&,a,b,d(x,x)) &&+1 (d &&+1 e(x)))))", 1, 0.81f)
        ;
    }

    @Test
    void Belief_Deduction_ComplexSeq_Uni() {

        test
                .volMax(26)
                .input("(a &&+1 ((b(x)&&c) &&+1 ((c(#1) && d(x,#1)) &&+1 (d &&+1 e(#1))))).")
                .input("(b(x)&&c).")
                .mustBelieve(cycles, "(a &&+2 (((d(x,#1)&&c(#1)) &&+1 d) &&+1 e(#1)))", 1, 0.81f) 
        ;
    }

    @Test
    void beliefDeduction_MidSequence_Conj2() {

        test
                .volMax(20)
                .input("(a &&+1 ((&&,a,b,c,d) &&+1 c)).")
                .input("((b && d) &&+1 c).")
                .mustBelieve(cycles, "(a &&+1 (a&&c))", 1, 0.81f)
        ;
    }
    @Test
    void beliefDeduction_MidSequence_Conj2b() {

        test
            .volMax(16)
            .inputAt( 0,"(z &&+1 (&&,a,b,c)). |")
            .inputAt(1, "(b && c). |")
            .mustBelieve(cycles, "(z &&+1 a)", 1, 0.81f, 0)
            .mustNotBelieve(cycles, "(z &&+1 (&&,a,b,c))", 1f, 0.81f, (s,e)->true) //weak repeat
        ;
    }
    @Test
    void beliefDeduction_MidSequence_Conj2c() {

        test
                .volMax(26)
                .input("(a &&+1 (&&,a(#1),b,c(#1))).")
                .input("(b && c(x)).")
                .mustBelieve(cycles, "(a &&+1 a(x))", 1, 0.81f)
        ;
    }
    @Test
    void question_MidSequence_Conj() {
        test
                .volMax(24)
                .input("(a &&+1 ((b(x)&&c) &&+1 ((c(#1) && d(x,#1)) &&+1 e(#1)))).")
                .inputAt(2, "(b(x)&&c)? |")
                .mustQuestion(cycles, "((a &&+2 (d(x,#1)&&c(#1))) &&+1 e(#1))", (s, e) -> s == 1)
        ;
    }

    @Test
    void question_unification_MidSequence_Conj() {

        test
                .volMax(24)
                .input("(a &&+1 ((b(#1)&&c) &&+1 ((c(#1) && d(x,#1)) &&+1 e(#1)))).")
                .inputAt(2, "(b(y)&&c)? |")
                .mustQuestion(cycles, "((a &&+2 (d(x,y)&&c(y))) &&+1 e(y))", (s, e) -> s == 1)
        ;
    }

    @Test
    void beliefDeduction_MidSequence_Disj() {

        test
                .volMax(20)
                .input("(a &&+1 ((b(x)||c) &&+1 (c(#1) &&+1 (d &&+1 e)))).")
                .input("c.")
                .mustBelieve(cycles, "((c(#1) &&+1 d) &&+1 e)", 1, 0.81f) 
        ;
    }

    @Disabled @Test
    void Deduction_MidSequenceDTernalComponentWithUnification() {

        String g = "(a &&+1 ((b(#1)&&c) &&+1 ((&&,c(#1),d(x,#1)) &&+1 d)))";
        String b = "c(x)";
        test
                .volMax(21)
                .believe(g)
                .believe(b)
                .mustBelieve(cycles, "((&&,d(x,x)) &&+1 d)", 1, 0.81f)
        ;
    }

    @Test
    void Goal_Before_Mid_ComplexEvent_Uni2() {
        Term c = $$("((d(x,#1) &&+1 e(#1)) &&+1 ((b(#1)&&c) &&+1 c(#1)))");
        Term e = $$("(b(x) &&+1 c(x))");
        Term y = $$("((d(x,x) &&+1 e(x)) &&+1 c)");

        test.volMax(24);
        test
                .believe(c.toString())
                .goal(e.toString())
                .mustGoal(cycles, y.toString(), 1, 0.81f) 
        //.mustGoal(cycles, "(&&,a,b,d(x,x))", 1, 0.81f) 
        ;
    }


    @Test
    void Goal_Before_Mid_ComplexEvent_Uni() {
        test
                .input("(y(#1,#2) &&+1 x(#1,#2)).")
                .input("x(1,1)!")
                .mustGoal(cycles, "y(1,1)", 1, 0.81f) 
        ;
    }

    @Test
    void GoalDeduction_Neg_ParallelWithDepVar() {
        test
                .input("(--x(#1,#2) && y(#1,#2)).")
                .input("--x(1,1)!")
                .mustGoal(cycles, "y(1,1)", 1, 0.81f) 
        ;
    }

    @Test
    void GoalDeduction_ParallelWithDepVar_and_Arithmetic() {
        test
                .volMax(24)
                .input("x(1,1)!")
                .input("(&&, x(#1,#2), (add(#1,#2)=#3), y(#3)).")
                .mustGoal(cycles, "y(2)", 1, 0.81f)
        ;
    }

    @Disabled @Test
    void Deduction_ParallelWithDepVar_and_Specific_Arithmetic() {
        test
                .volMax(17)
                .input("(&&, x(#1,#2), y(#1,#2), --(#1=#2)).")
                .input("x(1,1).")
                .input("x(1,2).")
                .mustBelieve(cycles, "y(1,2)", 1, 0.81f) 
                .mustNotOutput(cycles, "y(1,1)", BELIEF) 
        ;
    }


    @Disabled
    @Test
    void one() throws Narsese.NarseseException {

        NAR n = NARS.tmp();
        n.complexMax.set(20);
        n.time.dur(4);

        Lst<Term> log = new Lst();
        String goal = "done";
        n.addOp1("f", (x, nar) -> {
            System.err.println(x);

            //quench
            nar.want($.func("f", x).neg(), 1f, nar.confDefault(GOAL), Tense.Present);

            if (!log.isEmpty()) {
                Term prev = log.getLast();
                if (prev.equals(x))
                    return; //same
                nar.believe($.func("f", prev).neg(), nar.time()); //TODO truthlet quench?
            }

            log.add(x);

            nar.believe($.func("f", x), nar.time());

            //reinforce
            n.want($$(goal), 1f, n.confDefault(GOAL), Tense.Present);
        });

        String sequence = "(((f(a) &&+2 f(b)) &&+2 f(c)) &&+2 done)";
        n.believe(sequence);
        //n.want($$(goal), Tense.Eternal /* Present */, 1f);
        n.want($$(goal), 1f, n.confDefault(GOAL), Tense.Present);
        n.run(1400);
    }

    @Test
    void taskEventConjBeforeCorrectTime() {
        /*
        $0.0 (trackXY-->left)! 172456⋈172471 %.10;.01% {172470: m;2~ú;2À7}
            $.13 ((--,(trackXY-->left))&&(--,(trackXY-->near)))! %1.0;.01% {172464: m}
            $.02 ((--,(trackXY-->left)) &&+64 ((--,(trackXY-->left))&&(--,(trackXY-->near)))). 172392⋈172407 %.90;.81% {172470: 2~ú;2À7}
        */
        test
                .input("x!")//eternal
                .input("(y &&+10 x). |") //temporal
                .mustGoal(cycles, "y", 1, 0.81f, t -> t >= 0 /*-10*/);
    }

    @Test
    void taskEventConjBeforeCorrectTime2() {

        test
                .input("(--x&&z)!")//eternal
                .input("(y &&+10 (--x&&z)). |") //temporal
                .mustOutput(cycles, "y", GOAL, 1, 1, 0.6f, 0.81f, t -> t >= 0 /*-10*/);
    }


    @Disabled
    @Test
    void goalSequenceStructural() {
        test
                .input("((x &&+1 y) &&+1 z)!")
                //.input("x?")
                .mustGoal(cycles, "x", 1, 0.81f)
                //.mustNotGoal(cycles, "y")
                //.mustNotGoal(cycles, "z")
        ;
    }

    @ParameterizedTest
    @Disabled
    @ValueSource(strings = "&&")
    void SubParallelOutcome(String conj) {

        test
                .input("x!")
                .input('(' + conj + ",x,y,z).")
                .mustGoal(cycles, "(y" + conj + "z)", 1, 0.81f) 
        ;
    }

}