package nars.subterm;

import jcog.math.ShuffledPermutations;
import nars.Term;
import org.jetbrains.annotations.Nullable;

import java.util.random.RandomGenerator;

/**
 * proxy to a TermContainer providing access to its subterms via a shuffling order
 */
public final class ShuffledSubterms extends RemappedSubterms<Subterms> {

    private final ShuffledPermutations shuffle = new ShuffledPermutations();

    public ShuffledSubterms(Subterms subterms, RandomGenerator rng) {
        super(subterms);
        reset(rng);
    }

    @Override public boolean internables() {
        return false;
    }

    @Override
    public @Nullable Term sub(int i) {
        return ref.sub(shuffle.permute(i));
    }

    private void reset(RandomGenerator rng) {
        shuffle.restart(subs(), rng);
    }

    public final boolean shuffle() {
        if (shuffle.hasNext()) {
            shuffle.next();
            return true;
        }
        return false;
    }
}