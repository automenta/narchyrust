package nars.term.util.conj;

import nars.Term;
import nars.term.Compound;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.TreeSet;
import java.util.function.Supplier;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.Op.ETERNAL;
import static nars.term.atom.Bool.False;
import static nars.term.atom.Bool.True;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.*;

class ConjDiffTest {


    @Deprecated static ConjList subtract(ConjList from, Term exc, long when) {
        if (exc.CONJ()) {
            ((Compound) exc).conds(
                    when == ETERNAL ?
                            (subWhen, subExc) -> from.removeAll(subExc) :
                            from::remove
                    , when, true, false, false);
        } else {
            from.remove(when, exc);
        }
        return from;
    }

    @Deprecated static ConjBuilder diff(Term include, long includeAt, Term exclude, long excludeAt) {

        return subtract(ConjList.conds(include, includeAt, true, false), exclude, excludeAt);
//        boolean eNeg = exclude.op() == NEG;
//        return the(include, includeAt, eNeg ? exclude.unneg() : exclude, excludeAt, eNeg);
    }

    @Test
    void testConjDiff_EliminateSeq() {

        assertEq("c", diff(
                $$c("(b &&+5 c)"), 5, $$("(a &&+5 b)"), 0).term());
        assertEq("c", diff(
                $$c("(--b &&+5 c)"), 5, $$("(a &&+5 --b)"), 0).term());
    }


    @Disabled @Test
    void testConjDiff_Eliminate_invert() {

        assertEq(False,
                "(--(x &&+1 y) ==>+0 (y &&+1 z))");
//        assertEq("((--,(x &&+1 y)) ==>+2 z)",
//                "(--(x &&+1 y) ==>+1 (--y &&+1 z))");

//        assertEq("c", ConjDiff.the(
//                $$("(--x &&+5 c)"), 5, $$("x"), ETERNAL, true).target()); //unchanged
//        assertEq("((--,x) &&+5 c)", ConjDiff.the(
//                $$("(--x &&+5 c)"), 5, $$("x"), 0, true).target()); //unchanged

    }



    @Test
    void testConjDiff_EternalComponents_Same_Masked() {
        //x && ... common to both
        assertEq("z", diff(
                $$("(x&&z)"), 0, $$("(x&&y)"), 0).term());

        assertEq("z", diff(
                $$("(x&&z)"), 0, $$("(x&&y)"), ETERNAL).term());
//        assertEq("z", ConjDiff.the(
//                $$("(x&&z)"), 0, $$("(x&&y)"), ETERNAL).target());

        assertEq("z", diff(
                $$("(x&&z)"), 1, $$("(x&&y)"), 1).term());
        assertEq("(x&&z)", diff(
                $$("(x&&z)"), 1, $$("(x&&y)"), 0).term());
        assertEq("(x&&z)", diff(
                $$("(x&&z)"), 0, $$("(x&&y)"), 1).term());
    }
    @Test
    void testConjDiff_EternalComponents_Same_Masked_seq() {
        ConjBuilder d = diff(
                $$("((b &&+5 c)&&x)"), 0,
                $$("b" /*"(x&&(a &&+5 b))"*/), 0);
        assertEq("(x &&+5 (c&&x))", d.term());
    }

    @Test
    void testConjDiff_EternalComponents_Diff_Masked_Ete() {
        assertEq("(w&&z)", diff(
                $$("(w && z)"), 5, $$("(x && y)"), 0).term());
    }
    @Test
    void testConjDiff_EternalComponents_Diff_Masked_Seq() {

        assertEquals(5, $$("((a &&+5 b)&&x)").seqDur());

        assertEq(
                "(((a&&x) &&+5 (b&&x))==>(y &&+5 (c&&y)))",
                //"(((a &&+5 b)&&x)==>((b &&+5 c)&&y))",
                "(((a &&+5 b)&&x)==>((b &&+5 c)&&y))");
    }
    @Disabled @Test
    void testConjDiff_EternalComponents_Diff_Masked_Seq2() {
        //????
        assertEq("(y &&+5 (c&&y))", diff(
                $$("(y && (b &&+5 c))"), 5, $$("(x && (a &&+5 b))"), 0).term());
    }

    @Test void ConjWithoutPN_EliminateOnlyOneAtAtime_Seq() {
        Term x = $$("x"), y = $$("y");
        Term xy = $$("((x &&+4120 y) &&+1232 --y)");

//        String both = "[(x &&+5352 (--,y)), (x &&+4120 y)]";
        assertConjDiffPN(xy, y, "[x]"); //2 compound results
        assertConjDiffPN(xy, y.neg(), "[x]"); //2 compound results
        assertConjDiffPN(xy, x, "[(y &&+1232 (--,y))]"); //1 compound result
    }

    private final Term xy = $$("((x &&+4120 (y&&z)) &&+1232 --y)");

