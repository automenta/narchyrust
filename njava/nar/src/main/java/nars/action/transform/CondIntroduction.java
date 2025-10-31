package nars.action.transform;

import nars.Deriver;
import nars.NAL;
import nars.Term;
import nars.action.TaskTermTransformAction;
import org.jetbrains.annotations.Nullable;

/** introduction applied to subconditions */
public abstract class CondIntroduction extends TaskTermTransformAction {

//    {
//        if (!NAL.term.IMPL_IN_CONJ)
//            isNot(PremiseTask, IMPL);
//    }

    protected abstract Term apply(Term x, int volMax, Deriver d);

    static final int VOL_MARGIN = 3; //TODO TUNE

    /** dont separate in conj and impl components if variables because renormalization will cause them to become distinct HACK */
    @Override
    public final Term apply(Term x, @Nullable Deriver d) {

//        if (!NAL.term.IMPL_IN_CONJ) {
//            if (x.IMPL()) {
//                int j = d.randomInt(2);
//                Term sa = x.subUnneg(j);
//                if (validImplComponent(sa))
//                    tgt = sa;
//                else {
//                    Term sb = x.subUnneg(1 - j);
//                    if (validImplComponent(sb))
//                        tgt = sb;
//                    else
//                        return null;
//                }
//            }
//            //TODO any other extractors?
//            //ex: PROD
//        }

        return _apply(x, d);
//        if (tgt!=x) {
////            if (y instanceof Bool)
////                return Null; //fail
//            if (y!=null && y!=Null && tgt!=x)
//                return x.replace(tgt, y);
//        }
//        return y;
    }

    private static boolean validImplComponent(Term x) {
        return x.CONJ() && (NAL.term.CONJ_FACTOR || !x.SEQ());
    }

    private Term _apply(Term x, @Nullable Deriver d) {
        return apply(x, d !=null ? d.complexMax - VOL_MARGIN : Integer.MAX_VALUE, d);
    }

    @Deprecated public final Term apply(Term x) {
        return apply(x, null);
    }


}