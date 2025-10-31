package nars.focus.util;

import jcog.pri.PLink;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.op.PriMerge;
import jcog.thing.Part;
import nars.Focus;
import nars.NAR;
import nars.Term;
import nars.time.part.DurLoop;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;

public class FocusBag extends ArrayBag<Term, PLink<Focus>> {

	private final DurLoop loop;

	public FocusBag(int capacity, NAR n) {
		super(
			//PriMerge.plus
			PriMerge.replace
		);
		setCapacity(capacity);

		//sharp = 1;
		loop = n.onDur(this::updateDur);
		n.eventOnOff.on(this::indexPartChange);
	}

	private void updateDur() {
		commit(p -> p.pri(p.id.freq()));
	}

	public final Focus add(Focus w) {
		return put(new PLink<>(w, w.freq())).id;
	}
	
	public final boolean remove(Focus w) {
		return remove(w.id)!=null;
	}

	@Override public Term key(PLink<Focus> p) {
		return p.id.id;
	}

	/**
	 * updates indexes when a part is added or removed
	 *
	 * @param change a change event emitted by Parts
	 */
	private void indexPartChange(ObjectBooleanPair<Part<NAR>> change) {
		Part<NAR> p = change.getOne();
		if (p instanceof Focus w) {
			if (change.getTwo()) {
				add(w); //TODO handle rejection, eviction etc
			} else {
				remove(w.id);
			}
		}
	}

	public ObjectFloatHashMap<Focus> toMap() {
		var m = new ObjectFloatHashMap(size());
		forEach(z -> m.put(z.id, z.priElseZero()));
		return m;
	}
}