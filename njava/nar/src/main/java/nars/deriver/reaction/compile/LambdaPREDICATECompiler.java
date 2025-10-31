package nars.deriver.reaction.compile;

import nars.term.control.PREDICATE;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;

public enum LambdaPREDICATECompiler {
    ;

    public static <X> @Nullable PREDICATE<X> apply(PREDICATE<X> x) {
        var mh = x.mh();
        if (mh!=null)
            return new MHPredicate<>(x, mh);

        return x;
    }

    static final class MHPredicate<X> extends PREDICATE<X> {
        private final float cost;
        private final MethodHandle handle;

        MHPredicate(PREDICATE<X> p, MethodHandle handle) {
            super(p.term());
            this.handle = handle;
            this.cost = p.cost();
        }

        @Override
        public boolean test(X x) {
            try {
                return (boolean) handle.invokeExact(x);
            } catch (Throwable e) {
                throw new IllegalStateException("Error executing compiled predicate", e);
            }
        }

        @Override
        public float cost() {
            return cost;
        }
    }


}