package nars.gui;

import jcog.data.map.UnifriedMap;
import jcog.event.Off;
import jcog.exe.Loop;
import jcog.pri.PLink;
import jcog.pri.UnitPri;
import jcog.thing.Part;
import nars.*;
import nars.focus.PriNode;
import nars.focus.util.TaskAttention;
import nars.game.Game;
import nars.gui.concept.ConceptListView;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.meta.LoopPanel;
import spacegraph.space2d.meta.Triggering;
import spacegraph.space2d.meta.obj.ObjectSurface;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.button.Submitter;
import spacegraph.space2d.widget.chip.ReplChip;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.Labelling;
import spacegraph.space2d.widget.textedit.TextEdit;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static jcog.Str.n2;
import static nars.$.$$;
import static nars.gui.GameUI.gameUI;

public enum FocusUI {
    ;

    public static Surface focusUI(Focus f) {
        var m = new Bordering();
        var n = f.nar;

        Map<String, Supplier<Surface>> xx = Map.of(
                //"_", () -> componentsView(f),
                "Stat", () -> focusStats(f),

//                "Histo", () -> histo(f, n),
//                "Chart", () -> linksChart(f, n),

                "Input", () -> narseseInput(f),
                "Meta", () ->
                    new ObjectSurface(f, 4),
                    //new ObjectSurface3.BlockRenderer(new ObjectSurface3.GraphedObject(f, 5)).render()

//                "Tasks", () -> new TaskListView(f, 96),
//                "Spect", () -> NARui.tasklinkSpectrogram(f, 300),
//                "Graph", () -> LinkGraph2D.linkGraph(f, n),
//                "Links", () -> BagView.bagChart(f.attn._bag, n)
                "Concepts", () -> new ConceptListView(f, 64),
                "Tasks", () -> new BagView<>(f.attn._bag, n)

        );
        TreeMap<String,Supplier<Surface>> x = new TreeMap<>(xx);
//        x.put("BELIEFS", () -> BagView.bagChart(f.tasks.beliefs, n));
//        x.put("GOALS", () -> BagView.bagChart(f.tasks.goals, n));
//        x.put("QUESTIONS", () -> BagView.bagChart(f.tasks.questions, n));
//        x.put("QUESTS", () -> BagView.bagChart(f.tasks.quests, n));

        Game g = f.local(Game.class);
        if (g != null) x.put("Game", ()->gameUI(g));

        PriNode p = f.pri;
        //NARPart p = f.meta(NARPart.class);
        if (p != null) x.put("Att", ()->AttentionUI.objectGraphs(p, n));



        m.center(new TabMenu(x));
        m.west(new MemUI(n));
        m.west(new Gridding(
                new PushButton("Clear", f::clear), //TODO n::clear "Clear All"

                Submitter.text("Load", u -> {
                    System.err.println("Load: TODO");
                    //throw new TODO();
                }),
                Submitter.text("Save", u -> {
                    System.err.println("Save: TODO");
                    //new TaskSummary().addConceptTasks(w.concepts()).reindex();
                    //throw new TODO(); //tagging
                })
                //new PushButton("List", f.concepts::print) //TODO better
//			new PushButton("Impile", () -> SpaceGraph.window(impilePanel(w), 500, 500)) //TODO better

        ));
//        m.east(new Gridding(
//                //TODO interactive filter widgets
//        ));

        return m;
    }

    private static @NotNull Gridding histo(NAR n, TaskAttention tasks, FloatFunction<PLink<NALTask>> priUnwrap) {
        var g = new Gridding();
        if (tasks.model instanceof TaskAttention.PuncBagAttentionModel p){
            g.add(new Gridding(
                    Labelling.the("Beliefs", new BagView<>(p.beliefs, priUnwrap, n)),
                    Labelling.the("Goals", new BagView<>(p.goals, priUnwrap, n)),
                    Labelling.the("Questions", new BagView<>(p.questions, priUnwrap, n)),
                    Labelling.the("Quests", new BagView<>(p.quests, priUnwrap, n))
            ));
        } else {
            g.add(new BagView<>((TaskAttention.BagAttentionModel) tasks.model, priUnwrap, n));
        }
        return g;
    }

