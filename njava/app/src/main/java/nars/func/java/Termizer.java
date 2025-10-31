package nars.func.java;

import jcog.Util;
import nars.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

/**
 * Converts POJOs to NAL Term's, and vice-versa
 */
public interface Termizer {

//	Term VOID = Atomic.the("void");
	Term EMPTY = Atomic.atomic("empty");
	Term NULL = Atomic.atomic("null");

	@Nullable
	Term term(Object o);

	@Nullable
	Object object(Term t);

	default Object[] object(Term[] t) {
		return Util.map(this::object, new Object[t.length], t);
	}

	default Object[] object(Object prefix, Term[] t) {
		Object[] x = new Object[t.length+1];
		x[0] = prefix;
		return Util.map(this::object, x, 1, t, 0, t.length);
	}

}