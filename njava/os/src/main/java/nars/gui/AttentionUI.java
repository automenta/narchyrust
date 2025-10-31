package nars.gui;

import nars.Concept;
import nars.NAR;
import nars.focus.PriNode;
import nars.game.sensor.VectorSensor;
import nars.gui.concept.ConceptSurface;
import nars.gui.sensor.VectorSensorChart;
import spacegraph.space2d.Surface;
import spacegraph.space2d.meta.obj.ObjectGraphs;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class AttentionUI {


//    static class NodeUI extends Gridding {
//        public final PriNode node;
//
//        NodeUI(PriNode node) {
//            this.node = node;
//            add(new VectorLabel(node.toString()));
//            add();
//        }
//    }

//    public static GraphEdit serviceGraph(NAR n, NAgent a) {
//        GraphEdit<Surface> g = GraphEdit.window(800, 500);
//        g.add(new ExpandingChip(a.toString(), (x)->{
//            new ObjectSurface(a).forEach
//        }));
//        return g;
//    }
//
//    public static GraphEdit2D graphGraph(NAR n) {
//        GraphEdit2D g = new GraphEdit2D();
//        g.resize(800, 800);
//        //g.windoSizeMinRel(0.02f, 0.02f);
//
//        Exe.runLater(() -> {
////		g.add(NARui.game(a)).posRel(0.5f, 0.5f, 0.4f, 0.3f);
//            //g.add(NARui.top(n)).posRel(0.5f, 0.5f, 0.2f, 0.1f);
//            g.add(FocusUI.focusUI(n)).resize(400.25f, 400.25f);
//        });
//        return g;
//    }

    public static Surface objectGraphs(Object x, NAR n) {
        return objectGraphs(List.of(x), n);
    }

    public static Surface objectGraphs(PriNode x, NAR n) {
        return objectGraphs(() -> x.dfs(n.pri.graph).iterator(), n);
    }

    public static Map<Class, BiFunction<?, Object, Surface>> NAR_UI(NAR n) {
        return Map.of(
                NAR.class, (NAR nn, Object rel) -> new VectorLabel(nn.self().toString()),
                VectorSensor.class, (VectorSensor v, Object rel) -> new VectorSensorChart(v, n),
                //GameLoop.class, (GameLoop g, Object relation) -> g.components()
                Concept.class, (Concept c, Object rel) -> new ConceptSurface(c, n),
                //nars.focus.PriSource.class, (nars.focus.PriSource s, Object rel) -> new ConceptSurface(s.id.term(), n),
                PriNode.class, (PriNode v, Object rel) -> new ConceptSurface(v.id.term(), n)
//                ReactionModel.class, (ReactionModel m, Object rel) -> new Gridding(
//                        Stream.of(m.how).map(h ->
//                                //new ObjectSurface(h, 2)
//                                new PushButton(h.term().toString())
//                        )//.collect(toList())
//                )
//                How.class, (How h, Object rel) -> {
//                    return new PushButton(h.term().toString());
//                }
        );
    }

    /** TODO visualize the effective multiplicity of VectorSensor's */
    @Deprecated public static Surface objectGraphs(Iterable o, NAR n) {
        //TODO nars specific renderers
        ObjectGraphs g = new ObjectGraphs(o, NAR_UI(n), (xx, graph) -> {

            Object x = xx.id;

            float minSize = 0.01f;

            var nn = n.pri.node(x);
            if (nn != null) {
                PriNode id = nn.id;
                float P = id.pri();
                xx.pri =
                    minSize + P;
                    //minSize + (P * weightToScale(id.priweight()));
                for (var c : nn.nodes(false, true)) {
                    var e = graph.edge(xx, c.id);
                    if (e != null)
                        e.weight(0.25f).color(0.5f, 0.5f, 0.5f);
                }
            }
//            if (x instanceof Game gx) {
//                var e = graph.edge(x, gx.focus().pri);
//                if (e != null)
//                    e.weight(0.25f).color(0.5f, 0.5f, 0.5f);
//            }
//
//            if (x instanceof Pair px) {
//                //HACK dereference
//                x = px.getTwo();
//            }
//            Object XX = X;
//            if (x instanceof AND xa) {
//                NodeVis[] prev = {xx};
//                for (var s : xa.conditions()) {
//                    var S = graph.nodeOrAdd(s);
//                    graph.edge(prev[0], S).weight(1f).color(0.5f, 0.5f, 0.5f);
//                    prev[0] = S; //link to prev
//                }
//            } else if (x instanceof FORK fx) {
//                fx.forEach(s -> {
//                    var S = graph.nodeOrAdd(s);
//                    graph.edge(xx, S).weight(1f).color(0.5f, 0.5f, 0.5f);
//                });
//            }
        });

        return NARui.get(g, g::update, n);
                //.every(2);
    }


}