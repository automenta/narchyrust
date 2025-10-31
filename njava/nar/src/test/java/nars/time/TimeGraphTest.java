package nars.time;

import com.google.common.collect.Sets;
import jcog.data.list.Lst;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.Narsese;
import nars.Term;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeGraphTest {

    /**
     * example time graphs
     */
    private final List<Runnable> afterEach = new Lst<>();

    private final TimeGraph A = time();
    private final TimeGraph B = time();

    {
        A.know($$("((one &&+1 two) ==>+1 (three &&+1 four))"), ETERNAL);
        A.know($$("one"), 1);
        A.know($$("two"), 20);
    }

    {
        B.know($$("(y ==>+3 x)"), ETERNAL);
        B.know($$("(y ==>+2 z)"), ETERNAL);
    }

    private static LambdaTimeGraph time(long seed) {
        return time(seed, false);
    }

    private static LambdaTimeGraph time(long seed, boolean trace) {
        return new LambdaTimeGraph(new XoRoShiRo128PlusRandom(seed)) {

            @Override
            protected boolean solution(Event s) {
                if (trace) {
                    System.err.println("SOLUTION: " + s);
                    Thread.dumpStack();
                    System.out.println();
                }
                return super.solution(s);
            }

        };
    }

    private static LambdaTimeGraph time() {
        return time(1);
    }

    @Test
    void AtomEvent() {
        assertSolved("one", A, "one@1", "one@19");
    }

    @Test
    void SimpleConjWithOneKnownAbsoluteSubEvent1() {
        assertSolved("(one &&+1 two)", A,
                "(one &&+1 two)@1", "(one &&+1 two)@19");
    }

    @Test
    void SimpleConjWithOneKnownAbsoluteSubEvent2() {
        assertSolved("(one &&+- two)", A,
                "(one &&+1 two)@1", "(one &&+19 two)@1", "(one &&+1 two)@19", "(two &&+17 one)@2", "(one &&+1 two)@21");
    }

    @Test
    void SimpleConjOfTermsAcrossImpl1() {
        assertSolved("(two &&+1 three)", A,
                "(two &&+1 three)@2", "(two &&+1 three)@20");
    }

    @Test
    void SimpleConjOfTermsAcrossImpl2() {
        assertSolved("(two &&+- three)", A,
                "(two &&+1 three)@2", "(three &&+17 two)@3", "(two &&+1 three)@2", "(two &&+1 three)@2", "(two &&+1 three)@20", "(two &&+19 three)@2");
    }

    @Test
    void SimpleImplWithOneKnownAbsoluteSubEvent_fwd() {
        var C = time();
        C.know($$("y"), 0);
        C.know($$("x"), 1);
        assertSolved("(y ==>+- x)", C, "(y ==>+1 x)");
    }

    @Test
    void SimpleImplWithOneKnownAbsoluteSubEvent_rev() {
        var C = time();
        C.know($$("y"), 1);
        C.know($$("x"), 0);
        assertSolved("(y ==>+- x)", C, "(y ==>-1 x)");
    }

    @Test
    void SimpleImplWithOneKnownAbsoluteSubEvent_subj_neg() {
        var C = time();
        C.know($$("y"), 1);
        C.know($$("x"), 0);
        assertSolved("(--y ==>+- x)", C, "((--,y) ==>-1 x)");
    }

    @Test
    void SimpleImplWithOneKnownAbsoluteSubEvent2() {

        var g = time();
        g.know($$("(x ==>+1 y)"), 1);
        g.know($$("x"), 1);
        assertSolved("y", g, "y@2");
    }

    @ParameterizedTest
    @ValueSource(strings = {"==>+1", "==>-1", "&&+1", "&&-1"})
    void SimpleSelfImplWithOneKnownAbsoluteSubEvent2(String rel) {


        var g = time();
        g.know($$("(x " + rel + " x)"));
        g.know($$("x"), 1);

        assertSolved("x", g, "x@1", "x@2", "x@0" /* .. */);

    }

    @Test
    void implInvert1() {
        var g = time();
        g.know($$("(--x ==>+1 x)"));
        g.know($$("x"), 1);
        assertSolved("--x", g, "(--,x)@0");
    }

    @Test
    void implInvert2() {
        var g = time();
        g.know($$("(--x ==>+1 x)"));
        g.know($$("--x"), 1);
        assertSolved("x", g, "x@2");
    }

    @Test
    void ImplNegSubjFwd() {
        //$0.71089435 ($.75 r! 105200⋈105300 %1.0;.75% >> $.18 ((--,a) ==>-20 r). 14080⋈14760 %.50;.22%)
        var g = time();
        g.know($$("((--,a) ==>+1 r)"));
        g.know($$("r"), 1);
        assertSolved("(--,a)", g, "(--,a)@0");
        assertSolved("a", g, "a@0");
    }

    @Test
    void ImplNegSubjRev() {
        var g = time();
        g.know($$("((--,a) ==>-1 r)"));
        g.know($$("r"), 1);
        assertSolved("(--,a)", g, "(--,a)@2");
        assertSolved("a", g, "a@2");
    }

    @Test
    void implInvert3() {
        var g = time();
        g.know($$("(x ==>+1 y)"));
        g.know($$("(y ==>+1 z)"));
//        assertSolved("(x ==>+- z)", g, "(x ==>+2 z)");
        assertSolved("--(x ==>+- z)", g, "(--,(x ==>+2 z))");
    }


    @Test
    void SimpleImplWithOneKnownAbsoluteSubEventNegOpposite() {

        var cc1 = time();
        cc1.know($$("(x ==>+1 y)"), 1);
        cc1.know($$("x"), 1);
        assertSolved("y", cc1, "y@2");
    }

    @Disabled @Test
    void implSubjNegDecompose0a() {
        var g = time();
        g.know($$("((--,r) ==>+10 a)"));
        assertSolved("(a ==>+- r)", g, "(a ==>-10 r)");
    }

    @Disabled @Test
    void implSubjConjLoopDecompose() {
        var g = time();
        g.know($$("((r &&+1 (--,r)) ==>+1 a)"));
        assertSolved("(a ==>+- r)", g, "(a ==>-2 r)");
    }

    @Disabled
    @Test
    void implSubjDisjLoopDecompose1() {
        var g = time();
        g.know($$("((--,((--,r) &&+670 r))==>a)"));
        assertSolved("((--,r) ==>+- a)", g, "((--,r) ==>+670 a)");
    }

    @Disabled
    @Test
    void implSubjDisjLoopDecompose2() {
        var g = time();
        g.know($$("((--,((--,r) &&+670 r))==>a)"));
        assertSolved("(r ==>+- a)", g, "(r==>a)");
    }

    @Test
    void implDisj0() {
        var g = time();
        g.know($$("(--x ==> a)"));
        g.know($$("((--x || y) ==> z)"));
        assertSolved("(--a ==>+- z)", g, "((--,a)==>z)");
        assertSolved("(  a ==>+- z)", g, "(a==>z)");
    }

    @Test
    void implDisj() {
        var g = time();
        g.know($$("((x && --y)==>a)"));
        g.know($$("((x &&   y)==>a)"));
        assertSolved("(x ==>+- a)", g, "(x==>a)");
        assertSolved("(--(--(x&&--y) &&+- --(x&&y)) ==>+- a)", g,
                "(x==>a)");
    }

    @Test
    void ImplChain1() {

        assertSolved("(z ==>+- x)", B, "(z ==>+1 x)");
    }

    @Test
    void ImplChain2() {
        assertSolved("(x ==>+- z)", B, "(x ==>-1 z)");
    }

    @Test
    void ConjChain1() throws Narsese.NarseseException {
        /** c is the same event, @6 */
        var cc1 = time();
        cc1.know($("(a &&+5 b)"), 1);
        cc1.know($("(b &&+5 c)"), 6);

        assertSolved("(a &&+- c)", cc1,
                "(a &&+10 c)@1");
    }

    @Test
    void ConjNegComponent() throws Narsese.NarseseException {

        var g = time();
//        g.autoneg = true;
        g.know($("(--x || y)"));
        g.know($("x"), 0);
        assertSolved("y", g, "y@0");
        assertSolved("(--x || y)", g, "(y||(--,x))@0");
    }

    @Test
    void Exact() throws Narsese.NarseseException {
        var g = time();
        g.know($("(a &&+5 b)"), 1);

        assertSolved("(a &&+- b)", g, "(a &&+5 b)@1");
    }

    @Test
    void conjNeg1() throws Narsese.NarseseException {

        var g = time();
        g.know($("a"), 1);
        g.know($("b"), 2);
        g.know($("c"), 3);
        g.know($("d"), 4);

        assertSolved("--(b &&+- c)", g,
                "(--,(b &&+1 c))@2");

        assertSolved("((a &&+- (--,(b &&+- c))) &&+- d)", g,
                "((a &&+1 (--,(b &&+1 c))) &&+1 d)@1");
    }

    @Test
    void LinkedTemporalConj() throws Narsese.NarseseException {

        var g = time();
        g.know($("(a &&+5 b)"), 1);
        g.know($("(b &&+5 c)"), 6);

        assertSolved("((b &&+5 c) &&+- (a &&+5 b))", g,
                "((a &&+5 b) &&+5 c)@1"
        );
    }

    @Disabled
    @Test
    void nal3_inh_component_a() throws Narsese.NarseseException {
        var g = time();
        g.know($("((x&y)-->a)"), 1);
        assertSolved("(x-->a)", g, "(x-->a)@1");
    }

    @Disabled
    @Test
    void nal3_inh_component_b() throws Narsese.NarseseException {
        var g = time();
        g.know($("z"), 0);
        g.know($("(x-->a)"), 1);
        assertSolved("(z &&+- ((x&&y)-->a))", g,
                "(z &&+1 ((x-->a)&&(y-->a)))@0");
    }

    @Test
    void ImplWithConjPredicate1() {
        ExpectSolutions t = assertSolved(
                "(one ==>+- (two &&+1 three))", A,
                "(one ==>+1 (two &&+1 three))", "(one ==>+19 (two &&+1 three))", "(one ==>-17 (two &&+1 three))");
    }

    @Test
    void ImplWithConjPredicate1a() {
        ExpectSolutions t = assertSolved(
                "(one ==>+- two)", A,
                "(one ==>+1 two)", "(one ==>+19 two)"
                , "(one ==>-17 two)"
        );
    }

    @Test
    void DecomposeImplConj() throws Narsese.NarseseException {
        /*
          $.02 ((b &&+10 d)==>a). 6 %1.0;.38% {320: 1;2;3;;} (((%1==>%2),(%1==>%2),is(%1,"&&")),((dropAnyEvent(%1) ==>+- %2),((StructuralDeduction-->Belief))))
            $.06 (((b &&+5 c) &&+5 d) ==>-15 a). 6 %1.0;.42% {94: 1;2;3} ((%1,%2,time(raw),belief(positive),task("."),notImpl(%2),notImpl(%1)),((%2 ==>+- %1),((Induction-->Belief))))
        */
        var g = time();
        g.know($("(((b &&+5 c) &&+5 d) ==>-15 a)"), 6);
        assertSolved("((b &&+5 c) ==>+- a)", g, "((b &&+5 c) ==>-10 a)");
        assertSolved("((b &&+10 d) ==>+- a)", g, "((b &&+10 d) ==>-15 a)");
    }

    @Test
    void ImplWithConjPredicate2pre() {
        assertSolved("(two &&+- three)", A,
                "(three &&+17 two)@3", "(two &&+1 three)@2", "(two &&+1 three)@20", "(two &&+19 three)@2"
        );
    }

    @Test
    void ImplWithConjPredicate2() {
        //results may be random
        assertSolved("(one ==>+- (two &&+- three))", A,
                //TODO verify this is correct
                "(one ==>+1 (two &&+1 three))", "(one ==>+1 (two &&+19 three))", "(one ==>+19 (two &&+1 three))",
                "(one ==>+19 (two &&+19 three))", "(one ==>+2 (three &&+17 two))",
                "(one ==>+20 (three &&+17 two))", "(one ==>-16 (three &&+17 two))",
                "(one ==>-17 (two &&+1 three))", "(one ==>-17 (two &&+19 three))", "(one ==>-34 (three &&+17 two))]"

                //"(one ==>+- (three &&+17 two))", "(one ==>+- (two &&+1 three))", "(one ==>+- (two &&+19 three))", "(one ==>+1 (two &&+19 three))", "(one ==>+19 (two &&+1 three))", "(one ==>+19 (two &&+19 three))", "(one ==>+38 (three &&+17 two))"
                //"(one ==>+19 (two &&+19 three))"
                //"(one ==>+2 (three &&+17 two))", "(one ==>+20 (three &&+17 two))", "(one ==>-16 (three &&+17 two))", "(one ==>-17 (two &&+1 three))", "(one ==>-17 (two &&+19 three))"
        );
    }

    @Test
    void Conj3() throws Narsese.NarseseException {
        var C = time();
        C.know($("a"), 1);
        C.know($("b"), 2);
        C.know($("c"), 3);
        assertSolved("(&&+-, a,b,c)", C, "((a &&+1 b) &&+1 c)@1");
    }

    @Test
    void Conj3_parallel_seq() throws Narsese.NarseseException {
        var C = time();
        C.know($("(a&&b)"), 1);
        C.know($("c"), 2);
        assertSolved("(&&+-, a,b,c)", C, "((a&&b) &&+1 c)@1");
    }

    @Test
    void Conj3_parallel_seq_dur() throws Narsese.NarseseException {
        var C = time();
        C.know($("(a&&b)"), 1, 3);
        C.know($("c"), 2, 4);
        assertSolved("(&&+-, a,b,c)", C, "((a&&b) &&+1 c)@1..3");
    }

    @Test
    void Conj3_multiple_solutions() throws Narsese.NarseseException {
        var C = time();
        C.know($("a"), 0);
        C.know($("a"), 1);
        C.know($("b"), 2);
        C.know($("c"), 3);
        assertSolved("(&&+-, a,b,c)", C,
                "((a &&+1 b) &&+1 c)@1", "((a &&+2 b) &&+1 c)@0");
        C.graph.print();
    }

    @Test
    void Conj_repeat() throws Narsese.NarseseException {
        var C = time();
        C.know($("(a ==>+1 a)"));
        C.know($("a"), 0);
        assertSolved("(a &&+- a)", C,
                "(a &&+1 a)@-1", "(a &&+1 a)@0");
    }

    @Test
    void Impl_repeat() throws Narsese.NarseseException {
        var C = time();
        C.know($("(a &&+1 a)"));
        var ee = C.graph.edges().toList();
        //C.know($("a"), 0);
        assertSolved("(a ==>+- a)", C, "(a ==>+1 a)", "(a ==>-1 a)");
    }

    @Test
    void Impl_repeat_PN() throws Narsese.NarseseException {
        var C = time();
        C.know($("(a &&+1 --a)"));
        C.know($("a"), 0);
        assertSolved("(--a ==>+- a)", C, "((--,a) ==>-1 a)");
    }
    @Test
    void Conj3_repeat2a() throws Narsese.NarseseException {
        var C = abc();
        assertSolved("(&&+-, a,b,c)", C,
                "((a &&+2 b) &&+1 c)@0",
                "((a &&+3 b) &&+1 c)@-1"
        );
    }

    @Test
    void Conj3_repeat2() throws Narsese.NarseseException {
        var C = abc();
        assertSolved("(&&+-, (a &&+- a),b,c)", C,
    "((a &&+1 a) &&+2 (b &&+1 c))@-1"
            //"(((a &&+1 a) &&+1 b) &&+1 c)@0", "((a &&+2 b) &&+1 c)@0"
            //"((a &&+1 b) &&+1 c)@1", "((a &&+2 b) &&+1 c)@0", "((a &&+3 b) &&+1 c)@-1"
        );
    }

    private static LambdaTimeGraph abc() throws Narsese.NarseseException {
        var C = time();
        C.know($("(a ==>+1 a)"));
        C.know($("a"), 0);
        C.know($("b"), 2);
        C.know($("c"), 3);
        return C;
    }

    @Disabled
    @Test
    void repeatCollapse() throws Narsese.NarseseException {
        var C = time();
        C.know($("a"), 0);
        C.know($("b"), 2);
        assertSolved("(a &&+- a)", C, "a@0");
        assertSolved("((a &&+- a) ==>+- b)", C, "(a ==>+2 b)");
    }

    @Test
    void repeatCollapse2() throws Narsese.NarseseException {
        var g = time();
        g.know($("a"), 0);
        g.know($("a"), 2);
        assertSolved("(a ==>+- a)", g, "(a ==>+2 a)", "(a ==>-2 a)"); //1 solution
        assertSolvedIncludes("(a &&+- a)", g, "(a &&+2 a)@0"); //2+ solutions
    }

    @Test
    void Conj3_partial() throws Narsese.NarseseException {
        var C = time();
        C.know($("a"), 1);
        C.know($("b"), 2);
        //C.print();
        assertSolved("(&&+-, a,b,c)", C, "((a &&+1 b) &&+- c)@1");
    }

    @Test
    void ImplWithTwoConjPredicates() throws Narsese.NarseseException {
        var C = time();
        C.know($("(a &&+5 b)"), 1);
        C.know($("(c &&+5 d)"), 3);
        assertSolved("((a &&+5 b) ==>+- (c &&+5 d))", C,
                "((a &&+5 b) ==>-3 (c &&+5 d))");
    }

    @Test
    void ImplWithTwoConjPredicatesShared() throws Narsese.NarseseException {
        var C = time();
        C.know($("(a &&+5 b)"), 1);
        C.know($("(b &&+5 c)"), 3);
        assertSolved("((a &&+5 b) ==>+- (b &&+5 c))", C,
            "((a &&+5 b) ==>-3 (b &&+5 c))",
                "((a &&+5 b) ==>+3 (b &&+5 c))"
        );
    }

    @Test
    void ImplWithConjSubjDecomposeProperly() throws Narsese.NarseseException {


        var C = time();
        C.know($("b"), 6);
        C.know($("((a &&+1 a2)=|>b)"), 1);


        assertSolved("(a &&+1 a2)", C,
                "(a &&+1 a2)@5");

    }

    @Test
    void NoBrainerNegation_impl() {

        var C = time();
        C.know($$("x"), 1);
        C.know($$("y"), 2);
//        C.autoneg = true;

        assertSolved("(--x ==>+- y)", C, "((--,x) ==>+1 y)");
    }

    @Test
    void NoBrainerNegation_conj() {

        var C = time();
        C.know($$("x"), 1);
        C.know($$("y"), 2);
//        C.autoneg = true;

        assertSolved("(--x &&+- y)", C, "((--,x) &&+1 y)@1");
    }

    @Test
    void ComponentInTwoConjunctionSequences() {

        var C = time();
        C.know($$("(_1 &&+1 _2)"), ETERNAL);
        C.know($$("((_4 &&+1 _1) &&+1 (_2 &&+1 _3))"), ETERNAL);

        assertSolved("_4", C, "_4@ETE");

    }

    @Test
    void ConjSimpleOccurrences() throws Narsese.NarseseException {
        var C = time();
        C.know($("(x &&+5 y)"), 1);
        C.know($("(y &&+5 z)"), 6);
        C.know($("(w &&+5 x)"), -4);


        assertSolved("x", C, "x@1");
        assertSolved("y", C, "y@6");
        assertSolved("z", C, "z@11");
        assertSolved("w", C, "w@-4");

    }

    @Test
    void ConjTrickyOccurrences() throws Narsese.NarseseException {
        var g = time();
        g.know($("(x &&+5 y)"), 1);
        g.know($("(y &&+5 z)"), 3);
        assertSolved("y", g, "y@6", "y@3");
        assertSolved("x", g, "x@1"/*, "x@-2"*/);
        assertSolved("z", g, "z@8"/*, "z@11"*/);
    }

    @Test
    void implConjimplConj_temporal() throws Narsese.NarseseException {
        var C = time();
        C.know($("(x &&+5 y)"), 1);
        C.know($("((x &&+5 y)==>(b &&+5 c))"), 1);
        assertSolved("(b &&+5 c)", C, "(b &&+5 c)@6");
    }

    @Test
    void implConjimplConj_temporal_2() throws Narsese.NarseseException {
        var C = time();
        C.know($("x"), 1);
        C.know($("((x &&+5 y)==>(b &&+5 c))"), 1);
        assertSolved("(b &&+5 c)", C, "(b &&+5 c)@6");
    }

    @Test
    void implConjimplConj_temporal_3() throws Narsese.NarseseException {
        var C = time();
        C.know($("y"), 6);
        C.know($("((x &&+5 y)==>(b &&+5 c))"), 1);
        assertSolved("(b &&+5 c)", C, "(b &&+5 c)@6");
    }


    @Test
    void ImplCross_Subj_DternalConj() throws Narsese.NarseseException {
        for (String inner : new String[]{" ==>+1 ", " ==>-1 ", "==>"}) {
            var C = time();
            C.know($("((a&&x)" + inner + "b)"), 1);
            assertSolved("(a ==>+- b)", C, "(a" + inner + "b)");
        }
    }

    @Test
    void ImplCross_Pred_DternalConj() throws Narsese.NarseseException {

        for (String inner : new String[]{" ==>+1 ", " ==>-1 ", "==>"}) {
            var C = time();
            String i = "(b" + inner + "(a&&x))";
            Term ii = $(i);
            assertTrue(ii.CONDABLE(), i);
            C.know(ii, 1);
            //C.print();
            assertSolved("(b ==>+- a)", C, "(b" + inner + "a)");
        }
    }

    @Test
    void DepVarEvents() throws Narsese.NarseseException {
        var C = time();

        C.know($("#1"), 1);
        C.know($("x"), 3);

        assertSolved("(#1 ==>+- x)", C, "(#1 ==>+2 x)");

    }

    @Test
    void ImplConjComposeSubjNeg() throws Narsese.NarseseException {
        var C = time();

        int NA = 1; //Not/Applicable, should also work for ETE
        C.know($("((--,y) ==>+3 z)"), NA);
        C.know($("((--,x) ==>+5 z)"), NA);
        //C.print();

        assertSolved("((--x &&+- --y) ==>+- z)", C,
                "(((--,x) &&+2 (--,y)) ==>+3 z)");
    }

    @Test
    void ImplConjComposePred() throws Narsese.NarseseException {
        var C = time();

        int NA = 1; //Not/Applicable
        C.know($("(x ==>+1 y)"), NA);
        C.know($("(x ==>+2 z)"), NA);
        assertSolved("(x ==>+- (y &&+- z))", C, "(x ==>+1 (y &&+1 z))");

    }

    @Test
    void ImplConjComposePredSeq() throws Narsese.NarseseException {
        var C = time();
        C.know($("(x ==> (a &&+1 b))"));
        C.know($("(x ==> (c &&+1 d))"));
        assertSolved("((a &&+1 b) &&+- (c &&+1 d))", C, "((a&&c) &&+1 (b&&d))");
        assertSolved("(a &&+- c)", C, "(a&&c)");
        assertSolved("(c &&+- b)", C, "(c &&+1 b)");
        assertSolved("(x ==>+- (  (a &&+1 b) &&+-   (c &&+1 d)))", C, "(x==>((a&&c) &&+1 (b&&d)))");
    }

    @Test
    void ImplConjComposePredSeq2() throws Narsese.NarseseException {
        var C = time();
        C.know($("((x &&+1 y) ==> (a &&+1 b))"));
        C.know($("((x &&+1 y) ==> (c &&+1 d))"));
        assertSolved("((a &&+1 b) &&+- (c &&+1 d))", C, "((a&&c) &&+1 (b&&d))");
        assertSolved("((x &&+1 y) ==>+- (  (a &&+1 b) &&+-   (c &&+1 d)))", C, "((x &&+1 y)==>((a&&c) &&+1 (b&&d)))");
    }

    @Test
    void ImplConjComposePredDisj() throws Narsese.NarseseException {
        var C = time();
        C.know($("(x ==> (a &&+1 b))"));
        C.know($("(x ==> (c &&+1 d))"));
        assertSolved("((a &&+- b) &&+- (c &&+- d))", C, "((a&&c) &&+1 (b&&d))");
    }

    @Test
    void ImplConjComposePredDisj0() throws Narsese.NarseseException {
        var C = time();
        C.know($("(x ==> (a &&+1 b))"));
        C.know($("(x ==> (c &&+1 d))"));
        assertSolved("((a &&+1 b) &&+- (c &&+1 d))", C, "((a&&c) &&+1 (b&&d))");
    }

    @Test
    void ImplConjComposePredDisjImpl() throws Narsese.NarseseException {
        var C = time();
        C.know($("(x ==> (a &&+1 b))"));
        C.know($("(x ==> (c &&+1 d))"));
        assertSolved("(x ==>+- ((a &&+1 b) &&+- (c &&+1 d)))", C, "(x==>((a&&c) &&+1 (b&&d)))");
    }

    @Test
    void ImplConjComposePredDisjXternalSequence() throws Narsese.NarseseException {
        var C = time();
        C.know($("(x ==> (a &&+1 b))"));
        C.know($("(x ==> (c &&+1 d))"));
        assertSolved("(&&+-, a, c, b, d)", C, "((a&&c) &&+1 (b&&d))");
    }

    @Test
    void ImplCrossParallelInternalConj() throws Narsese.NarseseException {
        var C = time();
        C.know($("((a&&x) ==>+1 b)"), 1);
        assertSolved("(a ==>+- b)", C, "(a ==>+1 b)");
    }

    @AfterEach
    void test() {
        for (Runnable runnable : afterEach) {
            runnable.run();
        }
    }

    private ExpectSolutions assertSolved(String inputTerm, TimeGraph t, boolean equalsOrContains, String... solutions) {
        return new ExpectSolutions((LambdaTimeGraph) t, equalsOrContains, solutions).solve(inputTerm);
    }

    private ExpectSolutions assertSolved(String inputTerm, TimeGraph t, String... solutions) {
        return assertSolved(inputTerm, t, true, solutions);
    }

    private ExpectSolutions assertSolvedIncludes(String inputTerm, LambdaTimeGraph t, String... solutions) {
        return assertSolved(inputTerm, t, false, solutions);
    }

    @Test
    void ConjPartiallyEternal() {
        var C = time();
        C.know($$("x"), ETERNAL);
        C.know($$("y"), 0);
        assertSolved("(x &&+- y)", C, "(x&&y)@0");
    }

    @Test
    void ImplicationPartiallyEternal() {
        var C = time();
        C.know($$("x"), ETERNAL);
        C.know($$("y"), 0);
        assertSolved("(x ==>+- y)", C, "(x==>y)");
    }

    @Test
    void ImplicationPartiallyEternalReverse() {
        var C = time();
        C.know($$("x"), ETERNAL);
        C.know($$("y"), 0);
        assertSolved("(y ==>+- x)", C, "(y==>x)");
    }

    @Test
    void ImplicationPartiallyEternal_Conj() {
        var C = time();
        C.know($$("x"), 0);
        C.know($$("(y&&z)"), 1);
        assertSolved("(x ==>+- z)", C, "(x ==>+1 z)");
    }

    @Test
    void ImplicationPartiallyEternal_Conj_b() {
        var C = time();
        C.know($$("(y&&z)"), 1);
        assertSolved("(y &&+- z)", C, "(y&&z)@1");
    }

    @Test
    void ImplicationPartiallyEternal_ConjSeq_Inner_pre() {
        Term s = $$("((x &&+1 y) &&+1 z)");
        var C = time();
        C.know(s, 1);
        assertSolved("(y &&+- z)", C, "(y &&+1 z)@2");
        assertSolved("(x &&+- z)", C, "(x &&+2 z)@1");
    }

    @Test
    void seq_pos_from_inner_neg() {
        Term s = $$("((x &&+1 --y) &&+1 z)");
        var C = time();
        C.know(s, 1);
        assertSolved("y", C, "y@2");
    }

    @Test
    void seq_pos_seq_from_inner_neg() {
        Term s = $$("((x &&+1 --(a &&+1 b)) &&+1 z)");
        var C = time();
        C.know(s, 1);
        assertSolved("(a &&+1 b)", C, "(a &&+1 b)@2");
    }

    @Test
    void seq_neg_seq_from_inner_pos() {
        Term s = $$("((x &&+1 (a &&+1 b)) &&+1 z)");
        var C = time();
        C.know(s, 1);
        assertSolved("(--,(a &&+1 b))", C, "(--,(a &&+1 b))@2");
    }

    @Test
    void seq_neg_from_inner_pos() {
        Term s = $$("((x &&+1 y) &&+1 z)");
        var C = time();
        C.know(s, 1);
        assertSolved("--y", C, "(--,y)@2");
    }


    @Test
    void ImplicationPartiallyEternal_ConjSeq_Inner() {


        var C = time();
        C.know($$("((x &&+1 y) &&+1 z)"), 1);
        assertSolved("((x &&+- y) &&+1 z)", C,
                "((x &&+1 y) &&+1 z)@1");
    }

    @Test
    void ImplicationPartiallyEternal_ConjSeq_Outer() {
        var C = time();
        C.know($$("((x &&+1 y) &&+1 z)"), 1);
        assertSolved("((x &&+1 y) &&+- z)", C, "((x &&+1 y) &&+1 z)@1");
    }

    @Disabled
    @Test
    void TemporalInRelation1() {
        var C = time();

        C.know($$("a"), 1); //C.autoNeg.add($$("a"));
        C.know($$("b"), 2); //C.autoNeg.add($$("b"));

        assertSolved("(x-->(a &&+- b))", C, "(x-->(a &&+1 b))");
        //assertSolved("(x--> --(a &&+- b))", C, "(x-->(--,(a &&+1 b)))");
        assertSolved("(x-->(a &&+- --b))", C, "(x-->(a &&+1 (--,b)))");
        //assertSolved("(x--> --(a &&+- --b))", C, "(x-->(--,(a &&+1 (--,b))))");
        assertSolved("(x, (a ==>+- b))", C, "(x,(a ==>+1 b))");
    }

    @Test
    @Disabled
    void TemporalInRelation2() {
        var C = time();
        C.know($$("(a &&+1 b)")/*, ETERNAL*/);

        assertSolved("(x-->(a &&+- b))", C, "(x-->(a &&+1 b))");
        assertSolved("(x, (a ==>+- b))", C, "(x,(a ==>+1 b))");
    }

    @Disabled
    @Test
    void TemporalInRelation2_neg() {
        var C = time();
        C.know($$("(a &&+1 b)")/*, ETERNAL*/);

        assertSolved("(x-->(a &&+- --b))", C, "(x-->(a &&+1 (--,b)))");
        //assertSolved("(x--> --(a &&+- b))", C, "(x-->(--,(a &&+1 b)))");
        //assertSolved("(x--> --(a &&+- --b))", C, "(x-->(--,(a &&+1 (--,b))))");

    }

    private class ExpectSolutions extends ConcurrentSkipListSet<String> implements Predicate<TimeGraph.Event> {

        final Supplier<String> errorMsg;
        final String[] solutions;
        private final LambdaTimeGraph time;
        volatile int uniqueSolutions;
        volatile int repeatSolutions;

        ExpectSolutions(LambdaTimeGraph time, boolean equalsOrContains, String... solutions) {
            this.time = time;
            this.time.solution = this;
            this.solutions = solutions;
            errorMsg = () ->
                    "expect: " + Arrays.toString(solutions) + "\n   got: " + this;

            TreeSet<String> solutionSet = Sets.newTreeSet(List.of(solutions));

            afterEach.add(() -> {
                if (equalsOrContains)
                    assertEquals(solutionSet, this, errorMsg);
                else
                    assertTrue(containsAll(solutionSet), errorMsg);
            });
        }

        @Override
        public boolean test(TimeGraph.Event y) {
            add(y.toString());
            return true;
        }

        final ExpectSolutions solve(String x) {
            time.solve($$(x), true);
            return this;
        }


//        protected void validate() {
//
//            Term[] events = time.byTerm.keySet().toArray(Op.EmptyTermArray);
//
//            IntHashSet[][] dt = new IntHashSet[events.length][events.length];
//            for (int xx = 0, eventsLength = events.length; xx < eventsLength; xx++) {
//                Term x = events[xx];
//                for (int yy = 0, eventsLength1 = events.length; yy < eventsLength1; yy++) {
//                    if (xx == yy) continue;
//                    Term y = events[yy];
//
//                    IntHashSet d = dt[xx][yy] = new IntHashSet(2);
//                    Term between = CONJ.the(x, XTERNAL, y);
//                    time.solve(between, (each) -> {
//                        if (each.id.equalsRoot(between)) {
//                            int xydt = each.id.dt();
//                            if (xydt != DTERNAL && xydt != XTERNAL) {
//                                d.add(xydt);
//                            }
//                        }
//                        return true;
//                    });
//                }
//            }
//
//            System.out.println("\n");
//            System.out.println(Arrays.toString(events));
//            for (IntHashSet[] r : dt) {
//                System.out.println(Arrays.toString(r));
//            }
//
//
//            for (int xx = 0, eventsLength = events.length; xx < eventsLength; xx++) {
//                for (int yy = xx + 1, eventsLength1 = events.length; yy < eventsLength1; yy++) {
//                    assertEquals(dt[xx][yy], dt[yy][xx]);
//                }
//            }
//
//        }
//
//        void print() {
//            time.print();
//            System.out.println(uniqueSolutions + " unique solutions / " + repeatSolutions + " repeat solutions");
//        }
    }


}