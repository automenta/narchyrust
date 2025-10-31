package nars;

import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import nars.deriver.reaction.Reaction;
import nars.term.Termed;
import nars.term.atom.Atom;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.term.atom.Atomic.atom;

/**
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 * <p>
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 * <p>
 * note: Comparable as implemented here is not 100% consistent with Task.equals and Term.equals.  it is
 * sloppily consistent for its purpose in collating Premises in optimal sorts during hypothesizing
 */
public abstract class Premise implements Termed {

	public String toString() {
		return term().toString();
	}

	@Deprecated
	public transient Premise parent;

	@Override abstract public Term term(); //TEMPORARY

	public boolean delete() {
		setParent(null);
		return true;
	}

	abstract public void run(Deriver d);

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object that);

	/**
	 * taskTerm
	 */
	public abstract Term from();

	/**
	 * beliefTerm
	 */
	public abstract Term to();


	/**
	 * reaction which generated this premise
	 */
	@Nullable
	public abstract Reaction reaction();

	/** default unoptimized impl */
	@Nullable public Term other(Term x) {
		Term f = from(), t = to();
		if (f == t) return null;
		if (x.equals(f)) return t;
		if (x.equals(t)) return f;
		return null;
	}

	/** default unoptimized impl */
	@Nullable public Term other(int xh, Predicate<Term> xEq) {
		throw new TODO();
	}

	/**
	 * a single / self / structural premise
	 */
	public boolean self() {
		return from().equals(to());
	}

	public @Nullable Term equalReverse(Predicate<Term> fromEquals, int fromHash, Predicate<Term> toEquals, int toHash) {
		return toEquals.test(this.to()) &&
				!fromEquals.test(this.from()) ? this.from() : null;
	}

	@Nullable abstract public NALTask task();

	@Nullable abstract public NALTask belief();

	@Nullable public final Class<? extends Reaction> reactionType() {
        var r = reaction();
		return r != null ? r.type() : null;
	}

	public final float complexityMean() {
		Term f = from(), t = to();
		float fv = f.complexity();
		return f == t ? fv : Util.mean(fv, t.complexity());
	}

	public static final Premise[] EmptyPremiseArray = new Premise[0];
	public static final Atom Task = atom("task");
	public static final Atom Belief = atom("belief");
	public static final Atom TaskInline = atom("taskTerm");
	public static final Atom BeliefInline = atom("beliefTerm");

	public final void setParent(Premise parent) {
		if (parent==this)
			throw new WTF();
		this.parent = parent;
	}

}