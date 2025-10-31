package nars.func.kif;

import jcog.Util;
import nars.*;
import nars.action.answer.AnswerQuestionsFromIndex;
import nars.deriver.impl.SerialDeriver;
import nars.focus.time.ActionTiming;
import nars.memory.HijackMemory;
import nars.memory.RadixTreeMemory;
import nars.term.Neg;
import nars.term.Termed;
import nars.term.util.map.IndexedTermedList;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static nars.$.$$;
import static nars.Op.*;
import static nars.term.util.Testing.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

//@Disabled
class KIFTest {

    @Test void separateVariables() {
        final String x = "(=>" +
                "(instance ?ORE LeadOre)" +
                "(exists (?METAL)" +
                "(and" +
                "(instance ?METAL Lead)" +
                "(component ?METAL ?ORE))))";
        assertEquals("[task((({$ORE}-->LeadOre)==>(component(#METAL,$ORE)&&({#METAL}-->Lead))))]",
            new KIF(x).tasks().map(Termed::term).toList().toString());
    }

    /** sumo test, see sumo/tests/[method].kif.tq */
    @Test void TQG1() {
        /*
        (note TQG1)  ;; boolean version

        (time 240)

        (instance Org1-1 Organization)

        (query (exists (?MEMBER) (member ?MEMBER Org1-1)))

        (answer yes)

         */
        //TODO
    }

