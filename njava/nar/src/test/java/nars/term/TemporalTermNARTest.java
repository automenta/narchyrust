package nars.term;

import nars.*;
import nars.concept.TaskConcept;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.TreeSet;

import static nars.$.*;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.*;


class TemporalTermNARTest {

    private final NAR n = NARS.shell();

    @Test
    void Conceptualization() throws Narsese.NarseseException {


        Term t = $("(x==>y)");

        n.input("(x ==>+0 y).", "(x ==>+1 y).").run(2);

        TaskConcept xImplY = (TaskConcept) n.conceptualize(t);
        assertNotNull(xImplY);

        assertEquals("(x ==>+- y)", xImplY.toString());

        assertEquals(2, xImplY.beliefs().taskCount());

        int indexSize = n.memory.size();
        n.memory.print(System.out);

        n.input("(x ==>+1 y). :|:");
        n.run();


        assertEquals(3, xImplY.beliefs().taskCount());

        n.memory.print(System.out);
        assertEquals(indexSize, n.memory.size());

        //n.conceptualize("(x==>y)").print();
    }

    @Disabled
    @Test
    void CommutiveTemporalityConcepts2() throws Narsese.NarseseException {


        for (String op : new String[]{"&&"}) {
            Concept a = n.conceptualize($("(x " + op + "   y)"));
            Concept b = n.conceptualize($("(x " + op + "+1 y)"));

            assertSame(a, b);

            Concept c = n.conceptualize($("(x " + op + "+2 y)"));

            assertSame(b, c);

            Concept d = n.conceptualize($("(x " + op + "-1 y)"));

            assertSame(c, d);

            Term e0 = $("(x " + op + "+- y)");
            assertEquals("(x " + op + "+- y)", e0.toString());
            Concept e = n.conceptualize(e0);

            assertSame(d, e);

            Term f0 = $("(y " + op + "+- x)");
            assertEquals("(x " + op + "+- y)", f0.toString());
            assertEquals("(x " + op + "+- y)", f0.root().toString());

            Concept f = n.conceptualize(f0);
            assertSame(e, f, () -> e + "==" + f);


            Concept g = n.conceptualize($("(x " + op + "+- x)"));
            assertEquals("(x " + op + "+- x)", g.toString());


            Concept h = n.conceptualize($("(x " + op + "+- (--,x))"));
            assertEquals("((--,x) " + op + "+- x)", h.toString());


        }

    }

    private static void conjGood(String aa) {
        Compound a = (Compound) $$$(aa);
        assertEquals(aa, a.toString());
        assertFalse(a.NORMALIZED());
        assertEq(aa.replace("#5", "#2"), a.normalize());
    }

    @Test
    void conjWTF() {
        conjGood("(((--,((#1-->#5) &&+128 (#1-->#5))) &&+128 (#1-->#5)) &&+768 (#1-->#5))");
    }

    @Test
    void CommutiveTemporalityConcepts() throws Narsese.NarseseException {


        n.input("(goto(#1) &&+5 ((SELF,#1)-->at)).");


        n.input("(goto(#1) &&-5 ((SELF,#1)-->at)).");


        n.input("(goto(#1) && ((SELF,#1)-->at)).");

        n.input("(((SELF,#1)-->at) &&-3 goto(#1)).");


        n.run(2);

        TaskConcept a = (TaskConcept) n.conceptualize("(((SELF,#1)-->at) && goto(#1)).");
        Concept a0 = n.conceptualize("(goto(#1) && ((SELF,#1)-->at)).");
        assertNotNull(a);
        assertSame(a, a0);


        //a.beliefs().print();

        assertTrue(a.beliefs().taskCount() >= 4);
    }

    @Test
    void CoNegatedSubtermConceptConj() {
        NAR n = NARS.shell();
        assertEq("(x &&+- x)", n.conceptualize($$("(x &&+10 x)")).toString());

        assertEq("((--,x) &&+- x)", n.conceptualize($$("(x &&+10 (--,x))")).toString());
        assertEq("((--,x) &&+- x)", n.conceptualize($$("(x &&-10 (--,x))")).toString());


    }
    @Test
    void Atemporalization() throws Narsese.NarseseException {
        assertEq("(x ==>+- y)", n.conceptualize($("(x ==>+10 y)")).term());
    }

    @Test
    void CoNegatedSubtermConceptImpl() throws Narsese.NarseseException {
        assertEq("(x ==>+- x)", n.conceptualize($("(x ==>+10 x)")).term());
        assertEq("((--,x) ==>+- x)", n.conceptualize($("((--,x) ==>+10 x)")).term());

        Term xThenNegX = $("(x ==>+10 (--,x))");
        assertEquals("(x ==>+- x)", n.conceptualize(xThenNegX).toString());

        assertEquals("(x ==>+- x)", n.conceptualize($("(x ==>-10 (--,x))")).toString());

    }

    @Test
    void NonCommutivityImplConcept() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.input("(x ==>+5 y).", "(y ==>-5 x).");
        n.run(1);

        TreeSet d = new TreeSet();
        n.main().concepts().forEach(x -> d.add(x.term()));

        assertTrue(d.contains($("(x ==>+- y)")));
        assertTrue(d.contains($("(y ==>+- x)")));
    }


    @Test
    void AtemporalizationSharesNonTemporalSubterms() throws Narsese.NarseseException {

        Task a = n.inputTask("(x ==>+10 y).");
        Task c = n.inputTask("(x ==>+9 y).");
        Task b = n.inputTask("(x <-> y).");
        n.run();

        Term aa = a.term();
        assertNotNull(aa);

        @Nullable Concept na = n.concept(a.term(), true);
        assertNotNull(na);

        @Nullable Concept nc = n.concept(c.term(), true);
        assertNotNull(nc);

        assertSame(na, nc);

        assertSame(na.term().sub(0), nc.term().sub(0));


        assertEquals(n.concept(b.term(), true).term().sub(0), n.concept(c.term(), true).term().sub(0));

    }

    @Test
    void Anonymization2() throws Narsese.NarseseException {
        Termed nn = $("((do(that) &&+1 (a)) ==>+2 (b))");
        assertEquals("((do(that) &&+1 (a)) ==>+2 (b))", nn.toString());


        assertEquals("((do(that) &&+- (a)) ==>+- (b))", n.conceptualize(nn).toString());


    }

}