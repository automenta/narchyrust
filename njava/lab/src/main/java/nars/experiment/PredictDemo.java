package nars.experiment;

import jcog.Util;
import jcog.activation.SigmoidActivation;
import jcog.lstm.LSTM;
import jcog.math.FloatSupplier;
import jcog.nndepr.MLP;
import jcog.nndepr.optimizer.AdamOptimizer;
import jcog.tensor.Predictor;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.func.TruthPredict;
import nars.game.Game;
import nars.game.sensor.LambdaScalarSensor;
import nars.gui.LSTMView;
import nars.gui.LayerView;
import nars.gui.NARui;
import nars.time.clock.RealTime;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.grid.Gridding;

import java.util.List;
import java.util.stream.Stream;

import static nars.$.$$;
import static spacegraph.SpaceGraph.window;

public class PredictDemo {
    public static void main(String[] args) {
        float fps = 20f;
        int history = 24;
        int projections = 16;
        float dursPerSample = 4;

        NAR n = new NARS.DefaultNAR(0, true)
                .time(new RealTime.MS().durFPS(fps))
                .get();


        Game g = new Game($$("g"));

        float samplePeriod = n.dur() * dursPerSample;
        FloatSupplier f = () -> {
            float t = n.time() / n.dur();
            return (float) (0.5f + 0.5f * Math.sin(t / 5f));
            //return (float) (Math.abs(Math.cos((t/100f)%3))*Util.sqr((float) (0.5f + 0.5f * Math.sin(t / 60f))));
        };

        var X = new LambdaScalarSensor($.atomic("x"), f);
        g.addSensor(X);

        n.add(g);

        var p = new TruthPredict(List.of(X), List.of(X), history, ()->samplePeriod, projections,
            (i,o)->new MLP(i,
                //new MLP.Dense((i+o)/2, SigmoidActivation.the),
                new MLP.LinearLayerBuilder(o,
                        //new SigLinearActivation()
                        SigmoidActivation.the
                )
            ).optimizer(
                    new AdamOptimizer()
                    //new SGDOptimizer(0.1f)
            ),
            n, true
        );

        n.onDur(p);

        SpaceGraph.window(new Gridding(Gridding.VERTICAL,
                NARui.beliefCharts(List.of(X), n)
        ), 800, 800);

        n.startFPS(fps);

        n.runLater(() -> {
            Util.sleepMS(1000);
            Predictor P = p.predictor();
            if (P instanceof LSTM)
                window(NARui.get(new LSTMView(((LSTM) P)), LSTMView::run, n), 400, 400);
            else if (P instanceof MLP)
                window(NARui.get(new LayerView(Stream.of(((MLP) P).layers)), LayerView::run, n), 400, 400);

        });
    }
}