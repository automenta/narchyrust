package nars.term.compound;

import jcog.Hashed;
import jcog.The;
import nars.Op;
import nars.Term;

import static nars.Op.CONJ;
import static nars.Op.NEG;


/**
 * 1-element Compound impl
 */
public class CachedUnitCompound extends LightUnitCompound implements The, Hashed {

    /**
     * hash including this compound's op (cached)
     */
    protected final int _hash;

    /**
     * structure including this compound's op (cached)
     */
    private final int _struct;
    private final short _complexity;

    public CachedUnitCompound(Op op, Term sub) {
        super(op.id, sub);
        assert (op != NEG && op != CONJ);

        this._hash = hash1(op.id, sub);
        this._struct = structSubs() | op.bit;
        this._complexity = (short) (sub.complexity() + 1);
        assert (_complexity < Short.MAX_VALUE);
    }

    @Override
    public final int complexity() {
        return _complexity;
    }

    @Override
    public final int struct() {
        return _struct;
    }

    @Override
    public int varPattern() {
        return hasAny(Op.VAR_PATTERN) ? sub().varPattern() : 0;
    }

    @Override
    public int varDep() {
        return hasAny(Op.VAR_DEP) ? sub().varDep() : 0;
    }

    @Override
    public int varIndep() {
        return hasAny(Op.VAR_INDEP) ? sub().varIndep() : 0;
    }

    @Override
    public int varQuery() {
        return hasAny(Op.VAR_QUERY) ? sub().varQuery() : 0;
    }

    @Override
    public int vars() {
        return hasVars() ? sub().vars() : 0;
    }

    @Override
    public final int hashCode() {
        return _hash;
    }
}