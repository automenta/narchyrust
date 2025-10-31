package nars.deriver.op;

import jcog.Fuzzy;
import jcog.data.map.UnifriedMap;
import jcog.math.Intervals;
import nars.Deriver;
import nars.NAL;
import nars.Term;
import nars.premise.NALPremise;
import nars.term.Compound;
import nars.term.atom.Atomic;
import nars.truth.MutableTruthInterval;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static nars.Op.*;

public enum DerivedOccurrence {
    Task() {

    },

    Belief() {
        @Override
        public boolean taskOrBelief(Deriver d) {
            return false;
        }
    },

    /** task range + seqDur (assuming its CONJ), if it's non-point */
    TaskSeq() {
        @Nullable
        @Override
        protected void when(NALPremise i, byte punc, Deriver d, MutableTruthInterval t) {
            var tt = i.task;
            var ts = tt.start();
            if (ts != ETERNAL) {
                var te = tt.end() + tt.term().seqDur();
                if (ts != te)
                    t.occurr(ts, te);
            }
        }
    },

    /** smudges derivation to the union of the premise tasks.  resulting truth is diluted in proportion to any non-intersecting range  */
    Union() {
        @Override
        protected void when(MutableTruthInterval t, long ts, long te, long bs, long be, byte punc, Deriver d) {
            t.occurr(Math.min(ts, bs), Math.max(te, be));
        }
    },

    Intersection() {
        @Override
        protected void when(MutableTruthInterval t, long ts, long te, long bs, long be, byte punc, Deriver d) {
            var i = Intervals.intersectionRaw(ts, te, bs, be);
            if (i == null)
                t.clear(); //fail
            else
                t.occurr(i[0], i[1]);
        }
    },

    /**
     * dynamically choose either TaskRel and BeliefRel
     */
    EitherRel() {

        @Override
        public boolean taskOrBelief(Deriver d) {
            return d.randomBoolean();
        }

        @Override
        public boolean relative() {
            return true;
        }
    },

    TaskRel() {
        @Override
        public boolean relative() {
            return true;
        }
    },

    BeliefRel() {
        @Override
        public boolean relative() { return true; }

        @Override
        public boolean taskOrBelief(Deriver d) {
            return false;
        }
    },

    /** TODO not fully impl yet: works only with question derivations, see impl.strong.nal */
    Pre() {
        @Override
        protected void when(MutableTruthInterval t, long ts, long te, long bs, long be, byte punc, Deriver d) {
            prePost(false, ts, te, bs, be, (NALPremise) d.premise, t);
        }
    },

    /** TODO not fully impl yet: works only with question derivations, see impl.strong.nal */
    Post() {
        @Override
        protected void when(MutableTruthInterval t, long ts, long te, long bs, long be, byte punc, Deriver d) {
            prePost(true, ts, te, bs, be, (NALPremise) d.premise, t);
        }
    },

//    /** custom solver for goal-based conjunction decomposition */
//    CondAfter() {
//        @Override
//        public boolean relative() {
//            return true;
//        }
//
//        @Override
//        public Pair<Term, MutableTruthInterval> solve(NALPremise i, Term x, MutableTruthInterval t, byte punc, Deriver d) {
//            return conjShift(i, x, false, t, d);
//        }
//
//        @Override protected boolean taskOrBelief(Deriver d) {
//            return false; //BELIEF
//        }
//    },

    NearestNow() {
        @Override
        public boolean relative() {
            return true;
        }

        @Override
        public boolean taskOrBelief(Deriver d) {
            return nearestNow(d);
        }

        private static boolean nearestNow(Deriver d) {
            var P = (NALPremise) d.premise;

            var now = d.now();
            var t = P.task.timeBetweenTo(now);//P.taskMid();

            var b = P.belief().timeBetweenTo(now); //, P.beliefMid();

            //return abs(now - t) <= abs(now - b);
            return t <= b;
        }
    },

    Later() {
        @Override
        protected void when(MutableTruthInterval t, long ts, long te, long bs, long be, byte punc, Deriver d) {
            var start = Math.max(ts, bs);
            var range =
                    //te - ts; //task's range
                    Math.min(te - ts, be - bs); //min range
            t.occurr(start, start + range);
        }

        @Override
        public boolean taskOrBelief(Deriver d) {
            return later((NALPremise.DoubleTaskPremise)NALPremise.nalPremise(d.premise));
        }

        @Override
        public boolean relative() {
            return true;
        }

        /** choose the later of the two premise tasks.
         @return true = Task, false = Belief */
        private static boolean later(NALPremise.DoubleTaskPremise p) {
            return p.taskStart() >= p.beliefStart();
        }

    },