    @Disabled @Test
    void a1() {

//        NAL.DEBUG = true;


        //String O = "/home/me/d/sumo_merge.nal";
//        k.tasks.forEach(bb -> System.out.println(bb));


        NAR n = new NARS().memory(
            new RadixTreeMemory(128*1024)
            //new HijackMemory(256*1024, 4)
        ).get();

//        new Deriver(Derivers.nal(n, 1,8, "motivation.nal"));

        n.complexMax.set(14);

        n.time.dur(16);
        //TaskLinkWhat w = (TaskLinkWhat) n.what();


        List<Task> kif = KIF.tasks("Merge",
                "Mid-level-ontology",
                "emotion",
                "People",
                "Economy",
                "FinancialOntology"
                //            "Catalog"
                //"ComputerInput"
        );

        Focus f = n.main(); //new BagFocus($$("sumo_x"), 512);
        //n.add(w);

//        w.updater = new WhatThe.BagSustain(0.99f);
        Deriver d =
                //new BagDeriver(Derivers.core(Derivers.nal(1, 8)), n);
                new SerialDeriver(NARS.Rules.nal(1, 8).core().temporalInduction().compile(n), n);
		d.everyCycle(f);

        f.log();

        //seed stage:
        {
            n.beliefPriDefault.pri(0.1f);

            int cyclesPerKIFRule = 2;
            kif.forEach(t -> {
//            w.clear();
                f.accept(t); //HACK
                n.run(cyclesPerKIFRule);
                //w.bag.bag.commit(w.bag.bag.forget(3));
            });
        }

        n.beliefPriDefault.pri(0.5f);
        n.goalPriDefault.pri(0.5f);
        //System.out.println(Joiner.on("\n").join(n.stats().entrySet()));

        ActionTiming timing = new ActionTiming();
		//.add(new AdjacentLinks(new ExhaustiveIndexer()))
		//.add(new AdjacentLinks(new CommonAtomIndexer()))
		//.add(new AdjacentLinks(new FirstOrderIndexer()))
        //				Term y;
        //				if (x instanceof NALTask) {
        //					y = x.term().sub((byte) 0, (byte) 0);
        //				} else {
        //					y = (Term)x;
        //				}
        //				Term y;
        //				if (x instanceof NALTask) {
        //					y = x.term().sub((byte) 0, (byte) 0);
        //				} else {
        //					y = (Term)x;
        //				}
        new SerialDeriver(((NARS.Rules) NARS.Rules.nal(0, 0, "motivation.nal")

//                .add(new AnswerQuestionsFromConcepts.AnswerQuestionsFromTaskLinks(timing))
                .add(new AnswerQuestionsFromIndex<>(new IndexedTermedList<>(kif, y -> {
//				Term y;
//				if (x instanceof NALTask) {
//					y = x.term().sub((byte) 0, (byte) 0);
//				} else {
//					y = (Term)x;
                    if (y.IMPL()) {
                        y = y.sub(n.random().nextInt(2));
                    }
//				}
                    return IndexedTermedList.Atoms.apply(y);
                }), timing) {
                    {
                        maxAnswersPerQuestion.set(8);
                    }

                    @Override
                    protected Term term(Task t) {
                        return t.term().sub((byte) 0, (byte) 0);
                    }
                })).core().stm().temporalInduction().compile(n), n).everyCycle(f);


		f.clear();

        //n.input("$1.0 ({?ACT}-->JoystickMotion)?");
        //n.input("$1.0 classIntersection(?1,?2)?");
        //n.input("$1.0 (#1-->ComputerDisplay)!");
        //n.clear();
//        w.onTask(t-> {
//            if (!t.isQuestionOrQuest() && !t.isInput())
//                System.out.println(t.toString(true));
//        });
        //w.accept(new EternalTask($$("(#x --> Damaging)"), BELIEF, $.t(1, 0.9f), n).priSet(1));
        goal(f, "(#x <-> CausingHappiness)", "--(#x <-> Death)", "(#x <-> SentientAgent)",
            "--(user <-> Death)",
            "(user <-> Living)",
            "possesses(user,#everything)",
            "--possesses(--user,#anything)"
            );
//        believe(w, "(user --> Animal)", "--(Death <-> Living)");
        question(f, QUESTION,
            "patient(user,?x)",  "causes(user,?x)", "experiencer(user,?x)",
                "desires(user,?x)",
                "believes(user,?x)",
                "hasPurpose(user,?x)"
//            "(?1 <-> CausingHappiness)",
//            "(?1 <-> Damaging)",
//            "(?1 <-> Death)",
//            "(?1 <-> Unhappiness)"
        );
//        question(w, QUEST,
//            "located(?1,?2)"
////            "(&&,(?x-->holdsDuring),--(?x --> Death))",
////            "(&&,(?x-->holdsDuring),--(?x --> Unhappiness))"
//            );
//        w.accept(((Task) new EternalTask($$("(patient(#p,#a) && ({#p}-->Organism))"), BELIEF, $.t(1, 0.9f), n)).pri(0.05f));
//        w.accept(((Task) new EternalTask($$("({#x} --> Damaging)"), GOAL, $.t(0, 0.9f), n)).pri(0.05f));
//        w.accept(((Task) new EternalTask($$("(Death --> Damaging)"), BELIEF, $.t(1, 0.9f), n)).pri(0.05f));
//        w.accept(((Task) new EternalTask($$("(Unhappiness --> Damaging)"), BELIEF, $.t(1, 0.9f), n)).pri(0.05f));
        //w.accept(new EternalTask($$("(#x --> Death)"), GOAL, $.t(0, 0.9f), n).priSet(1));
        //w.accept(new EternalTask($$("(?x ==> (?y --> Damaging))"), QUESTION, null, n).priSet(1));
        //n.input("$1.0 possesses(I,#everything)!");
//        n.input("$1.0 benefits(#all, I)!");
//        n.input("$1.0 uses(#anything, I).");
//        n.input("$1.0 occupiesPosition(I,#position,#org)."); //http://sigma.ontologyportal.org:8080/sigma/Browse.jsp?flang=SUO-KIF&lang=EnglishLanguage&kb=SUMO&term=occupiesPosition
//        n.input("$1.0 --({I}-->Dead)!");
//        n.input("$1.0 Human:{I}.");
        n.run(30000);
//        w.bag.print();

//        n.concepts().forEach(c -> System.out.println(c));

    }


    static void goal(Focus w, String... assertions) {
        for (String assertion : assertions) {
            var t = $$(assertion);
            w.accept(NALTask.taskEternal(t.unneg(), GOAL, $.t(t instanceof Neg ? 0 : 1, 0.9f), w.nar).withPri(0.5f));
        }
    }
    static void believe(Focus w, String... assertions) {
        for (String assertion : assertions) {
            Term t = $$(assertion);
            w.accept(NALTask.taskEternal(t.unneg(), BELIEF, $.t(t instanceof Neg ? 0 : 1, 0.9f), w.nar).withPri(0.5f));
        }
    }

    static void question(Focus w, byte punc, String... questions) {
        for (String assertion : questions)
            w.accept(NALTask.taskEternal($$(assertion), punc, null, w.nar).withPri(0.5f));
    }

