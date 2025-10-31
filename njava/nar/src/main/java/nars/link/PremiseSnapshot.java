package nars.link;

import jcog.data.list.Lst;
import jcog.data.list.table.Baglike;
import nars.Premise;

import java.util.Arrays;
import java.util.Iterator;

/** takes snapshots of a premise bag */
public class PremiseSnapshot implements Runnable, Iterable<TaskLink> {

	private final Baglike<?, TaskLink> active;
	protected final Lst<TaskLink> snapshot;
	protected transient Premise[] items = Premise.EmptyPremiseArray;

	public PremiseSnapshot(Baglike<?, TaskLink> active) {
		this.active = active;
		this.snapshot = new Lst<>(0, new TaskLink[active.capacity()]);
	}

	@Override public final void run() {
		synchronized (snapshot) {
			snapshot.clearFast();
			int c = active.capacity();
			//TODO resize bitmap
			snapshot.ensureCapacity(c);
			active.forEach(snapshot::addFast);
			items = snapshot.array();
			int s = snapshot.size();
			if (s < c)
				Arrays.fill(items, s, items.length,null); //clear remainder of array
		}
	}

//	abstract protected void commit();

	@Override
	public Iterator<TaskLink> iterator() {
		return snapshot.iterator();
	}
}
