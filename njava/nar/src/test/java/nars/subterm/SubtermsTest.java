package nars.subterm;

import nars.Term;
import nars.term.Compound;
import nars.term.atom.Atomic;
import nars.term.util.SetSectDiff;
import nars.term.util.Terms;
import nars.term.util.Testing;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.$.$$c;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 3/1/16.
 */
class SubtermsTest {

    @Test
    void testUnionReusesInstance() {
        Compound container = $$c("{a,b}");
        Compound contained = $$c("{a}");
        assertSame(Terms.union(container.op(), container, contained), container);
        assertSame(Terms.union(contained.op(), contained, container), container);
        assertSame(Terms.union(container.op(), container, container), container);
    }

    @Test
    void testDifferReusesInstance() {
        Compound x = $$c("{x}");
        Compound y = $$c("{y}");
        assertSame(SetSectDiff.differenceSet(x.op(), x, y), x);
    }
    @Test
    void testIntersectReusesInstance() {
        Compound x = $$c("{x,y}");
        Compound y = $$c("{x,y}");
        assertSame(Terms.intersect(x.op(), x, y), x);
    }

    @Test
    void testSomething() {
        Compound x = $$c("{e,f}");
        Compound y = $$c("{e,d}");

//        System.out.println(Terms.intersect(x.op(), x, y));
//        System.out.println(SetSectDiff.differenceSet(x.op(), x, y));
//        System.out.println(Terms.union(x.op(), x, y));

    }

    @Test
    void testEqualityOfUniSubtermsImpls() {
        Term a = Atomic.atomic("a");
        Subterms x = new UnitSubterm(a);
        Subterms x0 = new UnitSubterm(a);
        Assertions.assertEquals(x, x0);

        Subterms y = new ArraySubterms(a);
        Assertions.assertEquals(y.hashCode(), x.hashCode());
        Assertions.assertEquals(y.hashCodeSubterms(), x.hashCodeSubterms());
        Assertions.assertEquals(x, y);
        Assertions.assertEquals(y, x);

        Subterms z = new UnitSubterm(a);
        Assertions.assertEquals(y.hashCode(), z.hashCode());
        Assertions.assertEquals(y.hashCodeSubterms(), z.hashCodeSubterms());
        Assertions.assertEquals(y, z);
        Assertions.assertEquals(x, z);
        Assertions.assertEquals(z, y);
        Assertions.assertEquals(z, x);

    }

    @Test
    void testEqualityOfBiSubtermsImpls() {
        Term a = Atomic.atomic("a");
        Term b = Atomic.atomic("b");
        Subterms x = new BiSubterm(a,b);
        Subterms x0 = new BiSubterm(a, b);
        Assertions.assertEquals(x, x0);

        Subterms y = new ArraySubterms(a, b);

        Testing.assertEq(x, y);

        Assertions.assertEquals(y.hashCode(), x.hashCode());
        Assertions.assertEquals(y.hashCodeSubterms(), x.hashCodeSubterms());
        Assertions.assertEquals(x, y);
        Assertions.assertEquals(y, x);

        Subterms z =  new BiSubterm(a,b);
        Assertions.assertEquals(y.hashCode(), z.hashCode());
        Assertions.assertEquals(y.hashCodeSubterms(), z.hashCodeSubterms());
        Assertions.assertEquals(y, z);
        Assertions.assertEquals(x, z);
        Assertions.assertEquals(z, y);
        Assertions.assertEquals(z, x);

    }

    @Test void BiSubtermsContainsNeg() {
        assertTrue( $$c("(--x &&+1 x)").containsNeg($$("x")) );
        assertTrue( $$c("(--x &&+1 x)").containsNeg($$("--x")) );
        assertTrue( $$c("(--x &&+1 x)").contains($$("x")) );
        assertTrue( $$c("(--x &&+1 x)").contains($$("--x")) );
    }
//    @Test
//    void testEqualityOfBiSubtermReverseImpls() {
//        Term a = Atomic.the("a");
//        Term b = Atomic.the("b");
//        Subterms ab = new BiSubterm(a,b);
//        Subterms x = new BiSubterm.ReversibleBiSubterm(a,b).reverse();
//        Subterms x0 = new BiSubterm.ReversibleBiSubterm(a,b).reverse();
//        Assertions.assertEquals(x, x0);
//        assertNotEquals(ab, x);
//
//        Subterms y = new ArrayTermVector(b, a);
//        Assertions.assertEquals(x, y);
//
//        Assertions.assertEquals(y.hashCode(), x.hashCode());
//        Assertions.assertEquals(y.hashCodeSubterms(), x.hashCodeSubterms());
//        Assertions.assertEquals(x, y);
//        Assertions.assertEquals(y, x);
//
//        Subterms z =  new BiSubterm.ReversibleBiSubterm(a,b).reverse();
//        Assertions.assertEquals(y.hashCode(), z.hashCode());
//        Assertions.assertEquals(y.hashCodeSubterms(), z.hashCodeSubterms());
//        Assertions.assertEquals(y, z);
//        Assertions.assertEquals(x, z);
//        Assertions.assertEquals(z, y);
//        Assertions.assertEquals(z, x);
//
//    }


//    /**
//     * recursively
//     */
//    @NotNull
//    private static boolean commonSubtermOrContainment(@NotNull Term a, @NotNull Term b) {
//
//        boolean aCompound = a instanceof Compound;
//        boolean bCompound = b instanceof Compound;
//        if (aCompound && bCompound) {
//            return Subterms.commonSubterms((Compound) a, ((Compound) b), false);
//        } else {
//            if (aCompound && !bCompound) {
//                return a.contains(b);
//            } else if (bCompound && !aCompound) {
//                return b.contains(a);
//            } else {
//
//                return a.equals(b);
//            }
//        }
//
//    }

    //    @Disabled
//    @Test
//    void testCommonSubterms() throws Narsese.NarseseException {
//        assertTrue(commonSubtermOrContainment($("x"), $("x")));
//        assertFalse(commonSubtermOrContainment($("x"), $("y")));
//        assertTrue(commonSubtermOrContainment($("(x,y,z)"), $("y")));
//        assertFalse(commonSubtermOrContainment($("(x,y,z)"), $("w")));
//        assertFalse(Subterms.commonSubterms($("(a,b,c)"), $("(x,y,z)"), false));
//        assertTrue(Subterms.commonSubterms($("(x,y)"), $("(x,y,z)"), false));
//    }
//
//    @Disabled @Test
//    void testCommonSubtermsRecursion() throws Narsese.NarseseException {
//        assertTrue(Subterms.commonSubterms($("(x,y)"), $("{a,x}"), false));
//        assertFalse(Subterms.commonSubterms($("(x,y)"), $("{a,b}"), false));
//
//        assertFalse(Subterms.commonSubterms($("(#x,y)"), $("{a,#x}"), true));
//        assertTrue(Subterms.commonSubterms($("(#x,a)"), $("{a,$y}"), true));
//    }




}