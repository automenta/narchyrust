package nars.unify;

import jcog.random.RandomBits;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.random.XorShift128PlusRandom;
import nars.NAL;
import nars.Narsese;
import nars.Op;
import nars.Term;
import nars.term.Compound;
import nars.term.anon.Anon;
import nars.term.util.Image;
import nars.term.util.transform.Retemporalize;
import nars.term.var.Variable;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static nars.$.*;
import static nars.term.util.Image.imageNormalize;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.*;


class UnifyTest {

    private static final int INITIAL_TTL = 128;

    @Test void testAlreadyUnified() {
        UnifyAny u = new UnifyAny();
        Variable v = (Variable) $$("%1");
        Term a = $$("a");
        Term b = $$("b");
        Term c = $$("#1");
        assertTrue(u.put(v, a));
        assertTrue(u.put(v, a));
        assertFalse(u.put(v, b));
        assertTrue(u.put(v, c));
        assertTrue(u.put(v, a));
        System.out.println(u);
    }

    @Test void testCommutiveConstantUnifiability() {
        @Nullable AbstractUnifier h = Unify.how($$("((Anna&&Edward)-->Friends)"), $$("((Bob&&Anna)-->#1)"), Op.Variables, 1, true);
        assertNull(h);
    }
//
//    /** HACK process completely to resolve built-in functors,
//     * to override VariableNormalization's override */
//    @Deprecated private static class PremiseRuleNormalization extends VariableNormalization {
//        @Override
//        public boolean preFilter(Compound x) {
//            return true;
//        }
//
//    }

    static Unify test(/**/ Op type, String s1, String s2, boolean shouldUnify) {
        Unify first = null;
        for (int seed : new int[]{1, 2, 3, 4}) {
            try {
                Unify u = test(seed, type, s1, s2, shouldUnify, false, false);
                if (seed == 1)
                    first = u;
                test(seed, type, s1, s2, shouldUnify, true, true);
            } catch (Narsese.NarseseException e) {
                throw new RuntimeException(e);
            }
        }
        return first;
    }

    private static Unify test(int rngSeed, /**/ Op type, String s1, String s2, boolean shouldSub, boolean anon1, boolean anon2) throws Narsese.NarseseException {


        Anon a = new Anon();


        Term t2 = Narsese.term(s2, true);
        if (anon2) t2 = a.put(t2).normalize();

        Term t1;
        if (type == Op.VAR_PATTERN && s1.contains("%")) {
            t1 = Narsese.term(s1, false);
            t1 = Retemporalize.patternify(((anon1 ? a.put(t1) : t1).normalize()));
        } else {
            t1 = Narsese.term(s1, true);
            if (anon1) t1 = a.put(t1).normalize();
        }


        assertNotNull(t1);
        assertNotNull(t2);


        Set<Term> vars = ((Compound) t1).recurseSubtermsToSet(type);
        vars.addAll(((Compound) t2).recurseSubtermsToSet(type));
        int n1 = vars.size();


//        boolean[] termuted = {false};
        AtomicBoolean subbed = new AtomicBoolean(false);

        Unify sub = new Unify(type, new RandomBits(new XorShift128PlusRandom(rngSeed)), NAL.unify.UNIFICATION_STACK_CAPACITY) {

//            @Override
//            protected boolean matches() {
//                if (!termutes.isEmpty())
//                    termuted[0] = true;
//                return super.matches();
//            }

            @Override
            public boolean match() {

                if (shouldSub) {
                    int[] matched = {0};
                    this.xy.forEachVersioned((k, v) -> {
                        if (var(k.opID())) {
                            assertNotNull(v);
                            matched[0]++;
                        }
                        return true;
                    });

                    if (matched[0] == n1) {
                        subbed.set(true);

                    } /*else {
                            System.out.println("incomplete:\n\t" + xy);
                        }*/


                } else {

                    assertTrue(n1 > xy.size(), () -> "why matched?: " + xy);

                }

                return true;
            }
        };


        //System.out.println("unify: " + t1 + " , \t" + t2);
        sub.setTTL(INITIAL_TTL);
        boolean u = sub.unify(t1, t2);
//        if (!termuted[0])
//            assertEquals(shouldSub, u);

        assertEquals(shouldSub, subbed.get());

        return sub;


    }

