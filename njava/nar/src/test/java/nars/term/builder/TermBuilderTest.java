package nars.term.builder;

import nars.term.Compound;
import org.junit.jupiter.api.Test;

import static nars.$.$$c;
import static nars.Op.*;
import static nars.term.util.Testing.assertEq;

class TermBuilderTest {


    @Test void inhInImplPostNormalize_fail() {

        Compound x = $$c("((&&,y,--$1) ==>+- (x-->$1))");
        var s = x.struct();
        assertEq("(((--,$1)&&y) ==>+- (x-->$1))",
                !hasAny(s, NEG.bit) || (s & Variables) == 0 ?
                        x : new TermBuilder.CompoundPostNormalizer(x).apply(x)
        );
    }





//    @Test
//    void testTaskPostNormalize1() {
//
//        assertEq("((--,#1)==>(x,#1))", "(--#1 ==> (x, #1))");
//
//        assertEq("((--,$1)==>(x,$1))", "(--$1 ==> (x, $1))");
//
//
//        assertEq("(((--,#1)==>x),#1)", "((--#1 ==> x),#1)");
//
//        Compound x5 = $$c("(--#1 ==> x)");
//        var s5 = x5.structure();
//        assertEq("(#1==>x)", !hasAny(s5, NEG.bit) || (s5 & Variables) == 0 ?
//                x5 : new TermBuilder.CompoundPostNormalizer(x5).apply(x5));
//
//        Compound x4 = $$c("(--?1 ==> x)");
//        var s4 = x4.structure();
//        assertEq("(?1==>x)", !hasAny(s4, NEG.bit) || (s4 & Variables) == 0 ?
//                x4 : new TermBuilder.CompoundPostNormalizer(x4).apply(x4));
//
//
//        Compound x3 = $$c("((--,#1),x)");
//        var s3 = x3.structure();
//        assertEq("(#1,x)", !hasAny(s3, NEG.bit) || (s3 & Variables) == 0 ?
//                x3 : new TermBuilder.CompoundPostNormalizer(x3).apply(x3));
//
//        Compound x2 = $$c("(--$1 ==> (x, --$1))");
//        var s2 = x2.structure();
//        assertEq("($1==>(x,$1))", !hasAny(s2, NEG.bit) || (s2 & Variables) == 0 ?
//                x2 : new TermBuilder.CompoundPostNormalizer(x2).apply(x2));
//        //multiple:
//
//        Compound x1 = $$c("((--#1,--#1) ==> x)");
//        var s1 = x1.structure();
//        assertEq("((#1,#1)==>x)", !hasAny(s1, NEG.bit) || (s1 & Variables) == 0 ?
//                x1 : new TermBuilder.CompoundPostNormalizer(x1).apply(x1));
//
//        Compound x = $$c("((--?1,--?1) ==> x)");
//        var s = x.structure();
//        assertEq("((?1,?1)==>x)", !hasAny(s, NEG.bit) || (s & Variables) == 0 ?
//                x : new TermBuilder.CompoundPostNormalizer(x).apply(x));
//
//    }
//
//    @Test
//    void prodBalance1a() {
//
//        Compound x = $$c("(#1,--#1)");
//        var s = x.structure();
//        assertEq("((--,#1),#1)", !hasAny(s, NEG.bit) || (s & Variables) == 0 ?
//                x : new TermBuilder.CompoundPostNormalizer(x).apply(x));
//    }
//
//    @Test
//    void prodBalance1b() {
//        //no change:
//
//        Compound x = $$c("((--,#1),#1)");
//        var s = x.structure();
//        assertEq("((--,#1),#1)", !hasAny(s, NEG.bit) || (s & Variables) == 0 ?
//                x : new TermBuilder.CompoundPostNormalizer(x).apply(x));
//    }
//
//    @Test
//    void prodBalance2() {
//
//        Compound x1 = $$c("(--#1,--#1, #1)");
//        var s1 = x1.structure();
//        assertEq("(#1,#1,(--,#1))", !hasAny(s1, NEG.bit) || (s1 & Variables) == 0 ?
//                x1 : new TermBuilder.CompoundPostNormalizer(x1).apply(x1));
//
//        Compound x = $$c("(#1,#1, --#1)");
//        var s = x.structure();
//        assertEq("(#1,#1,(--,#1))", !hasAny(s, NEG.bit) || (s & Variables) == 0 ?
//                x : new TermBuilder.CompoundPostNormalizer(x).apply(x));
//    }
//
//    @Test
//    void prodBalance3() {
//
//        Compound x2 = $$c("((--#1,--#1, #1) ==> x)");
//        var s2 = x2.structure();
//        assertEq("((#1,#1,(--,#1))==>x)", !hasAny(s2, NEG.bit) || (s2 & Variables) == 0 ?
//                x2 : new TermBuilder.CompoundPostNormalizer(x2).apply(x2));
//
//        Compound x1 = $$c("((--#1,#1,--#1) ==> x)");
//        var s1 = x1.structure();
//        assertEq("((#1,(--,#1),#1)==>x)", !hasAny(s1, NEG.bit) || (s1 & Variables) == 0 ?
//                x1 : new TermBuilder.CompoundPostNormalizer(x1).apply(x1));
//
//        Compound x = $$c("((--#1,--#1, #1,--#2,--#2) ==> x)");
//        var s = x.structure();
//        assertEq("((#1,#1,(--,#1),#2,#2)==>x)", !hasAny(s, NEG.bit) || (s & Variables) == 0 ?
//                x : new TermBuilder.CompoundPostNormalizer(x).apply(x));
//    }
//
//    @Test
//    void testTaskPostNormalize2() {
//
//        Compound x = $$c("((--#1,--#2) ==> x)");
//        var s = x.structure();
//        assertEq("((#1,#2)==>x)", !hasAny(s, NEG.bit) || (s & Variables) == 0 ?
//                x : new TermBuilder.CompoundPostNormalizer(x).apply(x));
//    }
//
////    @Test
////    void testPostNormalize() {
////        assertEq("((#1,#1)==>x)", $$("((--#1,--#1) ==> x)").normalize());
////    }
//
//    @Test
//    void testTaskPostNormalize_Indep() {
//
//        Compound x = $$c("((--$1,x) ==> a(--$1))");
//        var s = x.structure();
//        assertEq("(($1,x)==>a($1))", !hasAny(s, NEG.bit) || (s & Variables) == 0 ?
//                x : new TermBuilder.CompoundPostNormalizer(x).apply(x));
//    }
}