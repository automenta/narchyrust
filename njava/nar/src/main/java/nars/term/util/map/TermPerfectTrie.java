package nars.term.util.map;

import com.google.common.base.Joiner;
import jcog.Str;
import jcog.data.map.ObjIntHashMap;
import jcog.tree.perfect.Trie;
import jcog.tree.perfect.TrieNode;
import jcog.tree.perfect.TrieSequencer;
import nars.Term;
import nars.term.Termed;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;


/**
 * indexes sequences of (a perfectly-hashable fixed number
 * of unique) terms in a magnos trie
 */
public class TermPerfectTrie<K extends Termed, V> extends Trie<List<K>, V> implements TrieSequencer<List<K>> {

    private final ObjIntHashMap<Term> conds;

    public TermPerfectTrie(int capacity) {
        super(null);
        conds = new ObjIntHashMap<>(capacity);
    }

    /**
     * override this to implement custom merging behavior; there can be only one
     */
    protected boolean equals(K a, K b) {
        return a.equals(b);
    }

    @Override
    public int matches(List<K> seqA, int indexA, List<K> seqB, int indexB, int count) {
        int i;
        for (i = 0;
                i < count && equals(seqA.get(i + indexA),
                seqB.get(i + indexB));
                    i++) { }
        return i;
    }

    @Override
    public int lengthOf(List<K> sequence) {
        return sequence.size();
    }

    @Override
    public int hashOf(List<K> seq, int index) {
        return conds.getIfAbsentPut(seq.get(index).term(), conds::size);
    }

    public void print() {
        print(System.out);
    }

    public void print(PrintStream out) {
        printSummary(this.root, out);
    }

    private static <A, B> void printSummary(TrieNode<List<A>, B> node, PrintStream out) {

        for (TrieNode<List<A>, B> n : node) {
            List<A> seq = n.seq();

            int from = n.start();

            out.print(n.childCount() + "|" + n.size() + "  ");

            Str.indent(from * 4);

            out.println(Joiner.on(" , ").join(seq.subList(from, n.end())


            ));

            printSummary(n, out);
        }


    }


    
    @Deprecated
    public StreamingStatistics costAnalyze(FloatFunction<K> costFn, @Nullable PrintStream o) {

        StreamingStatistics termCost = new StreamingStatistics();
        StreamingStatistics sequenceLength = new StreamingStatistics();
        StreamingStatistics branchFanOut = new StreamingStatistics();
        StreamingStatistics endDepth = new StreamingStatistics();
        int[] currentDepth = new int[1];

        costAnalyze(costFn, termCost, sequenceLength, branchFanOut, endDepth, currentDepth, this.root);

        if (o != null) {
            o.println("termCost: " + s(termCost));
            o.println("sequenceLength: " + s(sequenceLength));
            o.println("branchFanOut: " + s(branchFanOut));
            o.println("endDepth: " + s(endDepth));
        }
        return termCost;
    }

    private static String s(StreamingStatistics s) {
        return s.getSummary().toString().replace('\n', ' ').replace("StatisticalSummaryValues: ", "");
    }

    private static <K, V> void costAnalyze(FloatFunction<K> costFn, StreamingStatistics termCost, StreamingStatistics sequenceLength, StreamingStatistics branchFanOut, StreamingStatistics endDepth, int[] depth, TrieNode<List<K>, V> root) {

        int nc = root.childCount();
        if (nc > 0)
            branchFanOut.addValue(nc);

        for (TrieNode<List<K>, V> n : root) {
            List<K> seq = n.seq();

            int from = n.start();


            int to = n.end();
            sequenceLength.addValue(to - from);

            for (int i = from; i < to; i++)
                termCost.addValue(costFn.floatValueOf(seq.get(i)));

            depth[0]++;

            costAnalyze(costFn, termCost, sequenceLength, branchFanOut, endDepth, depth, n);

            depth[0]--;

            endDepth.addValue(depth[0]);
        }
    }

}