    static Unify testUnify(Compound a, Compound b, boolean matches) {

        AtomicBoolean matched = new AtomicBoolean(false);

        Unify f = new Unify(Op.VAR_QUERY, new RandomBits(new XorShift128PlusRandom(1)), NAL.unify.UNIFICATION_STACK_CAPACITY, 128) {

            @Override
            public boolean match() {

                assertTrue(matches);

                matched.set(true);


                assertEquals("{?1=a}", xy.toString());


                assertEquals(
                        "(a-->b) (?1-->b) -?>",
                        a + " " + b + " -?>"  /*+ " remaining power"*/);

                return true;
            }
        };

        f.unify(b, a);

        assertEquals(matched.get(), matches);

        return f;
    }
    @Test void ResolveIndirect() {
        Unify u = unify();
        assertEquals(0, u.size());

        u.put($$v("%1"), $$$("#2"));
        assertEquals(1, u.size());

        u.put($$v("#2"), $$$("x"));
        assertEquals(2, u.size());

        assertEq("x", u.resolveTerm($$("%1")));
        assertEq("(--,x)", u.resolveTerm($$("--%1")));
    }

    @Test void AssignIndirect() {
        Unify u = unify();
        assertEquals(0, u.size());

        Variable depVar = $$v("#2");
        Variable patVar = $$v("%1");
        u.put(patVar, depVar);
        assertEquals(1, u.size());

        u.put(depVar, $$$("x"));
        assertEquals(2, u.size());

        assertEq("x", u.resolveTerm(patVar));
        assertEq("(--,x)", u.resolveTerm(patVar.neg()));
        assertEq("x", u.resolveTerm(depVar));
        assertEq("(--,x)", u.resolveTerm(depVar.neg()));
    }

    @Test void ResolvePosNeg() {
        Unify u = unify();
        u.put($$v("%1"), $$("x"));
        assertEq("(--,x)", u.resolveTerm($$("--%1")));
    }
    @Test void mobius_pre() {
        assertTrue(unify().unify($$$("(%S ==> %P)"), $$$("(x ==> y)")));
        assertTrue(unify().unify($$$("(--%S ==> %P)"), $$$("(--x ==> y)")));
    }

    @Test void mobius_not_on_constants() {
        //assertFalse(Unify.mobius);
        assertFalse(unify().unify($$$("(--%S ==> %P)"), $$$("(x ==> y)")));

        //assertEq("(--,x)", u.resolveTerm($$("--%1")));
    }
    private static UnifyAny unify() {
        return new UnifyAny(new XoRoShiRo128PlusRandom(1));
    }

//    @Test
//    void testCommonStructureAllVariables() {
//        Unify u = unify();
////        assertTrue(
////                Unify.possible($$("(#1,$2,?3)").subterms(), $$("(#3,$2,?1)").subterms(), u.varBits)
////        );
//    }

    @Test
    void testFindSubst1() throws Narsese.NarseseException {
        testUnify((Compound) $("<a-->b>"), (Compound) $("<?C-->b>"), true);
        testUnify((Compound) $("(--,(a))"), (Compound) $("<?C-->(b)>"), false);
    }

//    @Test void EllipsisContainingTermNotEqual() {
//        assertNotEquals( $$("{a, %X}"), $$("{a, %X..+}"));
//    }

    @Test
    void unificationP0() {
        test(Op.VAR_PATTERN,
                "<%A ==> %B>",
                "<<a --> A> ==> <b --> B>>",
                true
        );
    }

    @Test
    void unificationP1() {
        test(Op.VAR_DEP,
                "<(#1,#1) --> wu>",
                "<(a,b) --> wu>",
                false
        );
    }

    @Test
    void unificationP2() {
        test(Op.VAR_DEP,
                "<(#1,c) --> wu>",
                "<(a,b) --> wu>",
                false
        );
    }

    @Test
    void unificationP3() {
        test(Op.VAR_PATTERN,
                "<(%1,%1,#1) --> wu>",
                "<(lol,lol2,#1) --> wu>",
                false
        );
    }

