package nars.term.util;

import jcog.util.ArrayUtil;
import nars.Term;
import nars.term.Compound;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public enum TermPaths { ;

	private static final Predicate<Compound> nonCommutive = ((Predicate<Compound>)Term::COMMUTATIVE).negate();

	/**
	 * finds the shallowest and first deterministic subterm path for extracting a subterm in a compound.
	 * paths in subterms of commutive terms are excluded also because the
	 * position is undeterministic.
	 */
	public static @Nullable byte[] pathExact(Term container, Term subterm) {

		int cv = container.complexity();
		int sv = subterm.complexity();
		if (cv == sv)
			return container.equals(subterm) ? ArrayUtil.EMPTY_BYTE_ARRAY : null;
		else if (cv < sv)
			return null;

		if (container.COMMUTATIVE())
			return null;

		byte[][] p = new byte[1][];
		container.pathsTo(subterm,

			nonCommutive,

			(path, xx) -> {
				if (p[0] == null || p[0].length > path.size()) {
					//found first or shorter
					p[0] = path.isEmpty() ? ArrayUtil.EMPTY_BYTE_ARRAY : path.toArray();
				}
				return true; //continue
			});

		return p[0];
	}

}