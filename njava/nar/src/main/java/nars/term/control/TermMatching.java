package nars.term.control;

import nars.Term;
import nars.unify.constraint.TermMatch;

import java.util.function.Function;


/**
 * decodes a target from a provided context (X)
 * and matches it according to the matcher impl
 */
public final class TermMatching<X> extends PREDICATE<X> {

    public final TermMatch match;
    private final Function<X, Term> resolve;
    private final float cost;

    public TermMatching(TermMatch match, Function<X, Term> resolve, int depth) {
        super(match.name(resolve));
        this.resolve = resolve;
        this.match = match;
        this.cost = pathCost(depth) + match.cost();
    }

    private static float pathCost(int pathLen) {
        return pathLen * 0.25f;
    }

    @Override public float cost() {
        return cost;
    }

    @Override
    public final boolean test(X x) {
        var y = resolve.apply(x);
        return y!=null && match.test(y);
    }

//    @Override
//    public MethodHandle method(U u) {
//
//        //TODO test
//        MethodHandle c = MethodHandles.filterReturnValue(
//                insertArguments(R.bindTo(resolve), 0, u),
//                T.bindTo(match)
//        );
//
//        throw new TODO();
//        //return MethodHandles.guardWithTest(c, constant(boolean.class, trueOrFalse), constant(boolean.class, !trueOrFalse));
//    }

//    private static final MethodHandle R, T;
//    static {
//        MethodHandles.Lookup l = MethodHandles.lookup();
//
//        try {
//            Method rr = Function.class.getMethod("apply", Object.class); rr.trySetAccessible();
//            Method tt = PREDICATE.class.getMethod("test", Object.class); tt.trySetAccessible();
//            R = l.unreflect(rr);
//            T = l.unreflect(tt);
//        } catch (IllegalAccessException | NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }
//    }
}