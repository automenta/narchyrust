package nars.action.transform;

import nars.Deriver;
import nars.Op;
import nars.Term;
import nars.action.TaskTermTransformAction;
import nars.term.Compound;
import nars.term.util.var.DepIndepVarIntroduction;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * old implementation
 *
 I, I, hasAny(I,{"-->","<->","==>","&&"})  |- varIntro(I), (Punctuation:Identity, Time:Task)
 */
public class VariableIntroduction0 extends TaskTermTransformAction {

    static final boolean filter_excessively_abstract =
            true;
            //!NAL.ABSTRACT_TASKS_ALLOWED;

//    static final boolean EVIDENCE_ATTENUATE = false;

    public VariableIntroduction0() {
        volMin/*complexityMin*/(PremiseTask, 3);
        //hasAny(TheTask, Op.StatementBits | CONJ.bit, true);

//        codec = () -> new TermCodec.AnonTermCodec(false,true);
    }

    @Override
    public Term apply(Term x, Deriver d) {
        return apply(x, d.rng, d.unify.retransform);
    }

    @Nullable
    public static Term apply(Term x, RandomGenerator rng, @Nullable Map<Term, Term> retransform) {
        Term y = DepIndepVarIntroduction.the.apply((Compound) x,
                rng,
                retransform
            //null
        );
        if (y == null) return null;

//        if(y.opID()!= x.opID()) {
////            if (NAL.DEBUG)
////                throw new WTF(); //why might this happen?
//            return null;
//        }

        if (filter_excessively_abstract && !y.hasAny(Op.AtomicConstant))
            return null;

        return y;
    }

}