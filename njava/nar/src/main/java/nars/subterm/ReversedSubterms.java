package nars.subterm;

import jcog.Hashed;
import jcog.util.ArrayUtil;
import nars.Term;

import java.util.Arrays;

public final class ReversedSubterms extends RemappedSubterms<Subterms> implements Hashed {

    private final int hash;

    /**
     * make sure to calculate hash code in implementation's constructor
     */
    ReversedSubterms(Subterms base) {
        super(base);
        this.hash = super.hashCode();
    }

    public static Subterms reverse(Subterms x) {
        if (x.subs() <= 1)
            return x;

        return switch (x) {
            case ReversedSubterms rs -> rs.ref; //un-reverse
            case ArrayRemappedSubterms mx -> reverse(x, mx);
            default -> new ReversedSubterms(x);
        };
    }

    private static Subterms reverse(Subterms x, ArrayRemappedSubterms mx) {
        //TODO test if the array is already perfectly reversed without cloning then just undo
        byte[] q = mx.map, r;

        //palindrome or repeats?
        int qLen = q.length;
        if ((qLen==2 && q[0]==q[1]) || (qLen==3 && q[0]==q[2]) || (qLen==4 && q[0] == q[3] && q[1]==q[2]) /* ... */) {
            return x; //obvious palindrome/repeats
        } else {
            r = q.clone();
            ArrayUtil.reverse(r);
            if (Arrays.equals(q,r))
                return x;
        }
        return new ArrayRemappedSubterms(mx.ref, r);
    }

    @Override
    public Subterms reverse() {
        return ref;
    }

    @Override
    public Term sub(int i) {
        return ref.sub(subs -1-i);
    }


    @Override public int hashCode() { return hash; }

}