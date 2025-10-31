package nars.truth;

import jcog.Fuzzy;
import jcog.Is;
import jcog.math.LongInterval;
import nars.TruthFunctions;

/** indicates an implementation has, or is associated with a specific TruthValue */
public interface Truthed  {

    @Is("Quantum_spin") float freq();

    /**
     * the average frequency during the given interval
     * NaN if undefined
     */
    default float freq(long start, long end) {
        return freq();
    }

    default double evi(long start, long end) {
        return evi();
    }

    default float freq(LongInterval l) {
        return freq(l.start(), l.end());
    }

    default float freq(long[] startEnd) {
        return freq(startEnd[0], startEnd[1]);
    }

    /** amount of evidence ( confidence converted to weight, 'c2w()' ) */
    @Is({"Epistemology", "Evidence_law"}) double evi();

    /** provides high-precision confidence value, if implemented */
    default double conf() {
        return TruthFunctions.e2c(evi());
    }

    default double expectation() {
        return TruthFunctions.expectation(freq(), conf());
    }

    default double expectationNeg() {
        return TruthFunctions.expectation(freqNeg(), conf());
    }

    /** value between 0 and 1 indicating how distant the frequency is from 0.5 (neutral) */
    default double polarity() {
        return Fuzzy.polarity(freq());
    }

    /**
     * Check if the truth value is negative/never/false/etc.
     * Note that values of 0.5 are not considered positive, being an unbiased
     * midpoint value
     *
     * @return True if the frequence is less than (but not equal to) 1/2
     */
    default /* final */ boolean NEGATIVE() {
        return freq() < 0.5f;
    }

    /**
     * Check if the truth value is positive/always/true/etc.
     * Note that values of 0.5 are not considered positive, being an unbiased
     * midpoint value
     *
     * @return True if the frequence is greater than or equal to 1/2
     */
    default /* final */ boolean POSITIVE() {
        return freq() >= 0.5f;
    }


    default double eviEternalized(double eviRate) {
        return TruthFunctions.eternalize(evi() * eviRate);
    }

    default /* final */ float freqNeg() {
        return 1 - freq();
    }

    default /* final */ float freqNegIf(boolean neg) {
        return neg ? freqNeg() : freq();
    }

    /** a scalar reduction of a truth value. centered at 0, and ranges either positive or negatively in proportion to evidence and polarized frequency. */
    default double weight() {
        return Fuzzy.polarize(freq()) * evi();
    }

}