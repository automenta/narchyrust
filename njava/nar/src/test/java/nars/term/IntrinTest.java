package nars.term;

import jcog.TODO;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.util.ArrayUtil;
import nars.$;
import nars.Narsese;
import nars.Term;
import nars.subterm.*;
import nars.term.anon.Anon;
import nars.term.anon.Intrin;
import nars.term.atom.Anom;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.builder.MultiInterningTermBuilder;
import nars.term.builder.SmartTermBuilder;
import nars.term.builder.TermBuilder;
import nars.term.compound.LightCompound;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.RoaringBitmap;

import java.util.Random;

import static nars.$.*;
import static nars.Op.*;
import static nars.Op.terms;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.*;

class IntrinTest {

    static {
        if (!(terms_ instanceof TermBuilder))
            throw new TODO(terms_ + " not compatible with testing config");
    }

    private static final TermBuilder B = SmartTermBuilder.the;
    private static final Atomic a = Atom.atomic("a");
    private static final Atomic b = Atom.atomic("b");
    private static final Atomic c = Atom.atomic("c");

    private static Anon assertAnon(String expect, String test) {
        return assertAnon(expect, $$(test));
    }

    /**
     * roundtrip test
     */
    private static Anon assertAnon(String expect, Term x) {
        var a = new Anon();
        var y = a.put(x);
        var z = a.get(y);
        assertEquals(expect, y.toString());
        assertEquals(x, z);
        return a;
    }

    private static void testAnonTermVectorProducedByTermBuilder(TermBuilder b) {
        {
            var tri = b.subterms(varDep(1), Anom.anom(2), Anom.anom(1));
            assertSame(IntrinSubterms.class, tri.getClass());
        }

        {
            var triSub = b.compound(PROD, varDep(1), Anom.anom(2), Anom.anom(1));
            assertSame(IntrinSubterms.class, triSub.subterms().getClass());
        }

        {
            var bi = b.subterms(varDep(1), Anom.anom(2));
            assertSame(IntrinSubterms.class, bi.getClass());
        }

        {
            var biSub = b.compound(PROD, varDep(1), Anom.anom(2));
            assertSame(IntrinSubterms.class, biSub.subterms().getClass());
        }

        {
            var uni = b.subterms(varDep(1));
            assertSame(IntrinSubterms.class, uni.getClass());
        }
//        {
//            Term uniSub = b.compound(PROD, $.varDep(1));
//            assertEquals(IntrinSubterms.class, uniSub.subterms().getClass());
//        }
    }

    @Test
    void testAtoms() {
        assertAnon("_1", "abc");
        assertAnon("#1", varDep(1));

        assertNotEquals(Anom.anom(0), $.the(0));
        assertNotEquals(Anom.anom(2), $.the(2));
    }


    @Test
    void testCompounds() {
        assertAnon("(_1-->_2)", "(abc-->bcd)");



        assertAnon("{_1}", "{abc}");
        assertAnon("{_1,_2}", "{abc,b1}");
        assertAnon("{_1,_2,_3}", "{abc,b1,c1}");
        assertAnon("(_1 ==>+- _2)", "(x1 ==>+- y1)");
        {
            var a = assertAnon("((_1&&_2) ==>+- _3)", "((x1&&y1) ==>+- z1)");
            assertEquals("(x1&&y1)", a.get(CONJ.the(Anom.anom(1), Anom.anom(2))).toString());
        }


        assertAnon("(((_1-->(_2,_3,#1))==>(_4,_5)),?2)",
                "(((a1-->(b1,c1,#2))==>(e1,f1)),?1)");

    }

