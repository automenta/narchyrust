package nars.experiment;

import jcog.Util;
import jcog.exe.Exe;
import jcog.lab.Lab;
import jcog.lab.Optimize;
import nars.Player;
import nars.control.DefaultBudget;
import nars.game.Game;
import nars.truth.Truthed;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

public class PlayerOptimize {

    static int runSeconds =
        2 * 60;
        //60;
        //1;

    static int trials = 4;
    static int repeats = 1;

    private static float fps =
        60;
        //50;
        //40;
        //30;
        //25;

    private static int threads =
        //0; //AUTO
        //4;
        //6;
        8;

    public static void main(String[] args) {

        var e = new Lab<>(() -> player(
            new Tetris()
                .fallTime(2)
            //new PoleCart("p", false)
                //.frameSkip(4)
            //new TrackXY($$("x"), 3, 1); g.ui = false;
            //new TrackXY($$("x"), 3, 3); g.ui = false;
            //new NObvious("p", 1);
        ))
        //.sense("time", (Player x) -> x.nar.time())

        .sense("concepts", (Player x) -> x.nar.memory.size())
        .sense("cmplxMean", (Player x) -> x.nar.concepts().mapToInt(z -> z.term.complexity()).average().getAsDouble())
        .sense("confMean", (Player x) -> x.nar.tasks(true,false,true,false).mapToDouble(Truthed::conf).average().getAsDouble())

        .var("eternalization", 1E-2f, 0.5f, 0.05f, (Player p, float v) -> p.eternalization = v)

        .var("durMax",   1, 64, (p,v) -> p.durMax   = v)
        .var("durShift", 1, 64, (p,v) -> p.durShift = v)

        .var("complexMax", 10, 50, 10, (Player p, int c) -> p.complexMax = c)

//        .var("beliefConf",   0.1f, 0.98f, (p,v) -> p.beliefConf   = v)
//        .var("goalConf",   0.1f, 0.98f, (p,v) -> p.goalConf   = v)


        //.var("simple", 0.01f,  2, (p,v) -> p.ready(N -> p.games(g -> budget(g).simple.set(v) )) )
        .var("certain", 0.01f, 2, (p,v) -> p.ready(N -> p.games(g -> budget(g).certain.set(v))) )

        //.var("focusTasks", 32, 384, 64, (Player p, int n)-> { p.focusTasks = n; p.focusLinks = n*2; })
        .var("focusConcepts", 16, 512, 64, (Player p, int v)-> p.focusConcepts =v)

//        .var("derivePri", 0.1f,  1, (p,v) -> p.ready(N -> p.games(g -> budget(g).puncDerived.set(v) )) )
//        .var("confMin", 1E-4f, 1E-2f, (p, v) -> p.confMin = p.confRes = v)

//        .var("internComplexity", 4, 30, 3,
//                (Player p, int v)-> InterningTermBuilder.volMaxDefault = v)

//        .var("taskBagDeriverDepth", 4, 16, 4,
//                (Player p, int v)-> TaskBagDeriver.DEPTH_DEFAULT =v)

//            .var("linkIn", 0.01f, 1.5f, 0.2f, (p, v)->{
//                p.ready((N)->{
//                    p.games().forEach(g -> ((DefaultBudget)g.focus().budget).in.set(v));
//                });
        .optimize(repeats, PlayerOptimize::experiment, new Optimize.CMAESOptimizationStrategy(trials))
        .run(trials)
        .report();


//        window(TsneRenderer.view(e.data,
//                        MetalBitSet.bits(3).set(1/*, 2, 3*/)),
//                800, 800);

        //Decision tree: TODO needs fixed
//        var tree = e.tree(3, 6, 0);
//        tree.printExplanations();

        ((ForkJoinPool)Exe.executor()).shutdownNow();
        System.exit(0);
    }

    private static DefaultBudget budget(Game g) {
        return (DefaultBudget) g.focus().budget;
    }

    private static Player player(Game g) {
        Player p = new Player(g).fps(fps);
        p.exeAffinity = false;
        p.uiTop = false;
        if (threads > 0)
            p.threads = threads;
        return p;
    }

    private static double experiment(Supplier<Player> P) {
        try (var p = P.get()) {
            p.start();

            Util.sleepS(runSeconds);

            return p.rewardMean();
        }
    }


}
