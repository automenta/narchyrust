package nars.gui;

import jcog.pri.PriReference;
import nars.Focus;
import nars.Op;
import nars.Task;
import nars.focus.util.TaskAttention;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.util.math.Color4f;

public class TermListView extends AbstractTermView<Task> {

	public TermListView(TaskAttention tasks, int capacity, Focus f) {
		super(tasks, capacity, f);
	}

	@Override
	public float value(Object x) {
		return ((Task)x).priElseZero();
	}

	@Override
	protected Task transform(Object x) {
		return (Task) x;
	}

	@Override
	public Surface apply(int x, int y, PriReference<Task> tt) {
		Task t = tt.get();
		BitmapLabel l = new BitmapLabel(t.toStringWithoutBudget());
		l.align = AspectAlign.Align.LeftCenter;
		l.mipmap = false;
		l.antialias = false;
		Color4f color = l.fgColor;
		float linkPri = t.pri();
		if (linkPri!=linkPri) {
			color.x = color.y = color.z = 0.5f; //deleted
		} else {
			switch (t.punc()) {
				case Op.BELIEF -> {
					color.x = 0.25f + 0.75f * linkPri;
					color.y = 0.25f + 0.75f * linkPri / 3;
					color.z = 0.25f + 0.75f * linkPri / 3;
				}
				case Op.GOAL -> {
					color.x = 0.25f + 0.75f * linkPri / 3;
					color.y = 0.25f + 0.75f * linkPri;
					color.z = 0.25f + 0.75f * linkPri / 3;
				}
				case Op.QUEST, Op.QUESTION -> {
					color.x = 0.25f + 0.75f * linkPri / 3;
					color.y = 0.25f + 0.75f * linkPri / 3;
					color.z = 0.25f + 0.75f * linkPri;
				}
			}
		}
		return l;
	}

}