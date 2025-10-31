package nars;

import jcog.pri.UnitPri;
import nars.task.SerialTask;
import nars.term.Termed;
import nars.term.atom.Atom;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.hashCombine;
import static jcog.math.LongInterval.ETERNAL;
import static nars.Op.*;

/**
 * NAL Task to be processed, consists of a Sentence, stamp, time, and budget.
 */
public abstract class Task extends UnitPri implements Termed {

//    public static final Task[] EmptyTaskArray = new Task[0];


    public static final Atom BeliefAtom = $.quote(String.valueOf((char)BELIEF));
    public static final Atom GoalAtom =  $.quote(String.valueOf((char)GOAL));
    public static final Atom QuestionAtom =  $.quote(String.valueOf((char)QUESTION));
    public static final Atom QuestAtom =  $.quote(String.valueOf((char)QUEST));
    public static final Atom CommandAtom =  $.quote(String.valueOf((char)COMMAND));


//    /**
//     * fast, imprecise sort.  for cache locality and concurrency purposes
//     */
//    public static final Comparator<? extends Task> sloppySorter = Comparator
//            .comparingInt((Task x) ->
//                    x.term().concept().hashCode())
//            .thenComparing(x -> -x.priElseZero());

//    public static void fund(Task y, Task x, float factor, boolean copyOrMove) {
//        if (!copyOrMove)
//            throw new TODO();
//
////        Task.fund(y, new Task[] { x }, copyOrMove);
////        y.priMult(factor);
//        Task.merge(y, new Task[] { x }, factor * x.priElseZero());
//
//
////        //discount pri by increase in target complexity
////        float xc = xx.voluplexity(), yc = yy.voluplexity();
////        float priSharePct =
////                1f - (yc / (xc + yc));
////        yy.pri(0);
////        yy.take(xx, priSharePct * factor, false, copyOrMove);
//    }

    public abstract byte punc();

    public final int complexity() {
        return term().complexity();
    }

    public final Op op() {
        return Op.op(opID());
    }

    public final int opID() { return term().opID(); }

    @Override
    public boolean equals(Object that) {
        return this == that
            ||
                that instanceof Task
                    && !(that instanceof SerialTask) //HACK
                    && hashCode() == that.hashCode()
                    && equal(this, (Task) that);
    }

    /**
     * see equals()
     */
    protected static int hash(Term term, @Nullable Truth truth, byte punc, long start, long end, long[] stamp) {
        int h = hashCombine(term, punc);

        if (truth != null)
            h = hashCombine(h, truth);

        if (start != ETERNAL)
            h = hashCombine(h, start, end);

        h = hashCombine(h, stamp);

        //keep 0 as a special non-hashed indicator value
        return h != 0 ? h : 1;
    }

    /**
     * assumes identity and hash have been tested already.
     * <p>
     * if evidence is of length 1 (such as input or signal tasks,), the system
     * assumes that its ID is unique (or most likely unique)
     * and this becomes the only identity condition.
     * (start/stop and truth are not considered for equality)
     * this allows these values to mutate dynamically
     * while the system runs without causing hash or equality
     * inconsistency.  see hash()
     */
    protected static boolean equal(Task a, Task b) {
        byte p = a.punc();
        if (p != b.punc())
            return false;

        return (p==COMMAND || ((NALTask)a).equalNAL((NALTask)b))
               &&
               a.term().equals(b.term());
    }


//    static StableBloomFilter<Task> newBloomFilter(int cap, Random rng) {
//        return new StableBloomFilter<>(
//                cap, 1, 0.0005f, rng,
//                new BytesHashProvider<>(IO::taskToBytes));
//    }

    public void proof(int indent, StringBuilder sb) {
        appendTo(sb.append("  ".repeat(Math.max(0, indent))), true).append("\n  ");
    }

    public final boolean QUESTION() {
        return punc() == QUESTION;
    }

    public final boolean BELIEF() {
        return punc() == BELIEF;
    }

    public final boolean GOAL() {
        return punc() == GOAL;
    }

    public final boolean QUEST() {
        return punc() == QUEST;
    }

    public final boolean COMMAND() {
        return punc() == COMMAND;
    }

    public final boolean QUESTION_OR_QUEST() {
        return Op.QUESTION_OR_QUEST(punc());
    }

    public final boolean BELIEF_OR_GOAL() {
        return Op.BELIEF_OR_GOAL(punc());
    }

    @Nullable
    public Appendable toString(boolean showStamp) {
        return appendTo(new StringBuilder(128), showStamp);
    }


    public StringBuilder appendTo(@Nullable StringBuilder sb) {
        return appendTo(sb, false);
    }

    public String toStringWithoutBudget() {
        return appendTo(new StringBuilder(32)).toString();
    }


    @Deprecated
    public StringBuilder appendTo(StringBuilder buffer, boolean showStamp) {
        return term().appendTo(buffer);
    }


    @Deprecated public Task pri(NAL n) {
        pri(n.priDefault(punc()));
        return this;
    }


}