package nars.truth.dynamic;

import nars.Term;

import static nars.NAL.dyn.DYN_IMPL_LIMITING;

abstract class DynImplSect extends DynStatement.DynStatementSect {

    protected DynImplSect(boolean subjOrPred) {
        super(subjOrPred);
    }

    @Override
    public int levelMax() {
        return DYN_IMPL_LIMITING ? 1 : super.levelMax();
    }

    @Override
    public boolean projectComponents(Term template) {
        return false;
    }
}