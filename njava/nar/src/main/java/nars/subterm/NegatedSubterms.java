package nars.subterm;

import jcog.Util;
import nars.Op;
import nars.Term;
import nars.term.Neg;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

/** all subterms negated;
 * assumes the base subterms consists of no negation subterms (at the surface) itself  */
public final class NegatedSubterms extends RemappedPNSubterms {

    private static final IntObjectToIntFunction<Term> NEG_HASH =
        (a, b) -> Util.hashCombine(a, b.hashCodeNeg());


    NegatedSubterms(Subterms base) {
        super(base);
        hash = base.intifyShallow(1, NEG_HASH);
    }

    @Override
    protected boolean wrapsNeg() {
        return true;
    }

    @Override
    protected int negs() {
        return ref.subs();
    }


    @Override
    public Subterms negated() {
        return this.ref;
    }

    @Override public int structSurface() {
        return Op.NEG.bit;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof NegatedSubterms n ?
            ref.equals(n.ref) :
            super.equals(obj));
    }

    @Override
    public int subMap(int i) {
        return -(i+1);
    }

    @Override
    public boolean contains(Term x) {
        return x instanceof Neg && ref.contains(x.unneg());
    }

    @Override
    public boolean containsNeg(Term x) {
        return !(x instanceof Neg) && ref.contains(x);
    }
}