package nars.focus.util;

import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.MutableNode;
import jcog.data.list.Lst;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import nars.focus.PriNode;
import org.jetbrains.annotations.Nullable;

/**
 * hierarchical priority distribution graph (directed-acyclic)
 */
public class PriTree {

    /** call this every system duration */
    public synchronized void commit() {
        if (nodesDFS == null) {
            var nodesDFS = new Lst<PriNode>(graph.nodes().size());
            graph.bfsEach(z -> nodesDFS.addFast(z.id));
            this.nodesDFS = nodesDFS;
        }
        for (var x : nodesDFS)
            x.update(graph);
    }


    /**
     * hierarchical priority distribution DAG (TODO ensure acyclic)
     */
    public final MapNodeGraph<PriNode, Object> graph = new MapNodeGraph<>(
        new ConcurrentFastIteratingHashMap<>(new MutableNode[0])
    );

    private Lst<PriNode> nodesDFS = null;

    private void invalidate() {
        nodesDFS = null;
    }

    public MapNodeGraph.AbstractNode<PriNode, Object> add(PriNode p) {
        var n = graph.addNode(p);
        invalidate();
        return n;
    }

    public boolean remove(PriNode p) {
        var n = graph.removeNode(p);
        if (n) invalidate();
        return n;
    }
    public void removeAll(PriNode... p) {
        for (var pp:p)
            remove(pp);
    }

    /** attach a target to a source directly
     * TODO require attached nodes to discover the tree root by graph searching in reverse
     * */
    public final PriNode link(PriNode target, PriNode source) {
        return input(target, null, source);
    }

    private <P extends PriNode> P input(P target, @Nullable PriNode.Merge mode, PriNode... sources) {
        if (mode!=null)
            target.input(mode);

        var g = graph;
        var thisNode = g.addNode(target);
        synchronized (graph) {
            PriNode.parent(sources, g, thisNode);
            invalidate();
        }

        return target;
    }

    public MapNodeGraph.AbstractNode<PriNode, Object> node(Object x) {
        return graph.node(x);
    }

    public void print() {
        graph.edges().forEach(System.out::println);
    }
}