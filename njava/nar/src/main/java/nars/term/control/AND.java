package nars.term.control;

import jcog.Util;
import jcog.WTF;
import jcog.data.list.Lst;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static jcog.data.iterator.ArrayIterator.stream;
import static nars.Op.PROD;

public final class AND<X> extends PREDICATE<X> {
    private final float cost;

    /*@Stable*/
    public final PREDICATE<X>[] cond;

    @Override
    public final boolean test(X x) {
        for (var c : cond)
            if (!c.test(x))
                return false;
        return true;
    }

    AND(PREDICATE<X>[] cond) {
        validate(cond);
        super(
            PROD.the(ids(cond))
            //SimpleTermBuilder.the.compound(PROD, ids(cond))
        );
        this.cond = cond;
        this.cost = (float) Util.sum((FloatFunction<PREDICATE<X>>) (z -> {
            float c = z.cost();
            return Float.isFinite(c) ? c : 0;
        }), cond);

    }

    private static void validate(PREDICATE[] cond) {
        if (cond.length < 2)
            throw new UnsupportedOperationException("unnecessary use of AND");
        for (PREDICATE x : cond)
            if (x instanceof AND)
                throw new UnsupportedOperationException("should have been flattened");
    }

    public static <X> PREDICATE<X> the(List<PREDICATE<X>> cond) {
        int s = cond.size();
        return switch (s) {
            case 0 -> throw new UnsupportedOperationException();//TRUE;
            case 1 -> cond.getFirst();
            default -> the(cond.toArray(EmptyPredicateArray));
        };
    }

    public static <X> PREDICATE<X> the(PREDICATE<X>... cond) {
        //int s = cond.length;
        //if (s == 0) return null;

        PREDICATE<X>[] COND;
//        boolean needsFlat = Util.or(c -> c instanceof AND || c == TRUE, cond);
//        if (!needsFlat) {
//            COND = cond;
//        } else {//TODO sort?
            COND = stream(cond).flatMap(x -> x instanceof AND<X> X ?
                            X.conditions().stream() :
                            Stream.of(x)
                    ).filter(x -> x != TRUE)
                    .distinct().toArray(PREDICATE[]::new);
//        }
        if (Util.or(x -> x == FALSE, COND))
            return FALSE;

        return switch (COND.length) {
            case 0 -> throw new WTF();
            case 1 -> COND[0];
//            case 2 -> new AND2<>(cond[0], cond[1]);
//            case 3 -> new AND3<>(cond[0], cond[1], cond[2]);
            default -> {
                Arrays.sort(cond, CostIncreasing);
                //QuickSort.sort(cond, PREDICATE::cost);
                yield new AND<>(COND);
            }
        };
    }

    @Override
    public PREDICATE<X> transform(Function<PREDICATE<X>, PREDICATE<X>> f, boolean outer) {
        var ss = cond.clone();
        var change = false;
        for (int i = 0, xxLength = ss.length; i < xxLength; i++) {
            var x = ss[i];
            var y = x.transform(f, outer);
            if (x != y) change = true;
            ss[i] = y;
        }
        var inner = change ? the(ss) : this;
        return outer ? f.apply(inner) : inner;
    }

    public List<PREDICATE<X>> conditions() {
        return new Lst<>(cond.clone()); //to be safe
        //return new Lst<>(cond);
    }

    @Override
    public final float cost() {
        return cost;
    }

    @Override
    protected MethodHandle _mh() {
        var predicates = cond;
        var n = predicates.length;
        if (n < 2) throw new UnsupportedOperationException();

        MethodHandle y;
        y = predicates[n-1].mh();
        for (int i = n - 2; i >= 0; i--)
            y = MethodHandles.guardWithTest(predicates[i].mh(), y, FALSE_HANDLE);
        return y;

//            {
//            y = handle(predicates[0]);
//            for (var i = 1; i < predicateCount; i++)
//                y = MethodHandles.guardWithTest(y, handle(predicates[i]), DROP_FALSE_0);
//            }

    }
}
