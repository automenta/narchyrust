package nars.unify;

import jcog.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.Op;
import nars.Term;
import nars.term.Compound;
import nars.term.util.conj.CondMatch;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.TreeSet;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.Op.VAR_PATTERN;
import static nars.term.util.Testing.assertEq;
import static nars.unify.UnifyTest.test;
import static org.junit.jupiter.api.Assertions.*;

class UnifyConjTest {

    static void assertUnifies(String x, String y, boolean unifies) {
        Random rng = new XoRoShiRo128PlusRandom(1);
        assertEquals(unifies, new UnifyAny(rng).uni($$(x), $$(y)));
    }

    @Test
    void conjXternal() {
        test(VAR_PATTERN,
                "(x &&+- y)",
                "(x &&+1 y)",
                true);
    }
//    @Disabled
//    @Test void ConjInConjConstantFail2() {
//        for (int a : new int[] { 5 }) {
//            for (int b : new int[]{1, 5}) {
//                Term x = $$("((_1 &&+5 ((--,_1)&&_2)) &&+5 (((--,_2)&&_3) &&+" + a + " (--,_3)))");
//                Term y = $$("(_1 &&+" + b + " ((--,_1)&&_2))");
//                UnifyAny u = new UnifyAny();
//                boolean r = x.unify(y, u);
//                assertTrue(r==(b==5), ()->x + " " + y);
//            }
//        }
//    }

    @Test void ConjInConjConstantFail() {
        test(VAR_PATTERN,
                "((_1&|_2) &&+5 ((--,_1)&|(--,_2)))",
                "(_1 &&+5 ((--,_1)&|_2))",
                false);
    }

    @Test
    void testConjInConj() {
        test(VAR_PATTERN,
                "((_2(_1,%1) &&+- _3(%1)) &&+1 _4(%1))",
                "((_2(_1,_1) && _3(_1)) &&+- _4(_1))",
                true);
    }

    @Test
    void testConj_3aryXternal() {
        test(Op.VAR_DEP,
                "(&&+-, x(#1), y(#2), z(#3))",
                "((x(1) &&+1 y(2)) &&+1 z(3))",
                true);
        test(Op.VAR_DEP,
                "(&&+-, x(#1), y(#2), z(#3))",
                "((y(1) &&+1 x(2)) &&+1 z(3))",
                true);

        test(Op.VAR_DEP,
                "(&&+-, x(#1), y(#2), z(#3))",
                "((y(1) &&+1 y(2)) &&+1 z(3))",
                false);
    }

    @Test
    void testConj_3aryXternal_Partial_b() {

        test(Op.VAR_DEP,
                "(&&+-, x(#1), y(#2), z(#3))",
                "((y(1) &&+- x(2)) &&+1 z(3))",
                false);
    }

    @Test
    void testConj_seq_depvar() {
        test(Op.VAR_DEP,
                "(#1 &&+3 #1)",
                "(#1 &&+1 #1)",
                false);
    }

    @Test
    void testConj_3aryXternal_Partial_a() {
        test(Op.VAR_DEP,
                "(&&+-, x(#1), y(#2), z(#3))",
                "((y(1) &&+1 x(2)) &&+- z(3))",
                false);
    }
    @Test
    void testConj_3aryXternal_Partial_c() {
        test(Op.VAR_DEP,
                "(&&+-, x(#1), y(#1), z(#1))",
                "((y(1) &&+- x(2)) &&+1 z(3))",
                false);
    }

    @Test
    void testConjSeqAgainstVarFwdRev() {

        test(Op.VAR_DEP,
                "(#1 &&+1 (x,y))",
                "((a) &&+1 (x,y))",
                true);

        test(Op.VAR_DEP,
                "((x,y) &&+1 #1)",
                "((a) &&+1 (x,y))",
                false);

        test(Op.VAR_DEP,
                "(#1 &&+- (x,y))",
                "((a) &&+1 (x,y))",
                true);

        test(Op.VAR_DEP,
                "((x,y) &&+1 #1)",
                "((x,y) &&+1 (a))",
                true);


        test(Op.VAR_DEP,
                "(#1 &&+1 (x,y))",
                "((x,y) &&+1 (a))",
                false);
        test(Op.VAR_DEP,
                "((x,y) &&+1 #1)",
                "((x,y) &&+1 (a))",
                true);
    }