    TaskBeliefSpan() {
        @Override
        protected void when(MutableTruthInterval t, long ts, long te, long bs, long be, byte punc, Deriver d) {
            var beliefSpan = d.premise.to().seqDur();
            var taskSpan = te - ts;
            if (beliefSpan > taskSpan)
                t.occurr(Intervals.range(Fuzzy.mean(ts,te), beliefSpan));
            else
                t.occurr(ts, te);
        }
    },
    ;


    private static void prePost(boolean fwd, long ts, long te, long bs, long be, NALPremise p, MutableTruthInterval out) {

        var tt = p.from();
        var tImpl = tt.IMPL();
        var bb = p.to();
        var bImpl = bb.IMPL();
        assert(tImpl ^ bImpl);

        boolean occTB;
        if (bs == TIMELESS) occTB = true;
        else if (ts == ETERNAL) occTB = false;
        else if (bs == ETERNAL) occTB = true;
        else occTB = bImpl;

        long s, e;
        if (occTB) { s = ts; e = te; } else { s = bs; e = be; }

        if (s!=ETERNAL) {
            var impl = (Compound) (tImpl ? tt : bb);
            var idt = impl.dt();
            if (idt!=XTERNAL) {
                if (idt==DTERNAL) idt = 0;
                int shift;
                if (fwd) shift = idt;
                else     shift = -idt - impl.seqDurSub(0, false);
                s += shift;
                e += shift;
            }
        }
        out.occurr(s, e);
    }


    public static final Map<Term, DerivedOccurrence> solvers;

    static {
        var tm = new UnifriedMap<Term, DerivedOccurrence>();
        for (var m : values())
            tm.put(Atomic.atomic(m.name()), m);
        tm.trimToSize();
        solvers = tm;
    }

    public final Atomic term;

    DerivedOccurrence() {
        this.term = Atomic.atom(name());
    }

    /**
     * whether full occurrence solving is involved
     */
    public boolean relative() {
        return false;
    }

    /**
     *
     * @param i the premise which the occurrence is being derived from
     * @param punc the punctuator for the premise (e.g. question mark)
     * @param d the current context deriver
     * @return a new occurrence interval, or `null` if the result is invalid.
     */
    public final MutableTruthInterval when(NALPremise i, byte punc, Deriver d) {
        var t = new MutableTruthInterval();
        when(i, punc, d, t);
        return t.s == TIMELESS ? null : t;
    }

    /** if overriding, handle ETERNAL cases */
    protected @Nullable void when(NALPremise i, byte punc, Deriver d, MutableTruthInterval t) {
        long ts = i.taskStart(), te = i.taskEnd();
        if (i instanceof NALPremise.DoubleTaskPremise dp) {
            var bs = dp.beliefStart();
            if (bs != ETERNAL) {
                var be = dp.beliefEnd();
                if (ts == ETERNAL) {
                    t.occurr(bs, be);
                } else {
                    when(t, ts, te, bs, be, punc, d); //BOTH temporal, solve
                }
                return;
            }
        }

        t.occurr(ts, te);
    }


    /** implementations need to call 'o.occurr()' with the conclusion time */
    protected void when(MutableTruthInterval t, long ts, long te, long bs, long be, byte punc, Deriver d) {
        var taskOrBelief = taskOrBelief(d);
        t.occurr(occurrence(
            taskOrBelief ? new long[] {ts, te} : new long[] {bs, be},
            ts, te, bs, be, taskOrBelief, d));
    }

    private static long[] occurrence(long[] SE, long ts, long te, long bs, long be, boolean taskOrBelief, Deriver d) {
        if (NAL.premise.OCC_TRIM)
            Intervals.shrink(SE, taskOrBelief ? bs : ts, taskOrBelief ? be : te);

        return SE;
    }

    /** whether the task (true) or belief (false) is the dominant temporal target
     * @param d*/
    public boolean taskOrBelief(Deriver d) {
        return true; //default
    }
}