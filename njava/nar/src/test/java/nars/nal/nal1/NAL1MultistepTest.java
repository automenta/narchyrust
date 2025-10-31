package nars.nal.nal1;

import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import nars.test.impl.DeductiveChainTest;
import org.junit.jupiter.api.Test;

import static nars.test.impl.DeductiveChainTest.*;

public class NAL1MultistepTest extends NALTest {

    @Override protected NAR nar() {
        NAR n = NARS.tmp(6);
        n.complexMax.set(3);
        n.freqRes.set(0.25f);
        n.confRes.set(0.02f);
        return n;
    }

    @Test
    void multistepInh2() {
        new DeductiveChainTest(test, 2, 20, inh);
    }

    @Test
    void multistepSim2() { new DeductiveChainTest(test, 2, 20, sim);}

    @Test
    void multistepInh3() {
        new DeductiveChainTest(test, 3, 50, inh);
    }

    @Test
    void multistepSim3() {
        new DeductiveChainTest(test, 3, 200, sim);
    }

    @Test
    void multistepInh4() { new DeductiveChainTest(test, 4, 200, inh); }

    @Test
    void multistepSim4() {
        new DeductiveChainTest(test, 4, 280, sim);
    }

    @Test
    void multistepImpl2() {
        new DeductiveChainTest(test, 2, 20, impl);
    }

    @Test
    void multistepImpl3() {
        new DeductiveChainTest(test, 3, 70, impl);
    }

}