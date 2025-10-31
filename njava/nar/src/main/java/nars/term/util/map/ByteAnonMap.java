package nars.term.util.map;

import jcog.TODO;
import nars.Term;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;

import java.util.function.UnaryOperator;

/** this assumes < 127 unique elements */
public class ByteAnonMap implements ByteFunction<Term> {

    private static final int compactThesh = 64;

    /** target -> id */
    public final ObjectByteHashMap<Term> termToId;

    /** id -> target */
    public final TermList idToTerm;

    private ByteAnonMap(ObjectByteHashMap<Term> termToId, TermList idtoTerm) {
        this.termToId = termToId;
        this.idToTerm = idtoTerm;
    }

    public ByteAnonMap(ByteAnonMap clone) {
        this(new ObjectByteHashMap(clone.termToId), new TermList(clone.idToTerm));
    }

    public ByteAnonMap() {
        this(0);
    }

    public ByteAnonMap(int estSize) {
        this(new ObjectByteHashMap<>(estSize),new TermList(estSize));
    }

    public boolean isEmpty() {
        return idToTerm.isEmpty(); // && termToId.isEmpty();
    }

    public void clear() {
        int s = termToId.size();
        if (s > 0) {
            termToId.clear();
            idToTerm.clear();
        }
        if (s > compactThesh) {
            termToId.compact();
            idToTerm.clearCapacity(compactThesh);
        }
    }

    /** put: returns in range 1..Byte.MAX_VALUE (does not issue 0) */
    public final byte intern(Term x) {
        return termToId.getIfAbsentPutWithKey(x, this);
        //assert (b >= 0);
    }

    /** returns Byte.MIN_VALUE if missing */
    public final byte interned(Term x) {
        return termToId.getIfAbsent(x, Byte.MIN_VALUE);
    }

    /** get: accepts in range 1..Byte.MAX_VALUE (does not accept 0) */
    public final Term interned(byte id) {
        //assert(id > 0);
        return idToTerm.get(id-1);
    }

//    /** use when you're sure that you are only going to read from this afterward */
//    public void readonly() {
//        termToId.clear();
//    }

    public int termCount() {
        return idToTerm.size();
    }

    @Override
    public boolean equals(Object obj) {
        throw new TODO();
    }

    @Override
    public int hashCode() {
        throw new TODO();
    }

    @Override
    public String toString() {
        return idToTerm.toString();
    }

    public boolean updateMap(UnaryOperator<Term> f) {
        Term[] ii = idToTerm.array();
        int n = idToTerm.size();
        boolean changed = false;
        for (int i = 0; i < n; i++) {
            Term x = ii[i];
            Term y = f.apply(x);
            if (x!=y) {
                ii[i] = y;
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public final byte byteValueOf(Term x) {
        return idToTerm.addAndGetSizeAsByte(x);
    }

    public void putAll(ByteAnonMap b) {
        termToId.putAll(b.termToId);
        idToTerm.addAll((Subterms)b.idToTerm);
    }

}