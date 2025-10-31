package nars.gui.graph.run;

import jcog.Util;
import jcog.data.list.Lst;
import jcog.data.set.ArrayHashSet;
import jcog.pri.PLink;
import jcog.signal.IntRange;
import jcog.util.Flip;
import nars.NALTask;
import nars.NAR;
import nars.Premise;
import nars.Term;
import nars.gui.NARui;
import nars.link.TaskLink;
import nars.term.Termed;
import nars.term.var.Variable;
import nars.time.part.DurLoop;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.container.collection.MutableListContainer;
import spacegraph.space2d.container.graph.EdgeVis;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.container.layout.Force2D;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.meta.obj.ObjectSurface;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.Labelling;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * TODO edge capacity limiting
 */
public abstract class TermGraph2D<T> extends Graph2D<Termed> {
    public final IntRange limit = new IntRange(128, 0, 2048);

    protected final Lst<T> buffer = new Lst<>();
    protected final Flip<ArrayHashSet<Termed>> termbuffer = new Flip<>(ArrayHashSet::new);

    final AtomicBoolean busy = new AtomicBoolean(false);

    private final NAR nar;
    private DurLoop on;

//    public class Controls {
//    public final AtomicBoolean update = new AtomicBoolean(true);
//    }

    protected TermGraph2D(NAR n) {
        super();

        this.nar = n;

        int volMaxForLabel = 16;

        build(nn -> {
            Termed x = nn.id;
            Term xtt = x.term();
            nn.set(
                new Scale(
                    new PushButton(
                        new VectorLabel(xtt.complexity() < volMaxForLabel ?
                                x.toString() :
                                xtt.op().str),
                        () -> {
                            if (x instanceof Premise)
                                NARui.premiseWindow((Premise)x, nar);
                            else
                                NARui.conceptWindow(x.term(), nar);
                        }
                    ).color(0.5f,0.5f,0.5f,0.25f), 0.8f
                )
            );
        });

        this.update(getLayout());
//        layout(new TreeMap2D<>() {
//            @Override
//            public void layout(Graph2D<Term> g, int dtMS) {
//
//                g.forEachValue(nn -> {
//                    if (nn.showing())
//                        updateNode(nn);
//                });
//                super.layout(g, dtMS);
//            }
//        })

    }

    @Override
    protected void addControls(MutableListContainer cfg) {
        cfg.add(Labelling.the("capacity", new ObjectSurface(limit)));
    }

    @Override
    protected void doLayout(float dtS) {
        if (!Util.enterAlone(busy))
            return;
        try {
            super.doLayout(dtS);
        } finally {
            Util.exitAlone(busy);
        }
    }

    public void setAll(Stream<T> links) {
        if (!Util.enterAlone(busy)) return;
        try {
            int n = (int) Math.ceil(limit.asFloat());
            _set(links, n);
        } finally {
            Util.exitAlone(busy);
        }
    }

    protected void _set(Stream<T> links, int n) {
        //TODO double buffer
        Lst<T> in = this.buffer;
        in.clear();
        //in.ensureCapacity(Math.min(n, links.capacity()));

        links.limit(n).forEach(in::add);
//        var pp = links.iterator();
//        while (pp.hasNext()) {
//            var p = pp.next();
//            //if (p == null) continue;
//            in.add(p);
//            if (in.size() >= n) break;
//        }

        ArrayHashSet terms = this.termbuffer.commitWrite();
        terms.clear();

        if (!in.isEmpty()) {
            Consumer termsAdd = terms::add; //TODO addFast

            Predicate f = termFilter();

            for (T x : in) {
                terms(x, f, termsAdd); //HACK
                if (terms.size() >= n) //TODO limit y ahead of the add
                    break;
            }

        }

        set(terms.list);
    }

    @Nullable protected Predicate<? super Termed> termFilter() {
        return null;
    }

    /** extract terms */
    protected void terms(T z, @Nullable Predicate<T> filter, Consumer<T> each) {
        Object x;
        if (z instanceof PLink p)
            x = p.id;
        else
            x = z;

        if (x instanceof TaskLink l) {
            each.accept((T)l.from());
            if (!l.self())
                each.accept((T)l.to());
            return;
        }
        if (x instanceof NALTask t)
            x = t.term().concept();

        Object y = x instanceof Termed t ? t.term() : null;
        if (y != null) {
            if (filter != null && !filter.test((T) y))
                return;

            each.accept((T) y);
        }
    }

