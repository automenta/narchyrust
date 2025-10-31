package nars.game;

import com.google.common.collect.Streams;
import jcog.Is;
import jcog.data.list.FastCoWList;
import nars.Term;
import nars.focus.PriAmp;
import nars.game.reward.Reward;
import nars.term.Termed;

import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.stream.Stream;

@Is({"Reward_system", "Bellman_equation","Felicific_calculus"})
public class Rewards implements Iterable<Reward> {

    public final FastCoWList<Reward> rewards = new FastCoWList<>(Reward[]::new);

    public PriAmp pri;

    public final DoubleSummaryStatistics rewardStat = new DoubleSummaryStatistics();

//    enum RewardCombine {
//        Mean() {
//            @Override double happiness(long start, long end, float dur, FastCoWList<Reward> l) {
//                return l.meanBy(r -> r.happy(start, end, dur));
//            }
//        },
//        RMS() {
//            @Override double happiness(long start, long end, float dur, FastCoWList<Reward> l) {
//                return l.rmsBy(r -> r.happy(start, end, dur));
//            }
//        },
//        /** pi product */
//        Mult() {
//            @Override double happiness(long start, long end, float dur, FastCoWList<Reward> l) {
//                return l.multBy(r -> r.happy(start, end, dur));
//            }
//        },
//        GeometricMean() {
//            @Override double happiness(long start, long end, float dur, FastCoWList<Reward> l) {
//                return Math.pow(Mult.happiness(start, end, dur, l), 1.0/l.size());
//            }
//        },
//
//        /** combines Mult and Mean.
//         *  Mean acts as a non-zero base in cases where a reward component is near zero.
//         *  GeometricMean is maximum when ALL rewards are maximum.
//         */
//        MeanMix() {
//            @Override double happiness(long start, long end, float dur, FastCoWList<Reward> l) {
//                if (l.size()==1) return Mean.happiness(start, end, dur, l); //just in case
//
//                float split =
//                    0.5f;
//                    //1f/l.size();
//                return Util.lerpSafe(split,
//                        GeometricMean.happiness(start, end, dur, l),
//                        Mean.happiness(start, end, dur, l));
//            }
//        }
//        //TODO meanWeightedBy
//        //TODO rmsWeightedBy
//        ;
//        abstract double happiness(long start, long end, float dur, FastCoWList<Reward> l);
//    }
//
//    final RewardCombine rewardCombine =
//            RewardCombine.Mean;
//            //RewardCombine.MeanMix;

    /**
     * avg reward satisfaction, current measurement
     * happiness metric scalar aggregation of all reward components
     */
    @Is({"Multi-objective_optimization","Pareto_efficiency"})
    public final double happiness(long start, long end, float dur) {
        return rewards.meanWeightedBy(
            x->x.happy(start, end, dur),
            x->x.weight.asFloat()
        );
    }

    public void add(Reward r) {
        Term rt = r.term();
        var rte = rt.equals();
        if (rewards.OR(z -> rte.test(z.term())))
            throw new RuntimeException("reward exists with the ID: " + rt);

        rewards.add(r);
    }

    public final int size() {
        return rewards.size();
    }

    @Override
    public final Iterator<Reward> iterator() {
        return rewards.iterator();
    }

    public final void update(Game game) {
        rewards.forEach(game::update);
        rewardStat.accept(rewards.meanBy(Reward::rewardElseZero));
    }

    public final Stream<Reward> stream() {
        return rewards.stream();
    }

    public Stream<? extends Termed> components() {
        return stream().flatMap(r -> Streams.stream(r.components()) );
    }
}