/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.premise;

import jcog.Util;
import nars.*;
import nars.deriver.reaction.Reaction;
import nars.link.MutableTaskLink;
import nars.task.proxy.SpecialTermTask;
import nars.term.Compound;
import nars.term.CondAtomic;
import nars.term.Neg;
import nars.term.Termed;
import nars.term.util.transform.VariableShift;
import nars.truth.Stamp;
import org.jetbrains.annotations.Nullable;

import static nars.$.quote;
import static nars.Op.*;

/**
 * single or double premise ready for derivation
 */
public abstract class NALPremise extends AbstractPremise {

    public static SeedTaskPremise the(NALTask task) {
        return new SeedTaskPremise(task);
    }

    public static NALPremise the(final NALTask task, final @Nullable Termed _belief, boolean varShift) {
        if (_belief == null)
            return the(task); //HACK

        if (NAL.DEBUG)
            validate(task, _belief);

        Term taskTerm = task.term();

        Termed belief = belief(taskTerm, _belief, varShift);

//        if (!seed && (belief instanceof Term bt) && bt!=taskTerm && bt.equalsRoot(taskTerm) )
//            belief = taskTerm; //root equals, so copy from task (SELF)

        int hash = Util.hashCombine(task.hashCode(), belief.hashCode());

        return belief instanceof NALTask beliefTask ?
            new DoubleTaskPremise(task, beliefTask, hash) :
            new SingleTaskPremise(task, (Term) belief, hash);
    }

    public final NALTask task;

    private NALPremise(NALTask task, int hash) {
        super(hash);
        this.task = task;
    }

    public static NALPremise nalPremise(Premise p) {
        return p instanceof NALPremise P ? P : ((NALPremise)(p.parent));
    }

    @Override
    public final void run(Deriver d) {
        d.model.what.test(d);


        if (belief()!=null) {
            if (!this.self()) {
                //questions/quests/goals seem to need this.
                //  i think it's because how premise belief component can only be belief, so questions/quests/goals can never fit that role.
                if (task().QUESTION_OR_QUEST())
                    link(d);
            }
        }

        //if (!this.self() && (belief()!=null || linkable(d)))
        //if (!this.self() && belief()!=null)
    }

    private void link(Deriver d) {
        d.link(MutableTaskLink.link(from(), to()).priPunc(task.punc(), this, d));
    }


    /** TODO */
    @Override public Reaction reaction() {
        return null;
    }

    @Override
    public Term term() {
        return quote(toString());
    }

//    public static SingleTaskPremise store(NALTask x) {
//        final TaskPremise t = TaskPremise.the(x, x.term(), false, null);
//        ((SingleTaskPremise) t).store = true;
//        return (SingleTaskPremise) t;
//    }

    private static Termed belief(Term task, Termed belief, boolean varShift) {
        if (belief instanceof NALTask)
            return belief;
        else {
            Term beliefTerm = ((Term) belief).unneg();
            return varShift ? VariableShift.varShift(beliefTerm, task, false, false)
                : beliefTerm;
        }
    }

    private static void validate(NALTask t, @Nullable Termed b) {
        assert (b==null || !t.equals(b));
        assert (!t.COMMAND());
        assert ((b == null)
                    ||
                (b instanceof Term && (!(b instanceof Neg)))
                    ||
                (b instanceof NALTask B && !t.equals(b) && B.BELIEF()));

        if (!(t instanceof SpecialTermTask)) //HACK for ImageAlign.java
            NALTask.TASKS(t.term(), t.punc(), false);

        if (b instanceof Task B)
            if (!(b instanceof SpecialTermTask)) //HACK for ImageAlign.java
                NALTask.TASKS(b.term(), B.punc(), false);
    }

    @Override
    public final Term from() {
        return task.term();
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (getClass() != obj.getClass()) return false;
        var p = (NALPremise) obj;
        return hash == p.hash && task.equals(p.task) && beliefTermed().equals(p.beliefTermed());
    }

    public abstract Termed beliefTermed();

    public final Term beliefTerm() {
        return to();
    }

    @Override
    public final NALTask task() {
        return task;
    }

    @Override
    @Nullable public abstract NALTask belief();

    public abstract boolean overlapDouble();

    public abstract long[] stamp(byte concPunc);

    public abstract boolean single();

    public abstract boolean invalid(Deriver deriver);

    public long taskStart() { return task.start(); }
    public long taskEnd() { return task.end(); }

    protected long[] taskStamp() { return task.stamp(); }

