package nars.nal.nal5;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class AnonymousAnalogyTest extends AbstractNAL5Test {


    @Test
    void testAnonymousDeduction() {
        test
                .believe("(#1 ==> x)")
                .question("x")
                .mustBelieve(cycles, "x", 1f, 0.81f)
        ;
    }

    @Test
    void testAnonymousDeductionNeg() {
        test
                .believe("(#1 ==> --x)")
                .question("x")
                .mustBelieve(cycles, "x", 0f, 0.81f)
        ;
    }


    /**
     * experimental
     */
    @Disabled
    @Test
    void testAnonymousAbduction() {
        test
                .believe("(x ==> #1)")
                .question("x")
                .mustBelieve(cycles, "x", 1f, 0.45f)
        ;
    }

    /**
     * experimental
     */
    @Disabled
    @Test
    void testAnonymousAbductionNeg() {
        test
                .believe("(--x ==> #1)")
                .question("x")
                .mustBelieve(cycles, "x", 0f, 0.45f)
        ;
    }


}