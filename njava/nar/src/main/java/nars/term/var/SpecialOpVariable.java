package nars.term.var;

import nars.Op;

public final class SpecialOpVariable extends UnnormalizedVariable {

    private final Variable v;

    public SpecialOpVariable(Variable v, Op override) {
        super(override,
                override.ch + v.toString() //TODO byte[] optimize
        );
        assert(v.opID()!=override.id);
        this.v = v;
    }

    @Override
    public String toString() {
        return op().ch + v.toString();
    }
}