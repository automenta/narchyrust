package nars.gui.concept;

import nars.Concept;
import nars.NAR;
import nars.Term;
import nars.concept.PermanentConcept;
import nars.gui.NARui;
import nars.term.Termed;
import nars.truth.evi.EviInterval;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.meta.MetaFrame;
import spacegraph.space2d.meta.obj.ObjectSurface;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.text.Labelling;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import static nars.Op.*;

public class ConceptSurface extends TabMenu {

    public ConceptSurface(Termed id, NAR n) {
        this(id.term(), n);
    }

    public ConceptSurface(Term id, NAR n) {
        super(conceptMenu(id, n));
    }

    public static Map<String, Supplier<Surface>> conceptMenu(Term x, NAR n) {
        Map<String, Supplier<Surface>> m = Map.of(
                x.toString(), () ->
                    new Gridding(
                        new Gridding(
                                new PushButton("question now", ()-> n.ask(x, QUESTION, now(n))),
                                new PushButton("question " + DELTA.str + " now", ()-> n.ask(DELTA.the(x), QUESTION, now(n))),
                                new PushButton("quest now", ()-> n.ask(x, QUEST, now(n))),
                                new PushButton("quest " + DELTA.str + " now", ()-> n.ask(DELTA.the(x), QUESTION, now(n)))
                        ),
                        Labelling.the("print...",
                            new Gridding(
                                new PushButton("beliefs", ()-> n.conceptualizeDynamic(x).beliefs().print()),
                                new PushButton("goals", ()-> n.conceptualizeDynamic(x).goals().print()),
                                new PushButton("questions", ()-> n.conceptualizeDynamic(x).questions().print()),
                                new PushButton("quests", ()-> n.conceptualizeDynamic(x).quests().print())
    
                                //..
                            )
                        )
                    ),
//                "budget", () -> {
//
//                    Term xx = x.concept();
//                    Plot2D p = new Plot2D(64, Plot2D.Line)
//                            .add("pri", () -> n.concepts.pri(xx, 0));
//
////                    CheckBox boost = new CheckBox("Boost");
//                    return DurSurface.get(//new Splitting<>(
//                            //boost,
//                            p
//                    //        , 0.8f)
//                    ,
//                    n, (nn) -> {
//                        p.update();
////                        if (boost.on()) {
////                            n.activate(xx, 1f);
////                        }
//                    });
//                },
                "beliefs", () -> new MetaFrame(NARui.beliefChart(x, n))
//                        "termlinks", () -> new BagView("TermLinks", n.concept(id).termlinks(), n),
//                "tasklinks", () -> new LabeledPane("TaskLinks", new BagView(n.concept(x).tasklinks(), n)),
//                "goal", () -> {
//                    return new Gridding(
//                            new PushButton("gOAL tRUE", (b) -> {
//                                long now = n.time();
//                                n.input(NALTask.the(x, GOAL, $.t(1f, n.confDefault(GOAL)), now, now, Math.round(now + n.dur()), n.evidence()).priSet(n.priDefault(GOAL)));
//                            }),
//                            new PushButton("gOAL fALSE", (b) -> {
//                                long now = n.time();
//                                n.input(NALTask.the(x, GOAL, $.t(0f, n.confDefault(GOAL)), now, now, Math.round(now + n.dur()), n.evidence()).priSet(n.priDefault(GOAL)));
//                            })
//                    );
//                },
//                "predict", () -> {
//                    return new Gridding(
//                            new PushButton("What +1", (b) -> {
//                                long now = n.time();
//                                n.input(NALTask.the(x, QUESTION, null, now, now, Math.round(now + n.dur()), n.evidence()).priSet(n.priDefault(QUESTION)));
//                            }),
//                            new PushButton("What +4", (b) -> {
//                                long now = n.time();
//                                n.input(NALTask.the(x, QUESTION, null, now, now + n.dur() * 3, now + n.dur() * 4, n.evidence()).priSet(n.priDefault(QUESTION)));
//                            }),
//                            new PushButton("How +1", (b) -> {
//                                long now = n.time();
//                                n.input(NALTask.the(x, QUEST, null, now, now, now + n.dur(), n.evidence()).priSet(n.priDefault(QUEST)));
//                            })
//                    );
//                }
        );
        Concept c = n.concept(x);
        if (c instanceof PermanentConcept) {
            m = new TreeMap<>(m);
            m.put(c.getClass().getSimpleName(), () -> new ObjectSurface(c));
        }
        return m;
    }

    /** TODO use Focus time */
    @Deprecated private static long[] now(NAR n) {
        var w = new EviInterval();
        var moment = w.setCenter(n.time(), Math.round(n.dur()));
        return new long[] {moment.s, moment.e};
    }
}