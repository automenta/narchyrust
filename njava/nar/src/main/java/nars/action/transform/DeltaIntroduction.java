//package nars.action.transform;
//
//import nars.Term;
//import nars.control.Cause;
//import nars.Deriver;
//import nars.derive.reaction.NativeReaction;
//import nars.derive.reaction.Reaction;
//import nars.link.AtomicTaskLink;
//import nars.NALTask;
//import nars.term.util.Image;
//import nars.time.Tense;
//
//import static nars.Op.*;
//
///** TODO can this be an Op? or at least a Functor that can answer questions. dynamic belief table, etc */
//public class DeltaIntroduction extends NativeReaction {
//
//
//    {
//        taskEqualsBelief();
//        hasBeliefTask(false);
//        taskPunc(true,false, false,false);
//        //isNotAny(TheTask, CONJ.bit | IMPL.bit);
//        hasAny(PremiseTask, VAR_QUERY, false);
//
//    }
//
//    @Override
//    protected void run(Deriver d) {
//
//        NALTask X = d.premise.task();
//
//        Term x = X.term();
//        Term y = DELTA.the(Image.imageNormalize(x));
//
//        //deriveQuestion(y, d, why);
//        deriveTaskLink(y, d);
//    }
//
//    private void deriveTaskLink(Term y, Deriver d) {
//        d.add(AtomicTaskLink.link(y));
//    }
//
//    private void deriveQuestion(Term y, Deriver d) {
//        NALTask X = d.premise.task();
//        long xs = X.start(); //mid?
//        if (xs == ETERNAL)
//            xs = d.now();
//
//        int dither = d.ditherDT;
//
//        float dur = d.dur();
//        //long dt = Math.max(1, Math.max(d.nar.dtDither(), Math.round((xe-xs) + dur * 2)));
//        long dt = Math.max(1, Math.max(dither, Math.round(dur)));
//
//        //long s = tMid - window/2, e = tMid + window/2;
//        long s = xs - dt, e = xs;
//        if (dither > 1) {
//            s = Tense.dither(s, dither); e = Tense.dither(e, dither);
//        }
//
//        NALTask z = d.nar.answer(y, X.punc(), s, e);
//        if (z!=null)
//            d.add(z.why(why.ID));
//
//    }
//
//}