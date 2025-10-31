package nars;

import jcog.data.byt.DynBytes;
import jcog.io.BinTxt;
import jcog.math.LongInterval;
import jcog.pri.op.PriMerge;
import jcog.tree.rtree.HyperRegion;
import jcog.util.ArrayUtil;
import jcog.util.PriReturn;
import nars.io.IO;
import nars.task.*;
import nars.task.proxy.SpecialTruthAndOccurrenceTask;
import nars.task.util.TaskException;
import nars.task.util.TaskRegion;
import nars.task.util.ValidIndepBalance;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Termed;
import nars.term.Termlike;
import nars.term.util.Image;
import nars.term.util.TermException;
import nars.term.util.TermTransformException;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.var.Variable;
import nars.time.Moment;
import nars.time.Tense;
import nars.truth.*;
import nars.truth.evi.EviInterval;
import nars.truth.evi.EviProjector;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Predicate;

import static java.lang.System.identityHashCode;
import static nars.Op.*;


/**
 * base class for non-axiomatic logic tasks
 */
public abstract class NALTask extends Task implements Truthed, Stamp, TaskRegion {

    /**
     * although this field is named creation,
     * it is used to store the last re-activation time for novelty filtering.
     * begins TIMELESS to signal an un-initialized state
     */
    private long creation = TIMELESS; //TODO protected or private

    public static NALTask taskUnsafe(Term term, byte punc, @Nullable Truth truth, long start, long end, long[] evidence) {
        var neg = term instanceof Neg;
        if (neg) term = term.unneg();
        if (truth!=null) truth = truth(truth, neg);

        return start == ETERNAL ?
            new EternalTask(term, punc, truth, evidence) :
            new TemporalTask(term, punc, truth, start, end, evidence);
    }


    public static NALTask task(Term x, byte punc, @Nullable Truth truth, LongInterval occ, long[] evidence) {
        return task(x, punc, truth, occ.start(), occ.end(), evidence);
    }

    public static NALTask task(Term x, byte punc, @Nullable Truth truth, long start, long end, long[] evidence) {
        var term = taskTerm(x, punc, true, false);

        taskValidOcc(start, end, term, false);

        taskValidTruth(truth, term, punc, false);

        taskValidStamp(evidence, term);

        return taskUnsafe(term, punc, truth, start, end, evidence);
    }

    @Override
    public float freq(long start, long end) {
        return truth().freq(start, end);
    }

    public static boolean taskValidOcc(long start, long end, Term x, boolean safe) {
        var startEternal = start == ETERNAL;

        if (!startEternal && end - start > NAL.belief.TASK_RANGE_LIMIT)
            return fail(x, "excessive range: " + (end - start), safe);

        if ((startEternal && end != ETERNAL) ||
                (start > end) ||
                (start == TIMELESS) || (end == TIMELESS))
            return fail(x, "start=" + start + ", end=" + end + " is invalid task occurrence time", safe);

        return true;
    }



    public static boolean taskValidTruth(@Nullable Truth tr, Term x, byte punc, boolean safe) {

        var beliefOrGoal = (punc == BELIEF) || (punc == GOAL);
        if (beliefOrGoal == (tr == null))
            return fail(x, "truth/punc mismatch", safe);

//        if (truth!=null && truth.conf() < NAL.truth.TRUTH_EPSILON)
//            throw new Truth.TruthException("evidence underflow: conf=", truth.conf());

        if (beliefOrGoal && tr.evi() < NAL.truth.EVI_MIN)
            return fail(x, "evidence underflow", safe);

        return true;
    }

    public static void taskValidStamp(long[] evidence, Term x) {
        if (NAL.test.DEBUG_EXTRA)
            if (!Stamp.validStamp(evidence))
                throw new TaskException("invalid stamp: " + Arrays.toString(evidence), x);
    }

    public static NALTask taskEternal(Term x, byte punc, @Nullable Truth t, NAL n) {
        return task(x, punc, t, ETERNAL, ETERNAL, n.evidence());
    }


    /** complete clone to immutable instance */
    public static NALTask clone(NALTask x) {
        return clone(x, x.term(), x.truth(), x.punc());
    }

