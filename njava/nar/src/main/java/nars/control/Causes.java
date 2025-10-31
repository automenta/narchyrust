package nars.control;

import jcog.Is;
import jcog.data.list.Lst;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;

import java.util.ArrayDeque;
import java.util.stream.Stream;

/**
 * NAR Control
 * low-level system control interface
 * <p>
 * for guiding and specialize system activity and capabilities
 * useful for emulating motivation, instincts, etc
 * <p>
 * <p>
 * -- "activity equalizer": fixed-size bipolar control vetor for real-time behavior trajectory control
 * (TODO dynamically pluggable) control metrics
 * -- registers/unregisters plugins (causables). each is assigned a unique 16-bit (short) id.
 * -- profiles and measures the performance of plugins when used, with respect to current system objectives
 * --incl. rmeote logger, etc
 * -- decides future prioritization (and other parametrs) of each plugin through an abstract
 * reinforcement learning control interface
 */
@Is("Multi-armed_bandit")
public final class Causes {

    /**
     * list of known internal reasons for
     * why any mental activity could have happened.
     * for use in forming causal tracing
     */
    public final Lst<Cause> why = new Lst<>(0, new Cause[512]);

//    public MetaGoal.Report stats(PrintStream out) {
//        MetaGoal.Report r = new MetaGoal.Report();
//        r.add(why);
//        r.print(out);
//        return r;
//    }
    public <X> ArrayDeque<Cause<X>> newCauses(Stream<X> x) {
        ArrayDeque<Cause<X>> a = new ArrayDeque<>();
        synchronized(why) {
            x.forEach(xx -> a.add(newCause(xx)));
        }
        return a;
    }

    public Cause newCause(Object name) {
        return newCause((id) -> new Cause(id, name));
    }

    public <C extends Cause<?>> C newCause(ShortToObjectFunction<C> idToChannel) {
        synchronized (why) {
            short next = (short) why.size();
            if (next < 0) throw new UnsupportedOperationException("too many causes");

            C c = idToChannel.valueOf(next);
            why.add(c);
            return c;
        }
    }

//    public final void learn(int feature, Caused why, float v, NAR n) {
//        model.learn(feature, why.why(), v, n);
//    }
//
////    public final void learn(int feature, Term why, float v, NAR n) {
////        model.learn(feature, why, v, n);
////    }
//
//    public final void learn(int feature, short why, float v) {
//        model.learn(why, feature, v);
//    }




//    public void learn(NALTask x, NAR nar) {
//
//        byte punc = x.punc();
//        MetaGoal m = switch (punc) {
//            case BELIEF -> MetaGoal.Believe;
//            case GOAL -> MetaGoal.Goal;
//            case QUESTION, QUEST -> MetaGoal.Question;
//            default -> null;
//        };
//
//        if (m!=null) {// && !Util.equals(n.emotion.want[m.id], 0))
//            learn(m.ordinal(), x, (punc == BELIEF || punc == GOAL) ?
//                    (float) NAL.valueBeliefOrGoal(x, nar) :
//                    NAL.valueQuestion(x), nar);
//        }
//    }
}