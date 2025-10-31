package nars.action.transform;

import nars.Deriver;
import nars.NAL;
import nars.Term;
import nars.term.Compound;
import nars.term.util.Explode;
import nars.unify.constraint.TermMatch;

public class ExplodeAction extends CondIntroduction {

    static final int copiesMin = 2;

    final int varsPerIteration =
        Integer.MAX_VALUE;
        //1;

    {
        volMin(PremiseTask, 5);

        //taskPunc(true,/* TEMPORARY */false,true,/*TEMPORARY*/false);

        //HACK for unfactored CONJ
        if (!NAL.term.CONJ_FACTOR)
            ifNot(PremiseTask, TermMatch.SEQ);

//        if (!NAL.term.IMPL_IN_CONJ)
//            isNot(PremiseTask, IMPL); //TODO use in other plugins
    }

    @Override
    protected Term apply(Term x, int volMax, Deriver d) {
        return new Explode((Compound)x, varsPerIteration, copiesMin, d.complexMax).outEqXY;
    }

}