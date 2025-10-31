package nars.unify.constraint;

import nars.$;
import nars.Op;
import nars.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;

public abstract class StructMatcher extends TermMatch {

	private final Term param;
	private final float cost;
	final int struct;
	final boolean anyOrAll;

	StructMatcher(int struct, boolean anyOrAll) {
		assert(struct!=0);
		this.struct = struct;
		this.anyOrAll = anyOrAll;
		int bits = Integer.bitCount(struct);
		this.cost = 0.02f +
			(anyOrAll ? 0.001f * bits : 0.19f - 0.001f * bits); //TODO refine

		this.param = paramNew(struct, anyOrAll);
	}

	protected Term paramNew(int struct, boolean anyOrAll) {
		return Op.strucTerm(struct);
	}

	abstract static class AbstractStructMatcher extends StructMatcher {

		private static final Atom ANY = Atomic.atom("any"), ALL = Atomic.atom("all");

		AbstractStructMatcher(int struct, boolean anyOrAll) {
			super(struct, anyOrAll);
		}

		@Override
		protected final Term paramNew(int struct, boolean anyOrAll) {
			return $.p(anyOrAll ? ANY : ALL, Op.strucTerm(struct));
		}

		/** selects a structure vector for the given term */
		protected abstract int structure(Term x);

		@Override
		public final boolean test(Term t) {
			int x = structure(t), y = this.struct;
			return anyOrAll ? ((x & y) != 0) : ((x | y) == x);
		}

	}



	@Override
	public final Term param() {
		return param;
	}

	@Override
	public float cost() {
		//all is more specific so should be prioritized ahead
		//more # of bits decreases the cost
		return cost;
	}

	/**
	 * has the target in its structure, meaning it either IS or HAS
	 * one of the true bits of the provide vector ("has any")
	 */
	public static final class HasAll extends StructMatcher {

		public HasAll(Op op) {
			this(op.bit);
		}

		public HasAll(int struct) {
			super(struct, false);
		}


		@Override
		public boolean test(Term x) {
			return x.hasAll(struct);
		}

	}
	public static final class HasAny extends StructMatcher {

		public HasAny(Op op) {
			this(op.bit);
		}

		public HasAny(int struct) {
			super(struct, true);
		}


		@Override
		public boolean test(Term x) {
			return x.hasAny(struct);
		}

	}
	/** excludes the op of the compound, includes the subterms and everything it contains */
	public static class HasSubStruct extends AbstractStructMatcher {

		public HasSubStruct(Op op) {
			this(op.bit, true);
		}

		public HasSubStruct(int struct, boolean anyOrAll) {
			super(struct, anyOrAll);
		}

		@Override
		protected int structure(Term x) {
			return x.structSubs();
		}
	}

	/** only the ops in the immedaite subterms of the compound */
	public static class HasSurfaceStruct extends AbstractStructMatcher {

		public HasSurfaceStruct(Op x) {
			this(x.bit, true);
		}

		HasSurfaceStruct(int struct, boolean anyOrAll) {
			super(struct, anyOrAll);
		}

		@Override
		protected int structure(Term x) {
			return x.structSurface();
		}

	}
}