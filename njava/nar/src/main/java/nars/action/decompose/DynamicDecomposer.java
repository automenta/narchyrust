package nars.action.decompose;

import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.atom.Bool;
import nars.term.atom.Img;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.random.RandomGenerator;

@Deprecated public abstract class DynamicDecomposer implements BiFunction<Compound, RandomGenerator, Term> {

    @Nullable
    protected Term sampleDynamic(Compound x0, int depthRemain, RandomGenerator rng) {
        assert(!(x0 instanceof Neg));
        var x = x0;
        while (true) {
            var y = subterm(x, rng);

            if (depthRemain <= 1 || !(y instanceof Compound yc))
                return valid(y) ? y : ((x == x0) ? null : x);

            depthRemain--;
            x = yc;
        }
    }

    public static boolean valid(Term y) {
        return !(y instanceof Img || y instanceof Bool);
    }

    public Term subterm(Compound x, RandomGenerator rng) {
        return subterm(x.subtermsDirect(), null, rng);
    }

    public Term subterm(Compound x, RandomGenerator rng, Subterms tt) {
        return switch (tt.subs()) {
            case 0 -> x;
            case 1 -> tt.sub(0);
            default -> subterm(tt, x, rng);
        };
    }

    protected boolean unneg() {
        return true;
    }

    /** simple subterm choice abstraction TODO a good interface providing additional context */
    public abstract Term subterm(Subterms tt, Term parent, RandomGenerator rng);

    public abstract static class WeightedDynamicCompoundDecomposer extends DynamicDecomposer implements FloatFunction<Term> {

        @Override
        public final Term subterm(Subterms s, @Nullable Term parent, RandomGenerator rng) {
            var u = s.subRoulette(this, rng);
            assert(u!=null);
            return u.unneg();
        }

        @Override public float floatValueOf(Term subterm) {
            return
                1; //flat
                //subterm.unneg().complexity(); //select proportional to volume
                //Util.sqr(subterm.unneg().complexity()); //select proportional to volume sqr.  the exponent promotes decentralization of the term graph
                //(float) Math.sqrt(subterm.unneg().complexity());
                //1f/subterm.unneg().complexity(); //select proportional to low volume (bad)
        }
    }
}