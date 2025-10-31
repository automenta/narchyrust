package nars.subterm;

import jcog.data.iterator.ArrayIterator;
import nars.Term;

import java.util.Iterator;

/**
 * Holds a vector or tuple of terms.
 * Useful for storing a fixed number of subterms
 */
public final class ArraySubterms extends CachedSubterms {

//    /** since this would only be invoked after checking hashcode, it's utility is rare */
//    @Deprecated private static final boolean TERM_REPLACING_IF_HASHCODE_EQUALS = false;

    /*@NotNull*/
    /*@Stable*/
    private final Term[] terms;

    public ArraySubterms(Term... terms) {
        super(terms);
        this.terms = terms;
    }

    @Override
    public final String toString() {
        return Subterms.toString(terms);
    }

//    @Override
//    public boolean equalExhaustive(Subterms s) {
//        return TERM_REPLACING_IF_HASHCODE_EQUALS && s instanceof ArraySubterms a ?
//            equals(a) :
//            super.equalExhaustive(s);
//    }
//
//    private boolean equals(ArraySubterms a) {
//        Term[] xx = this.terms, yy = a.terms;
//        if (xx == yy)
//            return true;
//        if (xx.length!=yy.length)
//            return false;
//        for (int i = xx.length -1; i >= 0; i--) {
//            Term x = xx[i], y = yy[i];
//            if (x != y) {
//                if (x.equals(y)) {
//                    if (identityHashCode(x) < identityHashCode(y))
//                        yy[i] = x;
//                    else
//                        xx[i] = y;
//                } else
//                    return false;
//            }
//        }
//        if (identityHashCode(xx) < identityHashCode(yy))
//            a.terms = xx;
//        else
//            this.terms = yy;
//        return true;
//    }

    @Override
    public boolean subEquals(int i, Term x) {
        Term[] terms = this.terms;
        Term y = terms[i];
        //            if (TERM_REPLACING_IF_HASHCODE_EQUALS) {
        //                if (identityHashCode(x) < identityHashCode(y))
        //                    terms[i] = x;
        //            }
        if (x == y)
            return true;
        else return y.equals(x);
    }

    @Override
    public final Term sub(int i) {
        return terms[i];
    }

    @Override
    public final Term[] arrayClone() {
        return terms.clone();
    }

    @Override
    public final Term[] arrayShared() {
        return terms;
    }

    @Override
    public final int subs() {
        return terms.length;
    }

    @Override
    public final Iterator<Term> iterator() {
        return ArrayIterator.iterate(terms);
    }

}