    @Test void _TQG2() throws Narsese.NarseseException {
        //(query (property TheKB2_1 Inconsistent))
        //(answer yes)
        NAR n = new NARS().memory(
            //new RadixTreeMemory(128*1024)
            new HijackMemory(128*1024, 4)
        ).get();

//		standard(Derivers.nal(n, 6,8)).compile();

//        new Deriver(Derivers.nal(n, /*NAL*/6, /*NAL*/8), new FirstOrderIndexer()); // ~= PROLOG

        String t = "(instance TheKB2_1 ComputerProgram)\n" +
                "(instance Inconsistent Attribute)\n" +
                "\n" +
                "(=>\n" +
                "  (and\n" +
                "    (contraryAttribute ?ATTR1 ?ATTR2)\n" +
                "    (property ?X ?ATTR1)\n" +
                "    (property ?X ?ATTR2))\n" +
                "  (property TheKB2_1 Inconsistent))\n" +
                "\n" +
                "(instance Entity2_1 Organism)\n" +
                "(instance Entity2_2 Organism)\n" +
                "(mother Entity2_1 Entity2_2)\n" +
                "(father Entity2_1 Entity2_2)";
        KIF k = new KIF(t);
        n.input(k.tasks());

        n.input("$1.0 property(TheKB2_1, Inconsistent)?");
        n.run(1000);


    }

    @Test void _TQG4() throws Narsese.NarseseException {
        //            "(query (not (holdsDuring (WhenFn DoingSomething4-1) (attribute Entity4-1 Dead))))\n" +
//            "(answer yes)\n";
        NAR n = new NARS().memory(new RadixTreeMemory(128*1024)).get();

//		standard(Derivers.nal(n, 5,8)).compile();

//        new Deriver(Derivers.nal(n, /*NAL*/5, /*NAL*/8), new FirstOrderIndexer()); // ~= PROLOG
        n.log();

        String t = "(instance Entity4_1 Human)\n" +
                "\n" +
                "(instance DoingSomething4_1 IntentionalProcess)\n" +
                "\n" +
                "(agent DoingSomething4_1 Entity4_1)\n" +
                "\n" +
                "(=>\n" +
                "  (and\n" +
                "    (agent ?PROC ?AGENT)\n" +
                "    (instance ?PROC IntentionalProcess))\n" +
                "  (and\n" +
                "    (instance ?AGENT CognitiveAgent)\n" +
                "    (not\n" +
                "      (Dead ?PROC ?AGENT ) )))";
        KIF k = new KIF(t);
        n.input(k.tasks());

        n.input("$1.0 Dead(DoingSomething4_1,Entity4_1)?");
        n.run(3000);


    }

    private static final String formula1 = "(and (instance ?RELATION TransitiveRelation) (instance ?RELATION SymmetricRelation) (reflexiveOn ?RELATION ?CLASS))";

    @Test void formulaToArgs1() {
        assertEq(
                "(&&,reflexiveOn(#RELATION,#CLASS),({#RELATION}-->SymmetricRelation),({#RELATION}-->TransitiveRelation))",
                new KIF().
                        formulaToTerm(formula1, 1)
        );
    }

