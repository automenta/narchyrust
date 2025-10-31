package nars.term.util.conj;

import jcog.random.RandomBits;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.Term;
import nars.term.Compound;
import nars.unify.Unify;
import nars.unify.UnifyTransform;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.Op.XTERNAL;
import static nars.term.atom.Bool.Null;
import static nars.term.atom.Bool.True;
import static nars.term.util.conj.CondMatch.match;
import static org.junit.jupiter.api.Assertions.*;

class ConjMatchTest {

    private static Unify ut() {
        return new UnifyTransform(new RandomBits(new XoRoShiRo128PlusRandom(1))).ttl(10);
    }

    @Test void condTimeSeq_1() {
        assertEquals(0, $$c("(x &&+1 b)").when($$("x"), true));
        assertEquals(1, $$c("(x &&+1 b)").when($$("b"), true));
    }

    @Test
    void beforeAfter1() {


        Term seq = $$("( ( ( (d(x,#1) &&+1 e(#1)) &&+1 (b(#1)&&c))  &&+1 c(#1)) &&+1 f)");
        Term cmd = $$("(b(x) &&+1 c(x))");
        assertEquals($$("(c &&+2 f)"), match((Compound) seq, cmd, false,
                false, false, true, ut()));
        assertEquals($$("((d(x,x) &&+1 e(x)) &&+1 c)"), match((Compound) seq, cmd, false,
                true, false, false, ut()));
    }

    @Test
    void beforeAfter2() {

        Term seq = $$("((d(x,#1) &&+1 e(#1)) &&+1 ((b(#1)&&c) &&+1 c(#1)))");
        Term cmd = $$("(b(x) &&+1 c(x))");
        Term result = $$("((d(x,x) &&+1 e(x)) &&+1 c)");


        assertEquals(result, match((Compound) seq, cmd, false, true, false, false, ut()));

    }
    @Test
    void beforeAfter2_seqfactored() {
        Term c = $$("(z&&(x &&+1 y))");
        Term e = $$("(x&&z)");
        Term y = $$("(y&&z)");

        assertEquals(y, match((Compound) c, e, false, false, false, true, ut()));

    }
    @Test
    void dtTolerance() {
        Term seq = $$("((d(x,#1) &&+1 e(#1)) &&+1 ((b(#1)&&c) &&+1 c(#1)))");
        Term cmd = $$("(b(x) &&+2 c(x))");
        Term result = $$("((d(x,x) &&+1 e(x)) &&+1 c)");

        {
            Unify u = ut();
            assertEquals(Null, match((Compound) seq, cmd, false, true, false, false, u));
        }
        {
            Unify u = ut();
            u.dur = 2;
            assertEquals(result, match((Compound) seq, cmd, false, true, false, false, u));
        }
    }

    @Test
    void sequenceWarp() {
        int dur = 3;
        for (int ab : new int[] { 4, 5}) {
            for (int bc : new int[] { 4, 5}) {
                for (int cd : new int[] { 4, 5}) {
                    Term a = $$("(((a &&+5 b) &&+5 c) &&+5 d)");
                    Term b = $$("(((a &&+" + ab + " b) &&+" + bc + " c) &&+" + cd + " d)");
                    Unify u = ut();
                    u.dur = dur;
                    assertEquals(True, match((Compound) a, b, true, true, false, true, u),
                            ()->a + " != " + b);
                }
            }
        }
    }


    @Test void condFirst_but_equal() {
        assertTrue($$c("(a&&b)").condFirst($$("(a&&b)")));
    }

    @Test
    void condFirst_par() {
        assertTrue($$c("(a&&b)").condFirst($$("a")));
        assertFalse($$c("(a&&b)").condFirst($$("c")));


        assertTrue($$c("(--a && b)").condFirst($$("--a")));
    }

    @Test
    void condFirst_par_inh() {
        assertTrue($$c("(x:a && x:b)").condFirst($$("(a-->x)")));
        assertTrue($$c("(--x:a && x:b)").condFirst($$("--(a-->x)")));
        assertTrue($$c("(a:x && b:x)").condFirst($$("(x-->a)")));
    }
    @Test
    void condFirst_par_inh_seq() {
        assertTrue($$c("((x:a && x:b) &&+1 y)")
                .condFirst($$("(a-->x)")));
    }

    @Disabled
    @Test
    void condFirst_par_inh_seq2() {
        assertTrue($$c("((((a&&b)-->x)&&z) &&+1 y)").condFirst($$("(a-->x)")));
        assertFalse($$c("(y &&+1 ((a&&b)-->x))").condFirst($$("(a-->x)")));

        assertTrue($$c("(x-->(a&&b))").condFirst($$("(x-->a)")));
    }

    @Disabled @Test
    void condFirst_par_inh_neg() {
        assertTrue($$c("((--a && b)-->x)").condFirst($$("--(a-->x)")));
        assertFalse($$c("((--a && b)-->x)").condFirst($$("--(b-->x)")));
    }
    @Test
    void condFirst_par_inh_neg2() {
        Compound x = $$c("(((--a && b)-->x) &&+1 y)");
        Term y = $$("--(a-->x)");
        assertTrue(x.condFirst(y));
    }

    @Test
    void condFirst_seq() {
        assertTrue($$c("(a &&+1 b)").condFirst($$("a")));
        assertFalse($$c("(a &&+1 b)").condFirst($$("b")));
        assertFalse($$c("(a &&+1 b)").condFirst($$("c")));
        assertTrue($$c("(a &&+1 b)").condFirst($$("(a &&+1 b)")));
    }

    @Test
    void condFirst_subseq() {
        assertTrue($$c("((a &&+1 b) &&+1 c)").condFirst($$("(a &&+1 b)")));
    }

    @Test
    void condFirst_subseq_n() {
        assertTrue($$c("(--(a &&+1 b) &&+1 c)").condFirst($$("--(a &&+1 b)")));
        assertFalse($$c("(--(a &&+1 b) &&+1 c)").condFirst($$("a")));
    }

    @Test
    void condFirst_seq2() {
        assertFalse($$c("(a &&+1 b)").condFirst($$("(a && b)")));
    }

    @Test void condFirst_seq3_tp() {
        assertTrue($$c("((a && x) &&+1 b)").condFirst($$("x")));
    }
    @Test void condFirst_seq3_tn() {
        assertTrue($$c("((a && --x) &&+1 b)").condFirst($$("--x")));
    }

    @Test void condFirst_seq3_f() {
        assertFalse($$c("(b &&+1 (a && x))").condFirst($$("x")));
    }
    @Test
    void condFirst_seq_embed() {
        assertTrue($$c("(z&&(x &&+1 y))").condFirst($$("z")));
    }

    @Test
    void condTime_seq_sanity_test() {

        assertEquals(XTERNAL,
            $$c("(a:b &&+5 (c:d &&+5 x:y))")
                    .when($$("d"), true));
        
    }
    @Test
    void CondStart_parallel_test() {
        assertTrue(
            $$c("(a&&b)").condStartEnd($$("a"), true, false)
        );
        assertTrue(
            $$c("(((in)-->cam) && ((left)-->cam))").condStartEnd($$("cam(left)"), true, false)
        );
    }

}