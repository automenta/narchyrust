package nars.unify.mutate;

import nars.$;
import nars.Term;
import nars.subterm.Subterms;
import nars.term.ProxyCompound;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

public abstract class Termutator extends ProxyCompound {

	protected Termutator(Atom klass) {
		this(klass, null);
	}
	protected Termutator(Atom klass, @Nullable Term param) {
		super(param==null ? $.pFast(klass) : $.pFast(klass, param));
	}
	protected Termutator(Atom klass, Subterms x, Subterms y) {
		this(klass, $.pFast($.pFast(x), $.pFast(y)));
	}

	/**
	 * apply next termutation in the chain termutations recursing to the next after each successful one
	 * return false to CUT
	 */
	public abstract boolean apply(Termutator[] chain, int current, Unify u);

	public int getEstimatedPermutations() {
		return -1; /* unknown */
	}

	/** @return null to terminate the entire chain (CUT);
	 * this instance for no change
	 * or a reduced version (or NullTermutator for NOP) */
	public Termutator commit(Unify u) {
		return this;
	}

	public static final Termutator[] CUT = new Termutator[0];
	public static final Termutator ELIDE = new Termutator(Atomic.atom("Termutator_Elide")) {
		@Override public boolean apply(Termutator[] chain, int current, Unify u) {
			return u.match(chain, current);
		}
	};

	/** constant result for return from preprocess() call
	 * */
	@Nullable
	public static Termutator result(boolean success) {
		return success ? ELIDE : null;
	}

}