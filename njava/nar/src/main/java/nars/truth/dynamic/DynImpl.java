package nars.truth.dynamic;

import jcog.util.ObjectLongLongPredicate;
import nars.*;
import nars.term.Compound;
import nars.term.Neg;
import nars.time.Tense;
import nars.truth.DynTaskify;
import nars.truth.TruthCurve;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.dither;
import static nars.time.Tense.occToDT;

public final class DynImpl extends DynTruth {
    public static final DynImpl DynImplInduction = new DynImpl();

    private DynImpl() { }

    /** whether to project components to the target time */
    public boolean projectComponents(Term template) {
        return false;
    }

    @Override
    @Nullable
    public Predicate<NALTask> preFilter(int component, DynTaskify d) {
        if (!NAL.dyn.DYN_IMPL_filter || component!=0)
            return null;

        var thresh =
                NAL.truth.FREQ_EPSILON_half;
                //d.nar().freqResolution.asFloat();
        var neg = d.template.sub(0) instanceof Neg;
        return x -> {
            var t = x.truth();
            if (t instanceof TruthCurve) return true; //ignore curve
            var f = t.freq();
            return neg ? f < 1 - thresh : f > thresh;
        };
    }

    private static void occ(DynTaskify d) {
        long s = d.get(0).start(), p = d.get(1).start();
        var sEte = s == ETERNAL;
        if (sEte) {
            s = p; //use P's time.. if not also eternal:
            if (s == ETERNAL)
                d.occ(ETERNAL, ETERNAL);
        }
        d.occ(s, s + d.rangeMin());
    }



    /**
     *     B, A, --is(A,"==>") |-          polarizeTask((polarizeBelief(A) ==> B)), (Belief:InductionDD, Time:BeliefRelative)
     *     B, A, --is(B,"==>") |-          polarizeBelief((polarizeTask(B) ==> A)), (Belief:AbductionDD, Time:TaskRelative)
     */
    @Override  public Truth truth(DynTaskify d) {
        //assert(d.pn.test(1));
        occ(d);

        var subj = d.taskTruth(0);
        if (subj!=null) {
            var pred = d.taskTruth(1);
            if (pred!=null) {
                return TruthFunctions.Abduction.truth(
                        subj,
                        pred,
                        d.eviMin()
                );
            }
        }
        return null;
    }

    /** TODO ensure non-collapsing dt if collapse imminent */
    @Override public boolean decompose(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
        assert(superterm.IMPL());
        Term subj = superterm.sub(0), pred = superterm.sub(1);

        long as, ae, bs, be;
        if (start == ETERNAL) {
            if (subj.equalsRoot(pred))
                return false;
            as = ae = bs = be = ETERNAL;
        } else {
//            long mid = Fuzzy.mean(start, end);
//            as = start; ae = mid;
//            bs = mid+1; be = end;
            as = bs = start;
            ae = be = end;

            var bShift = subj.unneg().seqDur();
            var sdt = superterm.dt();
            if (!Tense.parallel(sdt))
                bShift += sdt;

            bs += bShift; be += bShift;
        }

        return each.accept(subj, as, ae) && each.accept(pred, bs, be);
    }

//    private static boolean coincident(Term subj, Term pred) {
//        subj = subj.unneg(); pred = pred.unneg();
//        if (subj==pred) return true;
//        boolean sc = subj.CONDS(), pc = pred.CONDS();
//        if (!sc && !pc)
//            return subj.equalsPN(pred);
//        else if (sc && !pc)
//            return ((Compound)subj).condOf(pred, 0);
//        else if (!sc && pc)
//            return ((Compound)pred).condOf(subj, 0);
//        else /*if (sc && pc)*/ {
//            //TODO PN test
//            return !Conj.intersectConds(subj, pred).isEmpty();
//        }
//    }

    @Override
    public Term recompose(Compound superterm, DynTaskify d) {
        NALTask subj = d.get(0), pred = d.get(1);
        return IMPL.the(
            subj.term(),
            implDT(subj, pred, d.timeRes),
            pred.term()
        );
    }

    /** computes appropriate dt interval between two tasks for an implication linking them */
    static int implDT(NALTask a, NALTask b, int ditherDT) {
        if (b==null)
            throw new NullPointerException(); //TEMPORARY
        long SS = a.start(), PS = b.start();
        if (SS == ETERNAL || PS == ETERNAL) {
            return DTERNAL;
        } else {
            Term st = a.term();;
            var stu = st.unneg();
            var rawDT = occToDT(PS - SS - stu.seqDur());
            var dt = dither(rawDT, ditherDT);
            var pt = b.term();
            if (dt==0 && stu.equals(pt.unneg()))
                dt = rawDT; //HACK use rawDT so as not to collapse due to dithering
            return dt;
        }
    }

    @Override
    public int componentsEstimate() {
        return 2;
    }

}