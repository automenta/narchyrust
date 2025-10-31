package nars.subterm;

import jcog.Hashed;
import nars.Term;
import nars.term.Compound;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** maximally proxies metadata access methods to the referant */
public abstract class DirectProxySubterms extends ProxySubterms<Subterms> implements Hashed {

    protected DirectProxySubterms(Subterms ref) {
        super(ref);
    }

    @Override
    public boolean NORMALIZED() {
        return ref.NORMALIZED();
    }

    @Override
    public void setNormalized() {
        ref.setNormalized();
    }

    @Override
    public boolean internables() {
        return ref.internables();
    }

    @Override
    public boolean equalTerms(Subterms x) {
        return ref.equalTerms(x);
    }

    @Override
    public boolean subEquals(int i, Term x) {
        return ref.subEquals(i, x);
    }

    @Override
    public void forEach(Consumer<? super Term> action) {
        ref.forEach(action);
    }

    @Override
    public Iterator<Term> iterator() {
        return ref.iterator();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj || (obj instanceof Subterms s && !(obj instanceof Compound)) &&
                ref.equalTerms(s);
    }

    @Override
    public final int hashCode() {
        return ref.hashCode();
    }

    @Override
    public int complexity() {
        return ref.complexity();
    }

    @Override
    public int height() {
        return ref.height();
    }

    @Override
    public int struct() {
        return ref.struct();
    }

    @Override
    public int structSubs() {
        return ref.structSubs();
    }

    @Override
    public int structSurface() {
        return ref.structSurface();
    }

    @Override
    public boolean impossibleSubStructure(int structure) {
        return ref.impossibleSubStructure(structure);
    }

    @Override
    public boolean impossibleSubComplexity(int otherTermComplexity) {
        return ref.impossibleSubComplexity(otherTermComplexity);
    }

    @Override
    public boolean BOOL(Predicate<? super Term> t, boolean andOrOr) {
        return ref.BOOL(t, andOrOr);
    }
//
//    @Override
//    public @Nullable Term subSub(byte[] path) {
//        return ref.subSub(path);
//    }

    @Override
    public Term subUnneg(int i) {
        return ref.subUnneg(i);
    }


    @Override
    public final int seqDur(boolean xternalSensitive) {
        return ref.seqDur(xternalSensitive);
    }

    @Override
    public Term[] arrayShared() {
        return ref.arrayShared();
    }

    //TODO others
}