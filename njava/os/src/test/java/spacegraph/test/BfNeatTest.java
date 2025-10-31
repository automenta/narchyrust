package spacegraph.test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.path.FromTo;
import jcog.io.Serials;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.container.graph.EdgeVis;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.NodeGraphRenderer;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.container.layout.Force2D;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;

import static spacegraph.SpaceGraph.window;

public class BfNeatTest {

    final MapNodeGraph<String,Double> g = new MapNodeGraph<>(new TreeMap());
    private final ArrayNode inputMeta;
    private final ArrayNode jeans;
    private final int inputCount;
    private final int firstHidden, firstOutput;

    public BfNeatTest(String file) throws IOException {

        var f = Serials.jsonNode(Files.readString(Path.of(file)));

        inputMeta = (ArrayNode) f.get("inputMeta");
        inputCount = inputMeta.size();

        jeans = (ArrayNode) f.get("gnome_jeans");

        firstHidden = f.get("Params").get("indirectHiddenRange").get("first").asInt();
        firstOutput = f.get("Params").get("indirectOutputRange").get("first").asInt();

        /* iterate the hidden and outputs specified in 'jeans' */
        int to = firstHidden; //first hidden node offset
        for (com.fasterxml.jackson.databind.JsonNode jean : jeans) {
            //System.out.println(jj);
            ArrayNode inputs  = (ArrayNode) jean.get("inputs");
            ArrayNode weights = (ArrayNode) jean.get("weights");
            int ii = 0;
            for (var i : inputs) {
                int from = i.asInt();
                double weight = weights.get(ii).asDouble();
                //System.out.println(from + " -> " + to +  " = " + weight);
                g.addEdge(id(from), weight, id(to));
                ii++;
            }
            to++;
        }

        g.print();

        NodeGraphRenderer<String, Double> r = new NodeGraphRenderer<>() {
            @Override
            protected void edge(NodeVis<MapNodeGraph.AbstractNode<String, Double>> from, FromTo<MapNodeGraph.AbstractNode<String, Double>, Double> edge, @Nullable EdgeVis<MapNodeGraph.AbstractNode<String, Double>> edgeVis, MapNodeGraph.AbstractNode<String, Double> to) {
                int edges = from.id.edgeCount(
                        true,true
                        //false, true
                );
                from.pri = edges / 10.0f;

                double weight = edge.id();
                edgeVis.r = (float) Math.max(0, -weight);
                edgeVis.g = (float) Math.max(0, +weight);
                edgeVis.b = 0;
                edgeVis.weight =
                    //0.5f;
                    0.01f + (float) Math.abs(weight);
            }
        };

        window(new Graph2D<MapNodeGraph.AbstractNode<String, Double>>()
            .update(
                new Force2D<>()
                //new SemiForce2D.TreeForce2D<>()
            )
            .render(r)
            .set(g).widget(),
        800, 600);
    }

    private String id(int x) {
        if (x < inputCount)
            return inputMeta.get(x).asText() + x;

        //TODO bias label

        if (x >= firstHidden) {
            if (x < firstOutput) {
                return "h" + x; //hidden
            } else {
                return "o" + x; //output
            }
        }



        //default:
        return Integer.toString(x);
    }

    public static void main(String[] args) throws IOException {
        new BfNeatTest("/home/me/d/elite0000000000000003303.26.json");

    }
}
