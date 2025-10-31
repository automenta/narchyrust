package nars.subterm;

import com.google.common.io.ByteArrayDataOutput;
import jcog.util.ArrayUtil;
import nars.Op;

import java.util.Arrays;

public final class ArrayRemappedSubterms extends RemappedPNSubterms {
    /**
     * TODO even more compact 2-bit, 3-bit etc representations
     */
    public final byte[] map;

    private final byte negs;

    private ArrayRemappedSubterms(byte[] map, Subterms base) {
        super(base);
        assert (base.subs() == map.length);
        assert (!(base instanceof IntrinSubterms)) : "IntrinSubterms can negate on its own";
        this.map = intern(map);
        this.negs = (byte) super.negs();
    }

    ArrayRemappedSubterms(Subterms base, byte[] map) {
        this(map, base);
        this.hash = hashExhaustive();
    }

    public ArrayRemappedSubterms(Subterms base, byte[] map, int hash) {
        this(map, base);
        this.hash = hash;
    }

    @Override
    public void write(ByteArrayDataOutput o) {
        byte[] xx = this.map;
        o.writeByte(xx.length);
        for (byte x : xx)
            writeSubterm(x, o);
    }

    private void writeSubterm(byte x, ByteArrayDataOutput o) {
        if (x < 0) {
            o.writeByte(Op.NEG.id);
            x = (byte) -x;
        }
        mapTerm(x).write(o);
    }

    @Override
    protected final boolean wrapsNeg() {
        return negs > 0;
    }

    @Override
    protected final int negs() {
        return negs;
    }


    @Override
    public final int subMap(int i) {
        return map[i];
    }


    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        else if (obj instanceof ArrayRemappedSubterms m)
            return equals(m);
        else if (obj instanceof Subterms s)
            return equalTerms(s);
        else
            return false;
    }

    private boolean equals(ArrayRemappedSubterms m) {
        return hash == m.hash && Arrays.equals(map, m.map) && ref.equals(m.ref);
    }


    /**
     * simple byte[] interner for low-count elements
     */
    public static byte[] intern(byte[] map) {
        return switch (map.length) {
            case 0 -> ArrayUtil.EMPTY_BYTE_ARRAY;
            case 1 -> switch (map[0]) {
                    case 0 -> BYTE_ZERO;
                    case 1 -> BYTE_ONE;
                    case 2 -> BYTE_TWO;
                    case 3 -> BYTE_THREE;
                    default -> throw internException();
                };
            case 2 -> {
                switch (map[0]) {
                    case 0 -> {
                        yield switch (map[1]) {
                            case 0 -> BYTE_ZERO_ZERO;
                            case 1 -> BYTE_ZERO_ONE;
                            default -> throw internException();
                        };
                    }
                    case 1 -> {
                        yield switch (map[1]) {
                            case 0 -> BYTE_ONE_ZERO;
                            case 1 -> BYTE_ONE_ONE;
                            case 2 -> BYTE_ONE_TWO;
                            case -1 -> BYTE_ONE_NEGONE;
                            case -2 -> BYTE_ONE_NEGTWO;
                            default -> throw internException();
                        };
                    }
                    case -1 -> {
                        yield switch (map[1]) {
                            case 1 -> BYTE_NEGONE_ONE;
                            case 2 -> BYTE_NEGONE_TWO;
                            case -1 -> BYTE_NEGONE_NEGONE;
                            case -2 -> BYTE_NEGONE_NEGTWO;
                            default -> throw internException();
                        };
                    }
                    case 2 -> {
                        yield switch (map[1]) {
                            case 1 -> BYTE_TWO_ONE;
                            case 2 -> BYTE_TWO_TWO;
                            case -1 -> BYTE_TWO_NEGONE;
                            case -2 -> BYTE_TWO_NEGTWO;
                            default -> throw internException();
                        };
                    }
                    default -> { yield map; }
                }
            }
            default -> map;
        };
    }

    private static RuntimeException internException() {
        return new UnsupportedOperationException();
    }

    /**
     * The number of distinct byte values.
     */
    private static final byte[] BYTE_ZERO = {0};
    private static final byte[] BYTE_ONE = {1};
    private static final byte[] BYTE_TWO = {2};
    private static final byte[] BYTE_THREE = {3};
    private static final byte[] BYTE_ZERO_ZERO = {0, 0};
    private static final byte[] BYTE_ZERO_ONE = {0, 1};
    private static final byte[] BYTE_ONE_ZERO = {1, 0};
    private static final byte[] BYTE_ONE_ONE = {1, 1};
    private static final byte[] BYTE_ONE_TWO = {1, 2};
    private static final byte[] BYTE_ONE_NEGONE = {1, -1};
    private static final byte[] BYTE_ONE_NEGTWO = {1, -2};
    private static final byte[] BYTE_TWO_ONE = {2, 1};
    private static final byte[] BYTE_TWO_TWO = {2, 2};
    private static final byte[] BYTE_TWO_NEGONE = {2, -1};
    private static final byte[] BYTE_TWO_NEGTWO = {2, -2};
    private static final byte[] BYTE_NEGONE_ONE = {-1, 1};
    private static final byte[] BYTE_NEGONE_TWO = {-1, 2};
    private static final byte[] BYTE_NEGONE_NEGONE = {-1, -1};
    private static final byte[] BYTE_NEGONE_NEGTWO = {-1, -2};

}