    /** TODO improve */
    private static Surface componentsView(Focus f) {
        Function<Runnable, Off> trigger = f.nar::onDur;

        TabMenu v = new TabMenu();
        return new Triggering(v, trigger, new Consumer<Surface>() {

            final Map<String,Supplier<Surface>> m = new UnifriedMap<>();

            @Override
            public void accept(Surface n) {
                var unseen = new UnifiedSet(f.meta.keySet());
                f.meta.forEach((x, y) -> {
                    unseen.remove(x);
                    String xs = label(x);
                    if (!m.containsKey(xs))
                        m.put(xs, view(y));
                });

                for (var u : unseen) {
                    v.disable(label(u));
                    m.remove(u);
                }

                v.set(m);
            }

            private String label(Object x) {
                if (x instanceof String s) return s;
                return (x instanceof Class c ? c : x.getClass()).getSimpleName() + "_" +
                        System.identityHashCode(x);
            }

            private Supplier<Surface> view(Object y) {
                return ()->new ObjectSurface(y);
            }
        });
    }

//    private static Surface linksChart(Focus f, NAR n) {
//        return NARui.focusPanel(f.links,
//            Prioritized::priElseZero,
//            x -> x.term().toString(),
//            n);
//    }

    private static Surface focusStats(Focus f) {
        TextEdit t = new TextEdit(33, 6);
        StringBuilder tt = new StringBuilder();

        return new LoopPanel(Loop.of(()->{
            focusStats(tt, f);
            t.text(tt.toString());
            tt.setLength(0);
        }).fps(1)).set(t);
    }

    private static void focusStats(StringBuilder s, Focus f) {
        {
            var b = f.attn.concepts();
            var c = new DoubleSummaryStatistics();
            b.forEach(x -> c.accept(x.term.complexity()));

            s.append("Link\n");
            appendDoubleStats("  Cpx: ", c, s);
            //s.append("  Pri: ").append(n4(b.priMin())).append("..").append(n4(b.priMax())).append('\n');
        }
    }

    private static void appendDoubleStats(String label, DoubleSummaryStatistics d, StringBuilder s) {
        s.append(label).append(d.getMin()).append(".. ").append(n2(d.getAverage())).append(" ..").append(d.getMax()).append('\n');
    }

    public static ReplChip narseseInput(Focus f) {
        NAR n = f.nar;
        return new ReplChip((cmd, receive) -> {
            cmd = cmd.trim();

            //try to parse task
            try {

                Task h = Narsese.task(cmd, n);
                if (h.COMMAND() && !cmd.endsWith(";"))
                    throw new RuntimeException("FocusUI HACK"); //HACK

                f.accept(h);


            } catch (Throwable e) {

                //try to parse term
                try {
                    Term t = $$(cmd);
                    NARui.conceptWindow(t, n);
                } catch (Throwable t) {
                    t.printStackTrace();
                    receive.accept(t.toString());
                }

                receive.accept(e.toString());
            }
        });
    }

//    private static Surface impilePanel(Focus w) {
//
//
//        Was h = new Was();
//        h.add(w);
//
//
//        float dur = w.dur();
//
//        Graph2D<Object> gg = new Graph2D<>();
//        Surface g = gg
//                .update(new Force2D())
//                .render(new NodeGraphRenderer<Term, Task>() {
//
//
//                    //@Override
//                    protected void updateEdge(Term x, NALTask task, Term y, EdgeVis<Term> ee) {
//                        //super.updateEdge(x, task, y, ee);
//
//                        boolean notQuestion = task.BELIEF_OR_GOAL();
//                        float freq = notQuestion ? task.freq() : 1f /* assume CONJ seq (question) */;
//                        float polarization = Math.abs(freq - 0.5f) * 2;
//                        int dt = task.term().dt(); //assuming IMPL
//                        if (dt == DTERNAL) dt = 0; //HACK
//
//                        float immediacy = 1f / (1 + Util.sqrt(Math.abs(dt) / dur));
//
//                        float R = polarization;
//                        float G = polarization;
//                        float B = Fuzzy.and(polarization, immediacy);
//                        float a = 0.25f;
//
//                        ee.color(R, G, B, a);
//
//                        ee.weight(0.01f + (notQuestion ? (float) task.conf() : 0));
//                    }
//
//
//                    //@Override
//                    protected void style(NodeVis<Term> node) {
//                        Term t = node.id;
//                        if (t instanceof Neg)
//                            node.color(0.75f, 0.25f, 0.25f, 0.75f);
//                        else
//                            node.color(0.25f, 0.75f, 0.25f, 0.75f);
//
//                        float s = 1f / (1 + Util.sqrt(t.volume()));
//                        node.pri = s * 2;
//                    }
//                })
//                .set(h).widget();
//
//        return new Bordering(g).south(
//                new Gridding(
//                        new PushButton("Reload", () -> {
//                            h.add(w);
//                            gg.set(h);
//                        }),
//                        new PushButton("Research", () -> {
//                            h.grow(w.nar);
//                            gg.set(h);
//                        }),
//                        new PushButton("Clear", () -> {
//                            h.clear();
//                            gg.set(h);
//                        }),
//                        new PushButton("Load", () -> {
//                            //
//                        }),
//                        new PushButton("Save", () -> {
//                            //
//                        })
//                )
//        );
//
//    }

