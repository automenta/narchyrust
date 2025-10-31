package nars.task.util;

import jcog.signal.FloatRange;
import nars.NALTask;
import nars.Op;
import nars.Term;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

public enum Eternalization { ;

//    public static final FloatFunction<NALTask> None = x -> 0;

    public static class Flat implements FloatFunction<NALTask> {

        public final FloatRange ete = new FloatRange(0, 0, 1);

        @Override
        public float floatValueOf(NALTask t) {
            return ete.floatValue();
        }

        public FloatFunction<NALTask> set(float ete) {
            this.ete.set(ete);
            return this;
        }
    }
    public static class Derived extends Eternalization.Flat {
        @Override
        public float floatValueOf(NALTask t) {
            return t.stampLength() > 1 ? ete.floatValue() : 0;
        }

    }

    /** impl or conds (conj, inh bundled) */
    public static class Temporals extends Eternalization.Flat {
        @Override
        public float floatValueOf(NALTask t) {
            return eternalizable(t.term()) ? ete.floatValue() : 0;
        }

        boolean eternalizable(Term t) {
            return t.isAny(Op.Temporals);
//            return switch (t.op()) {
//                case CONJ, IMPL -> true;
//                case INH -> NAL.term.INH_BUNDLE ? ConjBundle.bundled(t) : false;
//                default -> false;
//            };
        }
    }

    public static class TemporalsAndVariables extends Temporals {
        @Override boolean eternalizable(Term t) {
            return t.hasAny(Op.Variables) || super.eternalizable(t);
        }
    }

}