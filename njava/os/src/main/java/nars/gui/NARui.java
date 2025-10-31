package nars.gui;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import jcog.Util;
import jcog.cluster.BagClustering;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.table.Baglike;
import jcog.event.Off;
import jcog.pri.PLink;
import jcog.signal.IntRange;
import jcog.thing.Thing;
import nars.*;
import nars.gui.concept.ConceptColorIcon;
import nars.gui.concept.ConceptSurface;
import nars.link.PremiseSnapshot;
import nars.link.TaskLink;
import nars.term.Termed;
import nars.time.part.DurLoop;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.layout.TreeMap2D;
import spacegraph.space2d.meta.MetaFrame;
import spacegraph.space2d.meta.Surfaces;
import spacegraph.space2d.meta.Triggering;
import spacegraph.space2d.meta.obj.ObjectSurface;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.meter.ImmediateMatrixView;
import spacegraph.space2d.widget.meter.ScatterPlot2D;
import spacegraph.space2d.widget.meter.Spectrogram;
import spacegraph.space2d.widget.text.Labelling;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.space2d.widget.textedit.TextEdit;
import spacegraph.util.MutableRectFloat;
import spacegraph.video.Draw;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jogamp.newt.event.KeyEvent.VK_ENTER;
import static jcog.data.iterator.Concaterator.concat;
import static nars.$.$$;
import static nars.Op.*;
import static nars.gui.BagView.bagChart;
import static spacegraph.space2d.container.grid.Containers.grid;

/**
 * SpaceGraph-based visualization utilities for NARchy
 */
public enum NARui {
    ;

//	static {
//		ObjectSurface.builtin.put(Reflex.class,
//			(Reflex r, Object h) -> reflexUI(r));
//	}

    public static Surface beliefCharts(NAR nar, Termed... x) {
        return beliefCharts(ArrayIterator.iterable(x), nar);
    }

    public static Surface beliefCharts(Stream<? extends Termed> ii, NAR nar) {
        return beliefCharts((Iterable) (ii::iterator), nar);
    }

    public static Surface beliefCharts(Iterable<? extends Termed> ii, NAR nar) {
//		return new Triggering<>(nar::onDur, new Surfaces(()->stream(ii).map(i ->
//				new Triggering<>(new MetaFrame(new BeliefTableChart(i)), m ->
//						((BeliefTableChart)m.center()).update(nar))).iterator())
//		);
        BeliefTableChart.BeliefTableChartParams p = new BeliefTableChart.BeliefTableChartParams();
        return new Bordering<Surface>(new Triggering<>(nar::onDur, new Surfaces(Iterables.transform(ii, i ->
                Labelling.the(i.toString(), new Triggering<>(
                        new BeliefTableChart(i, p), m -> m.update(nar))))
        ))).south(new ObjectSurface(p));
    }

    public static Surface beliefChart(Termed x, NAR nar) {
        return /*new Widget*/(
                new MetaFrame(get(new BeliefTableChart(x), B -> B.update(nar), nar))
        );
    }

    public static Surface top(NAR n) {
        return new Bordering()
                .north(ExeUI.exeUI(n))
                .center(
                        new Splitting(
                                new TabMenu(Map.of(
                                        "*", () -> new TabMenu(menu(n)/* , new WallMenuView() */),
                                        "?", () -> bagChart(n.focus, n))),
                                0.8f,
                                FocusUI.focusUI(n)
                        ).resizeable()
                        //new LazySurface(()->AttentionUI.objectGraphs(n.control.graph.nodeIDs(), n))
                )
                //.south(new LazySurface(()->new OmniBox(new OmniBox.JShellModel()))) //TODO
                ;
    }

    public static HashMap<String, Supplier<Surface>> parts(Thing p) {
        return (HashMap<String, Supplier<Surface>>) p.partStream().collect(Collectors.toMap(Object::toString,
                s -> () -> new ObjectSurface(s),
                (a, b) -> b, (Supplier<HashMap<String, Supplier<Surface>>>) HashMap::new));
    }

