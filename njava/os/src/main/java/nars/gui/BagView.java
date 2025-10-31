package nars.gui;

import jcog.data.list.table.Baglike;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.bag.Bag;
import nars.NAR;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import spacegraph.space2d.HistogramChart;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.meta.WindowToggleButton;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.menu.TabMenu;
import spacegraph.space2d.widget.meter.BagChart;
import spacegraph.space2d.widget.meter.Plot2D;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.util.math.Color3f;

import java.util.Map;

import static spacegraph.space2d.container.grid.Containers.col;

public class BagView<P extends Prioritized> extends TabMenu {

    public BagView(Bag<?, P> bag, NAR nar) {
        this(bag, Prioritized::priElseZero, nar);
    }

    public BagView(Bag<?, P> bag, FloatFunction<P> pri, NAR nar) {
        super(Map.of(
                "info", () -> new Gridding(
                    new VectorLabel(bag.getClass().toString()),
                    new PushButton("clear", bag::clear),
                    new PushButton("print", bag::print)
                ),
                "stat", () -> {
                    var budgetChart = new Plot2D(64)
                        .addSeries("Mass", bag::mass)
                        .addSeries("Pressure", bag::pressure);
                    return NARui.get(budgetChart, budgetChart::update, nar);
                },
                "histo", () -> histogram(bag, 32, pri, nar),
                "treechart", () -> bagChart(bag, nar)
        ));

        enable("histo");
    }

    public static Surface bagChart(Baglike bag, NAR nar) {
        var b = new BagChart(bag);
        return NARui.get(b, (Runnable) (b::update), nar);
    }

    public static <P extends Prioritized> Surface histogram(Iterable<P> items, int bins, FloatFunction<P> pri, NAR n) {
        //TODO other modes besides priority: volume, etc..

        float[] d = new float[bins], d2 = new float[bins];
        var hc = NARui.get(new HistogramChart(
                () -> d,
                new Color3f(0.25f, 0.5f, 0f), new Color3f(1f, 0.5f, 0.1f)),
                () -> {
                    PriReference.histogram(items, d2, pri);
                    System.arraycopy(d2, 0, d, 0, d.length);
                },
        n);

        return col(hc, 0.1f, new Gridding(
                        new WindowToggleButton("Sonify", () ->
                                new HistogramSonification(d)
                        )
                ));
    }
}