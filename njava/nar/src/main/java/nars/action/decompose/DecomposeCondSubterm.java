package nars.action.decompose;

import nars.*;
import nars.term.Compound;
import nars.term.var.Variable;
import org.jetbrains.annotations.Nullable;

import static nars.deriver.reaction.PatternReaction.ImageAlignable;
import static nars.unify.constraint.TermMatch.Conds;

/** in-place decompose of a compound subterm that is a cond */
public class DecomposeCondSubterm extends DecomposeTerm {
    private final boolean fromOrTo;

    public DecomposeCondSubterm(Op host, boolean fromOrTo) {
        super(false);
        this.fromOrTo = fromOrTo;

        Variable target = fromOrTo ? PremiseTask : PremiseBelief;
        Variable c = fromOrTo ? PremiseBelief : PremiseTask;

        iff(target, Conds);
        iffNot(target, ImageAlignable);

        isAny(c, host);

        //in(oth, src); //recursive
        subtermOf(c, target, true, 0);
    }

    @Override
    protected Term term(Premise p) {
        return fromOrTo ? p.from() : p.to();
    }

    @Override
    protected void activate(Term x, Term y, Deriver d) {
        Premise p = d.premise;
        Term f, t;
        if (fromOrTo) {
            if (!NALTask.TASKS(y))
                return;
            f = y; t = p.to();
        } else {
            f = p.from(); t = y;
        }

        activate(f, t, true, d);
    }

    @Override
    public @Nullable Term decompose(Compound src, Deriver d) {
        return DecomposeCond.decomposeCond(src, d.rng);
    }
}