    @Test void NormalizedVariables() {
        assertAnon("(_1-->#1)", "(abc-->#1)");
    }
    @Test void Integers() {
        //noinspection PointlessBitwiseExpression
        assertEquals((Intrin.INT_POSs<<8) | 0, Intrin.id(Int.i(0)));
        assertEquals((Intrin.INT_POSs<<8) | 1, Intrin.id(Int.i(1)));
        assertEquals((Intrin.INT_POSs<<8) | 126, Intrin.id(Int.i(126)));
        assertEquals((Intrin.INT_POSs<<8) | 127, Intrin.id(Int.i(127)));
        assertEquals(0, Intrin.id(Int.i(128)));
        assertEquals((Intrin.INT_NEGs<<8) | 1, Intrin.id(Int.NEG_ONE));
        assertEquals((Intrin.INT_NEGs<<8) | 126, Intrin.id(Int.i(-126)));
        assertEquals((Intrin.INT_NEGs<<8) | 127, Intrin.id(Int.i(-127)));

        assertAnon("(_1-->1)", "(abc-->1)");
        assertAnon("(_1-->0)", "(abc-->0)");
        assertAnon("(_1-->-1)", "(abc-->-1)");

        assertAnon("(--,0)", "(--,0)");
        assertAnon("(--,-1)", "(--,-1)");
        assertAnon("(--,1)", "(--,1)");

        assertInstanceOf(Neg.NegIntrin.class, $$("(--,1)"));
        assertInstanceOf(IntrinSubterms.class, $$("(a,2,3)").subterms());
        assertInstanceOf(IntrinSubterms.class, $$("(a,(--,-2),3)").subterms());
        assertInstanceOf(IntrinSubterms.class, $$("((--,1),-2,a)").subterms());
    }
    @Test void Chars() {
        assertEquals((Intrin.CHARs << 8) | 'a', Intrin.id(Atomic.atomic('a')));
        assertEquals((Intrin.CHARs << 8) | 'A', Intrin.id(Atomic.atomic('A')));
        assertEquals((Intrin.CHARs << 8) | 'z', Intrin.id(Atomic.atomic('z')));

        assertAnon("(a-->1)", "(a-->1)");
        assertAnon("(a-->0)", "(a-->0)");
        assertAnon("(a-->-1)", "(a-->-1)");

        assertAnon("(--,a)", "(--,a)");
        assertAnon("(--,b)", "(--,b)");
        assertAnon("(--,c)", "(--,c)");

        assertInstanceOf(Neg.NegIntrin.class, $$("(--,a)"));
    }



    @Test void Chars_Subterms() {
        assertInstanceOf(IntrinSubterms.class, PROD.build(B, a, b, c).subterms());
    }

    @Test void Chars_Subterms_with_neg() {
        var p = PROD.build(B, a.neg(), b, c);
        assertInstanceOf(IntrinSubterms.class, p.subterms());
    }

    @Test
    void testCompoundsWithNegations() {
        assertAnon("((--,_1),_1,_2)", "((--,a1), a1, c1)");
        assertAnon("(--,((--,_1),_1,_2))", "--((--,a1), a1, c1)");
    }

    @Test
    @Disabled
    void testIntRange() throws Narsese.NarseseException {
        assertEquals("(4..6-->x)", $("((|,4,5,6)-->x)").toString());
        assertAnon("(_0-->_1)", "((|,4,5,6)-->x)");
    }

    @Test
    void testAnomVector() {

        Term[] x = {Anom.anom(3), Anom.anom(1), Anom.anom(2)};

        assertEq(new UnitSubterm(x[0]), new IntrinSubterms(x[0]));
        assertEq(new UnitSubterm(x[0]), new TermList(x[0]));

        assertEq(new BiSubterm(x[0], x[1]), new IntrinSubterms(x[0], x[1]));
        assertEq(new BiSubterm(x[0], x[1]), new TermList(x[0], x[1]));

        assertEq(new ArraySubterms(x), new IntrinSubterms(x));
        assertEq(new ArraySubterms(x), new TermList(x));

    }

    private static final Term[] x = {Anom.anom(3), Anom.anom(1), Anom.anom(2).neg()};

    @Test
    void testAnomVectorNegations() {


        var av = new IntrinSubterms(x);
        var bv = new ArraySubterms(x);
        assertEq(bv, av);

        assertFalse(av.contains(x[0].neg()));
        assertFalse(av.containsRecursively(x[0].neg()));

        assertFalse(av.contains($$("x")));
        assertFalse(av.containsRecursively($$("x")));
        assertFalse(av.contains($$("x").neg()));
        assertFalse(av.containsRecursively($$("x").neg()));

        var twoNeg = x[2];
        assertTrue(av.contains(twoNeg));
        assertTrue(av.containsRecursively(twoNeg));
        assertTrue(av.containsRecursively(twoNeg.neg()), () -> av + " containsRecursively " + twoNeg.neg());


    }

