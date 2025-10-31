package nars.term.control;

import jcog.Util;
import nars.$;
import nars.Op;
import nars.Term;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class PREDICATE<X> /*extends ProxyCompound*/ implements Termed, Comparable<PREDICATE> /* Predicate<X>*/ {

    protected final Compound ref;

    public PREDICATE(String term) {
        this(Atomic.atom/*$.$$*/(term));
    }

    protected PREDICATE(Term term) {
        this.ref = (term instanceof Compound c ? c : (Compound)$.p(term));
    }

    @Override
    public final Term term() {
        return ref;
    }

    @Override
    public final int compareTo(PREDICATE o) {
        return (this == o) ? 0 : term().compareTo(o.term());
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj || (obj instanceof PREDICATE p && term().equals(p.term()));
    }

    @Override
    public final int hashCode() {
        return term().hashCode();
    }

    @Override
    public final String toString() {
        return term().toString();
    }

    public static Term[] ids(Collection<PREDICATE<?>> cond) {
        return Util.map(PREDICATE::term, Op.EmptyTermArray, cond);
    }

    public static Term[] ids(PREDICATE<?>[] cond) {
        return Util.map(PREDICATE::term, new Term[cond.length], cond);
    }

    abstract public boolean test(X x);


    public PREDICATE<X> neg() {
        return new NOT<>(this);
    }

    public final PREDICATE<X> transform(Function<PREDICATE<X>, @Nullable PREDICATE<X>> f) {
        return transform(f, false);
    }

    public PREDICATE<X> transform(Function<PREDICATE<X>, @Nullable PREDICATE<X>> f, /*boolean inner,*/ boolean outer) {
        return f != null ? f.apply(this) : this;
    }

    /**
     * a relative global estimate (against its possible sibling PrediTerm's)
     * of the average computational cost of running the test method
     * warning: these need to return constant values for sort consistency
     *
     *
     * Some impl will need to return Float.POSITIVE_INFINITY to prevent their being position changing by sort:
     *   FORK, Action, TruthifyAction
     */
    abstract public float cost();

    public static final Comparator<PREDICATE> CostIncreasing = (a, b) -> {
        if (a==b) return 0;
        float ac = a.cost(), bc = b.cost();
        if (ac > bc) return +1;
        else if (ac < bc) return -1;
        else return a.compareTo(b);
    };

    public PREDICATE<X> unneg() {
        return this;
    }

    public final PREDICATE negIf(boolean b) {
        return b ? neg() : this;
    }

    public static final PREDICATE TRUE = new PREDICATE<>(Bool.True) {
        @Override
        public boolean test(Object o) {
            return true;
        }

        @Override
        public float cost() {
            return 0;
        }
    };
    public static final PREDICATE FALSE = new PREDICATE<>(Bool.False) {
        @Override
        public boolean test(Object o) {
            return false;
        }

        @Override
        public float cost() {
            return 0;
        }
    };

    public void conditionsRecursive(Consumer<PREDICATE<X>> o) {
        transform(z -> {
            o.accept(z);
            return z;
        });
    }

    /** override and return False if:
     *     -- the results are non-deterministic, ie. random
     *     -- produces necessary or temporary side effects
     */
    public boolean deterministic() {
        return true;
    }

    /** provide a MethodHandle interface */
    public final MethodHandle mh() {
        var t = term();
        var mh = m.get(t);
        if (mh!=null)
            return mh;

        mh = _mh();

        var mhExisting = m.putIfAbsent(t, mh);
        return mhExisting != null ? mhExisting : mh;
    }

    //private volatile transient MethodHandle m = null;
    private static final Map<Term,MethodHandle> m = new ConcurrentHashMap<>(1024);

    protected MethodHandle _mh() {
        return PREDICATE_TEST.bindTo(this);
    }

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
    private static final MethodType BO = MethodType.methodType(boolean.class, Object.class);
    private static final MethodType B = MethodType.methodType(boolean.class);
    static final MethodType V = MethodType.methodType(void.class, Object.class);
    private static final MethodHandle MH_FALSE = MethodHandles.constant(boolean.class, false).asType(B);
    public static final MethodHandle MH_T = MethodHandles.constant(boolean.class, true);
    private static final MethodHandle MH_TRUE = MH_T.asType(B);
    static final MethodHandle FALSE_HANDLE = MethodHandles.dropArguments(MH_FALSE, 0, Object.class);
    static final MethodHandle PREDICATE_TEST;
    static {
        try {
            PREDICATE_TEST = LOOKUP.findVirtual(PREDICATE.class, "test", BO);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static final PREDICATE[] EmptyPredicateArray = new PREDICATE[0];

}

//    public static <X> void test(PREDICATE<X> next, X x) {
//        var queue = new Lst<PREDICATE<X>>(8);
//        queue.addFast(next);
//
//        pop: while ((next = queue.poll())!=null) {
//
//            if (next instanceof AND<X> and) {
//                var a = and.cond;
//                var i = 0;
//                for (var length = a.length; i < length-1; i++)
//                    if (!a[i].test(x))
//                        continue pop;
//
//                next = a[i]; //fall-through
//            }
//
//            if (next instanceof FORK<X> fork) {
//                var b = fork.branch;
//                queue.addAll(1, b.length, b);
//                next = b[0]; //fall-through
//            }
//
//            next.test(x);
//        }
//    }