    public static Surface focusMixer(NAR n) {
        WhatMixer w = new WhatMixer(n);
        return NARui.get(w, w::commit, n);
    }

    public static Surface focusUI(NAR n) {

        return new TabMenu() {
            final Off updater;

            final Consumer<ObjectBooleanPair<Part<NAR>>> change = change -> {
                if (change.getOne() instanceof Focus) {
                    if (parent == null)
                        return;
                    update();
                }
            };

            {
                updater = n.eventOnOff.onWeak(change);

                update();
            }

            private void update() {
                n.runLater(this::_update);
            }

            final Map<String, Supplier<Surface>> attentions = new UnifriedMap<>();

            private synchronized void _update() {
                load();

                set(attentions);

                attentions.clear();
            }

            private void load() {
                //TODO live update:
                n.focus.stream().map(focusPLink -> focusPLink.id).forEach(
                    v  -> attentions.put(v.id.toString(), () -> focusUI(v)));
            }
        };
    }

    static class WhatMixer extends Gridding {

        private final NAR n;
        private Off off;

        WhatMixer(NAR n) {
            this.n = n;
        }

        private static Surface focusIcon(Focus p) {
            //TODO enable/disable checkbox
            return new Bordering()
                    .set(new Gridding(
                            new UnitPriSlider(p.freq).text("freq"),
                            new UnitPriSlider(p.pri.pri).text("pri")
                    )).north(
                            new PushButton(p.term().toString(), () -> {
                                //TODO refine
                                SpaceGraph.window(p, 400, 400);
                            })
                    );
        }

        @Override
        protected void starting() {
            super.starting();
            off = n.eventOnOff.on(this::update);
            update();
        }

        private void update() {
            List<Surface> channel = n.parts(Focus.class).map(WhatMixer::focusIcon).collect(toList());
            if (channel.isEmpty())
                set(new BitmapLabel("Empty"));
            else
                set(channel);
        }

        void commit() {
            forEachRecursively(x -> {
                if (x instanceof UnitPriSlider) {
                    ((UnitPriSlider) x).commit();
                }
            });
        }

        @Override
        protected void stopping() {
            off.close();
            off = null;
            super.stopping();
        }

        private static class UnitPriSlider extends FloatSlider {

            private final UnitPri p;
            float next = Float.NaN;

            UnitPriSlider(UnitPri p) {
                super(p);
                this.p = p;
            }

            public void commit() {
                float nextPri;
                if (next == next) {
                    p.pri(nextPri = next);
                    next = Float.NaN;
                } else {
                    nextPri = p.pri();
                }
                //System.out.println(p + " "+ nextPri);
                set(nextPri);
            }
        }
    }
}