    public static HashMap<String, Supplier<Surface>> menu(NAR n) {
        Map<String, Supplier<Surface>> m = Map.of(
                //"shl", () -> new ConsoleTerminal(new TextUI(n).session(10f)),
                "nar", () -> new ObjectSurface(n, 2),
                //"on", () -> new ObjectSurface(n.whens().entrySet(), 2),
                "exe", () -> ExeUI.exeStats(n),
                //"val", () -> ExeUI.valuePanel(n),
                "att", () -> attentionUI(n),
                "wha", () -> FocusUI.focusMixer(n),
                "mem", () -> new MemUI(n)
                //"mem", () -> MemEdit(n),
//                "how", () -> ExeCharts.howChart(n),
                //"can", () -> //ExeCharts.causeProfiler(n),
                //focusPanel(n)
                ///causePanel(n),
                //"svc", () -> new PartsTable(n),
                //"cpt", () -> new ConceptBrowser(n)
        );
        HashMap<String, Supplier<Surface>> mm = new HashMap<>() {{
            putAll(m);
//			put("snp", () -> memoryView(n));
            put("tsk", () -> taskView(n));
//            put("mem", () -> ScrollGrid.list(
//                (int x, int y, Term v) -> new PushButton(m.toString()).click(() ->
//                        window(
//                                ScrollGrid.list((xx, yy, zm) -> new PushButton(zm.toString()), n.memory.contents(v).collect(toList())), 800, 800, true)
//                ),
//                n.memory.roots().collect(toList())
//                )
//        );
        }};

        return mm;
    }

    private static Surface attentionUI(NAR n) {
        return AttentionUI.objectGraphs(
                () -> concat(n.partStream()
                                //.filter(x -> !(x.g instanceof DurSurface)) //TODO
                                .filter(x -> !(x instanceof DurLoop))
                                .iterator(),
                        n.pri.graph.nodeIDs()
                ),
                n);
    }

//    private static Surface priView(NAR n) {
//        TaskLinks cc = n.attn;
//
//        return Splitting.row(
//                new BagView<>(cc.links, n),
//                0.2f,
//                new Gridding(
//                     new ObjectSurface(
////                        new XYSlider(
////                                cc.activationRate
//                                cc.decay
//                                //.subRange(1/1000f, 1/2f)
//                        ) {
////                            @Override
////                            public String summaryX(float x) {
////                                return "forget=" + n4(x);
////                            }
////
////                            @Override
////                            public String summaryY(float y) {
////                                return "activate=" + n4(y);
////                            }
//                        },
//
//                        new PushButton("Print", () -> {
//                            Appendable a = null;
//                            try {
//                                a = TextEdit.out().append(
//                                        Joiner.on('\n').join(cc.links)
//                                );
//                                window(a, 800, 500);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }),
//                        new PushButton("Clear", () -> cc.links.clear())
//                )
//        );
//
//    }

    @SuppressWarnings("RedundantCast")
    public static Surface taskView(NAR n) {
        List<Predicate<NALTask>> filter = new CopyOnWriteArrayList<>();
        Consumer<Task> printer = t -> {
            if (Util.and(t, (Iterable) filter))
                System.out.println(t);
        };

        return Labelling.the("Trace",
                grid(
                        grid(
                                new CheckBox("Belief").on(taskTrace(n, BELIEF, printer)),
                                new CheckBox("Goal").on(taskTrace(n, GOAL, printer)),
                                new CheckBox("Question").on(taskTrace(n, QUESTION, printer)),
                                new CheckBox("Quest").on(taskTrace(n, QUEST, printer))
                        ),
                        grid(
                                (Surface) new CheckBox("Not Eternal").on(taskFilter(filter, (x) -> !x.ETERNAL())),
//					new CheckBox("Not Signal").on(taskFilter(filter, (x) -> !(x instanceof Signal))),
                                (Surface) new CheckBox("Not Input").on(taskFilter(filter, (x) -> x.stamp().length > 1))
                                //TODO priority and complexity sliders
                        )
                )
        );
    }

    static BooleanProcedure taskFilter(List<Predicate<NALTask>> ff, Predicate<NALTask> f) {
        return new BooleanProcedure() {
            @Override
            public synchronized void value(boolean on) {
                if (on) {
                    ff.add(f);
                } else {
                    var rem = ff.remove(f);
                    assert (rem);
                }
            }
        };
    }


