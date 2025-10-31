package nars.gui;

import jcog.pri.PriReference;
import jcog.pri.bag.util.Bagregate;
import jcog.pri.op.PriMerge;
import nars.Focus;
import spacegraph.space2d.container.ContainerSurface;
import spacegraph.space2d.container.ScrollXY;
import spacegraph.space2d.container.grid.GridRenderer;
import spacegraph.space2d.container.grid.ListModel;
import spacegraph.space2d.container.unit.MutableUnitContainer;

public abstract class AbstractTermView<X> extends MutableUnitContainer implements GridRenderer<PriReference<X>> {

	private final Bagregate<X> bag;
	private final ScrollXY<PriReference<X>> scroll;
	private final ListModel.AsyncListModel model;
	private final Iterable in;

	protected AbstractTermView(Iterable tasks, int capacity, Focus focus) {
		this.in = tasks;

		int internalCapacity = capacity * 2; //TODO adaptive

		this.bag = new Bagregate<>(internalCapacity, PriMerge.max);

		scroll = new ScrollXY<>(model = new ListModel.AsyncListModel<>(bag, capacity), this);

		set(NARui.get(scroll, this::commit, focus.nar) );
	}

	private void commit() {
		if (visible()) {
			for (var p : in) {
				accept(p);
			}
			bag.commit();
			model.update(scroll);
			var c =  scroll.center();
			if (c!=null)
				((ContainerSurface)c).layout(); //HACK force re-render
		} else {
			bag.clear();
		}
	}

	public final void accept(Object x) {
		//TODO option for only if visible
		if (filter(x))
            bag.put(transform(x), value(x));
	}

	abstract public float value(Object x);

	protected abstract X transform(Object x);

	protected static boolean filter(Object x) {
		return true;
	}

}