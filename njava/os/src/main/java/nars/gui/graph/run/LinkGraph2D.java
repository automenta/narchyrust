package nars.gui.graph.run;

import jcog.data.map.CellMap;
import jcog.pri.PLink;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Term;
import nars.link.TaskLink;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.var.Variable;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.container.graph.EdgeVis;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.NodeVis;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/** tasklinks and termlinks */
public class LinkGraph2D extends TermGraph2D {

    private final LinkVis vis;

    private LinkGraph2D(NAR n) {
        super(n);
        render(
                Graph2D.InvalidateEdges,
                //new TermlinkVis(n),
                //new SubtermVis(n),
                this.vis = new LinkVis()
                //new StatementVis(n)
        );
    }

    @Override
    protected void _set(Stream links, int n) {
        super._set(links, n);

        if (nodeCount()>0) {
            for (Object t : buffer)
                vis.update(t, edit);
        }
    }

//    @Override
//    protected Stream<Term> terms(Object t) {
//        if (t instanceof TaskLink z)
//            return z.self() ? Stream.of(z.from()) : Stream.of(z.from(), z.to());
//        else
//            return Stream.of(((NALTask)t).term());
//    }

//    public static Surface linkGraph(Focus links, NAR n) {
//
//        LinkGraph2D g = new LinkGraph2D(n);
//
//        return NARui.get(g.widget(), () -> {
//            g.setAll(
//                (//Stream.concat(links.tasks.stream(),
//                    links.links.stream()
//                ));
//        }, n);
//            //.every();
//
//    }


//    @Override
//    protected Termed key(T termed) {
//        //collapse self termlinks to the their term
//        return termed instanceof Premise ? ((Premise) termed).from()/*.concept()*/ : termed;
//    }

//	@Override
//	protected void addControls(Gridding cfg) {
//        cfg.add(new ObjectSurface(maxNodes));
//	}

    private static class LinkVis implements Graph2D.Graph2DRenderer<Object> {
        float COLOR_LERP_RATE = 0.1f;

        public final AtomicBoolean belief = new AtomicBoolean(true);
        public final AtomicBoolean goal = new AtomicBoolean(true);
        public final AtomicBoolean question = new AtomicBoolean(true);
        public final AtomicBoolean quest = new AtomicBoolean(true);
        public final AtomicBoolean subtermLinks = new AtomicBoolean(false);
        private final float[] tPri = new float[4];
        /**
         * a non-volatile cache; is this helpful?
         */
        private transient boolean _belief;
        private transient boolean _goal;
        private transient boolean _question;
        private transient boolean _quest;


        private LinkVis() {
        }

        @Override
        public void nodes(CellMap<Object, ? extends NodeVis<Object>> cells, GraphEditor<Object> edit) {

            _belief = this.belief.getOpaque();
            _goal = this.goal.getOpaque();
            _question = this.question.getOpaque();
            _quest = this.quest.getOpaque();
            if (!_belief && !_goal && !_question && !_quest)
                return;

            cells.forEachValue(nv -> {
                nv.pri = 0; //reset priorities
            });

            Graph2DRenderer.super.nodes(cells, edit);
        }

        @Override
        public void node(NodeVis<Object> n, GraphEditor<Object> graph) {
            Object x = n.id;
            //if (x instanceof TaskLink l)
            update(x, graph);
        }

