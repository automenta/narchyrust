package nars.subterm;

import nars.Term;
import nars.term.Termlike;

public abstract sealed class MappedSubterms<S extends Termlike> extends ProxySubterms<S> permits RemappedSubterms, RepeatedSubterms {

    private transient boolean normalizedKnown, normalized;

    MappedSubterms(S ref) {
        super(ref);
    }

    @Override
    public abstract Term sub(int i);
    @Override
    public abstract int subs();

    @Override
    public final boolean TEMPORAL_VAR() {
        return ref.TEMPORAL_VAR();
    }

    @Override
    public boolean internables() {
        return ((Subterms)ref).internables();
    }

    @Override
    @Deprecated public final void setNormalized() {
        normalizedKnown = normalized = true;
    }

    @Override
    public final boolean NORMALIZED() {
        if (!normalizedKnown) {
            boolean n = super.NORMALIZED();
            normalized = n;
            normalizedKnown = true;
            return n;
        } else
            return normalized;
    }

}