    @Test
    void unificationQ3() {
        test(Op.VAR_QUERY,
                "<(?1,?2,a) --> wu>",
                "<(lol,lol2,a) --> wu>",
                true
        );
        test(Op.VAR_QUERY,
                "<(?1,?1,#1) --> wu>",
                "<(lol,lol2,#1) --> wu>",
                false
        );
    }

    @Test
    void unificationP5() {
        test(Op.VAR_DEP,
                "<#x --> lock>",
                "<{lock1} --> lock>",
                true
        );
    }

    @Test
    void pattern_trySubs_Dep_Var() {
        test(Op.VAR_PATTERN,
                "<%A ==> %B>",
                "<<#1 --> A> ==> <?1 --> B>>",
                true);
    }


    @Test
    void pattern_trySubs_Var_2_product_and_common_depvar_bidirectional() {
        test(Op.VAR_DEP,
                "(<(#1,x) --> on>,<(SELF,x) --> at>)",
                "(<(SELF,x) --> on>,<(#1,x) --> at>)",
                true);


    }

    @Test
    void pattern_trySubs_2_product() {
        test(Op.VAR_QUERY,
                "(on(?1,x),     at(SELF,x))",
                "(on({t002},x), at(SELF,x))",
                true);
    }

    @Test
    void pattern_trySubs_Dep_Var_2_product() {
        test(Op.VAR_DEP,
                "(<(#1,x) --> on>,<(SELF,x) --> at>)",
                "(<({t002},x) --> on>,<(SELF,x) --> at>)",
                true);
    }

    @Test
    void pattern_trySubs_Indep_Var_2_set() {
        test(Op.VAR_DEP,
                "{<(#1,x) --> on>,<(SELF,x) --> at>}",
                "{<({t002},x) --> on>,<(SELF,x) --> at>}",
                true);
    }

