package nars.nal.nal7;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import org.junit.jupiter.api.BeforeEach;

abstract public class AbstractNAL7Test extends NALTest {

    protected static final int cycles = 40;
    @Deprecated public static final float TEMPORAL_CONF_TOLERANCE = 2; //200%

    @Override
    protected NAR nar() {
        return NARS.tmp(6, 8);
    }

    @BeforeEach
    void setTolerance() {
        test.volMax(12);
        test.confMin(0.3f);

        test.freqTolerance(0.1f);
        test.freqRes(0.02f);
        test.confTolerance(TEMPORAL_CONF_TOLERANCE);
        test.nar.confRes.set(0.02f);
    }

}