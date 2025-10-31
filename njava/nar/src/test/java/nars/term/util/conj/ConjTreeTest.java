package nars.term.util.conj;

import nars.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.ETERNAL;
import static nars.term.atom.Bool.False;
import static nars.term.util.Testing.assertEq;

class ConjTreeTest {

    private static final Term x = $$("x");
    private static final Term y = $$("y");

    @Test
    void testSimple() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x);
        t.add(ETERNAL, y);
        assertEq("(x&&y)", t.term());
    }

    @Test
    void testSimpleWithNeg() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x.neg());
        t.add(ETERNAL, y);
        assertEq("((--,x)&&y)", t.term());
    }


    @Test
    void testContradict1() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x);
        t.add(ETERNAL, x.neg());
        assertEq(False, t.term());
    }

    @Test
    void testDisjReductionOutward() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x);
        t.add(ETERNAL, $$("(x||y)"));
        assertEq(x, t.term());
    }
    @Test
    void testDisjReductionOutwardSeq() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x);
        t.add(0, $$("--(--y &&+1 --x)"));
        assertEq(x, t.term());
    }




    @Test
    void testContradictionInward() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x);
        t.add(1, x.neg());
        assertEq(False, t.term());
    }
    @Test
    void testContradictionInward2() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x);
        t.add(1, y);
        t.add(2, x.neg());
        assertEq(False, t.term());
    }

}