    static BooleanProcedure taskTrace(NAR n, byte punc, Consumer<Task> printer) {
        return new BooleanProcedure() {

            private Off off;

            @Override
            public synchronized void value(boolean b) {
                if (b) {
                    assert (off == null);
                    off = n.main().onTask(printer, punc);
                } else {
                    assert (off != null);
                    off.close();
                    off = null;
                }
            }
        };
    }

//    public static Surface taskTable(NAR n) {
//
//        int cap = 32;
//        float rate = 1f;
//
//        CheckBox updating = new CheckBox("Update");
//        updating.on(true);
//
//        /** TODO make multithread better */
//        PLinkArrayBag<Task> b = new PLinkArrayBag<>(PriMerge.replace, cap);
//        List<Task> taskList = new FasterList();
//
//        ScrollXY tasks = ScrollXY.listCached(t ->
//                        new Splitting<>(new FloatGuage(0, 1, t::priElseZero),
//                                new PushButton(new VectorLabel(t.toStringWithoutBudget())).click(() -> {
//                                    conceptWindow(t, n);
//                                }),
//                                false, 0.1f),
//                taskList, 64);
//        tasks.view(1, cap);
//
//        TextEdit0 input = new TextEdit0(16, 1);
//        input.onKey((k) -> {
//            if (k.getKeyType() == KeyType.Enter) {
//                //input
//            }
//        });
//
//
//        Surface s = new Splitting(
//                tasks,
//                new Gridding(updating, input /* ... */)
//                , 0.1f);
//
//        Off onTask = n.onTask((t) -> {
//            if (updating.on()) {
//                b.put(new PLinkHashCached<>(t, t.priElseZero() * rate));
//            }
//        });
//        return DurSurface.get(s, n, (nn) -> {
//
//        }, (nn) -> {
//            if (updating.on()) {
//                synchronized (tasks) {
//                    taskList.clear();
//                    b.commit();
//                    b.forEach(x -> taskList.add(x.get()));
//                    tasks.update();
//                }
//            }
//        }, (nn) -> {
//            onTask.off();
//        });
//    }

//	private static Surface memoryView(NAR n) {
//
//		return new ScrollXY<>(new KeyValueGrid(new MemorySnapshot(n).byAnon),
//			(x, y, v) -> {
//				if (x == 0) {
//					return new PushButton(v.toString()).clicked(() -> {
//
//					});
//				} else {
//					return new VectorLabel(((Collection) v).size() + " concepts");
//				}
//			});
//	}

    public static void conceptWindow(String t, NAR n) {
        conceptWindow($$(t), n);
    }

    public static void conceptWindow(Termed t, NAR n) {
        SpaceGraph.window(new ConceptSurface(t, n), 500, 500);
    }

    public static Surface implMatrix(Iterable<? extends Termed> subjs, Iterable<? extends Termed> preds, int dt, NAR nar) {
        return new Triggering<>(nar::onDur, new Gridding(Streams.stream(subjs).map(s -> {
            Gridding jj = new Gridding();
            for (Termed p : preds) {
                Gridding uu = new Gridding(2);
                for (boolean sNeg : new boolean[]{false, true}) {
                    uu.add(new Triggering<>(
                        new ConceptColorIcon(IMPL.the(s.term().negIf(sNeg), dt, p.term()), nar),
                        z -> z.update(nar)));
                }
                jj.add(uu);
            }
            return jj;
        })));
    }

    public static Surface beliefIcons(Iterable<? extends Termed> c, NAR nar) {
        return new Triggering<>(nar::onDur, new Gridding(Streams.stream(c).map(x ->
                new Triggering<>(new ConceptColorIcon(x, nar),
                        z -> z.update(nar)))));
    }

    public static TextEdit newNarseseInput(NAR n, Consumer<Task> onTask, Consumer<Exception> onException) {
        var input = new TextEdit(16, 1);
        input.onKeyPress((k) -> {
            if (k.getKeyCode() == VK_ENTER) {
                var s = input.text();
                input.text("");
                try {
                    var t = n.input(s);
                    for (Task task : t) {
                        onTask.accept(task);
                    }
                } catch (Narsese.NarseseException e) {
                    onException.accept(e);
                }
            }
        });
        return input;
    }

