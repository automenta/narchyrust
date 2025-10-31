package nars.term.control;

import nars.$;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.Function;

public final class IF<X> extends PREDICATE<X> {

    public final PREDICATE<X> cond, ifTrue, ifFalse;

    private final float cost;

    private IF(PREDICATE<X> cond, PREDICATE<X> ifTrue, PREDICATE<X> ifFalse) {
        super($.func("if", cond.term(), ifTrue.term(), ifFalse.term()));
        this.cond = cond;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
        float x = ifTrue.cost();
        this.cost = cond.cost() + Math.min(x, ifFalse.cost());
    }

    public static <X> PREDICATE<X> the(PREDICATE<X> cond, PREDICATE<X> ifTrue, PREDICATE<X> ifFalse) {
        if (ifTrue.equals(ifFalse)) return ifTrue;
        if (cond==TRUE) return ifTrue;
        if (cond==FALSE) return ifFalse;
        if (ifTrue == TRUE && ifFalse == FALSE)
            return cond;
        if (ifTrue == FALSE && ifFalse == TRUE)
            return cond.neg();
        return new IF<>(cond, ifTrue, ifFalse);
    }

    @Override
    public boolean test(X x) {
        return cond.test(x) ? ifTrue.test(x) : ifFalse.test(x);
    }

    @Override
    protected MethodHandle _mh() {
        return MethodHandles.guardWithTest(
            cond.mh(),
            ifTrue.mh(),
            ifFalse.mh()
        );
    }

    @Override
    public float cost() {
        return cost;
    }

    @Override
    public PREDICATE<X> transform(Function<PREDICATE<X>, @Nullable PREDICATE<X>> x, boolean outer) {
        var c = cond.transform(x, outer);
        var t = ifTrue.transform(x, outer);
        var f = ifFalse.transform(x, outer);
        var inner = c != cond || t != ifTrue || f != ifFalse ? the(c, t, f) : this;
        return outer ? x.apply(inner) : inner;
    }
}
