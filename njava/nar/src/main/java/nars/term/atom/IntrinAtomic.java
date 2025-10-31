package nars.term.atom;

import jcog.The;
import nars.Op;
import nars.Term;
import nars.term.anon.Intrin;

import java.util.function.Predicate;

public interface IntrinAtomic extends The {
	short intrin();

	boolean equals(Object x);

	static short termToId(Op o, byte id) {
		return (short)(id | (switch (o) {
			case ATOM -> Intrin.ANOMs;
			case VAR_DEP -> Intrin.VARDEPs;
			case VAR_INDEP -> Intrin.VARINDEPs;
			case VAR_QUERY -> Intrin.VARQUERYs;
			case VAR_PATTERN -> Intrin.VARPATTERNs;
			case IMG -> Intrin.IMGs;
			default -> throw new UnsupportedOperationException();
		} << 8));
	}

	/** meant to be a perfect hash among all normalized variables */
	default byte id() {
		return (byte)intrin();
	}


	abstract sealed class AbstractIntrinAtomic extends Atomic implements IntrinAtomic permits Anom, Img {

		protected AbstractIntrinAtomic(Op o, byte num) {
			super(o, num);
		}

		protected AbstractIntrinAtomic(short intrin, byte... bytes) {
			super(intrin, bytes);
		}

		@Override
		public final short intrin() {
			return (short)hash;
		}

		@Override
		public final Predicate<Term> equals() {
			return x -> x == this;
		}

		@Override
		public final Predicate<Term> equalsPN() {
			return x -> x.unneg() == this;
		}

		@Override
		public final boolean equals(Object x) {
			return x == this;
		}

	}
}