    public static Surface clusterView(BagClustering<NALTask> c, NAR n) {

        ScatterPlot2D.ScatterPlotModel<PLink<NALTask>> model = new ScatterPlot2D.SimpleXYScatterPlotModel<>() {

            final float[] c = new float[4];
            private long now;

            @Override
            public MutableRectFloat layout(float[][] in, float[][] out) {
                now = n.time();
                return super.layout(in, out);
            }

            @Override
            public void coord(PLink<NALTask> v, float[] target) {
                var t = v.id;
                target[0] = t.mid() - now; //to be certain of accuracy with 32-bit reduced precision assigned from long
                target[1] = t.priElseZero();
            }

            @Override
            public String label(PLink<NALTask> id) {
                return id.id
                        .term().toString();
                //toStringWithoutBudget();
            }

            @Override
            public float pri(PLink<NALTask> v) {
                return v.priElseZero();
            }

            @Override
            public void colorize(PLink<NALTask> v, NodeVis<PLink<NALTask>> node) {
//				var centroid = v.centroid;
//
                var a = v.priElseZero() * 0.5f + 0.5f;
//				if (centroid >= 0) {
//					Draw.colorHash(centroid, c, 1, 0.75f + 0.25f * v.priElseZero(), a);
//					node.color(c[0], c[1], c[2], c[3]);
//				} else {
                node.color(0.5f, 0.5f, 0.5f, a); //unassigned
//				}
            }

            @Override
            public float width(PLink<NALTask> v, int n) {
                var t = v.id;
                return t.range();
                //return (t.term().seqDur() + t.range());
                //return (0.5f + v.get().priElseZero()) * 1/20f;
            }

            @Override
            public float height(PLink<NALTask> v, int population) {
                return (float) (0.25f / Math.sqrt(population));
            }
        };

        ScatterPlot2D<PLink<NALTask>> s = new ScatterPlot2D<>(model)/* {
			@Override
			protected void paintIt(GL2 gl, ReSurface r) {
				super.paintIt(gl, r);
//				//TODO render centroids
//				c.net.clusters.forEach(z -> {
//					var x = z.center[0];
//				});
			}
		}*/;
        return get(s, () -> {
            s.set(c.bag); //Iterable Concat the Centroids as dynamic VLink's
        }, n);
                //.every();
    }

//	public static Surface taskBufferView(PriBuffer b, NAR n) {
//		var plot = new Plot2D(256, Plot2D.Line).add("load", b::load, 0, 1);
//		var plotSurface = DurSurface.get(plot, plot::commit, n);
//		var g = new Gridding(
//			plotSurface,
//			new MetaFrame(b),
//			new Gridding(
//				new FloatRangePort(
//					DurLoop.cache(b::load, 0, 1, 1, n).getOne(),
//					"load"
//				)
//			)
//		);
//		if (b instanceof PriBuffer.BagTaskBuffer)
//			g.add(new BagView(((PriBuffer.BagTaskBuffer) b).tasks, n));
//
//		return g;
//	}


//    public static Surface tasklinkSpectrogram(Focus w, int history) {
//        return tasklinkSpectrogram(w.links, history, w.nar);
//    }

    public static Surface tasklinkSpectrogram(Baglike<?, TaskLink> b, int history, NAR n) {
        return tasklinkSpectrogram(n, b, history, b.capacity());
    }

    public static void premiseWindow(Premise p, NAR nar) {
        SpaceGraph.window(new PremiseSurface(p, nar), 500, 500);
    }

    public static <S extends Surface> Triggering<S> get(S x, Runnable eachDur, NAR n) {
        return get(x, n, nn ->eachDur.run());
    }

    public static <S extends Surface>  Triggering<S> get(S x, Consumer<S> eachDur, NAR n) {
        return get(x, n, nn ->eachDur.accept(x));
    }

    public static Triggering<BitmapMatrixView> get(BitmapMatrixView x, NAR n) {
        return get(x, x::updateIfShowing, n);
    }

    public static <S extends Surface> Triggering<S> get(S surface, NAR n, Consumer<NAR> eachDur) {
        return new Triggering<>(surface, n::onDur, S -> eachDur.accept(n));

    }

    public static  <S extends Surface> Triggering<S> get(S narConsumer, NAR n) {
        return get(narConsumer, n, (Consumer<NAR>)narConsumer);
    }

    abstract static class BagSpectrogram<X> extends Bordering {

        public final NAR n;
        public final Supplier<Iterable<X>> active;
        final Gridding m;
        final Spectrogram s;
        int history, width;
        public ToIntFunction<X> color = (i) -> 0;

