package jcog.learn.nn.spiking;

import jcog.Util;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.path.FromTo;
import jcog.exe.Loop;
import jcog.nndepr.spiking0.AbstractNeuron;
import jcog.nndepr.spiking0.AbstractSynapse;
import jcog.nndepr.spiking0.InputNeuron;
import jcog.nndepr.spiking0.learn.STDPSynapseLearning;
import jcog.nndepr.spiking0.neuron.IFNeuron;
import jcog.nndepr.spiking0.neuron.RealtimeBrain;
import jcog.random.XoRoShiRo128PlusRandom;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.EdgeVis;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.NodeGraphRenderer;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.layout.Force2D;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.video.Draw;

import static spacegraph.SpaceGraph.window;

public class SpikingUI extends Gridding /* HACK */ {

    private static final float UPDATE_FPS = 55f;

    public static void main(String[] args) {
        int I = 8/*, O = 4*/;
        int H = 128;

        RealtimeBrain b = brain(I, H);

        b.synapseUpdate =
                //new HebbianSynapseLearning();
                new STDPSynapseLearning();

        Loop.of(()-> {

            for (int i = 0; i < I; i++)
                ((InputNeuron)b.neurons.get(i)).setInput(
                        //rng.nextFloat()
                        Math.sin(b.t/4 + i)
                );

            b.forward();

        }).fps(UPDATE_FPS);

        window(new SpikingUI(b), 1000, 1000);
        window(spikeMatrixUI(b), 1000, 800);
    }



    private final RealtimeBrain brain;

    public SpikingUI(RealtimeBrain brain) {
        this.brain = brain;

        MapNodeGraph mm = brainGraph(brain);

        Graph2D m = new Graph2D()
                .update(new Force2D<>())
                .render(new NodeGraphRenderer<>() {
                    @Override
                    protected void edge(NodeVis<MapNodeGraph.AbstractNode<Object, Object>> from, FromTo<MapNodeGraph.AbstractNode<Object, Object>, Object> edge, @Nullable EdgeVis<MapNodeGraph.AbstractNode<Object, Object>> edgeVis, MapNodeGraph.AbstractNode<Object, Object> to) {
                        super.edge(from, edge, edgeVis, to);
                        var x = edge.id();
                        if (x instanceof AbstractSynapse s) {
                            float sWeight = Util.clampSafe((float) s.weight, -1, +1);
                            float sWeightAbs = Math.abs(sWeight);
                            edgeVis.weight(0.01f + sWeightAbs);
                            float x2 = -sWeight;
                            float r = Math.max(x2, (float) 0);
                            float x1 = +sWeight;
                            float g = Math.max(x1, (float) 0);
                            float b = 1 - sWeightAbs;
                            edgeVis.color(r, g, b, 0.5f);
                        }
                    }

                    @Override
                    public void node(NodeVis<MapNodeGraph.AbstractNode<Object, Object>> from, Graph2D.GraphEditor<MapNodeGraph.AbstractNode<Object, Object>> graph) {
                        super.node(from, graph);
                        var x = from.id.id;
                        if (x instanceof IFNeuron s) {
                            float pot = (float) s.potential;
                            float x2 = -pot;
                            float r = Math.max(x2, (float) 0);
                            float x1 = +pot;
                            float g = Math.max(x1, (float) 0);
                            float b = 0;
                            from.color(r, g, b, 0.5f);
//                            from.pri =
//                                    s.synapses.size()/10f;
//                                    //Math.abs(pot);
                        }
                    }
                });
        Loop.of(()-> {
            m.set(mm);
            //m.layout();
        }).fps(UPDATE_FPS);

        add(m.widget());
    }


    private static Surface spikeMatrixUI(RealtimeBrain brain) {
        int N = brain.neurons.size();
        var synapses = brain.synapseStream().toList();
        var g = new Gridding();

        g.add(new BitmapMatrixView(
                i -> (float) brain.neurons.get(i).getOutput(),
                N, Draw::colorBipolar));

//        g.add(new BitmapMatrixView(
//                i -> (float) i instanceof SpikingNeuron.SimpleSpikingNeuron ii ?
//                        brain.neurons.get(i)).potential,
//                N, Draw::colorBipolar));

        g.add(new BitmapMatrixView(
                i -> (float) synapses.get(i).weight,
                synapses.size(), Draw::colorBipolar));

//        g.add(new BitmapMatrixView(
//            i -> {
//                var h = (IzhikevichNeuron) b.neurons.get(I + i);
//                return (float) h.getOutput();
//            },
//            H, Draw::colorBipolar)
//        );
//        g.add(new BitmapMatrixView(
//            i -> {
//                var h = (IzhikevichNeuron) b.neurons.get(I + i);
//                return (float) h.v/100;
//            },
//            H, Draw::colorBipolar)
//        );
//        g.add(new BitmapMatrixView(
//            i -> {
//                var h = (IzhikevichNeuron) b.neurons.get(I + i);
//                return (float) h.u/100;
//            },
//            H, Draw::colorBipolar)
//        );


        Loop.of(()->{
            g.forEachRecursively(x -> {
                if (x instanceof BitmapMatrixView bmv)
                    bmv.updateIfShowing();
            });
        }).fps(UPDATE_FPS);
        return g;
    }

    private static MapNodeGraph brainGraph(RealtimeBrain brain) {
        MapNodeGraph mm = new MapNodeGraph();
        brain.synapseStream().forEach(z -> mm.addEdge(z.source, z, z.target));
        return mm;
    }

    private static RealtimeBrain brain(int I, int H) {
        final int synapseDensity = 9;

        RealtimeBrain brain = new RealtimeBrain();

        var rng = new XoRoShiRo128PlusRandom();

        for (int i = 0; i < I; i++)
            brain.neurons.add(new InputNeuron());
//        for (int i = 0; i < O; i++)
//            b.neurons.add(new OutputNeuron());

        for (int i = 0; i < H; i++)
            brain.neurons.add(
                    new IFNeuron(rng.nextBoolean())
                    //new IzhikevichNeuron().regular()
            );

        int N = brain.neurons.size();


        wireRandom(brain, I, H, (I + H) * synapseDensity, rng, N);
        //wireFullyConnected(brain, I, H, rng);
        return brain;
    }

    private static void wireFullyConnected(RealtimeBrain b, int I, int O, XoRoShiRo128PlusRandom rng) {
        for (int i = 0; i < I; i++) {
            var f = b.neurons.get(i);
            for (int o = 0; o < O; o++) {
                var t = b.neurons.get(I + o);
                addSynapse(f, (IFNeuron) t, rng);
            }
        }
    }

    private static void wireRandom(RealtimeBrain b, int I, int H, int S, XoRoShiRo128PlusRandom rng, int N) {
        float pctInputSynapse = 0.1f;
        for (int i = 0; i < S; i++) {
            int from = rng.nextFloat() <= pctInputSynapse ? rng.nextInt(I) : (I + rng.nextInt(H));
            int to = rng.nextInt(N);
            var F = b.neurons.get(from);
            var T = b.neurons.get(to);
            if (T instanceof IFNeuron TI /* non-input? */)
                addSynapse(F, TI, rng);
        }
    }

    private static boolean addSynapse(AbstractNeuron F, IFNeuron T, XoRoShiRo128PlusRandom rng) {
        return T.synapses.add(new AbstractSynapse<>(F, T,
            F instanceof InputNeuron ?
                (T.positive ? +1 : -1) :
                rng.nextFloat(-1, +1)));
    }

    public static class SpikingXORDemo {

        public static void main(String[] args) {
            throw new jcog.TODO();
        }
    }

}
