package jcog.test.control;

import nars.$;
import nars.NAR;
import nars.game.Game;
import org.hipparchus.stat.descriptive.StreamingStatistics;

public abstract class MiniTest extends Game {

    public float rewardSum = 0;
    public final StreamingStatistics dex = new StreamingStatistics();

    protected MiniTest(NAR n) {
        super(MiniTest.class.getSimpleName());
        //statPrint = n.emotion.printer(System.out);

        reward($.atomic("reward"), 1f, () -> {
//                System.out.println(this + " avgReward=" + avgReward() + " dexMean=" + dex.getMean() + " dexMax=" + dex.getMax());
//                statPrint.run();
//                nar.stats(System.out);

            float yy = myReward();

            rewardSum += yy;
            dex.addValue(actions.dexterity());

            return yy;
        });

    }


    protected abstract float myReward();

    public float avgReward() {
        return rewardSum / (((float) nar.time()) / nar.dur());
    }
}
