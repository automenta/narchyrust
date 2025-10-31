package nars.gui;

import com.google.common.collect.Streams;
import com.jogamp.newt.event.KeyEvent;
import jcog.data.iterator.ArrayIterator;
import jcog.math.FloatCached;
import jcog.math.FloatMeanWindow;
import jcog.tensor.ClassicAutoencoder;
import jcog.tensor.model.VanillaAutoencoder;
import nars.NAR;
import nars.game.Actions;
import nars.game.Game;
import nars.game.reward.Reward;
import nars.game.reward.ScalarReward;
import nars.game.sensor.AbstractSensor;
import nars.game.sensor.VectorSensor;
import nars.gui.sensor.VectorSensorChart;
import nars.term.Termed;
import nars.video.AutoClassifiedBitmap;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.grid.Containers;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.meta.Triggering;
import spacegraph.space2d.meta.obj.ObjectSurface;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.meter.ChernoffFace;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.text.Labelling;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static jcog.signal.wave2d.ColorMode.Hue;
import static nars.gui.NARui.beliefIcons;
import static spacegraph.space2d.widget.meter.BitmapMatrixView.colorBipolar;
import static spacegraph.space2d.widget.meter.BitmapMatrixView.colorHue;

public enum GameUI {
    ;

    public static Surface gameUI(Game g) {

        Iterable rewards = () -> Streams.stream(g.rewards)
            .flatMap(r -> Streams.stream(r.components()))
            .map(x -> ((ScalarReward)x).sensor.concept).iterator();

        Iterable sensors = ()->g.sensors.components().iterator();

        var aa = new TabMenu(Map.of(
                g.toString(), () -> new ObjectSurface(g, 3),

                "stat", () -> new Triggering<>(g::afterFrame, Containers.col(
                        statPlot(happyLongerPlot(g), g),
//                        statPlot(happyLongPlot(g), g),
                        statPlot(happyPlot(g), g),
                        statPlot(dexPlot(g), g),
                        statPlot(priPlot(g), g)
                        //					new TriggeredSurface<>(
//						new Plot2D(512, Plot2D.Line).add("Frust", g::frustration, 0, 1),
//						Plot2D::commit),
//					new TriggeredSurface<>(
//						new Plot2D(512, Plot2D.Line).add("Coh", g::coherency, 0, 1),
//						Plot2D::commit)
                )),
//                        .addAt("Dex+2", () -> a.dexterity(a.now() + 2 * a.nar().dur()))
//                        .addAt("Dex+4", () -> a.dexterity(a.now() + 4 * a.nar().dur())), a),
                //"reward", () -> NARui.beliefCharts(rewards, g.nar()),
                "reward", () -> new Gridding(Streams.stream(g.rewards).map(r -> rewardUI(r, g.nar()))),

                "sense", () -> NARui.beliefCharts(sensors, g.nar()),
                "act", () -> NARui.beliefCharts(ArrayIterator.iterable(g.actions.components().toArray(Termed[]::new)), g.nar()),
//                "curi", () -> new ObjectSurface(g.actions.curiosity),
                "q", () -> NARui.implMatrix(g.actions.actions, g.rewards, +Math.round(g.nar.dur()), g.nar),
                "face", () -> face(g),
                "override", () -> new OverridePanel(g)
        ));
        return aa;
    }

    private static Triggering<Plot2D> statPlot(Plot2D p, Game g) {
        return new Triggering<>(p, Plot2D::update).invisibleUpdate(true);
    }

    private static Surface face(Game g) {
        final var f = new ChernoffFace.FaceSurface();
        return new Triggering<>(f, g::afterFrame, (F)->{
            final var t = g.time;
            var dur = t.dur;
            long s = t.s, e = t.e;
            var happyNow = g.happiness();
            var longHappinessDurs = 16;
            var r = e - s;
            var happyLong = g.happiness(s - r * longHappinessDurs, e, dur * longHappinessDurs);
            F.set(6, (float) happyNow);
            F.set(4, 1 - (float) happyLong);
            var dex = g.dexterity();

            F.set(8, dex==dex ? 0.75f + 0.25f * (float) Math.pow(dex, -5) : 0);

            var focusPri =
                    //g.focus().pri();
                    g.focus().focusPri();

            F.set(9, focusPri);
            F.set(10, focusPri);

        });
        //return f;
    }

    private static Surface rewardUI(Reward r, NAR n) {
        Iterable v = () -> Streams.stream(r.components()).iterator();
        return new Gridding(
            NARui.beliefCharts(v, n),
            new ObjectSurface(r)
        );
    }

    private static Plot2D happyLongPlot(Game g) {
        var p = new Plot2D(256);
        var mean = Plot2D.mean(g::happiness, 64);
        p.addSeries("happy", mean, 0, 1);
        return p;
    }

