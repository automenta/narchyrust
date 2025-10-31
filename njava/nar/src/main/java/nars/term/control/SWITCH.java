package nars.term.control;

import nars.Term;

import java.util.function.ToIntFunction;

public class SWITCH<D> extends PREDICATE<D> {

    protected final PREDICATE<D>[] cases;
    final ToIntFunction<D> branch;

    @Override
    public final boolean test(D d) {
        var k = branch.applyAsInt(d);
        return k < 0 || cases[k].test(d);
    }

    @Override
    public float cost() {
        return 0; //TODO
    }

    protected SWITCH(Term id, PREDICATE<D>[] cases, ToIntFunction<D> branch) {
        super(id);
        this.cases = cases;
        this.branch = branch;
    }

}