    private static NALTask clone(NALTask x, Term newContent, Truth newTruth, byte punc) {
        return clone(x, newContent, newTruth, punc, x.start(), x.end());
    }

    private static NALTask clone(NALTask x, Term newContent, Truth newTruth, byte punc, long start, long end) {
        return clone(x, newContent, newTruth, punc, start, end, x.stamp(), true);
    }

    public static NALTask clone(NALTask x, Term newContent, Truth newTruth, byte punc, long start, long end, long[] stamp, boolean safe) {

        //Term c = Task.taskValid(newContent, newPunc, newTruth, safe);
        //return c==null ? null :
        var t = safe ?
                task(newContent, punc, newTruth,  /* HACK */ start, end, stamp) :
                taskUnsafe(newContent, punc, newTruth,  /* HACK */ start, end, stamp);

        t.pri(x.pri());
        return t;

//        if (x.target().equals(y.target()) && x.isCyclic())
//            y.setCyclic(true);
    }

    @Nullable
    public static NALTask eternalize(NALTask x, float eviRate, double eviMin) {
        if (x.ETERNAL())
            return x;

        var tt = x.truth();
        var tre = tt.eviEternalized(eviRate);
        return tre < eviMin ? null :
            SpecialTruthAndOccurrenceTask.proxy(x, PreciseTruth.byEvi(tt.freq(), tre), ETERNAL, ETERNAL);
    }

//    /**
//     * assumes start!=ETERNAL
//     * TODO boolean eternalize=false
//     */
//    public static @Nullable NALTask projectRelative(NALTask x, long s, long e, float dur, long now, float ete, double eviMin) {
//        return project(x, s, e, (t, S, E) -> t.truthRelative(S, E, dur, now, ete, eviMin));
//    }

    public static @Nullable NALTask projectAbsolute(NALTask x, long s, long e, float dur, float ete, double eviMin) {
//        if (x.contains(s, e))
//            return x; //don't shrink it

        //this should not be used for evi dilution to work:
        //        if (x.containedBy(s, e))
        //            return x; //don't reproject, task is already within target interval


        Truth tt;
        if (x.BELIEF_OR_GOAL()) {
            tt = x.truth(s, e, dur, ete, eviMin);
            if (tt == null)
                return null;
        } else
            tt = null;
        return SpecialTruthAndOccurrenceTask.proxy(x, tt, s, e).copyMeta(x);
    }

    public static float mergeInBag(NALTask existing, NALTask incoming) {
        return merge(existing, incoming, NAL.taskPriMerge, PriReturn.Result);
    }

    public static float merge(NALTask e, NALTask i, PriMerge merge, @Nullable PriReturn returning) {
        if (e == i)
            return returning!=null ? returning.apply(0, e.priElseZero(), e.priElseZero()) : Float.NaN;

        shareStamp(e, i, e.stamp(), i.stamp());

        //non-serial task, or proxy to non-serial task
//        if (NAL.truth.ABSORB_ON_TASK_MERGE && e.BELIEF_OR_GOAL() && e.truth() instanceof PreciseTruth ep)
//            ep.absorb(i.truth());

        //use the latest creation time
        var iCreation = i.creation();
        if (iCreation != TIMELESS && iCreation > e.creation())
            e.setCreation(iCreation);

        return merge.apply(e, i.pri(), returning);
    }


    /**
     * with most common defaults
     */
    public static void merge(NALTask pp, NALTask tt) {
        merge(pp, tt, NAL.taskPriMerge);
    }
    private static void merge(NALTask pp, NALTask tt, PriMerge merge) {
        merge(pp, tt, merge, null);
    }

    @Nullable public static Term taskTerm(Term x, byte punc) {
        return taskTerm(x, punc, true, !NAL.test.DEBUG_EXTRA);
    }

    @Nullable public static Term taskTerm(Term x, byte punc, boolean safe) {
        return taskTerm(x, punc, true, safe);
    }

    /**
     * validates and prepares a term for use as a task's content
     */
    @Nullable public static Term taskTerm(Term x, byte punc, boolean imageNormalize, boolean safe) {
        boolean neg = x instanceof Neg; if (neg) x = x.unneg();

        var y = x instanceof Compound X ? taskTermCompound(X, imageNormalize, safe) : x;
        return TASKS(y, punc, safe) ?
            (neg ? y.neg() : y) : null;
    }