        public void update(Object x, GraphEditor<Object> graph) {
            Term from, to;
            boolean self;

            if (x instanceof TaskLink l) {
                from = l.from();
                to = l.to();
                self = l.self();
            } else {
                if (x instanceof PLink p)
                    x = p.id;
                Term X = ((Termed) x).term();
                from = to = X.CONCEPTUALIZABLE() ? X.concept() : X;
                self = true;
            }

            @Nullable NodeVis fromNode = graph.node(from);
            if (fromNode == null)
                return;

            float pri = x instanceof Prioritized p ? p.priElseZero() : 0;


            if (self) {
                fromNode.pri += pri;
            } else {
                var toNode = from == to ? fromNode : graph.node(to);
                if (toNode != null) {
                    //share half
                    float ph = pri / 2;
                    fromNode.pri += ph;
                    toNode.pri += ph;
                } else {
                    fromNode.pri += pri;
                }
            }

            if (to instanceof Variable || self)
                return; //done if variables or self links

            EdgeVis<Termed> e = graph.edge(fromNode, to);
            if (e != null) {

                e.colorLerp(COLOR_LERP_RATE, COLOR_LERP_RATE, COLOR_LERP_RATE, COLOR_FADE_RATE); //still visible a bit
//                if (l instanceof TaskLink) {
                float pSum = colorTaskLink(x, e);
//                } else {
//                    pSum = pri;
//                    colorLink(e, NALTask.i(l.task().punc()), pSum);
//                }
                e.weightLerp(COLOR_LERP_RATE + 0.6f * pSum, WEIGHT_UPDATE_RATE);
            }

            //subterm links
            TaskLink l = (TaskLink) x;
            if (subtermLinks.getOpaque()) {
                Term xx = l.from().term();
                if (xx instanceof Compound) {
                    Subterms ss = xx.subtermsDirect();
                    float a = 0.01f / ss.subs() * COLOR_UPDATE_RATE;
                    for (Term s : ss) {
                        s = s.unneg();
                        if (s.CONCEPTUALIZABLE()) {
                            EdgeVis gg = graph.edge(xx, graph.nodeOrAdd(s));
                            if (gg != null)
                                gg.colorAddLerp(1, 1, 1, a).weightAddLerp(a, WEIGHT_UPDATE_RATE);
                        }
                    }
                }
            }
        }

        public float colorTaskLink(Object t, EdgeVis<Termed> e) {
            float[] tPri = this.tPri;
            if (t instanceof TaskLink l)
                l.priGet(tPri);
//            else
//                loadTask((NALTask)t, tPri);

            //FILTERS
            filter(tPri);

            return sum(e, tPri);
        }

        //        private static void loadTask(NALTask tt, float[] tPri) {
//            var tp = tt.priElseZero()/4;
//            switch (tt.punc()) {
//                case BELIEF -> tPri[0] = tp;
//                case QUESTION -> tPri[1] = tp;
//                case GOAL -> tPri[2] = tp;
//                case QUEST -> tPri[3] = tp;
//            }
//        }

        private void filter(float[] tPri) {
            if (!_belief)   tPri[0] = 0;
            if (!_question) tPri[1] = 0;
            if (!_goal)     tPri[2] = 0;
            if (!_quest)    tPri[3] = 0;
        }

        private float sum(EdgeVis<Termed> e, float[] tPri) {
            float pSum = 0;
            for (int i = 0; i < 4; i++) {
                float ppi = tPri[i];
                if (ppi >= Prioritized.EPSILON) {
                    pSum += ppi;
                    colorLink(e, i, ppi);
                }
            }
            return pSum / 4;
        }

        public void colorLink(EdgeVis<Termed> e, int priID, float ppi) {
			      /*
                https://www.colourlovers.com/palette/848743/(_%E2%80%9D_)
                BELIEF   Red     189,21,80
                QUESTION Orange  233,127,2
                GOAL     Green   138,155,15
                QUEST    Yellow  248,202,0
                */
            int b;
            int g;
            int r;
            switch (priID) {
                case 0 -> {
                    r = 189;
                    g = 21;
                    b = 80;
                }
                case 1 -> {
                    r = 2;
                    g = 127;
                    b = 233;
                }
                case 2 -> {
                    r = 138;
                    g = 155;
                    b = 15;
                }
                case 3 -> {
                    r = 2;
                    g = 233;
                    b = 127;
                }
                default -> throw new UnsupportedOperationException();
            }

            e.colorLerp(r / 256f, g / 256f, b / 256f, COLOR_UPDATE_RATE * ppi);
        }
    }
}