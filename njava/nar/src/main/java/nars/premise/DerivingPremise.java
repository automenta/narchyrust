package nars.premise;

import jcog.Util;
import jcog.exe.flow.Feedback;
import nars.*;
import nars.deriver.op.Taskify;
import nars.deriver.op.time.OccSolver;
import nars.deriver.reaction.PatternReaction;
import nars.deriver.reaction.Reaction;
import nars.task.DerivedTask;
import nars.term.Neg;
import nars.truth.MutableTruthInterval;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

public final class DerivingPremise extends SubPremise<PatternReaction> {

    public final MutableTruthInterval t;
    public final byte puncOut;

    public DerivingPremise(PatternReaction rule, NALPremise parent, byte puncOut, MutableTruthInterval t) {
        super(rule, NAL.derive.TASKIFY_INLINE ? null : parent);
        this.puncOut = puncOut;
        this.t = t;
    }

    @Nullable
    private static Term taskTerm(Term x, byte punc) {
        var neg = x instanceof Neg;
        if (neg) x = x.unneg();

        var y = NALTask.taskTerm(x, punc);

        var q = Op.QUESTION_OR_QUEST(punc);

        if (q && y == null && NAL.derive.QUESTION_SALVAGE)
            y = questionSalvage(x, punc);

        if (y == null) return null;

        var z = y.negIf(neg && !q);

        return Util.maybeEqual(z, x);
    }

    @Nullable private static Term questionSalvage(Term x, byte punc) {
        return NALTask.taskTerm(
            Taskify.questionSalvage(x),
            punc, false, true);
    }


    private static NALPremise nalPremise(Premise premise) {
        return (NALPremise)(premise instanceof DerivingPremise dp ? dp.parent : premise);
    }

    @Override
    public Reaction reaction() {
        return id;
    }

    @Override
    public void run(Deriver d) {
        var pp = d.premise; //HACK
        id.termify.unify(this, d.unify, pp.from(), pp.to());
    }

    public final void taskify(Deriver d) {
        var x = termify(d);
        if (x != null)
            taskify(x, d);
    }

    @Nullable private Term termify(Deriver d) {
        return id.taskify.termify(d);
    }

    private void taskify(Term x, Deriver d) {
        var e = d.nar.emotion;

        if (!x.unneg().TASKABLE()) {
            Feedback.is("derive.NALTask.failTaskTerm", e.deriveFailTaskTerm);
            return;
        }

        var p = nalPremise(d.premise);

        Pair<Term, MutableTruthInterval> yy;
        try (var __ = e.time_derive_occurrify.time()) {
            if ((yy = OccSolver.occ(p, x, this.t, puncOut, d, id.time)) == null) {
                Feedback.is("derive.NALTask.failOcc", e.deriveFailTemporal);
                return;
            }
        }

        try (var __ = e.time_derive_taskify.time()) {

            var punc = puncOut;

            var t = yy.getTwo();
            if (Op.BELIEF_OR_GOAL(punc) && !t.ditherTruth(d.nar, d.eviMin)) {
                Feedback.is("derive.NALTask.failTruthDither", e.deriveFailTaskTerm);
                return;
            }

            var z = taskTerm(yy.getOne(), punc);
            if (z == null) {
                Feedback.is("derive.NALTask.failTaskTerm", e.deriveFailTaskTerm);
                return;
            }

            d.add(task(p, punc, z, t));
        }
    }

    private static NALTask task(NALPremise p, byte punc, Term x, MutableTruthInterval out) {
        var y = NALTask.task(x, punc,
            Op.BELIEF_OR_GOAL(punc) ? out : null,
            out, p.stamp(punc));

        return NAL.DEBUG ? new DerivedTask(y, p.task(), p.belief()) : y;
    }


}