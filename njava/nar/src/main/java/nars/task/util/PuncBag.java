package nars.task.util;

import jcog.decide.Roulette;
import jcog.signal.FloatRange;
import nars.NALTask;
import nars.Task;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.random.RandomGenerator;

import static nars.Op.*;

/** bag of punctuations (fixed set of them) */
public class PuncBag implements FloatFunction<NALTask> /* TODO FixedBag<..> */ {

    public final FloatRange belief, question, goal, quest;

    public PuncBag(float v) {
        this(v, 0, 1);
    }

    public PuncBag(float v, float min, float max) {
        this(v, v, v, v, min, max);
    }

    public PuncBag(float b, float q, float g, float Q, float min, float max) {
        belief = new FloatRange(1, min, max);
        question = new FloatRange(1, min, max);
        goal = new FloatRange(1, min, max);
        quest = new FloatRange(1, min, max);
        set(b, q, g, Q);
    }


    public final float apply(Task t) {
        return apply(t.punc());
    }

    public float apply(byte punc) {
        return (switch (punc) {
            case BELIEF -> belief;
            case GOAL -> goal;
            case QUESTION -> question;
            case QUEST -> quest;
            default -> throw new UnsupportedOperationException();
        }).floatValue();
    }

    @Override
    public float floatValueOf(NALTask t) {
        return apply(t);
    }

    /** see: NALTask.i(x) */
    private float component(int index) {
        return (switch (index) {
            case 0 -> belief;
            case 1 -> question;
            case 2 -> goal;
            case 3 -> quest;
            default -> throw new UnsupportedOperationException();
        }).floatValue();
    }

    public byte sample(RandomGenerator r) {
        int i = Roulette.selectRoulette(4, this::component, r);
        return i == -1 ? 0 : NALTask.p(i);
    }

    /**
     * beliefs, goals, questions, quests.  modifies the input array
     *
     * @return
     */
    public float[] mul(float[] p) {
        p[0] *= apply(BELIEF);
        p[1] *= apply(GOAL);
        p[2] *= apply(QUESTION);
        p[3] *= apply(QUEST);
        return p;
    }

    public final PuncBag set(float x) {
        return set(x, x, x, x);
    }

    public final PuncBag set(float x, float[] snapshot) {
        return set(x * snapshot[0], x * snapshot[1], x * snapshot[2], x * snapshot[3]);
    }

    public final PuncBag set(float bg, float qq) {
        return set(bg, qq, bg, qq);
    }

    public final PuncBag set(float b, float q, float g, float Q) {
        belief.set(b);
        question.set(q);
        goal.set(g);
        quest.set(Q);
        return this;
    }

    public float[] snapshot() {
        return new float[] {
            component(0), component(1), component(2), component(3)
        };
    }
}