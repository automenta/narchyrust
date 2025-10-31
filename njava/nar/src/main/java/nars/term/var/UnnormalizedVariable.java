package nars.term.var;

import jcog.The;
import nars.Op;
import nars.io.IO;

/**
 * Unnormalized, labeled variable
 */
public non-sealed class UnnormalizedVariable extends Variable implements The {

    private final byte op;

//    public UnnormalizedVariable(Op type, byte[] label) {
//        super(IO.SPECIAL_BYTE, label);
//        this.type = type.id;
//    }

    public UnnormalizedVariable(Op op, String label) {
        this(op.id, label);
    }

    public UnnormalizedVariable(byte opID, String label) {
        this(opID, label.getBytes());
    }

    protected UnnormalizedVariable(Op op, byte[] bytes) {
        this(op.id, bytes);
    }

    protected UnnormalizedVariable(byte opID, byte[] bytes) {
        super(IO.SPECIAL_BYTE, bytes);
        this.op = opID;
    }

    @Override
    public final byte opID() {
        return op;
    }


}