package nars.deriver;

import jcog.data.graph.AdjGraph;
import jcog.data.list.Lst;
import jcog.random.RandomBits;
import nars.NALTask;
import nars.Premise;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;

import java.util.concurrent.ThreadLocalRandom;

public sealed interface DeriverMeter {

    void premise(Premise parent, Premise child);
    void task(Premise parent, NALTask child);

    void sort(Lst<Premise> queue, RandomBits rng);

//    void prioritize(short[] hows, Action[] how, int n, RandomBits rng);

    /** the significance of a node is determined by both how much it can influence others (out-degree) and how much it is influenced by others (in-degree). */
    non-sealed class GraphDeriverMeter implements DeriverMeter {

        int count = 0;

        private ObjectFloatHashMap<String> odc = null;

        static class Transition {
            int count = 0;
        }

        //final MapNodeGraph<String,Transition> graph = new MapNodeGraph();
        final AdjGraph<String,Transition> graph = new AdjGraph<>(true);

        private String type(Premise p) {
            Class t = p.reactionType();
            if (t == null) t = p.getClass();
            return t.getSimpleName();
        }
        private String type(NALTask t) {
            return String.valueOf((char)t.punc());
        }
//        private String type(int t) {
//            return String.valueOf(t);
//        }

        @Override
        public void premise(Premise parent, Premise child) {
            String P = type(parent);
//            int cc = child.cause();
//            if (cc!=-1) {
//                String X = type(cc);
//                edge(P, X);
//                inc(X, type(child));
//            } else {
                inc(P, type(child));
//            }
        }

        @Override
        public void task(Premise parent, NALTask child) {
            inc(type(parent), type(child));
        }

        @Override
        public void sort(Lst<Premise> queue, RandomBits rng) {
            var o = odc;
            if (o != null) {
                if (queue.size() >= 2) {
                    queue.sortThisByFloat(a -> o.get(type(a)));
                    return;
                }
            }
            queue.shuffleThis(rng); //default, just sort
        }

        //        @Override
//        public void prioritize(short[] hows, Action[] how, int n, RandomBits rng) {
//            if (n < 2) return;
//            var o = odc;
//            if (o == null) return;
//            ShortArrays.quickSort(hows, 0, n, (a, b)-> {
//                 return Float.compare(o.get(b), o.get(a));
//            });
//        }

        private void inc(String p, String c) {
            edge(p, c);
            count++;
            if ((count+1) % 100000 == 0) {
                commit();
                if (ThreadLocalRandom.current().nextInt(10)==0)
                    graph.clear();
            }
        }

        private void edge(String p, String c) {
            var t = graph.edge(p, c);
            if (t == null) {
                //TODO better
                graph.addNode(p);
                graph.addNode(c);
                graph.setEdge(p, c, t = new Transition());
            }
            t.count++;
        }

        private void commit() {
//            for (var src : graph.nodes()) {
//                graph.neighborEdges(src, (tgt, edge) -> {
//                    System.out.println(src + "\t" + tgt + "\t" + edge.count);
//                });
//            }

            //GraphIO.writeDOT(graph, System.out);

            var odc = graph.outDegreeCentrality(e -> e.count);
//            var oo = odc.keyValuesView().toSortedListBy(each -> -each.getTwo());
//            oo.forEach(o ->
//                System.out.println(o.getOne().v + "\t" + n4(o.getTwo()))
//            );
//            System.out.println();

//            IntFloatHashMap xdc = new IntFloatHashMap();
//            odc.forEachKeyValue((_id, v)->{
//                String id = _id.v;
//                if (Character.isDigit(id.charAt(0)))
//                    xdc.put(Integer.parseInt(id), v);
//            });

            this.odc = odc;

        }

    }
}
