package nars.truth.dynamic;

import nars.Truth;
import nars.TruthFunctions;
import nars.truth.DynTaskify;
import nars.truth.PlainMutableTruth;

public abstract class DynSect extends DynTruth {

    @Override
    public final Truth truth(DynTaskify l) {
        PlainMutableTruth y = null;
        var truthFn = truthFn();

        var eviMin = l.eviMin();
        for (int i = 0, dSize = l.size(); i < dSize; i++) {
            var x = l.taskTruth(i);
            if (x == null)
                return null; //HACK truth was undefined or sth

            if (i == 0 /*y == null*/) {
                y = new PlainMutableTruth(x);
            } else {
                var yy = truthFn.truth(y, x, eviMin);
                if (yy == null)
                    return null;
                y.set(yy);
            }
        }
        return y;
    }

    protected TruthFunctions truthFn() {
        return TruthFunctions.Intersection;
    }

    @Override
    public int componentsEstimate() {
        return 4;
    }
}