    /** TODO cache all computations using FloatCached */
    private static Plot2D happyLongerPlot(Game g) {
        var baseFrames = 7;

        var p = new Plot2D(2048);
        var subBatch = Plot2D.mean(g::happiness, baseFrames);
        var subBatchCached = new FloatCached(()-> (float) subBatch.getAsDouble(), g::time);
        //var ewma = new Ewma(0.01);
        var mean2 = new FloatMeanWindow(64)
                    .minSize( 63 /* HACK */);
        var mean3 = new FloatMeanWindow(128)
                    .minSize( 127 /* HACK */);

        p.addSeries("happy..", ()-> {
            var h = subBatchCached.asFloat();
            return h == h ? mean2.acceptAndGetMean(h) : Float.NaN;
        });
        p.addSeries("happy...", ()-> {
            var h = subBatchCached.asFloat();
            return h == h ? mean3.acceptAndGetMean(h) : Float.NaN;
        });
        return p;
    }
    private static Plot2D happyPlot(Game g) {
        var p = new Plot2D(512);
        var gw = g.time;
        g.rewards.forEach(r -> p.addSeries(r.term() + " happy",
            ()-> r.happy(gw.s, gw.e, gw.dur)));
        return p;
    }

    private static Plot2D dexPlot(Game g) {
        var p = new Plot2D(512);
        g.actions.forEach(a ->
            p.addSeries(a.term() + " dex", a::dexterity));
        return p;
    }
    private static Plot2D priPlot(Game g) {
        var p = new Plot2D(512);
        g.actions.forEach(a ->
            p.addSeries(a.term() + " pri", a::priGoal));
        return p;
    }

    public static Surface row(AbstractSensor v, Game g) {
        return rowcol(v, true, g);
    }

    public static Surface col(AbstractSensor v, Game g) {
        return rowcol(v, false, g);
    }

    private static Surface rowcol(AbstractSensor v, boolean rowOrCol, Game g) {
        if (v instanceof VectorSensor V)
            return Labelling.the(v.term().toString(),
                new VectorSensorChart(V, rowOrCol ? V.size() : 1, rowOrCol ? 1 : V.size(), g));
        else
            return beliefIcons(List.of(v.term()), g.nar);
    }

    public static Surface aeUI(AutoClassifiedBitmap a) {
        a.reconstruct = true;

        var pixRecon = a.pixRecon;

        var reconstructColorMode = a.src.mode() == Hue ?
                colorHue(pixRecon) : colorBipolar(pixRecon);

        var g = a.game();

        Surface aeView;
        {
            aeView = a.ae instanceof ClassicAutoencoder ae ?
                    new BitmapMatrixView(ae.W)
                    :
                    new LayerView(Stream.of(
                        ((VanillaAutoencoder)a.ae).encoder.layer,
                        ((VanillaAutoencoder)a.ae).decoder.layer
                    ).flatMap(List::stream));
        }

        var s = new Gridding(
            aeView,
            new ObjectSurface(a),
            new BitmapMatrixView(pixRecon, reconstructColorMode),
            new VectorSensorChart(a, g).withControls()
        );

        //TODO make this non-leak by .off() when surface is destroyed

        g.afterFrame(() -> s.forEach(x -> {

            if (x instanceof Gridding gg)
                for (var gc : gg.children())
                    if (gc instanceof BitmapMatrixView gbc)
                        gbc.updateIfShowing();

            if (x instanceof LayerView l)
                l.run();

            if (x instanceof BitmapMatrixView b)
                b.updateIfShowing();
        }));
        return s;
    }

    private static class OverridePanel extends Widget {
        private final Game game;
        boolean enabled;

        public OverridePanel(Game g) {
            var x = new Gridding();
            var b = new Bordering(x);
            set(b);
            this.game = g;

            if (g.actions.overrides.o.isEmpty()) {
                x.add(new VectorLabel("no overrides defined"));
                return;
            }

            b.north(new CheckBox("Override", e-> {
                g.actions.overrides.enable(this.enabled = e);
            }));

            g.actions.overrides.o.forEach((k,v)->{
                x.add(new ActionPanel(k,v));
            });
        }

        @Override
        protected void starting() {
            super.starting();
            focus();
        }

        @Override
        public boolean key(KeyEvent e, boolean pressedOrReleased) {
            if (!enabled)
                return false;

            int c = e.getKeyChar();
            game.actions.overrides.o.entrySet().stream()
                .filter(z -> z.getKey() instanceof Actions.PushButtonControl p && p.keycode == c)
                .forEach(z -> ((Actions.PushButtonControl)z.getKey()).pressed = pressedOrReleased);
            return true;
        }

//        private void update() {
//            forEach(z -> {
//                if (z instanceof ActionPanel a) {
//                }
//            })
//        }

        private class ActionPanel extends Bordering {
            public ActionPanel(Actions.ActionControl k, Termed v) {
                center(new VectorLabel(v.term().toString()));
                south(new VectorLabel(k.toString()));
                //east(new ConceptColorIcon(v.concept, game.nar));
                //east(new BeliefTableChart(v.concept).update...);
            }
//            void update() {
//            }
        }
    }
}