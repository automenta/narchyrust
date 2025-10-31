package nars.action;

import nars.Deriver;
import nars.NAL;
import nars.control.Cause;
import nars.deriver.reaction.PatternReaction;
import nars.deriver.reaction.Reaction;
import nars.premise.NALPremise;

import static nars.Op.BELIEF;

/**
 * TODO static and/or move to Truthify.java
 */
public final class TruthifyAction extends Action<Deriver> {

    public final PatternReaction rule;

    public TruthifyAction(PatternReaction rule, Cause<Reaction<Deriver>> why) {
        super(why);
        this.rule = rule;
    }
    @Override
    public final boolean test(Deriver d) {
        var y = rule.premise(d);
        if (y != null) {
            if (NAL.derive.TASKIFY_INLINE)
                y.run(d);
            else
                d.add(y);
        } else
            d.nar.emotion.deriveFailTruthUnderflow.increment();
        return true; //continue
    }


    /**
     * WITH CORRECT PREDICATES, THESE CONDITIONS WILL HAVE BEEN ELIMINATED BEFORE REACHING HERE
     */
    private boolean valid(NALPremise p, byte punc) {

        return
                //single <=> no-belief (strict)
                p.single() == (p.belief() == null)

                &&

                //this condition is somewhat (but not completely?) prevented by NoOverlap predicate
                (!p.overlapDouble() || (punc == BELIEF ? rule.truthify.beliefOverlap : rule.truthify.goalOverlap));
    }


}
