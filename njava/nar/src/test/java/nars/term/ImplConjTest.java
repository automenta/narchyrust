package nars.term;

import nars.Narsese;
import nars.Op;
import nars.Term;
import nars.term.atom.Bool;
import nars.term.util.conj.ConjSeq;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ImplConjTest {

    @Test
    void ConjInImplicationTautology() {
        assertEq(Bool.True, "((x &&+2 x) ==>-2 x)");
    }


    @Test
    void ConjInImplicationTautology2() {
        assertEq(Bool.True, "((((_1,_2)&|(_1,_3)) &&+2 ((_1,_2)&|(_1,_3))) ==>-2 ((_1,_2)&|(_1,_3)))");
    }

    /**
     * TODO decide if it should not apply this reduction to eternal
     */
    @Disabled @Test
    void ConjImplReduction0() {
        assertEq(

                "((inside(john,playground)==>inside(bob,kitchen))&&inside(bob,office))",
                "(inside(bob,office) && (inside(john,playground)==>inside(bob,kitchen)))");
    }

    @Disabled @Test
    void ConjImplReduction() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>+1 (b,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.the(4, a, b);

        assertEquals(

                "(((a,b) ==>+1 (b,c)) &&+4 (c,d))",
                x.toString());
    }

    @Disabled @Test
    void ConjImplNonReductionNegConj() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>+1 (b,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.the(-4, a, b);

        assertEquals(
                "((c,d) &&+4 ((a,b) ==>+1 (b,c)))",

                x.toString());
    }

    @Disabled @Test
    void ConjImplReductionNegConj2() throws Narsese.NarseseException {
        Term b = $("(c,d)");
        Term a = $("((a,b) ==>+1 (b,c))");
        Term x = Op.CONJ.the(4, b, a);

        assertEquals(

                "((c,d) &&+4 ((a,b) ==>+1 (b,c)))",
                x.toString());
    }

    @Disabled @Test
    void ConjImplNonReductionNegConj2() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>+1 (b,c))");
        Term b = $("(c &&+1 d)");
        assertEq("((c &&+1 d) &&+4 ((a,b) ==>+1 (b,c)))", Op.CONJ.the(-4, a, b));
    }

    @Disabled @Test
    void ConjImplNonReductionNegConj3() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>+1 (b,c))");
        Term b = $("(c &&+1 d)");
        Term x = Op.CONJ.the(+4, a, b);

        assertEquals(

                "((((a,b) ==>+1 (b,c)) &&+4 c) &&+1 d)",
                x.toString());

        Term x2 = ConjSeq.conjAppend(a, 4, b, Op.terms);
        assertEquals(x, x2);
    }

    @Disabled
    @Test
    void ConjImplReductionNegConj2b() throws Narsese.NarseseException {
        Term b = $("(c,d)");
        Term a = $("((a,b) ==>-1 (b,c))");
        Term x = Op.CONJ.the(4, b, a);

        assertEquals(

                "((c,d) &&+4 ((a,b) ==>-1 (b,c)))",
                x.toString());
    }

    @Disabled @Test
    void ConjImplReductionNegImpl() throws Narsese.NarseseException {
        Term a = $("((a,b) ==>-1 (b,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.the(4, a, b);

        assertEquals(

                "(((a,b) ==>-1 (b,c)) &&+4 (c,d))",
                x.toString());
    }

    @Disabled @Test
    void ConjImplReductionWithVars() throws Narsese.NarseseException {
        Term a = $("((a,#1) ==>+1 (#1,c))");
        Term b = $("(c,d)");
        Term x = Op.CONJ.the(4, a, b);

        assertEquals(

                "(((a,#1) ==>+1 (#1,c)) &&+4 (c,d))",
                x.toString());
    }

    @Disabled @Test
    void ConjImplReduction1() {
        assertEq(

                "((inside(john,playground)==>inside(bob,kitchen))&&inside(bob,office))",
                "(inside(bob,office)&&(inside(john,playground)==>inside(bob,kitchen)))");
    }

    @Disabled @Test
    void ConjImplReduction2() throws Narsese.NarseseException {


        Term t = $("(inside(bob,office) &&+1 (inside(john,playground) ==>+1 inside(bob,kitchen)))");

        assertEquals(

                "(inside(bob,office) &&+1 (inside(john,playground) ==>+1 inside(bob,kitchen)))",
                t.toString()
        );
    }

    @Disabled @Test
    void ConjImplReductionNeg2() {

        assertEq(

                "(inside(bob,office) &&+1 ((--,inside(john,playground)) ==>+1 inside(bob,kitchen)))",
                "(inside(bob,office) &&+1 (--inside(john,playground) ==>+1 inside(bob,kitchen)))");
    }

    @Disabled @Test
    void ConjImplReduction3() {

        assertEq(

                "((j ==>-1 k) &&+1 b)",
                "((j ==>-1 k) &&+1 b)");

        assertEq(

                "((j ==>-1 k) &&+1 b)",
                "(b &&-1 (j ==>-1 k))");
    }



}