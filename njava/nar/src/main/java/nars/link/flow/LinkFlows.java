package nars.link.flow;

import jcog.Util;
import jcog.data.list.Lst;
import jcog.pri.Prioritized;
import jcog.pri.bag.Bag;
import jcog.pri.op.PriMerge;
import nars.Deriver;
import nars.Premise;
import nars.Term;
import nars.link.AtomicTaskLink;
import nars.link.MutableTaskLink;
import nars.link.TaskLink;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/** support methods and classes for LinkFlow */
public enum LinkFlows { ;

    /** warning: this is not atomic */
    public static double activate(float[] pri, Lst<MutableTaskLink> y, Bag<? extends Premise, ? extends Premise> b, PriMerge merge) {
        final int n = y.size();
        if (n > 1) Util.mul(pri, 1f/n);

        if (Util.max(pri) < TaskLink.EPSILON)
            return 0;

        double deltaSum = 0;
        for (MutableTaskLink Y : y)
            deltaSum += Y.mergeDelta(pri, merge);

        return deltaSum;
    }

    /** TODO use Snapshot */
    @Nullable private static Lst<MutableTaskLink> find(Premise x, /* TODO Iterable */ Bag<? extends Premise, ? extends Premise> b, int max, Function<Premise, BiPredicate<Term, Term>> matcher, @Nullable RandomGenerator rng) {
        assert(max > 0);

        BiPredicate<Term, Term> m = matcher.apply(x);
        if (m == null)
            return null; //HACK shouldnt happen

        Lst<MutableTaskLink> yy = null;
        var ii = rng!=null ? b.sampleUnique(rng) : b.iterator();
        int bs = b.size();
        while (ii.hasNext()) {
            MutableTaskLink Y = (MutableTaskLink) ii.next();
            if (m.test(Y.from(), Y.to())) {
                (yy != null ? yy : (yy = new Lst<>(Math.min(max, bs)))).add(Y);
                if (yy.size() >= max)
                    break;
            }
            bs--;
        }
        return yy;
    }

    private static final boolean createLink = true;

    public static void flow(Function<Premise, BiPredicate<Term, Term>> matcher, Premise x, float[/*4*/] pri, PriMerge merge, Bag<? extends Premise, ? extends Premise> b, int max, @Nullable RandomGenerator rng, Deriver d) {
        //x = imageNormalize(x);

        if (Util.max(pri) < TaskLink.TaskLinkEpsilon*max)
            return;

        var yy = find(x, b, max, matcher, rng);
        if (yy != null) {
            if (createLink) {
                Term xt = x.from();
                yy.replaceAll((y)->{
                    return AtomicTaskLink.link(xt, y.other(xt)); //TODO probabalistically swap order?
                });
            }

            float deltaSum = (float)activate(pri, yy, b, merge);

            if (createLink) {
                for (var y : yy)
                    d.link(y);
            } else {
                if (deltaSum >= Prioritized.EPSILON)
                    b.pressurize(deltaSum);
            }

            yy.delete();
        }
    }

    public static final Function<Premise, BiPredicate<Term, Term>> fromEqualsFromOrTo = x -> {
        Predicate<Term> xfEq = x.from().equals();
        return (yF, yT) -> !yF.equals(yT) && !yF.VAR() && !yT.VAR() && (xfEq.test(yF) || xfEq.test(yT));
    };

    public static final Function<Premise, BiPredicate<Term, Term>> fromEqualsFrom = x -> {
        Predicate<Term> xfEq = x.from().equals();
        return (yF, yT) -> !yF.equals(yT) && xfEq.test(yF);
    };

    public static final Function<Premise, BiPredicate<Term, Term>> fromEqualsTo = x -> {
        Predicate<Term> xfEq = x.from().equals();
        return (yF, yT) ->  !yF.equals(yT) && xfEq.test(yT);
    };

//
//            boolean ft; //true=yF, false=yT
//            if (match.test(yF)) ft = false;
//            else if (match.test(yT)) ft = true;
//            else return false;
//
//            return switch (/*other*/(ft ? yF : yT).op()) {
//                case CONJ, DELTA -> true;
//                case IMPL, EQ -> !ft;
//                default -> false;
//            };


    /** ensures acyclical flows */
    @Deprecated public static final Function<Premise, BiPredicate<Term, Term>> edgeToEdge = x -> {

        Term f = x.from(), t = x.to();
        if (f == t)
            return null; //shouldnt happen

        Predicate<Term> F = f.equals(), T = t.equals();

        return (yF, yT) -> {
            if (yF == yT) return false;
            return //F.test(yF) ||
                    F.test(yT) ||
                            T.test(yF)
                    //|| T.test(yT)
                    ;
        };
    };

}