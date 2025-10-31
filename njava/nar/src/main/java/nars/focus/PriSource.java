package nars.focus;

import jcog.data.graph.MapNodeGraph;

/** variably adjustable priority source */
public final class PriSource extends PriNode {

	public PriSource(Object id, float p) {
		super(id);
		pri(p);
	}

	@Override
	public void update(MapNodeGraph<PriNode, Object> graph) {

	}

	public void pri(float x) {
		this.pri.pri(x);
	}
}