    @Test
    void testMixedAnonVector() {

        Term[] x = {varDep(1), varIndep(2), varQuery(3), Anom.anom(4)};
        Random rng = new XoRoShiRo128PlusRandom(1);
        for (var i = 0; i < 4; i++) {

            ArrayUtil.shuffle(x, rng);

            assertEq(new UnitSubterm(x[0]), new IntrinSubterms(x[0]));
            assertEq(new BiSubterm(x[0], x[1]), new IntrinSubterms(x[0], x[1]));
            assertEq(new ArraySubterms(x), new IntrinSubterms(x));
        }
    }

//    @Test public void testAnonSortingOfRepeats() {
//        assertAnon("(_1,_1,_2)", "(1,1,2)");
//        assertAnon("(_2,_1,_1)", "(1,2,2)");
//    }

    @Test
    void testAnonSorting() {
        assertAnon("(&&,(--,_1),_2,_3,_4,_5)", "(&&,x1,x2,--x3,x4,x5)");
        assertAnon("(&&,(--,_1),_2,_3,_4,_5)", "(&&,--x1,x2,x3,x4,x5)");
        assertAnon("(_2(_1)&&_3)", "(&&,b1(a1),x3)");
        assertAnon("(_2(_1)&&_3)", "(&&,b1(a1),x1)");
        assertAnon("((_2(_1)&&_3) &&+- _4)", "((&&,x3(a0),x1) &&+- x4)");
        assertAnon("((_2(_1)&&_3) &&+- _4)", "(x1 &&+- (&&,b1(a2),x4))");
    }

    @Test
    void testTermSubs1() {
        var x = (Compound) $$c("(%1,%2)").normalize();
        assertSame(IntrinSubterms.class, x.subterms().getClass());
        for (var t : new Subterms[]{x, x.subterms()}) {
            assertEquals(2, t.count(VAR_PATTERN));
            assertEquals(0, t.count(VAR_DEP));
        }
    }
    @Test
    void testTermSubs2() {
        var y = (Compound) $$c("(%1,%2,(--,$3))").normalize();
        assertSame(IntrinSubterms.class, y.subterms().getClass());
        for (var t: new Subterms[]{y, y.subterms()}) {
            assertEquals(2, t.count(VAR_PATTERN));
            assertEquals(2, t.count(VAR_PATTERN));
            assertEquals(0, t.count(VAR_INDEP));
            assertEquals(1, t.count(NEG));
        }
    }

    @Test
    void testAutoNormalization() throws Narsese.NarseseException {
//        final MegaHeapTermBuilder h = MegaHeapTermBuilder.the;
        for (var s: new String[]{"($1)", "($1,$2)", "($1,#2)", "(%1,%1,%2)"}) {
            var t = $$c(s);
            assertEquals(s, t.toString());
            assertTrue(
                    UnitSubterm.class == t.subterms().getClass() ||
                            IntrinSubterms.class == t.subterms().getClass());
            assertTrue(t.NORMALIZED(), () -> t + " not auto-normalized but it could be");
        }
        for (var s: new String[]{"($2)", "($2,$1)", "($1,#3)", "(%1,%3,%2)"}) {
            var t = (Compound) Narsese.term(s, false);
            assertEquals(s, t.toString());
            assertTrue(
                    UnitSubterm.class == t.subterms().getClass() ||
                            IntrinSubterms.class == t.subterms().getClass(),
                    () -> t.getClass().toString() + ' ' + t.subterms().getClass());
            assertFalse(t.NORMALIZED(), () -> t + " auto-normalized but should not be");
        }
    }

    @Test
    void testAnonTermVectorProducedByHeapTermBuilder() {
        testAnonTermVectorProducedByTermBuilder(terms_);
    }

    @Test
    void testAnonTermVectorProducedByInterningTermBuilder() {
        testAnonTermVectorProducedByTermBuilder(
            new MultiInterningTermBuilder(SmartTermBuilder.the)
        );
    }

    @Test
    void testAnonVectorTransform() {
        //TODO
    }

