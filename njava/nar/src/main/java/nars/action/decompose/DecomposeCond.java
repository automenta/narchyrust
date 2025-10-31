package nars.action.decompose;

import nars.Deriver;
import nars.Term;
import nars.term.Compound;
import nars.term.util.conj.ConjList;
import org.jetbrains.annotations.Nullable;

import java.util.random.RandomGenerator;

import static nars.deriver.reaction.PatternReaction.ImageAlignable;
import static nars.unify.constraint.TermMatch.Conds;

public class DecomposeCond extends DecomposeTerm {

    public DecomposeCond() {
        super();
        iff(PremiseTask, Conds);
        iffNot(PremiseTask, ImageAlignable);
    }

    @Override @Nullable public Term decompose(Compound conj, Deriver d) {
        return decomposeCond(conj, d.rng);
    }

    static Term decomposeCond(Compound cond, RandomGenerator r) {
        for (var decomposeEte : cond.seqDur()==0 ? T : F_T) {
            try (var e = ConjList.conds(cond, decomposeEte, decomposeEte)) {
                if (e.size() > 1)
                    return e.get(r).unneg();
            }
        }
        return null;
    }

    @Deprecated private static final boolean[] T = {true}, F_T = {false, true};

    //    /** TODO options to bias removal index position (ie. event volume, or more central than edges)
//     *  TODO options for target size curve
//     */
//    static Term decomposeCondComplex(Compound cond, RandomGenerator rng) {
//
//        boolean x = cond.dt()!=DTERNAL && cond.TEMPORAL_VAR();
//        try (ConjList e = ConjList.conds(cond, !x, x)) {
//            int eventsBefore = e.size();
//            if (eventsBefore < 2)
//                throw new WTF();
//            int eventsAfter = events(eventsBefore, rng);
//            //System.out.println(eventsBefore + "/" + eventsAfter);
//            Term y;
//            if (eventsAfter == 1) {
//                y = e.get(rng);
//            } else {
//                int toRemove = eventsBefore - eventsAfter;
//                for (int i = 0; i < toRemove; i++)
//                    e.removeThe(rng.nextInt(eventsBefore--));
//
//                y = x ? CONJ.the(Op.XTERNAL, e) : e.term();
//            }
//            return y.unneg();
//        }
//    }


//    /** how many events to keep, 1 <= x < n */
//    private static int events(int n, RandomGenerator r) {
//        return 1 + Util.bin((float) Util.expUnit(r.nextFloat(), expCurve), n-1);
//        //return Util.clamp((int) Math.round(0.5f + (n - 1) * Math.pow(r.nextFloat(), expCurve)), 1, n - 1);
//    }
//    static Term decomposeCond0(Compound cond, RandomGenerator r) {
//        final boolean DECOMPOSE_ONLY_FIRST_LAST = false;
//        if (ConjBundle.bundled(cond))
//            return decomposeInh(cond, r);
//
//        return switch(cond.dt()) {
//            case 0, DTERNAL, XTERNAL ->
//                cond.sub(r.nextInt(cond.subs())).unneg();
//            default -> {
//                //get either the first or last in the sequence.
//                //TODO maybe weighted selection using DecomposeN
//                try (ConjList e = ConjList.conds(cond, false, false)) {
//                    if (DECOMPOSE_ONLY_FIRST_LAST)
//                        yield (r.nextBoolean() ? e.getFirst() : e.getLast()).unneg();
//                    else
//                        yield e.get(r).unneg();
//                }
//            }
//        };
//    }
//

//    @Nullable private static Term decomposeInh(Term x, RandomGenerator r) {
//        TermList f = ConjBundle.events(x);
//        try {
//            Term y0 = f.get(r);
////            if (y0 == null)
////                return null; //HACK
////            else
//                return y0.unneg();
//        } finally {
//            f.delete();
//        }
//    }

}