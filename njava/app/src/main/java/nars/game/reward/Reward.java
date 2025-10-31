package nars.game.reward;

import jcog.Util;
import jcog.math.FloatMeanPN;
import jcog.math.FloatSupplier;
import jcog.signal.FloatRange;
import nars.NAR;
import nars.Term;
import nars.game.FocusLoop;
import nars.game.Game;

/** TODO extend UniSignal */

public abstract class Reward implements FocusLoop<Game> {

    @Deprecated protected Game game;
	public final Term id;

	/** 'weight' determines:
	 *     -conf factor
	 *     -relative weight in (aggregate) game happiness */
	public final FloatRange weight = new FloatRange(1, 0, 1);

	/** internal value: goal confidence factor */
    public final FloatRange conf = new FloatRange(1, 0, 1);

//	public final FloatRange strengthMax = new FloatRange(1, 0, 1);
//	public final FloatRange strengthMin = new FloatRange(0, 0, 1);


	protected Reward(Term id) {
    	this.id = id;
    }

	/** gradual dischage / fade-out */
	public static FloatSupplier release(FloatSupplier s, float  halfLifePeriod) {
		return s.compose(new FloatMeanPN(1, (float)Util.halflifeRate(halfLifePeriod)));
	}
	/** (exponential) moving average "sustain effect" */
	public static FloatSupplier sustain(FloatSupplier s, float halfLifePeriod) {
		return s.compose(new FloatMeanPN((float)Util.halflifeRate(halfLifePeriod), (float)Util.halflifeRate(halfLifePeriod)));
	}
	/** gradual charge */
	public static FloatSupplier attack(FloatSupplier s, float halfLifePeriod) {
		return s.compose(new FloatMeanPN((float)Util.halflifeRate(halfLifePeriod), 1));
	}

    @Override public String toString() {
    	return id + ":" + getClass().getSimpleName();
	}

    /** raw value of last reward, may be NaN if not available.
	 *  if multiple components, this can return their sum reward()
	 * */
    public abstract double reward();

    public final double rewardElseZero() {
    	double r = reward();
		return r == r ? r : 0;
	}

    public final Reward conf(float c) {
    	this.conf.set(c);
    	return this;
	}

	public final Reward weight(float s) {
		if (s <= 0 || s > 1)
			throw new IllegalArgumentException();
		this.weight.set(s);
		return this;
	}

	/** goal conf factor */
    public float strength() {
    	return conf.floatValue() * weight.floatValue();
	}

	@Override
	public final Term term() {
		return id;
	}


    /** estimated current happiness/satisfaction of this reward
     *
     * happiness = 1 - Math.abs(rewardBeliefExp - rewardGoalExp)/Math.max(rewardBeliefExp,rewardGoalExp)
	 * NaN if unknown
     * */
	public abstract double happy(long start, long end, float dur);


    public final NAR nar() { return game.nar(); }


	public void start(Game g) {
		this.game = g;
	}


}