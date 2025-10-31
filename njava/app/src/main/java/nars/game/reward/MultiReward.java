package nars.game.reward;

import jcog.Util;
import jcog.math.FloatMeanEwma;
import jcog.math.FloatSupplier;
import nars.$;
import nars.Op;
import nars.Term;
import nars.game.Game;
import nars.term.Termed;
import nars.term.atom.Int;
import org.eclipse.collections.api.block.function.primitive.DoubleToDoubleFunction;

import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

public class MultiReward extends Reward {

    public final LambdaScalarReward[] r;
    private final FloatSupplier reward;
    float rewardNow = Float.NaN;

    public MultiReward(Term id, FloatSupplier reward, DoubleToDoubleFunction... transforms) {
        super(id);

        assert(transforms.length > 1);

        this.reward = reward;
        this.r = Util.arrayOf(i -> subReward(i, transforms),
            new LambdaScalarReward[transforms.length]);
    }

    protected LambdaScalarReward subReward(int i, DoubleToDoubleFunction[] transforms) {
        return new SubReward(i, transforms);
    }

    public static MultiReward ewma(Term id, FloatSupplier R, float... alphas) {
        return new MultiReward(id, R, Util.arrayOf(i -> i == 0 ? null :
                new FloatMeanEwma(alphas[i-1]).fn(), new DoubleToDoubleFunction[alphas.length+1]));
    }

    /** assumes input is already unitized, so the split is 0.5 */
    public static MultiReward polarSplit(Term id, FloatSupplier R) {
        return new MultiReward(id, R, Util.arrayOf(i -> i == 0 ?
                (x -> x >= 0.5f ?  2*(x-0.5f) : 0) :
                (x -> x <  0.5f ? 1-2*(0.5f-x) : 1),
            new DoubleToDoubleFunction[2]));
    }


    @Override
    public final float resolution() {
        return r[0].resolution();
    }

    public final MultiReward resolution(float f) {
        for (ScalarReward x : r)
            x.resolution(f);
        return this;
    }

    @Override
    public double reward() {
        return Util.sum((ToDoubleFunction<Reward>) Reward::rewardElseZero, r);
    }

    @Override
    public void start(Game g) {
        super.start(g);
        for (LambdaScalarReward x : r)
            x.start(g);
    }

    @Override
    public void accept(Game g) {
        rewardNow = reward.asFloat();
        for (LambdaScalarReward x : r)
            x.accept(g);
    }

    /** TODO combine with DeMultiplexed Sensor's, etc.. equivalent way of doing this */
    protected Term term(int i) {
        Int I = Int.i(i);
        return id.hasAny(Op.VAR_DEP) ? id.replace($.varDep(1), I) : $.inh(id, I);
    }

    @Override
    public Iterable<? extends Termed> components() {
        return Stream.of(r).map(z -> (Termed) (z.sensor))::iterator;
    }

    @Override
    public double happy(long start, long end, float dur) {
        return Util.mean((ToDoubleFunction<LambdaScalarReward>) (R -> R.happy(start, end, dur)), r);
    }

    public final void amp(float p) {
        for (var z : r) z.sensor.sensing.amp(p);
    }

    private final class SubRewardFn implements FloatSupplier {

        private final DoubleToDoubleFunction f;

        SubRewardFn(DoubleToDoubleFunction f) {
            this.f = f;
        }

        @Override
        public float asFloat() {
            float r = rewardNow;
            return r != r ? Float.NaN : (float) f.valueOf(r);
        }
    }

    private class SubReward extends LambdaScalarReward {

        SubReward(int i, DoubleToDoubleFunction[] transforms) {
            super(MultiReward.this.term(i), transforms[i] == null ? () -> MultiReward.this.rewardNow :
                    new SubRewardFn(transforms[i]));
        }

        @Override
        public float strength() {
            return MultiReward.this.strength();
        }

    }
}