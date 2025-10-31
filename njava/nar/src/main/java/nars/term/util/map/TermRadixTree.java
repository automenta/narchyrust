package nars.term.util.map;

import jcog.data.byt.ArrayBytes;
import jcog.data.byt.ByteSequence;
import jcog.data.byt.RecycledDynBytes;
import jcog.tree.radix.MyRadixTree;
import nars.Term;
import nars.io.IO;
import nars.term.atom.Atomic;

/**
 * String interner that maps strings to integers and resolves them
 * bidirectionally with a globally shared Atomic concept
 */
public class TermRadixTree<X> extends MyRadixTree<X> {

    /**
     * target with volume byte prepended for sorting by volume
     */
    public static ByteSequence termByVolume(Term x) {

        int vol = x.complexity();

        //TermBytes y = new TermBytes(vol * 4 + 64 /* ESTIMATE */);
		try (RecycledDynBytes y = RecycledDynBytes.get()) {

            y.writeShort(vol);

            x.write(y);

            return new ArrayBytes(y.arrayCopy());
        }
    }

    @Override
    public final X put(X value) {
        return put(key(value), value);
    }


    public final X put(Term key, X value) {
        return put(key(key), value);
    }
    public final X putIfAbsent(Term key, X value) {
        return putIfAbsent(key(key), value);
    }

    /** must override if X is not instanceof Termed */
    public static ByteSequence key(Object k) {
		//            try (RecycledDynBytes d = RecycledDynBytes.get()) { //termBytesEstimate(t) /* estimate */);
		//                TermIO.the.write(t, d);
		//                return d.arrayCopy();
		//            }
		return new ArrayBytes(k instanceof Atomic ? ((Atomic) k).bytes() : IO.termToBytes(((Term) k).term()));
    }


    public X get(Term key) {
        return get(key(key));
    }
}