    @Test void ConjWithoutPN_EliminateOnlyOneAtAtime_Seq_with_inner_Comm() {
        assertConjDiffPN(xy, $$("(y&&z)"), "[x]");
        //assertConjDiffPN(xy, $$("(y&&z)").neg(), "[(x &&+5352 (--,y))]");
    }

    @Disabled @Test void ConjWithoutPN_EliminateOnlyOneAtAtime_Seq_with_inner_Comm_unify() {
        assertConjDiffPN(xy, $$("(z &&+1232 --y)"), "[(x &&+4120 y)]");
    }

    @Test void ConjWithoutPN_EliminateOnlyOneAtAtime_Comm2() {
        Term x = $$("x"), y = $$("y");
        Term xy = $$("(x && y)");
        assertConjDiffPN(xy, y, "[x]");
        assertConjDiffPN(xy, x, "[y]");
    }

    @Test void parallelSubEvent() {
        Term abc = $$("(&&,a,b,c)");
        Term bc = $$("(&&,b,c)");
        assertEventOf(abc, bc);
        assertEq("a", CondDiff.diffAllPN(abc, bc));
    }



    @Disabled
    @Test void diffCommComm() {
        assertEq("(b&&c)", CondDiff.diffAll($$("(&&,a,b,c)"), $$("(&&,a,d,e)")));
        assertEq("a", CondDiff.diffAll($$("(&&,a,b,c)"), $$("(&&,b,c,e)")));
        assertEq("b", CondDiff.diffAll($$("(&&,a,b,c)"), $$("(&&,a,c,e)")));

        assertEq("b", CondDiff.diffAll($$("(&&,a,b)"), $$("(&&,a,c)")));
        assertEq("a", CondDiff.diffAll($$("(&&,a,b)"), $$("(&&,b,c)")));

    }

    @Test void conjWithoutPN_repeat_remove_only_one() {
        assertEq(True, CondDiff.diffAllPN($$("(x &&+1 x)"), $$("x")));
        assertEq("x", CondDiff.diffFirst($$("(x &&+1 x)"), $$("x"), true));

    }
    @Test void conjWithoutPN_repeat_remove_only_one2() {
        assertConjDiffPN(
                $$("(((--,y(z(4,1))) &&+22880 (--,x(z(4,4)))) &&+480 (--,x(z(4,4))))"),
                $$("x(z(4,4))"),
                false,
                "[((--,y(z(4,1))) &&+23360 (--,x(z(4,4))))]"
        );
    }
    @Test void conjWithoutPN_repeat_remove_only_one3() {
        final Term a = $$("((a &&+1 (&&,a,b,c,d)) &&+1 c)");
        final Term b = $$("((b&&d) &&+1 c)");
        assertConjDiffPN(
                a,
                b,
                false,
                "[(a &&+1 (a&&c))]"
        );
    }
    @Disabled @Test void eventOfSeqSeqBundled() {
        Compound x = $$c("((a &&+1 ((x)-->(a&&c))) &&+1 b)");
        Term y = $$("(c(x) &&+1 b)");
        assertTrue(x.condOf(y));
    }

    @Test void diffSubSeq1() {
        Term x = $$("(a &&+1 (((x)-->(a&&c))&&b))");
        Term y = $$("(c(x)&&b)");
        assertEq("(a &&+1 a(x))",
                CondDiff.diffFirst(x, y)
        );
    }

    @Test void negatedSubEvent() {
        String s = "(((--,t(2,1))&&(t-->nearY)) &&+- (--,(t-->nearY)))";
        assertEq(s, CondDiff.diffAll($$(s), $$("t(2,1)")));
        assertEq("((--,(t-->nearY)) &&+- (t-->nearY))", CondDiff.diffAll($$(s), $$("t(2,1)").neg()));
    }
    private static void assertConjDiffPN(Term xy, Term r, String s) {
        assertConjDiffPN(xy, r, true, s);
    }
    private static void assertConjDiffPN(Term xy, Term r, boolean allOrFirst, String s) {

        //assertEventOf(xy, r);

        Supplier<TreeSet> collectionFactory = TreeSet::new;
        var results = collectionFactory.get();
        for (int i = 0; i < 16; i++) {
            results.add( allOrFirst ? CondDiff.diffAllPN(xy, r) : CondDiff.diffFirst(xy, r, true));
        }
        assertEquals(s, results.toString());
    }

    static void assertEventOf(Term xy, Term x) {
        assertTrue(((Compound) xy).condOf(x), ()->"eventOf(" + xy + ","+ x +")" ); //test detection
    }
    static void assertNotEventOf(Term xy, Term x) {
        assertFalse(((Compound) xy).condOf(x), ()->"!eventOf(" + xy + ","+ x +")" ); //test detection
    }

}