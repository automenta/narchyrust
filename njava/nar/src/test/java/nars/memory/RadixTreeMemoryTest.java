package nars.memory;

import jcog.data.byt.ArrayBytes;
import nars.*;
import nars.term.util.map.TermRadixTree;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by me on 10/21/16.
 */
class RadixTreeMemoryTest {

    @Test
    void testTermIndex() {
        TermRadixTree<Term> t = new TermRadixTree<>();
        byte[] a = TermRadixTree.key($.func("x", $.atomic("y"))).arrayCompactDirect();
        System.out.println(Arrays.toString(a));
        t.put($.func("x", $.atomic("y")), $.the(1));
        t.put($.func("x", $.atomic("z")), $.the(2));
        Term y = t.get(
                new ArrayBytes(a)
        );
        assertEquals($.the(1), y);

//        Term yy = t.get(new ConcatBytes(
//                    new ArrayBytes((byte)2, (byte)2, (byte)8, (byte)1),
//                    new ArrayBytes((byte)0, (byte)0, (byte)1, (byte)121,
//                                   (byte)0, (byte)0, (byte)1, (byte)120
//                    )
//                ));
//        assertEquals($.the(1), yy);


        System.out.println(t.prettyPrint());

    }

    @Test
    void testVolumeSubTrees() throws Narsese.NarseseException {
        RadixTreeMemory t = new RadixTreeMemory( 128);
        NAR n = new NARS.DefaultNAR(1,false).memory(t).get();
        int sizeAtStart = t.size();
        n.concept($("a"), true);
        n.concept($("(a)"), true);
        n.concept($("(a-->b)"), true);
        n.concept($("(a-->(b,c,d))"), true);
        n.concept($("(a-->(b,c,d,e,f,g))"), true);
        n.concept($("(a-->(b,c,d,e,f,g,h,i,j,k))"), true);
        t.concepts.prettyPrint(System.out);
        t.print(System.out);
        assertEquals(6, t.size() - sizeAtStart);
        System.out.println(t.concepts.root);

        
//        List<MyRadixTree.Node> oe = t.concepts.root.getOutgoingEdges();
//        assertEquals(6, oe.size());
//        assertTrue(oe.get(0).toString().length() < oe.get(1).toString().length());
//        assertTrue(oe.get(0).toString().length() < oe.get(oe.size()-1).toString().length());
    }
}