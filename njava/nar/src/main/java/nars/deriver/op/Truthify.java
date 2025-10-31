package nars.deriver.op;

import nars.Deriver;
import nars.Term;
import nars.deriver.util.PuncMap;
import nars.premise.NALPremise;
import nars.term.Compound;
import nars.term.ProxyCompound;
import nars.truth.func.TruthFunction;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

/**
 * Evaluates the (maximum possible) truth of a premise
 * After temporalization, truth may be recalculated.  the confidence
 * will not exceed the prior value calculated here.
 */
public class Truthify extends ProxyCompound {

    public final TruthFunction belief, goal;

    /**
     * mode:
     * +1 single premise
     * 0 double premise
     * -1 disabled
     */
    public final byte beliefMode, goalMode;
    public final boolean beliefOverlap, goalOverlap;

    /**
     * punctuation transfer function
     * maps input punctuation to output punctuation. a result of zero cancels
     */
    public final PuncMap punc;

    public Truthify(PuncMap punc, TruthFunction belief, TruthFunction goal) {
        super((Compound)PROD.the(
                punc.term(),
                label(belief),
                label(goal)
        ));
        this.punc = punc;
        this.belief = belief;

        if (belief != null) {
            beliefMode = (byte) (belief.single() ? +1 : 0);
            beliefOverlap = /*NAL.derive.OVERLAP_GLOBAL_ALLOW || */belief.allowOverlap();
        } else {
            beliefMode = -1;
            beliefOverlap = false; //N/A
        }

        this.goal = goal;
        if (goal != null) {
            goalMode = (byte) (goal.single() ? +1 : 0);
            goalOverlap = /*NAL.derive.OVERLAP_GLOBAL_ALLOW || */goal.allowOverlap();
        } else {
            goalMode = -1;
            goalOverlap = false; //N/A
        }

    }

    private static Term label(TruthFunction belief) {
        return belief != null ? belief.term() : EmptyProduct;
    }

    @Nullable public TruthFunction function(NALPremise p, byte punc, Deriver d) {
        return switch (punc) {
            case QUEST, QUESTION -> null;
            case BELIEF, GOAL -> punc == BELIEF ? belief : goal;
            default -> throw new InvalidPunctuationException(punc);
        };
    }
}