package nars.nal.nal6;

import jcog.Fuzzy;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.*;

public class NAL6Test extends AbstractNAL6Test {


    @Test
    void variable_unification_revision() {
        test
                .mustBelieve(cycles, "(($1 --> bird) ==> ($1 --> flyer))", 0.79f, 0.92f)
                .believe("(($x --> bird) ==> ($x --> flyer))")
                .believe("(($y --> bird) ==> ($y --> flyer))", 0.00f, 0.70f)
        ;
    }

    @Test
    void variable_unification2() {

        test
                .volMax(8)
                .believe("<($x --> bird) ==> <$x --> animal>>")
                .believe("<<$y --> robin> ==> <$y --> bird>>")
                .mustBelieve(cycles, "<<$1 --> robin> ==> <$1 --> animal>>", 1.00f, 0.81f)
                .mustBelieve(cycles, "<<$1 --> animal> ==> <$1 --> robin>>", 1.00f, 0.45f);

    }


    @Test
    void variable_unification4() {

        test.volMax(12);

        test.believe("<<bird --> $x> ==> <robin --> $x>>");
        test.believe("<<swimmer --> $y> ==> <robin --> $y>>", 0.70f, 0.90f);
        test.mustBelieve(cycles, "(((bird --> $1) && (swimmer --> $1)) ==> (robin --> $1))", 0.7f /*1f? */, 0.81f);

        test.mustBelieve(cycles, "<(bird --> $1) ==> (swimmer --> $1)>", 1f, 0.36f /*0.73F*/);
        test.mustBelieve(cycles, "<(swimmer --> $1) ==> (bird --> $1)>", 0.7f, 0.45f);


    }


    @Test
    void variable_unification6() {


        test.volMax(19)
                .believe("((&&,($x --> flyer),($x --> [chirping]), food($x, worms)) ==> ($x --> bird))")
                .believe("((&&,($y --> [chirping]),($y --> [withWings])) ==> ($y --> bird))")
                .mustBelieve(cycles, "((($1 --> flyer) && food($1,worms)) ==> ($1 --> [withWings]))", 1.00f,
                        0.45f)
                .mustBelieve(cycles, "(($1 --> [withWings]) ==> (($1 --> flyer) && food($1,worms)))", 1.00f,
                        0.45f);


        /*
        <patham9> 
        first result:
            (&&,($1 --> flyer),<($1,worms) --> food>) ==> ($1 --> [withWings])>
        it comes from the rule
            ((&&,C,A_1..n) ==> Z), ((&&,C,B_1..m) ==> Z) |- ((&&,A_1..n) ==> (&&,B_1..m)), (Truth:Induction)
        which basically says: if two different precondition conjunctions, with a common element lead to the same conclusion,
        it might be that these different preconditions in the specific conjunctions imply each other
        (each other because the premises can be swapped for this rule and it is still valid)

        second result:
            <($1 --> [withWings]) ==> (&&,($1 --> flyer),<($1,worms) --> food>)>
        by the same rule:
            ((&&,C,A_1..n) ==> Z), ((&&,C,B_1..m) ==> Z) |- ((&&,B_1..m) ==> (&&,A_1..n)), (Truth:Induction)
        where this time the diffierent preconditions of the second conjunction imply the different preconditions of the first
        no, no additionally info needed
        now I will show you what I think went wrong in your system:
        you got:
            ((&&,(($1,worms)-->food),($1-->flyer),($1-->[chirping]))==>(($1-->[withWings])&&($1-->[chirping]))).
        abduction in progress ^^
        your result is coming from the induction rule
            (P ==> M), (S ==> M), not_equal(S,P) |- (S ==> P), (Truth:Induction, Derive:AllowBackward)
        there are two possibilities, either restrict not_equal further to no_common_subter,
        or make the constructor of ==> make sure that the elements which occur in predicate and subject as well are removed
        its less fatal than in the inheritance composition, the derivation isnt wrong fundamentally, but if you can do so efficiently, let it avoid it
        additionally make sure that the two
            ((&&,C,A_1..n) ==> Z), ((&&,C,B_1..m) ==> Z) |- ((&&,A_1..n) ==> (&&,B_1..m)), (Truth:Induction)
        rules work, they are demanded by this reasoning about preconditions
        *hypothetical reasoning about preconditions to be exact
         */
    }


