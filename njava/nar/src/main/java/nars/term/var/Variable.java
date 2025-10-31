package nars.term.var;

import nars.Op;
import nars.term.atom.Atomic;

import static nars.Op.*;


/**
 * similar to a plain atom, but applies altered operating semantics according to the specific
 * varible type, as well as serving as something like the "marker interfaces" of Atomic, Compound, ..
 * <p>
 * implemented by both raw variable terms and variable concepts
 **/
public abstract sealed class Variable extends Atomic permits NormalizedVariable, UnnormalizedVariable {

    protected Variable(Op o, byte num) {
        super(o, num);
    }

    protected Variable(byte prefix, byte[] raw) {
        super(prefix, raw);
    }
    
    @Override
    public boolean VAR_DEP() {
        return opID() == VAR_DEP.id;
    }

    @Override
    public boolean VAR_INDEP() {
        return opID() == VAR_INDEP.id;
    }

    @Override
    public boolean VAR_QUERY() {
        return opID() == VAR_QUERY.id;
    }

    @Override
    public boolean VAR_PATTERN() {
        return opID() == VAR_PATTERN.id;
    }

    @Override
    public final boolean VAR() {
        return true;
    }

    @Override
    public final NormalizedVariable normalize(byte offset) {
        return NormalizedVariable.varNorm(opID(), (byte)(offset+1));
    }

    @Override
    public final boolean hasVars() {
        return true;
    }

    @Override
    public boolean hasVarIndep() {
        return opID() == VAR_INDEP.id;
    }

    @Override
    public final int varIndep() {
        return hasVarIndep() ? 1 : 0;
    }

    @Override
    public final int varDep() {
        return hasVarDep() ? 1 : 0;
    }

    @Override public boolean hasVarDep() {
        return opID() == VAR_DEP.id;
    }

    @Override
    public final int varQuery() {
        return hasVarQuery() ? 1 : 0;
    }

    @Override public boolean hasVarQuery() {
        return opID() == VAR_QUERY.id;
    }

    @Override
    public final int varPattern() {
        return hasVarPattern() ? 1 : 0;
    }

    @Override public boolean hasVarPattern() {
        return opID() == VAR_PATTERN.id;
    }

}