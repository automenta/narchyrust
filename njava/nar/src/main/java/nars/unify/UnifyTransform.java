package nars.unify;

import nars.NAL;
import nars.Op;
import nars.Term;
import org.jetbrains.annotations.Nullable;

import java.util.random.RandomGenerator;

/** unify + transform */
public class UnifyTransform extends Unify {

	@Nullable private Term transformed;
	@Nullable private Term result;

	/** force novel (non-equal) results */
	public boolean novel;

	public UnifyTransform(RandomGenerator rng) {
		super(Op.Variables, rng, NAL.unify.UNIFICATION_STACK_CAPACITY);
	}

	@Override
	public UnifyTransform clear() {
		this.novel = false;
		this.result = this.transformed = null;
		super.clear();
		return this;
	}


	/** filters acceptable results */
	protected boolean filter(Term y) {
		return true;
	}

	/** may need to call clear first */
	@Nullable public Term unifySubst(Term x, Term y, @Nullable Term transformed) {
		this.transformed = transformed;
		this.result = null;

		unify(x, y);

		return result;
	}


	/**
	 * terminate after the first match
	 */
	@Override
	protected final boolean match() {
		Term x = this.transformed;
		if (x == null)
			return false; //done

		Term y = transform(x);
		if (y!=null) {
			this.result = y;
			return false; //done
		}
		return true; //kontinue
	}

	@Nullable
	public Term transform(Term x) {
		Term y = apply(x);
		return y != null && y.CONCEPTUALIZABLE() && (!novel || novel(x, y)) && filter(y) ?
				y : null;
	}

	private static boolean novel(Term x, Term y) {
		return !x.normalize().equals(y.normalize());
	}

	public final UnifyTransform clear(int ttl) {
		setTTL(ttl);
		return clear();
	}

}