package nars.term;

import jcog.random.RandomBits;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.Narsese;
import nars.Term;
import nars.term.atom.Int;
import nars.term.compound.LightCompound;
import nars.term.util.TermTransformException;
import org.eclipse.collections.api.tuple.Twin;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;
import java.util.random.RandomGenerator;

import static nars.$.$$;
import static nars.$.$$c;
import static nars.Op.INH;
import static nars.term.atom.Bool.Null;
import static nars.term.util.Image.*;
import static nars.term.util.Testing.assertEq;
import static org.eclipse.collections.impl.tuple.Tuples.twin;
import static org.junit.jupiter.api.Assertions.*;

/** the NAL "Image" operator, not bitmaps or photos */
class ImageTest {

    @Test
    void PreNormalize() {
        Compound t = $$c("(acid --> (reaction,/,base))");
//        assertSame(CachedCompound.SimpleCachedCompound.class, t.getClass());
        t.NORMALIZED();
    }

    @Test
    void CompoundSubtermsNormalized() {
        assertEq("(x,reaction(acid,base))", "(x,(acid --> (reaction,/,base)))");
        assertEq("(x,reaction(#1,#2))", "(x,(#1 --> (reaction,/,#2)))");
        assertEq("(reaction(#1,#2)&&x)", "(x && (#1 --> (reaction,/,#2)))");
        assertEq("(x &&+1 reaction(#1,#2))", "(x &&+1 (#1 --> (reaction,/,#2)))");
        assertEq("((x &&+1 y) &&+1 reaction(#1,#2))", "(x &&+1 (y &&+1 (#1 --> (reaction,/,#2))))");
        assertEq("((x &&+1 (y||(--,x))) &&+1 reaction(#1,#2))", "(x &&+1 ((y||--x) &&+1 (#1 --> (reaction,/,#2))))");
    }
    @Test
    void CompoundSubtermsNegNormalized() {
        assertEq("(x,reaction((--,acid),base))", "(x,(acid --> (reaction,(--,/),base)))");
    }

    @Test
    void NormlizeExt() {
        assertEq(
                "reaction(acid,base)",
                imageNormalize($$("(acid --> (reaction,/,base))"))
        );
        assertEq(
                "reaction(acid,base)",
                imageNormalize($$("(base --> (reaction,acid,/))"))
        );

        assertEq(
                "reaction(acid)",
                imageNormalize($$("(acid --> (reaction,/))"))
        );


    }


    @Test
    void NormlizeInt() {

        assertEq(
                "(neutralization-->(acid,base))",
                imageNormalize($$("((neutralization,\\,base) --> acid)"))
        );
        assertEq(
                "(neutralization-->(acid,base))",
                imageNormalize($$("((neutralization,acid,\\) --> base)"))
        );
        assertEq(
                "(neutralization-->(acid))",
                imageNormalize($$("((neutralization,\\) --> acid)"))
        );

    }

    @Test
    void CanNotNormlizeIntExt() {
        assertEq(
                "((neutralization,\\,base)-->(reaction,/,base))",
                imageNormalize($$("((neutralization,\\,base) --> (reaction,/,base))"))
        );
    }

    @Test
    void NormalizeSubtermsSIM() {

        assertEq(
                "(reaction(acid,base)<->x)",
                $$("(x <-> (acid --> (reaction,/,base)))")
        );
        assertEq(
                "(reaction(acid)<->x)",
                $$("(x <-> (acid --> (reaction,/)))")
        );
    }

    @Test
    void ImageIntWithNumbers() {
        Term xx = $$("(bitmap-->(0,1,0))");
        assertEq("((bitmap,\\,1,\\)-->0)", imageInt(xx, Int.i(0)));
        assertEq("((bitmap,0,\\,0)-->1)", imageInt(xx, Int.i(1)));

        assertEq(xx, imageNormalize(imageInt(xx, Int.i(1))));
        assertEq(xx, imageNormalize(imageInt(xx, Int.i(0))));
    }

    @Test
    void ImageExtWithNumbers() {
        Term xx = $$("((0,1,0)-->bitmap)");
        assertEq("(0-->(bitmap,/,1,/))", imageExt(xx, Int.i(0)));
        assertEq("(1-->(bitmap,0,/,0))", imageExt(xx, Int.i(1)));

        assertEq(xx, imageNormalize(imageExt(xx, Int.i(1))));
        assertEq(xx, imageNormalize(imageExt(xx, Int.i(0))));
    }

