package nars.table.dynamic;

import nars.Op;
import nars.Term;
import nars.table.EmptyBeliefTable;

/** does not store tasks but only generates them on query.  usually implementations will override match(Answer) */
public abstract class DynBeliefTable extends EmptyBeliefTable {

    public final boolean beliefOrGoal;

    public final Term term;

    protected DynBeliefTable(Term c, boolean beliefOrGoal) {
        this.term = c;
        this.beliefOrGoal = beliefOrGoal;
    }

    /** this is very important:  even if size==0 this must return false */
    @Override public boolean isEmpty() {
        return false;
    }

    public final byte punc() {
        return beliefOrGoal ? Op.BELIEF : Op.GOAL;
    }

    @Override
    public final int capacity() {
        return 0;
    }
}