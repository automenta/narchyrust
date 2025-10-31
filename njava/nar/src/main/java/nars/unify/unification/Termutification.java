package nars.unify.unification;

import jcog.Util;
import jcog.data.list.Lst;
import jcog.data.map.UnifriedMap;
import jcog.data.set.ArrayHashSet;
import nars.Term;
import nars.term.atom.Bool;
import nars.term.var.Variable;
import nars.unify.Unification;
import nars.unify.Unify;
import nars.unify.mutate.Termutator;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Termutator-permuted Unification
 *      not thread-safe
 */
public class Termutification extends ArrayHashSet<DeterministicUnification> implements Unification {

    private final Unify base;
    private final Termutator[] termutes;

    public Termutification(Unify u, DeterministicUnification pre, Termutator[] termutes) {
        super(0);
        this.termutes = termutes;

        var baseMap = new UnifriedMap<Term, Term>(4, 1f);
        var base = new Unify.ContinueUnify(u, baseMap);
        var applied = pre.apply(base);
        assert(applied);
        baseMap.trimToSize();
        this.base = base;


//        restart = discovery.size();
    }


    /**
     * returns how many TTL used
     */
    public void discover(Unify ctx, int discoveriesMax, int ttl) {


        var u = new Discovery(this.base, discoveriesMax);
        u.setTTL(ttl);

        u.match(termutes, -1);

        var spent = Util.clampSafe(ttl - u.ttl, 0, ttl);

        ctx.use(spent);
    }

    @Override
    public final int forkKnown() {
        return size();
    }

    @Override
    public Iterable<Term> apply(Term x) {
        return switch (size()) {
            case 0 -> Util.emptyIterable;
            case 1 -> first().apply(x);
            default -> applyN(x);
        };
    }

    private List<Term> applyN(Term x) {
        //HACK could be better
                /* equals between Term and Unification:
                Reports calls to .equals() where the target and argument are of incompatible types. While such a call might theoretically be useful, most likely it represents a bug. */
        var s = shuffle(this, base.random);
        List<Term> l = new Lst<>(s.size());
        for (var a : s) {
            var b = a.transform(x);
            if (b!= Bool.Null)
                l.add(b);
        }
        return l.stream().filter(Objects::nonNull
                        /*&&
                        z != Unification.Null(*/).toList();
    }

    private static List<DeterministicUnification> shuffle(ArrayHashSet<DeterministicUnification> fork, RandomGenerator rng) {
        return fork.list.clone().shuffleThis(rng);
    }

//    public List<DeterministicUnification> listClone() {
//        Lst<DeterministicUnification> l = list;
//        return switch (l.size()) {
//            case 0 -> EMPTY_LIST;
//            case 1 -> List.of(l.getOnly());
//            default -> list.clone();
//        };
//    }


    private class Discovery extends Unify.ContinueUnify {

        private final Unify parent;
        private int discoveriesRemain;

        /**
         * if xy is null then inherits the Map<Term,Term> from u
         * otherwise, no mutable state is shared between parent and child
         *
         * @param parent
         * @param xy
         */
        Discovery(Unify parent, int discoveriesRemain) {
            super(parent, new UnifriedMap<>(0));
            this.parent = parent;
            this.discoveriesRemain = discoveriesRemain;
        }

        @Override
        @Deprecated public Term resolveVar(Variable x) {
            var y = parent.resolveVar(x);
            if (y != null && y != x) {
                if (size==0 || !var(y))
                    return y; //constant

                x = (Variable) y;   //recurse thru this resolver
            }

            return size > 0 ? super.resolveVar(x) : x;
        }

//        @Override
//        public boolean live() {
//            return super.live() && discoveriesRemain > 0;
//        }

        @Override
        protected boolean match() {

            var z = unification(false);
            if (z != Null) {
                if (z!=Self && z instanceof DeterministicUnification) {
                    if (Termutification.this.add((DeterministicUnification) z)) {
                        //TODO calculate max possible permutations from Termutes, and set done
                    }
                } else {
                    /*else {
                        return Iterators.getNext(a.apply(x).iterator(), null); //HACK
                    }*/
                }
            }

            return --discoveriesRemain > 0;
        }
    }
}