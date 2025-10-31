package nars.term.util;

import jcog.Hashed;
import nars.Term;
import nars.term.Compound;

import java.util.function.Predicate;

/**
 * TODO subclass for Neg
 */
public class TermEquality implements Predicate<Term> {

    private final Term x;

    private final int xOp;
    private final boolean xCompound;

    /** lazily computed */
    private int xHash;

    public TermEquality(Term x) {
        this.x = x;
        this.xCompound = x instanceof Compound;
        this.xOp = x.opID();
    }

    private int hashX() {
        int h = this.xHash;
        return h == 0 ? (this.xHash = x.hashCode()) : h;
    }

    @Override
    public boolean test(Term y) {
        return x==y ||
               (xCompound == y instanceof Compound
               && xOp == y.opID()
               && eqHash(y)
               && x.equals(y));
    }

    private boolean eqHash(Term y) {
        return !(y instanceof Hashed)
               ||
               hashX() == y.hashCode();
    }

}