    @Test
    void ImagizeDepVar1() {
        Term x = $$("reaction(acid,#1)");
        Term y = imageExt(x, $.varDep(1));
        assertEquals("(#1-->(reaction,acid,/))", y.toString());
        assertEquals(x, imageNormalize(y));
    }

    @Test
    void ImagizeDepVar2() {
        Term x = $$("reaction(#1,#2)");
        Term y1 = imageExt(x, $.varDep(1));
        Term y2 = imageExt(x, $.varDep(2));
        assertEq("(#1-->(reaction,/,#2))", y1);
        assertEq("(#2-->(reaction,#1,/))", y2);
        assertEquals(x, imageNormalize(y1));
        assertEquals(x, imageNormalize(y2));
    }

    @Test
    void neg_imageExt() {
        Term x = $$("a((--,b),c)");
        assertEq("(b-->(a,(--,/),c))", imageExt(x, $$("b")));
        assertEq("(b-->(a,(--,/),c))", imageExt(x, $$("--b"))); //same
    }
    @Test
    void neg_imageInt() {
        Term x = $$("(a --> ((--,b),c))");
        assertEq("b(a,(--,\\),c)", imageInt(x, $$("b")));
    }

    @Test
    void neg_mix_imageExt() {
        Term x = $$("a((--,b),c,b)");
        assertEq("(b-->(a,(--,/),c,/))", imageExt(x, $$("b")));
        assertEq("(b-->(a,(--,/),c,/))", imageExt(x, $$("--b"))); //same
    }

    @Test
    void neg_imageExt_normalize() {
        assertEq("a((--,b),c)",
                imageNormalize($$("(b-->(a,(--,/),c))")));
    }

    @Test
    void OneArgFunctionAsImage() {
        assertEq("(y-->(x,/))", $.funcImg($.atomic("x"), $.atomic("y")));
    }

    @Test
    void OneArgFunctionAsImage_then_normalized() {
        Term y = $.funcImg($.atomic("x"), $.atomic("y"));
        assertEq("(y-->(x,/))", y);
        assertThrows(TermTransformException.class, () -> imageExt(y, $$("/")));

        assertEq(Null, imageExt(y, $$("x")));
        assertEq(Null, imageExt(y, $$("(x,/)")));
        assertEq(Null, imageExt(y, $$("y")));

        assertEq(Null, imageInt(y, $$("(x,/)")));
        assertEq(Null, imageInt(y, $$("y")));
        assertEq(Null, imageInt(y, $$("x"))); //<- != x(y,\,/)

    }

    @Test
    void RecursiveUnwrapping() {
        //assertEquals(
        //"reaction(acid,base)",
        Term a1 = $$("(((chemical,reaction),base)-->acid)");

        Term a2Bad = imageExt(a1, $$("reaction"));
        assertEquals(Null, a2Bad); //not in the next reachable level

        Term a2 = imageExt(a1, $$("(chemical,reaction)"));
        assertEquals("((chemical,reaction)-->(acid,/,base))", a2.toString());

        Term a3Bad = imageInt(a2, $$("reaction"));
        assertEquals(Null, a3Bad);

        Term a3 = imageExt(a2, $$("reaction"));
        assertEquals("(reaction-->((acid,/,base),chemical,/))", a3.toString());

        //reverse

        Term b2 = imageNormalize(a3);
        assertEquals(a1, b2);

//        Term b1 = Image.imageNormalize(b2);
//        assertEquals(a1, b1);

    }

    @Test
    void NonRepeatableImage() {
        assertEq("(c-->(a,b,/,/))", imageExt($$("a(b,/,c)"), $$("c"))); //TODO test
    }

    @Test
    void RepeatableImageInSubterm() {
        assertEq("(b-->(a,/,(c,/)))", imageExt($$("a(b,(c,/))"), $$("b")));
        assertEq("((c,/)-->(a,b,/))", imageExt($$("a(b,(c,/))"), $$("(c,/)")));
    }

    @Test
    void NormalizeVsImageNormalize() {
        Compound x = $$c("(acid-->(reaction,/))");
        assertTrue(x.NORMALIZED());
        assertEquals(
                "(acid-->(reaction,/))",
                x.normalize().toString()
        );
        assertNotEquals(x.normalize(), imageNormalize(x));
    }

