package nars.task.util;

import nars.*;
import nars.action.resolve.Answerer;
import nars.action.resolve.TaskResolver;
import nars.task.proxy.SpecialNegTask;
import nars.term.Compound;
import nars.term.Neg;
import nars.time.Tense;
import nars.truth.Stamp;
import nars.truth.evi.EviInterval;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import static jcog.math.Intervals.shrink;
import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.truth.MutableTruthInterval.reprojectFactor;

/**
 * emulates impl.strong derivation with inline truth resolution of the opposite subj/pred condition
 */
public final class ImplTaskify {

    private static final boolean ditherTruth = true, ditherTime = true;

    private static final boolean FILTER_GOAL_LOOP =
        false;
        //true;

    private final EviInterval evi;


    public ImplTaskify(Focus f) {
        this(f.when(), f.dur());
    }

    private ImplTaskify(long[] se, float durFocus) {
        this(se[0], se[1], durFocus);
    }

    public ImplTaskify(long s, long e, float durFocus) {
        evi = new EviInterval(s, e, durFocus);
    }

    @Nullable public static NALTask taskify(NALTask impl, boolean fwd, boolean beliefOrGoal, Deriver d) {
        return new ImplTaskify(d.focus).task(impl, null, fwd, beliefOrGoal, d);
    }

    static public boolean validImpl(NALTask impl) {
        return impl.BELIEF() && validImpl(impl.term());
    }
    static private boolean validImpl(Term z) {
        return z.IMPL() && validImplSub(z.subUnneg(0)) && validImplSub(z.sub(1));
    }
    static private boolean validImplSub(Term sub) {
        return !sub.VAR();
    }

    /** x is a context parameter, which is either instanceof Deriver or Focus */
    @Nullable public NALTask task(NALTask implTask, @Nullable NALTask condTask, boolean fwd, boolean beliefOrGoal, Object x) {
        if (!validImpl(implTask))
            return null;

        var implTerm = implTask.term();
        var condTerm = condTerm(implTerm, fwd);

        if (!beliefOrGoal && FILTER_GOAL_LOOP && hasLoop(implTask, fwd, condTerm))
            return null;

        var dt = dtShift(implTask.term());
        if (condTask == null) {
            condTask = resolve(condTerm, beliefOrGoal, fwd ? 0 : dt, x);
            if (condTask == null) return null;
        }

        if (condTask.punc()!=(beliefOrGoal?BELIEF:GOAL))
            throw new UnsupportedOperationException("supplied condition/impl punc mismatch");

        var condTaskTerm = condTask.term();
//        if (!condTerm.equalsPN(condTaskTerm))
//            return null; //throw new UnsupportedOperationException("supplied condition/impl term mismatch, fwd=" + fwd + "\t" + implTerm + "\t" + condTerm);

        if (condTerm instanceof Neg && !(condTaskTerm instanceof Neg))
            condTask = SpecialNegTask.neg(condTask);

        return taskable(implTask, condTask) ?
            _task(condTask, implTask, fwd, beliefOrGoal, dt,
                    x instanceof Focus f ? f : ((Deriver)x).focus) : null;
    }

    private static boolean hasLoop(NALTask implTask, boolean fwd, Term condTerm) {
        var i = concTerm(implTask, fwd);
        return i instanceof Compound c ?
            c.condOf(condTerm, 0) :
            i.equalsPN(condTerm);
    }

    private static boolean taskable(NALTask impl, NALTask cond) {
        return !NAL.premise.OVERLAP_DOUBLE_MODE.overlapping(impl, cond);
        //return !impl.stampOverlapping(cond);
    }

    private static int dtShift(Term implTerm) {
        return implTerm.subUnneg(0).seqDur() + implTerm.DT();
    }