    public Graph2DUpdater getLayout() {
        return new Force2D<>();
        //return new EfficientForce2D<>();
        //return new SemiForce2D.TreeForce2D<>();

    }

//    static void updateNode(NodeVis<Termed> nn) {
//        Termed i = nn.id;
//        if (i != null && nn.visible()) {
//
//            float pri =
//                    //Math.max(nar.concepts.pri(i, 0f), ScalarValue.EPSILON);
//                    //0.5f;
//                    nn.pri;
//
//            nn.color(pri, pri / 2f, 0f);
//
////            nn.pri = pri;
//        }
//    }

    @Override
    protected void stopping() {
        if (on!=null) {
            on.close();
            on = null;
        }
        super.stopping();
    }

    static final float WEIGHT_UPDATE_RATE = 0.5f;
    static final float COLOR_UPDATE_RATE = 0.5f;
    static final float COLOR_FADE_RATE = 0.125f;

//    private static class TermlinkVis implements Graph2DRenderer<Term> {
//        public final AtomicBoolean termlinks = new AtomicBoolean(true);
//        final NAR n;
//
//        private TermlinkVis(NAR n) {
//            this.n = n;
//        }
//
//        @Override
//        public void node(NodeVis<Term> node, GraphEditing<Term> graph) {
//            if (!termlinks.get())
//                return;
//
//            Concept id = node.id;
//            if (id!=null) {
//                id.termlinks().forEach(l -> {
//                    Graph2D.EdgeVis<Term> e = graph.edge(node, wrap(l.get()));
//                    if (e != null) {
//                        float p = l.priElseZero();
//                        e.weightLerp(p, WEIGHT_UPDATE_RATE)
//                                .colorLerp((0.9f * p) + 0.1f, Float.NaN, Float.NaN, COLOR_UPDATE_RATE)
//                                .colorLerp(Float.NaN,0,0,COLOR_FADE_RATE)
//                        ;
//
//                    }
//                });
//            }
//        }
//    }



//    private class SubtermVis implements Graph2D.Graph2DRenderer<Term> {
//
//        public final AtomicBoolean subterms = new AtomicBoolean(false);
//
//        public final AtomicBoolean visible = new AtomicBoolean(true);
//
//        public final FloatRange strength = new FloatRange(0.1f, 0, 1f);
//
//        final NAR n;
//
//        private SubtermVis(NAR n) {
//            this.n = n;
//        }
//
//        @Override
//        public void node(NodeVis<Term> node, GraphEditing<Term> graph) {
//            if (!subterms.getOpaque())
//                return;
//
//            boolean visible = this.visible.getOpaque();
//
//            float p = strength.floatValue();
//
//            Term t = node.id;
//            if (t == null) return;
//            Concept c = n.concept(t);
//            if (c != null) {
//
//                c.linker().targets().forEach(s -> {
//                    if (s.CONCEPTUALIZABLE()) {
//                        if (t != null) {
//                            int v = t.volume();
//                            @Nullable EdgeVis<Term> e = graph.edge(node, s.term().concept());
//                            if (e != null) {
//                                e.weightLerp(p, WEIGHT_UPDATE_RATE);
//                                if (visible)
//                                    e.colorAdd(p, p, p, COLOR_UPDATE_RATE / v);
//                            }
//                        }
//                    }
//                });
//
//
//            }
//        }
//    }

    private static class StatementVis implements Graph2DRenderer<Term> {
        public final AtomicBoolean statements = new AtomicBoolean(true);
        final NAR n;

        private StatementVis(NAR n) {
            this.n = n;
        }

        @Override
        public void node(NodeVis<Term> node, GraphEditor<Term> graph) {

            if (!statements.get())
                return;

            Term t = node.id;
            if (t.STATEMENT()) {
                Term subj = t.sub(0);
                if (!(subj instanceof Variable)) {
                    Term pred = t.sub(1);
                    if (!(pred instanceof Variable)) {
                        @Nullable EdgeVis<Term> e = graph.edge(subj, pred);
                        if (e != null) {
                            float p = 0.5f;
                            e.weightLerp(p, WEIGHT_UPDATE_RATE)
                                .colorLerp(Float.NaN, Float.NaN, (0.9f * p) + 0.1f, COLOR_UPDATE_RATE)
                            ;
                        }
                    }
                }

//                }
            }
        }

    }


}