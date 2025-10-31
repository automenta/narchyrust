package nars.action.answer;

import com.google.common.collect.Iterables;
import nars.Deriver;
import nars.Task;
import nars.Term;
import nars.focus.time.ActionTiming;
import nars.term.Termed;
import nars.term.util.map.IndexedTermedList;

public class AnswerQuestionsFromIndex<T extends Termed> extends AnswerQuestionsFromConcepts.AnswerQuestionsFromConceptIndex {

	final int size;
	private final IndexedTermedList<T> list;

	public AnswerQuestionsFromIndex(IndexedTermedList<T> list, ActionTiming timing) {
		super(timing);
		this.list = list;
		this.size = list.size();
	}

	@Override
	protected int tries(Deriver d) {
		return size;
	}

	@Override
	protected Iterable<Term> source(Task q, Deriver d) {
        //List<Term> iii = new FasterList(ii);
		return Iterables.transform(list.match(q.term(), true), z -> term(z).normalize());
	}

	protected Term term(T t) {
		return t.term();
	}
}