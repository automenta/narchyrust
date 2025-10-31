package nars.task.util;

import jcog.data.list.Lst;
import jcog.signal.FloatRange;
import nars.NALTask;
import nars.Op;
import nars.Term;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

public class OpPri implements FloatFunction<NALTask> {

    public final Lst<FloatRange> op = new Lst<>(Op.values().length);

    public OpPri() {
        this(0, 2);
    }

    public OpPri(float min, float max) {
        for (Op o : Op.values())
            op.addFast(o.taskable ? new FloatRange(1, min, max) : null);
    }

    public float apply(int opID) {
        FloatRange f = op.get(opID);
        return f == null ? 1 : f.floatValue();
    }

    public final float apply(Op o) {
        return apply(o.id);
    }

    public final float apply(Term x) {
        return apply(x.opID());
    }

    @Override
    public final float floatValueOf(NALTask t) {
        return apply(t.op());
    }
}