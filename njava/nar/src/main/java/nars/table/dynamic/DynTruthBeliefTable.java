package nars.table.dynamic;

import nars.Answer;
import nars.Term;
import nars.truth.dynamic.DynTruth;


/**
 * computes dynamic truth according to implicit truth functions
 * determined by recursive evaluation of the compound's sub-component's truths
 */
public class DynTruthBeliefTable extends DynBeliefTable {

    public final DynTruth[] model;

    public DynTruthBeliefTable(Term c, DynTruth[] model, boolean beliefOrGoal) {
        super(c, beliefOrGoal);
        this.model = model;
    }

    @Override
    public void match(Answer a) {
        throw new UnsupportedOperationException();
    }
}