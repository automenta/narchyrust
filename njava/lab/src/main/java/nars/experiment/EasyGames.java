package nars.experiment;

import jcog.Util;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.*;
import nars.game.Game;
import nars.game.adapter.AIGymGame;
import nars.term.Termed;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static jcog.Str.n2;
import static nars.$.$$;

public enum EasyGames { ;

    static boolean logAll;
    static int logVolMax =
            Integer.MAX_VALUE;
            //9;

    static boolean logGoals;

    static class A_SineRider {
        static void main(String[] args) {
            run(new SineRider("s"));
        }
    }

    static class Apos {
        static void main(String[] args) {
            run(new NObvious("p", 1));
        }
    }

    static class Aneg {
        static void main(String[] args) {
            run(new NObvious("n", 0));
        }
    }

    static class Aposneg {
        static void main(String[] args) {
            run(new NObvious("c", 0, 1));
        }
    }

    static class A3 {
        static void main(String[] args) {
            run(new NObvious("c", 0, 0.5f, 1));
        }
    }
    static class ARandom {
        static void main(String[] args) {
            run(new NObvious("c", rngVector(3)));
        }

        private static float[] rngVector(int n) {
            var rng = new XoRoShiRo128PlusRandom();
            var f = new float[n];
            for (int i = 0; i < n; i++)
                f[i] =
                    //rng.nextFloat();
                    rng.nextBoolean() ? 1 : 0; //BINARY
            return f;
        }
    }
    static class B {
        static void main(String[] args) {
            run(
                new TrackXY($.atomic("t"), 3, 1)
            );
        }
    }

    static class C {
        static void main(String[] args) {
            run(
                new CopyGame("c", 1)
            );
        }
    }
    static class D {
        static void main(String[] args) {
            BitGame.BitGame1 b = new BitGame.BitGame1($$("b"), 2, 1);
            Player p = run(b);
            b.view();
        }
    }

    static class AGym {
        static void main(String[] args) throws IOException {
            Player p = new Player(new AIGymGame(
                //"Acrobot-v1"
                //"Pendulum-v1"
                "LunarLander-v2"
                //"BipedalWalker-v3"
                //"FrozenLake-v1"
                //"Blackjack-v1"
            )).fps(30);
            p.arith = false;
            //p.freqRes = 0.1f;
            p.nalStructural = false;
            p.complexMax = 16;
            //p.gameReflex = true;
            p.start();
        }
    }

    private static Player run(Game... x) {
        Player p = games(32, x);
        p.meta = false;
        p.complexMax = 9;
        p.threads = 4;
        p.nalStructural = false;
        p.arith = false;
        //p.focusLinks = 128;
        //p.implyerGame = p.implyer = false;
        //p.implBeliefify = p.implGoalify = false;
        //p.nalDelta = false;
        p.confMin = 0.001f;
        p.start();
        return p;
    }

    public static Player games(float fps, Game... x) {
        NAL.DEBUG = true;
        NAL.DEBUG_DERIVED_NALTASKS = false;//NAL.DEBUG;

        Collection<Game> g = List.of(x);

        Player p = new Player(
                g
//				new TrackXY_NAR($.the("b"), 3, 3, fps),
                //new TrackXY_NAR($.the("c"), 3, 3, fps)
//				new TrackXY_NAR($.the("t"),
//						//2, 1,
//						//3,1,
//						//3,3,
//						4,4,
//						//8,8,
//						fps)

        ).fps(fps);
//        p.ready(n-> g.forEach(G -> {
//            G.focus().dur(fps==0 ? 1 : 50);
////                G.onFrame(()->{
////                    System.out.println("durLoop=" + G.dur() + "\tdurFocus=" + G.durFocus());
////                });
//        }));



        //p.inperience = /*p.implBeliefify = p.implGoalify = */false;
        //p.eternalization = 0;

        //p.confMin = 1e-4f;
        //p.nalMin = 6;
        //p.freqRes = 0.25f;
        //g.forEach(G -> G.capacity(512));
        //p.conceptsMax = 16*1024;

        //p.eternalization = 0;

        //p.timeRes = 2;


        if (logAll)
            g.forEach(G->G.focus().log(t -> ((NALTask)t).complexity() <= logVolMax));

        //TEMPORARY
        //Exe.timer().schedule(()->System.exit(0), 5, TimeUnit.SECONDS);

//        g.forEach(G -> {
////            G.actions.forEach(a -> {
//                G.rewards.forEach(r -> {
//                    G.onFrame(()->{
////                        final Term ar = CONJ.the(a.term(), r.term());
//                        ImplTree.ImplTreeEvaluator e = new ImplTree(r.term(), p.nar).truth(p.nar);
//                        e.want($.t(1, p.nar.goalConfDefault.conf()));
//                    });
//                });
////            });
//        });

        if (logGoals) {
            g.forEach(G -> {
                Set<Term> a = G.actions.components().map(Termed::term).collect(toSet());
                G.focus().onTask(t -> {
                    if (/*!(t instanceof Curiosity.CuriosityTask) &&*/ t.GOAL() && a.contains(t.term())) {
                        //System.out.println(t);
                        NAR.proofPrint((NALTask) t);
                    }
                });
            });
        }
        //TEMPORARY
//        g.forEach(G -> {
//            Set<Term> watch =
//                    Stream.of(
//                            "(a==>r)","(--a ==> r)","(--a && --r)","(a&&r)"
//                    ).map(z -> $$(z).concept()).collect(toSet());
//            G.focus().onTask(t -> {
//                if (t.BELIEF() && watch.contains(t.term().concept())) {
//                    //System.out.println(t);
//                    p.nar.proofPrint((NALTask) t);
//                }
//            });
//        });
        return p;
    }

    static class NObviousUnitTests {
        static void main(String[] args) {
            int volMax = 6;
            int runSeconds = 4;
            float gameDur = 250;
            var g =
                    new NObvious("x", 1);
                    //new PoleCart($$("x"))
                    //new TrackXY($$("x"), 3, 3); g.ui = false;
                    //new Tetris();

            Player p = games(0, g);
            g.focus().log();
            p.uiTop = false;
            p.threads = 1;
            p.complexMax = volMax;
            p.timeRes = 1;
            //p.confMin = 0.1f;
            p.start();
            g.time.dur(gameDur);
            Util.sleepS(runSeconds);

            float rewardMean = (float) p.rewardMean();

            p.close();

            System.out.println("@" + p.nar.time() + " rewardMean=" + n2(rewardMean));

        }
    }
}