package nars.nal.nal4;

import nars.test.NALTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class RelationInjectionTest extends NALTest {

    private static final int cycles = 24;

    @Test
    void testRelationExtraction_2ary() {
        test
                .volMax(7)
                .believe("((x,z)-->(y,z))")
                .mustBelieve(cycles, "(x --> y)", 1, 0.81f)
        ;
    }

    @Test
    void testRelationExtraction_2ary_inh() {
        test
                .volMax(12)
                .believe("(a(x,z) --> a(y,z))")
                .mustBelieve(cycles, "(x --> y)", 1, 0.81f)
        ;
    }

    @Test
    void testRelationExtraction_inh() {
        test
                .volMax(7)
                .believe("(a:b --> a:c)")
                .mustBelieve(cycles, "((b --> c)-->a)", 1, 0.81f)
                .mustNotQuestion(cycles, "((b-->a)<->(b-->?1))")
        ;
    }

    @Test
    void testRelationInjection_inh() {
        test
                .volMax(12)
                .believe("a:b")
                .believe("((b --> c)-->a)")
                .mustBelieve(cycles, "(a:b --> a:c)", 1, 0.81f)
        ;
    }
}