package nars.focus;

import jcog.Fuzzy;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.MutableNode;
import jcog.data.graph.path.FromTo;
import jcog.data.graph.search.Search;
import jcog.data.list.Lst;
import jcog.pri.Prioritized;
import jcog.pri.UnitPri;
import nars.$;
import nars.Term;
import nars.term.Termed;
import org.eclipse.collections.api.block.function.primitive.DoubleDoubleToDoubleFunction;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;

import java.util.Iterator;
import java.util.List;

/**
 * a node in the PrioriTREE
 * TODO make abstract, only use UnitPri pri in certain impl that actually need to store it and dont just copy an outside value like Source
 */
public class PriNode implements Prioritized {

    public final Term id;
    /**
     * internal priority storage variable
     */
    public final UnitPri pri = new UnitPri(Float.NaN);

    protected Merge input = Merge.Plus;

    @Deprecated
    private transient MapNodeGraph.AbstractNode<PriNode, Object> _node;

    public PriNode(Object id) {
        this.id = id instanceof Termed ? ((Termed) id).term() : $.identity(id);
    }

    public Iterable<PriNode> dfs(MapNodeGraph<PriNode, Object> g) {
        return new PriNodeDFS(g);
    }

    @Override
    public String toString() {
        return "$" + pri + " " + id.toString();
    }

    @Override
    public final float pri() {
        var p = this.pri.pri();
        if (p!=p)
            throw new UnsupportedOperationException(this + " disconnected");
        return p;
    }

    /**
     * how the incoming priority is combined from sources
     */
    public PriNode input(Merge m) {
        this.input = m;
        return this;
    }

    public void update(MapNodeGraph<PriNode, Object> graph) {
        this.pri.pri(in(input.merge(node(graph).nodes(true, false))));
    }

    /**
     * override to manipulate the incoming priority value (ex: transfer function)
     */
    protected float in(double p) {
        return (float) p;
    }

    /**
     * re-parent
     */
    public static void parent(PriNode[] parent, MapNodeGraph<PriNode, Object> g, MutableNode<PriNode, Object> thisNode) {

        assert (parent.length > 0);

        for (var nodeObjectFromTo : thisNode.edges(true, false))
            g.edgeRemove(nodeObjectFromTo);


        for (var p : parent) {
            //assert (!this.equals(p));
            g.addEdge(p, "pri", thisNode);
        }

    }

    /**
     * cached
     */
    public MapNodeGraph.AbstractNode<PriNode, Object> node(MapNodeGraph<PriNode, Object> graph) {
        if (_node == null)
            _node = graph.node(this); //cache
        return _node;
    }


    public enum Merge {
        Plus {
            @Override
            public double merge(Iterable<? extends MapNodeGraph.AbstractNode<PriNode, Object>> in) {
                return reduce(in, 0, Double::sum);
            }
        },
        And {
            @Override
            public double merge(Iterable<? extends MapNodeGraph.AbstractNode<PriNode, Object>> in) {
                return reduce(in, 1, (p, c) -> p * c);
            }
        },
        Or {
            @Override
            public double merge(Iterable<? extends MapNodeGraph.AbstractNode<PriNode, Object>> in) {
                return reduce(in, 0, Fuzzy::or);
            }
        };

        /**
         * @param f f(accumulator, nodePri)
         */
        protected static double reduce(Iterable<? extends MapNodeGraph.AbstractNode<PriNode, Object>> in, double accum, DoubleDoubleToDoubleFunction f) {
            for (var n : in) {
                var np = n.id.pri();
                if (np == np)
                    accum = f.applyAsDouble(accum, np);
            }
            return accum;
        }

        public abstract double merge(Iterable<? extends MapNodeGraph.AbstractNode<PriNode, Object>> in);
    }

    private class PriNodeDFS extends Search<PriNode, Object> implements Iterable<PriNode> {

        private final Lst<PriNode> p = new Lst<>();
        private final MapNodeGraph<PriNode, Object> g;

        PriNodeDFS(MapNodeGraph<PriNode, Object> g) {
            this.g = g;
        }

        @Override
        public Iterator<PriNode> iterator() {
            p.clear();
            p.add(PriNode.this);
            g.dfs(PriNode.this, this);
            return p.iterator();
        }

        @Override
        protected boolean go(List<BooleanObjectPair<FromTo<MapNodeGraph.AbstractNode<PriNode, Object>, Object>>> path, MapNodeGraph.AbstractNode<PriNode, Object> next) {
            p.add(next.id);
            return true;
        }
    }
}