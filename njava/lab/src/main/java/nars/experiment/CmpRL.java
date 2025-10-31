package nars.experiment;

import jcog.Util;
import jcog.lab.Lab;
import jcog.signal.FloatRange;
import nars.Player;
import nars.game.Game;
import nars.game.util.GameStats;

import java.util.function.Supplier;

import static nars.$.$$;

public class CmpRL {

    public static void compare(Supplier<Game> game, Player... agent) {
        assert(agent.length > 0);

        float fps = 25; //HACK

        for (Player n : agent) {
//            n.clock = new CycleTime();
            n.add(game.get());
        }
        for (Player n : agent) {
            n.fps(fps);
            n.start();
//            Exe.run(()->{
//                n.runCycles(1000);
//            });
        }


    }

    public static void main(String[] args) {
        Player narBasic = new Player() {
            {
                inperienceGoals = false;
                arith = false;
                factorize = false;
                freqRes = 0.05f;
                confMin = 0.01f;
                        //NAL.truth.CONF_MIN;
                meta = false;
                complexMax = 16;
                conceptsMax = 8 * 1024;
                threads = 2;
            }
        };
        Player rlOnly = new Player() {
            {
                gameReflex = true;

                inperienceGoals = false;
                clusterBelief = false;
                clusterGoal = false;
                stmLinker = false;
                arith = false;
                factorize = false;
                motivation = false;
                freqRes = 0.05f;
                meta = false;
                complexMax = 16;
                conceptsMax = 1 * 1024;
                threads = 2;
            }
        };
        compare(

//                () -> new TrackXY($$("x"), 2, 2)
                ()->new PoleCart($$("x"))
//                ()->new ArkaNAR(false, 20, 8)
            //() -> new OneObviousChoice("x"),
            //() -> new TrackXY($$("x"), 4, 1),
  //          () -> new TrackXY($$("x"), 2, 2),
//            ()->new PoleCart($$("x")),

                ,
                rlOnly
                //narBasic
        );
    }

    public static class Experiment {
//        public final FloatRange BelfConf = new FloatRange(0.5f, 0.1f, 0.9f);
//        public final FloatRange GoalConf = new FloatRange(0.5f, 0.1f, 0.9f);

        public final FloatRange simple = new FloatRange(1, 0.0f, 2f);
        //public final FloatRange feedback = new FloatRange(1, 0.0f, 2f);
//        public final FloatRange eternalization = new FloatRange(0f, 0f, 0.5f);

        public double score() {
            Player p = new Player() {
                {

                    inperienceGoals = false;
                    arith = false;
                    factorize = false;

//                    beliefConf = BelfConf.floatValue();
//                    goalConf = GoalConf.floatValue();
                    meta = false;
                    complexMax = 16;
                    conceptsMax = 8 * 1024;
                    threads = 4;
                    uiTop = false;
                }

            };
            TrackXY g = new TrackXY($$("x"), 4, 4);
            g.ui = false;

            GameStats s = new GameStats(g);
            p.add(g);
            p.fps(100);
            p.start();

//            ((BasicActivator)g.focus().activator).simple.set(this.simple.floatValue());
            //((BasicActivator)g.focus().activator).feedback.set(this.feedback.floatValue());
//            g.nar.eternalization.set(this.eternalization.floatValue());


            Util.sleepMS(60000);
            p.stop();

//            g.actions.forEach(a -> {
//                final ImplTree i = new ImplTree(a.term(), p.nar);
//                //i.print(System.out);
//                i.disjunctify(0, g.nar.dtDither(), DTERNAL);
//            });

            //return (float) s.happinessMean();
            return s.dexterityMean();
        }


        public static void main(String[] args) throws NoSuchMethodException {
            new Lab<>(Experiment.class).auto().optimizeEach(Experiment::score).run(90).print();
        }
    }

}