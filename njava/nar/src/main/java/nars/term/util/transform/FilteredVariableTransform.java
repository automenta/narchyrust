package nars.term.util.transform;

import nars.term.Compound;
import nars.term.atom.Atomic;

import static nars.Op.hasAny;

abstract public class FilteredVariableTransform extends VariableTransform {
    protected final int varBits;

    protected FilteredVariableTransform(int varBits) {
        this.varBits = varBits;
    }

    @Override
    protected boolean variable(Atomic a) {
        return super.variable(a) && hasAny(varBits, a.structOp());
    }

    @Override
    public boolean preFilter(Compound x) {
        return x.hasAny(varBits);
    }
}