        BagSpectrogram(Supplier<Iterable<X>> active, int history, int width, NAR n) {

            this.n = n;
            this.active = active;
            this.history = history;
            this.width = width;

            //mode select menu
            m = new Gridding();

            s = new Spectrogram(true, history, width);

            center(NARui.get(s, () -> {
//				for (X x : active.get()) {
//					int i = color.applyAsInt(x);
//					s.next(i);
//				}
                s.next(active.get(), this::colorize);//colorize(active.get()));
                //s.next(colorize(active.get()));
            }, n));
            west(m);
        }

        protected abstract int colorize(X x);

    }

    static final int[] opColors = new int[count];

    static {
        for (Op o : Op.values())
            opColors[o.id] = Draw.hsb(((float) o.id) / count, 1, 0.75f);
    }

    static final ToIntFunction<TaskLink> opColor = x ->
            x == null ? 0 : opColors[x.from().opID()];

    static final ToIntFunction<TaskLink> volColor = x -> {
        if (x == null) return 0;

        float h = Math.min(1, x.from().complexity() / 24f);
        final float b =
                //0.25f + 0.75f * x.priElseZero();
                1;

        return Draw.colorHSB(h, 1, b); //TODO
    };

    /**
     * TODO extract as BagSpectrogram class
     * TODO taskSpectrogram for TaskBag
     */
    public static Surface tasklinkSpectrogram(NAR n, Baglike<?, TaskLink> active, int history, int width) {
        PremiseSnapshot snap = new PremiseSnapshot(active);
        return new BagSpectrogram<>(() -> {
            snap.run();
            return snap;
        }, history, width, n) {

            final ToIntFunction<TaskLink> puncColor = new PuncColor();

            @Override
            protected int colorize(TaskLink premise) {
                return color.applyAsInt(premise);
            }

            {
                m.set(
                    new PushButton("Punc", () -> color = puncColor),
                    new PushButton("Op", () -> color = opColor),
                    new PushButton("Vol", () -> color = volColor)
                );
                color = puncColor;
            }
        };
    }

//    @Deprecated public static void agentOld(NAgent a) {
//        NAR nar = a.nar();
//        //nar.runLater(() -> {
//            window(
//                    grid(
//                            new ObjectSurface(a),
//
//                            beliefCharts(a.actions(), a.nar()),
//
//                            new EmotionPlot(64, a),
//                            grid(
//
//                                    new TextEdit() {
//                                        @Override
//                                        protected void onKeyEnter() {
//                                            String s = text();
//                                            text("");
//                                            try {
//                                                nar.conceptualize(s);
//                                            } catch (Narsese.NarseseException e) {
//                                                e.printStackTrace();
//                                            }
//                                            conceptWindow(s, nar);
//                                        }
//                                    }.surface(),
//
//
//                                    new PushButton("dump", () -> {
//                                        try {
//                                            nar.output(Files.createTempFile(a.toString(), "" + System.currentTimeMillis()).toFile(), false);
//                                        } catch (IOException e) {
//                                            e.printStackTrace();
//                                        }
//                                    }),
//
//                                    new PushButton("clear", () -> {
//                                        nar.runLater(NAR::clear);
//                                    }),
//
//
//                                    new WindowToggleButton("top", () -> new ConsoleTerminal(new nars.TextUI(nar).session(10f))),
//
//                                    new WindowToggleButton("concept graph", () -> {
//                                        DynamicConceptSpace sg;
//                                        SpaceGraphPhys3D s = new SpaceGraphPhys3D<>(
//                                                sg = new DynamicConceptSpace(nar, () -> nar.attn.active().iterator(),
//                                                        128, 16)
//                                        );
//                                        EdgeDirected3D fd = new EdgeDirected3D();
//                                        s.dyn.addBroadConstraint(fd);
//                                        fd.condense.setAt(fd.condense.get() * 8);
//
//                                        s.addAt(new SubOrtho(
//
//                                                grid(new ObjectSurface<>(fd), new ObjectSurface<>(sg.vis))) {
//
//                                        }.posWindow(0, 0, 1f, 0.2f));
//
//
//
//
//                                        s.camPos(0, 0, 90);
//                                        return s;
//                                    })
//
//
//
//
//
//
//
//
//
//
//
//
//
//
////
////                                    a instanceof NAgentX ?
////                                            new WindowToggleButton("vision", () -> grid(((NAgentX) a).sensorCam.stream().map(cs -> new AspectAlign(
////                                                    new CameraSensorView(cs, a).withControls(), AspectAlign.Align.Center, cs.width, cs.height))
////                                                    .toArray(Surface[]::new))
////                                            ) : grid()
//                            )
//                    ),
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//                    900, 600);
//        //});
//    }