    @Nullable private static Term taskTermCompound(Compound x, boolean imageNormalize, boolean safe) {
        if (!validTaskTermPre(x, safe))
            return null; //basic enough to check before normalize

        var y = (imageNormalize ? Image.imageNormalize(x) : x).normalize();

        if (y instanceof Neg)
            throw new TermTransformException("normalization negation", x, y);

        if (y.IMPL())
            y = NAL.term.NORMALIZE_IMPL_SUBJ_NEG_VAR ? taskTermImpl(y) : y;

        //TODO unnegated loose variables completely
        //HACK this solves only the simplest 1 variable (#,?) case
        return y.vars() == 1 && y.varIndep() == 0 && y.hasAny(NEG) ?
            UnnegOnlyVariable.apply(y) : y;
    }

    private static Term taskTermImpl(Term x) {
        //unneg IMPL with negated 'loose' variable as subject
        var subj = x.sub(0);
        if (subj instanceof Neg) {
            var su = subj.unneg();
            if (su.VAR()) {
                var pred = x.sub(1);
                if (!pred.containsRecursivelyOrEquals(su))
                    x = IMPL.the(su, x.dt(), pred);
            }
        }
        return x;
    }

    /** validity as a task's term */
    public static boolean TASKS(@Nullable Term t) {
        return TASKS(t, (byte) 0);
    }

    /** validity as a particular punctuation task's term */
    public static boolean TASKS(@Nullable Term t, byte punc) {
        return TASKS(t, punc, true);
    }

    public static boolean TASKS(@Nullable Term t, byte punc, boolean safe) {

        var x = validTaskTermPre(t, safe);
        if (!x)
            return false;

        if (punc != COMMAND && (t instanceof Compound T && !T.NORMALIZED())) {
//
//                @Nullable Term n = t.normalize();
//                if (!n.equals(t))
            return fail(t, "task target not a normalized Compound", safe);
//                else
//                    t = n;
        }

        if (t.hasAny(VAR_PATTERN))
            return fail(t, "pattern variables", safe);

        if (!NAL.ABSTRACT_TASKS_ALLOWED && !t.hasAny(AtomicConstant))
            return fail(t, "excessively abstract", safe);

        var beliefOrGoal = punc == BELIEF || punc == GOAL;
        if (beliefOrGoal) {
            if (t.hasVarQuery())
                return fail(t, "belief/goal query variable", safe);
            if (t.TEMPORAL_VAR())
                return fail(t, "belief/goal dt=XTERNAL", safe);
        }

        if ((punc == GOAL || punc == QUEST) && t.IMPL() /*hasAny(IMPL)*/)
            return fail(t, "goal/quest implication", safe);

        return /*(punc==0 || !beliefOrGoal) ||*/ ValidIndepBalance.valid(t, safe);
    }

    private static boolean validTaskTermPre(@Nullable Term t, boolean safe) {
        if (t == null)
            return fail(null, "null", safe);

        if (!t.TASKABLE())
            return fail(t, "taskable", safe);

        return true;
    }

    public static boolean fail(@Nullable Object t, String reason, boolean safe) {
        if (safe)
            return false;
        else
            throw t instanceof Termed T?
                new TaskException(reason, T) :
                new TermException(reason, (Termlike) t);
    }

    /**
     * misc verification tests which are usually disabled
     */
    public static void verify(NALTask x, NAL nar) {

        if (NAL.test.DEBUG_ENSURE_DITHERED_TRUTH && x.BELIEF_OR_GOAL())
            Truth.assertDithered(x.truth(), nar);

        if (NAL.test.DEBUG_ENSURE_DITHERED_DT || NAL.test.DEBUG_ENSURE_DITHERED_OCCURRENCE) {
            var d = nar.timeRes();
            if (d > 1) {
//                if (!x.isInput()) {
                if (NAL.test.DEBUG_ENSURE_DITHERED_DT)
                    Tense.assertDithered(x.term(), d);
                if (NAL.test.DEBUG_ENSURE_DITHERED_OCCURRENCE)
                    Tense.assertDithered(x, d);
//                }
            }
        }
    }

