package nars.gui.concept;

import com.jogamp.opengl.GL2;
import nars.NAR;
import nars.term.Termed;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.util.math.Color3f;
import spacegraph.video.Draw;

import java.util.function.BiConsumer;

import static jcog.Fuzzy.polarize;

public class ConceptColorIcon extends ConceptView {

    final Color3f color = new Color3f(0.5f, 0.5f, 0.5f);
    private final BiConsumer<Termed, Color3f> colorize;

    public ConceptColorIcon(Termed t, NAR n) {
        this(t, defaultColoring(n));
    }

    public ConceptColorIcon(Termed t, BiConsumer<Termed, Color3f> colorize) {
        super(t);

        this.colorize = colorize;

        set(new BitmapLabel(t.term().toString()));
    }

    static BiConsumer<Termed, Color3f> defaultColoring(NAR nar) {
        return (t, color) -> {
            //if (t != null) {

            long now = nar.time();
            float D = nar.dur();
            int d = (int) D;
            var b = nar.beliefTruth(t, now - d/2, now+d/2, D);
            if (b != null) {
                var f = polarize(b.freq());
                //var conf = (float) b.conf();
                //var a = polarity(f);// * 0.25f + conf * 0.75f;
//                color.set((1 - f) * a, f * a, 0);
                //color.set(0,  f * a, (1 - f) * a);
                float R = f < 0 ? -f : 0;
                float G = f > 0 ? +f : 0;
                float B = 0;

                color.set(R, G, B);
                //color.set(f, f, f);
            } else {
                //color.set(0.5f);
                color.set(0);
            }
            //}
        };
    }

    @Override
    protected void paintIt(GL2 gl, ReSurface rd) {
        Draw.color(color, gl);
        Draw.rect(bounds, gl);
    }

    @Override
    public void update(NAR nar) {
//        Concept c = concept(nar);
        //TODO if (c!=null)
        colorize.accept(term, color);
    }
}