package nars.subterm;

import jcog.Hashed;
import nars.Term;

public final class RepeatedSubterms extends MappedSubterms<Term> implements Hashed {
    //TODO extends HashCachedRemappedSubterms<T> {

    private final int n;
    private final int hash;

    RepeatedSubterms(Term base, int n) {
        super(base);
        assert(n>1);
        this.n = n;
        this.hash = super.hashCode();
    }

    @Override
    public int structSubs() {
        return ref.struct();
    }

    @Override
    public int structSurface() {
        return ref.structOp();
    }

    @Override
    public final int hashCode() {
        return hash;
    }


    @Override
    public int complexity() {
        return 1 + ref.complexity() * n;
    }

    //TODO others

    @Override
    public int indexOf(Term t, int after) {
        return ref.equals(t) ? ((after < n-1) ? after+1 : -1) : -1;
    }

    @Override
    public int vars() {
        return ref.vars() * n;
    }

    @Override
    public int varDep() {
        return ref.varDep() * n;
    }

    @Override
    public int varIndep() {
        return ref.varIndep() * n;
    }

    @Override
    public int varPattern() {
        return ref.varPattern() * n;
    }

    @Override
    public int varQuery() {
        return ref.varQuery() * n;
    }


    @Override
    public int subs() {
        return n;
    }

    @Override
    public Term sub(int i) {
        return ref;
    }

}