    /**
     * punc -> index, @see p(punc)
     */
    public static byte i(byte punc) {
        return (byte) switch (punc) {
            case BELIEF -> 0;
            case QUESTION -> 1;
            case GOAL -> 2;
            case QUEST -> 3;
            default -> -1;
        };
    }

    /** index -> punc, @see i(index) */
	public static byte p(int index) {
		return switch (index) {
			case 0 -> BELIEF;
			case 1 -> QUESTION;
			case 2 -> GOAL;
			case 3 -> QUEST;
			default -> (byte)-1;
		};
	}

    public static boolean same(NALTask x, NALTask y, NAL n) {
        return same(x, y.term(), y.punc(), y.start(), y.end(), y.truth(), true, n);
    }

    public static boolean same(NALTask x, Term y, byte yPunc, MutableTruthInterval yTruth, boolean testTruth, NAL n) {
        return same(x, y, yPunc, yTruth.s, yTruth.e, yTruth, testTruth, n);
    }

    /**
     *  @param x existing task
     * @param y new task term
     */
    public static boolean same(NALTask x, Term y, byte yPunc, long ys, long ye, Truth yt, boolean testTruth, NAL n) {
        return x.punc() == yPunc &&
            x.contains(ys, ye) //parent.start() == s && parent.end() == e &&
            && (!testTruth || Op.QUESTION_OR_QUEST(yPunc) || x.truth().equalsStronger(yt, y instanceof Neg, n)) &&
            y.unneg().equals(x.term());
    }

    /** immutable */
    protected static Truth truth(Truth t, boolean neg) {
        if (t instanceof AbstractMutableTruth T)
            return T.immutableUnsafe(neg);
        return neg ? t.neg() : t;
    }

    public static Predicate<NALTask> notEqualTo(NALTask task) {
        //return ((Predicate<NALTask>) task::equals).negate();
        return t->!task.equals(t);
    }



    public static int compareSerialized(NALTask a, NALTask b) {
        return serialize(a).compareTo(serialize(b));
    }

    private static DynBytes serialize(NALTask a) {
        return IO.bytes(a, false, false, new DynBytes(32));
    }

//        private int compareIdentity(TaskRegion a, TaskRegion b) {
//            return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
//        }


    @Nullable
    public abstract Truth truth();


    /** produce a concrete, non-proxy clone.
     * "metadata" (pri, creation, why) fields are NOT propagated automatically; use copyMeta() to do so explicitly
     * ex:
     * //                .why(why)
     * //                .withPri(pri())
     * //                .withCreation(creation())
     * */
    public NALTask the() {
        return (this instanceof ProxyTask || this instanceof SerialTask) && !(this instanceof DerivedTask) ?
                taskUnsafe(term(), punc(), truth(), start(), end(), stamp()).copyMeta(this) : this;
    }

    @Override
    public final float freq() {
        return truth().freq();
    }

    @Override
    public double evi() {
        return truth().evi();
//        var t = truth();
//        if (t == null)
//            throw new NullPointerException(); //TEMPORARY
//        return t.evi();
    }

    @Deprecated public final long creation() {
        return creation;
    }

    public final boolean uncreated() {
        return creation==TIMELESS;
    }

    public final NALTask setCreation(long creation) {
        this.creation = creation;
        return this;
    }

    @Override
    public TaskRegion mbr(HyperRegion y) {
        return TaskRegion.mbr((TaskRegion) y, this);
    }

    @Override
    public float freqMean() {
        return freq();
    }

    @Override
    public float freqMin() {
        return freqMean();
    }

    @Override
    public float freqMax() {
        return freqMean();
    }

    @Override
    public float confMean() {
        return (float) conf();
    }

    @Override
    public float confMin() {
        return confMean();
    }

    @Override
    public float confMax() {
        return confMean();
    }

    /**
     * computes an average truth measurement for interval qStart..qEnd
     * @param dur if negative, auto-dur
     */
    @Nullable
    public final Truth truth(long qStart, long qEnd, float dur, float ete, double eviMin) {
        return ETERNAL() ?
            truthEternal(eviMin) :
            truthTemporal(qStart, qEnd, dur, ete, eviMin);
    }

