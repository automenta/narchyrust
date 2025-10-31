package nars.term.control;

import jcog.Util;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.Lst;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nars.Op.SETe;


/**
 * parallel branching
 * <p>
 * TODO generify beyond only Derivation
 */
public final class FORK<X> extends PREDICATE<X> {

    //@Stable
    public final PREDICATE<X>[] branch;

    private FORK(SortedSet<PREDICATE<X>> actions) {
        this(actions.toArray(EmptyPredicateArray));
    }

    private FORK(PREDICATE<X>[] branches) {
        super(SETe.the(ids(branches)));
        this.branch = branches;
        assert (branches.length > 1);
    }

    public static <X> PREDICATE<X> fork(Stream<PREDICATE<X>> c) {
        return fork(c.collect(Collectors.toCollection(() -> new TreeSet<>())));
    }

    public static <X> PREDICATE<X> fork(List<PREDICATE<X>> c) {
        return switch (c.size()) {
            case 0 -> TRUE;
            case 1 -> c.getFirst();
            default -> fork(c instanceof SortedSet S ? S : new TreeSet<>(c));
        };
    }

    private static <X> PREDICATE<X> fork(SortedSet<PREDICATE<X>> c, Function<SortedSet<PREDICATE<X>>, PREDICATE<X>> builder) {
        return switch (c.size()) {
            case 0 -> TRUE;
            case 1 -> c.first();
            default -> builder.apply(c);
        };
    }

    public static <X> PREDICATE<X> fork(SortedSet<PREDICATE<X>> c) {
        return fork(c, FORK::_fork);
    }

    /**
     * flatten Fork's in Fork
     */
    private static <X> PREDICATE<X> _fork(SortedSet<PREDICATE<X>> predicates) {
        return new FORK<>(flatten(predicates));
    }

    private static <X> SortedSet<PREDICATE<X>> flatten(SortedSet<PREDICATE<X>> p) {
        var toAdd = new Lst<FORK<X>>();
        for (var ii = p.iterator(); ii.hasNext(); ) {
            if (ii.next() instanceof FORK<X> f) {
                ii.remove();
                toAdd.add(f);
            }
        }
        for (var f : toAdd)
            p.addAll(Arrays.asList(f.branch));
        return p;
    }

    public Iterable<PREDICATE<X>> branches() {
        return ArrayIterator.iterable(branch);
    }

    public Stream<PREDICATE<X>> branchStream() {
        return ArrayIterator.stream(branch);
    }

    @Override
    public float cost() {
        return Float.POSITIVE_INFINITY;
    }

    /**
     * simple exhaustive impl
     */
    @Override
    public final boolean test(X x) {
        for (var b : branch)
            b.test(x);
        return true;
    }

    @Override
    public PREDICATE<X> transform(Function<PREDICATE<X>, PREDICATE<X>> f, boolean outer) {

        TreeSet<PREDICATE<X>> yy = null;

        for (int i = 0, branchLength = branch.length; i < branchLength; i++) {
            var x = branch[i];
            var y = x.transform(f, outer);
            if (x != y) {
                if (yy == null) {
                    yy = new TreeSet<>();
                    //noinspection ManualArrayToCollectionCopy
                    for (var k = 0; k < i; k++)
                        yy.add(branch[k]);
                }
            }
            if (yy != null) yy.add(y);
        }

        //return f.apply(yy != null ? fork(yy) : this);
        var inner = yy!=null ? fork(yy) : this;
        return outer ? f.apply(inner) : inner;
    }

    @Override
    protected MethodHandle _mh() {
        return MethodHandles.filterReturnValue(
            Util.sequence(
                Util.arrayOf(i -> branch[i].mh().asType(V),
                    new MethodHandle[branch.length])),
            MH_T);
    }
}



