package nars.term.anon;

import jcog.Is;
import nars.NAL;
import nars.Term;
import nars.term.Neg;
import nars.term.atom.Atomic;
import nars.term.atom.Img;
import nars.term.atom.Int;
import nars.term.atom.IntrinAtomic;
import org.eclipse.collections.impl.map.mutable.primitive.ShortObjectHashMap;

import static nars.Op.*;
import static nars.term.atom.Anom.anom;
import static nars.term.atom.Atomic._atomic;
import static nars.term.var.NormalizedVariable.varNorm;

/**
 * INTrinsic terms
 *   a finite set of terms canonically addressable by an integer value
 *   used by the system in critical parts to improve space and time efficiency
 *
 * indicates the target has an anonymous, canonical integer identifier
 */
@Is("G%C3%B6del_numbering_for_sequences")
public enum Intrin  { ;

    /** code pages: categories of 8-bit numerically indexable items. */
    public static final short ANOMs = 0;

    //keep the variables in this order
    public static final short VARDEPs = 1;
    public static final short VARINDEPs = 2;
    public static final short VARQUERYs = 3;
    public static final short VARPATTERNs = 4;

    public static final short IMGs = 5; // TODO make this a misc category

    /** 0..255 */
    public static final short INT_POSs = 6;

    /** -1..-255 */
    public static final short INT_NEGs = 7;

    /** ASCII 0..255 */
    public static final short CHARs = 8;



    /** @param i positive values only  */
    public static Atomic _term(short i) {
        int n = i & 0xff;
        return switch (group(i)) {
            case ANOMs -> anom(n);
            case INT_POSs -> Int.i(+n);
            case INT_NEGs -> Int.i(-n);
            case CHARs -> _atomic(n);
            case IMGs -> image(n);
            case VARDEPs -> varNorm(VAR_DEP.id, n);
            case VARINDEPs -> varNorm(VAR_INDEP.id, n);
            case VARQUERYs -> varNorm(VAR_QUERY.id, n);
            case VARPATTERNs -> varNorm(VAR_PATTERN.id, n);
            default -> throw new UnsupportedOperationException();
        };
    }

    private static Img image(int n) {
        return n == '/' ? ImgExt : ImgInt;
    }

    public static Term term(int /* short */ i) {
        return term((short)i);
    }

    public static Term term(short i) {
        return i < 0 ? neg((short) -i) : _term(i);
    }

     /** HACK TODO determine without lookup if possible */
     public static byte op(short i) {
         return i < 0 ? NEG.id : _term(i).opID();
    }

    private static final ShortObjectHashMap<Neg.NegIntrin> negs = new ShortObjectHashMap<>(4096);

    public static Neg.NegIntrin neg(short i) {
        return NAL.term.NEG_INTRIN_CACHE ? _negCached(i) : _neg(i);
    }

    private static Neg.NegIntrin _negCached(short i) {
        return negs.getIfAbsentPutWithKey(i, Intrin::_neg);
    }

    static Neg.NegIntrin _neg(short i) {
        return new Neg.NegIntrin(i);
    }

    public static int group(int i) {
        return (i & 0xff00)>>8;
    }

    /** returns 0 if the target is not anon ID or a negation of one */
    public static short id(Term t) {
        return t instanceof Neg ? idNeg(t) : _id(t);
    }

    private static short idNeg(Term t) {
        return (short) -(t instanceof Neg.NegIntrin ?
                ((Neg.NegIntrin) t).sub :
                _id(t.unneg()));
    }

    private static short _id(Term t) {
        return t instanceof IntrinAtomic a ? a.intrin() : 0;
    }

    public static boolean intrin(Term[] xx) {
        //return Util.and(Intrin::intrin, xx);

        for (Term x : xx) if (!intrin(x)) return false;
        return true;
    }

    public static boolean intrin(Term x) {
        return id(x)!=0;
    }

    /** returns -1 if not variable */
    public static int varID(short i) {
        return isVar(i) ? i & 0xff : 0;
    }

    private static boolean isVar(int i) {
        int g = group(i);
        return g >= VARDEPs && g <= VARPATTERNs;
    }
}