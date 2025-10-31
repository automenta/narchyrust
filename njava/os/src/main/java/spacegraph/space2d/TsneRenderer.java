package spacegraph.space2d;

import com.jogamp.opengl.GL2;
import jcog.TODO;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.data.map.CellMap;
import jcog.table.DataTable;
import jcog.table.DataTable.Instance;
import jcog.tree.rtree.rect.RectF;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.Graph2D.Graph2DRenderer;
import spacegraph.space2d.container.graph.Graph2D.GraphEditor;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.video.Draw;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.DoubleSummaryStatistics;
import java.util.stream.Stream;

public abstract class TsneRenderer implements Graph2DRenderer<Instance> {

    /** TODO call off.close() if widget deleted */
    public static Graph2D view(Table data, @Nullable MetalBitSet cols, int priColumn, int scaleColumn) {
        if (cols == null) {
            cols = MetalBitSet.bits(data.columnCount()).negateThis();
            if (priColumn>=0)
                cols.clear(priColumn);
        }

        var m = new TsneModel(cols, scaleColumn);

        var r = new TsneRenderer() {
            double min, max;

            @Override
            public void nodes(CellMap<Instance, ? extends NodeVis<Instance>> cells, GraphEditor<Instance> edit) {
                if (priColumn>=0) {
                    var scoreStats = stats(data.column(priColumn));
                    min = scoreStats.getMin();
                    max = scoreStats.getMax();
                }
                super.nodes(cells, edit);
            }

            static DoubleSummaryStatistics stats(Column<?> values) {
                var s = new DoubleSummaryStatistics();
                int n = values.size();
                for (int i = 0; i < n; i++)
                    s.accept(value(values, i));
                return s;
            }

            private static double value(Column<?> values, int i) {
                double v;
                switch (values) {
                    case DoubleColumn D -> v = D.getDouble(i);
                    case BooleanColumn B -> v = B.get(i) ? 1 : 0;
                    case IntColumn I -> v = I.getInt(i);
                    default -> throw new TODO("unsupported column class " + values.getClass() + " for column " + values);
                }
                return v;
            }

            @Override
            protected void paintNode(GL2 gl, Surface surface, Instance id) {
                double scoreNormal;
                if (priColumn>=0) {
                    double score = (Double) id.data.get(priColumn);
                    scoreNormal = Util.normalizeRange(score, min, max);
                } else
                    scoreNormal = 1;
                //Draw.colorGrays(gl, (float)(0.1f + 0.9f * scoreNormal));
                Draw.hsl(gl, (float) scoreNormal, 0.9f, 0.5f, 0.5f);
                Draw.rect(surface.bounds, gl);
            }
        };

        return new Graph2D<Instance>()
                .update(m)
                .render(r)
                .set(rows(data));
    }

    public static Stream<Instance> rows(Table data) {
        return data.stream().map(rr -> DataTable.instance(rr, data));
    }


    @Override
    public void node(NodeVis<Instance> node, GraphEditor<Instance> graph) {
        node.set(new PushButton() {
            @Override protected void paintWidget(RectF bounds, GL2 gl) { }

            @Override
            protected void paintIt(GL2 gl, ReSurface r) {
                paintNode(gl, this, node.id);
            }
        }.clicked(() -> System.out.println(node.id.data)));
    }

    abstract protected void paintNode(GL2 gl, Surface surface, Instance id);
//        Draw.colorHash(gl, id.hashCode(), 0.8f);
//        Draw.rect(surface.bounds, gl);
//    }
}