    @Test
    void testAnonVectorReplace() {
        var xx = (IntrinSubterms)
            B.subterms(varDep(1), Anom.anom(2).neg(), Anom.anom(1));

        Term x = LightCompound.the(PROD, xx);
        
        {
            var yAnon = x.replace(varDep(1), Anom.anom(3)).subterms();
            assertEquals("(_3,(--,_2),_1)", yAnon.toString());
//            assertSame(x.subterms().getClass(), yAnon.getClass(), "should remain AnonVector, not something else");

            var yNotFound = x.replace(varDep(4), Anom.anom(3)).subterms();
            assertSame(x.subterms(), yNotFound);
        }

        {
            var yAnon = x.replace(Anom.anom(2).neg(), Anom.anom(3)).subterms();
            assertEquals("(#1,_3,_1)", yAnon.toString());
//            assertSame(x.subterms().getClass(), yAnon.getClass(), "should remain AnonVector, not something else");

            var yNotFound = x.replace(Anom.anom(1).neg(), Anom.anom(3)).subterms();
            assertSame(x.subterms(), yNotFound);
        }


        {
            var yAnon = x.replace(Anom.anom(2), Anom.anom(3)).subterms();
            assertEquals("(#1,(--,_3),_1)", yAnon.toString());
//            assertSame(x.subterms().getClass(), yAnon.getClass(), "should remain AnonVector, not something else");
        }
        {
            var yAnon = x.replace(Anom.anom(2), Anom.anom(3).neg()).subterms();
            assertEquals("(#1,_3,_1)", yAnon.toString());
//            assertSame(x.subterms().getClass(), yAnon.getClass(), "should remain AnonVector, not something else");
        }

        {
            var yNonAnon = x.replace(varDep(1), PROD.the(atomic("X"))).subterms();
            assertEquals("((X),(--,_2),_1)", yNonAnon.toString());
            assertNotEquals(x.subterms().getClass(), yNonAnon.getClass());

            var yNotFound = x.replace(PROD.the(atomic("X")), PROD.the(atomic("Y"))).subterms();
            assertSame(x.subterms(), yNotFound);

        }

        {
            Term xxx = LightCompound.the(PROD, terms.subterms(varDep(1), Anom.anom(2).neg(), Anom.anom(2)));
            assertEquals("(#1,(--,_3),_3)", xxx.replace(Anom.anom(2), Anom.anom(3)).toString());
            assertEquals("(#1,_3,_2)", xxx.replace(Anom.anom(2).neg(), Anom.anom(3)).toString());
        }


        {
            Term xxx = LightCompound.the(PROD, terms.subterms(varDep(1), Anom.anom(2).neg(), Anom.anom(2)));
            assertEquals("(#1,(--,()),())", xxx.replace(Anom.anom(2), EmptyProduct).toString());
            assertEquals("(#1,(),_2)", xxx.replace(Anom.anom(2).neg(), EmptyProduct).toString());
        }

    }

//    @Test void ShiftWithIndexGap() {
//        //in ths example,
//        // because there is no #1 variable,
//        // the shift must replace x's #2 with #3 (not #2) which would collapse against itself
//        Term x = $$("(Level(low) ==>+1 ((--,At(#1))&&At(#2)))");
//        Term b = $$("(_1($1) ==>+1 ((--,_1($1))&&_1(#2)))");
//        Term y = new AnonWithVarShift(16, Op.VAR_DEP.bit | Op.VAR_QUERY.bit).putShift(x, b, null);
//        assertEquals("(_2(_1) ==>+1 ((--,_3(#3))&&_3(#4)))", y.toString());
//    }

    @Test void ConjSeq() {
        //0:((--,(tetris-->rotate))&&#_f),690:((--,(tetris-->right))&&(--,(tetris-->rotate))),800:(tetris-->left),3520:left(#1,#2)
        var t = "((((--,x)&&#_f) &&+690 ((--,x)&&(--,y))) &&+800 (z &&+3520 w))";
        var T = $$(t);
        //assertEq(t, T);
//        assertEquals(T.volume(), T.anon().volume(), ()->"difference:\n" + T + "\n" + T.anon());
    }

    @Test void testPosInts_gt127() {
        var b = new RoaringBitmap();
        b.add(134); b.add(135); b.add(136);
        assertEq("x({{134,135,136}})", func("x", SETe.the(sete(b)))); //beyond 127 intern limit

        assertEq("x({{134,135,136}})", func("x", SETe.the(seteShort(b))));
    }

    @Test void testNegInts_lt_minus127() {
        var b = new RoaringBitmap();
        b.add(-134); b.add(-135); b.add(-136);
        assertEq("x({{-136,-135,-134}})", func("x", SETe.the(sete(b)))); //beyond 127 intern limit

        assertEq("x({{-136,-135,-134}})", func("x", SETe.the(seteShort(b))));
    }

}