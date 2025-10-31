package nars.term.control;

import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Function;

public final class NOT<X> extends PREDICATE<X> {
    public final PREDICATE<X> cond;

    NOT(PREDICATE<X> p) {
        super(p.ref.neg());
        this.cond = p;
    }

    @Override
    public PREDICATE<X> transform(Function<PREDICATE<X>, @Nullable PREDICATE<X>> f, boolean outer) {
        final var ux = cond;
        final var uy = cond.transform(f, outer);
        return ux == uy ? this /* unchanged */ : uy.neg();
    }

    //    @Deprecated @Override
//    public MethodHandle method(X x) {
//        return guardWithTest(p.method(x), CONSTANT_FALSE, CONSTANT_TRUE);
//    }


    @Override
    public float cost() {
        return cond.cost() + 0.001f;
    }

    @Override
    public PREDICATE<X> neg() {
        return cond; //unneg
    }

    @Override
    public boolean test(X o) {
        return !cond.test(o);
    }

    @Override
    protected MethodHandle _mh() {
        return MethodHandles.filterReturnValue(cond.mh(), NEGATE_HANDLE);
        //return MethodHandles.guardWithTest(cond.mh(), FALSE_HANDLE, TRUE_HANDLE);
    }

    @Override public PREDICATE<X> unneg() {
        return cond;
    }

    private static final MethodHandle NEGATE_HANDLE;
    static {
        try {
            NEGATE_HANDLE = MethodHandles.lookup()
                    .findStatic(NOT.class, "negate", MethodType.methodType(boolean.class, boolean.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new ExceptionInInitializerError("Failed to initialize NEGATE_HANDLE");
        }
    }
    public static boolean negate(boolean value) {
        return !value;
    }
}