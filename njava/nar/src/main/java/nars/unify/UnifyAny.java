package nars.unify;

import jcog.random.RandomBits;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.NAL;
import nars.Term;

import java.util.Random;
import java.util.random.RandomGenerator;

/** stops after the first unification.  can be used to test whether two terms unify at least one way. */
public class UnifyAny extends Unify {

    public UnifyAny() {
        this(new XoRoShiRo128PlusRandom());
    }

    public UnifyAny(Random rng) {
        this(new RandomBits(rng));
    }

    public UnifyAny(RandomGenerator rng) {
        super(null, rng, NAL.unify.UNIFICATION_STACK_CAPACITY);
    }

    @Deprecated private int matches;

    @Override
    protected boolean match() {
        matches++;
        return false; //stop after the first
    }

    public boolean unifies(Term x, Term y) {
        clear();
        int matchesBefore = matches;
        return unify(x, y) && matchesBefore < matches;
    }

}