    @Test
    void pattern_trySubs_Indep_Var_2_set2() {
        test(Op.VAR_DEP,
                "{<(#1,x) --> on>,<(SELF,x) --> at>}",
                "{<(z,x) --> on>,<(SELF,x) --> at>}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setSimple() {
        test(Op.VAR_PATTERN,
                "{%1,y}",
                "{z,y}",
                true);

    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setSimpler() {
        test(Op.VAR_PATTERN,
                "{%1}",
                "{z}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>,y}",
                "{<(z,x) --> on>,y}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_1() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>,<x-->y>}",
                "{<(z,x) --> on>,<x-->y>}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_2() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>,(a,b)}",
                "{<(z,x) --> on>,(a,b)}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_3() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:(a)}",
                "{<(z,x) --> on>, c:(a)}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_4() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:a:b}",
                "{<(z,x) --> on>, c:a:b}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:(a,b)}",
                "{<(z,x) --> on>, c:(a,b)}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5_n() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:(a,b)}",
                "{<(z,x) --> on>, c:(b,a)}",
                false);
    }
    @Test
    void pattern_conj_xternal_simple() {
        test(Op.VAR_PATTERN,
                "(x &&+- y)",
                "(x && y)",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5_1() {
        test(Op.VAR_PATTERN,
                "{ on(%1,x), c:(a && b)}",
                "{ on(z,x), c:(a && b) }",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5_c() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>, c:{a,b}}",
                "{<(z,x) --> on>, c:{a,b}}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5_c1() {
        test(Op.VAR_PATTERN,
                "{<(z,%1) --> on>, c:{a,b}}",
                "{<(z,x) --> on>, c:{a,b}}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5_c2() {
        test(Op.VAR_PATTERN,
                "{<(%1, z) --> on>, w:{a,b,c}}",
                "{<(x, z) --> on>, w:{a,b,c}}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5_s() {

        test(Op.VAR_PATTERN,
                "{<{%1,x} --> on>, c:{a,b}}",
                "{<{z,x} --> on>, c:{a,b}}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex0_5_r() {
        test(Op.VAR_PATTERN,
                "{on:{%1,x}, c}",
                "{on:{z,x}, c}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex1() {
        test(Op.VAR_PATTERN,
                "{%1,<(SELF,x) --> at>}",
                "{z,<(SELF,x) --> at>}",
                true);
    }

    @Test
    void pattern_trySubs_Pattern_Var_2_setComplex2() {
        test(Op.VAR_PATTERN,
                "{<(%1,x) --> on>,<(SELF,x) --> at>}",
                "{<(z,x) --> on>,<(SELF,x) --> at>}",
                true);
    }

    @Test
    void pattern_trySubs_Dep_Var_2_set() {
        test(Op.VAR_DEP,
                "{<(#1,x) --> on>,<(SELF,x) --> at>}",
                "{<({t002},x) --> on>,<(SELF,x) --> at>}",
                true);
    }

    @Test
    void pattern_trySubs_Indep_Var_32() {
        test(Op.VAR_PATTERN,
                "<%A ==> <(SELF,$1) --> reachable>>",
                "<(&&,<($1,#2) --> on>,<(SELF,#2) --> at>) ==> <(SELF,$1) --> reachable>>",
                true);
    }

    @Test
    void pattern_trySubs_set3() {
        test(Op.VAR_PATTERN,
                "{%1,%2,%3}",
                "{a,b,c}",
                true);
    }

    @Test
    void pattern_trySubs_set2_1() {
        test(Op.VAR_PATTERN,
                "{%1,b}", "{a,b}",
                true);
    }

    @Test
    void pattern_trySubs_set2_2() {
        test(Op.VAR_PATTERN,
                "{a,%1}", "{a,b}",
                true);
    }

    @Test
    void pattern_trySubs_set3_1_b() {
        test(Op.VAR_PATTERN,
                "{a,b,c}",
                "{%1,b,%2}",
                true);
    }

    @Test
    void pattern_trySubs_set3_1_b_reverse() {
        test(Op.VAR_PATTERN,
                "{%1,b,%2}",
                "{a,b,c}",
                true);
    }

    @Test
    void pattern_trySubs_set3_1_b_commutative_inside_statement() {
        test(Op.VAR_PATTERN,
                "({a,b,c} --> d)",
                "({%1,b,%2} --> %3)",
                true);
    }

    @Test
    void pattern_trySubs_set3_1_statement_of_specific_commutatives() {
        test(Op.VAR_PATTERN,
                "<{a,b} --> {c,d}>",
                "<{%1,b} --> {c,%2}>",
                true);
    }

    @Test
    void pattern_trySubs_set3_1_statement_of_specific_commutatives_reverse() {
        test(Op.VAR_PATTERN,
                "<{%1,b} --> {c,%2}>",
                "<{a,b} --> {c,d}>",
                true);
    }

    @Test
    void pattern_trySubs_set3_1_c() {
        test(Op.VAR_PATTERN,
                "{a,b,c}",
                "{%1,%2,c}",
                true);
    }

    @Test
    void pattern_trySubs_set3_1_c_reverse() {
        test(Op.VAR_PATTERN,
                "{%1,%2,c}",
                "{a,b,c}",
                true);
    }

    @Test
    void pattern_trySubs_set4() {
        test(Op.VAR_PATTERN,
                "{%1,%2,%3,%4}",
                "{a,b,c,d}",
                true);
    }

    @Test
    void impossibleMatch1() {
        test(Op.VAR_DEP,
                "(a,#1)",
                "(b,b)",
                false);
    }

    @Test
    void notUnifyingType1() {
        test(Op.VAR_INDEP, "(x,$1)", "(x,y)", true);

        test(Op.VAR_DEP, "(x,$1)", "(x,$1)", true);
        test(Op.VAR_DEP, "(x,$1)", "(x,y)", false);
        test(Op.VAR_DEP, "(x,y)", "(x,$1)", false);
    }

    @Test
    void patternSimilarity1() {
        test(Op.VAR_PATTERN,
                "<%1 <-> %2>",
                "<a <-> b>",
                true);
    }

    @Test
    void patternNAL2Sample() {
        test(Op.VAR_PATTERN,
                "(<%1 --> %2>, <%2 --> %1>)",
                "(<bird --> {?1}>, <bird --> swimmer>)",
                false);
    }

    @Test
    void patternNAL2SampleSim() {
        test(Op.VAR_PATTERN,
                "(<%1 <-> %2>, <%2 <-> %1>)",
                "(<bird <-> {?1}>, <bird <-> swimmer>)",
                false);
    }

    @Test
    void patternLongProd_NO_1() {
        test(Op.VAR_PATTERN,
                "(a,b,c,d,e,f,g,h,j)",
                "(x,b,c,d,e,f,g,h,j)",
                false);
    }

    @Test
    void patternLongProd_NO_2() {
        test(Op.VAR_PATTERN,
                "(a,b,c,d,e,f,g,h,j)",
                "(a,b,c,d,e,f,g,h,x)",
                false);
    }

    @Test
    void patternMatchesQuery1() {
        test(Op.VAR_PATTERN,
                "((%1 <-> %2), (%3 <-> %2))",
                "((x <-> ?1), (y <-> ?1))",
                true);
    }

    @Test
    void patternMatchesQuery2() {
        test(Op.VAR_PATTERN,
                "(<%1 <-> %2>, <%3 <-> %2>)",
                "(<bird <-> {?1}>, <bird <-> {?1}>)",
                true);
    }

    @Test
    void varDep2() {
        test(Op.VAR_DEP,
                "t:(#x | {#y})",
                "t:(x | {y})",
                true);
    }

    @Test
    void implXternal_pattern_var() {
        test(Op.VAR_PATTERN,
                "((--,%1) ==>+- %2)",
                "((--,(_1&&_2))==>_3)",
                true);
    }

    @Test
    void implXternal() {
        test(Op.VAR_PATTERN,
                "(x ==>+- y)",
                "(x ==>+1 y)",
                true);
    }


    @Test void VariableMobiusOrdering() {
        Term a = $$("#1"), b = $$("(--,%1)");
        UnifyAny u = new UnifyAny();
        assertTrue( u.unify(a, b) );
        assertEquals("{%1=(--,#1)}", u.xy.toString()); //WRONG: "{#1=(--,%1)}",
    }

    @Test void VariableOrderingReverseA() {
        Term a = $$("(--,#1)"), b = $$("%1");
        UnifyAny u = new UnifyAny();
        assertTrue( u.unify(a, b) );
        assertEquals("{%1=(--,#1)}", u.xy.toString()); //WRONG: "{#1=(--,%1)}",
    }

    @Test void VariableOrderingReverseB() {
        UnifyAny u = new UnifyAny();
        Term a = $$("#1"), b = $$("(--,%1)");
        assertTrue(u.unify(b, a));
        assertEquals("{%1=(--,#1)}", u.xy.toString()); //WRONG: "{#1=(--,%1)}",
    }

    @Test void images1() {
        UnifyAny u = new UnifyAny();
        Term a = $$("(x-->(a,b))"), b = Image.imageInt(a, atomic("a")).replace(atomic("b"), varDep(1));
        assertTrue(u.uni(imageNormalize(a), imageNormalize(b)));
        assertEquals("{#1=b}", u.xy.toString()); //WRONG: "{#1=(--,%1)}",

    }
    //testUnifyNegativeMobiusStrip

    @Test void impossibleComplexity() {
        assertFalse(new UnifyAny().unifies($$("x(#1,#2)"), $$("x(a)"), 1));
    }
}


//
//    @Test
//    void ellipsisCommutive1a() {
//        test(Op.VAR_PATTERN,
//            "{%X..+}",
//            "{a}", true);
//    }
//
//    @Test
//    void ellipsisCommutive1b() {
//        test(Op.VAR_PATTERN,
//            "{a, %X..+}",
//            "{a}", false);
//    }
//
//    @Test
//    void ellipsisCommutive1c() {
//        test(Op.VAR_PATTERN,
//            "{a, %X..*}",
//            "{a}", true);
//    }
//
//    @Test
//    void ellipsisCommutive2a() {
//
//        test(Op.VAR_PATTERN,
//            "{a, %X..+}",
//            "{a, b}", true);
//    }
//
//    @Test
//    void ellipsisCommutive2b() {
//        test(Op.VAR_PATTERN,
//            "{%X..+, a}",
//            "{a, b, c, d}", true);
//    }
//
//    @Test
//    void ellipsisCommutive2c() {
//        test(Op.VAR_PATTERN,
//            "{a, %X..+, e}",
//            "{a, b, c, d}", false);
//    }
//
//    @Disabled
//    @Test
//    void ellipsisLinearOneOrMoreAll() {
//        test(Op.VAR_PATTERN,
//            "(%X..+)",
//            "(a)", true);
//    }
//
//    @Disabled
//    @Test
//    void ellipsisLinearOneOrMoreSuffix() {
//        test(Op.VAR_PATTERN,
//            "(a, %X..+)",
//            "(a, b, c, d)", true);
//    }
//
//    @Disabled
//    @Test
//    void ellipsisLinearOneOrMoreSuffixNoneButRequired() {
//        test(Op.VAR_PATTERN,
//            "(a, %X..+)",
//            "(a)", false);
//    }
//
//    @Disabled
//    @Test
//    void ellipsisLinearOneOrMorePrefix() {
//        test(Op.VAR_PATTERN,
//            "(%X..+, a)",
//            "(a, b, c, d)", false);
//    }
//
//    @Disabled
//    @Test
//    void ellipsisLinearOneOrMoreInfix() {
//        test(Op.VAR_PATTERN,
//            "(a, %X..+, a)",
//            "(a, b, c, d)", false);
//    }
//
//    @Disabled
//    @Test
//    void ellipsisLinearZeroOrMore() {
//        test(Op.VAR_PATTERN,
//            "(a, %X..*)",
//            "(a)", true);
//    }
//
//    @Disabled
//    @Test
//    void ellipsisLinearRepeat1() {
//        test(Op.VAR_PATTERN,
//            "((a, %X..+), %X..+)",
//            "((a, b, c, d), b, c, d)", true);
//    }
//
//    @Disabled @Test
//    void ellipsisLinearRepeat2() {
//        test(Op.VAR_PATTERN,
//            "((a, %X..+), (z, %X..+))",
//            "((a, b, c, d), (z, b, c, d))", true);
//    }
//
//    @Test
//    void ellipsisCommutiveRepeat2_a() {
//
//        test(Op.VAR_PATTERN,
//            "{{a, %X..+}, {z, %X..+}}",
//            "{{a, b, c, d}, {z, b, c, d}}", true);
//    }
//
//    @Test
//    void ellipsisCommutiveRepeat2_aa() {
//
//        test(Op.VAR_PATTERN,
//            "({a, %X..+}, {z, %X..+})",
//            "({a, b, c, d}, {z, b, c, d})", true);
//    }
//
//    @Test
//    void ellipsisCommutiveRepeat2_aa_mismatch() {
//        test(Op.VAR_PATTERN,
//            "({a, %X..+}, {z, b, %X..+})",
//            "({a, b, c, d}, {z, b, c, d})", false);
//    }
//
//    @Test
//    void ellipsisCommutiveRepeat2_set() {
//        test(Op.VAR_PATTERN,
//            "({a, %X..+, %B}, {z, %X..+, %A})",
//            "({a, b, c, d}, {z, b, c, d})", true);
//
//        test(Op.VAR_PATTERN,
//            "{{a, %X..+, %B}, {z, %X..+, %A}}",
//            "{{a, b, c, d}, {z, b, c, d}}", true);
//    }
//
//    @Test
//    void ellipsisCommutiveRepeat2_product() {
//        test(Op.VAR_PATTERN,
//            "({a, %X..+, %B}, {z, %X..+, %A})",
//            "({a, b, c, d}, {z, b, c, d})", true);
//    }
//
//    @Test
//    void ellipsisCommutiveRepeat2_c() {
//
//        test(Op.VAR_PATTERN,
//            "{{a, %X..+}, {b, %Y..+}}",
//            "{{a, b, c}, {d, b, c}}", true);
//    }
//
//    @Test
//    void ellipsisCommutiveRepeat2_cc() {
//
//        test(Op.VAR_PATTERN,
//            "{{a, %X..+}, {b, %Y..+}}",
//            "{{a, b, c, d}, {z, b, c, d}}", true);
//    }
//
//    @Disabled @Test
//    void ellipsisLinearInner() {
//
//
//        test(Op.VAR_PATTERN,
//            "(a, %X..+, d)",
//            "(a, b, c, d)", true);
//    }
//
//    @Test
//    void ellipsisSequence() {
//
//    }
//
//    /**
//     * this case is unrealistic as far as appearing in rules but it would be nice to get working
//     */
//    @Test
//    void ellipsisCommutiveRepeat() {
//        test(Op.VAR_PATTERN,
//            "{{a, %X..+}, %X..+}",
//            "{{a, b, c, d}, b, c, d}", true);
//    }
//