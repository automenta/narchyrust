package nars.gui.graph.run;

import com.google.common.base.Joiner;
import jcog.exe.Loop;
import nars.*;
import nars.func.kif.KIF;
import nars.gui.AttentionUI;
import nars.gui.NARui;
import nars.memory.SimpleMemory;
import nars.term.control.PREDICATE;
import spacegraph.SpaceGraph;

import java.io.IOException;
import java.util.List;

class ConceptGraph2DTest {


    static class DeriverView {
        public static void main(String[] args) {
            NAR n = NARS.shell();

            PREDICATE<Deriver> g = NARS.Rules.nal(1, 8).compile(n).what;
            SpaceGraph.window(AttentionUI.objectGraphs(g, n), 500, 500);

            n.startFPS(10f);
        }
    }

//    static class GraphMesh {
//        public static void main(String[] args) {
//
//            NAR n = NARS
//                    .tmp(8);
//            //.threadSafe(8);
//            //NAL.TRACE=true;
////            n.log();
//
//            Focus w = n.main();
//            n.questionPriDefault.pri(0.5f);
//            n.beliefPriDefault.pri(0.5f);
//            n.complexMax.set(16);
//
//
//            SpaceGraph.window(FocusUI.focusUI(w), 800, 500);
//            //            wg.dev();
//
//            n.startFPS(8);
//
//            new DeductiveMeshTest(n, 5, 5) {
//                @Override
//                protected Term edge(Term a, Term b) {
//                    return IMPL.the(a, b);
//                }
//            };
//
//            Surface g = LinkGraph2D.linkGraph(w, n);
//
//            SpaceGraph.window(g, 1200, 800);
//
//        }
//
//    }

//    static class InhibitionTest {
//        public static void main(String[] args) {
////            NAR n = NARS
////                    //.tmp(4);
////                    .threadSafe(7);
//            NAR n = new NARS.DefaultNAR(8, true) {
//                @Override
//                public Deriver deriver(ReactionModel m, NAR n) {
//                    return new TaskBagDeriver(m, n);
//                }
//            }.get();
////			standard(Derivers.nal(n, 1, 8)).compile();
//            //                try {
//            //                    System.in.read();
//            //                } catch (IOException e) {
//            //                    e.printStackTrace();
//            //                }
//            n.main().onTask(t -> n.proofPrint((NALTask) t));
//
//            n.time.dur(10);
//            n.complexMax.set(9);
//            n.freqRes.set(0.01f);
//
//            Surface g = LinkGraph2D.linkGraph(n.main(), n);
//            SpaceGraph.window(new Splitting(g, 0.2f, new Gridding(
//                        new Splitting(
//                                new Splitting(
//                                        new PushButton("good+").clicked(() ->
//                                                n.believe($$("good"), 1f, 0.9f, n.time(), Math.round(n.time() + ((double) n.dur())))
//                                        ), 0.5f,
//                                        new PushButton("good-").clicked(() ->
//                                                n.believe($$("good"), 0f, 0.9f, n.time(), Math.round(n.time() + ((double) n.dur())))
//                                        )
//                                ),
//                                0.5f,
//                                new Splitting(
//                                        new PushButton("bad+").clicked(() ->
//                                                n.believe($$("bad"), 1f, 0.9f, n.time(), Math.round(n.time() + ((double) n.dur())))
//                                        ), 0.5f,
//                                        new PushButton("bad-").clicked(() ->
//                                                n.believe($$("bad"), 0f, 0.9f, n.time(), Math.round(n.time() + ((double) n.dur())))
//                                        )
//                                )
//                        ),
//                        new Gridding(
//                                NARui.beliefCharts(n, $$("good"), $$("bad"), $$("reward")),
//                                NARui.beliefIcons(List.of($$("good"), $$("bad"), $$("reward")), n)
//                        ),
//                        new Gridding(
//                                new PushButton("reset").clicked(n::reset),
//                                new PushButton("print").clicked(() -> n.tasks().forEach(System.out::println))
//                        )
//                )), 1200, 800);
//
//            //n.log();
//
//
//            n.want("reward");
//            n.believe("(good ==> reward)", 1, 0.9f);
//            n.believe("(bad ==> reward)", 0, 0.9f);
//            //.mustGoal(cycles, "good", 1.0f, 0.81f)
//            //.mustGoal(cycles, "bad", 0.0f, 0.81f);
//
//            n.startFPS(8f);
//
//        }
//    }

//    static class GraphRDFTest1 {
//        public static void main(String[] args) throws FileNotFoundException {
//
//
//            NAR n = NARS.tmp();
////            n.log();
//
//
//            NQuadsRDF.input(n, new File("/home/me/d/valueflows.nquad"));
//
//            Surface g = LinkGraph2D.linkGraph(n.main(), n);
//
//            SpaceGraph.window(new Windo(g), 1200, 800);
//
//            n.startFPS(16f);
//        }
//
//    }

    /** SUMO/KIF demo */
    static class KIFDemo {

        private static boolean guiSpaceGraph = true;

