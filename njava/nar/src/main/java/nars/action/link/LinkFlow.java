//package nars.action.link;
//
//import jcog.pri.bag.Bag;
//import jcog.pri.op.PriMerge;
//import jcog.signal.IntRange;
//import nars.Term;
//import nars.Deriver;
//import nars.derive.reaction.NativeReaction;
//import nars.link.flow.LinkFlows;
//import nars.Premise;
//
//import java.util.function.BiPredicate;
//import java.util.function.Function;
//
///**
// * TODO parameters for punc inputs and outputs. currently this is hardcoded for goal->goal
// * TODO use TermAdjacency
// */
//public class LinkFlow extends NativeReaction {
//
////    public final PuncPri punc = new PuncPri(0, 2);
//    private final Function<Premise, BiPredicate<Term /* yF */, Term /* yT */>> matcher;
//
//    /**
//     * computes punctuation priority activation,
//     * for the given input tasklink,
//     * to be applied to matches
//     *
//     * ex: l.priGet(punc) */
//    private final Function<Premise, float[]> pri;
//
//    public PriMerge merge;
//
//    /** TODO FloatRange with random fraction */
//    public final IntRange spread = new IntRange(1, 1, 8);
//
//    public LinkFlow(Function<Premise, BiPredicate<Term, Term>> matcher, Function<Premise, float[]> pri, PriMerge merge) {
//        //tasklink();
//        //taskEqualsBelief();
//        single();
//        this.matcher = matcher;
//        this.pri = pri;
//        this.merge = merge;
//    }
//
//    @Override
//    protected void run(Deriver d) {
//        flow(d.premise.parent, d.focus.links, spread.getAsInt(), d);
//        //flow(TaskLink.parentLink(d.premise), d.focus, spread.getAsInt(), d.random);
//    }
//
////    public void flow(TaskLink l, Focus f, int max, Random rng) {
////        flow(l, f.links, max, rng, d.focus);
////    }
//
//    public void flow(Premise p, Bag<? extends Premise, ? extends Premise> b, int max, Deriver d) {
//        float[] r = pri.apply(p);
//        if (r!=null)
//            LinkFlows.flow(matcher, p, r, merge, b, max, d.rng, d);
//    }
//
//
//
//
//}