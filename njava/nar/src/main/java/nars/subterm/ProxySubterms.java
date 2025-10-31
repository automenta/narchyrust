package nars.subterm;

import nars.Term;
import nars.term.Termlike;

public class ProxySubterms<S extends Termlike> implements Subterms {

    public final S ref;

    protected ProxySubterms(S ref) {
        this.ref = ref;
    }

    @Override public int hashCode() {
        return Subterms.super.hashCodeSubterms();
    }

    @Override
    public final int hashCodeSubterms() {
        return hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof Subterms s) && equalTerms(s);
    }

    @Override
    public final String toString() {
        return Subterms.toString(this);
    }

    @Override
    public Term sub(int i) {
        return ref.sub(i);
    }

    @Override
    public int subs() {
        return ref.subs();
    }

    @Override
    public int varDep() {
        return ref.varDep();
    }

    @Override
    public int varIndep() {
        return ref.varIndep();
    }

    @Override
    public int varQuery() {
        return ref.varQuery();
    }

    @Override
    public int varPattern() {
        return ref.varPattern();
    }

    @Override
    public final boolean hasVars() {
        return ref.hasVars();
    }

    @Override
    public int vars() {
        return ref.vars();
    }
}