    public final Truth truth(Moment m, NAL n) {
        return truth(m, n.eternalization.floatValueOf(this), n.eviMin());
    }
    public final Truth truth(Moment m, float ete, double eviMin) {
        return truth(m.s, m.e, m.dur, ete, eviMin);
    }

    @Nullable
    private Truth truthEternal(double eviMin) {
        var t = truth();
        return t.evi() < eviMin ? null : t;
    }

    @Nullable
    private Truth truthTemporal(long qStart, long qEnd, float dur, float ete, double eviMin) {
        return truth().cloneEviMult(EviProjector.integrate(this, qStart, qEnd, dur, ete, true), eviMin);
    }


//    @Nullable
//    public final Truth truthRelative(long ts, long te, float dur, long now, float ete, double eviMin) {
//        double p = project(now, ts, te, dur, ete);
//        return truth().eviMult(p, eviMin);
////		return truth().confMult(p, eviMin);
//    }

    public double eviMean(long qStart, long qEnd, float dur, float ete) {
        var s = start();
        return s == ETERNAL ? evi() :
            new EviInterval(qStart, qEnd, dur).eviMean(this, ete);
    }

    public final double eviMean(EviInterval q) {
        return eviMean(q, 0);
    }

    public double eviMean(EviInterval q, float ete) {
        var s = start();
        return s == ETERNAL ? evi() : q.eviMean(this, ete);
    }


    public String proof() {
        return proof(new StringBuilder(1024)).toString().trim();
    }


    private StringBuilder proof(StringBuilder temporary) {
        proof(0, temporary);
        return temporary;
    }

    @Override
    public String toStringWithoutBudget() {
        return appendTo(new StringBuilder(128), true, false,
                false,
                false
        ).toString();
    }

    @Override
    public StringBuilder appendTo(StringBuilder buffer, boolean showStamp) {
        var notCommand = punc() != COMMAND;
        return appendTo(buffer, true,
            showStamp && notCommand,
            notCommand,
            showStamp
        );
    }

    private StringBuilder appendTo(@Nullable StringBuilder buffer, boolean term, boolean showStamp, boolean showBudget, boolean showLog) {

        var contentName = term ? term().toString() : "";

        CharSequence tenseString;

        appendOccurrenceTime(
                (StringBuilder) (tenseString = new StringBuilder()));

        var stampString = showStamp ? stampAsStringBuilder() : null;

        var stringLength = contentName.length() + tenseString.length() + 1 + 1;

        var hasTruth = BELIEF_OR_GOAL();
        if (hasTruth)
            stringLength += 11;

        if (showStamp)
            stringLength += stampString.length() + 1;

        /*if (showBudget)*/

        stringLength += 1 + 6 + 1 + 6 + 1 + 6 + 1 + 1;


        if (buffer == null)
            buffer = new StringBuilder(stringLength);
        else
            buffer.ensureCapacity(stringLength);


        if (showBudget)
            toBudgetStringExternal(buffer).append(' ');


        buffer.append(contentName).append((char) punc());

        if (!tenseString.isEmpty())
            buffer.append(' ').append(tenseString);

        if (hasTruth) {
            buffer.append(' ');
            Truth.appendString(buffer, 2, freq(), (float) conf());
        }

        if (showStamp)
            buffer.append(' ').append(stampString);

        return buffer;
    }

    private void appendOccurrenceTime(StringBuilder sb) {
        var oc = start();

        /*if (oc == Stamp.TIMELESS)
            throw new RuntimeException("invalid occurrence time");*/

        if (oc != ETERNAL) {
            var estTimeLength = 8; /* # digits */
            sb.ensureCapacity(estTimeLength);
            sb.append(oc);

            var end = end();
            if (end != oc)
                sb.append((char) 0x22c8 /* bowtie, horizontal hourglass */).append(end);
        }
    }

    public NALTask copyMeta(NALTask t) {
        if (t != this && !(this instanceof SerialTask)) {
            withPri(t);
        }
        return this;
    }

    public NALTask asLateAs(NALTask x) {
        if (this != x) {
            var xs = x.creation;
            if (xs != TIMELESS) {
                var s = creation;
                setCreation(s == TIMELESS ? xs : Math.max(xs, s));
            }
        }
        return this;
    }

