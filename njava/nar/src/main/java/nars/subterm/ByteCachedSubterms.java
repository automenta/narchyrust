package nars.subterm;

import com.google.common.io.ByteArrayDataOutput;
import jcog.util.ByteSearch;
import nars.Term;
import nars.term.Compound;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class ByteCachedSubterms extends DirectProxySubterms {

    private final byte[] bytes;

//    public static ByteCachedSubterms the(Subterms ref) {
//        if (ref instanceof ByteCachedSubterms) return (ByteCachedSubterms) ref;
//
//        DynBytes b = new DynBytes(ref.subs() * 64 /* estimate */);
//        ref.write(b);
//        return new ByteCachedSubterms(ref, b.arrayCompactDirect());
//    }

    public ByteCachedSubterms(Subterms ref, byte[] bytes) {
        super(ref);
        this.bytes = bytes; //TODO defer until actually interned
    }

    @Override
    public void write(ByteArrayDataOutput out) {
        out.write(bytes);
    }

    @Override
    public int indexOf(Term x, int after) {
        if (after == -1 && x instanceof Atomic && !maybeContainsAtomicRecursively((Atomic)x))
            return -1;
        return ref.indexOf(x, after);
    }

    /** experimental */
    @Override
    public boolean containsRecursively(Term x, @Nullable Predicate<Compound> subTermOf) {
        if (x instanceof Atomic && subTermOf==null && !maybeContainsAtomicRecursively((Atomic)x))
            return false;

        return ref.containsRecursively(x, subTermOf);
    }

    private boolean maybeContainsAtomicRecursively(Atomic x) {
        byte[] xx = x.bytes();
        byte[] yy = this.bytes;
        if (xx.length >= yy.length)
            return false;
        return ByteSearch.indexOfDirect(yy, xx) != -1;
    }


}