    @Test
    void variable_unification7() {

        test.volMax(14);
        test.confMin(0.43f);
        test.believe("((&&,($x --> flyer),(($x,worms) --> food)) ==> ($x --> bird))");
        test.believe("(($y --> flyer) ==> ($y --> [withWings]))");
        test.mustBelieve(cycles, "<(&&,($1 --> [withWings]),<($1,worms) --> food>) ==> ($1 --> bird)>", 1.00f, 0.45f);

    }

    @Test
    void variable_unification7_neg() {

        test.volMax(14).confMin(0.4f);
        test.believe("((&&,($x --> flyer),(($x,worms) --> food)) ==> ($x --> bird))");
        test.believe("(($y --> flyer) ==> ($y --> [withWings]))");
        test.mustBelieve(cycles, "<(&&,($1 --> [withWings]),<($1,worms) --> food>) ==> ($1 --> bird)>", 1.00f, 0.45f);

    }


    @Test
    void variable_introduction() {

        test.volMax(7).confMin(0.35f).confTolerance(0.5f)
                .believe("(swan --> bird)")
                .believe("(swan --> swimmer)", 0.80f, 0.9f)
                .mustBelieve(cycles, "(($1 --> swimmer) ==> ($1 --> bird))", 1.00f, 0.39f)
                .mustBelieve(cycles, "(($1 --> bird) ==> ($1 --> swimmer))", 0.80f, 0.45f)
                .mustBelieve(cycles, "((#1 --> bird) && (#1 --> swimmer))", 0.80f, 0.81f);

    }

    @Test
    void variable_introduction_with_existing_vars() {
        test.confTolerance(0.5f);
        test.believe("(swan --> (#1-->birdlike))");
        test.believe("(swan --> swimmer)", 0.80f, 0.9f);
        test.mustBelieve(cycles, "(($1 --> (#2 --> birdlike)) ==> ($1 --> swimmer))", 0.80f, 0.45f);
    }


    @Test
    void variable_introduction2() {
        /*
        originally: https:
        mustBelieve(cycles, "<(gull --> $1) ==> <swan --> $1>>", 0.80f, 0.45f); 
        mustBelieve(cycles, "<<swan --> $1> ==> (gull --> $1)>", 1.00f, 0.39f); 
        mustBelieve(cycles, "<(gull --> $1) <=> <swan --> $1>>", 0.80f, 0.45f); 
        mustBelieve(cycles, "(&&,<gull --> #1>,<swan --> #1>)", 0.80f, 0.81f); 
         */

        test.volMax(13);
        test.confTolerance(0.5f).confMin(0.2f);
        test.believe("(gull --> swimmer)");
        test.believe("(swan --> swimmer)", 0.80f, 0.9f);
        test.mustBelieve(cycles, "((gull --> swimmer) ==> (swan --> swimmer))", 0.80f, 0.45f);
        test.mustBelieve(cycles, "((swan --> swimmer) ==> (gull --> swimmer))", 1, 0.39f);
        test.mustBelieve(cycles, "((gull --> $1) ==> (swan --> $1))", 0.80f, 0.45f);
        test.mustBelieve(cycles, "((swan --> $1) ==> (gull --> $1))", 1.00f, 0.39f);
//        test.mustBelieve(cycles, "(&&,<gull --> #1>,<swan --> #1>)", 0.80f, 0.81f);
    }

