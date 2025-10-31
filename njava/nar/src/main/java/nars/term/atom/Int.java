package nars.term.atom;

import jcog.data.IntCoding;
import nars.Term;
import nars.term.anon.Intrin;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

import static nars.NAL.term.ANON_INT_MAX;
import static nars.Op.INT;

/**
 * 32-bit signed integer
 */
public final class Int extends Atomic implements IntrinAtomic, IntSupplier, DoubleSupplier {

	private static final int INT_CACHE_SIZE = ANON_INT_MAX * 2;
	static final Int[] pos = new Int[INT_CACHE_SIZE];
	private static final Int[] neg = new Int[INT_CACHE_SIZE];

	static {
		for (int i = 0; i < pos.length; i++)
			pos[i] = new Int(+i);
		for (int i = 1; i < neg.length; i++)
			neg[i] = new Int(-i);
	}

	public static final Int ZERO = i(0);
	public static final Int ONE = i(1);
	public static final Int TWO = i(2);
	public static final Int NEG_ONE = i(-1);


	//    protected Int(int id, byte[] bytes) {
//        this.id = id;
//        this.bytesCached = bytes;
//    }

	private Int(int i) {
		super(i, bytes(i));
	}

	/** 1..4 bytes */
	private static byte[] bytes(int i) {
		int u = IntCoding.encodeZigZagInt(i);
		byte[] b = new byte[1 + IntCoding.variableByteLengthOfUnsignedInt(u)];
		b[0] = INT.id;
		IntCoding.encodeUnsignedVariableInt(u, b, 1);
		return b;
	}

	/** unsafely decodes integer primitive from term thought to be Int */
	public static int i(Term x) {
		return i((Int)x);
	}

	public static int i(Int x) {
		return x.hash;
	}

	@Override
    public final short intrin() {
		return intrin(hash);
    }

	private static short intrin(int i) {
		var ia = Math.abs(i);
        return ia > ANON_INT_MAX ? 0 : intrin(i, ia);
    }

	private static short intrin(int i, int ia) {
		return (short) ((i >= 0 ? Intrin.INT_POSs : Intrin.INT_NEGs) << 8 | ia);
	}

	public static Int i(int i) {
		return (i >= 0 && i < pos.length) ?
			pos[i]
			:
			(i < 0 && i > -neg.length ? neg[-i] : new Int(i));
	}

	@Override
	public boolean INT() {
		return true;
	}

	@Override
	public byte opID() {
		return INT.id;
	}

	@Override
	public String toString() {
		return Integer.toString(hash);
	}

	/** multiplies by -1 (not NEGATION) */
	public final Int negative() {
		int i = this.hash;
		return i ==0 ? this : i(i * -1);
	}

	@Override
	public double getAsDouble() {
		return hash;
	}

	@Override
	public final int getAsInt() {
		return hash;
	}


}
//	/** Int-only subterms converted to int[] */
//	public static int[] the(Subterms x) {
//		int s = x.subs();
//		int[] y = new int[s];
//		for (int i = 0; i < s; i++)
//			y[i] = i(x.sub(i));
//		return y;
//	}
//
//	/** int[] converted to Subterms */
//	public static Subterms the(int[] x) {
//		return Op.terms.subterms($.ints(x));
//	}

//    public static class RotatedInt implements Termed {
//
//        private final int min, max;
//        private Int i;
//
//        public RotatedInt(int min /* inclusive */, int max /* exclusive */) {
//            this.min = min;
//            this.max = max;
//            this.i = Int.the((min + max) / 2);
//        }
//
//        @Override
//        public Term target() {
//            Term cur = i;
//            int next = this.i.id + 1;
//            if (next >= max)
//                next = min;
//            this.i = Int.the(next);
//            return cur;
//        }
//    }