    @Test void formulaToProlog1() {
        assertEquals(
            "and(instance(RELATION,'TransitiveRelation'),instance(RELATION,'SymmetricRelation'),reflexiveOn(RELATION,CLASS))",
            KIF.formulaToProlog(formula1)
        );
    }

//    @Test
//    public void SUMOViaMemory() {
//        String sumo =
//                "Transportation";
//        //"People";
//        //"Merge";
//        //"Law";
//        String inURL = "file:///home/me/sumo/" + sumo + ".kif";
//
//        NAR n = NARS.shell();
//        n.memoryExternal.on(KIF.load);
//
//
//        Term I = $$(inURL);
//        Term O =
//                //n.self();
//                //Atomic.the("stdout");
//                Atomic.the("file:///tmp/x.nalz");
//
//        Runnable r = n.memoryExternal.copy(I, O);
//        r.run();
//
//
//    }
//
//    @Test
//    public void SUMOViaMemory2() {
//        String sumo =
//                "Merge";
//        //"People";
//        //"Merge";
//        //"Law";
//        String inURL = "file:///home/me/sumo/" + sumo + ".kif";
//
//        NAR n = NARS.
//                tmp();
//
//
//        n.memoryExternal.on(KIF.load);
//
//
//        Term I = $.quote(inURL);
//        Term O =
//                //Atomic.the("stdout");
//                //Atomic.the("file:///tmp/x.nal");
//                n.self();
//
//        n.log();
//
//        Runnable r = n.memoryExternal.copy(I, O);
//        r.run();
//
//
//        n.run(10000);
//
//        AdjGraph<Term, Task> structure = new AdjGraph<>(true);
//        n.tasks().forEach(t -> {
//            switch (t.op()) {
//                case INH: {
//                    int s = structure.addNode(t.sub(0));
//                    int p = structure.addNode(t.sub(1));
//                    structure.edge(s, p, t);
//                    break;
//                }
//
//            }
//        });
//
//
//        structure.nodeSet().forEach((t) -> {
//            System.out.println(t + " " +
//                    GraphMeter.clustering((AdjGraph) structure, t)
//            );
//        });
//
//
//    }
//
//    @Test
//    public void Generate() {
//        String sumoDir = "file:///home/me/sumo/";
//
//        try {
//            Files.createDirectory(Paths.get("/tmp/sumo"));
//        } catch (IOException e) {
//            //e.printStackTrace();
//        }
//
//        NAR n = NARS.shell();
//
//        n.memoryExternal.on(KIF.load);
//
//
//        n.memoryExternal.contents(sumoDir).parallel().forEach(I -> {
//            String ii = Texts.unquote(I.toString());
//            if (!ii.endsWith(".kif"))
//                return;
//
//            if (ii.contains("WorldAirports")) //exclusions
//                return;
//
//            String name = ii.substring(ii.lastIndexOf('/') + 1, ii.lastIndexOf('.')).toLowerCase();
//
//            n.memoryExternal.copy(I, Atomic.the("file:///tmp/sumo/" + name + ".nalz")).run();
//            n.memoryExternal.copy(I, Atomic.the("file:///tmp/sumo/" + name + ".nal")).run();
//        });
//
//    }

    @Disabled
    @Test
    void Load() throws Narsese.NarseseException {


        NAR n = NARS.tmp();
        //NARchy.core();

        n.beliefPriDefault.pri(0.05f);

        n.input("load(\"file:///tmp/sumo/merge.nalz\");");
//        n.input("load(\"file:///tmp/sumo/Mid-level-ontology.kif.nalz\");");
//        n.input("load(\"file:///tmp/sumo/FinancialOntology.kif.nalz\");");
//        n.input("load(\"file:///tmp/sumo/Economy.kif.nalz\");");
        n.run(1);
        System.err.println(n.memory.size() + " concepts");
        n.clear();

//        n.logPriMin(System.out, 0.01f);


//        Deriver.derivers(n).forEach( (d)->
//                ((DefaultDerivePri)(((BatchDeriver)d).budgeting))
//                        .gain.setAt(0.2f) );

        n.input("$1.0 possesses(I,#everything)!");
        n.input("$1.0 uses(#anything, I).");
        n.input("$1.0 --({I}-->Dead)!");
        n.input("$1.0 Human:{I}.");
        n.input("$1.0 --needs(I, #all)!");
        n.input("$1.0 --lacks(I, #anything)!");
        n.input("$1.0 benefits(#all, I)!");
        n.input("$1.0 --suffers(#anything, I)!");
        n.input("$1.0 income(I, #money, #anyReason)!");
        n.input("$1.0 --(I-->Hungry)!");
        n.input("$1.0 --(I-->Thirsty)!");
        n.input("$1.0 enjoys(I,?x)?");
        n.input("$1.0 dislikes(I,?x)?");
        n.input("$1.0 needs(I,?x)?");
        n.input("$1.0 wants(I,?x)?");
        n.input("$1.0 patient(?what,I)?");


        n.startFPS(10f);
        Util.sleepS(40);
        n.stop();

    }


    @Disabled @Test
    void CapabilityExtension() throws IOException {

        KIF k = KIF.file("/home/me/sumo/Merge.kif");
        for (var assertion : k.assertions) {
            System.out.println(assertion);
        }

    }

}