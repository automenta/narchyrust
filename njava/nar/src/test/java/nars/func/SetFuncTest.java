package nars.func;

import jcog.data.list.Lst;
import nars.NAR;
import nars.NARS;
import nars.Term;
import nars.eval.Evaluation;
import nars.eval.Evaluator;
import nars.term.Functor;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.func.FunctorTest.eval;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SetFuncTest {

    private static final NAR n = NARS.shell();

    @Test
    void testSortDirect() {

        assertEquals(Set.of($$("(a,b,c)")),
                eval($$c("sort((c,b,a),quote)"), n));
        assertEquals(
                Set.of($$("(1,2)")),
                eval($$c("sort({1,2},quote)"), n));
    }

    @Test
    void testSortApply_inline() {

        assertEquals(Set.of($$("(a,b,(c,d))")),
            eval($$c("sort(((c,d),b,a),complexity)"), n));
    }

    @Test
    void testSortApply_output() {
        assertEquals(
                Set.of($$("sort(((c,d),b,a),complexity,(a,b,(c,d)))")),
                eval($$c("sort(((c,d),b,a),complexity,#x)"), n));
    }


    @Test
    void testSortSubst1() {
        assertEquals(Set.of($$("sort((2,1),quote,(1,2))")),
                eval($$c("sort((2,1),quote,#a)"), n));
        assertEquals(
                Set.of($$(/*"(sort((2,1),quote,(1,2))==>(1,2))"*/ "(1,2)")),
                eval($$c("(sort((2,1),quote,#a) ==> #a)"), n));
    }


    @Test
    void testSortSubst2() {
        assertEquals(
                Set.of($$("(&&,sort((1,2),quote,(1,2)),append(1,(),1),append(2,(),2))")),
                eval($$c("(&&, append(1,(),#a),append(2,(),#b),sort((#a,#b),quote,#sorted))"), n));
    }
    @Test
    void testSortSubst3() {
        assertEquals(
                Set.of($$(/*"(sort((3,2),quote,(2,3))&&add(1,2,3))"*/"add(1,2,3)")),
                eval($$c("(&&,add(1,#x,#a),sort((#a,2),quote,(2,3)))"), n));
    }


    @Test void TermutesShuffled() {

        Term s = $$("(member(#1,{a,b,c})&&(x-->#1)))");

        Set<List<Term>> permutes = new HashSet();

        for (int i = 0; i < 64; i++) {
            List<Term> f = new Lst();
            if (Functor.evalable(s)) {
                new Evaluation(new Evaluator(n::axioms), f::add).eval(s);
            }
            permutes.add(f);
        }

        System.out.println(permutes);
        assertEquals(6, permutes.size());
    }
}