    public static class SeedTaskPremise extends NALPremise {

//        /** generic context object, for use by Derivers */
//        public Object ctx;

        private SeedTaskPremise(NALTask t) {
            super(t, t.hashCode());
        }

        @Override
        public String toString() {
            return task.toString();
        }

        @Override
        public Termed beliefTermed() {
            return to();
        }

        @Override
        public Term to() {
            return task.term();
        }

        @Override public @Nullable NALTask belief() {
            return null;
        }

        @Override
        public boolean single() {
            return true;
        }

        @Override
        public boolean overlapDouble() {
            return false;
        }

        @Override
        public long[] stamp(byte punc) {
            return taskStamp();
        }

        @Override
        public boolean invalid(Deriver d) {
            return d.invalid(task);
        }
    }

    public abstract static class NonSeedTaskPremise extends NALPremise {

        transient public boolean structural;

        private NonSeedTaskPremise(NALTask task, int hash) {
            super(task, hash);
        }

        public boolean isTemporal() {
            Term taskTerm, beliefTerm;
            NALTask task = this.task;
            Termed B = beliefTermed();
            return !task.ETERNAL() ||
                    (!single() && task != B && (B!=null && !((NALTask) B).ETERNAL())) ||
                    temporal(taskTerm = from()) ||
                    (!taskTerm.equals(beliefTerm = B.term()) && temporal(beliefTerm));
        }

        /**
         * whether a term is 'temporal' and its derivations need analyzed by the temporal solver:
         * if there is any temporal terms with non-DTERNAL dt()
         */
        @Deprecated
        public static boolean temporal(Term x) {
            if (!(x instanceof Compound c) || x instanceof CondAtomic)
                return false;

            int o = c.opID;
            if (o == CONJ.id || o == IMPL.id) { //x.TEMPORAL()
                if (dtSpecial(c.dt())) return true;
            }

            return hasAny(c.structSubs(), Temporals) &&
                    c.OR(NonSeedTaskPremise::temporal);
        }

    }

    public static class SingleTaskPremise extends NonSeedTaskPremise {
        public final Term belief;

        SingleTaskPremise(NALTask task, Term belief, int hash) {
            super(task, hash);
            this.belief = belief;
        }

        @Override
        public boolean overlapDouble() {
            return false;
        }

        @Override
        public long[] stamp(byte concPunc) {
            return taskStamp();
        }

        @Override
        public boolean single() {
            return true;
        }

        @Override
        public Termed beliefTermed() {
            return to();
        }

        @Override
        public @Nullable NALTask belief() {
            return null;
        }

        @Override
        public boolean invalid(Deriver d) {
            return d.invalid(task) || d.invalidVol(belief);
        }

        @Override
        public Term to() {
            return belief;
        }

        @Override
        public String toString() {
            return task + (
                belief.equals(task.term()) ? "" : (" >> " + belief)
            );
        }

    }

    public static final class DoubleTaskPremise extends NonSeedTaskPremise {
        public final NALTask belief;
        public boolean overlapDouble;

        private long[] stampDouble;

        private DoubleTaskPremise(NALTask task, NALTask belief, int hash) {
            if (!belief.BELIEF())
                throw new RuntimeException("belief must be punctuation belief");
            super(task, hash);
            this.belief = belief;
            this.overlapDouble = NAL.premise.OVERLAP_DOUBLE_MODE.overlapping(this.task, this.belief);
        }

        @Override
        public boolean invalid(Deriver d) {
            return d.invalid(task) || d.invalid(belief);
        }

        @Override
        public String toString() {
            return task + " >> " + belief;
        }

        @Override
        public Term to() {
            return belief.term();
        }

        @Override
        public Termed beliefTermed() {
            return belief;
        }

        public long beliefStart() { return belief.start(); }

        public long beliefEnd() {
            return belief.end();
        }

        @Override
        public boolean single() {
            return false;
        }

        @Override @Nullable
        public NALTask belief() {
            return belief;
        }

        @Override
        public boolean overlapDouble() {
            return overlapDouble;
        }

        @Override public long[] stamp(byte concPunc) {
            if (stampDouble == null) {
                long[] beliefStamp = belief.stamp();
                stampDouble = !NAL.premise.QUESTION_TO_BELIEF_ZIP && QUESTION_OR_QUEST(task.punc()) && !QUESTION_OR_QUEST(concPunc) ?
                    beliefStamp : //special case: (question,belief)->belief
                    Stamp.zip(taskStamp(), beliefStamp, task, belief);  //lazy
            }

            return stampDouble;
        }

    }

}