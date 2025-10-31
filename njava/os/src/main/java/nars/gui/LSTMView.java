package nars.gui;

import jcog.lstm.LSTM;
import jcog.nndepr.BackpropRecurrentNetwork;
import jcog.nndepr.RecurrentNetwork;
import jcog.nndepr.ntm.NTM;
import jcog.nndepr.ntm.control.UMatrix;
import jcog.nndepr.spiking0.critterding.CritterdingBrain;
import jcog.tensor.ClassicAutoencoder;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.video.Draw;


@Deprecated public class LSTMView extends Gridding implements Runnable {

    public LSTMView(LSTM x) {
        super(
            new BitmapMatrixView(x.in, 8, Draw::colorBipolar),
            new BitmapMatrixView(x.full_hidden, 4, Draw::colorBipolar),
            new BitmapMatrixView(x.weightsF),
            new BitmapMatrixView(x.dSdF),
            new BitmapMatrixView(x.weightsG),
            new BitmapMatrixView(x.dSdG),
            new BitmapMatrixView(x.sumF, 4, Draw::colorBipolar),
            new BitmapMatrixView(x.actF, 4, Draw::colorBipolar),
            new BitmapMatrixView(x.sumG, 4, Draw::colorBipolar),
            new BitmapMatrixView(x.actG, 4, Draw::colorBipolar),
            new BitmapMatrixView(x.actH, 4, Draw::colorBipolar),
            new BitmapMatrixView(x.deltaH, 4, Draw::colorBipolar),
            new BitmapMatrixView(x.weightsOut),
            new BitmapMatrixView(x.deltaOut, 4, Draw::colorBipolar),
            new BitmapMatrixView(x.out, 4, Draw::colorBipolar)
        );
    }

    @Override public void run() {
        forEach(x -> ((BitmapMatrixView)x).updateIfShowing());
    }

    static class AEView extends GridRun {

        AEView(ClassicAutoencoder ae) {
            add(
                new BitmapMatrixView(ae.x),
                new BitmapMatrixView(ae.z), //reconstruction
                new BitmapMatrixView(ae.W) //weights
//                new ObjectSurface(ae)
            );
        }

    }
    public static class NTMView extends Gridding implements Runnable {

        private final NTM n;

        public NTMView(NTM n) {
            super();
            this.n = n;
        }


        @Override public void run() {
            if (size()==0) {
                if (/*x.input!=null && */n.control.hidden.neurons.value!=null) {
                    //HACK add later
                    UMatrix ih = n.control.hidden.inputToHiddenWeights;
                    add(
                        //new BitmapMatrixView(x.input, 8, Draw::colorBipolar),
                        new BitmapMatrixView(ih.width(), ih.height(), (x, y, i)->Draw.colorBipolar((float) ih.value(x,y)))
//                        new BitmapMatrixView(x.control.hidden.neurons.value, 8, Draw::colorBipolar),
//                        new BitmapMatrixView(x.control.hidden.neurons.grad, 8, Draw::colorBipolar)
                    );
                }
            }
            forEach(x -> ((BitmapMatrixView)x).updateIfShowing());
        }


    }

    abstract static class GridRun extends Gridding implements Runnable {
        @Override public final void run() {
            forEachRecursively(x -> {
                if (x instanceof BitmapMatrixView bmv)
                    bmv.updateIfShowing();
            });
        }
    }

    public static class RecurrentView extends GridRun {

        private static final boolean showIterations = false;

        public RecurrentView(RecurrentNetwork r) {
            int n = r.n();
            int inputSkip = r.inputs * n;
            add(
                new BitmapMatrixView(
                    i->(float)r.weights.weight((inputSkip+i)%n, (inputSkip + i)/n),
                    n * (n - r.inputs), Draw::colorBipolar)
            );
            if (r instanceof BackpropRecurrentNetwork && showIterations) {
                Gridding g = new Gridding();
                for (double[] d : ((BackpropRecurrentNetwork)r).deltaReverse) {
                    g.add(
                            new BitmapMatrixView(
                                    //i->(float)d[i], n*n,
                                    d,
                                    Draw::colorBipolar)
                    );
                }
                add(g);
            }
        }

    }
    public static class CritterdingView extends GridRun {

        private static final boolean showIterations = false;

        public CritterdingView(CritterdingBrain r) {
            int ins = r.getInputs().size();
            int inters = r.interNeuronCount();
            add(new BitmapMatrixView(
                i-> (float) r.getInputs().get(i).getOutput(),
                ins, Draw::colorBipolar)
            );
            add(new BitmapMatrixView(
                i-> (float) r.getInter().get(i).getPotential(),
                inters, Draw::colorBipolar)
            );
            add(new BitmapMatrixView(
                    i-> (float) r.getInter().get(i).getOutput(),
                    inters, Draw::colorBipolar)
            );
            int s = r.synapseCount();
            add(new BitmapMatrixView(
                    i-> (float) r.synapses.get(i).weight,
                    s, Draw::colorBipolar)
            );
        }
    }
}