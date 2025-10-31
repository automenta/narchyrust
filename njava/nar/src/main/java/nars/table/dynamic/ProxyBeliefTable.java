package nars.table.dynamic;

import nars.Answer;
import nars.NALTask;
import nars.Term;
import nars.action.memory.Remember;
import nars.task.SerialTask;
import nars.task.proxy.SpecialTermTask;
import nars.term.util.TermTransformException;

/** transforms task insertions to another concept's belief table */
public abstract class ProxyBeliefTable extends DynBeliefTable {

	protected ProxyBeliefTable(Term c, boolean beliefOrGoal) {
		super(c, beliefOrGoal);
	}

	/** map an external term to an internal term */
	public abstract Term resolve(Term external);

	@Override
	public final void remember(Remember r) {

        var external = r.input;
		if (external instanceof SerialTask)
			return; //TODO test

        var externalTerm = external.term();
        var internalTerm = resolve(externalTerm);

        var internalConcept = r.nar().conceptualizeTask(internalTerm);
		if (internalConcept == null)
			return; //??

        var externalConcept = r.concept;
		try {
			r.concept = internalConcept;
			r.input = internal(external, externalTerm, internalTerm);

			if (internalConcept.table(external.punc()).tryRemember(r)) {
				//restore original input
				//r.stored = external.copyMetaAndCreation(r.stored);
				r.stored = external.asLateAs(r.stored);
			} else {
				//assert(r.stored==null);
			}
			r.input = external;
		} finally {
			//restore
			r.concept = externalConcept;
		}

	}

	private static NALTask internal(NALTask external, Term externalTerm, Term internalTerm) {
        var internal = SpecialTermTask.proxy(external, internalTerm, true);
		if (internal == null)
			throw new TermTransformException("proxy task failure", externalTerm, internalTerm);
		return internal.copyMetaAndCreation(external);
	}


	/** forward to the host concept's appropriate table */
	@Override  public void match(Answer a) {

        var _x = a.template();
		Term x;
		if (_x == null) x = term; //use default
		else x = _x;

        var y = resolve(x);
        var t = a.nar.table(y, beliefOrGoal);
		if (t == null)
			return;

		assert(t!=this);
		a.template(y); //save (push)
		a.match(t);
		a.template(_x); //restore (pop)
		a.replace(y, x);
	}


}