package nars.table.dynamic;

import nars.Term;
import nars.term.atom.BytesAtom;

import static nars.action.transform.Abbreviate.unabbreviateTerm;

public class AbbreviationBeliefTable extends ProxyBeliefTable {

	public AbbreviationBeliefTable(BytesAtom abbr, boolean beliefOrGoal) {
		super(abbr, beliefOrGoal);
	}

	@Override
	public Term resolve(Term external) {
		return unabbreviateTerm(external);
	}

}