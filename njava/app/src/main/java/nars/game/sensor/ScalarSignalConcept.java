package nars.game.sensor;

import nars.BeliefTable;
import nars.NAR;
import nars.Term;
import nars.focus.PriAmp;

/** an independent scalar (1-dimensional) signal
 * TODO impl that takes a lambda Supplier<Truth> for the signal and base ScalarSignal off that to remove its truth() method
 * */
public class ScalarSignalConcept extends SignalConcept {

	public ScalarSignalConcept(Term term, BeliefTable beliefTable, BeliefTable goalTable, NAR n) {
		this(term, beliefTable, goalTable, null, n);
	}

	public ScalarSignalConcept(Term term, NAR n) {
		this(term,
			beliefTable(term, true, true, n),
			beliefTable(term, false, false, n), n);
	}

	protected ScalarSignalConcept(Term term, BeliefTable beliefTable, BeliefTable goalTable, PriAmp pri, NAR n) {
		super(term, beliefTable, goalTable, n);
	}


}