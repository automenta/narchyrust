package nars.action.decompose;

import nars.Term;
import nars.term.Compound;
import org.jetbrains.annotations.Nullable;

import java.util.random.RandomGenerator;

public class DecomposeN extends DynamicDecomposer.WeightedDynamicCompoundDecomposer {
    final int depth;

    protected DecomposeN(int depth) {
        this.depth = depth;
    }

    @Override
    public @Nullable Term apply(Compound t, RandomGenerator rng) {
        return sampleDynamic(t, depth, rng);
    }
}