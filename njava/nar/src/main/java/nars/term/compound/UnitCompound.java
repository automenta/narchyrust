package nars.term.compound;

import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.UnitSubterm;
import nars.term.Compound;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.function.ToIntFunction;

import static nars.Op.DTERNAL;
import static nars.Op.XTERNAL;

public abstract class UnitCompound extends Compound {

    private boolean normalized, normalizedKnown;

    protected UnitCompound(int op) {
        super(op);
    }

    protected abstract Term sub();

    public final Term[] arrayClone() {
        return new Term[]{sub()};
    }

    @Override
    public void setNormalized() {
        normalized = true;
        normalizedKnown = true;
        if (sub() instanceof Compound c)
            c.setNormalized();
    }

    @Override
    public boolean containsInstance(Term t) {
        return sub()==t;
    }

    @Override
    public final Term sub(int i) {
        if (i!=0)
            throw new ArrayIndexOutOfBoundsException();
        return sub();
    }

    @Override
    public boolean internables() {
        return sub().internable();
    }

    @Override
    public boolean containsNeg(Term x) {
        return sub().equalsNeg(x);
    }

    @Override
    public int hashCode() {
        return hash1(opID, sub());
        //return Compound.hash(this);
    }

    @Override
    public final int hashCodeSubterms() {
        return Subterms.hash(sub());
    }

    @Override
    public final int subs() {
        return 1;
    }

    @Override
    public final Subterms subterms() {
        return new UnitSubterm(sub());
    }

    @Override
    public final int dt() {
        return DTERNAL;
    }

    @Override
    public int complexity() {
        return sub().complexity()+1;
    }

    @Override
    public final int sum(ToIntFunction<Term> value) {
        return value.applyAsInt(sub());
    }

    @Override
    public final int max(ToIntFunction<Term> value) {
        return sum(value);
    }

    @Override
    public final int structSurface() {
        return sub().structOp();
    }

    @Override
    public final int structSubs() {
        return sub().struct();
    }

    @Override
    public int varPattern() {
        return sub().varPattern();
    }

    @Override
    public int varDep() {
        return sub().varDep();
    }

    @Override
    public int varIndep() {
        return sub().varIndep();
    }

    @Override
    public int varQuery() {
        return sub().varQuery();
    }

    @Override
    public int vars() {
        return sub().vars();
    }

    @Override
    public final int intifyShallow(int v, IntObjectToIntFunction<Term> reduce) {
        return reduce.intValueOf(v, sub());
    }

//    @Override
//    public final int intifyRecurse(int v, IntObjectToIntFunction<Term> reduce) {
//        return reduce.intValueOf(sub().intifyRecurse(v, reduce), this);
//    }

    @Override
    public final boolean NORMALIZED() {
        if (!normalizedKnown) {
            normalized = super.NORMALIZED();
            normalizedKnown = true;
        }
        return normalized;
    }

    @Override
    public Subterms subtermsDirect() {
        return this;
    }

    @Override
    public boolean TEMPORAL_VAR() {
        return dt() == XTERNAL || super.TEMPORAL_VAR();
    }
}