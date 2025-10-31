package nars.action.decompose;

import nars.Deriver;
import nars.Term;
import nars.term.Compound;
import org.jetbrains.annotations.Nullable;

import static nars.Op.IMPL;

public class DecomposeImpl extends DecomposeStatement {

    private final boolean secondLayer;

    private static final float secondLayerCond = 1/2f;

//    @Deprecated private static final float secondLayerInhSim =
//            0; //DISABLED
//            //0.1f;

    public DecomposeImpl(boolean secondLayer) {
        super(IMPL);
        this.secondLayer = secondLayer;
    }

    @Override protected boolean structural() {
        return false;
    }

    @Override
    public @Nullable Term decompose(Compound root, Deriver d) {
        Term a = super.decompose(root, d);

        if (secondLayer)
            if (a instanceof Compound ac && ac.CONDS() && d.randomBoolean(secondLayerProb(ac)))
                return DecomposeCond.decomposeCond(ac, d.rng);

        return a;
    }

    private static float secondLayerProb(Compound x) {
        return secondLayerCond;
    }

}