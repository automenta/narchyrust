package nars.game.reward;

import jcog.math.FloatSupplier;
import nars.Term;
import nars.game.Game;

public class LambdaScalarReward extends ScalarReward {

    private final FloatSupplier rewardFunc;

    public LambdaScalarReward(Term id, float freq, FloatSupplier r) {
        this(id, r);
        freq(freq);
    }

    public LambdaScalarReward(Term id, FloatSupplier r) {
        super(id);
        this.rewardFunc = r;
    }

    @Override
    protected final float reward(Game a) {
        return rewardFunc.asFloat();
    }


}