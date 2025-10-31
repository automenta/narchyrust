//package nars.term.util.conj;
//
//import nars.unify.UnifyAny;
//import org.junit.jupiter.api.Test;
//
//import static nars.$.$$;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//class ConjPatternTest {
//
//
//    @Test
//    void simple() {
//        assertTrue(ConjPattern.the($$("z")).match($$("((x &&+1 y) &&+1 z)"), new UnifyAny()));
//        assertTrue(ConjPattern.the($$("z")).match($$("(&&,x,y,z)"), new UnifyAny()));
//        assertTrue(ConjPattern.the($$("z")).match($$("(x &&+- z)"), new UnifyAny()));
//        assertFalse(ConjPattern.the($$("w")).match($$("((x &&+1 y) &&+1 z)"), new UnifyAny()));
//    }
//    @Test
//    void simple_parallel() {
//        assertTrue(ConjPattern.the($$("(x&&y)")).match($$("(&&,x,y,z)"), new UnifyAny()));
//        assertFalse(ConjPattern.the($$("(x&&w)")).match($$("(&&,x,y,z)"), new UnifyAny()));
//    }
//    @Test
//    void simple_seq() {
//        assertTrue(ConjPattern.the($$("(x &&+1 y)")).match($$("(x &&+1 y)"), new UnifyAny()));
//        assertTrue(ConjPattern.the($$("(x &&+1 y)")).match($$("((x &&+1 y) &&+1 z)"), new UnifyAny()));
//        assertTrue(ConjPattern.the($$("(x &&+1 y)")).match($$("(z &&+1 (x &&+1 y))"), new UnifyAny()));
//    }
//    @Test
//    void simple_seq2() {
//        assertFalse(ConjPattern.the($$("(x &&+2 y)")).match($$("(z &&+1 (x &&+1 y))"), new UnifyAny()));
//        assertFalse(ConjPattern.the($$("(y &&+1 x)")).match($$("(z &&+1 (x &&+1 y))"), new UnifyAny()));
//
//        //within acceptable tolerance:
//        UnifyAny ud = new UnifyAny();
//        ud.dur = 1;
//        assertTrue(ConjPattern.the($$("(x &&+2 y)")).match($$("(z &&+1 (x &&+1 y))"), ud));
//
//    }
//
//    @Test
//    void glob_Conj_1_to_2_seq_end () {
////        Unify u = test(Op.VAR_PATTERN,
////                "((x &&+1 y) &&+1 z)",
////                "(x &&+1 %1)",
////                true);
////        assertEq("(y &&+1 z)", u.resolveVar($.varPattern(1)));
//    }
//
//    @Test
//    void glob_Conj_1_to_2_seq_mid () {
////        Unify u = test(Op.VAR_PATTERN,
////                "(((x &&+1 y) &&+1 z) &&+1 w)",
////                "((x &&+1 %1) &&+1 w)",
////                true);
////        assertEquals("{%1=(y &&+1 z)}", u.xy.toString());
////        assertEq("(y &&+1 z)", u.resolveVar($.varPattern(1)));
//    }
//
//
//}