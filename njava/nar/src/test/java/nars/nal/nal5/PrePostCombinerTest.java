package nars.nal.nal5;

import nars.Term;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PrePostCombinerTest extends AbstractNAL5Test {

    static final int cycles = 200;

    @Test
    void preCombiner_SymmetricPP() {
        test.volMax(5).confMin(0.4f)
                .believe("((x1 && a) ==> c)")
                .believe("((y1 && a) ==> c)")
                .mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f)
                .mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void preCombiner_SymmetricNN() {
        test.volMax(5).confMin(0.4f)
                .believe("((x1 && a) ==> --c)")
                .believe("((y1 && a) ==> --c)")
                .mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f)
                .mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void preCombiner_SymmetricPN_0() {
        test.volMax(4).confMin(0.4f)
                .believe("(x1 ==>   c)")
                .believe("(y1 ==> --c)")
                .mustBelieve(cycles, "(  x1 ==> y1)", 0.00f, 0.45f)
                .mustBelieve(cycles, "(  y1 ==> x1)", 0.00f, 0.45f)
                //.mustBelieve(cycles, "(--y1 ==> x1)", 1.00f, 0.45f)
        ;
    }

    @Test
    void preCombiner_SymmetricPN() {
        test.volMax(5).confMin(0.4f)
                .believe("((x1 && a) ==>   c)")
                .believe("((y1 && a) ==> --c)")
                .mustBelieve(cycles, "(x1 ==> y1)", 0.00f, 0.45f)
                .mustBelieve(cycles, "(y1 ==> x1)", 0.00f, 0.45f);
    }

    @Test
    void preCombiner_AsymmetricPP() {

        test.volMax(5).confMin(0.35f)
                .believe("((x1 && a) ==> c)", 1, 0.9f)
                .believe("((y1 && a) ==> c)", 0.9f, 0.9f)
                .mustBelieve(cycles, "(x1 ==> y1)", 1, 0.42f/*0.9f, 0.45f*/)
                .mustBelieve(cycles, "(y1 ==> x1)", 1, 0.42f)
                ;
    }

    @Test
    void conditional_induction0Simple_NegInner() {
        test.volMax(8).confMin(0.4f)
                .believe("((x1 && a) ==> c)")
                .believe("((--y1 && a) ==> c)")
                .mustBelieve(cycles, "(x1 ==> y1)", 0.00f, 0.45f)
                .mustBelieve(cycles, "(--y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0SimpleDepVar() {
        test.volMax(5).confMin(0.4f)
                .believe("((x1 && #1) ==> c)")
                .believe("((y1 && #1) ==> c)")
                .mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f)
                .mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0SimpleDepVar2() {

        test.volMax(12).confMin(0.43f);
        test.believe("((x1 && #1) ==> (a && #1))");
        test.believe("((y1 && #1) ==> (a && #1))");
        test.mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f);
        test.mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0SimpleDepVar3() {

        test.volMax(7).confMin(0.4f);
        test.believe("((x1 && #1) ==> (a && #1))");
        test.believe("((#1 && #2) ==> (a && #2))");
        test.mustBelieve(cycles, "(x1 ==> #1)", 1.00f, 0.45f);
        test.mustBelieve(cycles, "(#1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0SimpleIndepVar() {

        test.volMax(8).confMin(0.43f);
        test.believe("((x1 && $1) ==> (a,$1))");
        test.believe("((y1 && $1) ==> (a,$1))");
        test.mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f);
        test.mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }
    @Test
    void conditional_induction_2ary() {
        test.volMax(9).confMin(0.43f)
            .believe("((x1 && a) ==> c)")
            .believe("((y1 && a) ==> c)")
            .mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f)
            .mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }
    @Test
    void conditional_induction_2ary_temporal() {
        test.volMax(9).confMin(0.43f)
                .believe("((x1 && a) ==>+1 c)")
                .believe("((y1 && a) ==>+2 c)")
                .mustNotBelieve(cycles, "((a&&y1)==>x1)")
                .mustNotBelieve(cycles, "(x1==>y1)")
                .mustBelieve(cycles, "(x1 ==>-1 y1)", 1.00f, 0.45f)
                .mustBelieve(cycles, "(y1 ==>+1 x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction_3ary() {
        test.volMax(12).confMin(0.43f)
            .believe("((&&,x1,x2,a) ==> c)")
            .believe("((&&,y1,y2,a) ==> c)")
            .mustBelieve(cycles, "((x1&&x2) ==> (y1&&y2))", 1.00f, 0.45f)
            .mustBelieve(cycles, "((y1&&y2) ==> (x1&&x2))", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction_3ary_some_inner_Neg_other() {
        test.volMax(12)
                .confMin(0.43f)
                .believe("((&&,x1,--x2,a) ==> c)")
                .believe("((&&,y1,y2,a) ==> c)")
                .mustBelieve(cycles, "((x1&&--x2) ==> (y1&&y2))", 1.00f, 0.45f)
                .mustBelieve(cycles, "((y1&&y2) ==> (x1&&--x2))", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction_3ary_some_inner_Neg_the() {
        test.volMax(12).confMin(0.43f)
                .believe("((&&,x1,x2,--a) ==> c)")
                .believe("((&&,y1,y2,--a) ==> c)")
                .mustBelieve(cycles, "((x1&&x2) ==> (y1&&y2))", 1.00f, 0.45f)
                .mustBelieve(cycles, "((y1&&y2) ==> (x1&&x2))", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0NegBothSimple() {

        test.volMax(7).confMin(0.35f);
        test.believe("--((x&&a) ==> c)");
        test.believe("--((x&&b) ==> c)");
        test.mustBelieve(cycles, "(a ==> b)", 1.00f, 0.45f);
        test.mustBelieve(cycles, "(b ==> a)", 1.00f, 0.45f);
    }

    @Disabled
    @Test
    void conditional_induction0NegBoth() {
        Term both = $$("(((x1&&x2) ==> (y1&&y2))&&((y1&&y2) ==> (x1&&x2)))");
        assertEquals("(((x1&&x2)==>(y1&&y2))&&((y1&&y2)==>(x1&&x2)))",
                both.toString());


        test.volMax(7).confMin(0.35f);

        test.believe("--((&&,x1,x2,a) ==> c)");
        test.believe("--((&&,y1,y2,a) ==> c)");
        test.mustBelieve(cycles, "((x1&&x2) ==> (y1&&y2))", 1.00f, 0.45f);
        test.mustBelieve(cycles, "((y1&&y2) ==> (x1&&x2))", 1.00f, 0.45f);
    }

//    @Test
//    void conditional_induction0NegInner() {
//
//        test.nar.termVolumeMax.setAt(9);
//        test.believe("((x&&a) ==> c)");
//        test.believe("(--(x&&b) ==> c)");
//        test.mustBelieve(cycles, "(a ==> --b)", 1.00f, 0.45f);
//        test.mustBelieve(cycles, "(--b ==> a)", 1.00f, 0.45f);
//    }
}