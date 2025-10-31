package nars.nal.nal1;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import org.junit.jupiter.api.Test;

import static jcog.Util.sqr;

public class HypothesisTest extends NALTest {

    protected static int cycles = 35;

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(1);
        n.complexMax.set(7);
        n.confMin.set(0);
        return n;
    }

    @Test
    void deduction() {
        float c = 0.03f;
        test
            .believe("(bird --> animal)", 1, c)
            .believe("(robin --> bird)", 1, c)
            .mustBelieve(cycles, "(robin --> animal)", sqr(c));
    }

}