    //    static class NarseseJShellModel extends OmniBox.JShellModel {
//        private final NAR nar;
//
//        public NarseseJShellModel(NAR n) {
//            this.nar = n;
//        }
//
//        @Override
//        public void onTextChange(String text, int cursorPos, MutableContainer target) {
//            super.onTextChange(text, cursorPos, target);
//        }
//
//        @Override
//        public void onTextChangeControlEnter(String text, MutableContainer target) {
//            text = text.trim();
//            if (text.isEmpty())
//                return;
//            try {
//                nar.input(text);
//            } catch (Narsese.NarseseException e) {
//                super.onTextChangeControlEnter(text, target);
//            }
//        }
//
//    }

    public static ImmediateMatrixView matrix(double[] dw) {
        return dw.length > 2048 ?
                ImmediateMatrixView.scroll(dw, false, 64, 8) :
                new ImmediateMatrixView(dw.length, 1, BitmapMatrixView.viewFunction2D(
                        (i) -> (float) dw[i],
                        dw.length,
                        //dw.length / Math.max(1, (int) Math.ceil(sqrt(dw.length))),
                        Draw::colorBipolar)
                );
    }


    public static <X> Surface focusPanel(Iterable<X> all, FloatFunction<X> pri, Function<X, String> str, NAR nar) {
        final IntRange capacity = new IntRange(64, 2, 256);

        var s = new Graph2D<X>()
                .render((node, g) -> {
                    var c = node.id;
                    var p = pri.floatValueOf(c);
                    node.pri = p;

                    var v = p;
                    node.color(p, v / 2, 0);
                })
                .update(new TreeMap2D<>())
                .build(node -> {
                    node.set(new VectorLabel(((Premise) node.id).term().toString()));
                });

        Iterable<X> allLimited = () -> Iterators.limit(all.iterator(), capacity.intValue());
        //TODO MutableEnum for color mode

        return get(
                new Splitting(s, 0.1f, s.configWidget(capacity)).resizeable(),
                () -> s.set(allLimited),
                nar
        );
    }

    private static class PuncColor implements ToIntFunction<TaskLink> {

        final float[] puncBuf = new float[4];

        @Override
        public int applyAsInt(TaskLink x) {

//            float r, g, b;
//            if (x instanceof TaskLink tx) {
                x.priGet(puncBuf);
                float r = puncBuf[0];
            float g = puncBuf[2];
            float b = (puncBuf[1] + puncBuf[3]) / 2;
//            } else {
//                Task xf = x.task();
//                float a = x.priElseZero();
//                switch (xf.punc()) {
//                    case BELIEF -> r = a;
//                    case GOAL -> g = a;
//                    case QUESTION, QUEST -> b = a;
//                    default -> throw new WTF();
//                }
//            }
            return color(r, g, b);
        }

        protected int color(float r, float g, float b) {
            return Draw.rgbInt(r, g, b);
        }
    }


//	public static Surface exeChart(WorkerExec exe) {
//
//		NAR nar = exe.nar;
//
//		return new Gridding(
//			exe.loops.stream().map(s-> {
//
//				QueueDeriver d = (s.deriver);
//
//				PriArrayBag<Premise> inBag = ((QueueDeriver.BufferedPremiseSource) d.in).local;
//
//				PriArrayBag<NALTask> nalBag = ((QueueDeriver.BufferedTarget) d.out).nalBag;
//				PriArrayBag<TaskLink> linkBag = ((QueueDeriver.BufferedTarget) d.out).linkBag;
//
//
//				return new Gridding(
//						//new ObjectSurface(s)
//						new BagView<>(inBag, nar),
//						new BagView<>(nalBag, nar),
//						new BagView<>(linkBag, nar),
//						DurSurface.get(new BitmapMatrixView(i ->{
//							float[] ss = s.schedule.read();
//							if (ss == null || ss.length <= i) return 0;
//							else {
//								float x = ss[i];
//								return x > 0 ? 0.2f + (0.8f*x) : 0f;
//							}
//						}, 16, Draw::colorBipolar), nar) //TODO dynamic size
//				);
//			})
//		);
//	}
}