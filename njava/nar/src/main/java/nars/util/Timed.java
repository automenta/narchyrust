package nars.util;

import jcog.Is;
import jcog.random.RandomBits;

import java.util.Random;
import java.util.random.RandomGenerator;

/** implementations provide a time-bound context which supports
 *  awareness of temporal determinism */
public interface Timed {

    /** absolute current time, in time units */
    long time();

    /** time units (cycles) constituting a "duration" */
    @Is("Time_constant") float dur();

    @Deprecated
    RandomGenerator random();

    default RandomBits rng() {
        RandomGenerator r = random();
        return r instanceof RandomBits ? (RandomBits) r : new RandomBits((Random) r);
    }


}