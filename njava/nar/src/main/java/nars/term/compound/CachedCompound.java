package nars.term.compound;

import jcog.Hashed;
import jcog.The;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.util.TermException;

import static nars.Op.DTERNAL;


/**
 * on-heap, caches many commonly used methods for fast repeat access while it survives
 */
public final class CachedCompound extends SeparateSubtermsCompound implements The, Hashed {

    private final int dt;

    private transient int hash;

    /** (cached) */
    private final int _struct, _complexity;

    public static Compound the(Op op, Subterms subterms) {
        return the(op, DTERNAL, subterms);
    }

    public static Compound the(Op op, int dt, Subterms s) {
        assertSubtermsValid(op, dt, s);
        return new CachedCompound(op.id, dt, s);
    }

    private static void assertSubtermsValid(Op op, int dt, Subterms s) {
        if (s instanceof Compound || s instanceof TermList)
            throw new TermException("requires non-Compound, non-TermList Subterms instance", op, dt, s);
    }

    private CachedCompound(int op, int dt, Subterms s) {
        super(op, s);
        this.dt = dt;
        this._struct = structOp() | s.struct();
        this._complexity = s.complexity();

//        //TEMPORARY
//        if (op==Op.CONJ.id && s.subs()==2 && s.sub(0) instanceof Neg != s.sub(1) instanceof Neg)
//            Util.nop();
//        if (op==CONJ.id && dt==DTERNAL && s.hasAll(NEG.bit | CONJ.bit) && !s.NORMALIZED()) {
//            Term x = normalize();
//            if (x.complexity()!=complexity())
//                Util.nop();
//        }
    }

    @Override
    public int complexity() {
        return _complexity;
    }

    @Override
    public int struct() {
        return _struct;
    }

    @Override
    public final int dt() {
        return dt;
    }

    @Override
    public final int hashCode() {
        int h = hash;
        return h == 0 ? hash = super.hashCode() : h;
    }
}