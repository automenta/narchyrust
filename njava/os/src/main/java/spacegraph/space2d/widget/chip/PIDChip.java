package spacegraph.space2d.widget.chip;

import jcog.pid.MiniPID;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.meta.MetaFrame;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.port.FloatPort;
import spacegraph.space2d.widget.text.Labelling;

/** TODO abstract to generic process controller */
public class PIDChip extends Widget {

    public final FloatPort actual = new FloatPort();
    public final FloatPort out = new FloatPort();
    private final MiniPID pid;

    public PIDChip() {
        this(new MiniPID(0.5,0.5,0.5));
    }

    public PIDChip(MiniPID pid) {
        this.pid = pid;
        set(new Gridding(
            new MetaFrame(pid),
            new Gridding(
                Labelling.the("ACTUAL", actual.update(this::update)),
                Labelling.the("OUT", out)
            )
        ));
    }

    protected void update() {
        double a = actual.getAsDouble();
        if (a == a)
            this.out.out((float) this.pid.out(a));
    }
}