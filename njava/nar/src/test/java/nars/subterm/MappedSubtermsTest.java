package nars.subterm;

import nars.$;
import nars.Term;
import nars.term.atom.Atom;
import nars.term.builder.SmartTermBuilder;
import nars.term.util.Testing;
import nars.term.var.CommonVariable;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.$.$$$;
import static org.junit.jupiter.api.Assertions.*;

class MappedSubtermsTest {

    private static final Term x = $$("x");
    private static final Term y = $$("y");
    private static final Term z = $$("z");

    private static final Term X = $.p("x");

    @Test void TwoAry() {
        assertEq($$("x"), $$("(y --> (z,/))"));
    }

    @Test
    void test3ary_0() {
        var direct = assertUnpermute(new Term[]{x, y, z}, new Term[]{x, y, z});
        assertFalse(direct instanceof RemappedPNSubterms);
    }

    @Test
    void test3ary_1() {
        var remapped2 = assertUnpermute(new Term[]{x, x, z}, new Term[]{x, x, z}); //repeats
        assertFalse(remapped2 instanceof RemappedPNSubterms);
    }
    @Test
    void test3ary_2() {
        var remapped0 = assertUnpermute(new Term[]{y, x, z}, new Term[]{y, x, z}); //non-canonical, mapped order
        assertInstanceOf(IntrinSubterms.class, remapped0); /* ?? */

        var remapped = assertUnpermute(new Term[]{x, $.p(y, y), z}, new Term[]{x, $.p(y, y), z}); //non-canonical, mapped order
        assertInstanceOf(RemappedPNSubterms.class, remapped);

    }
    @Test
    void test3ary_3() {
        var remapped3 = assertUnpermute(new Term[]{x, z, x}, new Term[]{x, z, x}); //repeats, unordered
        assertInstanceOf(IntrinSubterms.class, remapped3);
    }

    @Test
    void test3aryNeg() {
        var remapped3Neg = assertUnpermute(
                new Term[]{x.neg(), z, X}, new Term[]{x.neg(), z, X}); //repeats, unordered
        assertInstanceOf(RemappedPNSubterms.class, remapped3Neg);
    }

    static Subterms assertEq(Term... x) {
        return assertUnpermute(x, x);
    }

    /** subterms as array/vector */
    static Subterms assertUnpermute(Term[] aa, Term[] bb) {
        var a = new ArraySubterms(aa);
        var b = new SmartTermBuilder(true).subterms(bb);
        Testing.assertEq(a, b);
        return b;
    }

    @Test void RepeatedSubterms() {
        {

            var abc = Atom.atomic("abc");
            Subterms s = new RepeatedSubterms(abc, 4);//$$("(abc,abc,abc,abc)");
            assertSame(RepeatedSubterms.class, s.getClass());
            assertEquals(4, s.subs());
            assertEquals(5, s.complexity());
            assertEquals("(abc,abc,abc,abc)", s.toString());
        }
        {
            Subterms s = new RepeatedSubterms($$$("#a"), 3);
            assertSame(RepeatedSubterms.class, s.getClass());
            assertEquals(3, s.subs());
            assertEquals(3, s.vars());
            assertEquals(3, s.varDep());
            assertTrue(s.hasVars());
            assertTrue(s.hasVarDep());
            assertEquals(1 + 3, s.complexity());
            assertEquals(1, s.complexityConstants());
            assertEquals("(#a,#a,#a)", s.toString());
        }
    }
    @Test void BiSubtermWeird() {
        var allegedTarget = $$$("( &&+- ,(--,##2#4),_1,##2#4,$1)");
        Subterms base = new BiSubterm(allegedTarget, $$$("$1"));
        assertEquals(
                "(( &&+- ,(--,##2#4),_1,##2#4,$1),$1)"
                //"(( &&+- ,(--,##2#4),_1,$1,##2#4),$1)"
                , base.toString());

        var target = $$$("( &&+- ,(--,##2#4),_1,##2#4,$1)");
        assertEquals(0, base.indexOf(target));

        /*
        ( &&+- ,(--,($1-->_1)),(--,##2#5),##2#5) (&&[TemporalCachedCompound] c5,v8,dt=2147483647,dtRange=0 110000010000111)
          (--,($1-->_1)) (--[Neg] c3,v4,dt=-2147483648,dtRange=0 100000000000111)
            ($1-->_1) (-->[SimpleCachedCompound] c2,v3,dt=-2147483648,dtRange=0 100000000000101)
              $1 ($[VarIndep] c0,v1,dt=-2147483648,dtRange=0 100000000000000)
              _1 (.[Anom] c1,v1,dt=-2147483648,dtRange=0 1)
          (--,##2#5) (--[Neg] c1,v2,dt=-2147483648,dtRange=0 10000000000010)
            ##2#5 (#[CommonVariable] c0,v1,dt=-2147483648,dtRange=0 10000000000000)
          ##2#5 (#[CommonVariable] c0,v1,dt=-2147483648,dtRange=0 10000000000000)

        ( &&+- ,(--,($1-->_1)),(--,##2#5),##2#5) (&&[TemporalCachedCompound] c5,v8,dt=2147483647,dtRange=0 110000010000111)
          (--,($1-->_1)) (--[Neg] c3,v4,dt=-2147483648,dtRange=0 100000000000111)
            ($1-->_1) (-->[SimpleCachedCompound] c2,v3,dt=-2147483648,dtRange=0 100000000000101)
              $1 ($[VarIndep] c0,v1,dt=-2147483648,dtRange=0 100000000000000)
              _1 (.[Anom] c1,v1,dt=-2147483648,dtRange=0 1)
          (--,##2#5) (--[Neg] c1,v2,dt=-2147483648,dtRange=0 10000000000010)
            ##2#5 (#[CommonVariable] c0,v1,dt=-2147483648,dtRange=0 10000000000000)
          ##2#5 (#[CommonVariable] c0,v1,dt=-2147483648,dtRange=0 10000000000000)
     */
        assertInstanceOf(CommonVariable.class, $$$("##2#5"));
        assertEquals($$$("##2#5"), $$$("##2#5"));
        assertEquals($$$("##2#5").neg(), $$$("##2#5").neg());
        assertEquals($$$("--##2#5").unneg(), $$$("--##2#5").unneg());
    }
}