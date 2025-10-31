package nars.term.atom;

/** special atoms which are universally singleton */
public abstract sealed class SpecialAtomic extends Atomic permits Bool {
	private final String label;

	SpecialAtomic(String label, byte... bytes) {
		super(bytes);
		this.label = label;
	}

	@Override
	public final boolean equals(Object u) {
		return u == this;
	}

	@Override
	public final String toString() {
		return label;
	}

}