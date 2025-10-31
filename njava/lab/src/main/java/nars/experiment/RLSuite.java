package nars.experiment;

import jcog.data.list.Lst;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.game.Game;
import nars.game.action.util.Reflex0;
import nars.game.action.util.Reflex0.ReflexBuilder;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static jcog.Str.n2;
import static jcog.Str.n4;

public class RLSuite {

    static final int THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    final Lst<Lst<?>> data = new Lst<>();
    final StringBuilder results = new StringBuilder(1024 * 16);
    int cycles = 250000;
    int repeats = 4;

    ReflexBuilder[] aa = {
//            new ReflexBuilder("DQNmlp_big", PolicyAgent::DQNbig, 1),
//            new ReflexBuilder("DQNmlp_big", PolicyAgent::DQNbig, 1, 2),
            //new ReflexBuilder("DQNmlp_min", ValuePredictAgent::DQNmini, 1, 2, 4),
//            new ReflexBuilder("DQNmlp_med", Agents::DQmlp, true, true, 1),
//            new ReflexBuilder("DQNmlp_med", Agents::DQmlp, true, true, 1, 2),

            //new ReflexBuilder("DQNmlp_munch", ValuePredictAgent::DQNmunch, 1),
            //new ReflexBuilder("DQNmlp_munch", ValuePredictAgent::DQNmunch, 1, 2),

//            new ReflexBuilder("DQNmlp_intense", ValuePredictAgent::DQNintense, 1),
//            //new ReflexBuilder("DQNmlp_intense", ValuePredictAgent::DQNintense, 1, 2),
//            new ReflexBuilder("DQNmlp_intenser", ValuePredictAgent::DQNintenser, 1),
            //new ReflexBuilder("DQNmlp_intenser", ValuePredictAgent::DQNintenser, 1, 2),
            //new ReflexBuilder("DQNmlp_med", ValuePredictAgent::DQN, 1, 2, 4),
            //new ReflexBuilder("DQNmlp_med_ae", ValuePredictAgent::DQNae, 1),
            //new ReflexBuilder("DQNmlp_med_ae", ValuePredictAgent::DQNae, 1, 2),
            //new ReflexBuilder("DQNmlp_med_prec", ValuePredictAgent::DQNprec, 1),
            //new ReflexBuilder("DQNmlp_med_prec", ValuePredictAgent::DQNprec, 1, 2),
            //new ReflexBuilder("DQNrec", ValuePredictAgent::DQrecurrent, 1),
            //new ReflexBuilder("DQNrec", ValuePredictAgent::DQrecurrent, 1, 2),
            ReflexBuilder.RANDOM,
    };

    RLSuite() {

        var exe = new ForkJoinPool(THREADS);

        List.of(aa).forEach(a -> {
            games().forEach(g -> {

                exe.submit(() -> {

                    DescriptiveStatistics rewards = new DescriptiveStatistics(repeats);

                    var Gtmp = g.get(); //HACK only to get the name of the instance

                    for (int i = 0; i < repeats; i++) {

                        try {
                            double experimentRewardSum =
                            //double experimentRewardSum = exe.submit(() -> {
                                //return
                                        experiment(a, g.get());
                            //}).get();

                            synchronized (rewards) {
                                rewards.accept(experimentRewardSum);
                            }
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }

                    double rewardMean = rewards.getMean();
                    double rewardStdevPct = rewards.getStandardDeviation() / rewardMean * 100;

                    synchronized (data) {

                        data.addAll(
                                new Lst<>(5).adding(
                                        a,
                                        Gtmp.getClass().getSimpleName(),
                                        rewardMean,
                                        rewardStdevPct
                                )
                        );

                    }
                });

            });
        });

        exe.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MINUTES);
        //exe.shutdownNow();

        for (Lst x : data) {
            var agent = x.get(0);
            double mean = (double) x.get(2);
            var game = x.get(1);
            double rewardStdevPct = (double) x.get(3);
            double gainPct;
            if (agent != ReflexBuilder.RANDOM) {
                int randomBaselineIndex = data.indexOf((Lst<?> z) -> z.get(0) == ReflexBuilder.RANDOM && z.get(1).equals(game));
                var randomBaseline = data.get(randomBaselineIndex);
                double randomMean = (double) randomBaseline.get(2);
                gainPct = (mean - randomMean) / randomMean;
            } else
                gainPct = 0;

            x.add(gainPct);

            results.append(
                    ((gainPct > 0 ? "+" : "") + n2(gainPct * 100)) + "%\t" +
                            game + "\t" +
                            agent + "\t" +
                            n4(mean) + "±" + n2(rewardStdevPct) + "%"
            );
            results.append('\n');
        }


        System.out.println();
        System.out.println(results);
    }

    public static void main(String[] args) {
        new RLSuite();
    }

    private List<Supplier<Game>> games() {
        return List.of(
                () -> new Tetris(),
                () -> new NObvious("o", 1),
//            new NObvious("o", 0);
//            new NObvious("o", 0.5f);
                () -> new PoleCart($.atomic("p"))

                //new ArkaNAR($.$$("a")),
//            new Arm2D("a"),
//            new TrackXY($.the("t"), 3, 1),
//            new TrackXY($.the("t"), 3, 3),
//            new BitGame.BitGame1($.the("t"), 2, 2)

                //new SineRider("s");
                //new NARio();
                //new Gradius($$("g"));
                //new Recog2D();
                //new Pacman("p");
        );
    }

    private double experiment(ReflexBuilder a, Game g) {
        NAR n = NARS.shell();
        //n.time.dur(1);

        Reflex0 r = new Reflex0(a, g);
        n.add(g);
        g.nar = n; //HACK
        //new GameStats(p);

        //n.start();
        n.run(cycles);

        double rewardSum = g.rewards.rewardStat.getSum();
        //n.stop();
        n.delete();

        return rewardSum;
    }

}