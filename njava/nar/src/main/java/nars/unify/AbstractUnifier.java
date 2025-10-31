package nars.unify;

import jcog.data.list.Lst;
import jcog.util.ArrayUtil;
import nars.Op;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.Neg;
import nars.unify.mutate.CommutivePermutations;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.unify.Unifier.Equal;

@FunctionalInterface public interface AbstractUnifier {

    @Nullable
    static AbstractUnifier compileCommute(Subterms xx, Subterms yy, int var, int dur) {
        int n = xx.subs();
        if (n != yy.subs())
            return null;

        Subterms[] exy = Unifier.eliminate(xx, yy);
        if (exy!=null) {
            if (exy.length == 0) //the signal for complete elimination
                return Equal;
            xx = exy[0];
            yy = exy[1];

            //some eliminated, test again
            n = xx.subs();
            if (n > 1 && null == Unifier.howSubterms(Op.SETe.id, xx, yy, var, dur, false))
                return null;
        }

        return n == 1 ? compile1(xx, yy, var, dur) : new CommutivePermutations(xx, yy);
    }

    @Nullable private static AbstractUnifier compile1(Subterms xx, Subterms yy, int var, int dur) {
        Term x0 = xx.sub(0), y0 = yy.sub(0);
        if (x0 == y0)
            return Equal;

        if (x0 instanceof Neg && y0 instanceof Neg) {
            x0 = x0.unneg(); y0 = y0.unneg();
        }

        AbstractUnifier xy = Unify.how(x0, y0, var, dur, true);
        return xy!=null ? xy.compile(x0, y0) : null;
    }

    boolean apply(Term x, Term y, Unify u);

    default float cost() {
        return 1;
    }

    default CompiledUnification compile(Term xi, Term yi) {
        return new CompiledUnification(this, xi, yi);
    }

    record CompiledUnification(AbstractUnifier how, Term xi, Term yi) implements Predicate<Unify>, AbstractUnifier {

        @Override
        public CompiledUnification compile(Term xi, Term yi) {
            return this;
        }

        @Override
        public boolean apply(Term x, Term y, Unify u) {
            return test(u);
        }

        public final float cost() {
            return how.cost();
        }

        @Override
        public boolean test(Unify u) {
            return how.apply(xi, yi, u);
        }
    }

    final class CompiledUnificationList extends Lst<AbstractUnifier> implements AbstractUnifier {

        CompiledUnificationList(int initialCapacity) {
            super(0, new AbstractUnifier[initialCapacity]);
        }

        void addUnification(AbstractUnifier u) {
            if (u instanceof CompiledUnificationList U)
                addAll(U);
            else
                add(u);
        }

        @Override
        public boolean apply(Term X, Term Y, Unify U) {
            for (AbstractUnifier abstractUnifier : this) {
                if (!abstractUnifier.apply(null, null, U))
                    return false;
            }
            return true;
        }

        private AbstractUnifier compile() {
            return switch (size) {
                case 0  -> Equal;  /* HACK */
                case 1  -> getFirst();
                default -> thisSorted();
            };
        }

        private CompiledUnificationList thisSorted() {
            //shuffling in tiers: shuffle spans of equivalent items
            sortThisByFloat(AbstractUnifier::cost, true);
            ArrayUtil.shuffleTiered(i -> items[i].cost(), this::swap, size);
            return this;
        }
    }

    //AtomicInteger rngSeed = new AtomicInteger();
    //() -> new XoRoShiRo128PlusRandom(rngSeed.incrementAndGet())

    @Nullable
    static AbstractUnifier compileLinear(Subterms xx, Subterms yy, int vars, int dur) {
        CompiledUnificationList y = null;
        int n = xx.subs();
        for (int i = n - 1; i >= 0; i--) {
            Term xi = xx.sub(i), yi = yy.sub(i);
            if (xi == yi) continue;

            if (xi instanceof Neg && yi instanceof Neg) {
                xi = xi.unneg(); yi = yi.unneg();
            }

            AbstractUnifier how = Unify.how(xi, yi, vars, dur, true);
            if (how == null) return null;
            else if (how != Equal) {
                if (y == null) y = new CompiledUnificationList(i + 1);
                y.addUnification(how.compile(xi, yi));
            }
        }
        return y!=null ? y.compile() : Equal;
    }

}