    @Test
    void NormalizeVsImageNormalize_impl() throws Narsese.NarseseException {
        Term x = Narsese.term("((--,(4-->(ang,fz,/))) ==>-7600 ((race-->fz),((8,5)-->(cam,fz))))", false);
//        assertFalse(x.isNormalized());

        String y = "((--,ang(fz,4)) ==>-7600 ((race-->fz),((8,5)-->(cam,fz))))";
        assertTrue(((Compound) Narsese.term(y, false)).NORMALIZED());

        Term xx = x.normalize();

        assertEquals(y, xx.toString());

        assertEquals($$(y), xx);
    }


    @Test
    void NormalizeVsImageNormalize_prod() {
        assertEq("((--,ang(fz,4)),x)", "((--,(4-->(ang,fz,/))),x)");
        assertEq("((--,ang(fz,4)),x,y)", "((--,(4-->(ang,fz,/))),x,y)");
    }

    @Test
    void NormalizeVsImageNormalize_sim() {
        assertEq("(--,(ang(fz,4)<->x))", "((--,(4-->(ang,fz,/))) <-> x)");
    }

    @Test
    void NormalizeVsImageNormalize_conj() {
        assertEq("((--,ang(fz,4))&&x)", "((--,(4-->(ang,fz,/))) && x)");
        assertEq("(&&,(--,ang(fz,4)),x,y)", "(&&,(--,(4-->(ang,fz,/))),x,y)");
    }

    @Test
    void NormalizeVsImageNormalize_prod2() {
        assertEq("((--,ang(fz,4)),(a,b,c,d,e,f,g,h,i,j),(a,b,c,d,e,f,g,h,i,j))",
                "((--,(4-->(ang,fz,/))),(a,b,c,d,e,f,g,h,i,j),(a,b,c,d,e,f,g,h,i,j))");

        assertEq("((--,ang(fz,4)),((race-->fz),((8,5)-->(cam,fz))))",
                "((--,(4-->(ang,fz,/))),((race-->fz),((8,5)-->(cam,fz))))");
    }

    @Test
    void ImageExt() {
        assertEq("(TRUE-->((isRow,tetris,/),14,/))",
                imageExt($$("((14,TRUE)-->(isRow,tetris,/))"), $$("TRUE")));
    }
    @Test
    void ImageExtNeg() {
        Term x = $$("((14,(--,TRUE))-->(isRow,tetris,/))");
        String y = "(TRUE-->((isRow,tetris,/),14,(--,/)))";
        assertEq(y, imageExt(x, $$("TRUE")));
        assertEq(y, imageExt(x, $$("--TRUE")));
    }

    @Test
    void ConjNegatedNormalizeWTF() {
        assertEq("((--,(delta-->vel)) &&+280 (--,vel(fz,y)))", "((--,(delta-->vel)) &&+280 (--,(y-->(vel,fz,/))))");
    }

    @Test
    void ConjNegatedNormalizeWTF2() {
        assertEq("(((--,v(fz,x)) &&+2 (--,v(fz,y))) &&+1 z)",
                "(((--,(x-->(v,fz,/))) &&+2 (--,(y-->(v,fz,/)))) &&+1 z)");
    }

    @Test
    void ConjNegatedNormalizeWTF3() {
        assertEq("((((--,v(fz,x))&&(--,v(fz,y))) &&+2 (--,v(fz,y))) &&+1 z)",
                "(((&&,(--,(x-->(v,fz,/))),(--,(y-->(v,fz,/)))) &&+2 (--,(y-->(v,fz,/)))) &&+1 z)");
    }

    @Test
    void ConjNegatedNormalizeWTF4() {
        assertEq("(((&&,(--,v(fz,x)),(--,v(fz,y)),w) &&+2 (--,v(fz,y))) &&+1 z)",
                "(((&&,(--,(x-->(v,fz,/))),(--,(y-->(v,fz,/))),w) &&+2 (--,(y-->(v,fz,/)))) &&+1 z)");
    }

    @Test
    void ImageProductNormalized() throws Narsese.NarseseException {
        Compound x = (Compound) Narsese.term("(y,\\,x)", false);
        assertTrue(x.NORMALIZED());
    }

    @Test
    void ImageRecursionFilter() {


        Term x0 = $$("(_ANIMAL-->((cat,ANIMAL),cat,/))");
        assertEq("((cat,_ANIMAL)-->(cat,ANIMAL))", imageNormalize(x0)); //to see what would happen
    }