    @Test
    @Disabled
    void variable_introduction3() {

        TestNAR tester = test;
        tester.believe("(gull --> swimmer)", 1f, 0.9f);
        tester.believe("(swan --> swimmer)", 0f, 0.9f);
        tester.mustBelieve(cycles, "(&&,<gull --> #1>,<swan --> #1>)", 0.0f, 0.81f);
        tester.mustBelieve(cycles, "(&&,<gull --> #1>,(--,<swan --> #1>))", 1.0f, 0.81f);


    }

    @Test
    void variable_introduction_with_existing_vars2() {


        test.volMax(16).confMin(0.35f).confTolerance(0.5f);
        test.believe("(#1 --> swimmer)");
        test.believe("(swan --> swimmer)", 0.80f, 0.9f);

        test.mustBelieve(cycles, "<<#1 --> $2> ==> <swan --> $2>>", 0.80f, 0.45f);
        test.mustBelieve(cycles, "((swan --> $1) ==> (#2 --> $1))", 1.00f, 0.39f);

//        test.mustBelieve(cycles, "((#1 --> #2) && (swan --> #2))", 0.80f, 0.81f);
    }

    @Test
    void variables_introduction() {

        test.volMax(12).confMin(0.42f).confTolerance(0.5f)
                .believe("open({key1},{lock1})")
                .believe("key:{key1}")
                .mustBelieve(cycles, "(key:{key1} ==> open({key1},{lock1}))", 1.00f, 0.45f)
                .mustBelieve(cycles, "(&&,open({#1},{lock1}),key:{#1})", 1.00f, 0.81f);
        //.mustBelieve(cycles, "(key:{$1} ==> open({$1},{lock1}))", 1.00f, 0.45f)
//                .mustBelieve(cycles, "(key:{$1} ==> open({$1},{lock1}))", 1.00f, 0.42f)


    }

    @Test
    void multiple_variables_introduction() {

        test
                .confTolerance(0.5f)
                .confMin(0.75f)
                .volMax(13)
                .believe("(open($x,lock1) ==> key:$x)")
                .believe("(open(lock,lock1) ==> lock:lock1)")
//            .mustBelieve(cycles, "(open(lock,lock1) ==> (key:lock && lock:lock1))",
//                    1.00f, 0.81f)
                .mustBelieve(cycles, "(open($1,$2) ==> (key:$1 && $1:$2))",
                        1.00f, 0.81f);


    }


    @Test
    void multiple_variables_introduction2() {

        test.volMax(18);
        test.confTolerance(0.5f).confMin(0.75f);
        test.believe("(key:#x && open(#x,{lock1}))");
        test.believe("lock:{lock1}");


        test.mustBelieve(cycles, "(&&,open(#1,{lock1}),({lock1}-->lock),(#1-->key))", 1.00f, 0.81f);
        //tester.mustBelieve(cycles, "(&&,open(#1,{#2}),({#2}-->lock),(#1-->key))", 1.00f, 0.81f);
        //tester.mustBelieve(cycles, "(&&, key:#1, lock:#2, open(#1,#2))", 1.00f, 0.81f);


        test.mustBelieve(cycles, "(lock:{$1} ==> (key:#2 && open(#2,{$1})))", 1.00f, 0.45f); //this is ok too
        //tester.mustBelieve(cycles, "(lock:$1 ==> (key:#2 && open(#2,$1)))", 1.00f, 0.45f);

    }

    @Test
    void second_level_variable_unificationNoImgAndAsPreconditionAllIndep() {


        test.confMin(0.6f);
        test.volMax(15);
        test.believe("((($1 --> lock)&&($2 --> key)) ==> open($1,$2))", 1.00f, 0.90f);
        test.believe("({key1} --> key)", 1.00f, 0.90f);
        test.mustBelieve(cycles * 2, "(($1-->lock)==>open($1,{key1}))", 1.00f,
                0.81f);
    }

