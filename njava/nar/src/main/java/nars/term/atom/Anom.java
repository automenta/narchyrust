package nars.term.atom;

import jcog.Util;
import nars.io.IO;

import static nars.Op.ATOM;

/* indexed anonymous target */
public final class Anom extends IntrinAtomic.AbstractIntrinAtomic {

    private Anom(byte i) {
        super(i, IO.opAndEncoding(ATOM, (byte) 1), i);
    }

    private byte i() {
        return bytes[1];
    }

    @Override
    public byte opID() {
        return ATOM.id;
    }

    @Override
    public String toString() {
        return '_' +  Integer.toString(i());
    }

    /** intrinsic anom */
    private static final Anom[] the =
        Util.arrayOf(i -> new Anom((byte) i), 0, Byte.MAX_VALUE, Anom[]::new);

//    /** intrinsic anoms negated */
//    private static final Neg.NegIntrin[] theNeg =
//        Util.arrayOf(i -> the[i].neg(), 0, Byte.MAX_VALUE, Neg.NegIntrin[]::new);

    static {
           the[0] = null;
//        theNeg[0] = null;
    }

    public static Anom anom(int i) {
        return the[i];
    }

//    @Override
//    public Term neg() {
//        return theNeg[i()];
//    }
}