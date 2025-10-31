package nars.gui;

import jcog.Util;
import jcog.data.list.Lst;
import jcog.pri.Prioritized;
import jcog.signal.FloatRange;
import jcog.signal.IntRange;
import nars.NAR;
import nars.control.Cause;
import nars.time.clock.RealTime;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.meta.LoopPanel;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.slider.IntSlider;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import java.util.List;

import static jcog.Util.lerpSafe;
import static spacegraph.space2d.container.grid.Containers.col;
import static spacegraph.space2d.container.grid.Containers.grid;

public class ExeUI {
    private static Surface metaGoalPlot2(NAR nar) {

        var s = nar.causes.why.size();

        List<WhyIconButton> controls = new Lst(s);
        for (var w : nar.causes.why)
			controls.add(new WhyIconButton(w));

        Surface g = new Gridding(controls);
//        BagChart g =
//            new BagChart<>(controls);
//        BitmapMatrixView bmp = new BitmapMatrixView(i ->
//            //Util.tanhFast(
//            gain.floatValue() * nar.control.why.get(i).pri()
//            //)
//            , s, Draw::colorBipolar);

        return NARui.get(g, ()->{
            for (var control : controls)
                control.update();
//            g.update();
        }, nar);
    }

    static class WhyIconButton extends PushButton implements Prioritized {
    	final Cause w;
    	float pri;
    	WhyIconButton(Cause w) {
    		super(new BitmapLabel(w.toString(), 16),
                    () -> SpaceGraph.window(w, 500, 300));
    		this.w = w;
    		update();
            //set(new Bordering(l)
//                .south(new FloatSlider(0, -2, +2).on(x->{
//                float p = (float) Math.pow(x, 10);
//                w.setPri(p);
//            })));
		}

        @Override
        public float pri() {
            return pri;
        }


        public void update() {
            this.pri = //unitizeSafe((1 + w.priElseZero())/2);
                    w.pri;
            Draw.hsl(
                    lerpSafe(pri, 0f, 1f),
                    0.9f, 0.5f,
                    this.color);
//            float v = w.value();
//            color.y = v!=v ? 0 : Util.unitizeSafe(v);
		}
	}

	private static Surface metaGoalPlot(NAR nar) {

        var s = nar.causes.why.size();

        var gain = new FloatRange(1f, 0f, 100f);

        var bmp = new BitmapMatrixView(i ->
                //Util.tanhFast(
                    gain.floatValue() * nar.causes.why.get(i).pri()
                //)
                , s, Draw::colorBipolar);

        return col(NARui.get(bmp, nar), 0.05f, new FloatSlider(gain, "Display Gain"));
    }

//    private static Surface metaGoalControls(CreditControlModel model, NAR n) {
//        CheckBox auto = new CheckBox("Auto");
//        auto.on(true);
//
//        float min = -1f;
//        float max = +1f;
//
//        double[] want = model.want;
//        //return DurSurface.get(
//         return grid( IntStream.range(0, want.length).mapToObj(
//            w -> new FloatSlider((float) want[w], min, max) {
//                @Override
//                protected void paintWidget(RectF bounds, GL2 gl) {
//                    if (auto.on()) {
//                        set((float) want[w]);
//                    }
//                }
//            }
//            .text(MetaGoal.values()[w].name())
//            .type(SliderModel.KnobHoriz)
//            .on((s, v) -> {
//                if (!auto.on())
//                    want[w] = v;
//            })
//        ));
//    }

    static Surface exeStats(NAR n) {


        var plotHistory = 500;
//        MetalConcurrentQueue busyBuffer = new MetalConcurrentQueue(plotHistory);
//        MetalConcurrentQueue queueSize = new MetalConcurrentQueue(plotHistory);

        List<Plot2D> plots = new Lst<>();
//        if (n.exe instanceof WorkQueueExec te) {
//            plots.add(new Plot2D(plotHistory)
//                    .add("queueSize", te::queueSize));
//        }
        plots.add(new Plot2D(plotHistory).type(Plot2D.PlotType.BAR).addSeries("Busy", n.emotion.busyVol, 0, Float.NaN));
//        plots.add(new Plot2D(plotHistory)
//                .addSeries("DerivedTasks", n.emotion.derivedTask));

        var g = grid(plots);
        //            final Off c = n.onCycle((nn) -> {
//                busyBuffer.offer();
//                Exec nexe = n.exe;
//                if (nexe instanceof ThreadedExec)
//                    queueSize.offer();
//            });
        return NARui.get(g, n, nn -> {
            for (var p : plots)
                p.update();
        });
    }

