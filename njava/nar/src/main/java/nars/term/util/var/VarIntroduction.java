package nars.term.util.var;

import nars.Term;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.atom.Bool;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.random.RandomGenerator;

abstract class VarIntroduction {

    /** returns null if not applicable */
    @Nullable
    public final Term apply(Compound x, RandomGenerator rng, @Nullable Map<Term, Term> retransform) {

        if (x.complexity() < 3 /*|| x.complexity() < 2*/)
            return null;

        Term[] xx = options(x.subtermsDirect());
        if (xx.length == 0)
            return null;

        Term victim = xx.length > 1 ? choose(x, xx, rng) : xx[0];

        return apply(x, victim, retransform);
    }

    public final Term apply(Compound x, Term victim) {
        return apply(x, victim, null);
    }

    @Nullable public Term apply(Compound x, Term victim, @Nullable Map<Term, Term> retransform) {
        Term tgt = introduce(x, victim);
        if (tgt != null && !(tgt instanceof Bool)) {

//            int xVictimCount = x.subtermsDirect().countRecursive(victim.equals());
//            int volSavings = xVictimCount * (victim.volume() - 1);
//            /* detects if term collapsed as a result of variable introducion, which means semantics have changed */
//            int volExpected = x.volume() - volSavings;

            Term y = x.replace(victim, tgt);

            if (y!=null) {
                if (y.CONCEPTUALIZABLE() && !y.equals(x) /*&& y.volume() == volExpected*/) {
                    if (retransform != null) {
                        //TODO may not survive normalization
                        retransform.put(y, x);
                    }
                    return y;
                }
            }
        }

        return null;
    }

    /** determine the choice of subterms which can be replaced with a variable */
    protected abstract @Nullable Term[] options(Subterms input);

    protected abstract Term choose(Term x, Term[] y, RandomGenerator rng);

    /**
     * provides the next terms that will be substituted in separate permutations; return null to prevent introduction
     */
    protected abstract @Nullable Term introduce(Compound input, Term selection);
}