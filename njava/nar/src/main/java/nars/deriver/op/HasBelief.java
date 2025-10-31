package nars.deriver.op;

import nars.Deriver;
import nars.term.atom.Atomic;
import nars.term.control.PREDICATE;


public final class HasBelief extends PREDICATE<Deriver> {

    static final Atomic D = Atomic.atom("double");

    public static final PREDICATE<Deriver> DOUBLE = new HasBelief(), SINGLE = DOUBLE.neg();

    private HasBelief() {
        super(D);
    }

    @Override
    public boolean test(Deriver d) {
        return d.premise.belief() != null;
    }

    @Override
    public float cost() {
        return 0.001f;
    }

}