package nars.term.anon;

import nars.Term;
import nars.term.Compound;
import nars.term.atom.Anom;
import nars.term.atom.Atomic;
import nars.term.util.map.ByteAnonMap;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.var.Variable;
import org.jetbrains.annotations.Nullable;

/**
 * target anonymization context, for canonicalization and generification of compounds
 * //return new DirectTermTransform() {
 * //return new TermTransform.NegObliviousTermTransform() {
 */
public class Anon extends RecursiveTermTransform {

    final ByteAnonMap map;

    protected boolean putOrGet = true;

    public boolean keepIntrin = true;
    public boolean keepVariables = true;

    public Anon() {
        this(1);
    }

    Anon(int estSize) {
        this(new ByteAnonMap(estSize));
    }

    public Anon(ByteAnonMap map) {
        this.map = map;
    }

    public int uniques() {
        return map.termCount();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    @Override
    public final @Nullable Term applyAtomic(Atomic atomic) {
        return putOrGet ? putAtomic(atomic) : getAtomic(atomic);
    }

    public final Term put(Term x) {
        return x instanceof Compound ? putCompound((Compound) x) : putAtomic((Atomic) x);
    }

    /** determines what Atomics are considered intrinsic (and thus not internable) */
    public boolean intrin(Atomic x) {
        return Intrin.intrin(x);
    }

    final Term putAtomic(Atomic x) {

//        if (x instanceof UnnormalizedVariable)
//            return x; //HACK is this necessary?
        if (keepIntrin && intrin(x))
            return putIntrin(x);

        if (intern(x))
            return putIntern(x);
        else
            return x;
    }

    private Anom putIntern(Term x) {
        return Anom.anom(map.intern(x));
    }

    /** default implementation: anonymize atoms, but also could be a Compound -> Atom anonymize */
    protected boolean intern(Atomic x) {
        return (!keepVariables || !(x instanceof Variable));
    }

    /** anon filter in which subclasses can implement variable shifting */
    Term putIntrin(Term x) {
        return x;
    }

    public final Term get(Term x) {
        return x instanceof Compound c ?
            getCompound(c) :
            getAtomic((Atomic) x);
    }

    final Term getAtomic(Atomic x) {
        return x instanceof Anom ? map.interned(((Anom) x).id()) : x;
    }

    protected Term getCompound(Compound x) {
        putOrGet = false;
        return applyCompound(x);
    }

    protected Term putCompound(Compound x) {
        if (abbreviate(x)) {
            return putIntern(x);
        } else {
            putOrGet = true;
            return applyCompound(x);
        }
    }

    /** if true, a compound will be interned as a whole rather than recursively decomposed */
    protected boolean abbreviate(Compound x) {
        return false;
    }

    public void clear() {
        map.clear();
    }

}