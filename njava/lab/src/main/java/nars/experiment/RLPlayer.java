package nars.experiment;

import jcog.agent.Agent;
import jcog.tensor.Agents;
import nars.NAR;
import nars.NARS;
import nars.game.Game;
import nars.game.action.util.Reflex0;
import nars.gui.NARui;
import nars.gui.ReflexUI;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.jetbrains.annotations.Nullable;

import static spacegraph.SpaceGraph.window;

public class RLPlayer {
    public static void main(String[] args) {
        Game g =
            //new NObvious("p", 1);
            //new NObvious("n", 0);
            //new NObvious("m", 0.5f);

            //new SineRider("s", 0.001f);

            //new TrackXY(2, 2);
            //new TrackXY(3, 1);
            //new TrackXY(8, 1);
            //new TrackXY(3, 3);
            //new TrackXY(4, 1);
            //new TrackXY(4, 4);
            //new TrackXY(3, 3);
            //new TrackXY(5, 5);

            //new Tetris().fallTime(4); window(Tetris.tetrisView(((Tetris)g)), 400, 800);
            new Tetris().fallTime(2); window(Tetris.tetrisView(((Tetris)g)), 400, 800);
            //new Tetris().fallTime(1); window(Tetris.tetrisView(((Tetris)g)), 400, 800);

//            new Tetris.Dotris($.$$("t"),
//                    5, 12
//                    //7, 12
//                    //10, 16
//            ); window(Tetris.tetrisView(((Tetris)g)), 400, 800);

            //new Recog2D();

//            new ArkaNAR() {
//                @Override protected void init() {
//                    super.init();
//                    nar.runLater(()-> window(view(), 400, 400));
//                }
//            };

            //new PoleCart("p", true);

            //new Acrobot();
            //new Phys2DGame.Arm2D("a").window();
                    //CartPole("a")
            //new FZero($.$$("f"));
            //new NARio();
            //new Gradius($.$$("g"));
            //new BitGame.BitGame1($.$$("b"), 4, 4).view();

            //new Pacman("p");

//            new AIGymGame(
//                //"Acrobot-v1"
//                //"Pendulum-v1"
//                //"LunarLander-v2"
//                "BipedalWalker-v3"
//                //"FrozenLake-v1"
//                //"Blackjack-v1"
//                //"CartPole-v1"
//                //"Taxi-v3"
//            );

        run(g,
            0 //full speed
            //50
            //100
            //10
        );
    }

    private static final boolean curiosity = true;

    public static void run(Game g, float fps) {

        g.curiosity.enabled.set(curiosity);

        float[] history = {
            1
            //1, 2
            //1, 2, 3
            //1, 2, 4
            //1, 3
            //1, 8
            //1,2,3,4
            //1, 2, 4, 8
            //1, 2, 4, 8, 16, 32, 64, 128
            //1, 4, 16, 64
            //1, 5, 10, 15, 20, 25, 30
            //1,4
            //1,8
            //1,4
            //1,8
            //1,2,4,8,12,16,20,24,28,32
            //1, 2, 16, 32
            //1,2,4,8,16,32,64
        };

        IntIntToObjectFunction<Agent> policy =
            Agents::PPO
            //Agents::StreamAC
            //Agents::VPG
            //Agents::REINFORCE
            //Agents::SAC
            //Agents::DDPG
            //Agents::A2C
            //Agents::ReinforceDNC
            //Agents::ReinforceODE //<--- !!!!!
            //Agents::ReinforceLiquid
            //Agents::Random
            //Agents::DQmlp
            //Agents::DQN_NTM;
            //Agents::DQrecurrent
            //Agents::DQevolving
            //Agents::CMAES
            //Agents::Spiking
            //Agents::DynamicNeuro
        ;

        run(g, policy, fps, -1, history);
    }

    public static class RLResult {
        public long timeStart, timeEnd;
        public String game;
        public String agent;
        public long wallTimeNS;
        public double rewardMean;
        //TODO reward history
        //TODO cycle time variability
        //TODO other metrics (peak memory usage etc)
        @Nullable public Throwable fail;
    }

    public static RLResult run(Game g, IntIntToObjectFunction<Agent> agent, int maxCycles, @Deprecated float[] history) {
        return run(g, agent, 0, maxCycles, history);
    }

    public static RLResult run(Game g, IntIntToObjectFunction<Agent> agent, float fps, int maxCycles, @Deprecated float[] history) {
        NAR n = (fps > 0 ? NARS.realtime(fps) : new NARS.DefaultNAR(0, true)).get();

        //new GameStats(p);
        n.add(g);

        boolean gui =
            //true;
            //false;
            maxCycles <= 0;

        Reflex0 r = new Reflex0(agent, g, history) {
            @Override public void startIn(Game g) {
                super.startIn(g);
                if (gui) {
                    n.runLater(()-> {
                        var p = ReflexUI.reflexUI(this);
                        window(p, 900, 900);
                    }); //HACK
                }
            }
        };

        boolean meta = false;
        if (meta) {
            var mr = r.addMeta(g);
            if (gui)
                n.runLater(()-> window(ReflexUI.reflexUI(mr), 600, 600));
        }


//        r.afterFrame(()->{
//           System.out.println(n.time() + "\t" + r.time() + " " + r.rewardCurrent());
//        });

        if (gui)
            window(NARui.top(n), 800, 500);

        //n.exe.synch();

        if (maxCycles > 0) {
            RLResult y = new RLResult();
            y.agent = r.agent.toString();
            y.game = g.toString();
            y.timeStart = n.time();

//            DoubleArrayList rewards = new DoubleArrayList(Math.max(0, maxCycles));
            long start = System.nanoTime();
            try {
//                r.afterFrame(() -> {
//                    rewards.add(r.rewardCurrent());
//                });
                if (fps <= 0)
                    n.run(maxCycles);
                else {
                    //TODO NOT WORKING YET
                    n.runAt(n.time() + (long) (maxCycles * n.dur()), ()->n.stop());
                    n.startFPS(fps);
                }
                y.rewardMean = r.rewardMean(false);
            } catch (Throwable t) {
                y.fail = t;
                y.rewardMean = 0;
            }
            y.timeEnd = n.time();
            long end = System.nanoTime();
            y.wallTimeNS = end-start;


//            int len = 4;
//            int s = rewards.size();
//            int stride = s / len;
//            FloatArrayList f = new FloatArrayList();
//            for (int i = 0; i < s; ) {
//                double sum = 0;
//                for (int k = 0; k < stride; k++)
//                    sum += rewards.get(i + k);
//                f.add((float) (sum/stride));
//                i += stride;
//            }
//            String l = jcog.io.SparkLine.renderFloats(f, 0, 1);

//            String rewardHistory = n4(f.toArray());
            //y.rewardHistory = rewardHistory;

            return y;
        } else {
            n.startFPS(fps);
            Runtime.getRuntime().addShutdownHook(new Thread(()->{
                System.out.println("@" + n.time() + " rewardMean=" + r.rewardMean(false));
            }));
            return null;
        }
    }



}