package nars.game;

import jcog.math.FloatSupplier;
import jcog.math.normalize.FloatNormalized;
import jcog.math.normalize.FloatNormalizer;
import jcog.math.normalize.Percentilizer;
import nars.Term;
import nars.game.reward.LambdaScalarReward;
import nars.game.reward.Reward;

public interface NReward {

    /** adds/registers a reward to a game */
    void reward(Reward r);

    /**
     * default reward target builder from String
     */
    Term rewardTerm(String r);


    default LambdaScalarReward reward(Term id, float freq, FloatSupplier f) {
        LambdaScalarReward r = new LambdaScalarReward(id, freq, f);
        reward(r);
        return r;
    }

    default LambdaScalarReward reward(FloatSupplier rewardfunc) {
        return reward(rewardTerm("happy"), 1, rewardfunc);
    }

    default LambdaScalarReward reward(String reward, FloatSupplier rewardFunc) {
        return reward(reward, 1, rewardFunc);
    }

    default LambdaScalarReward reward(Term reward, FloatSupplier rewardFunc) {
        return reward(reward, 1, rewardFunc);
    }

    default LambdaScalarReward reward(String reward, float freq, FloatSupplier rewardFunc) {
        return reward(rewardTerm(reward), freq, rewardFunc);
    }

    default LambdaScalarReward rewardNormalized(Term id, FloatSupplier f) {
        return rewardNormalized(id, 1, false, f);
    }
    default LambdaScalarReward rewardNormalizedPolar(Term id, FloatSupplier f) {
        return rewardNormalized(id, 1, true, f);
    }

    default LambdaScalarReward rewardNormalized(Term reward, float freq, boolean polar, FloatSupplier rewardFunc) {
        assert(!(rewardFunc instanceof FloatNormalizer));
        FloatNormalized f = new FloatNormalized(rewardFunc);
        if (polar) f.polar();
        return reward(reward, freq, f);
    }
    default LambdaScalarReward rewardPercentile(Term reward, FloatSupplier rewardFunc) {
        boolean preNorm = true;
        if (preNorm)
            rewardFunc = new FloatNormalized(rewardFunc);

        FloatSupplier F = rewardFunc;
        Percentilizer p = new Percentilizer(1000, false);
        return reward(reward, ()->{
            float x = F.asFloat();
            return x != x ? Float.NaN : p.valueOf(x);
        });
    }
}