    @Test
    void ImageRecursionFilter2() {

        assertEq(Null, imageNormalize(LightCompound.the(INH, $$("ANIMAL"), $$("((cat,ANIMAL),cat,/)"))));

        assertEq(Null /* True */, "(ANIMAL-->((cat,ANIMAL),cat,/))");
    }

    @Test
    void imageAlignOpposite() {
        assertAligned("abc(exe,xex)", "(a-->(exe,xex))", "[(exe-->(abc,/,xex)):exe(a,\\,xex), (xex-->(abc,exe,/)):xex(a,exe,\\)]");
    }

    @Test
    void imageAlignSame() {
        assertAligned("abc(exe,xex)", "wtf(exe,xex))",
            "(exe-->(abc,/,xex)):(exe-->(wtf,/,xex))",
            "(xex-->(abc,exe,/)):(xex-->(wtf,exe,/))"
        );
    }

    @Test
    void imageAlignSame_neg() {
        assertAligned(
            "abc(exe,xex)", "wtf(--exe,xex))",
        "(exe-->(abc,/,xex)):(exe-->(wtf,(--,/),xex))",
        "(xex-->(abc,exe,/)):(xex-->(wtf,(--,exe),/))");
    }

    /** TODO use Joiner to assemble N results */
    @Deprecated private static void assertAligned(String a, String b, String ab1, String ab2) {
        assertAligned(a, b, "[" + ab1 + ", " + ab2 + "]");
    }

    @Test
    void imageAlignSemi() {
        assertAligned("(neutralization-->(acid,base))", "(sulfuric_acid-->acid)", "[acid(neutralization,\\,base):(sulfuric_acid-->acid)]"
        );
    }

    @Test
    void imageAlignSemi2() {
        assertEq("y((b,c),\\,a)", imageInt($$("((b,c)-->(y,a))"), $$("y")));
        assertAligned("(x-->y)", "((b,c)-->(y,a))", "[(x-->y):y((b,c),\\,a)]"
        );
    }

    @Test
    void imageAlignSemiPosNeg() {
        assertAligned("(neutralization-->(--acid,base))", "(sulfuric_acid-->acid)", "[acid(neutralization,(--,\\),base):(sulfuric_acid-->acid)]"
        );
    }

    private static void assertAligned(String a, String b, String ab) {
        Compound x = $$c(a);
        Compound y = $$c(b);
        RandomGenerator rng = new RandomBits(new XoRoShiRo128PlusRandom(1));
        Set<Twin<Term>> s = new TreeSet();
        int n = x.complexity() * y.complexity();
        for (int i = 0; i < n; i++) {
            Term[] xy = align(x, y, rng, false);
            if (xy != null)
                s.add(twin(xy[0], xy[1]));
        }
        assertEquals(ab, s.toString());
    }

    @Test
    void imgMiscTest() {
        assertEq(Null, $$("tetris(dex(tetris,a),\\,a)"));
    }

    @Test
    void NormalizeVsImageNormalize_neg() {
        assertEq("(--,ang(fz,4))",
                imageNormalize($$("(--,(4-->(ang,fz,/)))")));
    }

    @Disabled
    @Test
    void bothImageTypesInProduct() {
        assertEq("(c-->(a,b,\\,/))",
                imageExt($$("a(b,\\,c)"), $$("c")));
    }


    @Disabled
    @Test
    void bothImageTypesInProduct2() {
        assertEq("TODO", imageNormalize($$("((b,\\,c,/)-->a)")));
        assertEq("TODO", imageNormalize($$("(a-->(b,\\,c,/))")));
    }

    @Disabled
    @Test
    void imgBoolTest() {
        assertEq("(((x,y),/,b)-->(false,/,b))", "(((x,y),/,b)-->(false,/,b))");
        assertEq("", imageNormalize($$("(((x,y),/,b)-->(false,/,b))")));

        //TODO
        //assertEq("(false,/,b):((x,y),/,b)", "(false,/,b):((x,y),/,b)");
    }

    @Test
    void imageAlignTo() {
        assertEq("(x-->(z,/,y))", imageExt($$("((x,y) --> z)"), $$("x")));

        assertEq(
                "z(x,\\,y)",
                alignTo($$c("((x,y) --> z)"), $$c("(x --> (z,y))"), new RandomBits(new XoRoShiRo128PlusRandom(1)))
        );
    }

}