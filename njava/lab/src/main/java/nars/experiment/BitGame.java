package nars.experiment;

import jcog.event.Off;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Term;
import nars.game.Game;
import nars.game.action.AbstractGoalAction;
import nars.game.sensor.BitSensor;
import nars.gui.sensor.VectorSensorChart;
import nars.truth.proj.TruthProjection;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.ImmediateMatrixView;
import spacegraph.video.Draw;

import static nars.gui.NARui.beliefIcons;
import static spacegraph.SpaceGraph.window;

abstract class BitGame extends Game implements FloatSupplier {

    private final int width, words;
    protected BitSensor bits;
    private boolean trace = false;

    BitGame(Term id, int width, int words) {
        super(id);
        this.width = width;
        this.words = words;
    }

    @Override
    protected void init() {
        bits = new BitSensor(id, width, words, this);
        //bits.resolution(0.1f);

        reward(this);


        if (trace) {
            for (AbstractGoalAction A : bits.write)
                A.trace = true;
            onFrame(() -> {
                for (AbstractGoalAction aa : bits.write) {
                    AbstractGoalAction A = aa;
                    TruthProjection r = A.reason();
                    if (r != null && A.goalTruthOrganic.is()) {
                        if (A.goalTruthOrganic.freq() < 0.9f) {
                            System.out.println(nar.time());
                            nar.proofPrint(r);
                            System.out.println();
                        }
                    }
                }
            });
        }



    }

    static Surface BitSensorView(BitSensor bits, NAR nar) {
        ImmediateMatrixView m = new ImmediateMatrixView(bits.width, bits.words, (x, y, i) -> {
            //return Draw.colorHSB(bits.get(x, y), 0.5f, 0.5f);
            return Draw.colorBipolar((bits.get(x, y) - 0.5f) * 2);
        });

        return new Splitting(
                new Gridding(
                        beliefIcons(bits.components(), nar),
                        new VectorSensorChart(bits.read, nar)
                        //NARui.beliefChart(bits.addrPct, bits.nar)
                ),
                0.75f, m).resizeable();
    }

    public static class BitGame1 extends BitGame {

        public BitGame1(Term id, int width, int words) {
            super(id, width, words);
        }

        @Override
        public float asFloat() {
            //return allHigh();
            return gradient();
        }

        public float allHigh() {
            double count = 0;
            for (int j = 0; j < bits.words; j++)
                for (int i = 0; i < bits.width; i++)
                    count += bits.get(i, j);
            return (float) ((float) (count / (bits.words * bits.width)));
        }

        public float gradient() {
            double count = 0;
            double step = 1.0 / (bits.width);
            for (int j = 0; j < bits.words; j++)
                for (int i = 0; i < bits.width - 1; i++) {
                    float bigger = bits.get(i, j);
                    float smaller = bits.get(i + 1, j);
                    count += Math.max(0, (1 - Math.abs((bigger - smaller) - step) / step));
                }
            return (float) ((float) (count / (bits.words * (bits.width - 1))));
        }

        Off view = null;
        public BitGame1 view() {
            view = onFrame(()->{
                window(BitSensorView(bits, this.nar), 600, 600);
                view.close();
                view = null;
            });
            return this;
        }
    }
}