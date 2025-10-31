package nars.term.util.map;

import com.google.common.collect.Iterables;
import nars.Term;
import nars.term.Compound;
import nars.term.Termed;
import org.roaringbitmap.RoaringBitmap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static nars.Op.ATOM;


public class IndexedTermedList<T extends Termed> {


	public final List<T> list;

	private final Function<Term, Iterable<Term>> decomposer;

	private final Map<Term, RoaringBitmap> index = new HashMap();

	public IndexedTermedList(List<T> list, Function<Term,Iterable<Term>> decomposer) {
		this.list = list;
		this.decomposer = decomposer;

		index(list);
	}

	private void index(List<T> list) {
		int i = 0;
		for(T x : list) {
			for (Term xx : decomposer.apply(x.term()))
				entry(xx).add(i);
			i++;
		}
		optimize();
	}

	private void optimize() {
		for (RoaringBitmap r : index.values()) {
			r.trim();
			r.runOptimize();
		}
	}

	private RoaringBitmap entry(Term x) {
		return index.computeIfAbsent(x, (z)->new RoaringBitmap());
	}

	public Iterable<T> match(Term x, boolean allOrAny) {
		return Iterables.transform(matchIndices(x, allOrAny), list::get); //TODO use IntIterator
	}

	private Iterable<Integer> matchIndices(Term x, boolean allOrAny) {
		RoaringBitmap m  = new RoaringBitmap();
		boolean init = true;
		for (Term xx : decomposer.apply(x)) {
			RoaringBitmap xr = index.get(xx);
			if (xr!=null) {
				if (!allOrAny || init) {
					m.or(xr);
					init = false;
				} else
					m.and(xr);
			} else {
				if (allOrAny) {
					//no results for this term, but all are required
					return Collections.EMPTY_LIST;
				}
			}
		}
		return m;
	}

	public static Function<Term, Iterable<Term>> Atoms = (t) -> {
		if (t.ATOM())
			return List.of(t);
		else if (t instanceof Compound)
			return ((Compound) t).recurseSubtermsToSet(ATOM);
		else
			return Collections.EMPTY_LIST;
	};

	public int size() {
		return list.size();
	}
}