    @Test
    void second_level_variable_unificationNoImgAndAsPrecondition() {
        test.volMax(15);
        test.confMin(0.6f);
        TestNAR tester = test;
        tester.believe("(((#1 --> lock)&&($2 --> key)) ==> open(#1,$2))", 1.00f, 0.90f);
        tester.believe("({key1} --> key)", 1.00f, 0.90f);
        tester.mustBelieve(cycles, "((({key1}-->key)&&(#1-->lock))==>open(#1,{key1}))", 1f, 0.81f);
        tester.mustBelieve(cycles, "((#1-->lock)==>open(#1,{key1}))", 1.00f, 0.81f);
    }

    @Disabled
    @Test
    void second_level_variable_unification() {
        /* there is a lock which is opened by all keys */
        test
                .believe("(((#1 --> lock) && ($2 --> key)) ==> open($2, #1))", 1.00f, 0.90f)
                .believe("({key1} --> key)", 1.00f, 0.90f)
                .mustBelieve(cycles, "((#1 --> lock) && open({key1}, #1))", 1.00f, 0.81f);
    }


    @Disabled
    @Test
    void second_level_variable_unification_neg() {
        /* there is a lock which is opened by all non-keys */
        test.believe("(((#1 --> lock) && --($2 --> key)) ==> open($2, #1))")
                .believe("--({nonKey1} --> key)")
                .mustBelieve(cycles, "((#1 --> lock) && open({nonKey1}, #1))", 1.00f, 0.81f);
    }


    @Test
    void second_level_variable_unification2() {
        test.volMax(15);
        test.believe("(($1 --> lock) ==> ((#2 --> key) && open(#2,$1)))", 1.00f, 0.90f);
        test.believe("({key1} --> key)", 1.00f, 0.90f);
        test.mustBelieve(cycles, "(($1 --> lock) ==> (({key1}-->key)&&open({key1},$1)))", 1.00f, 0.81f);
        test.mustBelieve(cycles, "(($1 --> lock) ==> open({key1},$1))", 1.00f, 0.66f);
        //0.73f
        //0.43f);

    }

    @Test
    void SimpleIndepUnification() {

        test.input("(y:$x ==> z:$x).");
        test.input("y:x.");
        test.mustBelieve(cycles, "z:x", 1.0f, 0.81f);
    }


    @Test
    void second_variable_introduction_induction() {

        test.volMax(17);
        test.believe("(open($1,lock1) ==> key:$1)");
        test.believe("open(lock,lock1)");
        test.mustBelieve(cycles,
                "((open(lock,#1) && open($2,#1)) ==> key:$2)",
                1.00f, 0.45f /*0.81f*/);

    }

    @Test
    void deductionBeliefWithVariable() {
        test
                .volMax(9)
                .believe("(x($1)==>y($1))", 1.00f, 0.90f)
                .believe("x(a)", 1.00f, 0.90f)
                .mustBelieve(cycles, "y(a)", 1.00f, 0.81f);
    }

    @Test
    void deductionBeliefWithVariableNeg() {
        test
                .believe("--(x($1)==>y($1))", 1.00f, 0.90f)
                .believe("x(a)", 1.00f, 0.90f)
                .mustBelieve(cycles, "--y(a)", 1.00f, 0.81f);
    }


//    @Test
//    void deductionBeliefWeakPositiveButNotNegative() {
//        test
//                .believe("(a==>b)", 0.55f, 0.90f)
//                .believe("a", 0.55f, 0.90f)
//                .mustBelieve(cycles, "b", 0.55f, 0.25f);
//    }
//    @Test
//    void deductionBeliefWeakNegativeButNotPositive() {
//        test
//                .believe("(a==>b)", 0.45f, 0.90f)
//                .believe("a", 0.45f, 0.90f)
//                .mustBelieve(cycles, "b", 0.49f, 0.81f);
//    }

