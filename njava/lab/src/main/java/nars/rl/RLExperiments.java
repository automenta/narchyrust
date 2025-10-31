package nars.rl;

import jcog.data.list.Lst;
import jcog.table.ObjectTable;
import jcog.tensor.Agents;
import nars.experiment.Acrobot;
import nars.experiment.PoleCart;
import nars.experiment.RLPlayer;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.concurrent.ForkJoinPool.commonPool;

/**
 * Task start,end -> RangedTruth, with all the curves etc
 * CMAES (direct?) meta-controllers, at first hardcoded, then reflection-based
 *
 */
public class RLExperiments {
    public static void main(String[] args) throws InterruptedException {

        int repeats = 1;

        List<RLPlayer.RLResult> results = new CopyOnWriteArrayList<>();

        Lst<Runnable> experiments = new Lst();
        for (var agent : new IntIntToObjectFunction[] {
                Agents::Random,
                Agents::RandomGaussian,
                Agents::VPG,
                Agents::PPO,
                Agents::SAC
                //Agents::DQmlp,
                //Agents::CMAES,
                //Agents::DQrecurrent
        }) {
            for (int i = 0; i < repeats; i++) {
                experiments.add(()-> {
                    Acrobot.ui = false;
                    var game =
                            new PoleCart("p", false);
                            //new Acrobot();
                            //new TrackXY(4, 1);

                    game.time.dur(4);

                    float[] history =
                            {1};
                            //{1, 2};

                    int maxCycles =
                        300000;
                        //300000;
                        //400000;
                        //200_000;

                    var result = RLPlayer.run(game, agent, maxCycles, history);
                    //System.out.println(ObjectTable.toString(result) + "\n");
                    results.add(result);
                });
            }
        }


        int nThreads = 8;
        commonPool().setParallelism(nThreads);
        //var p = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        experiments.parallelStream().forEach(Runnable::run);
        //experiments.forEach(Runnable::run);

//        p.setThreadFactory(new NamedThreadFactory(RLExperiments.class.getSimpleName()));
//        p.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);



        ObjectTable t = new ObjectTable(RLPlayer.RLResult.class, results);
        t.removeColumnsWithEqualValues();
        //t = t.sortOn("rewardMean");
        t.write().csv(System.out);
        //t.write().csv(new GZIPOutputStream(o));

        //String ts = t.printAll();
        //System.out.println(ts);
    }
}
