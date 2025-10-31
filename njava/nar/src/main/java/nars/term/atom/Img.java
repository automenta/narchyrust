package nars.term.atom;

import nars.Op;
import nars.Term;

/** the / and \ Image operators */
public final class Img extends IntrinAtomic.AbstractIntrinAtomic {

    public static final Img Int = new Img((byte) Op.imIntSym);
    public static final Img Ext = new Img((byte) Op.imExtSym);

    private final String str;

    private Img(byte sym) {
        super(Op.IMG, sym);
        this.str = String.valueOf((char) sym);
    }

    @Override
    public Term neg() {
        return this==Op.ImgExt ? Op.ImgExtNeg : Op.ImgIntNeg;
    }

    @Override
    public byte opID() {
        return Op.IMG.id;
    }

    @Override
    public final String toString() {
        return str;
    }

}