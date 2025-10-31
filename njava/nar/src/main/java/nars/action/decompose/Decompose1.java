package nars.action.decompose;

import nars.Deriver;
import nars.Op;
import nars.Term;
import nars.term.Compound;
import org.jetbrains.annotations.Nullable;

public class Decompose1 extends DecomposeTerm {

    public Decompose1() {
        super();
    }

    public Decompose1(Op... ops) {
        this();
        isAny(PremiseTask, ops);
    }

    @Override
    @Nullable
    public Term decompose(Compound src, Deriver d) {
        return decomposer.apply(src, d.rng);
    }

    static final DynamicDecomposer decomposer = new DecomposeN(1);

//    @Deprecated static Term secondLayerInhSim(Term a, Term root, float secondLayerInhSim, Deriver d) {
//        if (secondLayerInhSim > 0 && a.isAny(INH.bit | SIM.bit) && d.randomBoolean(secondLayerInhSim * (a.complexity() / ((float)root.complexity()))))
//            return decomposer.subterm((Compound) a, d.rng);
//        else
//            return a;
//    }

}