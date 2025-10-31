package nars.link;

import jcog.data.bit.FixedPoint;
import jcog.data.graph.path.FromTo;
import jcog.decide.Roulette;
import jcog.pri.Prioritizable;
import jcog.pri.UnitPri;
import jcog.pri.op.PriMerge;
import jcog.util.PriReturn;
import nars.Deriver;
import nars.NALTask;
import nars.Premise;
import nars.Term;
import nars.task.util.PuncBag;
import org.jetbrains.annotations.Nullable;

import java.util.random.RandomGenerator;

import static java.lang.Math.max;
import static nars.Op.*;

/**
 * the function of a tasklink is to be a prioritizable strategy for resolving a Task in a NAR.
 * this does not mean that it must reference a specific Task but certain properties of it
 * that can be used ot dynamically match a Task on demand.
 * <p>
 * note: seems to be important for Tasklink to NOT implement Termed when use with common Map's with Termlinks
 *
 * (b q g Q) = (belief question goal quest)
 *
 * note: these arent exactly "task"-links, since they do not refer to a specific
 * task - but rather a 'type' of task.  maybe a more appropriate name here
 * would be 'SentenceLink'
 */
public abstract class TaskLink extends Premise implements Prioritizable, FromTo<Term, Premise> {

    public static final double TaskLinkEpsilon = FixedPoint.Epsilon16;
    public static final float TaskLinkEpsilonF = (float) FixedPoint.Epsilon16;

    public static TaskLink parentLink(Premise p) {
        //return p instanceof TaskLink ? (TaskLink) p : (TaskLink) p.parent;
        while (!(p instanceof TaskLink)) {
            if (p == null) return null;
            p = p.parent;
        }
        return (TaskLink) p;
    }

    @Override
    public void run(Deriver d) {
        throw new UnsupportedOperationException();
    }

    public /* final */ float priPunc(byte punc) { return priComponent(NALTask.i(punc)); }

    /** index will be either 0, 1, 2, or 3 */
    public abstract float priComponent(byte c);

    abstract public float merge(TaskLink incoming, PriMerge merge, PriReturn r);
//    public float merge(TaskLink incoming, PriMerge merge, PriReturn r) {
//        float y = merge.apply(this, incoming.pri(), r);
//        //mergeWhy(incoming);
//        return y;
//    }

    final UnitPri pri = new UnitPri(Float.NaN);

    @Override public final float pri() {
        return pri.pri();
    }

    @Override public void pri(float p) {
        pri.pri(p);
    }

    /**
     * sample punctuation by relative priority
     * returns 0 for none
     */
    public byte punc(RandomGenerator rng) {
        return punc(rng, priGet());
    }

    public byte punc(PuncBag mul, RandomGenerator rng) {
        return punc(rng, mul.mul(priGet()));
    }

    private static byte punc(RandomGenerator rng, float[] p) {
        var i = Roulette.selectRoulette(rng, p);
        return NALTask.p(i == - 1 ? rng.nextInt(p.length) : i);
    }

//    public byte punc(@Nullable PuncBag punc, RandomGenerator rng) {
//        Term x = from();
//
//        float[] pp = priGet(); //beliefs, goals, questions, quests
//        if (punc!=null)
//            punc.mul(pp);
//
//        boolean b = true, g = true, q = true, Q = true;
//        if (x.hasAny(VAR_QUERY.bit | VAR_PATTERN.bit))
//            b = g = false;
//
//        if (g && !x.op().goalable)
//            g = Q = false;
//
//
//        //channel unused priority to used channels
//        if (!b) {
//            //assert(q);
//            pp[1] += pp[0];
//            pp[0] = 0;
//        }
//        if (!g) {
//            if (b) pp[0] += pp[2];
//            else /*if (Q)*/ { /* assert(Q); */ pp[3] += pp[2]; }
//            pp[2] = 0;
//        }
//
//        if (!Q) { /* assert(q); */
//            pp[1] += pp[3];
//            pp[3] = 0;
//        }
//
//        switch (punc(rng, pp)) {
//            case BELIEF   -> {if (b) return BELIEF;}
//            case QUESTION -> {if (q) return QUESTION;}
//            case GOAL     -> {if (g) return GOAL;}
//            case QUEST    -> {if (Q) return QUEST;}
//        }
//
//        if (!b /*&& !g*/) {
//            return rng.nextBoolean() ? QUESTION : QUEST;
//        } else if (!g/* && !Q*/)
//            return rng.nextBoolean() ? BELIEF : QUESTION;
//        else {
//            return switch (rng.nextInt(4)) {
//                case 0 -> BELIEF;
//                case 1 -> QUESTION;
//                case 2 -> GOAL;
//                case 3 -> QUEST;
//                default -> (byte)-1;
//            };
//        }
//    }

    public abstract void delete(byte punc);


    /**
     * clones a new float[4] { beliefs, goals, questions, quests }
     */
    public float[] priGet() {
        var b = new float[4];
        priGet(b);
        return b;
    }

    public float[] priGet(float[] buf) {
        buf[0] = priPunc(BELIEF);
        buf[1] = priPunc(QUESTION);
        buf[2] = priPunc(GOAL);
        buf[3] = priPunc(QUEST);
        return buf;
    }

//    public void priMask(boolean b, boolean q, boolean g, boolean Q) {
//        throw new TODO();
//    }

//    /** which component is strongest */
//    public byte puncMax() {
//        return switch (Util.maxIndex(priPunc(BELIEF), priPunc(QUESTION), priPunc(GOAL), priPunc(QUEST))) {
//            case 0 -> BELIEF;
//            case 1 -> GOAL;
//            case 2 -> QUESTION;
//            case 3 -> QUEST;
//            default -> -1;
//        };
//    }


//    /** special tasklink for signals which can stretch and so their target time would not correspond well while changing */
//    class DirectTaskLink extends PLinkUntilDeleted<Task> implements TaskLink {

    @Override
    public final NALTask task() {
        return null;
    }

    @Override
    public final @Nullable NALTask belief() {
        return null;
    }

    public final float[] priGet(PuncBag mult) {
        var X = priGet();
        X[0] *= mult.belief.floatValue();
        X[1] *= mult.question.floatValue();
        X[2] *= mult.goal.floatValue();
        X[3] *= mult.quest.floatValue();
        return X;
    }

    public final int volMax() {
        Term f = from(), t = to();
        var fv = f.complexity();
        return f == t ? fv : max(fv, t.complexity());
    }

    public final int complexitySum() {
        Term f = from(), t = to();
        var fv = f.complexity();
        return f == t ? fv : fv + t.complexity();
    }

}