    /*



    (B --> K), (&&,(#X --> L),(($Y --> K) ==> A)) |- substitute((&&, (#X --> L), A), $Y,B), (Truth:Deduction)
    (B --> K), (&&,(#X --> L),(($Y --> K) ==> (&&,A..+))) |- substitute((&&,(#X --> L),A..+),$Y,B), (Truth:Deduction)
     */

    @Test
    void implConjInConjFail() {
        test(VAR_PATTERN,
                "((_2(_1,%1)&|_3(%1)) &&+1 _4(%1))",
                "(_2(_1,_1) &&+- _4(_1))",
                false);
    }


    @Test
    void glob_Conj_1_to_2_dternal () {
        Unify u = test(VAR_PATTERN,
                "(&&,x,y,z)",
                "(x && %1)",
                true);
        assertEq("(y&&z)", u.resolveVar($.varPattern(1)));
    }

    @Test
    void glob_Conj_1_to_2_dternal_prefer_non_xternal_A () {
        Unify u = test(VAR_PATTERN,
                "(&&,x,y,z)",
                "(x &&+- %1)",
                true);
        assertEq("(y&&z)", u.resolveVar($.varPattern(1)));
    }

    @Test
    void glob_Conj_1_to_2_dternal_prefer_non_xternal_B () {
        Unify u = test(VAR_PATTERN,
                "(&&+- ,x,y,z)",
                "(x && %1)",
                true);
        assertEq("(y &&+- z)", u.resolveVar($.varPattern(1)));
    }

    @Test
    void glob_Conj_1_to_2_dternal_neg () {
        Unify u = test(VAR_PATTERN,
                "(&&,x,y,z)",
                "(x && --%1)",
                true);
        assertEq("(--,(y&&z))", u.resolveVar($.varPattern(1)));
    }

    @Test
    void glob_Conj_1_to_2_xternal () {
        Unify u = test(VAR_PATTERN,
                "(&&+- ,x,y,z)",
                "(x && %1)",
                true);
        assertEq("(y &&+- z)", u.resolveVar($.varPattern(1)));
    }

    @Test
    void glob_Conj_1_to_2_seq_xternal_other () {
        {
            Unify u = test(VAR_PATTERN,
                    "((x &&+1 y) &&+1 z)",
                    "(%1 &&+- z)",
                    true);
            assertEq("(x &&+1 y)", u.resolveVar($.varPattern(1)));
        }
    }

    @Test
    void glob_Conj_1_to_2_seq_xternal_other_reverse () {


        //reverse order:
        {
            Unify u = test(VAR_PATTERN,
                    "(%1 &&+- z)",
                    "((x &&+1 y) &&+1 z)",
                    true);
            assertEq("(x &&+1 y)", u.resolveVar($.varPattern(1)));
        }
    }

    @Test
    void glob_Conj_1_to_2_seq_beginning () {
        Unify u = test(VAR_PATTERN,
                "((x &&+1 y) &&+1 z)",
                "(%1 &&+1 z)",
                true);
        assertEq("(x &&+1 y)", u.resolveVar($.varPattern(1)));
    }

    @Test
    void glob_Conj_1_to_2_seq_end () {
        Unify u = test(VAR_PATTERN,
                "((x &&+1 y) &&+1 z)",
                "(x &&+1 %1)",
                true);
        assertEq("(y &&+1 z)", u.resolveVar($.varPattern(1)));
    }

    @Test
    void glob_Conj_1_to_2_seq_mid () {
        Unify u = test(VAR_PATTERN,
                "(((x &&+1 y) &&+1 z) &&+1 w)",
                "((x &&+1 %1) &&+1 w)",
                true);
        assertEquals("{%1=(y &&+1 z)}", u.xy.toString());
        assertEq("(y &&+1 z)", u.resolveVar($.varPattern(1)));
    }