    /**
     * Check if a Task is a direct input,
     * or if its origin has been forgotten or never known
     */
    public boolean isInput() {
        return stampLength() <= 1;
    }


    public boolean ETERNAL() {
        return this instanceof EternalTask || start() == ETERNAL;
    }

    //	@Deprecated public final Predicate<NALTask> timeIntersecting() {
//		long xs;
//		if ((xs = start()) == ETERNAL)
//			return t->true;
//		else
//			return new TimeIntersecting(xs, end() + term().eventRange());
//	}


//	private static final class StampOverlapping implements Predicate<NALTask> {
//		long[] xStamp;
//
//		private StampOverlapping(long[] xStamp) {
//			this.xStamp = xStamp;
//		}
//
//		@Override
//		public boolean test(NALTask y) {
//			return Stamp.overlapsAny(xStamp, y.stamp());
//		}
//	}

//	private static class TimeIntersecting implements Predicate<NALTask> {
//		long xs, xe;
//
//		public TimeIntersecting(long xs, long xe) {
//			this.xs = xs;
//			this.xe = xe;
//		}
//
//		@Override
//		public boolean test(NALTask t) {
//			return t.intersects(xs, xe);
//		}
//	}

    public boolean equalNAL(NALTask b) {
        return
            equalStamp(this, b)
            &&
            (!BELIEF_OR_GOAL() || this.truth().equals(b.truth()))
            &&
            (this.start() == b.start()) && (this.end() == b.end())
        ;
    }

    private static boolean equalStamp(NALTask a, NALTask b) {
        long[] aa = a.stamp(), bb = b.stamp();
        if (aa == bb)
            return true;
        else if (ArrayUtil.equals(aa, bb)) {
            shareStamp(a, b, aa, bb);
            return true;
        } else
            return false;
    }

    /** assumes aa & bb known to be equal */
    public static int shareStamp(NALTask a, NALTask b, long[] aa, long[] bb) {
        var ab = Integer.compare(identityHashCode(aa), identityHashCode(bb)); //identityHashCode(aa) < identityHashCode(bb);
        switch (ab) {
            case -1 -> {  if (b instanceof AbstractNALTask B) {  B.stamp = aa;  return 1;  }  }
            case +1 -> {  if (a instanceof AbstractNALTask A) {  A.stamp = bb;  return 0;  }  }
        }
        return -1;
    }

    private CharSequence stampAsStringBuilder() {

        var x = stamp();
        var n = x.length;
        var estimatedInitialSize = 8 + (n * 3); //TODO tune

        var y = new StringBuilder(estimatedInitialSize);
        y.append(STAMP_OPENER);

        /*if (creation() == TIMELESS) {
            buffer.append('?');
        } else */
        /*if (!(start() == ETERNAL)) {
            appendTime(buffer);
        } else {*/
        var c = creation();
        if (c != TIMELESS)
            y.append(c);

        y.append(STAMP_STARTER).append(' ');

        for (var i = 0; i < n; i++) {
            BinTxt.append(y, x[i]);
            if (i < n - 1)
                y.append(STAMP_SEPARATOR);
        }

        return y.append(STAMP_CLOSER);
    }


    public NALTask copyMetaAndCreation(NALTask x) {
        return copyMeta(x).setCreation(x.creation());
    }

    public static final NALTask[] EmptyNALTaskArray = new NALTask[0];

    public final boolean equalsTerm(NALTask x) {
        return this == x || term().equals(x.term());
    }

    private static final RecursiveTermTransform UnnegOnlyVariable = new RecursiveTermTransform() {
        @Override
        protected Term applyCompound(Compound x) {
            if (x instanceof Neg) {
                var xu = x.unneg();
                if (xu instanceof Variable)
                    return xu; //the only variable
            }

            var xs = x.struct();
            return hasAll(xs, NEG.bit) && hasAny(xs, VAR_DEP.bit | VAR_QUERY.bit) ?
                super.applyCompound(x) : x;
        }
    };


    public double eviInteg() {
        return evi() * range();
    }

    public final @Nullable Truth truth(long start, long end) {
        return truth(start, end, 0, 0, NAL.truth.EVI_MIN);
    }
}