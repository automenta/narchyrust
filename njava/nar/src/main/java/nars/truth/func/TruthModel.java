package nars.truth.func;

import jcog.data.map.UnifriedMap;
import nars.Term;
import nars.TruthFunctions;
import nars.term.atom.Atomic;
import nars.truth.func.TruthFunction.RepolarizedTruth;
import org.jetbrains.annotations.Nullable;

/**
 * manages permuting variants of the truth function terms,
 * indicated by attached suffix modifiers:
 *
 *    PP          - default; alias for default un-negated argument form*
 *    NP          - negate task truth
 *    N           - shorthand for NP when belief truth is irrelevant or assumed true
 *    PN          - negate belief truth
 *    NN          - negate task and belief truth
 *    DD		  - depolarize task & belief
 *    DP,DN	      - depolarize task
 *    PD,ND		  - depolarize belief
 *
 *    X           - task and belief arguments swapped.  this is applied to the above and will always appear as the final suffix modifier
 */
public class TruthModel {

	/** used during construction */
	private final transient UnifriedMap<Term, TruthFunction> table = new UnifriedMap<>(256);

	public TruthModel(TruthFunctions[] values) {
		add(values);
		table.trimToSize();
	}

	public final @Nullable TruthFunction get(Term a) {
		return table.get(a);
	}

	protected void add(TruthFunction... values) {
		for (var t : values)
			add(t);
	}

	protected void add(TruthFunction t) {

		_add(t); //default, no modifiers

		_add(t, "P"); //PP
		_add(t, "PP"); //PP

		_add(new RepolarizedTruth(t, -1, +1, "N")); //NP
		_add(new RepolarizedTruth(t, -1, +1, "NP")); //NP

		_add(new RepolarizedTruth(t, 0, 0, "DD"));

		if (!t.single()) {

			_add(new RepolarizedTruth(t, +1, -1, "PN"));
			_add(new RepolarizedTruth(t, -1, -1, "NN"));

			_add(new RepolarizedTruth(t, +1, 0, "PD"));
			_add(new RepolarizedTruth(t, -1, 0, "ND"));
			_add(new RepolarizedTruth(t, 0, +1, "DP"));
			_add(new RepolarizedTruth(t, 0, -1, "DN"));
		}
	}

	protected void _add(TruthFunction t) {
		_add(t, "");
	}

	/** adds it and the swapped */
	protected void _add(TruthFunction t, String postfix) {
        var name = t + postfix;
		__add(name, t);
		if (!t.single())
			__add(name + "X",
				t instanceof RepolarizedTruth ?  ((RepolarizedTruth)t).swapped() : new TruthFunction.SwappedTruth(t));
	}

	private void __add(String name, TruthFunction t) {
		table.put(Atomic.atomic(name), t);
	}
}