    @Test
    void glob_Conj_1_to_2_seq_beginning_and_end () {
        Unify u = test(VAR_PATTERN,
                "((x &&+1 y) &&+1 z)",
                "((%1 &&+1 y) &&+1 %2)",
                true);
        assertEq("x", u.resolveVar($.varPattern(1)));
        assertEq("z", u.resolveVar($.varPattern(2)));
    }

    @Test
    void glob_Conj_1_to_3_dternal () {
        Unify u = test(VAR_PATTERN,
                "(&&,x,y,z,w)",
                "(&&,x,%1)",
                true);
        assertEq("(&&,w,y,z)", u.resolveVar($.varPattern(1)));
    }
    @Test
    void pattern_trySubs_Var_2_parallel() {
        test(Op.VAR_QUERY,
                "(((?1,x) --> on) && ((SELF,a) --> at))",
                "((({t002},x) --> on) && ((SELF,a) --> at))",
                true);
    }
    @Test
    void glob_Conj_extra_variable () {
        //was: _assigned_True_ ?
        test(VAR_PATTERN,
                "(&&,%1,%2,%3)",
                "(&&,x,y)",
                false);
    }
    @Test
    void glob_Conj_extra_variable_inh () {
        //was: _assigned_True_ ?
        test(VAR_PATTERN,
                "(a --> (&&,%1,%2,%3))",
                "(a --> (&&,x,y))",
                false);
    }

    @Disabled @Test
    void glob_Conj_extra_variable_assigned_True_inh_2 () {
        final String A = "(a --> (&&,x,%2,%3))";
        final String B = "(a --> (&&,x,y))";

        Term a = $$(A);
        Term b = $$(B);

        assertEquals(Unifier.Conj,
            CondMatch.unifyPossibleConjSubterms((Compound) a.sub(1), (Compound) b.sub(1), VAR_PATTERN.bit));
        assertTrue(Unify.isPossible(a, b, VAR_PATTERN.bit, 0));

        Unify u = test(VAR_PATTERN, A, B, true);
    }

    @Test
    void glob_Conj_2_to_3_dternal () {
        Unify u = test(VAR_PATTERN,
                "(&&,x,y,z,w)",
                "(&&,x,%1,%2)",
                true);

        TreeSet<Term> results = new TreeSet();
        for (int i = 0; i < 32; i++)
            results.add(u.resolveVar($.varPattern(1)));

        //6 combinations for partitioning set of 3 into 2
        assertEquals(6, results.size());
        assertEquals("TODO", results.toString());
    }

    @Test void earlyFailConj() {
        //the difference in structs should fail early before decomposing to events
        Compound a = $$c("((want(z,(tetris-->dex),1) &&+250 want(z,(tetris-->happy),1)) &&+370 (tetris-->happy))");
        Compound b = $$c("((want(z,(tetris-->dex),1)&&want(z,(tetris-->happy),1)) &&+- (--,(tetris-->clear)))");
        UnifyTest.testUnify(a, b, false);
    }

    @Disabled
    @Test
    void glob_Conj_True () {
        Unify u = test(VAR_PATTERN,
                "(&&,x,y)",
                "(&&,x,y,%2)",
                true);

        TreeSet<Term> results = new TreeSet();
        for (int i = 0; i < 32; i++) {
            results.add(u.resolveVar($.varPattern(1)));
        }
        assertEquals("{%2=True}", results.toString());
    }
    @Test
    void unifyPossibleConstant() {
        assertTrue(Unify.isPossible($$("(&&,x,y,z)"), $$("(&&,x,#1,y)"), Op.Variables, 1));
        assertFalse(Unify.isPossible($$("(&&,x,y,z)"), $$("(&&,x,y)"), Op.Variables, 1));
        assertTrue(Unify.isPossible($$("(&&,x,y,(a &&+- b))"), $$("(&&,x,y,(a &&+1 b))"), Op.Variables, 1));

        //possible now with glob:
        //assertFalse(Unify.isPossible($$("(&&,x,y,z)"), $$("(&&,x,#1)"), Op.Variable, 1));
    }

    @Test void unifyXternalSequence_repeats() {
        assertUnifies("(x &&+- x)", "(x &&+1 x)", true);
    }

