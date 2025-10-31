package nars.func;

import nars.*;
import nars.action.transform.Arithmeticize;
import nars.term.atom.Int;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.atom.Bool.False;
import static nars.term.atom.Bool.Null;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** arithmetic operators and arithmetic introduction tests */
public class MathFuncTest {
    private final NAR n = NARS.shell();


    @Test void add_set() {
        assertEval("6", "add(1,2,3)");
        assertEval("6", "add(1,2,3)");
        assertEval("add(#1,3)", "add(1,2,#z)");
        assertEval("add(#1,#2,3)", "add(1,2,#a,#b)");
    }
    @Test void mul_set() {
        assertEval("mul(#1,2)", "mul(1,2,#z)");
        assertEval("6", "mul(1,2,3)");
        assertEval("mul(#1,2)", "mul(1,2,#z)");
        assertEval("mul(#1,#2,2)", "mul(1,2,#a,#b)");
    }

    @Test
    void add_flatten_sort_3ary() {
        assertEval("add(a(#1),b(#1),c(#1))", "add(add(a(#x),b(#x)),c(#x))");
        assertEval("add(a(#1),b(#1),c(#1))", "add(add(c(#x),b(#x)),a(#x))");
    }
    @Test
    void add_flatten_sort_4ary() {
        assertEval("add(a(#1),b(#1),c(#1),d(#1))", "add(add(add(a(#x),d(#x)), b(#x)),c(#x)))");
        assertEval("add(a(#1),b(#1),c(#1),d(#1))", "add(add(add(a(#x),c(#x)), b(#x)),d(#x)))");
        assertEval("mul(a(#1),b(#1),c(#1),d(#1))", "mul(mul(mul(a(#x),d(#x)), b(#x)),c(#x)))");
        assertEval("mul(a(#1),b(#1),c(#1),d(#1))", "mul(mul(mul(a(#x),c(#x)), b(#x)),d(#x)))");
    }

    @Test
    void embeddedSort_repeats() {
        assertEval("add(mul(a(#1),2),b(#1))", "add(add(a(#x),b(#x)),a(#x))");
    }


    @Test void addSet_repeats_to_mult() {
        assertEval("add(mul(#1,2),#2,6)", "add(1,2,#a,#b,#a,3)");
    }

    @Test void addSet_eval() {
        assertEval("3", "((#y=add(1,2)) && #y)");
        assertEval("1", "((#y=add(1)) ==> #y)");
        assertEval("1", "(($y=add(1)) ==> $y)");
        assertEval("6", "((#y=add(1,2,3)) && #y)");
    }

    @Test
    void testAddSolve() throws Narsese.NarseseException {

        n.believe("((3=add(1,$x))==>its($x))");
        n.run(2);
        //TODO
    }

    @Test
    void testAdd_2_const_pos_pos() {
        assertEval(Int.i(2), "add(1,1)");
    }

    @Test
    void testAdd_2_const_nonNumeric_nonVar_unchanged() {
        assertEval("add(x,y)", "add(x,y)");
    }

    @Test
    void testMul_2_const_pos_pos() {
        assertEval(Int.i(6), "mul(2,3)");
    }
    @Test
    void testAdd_2_const_pos_neg() {
        assertEval(Int.ONE, "add(2,-1)");
    }

    @Test void Add_1_const_1_var() {
        assertEval($.varDep(1), "add(#1,0)");
    }




    @Test
    void testAddIdentity() {
        assertEval($.varDep(1), "add(0,#1)");
        assertEval($.varDep(1), "add(#1,0)");
        assertEval("x", "add(x,0)");
    }

    @Test
    void testMulIdentity() {
//        assertEval(Int.the(0), "mul(x,0)");
        assertEval("x", "mul(x,1)");
        assertEval($.varDep(1), "mul(1,#1)");
        assertEval($.varDep(1), "mul(#1,1)");
    }

    @Test
    void vectorAdd() {
        assertEval("(2,5)", "add((1,2),(1,3))");
        assertEval("(4,9)", "add((1,2),(1,3),(2,4))");
        assertEval("(4,9,1)", "add((1,2,0),(1,3,0),(2,4,1))");
    }

    @Test void vector_add_identity() {
        assertEval("(1,1)", "add((0,0),(1,1))");
    }

    @Test void vector_add_uncomputable_dont_expand() {
        final String s = "add((#1,#2),(3,3))";
        assertEval(s,s);
    }

    @Test
    void vector_add_shape_mismatch() {
        assertEval(Null, "add((1,2),3)");
        assertEval(Null, "add((1,2),(2,3),4)");
        assertEval(Null, "add((1,2),(2,3,4))");
        assertEval(Null, "add((1,2),(2,3,4),5)");
    }
    @Test
    void vector_mul_shape_mismatch() {
        assertEval(Null, "mul((1,2),(2,3,4))");
        assertEval(Null, "mul((1,2),(2,3,4),5)");
    }

    @Test
    void mul_vector_scalar() {
        assertEval("(1,2)", "mul((1,2),1)");
        assertEval("(2,4)", "mul((1,2),2)");
        assertEval("(2,4)", "mul(2,(1,2))");
    }

    @Test
    void mul_vector_scalar_vars() {
        assertEval("(mul(#1,2),4)", "mul((#1,2),2)");
        assertEval("(mul(#1,2),4)", "mul(2,(#1,2))");
    }

    @Test
    void add_vector_vector_vars() {
        assertEval("(add(#1,2),5)", "add((#1,2),(2,3))");
        assertEval("(add(#1,7),11)", "add((#1,2),(2,3),(4,5),(1,1))");
    }


    public static void assertEval(String out, String in) {
        assertEval($$(out), in);
    }

    public static void assertEval(Term out, String in) {
        assertEval(out, $$(in));
    }
    public static void assertEval(String out, Term in) {
        assertEval($$(out), in);
    }

    public static void assertEval(Term out, Term in) {
        assertEquals( out, NARS.shell().eval(in));
    }

    static void assertEvalFalse(String s) {
        assertEval(False /*"(--," + s + ")"*/ , s);
    }

    @Disabled
    @Test
    void testSimBackSubstitution() throws Narsese.NarseseException {
        assertSolves("(#1,3)", "(&&,(#1,#2),(#2 <-> 3))");
    }


    static void assertSolves(String o, String i) throws Narsese.NarseseException {
        //1.
//        assertEval($$(o),  i);

        //2.
        NAR n = NARS.tmp(2);
        //n.termVolumeMax.setAt(14);
        TestNAR t = new TestNAR(n);
        t.volMax(16);
        t.mustBelieve(16, o, 1f,1f, 0.9f,0.9f);
        n.input(i + ".");
        t.run();
    }

    @Disabled @Test
    void testCompleteAddInduction() {

//        n.log();

        new Arithmeticize.ArithmeticIntroduction0();

        TestNAR t = new TestNAR(n);
        t.confTolerance(0.8f);
        n.freqRes.set(0.25f);
        n.complexMax.set(19);


        for (int a = 1; a <= 2; a++) {
            t.believe(("(a," + a + ")"));
        }

        final int cycles = 500;
        for (int x = 3; x <= 4; x++) {
            //t.input("(a," + x + ")?");
            t.mustBelieve(cycles, "(a," + x + ")", 1f, 0.5f);
        }
        t.run();
    }

    @Disabled @Test void XOR() {
        assertEval("xor(true,false)", "xor(true,false)");
        assertEval("xor(#1,true)", "xor(#x,true)");
        assertEval("(--,xor(true,true))", "xor(true,true)");
    }





}