    /**
     * TODO implement this as a streamlined NALPremise evaluation strategy that can be applied
     * to other derivation rules when specific parts of the derivation are already known or can be
     * assumed, or calculated directly
     */
    private static NALTask _task(NALTask condTask, NALTask implTask, boolean fwd, boolean beliefOrGoal, int dt, Focus f) {
        long is = implTask.start(), ie = implTask.end();
        long cs = condTask.start(), ce;
        if (cs == EviInterval.ETERNAL) {
            cs = is; ce = ie;
        } else {
            ce = condTask.end();
            if (NAL.premise.OCC_TRIM) {
                var cse = shrink(cs, ce, is, ie);
                cs = cse[0]; ce = cse[1];
            }
        }

        var n = f.nar;
        var ete = n.eternalization;
        var dur = n.dur();
        var eviMin = n.eviMin();

        var implTruth = truth(implTask, cs, ce, ete, eviMin, dur);
        if (implTruth == null) return null;

        var condTruth = truth(condTask, cs, ce, ete, eviMin, dur);
        if (condTruth == null) return null;

        var concTruth = TruthFunctions.implStrong(fwd, beliefOrGoal).truth(condTruth, implTruth, eviMin);
        if (concTruth == null) return null;

        long concStart, concEnd;
        if (condTask.ETERNAL() && implTask.ETERNAL())
            concStart = concEnd = EviInterval.ETERNAL;
        else {
            concStart = cs; concEnd = ce;
            if (dt != 0) {
                var shift = (fwd ? +1 : -1) * dt;
                concStart += shift; concEnd += shift;
            }
        }

        if (ditherTime && concStart != EviInterval.ETERNAL) {
            var dtDither = n.timeRes();
            if (dtDither > 1) {
                concStart = Tense.dither(concStart, dtDither);
                concEnd = Tense.dither(concEnd, dtDither);
            }
        }

        if (NAL.revision.PROJECT_REL_OCC_SHIFT) {
            var factor = reprojectFactor(cs, ce, concStart, concEnd, dur);
            concTruth.cloneEviMult(factor, 0);
            if (concTruth.evi() < eviMin) return null;
        }

        if (ditherTruth) {
            concTruth = concTruth.dither(n);
            if (concTruth == null) return null;
        }

        return task(condTask, beliefOrGoal, implTask, fwd, concTruth, concStart, concEnd, f);
    }

    private static @Nullable NALTask task(NALTask condTask, boolean beliefOrGoal, NALTask implTask, boolean fwd, Truth concTruth, long concStart, long concEnd, Focus f) {
        var z = NALTask.task(concTerm(implTask, fwd), beliefOrGoal ? BELIEF : GOAL, concTruth, concStart, concEnd,
            Stamp.zip(condTask, implTask)
        );

        var p = f.budget.priDerived(z, condTask, implTask, f);
        if (p!=p)
            return null;
        else {
            z.pri(p);
            return z;
        }
    }

    private static Term concTerm(Term impl, boolean fwd) {
        return impl.sub(fwd ? 1 : 0);
    }
    private static Term concTerm(NALTask implTask, boolean fwd) {
        return concTerm(implTask.term(), fwd);
    }
    private static Term condTerm(Term impl, boolean fwd) {
        return concTerm(impl, !fwd);
    }

    //    private final Answerer.AbstractTaskResolver resolver =
//            new Answerer.DirectTaskResolver().occSpecific(true);

    private static final TaskResolver resolver =
        new Answerer.DirectTaskResolver(false);
        //Answerer.AnyTaskResolver;
        //Answerer.ExactTaskResolver;

    @Nullable
    private NALTask resolve(Term t, boolean beliefOrGoal, int shift, Object x) {
        long s = evi.s, e = evi.e;
        if (s != EviInterval.ETERNAL && shift != 0) {
            s += shift; e += shift;
        }
        var p = beliefOrGoal ? BELIEF : GOAL;
        return x instanceof Deriver d ?
            resolver.resolveTaskPolar(t, p, s, e, d, null) :
            ((Focus) x).nar.answer(t, p, s, e);
    }

    private static Truth truth(NALTask t, long s, long e, FloatFunction<NALTask> ete, double eviMin, float dur) {
        return t.truth(s, e, dur, ete.floatValueOf(t), eviMin);
    }


}