    @Test @Disabled
    void unifyXternalSequence_repeats2() {

        assertUnifies("(x &&+- x)", "(x &&+1 (x &&+1 x))", true);
        assertUnifies("(&&+-, x, y, z)", "(x &&+1 (y &&+1 (x &&+1 z)))", true);
        assertUnifies("(&&+-, x, y, z)", "(x &&+1 (#y &&+1 (x &&+1 z)))", true);

    }

    @Test
    void unifySequence_Sequence_with_vars() {
        assertUnifies("(x &&+1 (%y &&+1 z))", "(x &&+1 (y &&+1 z))", true);
        assertUnifies("(%a,(x &&+1 (y &&+1 z)))", "((a,b,c),(x &&+1 (y &&+1 z)))", true); //constant, for sanity test
    }
    @Test
    void unifySequence_Sequence_with_vars2() {
        assertUnifies("(x &&+1 (%y &&+1 z))", "(x &&+1 ((y,w) &&+1 z))", true);
    }


    @Test
    void unifyXternalParallel() {
        assertUnifies("(&&+-, --x, y, z)", "(&&, x, y, z)", false);
        assertUnifies("(&&+-, x, y, z)", "(&&, x, y, z)", true);
        assertUnifies("(&&+-, --x, y, z)", "(&&, --x, y, z)", true);
        assertUnifies("(&&+-, x, y, z)", "(&&, --x, y, z)", false);
    }
    @Test
    void unifyXternalParallelWithVars() {
        assertUnifies("(&&+-, x, y, z)", "(&&, #x, y, z)", true);
        assertUnifies("(&&+-, x, y, z)", "(&&, #x, %y, z)", true);
    }

    @Test
    void unifyXternalSequence2() {
        assertUnifies("(x &&+- y)", "(x &&+1 y)", true);
        assertUnifies("(x &&+- y)", "(x &&+1 --y)", false);
        assertUnifies("(--x &&+- y)", "(--x &&+1 y)", true);
    }

    @Test
    void unifyXternalSequence2Repeating() {
        assertUnifies("(x &&+- x)", "(x &&+1 x)", true);
        assertUnifies("(x &&+- --x)", "(x &&+1 --x)", true);
        assertUnifies("(x &&+- --x)", "(--x &&+1 x)", true);
    }


    @Test
    void unifyXternalXternal_vs_Sequence() {

        assertUnifies("(&&+-, x, y, z)", "(x &&+1 (y &&+1 z))", true);
        assertUnifies("(&&+-, x, --y, z)", "(x &&+1 (y &&+1 z))", false);
        assertUnifies("(&&+-, x, y, z)", "(z &&+1 (x &&+1 y))", true);
        assertUnifies("(&&+-, x, --y, z)", "(x &&+1 (--y &&+1 z))", true);
        assertUnifies("(&&+-, x, y, z)", "(x &&+1 (--y &&+1 z))", false);
    }

    @Test void XternalConjUnifyWTF() {
        UnifyAny u = new UnifyAny();
        Term
                a = $$("(hasGUEState($1,GUE_MaximizedState) &&+- hasGUEState($1,GUE_UncoveredState))"),
                b = $$("(agent($1,#2) &&+- ({#2}-->ComputerUser))");
        assertFalse( u.unifies(a, b) );
    }
    @Test void XternalConjUnifyWTF2() {
        UnifyAny u = new UnifyAny();
        Term
                a = $$("(x-->y)"),
                b = $$("((x-->y) &&+- (x -->y))");
        assertFalse( u.unifies(a, b) );
    }
    @Test void XternalConjUnifyWTF3() {
        UnifyAny u = new UnifyAny();
        Term
                a = $$("(x-->y)"),
                b = $$("(($3-->$1) &&+- ($3 --> $2))");
        for (int i = 0; i < 10; i++) {
            assertFalse(u.unifies(a, b));
            assertFalse(u.unifies(b, a));
        }
    }

    @Test void ununifiableConj1() {
        assertFalse(new UnifyAny().unifies($$("((--,(grow&&forget))&&(--,clear))"), $$("(grow&&forget)"), 1));
    }


}