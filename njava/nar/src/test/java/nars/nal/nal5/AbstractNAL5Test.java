package nars.nal.nal5;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;

abstract class AbstractNAL5Test extends NALTest {

    static final int cycles = 25;

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(6, 6);
        n.complexMax.set(5);
        n.confMin.set(0.2f);
        return n;
    }

}