    static Surface valuePanel(NAR n) {
        var b = new Bordering(
            //metaGoalPlot(n),
            metaGoalPlot2(n)
        );
//        if (n.control.model instanceof CreditControlModel) {
//            b.east(
//                    metaGoalControls(((CreditControlModel)n.control.model), n)
//            );
//        }
        /*.south(
            //TODO menu selector
            new ObjectSurface(n.exe.governor)
        )*/
        //        //TODO
//        if (n.exe.governor instanceof Should.MLPGovernor) {
////            Should.MLPGovernor.Predictor[] p = ((Should.MLPGovernor) n.exe.governor).predictor;
////            if (p.length > 0) {
////                p[0].
////            }
//
//        }
        return b;

    }

//    static class CausableWidget<X> extends Widget {
//
//        CausableWidget(X c, String s) {
//            set(new VectorLabel(s));
//        }
//    }

//    enum CauseProfileMode implements FloatFunction<How> {
//        Pri() {
//            @Override
//            public float floatValueOf(How w) {
//                return w.pri();
//            }
//        },
//        Value() {
//            @Override
//            public float floatValueOf(How w) {
//                return w.value;
//            }
//        },
//        ValueRateNormalized() {
//            @Override
//            public float floatValueOf(How w) {
//                return w.valueRateNormalized;
//            }
//        },
////        Time() {
////            @Override
////            public float floatValueOf(TimedLink w) {
////                return Math.max(0,w.time.get());
////            }
////        }
//        ;
//        /* TODO
//                                //c.accumTimeNS.get()/1_000_000.0 //ms
//                                //(c.iterations.getMean() * c.iterTimeNS.getMean())/1_000_000.0 //ms
//                                //c.valuePerSecondNormalized
//                                //c.valueNext
//                                //c.iterations.getN()
//                                //c...
//
//                        //,0,1
//         */
//    }
//
//    static Surface causeProfiler(AntistaticBag<How> cc, NAR nar) {
//
//        int history = 128;
//        Plot2D pp = new Plot2D(history,
//                //Plot2D.BarLanes
//                Plot2D.LineLanes
//                //Plot2D.Line
//        );
//
//        final MutableEnum<CauseProfileMode> mode = new MutableEnum<>(CauseProfileMode.Pri);
//
//        for (How c : cc) {
//            String label = c.toString();
//            //pp[i] = new Plot2D(history, Plot2D.Line).addAt(label,
//            pp.add(label, ()-> mode.get().floatValueOf(c));
//        }
//
//        Surface controls = new Gridding(
//                EnumSwitch.the(mode, "Mode"),
////                new PushButton("Print", ()-> {
////                    Appendable t = TextEdit.out();
////                    nar.exe.print(t);
////                    window(t, 400, 400);
////                }),
//                new PushButton("Clear", ()->pp.series.forEach(Plot2D.Series::clear))
//        );
//        return DurSurface.get(Splitting.column(pp, 0.1f, controls), nar, pp::commit);
//    }
//
//    public static Surface howChart(NAR n) {
//        return NARui.<How>focusPanel(n.how, h->h.pri(), h -> h.id.toString(), n);
//    }

    //    private static void causeSummary(NAR nar, int top) {
//        TopN[] tops = Stream.of(MetaGoal.values()).map(v -> new TopN<>(new Cause[top], (c) ->
//                (float) c.credit[v.ordinal()].total())).toArray(TopN[]::new);
//        nar.causes.forEach((Cause c) -> {
//            for (TopN t : tops)
//                t.add(c);
//        });
//
//        for (int i = 0, topsLength = tops.length; i < topsLength; i++) {
//            TopN t = tops[i];
//            System.out.println(MetaGoal.values()[i]);
//            t.forEach(tt->{
//                System.out.println("\t" + tt);
//            });
//        }
//    }


    /**
     * adds duration control
     */
    static class NARLoopPanel extends LoopPanel {

        final IntRange durMS = new IntRange(1, 1, 10000);
        private final FloatSlider durSlider;

        NARLoopPanel(NAR.NARLoop loop) {
            super(loop);
            var nar = loop.nar();
            durMS.set(nar.dur());
            durSlider = new IntSlider("Dur(ms)", durMS);
            if (nar.time instanceof RealTime) {
                add(
                    durSlider.on(durMS->nar.time.dur(Math.max(Math.round(durMS), 1))),
                    new FloatSlider(nar.cpuThrottle, "CPU")
                );
            } else {
                //TODO
                //time = null;
            }
        }

        @Override
        public void update() {

            super.update();

            if (loop.isRunning()) {
                var n = ((NAR.NARLoop) loop).nar();
                if (n.time instanceof RealTime tr) {
                    var actualMS = tr.durSeconds() * 1000.0;
                    if (!Util.equals(durMS.doubleValue(), actualMS, 0.1)) {
                        durMS.set(actualMS); //external change singificant
                    }
                }
            }

        }
    }

    static Surface exeUI(NAR n) {
        var nameLabel = new BitmapLabel(n.self().toString());
        var control = new NARLoopPanel(n.loop);
        var clock = new ClockUI(n);
        return NARui.get(
            new Splitting(new Gridding(nameLabel, clock), 0.25f, control).horizontal(),
                () -> {
                    control.update();
                    clock.update();
                }, n);
    }

    private static class ClockUI extends Bordering {

        private final VectorLabel digits;
        private final NAR nar;
        private long _now = Long.MIN_VALUE;

        ClockUI(NAR n) {
            this.nar = n;
            center(digits = new VectorLabel());
//            var controls = new Gridding(
//                new PushButton("Reset", ()->{
//                    nar.time.reset();
//                })
//                //new CheckBox("Run", true)
//            );
//            east(controls);
            update();
        }

        public void update() {
            var now = nar.time();
            if (now != this._now)
                digits.text(Long.toString(this._now = now));
        }
    }


}