    @Test
    void abductionBeliefWeakPositivesButNotNegative() {

        test.volMax(3)
                .believe("(a==>b)", 0.9f, 0.90f)
                .believe("b", 0.9f, 0.90f)
                .mustBelieve(cycles, "a",
                        1f, 0.45f
                );
    }

    @Test
    void abductionBeliefWeakNegativesButNotNegative() {

        test
                .volMax(3)
                .believe("(a==>b)", 0.1f, 0.90f)
                .believe("b", 0.1f, 0.90f)
                .mustBelieve(cycles, "a", 1, 0.37f);
    }


    @Test
    void abductionBeliefOffCenteredPositiveNegativeButNotTotalFail() {

        test.confMin(0.1f);
        test
                .believe("(a==>b)", 0.9f, 0.95f)
                .believe("b", 0.45f, 0.95f)
                .mustBelieve(cycles, "a",
                        0.46f, 0.29f
                );
    }


    /**
     * Conditional Abduction via Multi-conditional Syllogism
     */
    @Test
    void abduction_with_variable_elimination_in_conj() {

        test
                .volMax(13)
                .believe("(open($1,lock1) ==> ($1 --> key))", 1.00f, 0.90f)
                .believe("(((#1 --> lock) && open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f)
                .mustBelieve(cycles, "lock:lock1", 1.00f, 0.45f)
        ;
    }

    @Test
    void abduction_with_variable_elimination_in_disj() {

        test
                .volMax(16)
                .believe("(open($1,lock1) ==> ($1 --> key))", 1.00f, 0.90f)
                .believe("(((#1 --> lock) || open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f)
                .mustBelieve(cycles, "lock:lock1", 1.00f, 0.45f)
        ;
    }

    /**
     * Conditional Abduction via Multi-conditional Syllogism
     */
    @Test
    void abduction_with_variable_elimination_negated() {

        test
                .volMax(14)
                .believe("(open($1,lock1) ==> ($1 --> key))", 1.00f, 0.90f)
                .believe("((--(#1 --> lock) && open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f)
                .mustBelieve(cycles, "lock:lock1", 0.00f, 0.45f)
                .mustNotOutput(cycles, "lock:lock1", BELIEF, 0.5f, 1f, 0, 1f, ETERNAL)
        ;
    }

    /**
     * Conditional Abduction via Multi-conditional Syllogism
     */
    @Test
    void abduction_positive_negative_mix_depolarized() {

        test
                .volMax(7).confMin(0.3f)
                .believe("(P ==> N)", 0.00f, 0.90f)
                .believe("((A && --N) ==> P)", 1.00f, 0.90f)
                .mustBelieve(cycles, "A", 1.00f, 0.45f)
        ;
    }

    @Test
    void strong_unification_simple() {

        test
                .believe("(pair($a,$b) ==> ($a --> $b))", 1.00f, 0.90f)
                .believe("pair(x,y)", 1.00f, 0.90f)
                .mustBelieve(cycles * 4, "(x --> y)", 1.00f, 0.81f);
    }

    @Test
    void strong_unification_indep_0() {
        test.volMax(7)
                .believe("(a --> y)")
                .believe("(($x --> y) ==> ($x --> z))")
                .mustBelieve(cycles, "(a-->z)", 1f, 0.81f);
    }

    @Test
    void strong_unification_query_indep_0() {
        test.volMax(7)
                .question("(?1 --> z)")
                .believe("(($x --> y) ==> ($x --> z))")
                .mustQuestion(cycles, "(?1-->y)");
    }

    @Test
    void strong_unification_dep_indep_pre() {
        test.volMax(7)
                .believe("(#x --> y)")
                .believe("(($x --> y) ==> ($x --> z))")
                .mustBelieve(cycles, "(#x-->z)", 1f, 0.81f);
    }

    @Test
    void strong_unification_dep_indep_post() {
        test.volMax(7)
                .believe("(#x --> z)")
                .believe("(($x --> y) ==> ($x --> z))")
                .mustBelieve(cycles, "(#x-->y)", 1f, 0.45f);
    }

    @Test
    void strong_unification_simple2() {

        test.volMax(9)
                .believe("<<($a,$b) --> pair> ==> {$a,$b}>", 1.00f, 0.90f)
                .believe("<(x,y) --> pair>", 1.00f, 0.90f)
                .mustBelieve(cycles, "{x,y}", 1.00f, 0.81f);
    }


    @Test
    void strong_unification_pos() {

        test.believe("(sentence($a,is,$b) ==> ($a --> $b))", 1.00f, 0.90f);
        test.believe("sentence(bmw,is,car)", 1.00f, 0.90f);
        test.mustBelieve(cycles, "car:bmw", 1.00f, 0.81f);

    }

    @Test
    void strong_unification_neg() {
        test.volMax(12);
        test.believe("( --sentence($a,is,$b) ==> ($a --> $b) )", 1.00f, 0.90f);
        test.believe("sentence(bmw,is,car)", 0.00f, 0.90f);
        test.mustBelieve(cycles, "car:bmw", 1.00f, 0.81f);
        test.mustNotOutput(cycles, "car:bmw", BELIEF, 0, 0.5f, 0, 1f);
    }

    @Test
    void strong_unification_neg_neg() {
        test.volMax(12);
        test.believe("( --sentence($a,is,$b) ==> ($a --> $b) )", 0.00f, 0.90f);
        test.believe("sentence(bmw,is,car)", 0.00f, 0.90f);
        test.mustBelieve(cycles, "car:bmw", 0.00f, 0.81f);
        test.mustNotOutput(cycles, "car:bmw", BELIEF, 0.5f, 1f, 0, 1f);
    }

    @Test
    void impliesUnbelievedYet() {

        test.volMax(7);
        test.believe("(x:a ==> c:d).");
        test.believe("x:a.");
        test.mustBelieve(cycles, "c:d", 1.00f, 0.81f);
    }

    @Test
    void implVariableSubst() {

        test.volMax(7);
        test.believe("x:y.");
        test.believe("(x:$y==>$y:x).");
        test.mustBelieve(cycles, "y:x", 1.00f, 0.81f);
    }


    @Test
    void ComposePredSeq() {
        test
            .believe("( x ==> (y &&+1 z) )")
            .believe("( x ==> (a &&+2 b) )")
            .mustBelieve(cycles, "( x ==> (((a&&y) &&+1 z) &&+1 b) )", 1f, 0.81f);
    }

    @Test
    void DecomposeImplPredConjQuestion() {
        test
                .question("( x ==> (y && z) )")
                .mustOutput(cycles, "( x ==> y )", QUESTION)
                .mustOutput(cycles, "( x ==> z )", QUESTION)
        ;
    }

    @Disabled @Test
    void DecomposeImplPredDisjQuestion() {
        test
                .question("( x ==> (y || z) )")
                .mustOutput(cycles, "( x ==> y )", QUESTION)
                .mustOutput(cycles, "( x ==> z )", QUESTION)
        ;
    }


    @Disabled @Test
    void HypothesizeSubconditionIdentityPre() {
        test
                .volMax(14)
                .believe("((f(x) && f($1)) ==> g($1))", 1f, 0.9f)
                .mustBelieve(cycles, "(f($1) ==> g($1))", 1f, 0.81f)
                .mustBelieve(cycles, "(f(x) ==> g(x))", 1f, 0.81f)
        ;
    }

    @Disabled @Test
    void HypothesizeSubconditionIdentityPost() {
        test
                .volMax(14)
                .believe("(g($1) ==> (f(x) && f($1)))", 1f, 0.9f)
                .mustBelieve(cycles, "(g($1) ==> f($1))", 1f, 0.81f)
                .mustBelieve(cycles, "(g(x) ==> f(x))", 1f, 0.81f)
        ;
    }

    @Test
    void HypothesizeSubconditionIdentityConj() {
        test.volMax(17);
        test
                .believe("(&&,f(x),f(#1),g(#1))", 1f, 0.9f)
                .mustBelieve(cycles, "(&&,f(x),g(x))", 1f, 0.81f)
        ;
    }

    @Test
    void HypothesizeSubconditionNeg_Conj_ShortCircuit() {
        test.volMax(13);
        test
                .believe("(--(f(x) && --f(#1)) ==> a)", 1f, 0.9f)
                .mustBelieve(cycles, "a", 1f, 0.43f)
        ;
    }

    @Disabled @Test
    void ImplIntersectionPos() {
        test
                .believe("(A ==> M)", 0.6f, 0.9f)
                .believe("(B ==> M)", 0.7f, 0.9f)
                .mustBelieve(cycles, "((A && B) ==> M)", Fuzzy.or(0.6f, 0.7f), 0.81f)
        ;
    }

    @Test
    void ImplUnionPos() {
        test
            .believe("(A ==> M)", 0.6f, 0.9f)
            .believe("(B ==> M)", 0.7f, 0.9f)
            .mustBelieve(cycles, "((A || B) ==> M)", .42f, 0.34f)
        ;
    }

    @Disabled @Test
    void ImplUnionNeg() {
        test
                .believe("(A ==> M)", 0.6f, 0.9f)
                .believe("(B ==> M)", 0.3f, 0.9f)
                .mustBelieve(cycles, "((A && B) ==> M)", .72f, 0.81f)
        ;
    }


    @Test
    void ImplSubjQuestion() {
        test
                .believe("(x ==> y)")
                .question("x")
                .mustQuestion(cycles, "y")
        ;
    }

    @Test
    void ImplSubjDisjDecompose() {
        test
                .believe("((a||b) ==> y)", 0.75f, 0.9f)
                .mustBelieve(cycles, "(a ==> y)", 0.75f, 0.61f)
        ;
    }

    @Test
    void ImplSubjDisjDeduction() {
        test
                .volMax(8)
                .believe("((a||b) ==> y)")
                .believe("a")
                //.mustNotOutput(cycles, "(a==>y)", BELIEF,1,1, 0.45f,0.45f,ETERNAL)
                .mustNotOutput(cycles, "(a ==>+- (a==>y))", QUESTION)
                .mustNotOutput(cycles, "(((--,a) &&+- b) ==>+- y)", QUESTION)
                .mustNotOutput(cycles, "(a ==>+- (a==>y))", QUESTION)
                .mustBelieve(cycles, "y", 1, 0.81f)
        ;
    }

    @Test
    void ImplPredConjDeduction() {
        test
                .volMax(8)
                .believe("(y ==> (a&&b))")
                .believe("a")
                .mustBelieve(cycles, "y", 1, 0.45f)
        ;
    }

    @Test
    void ImplSubjNegQuestion() {
        test
                .confMin(0.9f)
                .believe("(--x ==> y)")
                .question("x")
                .mustQuestion(cycles, "y")
        ;
    }

    @Test
    void ImplPredQuestion() {
        test
                .confMin(0.9f)
                .believe("((x&&y)==>z)")
                .question("z")
                .mustQuestion(cycles, "(x&&y)")
        ;
    }
    @Test
    void ImplPredQuest() {
        test
                .volMax(7)
                .confMin(0.9f)
                .believe("((x&&y)==>z)")
                .quest("z")
                .mustQuest(cycles, "(x &&+- y)")
                //.mustQuest(cycles, "(x&&y)")
        ;
    }
    @Test
    void ImplPredQuestionUnify() {
        test
                .volMax(8)
                .confMin(0.9f)
                .believe("((x && $1)==>z($1))")
                .question("z(y)")
                .mustQuestion(cycles, "(x && y)")
        ;
    }



    @Disabled
    @Test
    void ImplConjPredQuestion() {
        test
                .volMax(7)
                .confMin(0.9f)
                .believe("((x&&y)==>z)")
                .question("z")
                .mustQuestion(cycles, "x")
                .mustQuestion(cycles, "y")
        ;
    }

    @Test
    void ImplSubjQuestionUnificationConst() {
        test
                .volMax(13)
                .confMin(0.9f)
                .believe("(Criminal($1) ==> (&&,Sells($1,#2,#3),z))")
                .question("Criminal(x)")
                .mustQuestion(cycles, "(&&,Sells(x,#2,#3),z)")
        ;
    }

    @Test
    void ImplSubjQuestionUnificationQuery() {
        test
                .volMax(13)
                .confMin(0.9f)
                .believe("(Criminal($1) ==> (&&,Sells($1,#2,#3),z))")
                .question("Criminal(?x)")
                .mustQuestion(cycles, "(&&,Sells(?1,#2,#3),z)")
        ;
    }

    @Test
    void ImplSubjNegQuestionUnificationConst() {
        test
                .volMax(14)
                .confMin(0.9f)
                .believe("(--Criminal($1) ==> (&&,Sells($1,#2,#3),z))")
                .question("Criminal(x)")
                .mustQuestion(cycles, "(&&,Sells(x,#2,#3),z)")
        ;
    }

    @Test
    void ImplPredQuestionUnification() {
        test
                .confMin(0.9f)
                .believe("((Sells($1,#2,#3) && z) ==> Criminal($1))")
                .question("Criminal(?x)")
                .mustQuestion(cycles, "(Sells(?1,#2,#3) && z)")
        ;
    }


    @Disabled @Test
    void ConjBelief_2DepVars_Decompose() {
        test
                .believe("(x(#1) && y(#2))")
                .mustBelieve(cycles, "x(#1)", 1f, 0.81f)
                .mustBelieve(cycles, "y(#1)", 1f, 0.81f)
        ;
    }

    @Test
    void ConjBelief_DepVar_Decompose2() {
        test.volMax(14).believe("(((#1,_1)-->(_1,#2))&&cmp(#1,#2,_2))").mustBelieve(cycles, "cmp(#1,#2,_2)", 1f, 0.81f);
    }

    @Test
    void ConjQuestion_2DepVars_Decompose() {
        test
                .question("(x(#1) && y(#2))")
                .mustQuestion(cycles, "x(#1)")
                .mustQuestion(cycles, "y(#1)")
        ;
    }
    @Test
    void negConjDecompose() {
        test
            .believe("--(x&&y)")
            .believe("x")
            .mustBelieve(cycles, "--y", 1f, 0.81f)
        ;
    }

    @Test
    void implPredDisj() {
        float a = 0.75f, b = 0.6f;
        test
                .believe("(x ==> a)", a, 0.9f)
                .believe("(x ==> b)", b, 0.9f)
                .mustBelieve(cycles, "(x ==> (a && b))", Fuzzy.and(a, b), 0.81f)
                .mustBelieve(cycles, "(x ==> (a || b))", Fuzzy.or(a, b), 0.81f)
        ;
    }

//    @Test
//    void ImplToDisj_Subj() {
//        //reverse of: https://github.com/opencog/opencog/blob/master/opencog/pln/rules/wip/or-transformation.scm
//        test
//                .believe("(--x ==> y)")
//                .mustBelieve(cycles, "(||,x,y)", 1f, 0.81f)
//        ;
//    }
//    @Test
//    void ImplToDisj_Pred() {
//        //reverse of: https://github.com/opencog/opencog/blob/master/opencog/pln/rules/wip/or-transformation.scm
//        test
//                .believe("(x ==> --y)")
//                .mustBelieve(cycles, "(||,x,y)", 1f, 0.81f)
//        ;
//    }

}