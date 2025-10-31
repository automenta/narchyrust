package nars.io;

import nars.NAL;
import nars.time.clock.CycleTime;

import java.util.concurrent.ThreadLocalRandom;

@Deprecated final class DummyNAL extends NAL {
    DummyNAL() {
        super(new CycleTime(), ThreadLocalRandom::current);
    }
}
