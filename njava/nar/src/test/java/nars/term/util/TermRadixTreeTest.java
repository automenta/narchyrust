package nars.term.util;

import jcog.data.byt.ByteSequence;
import nars.Narsese;
import nars.term.atom.Atomic;
import nars.term.util.map.TermRadixTree;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 10/14/16.
 */
class TermRadixTreeTest {

    @Test
    void testAtomInsertion() throws Narsese.NarseseException {

        TermRadixTree tree = new TermRadixTree();

        ByteSequence s4 = TermRadixTree.termByVolume($("concept"));
        tree.putIfAbsent(s4, (Atomic.atomic(s4.toString())));
        ByteSequence s3 = TermRadixTree.termByVolume($("target"));
        tree.putIfAbsent(s3, (Atomic.atomic(s3.toString())));
        ByteSequence s2 = TermRadixTree.termByVolume($("termutator"));
        tree.putIfAbsent(s2, (Atomic.atomic(s2.toString())));


        assertNotNull(tree.get(TermRadixTree.termByVolume($("target"))));
        assertNull(tree.get(TermRadixTree.termByVolume($("xerm"))));
        assertNull(tree.get(TermRadixTree.termByVolume($("te"))));

        ByteSequence s1 = TermRadixTree.termByVolume($("target"));
        assertNotNull(tree.putIfAbsent(s1, (Atomic.atomic(s1.toString()))));
        assertEquals(3, tree.size());

        ByteSequence s = TermRadixTree.termByVolume($("termunator"));
        assertNotNull(tree.putIfAbsent(s, (Atomic.atomic(s.toString()))));

        tree.prettyPrint(System.out);

        assertEquals(4, tree.size());





    }



}