        public static void main(String[] args) throws Narsese.NarseseException {

            final float dur = 8;

            NAR n = NARS.tmp(); //TODO increase max concepts
            n.beliefConfDefault.set(0.5f);
            n.goalConfDefault.set(0.5f);
            n.time.dur(dur);
//            n.beliefPriDefault.pri(0.15f);
//            n.questionPriDefault.pri(0.2f);
//            n.goalPriDefault.pri(0.3f);
//            n.questPriDefault.pri(0.1f);
            n.complexMax.set(32);
            Focus w = n.main();
            w.dur(dur);
            //w.logTo(System.out, (NALTask x) -> !x.isInput());
            w.log();

            ((SimpleMemory)(n.memory)).capacity(1024*1024);

            //new QueueDeriver(Derivers.core(Derivers.nal(0, 8, "motivation.nal")
//            .add((new AnswerQuestionsFromConcepts.AnswerQuestionsFromConceptIndex(new EternalTiming())
//
////                    .add(new AnswerQuestionsFromConcepts.AnswerQuestionsFromTaskLinks(timing))
////                    .add(new AnswerQuestionsFromIndex<>(new IndexedTermedList<>(kif, x -> {
////                        Term y;
////                        if (x instanceof Task) {
////                            y = x.subPath((byte) 0, (byte) 0);
////
////                        } else {
////                            y = x;
////                            if (y.IMPL()) {
////                                y = y.sub(n.random().nextInt(2));
////                            }
////                        }
////                        return IndexedTermedList.Atoms.apply(y);
////                    }), timing) {
////                        {
////                            maxAnswersPerQuestion.set(8);
////                        }
////
////                        @Override
////                        protected Term term(Task t) {
////                            return t.term().subPath((byte) 0, (byte) 0);
////                        }
////                    })))
//            ))), n)
//            ), n).everyCycle(w);

//            new QueueDeriver(new DeriverRules(n).add(new AnswerQuestionsFromConcepts.AnswerQuestionsFromTaskLinks(new DefaultTiming())
//                    .log(true)
//                    .taskVol(2, 12)
//            )
//                ).size(
//                    16,32, 16
//            ).onCycle(n);

            //n.start();
            //n.start();
            n.startFPS(32f);
            //n.run(1000);

            List<Task> kif = KIF.tasks(
                    //"tinySUMO"
                    "Merge"
                    //"WMD"
                    //"engineering"
            );
            kif.forEach(System.out::println);


            List<Task> tasks = n.input(Joiner.on('\n').join(
//                    "(?1 --> Process)?",
//                    "(#x --> CausingHappiness)!",

                    "(&&,(#x --> Organism),(#x --> Happiness),(#x --> Living),--(#x --> Dead))!",

                    //"(user --> Living)!",
                    //"--(user --> Dead)!",

                    //"--(#x --> Death)!",
                    //"--(#x --> Killing)!",

                    //"possesses(#u,#everything)!",
                    //"--possesses(--#u, #anything)!",
                    "patient(?u,?x)?",
                    "causes(?u,?x)?",
                    "experiencer(?u,?x)?",
                    "desires(?u,?x)?",
                    "believes(?u,?x)?",
                    "hasPurpose(?u,?x)?",
                    "(?x --> Likely)?",
                    "gainsControl(?event,?agent)?"
//                    n.input("$1.0 possesses(I,#everything)!");
//            n.input("$1.0 --({I}-->Dead)!");
//            n.input("$1.0 Human:{I}.");
//            n.input("$1.0 uses(#anything, I).");
//            n.input("$1.0 --needs(I, #all)!");
//            n.input("$1.0 --lacks(I, #anything)!");
//            n.input("$1.0 benefits(#all, I)!");
//            n.input("$1.0 --suffers(#anything, I)!");
//            n.input("$1.0 income(I, #money, #anyReason)!");
//            n.input("$1.0 --(I-->Hungry)!");
//            n.input("$1.0 --(I-->Thirsty)!");
//            n.input("$1.0 enjoys(I,?x)?");
//            n.input("$1.0 dislikes(I,?x)?");
//            n.input("$1.0 needs(I,?x)?");
//            n.input("$1.0 wants(I,?x)?");
//            n.input("$1.0 patient(?what,I)?");
            ));



//            Surface g = TaskLinkGraph2D.tasklinks(n);
//            window(new Windo(g), 1200, 800);

            Loop.of(() -> n.runLater(new Runnable() {
                /** TODO CAS */
                volatile boolean busy = false;

                @Override
                public void run() {

                    if (busy)
                        return;

                    try {
                        busy = true;

                        int kifs = 10;
                        for (int i = 0; i < kifs; i++)
                            reinput(kif, n);

                        reinput(tasks, n);
                    } finally {
                        busy = false;
                    }
                }
            })).setPeriodMS(1000);
            //fps(16f);

            if (guiSpaceGraph)
                SpaceGraph.window(NARui.top(n), 800, 500);
            else {
                try {
                    new TUI(n);
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        static void reinput(List<Task> l, NAR n) {
            Task t = l.get(n.random().nextInt(l.size()));
            t.pri(n.priDefault(t.punc()));
            n.input(t);
        }
    }
}