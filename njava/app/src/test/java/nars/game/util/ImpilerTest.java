package nars.game.util;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import nars.*;
import nars.game.util.Impiler.ImpilerDeduction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ImpilerTest {

    @Test
    void testEternal1() throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();
        n.input("(a ==> b).");
        n.input("(b ==> c).");
        Impiler.load(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[((a&&b)==>c). 0 %1.0;.81%]", deduceStr(d, $$("a"), true, 0));
        assertEquals("[((a&&b)==>c). 0 %1.0;.81%]", deduceStr(d, $$("c"), false, 0));

    }

    @Test
    void testEternalInnerNegation_must_match_fwd1() throws Narsese.NarseseException {
        //test both matching and non-matching case, make sure only matching is invovled
        NAR n = NARS.threadSafe();
        n.input("(a ==> b).");
        n.input("(      b ==> c)."); //likely
        n.input("(    --b ==> d)."); //unlikely
        Impiler.load(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[((a&&b)==>c). 0 %1.0;.81%]", deduceStr(d, $$("a"), true, 0));
    }
    @Test
    void testEternalInnerNegation_semi_match_fwd1() throws Narsese.NarseseException {
        //test both matching and non-matching case, make sure only matching is invovled
        NAR n = NARS.threadSafe();
        n.input("(a ==> b).");
        n.input("(      b ==> c). %1.00;0.90%"); //likely
        n.input("(      b ==> d). %0.25;0.90%"); //unlikely
        Impiler.load(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[((a&&b)==>c). 0 %1.0;.81%,((a&&b)==>d). 0 %.25;.61%]", deduceStr(d, $$("a"), true, 0));
    }
    @Test
    void testEternalInnerNegation_must_match_fwd_inverse() throws Narsese.NarseseException {
        //TODO
        NAR n = NARS.threadSafe();
        n.input("(a ==> --b).");
        n.input("(        b ==> c).");  //unlikely
        n.input("(      --b ==> d).");  //likely
        Impiler.load(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[(((--,b)&&a)==>d). 0 %1.0;.81%]",
                deduceStr(d, $$("a"), true, 0));
    }

    @Test
    void testEternalInnerNegation_must_match_rev() throws Narsese.NarseseException {
        //test both matching and non-matching case, make sure only matching is invovled
        NAR n = NARS.threadSafe();
        n.input("(a ==>   b)."); //likely
        n.input("(x ==> --b)."); //unlikely
        n.input("(        b ==> c).");
        Impiler.load(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[((a&&b)==>c). 0 %1.0;.81%]", deduceStr(d, $$("c"), false, 0));
    }

    @Test
    void testEternalOuterNegation_FwdStart() throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();
        n.input("(--a ==> b).");
        n.input("(b ==> c).");
        Impiler.load(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[(((--,a)&&b)==>c). 0 %1.0;.81%]", deduceStr(d, $$("a"), true, 0));
    }
    @Test
    void testEternalOuterNegation_FwdEnd() throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();
        n.input("(a ==> b).");
        n.input("(b ==> --c).");
        Impiler.load(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[((a&&b)==>c). 0 %0.0;.81%]", deduceStr(d, $$("a"), true, 0));
    }

    @Test
    void testEternal2() throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();

        n.input("(a ==> b).");
        n.input("(--a ==> c). %0.9;0.5%");
        n.input("((c&&d) ==> e). %1.0;0.9%");

        n.input("(b ==> c). %0.9;0.5%");
        n.input("(c ==> d). %0.9;0.5%");
        n.input("(d ==> e). %0.9;0.5%");
        n.input("(e ==> f). %0.9;0.5%");


        Impiler.load(n);

//        Impiler.graphGML(n.concepts()::iterator, System.out);

        ImpilerDeduction d = new ImpilerDeduction(n);

        assertEquals(2, d.get($$("a"), 0, true).size());

        assertEquals(2, d.get($$("d"), 0, false).size());

    }

    private static String deduceStr(ImpilerDeduction d, Term x, boolean forward, long at) {
        List<NALTask> r = d.get(x, at, forward);
        return '[' + Joiner.on(",").join(Iterables.transform(r, Task::toStringWithoutBudget)) + ']';
    }

    @Test
    void testDeductionChainPositive() throws Narsese.NarseseException {
        NAR n = NARS.tmp(1);




//        Impiler.ImpilerDeduction d = new Impiler.ImpilerDeduction(n) {
//
//        };

//        n.synch();

//        n.log();
//        n.run(16);


        n.input("(a ==> b). ");
        n.run(1);
        n.input("(b ==> c). ");
        n.run(1);
        n.input("(c ==> d). ");
        n.run(1);
        n.input("(d ==> e). ");
        n.run(1);


        n.input("a@");
        n.run(1);

//        assertTrue(4 <= edges[0]);


        //d.graphGML(System.out)

    }

}