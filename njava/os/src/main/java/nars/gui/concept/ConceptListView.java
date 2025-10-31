package nars.gui.concept;

import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.Focus;
import nars.Term;
import nars.gui.AbstractTermView;
import nars.term.Termed;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.VectorLabel;

public class ConceptListView extends AbstractTermView<Term> {

	/** TODO add switch between Tasks and Links */

	public ConceptListView(Focus f, int capacity) {
		super(f.attn._bag, capacity, f);
	}

	@Override
	public float value(Object x) {
		return ((PLink)x).priElseZero();
	}

	@Override
	protected Term transform(Object x) {
		return ((PLink<? extends Termed>)x).id.term();
	}

	@Override
	public Surface apply(int x, int y, PriReference<Term> value) {
		return new PushButton(new VectorLabel(value.get().toString()));
	}
}