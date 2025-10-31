package nars.term.atom;

import nars.Term;
import nars.term.anon.Intrin;

/** optimized intrinsic ASCII character Atoms */
public final class CharAtom extends Atom implements IntrinAtomic {

	static final CharAtom[] chars = new CharAtom[256];
	static {
		for (char i = 0; i < 256; i++)
			chars[i] = new CharAtom(i);
	}

	private final short intrin;

	private CharAtom(char c) {
		super(String.valueOf(c));
		this.intrin = (short)((Intrin.CHARs << 8) | c);
	}

	public static char the(Term x) {
		return ((CharAtom)x).chr();
	}

	@Override
	public short intrin() {
		return intrin;
	}

	public char chr() {
		return (char) bytes[3];
	}
}