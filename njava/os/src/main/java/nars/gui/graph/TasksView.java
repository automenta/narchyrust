package nars.gui.graph;

import com.jogamp.opengl.GL2;
import nars.NALTask;
import nars.NAR;
import nars.NARS;
import nars.Task;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.space2d.container.time.Timeline2D;
import spacegraph.space2d.container.time.Timeline2DEvents;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.video.Draw;

import java.util.function.Consumer;

import static nars.Op.*;

public class TasksView implements Timeline2D.EventBuffer<NALTask> {

    private static final Consumer<NodeVis<NALTask>> TaskRenderer = (n) -> n.set(new Scale(new TaskIcon(n.id), 0.9f));

    private final Iterable<NALTask> tasks;

    public TasksView(Iterable<NALTask> tasks) {
        this.tasks = tasks;
    }

    public static void main(String[] args) {
        NAR n = NARS.tmp();
        n.log();
        //n.inputAt(0, "(x &&+1 y). |");
        n.inputAt(0, "(x ==>+1 y). |");
        n.inputAt(2,"y! |");
        n.inputAt(3,"x. |");
        n.run(10);

        Iterable<NALTask> tasks = ()->n.tasks().filter(x->!x.ETERNAL()).iterator();

        Timeline2D t = timeline(tasks, 0, n.time());
        SpaceGraph.window(t.withControls(), 1200, 500);
    }

    public static Timeline2D timeline(Iterable<NALTask> tasks, long s, long e) {
        return new Timeline2D(s, e, new Stacking()).addEvents(new TasksView(tasks), TaskRenderer, new Timeline2DEvents.LaneTimelineUpdater());
    }

    @Override
    public Iterable<NALTask> events(long start, long end) {
        return tasks;
    }

    @Override
    public long[] range(NALTask t) {
        //if (!t.term().CONJ())
            return new long[] { t.start(), t.end()+1 };
        //else
            //return new long[] { t.start(), t.term().seqDur() + t.end()+1 };
    }


    static class TaskIcon extends PushButton {
        public final Task task;

        float r;
        float g;
        float b;
        float a;

        TaskIcon(NALTask x) {
            super(x.toStringWithoutBudget());
            this.task = x;


            switch (x.punc()) {
                case BELIEF -> {
                    float f = x.freq();
                    r = 0.8f * (1f - f);
                    g = 0.8f * f;
                    b = 0.2f + 0.8f * (float) x.conf();
                    a = 1;
                }
                case GOAL -> {
                    float f = x.freq();
                    g = 0.8f * f;
                    b = r = 0;
                    a = 0.2f + 0.8f * (float) x.conf();
                }
                case QUESTION -> {
                    r = 0;
                    g = 0.25f;
                    b = 1f;
                    a = 0.25f;
                }
                case QUEST -> {
                    r = 0;
                    g = 0.5f;
                    b = 0.75f;
                    a = 0.25f;
                }
            }
        }

        @Override
        protected void paintIt(GL2 gl, ReSurface r) {
            Draw.rectRGBA(bounds, this.r, g, b, 0.5f, gl);
        }

        @Override
        public Surface move(float dxIgnored, float dy) {
            return super.move(0, dy); 
        }

        @Override
        public float radius() {
            return h(); 
        }

    }

}