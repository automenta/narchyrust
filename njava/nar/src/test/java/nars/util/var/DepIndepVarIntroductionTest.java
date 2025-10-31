package nars.util.var;

import jcog.random.XoRoShiRo128PlusRandom;
import nars.*;
import nars.action.transform.VariableIntroduction0;
import nars.term.util.Terms;
import nars.term.util.var.DepIndepVarIntroduction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.Op.QUESTION;
import static nars.term.util.Testing.assertEq;
import static nars.term.util.var.DepIndepVarIntroduction.depIndepFilter;
import static nars.term.util.var.DepIndepVarIntroduction.nonNegdepIndepFilter;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 3/30/17.
 */
class DepIndepVarIntroductionTest {

    @Test void invalidTaskWithIndepVar() {
        var x = $$c("(&&,Sells($1,#2,#3),Weapon(#2),Hostile(#3))");
        assertNull(NALTask.taskTerm(x, QUESTION, true));
        //NALTask y = NALTask.taskEternal(x, QUESTION, null, NARS.tmp(1));

    }

    @Test
    void IntroduceDepVar2() {
        var i = introduce("(((a,#1)-->c) &&+1 ((b,#1)-->c))", 16);
        assertEquals("[(((a,#1)-->#3) &&+1 ((b,#1)-->#3))]",
                i.toString());
    }
    @Test
    void IntroduceIndepVar() {
        assertEquals("[((a-->$1)==>(b-->$1))]",
                introduce("((a-->c)==>(b-->c))", 16).toString());
    }

    private static final String none = "[☢]";

    @Test
    void DontReplaceImages() {
        assertEquals(none, introduce("((x,/,y)==>(w,/,z))", 16).toString());
    }

    @Test
    void IntroduceIndepVar2() {
        var x = "((a-->(x,#1))==>(b-->(x,#1)))";
        var input = $$(x);
        var r = DepIndepVarIntroduction.subtermRepeats(input.subterms(), depIndepFilter, 2);
        assertEquals(2, r.length);
        Arrays.sort(r);
        assertEq("(x,#1)", r[0]);
        assertEq("x", r[1]);


        assertEquals("[((a-->($3,#1))==>(b-->($3,#1))), ((a-->$3)==>(b-->$3))]",
                introduce(x, 128).toString());
    }

    @Test
    void DontIntroduceIndepVarInNeg() {
        var x = "((a,--(x,#1))==>(b,--(x,#1)))";
        var input = $$(x);
        var r = DepIndepVarIntroduction.subtermRepeats(input.subterms(), nonNegdepIndepFilter, 2);
        assertNotNull(r);
        assertEquals(2, r.length);
        Arrays.sort(r);
        assertEq("(x,#1)", r[0]);
        assertEq("x", r[1]);

    }

    @Test
    void SubtermScore() {
        assertEquals("{y=3, x=4}",
                Terms.subtermScore($$c("((x,x,x,x),(y,y,y))"), (t1) -> 1, 2).toString());
    }

    @Test
    void SubtermScore_Intrinsic() {
        assertEquals("{%1=4, %2=3}",
            Terms.subtermScore($$c("((%1,%1,%1,%1),(%2,%2,%2))"), t -> 1, 2).toString());
    }

    @Test
    void IntroduceDepVar() {
        assertVarIntro("((a-->c),(b-->c))", "((a-->#1),(b-->#1))");
    }

    @Test
    void IntroduceDepVar3() {
//        assertEquals("[((#1-->a)&&(#1-->b))]",
//                introduce("(&&,(c-->a),(c-->b))", 16).toString());

        assertEquals("[((#1-->(a&&b))&&(c-->#1))]",
                introduce("(&&,(x-->a),(x-->b),(c-->x))", 16).toString());
    }
    @Test
    void IntroduceDepVar4() {
        assertEquals("[((c-->(#1,b,x))&&(x-->(b&&#1))), ((c-->(a,b,#1))&&(#1-->(a&&b))), ((c-->(a,#1,x))&&(x-->(a&&#1))), ((c-->(#1,b,x))&&(x-->(b&&#1)))]",
                introduce("(&&,(x-->a),(x-->b),(c-->(a,b,x)))", 32).toString());
    }

    @Test
    void IntroduceDepVar_repeats() {
        assertEquals("[((#1-->c) &&+1 (#1-->c)), ((a-->#1) &&+1 (a-->#1)), (#1 &&+1 #1)]",
                introduce("((a-->c) &&+1 (a-->c))", 64).toString());

    }

    private final NAR n = NARS.shell();

    private SortedSet<Term> introduce(String term, int iterations) {
        Supplier<SortedSet> collectionFactory = TreeSet::new;
        var sortedSet = collectionFactory.get();
        for (var i = 0; i < iterations; i++) {
            var varIntro = n.eval($.func("varIntro", $$(term).normalize()));
            if (varIntro != null) {
                sortedSet.add(varIntro);
            }
        }
        return (SortedSet<Term>) sortedSet;
    }

    @Disabled @Test void implPred() {
        var x = $$c("((((fz-->$3) &&+380 (fz-->((#1,#2),x))) ==>+5840 (fz-->$3))&&(cmp(#1,#2)=1))");
        var y = DepIndepVarIntroduction.the.apply(x, new XoRoShiRo128PlusRandom(3), null);
        assertEq("(((($7-->$3) &&+380 ($7-->((#1,#2),x))) ==>+5840 ($7-->$3))&&(cmp(#1,#2)=1))", y);

//        assertVarIntro("((((fz-->$3) &&+380 (fz-->((#1,#2),x))) ==>+5840 (fz-->$3))&&(cmp(#1,#2)=1))",
//                "(((($7-->$3) &&+380 ($7-->((#1,#2),x))) ==>+5840 ($7-->$3))&&(cmp(#1,#2)=1))");
    }

    @Disabled @Test void seq() {
        assertVarIntro("((--,((PoleCart-->clear) &&+20 (PoleCart-->happy)))&&(PoleCart-->((--,shift)&&happy)))", "((--,((#1-->clear) &&+20 (#1-->happy)))&&(#1-->((--,shift)&&happy)))");
    }

    @Deprecated private static void assertVarIntro(String X, String Y) {
        var V = new VariableIntroduction0();

        var x = $$c(X);
        var y = VariableIntroduction0.apply(x, new XoRoShiRo128PlusRandom(1), null);
        assertNotNull(y);
        assertEq(Y,y);
    }

}